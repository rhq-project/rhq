package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.server.metrics.aggregation.IndexIterator;
import org.rhq.server.metrics.domain.IndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class IndexIteratorTest extends MetricsTest {

    private static final int PAGE_SIZE = 2;

    private int numPartitions;

    @BeforeMethod
    public void setUp() {
        numPartitions = 2;
        purgeDB();
    }

    @Override
    protected void initIndexPageSize() {
        System.setProperty("rhq.metrics.index.page-size", Integer.toString(PAGE_SIZE));
    }

    @Test
    public void iterateOverEmptyIndex() {
        IndexIterator iterator = new IndexIterator(hour(4), hour(5), Hours.hours(1).toStandardDuration(),
            MetricsTable.RAW, dao, numPartitions, PAGE_SIZE);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void iterateOverNonEmptyIndex() {
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(3), 100)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(4), 100)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(4), 101)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(4), 102)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(4), 103)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(4), 104)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(4), 107)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(4), 109)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(5), 108)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(5), 109)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(5), 111)).get();
        dao.insertIndexEntry(newIndexEntry(MetricsTable.RAW, hour(6), 108)).get();

        List<IndexEntry> expected = asList(
            newIndexEntry(MetricsTable.RAW, hour(4), 100),
            newIndexEntry(MetricsTable.RAW, hour(4), 102),
            newIndexEntry(MetricsTable.RAW, hour(4), 104),
            newIndexEntry(MetricsTable.RAW, hour(4), 101),
            newIndexEntry(MetricsTable.RAW, hour(4), 103),
            newIndexEntry(MetricsTable.RAW, hour(4), 107),
            newIndexEntry(MetricsTable.RAW, hour(4), 109),
            newIndexEntry(MetricsTable.RAW, hour(5), 108),
            newIndexEntry(MetricsTable.RAW, hour(5), 109),
            newIndexEntry(MetricsTable.RAW, hour(5), 111)
        );
        List<IndexEntry> actual = new ArrayList<IndexEntry>();

        IndexIterator iterator = new IndexIterator(hour(4), hour(6), Hours.hours(1).toStandardDuration(),
            MetricsTable.RAW, dao, numPartitions, PAGE_SIZE);

        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        assertEquals(actual, expected, "The index entries do not match");
    }

    private IndexEntry newIndexEntry(MetricsTable bucket, DateTime time, int scheduleId) {
        return new IndexEntry(bucket, (scheduleId % numPartitions), time, scheduleId);
    }
}
