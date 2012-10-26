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
import org.rhq.enterprise.server.content.ContentManagerHelper;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.TestContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.TestContentServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoSyncingTest extends AbstractEJB3Test {
    private static final boolean ENABLED = false;

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

    @Test(enabled = ENABLED)
    public void testSyncResults() throws Exception {
        // We have to commit because bean has new transaction inside it
        getTransactionManager().commit();

        boolean synced = pluginService.getContentProviderManager().synchronizeRepo(repo.getId());
        RepoSyncResults results = getSyncResults(repo.getId());
        System.out.println("results : " + results.getResults());
        // Testing that we merged all the results into the RepoSyncReport
        assert (results.getResults().indexOf("MERGE COMPLETE.") > 0);
        assert (results.getPercentComplete().equals(new Long(100)));
    }

    @Test(enabled = ENABLED)
    public void testMultipleSyncResults() throws Exception {
        getTransactionManager().commit();
        // Sync 2x so we get multiple results
        pluginService.getContentProviderManager().synchronizeRepo(repo.getId());
        pluginService.getContentProviderManager().synchronizeRepo(repo.getId());

        em.refresh(repo);

        Query q = em.createNamedQuery(RepoSyncResults.QUERY_GET_ALL_BY_REPO_ID);
        q.setParameter("repoId", repo.getId());
        List<RepoSyncResults> rlist = q.getResultList(); // will be ordered by start time descending
        RepoSyncResults r1 = rlist.get(0);
        RepoSyncResults r2 = rlist.get(1);
        assert (r1.getId() > r2.getId());

        rlist = repo.getSyncResults();
        r1 = rlist.get(0);
        r2 = rlist.get(1);
        assert (r1.getId() > r2.getId());
    }

    @Test(enabled = ENABLED)
    public void testSyncRepos() throws Exception {
        p1.setLongRunningSynchSleep(5000);

        assert repo.getContentSources().size() == 1;
        // We have to commit because bean has new transaction inside it
        getTransactionManager().commit();

        RepoSyncResults results = getSyncResults(repo.getId());
        assert results == null;

        System.out.println("Starting sync: " + repo.getId());
        // 

        SyncerThread st = new SyncerThread();
        st.start();

        boolean gotPackageBits = false;
        boolean gotResults = false;
        // Run 30 times or until it is synced. 
        for (int i = 0; ((i < 300) && !st.isSynced() && !st.isErrored()); i++) {
            results = getSyncResults(repo.getId());
            if (results != null) {
                if (results.getStatus() == ContentSyncStatus.PACKAGEMETADATA) {
                    gotPackageBits = true;
                }
                if (results.getResults() != null) {
                    gotResults = true;
                }
            }
            Thread.sleep(1000);
        }
        System.out.println("Finished sync");
        assert gotPackageBits;
        assert gotResults;

        assert st.isSynced();
        results = getSyncResults(repo.getId());
        assert results != null;

        assert (results.getStatus() == ContentSyncStatus.SUCCESS);
        assert (results.getResults() != null);

        // Testing that we merged all the results into the RepoSyncReport
        assert (results.getResults().indexOf("MERGE COMPLETE.") > 0);

        results = LookupUtil.getRepoManagerLocal().getMostRecentSyncResults(
            LookupUtil.getSubjectManager().getOverlord(), repo.getId());
        assert results != null;

    }

    @Test(enabled = ENABLED)
    public void testSyncCount() throws Exception {

        int[] ids = { repo.getId() };
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        getTransactionManager().commit();
        int syncCount = LookupUtil.getRepoManagerLocal().synchronizeRepos(overlord, ids);

        assert syncCount == 1;

    }

    @Test(enabled = ENABLED)
    public void testCancelSync() throws Exception {
        getTransactionManager().commit();
        p1 = new TestContentProvider();
        p1.setLongRunningSynchSleep(10000);
        pluginService.associateContentProvider(contentSource, p1);

        prepareScheduler();
        ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
        pc.syncRepoNow(repo);

        for (int i = 0; i < 10; i++) {
            RepoSyncResults res = getSyncResults(repo.getId());
            if (res != null) {
                System.out.println("status: [" + res.getStatus() + "] CTRL+C to exit");
                if (res.getStatus() == ContentSyncStatus.CANCELLED) {
                    break;
                }
            } else {
                System.out.println("CTRL+C to exit");
            }
            Thread.sleep(1000);
            if (i == 5) {
                pc.cancelRepoSync(LookupUtil.getSubjectManager().getOverlord(), repo);
            }

        }
        RepoSyncResults res = getSyncResults(repo.getId());
        assert res != null;
        assert res.getStatus() == ContentSyncStatus.CANCELLED;
        unprepareScheduler();
    }

    private RepoSyncResults getSyncResults(int repoId) {
        Query q = em.createNamedQuery(RepoSyncResults.QUERY_GET_ALL_BY_REPO_ID);
        q.setParameter("repoId", repoId);
        List<RepoSyncResults> rlist = q.getResultList(); // will be ordered by start time descending
        if (rlist != null && rlist.size() > 0) {
            assert rlist.size() == 1;
            RepoSyncResults retval = rlist.get(0);
            em.refresh(retval);
            return rlist.get(0);
        } else {
            return null;
        }
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        unprepareServerPluginService();

        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        ContentSource deleteSrc = em.find(ContentSource.class, contentSource.getId());
        // em.remove(deleteSrc.getContentSourceType());
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
        boolean errored = false;

        public boolean isErrored() {
            return errored;
        }

        @Override
        public void run() {
            try {
                TransactionManager tx = getTransactionManager();
                tx.begin();
                System.out.println("SyncerThread :: Starting sync");
                synced = pluginService.getContentProviderManager().synchronizeRepo(repo.getId());
                System.out.println("SyncerThread :: Finished sync : " + synced);
                tx.commit();
            } catch (Exception e) {
                errored = true;
                throw new RuntimeException(e);
            }

        }

        public boolean isSynced() {
            return synced;
        }

    }
}
