package org.rhq.server.metrics;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.apache.avro.tool.ToTextTool;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;

/**
 * Used by the DAO to return a row.
 */
public class CallTimeRow {

    private final int scheduleId;
    private final String dest;
    private final Date begin;
    private final Date end;
    private final double min;
    private final double max;
    private final double total;
    private final long count;

    /**
     * Row value.
     */
    public CallTimeRow(int scheduleId, String dest, Date begin, Date end, double min, double max, double total, long count) {
        this.scheduleId = scheduleId;
        this.dest = dest;
        this.begin = begin;
        this.end = end;
        this.min = min;
        this.max = max;
        this.total = total;
        this.count = count;
    }

    /**
     * Converts this object to a composite.
     */
    public CallTimeDataComposite toComposite() {
        return new CallTimeDataComposite(dest, min, max, total, count, total/count);
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public String getDest() {
        return dest;
    }

    public Date getBegin() {
        return begin;
    }

    public Date getEnd() {
        return end;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getTotal() {
        return total;
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "CallTimeRow [scheduleId=" + scheduleId + ", dest=" + dest
                + ", begin=" + begin + ", end=" + end + ", min=" + min
                + ", max=" + max + ", total=" + total + ", count=" + count + "]";
    }

    /**
     * Aggregate this row.
     * @param rowlist
     * @return one aggregate
     */
    public static CallTimeRow aggregate(List<CallTimeRow> rowlist) {
        if (rowlist.isEmpty()) {
            throw new IllegalArgumentException();
        }
        CallTimeRow row0 = rowlist.get(0);
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int count = 0;
        double total = 0;
        Date end = row0.end;
        for (CallTimeRow row : rowlist) {
            min = Math.min(min, row.min);
            max = Math.max(max, row.max);
            count += row.count;
            total += row.total;
            end = row.end;
        }
        // assume first entry is oldest
        return new CallTimeRow(row0.scheduleId, row0.dest, row0.begin, end, min, max, total, count);
    }

}
