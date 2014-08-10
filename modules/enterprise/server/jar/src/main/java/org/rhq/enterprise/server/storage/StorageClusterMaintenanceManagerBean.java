package org.rhq.enterprise.server.storage;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.job.DeployCalculator;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;

/**
 * @author John Sanda
 */
@Singleton
public class StorageClusterMaintenanceManagerBean implements StorageClusterMaintenanceManagerLocal {

    private final Log log = LogFactory.getLog(StorageClusterMaintenanceManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    @EJB
    private StorageClusterMaintenanceManagerLocal maintenanceManager;

    @EJB
    private OperationManagerLocal operationManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @Override
    public void scheduleMaintenance(StorageMaintenanceJob job) {
        MaintenanceStep baseStep = job.getBaseStep();

        log.info("Adding " + job + " to maintenance job queue");

        entityManager.persist(baseStep.getConfiguration());
        entityManager.persist(baseStep);
        baseStep.setJobNumber(baseStep.getId());
    }

    @Override
    public List<StorageMaintenanceJob> loadQueue() {
        List<MaintenanceStep> steps = entityManager.createNamedQuery(MaintenanceStep.FIND_ALL, MaintenanceStep.class)
            .getResultList();
        List<StorageMaintenanceJob> queue = new LinkedList<StorageMaintenanceJob>();

        if (steps.isEmpty()) {
            return Collections.emptyList();
        }

        Iterator<MaintenanceStep> iterator = steps.iterator();
        StorageMaintenanceJob job = new StorageMaintenanceJob(iterator.next());
        queue.add(job);

        while (iterator.hasNext()) {
            MaintenanceStep step = iterator.next();
            if (step.getJobNumber() == job.getJobNumber()) {
                job.addStep(step);
            } else {
                job = new StorageMaintenanceJob(step);
            }
        }

        return queue;
    }

    private void refreshJob(StorageMaintenanceJob job) {
        StepCalculator stepCalculator = getStepCalculator(job.getJobType());
        stepCalculator.calculateSteps(job, storageNodeManager.getClusterNodes());
    }

    private StepCalculator getStepCalculator(MaintenanceStep.JobType jobType) {
        try {
            if (jobType == MaintenanceStep.JobType.DEPLOY) {
                return (StepCalculator) new InitialContext().lookup(
                    "java:global/rhq/rhq-server/" + DeployCalculator.class.getSimpleName());
            }
            throw new UnsupportedOperationException("There is no support yet for calculating steps for jobs of type " +
                jobType);
        } catch (NamingException e) {
            throw new RuntimeException("Failed to look up step calculator", e);
        }
    }

    @Override
    public void deleteStep(int stepId) {
        MaintenanceStep step = entityManager.find(MaintenanceStep.class, stepId);

        if (log.isDebugEnabled()) {
            log.debug("Deleting " + step.toString(true));
        }

        if (step == null) {
            log.info("Nothing to delete. No step found with id " + stepId);
        } else {
            entityManager.remove(step);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void execute() {
        List<StorageMaintenanceJob> queue = maintenanceManager.loadQueue();
        if (queue.isEmpty()) {
            log.info("There are no jobs to execute");
            return;
        }
        StorageMaintenanceJob job = queue.remove(0);
        refreshJob(job);

        log.info("Executing " + job);
        for (MaintenanceStep step : job) {
            log.info("Executing " + step);
            MaintenanceStepRunner stepRunner = getStepRunner(step);
            stepRunner.setOperationManager(operationManager);
            stepRunner.setStorageNodeManager(storageNodeManager);
            stepRunner.setSubjectManager(subjectManager);
            stepRunner.execute(step);
            maintenanceManager.deleteStep(step.getId());
        }
        maintenanceManager.deleteStep(job.getBaseStep().getId());
        log.info("Finished executing " + job);
    }

    private MaintenanceStepRunner getStepRunner(MaintenanceStep step) {
        try {
            Class clazz = Class.forName(step.getName());
            MaintenanceStepRunner runner = (MaintenanceStepRunner) clazz.newInstance();

            return runner;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
