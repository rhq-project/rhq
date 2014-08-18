package org.rhq.enterprise.server.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.DefaultStepRunnerFactory;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceStepRunnerFactory;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureStrategy;

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

    @EJB
    private StorageClientManager storageClientManager;

    private CalculatorLookup calculatorLookup = new DefaultCalculatorLookup();

    private MaintenanceStepRunnerFactory stepRunnerFactory = new DefaultStepRunnerFactory();

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
        // If multiple jobs are scheduled in the same transaction, it can lead to a
        // constraint violation on the job number/step number index. I am not sure
        // why that is, but that is why this method now requires a new transaction.

        MaintenanceStep baseStep = job.getBaseStep();

        entityManager.persist(baseStep);
        baseStep.setJobNumber(baseStep.getId());

        for (MaintenanceStep step : job) {
            step.setJobNumber(job.getJobNumber());
            entityManager.persist(step);
        }

        log.info("Adding " + job + " to maintenance job queue");
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
        MaintenanceStep baseStep = iterator.next();
        List<MaintenanceStep> jobSteps = new ArrayList<MaintenanceStep>();

        while (iterator.hasNext()) {
            MaintenanceStep step = iterator.next();
            if (step.getJobNumber() == baseStep.getJobNumber()) {
                jobSteps.add(step);
            } else {
                queue.add(new StorageMaintenanceJob(baseStep, jobSteps));
                baseStep = step;
                jobSteps = new ArrayList<MaintenanceStep>();
            }
        }
        queue.add(new StorageMaintenanceJob(baseStep, jobSteps));

        return queue;
    }

    @Override
    public StorageMaintenanceJob refreshJob(int jobNumber) {
        StorageMaintenanceJob job = loadJob(jobNumber);

        log.info("Checking to see if steps to need to (re)calculated for " + job);

        Set<String> currentClusterSnapshot = createSnapshot(storageNodeManager.getClusterNodes());
        if (job.getClusterSnapshot().isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Calculating steps for " + job);
            }
            calculateAndPersistSteps(job, currentClusterSnapshot);
        } else if (currentClusterSnapshot.equals(job.getClusterSnapshot())) {
            // We already have steps and they do not need to be recalculated
            if (log.isDebugEnabled()) {
                log.debug("No changes are necessary to " + job + ". Steps are up to date");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Recalculating steps for " + job);
            }
            // Delete any existing steps and add new ones
            for (MaintenanceStep step : job) {
                entityManager.remove(step);
            }
            job.clearSteps();
            // Delete the old cluster snapshot
            entityManager.remove(job.getClusterSnapshotProperty());
            job.setClusterSnapshot(currentClusterSnapshot);

            // now calculate and persist the new steps
            calculateAndPersistSteps(job, currentClusterSnapshot);
        }

        return job;
    }

    private void calculateAndPersistSteps(StorageMaintenanceJob job, Set<String> clusterSnapshot) {
        job.setClusterSnapshot(clusterSnapshot);

        StepCalculator stepCalculator = calculatorLookup.lookup(job.getJobType());
        stepCalculator.calculateSteps(job);
        for (MaintenanceStep step : job) {
            entityManager.persist(step);
        }
    }

    private Set<String> createSnapshot(List<StorageNode> cluster) {
        Set<String> snapshot = new HashSet<String>();
        for (StorageNode node : cluster) {
            snapshot.add(node.getAddress());
        }
        return snapshot;
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
        log.info("Loaded maintenance job queue: " + queue);

        for (StorageMaintenanceJob job : queue) {
            executeJob(maintenanceManager.refreshJob(job.getJobNumber()));
        }
    }

    private void executeJob(StorageMaintenanceJob job) {
        log.info("Executing " + job);
        for (MaintenanceStep step : job) {
            MaintenanceStepRunner stepRunner = stepRunnerFactory.newStepRunner(step);
            stepRunner.setClusterSnapshot(job.getClusterSnapshot());
            boolean succeeded = executeStep(maintenanceManager.reloadStep(step.getId()), stepRunner);
            if (succeeded) {
                maintenanceManager.deleteStep(step.getId());
            } else if (stepRunner.getFailureStrategy() == StepFailureStrategy.CONTINUE) {
                StepCalculator stepCalculator = calculatorLookup.lookup(job.getJobType());
                StorageMaintenanceJob newJob = stepCalculator.calculateSteps(job, step);
                maintenanceManager.scheduleMaintenance(newJob);
                maintenanceManager.deleteStep(step.getId());
            } else {  // failure strategy == ABORT
                log.info("Aborting " + job);
                maintenanceManager.rescheduleJob(job.getJobNumber());
                return;
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
            stepRunner.setStorageClientManager(storageClientManager);
            stepRunner.execute(step);

            return true;
        } catch (StepFailureException e) {
            log.info(step + " failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn(step + " failed with an unexpected exception", e);
            return false;
        }
    }

}
