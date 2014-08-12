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

    void scheduleMaintenance(StorageMaintenanceJob job);

    void rescheduleJob(int jobNumber);

    List<StorageMaintenanceJob> loadQueue();

    void deleteStep(int stepId);

    void execute();

    StorageMaintenanceJob refreshJob(int jobNumber);

    MaintenanceStep reloadStep(int stepId);

}
