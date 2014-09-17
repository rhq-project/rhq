/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.content.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.server.content.ContentDiscoveryReport;
import org.rhq.core.clientapi.server.content.DeletePackagesRequest;
import org.rhq.core.clientapi.server.content.DeployPackagesRequest;
import org.rhq.core.clientapi.server.content.RetrievePackageBitsRequest;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.ContentRequestType;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.InstalledPackageHistoryStatus;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemoveIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;

@Test
public class ContentManagerBeanTest extends AbstractEJB3Test {
    // Attributes  --------------------------------------------

    private static final boolean ENABLE_TESTS = true;

    /**
     * ContentAgentService method implementations should synchronize on this to allow the test method to pause before
     * the reponse is sent and check the state of the server.
     */
    private static final Object responseLock = new Object();

    private ContentManagerLocal contentManager;
    private SubjectManagerLocal subjectManager;

    private MockContentAgentService contentAgentService = new MockContentAgentService();

    private PackageType packageType1;
    private PackageType packageType2;
    private PackageType packageType3;
    private PackageType packageType4;

    /**
     * Type: packageType1 Versions: 2
     */
    private Package package1;

    /**
     * Type: packageType2 Versions: 2
     */
    private Package package2;

    /**
     * Type: packageType3 Versions: 1
     */
    private Package package3;

    /**
     * Type: packageType4 Versions: 2 Installed: Version 1
     */
    private Package package4;

    /**
     * Type: packageType4 Versions: 1 Installed: Version 1
     */
    private Package package5;

    private InstalledPackage installedPackage1;
    private InstalledPackage installedPackage2;

    /**
     * Architecture used in the creation of all sample package versions.
     */
    private Architecture architecture1;

    /**
     * Extra architecture used in tests.
     */
    private Architecture architecture2;

    private List<DeployPackageStep> stepResults;

    private ResourceType resourceType1;
    private Resource resource1;

    // Setup  --------------------------------------------

    //@BeforeClass don't use BeforeClass as Arquillian 1.0.2 invokes it on every test method
    protected void beforeClass() throws Exception {
        contentManager = LookupUtil.getContentManager();
        subjectManager = LookupUtil.getSubjectManager();

        populateResponseSteps();
    }

    @Override
    protected void beforeMethod() throws Exception {
        beforeClass();

        setupTestEnvironment();
    }

    @Override
    protected void afterMethod() throws Exception {
        tearDownTestEnvironment();
    }

    // Test Cases  --------------------------------------------

    @Test(enabled = ENABLE_TESTS)
    public void testPersistSafely() throws Throwable {

        final String pkgName = "testPersistSafelyPackage";
        final String pvName = "testPersistSafelyPackageVersion version";
        final PackageType pkgType = packageType1;
        final Architecture arch = architecture1;

        // these will not have any JPA stubs inside them (i.e. no lazy exceptions will occur with these)
        Package pkgPojo = new Package(pkgName, pkgType);
        PackageVersion pvPojo = new PackageVersion(pkgPojo, pvName, arch);
        Configuration configPojo = new Configuration();
        configPojo.put(new PropertySimple("one", "two"));
        pvPojo.setExtraProperties(configPojo);

        Package persistedPkg = null;
        PackageVersion persistedPV = null;

        try {
            // create the entities that we will pass to the persist methods - these will get altered
            Package pkg = new Package(pkgName, pkgType);
            PackageVersion pv = new PackageVersion(pkg, pvName, arch);
            pv.setExtraProperties(configPojo);

            persistedPkg = this.contentManager.persistOrMergePackageSafely(pkg);
            assert persistedPkg != null;
            assert persistedPkg.getId() > 0;
            assert persistedPkg.equals(pkgPojo) : "not equal: " + persistedPkg + "<>" + pkgPojo;

            persistedPV = this.contentManager.persistOrMergePackageVersionSafely(pv);
            assert persistedPV != null;
            assert persistedPV.getId() > 0;
            assert persistedPV.equals(pvPojo) : "not equal: " + persistedPV + "<>" + pvPojo;
            assert persistedPV.getExtraProperties() != null;
            assert persistedPV.getExtraProperties().getId() > 0;
            assert persistedPV.getExtraProperties().getSimple("one").getStringValue().equals("two");

            // remember their new IDs - we want to make sure the same IDs are reused later
            int pkgId = persistedPkg.getId();
            int pvId = persistedPV.getId();
            int configId = persistedPV.getExtraProperties().getId();
            int propId = persistedPV.getExtraProperties().getSimple("one").getId();

            // we've persisted them, let's make sure our "safely" methods still work
            // create new entities that we will pass to the persist methods - these will get altered
            pkg = new Package(pkgName, pkgType);
            pv = new PackageVersion(pkg, pvName, arch);
            pv.setExtraProperties(configPojo);

            persistedPkg = this.contentManager.persistOrMergePackageSafely(pkg);
            assert persistedPkg != null;
            assert persistedPkg.getId() > 0;
            assert persistedPkg.equals(pkgPojo) : "not equal: " + persistedPkg + "<>" + pkgPojo;

            persistedPV = this.contentManager.persistOrMergePackageVersionSafely(pv);
            assert persistedPV != null;
            assert persistedPV.getId() > 0;
            assert persistedPV.equals(pvPojo) : "not equal: " + persistedPV + "<>" + pvPojo;
            assert persistedPV.getExtraProperties() != null;
            assert persistedPV.getExtraProperties().getId() > 0;
            assert persistedPV.getExtraProperties().getSimple("one").getStringValue().equals("two");

            // make sure we merged the existing entities - we should not have created new ones
            assert pkgId == persistedPkg.getId();
            assert pvId == persistedPV.getId();
            assert configId == persistedPV.getExtraProperties().getId();
            assert propId == persistedPV.getExtraProperties().getSimple("one").getId();

            // now pass in existing entities with non-zero IDs
            pkg = new Package(pkgName, pkgType);
            pv = new PackageVersion(pkg, pvName, arch);
            pv.setExtraProperties(configPojo);
            pkg.setId(persistedPkg.getId());
            pv.setId(persistedPV.getId());

            persistedPkg = this.contentManager.persistOrMergePackageSafely(pkg);
            assert persistedPkg != null;
            assert persistedPkg.getId() > 0;
            assert persistedPkg.equals(pkgPojo) : "not equal: " + persistedPkg + "<>" + pkgPojo;

            persistedPV = this.contentManager.persistOrMergePackageVersionSafely(pv);
            assert persistedPV != null;
            assert persistedPV.getId() > 0;
            assert persistedPV.equals(pvPojo) : "not equal: " + persistedPV + "<>" + pvPojo;
            assert persistedPV.getExtraProperties() != null;
            assert persistedPV.getExtraProperties().getId() > 0;
            assert persistedPV.getExtraProperties().getSimple("one").getStringValue().equals("two");

            // make sure we merged the existing entities - we should not have created new ones
            assert pkgId == persistedPkg.getId();
            assert pvId == persistedPV.getId();
            assert configId == persistedPV.getExtraProperties().getId();
            assert propId == persistedPV.getExtraProperties().getSimple("one").getId();

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().begin();

            try {
                if (persistedPV != null && persistedPV.getId() > 0) {
                    persistedPV = em.find(PackageVersion.class, persistedPV.getId());
                    em.remove(persistedPV);
                }
                if (persistedPkg != null && persistedPkg.getId() > 0) {
                    persistedPkg = em.find(Package.class, persistedPkg.getId());
                    em.remove(persistedPkg);
                }
            } finally {
                getTransactionManager().commit();
            }
        }

    }

