/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.enterprise.server.plugin.pc.content;

import java.util.ArrayList;
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
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoDistribution;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.DistributionManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ContentProviderManagerSyncRepoTest extends AbstractEJB3Test {

    private static final boolean TESTS_ENABLED = true;

    private TestContentServerPluginService pluginService;

    private TestContentProvider contentProvider1 = new TestContentProvider();
    private TestContentProvider contentProvider2 = new TestContentProvider();

    // The following variables need to be cleaned up at the end of the test

    private ContentSourceType contentSourceType;
    private PackageType packageType;
    private ResourceType resourceType;

    private List<ContentSource> repoContentSources = new ArrayList<ContentSource>();
    private Repo repoToSync;


    @BeforeMethod
    public void setupBeforeMethod() throws Exception {

        // Plugin service setup
        prepareScheduler();
        pluginService = new TestContentServerPluginService(this);

        TransactionManager tx = getTransactionManager();
        try {
            tx.begin();
            EntityManager entityManager = getEntityManager();

            ContentSourceManagerLocal contentManager = LookupUtil.getContentSourceManager();
            RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            Subject overlord = subjectManager.getOverlord();

            // Create a sample content source type that will be used in this test
            contentSourceType = new ContentSourceType("testType");
            entityManager.persist(contentSourceType);
            entityManager.flush();

            // A repo sync will query all providers for that repo, so add multiple providers
            ContentSource cs1 = new ContentSource("contentSource1", contentSourceType);
            cs1.setDownloadMode(DownloadMode.DATABASE);
            ContentSource cs2 = new ContentSource("contentSource2", contentSourceType);
            cs2.setDownloadMode(DownloadMode.DATABASE);

            cs1 = contentManager.simpleCreateContentSource(overlord, cs1);
            cs2 = contentManager.simpleCreateContentSource(overlord, cs2);

            pluginService.associateContentProvider(cs1, contentProvider1);
            pluginService.associateContentProvider(cs2, contentProvider2);

            repoContentSources.add(cs1);
            repoContentSources.add(cs2);

            // Create the package type packages will be created against
            resourceType = new ResourceType(TestContentProvider.RESOURCE_TYPE_NAME,
                TestContentProvider.RESOURCE_TYPE_PLUGIN_NAME, ResourceCategory.PLATFORM, null);
            entityManager.persist(resourceType);

            packageType = new PackageType(TestContentProvider.PACKAGE_TYPE_NAME, resourceType);
            entityManager.persist(packageType);

            // Create the repo to be syncced
            Repo repo = new Repo(TestContentProvider.REPO_WITH_PACKAGES);
            repo.addContentSource(cs1);
//        repo.addContentSource(cs2);  Disabled until we implement a second test content source to return new stuff
            repoToSync = repoManager.createRepo(overlord, repo);

            tx.commit();
        } catch (Throwable t) {
            tx.rollback();
            // rethrow because if we swallow the exception then tests proceed to execute when they probably should not
            throw new RuntimeException(t);
        }
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {

        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        Query query;

        ContentSourceManagerLocal contentSourceManagerLocal = LookupUtil.getContentSourceManager();
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        DistributionManagerLocal distroManager = LookupUtil.getDistributionManagerLocal();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord = subjectManager.getOverlord();

        // Delete all distributions
        distroManager.deleteDistributionMappingsForRepo(overlord, repoToSync.getId());

        for (String distroLabel : TestContentProvider.DISTRIBUTIONS.keySet()) {
            Distribution distro = distroManager.getDistributionByLabel(distroLabel);
            if (distro != null) {
                // Delete the files
                query = entityManager.createNamedQuery(DistributionFile.DELETE_BY_DIST_ID);
                query.setParameter("distId", distro.getId());
                query.executeUpdate();

                // Delete the actual distro
                distroManager.deleteDistributionByDistId(overlord, distro.getId());
            }
        }

        // Delete all package version <-> content source mappings
        for (ContentSource source : repoContentSources) {
            contentSourceManagerLocal.deleteContentSource(overlord, source.getId());
        }
        repoContentSources.clear();

        // Delete the repo
        repoManager.deleteRepo(overlord, repoToSync.getId());

        // Delete any packages that were created
        for (ContentProviderPackageDetails details : TestContentProvider.PACKAGES.values()) {
            String packageName = details.getContentProviderPackageDetailsKey().getName();

            query = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID);
            query.setParameter("name", packageName);
            query.setParameter("packageTypeId", packageType.getId());

            Package p = (Package) query.getSingleResult();
            entityManager.remove(p);
        }

        // Delete the package type
        packageType = entityManager.find(PackageType.class, packageType.getId());
        entityManager.remove(packageType);

        resourceType = entityManager.find(ResourceType.class, resourceType.getId());
        entityManager.remove(resourceType);

        // Delete the content source type
        contentSourceType = entityManager.find(ContentSourceType.class, contentSourceType.getId());
        entityManager.remove(contentSourceType);

        tx.commit();

        // Cleanup providers between tests
        contentProvider1.reset();
        contentProvider2.reset();

        // Plugin service teardown
        unprepareServerPluginService();
        unprepareScheduler();
    }

    @Test(enabled = TESTS_ENABLED)
    @SuppressWarnings("unchecked")
    public void synchronizeRepo() throws Exception {

        // Test
        // --------------------------------------------
        boolean completed = pluginService.getContentProviderManager().synchronizeRepo(repoToSync.getId());
        assert completed;

        // Verify
        // --------------------------------------------

        // Make sure the proper calls were made into the provider
        assert contentProvider1.getLogSynchronizePackagesRepos().size() == 1 :
            "Expected: 1, Found: " + contentProvider1.getLogSynchronizePackagesRepos().size();

            // Need to add in distro packages being syncced
        assert contentProvider1.getLogGetInputStreamLocations().size() == TestContentProvider.PACKAGE_COUNT_FOR_BITS :
            "Expected: " + TestContentProvider.PACKAGE_COUNT_FOR_BITS +
            ", Found: " + contentProvider1.getLogGetInputStreamLocations().size();

        // Make sure all of the packages were added
        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        Query query = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_REPO_ID);
        query.setParameter("repoId", repoToSync.getId());
        List<PackageVersion> repoPackages = query.getResultList();

        assert repoPackages.size() == TestContentProvider.PACKAGES.size() :
            "Expected: " + TestContentProvider.PACKAGES.size() + ", Found: " + repoPackages.size();

        // Make sure all of the distributions were added
        query = entityManager.createNamedQuery(RepoDistribution.QUERY_FIND_BY_REPO_ID);
        query.setParameter("repoId", repoToSync.getId());
        List<RepoDistribution> repoDistributions = query.getResultList();

        assert repoDistributions.size() == TestContentProvider.DISTRIBUTIONS.size() :
            "Expected: " + TestContentProvider.DISTRIBUTIONS.size() + ", Found: " + repoDistributions.size();

        // Make sure each distribution has the correct files associated
        int distro1FileCount = countDistroFiles(entityManager, TestContentProvider.DISTRIBUTION_1_LABEL);
        assert distro1FileCount == 2 : "Expected: 2, Found: " + distro1FileCount;

        int distro2FileCount = countDistroFiles(entityManager, TestContentProvider.DISTRIBUTION_2_LABEL);
        assert distro2FileCount == 1 : "Expected: 1, Found: " + distro1FileCount;

        tx.rollback();
    }

    private int countDistroFiles(EntityManager entityManager, String label) {
        Query query = entityManager.createNamedQuery(Distribution.QUERY_FIND_BY_DIST_LABEL);
        query.setParameter("label", label);
        Distribution distro = (Distribution) query.getSingleResult();

        query = entityManager.createNamedQuery(DistributionFile.SELECT_BY_DIST_ID);
        query.setParameter("distId", distro.getId());
        List distroFiles = query.getResultList();

        if (distroFiles == null) {
            return 0;
        }
        else {
            return distroFiles.size();
        }
    }
}
