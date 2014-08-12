package org.rhq.enterprise.server.storage;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceStepRunnerFactory;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureException;

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

    @EJB
    private StorageClusterSettingsManagerLocal clusterSettingsManager;

    private CalculatorLookup calculatorLookup = new DefaultCalculatorLookup();

    private MaintenanceStepRunnerFactory stepRunnerFactory;

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void init(CalculatorLookup calculatorLookup, MaintenanceStepRunnerFactory stepRunnerFactory) {
        log.warn("This method is for testing only. It should not be used in production code");
        this.calculatorLookup = calculatorLookup;
        this.stepRunnerFactory = stepRunnerFactory;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void scheduleMaintenance(StorageMaintenanceJob job) {
        MaintenanceStep baseStep = job.getBaseStep();

        log.info("Adding " + job + " to maintenance job queue");

        entityManager.persist(baseStep.getConfiguration());
        entityManager.persist(baseStep);
        baseStep.setJobNumber(baseStep.getId());
    }

    @Override
    public void rescheduleJob(int jobNumber) {
        StorageMaintenanceJob job = loadJob(jobNumber);
        MaintenanceStep oldBaseStep = job.getBaseStep();
        MaintenanceStep newBaseStep = new MaintenanceStep()
            .setName(oldBaseStep.getName())
            .setDescription(oldBaseStep.getDescription())
            .setJobType(oldBaseStep.getJobType())
            .setConfiguration(oldBaseStep.getConfiguration().deepCopyWithoutProxies());

        entityManager.remove(oldBaseStep);
        entityManager.persist(newBaseStep.getConfiguration());
        entityManager.persist(newBaseStep);
        newBaseStep.setJobNumber(newBaseStep.getId());
        for (MaintenanceStep step : job) {
            step.setJobNumber(newBaseStep.getJobNumber());
            entityManager.merge(step);
        }
    }

    private StorageMaintenanceJob loadJob(int jobNumber) {
        List<MaintenanceStep> steps = entityManager.createNamedQuery(MaintenanceStep.FIND_BY_JOB_NUM,
            MaintenanceStep.class).setParameter("jobNumber", jobNumber).getResultList();
        return new StorageMaintenanceJob(steps);
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
                queue.add(job);
            }
        }

        return queue;
    }

    private StorageMaintenanceJob refreshJob(StorageMaintenanceJob job) {
        StepCalculator stepCalculator = calculatorLookup.lookup(job.getJobType());
        return stepCalculator.calculateSteps(job, storageNodeManager.getClusterNodes());
    }

    @Override
    public StorageMaintenanceJob refreshJob(int jobNumber) {
        StorageMaintenanceJob job = loadJob(jobNumber);
        StepCalculator stepCalculator = calculatorLookup.lookup(job.getJobType());
        stepCalculator.setSubjectManager(subjectManager);
        stepCalculator.setEntityManager(entityManager);
        stepCalculator.setStorageClusterSettingsManager(clusterSettingsManager);

        return stepCalculator.calculateSteps(job, storageNodeManager.getClusterNodes());
    }

    @Override
    public MaintenanceStep reloadStep(int stepId) {
        return entityManager.createNamedQuery(MaintenanceStep.FIND_STEP_AND_CONFIG, MaintenanceStep.class)
            .setParameter("stepId", stepId).getSingleResult();
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
        for (StorageMaintenanceJob job : queue) {
            executeJob(maintenanceManager.refreshJob(job.getJobNumber()));
        }
    }

    private void executeJob(StorageMaintenanceJob job) {
        log.info("Executing " + job);
        for (MaintenanceStep step : job) {
            MaintenanceStepRunner stepRunner = stepRunnerFactory.newStepRunner(step);
            boolean succeeded = executeStep(maintenanceManager.reloadStep(step.getId()), stepRunner);
            if (succeeded) {
                maintenanceManager.deleteStep(step.getId());
            } else {
                switch (stepRunner.getFailureStrategy()) {
                    case ABORT:
                        log.info("Aborting " + job);
                        maintenanceManager.rescheduleJob(job.getJobNumber());
                        return;
                    case CONTINUE:
                        // TODO schedule new job for failed step
                    default:
                        throw new IllegalStateException("We shouldn't get here");
                }
            }
        }
        log.info("Finished executing " + job);
        maintenanceManager.deleteStep(job.getBaseStep().getId());
    }

    private boolean executeStep(MaintenanceStep step, MaintenanceStepRunner stepRunner) {
        try {
            log.info("Executing " + step);
            stepRunner.setOperationManager(operationManager);
            stepRunner.setStorageNodeManager(storageNodeManager);
            stepRunner.setSubjectManager(subjectManager);
            stepRunner.execute(step);

            return true;
        } catch (StepFailureException e) {
            log.info(step + " failed: " + e.getMessage());
            return false;
        }
    }

    private MaintenanceStepRunner getStepRunner(MaintenanceStep step) {
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
