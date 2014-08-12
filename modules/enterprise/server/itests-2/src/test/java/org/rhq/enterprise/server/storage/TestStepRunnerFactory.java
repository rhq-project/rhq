package org.rhq.enterprise.server.storage;

import static org.testng.Assert.assertEquals;

import java.util.LinkedList;
import java.util.Queue;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceStepRunnerFactory;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;

/**
 * @author John Sanda
 */
public class TestStepRunnerFactory implements MaintenanceStepRunnerFactory {

    Queue<FakeStepRunner> queue = new LinkedList<FakeStepRunner>();

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
