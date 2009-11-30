package org.rhq.enterprise.server.content.test;

import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.plugin.pc.content.TestContentServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoSyncingTest extends AbstractEJB3Test {

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        tx.begin();
    }

    @Test(enabled = true)
    public void testSyncRepos() throws Exception {

        TestContentServerPluginService pluginService = new TestContentServerPluginService(this);
        Repo repo = new Repo("testSyncRepos");

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        LookupUtil.getRepoManagerLocal().createRepo(overlord, repo);

        boolean synced = pluginService.getContentProviderManager().synchronizeRepo(repo.getId());
        assert synced;

    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        unprepareServerPluginService();

        TransactionManager tx = getTransactionManager();
        if (tx != null) {
            tx.rollback();
        }
    }

}
