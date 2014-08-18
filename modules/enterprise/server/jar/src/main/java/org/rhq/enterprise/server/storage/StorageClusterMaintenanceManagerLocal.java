package org.rhq.enterprise.server.storage;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceStepRunnerFactory;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
@Local
public interface StorageClusterMaintenanceManagerLocal {

    /**
     * <strong>Note:</strong> This only here for testing.
     *
     * @param calculatorLookup The lookup class to use during tests
     * @param stepRunnerFactory The step runner factory to use during tests
     */
    void init(CalculatorLookup calculatorLookup, MaintenanceStepRunnerFactory stepRunnerFactory);

    /**
     * Adds a job to the maintenance queue for later execution. Clients should use this method to schedule
     * maintenance such as deploying a new node into the cluster or changing a node's endpoint address. This method
     * should only be used for scheduling new jobs. As such all of the
     * {@link org.rhq.core.domain.storage.MaintenanceStep steps} in the job should be transient, i.e., not yet
     * persisted in the database.
     *
     * @param job The {@link org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob job} to schedule
     */
    void scheduleMaintenance(StorageMaintenanceJob job);

    void scheduleMaintenance(int jobNumer, int failedStepNumber);

    void rescheduleJob(int jobNumber);

    StorageMaintenanceJob loadJob(int jobNumber);

    List<StorageMaintenanceJob> loadQueue();

    void deleteStep(int stepId);

    void execute();

    StorageMaintenanceJob refreshJob(int jobNumber);

    MaintenanceStep reloadStep(int stepId);

}
