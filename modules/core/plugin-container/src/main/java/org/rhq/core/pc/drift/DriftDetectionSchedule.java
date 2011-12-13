package org.rhq.core.pc.drift;

import java.io.Serializable;

import org.rhq.core.domain.drift.DriftDefinition;

public class DriftDetectionSchedule implements Comparable<DriftDetectionSchedule>, Serializable {

    private static final long serialVersionUID = 1L;

    private int resourceId;

    private DriftDefinition driftDef;

    private long nextScan;

    public DriftDetectionSchedule(int resourceId, DriftDefinition definition) {
        this.resourceId = resourceId;
        this.driftDef = definition;
        nextScan = -1;
    }

    public int getResourceId() {
        return resourceId;
    }

    public DriftDefinition getDriftDefinition() {
        return driftDef;
    }

    public long getNextScan() {
        return nextScan;
    }

    public void updateShedule() {
        nextScan = System.currentTimeMillis() + (driftDef.getInterval() * 1000);
    }

    public void resetSchedule() {
        nextScan = -1;
    }

    public DriftDetectionSchedule copy() {
        DriftDetectionSchedule copy = new DriftDetectionSchedule(resourceId, new DriftDefinition(driftDef
            .getConfiguration().deepCopyWithoutProxies()));
        copy.driftDef.setId(driftDef.getId());
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[resourceId: " + resourceId + ", driftDefinitionId: " + driftDef.getId()
            + ", driftDefinitionName: " + driftDef.getName() + "]";
    }
}
