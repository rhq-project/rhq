package org.rhq.enterprise.server.storage.maintenance.job;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.step.RunRepair;

/**
 * @author John Sanda
 */
public class FailedRepairCalculator implements MaintenanceJobFactory {

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
        String target = job.getTarget();
        Configuration jobConfig = job.getConfiguration();
        PropertySimple keyspaceProperty = jobConfig.getSimple("keyspace");
        PropertyList tablesProperty = jobConfig.getList("tables");
        PropertyList rangesProperty = jobConfig.getList("ranges");

        for (Property range : rangesProperty.getList()) {
            PropertyMap rangeMap = (PropertyMap) range;

            job.addStep(new MaintenanceStep()
                .setName(RunRepair.class.getName())
                .setDescription("Run repair on " + target)
                .setConfiguration(new Configuration.Builder()
                    .addSimple(JobProperties.TARGET, target)
                    .openMap(JobProperties.PARAMETERS)
                        .addSimple("keyspace", keyspaceProperty.getStringValue())
                        .addSimple("snapshot", true)
                        .addSimple("primaryRange", false)
                        .openList("tables", "table")
                            .addSimples(getTables(tablesProperty))
                        .closeList()
                        .openMap("range")
                            .addSimple("start", rangeMap.getSimple("start").getStringValue())
                            .addSimple("end", rangeMap.getSimple("end").getStringValue())
                        .closeMap()
                    .closeMap()
                    .build()));
        }

        return job;
    }

    private String[] getTables(PropertyList tablesList) {
        String[] tables = new String[tablesList.getList().size()];
        for (int i = 0; i < tablesList.getList().size(); ++i) {
            PropertySimple table = (PropertySimple) tablesList.getList().get(i);
            tables[i] = table.getStringValue();
        }
        return tables;
    }

    @Override
    public void updateSteps(StorageMaintenanceJob job, MaintenanceStep failedStep) {

    }

}
