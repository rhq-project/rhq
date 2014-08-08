package org.rhq.enterprise.server.storage;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.storage.MaintenanceJob;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.core.domain.storage.StorageMaintenanceJob;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.job.JobBuilder;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;

/**
 * @author John Sanda
 */
@Stateless
public class StorageClusterMaintenanceManagerBean implements StorageClusterMaintenanceManagerLocal {

    private final Log log = LogFactory.getLog(StorageClusterMaintenanceManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    @EJB
    private StorageClusterMaintenanceManagerLocal maintenanceManager;

    @Override
    public void scheduleMaintenance(StorageMaintenanceJob job) {
        MaintenanceStep baseStep = job.getBaseStep();

        log.info("Adding " + job + " to maintenance job queue");

        entityManager.persist(baseStep);
        baseStep.setJobNumber(baseStep.getId());
    }

    @Override
    public MaintenanceJob updateQueue(MaintenanceJob job) {
        return entityManager.merge(job);
    }

    @Override
    public StorageMaintenanceJob getNextJob() {
//        List<MaintenanceJob> jobs = entityManager.createNamedQuery(MaintenanceJob.QUERY_FIND_ALL, MaintenanceJob.class)
//            .getResultList();
//
//        if (jobs.isEmpty()) {
//            return null;
//        }
//
//        MaintenanceJob job = jobs.get(0);
//        if (job.isStepRecalculationNeeded()) {
//            JobBuilder jobBuilder = new JobBuilder(storageNodeManager.getStorageNodes());
//            job = jobBuilder.build(job);
//            job.setStepRecalculationNeeded(false);
//        }
//
//        return entityManager.merge(job);
        List<MaintenanceStep> steps = entityManager.createNamedQuery(MaintenanceStep.FIND_ALL, MaintenanceStep.class)
            .getResultList();
        if (steps.isEmpty()) {
            return null;
        }
        return new StorageMaintenanceJob(steps);
    }

    @Override
    public MaintenanceJob updateSteps(MaintenanceJob job) {
        JobBuilder jobBuilder = new JobBuilder(storageNodeManager.getStorageNodes());
        return entityManager.merge(jobBuilder.build(job));
    }

    @Override
    public void deleteStep(MaintenanceStep step) {
//        entityManager.remove(step);
        int result = entityManager.createNamedQuery(MaintenanceStep.DELETE_STEP).setParameter("id", step.getId())
            .executeUpdate();
        if (result == 0) {
            log.warn("Failed to delete " + step);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void execute() {
//        List<MaintenanceJob> jobs = entityManager.createNamedQuery(MaintenanceJob.QUERY_FIND_ALL, MaintenanceJob.class)
//            .getResultList();
////        JobBuilder jobBuilder = new JobBuilder(storageNodeManager.getStorageNodes());
//        for (MaintenanceJob job : jobs) {
//            log.info("Executing " + job);
//            job = maintenanceManager.updateSteps(job);
//
//            List<MaintenanceStep> steps = job.getMaintenanceSteps();
//            Iterator<MaintenanceStep> iterator = steps.iterator();
//
//            while (iterator.hasNext()) {
//                MaintenanceStep step = iterator.next();
//                MaintenanceStepRunner runner = getStepRunner(step);
//                runner.execute(step);
//                iterator.remove();
//                maintenanceManager.deleteStep(step);
//            }
//        }

        StorageMaintenanceJob job = maintenanceManager.getNextJob();
        if (job == null) {
            log.info("There are no jobs to execute");
            return;
        }
        log.info("Executing " + job);
        maintenanceManager.deleteStep(job.getBaseStep());
        log.info("Finished executing " + job);
    }

    private MaintenanceStepRunner getStepRunner(MaintenanceStep step) {
        try {
            Class clazz = Class.forName("org.rhq.enterprise.server.storage.maintenance.step." + step.getName());
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
