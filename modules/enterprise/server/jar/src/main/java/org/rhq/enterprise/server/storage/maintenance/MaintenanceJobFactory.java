package org.rhq.enterprise.server.storage.maintenance;

import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public interface MaintenanceJobFactory {

    /**
     * Calculates the steps for a job. This method is invoked immediately before a job is run. If the job is put back
     * in the maintenance queue, this will will be invoked again only if there is a change in cluster topology. Such
     * changes include,
     * <ul>
     *   <li>node up (that was previously down)</li>
     *   <li>node down (that was previously up)</li>
     *   <li>node added</li>
     *   <li>node removed</li>
     *   <li>change in gossip endpoint address</li>
     * </ul>
     *
     * @param job The job update
     * @return The input job. (Note: I think we can make the return type void)
     */
    StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job);

    /**
     * Invoked when a job step fails, but the job execution should continue. This method is responsible for adding or
     * removing any steps necessary based on the failed step. Steps added to the job will be persisted, and steps
     * removed from the job will be deleted from the database.
     *
     * <p>
     * <strong>Note:</strong> I am not 100% convinced that this method belongs here. It
     * </p>
     *
     * @param job The job currently being run
     * @param failedStep The step that failed
     */
    void updateSteps(StorageMaintenanceJob job, MaintenanceStep failedStep);

}
