package org.rhq.server.metrics;

import static java.util.Collections.emptySet;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.FutureCallback;

/**
 * Tests {@link TraitsDAO}.
 */
public class TraitsDAOTest extends CassandraIntegrationTest {

    private final Log log = LogFactory.getLog(TraitsDAOTest.class);

    private TraitsDAO dao;

    @BeforeClass
    public void initDAO() throws Exception {
        TraitsCleanup.LIMIT = 25; // for testing paging
        dao = new TraitsDAO(storageSession, new TraitsConfiguration());
    }

    @AfterClass
    public void after() {
        dao.shutdown();
    }

    @BeforeMethod
    public void resetDB() throws Exception {
        session.execute("TRUNCATE " + TraitsDAO.TABLE);
    }

    @Test
    public void simpleUpdate() throws Exception {
        log.info("simpleUpdate");
        int scheduleId = 1;

        assertEquals(dao.currentTrait(scheduleId), null, "no trait data expected");

        long timestamp = System.currentTimeMillis();
        String value = "testing";
        MeasurementDataTrait data = trait(scheduleId, timestamp, value);
        insertTrait(data);

        assertEquals(dao.currentTrait(scheduleId), data, "inserted trait data expected");

        String value2 = "testing 2";
        MeasurementDataTrait data2 = trait(scheduleId, timestamp + 10000, value2);
        insertTrait(data2);
        assertEquals(dao.currentTrait(scheduleId), data2, "inserted trait data expected");

        value2 = "testing 2";
        MeasurementDataTrait data3 = trait(scheduleId, timestamp + 20000, value2);
        insertTrait(data3); // duplicate trait value (should be ignored)

        List<MeasurementDataTrait> history = dao.historyFor(scheduleId);
        assertEquals(2, history.size(), "only should have two records");
        assertEquals(data2, history.get(0), "newest value first");
        assertEquals(data, history.get(1), "oldest");

        log.debug("do cleanup");
        dao.cleanup(new Date(0)); // remove duplicated row 'data3'
        history = dao.historyFor(scheduleId);
        assertEquals(2, history.size(), "still have two records");
        assertEquals(data2, history.get(0), "newest");
        assertEquals(data, history.get(1), "oldest");
        dao.deleteSchedule(scheduleId);

        history = dao.historyFor(scheduleId);
        assertEquals(0, history.size(), "no history");

        assertEquals(null, dao.currentTrait(scheduleId), "no trait datak");

        Set<MeasurementDataTrait> traits = new HashSet<MeasurementDataTrait>();
        traits.add(data);
        traits.add(data2);
        traits.add(data3);
        final CountDownLatch latch = new CountDownLatch(1);
        FutureCallback<Object> callback = new FutureCallback<Object>() {

            @Override
            public void onSuccess(Object result) {
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("failure", t);
                latch.countDown();
            }
        };
        dao.insertTraits(traits, callback);
        boolean await = latch.await(1, TimeUnit.MINUTES);
        assertEquals(await, true, "should have gotten success");

        Collection<MeasurementSchedule> schedules = new ArrayList<MeasurementSchedule>();
        MeasurementSchedule s1 = new MeasurementSchedule();
        s1.setId(data2.getScheduleId());
        schedules.add(s1);
        history = dao.queryAll(schedules, true);
        assertEquals(history.size(), 2);
        history = dao.queryAll(schedules, false);
        assertEquals(history.size(), 1);
    }

    private MeasurementDataTrait trait(int scheduleId, long timestamp,
            String value2) {
        return new MeasurementDataTrait(new MeasurementDataPK(timestamp + 10000, scheduleId), value2);
    }

    private void insertTrait(MeasurementDataTrait data) throws Exception {
        dao.insertTrait(data).get();
    }

    @Test
    public void testCleanup() throws Exception {

        long start = System.currentTimeMillis() - 100000;
        int count = 50;
        for (int i = 0; i < count; i++) {
            long timestamp = start + (5000 * i);
            String value = "testing";
            insertTrait(trait(i, timestamp, value));
            insertTrait(trait(i, timestamp + 2000, value));
            insertTrait(trait(i, timestamp + 3000, value + "x"));
        }

        assertEquals(count(), count * 3);
        dao.cleanup(new Date(start));
        // the count isn't exactly * 2 because of how the result paging works.
        // by selecting > token(x), the some times are split...
        int count2 = count();
        assertEquals(count2 >= count * 2, true, "retain all rows " + count2);
        assertEquals(count2 <= count * 2 + 10, true, "delete most rows " + count2);
    }

    private int count() {
        ResultSet result = storageSession.execute("select count(*) from " + TraitsDAO.TABLE);
        return (int)result.one().getLong(0);
    }

    @Test
    public void testEmptySet() throws Exception {
        Set<MeasurementDataTrait> traits = emptySet();
        final CountDownLatch latch = new CountDownLatch(1);
        FutureCallback<Object> callback = new FutureCallback<Object>() {
            public void onSuccess(Object result) { latch.countDown(); }
            public void onFailure(Throwable t) {}
        };
        dao.insertTraits(traits, callback);
        boolean await = latch.await(1, TimeUnit.MINUTES);
        assertEquals(await, true, "should have gotten success");
    }

}
