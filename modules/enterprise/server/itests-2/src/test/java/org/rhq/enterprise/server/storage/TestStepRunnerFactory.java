package org.rhq.enterprise.server.storage;

import static org.testng.Assert.assertEquals;

import java.util.LinkedList;
import java.util.Queue;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceStepRunnerFactory;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;

/**
 * A {@link MaintenanceStepRunnerFactory maintenance step factory} which verifies that
 * {@link MaintenanceStepRunner step runners} are requested in the order specified as determined by the constructor.
 * @author John Sanda
 */
public class TestStepRunnerFactory implements MaintenanceStepRunnerFactory {

    Queue<FakeStepRunner> queue = new LinkedList<FakeStepRunner>();

    /**
     * The order of the step runners passed to the constructor is significant. This factory assumes that
     * {@link #newStepRunner(org.rhq.core.domain.storage.MaintenanceStep)} will be called with steps corresponding to
     * the specified runners in the same order.
     *
     * @param stepRunners The step runners to return to
     * {@link org.rhq.enterprise.server.storage.StorageClusterMaintenanceManagerLocal StorageClusterMaintenanceManagerBean}
     */
    public TestStepRunnerFactory(FakeStepRunner... stepRunners) {
        for (FakeStepRunner stepRunner : stepRunners) {
            queue.offer(stepRunner);
        }
    }

    @Override
    public MaintenanceStepRunner newStepRunner(MaintenanceStep step) {
        FakeStepRunner next = queue.poll();
        assertEquals(step.getName(), next.stepName, "The step name for " + step +
            " does not match the expected value");
        assertEquals(step.getStepNumber(), next.stepNumber, "The step number for " + step +
            " does not match the expected value");
        return next;
    }

}
