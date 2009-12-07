package org.rhq.enterprise.server.content.test;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.ContentSyncStatus;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoDistribution;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.enterprise.server.plugin.pc.content.TestContentProvider;
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

        repo = new Repo(TestContentProvider.EXISTING_IMPORTED_REPO_NAME);
        repo.addContentSource(contentSource);
        em.persist(repo);
        em.flush();
    }

    @Test(enabled = true)
    public void testSyncRepos() throws Exception {

        assert repo.getContentSources().size() == 1;
        // We have to commit because bean has new transaction inside it
        getTransactionManager().commit();

        Query q = em.createNamedQuery(RepoSyncResults.QUERY_GET_INPROGRESS_BY_REPO_ID);
        q.setParameter("repoId", repo.getId());

        List<RepoSyncResults> rlist = q.getResultList(); // will be ordered by start time descending
        assert (rlist != null);
        assert (rlist.size() == 0);

        boolean synced = pluginService.getContentProviderManager().synchronizeRepo(repo.getId());
        assert synced;

        q = em.createNamedQuery(RepoSyncResults.QUERY_GET_INPROGRESS_BY_REPO_ID);
        q.setParameter("repoId", repo.getId());

        rlist = q.getResultList(); // will be ordered by start time descending
        assert (rlist != null);
        assert (rlist.size() > 0);
        RepoSyncResults results = rlist.get(0);
        assert (results.getResults() != null);
        assert (results.getStatus() == ContentSyncStatus.INPROGRESS);

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
        ContentSource deleteSrc = em.find(ContentSource.class, contentSource.getId());
        em.remove(deleteSrc);

        Query q = em.createNamedQuery(RepoDistribution.QUERY_FIND_BY_REPO_ID);
        q.setParameter("repoId", repo.getId());
        List<RepoDistribution> rds = q.getResultList();
        for (RepoDistribution rd : rds) {
            Distribution d = rd.getRepoDistributionPK().getDistribution();
            List<DistributionFile> dfiles = LookupUtil.getDistributionManagerLocal().getDistributionFilesByDistId(
                d.getId());
            for (DistributionFile df : dfiles) {
                DistributionFile dfl = em.find(DistributionFile.class, df.getId());
                em.remove(dfl);
            }
            em.remove(rd);
            em.remove(d);
        }

        Repo delRepo = em.find(Repo.class, repo.getId());
        em.remove(delRepo);

        TransactionManager tx = getTransactionManager();
        if (tx != null) {
            tx.commit();
        }
    }
}
