package org.rhq.core.pc.drift;

import java.io.Serializable;

import org.rhq.core.domain.drift.DriftConfiguration;

public class DriftDetectionSchedule implements Comparable<DriftDetectionSchedule>, Serializable {

    private static final long serialVersionUID = 1L;

    private int resourceId;

    private DriftConfiguration driftConfig;

    private long nextScan;

    public DriftDetectionSchedule(int resourceId, DriftConfiguration configuration) {
        this.resourceId = resourceId;
        driftConfig = configuration;
        nextScan = System.currentTimeMillis();
    }

    public int getResourceId() {
        return resourceId;
    }

    public DriftConfiguration getDriftConfiguration() {
        return driftConfig;
    }

    public long getNextScan() {
        return nextScan;
    }

    public void updateShedule() {
        nextScan = System.currentTimeMillis() + (driftConfig.getInterval() * 1000);
    }

    public DriftDetectionSchedule copy() {
        DriftDetectionSchedule copy = new DriftDetectionSchedule(resourceId,
            new DriftConfiguration(driftConfig.getConfiguration().deepCopyWithoutProxies()));
        copy.nextScan = nextScan;
        return copy;
    }

    @Override
    public int compareTo(DriftDetectionSchedule other) {
        if (this.nextScan < other.nextScan) {
            return -1;
        }

        if (this.nextScan > other.nextScan) {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof DriftDetectionSchedule) {
            DriftDetectionSchedule that = (DriftDetectionSchedule) obj;
            return this.nextScan == that.nextScan;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(nextScan).hashCode();
    }
}
