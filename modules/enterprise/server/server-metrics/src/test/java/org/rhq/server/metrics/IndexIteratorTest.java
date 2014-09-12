package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.server.metrics.aggregation.IndexIterator;
import org.rhq.server.metrics.domain.IndexBucket;
import org.rhq.server.metrics.domain.IndexEntry;

/**
 * @author John Sanda
 */
public class IndexIteratorTest extends MetricsTest {

    private static final int PAGE_SIZE = 2;

    private int numPartitions = 2;

    @BeforeMethod
    public void setUp() {
        numPartitions = 2;
        purgeDB();
    }


    @Override
    protected MetricsConfiguration createConfiguration() {
        MetricsConfiguration configuration = new MetricsConfiguration();
        configuration.setIndexPageSize(PAGE_SIZE);
        configuration.setIndexPartitions(numPartitions);

        return configuration;
    }

    @Test
    public void iterateOverEmptyIndex() {
        IndexIterator iterator = new IndexIterator(hour(4), hour(5), IndexBucket.RAW, dao, configuration);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void iterateOverNonEmptyIndex() {
        dao.updateIndex(IndexBucket.RAW, hour(3).getMillis(), 100).get();
        dao.updateIndex(IndexBucket.RAW, hour(4).getMillis(), 100).get();
        dao.updateIndex(IndexBucket.RAW, hour(4).getMillis(), 101).get();
        dao.updateIndex(IndexBucket.RAW, hour(4).getMillis(), 102).get();
        dao.updateIndex(IndexBucket.RAW, hour(4).getMillis(), 103).get();
        dao.updateIndex(IndexBucket.RAW, hour(4).getMillis(), 104).get();
        dao.updateIndex(IndexBucket.RAW, hour(4).getMillis(), 107).get();
        dao.updateIndex(IndexBucket.RAW, hour(4).getMillis(), 109).get();
        dao.updateIndex(IndexBucket.RAW, hour(5).getMillis(), 108).get();
        dao.updateIndex(IndexBucket.RAW, hour(5).getMillis(), 109).get();
        dao.updateIndex(IndexBucket.RAW, hour(5).getMillis(), 111).get();
        dao.updateIndex(IndexBucket.RAW, hour(6).getMillis(), 108).get();

        List<IndexEntry> expected = asList(
            newIndexEntry(IndexBucket.RAW, hour(4), 100),
            newIndexEntry(IndexBucket.RAW, hour(4), 102),
            newIndexEntry(IndexBucket.RAW, hour(4), 104),
            newIndexEntry(IndexBucket.RAW, hour(4), 101),
            newIndexEntry(IndexBucket.RAW, hour(4), 103),
            newIndexEntry(IndexBucket.RAW, hour(4), 107),
            newIndexEntry(IndexBucket.RAW, hour(4), 109),
            newIndexEntry(IndexBucket.RAW, hour(5), 108),
            newIndexEntry(IndexBucket.RAW, hour(5), 109),
            newIndexEntry(IndexBucket.RAW, hour(5), 111)
        );
        List<IndexEntry> actual = new ArrayList<IndexEntry>();

        IndexIterator iterator = new IndexIterator(hour(4), hour(6), IndexBucket.RAW, dao, configuration);

        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        assertEquals(actual, expected, "The index entries do not match");
    }

    private IndexEntry newIndexEntry(IndexBucket bucket, DateTime time, int scheduleId) {
        return new IndexEntry(bucket, (scheduleId % numPartitions), time, scheduleId);
    }
}
