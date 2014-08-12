package org.rhq.enterprise.server.storage;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.testng.annotations.Test;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceStepRunnerFactory;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureException;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;

/**
 * @author John Sanda
 */
public class StorageClusterMaintenanceManagerBeanTest extends AbstractEJB3Test {

    @EJB
    private StorageClusterMaintenanceManagerLocal maintenanceManager;

    @Override
    protected void beforeMethod() throws Exception {
        resetDB();
    }

    @Override
    protected void afterMethod() throws Exception {
        resetDB();
    }

    @Test
    public void runMaintenanceWhenQueueIsEmpty() {
        maintenanceManager.execute();
    }

    @Test
    public void runMaintenanceWhenQueueHasOneJob() throws Exception {
        final AtomicBoolean stepExecuted = new AtomicBoolean(false);
        final String stepName = "FakeStep1";

        final CalculatorLookup calculatorLookup = new CalculatorLookup() {
            @Override
            public StepCalculator lookup(MaintenanceStep.JobType jobType) {
                return new TestDeployCalculator() {
                    @Override
                    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job, List<StorageNode> cluster) {
                        MaintenanceStep step = new MaintenanceStep()
                            .setJobNumber(job.getJobNumber())
                            .setJobType(job.getJobType())
                            .setName(stepName)
                            .setDescription(stepName)
                            .setStepNumber(1);
                        em.persist(step);
                        job.addStep(step);

                        return job;
                    }
                };
            }
        };

        MaintenanceStepRunnerFactory stepRunnerFactory = new MaintenanceStepRunnerFactory() {
            @Override
            public MaintenanceStepRunner newStepRunner(MaintenanceStep step) {
                return new FakeStepRunner() {
                    @Override
                    public void execute(MaintenanceStep maintenanceStep) throws StepFailureException {
                        stepExecuted.set(true);
                    }
                };
            }
        };

        maintenanceManager.init(calculatorLookup, stepRunnerFactory);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                StorageMaintenanceJob job = new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY, "test deploy",
                    new Configuration());
                maintenanceManager.scheduleMaintenance(job);
            }
        }, "Failed to create test deploy job");

        maintenanceManager.execute();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                assertEquals("The job queue should be empty", 0, maintenanceManager.loadQueue().size());
                assertTrue(stepName + " was not executed", stepExecuted.get());
            }
        }, "Failed to verify whether the test deploy job was run");
    }

    private void executeInTransaction(TransactionCallback callback, String errorMsg) {
        try {
            getTransactionManager().begin();
            callback.execute();
            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                getTransactionManager().rollback();
                org.testng.Assert.fail(errorMsg, e);
            } catch (SystemException e1) {
                org.testng.Assert.fail(errorMsg + " - Failed to rollback transaction", e1);
            }
        } catch (AssertionError e) {
            try {
                getTransactionManager().rollback();
                throw e;
            } catch (SystemException e1) {
                throw new AssertionError("Failed to rollback transaction: " + e1.getMessage(), e);
            }
        }
    }

    private void resetDB() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeTable(MaintenanceStep.class);
                purgeTable(Configuration.class);
            }
        }, "Failed to clean up database");
    }

    private void purgeTable(Class clazz) {
        EntityManager em = getEntityManager();
        em.createQuery("DELETE FROM " + clazz.getSimpleName()).executeUpdate();
    }

}
