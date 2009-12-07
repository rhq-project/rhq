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
    TestContentProvider p1;

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
        p1 = new TestContentProvider();
        pluginService.associateContentProvider(contentSource, p1);
        repo.addContentSource(contentSource);
        em.persist(repo);
        em.flush();
    }

    @Test(enabled = true)
    public void testSyncRepos() throws Exception {
        p1.setLongRunningSyncs(true);

        assert repo.getContentSources().size() == 1;
        // We have to commit because bean has new transaction inside it
        getTransactionManager().commit();
        getTransactionManager().begin();

        RepoSyncResults results = getSyncResults(repo.getId());
        assert results == null;

        System.out.println("Starting sync");
        SyncerThread st = new SyncerThread();
        st.start();

        boolean gotInProgress = false;
        boolean gotResults = false;
        // Run 30 times or until it is synced. 
        for (int i = 0; ((i < 30) && !st.isSynced()); i++) {
            results = getSyncResults(repo.getId());
            if (results != null) {
                if (results.getStatus() == ContentSyncStatus.INPROGRESS) {
                    gotInProgress = true;
                }
                if (results.getResults() != null) {
                    gotResults = true;
                }
            }
            System.out.println("st: " + st.isSynced());
            Thread.sleep(1000);
        }
        System.out.println("Finished sync");
        getTransactionManager().commit();
        assert gotInProgress;
        assert gotResults;

        assert st.isSynced();
        results = getSyncResults(repo.getId());
        assert results != null;
        assert (results.getStatus() == ContentSyncStatus.SUCCESS);
        assert (results.getResults() != null);

        results = LookupUtil.getRepoManagerLocal().getMostRecentSyncResults(
            LookupUtil.getSubjectManager().getOverlord(), repo.getId());
        assert results != null;

    }

    private RepoSyncResults getSyncResults(int repoId) {
        Query q = em.createNamedQuery(RepoSyncResults.QUERY_GET_INPROGRESS_BY_REPO_ID);
        q.setParameter("repoId", repoId);

        List<RepoSyncResults> rlist = q.getResultList(); // will be ordered by start time descending
        if (rlist != null && rlist.size() > 0) {
            return rlist.get(0);
        } else {
            return null;
        }
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

    class SyncerThread extends Thread {

        boolean synced = false;

        @Override
        public void run() {
            try {
                synced = pluginService.getContentProviderManager().synchronizeRepo(repo.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        public boolean isSynced() {
            return synced;
        }

    }
}
