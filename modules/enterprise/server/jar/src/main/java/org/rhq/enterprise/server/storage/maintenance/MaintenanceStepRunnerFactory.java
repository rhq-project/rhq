package org.rhq.enterprise.server.storage.maintenance;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;

/**
 * @author John Sanda
 */
public interface MaintenanceStepRunnerFactory {

    MaintenanceStepRunner newStepRunner(MaintenanceStep step);

}
