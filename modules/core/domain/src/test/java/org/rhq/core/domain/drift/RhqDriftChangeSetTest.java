package org.rhq.core.domain.drift;

import javax.transaction.SystemException;

import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.test.AbstractEJB3Test;

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;

public class RhqDriftChangeSetTest extends AbstractEJB3Test {

    static interface TransactionCallback {
        void execute() throws Exception;
    }

    Resource resource;

    @BeforeGroups(groups = {"drift.changeset"})
    public void initResource() throws Exception {
        resource = new ResourceBuilder().createRandomServer().build();
        resource.setId(0);

        final ResourceType type = resource.getResourceType();
        type.setId(0);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                getEntityManager().createQuery("delete from RhqDriftChangeSet").executeUpdate();
                getEntityManager().createQuery("delete from Resource").executeUpdate();
                getEntityManager().createQuery("delete from ResourceType").executeUpdate();
            }
        });

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                getEntityManager().persist(type);
                getEntityManager().persist(resource);
            }
        });
    }

    @BeforeMethod(groups = {"drift.changeset"})
    public void setup() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                getEntityManager().createQuery("delete from RhqDriftChangeSet").executeUpdate();
            }
        });
    }

    @Test(groups = {"integration.ejb3", "drift.changeset"}, enabled = false)
    public void insertAndLoad() throws Exception {
        final RhqDriftChangeSet changeSet = new RhqDriftChangeSet();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                changeSet.setCategory(COVERAGE);
                changeSet.setVersion(0);
                changeSet.setResource(resource);

                getEntityManager().persist(changeSet);
            }
        });

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                // Verify that we can both load by id and by JPQL to ensure that using a
                // custom type for the id works.

                RhqDriftChangeSet actual = getEntityManager().find(RhqDriftChangeSet.class, changeSet.getId());
                assertNotNull("Failed to load change set by id", actual);

                actual = (RhqDriftChangeSet) getEntityManager().createQuery("from RhqDriftChangeSet where id = :id")
                    .setParameter("id", actual.getId())
                    .getSingleResult();
                assertNotNull("Failed to load change set with JPQL query", actual);
            }
        });
    }

    void executeInTransaction(TransactionCallback callback) {
        try {
            getTransactionManager().begin();
            callback.execute();
            getTransactionManager().commit();
        } catch (Throwable t) {
            try {
                getTransactionManager().rollback();
            } catch (SystemException e) {
                throw new RuntimeException("Failed to rollback transaction", e);
            }
            throw new RuntimeException(t.getCause());
        }
    }

}
