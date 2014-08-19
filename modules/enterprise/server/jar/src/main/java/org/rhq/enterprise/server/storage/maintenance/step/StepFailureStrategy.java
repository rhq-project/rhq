package org.rhq.enterprise.server.storage.maintenance.step;

/**
 * Specifies what to do when a step fails. We may need to turn this into a callback interface of sorts if it turns out
 * that there is special cases that cannot be easily handled by StorageClusterMaintenanceManagerBean where it would be
 * simpler for the step runner to carry out whatever work is necessary for failure situations.
 *
 * @author John Sanda
 */
public enum StepFailureStrategy {

    /**
     * Abort the job and reschedule it for later execution.
     */
    ABORT,

    /**
     * Continue execution of the job. A new job will be created for the failed step and added to the queue.
     *
     */
    CONTINUE;

    public static StepFailureStrategy fromString(String strategy) {
        if (strategy.equals(ABORT.toString())) {
            return ABORT;
        } else if (strategy.equals(CONTINUE.toString())) {
            return CONTINUE;
        } else {
            throw new IllegalArgumentException(strategy + " is not a " + StepFailureStrategy.class.getSimpleName());
        }
    }

}
