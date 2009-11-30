package org.rhq.enterprise.server.plugin.pc.content;

import java.io.InputStream;
import java.util.Collection;

import org.rhq.core.domain.configuration.Configuration;

public class TestContentProvider implements ContentProvider, PackageSource, RepoSource {

    public static final String EXISTING_IMPORTED_REPO_NAME = "testRepoImportedExisting";
    public static final String EXISTING_CANDIDATE_REPO_NAME = "testRepoCandidateExisting";

    public RepoImportReport importRepos() throws Exception {
        RepoImportReport report = new RepoImportReport();

        // Create a repo group in the system
        RepoGroupDetails group1 = new RepoGroupDetails("testRepoGroup", "family");
        report.addRepoGroup(group1);

        // Simple repo
        RepoDetails repo1 = new RepoDetails("testRepo1");
        repo1.setDescription("First test repo");
        report.addRepo(repo1);

        // Repo belonging to a group that was created in the sync
        RepoDetails repo2 = new RepoDetails("testRepo2");
        repo2.setRepoGroup("testRepoGroup");
        report.addRepo(repo2);

        // Repo with a parent repo created in this sync
        // Parent explicitly added to this list *after* this child to ensure that's not a problem
        RepoDetails repo3 = new RepoDetails("testRepo3");
        report.addRepo(repo3);

        RepoDetails repo4 = new RepoDetails("testRepo4");
        repo4.setParentRepoName("testRepo3");
        report.addRepo(repo4);

        // Repo that was already imported in the system
        RepoDetails repo5 = new RepoDetails(EXISTING_IMPORTED_REPO_NAME);
        report.addRepo(repo5);

        // Repo that was already a candidate in the system
        RepoDetails repo6 = new RepoDetails(EXISTING_CANDIDATE_REPO_NAME);
        report.addRepo(repo6);

        return report;
    }

    public void synchronizePackages(String repoName, PackageSyncReport report,
        Collection<ContentProviderPackageDetails> existingPackages) throws Exception {
        // No-op
    }

    public void initialize(Configuration configuration) throws Exception {
        // No-op
    }

    public void shutdown() {
        // No-op
    }

    public void testConnection() throws Exception {
        // No-op
        System.out.println("Connection tested.");
    }

    public InputStream getInputStream(String location) throws Exception {
        // No-op
        return null;
    }
}
