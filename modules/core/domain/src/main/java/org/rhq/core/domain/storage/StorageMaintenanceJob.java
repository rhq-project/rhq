package org.rhq.core.domain.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author John Sanda
 */
public class StorageMaintenanceJob implements Serializable  {

    public static final long serialVersionUID = 1L;

    private List<MaintenanceStep> steps = new ArrayList<MaintenanceStep>();

    public StorageMaintenanceJob(MaintenanceStep.JobType jobType, String name) {
        this(jobType, name, name);
    }

    public StorageMaintenanceJob(MaintenanceStep.JobType jobType, String name, String description) {
        steps.add(new MaintenanceStep().setJobType(jobType).setName(name).setDescription(description));
    }

    public StorageMaintenanceJob(List<MaintenanceStep> steps) {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a maintenance job from an empty list of steps");
        }
        this.steps = steps;
    }

    public MaintenanceStep getBaseStep() {
        return steps.get(0);
    }

    @Override
    public String toString() {
        return "StorageMaintenanceJob[jobName = " + getBaseStep().getName() + ", jobNumber = " +
            getBaseStep().getJobNumber() + ", jobType = " + getBaseStep().getJobType() + "]";
    }
}
