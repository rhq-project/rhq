package org.rhq.server.metrics.domain;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

/**
 * @author John Sanda
 */
public class CacheIndexEntryMapper {

    public CacheIndexEntry map(Row row) {
        CacheIndexEntry indexEntry = new CacheIndexEntry();
        indexEntry.setBucket(MetricsTable.fromString(row.getString(0)));
        indexEntry.setInsertTimeSlice(row.getDate(1).getTime());
        indexEntry.setPartition(row.getInt(2));
        indexEntry.setStartScheduleId(row.getInt(3));
        indexEntry.setCollectionTimeSlice(row.getDate(4).getTime());
        indexEntry.setScheduleIds(row.getSet(5, Integer.class));

        return indexEntry;
    }

    public List<CacheIndexEntry> map(ResultSet resultSet) {
        List<CacheIndexEntry> entries = new ArrayList<CacheIndexEntry>();
        for (Row row : resultSet) {
            entries.add(map(row));
        }
        return entries;
    }

}