    @SuppressWarnings("unchecked")
    @Test(enabled = ENABLE_TESTS)
    public void testInventoryMerge() throws Exception {
        // Setup  --------------------------------------------
        ContentDiscoveryReport report = new ContentDiscoveryReport();
        report.setResourceId(resource1.getId());

        // Package version that exists
        PackageDetailsKey key1 = new PackageDetailsKey(package1.getName(), "2.0.0",
            package1.getPackageType().getName(), architecture1.getName());
        ResourcePackageDetails package1 = new ResourcePackageDetails(key1);

        report.addDeployedPackage(package1);

        // Package version number that doesn't exist
        PackageDetailsKey key2 = new PackageDetailsKey(package2.getName(), "3.0.0", this.package2.getPackageType()
            .getName(), architecture1.getName());
        ResourcePackageDetails package2 = new ResourcePackageDetails(key2);

        report.addDeployedPackage(package2);

        // Package with same version number but different architecture
        PackageDetailsKey key3 = new PackageDetailsKey(package3.getName(), "1.0.0", this.package3.getPackageType()
            .getName(), architecture2.getName());
        ResourcePackageDetails package3 = new ResourcePackageDetails(key3);

        report.addDeployedPackage(package3);

        // Package where entire package does not exist
        PackageDetailsKey key4 = new PackageDetailsKey("PackageX", "1.0.0", packageType4.getName(),
            architecture1.getName());
        ResourcePackageDetails package4 = new ResourcePackageDetails(key4);

        //   Fully populate this version to make sure the translation from details -> domain model works
        //   Don't need to do this on all packages
        package4.setClassification("Package X1 Category");
        package4.setDisplayName("Package X1 Display Name");
        package4.setDisplayVersion("Package X1 Display Version");
        package4.setFileCreatedDate(System.currentTimeMillis());
        package4.setFileName("package4.tar.gz");
        package4.setFileSize(1000L);
        package4.setLicenseName("GPL");
        package4.setLicenseVersion("2");
        package4.setLongDescription("Package X1 Long Description");
        package4.setMD5("7bf1adec93fdb899aeca248a38603d58");
        package4.setSHA256("935f051cab5240b979bba87ce58183d50f592a962202ca683f12a7966414fe6c");
        package4.setShortDescription("Package X1 Short Description");

        String package4Metadata = "Package X1 Metadata";
        package4.setMetadata(package4Metadata.getBytes());

        Configuration package4DeploymentConfiguration = new Configuration();
        package4DeploymentConfiguration.put(new PropertySimple("property1", "value1"));
        package4.setDeploymentTimeConfiguration(package4DeploymentConfiguration);

        Configuration package4ExtraProperties = new Configuration();
        package4ExtraProperties.put(new PropertySimple("property2", "value2"));
        package4.setExtraProperties(package4ExtraProperties);

        report.addDeployedPackage(package4);

        // Upgraded package to package version known to the system
        PackageDetailsKey key5 = new PackageDetailsKey("Package4", "2.0.0", this.package4.getPackageType().getName(),
            architecture1.getName());
        ResourcePackageDetails package5 = new ResourcePackageDetails(key5);

        report.addDeployedPackage(package5);

        // In most cases, the same package report will come back from the agent (i.e. no changes). This is a simple way
        // of testing that to ensure we're not messing up and adding multiple installed packages.
        for (int ii = 0; ii < 2; ii++) {
            // Test  --------------------------------------------
            contentManager.mergeDiscoveredPackages(report);

            // Verify  --------------------------------------------
            getTransactionManager().begin();

            try {
                // Simple count checks
                Query installedPackageQuery = em.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID);
                installedPackageQuery.setParameter("resourceId", resource1.getId());
                List<InstalledPackage> installedPackages = installedPackageQuery.getResultList();
                assert installedPackages.size() == 5 : "Incorrect number of packages discovered. Expected 5, Found "
                    + installedPackages.size();

                // PackageX didn't already exist in the system, so ensure it was created and populated correctly from our
                // discovered package
                Query packageXQuery = em.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID);
                packageXQuery.setParameter("name", "PackageX");
                packageXQuery.setParameter("packageTypeId", packageType4.getId());
                Package packageX = (Package) packageXQuery.getSingleResult();

                assert packageX.getName().equals("PackageX") : "Package name not specified on created package";
                assert packageX.getPackageType().equals(packageType4) : "Package type incorrect on created package. Expected: "
                    + packageType4 + ", Found: " + packageX.getPackageType();

                Query packageVersionQuery = em.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_ID);
                packageVersionQuery.setParameter("packageId", packageX.getId());
                List<PackageVersion> packageVersions = packageVersionQuery.getResultList();

                assert packageVersions.size() == 1 : "Incorrect number of versions for package. Expected: 1, Found: "
                    + packageVersions.size();

                PackageVersion packageVersionX = packageVersions.get(0);

                assert packageVersionX.getArchitecture().equals(architecture1) : "Incorrect architecture on package version. Expected: "
                    + architecture1 + ", Found: " + packageVersionX.getArchitecture();
                assert packageVersionX.getDisplayName().equals(package4.getDisplayName()) : "Incorrect display name on package version. Expected: "
                    + package4.getDisplayName() + ", Found: " + packageVersionX.getDisplayName();
                assert packageVersionX.getDisplayVersion().equals(package4.getDisplayVersion()) : "Incorrect display version on package version. Expected: "
                    + package4.getDisplayVersion() + ", Found: " + packageVersionX.getDisplayVersion();
                //            assert packageVersionX.getFileCreatedDate().equals(package4.getFileCreatedDate()) : "Incorrect file created date on package version. Expected: " + package4.getFileCreatedDate() + ", Found: " + packageVersionX.getFileCreatedDate();
                assert packageVersionX.getFileName().equals(package4.getFileName()) : "Incorrect file name on package version. Expected: "
                    + package4.getFileName() + ", Found: " + packageVersionX.getFileName();
                assert packageVersionX.getFileSize().equals(package4.getFileSize()) : "Incorrect file size on package version. Expected: "
                    + package4.getFileSize() + ", Found: " + packageVersionX.getFileSize();
                assert packageVersionX.getLicenseName().equals(package4.getLicenseName()) : "Incorrect license name on package version. Expected: "
                    + package4.getLicenseName() + ", Found: " + packageVersionX.getLicenseName();
                assert packageVersionX.getLicenseVersion().equals(package4.getLicenseVersion()) : "Incorrect license version on package version. Expected: "
                    + package4.getLicenseVersion() + ", Found: " + packageVersionX.getLicenseVersion();
                assert packageVersionX.getLongDescription().equals(package4.getLongDescription()) : "Incorrect long description on package version. Expected: "
                    + package4.getLongDescription() + ", Found: " + packageVersionX.getLongDescription();
                assert packageVersionX.getMD5().equals(package4.getMD5()) : "Incorrect MD5 on package version. Expected: "
                    + package4.getMD5() + ", Found: " + packageVersionX.getMD5();
                assert Arrays.equals(packageVersionX.getMetadata(), package4.getMetadata()) : "Incorrect metadata on package version.";
                assert packageVersionX.getPackageBits() == null : "Non-null package bits specified for package version: "
                    + packageVersionX.getPackageBits();
                assert packageVersionX.getSHA256().equals(package4.getSHA256()) : "Incorrect SHA256 for package version. Expected: "
                    + package4.getSHA256() + ", Found: " + packageVersionX.getSHA256();
                assert packageVersionX.getShortDescription().equals(package4.getShortDescription()) : "Incorrect short description. Expected: "
                    + package4.getShortDescription() + ", Found: " + packageVersionX.getShortDescription();
                assert packageVersionX.getVersion().equals(package4.getVersion()) : "Incorrect version. Expected: "
                    + package4.getVersion() + ", Found: " + packageVersionX.getVersion();

