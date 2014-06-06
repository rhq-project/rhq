package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.server.metrics.domain.MetricsTable.RAW;
import static org.testng.Assert.assertEquals;

import java.util.List;

import com.google.common.collect.ImmutableSet;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import org.rhq.server.metrics.aggregation.IndexEntriesLoader;
import org.rhq.server.metrics.domain.CacheIndexEntry;

/**
 * @author John Sanda
 */
public class IndexEntriesLoaderTest extends MetricsTest {

    private static final int PARTITION = 0;

    @Test
    public void loadPastIndexEntries() {
        IndexEntriesLoader indexEntriesLoader = new IndexEntriesLoader(hour(5), today(), dao);
        dateTimeService.setNow(hour(5));

        updateCacheIndex(today().minusDays(3), 3, 100, 100, 101, 102);
        updateCacheIndex(today().minusDays(3), 7, 100, 100, 101, 102);
        updateCacheIndex(today().minusDays(2), 5, 150, 150, 151, 152);
        updateCacheIndex(today(), 3, 100, 101);
        updateCacheIndex(today(), 5, 120, 122);

        List<CacheIndexEntry> actual = indexEntriesLoader.loadPastIndexEntries(today().minusDays(3));
        List<CacheIndexEntry> expected = asList(
            newRawCacheIndexEntry(today().minusDays(3).plusHours(7), 100, 100, 101, 102),
            newRawCacheIndexEntry(today().minusDays(2).plusHours(5), 150, 150, 151, 152),
            newRawCacheIndexEntry(today().plusHours(3), 100, 101)
        );

        assertEquals(actual, expected, "The cache index entries do not match the expected values");
    }

    @Test
    public void loadCurrentIndexEntries() {
        IndexEntriesLoader indexEntriesLoader = new IndexEntriesLoader(hour(5), today(), dao);
        int pageSize = 2;
        dateTimeService.setNow(hour(5));

        updateCacheIndex(today().minusDays(2), 5, 150, 150, 151, 152);
        updateCacheIndex(today(), 3, 100, 101);
        updateCacheIndex(today(), 5, 120, 122);
        updateCacheIndex(today(), 5, 125, 125, 126, 127);
        updateCacheIndex(today(), 5, 140, 142, 144);
        updateCacheIndex(today(), 5, 145, 145, 147);
        updateCacheIndex(today(), 5, 150, 150, 151, 152);
        updateCacheIndex(today(), 5, 200, 201, 202);
        updateCacheIndex(today(), 5, 230, 233);

        List<CacheIndexEntry> actual = indexEntriesLoader.loadCurrentCacheIndexEntries(pageSize, RAW);
        List<CacheIndexEntry> expected = asList(
            newRawCacheIndexEntry(hour(5), 120, 122),
            newRawCacheIndexEntry(hour(5), 125, 125, 126, 127),
            newRawCacheIndexEntry(hour(5), 140, 142, 144),
            newRawCacheIndexEntry(hour(5), 145, 145, 147),
            newRawCacheIndexEntry(hour(5), 150, 150, 151, 152),
            newRawCacheIndexEntry(hour(5), 200, 201, 202),
            newRawCacheIndexEntry(hour(5), 230, 233)
        );

        assertEquals(actual, expected, "The cache index entries do not match the expected values");
    }

    private void updateCacheIndex(DateTime day, int hour, int startScheduleId, Integer... scheduleIds) {
        DateTime timeSlice = day.plusHours(hour);
        StorageResultSetFuture future = dao.updateCacheIndex(RAW, day.getMillis(), PARTITION, timeSlice.getMillis(),
            startScheduleId, timeSlice.getMillis(), ImmutableSet.copyOf(scheduleIds));
        future.get();
    }

}
