package org.rhq.enterprise.server.storage.maintenance;

import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public class DefaultStepRunnerFactory implements MaintenanceStepRunnerFactory {

    @Override
    public MaintenanceStepRunner newStepRunner(MaintenanceStep step) {
        try {
            Class clazz = Class.forName(step.getName());
            return (MaintenanceStepRunner) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
