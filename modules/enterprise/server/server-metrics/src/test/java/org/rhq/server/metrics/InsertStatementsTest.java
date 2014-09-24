package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.datastax.driver.core.PreparedStatement;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
public class InsertStatementsTest extends MetricsTest {

    private InsertStatements insertStatements;

    @BeforeClass
    public void setUpClass() {
        dateTimeService.setConfiguration(configuration);
        insertStatements = new InsertStatements(new StorageSession(session), dateTimeService, configuration);
    }

    @BeforeMethod
    public void setUp() {
        purgeDB();
    }


    @Test
    public void insertAfterInitializingStatements() {
        insertStatements.init();
        DateTime today = dateTimeService.current24HourTimeSlice();
        DateTime oldestDay = today.minusDays(7);

        PreparedStatement rawInsertToday = insertStatements.raw.get(today);
        session.execute(rawInsertToday.bind(100, today.plusHours(2).toDate(), 2.22));

        PreparedStatement rawInsert7DaysAgo = insertStatements.raw.get(oldestDay);
        session.execute(rawInsert7DaysAgo.bind(100, oldestDay.plusHours(2).toDate(), 1.11));

        assertRawDataEquals(100, oldestDay, today.plusDays(1), asList(
            new RawNumericMetric(100, oldestDay.plusHours(2).getMillis(), 1.11),
            new RawNumericMetric(100, today.plusHours(2).getMillis(), 2.22)
        ));

        PreparedStatement insert1HourToday = insertStatements.oneHour.get(today);
        session.execute(insert1HourToday.bind(100, today.plusHours(2).toDate(), 200.0, 200.0, 200.0));

        PreparedStatement insert1Hour7DaysAgo = insertStatements.oneHour.get(oldestDay);
        session.execute(insert1Hour7DaysAgo.bind(100, oldestDay.plusHours(2).toDate(), 100.0, 100.0, 100.0));

        assert1HourDataEquals(100, asList(
            new AggregateNumericMetric(100, Bucket.ONE_HOUR, 100.00, 100.0, 100.0, oldestDay.plusHours(2).getMillis()),
            new AggregateNumericMetric(100, Bucket.ONE_HOUR, 200.00, 200.0, 200.0, today.plusHours(2).getMillis())
        ));

        PreparedStatement insert6Hour7DaysAgo = insertStatements.sixHour.get(oldestDay);
        session.execute(insert6Hour7DaysAgo.bind(100, oldestDay.plusHours(6).toDate(), 100.0, 100.0, 100.0));

        PreparedStatement insert6HourToday = insertStatements.sixHour.get(today);
        session.execute(insert6HourToday.bind(100, today.plusHours(6).toDate(), 200.0, 200.0, 200.0));

        assert6HourDataEquals(100, asList(
            new AggregateNumericMetric(100, Bucket.SIX_HOUR, 100.00, 100.0, 100.0, oldestDay.plusHours(6).getMillis()),
            new AggregateNumericMetric(100, Bucket.SIX_HOUR, 200.00, 200.0, 200.0, today.plusHours(6).getMillis())
        ));

        PreparedStatement insert24Hour7DaysAgo = insertStatements.twentyFourHour.get(oldestDay);
        session.execute(insert24Hour7DaysAgo.bind(100, oldestDay.toDate(), 100.0, 100.0, 100.0));

        PreparedStatement insert24HourToday = insertStatements.twentyFourHour.get(today);
        session.execute(insert24HourToday.bind(100, today.toDate(), 200.0, 200.0, 200.0));

        assert24HourDataEquals(100, asList(
            new AggregateNumericMetric(100, Bucket.TWENTY_FOUR_HOUR, 100.00, 100.0, 100.0, oldestDay.getMillis()),
            new AggregateNumericMetric(100, Bucket.TWENTY_FOUR_HOUR, 200.00, 200.0, 200.0, today.getMillis())
        ));
    }

    @Test(dependsOnMethods = "insertAfterInitializingStatements")
    public void insertAfterUpdatingStatements() {
        dateTimeService.setNow(dateTimeService.current24HourTimeSlice().plusDays(1));

        insertStatements.update();

        DateTime today = dateTimeService.current24HourTimeSlice();
        DateTime removedDay = today.minusDays(8);
        DateTime tomorrow = today.plusDays(1);

        assertTrue(insertStatements.raw.get(removedDay) == null);
        assertTrue(insertStatements.oneHour.get(removedDay) == null);
        assertTrue(insertStatements.sixHour.get(removedDay) == null);
        assertTrue(insertStatements.twentyFourHour.get(removedDay) == null);

        assertTrue(insertStatements.raw.get(tomorrow) != null);
        assertTrue(insertStatements.oneHour.get(tomorrow) != null);
        assertTrue(insertStatements.sixHour.get(tomorrow) != null);
        assertTrue(insertStatements.twentyFourHour.get(tomorrow) != null);

        DateTime oldest = insertStatements.raw.firstKey();
        DateTime youngest = insertStatements.raw.lastKey();

        insertStatements.update();

        assertEquals(insertStatements.raw.firstKey(), oldest);
        assertEquals(insertStatements.raw.lastKey(), youngest);
    }

}
