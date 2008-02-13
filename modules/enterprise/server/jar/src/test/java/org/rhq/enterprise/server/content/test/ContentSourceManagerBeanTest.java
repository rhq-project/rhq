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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceSyncStatus;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.MD5Generator;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestContentSourcePluginService;
import org.rhq.enterprise.server.util.LookupUtil;

@Test
public class ContentSourceManagerBeanTest extends AbstractEJB3Test {
    private static final boolean TESTS_ENABLED = true;

    private ContentManagerLocal contentManager;
    private ContentSourceManagerLocal contentSourceManager;
    private ContentSourceMetadataManagerLocal contentSourceMetadataManager;
    private ChannelManagerLocal channelManager;
    private Subject overlord;
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

    private ResourceType resourceType1;
    private Resource resource1;
    private TestContentSourcePluginService pluginService;

    @BeforeClass
    public void setupBeforeClass() throws Exception {
        contentManager = LookupUtil.getContentManager();
        contentSourceManager = LookupUtil.getContentSourceManager();
        contentSourceMetadataManager = LookupUtil.getContentSourceMetadataManager();
        channelManager = LookupUtil.getChannelManagerLocal();
    }

    @AfterClass
    public void tearDownAfterClass() throws Exception {
    }

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        setupTestEnvironment();
        overlord = LookupUtil.getSubjectManager().getOverlord();
        prepareScheduler();
        pluginService = prepareContentSourcePluginService();
        pluginService.startPluginContainer();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        tearDownTestEnvironment();
        unprepareContentSourcePluginService();
        unprepareScheduler();
    }

    @Test(enabled = TESTS_ENABLED)
    public void testGetSyncResultsList() throws Throwable {
        getTransactionManager().begin();

        try {
            // create a content source type and a content source
            ContentSourceType type = new ContentSourceType("testGetSyncResultsListCST");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            ContentSource contentSource = new ContentSource("testGetSyncResultsListCS", type);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);

            // make sure we have nothing yet
            PageList<ContentSourceSyncResults> list;
            list = contentSourceManager.getContentSourceSyncResults(overlord, contentSource.getId(), PageControl
                .getUnlimitedInstance());
            assert list.size() == 0 : "-->" + list;

            // create our first INPROGRESS result
            ContentSourceSyncResults results = new ContentSourceSyncResults(contentSource);
            results = contentSourceManager.persistContentSourceSyncResults(results);
            assert results != null;

            // make sure it persisted
            list = contentSourceManager.getContentSourceSyncResults(overlord, contentSource.getId(), PageControl
                .getUnlimitedInstance());
            assert list.size() == 1 : "-->" + list;
            assert list.get(0).getId() == results.getId() : "-->" + list;

            // try to create another INPROGRESS, this is not allowed so null must be returned by persist
            ContentSourceSyncResults another = new ContentSourceSyncResults(contentSource);
            another = contentSourceManager.persistContentSourceSyncResults(another);
            assert another == null : "Not allowed to have two INPROGRESS results persisted";

            // verify that we really did not persist a second one
            list = contentSourceManager.getContentSourceSyncResults(overlord, contentSource.getId(), PageControl
                .getUnlimitedInstance());
            assert list.size() == 1 : "-->" + list;
            assert list.get(0).getId() == results.getId() : "-->" + list;

            // try to create another but this one is a FAILURE, this is allowed
            another = new ContentSourceSyncResults(contentSource);
            another.setStatus(ContentSourceSyncStatus.FAILURE);
            another.setEndTime(System.currentTimeMillis());
            another = contentSourceManager.persistContentSourceSyncResults(another);
            assert another != null : "Allowed to have two results persisted if only one is INPROGRESS";

            // verify that we really did persist a second one
            list = contentSourceManager.getContentSourceSyncResults(overlord, contentSource.getId(), PageControl
                .getUnlimitedInstance());
            assert list.size() == 2 : "-->" + list;
            assert list.get(0).getId() == another.getId() : "-->" + list;
            assert list.get(1).getId() == results.getId() : "-->" + list;

            // delete the content source and make sure we cascade delete the results
            contentSourceManager.deleteContentSource(overlord, contentSource.getId());

            list = contentSourceManager.getContentSourceSyncResults(overlord, contentSource.getId(), PageControl
                .getUnlimitedInstance());
            assert list.size() == 0 : "-->" + list;

            contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testMergeSyncReport() throws Exception {
        PageControl pc;
        int channelId = 0;
        int contentSourceId = 0;

        try {
            // create content source type
            ContentSourceType type = new ContentSourceType("testMergeSyncReportCST");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            type = contentSourceManager.getContentSourceType(type.getName());
            assert type != null;
            assert type.getId() > 0;

            // create content source
            ContentSource contentSource = new ContentSource("testMergeSyncReportCS", type);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);
            assert contentSource != null;
            contentSourceId = contentSource.getId();
            assert contentSourceId > 0;

            // this report will add a mapping to PV->CS
            // we didn't set up any mappings like that yet - this will be the first one
            PackageSyncReport report = new PackageSyncReport();
            ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey("testCreateContentSourceFoo",
                "testCreateContentSourceVer", packageType1.getName(), architecture1.getName(), resourceType1.getName(),
                resourceType1.getPlugin());
            ContentSourcePackageDetails details = new ContentSourcePackageDetails(key);
            details.setLocation("dummy-location");
            details.setMetadata("dummy-metadata".getBytes());
            details.addResourceVersion("1.0.0");
            details.addResourceVersion("2.0.0");
            report.addNewPackage(details);
            Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous;
            previous = new HashMap<ContentSourcePackageDetailsKey, PackageVersionContentSource>();

            // merge the report!
            ContentSourceSyncResults results = new ContentSourceSyncResults(contentSource);
            results = contentSourceManager.persistContentSourceSyncResults(results);
            assert results != null;

            results = contentSourceManager.mergeContentSourceSyncReport(contentSource, report, previous, results);
            assert results != null;

            // Verify the product version was created
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            try {
                resourceType1 = em.find(ResourceType.class, resourceType1.getId());

                Query productVersionQuery = em.createNamedQuery(ProductVersion.QUERY_FIND_BY_RESOURCE_TYPE_AND_VERSION);
                productVersionQuery.setParameter("resourceType", resourceType1);
                productVersionQuery.setParameter("version", "1.0.0");

                List productVersionList = productVersionQuery.getResultList();
                assert productVersionList.size() > 0 : "Could not find product version for 1.0.0";

                productVersionQuery = em.createNamedQuery(ProductVersion.QUERY_FIND_BY_RESOURCE_TYPE_AND_VERSION);
                productVersionQuery.setParameter("resourceType", resourceType1);
                productVersionQuery.setParameter("version", "2.0.0");

                productVersionList = productVersionQuery.getResultList();
                assert productVersionList.size() > 0 : "Could not find product version for 2.0.0";
            } finally {
                getTransactionManager().rollback();
                em.close();
            }

            // create a channel
            pc = PageControl.getUnlimitedInstance();
            int origChannelCount = channelManager.getAllChannels(overlord, pc).size();
            Channel channel = new Channel("testMergeSyncReportChannel");
            channel = channelManager.createChannel(overlord, channel);
            assert (origChannelCount + 1) == channelManager.getAllChannels(overlord, pc).size();
            channelId = channel.getId();

            // see that the resource sees no metadata yet - not subscribed yet
            pc = PageControl.getUnlimitedInstance();
            PageList<PackageVersionMetadataComposite> metadataList;
            metadataList = contentSourceManager.getPackageVersionMetadata(resource1.getId(), pc);
            assert metadataList != null;
            assert metadataList.size() == 0 : "-->" + metadataList;
            String metadataMd5 = contentSourceManager.getResourceSubscriptionMD5(resource1.getId());
            assert metadataMd5 != null;
            assert metadataMd5.length() == 32 : "-->" + metadataMd5;
            assert metadataMd5.equals("d41d8cd98f00b204e9800998ecf8427e") : "-->" + metadataMd5;

            // just to make sure the MD5 for empty data is what we think it is...
            metadataMd5 = contentSourceManager.getResourceSubscriptionMD5(Integer.MIN_VALUE); // should find no metadata at all
            assert metadataMd5 != null;
            assert metadataMd5.length() == 32 : "-->" + metadataMd5;
            assert metadataMd5.equals("d41d8cd98f00b204e9800998ecf8427e") : "-->" + metadataMd5;

            // add the content source's packages to the channel
            channelManager.addContentSourcesToChannel(overlord, channelId, new int[] { contentSourceId });

            // see the package versions have been assigned to the channel and content source
            List<PackageVersion> inChannel;
            List<PackageVersionContentSource> inContentSources;
            List<PackageVersionContentSource> inContentSource;

            pc = PageControl.getUnlimitedInstance();
            inChannel = channelManager.getPackageVersionsInChannel(overlord, channelId, pc);
            pc = PageControl.getUnlimitedInstance();
            inContentSources = contentSourceManager.getPackageVersionsFromContentSources(overlord,
                new int[] { contentSourceId }, pc);
            inContentSource = contentSourceManager.getPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert inChannel != null;
            assert inContentSources != null;
            assert inContentSource != null;
            assert inChannel.size() == 1 : inChannel;
            assert inContentSources.size() == 1 : inContentSources;
            assert inContentSource.size() == 1 : inContentSource;

            // confirm that we didn't load the bits yet
            pc = PageControl.getUnlimitedInstance();
            List<PackageVersionContentSource> unloaded;
            unloaded = contentSourceManager.getUnloadedPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert unloaded != null;
            assert unloaded.size() == 1;

            // check the counts
            long pvccount = channelManager.getPackageVersionCountFromChannel(overlord, channel.getId());
            assert (pvccount == 1) : "-->" + pvccount;
            long pvcscount = contentSourceManager.getPackageVersionCountFromContentSource(overlord, contentSourceId);
            assert (pvcscount == 1) : "-->" + pvcscount;

            // subscribe the resource
            channelManager.subscribeResourceToChannels(overlord, resource1.getId(), new int[] { channelId });

            // confirm the resource is subscribed
            pc = PageControl.getUnlimitedInstance();
            metadataList = contentSourceManager.getPackageVersionMetadata(resource1.getId(), pc);
            assert metadataList != null;
            assert metadataList.size() == 1 : "-->" + metadataList;
            metadataMd5 = contentSourceManager.getResourceSubscriptionMD5(resource1.getId());
            assert metadataMd5 != null;
            assert metadataMd5.length() == 32 : "-->" + metadataMd5;

            // MD5 is based on the hash code of last modified time
            channel = channelManager.getChannel(overlord, channelId);
            String datehash = Integer.toString(channel.getLastModifiedDate().hashCode());
            assert metadataMd5.equals(MD5Generator.getDigestString(datehash)) : "-->" + metadataMd5;

            channelManager.unsubscribeResourceFromChannels(overlord, resource1.getId(), new int[] { channelId });

            // confirm the resource is unsubscribed
            pc = PageControl.getUnlimitedInstance();
            metadataList = contentSourceManager.getPackageVersionMetadata(resource1.getId(), pc);
            assert metadataList != null;
            assert metadataList.size() == 0 : "-->" + metadataList;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                // clean up - delete all created entities
                if (channelId != 0) {
                    channelManager.deleteChannel(overlord, channelId);
                }

                if (contentSourceId != 0) {
                    contentSourceManager.deleteContentSource(overlord, contentSourceId);
                }

                contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
            } catch (Throwable t) {
            }
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testMergeSyncReportAddRemoveUpdate() throws Exception {
        List<PackageVersionContentSource> inCS;
        PageControl pc = PageControl.getUnlimitedInstance();
        int contentSourceId = 0;

        try {
            // create content source type and content source
            ContentSourceType type = new ContentSourceType("testMergeSyncReportAMUCST");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            ContentSource contentSource = new ContentSource("testMergeSyncReportAMUCS", type);
            contentSource.setLazyLoad(true);
            contentSource.setDownloadMode(DownloadMode.DATABASE);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);
            contentSourceId = contentSource.getId();
            assert contentSourceId > 0;

            // just make sure there are no package versions yet
            inCS = contentSourceManager.getPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert inCS != null;
            assert inCS.size() == 0 : inCS;

            // need this to pass to merge
            ContentSourceSyncResults results = new ContentSourceSyncResults(contentSource);
            results = contentSourceManager.persistContentSourceSyncResults(results);
            assert results != null;

            // this report will add a mapping to PV->CS
            PackageSyncReport report = new PackageSyncReport();
            ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey("testARUFoo", "testARUVer",
                packageType1.getName(), architecture1.getName(), resourceType1.getName(), resourceType1.getPlugin());
            ContentSourcePackageDetails details = new ContentSourcePackageDetails(key);
            details.setLocation("dummy-location-aru");
            details.setFileSize(1234L); // lazy load is on, this should not matter
            report.addNewPackage(details);
            Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous;
            previous = new HashMap<ContentSourcePackageDetailsKey, PackageVersionContentSource>();

            // ADD: merge the report!
            results = contentSourceManager.mergeContentSourceSyncReport(contentSource, report, previous, results);
            assert results != null;

            // see the package version has been assigned to the content source
            inCS = contentSourceManager.getPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert inCS != null;
            assert inCS.size() == 1 : inCS;

            // confirm that we didn't load the bits yet
            List<PackageVersionContentSource> unloaded;
            unloaded = contentSourceManager.getUnloadedPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert unloaded != null;
            assert unloaded.size() == 1;

            // check the count
            long pvcscount = contentSourceManager.getPackageVersionCountFromContentSource(overlord, contentSourceId);
            assert (pvcscount == 1) : "-->" + pvcscount;

            // this is the new one we just added - we'll pass this to our next merge as the previous state
            PackageVersionContentSource addedPVCS = unloaded.get(0);
            assert addedPVCS.getPackageVersionContentSourcePK().getPackageVersion().getFileSize() == 1234L;
            previous.put(key, addedPVCS);

            System.out.println("content source merge ADD works!");

            // create a new report that updates the one we just added
            report = new PackageSyncReport();
            details.setFileSize(9999L);
            report.addUpdatedPackage(details);

            // UPDATE: merge the report!
            results = contentSourceManager.mergeContentSourceSyncReport(contentSource, report, previous, results);
            assert results != null;

            // see the package version is still assigned to the content source
            inCS = contentSourceManager.getPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert inCS != null;
            assert inCS.size() == 1 : inCS;

            // it should still be unloaded, make sure and check that it really was updated
            unloaded = contentSourceManager.getUnloadedPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert unloaded != null;
            assert unloaded.size() == 1;
            assert unloaded.get(0).getPackageVersionContentSourcePK().getPackageVersion().getFileSize() == 9999L;

            System.out.println("content source merge UPDATE works!");

            // create a report that removes the one we added/updated, our 'previous' map is still valid
            report = new PackageSyncReport();
            report.addDeletePackage(details);

            // REMOVE: merge the report!
            results = contentSourceManager.mergeContentSourceSyncReport(contentSource, report, previous, results);
            assert results != null;

            // see the package version is gone
            inCS = contentSourceManager.getPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert inCS != null;
            assert inCS.size() == 0 : inCS;

            // check the count - should no longer be any package versions
            pvcscount = contentSourceManager.getPackageVersionCountFromContentSource(overlord, contentSourceId);
            assert (pvcscount == 0) : "-->" + pvcscount;

            System.out.println("content source merge REMOVE works!");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (contentSourceId != 0) {
                    contentSourceManager.deleteContentSource(overlord, contentSourceId);
                }

                contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
            } catch (Throwable t) {
            }
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testMergeSyncReportAddRemoveUpdateWithChannel() throws Exception {
        PageControl pc = PageControl.getUnlimitedInstance();
        int contentSourceId = 0;
        int channelId = 0;

        try {
            // create content source type and content source and channel
            ContentSourceType type = new ContentSourceType("testMergeSyncReportAMU2CST");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            ContentSource contentSource = new ContentSource("testMergeSyncReportAMU2CS", type);
            contentSource.setLazyLoad(true);
            contentSource.setDownloadMode(DownloadMode.DATABASE);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);
            contentSourceId = contentSource.getId();
            assert contentSourceId > 0;
            Channel channel = new Channel("testMergeSyncReportAMU2Ch");
            channelManager.createChannel(overlord, channel);
            channelId = channel.getId();
            assert channelId > 0;
            channelManager.addContentSourcesToChannel(overlord, channelId, new int[] { contentSourceId });

            // just make sure there are no package versions yet
            assert 0 == contentSourceManager.getPackageVersionCountFromContentSource(overlord, contentSourceId);
            assert 0 == channelManager.getPackageVersionCountFromChannel(overlord, channelId);

            // need this to pass to merge
            ContentSourceSyncResults results = new ContentSourceSyncResults(contentSource);
            results = contentSourceManager.persistContentSourceSyncResults(results);
            assert results != null;

            // this report will add a mapping to PV->CS
            PackageSyncReport report = new PackageSyncReport();
            ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey("testARU2Foo", "testARU2Ver",
                packageType1.getName(), architecture1.getName(), resourceType1.getName(), resourceType1.getPlugin());
            ContentSourcePackageDetails details = new ContentSourcePackageDetails(key);
            details.setLocation("dummy-location-aru");
            report.addNewPackage(details);
            Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous;
            previous = new HashMap<ContentSourcePackageDetailsKey, PackageVersionContentSource>();

            // ADD: merge the report!
            results = contentSourceManager.mergeContentSourceSyncReport(contentSource, report, previous, results);
            assert results != null;

            List<PackageVersionContentSource> unloaded;
            unloaded = contentSourceManager.getUnloadedPackageVersionsFromContentSource(overlord, contentSourceId, pc);
            assert unloaded != null;
            assert unloaded.size() == 1;

            // check the count - since content source was in a channel, the channel gets the PV too
            assert 1 == contentSourceManager.getPackageVersionCountFromContentSource(overlord, contentSourceId);
            assert 1 == channelManager.getPackageVersionCountFromChannel(overlord, channelId);

            // this is the new one we just added - we'll pass this to our next merge as the previous state
            PackageVersionContentSource addedPVCS = unloaded.get(0);
            previous.put(key, addedPVCS);

            // create a report that removes the one we added
            report = new PackageSyncReport();
            report.addDeletePackage(details);

            // REMOVE: merge the report!
            results = contentSourceManager.mergeContentSourceSyncReport(contentSource, report, previous, results);
            assert results != null;

            // check the count - note the channel's PV remains intact!!
            assert 0 == contentSourceManager.getPackageVersionCountFromContentSource(overlord, contentSourceId);
            assert 1 == channelManager.getPackageVersionCountFromChannel(overlord, channelId);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (channelId != 0) {
                    channelManager.deleteChannel(overlord, channelId);
                }

                if (contentSourceId != 0) {
                    contentSourceManager.deleteContentSource(overlord, contentSourceId);
                }

                contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
            } catch (Throwable t) {
            }
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeleteContentSource() throws Exception {
        PageControl pc = PageControl.getUnlimitedInstance();

        try {
            ContentSourceType type = null;
            ContentSource contentSource = null;

            int csTypeCount = contentSourceManager.getAllContentSourceTypes().size();
            int csCount = contentSourceManager.getAllContentSources(overlord, pc).size();

            // create the content source type
            type = new ContentSourceType("testDeleteContentSourceCST");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            type = contentSourceManager.getContentSourceType(type.getName());
            assert type != null;
            assert type.getId() > 0;

            // test the getAll API
            assert (csTypeCount + 1) == contentSourceManager.getAllContentSourceTypes().size();

            // create the content source
            contentSource = new ContentSource("testDeleteContentSource", type);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);
            assert contentSource != null;
            int contentSourceId = contentSource.getId();
            assert contentSourceId > 0;

            // test the getAll API
            assert (csCount + 1) == contentSourceManager.getAllContentSources(overlord, pc).size();

            // create a channel and associate the new content source with it
            Channel channel = new Channel("testDeleteContentSourceChannel");
            channelManager.createChannel(overlord, channel);
            channelManager.addContentSourcesToChannel(overlord, channel.getId(), new int[] { contentSourceId });

            // try to delete the content source
            assert null != contentSourceManager.getContentSource(overlord, contentSourceId) : "should exist";
            contentSourceManager.deleteContentSource(overlord, contentSourceId);
            assert null == contentSourceManager.getContentSource(overlord, contentSourceId) : "should have been deleted";

            // I need to clean these up now
            channelManager.deleteChannel(overlord, channel.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testUpdateContentSourceNoConfig() throws Exception {
        try {
            ContentSourceType type = null;
            ContentSource contentSource = null;

            // create the content source type
            type = new ContentSourceType("testUpdateContentSourceCST");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            type = contentSourceManager.getContentSourceType(type.getName());
            assert type != null;
            assert type.getId() > 0;

            // create the content source - note it doesn't have any config yet
            contentSource = new ContentSource("testUpdateContentSource", type);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);
            assert contentSource != null;
            int contentSourceId = contentSource.getId();
            assert contentSourceId > 0;

            // now update its description
            ContentSource loaded = contentSourceManager.getContentSource(overlord, contentSourceId);
            assert loaded != null : "should exist";
            assert loaded.getDescription() == null;
            loaded.setDescription("new updated description");
            loaded = contentSourceManager.updateContentSource(overlord, loaded);
            assert loaded != null : "should have been updated";
            loaded = contentSourceManager.getContentSource(overlord, contentSourceId);
            assert loaded.getDescription().equals("new updated description");

            // now give it a config
            assert loaded.getConfiguration() == null : "should not have a config yet";
            Configuration config = new Configuration();
            config.put(new PropertySimple("updateCSName", "updateCSValue"));
            loaded.setConfiguration(config);
            loaded = contentSourceManager.updateContentSource(overlord, loaded);
            assert loaded != null : "should have been updated";
            config = loaded.getConfiguration();
            assert config != null : "should have a config now";
            assert config.getSimple("updateCSName").getStringValue().equals("updateCSValue") : config;
            assert loaded.getDescription().equals("new updated description") : "desc should still be there";

            // delete the content source
            contentSourceManager.deleteContentSource(overlord, contentSourceId);
            assert null == contentSourceManager.getContentSource(overlord, contentSourceId) : "should have been deleted";
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testUpdateContentSourceConfig() throws Exception {
        try {
            ContentSourceType type = null;
            ContentSource contentSource = null;

            // create the content source type
            type = new ContentSourceType("testUpdateContentSourceCST2");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            type = contentSourceManager.getContentSourceType(type.getName());
            assert type != null;
            assert type.getId() > 0;

            // create the content source - note it has a config that should be persisted
            Configuration config = new Configuration();
            config.put(new PropertySimple("updateCSName", "updateCSValue"));
            contentSource = new ContentSource("testUpdateContentSource2", type);
            contentSource.setConfiguration(config);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);
            assert contentSource != null;
            int contentSourceId = contentSource.getId();
            assert contentSourceId > 0;

            // now update its config
            ContentSource loaded = contentSourceManager.getContentSource(overlord, contentSourceId);
            assert loaded != null : "should exist";
            assert loaded.getConfiguration() != null;
            assert loaded.getConfiguration().getSimple("updateCSName").getStringValue().equals("updateCSValue");
            loaded.getConfiguration().getSimple("updateCSName").setStringValue("UPDATED");
            loaded = contentSourceManager.updateContentSource(overlord, loaded);
            assert loaded != null : "should have been updated";
            assert loaded.getConfiguration() != null;
            assert loaded.getConfiguration().getSimple("updateCSName").getStringValue().equals("UPDATED");
            loaded = contentSourceManager.getContentSource(overlord, contentSourceId);
            assert loaded != null : "should exist";
            assert loaded.getConfiguration() != null;
            assert loaded.getConfiguration().getSimple("updateCSName").getStringValue().equals("UPDATED");

            // now delete its config
            loaded.setConfiguration(null);
            loaded = contentSourceManager.updateContentSource(overlord, loaded);
            assert loaded != null : "should have been updated";
            assert loaded.getConfiguration() == null : "config should be null -> " + loaded.getConfiguration();

            // delete the content source
            contentSourceManager.deleteContentSource(overlord, contentSourceId);
            assert null == contentSourceManager.getContentSource(overlord, contentSourceId) : "should have been deleted";
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testConstraintViolation() throws Exception {
        ContentSourceType type = null;
        ContentSource contentSource = null;

        TransactionManager tx = getTransactionManager();
        tx.begin();
        try {
            type = new ContentSourceType("testConstraintViolationCST");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            type = contentSourceManager.getContentSourceType(type.getName());
            assert type != null;
            assert type.getId() > 0;
            contentSource = new ContentSource("testConstraintViolation", type);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);
            assert contentSource != null;
            int contentSourceId = contentSource.getId();
            assert contentSourceId > 0;

            ContentSource dup = new ContentSource("testConstraintViolation", type);

            try {
                contentSourceManager.createContentSource(overlord, dup);
                tx.commit();
                assert false : "Should not have been able to create the same content source";
            } catch (Exception expected) {
            } finally {
                tx = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (tx != null) {
                tx.rollback();
            }
        }
    }

    /**
     * This tests that when you merge a sync report and a channel is associated with a content source, the channel gets
     * the package versions assigned to it. It also tests that deleting channel and content source will purge orphaned
     * PVs.
     *
     * @throws Exception
     */
    @Test(enabled = TESTS_ENABLED)
    public void testMergeWithChannel() throws Exception {
        try {
            ContentSourceType type = null;
            ContentSource contentSource = null;

            // create the content source type
            type = new ContentSourceType("testMergeWithChannelCST");
            Set<ContentSourceType> types = new HashSet<ContentSourceType>();
            types.add(type);
            contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
            type = contentSourceManager.getContentSourceType(type.getName());
            assert type != null;
            assert type.getId() > 0;

            // create the content source
            contentSource = new ContentSource("testMergeWithChannelCS", type);
            contentSource = contentSourceManager.createContentSource(overlord, contentSource);
            assert contentSource != null;
            int contentSourceId = contentSource.getId();
            assert contentSourceId > 0;

            // create a channel and associate the new content source with it
            Channel channel = new Channel("testMergeWithChannel");
            channelManager.createChannel(overlord, channel);
            channelManager.addContentSourcesToChannel(overlord, channel.getId(), new int[] { contentSourceId });

            // this report will add a mapping to PV->CS
            // we didn't set up any mappings like that yet - this will be the first one
            // since a channel has this CS - the channel->PV will also get mapped
            PackageSyncReport report = new PackageSyncReport();
            ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey("testMergeWithChannelfoo",
                "testMergeWithChannel-Version", packageType1.getName(), architecture1.getName(), resourceType1
                    .getName(), resourceType1.getPlugin());
            ContentSourcePackageDetails details = new ContentSourcePackageDetails(key);
            details.setExtraProperties(new Configuration());
            details.getExtraProperties().put(new PropertySimple("hello", "world"));
            details.setLocation("dummy-location");
            details.setFileSize(0L); // under the covers this ends up allowing us to create a package bits of size 0
            report.addNewPackage(details);
            Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous;
            previous = new HashMap<ContentSourcePackageDetailsKey, PackageVersionContentSource>();

            ContentSourceSyncResults results = new ContentSourceSyncResults(contentSource);
            results = contentSourceManager.persistContentSourceSyncResults(results);
            assert results != null;

            contentSourceManager.mergeContentSourceSyncReport(contentSource, report, previous, results);

            List<PackageVersion> inChannel;
            inChannel = channelManager.getPackageVersionsInChannel(overlord, channel.getId(), PageControl
                .getUnlimitedInstance());
            assert inChannel != null;
            assert inChannel.size() == 1;
            assert "testMergeWithChannel-Version".equals(inChannel.get(0).getVersion());

            // sanity check - make sure our own entity manager can find the PV entity
            getTransactionManager().begin();
            try {
                PackageVersion foundPv = getEntityManager().find(PackageVersion.class, inChannel.get(0).getId());
                assert foundPv != null;
                assert foundPv.getExtraProperties() != null;
                assert foundPv.getExtraProperties().getSimple("hello").getStringValue().equals("world");
            } finally {
                getTransactionManager().rollback();
            }

            // delete the content source first
            contentSourceManager.deleteContentSource(overlord, contentSourceId);

            // make sure our PV isn't orphaned yet! It is directly related to the channel still
            getTransactionManager().begin();
            try {
                assert null != getEntityManager().find(PackageVersion.class, inChannel.get(0).getId());
            } finally {
                getTransactionManager().rollback();
            }

            // delete the channel - this finally orphans the PV, so the PV should get deleted automatically
            channelManager.deleteChannel(overlord, channel.getId());

            // test to make sure we purged the orphaned package version (since both content source and channel are gone now)
            getTransactionManager().begin();
            try {
                assert null == getEntityManager().find(PackageVersion.class, inChannel.get(0).getId());
            } finally {
                getTransactionManager().rollback();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
        }
    }

    private void setupTestEnvironment() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                architecture1 = em.find(Architecture.class, 1);
                architecture2 = em.find(Architecture.class, 2);

                resourceType1 = new ResourceType("platform-" + System.currentTimeMillis(), "TestPlugin",
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
                package5 = new Package("Package5", packageType4);

                PackageVersion package5Installed = new PackageVersion(package5, "1.0.0", architecture1);
                package5.addVersion(package5Installed);

                em.persist(package5);

                // Create resource against which we'll merge the discovery report
                resource1 = new Resource("parent" + System.currentTimeMillis(), "name", resourceType1);
                em.persist(resource1);

                // Install packages on the resource
                installedPackage1 = new InstalledPackage();
                installedPackage1.setResource(resource1);
                installedPackage1.setPackageVersion(package4Installed);
                resource1.addInstalledPackage(installedPackage1);

                installedPackage2 = new InstalledPackage();
                installedPackage2.setResource(resource1);
                installedPackage2.setPackageVersion(package4Installed);
                resource1.addInstalledPackage(installedPackage2);

                installedPackage1.setResource(resource1);
                installedPackage2.setResource(resource1);

                getTransactionManager().commit();
            } catch (Exception e) {
                e.printStackTrace();
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            em.close();
        }
    }

    private void tearDownTestEnvironment() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                resource1 = em.find(Resource.class, resource1.getId());
                for (InstalledPackage ip : resource1.getInstalledPackages()) {
                    em.remove(ip);
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

                em.remove(resource1);

                resourceType1 = em.find(ResourceType.class, resourceType1.getId());
                em.remove(resourceType1);

                getTransactionManager().commit();
            } catch (Exception e) {
                e.printStackTrace();
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            em.close();
        }
    }
}