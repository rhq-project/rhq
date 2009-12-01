package org.rhq.enterprise.server.plugin.pc.content;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;

public class TestContentProvider implements ContentProvider, PackageSource, RepoSource {

    /**
     * Packages returned in a call to {@link #synchronizePackages(String, PackageSyncReport, Collection)} will
     * indicate they are of this type. Any test attempting to call this synchronize method should be sure to
     * create a package type of this name in the database prior to calling it.
     */
    public static final String PACKAGE_TYPE_NAME = "testContentProviderFakePackage";

    /**
     * In order to create a package type (as needed to create the type indicated in {@link #PACKAGE_TYPE_NAME}), the
     * package type must be created as part of this resource type.
     */
    public static final String RESOURCE_TYPE_NAME = "testContentProviderFakeResourceType";

    /**
     * In order to create a resource type (as needed to create the type indicated in {@link #RESOURCE_TYPE_NAME}), this
     * resource type plugin name should be used.
     */
    public static final String RESOURCE_TYPE_PLUGIN_NAME = "testContentProviderFakeResourceTypePlugin";

    public static final String EXISTING_IMPORTED_REPO_NAME = "testRepoImportedExisting";
    public static final String EXISTING_CANDIDATE_REPO_NAME = "testRepoCandidateExisting";

    /**
     * This content provider will return packages when asked to synchronize a repo with this name.
     */
    public static final String REPO_WITH_PACKAGES = EXISTING_IMPORTED_REPO_NAME;

    /**
     * Collection of packages that will be returned from calling
     * {@link #synchronizePackages(String, PackageSyncReport, Collection)} passing in a repo with the name
     * {@link #REPO_WITH_PACKAGES}.
     */
    public static final Map<ContentProviderPackageDetailsKey, ContentProviderPackageDetails> PACKAGES =
        new HashMap<ContentProviderPackageDetailsKey, ContentProviderPackageDetails>(3);
    static {

        ContentProviderPackageDetailsKey key1 =
            new ContentProviderPackageDetailsKey("package1", "version1", PACKAGE_TYPE_NAME, "noarch",
                RESOURCE_TYPE_NAME, RESOURCE_TYPE_PLUGIN_NAME);
        ContentProviderPackageDetails details1 = new ContentProviderPackageDetails(key1);
        details1.setFileName("filename1");
        details1.setLocation("foo1");

        PACKAGES.put(key1, details1);

        ContentProviderPackageDetailsKey key2 =
            new ContentProviderPackageDetailsKey("package2", "version2", PACKAGE_TYPE_NAME, "noarch",
                RESOURCE_TYPE_NAME, RESOURCE_TYPE_PLUGIN_NAME);
        ContentProviderPackageDetails details2 = new ContentProviderPackageDetails(key2);
        details2.setFileName("filename2");
        details2.setLocation("foo2");

        PACKAGES.put(key2, details2);
    }

    /**
     * If <code>true</code>, the call to {@link #testConnection()} will throw an exception.
     */
    private boolean failTest = false;

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

        if (!REPO_WITH_PACKAGES.equals(repoName)) {
            return;
        }

        // For each package this provider wants to introduce, make sure it doesn't exist already.
        // This basically means the report will only ever contain "added" packages and will only do so
        // on the first call to this method. This will likely be changed as new requirements emerge.
        for (ContentProviderPackageDetails pkg : PACKAGES.values()) {

            ContentProviderPackageDetails existingPackage =
                findDetailsByKey(pkg.getContentProviderPackageDetailsKey(), existingPackages);

            if (existingPackage == null) {
                report.addNewPackage(pkg);
            }

        }
    }

    public void initialize(Configuration configuration) throws Exception {
        // No-op
    }

    public void shutdown() {
        // No-op
    }

    public void testConnection() throws Exception {

        if (failTest) {
            throw new Exception("Mock content provider configured to fail the connection test");
        }

        System.out.println("Connection tested.");
    }

    public InputStream getInputStream(String location) throws Exception {

        String seed = "Test Bits " + System.currentTimeMillis();
        ByteArrayInputStream bais = new ByteArrayInputStream(seed.getBytes());
        BufferedInputStream bis = new BufferedInputStream(bais);

        return bis;
    }

    /**
     * Indicates if the {@link #testConnection()} method should fail (i.e. throw an exception).
     *
     * @param failTest drives the behavior of the test connection call
     */
    public void setFailTest(boolean failTest) {
        this.failTest = failTest;
    }

    private ContentProviderPackageDetails findDetailsByKey(ContentProviderPackageDetailsKey key,
                                                           Collection<ContentProviderPackageDetails> packages) {

        for (ContentProviderPackageDetails pkg : packages) {
            if (pkg.getContentProviderPackageDetailsKey().equals(key)) {
                return pkg;
            }
        }

        return null;
    }
}
