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

    /**
     * Schedules a new job for later execution when the currently running job encounters a step failure with a failure
     * strategy of {@link org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy#CONTINUE CONTINUE}.
     * <p>
     * This method calls
     * {@link org.rhq.enterprise.server.storage.maintenance.job.StepCalculator#createNewJob(StorageMaintenanceJob, MaintenanceStep) StepCalculator.createNewJob(StorageMaintenanceJob, MaintenanceStep)}
     * to get the new job for the failed step to be scheduled for later execution. A check is performed to see if a
     * similar job is already in the queue. The new job is persisted only if no other similar job is found. The reason
     * for this is to avoid creating duplicate, redundant work. For example, we would not want to keep scheduling a
     * repair operation over and over when we just need it to run once on a given node. If a repair operation fails,
     * and there is already a job in the queue for running repair over the same table/data, then there is no need to
     * schedule another job to do the same work.
     * </p>
     * <p>
     * {@link org.rhq.enterprise.server.storage.maintenance.job.StepCalculator#updateSteps(StorageMaintenanceJob, MaintenanceStep)}  StepCalculator.updateSteps(StorageMaintenanceJob, MaintenanceStep)}
     * is called so that steps can be added to or removed from the job if necessary. The addition/removal of steps is
     * updated in the database as well.
     * </p>
     *
     * @param jobNumer The job number of the job currently running
     * @param failedStepNumber The number of the step that just was executed and failed
     */
    void scheduleMaintenance(int jobNumer, int failedStepNumber, StorageMaintenanceJob newJob);

    /**
     * Adds a job back to the queue. This is done when a job is aborted due a to a step failure. When this method
     * completes all of the steps associated with <code>jobNumber</code> will have a new job number.
     *
     * @param jobNumber The original job number of the job to be rescheduled
     */
    void rescheduleJob(int jobNumber);

    /**
     * Loads the specified job including all its steps. It is assumed that callers of this method will work with the
     * job outside of a transactional context; therefore, each step's configuration is eagerly loaded to avoid lazy
     * init exceptions.
     */
    StorageMaintenanceJob loadJob(int jobNumber);

    List<StorageMaintenanceJob> loadQueue();

    void deleteStep(int stepId);

    void execute();

    StorageMaintenanceJob refreshJob(int jobNumber);

    MaintenanceStep reloadStep(int stepId);

}
