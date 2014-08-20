package org.rhq.enterprise.server.storage.maintenance;

import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public interface MaintenanceStepRunnerFactory {

    MaintenanceStepRunner newStepRunner(MaintenanceStep step);

}
