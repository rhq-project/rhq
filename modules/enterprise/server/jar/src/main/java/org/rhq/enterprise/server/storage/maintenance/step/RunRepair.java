package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;
import org.rhq.enterprise.server.storage.maintenance.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public class RunRepair extends ResourceOperationStepRunner {

    public RunRepair() {
        super("repair");
    }

    @Override
    public void execute() throws StepFailureException {
        super.execute();
        Configuration results = history.getResults();
        PropertyList failedSessions = results.getList("failedSessions");
        if (failedSessions != null && !failedSessions.getList().isEmpty()) {
            // TODO log or include in the exception the params
            throw new StepFailureException("Resource operation [repair] against " + getTarget() + " failed for " +
                failedSessions.getList().size() + " range(s)");
        }
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.CONTINUE;
    }

    @Override
    public StorageMaintenanceJob createNewJobForFailedStep() {
        Configuration results = history.getResults();
        PropertyList failedSessions = results.getList("failedSessions");

        Configuration jobParams = new Configuration();
        jobParams.put(new PropertySimple(JobProperties.TARGET, getTarget()));

        PropertyList ranges = new PropertyList("ranges");
        for (Property p : failedSessions.getList()) {
            ranges.add(p.deepCopy(false));
        }

        jobParams.put(ranges);
        jobParams.put(new PropertySimple("keyspace", results.getSimpleValue("keyspace")));
        jobParams.put(results.get("tables").deepCopy(false));

        return new StorageMaintenanceJob(MaintenanceStep.JobType.FAILED_REPAIR, "Repair " + getTarget(), jobParams);
    }
}
