package org.rhq.enterprise.server.storage;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
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
        System.setProperty("rhq.storage.maintenance.calculator.lookup", TestCalculatorLookup.class.getName());
        maintenanceManager.init();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                StorageMaintenanceJob job = new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY, "test deply",
                    new Configuration());
                maintenanceManager.scheduleMaintenance(job);
//                em.persist(new MaintenanceStep()
//                    .setJobType(MaintenanceStep.JobType.DEPLOY)
//                    .setName("BASE_STEP")
//                    .setStepNumber(0)
//                    .setDescription("Deploy 127.0.0.2"));
            }
        }, "Failed to create test deploy job");

        maintenanceManager.execute();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                assertEquals("The job queue should be empty", 0, maintenanceManager.loadQueue().size());
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
