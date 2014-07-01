package org.rhq.server.metrics;

import static java.util.Collections.singleton;
import static org.testng.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Tests {@link CallTimeDAO}.
 */
public class CallTimeDAOTest extends CassandraIntegrationTest {

    private final Log log = LogFactory.getLog(CallTimeDAOTest.class);

    private CallTimeDAO dao;

    private CallTimeConfiguration config;

    @BeforeClass
    public void initDAO() throws Exception {
        config = new CallTimeConfiguration();
        dao = new CallTimeDAO(storageSession, config);
    }

    @AfterClass
    public void after() {
        dao.shutdown();
    }

    @BeforeMethod
    public void resetDB() throws Exception {
        session.execute("TRUNCATE " + CallTimeDAO.TABLE);
    }

    @Test
    public void simpleUpdate() throws Exception {
        log.info("simpleUpdate");
        int scheduleId = 1;

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -10);

        c.add(Calendar.HOUR, 1);
        Date d0 = c.getTime();
        c.add(Calendar.HOUR, 1);
        Date d1 = c.getTime();
        c.add(Calendar.HOUR, 1);
        Date d2 = c.getTime();
        c.add(Calendar.HOUR, 1);
        Date d3 = c.getTime();
        c.add(Calendar.HOUR, 1);
        Date d4 = c.getTime();

        CallTimeData data = new CallTimeData(scheduleId);
        String destination  = "dest";
        String destination2 = "dest2";
        double minimum = 4;
        double maximum = 5;
        double total = 100.5;
        long count = 3;
        data.addAggregatedCallData(destination, d1, d2, minimum, maximum, total, count);
        data.addAggregatedCallData(destination2, d2, d3, minimum + 1, maximum + 1, total - 1, count + 2);
        dao.insert(data);

        log.info("select two rows");
        List<CallTimeRow> rows = dao.select(scheduleId, d1, d3);
        assertEquals(rows.size(), 2, "two rows");
        CallTimeRow row0 = rows.get(0);
        assertEquals(destination, row0.getDest());
        assertEquals(d1, row0.getBegin());
        assertEquals(d2, row0.getEnd());
        assertEquals(minimum, row0.getMin());
        assertEquals(maximum, row0.getMax());
        assertEquals(total, row0.getTotal());
        assertEquals(count, row0.getCount());
        CallTimeRow row1 = rows.get(1);
        assertEquals(destination2, row1.getDest());

        CallTimeRow agg = CallTimeRow.aggregate(rows);
        assertEquals(agg.getBegin(), row0.getBegin());
        assertEquals(agg.getEnd(), row1.getEnd());
        assertEquals(agg.getMin(), row0.getMin());
        assertEquals(agg.getMax(), row1.getMax());
        assertEquals(agg.getTotal(), row0.getTotal() + row1.getTotal());
        assertEquals(agg.getCount(), row0.getCount() + row1.getCount());

        log.info("select one row");
        rows = dao.select(scheduleId, d2, d3);
        assertEquals(rows.size(), 1, "one row");

        log.info("select one row based on destination");
        rows = dao.select(scheduleId, destination2, d0, d4);
        assertEquals(rows.size(), 1, "one row");
        assertEquals(destination2, rows.get(0).getDest());

        log.info("delete schedule");
        dao.deleteSchedule(scheduleId).get();
        rows = dao.select(scheduleId, d0, d4);
        assertEquals(rows.size(), 0, "no rows");

        log.info("test async");
        CallTimeData data2 = new CallTimeData(scheduleId + 1);
        data2.addAggregatedCallData(destination, d2, d3, minimum - 1, maximum + 2, 0, 3);

        final CountDownLatch latch = new CountDownLatch(1);
        FutureCallback<Object> callback = new FutureCallback<Object>() {
            public void onSuccess(Object result) {
                latch.countDown();
            }
            public void onFailure(Throwable t) {
                log.error("failure", t);
                latch.countDown();
            }
        };
        dao.insert(singleton(data2), callback);
        boolean await = latch.await(1, TimeUnit.MINUTES);
        assertEquals(await, true, "should have gotten success");

        log.info("test async result");
        rows = dao.select(data2.getScheduleId(), d0, d4);
        assertEquals(rows.size(), 1, "one row");

        log.info("test idempotent");
        config.setIdempotentInsert(true);
        dao.deleteSchedule(scheduleId).get();
        dao.insert(data).get();
        dao.insert(data).get();
        log.info("duplicate insert, but find one row only");
        rows = dao.select(scheduleId, d1, d2);
        assertEquals(rows.size(), 1, "one row");
    }

}
