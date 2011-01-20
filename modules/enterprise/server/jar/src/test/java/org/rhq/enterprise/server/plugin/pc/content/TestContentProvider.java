package org.rhq.enterprise.server.plugin.pc.content;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageVersion;

public class TestContentProvider implements ContentProvider, PackageSource, RepoSource, DistributionSource {

    /**
     * Packages returned in a call to
     * {@link #synchronizePackages(String, PackageSyncReport, Collection)} will
     * indicate they are of this type. Any test attempting to call this
     * synchronize method should be sure to create a package type of this name
     * in the database prior to calling it.
     */
    public static final String PACKAGE_TYPE_NAME = "testContentProviderFakePackage";

    /**
     * In order to create a package type (as needed to create the type indicated
     * in {@link #PACKAGE_TYPE_NAME}), the package type must be created as part
     * of this resource type.
     */
    public static final String RESOURCE_TYPE_NAME = "testContentProviderFakeResourceType";

    /**
     * In order to create a resource type (as needed to create the type
     * indicated in {@link #RESOURCE_TYPE_NAME}), this resource type plugin name
     * should be used.
     */
    public static final String RESOURCE_TYPE_PLUGIN_NAME = "testContentProviderFakeResourceTypePlugin";

    public static final String EXISTING_IMPORTED_REPO_NAME = "testRepoImportedExisting";
    public static final String EXISTING_CANDIDATE_REPO_NAME = "testRepoCandidateExisting";

    /**
     * This content source will return packages when asked to synchronize a
     * repo with this name.
     */
    public static final String REPO_WITH_PACKAGES = EXISTING_IMPORTED_REPO_NAME;

    /**
     * This content source will return distributions when asked to synchronize
     * a repo with this name.
     */
    public static final String REPO_WITH_DISTRIBUTIONS = EXISTING_IMPORTED_REPO_NAME;

    /**
     * Collection of packages that will be returned from calling
     * {@link #synchronizePackages(String, PackageSyncReport, Collection)}
     * passing in a repo with the name {@link #REPO_WITH_PACKAGES}.
     */
    public static final Map<ContentProviderPackageDetailsKey, ContentProviderPackageDetails> PACKAGES = new HashMap<ContentProviderPackageDetailsKey, ContentProviderPackageDetails>(
        2);
    static {

        ContentProviderPackageDetailsKey key1 = new ContentProviderPackageDetailsKey("package1", "version1",
            PACKAGE_TYPE_NAME, "noarch", RESOURCE_TYPE_NAME, RESOURCE_TYPE_PLUGIN_NAME);
        ContentProviderPackageDetails details1 = new ContentProviderPackageDetails(key1);
        details1.setFileName("filename1");
        details1.setFileSize(4L);
        details1.setLocation("foo1");

        PACKAGES.put(key1, details1);

        ContentProviderPackageDetailsKey key2 = new ContentProviderPackageDetailsKey("package2", "version2",
            PACKAGE_TYPE_NAME, "noarch", RESOURCE_TYPE_NAME, RESOURCE_TYPE_PLUGIN_NAME);
        ContentProviderPackageDetails details2 = new ContentProviderPackageDetails(key2);
        details2.setFileName("filename2");
        details2.setFileSize(4L);
        details2.setLocation("foo2");

        PACKAGES.put(key2, details2);
    }

    /**
     * Number of associated distribution files: 2
     */
    public static final String DISTRIBUTION_1_LABEL = "distribution1";

    /**
     * Number of associated distribution files: 1
     */
    public static final String DISTRIBUTION_2_LABEL = "distribution2";

    public static final Map<String, DistributionDetails> DISTRIBUTIONS = new HashMap<String, DistributionDetails>(2);
    static {

        // Note: The type "kickstart" should already be in the database from
        // installation

        /*
         * Note: The md5 sums below will be used to determine if the file needs
         * to be loaded through a call to getPackageBits. I set these to an
         * invalid MD5 so they will *always* be downloaded.
         */
        DistributionDetails dis1 = new DistributionDetails(DISTRIBUTION_1_LABEL, "kickstart");
        dis1.setDistributionPath("/kstrees");
        DistributionFileDetails file11 = new DistributionFileDetails("dist1file1", System.currentTimeMillis(), "zzz");
        DistributionFileDetails file12 = new DistributionFileDetails("dist1file2", System.currentTimeMillis(), "zzz");
        dis1.addFile(file11);
        dis1.addFile(file12);

        DISTRIBUTIONS.put(dis1.getLabel(), dis1);

        DistributionDetails dis2 = new DistributionDetails(DISTRIBUTION_2_LABEL, "kickstart");
        dis2.setDistributionPath("/kstrees");
        DistributionFileDetails file21 = new DistributionFileDetails("dist2file1", System.currentTimeMillis(), "zzz");
        dis2.addFile(file21);

        DISTRIBUTIONS.put(dis2.getLabel(), dis2);
    }

    public static final int PACKAGE_COUNT_FOR_BITS = PACKAGES.size() + 3; // packages
    // +
    // number
    // of
    // distro
    // files

    /**
     * If <code>true</code>, the call to {@link #testConnection()} will throw an
     * exception.
     */
    private boolean failTest = false;

