package org.rhq.core.domain.storage;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
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
                    .setDescription("Deploy 127.0.0.1")
                    .setConfiguration(convert(new Configuration.Builder().addSimple("address", "127.0.0.1").build()));

                em.persist(step.getConfiguration());
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

    private Configuration convert(Configuration params) {
        Configuration configuration = new Configuration();
        PropertyMap propertyMap = new PropertyMap("parameters");
        for (Property p : params.getProperties()) {
            propertyMap.put(p.deepCopy(false));
        }
        configuration.put(propertyMap);

        return configuration;
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
                    .setDescription("Announce 127.0.0.1 to 127.0.0.2")
                    .setConfiguration(new Configuration.Builder()
                        .openMap("parameters")
                        .addSimple("address", "127.0.0.1")
                        .closeMap()
                        .build()));
                em.persist(new MaintenanceStep()
                    .setJobNumber(jobNumber.get())
                    .setJobType(MaintenanceStep.JobType.DEPLOY)
                    .setName("Announce")
                    .setStepNumber(2)
                    .setDescription("Announce 127.0.0.1 to 127.0.0.3")
                    .setConfiguration(new Configuration.Builder()
                        .openMap("parameters")
                        .addSimple("address", "127.0.0.1")
                        .closeMap()
                        .build()));
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

                Configuration expectedConfig = new Configuration.Builder()
                    .openMap("parameters")
                    .addSimple("address", "127.0.0.1")
                    .closeMap()
                    .build();

                org.testng.Assert.assertEquals(steps, expected, "The job steps do not match the expected values");
                assertEquals(steps.get(1).getConfiguration(), expectedConfig,
                    "The configuration is wrong for step " + steps.get(1).getStepNumber());
                assertEquals(steps.get(2).getConfiguration(), expectedConfig,
                    "The configuration is wrong for step " + steps.get(2).getStepNumber());

            }
        }, "There was an unexpected error fetching the job steps");
    }

    @Test(groups = "integration.ejb3")
    public void deleteStepFromJob() throws Exception {
        final AtomicInteger jobNumber = new AtomicInteger();
        final AtomicInteger configId = new AtomicInteger();
        final AtomicInteger stepId = new AtomicInteger();

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
                    .setDescription("Announce 127.0.0.1 to 127.0.0.2")
                    .setConfiguration(new Configuration.Builder()
                        .openMap("parameters")
                        .addSimple("address", "127.0.0.1")
                        .closeMap()
                        .build()));
                em.persist(new MaintenanceStep()
                    .setJobNumber(jobNumber.get())
                    .setJobType(MaintenanceStep.JobType.DEPLOY)
                    .setName("Announce")
                    .setStepNumber(2)
                    .setDescription("Announce 127.0.0.1 to 127.0.0.3")
                    .setConfiguration(new Configuration.Builder()
                        .openMap("parameters")
                        .addSimple("address", "127.0.0.1")
                        .closeMap()
                        .build()));
            }
        }, "Failed to persist new job steps");

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                List<MaintenanceStep> steps = em.createNamedQuery(MaintenanceStep.FIND_BY_JOB_NUM,
                    MaintenanceStep.class).setParameter("jobNumber", jobNumber.get()).getResultList();

                assertEquals("Cannot proceed with deletion. The number of steps is wrong.", 3, steps.size());

                MaintenanceStep step = steps.get(1);
                stepId.set(step.getId());
                configId.set(step.getConfiguration().getId());
                em.remove(step);
            }
        }, "Failed to delete step");

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                assertNull("The step configuration was not deleted", em.find(Configuration.class, configId.get()));
                assertNull("The step was not deleted", em.find(MaintenanceStep.class, stepId.get()));
            }
        }, "Failed to verify the step deletion");
    }

    @Test(groups = "integration.ejb3")
    public void findStepsByJobNumber() throws Exception {
        final AtomicInteger jobNumber = new AtomicInteger();
        final List<MaintenanceStep> steps = new ArrayList<MaintenanceStep>();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                MaintenanceStep baseStep = new MaintenanceStep()
                    .setJobType(MaintenanceStep.JobType.DEPLOY)
                    .setStepNumber(0)
                    .setName("LoadJobTest")
                    .setDescription("LoadJobTest")
                    .setConfiguration(new Configuration.Builder()
                        .openMap("parameters")
                        .addSimple("target", "127.0.0.1")
                        .closeMap()
                        .build());
                em.persist(baseStep);
                baseStep.setJobNumber(baseStep.getId());

                jobNumber.set(baseStep.getJobNumber());

                em.persist(new MaintenanceStep()
                    .setJobType(MaintenanceStep.JobType.DEPLOY)
                    .setStepNumber(1)
                    .setJobNumber(baseStep.getJobNumber())
                    .setName("Step1")
                    .setDescription("Step1")
                    .setConfiguration(new Configuration.Builder()
                        .addSimple("target", "127.0.0.1")
                        .openMap("parameters")
                        .addSimple("address", "127.0.0.2")
                        .closeMap()
                        .build()));
            }
        }, "Failed to persist job steps");

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                steps.addAll(em.createNamedQuery(MaintenanceStep.FIND_BY_JOB_NUM, MaintenanceStep.class)
                    .setParameter("jobNumber", jobNumber.get()).getResultList());
            }
        }, "Failed to load steps");

        for (MaintenanceStep step : steps) {
            // call Configuration.toString() to make sure the config is loaded
            assertNotNull("The step configuration should be loaded", step.getConfiguration().toString(true));
        }
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

    private void assertEquals(Configuration actual, Configuration expected, String msg) {
        if (!actual.equals(expected)) {
            fail(msg + ": Expected " + expected.toString(true) + " but was " + actual.toString(true));
        }
    }

}
