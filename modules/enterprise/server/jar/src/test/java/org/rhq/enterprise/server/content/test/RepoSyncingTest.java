package org.rhq.enterprise.server.content.test;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.plugin.pc.content.TestContentServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoSyncingTest extends AbstractEJB3Test {
    private EntityManager em;
    private Repo repo;
    private ContentSource contentSource;
    private TestContentServerPluginService pluginService;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {

        TransactionManager tx = getTransactionManager();
        tx.begin();
        em = getEntityManager();
        pluginService = new TestContentServerPluginService(this);

        ContentSourceType type = new ContentSourceType("testGetSyncResultsListCST");
        em.persist(type);
        em.flush();
        contentSource = new ContentSource("testGetSyncResultsListCS", type);
        em.persist(contentSource);
        em.flush();

        repo = new Repo("testSyncRepos");
        repo.addContentSource(contentSource);
        em.persist(repo);
        em.flush();
    }

    @Test(enabled = true)
    public void testSyncRepos() throws Exception {

        assert repo.getContentSources().size() == 1;
        // We have to commit because bean has new transaction inside it
        getTransactionManager().commit();
        boolean synced = pluginService.getContentProviderManager().synchronizeRepo(repo.getId());

        assert synced;

    }

    @Test(enabled = true)
    public void testSyncCount() throws Exception {

        Integer[] ids = { repo.getId() };
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        getTransactionManager().commit();
        int syncCount = LookupUtil.getRepoManagerLocal().synchronizeRepos(overlord, ids);

        assert syncCount == 1;

    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        unprepareServerPluginService();

        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        Repo delRepo = em.find(Repo.class, repo.getId());
        em.remove(delRepo);
        ContentSource deleteSrc = em.find(ContentSource.class, contentSource.getId());
        em.remove(deleteSrc);

        TransactionManager tx = getTransactionManager();
        if (tx != null) {
            tx.commit();
        }
    }

}
