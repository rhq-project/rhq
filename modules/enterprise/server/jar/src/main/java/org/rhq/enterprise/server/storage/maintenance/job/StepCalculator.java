package org.rhq.enterprise.server.storage.maintenance.job;

import java.util.List;

import javax.persistence.EntityManager;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public interface StepCalculator {

    void setSubjectManager(SubjectManagerLocal subjectManager);

    void setStorageClusterSettingsManager(StorageClusterSettingsManagerLocal clusterSettingsManager);

    void setEntityManager(EntityManager entityManager);

    StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job, List<StorageNode> cluster);

    StorageMaintenanceJob calculateSteps(StorageMaintenanceJob originalJob, MaintenanceStep failedStep);

}