    // Set this value if you want the packageSync step to take a while
    private int longRunningSynchSleep = 0;

    /**
     * Holds a list of all repo names that were passed into calls to
     * {@link #synchronizePackages(String, PackageSyncReport, Collection)} to
     * track when and with what data these calls are made.
     */
    private List<String> logSynchronizePackagesRepos = new ArrayList<String>();

    /**
     * Holds a list of all locations passed into calls to
     * {@link #getInputStream(String)}.
     */
    private List<String> logGetInputStreamLocations = new ArrayList<String>();

    /**
     * Holds a list of all repo names that were passed into calls to
     * {@link #synchronizeDistribution(String, DistributionSyncReport, Collection)}
     * .
     */
    private List<String> logSynchronizeDistroRepos = new ArrayList<String>();

    public void initialize(Configuration configuration) throws Exception {
        // No-op
    }

    public void shutdown() {
        // No-op
    }

    public void testConnection() throws Exception {

        if (failTest) {
            throw new Exception("Mock content source configured to fail the connection test");
        }

        System.out.println("Connection tested.");
    }

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
        // Parent explicitly added to this list *after* this child to ensure
        // that's not a problem
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
        Collection<ContentProviderPackageDetails> existingPackages) throws SyncException, InterruptedException {

        logSynchronizePackagesRepos.add(repoName);

        if (!REPO_WITH_PACKAGES.equals(repoName)) {
            return;
        }

        // For each package this provider wants to introduce, make sure it
        // doesn't exist already.
        // This basically means the report will only ever contain "added"
        // packages and will only do so
        // on the first call to this method. This will likely be changed as new
        // requirements emerge.
        for (ContentProviderPackageDetails pkg : PACKAGES.values()) {

            ContentProviderPackageDetails existingPackage = findDetailsByKey(pkg.getContentProviderPackageDetailsKey(),
                existingPackages);

            if (existingPackage == null) {
                report.addNewPackage(pkg);
            }

        }
        System.out.println(this.getClass().getSimpleName() + ".synchronizePackages sleeping for "
            + this.longRunningSynchSleep + " seconds");
        Thread.sleep(this.longRunningSynchSleep);
    }

    public InputStream getInputStream(String location) throws Exception {

        logGetInputStreamLocations.add(location);

        String seed = "Test Bits " + System.currentTimeMillis();
        ByteArrayInputStream bais = new ByteArrayInputStream(seed.getBytes());
        BufferedInputStream bis = new BufferedInputStream(bais);

        return bis;
    }

    public Comparator<PackageVersion> getPackageVersionComparator() {
        return null;
    }
    
    public void synchronizeDistribution(String repoName, DistributionSyncReport report,
        Collection<DistributionDetails> existingDistros) throws SyncException, InterruptedException {

        logSynchronizeDistroRepos.add(repoName);

        if (!REPO_WITH_DISTRIBUTIONS.equals(repoName)) {
            return;
        }

        for (DistributionDetails distro : DISTRIBUTIONS.values()) {

            boolean existing = false;
            for (DistributionDetails existingDistro : existingDistros) {
                if (distro.getLabel().equals(existingDistro.getLabel())) {
                    existing = true;
                    break;
                }
            }

            if (!existing) {
                report.addDistro(distro);
            }
        }
        System.out.println(this.getClass().getSimpleName() + ".synchronizeDistribution sleeping for "
            + this.longRunningSynchSleep + " seconds");

        Thread.sleep(this.longRunningSynchSleep);
    }

    public String getDistFileRemoteLocation(String repoName, String label, String relativeFilename) {
        return "foo";
    }

    /**
     * Returns a list of repo names that were used to all calls to
     * {@link #synchronizePackages(String, PackageSyncReport, Collection)}
     * either since the creation of this instance or the last call to
     * {@link #reset()}.
     * 
     * @return handle to the actual list used to capture these names; be careful
     *         about iterating this list while making other calls against this
     *         instance
     */
    public List<String> getLogSynchronizePackagesRepos() {
        return logSynchronizePackagesRepos;
    }

    /**
     * Returns a list of locations passed into all calls to
     * {@link #getInputStream(String)} since the creation of this instance or
     * the lsat call to {@link #reset()}.
     * 
     * @return handle to the actual list used to capture these names; be careful
     *         about iterating this list while making other calls against this
     *         instance
     */
    public List<String> getLogGetInputStreamLocations() {
        return logGetInputStreamLocations;
    }

    /**
     * Indicate how long you want the provider to sleep during the PackageSync
     * step
     * 
     * @param longRunningSynchSleep
     */
    public void setLongRunningSynchSleep(int longRunningSynchSleep) {
        this.longRunningSynchSleep = longRunningSynchSleep;
    }

    /**
     * Rests the logging of calls made into this instance.
     */
    public void reset() {
        logGetInputStreamLocations.clear();
        logSynchronizePackagesRepos.clear();
        logSynchronizeDistroRepos.clear();
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

    public SyncProgressWeight getSyncProgressWeight() {
        return SyncProgressWeight.DEFAULT_WEIGHTS;
    }

}