                Configuration extraProperties = packageVersionX.getExtraProperties();
                assert extraProperties != null : "Package version extra properties not persisted.";
                assert extraProperties.getSimple("property2") != null : "Properties inside of extra properties were missing";
                assert extraProperties.getSimple("property2").getStringValue().equals("value2") : "Incorrect value of property2";

                // Load the installed package for the above package to check for the rest of the properties
                Query packageXInstalledPackageQuery = em
                    .createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID);
                packageXInstalledPackageQuery.setParameter("resourceId", resource1.getId());
                packageXInstalledPackageQuery.setParameter("packageVersionId", packageVersionX.getId());
                InstalledPackage packageXInstalledPackage = (InstalledPackage) packageXInstalledPackageQuery
                    .getSingleResult();

                assert packageXInstalledPackage != null;
            } finally {
                getTransactionManager().rollback();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testSuccessfulDeployPackages() throws Exception {
        // Setup  --------------------------------------------
        Subject overlord = subjectManager.getOverlord();

        Set<ResourcePackageDetails> installUs = new HashSet<ResourcePackageDetails>(2);

        // Package 1, Version 2 with configuration values
        PackageVersion packageVersion1 = package1.getVersions().get(0);
        Configuration deploymentConfiguration1 = new Configuration();
        deploymentConfiguration1.put(new PropertySimple("property1", "value1"));

        PackageDetailsKey key1 = new PackageDetailsKey(package1.getName(), packageVersion1.getVersion(), package1
            .getPackageType().getName(), packageVersion1.getArchitecture().getName());
        ResourcePackageDetails packageDetails1 = new ResourcePackageDetails(key1);
        packageDetails1.setDeploymentTimeConfiguration(deploymentConfiguration1);

        installUs.add(packageDetails1);

        // Package 2, Version 1 (intentionally older version) with no configuration values
        PackageVersion packageVersion2 = package2.getVersions().get(0);

        PackageDetailsKey key2 = new PackageDetailsKey(package2.getName(), packageVersion2.getVersion(), package2
            .getPackageType().getName(), packageVersion2.getArchitecture().getName());
        ResourcePackageDetails packageDetails2 = new ResourcePackageDetails(key2);

        installUs.add(packageDetails2);

        // Make sure the mock is configured to return a success
        this.contentAgentService.setResponseReturnStatus(ContentResponseResult.SUCCESS);
        this.contentAgentService.setThrowError(false);
        this.contentAgentService.setReturnIndividualResponses(true);

        String notes = "Test package deployment";

        // Test  --------------------------------------------

        // Perform the deploy while locking the agent service. This allows us to check the state after the request
        // is sent to the agent but before the agent has replied.
        synchronized (responseLock) {
            contentManager.deployPackages(overlord, resource1.getId(), installUs, notes);

            // Check to see if the request and installed package were created and have the right status
            getTransactionManager().begin();

            try {
                // Content request
                Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
                query.setParameter("resourceId", resource1.getId());

                List<?> results = query.getResultList();
                assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                    + results.size();

                ContentServiceRequest request = (ContentServiceRequest) results.get(0);
                assert request.getContentRequestType() == ContentRequestType.DEPLOY : "Request type incorrect. Expected: DEPLOY, Found: "
                    + request.getContentRequestType();
                assert request.getStatus() == ContentRequestStatus.IN_PROGRESS : "Request status incorrect. Expected: IN_PROGRESS, Found: "
                    + request.getStatus();
                assert request.getInstalledPackageHistory().size() == 2 : "Incorrect number of installed packages attached to request. Expected: 2, Found: "
                    + request.getInstalledPackageHistory().size();
                assert request.getNotes() != null : "Null notes found";
                assert request.getNotes().equals(notes) : "Incorrect notes found: " + request.getNotes();

                // Verify a history entry has been added for each package in the request
                Set<InstalledPackageHistory> history = request.getInstalledPackageHistory();

                assert history.size() == 2 : "Incorrect number of history entries on request. Expected: 2, Found: "
                    + history.size();

                for (InstalledPackageHistory historyEntry : history) {
                    assert historyEntry.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect state on history entity. Expected: BEING_INSTALLED, Found: "
                        + historyEntry.getStatus();
                }

                // Ensure the installed package has not been added to the resource yet

                // Package 1, Version 2
                query = em.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID);
                query.setParameter("resourceId", resource1.getId());
                query.setParameter("packageVersionId", packageVersion1.getId());

                results = query.getResultList();
                assert results.size() == 0 : "Incorrect number of installed packages for package 1, version 2. Expected: 0, Found: "
                    + results.size();

                // Check status of audit trail
                query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
                query.setParameter("contentServiceRequestId", request.getId());
                query.setParameter("packageVersionId", packageVersion1.getId());

                results = query.getResultList();

                assert results.size() == 1 : "Incorrect number of audit trail entries. Expected: 1, Found: "
                    + results.size();

                InstalledPackageHistory historyEntity = (InstalledPackageHistory) results.get(0);
                assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on first entity. Expected: BEING_INSTALLED, Found: "
                    + historyEntity.getStatus();

                // Package 2, Version 1
                query = em.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID);
                query.setParameter("resourceId", resource1.getId());
                query.setParameter("packageVersionId", packageVersion2.getId());

                results = query.getResultList();
                assert results.size() == 0 : "Incorrect number of installed packages for package 2, version 1. Expected: 0, Found: "
                    + results.size();

                // Check status of audit trail
                query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
                query.setParameter("contentServiceRequestId", request.getId());
                query.setParameter("packageVersionId", packageVersion2.getId());

                results = query.getResultList();

                assert results.size() == 1 : "Incorrect number of audit trail entries. Expected: 1, Found: "
                    + results.size();

                historyEntity = (InstalledPackageHistory) results.get(0);
                assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on second entity. Expected: BEING_INSTALLED, Found: "
                    + historyEntity.getStatus();

            } finally {
                responseLock.notifyAll();

                getTransactionManager().rollback();
            }
        }

        // Verify  --------------------------------------------

        // Give the agent service a second to make sure it finishes out its call
        Thread.sleep(1000);

        getTransactionManager().begin();

        try {
            // Content request
            Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
            query.setParameter("resourceId", resource1.getId());

            List<?> results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                + results.size();

            ContentServiceRequest request = (ContentServiceRequest) results.get(0);
            assert request.getStatus() == ContentRequestStatus.SUCCESS : "Request status incorrect. Expected: SUCCESS, Found: "
                + request.getStatus();

            // Verify a history entry has been added for the completion of the request per package
            Set<InstalledPackageHistory> history = request.getInstalledPackageHistory();

            assert history.size() == 4 : "Incorrect number of history entries on request. Expected: 4, Found: "
                + history.size();

            // Check for Package 1
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
            query.setParameter("contentServiceRequestId", request.getId());
            query.setParameter("packageVersionId", packageVersion1.getId());

            results = query.getResultList();

            assert results.size() == 2 : "Incorrect number of history entries. Expected: 2, Found: " + results.size();

            InstalledPackageHistory historyEntity = (InstalledPackageHistory) results.get(0);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.INSTALLED : "Incorrect status on first entity. Expected: INSTALLED, Found: "
                + historyEntity.getStatus();

            historyEntity = (InstalledPackageHistory) results.get(1);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on first entity. Expected: BEING_INSTALLED, Found: "
                + historyEntity.getStatus();

            // Check for Package 2
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
            query.setParameter("contentServiceRequestId", request.getId());
            query.setParameter("packageVersionId", packageVersion2.getId());

            results = query.getResultList();

            assert results.size() == 2 : "Incorrect number of history entries. Expected: 2, Found: " + results.size();

            historyEntity = (InstalledPackageHistory) results.get(0);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.INSTALLED : "Incorrect status on first entity. Expected: INSTALLED, Found: "
                + historyEntity.getStatus();

            historyEntity = (InstalledPackageHistory) results.get(1);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on first entity. Expected: BEING_INSTALLED, Found: "
                + historyEntity.getStatus();

            // Add a few tests for the new Criteria Search feature
            PackageVersionCriteria criteria = new PackageVersionCriteria();
            criteria.addFilterResourceId(resource1.getId());
            PageList<PackageVersion> pageList = contentManager.findPackageVersionsByCriteria(overlord, criteria);
            assertNotNull(pageList);
            ArrayList<PackageVersion> pvs = pageList.getValues();
            assertEquals(2, pvs.size());
            PackageVersion pv0 = pvs.get(0);

            criteria.addFilterPackageTypeId(pv0.getGeneralPackage().getPackageType().getId());
            pageList = contentManager.findPackageVersionsByCriteria(overlord, criteria);
            assertNotNull(pageList);
            pvs = pageList.getValues();
            assertEquals(1, pvs.size());
            assertEquals(pv0.getId(), pvs.get(0).getId());

            // there is no repo assignment, any valid ID should eliminate all PVs
            criteria.addFilterRepoId(38465);
            pageList = contentManager.findPackageVersionsByCriteria(overlord, criteria);
            assertNotNull(pageList);
            pvs = pageList.getValues();
            assertEquals(0, pvs.size());
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testDeployWithSteps() throws Exception {
        // Setup  --------------------------------------------
        Subject overlord = subjectManager.getOverlord();

        Set<ResourcePackageDetails> installUs = new HashSet<ResourcePackageDetails>(2);

        // Package 1, Version 2 with configuration values
        PackageVersion packageVersion1 = package1.getVersions().get(0);
        Configuration deploymentConfiguration1 = new Configuration();
        deploymentConfiguration1.put(new PropertySimple("property1", "value1"));

        PackageDetailsKey key1 = new PackageDetailsKey(package1.getName(), packageVersion1.getVersion(), package1
            .getPackageType().getName(), packageVersion1.getArchitecture().getName());
        ResourcePackageDetails packageDetails1 = new ResourcePackageDetails(key1);
        packageDetails1.setDeploymentTimeConfiguration(deploymentConfiguration1);

        installUs.add(packageDetails1);

        // Make sure the mock is configured to return a success
        this.contentAgentService.setResponseReturnStatus(ContentResponseResult.SUCCESS);
        this.contentAgentService.setThrowError(false);
        this.contentAgentService.setReturnIndividualResponses(true);
        this.contentAgentService.setDeployPackageSteps(stepResults);

        String notes = "Test deploy notes";

        // Test  --------------------------------------------

        // Perform the deploy while locking the agent service. This allows us to check the state after the request
        // is sent to the agent but before the agent has replied.
        synchronized (responseLock) {
            contentManager.deployPackages(overlord, resource1.getId(), installUs, notes);

            // Check to see if the request and installed package were created and have the right status
            getTransactionManager().begin();

            try {
                // Content request
                Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
                query.setParameter("resourceId", resource1.getId());

                List<?> results = query.getResultList();
                assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                    + results.size();

                ContentServiceRequest request = (ContentServiceRequest) results.get(0);
                assert request.getContentRequestType() == ContentRequestType.DEPLOY : "Request type incorrect. Expected: DEPLOY, Found: "
                    + request.getContentRequestType();
                assert request.getStatus() == ContentRequestStatus.IN_PROGRESS : "Request status incorrect. Expected: IN_PROGRESS, Found: "
                    + request.getStatus();
                assert request.getInstalledPackageHistory().size() == 1 : "Incorrect number of installed packages attached to request. Expected: 1, Found: "
                    + request.getInstalledPackageHistory().size();
                assert request.getNotes() != null : "Null notes found";
                assert request.getNotes().equals(notes) : "Incorrect notes found: " + request.getNotes();

                // Verify a history entry has been added for each package in the request
                Set<InstalledPackageHistory> history = request.getInstalledPackageHistory();

                assert history.size() == 1 : "Incorrect number of history entries on request. Expected: 2, Found: "
                    + history.size();

                for (InstalledPackageHistory historyEntry : history) {
                    assert historyEntry.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect state on history entity. Expected: BEING_INSTALLED, Found: "
                        + historyEntry.getStatus();
                }

            } finally {
                responseLock.notifyAll();

                getTransactionManager().rollback();
            }
        }

        // Verify  --------------------------------------------

        // Give the agent service a second to make sure it finishes out its call
        Thread.sleep(1000);

        getTransactionManager().begin();

        try {
            // Content request
            Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
            query.setParameter("resourceId", resource1.getId());

            List<?> results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                + results.size();

            ContentServiceRequest request = (ContentServiceRequest) results.get(0);
            assert request.getStatus() == ContentRequestStatus.SUCCESS : "Request status incorrect. Expected: SUCCESS, Found: "
                + request.getStatus();

            // Verify a history entry has been added for the completion of the request per package
            Set<InstalledPackageHistory> history = request.getInstalledPackageHistory();

            assert history.size() == 2 : "Incorrect number of history entries on request. Expected: 2, Found: "
                + history.size();

            // Check for Package 1
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
            query.setParameter("contentServiceRequestId", request.getId());
            query.setParameter("packageVersionId", packageVersion1.getId());

            results = query.getResultList();

            assert results.size() == 2 : "Incorrect number of history entries. Expected: 2, Found: " + results.size();

            InstalledPackageHistory historyEntity = (InstalledPackageHistory) results.get(0);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.INSTALLED : "Incorrect status on first entity. Expected: INSTALLED, Found: "
                + historyEntity.getStatus();

            // The installed entry should contain the steps that were done in the installation
            List<PackageInstallationStep> installationSteps = historyEntity.getInstallationSteps();

            assert installationSteps != null : "Installation steps were null";
            assert installationSteps.size() == 3 : "Incorrect number of installation steps. Expected: 3, Found: "
                + installationSteps.size();

            PackageInstallationStep step = installationSteps.get(0);

            assert step.getOrder() == 0 : "Incorrect order applied for step";
            assert step.getDescription() != null : "Description not saved";
            assert step.getResult() == ContentResponseResult.SUCCESS : "Incorrect status on step. Expected: SUCCESS, Found: "
                + step.getResult();
            assert step.getErrorMessage() == null : "Error message found on successful step";
            assert step.getInstalledPackageHistory() != null : "Relationship to packge history isn't established";

            step = installationSteps.get(1);

            assert step.getOrder() == 1 : "Incorrect order applied for step";
            assert step.getDescription() != null : "Description not saved";
            assert step.getResult() == ContentResponseResult.NOT_PERFORMED : "Incorrect status on step. Expected: NOT_PERFORMED, Found: "
                + step.getResult();
            assert step.getErrorMessage() == null : "Error message found on skipped step";
            assert step.getInstalledPackageHistory() != null : "Relationship to packge history isn't established";

            step = installationSteps.get(2);

            assert step.getOrder() == 2 : "Incorrect order applied for step";
            assert step.getDescription() != null : "Description not saved";
            assert step.getResult() == ContentResponseResult.FAILURE : "Incorrect status on step. Expected: FAILURE, Found: "
                + step.getResult();
            assert step.getErrorMessage() != null : "Null error message found on error step";
            assert step.getInstalledPackageHistory() != null : "Relationship to packge history isn't established";

            historyEntity = (InstalledPackageHistory) results.get(1);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on first entity. Expected: BEING_INSTALLED, Found: "
                + historyEntity.getStatus();

        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testFailedDeployPackages() throws Exception {
        // Setup  --------------------------------------------
        Subject overlord = subjectManager.getOverlord();

        Set<ResourcePackageDetails> installUs = new HashSet<ResourcePackageDetails>(1);

        // Package 1, Version 2 with configuration values
        Configuration deploymentConfiguration1 = new Configuration();
        deploymentConfiguration1.put(new PropertySimple("property1", "value1"));

        PackageVersion packageVersion1 = package1.getVersions().get(1);
        PackageDetailsKey key1 = new PackageDetailsKey(package1.getName(), packageVersion1.getVersion(), package1
            .getPackageType().getName(), packageVersion1.getArchitecture().getName());

        ResourcePackageDetails packageDetails1 = new ResourcePackageDetails(key1);
        packageDetails1.setDeploymentTimeConfiguration(deploymentConfiguration1);

        installUs.add(packageDetails1);

        // Make sure the mock is configured to return a failure
        this.contentAgentService.setResponseReturnStatus(ContentResponseResult.FAILURE);
        this.contentAgentService.setThrowError(false);
        this.contentAgentService.setReturnIndividualResponses(true);

        String notes = "Test deploy notes";

        // Test  --------------------------------------------
        contentManager.deployPackages(overlord, resource1.getId(), installUs, notes);

        // Verify  --------------------------------------------

        // Give the agent service a second to make sure it finishes out its call
        Thread.sleep(1000);

        getTransactionManager().begin();

        try {
            // Content request
            Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
            query.setParameter("resourceId", resource1.getId());

            List<?> results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                + results.size();

            ContentServiceRequest request = (ContentServiceRequest) results.get(0);
            assert request.getStatus() == ContentRequestStatus.FAILURE : "Request status incorrect. Expected: IN_PROGRESS, Found: "
                + request.getStatus();
            assert request.getNotes() != null : "Null notes found";
            assert request.getNotes().equals(notes) : "Incorrect notes found: " + request.getNotes();

            // Check for Package 1
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
            query.setParameter("contentServiceRequestId", request.getId());
            query.setParameter("packageVersionId", packageVersion1.getId());

            results = query.getResultList();

            assert results.size() == 2 : "Incorrect number of history entries. Expected: 2, Found: " + results.size();

            InstalledPackageHistory historyEntity = (InstalledPackageHistory) results.get(0);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.FAILED : "Incorrect status on first entity. Expected: FAILED, Found: "
                + historyEntity.getStatus();

            historyEntity = (InstalledPackageHistory) results.get(1);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on first entity. Expected: BEING_INSTALLED, Found: "
                + historyEntity.getStatus();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testDeployPackageFailToContactAgent() throws Exception {
        // Setup  --------------------------------------------
        Subject overlord = subjectManager.getOverlord();

        Set<ResourcePackageDetails> installUs = new HashSet<ResourcePackageDetails>(1);

        // Package 1, Version 2 with configuration values
        Configuration deploymentConfiguration1 = new Configuration();
        deploymentConfiguration1.put(new PropertySimple("property1", "value1"));

        PackageVersion packageVersion1 = package1.getVersions().get(1);
        PackageDetailsKey key1 = new PackageDetailsKey(package1.getName(), packageVersion1.getVersion(), package1
            .getPackageType().getName(), packageVersion1.getArchitecture().getName());
        ResourcePackageDetails packageDetails1 = new ResourcePackageDetails(key1);
        packageDetails1.setDeploymentTimeConfiguration(deploymentConfiguration1);

        installUs.add(packageDetails1);

        // Make sure the mock is configured to return a failure
        this.contentAgentService.setResponseReturnStatus(ContentResponseResult.FAILURE);
        this.contentAgentService.setThrowError(true);
        this.contentAgentService.setReturnIndividualResponses(false);

        String notes = "Test deploy notes";

        // Test  --------------------------------------------
        try {
            contentManager.deployPackages(overlord, resource1.getId(), installUs, notes);
            assert false : "No exception thrown from deploy call";
        } catch (Exception e) {
            // Expected
        }

        // Verify  --------------------------------------------

        getTransactionManager().begin();

        try {
            // Content request
            Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
            query.setParameter("resourceId", resource1.getId());

            List<?> results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                + results.size();

            ContentServiceRequest request = (ContentServiceRequest) results.get(0);
            assert request.getStatus() == ContentRequestStatus.FAILURE : "Request status incorrect. Expected: IN_PROGRESS, Found: "
                + request.getStatus();
            assert request.getNotes() != null : "Null notes found";
            assert request.getNotes().equals(notes) : "Incorrect notes found: " + request.getNotes();

            // Check for Package 1
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
            query.setParameter("contentServiceRequestId", request.getId());
            query.setParameter("packageVersionId", packageVersion1.getId());

            results = query.getResultList();

            assert results.size() == 2 : "Incorrect number of history entries. Expected: 2, Found: " + results.size();

            InstalledPackageHistory historyEntity = (InstalledPackageHistory) results.get(0);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.FAILED : "Incorrect status on first entity. Expected: FAILED, Found: "
                + historyEntity.getStatus();

            historyEntity = (InstalledPackageHistory) results.get(1);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on first entity. Expected: BEING_INSTALLED, Found: "
                + historyEntity.getStatus();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testDeployPackagesNoIndividualResponses() throws Exception {
        // Setup  --------------------------------------------
        Subject overlord = subjectManager.getOverlord();

        Set<ResourcePackageDetails> installUs = new HashSet<ResourcePackageDetails>(1);

        // Package 1, Version 2 with configuration values
        Configuration deploymentConfiguration1 = new Configuration();
        deploymentConfiguration1.put(new PropertySimple("property1", "value1"));

        PackageVersion packageVersion1 = package1.getVersions().get(1);
        PackageDetailsKey key1 = new PackageDetailsKey(package1.getName(), packageVersion1.getVersion(), package1
            .getPackageType().getName(), packageVersion1.getArchitecture().getName());
        ResourcePackageDetails packageDetails1 = new ResourcePackageDetails(key1);
        packageDetails1.setDeploymentTimeConfiguration(deploymentConfiguration1);

        installUs.add(packageDetails1);

        // Make sure the mock is configured to return a failure
        this.contentAgentService.setResponseReturnStatus(ContentResponseResult.FAILURE);
        this.contentAgentService.setThrowError(false);
        this.contentAgentService.setReturnIndividualResponses(false);

        String notes = "Test deploy notes";

        // Test  --------------------------------------------
        contentManager.deployPackages(overlord, resource1.getId(), installUs, notes);

        // Give the agent service a second to make sure it finishes out its call
        Thread.sleep(1000);

        // Verify  --------------------------------------------

        getTransactionManager().begin();

        try {
            // Content request
            Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
            query.setParameter("resourceId", resource1.getId());

            List<?> results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                + results.size();

            ContentServiceRequest request = (ContentServiceRequest) results.get(0);
            assert request.getStatus() == ContentRequestStatus.FAILURE : "Request status incorrect. Expected: FAILURE, Found: "
                + request.getStatus();
            assert request.getNotes() != null : "Null notes found";
            assert request.getNotes().equals(notes) : "Incorrect notes found: " + request.getNotes();

            // Check for Package 1
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
            query.setParameter("contentServiceRequestId", request.getId());
            query.setParameter("packageVersionId", packageVersion1.getId());

            results = query.getResultList();

            assert results.size() == 2 : "Incorrect number of history entries. Expected: 2, Found: " + results.size();

            InstalledPackageHistory historyEntity = (InstalledPackageHistory) results.get(0);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.FAILED : "Incorrect status on first entity. Expected: FAILED, Found: "
                + historyEntity.getStatus();

            historyEntity = (InstalledPackageHistory) results.get(1);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on first entity. Expected: BEING_INSTALLED, Found: "
                + historyEntity.getStatus();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(enabled = ENABLE_TESTS)
    public void testLoadDependencies() throws Exception {
        // Setup  --------------------------------------------
        Set<PackageDetailsKey> loadUs = new HashSet<PackageDetailsKey>(3);

        // Package 1, Version 2
        PackageVersion packageVersion1 = package1.getVersions().get(1);
        PackageDetailsKey packageKey1 = new PackageDetailsKey(package1.getName(), packageVersion1.getVersion(),
            package1.getPackageType().getName(), packageVersion1.getArchitecture().getName());

        loadUs.add(packageKey1);

        // Package 2, Version 2
        PackageVersion packageVersion2 = package2.getVersions().get(1);
        PackageDetailsKey packageKey2 = new PackageDetailsKey(package2.getName(), packageVersion2.getVersion(),
            package2.getPackageType().getName(), packageVersion2.getArchitecture().getName());

        loadUs.add(packageKey2);

        // Package that does not exist in the server
        PackageDetailsKey packageKey3 = new PackageDetailsKey("foo", "1.0.0", "fooType", "fooArch");

        loadUs.add(packageKey3);

        // Need to create a content service request off of which to hang the dependencies as new installed package requests
        getTransactionManager().begin();

        ContentServiceRequest request = null;
        try {
            resource1 = em.find(Resource.class, resource1.getId());
            Subject overlord = subjectManager.getOverlord();
            PackageVersion packageVersion = em.find(PackageVersion.class, package3.getVersions().get(0).getId());

            request = new ContentServiceRequest(resource1, overlord.getName(), ContentRequestType.DEPLOY);

            InstalledPackageHistory originalRequestedPackage = new InstalledPackageHistory();
            originalRequestedPackage.setContentServiceRequest(request);
            originalRequestedPackage.setPackageVersion(packageVersion);
            originalRequestedPackage.setStatus(InstalledPackageHistoryStatus.BEING_INSTALLED);
            originalRequestedPackage.setTimestamp(System.currentTimeMillis());
            originalRequestedPackage.setResource(resource1);

            request.addInstalledPackageHistory(originalRequestedPackage);

            em.persist(request);

            getTransactionManager().commit();
        } catch (Throwable t) {
            getTransactionManager().rollback();

            assert false : "Error during setup: " + t;
        }

        // Test  --------------------------------------------
        contentManager.loadDependencies(request.getId(), loadUs);

        // Verify  --------------------------------------------

        // Verify installed packages were created for the two dependency packages
        getTransactionManager().begin();

        try {
            // Overall package count on the request
            Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID);
            query.setParameter("id", request.getId());

            List<ContentServiceRequest> resultList = query.getResultList();

            assert resultList.size() == 1 : "Incorrect number of requests loaded. Expected: 1, Found: "
                + resultList.size();

            request = resultList.get(0);

            assert request.getInstalledPackageHistory().size() == 3 : "Incorrect number of being installed packages on request. Expected: 3, Found: "
                + resultList.size();

            // Quick check for the status of each
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID);
            query.setParameter("contentServiceRequestId", request.getId());

            List<InstalledPackageHistory> historyEntries = query.getResultList();

            for (InstalledPackageHistory historyEntry : historyEntries) {
                assert historyEntry.getContentServiceRequest().equals(request) : "ContentServiceRequest relationship not set up correctly for package history";
                assert historyEntry.getStatus() == InstalledPackageHistoryStatus.BEING_INSTALLED : "Incorrect status on installed package history. Expected: BEING_INSTALLED, Found: "
                    + historyEntry.getStatus();
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testSuccessfullyDeletePackages() throws Exception {
        // Setup  --------------------------------------------
        Subject overlord = subjectManager.getOverlord();

        // Delete installed package for package4
        int[] deleteUs = new int[] { installedPackage1.getId() };

        // Leave package5 installed

        // Make sure the mock is configured to return a success
        this.contentAgentService.setResponseReturnStatus(ContentResponseResult.SUCCESS);
        this.contentAgentService.setThrowError(false);
        this.contentAgentService.setReturnIndividualResponses(true);

        String notes = "Test delete notes";

        // Test  --------------------------------------------

        // Perform the deploy while locking the agent service. This allows us to check the state after the request
        // is sent to the agent but before the agent has replied.
        synchronized (responseLock) {
            contentManager.deletePackages(overlord, resource1.getId(), deleteUs, notes);

            getTransactionManager().begin();

            try {
                // Content request
                Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
                query.setParameter("resourceId", resource1.getId());

                List<?> results = query.getResultList();
                assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                    + results.size();

                ContentServiceRequest request = (ContentServiceRequest) results.get(0);
                assert request.getStatus() == ContentRequestStatus.IN_PROGRESS : "Request status incorrect. Expected: IN_PROGRESS, Found: "
                    + request.getStatus();
                assert request.getNotes() != null : "Null notes found";
                assert request.getNotes().equals(notes) : "Incorrect notes found: " + request.getNotes();

                // Verify a history entry has been added for each package in the request
                Set<InstalledPackageHistory> history = request.getInstalledPackageHistory();

                assert history.size() == 1 : "Incorrect number of history entries on request. Expected: 1, Found: "
                    + history.size();

                for (InstalledPackageHistory historyEntry : history) {
                    assert historyEntry.getStatus() == InstalledPackageHistoryStatus.BEING_DELETED : "Incorrect state on history entity. Expected: BEING_DELETED, Found: "
                        + historyEntry.getStatus();
                }
            } finally {
                getTransactionManager().rollback();
            }
        }

        // Verify  --------------------------------------------

        // Give the agent service a second to make sure it finishes out its call
        Thread.sleep(1000);

        getTransactionManager().begin();

        try {
            // Content request
            Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
            query.setParameter("resourceId", resource1.getId());

            List<?> results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                + results.size();

            ContentServiceRequest request = (ContentServiceRequest) results.get(0);
            assert request.getStatus() == ContentRequestStatus.SUCCESS : "Request status incorrect. Expected: SUCCESS, Found: "
                + request.getStatus();

            // Verify history entries
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
            query.setParameter("contentServiceRequestId", request.getId());
            query.setParameter("packageVersionId", installedPackage1.getPackageVersion().getId());

            results = query.getResultList();

            assert results.size() == 2 : "Incorrect number of history entries. Expected: 2, Found: " + results.size();

            InstalledPackageHistory historyEntity = (InstalledPackageHistory) results.get(0);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.DELETED : "Incorrect status on first entity. Expected: DELETED, Found: "
                + historyEntity.getStatus();

            historyEntity = (InstalledPackageHistory) results.get(1);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_DELETED : "Incorrect status on first entity. Expected: BEING_DELETED, Found: "
                + historyEntity.getStatus();

            // Package 4, Version 2
            query = em.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID);
            query.setParameter("resourceId", resource1.getId());
            query.setParameter("packageVersionId", installedPackage1.getPackageVersion().getId());

            results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of installed packages for package 1, version 2. Expected: 0, Found: "
                + results.size();

            // Package 2, Version 1
            query = em.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID);
            query.setParameter("resourceId", resource1.getId());
            query.setParameter("packageVersionId", package5.getVersions().get(0).getId());

            results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of installed packages for package 2, version 1. Expected: 1, Found: "
                + results.size();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testDeletePackagesNoIndividualResponses() throws Exception {
        // Setup  --------------------------------------------
        Subject overlord = subjectManager.getOverlord();

        // Delete installed package for package4
        int[] deleteUs = new int[] { installedPackage1.getId() };

        // Leave package5 installed

        // Make sure the mock is configured to return a success
        this.contentAgentService.setResponseReturnStatus(ContentResponseResult.FAILURE);
        this.contentAgentService.setThrowError(false);
        this.contentAgentService.setReturnIndividualResponses(false);

        String notes = "Test delete notes";

        // Test  --------------------------------------------

        // Perform the deploy while locking the agent service. This allows us to check the state after the request
        // is sent to the agent but before the agent has replied.
        synchronized (responseLock) {
            contentManager.deletePackages(overlord, resource1.getId(), deleteUs, notes);

            getTransactionManager().begin();

            try {
                // Content request
                Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
                query.setParameter("resourceId", resource1.getId());

                List<?> results = query.getResultList();
                assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                    + results.size();

                ContentServiceRequest request = (ContentServiceRequest) results.get(0);
                assert request.getStatus() == ContentRequestStatus.IN_PROGRESS : "Request status incorrect. Expected: IN_PROGRESS, Found: "
                    + request.getStatus();
                assert request.getNotes() != null : "Null notes found";
                assert request.getNotes().equals(notes) : "Incorrect notes found: " + request.getNotes();

                // Verify a history entry has been added for each package in the request
                Set<InstalledPackageHistory> history = request.getInstalledPackageHistory();

                assert history.size() == 1 : "Incorrect number of history entries on request. Expected: 1, Found: "
                    + history.size();

                for (InstalledPackageHistory historyEntry : history) {
                    assert historyEntry.getStatus() == InstalledPackageHistoryStatus.BEING_DELETED : "Incorrect state on history entity. Expected: BEING_DELETED, Found: "
                        + historyEntry.getStatus();
                }
            } finally {
                getTransactionManager().rollback();
            }
        }

        // Verify  --------------------------------------------

        // Give the agent service a second to make sure it finishes out its call
        Thread.sleep(1000);

        getTransactionManager().begin();

        try {
            // Content request
            Query query = em.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_RESOURCE);
            query.setParameter("resourceId", resource1.getId());

            List<?> results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of content service requests. Expected: 1, Found: "
                + results.size();

            ContentServiceRequest request = (ContentServiceRequest) results.get(0);
            assert request.getStatus() == ContentRequestStatus.FAILURE : "Request status incorrect. Expected: FAILURE, Found: "
                + request.getStatus();

            // Verify history entries
            query = em.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID);
            query.setParameter("contentServiceRequestId", request.getId());
            query.setParameter("packageVersionId", installedPackage1.getPackageVersion().getId());

            results = query.getResultList();

            assert results.size() == 2 : "Incorrect number of history entries. Expected: 2, Found: " + results.size();

            InstalledPackageHistory historyEntity = (InstalledPackageHistory) results.get(0);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.FAILED : "Incorrect status on first entity. Expected: FAILED, Found: "
                + historyEntity.getStatus();

            historyEntity = (InstalledPackageHistory) results.get(1);
            assert historyEntity.getStatus() == InstalledPackageHistoryStatus.BEING_DELETED : "Incorrect status on first entity. Expected: BEING_DELETED, Found: "
                + historyEntity.getStatus();

            // Package 4, Version 2
            query = em.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID);
            query.setParameter("resourceId", resource1.getId());
            query.setParameter("packageVersionId", installedPackage1.getPackageVersion().getId());

            results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of installed packages for package 1, version 2. Expected: 0, Found: "
                + results.size();

            // Package 2, Version 1
            query = em.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID);
            query.setParameter("resourceId", resource1.getId());
            query.setParameter("packageVersionId", package5.getVersions().get(0).getId());

            results = query.getResultList();
            assert results.size() == 1 : "Incorrect number of installed packages for package 2, version 1. Expected: 1, Found: "
                + results.size();
        } finally {
            getTransactionManager().rollback();
        }
    }

    // Private  --------------------------------------------

    private void setupTestEnvironment() throws Exception {
        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.contentService = contentAgentService;

        getTransactionManager().begin();

        try {
            architecture1 = em.find(Architecture.class, 1);
            architecture2 = em.find(Architecture.class, 2);

            resourceType1 = new ResourceType("resourcetype-" + System.currentTimeMillis(), "TestPlugin",
                ResourceCategory.PLATFORM, null);
            em.persist(resourceType1);

            // Add package types to resource type
            packageType1 = new PackageType();
            packageType1.setName("package1-" + System.currentTimeMillis());
            packageType1.setDescription("");
            packageType1.setCategory(PackageCategory.DEPLOYABLE);
            packageType1.setDisplayName("TestResourcePackage");
            packageType1.setCreationData(true);
            packageType1.setResourceType(resourceType1);
            em.persist(packageType1);

            packageType2 = new PackageType();
            packageType2.setName("package2-" + System.currentTimeMillis());
            packageType2.setDescription("");
            packageType2.setCategory(PackageCategory.DEPLOYABLE);
            packageType2.setDisplayName("TestResourcePackage2");
            packageType2.setCreationData(true);
            packageType2.setResourceType(resourceType1);
            em.persist(packageType2);

            packageType3 = new PackageType();
            packageType3.setName("package3-" + System.currentTimeMillis());
            packageType3.setDescription("");
            packageType3.setCategory(PackageCategory.DEPLOYABLE);
            packageType3.setDisplayName("TestResourcePackage3");
            packageType3.setCreationData(true);
            packageType3.setResourceType(resourceType1);
            em.persist(packageType3);

            packageType4 = new PackageType();
            packageType4.setName("package4-" + System.currentTimeMillis());
            packageType4.setDescription("");
            packageType4.setCategory(PackageCategory.DEPLOYABLE);
            packageType4.setDisplayName("TestResourcePackage4");
            packageType4.setCreationData(true);
            packageType4.setResourceType(resourceType1);
            em.persist(packageType4);

            resourceType1.addPackageType(packageType1);
            resourceType1.addPackageType(packageType2);
            resourceType1.addPackageType(packageType3);

            // Package 1 - Contains 2 versions
            package1 = new Package("Package1", packageType1);

            package1.addVersion(new PackageVersion(package1, "1.0.0", architecture1));
            package1.addVersion(new PackageVersion(package1, "2.0.0", architecture1));

            em.persist(package1);

            // Package 2 - Contains 2 versions
            package2 = new Package("Package2", packageType2);

            package2.addVersion(new PackageVersion(package2, "1.0.0", architecture1));
            package2.addVersion(new PackageVersion(package2, "2.0.0", architecture1));

            em.persist(package2);

            // Package 3 - Contains 1 version
            package3 = new Package("Package3", packageType3);

            package3.addVersion(new PackageVersion(package3, "1.0.0", architecture1));

            em.persist(package3);

            // Package 4 - Contains 2 versions, the first is installed
            package4 = new Package("Package4", packageType4);

            PackageVersion package4Installed = new PackageVersion(package4, "1.0.0", architecture1);
            package4.addVersion(package4Installed);
            package4.addVersion(new PackageVersion(package4, "2.0.0", architecture1));

            em.persist(package4);

            // Package 5 - Contains 1 version, it is installed
            package5 = new Package("Package5", packageType3);

            PackageVersion package5Installed = new PackageVersion(package5, "1.0.0", architecture1);
            package5.addVersion(package5Installed);

            em.persist(package5);

            // Create resource against which we'll merge the discovery report
            resource1 = new Resource("parent" + System.currentTimeMillis(), "name", resourceType1);
            resource1.setUuid("" + new Random().nextInt());
            resource1.setInventoryStatus(InventoryStatus.COMMITTED);
            em.persist(resource1);

            // Install packages on the resource
            installedPackage1 = new InstalledPackage();
            installedPackage1.setResource(resource1);
            installedPackage1.setPackageVersion(package4Installed);
            resource1.addInstalledPackage(installedPackage1);

            installedPackage2 = new InstalledPackage();
            installedPackage2.setResource(resource1);
            installedPackage2.setPackageVersion(package5Installed);
            resource1.addInstalledPackage(installedPackage2);

            installedPackage1.setResource(resource1);
            installedPackage2.setResource(resource1);

            getTransactionManager().commit();
        } catch (Exception e) {
            e.printStackTrace();
            getTransactionManager().rollback();
            throw e;
        }
    }

    private void tearDownTestEnvironment() throws Exception {
        getTransactionManager().begin();

        try {
            try {
                resource1 = em.find(Resource.class, resource1.getId());
                for (InstalledPackage ip : resource1.getInstalledPackages()) {
                    em.remove(ip);
                }

                for (InstalledPackageHistory history : resource1.getInstalledPackageHistory()) {
                    em.remove(history);
                }

                for (ContentServiceRequest request : resource1.getContentServiceRequests()) {
                    em.remove(request);
                }

                package1 = em.find(Package.class, package1.getId());
                em.remove(package1);

                package2 = em.find(Package.class, package2.getId());
                em.remove(package2);

                package3 = em.find(Package.class, package3.getId());
                em.remove(package3);

                package4 = em.find(Package.class, package4.getId());
                em.remove(package4);

                package5 = em.find(Package.class, package5.getId());
                em.remove(package5);

                packageType1 = em.find(PackageType.class, packageType1.getId());
                em.remove(packageType1);

                packageType2 = em.find(PackageType.class, packageType2.getId());
                em.remove(packageType2);

                packageType3 = em.find(PackageType.class, packageType3.getId());
                em.remove(packageType3);

                packageType4 = em.find(PackageType.class, packageType4.getId());
                em.remove(packageType4);

                ResourceTreeHelper.deleteResource(em, resource1);

                resourceType1 = em.find(ResourceType.class, resourceType1.getId());
                em.remove(resourceType1);

                getTransactionManager().commit();
            } catch (Exception e) {
                e.printStackTrace();
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            unprepareForTestAgents();
        }
    }

    private void populateResponseSteps() {
        stepResults = new ArrayList<DeployPackageStep>(3);

        DeployPackageStep step1 = new DeployPackageStep("Step1", "First step");
        step1.setStepResult(ContentResponseResult.SUCCESS);

        DeployPackageStep step2 = new DeployPackageStep("Step2", "Second step");
        step2.setStepResult(ContentResponseResult.NOT_PERFORMED);

        DeployPackageStep step3 = new DeployPackageStep("Step3", "Third step");
        step3.setStepResult(ContentResponseResult.FAILURE);
        step3.setStepErrorMessage("Error executing the third step");

        stepResults.add(step1);
        stepResults.add(step2);
        stepResults.add(step3);
    }

    // Inner Classes  --------------------------------------------

    private class MockContentAgentService implements ContentAgentService {
        // Attributes  --------------------------------------------

        private ContentResponseResult responseReturnStatus;

        private boolean throwError;
        private boolean returnIndividualResponses = true;

        private List<DeployPackageStep> deployPackageSteps;

        // Public  --------------------------------------------

        /**
         * The value set in this method will be the status of the response object sent back to the EJB.
         *
         * @param responseReturnStatus status to use for all response objects
         */
        public void setResponseReturnStatus(ContentResponseResult responseReturnStatus) {
            this.responseReturnStatus = responseReturnStatus;
        }

        /**
         * If this is set to <code>true</code>, calls into this mock will trigger a <code>RuntimeException</code>.
         *
         * @param throwError flag indicating if an error should be thrown
         */
        public void setThrowError(boolean throwError) {
            this.throwError = throwError;
        }

        /**
         * Calls to deploy will include this list for all packages in the deployment request.
         *
         * @param deployPackageSteps indicates if deployed package responses should have steps attached to them
         */
        public void setDeployPackageSteps(List<DeployPackageStep> deployPackageSteps) {
            this.deployPackageSteps = deployPackageSteps;
        }

        /**
         * If this is set to <code>true</code> the response will include an entry for each package requested.
         *
         * @param returnIndividualResponses flag indicating if individual package results should be returned
         */
        public void setReturnIndividualResponses(boolean returnIndividualResponses) {
            this.returnIndividualResponses = returnIndividualResponses;
        }

        // ContentAgentService Implementation  --------------------------------------------

        public Set<ResourcePackageDetails> getLastDiscoveredResourcePackages(int resourceId) {
            return null;
        }

        public ContentDiscoveryReport executeResourcePackageDiscoveryImmediately(int resourceId, String packageTypeName)
            throws PluginContainerException {
            return null;
        }

        public List<DeployPackageStep> translateInstallationSteps(int resourceId, ResourcePackageDetails packageDetails) {
            return null;
        }

        public void deployPackages(final DeployPackagesRequest request) {
            if (throwError) {
                throw new RuntimeException("Mock exception - This is expected");
            }

            Runnable responseRunner = new Runnable() {
                public void run() {
                    synchronized (responseLock) {
                        DeployPackagesResponse response = new DeployPackagesResponse(responseReturnStatus);

                        // This setting of the ID will be done by the plugin container; we're not relying on the plugin
                        // to do it. Since I'm skipping the PC entirely for this test, I'll do it here.
                        response.setRequestId(request.getRequestId());

                        if (returnIndividualResponses) {
                            for (ResourcePackageDetails packageDetails : request.getPackages()) {
                                DeployIndividualPackageResponse individualResponse = new DeployIndividualPackageResponse(
                                    packageDetails.getKey(), responseReturnStatus);

                                individualResponse.setDeploymentSteps(deployPackageSteps);

                                response.addPackageResponse(individualResponse);
                            }
                        }

                        ContentManagerBeanTest.this.contentManager.completeDeployPackageRequest(response);
                    }
                }
            };

            Thread runner = new Thread(responseRunner);
            runner.start();
        }

        public void deletePackages(final DeletePackagesRequest request) {
            if (throwError) {
                throw new RuntimeException("Mock exception - This is expected");
            }

            Runnable responseRunner = new Runnable() {
                public void run() {
                    synchronized (responseLock) {
                        RemovePackagesResponse response = new RemovePackagesResponse(responseReturnStatus);

                        // This setting of the ID will be done by the plugin container; we're not relying on the plugin
                        // to do it. Since I'm skipping the PC entirely for this test, I'll do it here.
                        response.setRequestId(request.getRequestId());

                        if (returnIndividualResponses) {
                            for (ResourcePackageDetails packageDetails : request.getPackages()) {
                                RemoveIndividualPackageResponse individualResponse = new RemoveIndividualPackageResponse(
                                    packageDetails.getKey(), responseReturnStatus);
                                response.addPackageResponse(individualResponse);
                            }
                        }

                        ContentManagerBeanTest.this.contentManager.completeDeletePackageRequest(response);
                    }
                }
            };

            Thread runner = new Thread(responseRunner);
            runner.start();
        }

        public void retrievePackageBits(RetrievePackageBitsRequest request) {
        }
    }
}