package org.rhq.core.domain.storage;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.shared.TransactionCallback;
import org.rhq.core.domain.test.AbstractEJB3Test;

/**
 * @author John Sanda
 */
public class MaintenanceStepTest extends AbstractEJB3Test {

    private static Log log = LogFactory.getLog(MaintenanceStepTest.class);

    protected void beforeMethod() throws Exception {
        resetDB();
    }

    public void afterMethod() throws Exception {
        resetDB();
    }

    @Test(groups = "integration.ejb3")
    public void createAndFindJobWithNoSteps() throws Exception {
        final AtomicInteger jobNumber = new AtomicInteger();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                MaintenanceStep step = new MaintenanceStep()
                    .setJobType(MaintenanceStep.JobType.DEPLOY)
                    .setName("BASE_STEP")
                    .setStepNumber(0)
                    .setDescription("Deploy 127.0.0.1");

                em.persist(step);

                step.setJobNumber(step.getId());
                jobNumber.set(step.getJobNumber());
            }
        }, "Failed to persist maintenance job");

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                List<MaintenanceStep> steps = em.createNamedQuery(MaintenanceStep.FIND_BY_JOB_NUM,
                    MaintenanceStep.class).setParameter("jobNumber", jobNumber.get()).getResultList();

                assertEquals("Expected to get back one step", 1, steps.size());
                MaintenanceStep expected = new MaintenanceStep().setJobNumber(jobNumber.get()).setStepNumber(0);
                org.testng.Assert.assertEquals(steps.get(0), expected, "The step does not match the expected value");

            }
        }, "Failed to find job " + jobNumber + " which should not have any steps yet");
    }

    @Test(groups = "integration.ejb3")
    public void addStepsToJob() throws Exception {
        final AtomicInteger jobNumber = new AtomicInteger();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                MaintenanceStep step = new MaintenanceStep()
                    .setJobType(MaintenanceStep.JobType.DEPLOY)
                    .setName("BASE_STEP")
                    .setStepNumber(0)
                    .setDescription("Deploy 127.0.0.1");

                em.persist(step);

                step.setJobNumber(step.getId());
                jobNumber.set(step.getJobNumber());
            }
        }, "Cannot add steps to job. Failed to persist base step.");

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                em.persist(new MaintenanceStep()
                    .setJobNumber(jobNumber.get())
                    .setJobType(MaintenanceStep.JobType.DEPLOY)
                    .setName("Announce")
                    .setStepNumber(1)
                    .setDescription("Announce 127.0.0.1 to 127.0.0.2"));
                em.persist(new MaintenanceStep()
                    .setJobNumber(jobNumber.get())
                    .setJobType(MaintenanceStep.JobType.DEPLOY)
                    .setName("Announce")
                    .setStepNumber(2)
                    .setDescription("Announce 127.0.0.1 to 127.0.0.3"));
            }
        }, "Failed to persist new job steps");

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                List<MaintenanceStep> steps = em.createNamedQuery(MaintenanceStep.FIND_BY_JOB_NUM,
                    MaintenanceStep.class).setParameter("jobNumber", jobNumber.get()).getResultList();

                List<MaintenanceStep> expected = asList(
                    new MaintenanceStep().setJobNumber(jobNumber.get()).setStepNumber(0),
                    new MaintenanceStep().setJobNumber(jobNumber.get()).setStepNumber(1),
                    new MaintenanceStep().setJobNumber(jobNumber.get()).setStepNumber(2)
                );

                org.testng.Assert.assertEquals(steps, expected, "The job steps do not match the expected values");
            }
        }, "There was an unexpected error fetching the job steps");
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
            }
        }, "Failed to clean up database");
    }

    private void purgeTable(Class clazz) {
        EntityManager em = getEntityManager();
        em.createQuery("DELETE FROM " + clazz.getSimpleName()).executeUpdate();
    }

}
