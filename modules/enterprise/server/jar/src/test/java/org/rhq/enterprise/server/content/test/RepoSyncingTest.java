package org.rhq.enterprise.server.content.test;

import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.content.ContentTestHelper;
import org.rhq.enterprise.server.plugin.pc.content.TestContentServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

public class RepoSyncingTest extends AbstractEJB3Test {

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        tx.begin();
    }

    @Test(enabled = true)
    public void testSyncRepos() throws Exception {

        TestContentServerPluginService pluginService = new TestContentServerPluginService(this);

        Repo repo = ContentTestHelper.getTestRepoWithContentSource();
        assert repo.getContentSources().size() == 1;
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
