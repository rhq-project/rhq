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
package org.rhq.enterprise.server.content;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.util.StringPropertyReplacer;

import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ChannelContentSource;
import org.rhq.core.domain.content.ChannelPackageVersion;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceSyncStatus;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.PackageVersionContentSourcePK;
import org.rhq.core.domain.content.ProductVersionPackageVersion;
import org.rhq.core.domain.content.composite.LoadedPackageBitsComposite;
import org.rhq.core.domain.content.composite.PackageVersionFile;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.MD5Generator;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.plugin.content.ContentSourceAdapterManager;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginContainer;
import org.rhq.enterprise.server.resource.ProductVersionManagerLocal;

/**
 * A SLSB wrapper around our server-side content source plugin container. This bean provides access to the
 * {@link ContentSource} objects deployed in the server, thus allows the callers to access data about and from remote
 * content repositories.
 *
 * @author John Mazzitelli
 */
// TODO: all authz checks need to be more fine grained... entitlements need to plug into here somehow?
@Stateless
@WebService(endpointInterface = "org.rhq.enterprise.server.content.ContentSourceManagerRemote")
public class ContentSourceManagerBean implements ContentSourceManagerLocal, ContentSourceManagerRemote {
    private final Log log = LogFactory.getLog(ContentSourceManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @EJB
    private ContentSourceManagerLocal contentSourceManager; //self
    @EJB
    private ContentManagerLocal contentManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private ProductVersionManagerLocal productVersionManager;

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void purgeOrphanedPackageVersions(Subject subject) {
        // get all orphaned package versions that have extra props, we need to delete the configs
        // separately. We do this using em.remove so we can get hibernate to perform the cascading for us.
        // Package versions normally do not have extra props, so we gain the advantage of using hibernate
        // to do the cascade deletes without incurring too much overhead in performing multiple removes.
        Query q = entityManager.createNamedQuery(PackageVersion.FIND_EXTRA_PROPS_IF_NO_CONTENT_SOURCES_OR_CHANNELS);
        List<PackageVersion> pvs = q.getResultList();
        for (PackageVersion pv : pvs) {
            entityManager.remove(pv.getExtraProperties());
            pv.setExtraProperties(null);
        }

        // Remove the bits on the filesystem if some were downloaded by a content source with DownloadMode.FILESYSTEM.
        // Query the package bits table and get the package bits where bits column is null - for those get the
        // related package versions and given the package version/filename you can get the files to delete.
        // Do not delete the files yet - just get the (package version ID, filename) composite list.
        q = entityManager.createNamedQuery(PackageVersion.FIND_FILES_IF_NO_CONTENT_SOURCES_OR_CHANNELS);
        List<PackageVersionFile> pvFiles = q.getResultList();

        // get ready for bulk delete by clearing entity manager
        entityManager.flush();
        entityManager.clear();

        // remove the productVersion->packageVersion mappings for all orphaned package versions
        entityManager.createNamedQuery(PackageVersion.DELETE_PVPV_IF_NO_CONTENT_SOURCES_OR_CHANNELS).executeUpdate();

        // remove the orphaned package versions
        int count = entityManager.createNamedQuery(PackageVersion.DELETE_IF_NO_CONTENT_SOURCES_OR_CHANNELS)
            .executeUpdate();

        // remove the package bits corresponding to the orphaned package versions we just deleted
        entityManager.createNamedQuery(PackageBits.DELETE_IF_NO_PACKAGE_VERSION).executeUpdate();

        // flush our bulk deletes
        entityManager.flush();
        entityManager.clear();

        // Now that we know we deleted all orphaned package versions, go ahead and delete
        // the files for those package bits that were stored on the filesystem.
        for (PackageVersionFile pvFile : pvFiles) {
            try {
                File doomed = getPackageBitsLocalFilesystemFile(pvFile.getPackageVersionId(), pvFile.getFileName());
                if (doomed.exists()) {
                    doomed.delete();
                }
            } catch (Exception e) {
                log.warn("Cannot purge orphaned package version file [" + pvFile.getFileName() + "] ("
                    + pvFile.getPackageVersionId() + ")");
            }
        }

        log.info("User [" + subject + "] purged [" + count + "] orphaned package versions");
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteContentSource(Subject subject, int contentSourceId) {
        log.debug("User [" + subject + "] is deleting content source [" + contentSourceId + "]");

        // bulk delete m-2-m mappings to the doomed content source
        // get ready for bulk delete by clearing entity manager
        entityManager.flush();
        entityManager.clear();

        entityManager.createNamedQuery(ChannelContentSource.DELETE_BY_CONTENT_SOURCE_ID).setParameter(
            "contentSourceId", contentSourceId).executeUpdate();

        entityManager.createNamedQuery(PackageVersionContentSource.DELETE_BY_CONTENT_SOURCE_ID).setParameter(
            "contentSourceId", contentSourceId).executeUpdate();

        ContentSource cs = entityManager.find(ContentSource.class, contentSourceId);
        if (cs != null) {
            entityManager.remove(cs);
            log.debug("User [" + subject + "] deleted content source [" + cs + "]");

            // make sure we stop its adapter and unschedule any sync job associated with it
            try {
                ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
                pc.unscheduleSyncJob(cs);
                pc.getAdapterManager().shutdownAdapter(cs);
            } catch (Exception e) {
                log.warn("Failed to shutdown adapter for [" + cs + "]", e);
            }
        } else {
            log.debug("Content Source ID [" + contentSourceId + "] doesn't exist - nothing to delete");
        }

        // remove any unused, orphaned package versions
        purgeOrphanedPackageVersions(subject);

        return;
    }

    @SuppressWarnings("unchecked")
    public Set<ContentSourceType> getAllContentSourceTypes() {
        Query q = entityManager.createNamedQuery(ContentSourceType.QUERY_FIND_ALL);
        List<ContentSourceType> resultList = q.getResultList();
        return new HashSet<ContentSourceType>(resultList);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ContentSource> getAllContentSources(Subject subject, PageControl pc) {
        pc.initDefaultOrderingField("cs.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ContentSource.QUERY_FIND_ALL_WITH_CONFIG, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, ContentSource.QUERY_FIND_ALL);

        List<ContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ContentSource>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ContentSource> getAvailableContentSourcesForChannel(Subject subject, Integer channelId,
        PageControl pc) {
        pc.initDefaultOrderingField("cs.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ContentSource.QUERY_FIND_AVAILABLE_BY_CHANNEL_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            ContentSource.QUERY_FIND_AVAILABLE_BY_CHANNEL_ID);

        query.setParameter("channelId", channelId);
        countQuery.setParameter("channelId", channelId);

        List<ContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ContentSource>(results, (int) count, pc);
    }

    public ContentSourceType getContentSourceType(String name) {
        Query q = entityManager.createNamedQuery(ContentSourceType.QUERY_FIND_BY_NAME_WITH_CONFIG_DEF);
        q.setParameter("name", name);
        ContentSourceType type = (ContentSourceType) q.getSingleResult();
        return type;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ContentSource getContentSource(Subject subject, int contentSourceId) {
        Query q = entityManager.createNamedQuery(ContentSource.QUERY_FIND_BY_ID_WITH_CONFIG);
        q.setParameter("id", contentSourceId);

        ContentSource contentSource = null;

        try {
            contentSource = (ContentSource) q.getSingleResult();
        } catch (NoResultException nre) {
        }

        return contentSource;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ContentSource getContentSourceByNameAndType(Subject subject, String name, String typeName) {
        Query q = entityManager.createNamedQuery(ContentSource.QUERY_FIND_BY_NAME_AND_TYPENAME);
        q.setParameter("name", name);
        q.setParameter("typeName", typeName);

        ContentSource cs = null;

        try {
            cs = (ContentSource) q.getSingleResult();
        } catch (NoResultException nre) {
        }

        return cs;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Channel> getAssociatedChannels(Subject subject, int contentSourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Channel.QUERY_FIND_BY_CONTENT_SOURCE_ID,
            pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Channel.QUERY_FIND_BY_CONTENT_SOURCE_ID);

        query.setParameter("id", contentSourceId);
        countQuery.setParameter("id", contentSourceId);

        List<Channel> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Channel>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ContentSourceSyncResults> getContentSourceSyncResults(Subject subject, int contentSourceId,
        PageControl pc) {
        pc.initDefaultOrderingField("cssr.startTime", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ContentSourceSyncResults.QUERY_GET_ALL_BY_CONTENT_SOURCE_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            ContentSourceSyncResults.QUERY_GET_ALL_BY_CONTENT_SOURCE_ID);

        query.setParameter("contentSourceId", contentSourceId);
        countQuery.setParameter("contentSourceId", contentSourceId);

        List<ContentSourceSyncResults> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ContentSourceSyncResults>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteContentSourceSyncResults(Subject subject, int[] ids) {
        if (ids != null) {
            for (int id : ids) {
                ContentSourceSyncResults doomed = entityManager.getReference(ContentSourceSyncResults.class, id);
                entityManager.remove(doomed);
            }
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ContentSource createContentSource(Subject subject, String name, String description, String typeName,
        Configuration configuration, boolean lazyLoad, DownloadMode downloadMode) throws ContentSourceException {
        log.debug("User [" + subject + "] is creating a content source [" + name + "] of type [" + typeName + "]");

        // we first must get the content source type - if it doesn't exist, we throw an exception
        Query q = entityManager.createNamedQuery(ContentSourceType.QUERY_FIND_BY_NAME);
        q.setParameter("name", typeName);
        ContentSourceType type = (ContentSourceType) q.getSingleResult();

        // if download mode isn't specified, use the default specified in the content source type definition
        if (downloadMode == null) {
            downloadMode = type.getDefaultDownloadMode();
        }

        // Store the content source
        ContentSource source = new ContentSource(name, type);
        source.setDescription(description);
        source.setConfiguration(configuration);
        source.setLazyLoad(lazyLoad);
        source.setDownloadMode(downloadMode);

        validateContentSource(source);

        source = createContentSource(subject, source);

        return source;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ContentSource createContentSource(Subject subject, ContentSource contentSource)
        throws ContentSourceException {
        validateContentSource(contentSource);

        log.debug("User [" + subject + "] is creating content source [" + contentSource + "]");

        // these aren't cascaded during persist, but I want to set them to null anyway, just to be sure
        contentSource.setSyncResults(null);

        entityManager.persist(contentSource);

        log.debug("User [" + subject + "] created content source [" + contentSource + "]");

        // now that a new content source has been added to the system, let's start its adapter now
        try {
            ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
            pc.getAdapterManager().startAdapter(contentSource);
            pc.scheduleSyncJob(contentSource);
        } catch (Exception e) {
            log.warn("Failed to start adapter for [" + contentSource + "]", e);
        }

        return contentSource; // now has the ID set
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ContentSource updateContentSource(Subject subject, ContentSource contentSource)
        throws ContentSourceException {

        log.debug("User [" + subject + "] is updating content source [" + contentSource + "]");
        contentSource = entityManager.merge(contentSource);
        log.debug("User [" + subject + "] updated content source [" + contentSource + "]");

        // now that the content source has been changed,
        // restart its adapter and reschedule its sync job because the config might have changed.
        // synchronize it now, too
        try {
            ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
            pc.unscheduleSyncJob(contentSource);
            pc.getAdapterManager().restartAdapter(contentSource);
            pc.scheduleSyncJob(contentSource);
            pc.syncNow(contentSource);
        } catch (Exception e) {
            log.warn("Failed to restart adapter for [" + contentSource + "]", e);
        }

        return contentSource;
    }

    private void validateContentSource(ContentSource cs) throws ContentSourceException {

        String name = cs.getName();
        ContentSourceType type = cs.getContentSourceType();

        if (name == null || name.trim().equals("")) {
            throw new ContentSourceException("ContentSource name attribute is required");
        }

        // If a content source with this name and type combination exists, throw an error as it's a violation
        // of the DB uniqueness constraints
        Query q = entityManager.createNamedQuery(ContentSource.QUERY_FIND_BY_NAME_AND_TYPENAME);
        q.setParameter("name", name);
        q.setParameter("typeName", type.getName());

        List existingMatchingContentSources = q.getResultList();
        if (existingMatchingContentSources.size() > 0) {
            throw new ContentSourceException("Content source with name [" + name + "] and of type [" + type.getName()
                + "] already exists, please specify a different name.");
        }

    }

    public boolean testContentSourceConnection(int contentSourceId) {
        try {
            ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
            return pc.getAdapterManager().testConnection(contentSourceId);
        } catch (Exception e) {
            log.info("Failed to test connection to [" + contentSourceId + "]. Cause: "
                + ThrowableUtil.getAllMessages(e));
            log.debug("Content source test connection failure stack follows for [" + contentSourceId + "]", e);

            return false;
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void synchronizeAndLoadContentSource(Subject subject, int contentSourceId) {
        try {
            ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
            ContentSource contentSource = entityManager.find(ContentSource.class, contentSourceId);

            if (contentSource != null) {
                pc.syncNow(contentSource);
            } else {
                log.warn("Asked to synchronize a non-existing content source [" + contentSourceId + "]");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not spawn the sync job for content source [" + contentSourceId + "]");
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersionContentSource> getPackageVersionsFromContentSource(Subject subject,
        int contentSourceId, PageControl pc) {
        pc.initDefaultOrderingField("pvcs.contentSource.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID, pc);
        query.setParameter("id", contentSourceId);

        List<PackageVersionContentSource> results = query.getResultList();
        long count = getPackageVersionCountFromContentSource(subject, contentSourceId);

        return new PageList<PackageVersionContentSource>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getPackageVersionCountFromContentSource(Subject subject, int contentSourceId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_COUNT);

        countQuery.setParameter("id", contentSourceId);
        Long count = (Long) countQuery.getSingleResult();
        return count.longValue();
    }

    public long getPackageBitsLength(int resourceId, PackageDetailsKey packageDetailsKey) {
        Query q = entityManager.createNamedQuery(PackageVersion.QUERY_GET_PKG_BITS_LENGTH_BY_PKG_DETAILS_AND_RES_ID);

        q.setParameter("packageName", packageDetailsKey.getName());
        q.setParameter("packageTypeName", packageDetailsKey.getPackageTypeName());
        q.setParameter("version", packageDetailsKey.getVersion());
        q.setParameter("architectureName", packageDetailsKey.getArchitectureName());
        q.setParameter("resourceId", resourceId);
        Long count = (Long) q.getSingleResult();
        return count.longValue();
    }

    /////////////////////////////////////////////////////////////////////
    // The methods below probably should not be exposed to remote clients

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersionContentSource> getPackageVersionsFromContentSources(Subject subject,
        int[] contentSourceIds, PageControl pc) {
        pc.initDefaultOrderingField("pvcs.contentSource.id");

        List<Integer> idList = new ArrayList<Integer>(contentSourceIds.length);
        for (Integer id : contentSourceIds) {
            idList.add(id);
        }

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersionContentSource.QUERY_FIND_BY_ALL_CONTENT_SOURCE_IDS, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersionContentSource.QUERY_FIND_BY_ALL_CONTENT_SOURCE_IDS_COUNT);

        query.setParameter("ids", idList);
        countQuery.setParameter("ids", idList);

        List<PackageVersionContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<PackageVersionContentSource>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersionContentSource> getUnloadedPackageVersionsFromContentSource(Subject subject,
        int contentSourceId, PageControl pc) {
        pc.initDefaultOrderingField("pvcs.contentSource.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_NOT_LOADED, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_NOT_LOADED_COUNT);

        query.setParameter("id", contentSourceId);
        countQuery.setParameter("id", contentSourceId);

        List<PackageVersionContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<PackageVersionContentSource>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(1000 * 60 * 30)
    public PackageBits downloadPackageBits(Subject subject, PackageVersionContentSource pvcs) {
        PackageVersionContentSourcePK pk = pvcs.getPackageVersionContentSourcePK();
        int contentSourceId = pk.getContentSource().getId();
        int packageVersionId = pk.getPackageVersion().getId();
        String packageVersionLocation = pvcs.getLocation();

        switch (pk.getContentSource().getDownloadMode()) {
        case NEVER: {
            return null; // no-op, our content source was told to never download package bits
        }

        case DATABASE: {
            log.debug("Downloading package bits to DB for package located at [" + packageVersionLocation
                + "] on content source [" + contentSourceId + "]");
            break;
        }

        case FILESYSTEM: {
            log.debug("Downloading package bits to filesystem for package located at [" + packageVersionLocation
                + "] on content source [" + contentSourceId + "]");
            break;
        }

        default: {
            throw new IllegalStateException(" Unknown download mode - this is a bug, please report it: " + pvcs);
        }
        }

        InputStream bitsStream = null;
        PackageBits packageBits = null;

        try {
            ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
            bitsStream = pc.getAdapterManager().loadPackageBits(contentSourceId, packageVersionLocation);

            Connection conn = null;
            PreparedStatement ps = null;

            try {
                packageBits = new PackageBits();
                entityManager.persist(packageBits);

                PackageVersion pv = entityManager.find(PackageVersion.class, packageVersionId);
                pv.setPackageBits(packageBits);

                entityManager.flush(); // push the new package bits row to the DB

                if (pk.getContentSource().getDownloadMode() == DownloadMode.DATABASE) {
                    // fallback to JDBC so we can stream the data to the blob column
                    conn = dataSource.getConnection();
                    ps = conn.prepareStatement("UPDATE " + PackageBits.TABLE_NAME + " SET BITS = ? WHERE ID = ?");
                    ps.setBinaryStream(1, bitsStream, pv.getFileSize().intValue());
                    ps.setInt(2, packageBits.getId());
                    if (ps.executeUpdate() != 1) {
                        throw new Exception("Did not download the package bits to the DB for [" + pv + "]");
                    }
                } else {
                    // store content to local file system
                    File outputFile = getPackageBitsLocalFilesystemFile(pv.getId(), pv.getFileName());
                    if (outputFile.exists()) {
                        // hmmm... it already exists, maybe we already have it?
                        // if the MD5's match, just ignore this download request and continue on
                        String expectedMD5 = (pv.getMD5() != null) ? pv.getMD5() : "<unspecified MD5>";
                        String actualMD5 = MD5Generator.getDigestString(outputFile);
                        if (!expectedMD5.trim().toLowerCase().equals(actualMD5.toLowerCase())) {
                            throw new Exception("Already have package bits for [" + pv + "] located at [" + outputFile
                                + "] but the MD5 hashcodes do not match. Expected MD5=[" + expectedMD5
                                + "], Actual MD5=[" + actualMD5 + "]");
                        } else {
                            log.info("Asked to download package bits but we already have it at [" + outputFile
                                + "] with matching MD5 of [" + actualMD5 + "]");
                        }
                    } else {
                        StreamUtil.copy(bitsStream, new FileOutputStream(outputFile), true);
                        bitsStream = null;
                    }
                }
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception e) {
                        log.warn("Failed to close prepared statement for package bits [" + packageVersionLocation
                            + "] on content source [" + contentSourceId + "]");
                    }
                }

                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception e) {
                        log.warn("Failed to close connection for package bits [" + packageVersionLocation
                            + "] on content source [" + contentSourceId + "]");
                    }
                }
            }
        } catch (Throwable t) {
            // put the cause in here using ThrowableUtil because it'll dump SQL nextException messages too
            throw new RuntimeException("Did not download the package bits for [" + pvcs + "]. Cause: "
                + ThrowableUtil.getAllMessages(t), t);
        } finally {
            if (bitsStream != null) {
                try {
                    bitsStream.close();
                } catch (Exception e) {
                    log.warn("Failed to close stream to package bits located at [" + packageVersionLocation
                        + "] on content source [" + contentSourceId + "]");
                }
            }
        }

        return packageBits;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public boolean internalSynchronizeContentSource(int contentSourceId) throws Exception {
        ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
        return pc.getAdapterManager().sychronizeContentSource(contentSourceId);
    }

    @SuppressWarnings("unchecked")
    public ContentSourceSyncResults persistContentSourceSyncResults(ContentSourceSyncResults results) {
        Query q = entityManager.createNamedQuery(ContentSourceSyncResults.QUERY_GET_INPROGRESS_BY_CONTENT_SOURCE_ID);
        q.setParameter("contentSourceId", results.getContentSource().getId());
        List<ContentSourceSyncResults> inprogressList = q.getResultList(); // will be ordered by start time descending

        boolean alreadyInProgress = false; // will be true if there is already a sync in progress

        if (inprogressList.size() > 0) {
            // If there is 1 in progress and we are being asked to persist one in progress,
            // then we either abort the persist if its recent, or we "kill" the old one by marking it failed.
            // We mark any others after the 1st one as a failure. How can you have more than 1 inprogress at
            // the same time? We shouldn't under normal circumstances, this is what we are trying to avoid in
            // this method - so we mark the status as failure because we assume something drastically bad
            // happened that left them in a bad state which will most likely never change unless we do it here.
            // If a content source sync takes longer than 24 hours, then we've made a bad assumption here and
            // this code needs to change - though I doubt any content source will take 24 hours to sync.
            if (results.getStatus() == ContentSourceSyncStatus.INPROGRESS) {
                if ((System.currentTimeMillis() - inprogressList.get(0).getStartTime()) < (1000 * 60 * 60 * 24)) {
                    alreadyInProgress = true;
                    inprogressList.remove(0); // we need to leave this one as-is, so get rid of it from list
                }
            }

            // take this time to mark all old inprogress results as failed
            for (ContentSourceSyncResults inprogress : inprogressList) {
                inprogress.setStatus(ContentSourceSyncStatus.FAILURE);
                inprogress.setEndTime(System.currentTimeMillis());
                inprogress.setResults("This synchronization seems to have stalled or ended abnormally.");
            }
        }

        ContentSourceSyncResults persistedResults = null; // leave it as null if something is already in progress

        if (!alreadyInProgress) {
            entityManager.persist(results);
            persistedResults = results;
        }

        return persistedResults;
    }

    // we want this in its own tx so other tx's can see it immediately, even if calling method is already in a tx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ContentSourceSyncResults mergeContentSourceSyncResults(ContentSourceSyncResults results) {
        return entityManager.merge(results);
    }

    public ContentSourceSyncResults getContentSourceSyncResults(int resultsId) {
        return entityManager.find(ContentSourceSyncResults.class, resultsId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    // we really want NEVER, but support tests that might be in a tx
    public ContentSourceSyncResults mergeContentSourceSyncReport(ContentSource contentSource, PackageSyncReport report,
        Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous, ContentSourceSyncResults syncResults) {
        try {
            StringBuilder progress = new StringBuilder();
            if (syncResults.getResults() != null) {
                progress.append(syncResults.getResults());
            }

            // First remove any old package versions no longer available,
            // then add new package versions that didn't exist before
            // then update package versions that have changed since the last sync.
            // We do these each in their own, new tx - if one fails we'd at least keep the changes from the previous.
            // Note that we do the ADD in chunks since it could take a very long time with a large list of packages.
            // The typical content source rarely removed or updates packages, so we do that in one big tx
            // (consider chunking those two in the future, if we see the need).

            //////////////////
            // REMOVE
            syncResults = contentSourceManager._mergeContentSourceSyncReportREMOVE(contentSource, report, previous,
                syncResults, progress);

            //////////////////
            // ADD
            List<ContentSourcePackageDetails> newPackages;
            newPackages = new ArrayList<ContentSourcePackageDetails>(report.getNewPackages());

            int chunkSize = 200;
            int fromIndex = 0;
            int toIndex = chunkSize;
            int newPackageCount = newPackages.size();

            progress.append(new Date()).append(": ").append("Adding");
            syncResults.setResults(progress.toString());
            syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);

            while (fromIndex < newPackageCount) {
                if (toIndex > newPackageCount) {
                    toIndex = newPackageCount;
                }

                List<ContentSourcePackageDetails> pkgs = newPackages.subList(fromIndex, toIndex);
                syncResults = contentSourceManager._mergeContentSourceSyncReportADD(contentSource, pkgs, previous,
                    syncResults, progress, fromIndex);
                fromIndex += chunkSize;
                toIndex += chunkSize;
            }

            progress.append("...").append(toIndex).append('\n');
            syncResults.setResults(progress.toString());
            syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);

            //////////////////
            // UPDATE
            syncResults = contentSourceManager._mergeContentSourceSyncReportUPDATE(contentSource, report, previous,
                syncResults, progress);

            // if we added/updated/deleted anything, change the last modified time of all channels
            // that get content from this content source
            if ((report.getNewPackages().size() > 0) || (report.getUpdatedPackages().size() > 0)
                || (report.getDeletedPackages().size() > 0)) {
                contentSourceManager._mergeContentSourceSyncReportUpdateChannel(contentSource.getId());
            }

            // let our sync results object know that we completed the merge
            // don't mark it as successful yet, let the caller do that
            progress.append(new Date()).append(": ").append("MERGE COMPLETE.\n");
            syncResults.setResults(progress.toString());
            syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);
        } catch (Throwable t) {
            // ThrowableUtil will dump SQL nextException messages, too
            String errorMsg = "Could not process sync report from [" + contentSource + "]. Cause: "
                + ThrowableUtil.getAllMessages(t);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg, t);
        }

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void _mergeContentSourceSyncReportUpdateChannel(int contentSourceId) {
        // this method should be called only after a merge of a content source
        // added/updated/removed one or more packages.  When this happens, we need to change
        // the last modified time for all channels that get content from the changed content source
        long now = System.currentTimeMillis();
        ContentSource contentSource = entityManager.find(ContentSource.class, contentSourceId);
        Set<ChannelContentSource> ccss = contentSource.getChannelContentSources();
        for (ChannelContentSource ccs : ccss) {
            ccs.getChannelContentSourcePK().getChannel().setLastModifiedDate(now);
        }

        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ContentSourceSyncResults _mergeContentSourceSyncReportREMOVE(ContentSource contentSource,
        PackageSyncReport report, Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous,
        ContentSourceSyncResults syncResults, StringBuilder progress) {
        progress.append(new Date()).append(": ").append("Removing");
        syncResults.setResults(progress.toString());
        syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);

        Query q;
        int flushCount = 0; // used to know when we should flush the entity manager - for performance purposes
        int removeCount = 0;

        // remove all packages that are no longer available on the remote repository
        // for each removed package, we need to purge the PVCS mapping and the PV itself

        for (ContentSourcePackageDetails doomedDetails : report.getDeletedPackages()) {
            // this is the mapping between the content source and the package version that needs to be deleted
            ContentSourcePackageDetailsKey doomedDetailsKey = doomedDetails.getContentSourcePackageDetailsKey();
            PackageVersionContentSource doomedPvcs = previous.get(doomedDetailsKey);
            doomedPvcs = entityManager.find(PackageVersionContentSource.class, doomedPvcs
                .getPackageVersionContentSourcePK());
            if (doomedPvcs != null) {
                entityManager.remove(doomedPvcs);
            }

            // this is the package version itself that we want to remove
            // but only delete if there are no other content sources that also serve that PackageVersion
            // or channels that are directly associated with this package version
            PackageVersion doomedPv = doomedPvcs.getPackageVersionContentSourcePK().getPackageVersion();
            q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_ID_IF_NO_CONTENT_SOURCES_OR_CHANNELS);
            q.setParameter("id", doomedPv.getId());
            try {
                doomedPv = (PackageVersion) q.getSingleResult();
                entityManager.remove(doomedPv);
            } catch (NoResultException nre) {
            }

            if ((++flushCount % 200) == 0) {
                entityManager.flush();
                entityManager.clear();
            }

            if ((++removeCount % 200) == 0) {
                progress.append("...").append(removeCount);
                syncResults.setResults(progress.toString());
                syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);
            }
        }

        progress.append("...").append(removeCount).append('\n');
        syncResults.setResults(progress.toString());
        syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);

        return syncResults;
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ContentSourceSyncResults _mergeContentSourceSyncReportADD(ContentSource contentSource,
        Collection<ContentSourcePackageDetails> newPackages,
        Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous,
        ContentSourceSyncResults syncResults, StringBuilder progress, int addCount) {
        Query q;
        int flushCount = 0; // used to know when we should flush the entity manager - for performance purposes

        Map<ResourceType, ResourceType> knownResourceTypes = new HashMap<ResourceType, ResourceType>();
        Map<PackageType, PackageType> knownPackageTypes = new HashMap<PackageType, PackageType>();
        Map<Architecture, Architecture> knownArchitectures = new HashMap<Architecture, Architecture>();
        List<Channel> associatedChannels = null;

        Map<ResourceType, Map<String, ProductVersion>> knownProductVersions = new HashMap<ResourceType, Map<String, ProductVersion>>();

        // add new packages that are new to the content source.
        // for each new package, we have to find its resource type and package type
        // (both of which must exist, or we abort that package and move on to the next);
        // then find the package and architecture, creating them if they do not exist;
        // then create the new PV as well as the new PVCS mapping.
        // if a channel is associated with the content source, the PV is directly associated with the channel.

        for (ContentSourcePackageDetails newDetails : newPackages) {
            ContentSourcePackageDetailsKey key = newDetails.getContentSourcePackageDetailsKey();

            // find the new package's associated resource type (should already exist)
            ResourceType rt = new ResourceType();
            rt.setName(key.getResourceTypeName());
            rt.setPlugin(key.getResourceTypePluginName());

            if (!knownResourceTypes.containsKey(rt)) {
                q = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN);
                q.setParameter("name", rt.getName());
                q.setParameter("plugin", rt.getPlugin());

                try {
                    rt = (ResourceType) q.getSingleResult();
                    knownResourceTypes.put(rt, rt); // cache it so we don't have to keep querying the DB
                    knownProductVersions.put(rt, new HashMap<String, ProductVersion>());
                } catch (NoResultException nre) {
                    log.warn("Content source adapter found a package for an unknown resource type ["
                        + key.getResourceTypeName() + "|" + key.getResourceTypePluginName() + "] Skipping it.");
                    continue; // skip this one but move on to the next
                }
            } else {
                rt = knownResourceTypes.get(rt);
            }

            // find the new package's type (package types should already exist, agent plugin descriptors define them)
            PackageType pt = new PackageType(key.getPackageTypeName(), rt);

            if (!knownPackageTypes.containsKey(pt)) {
                q = entityManager.createNamedQuery(PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_NAME);
                q.setParameter("typeId", rt.getId());
                q.setParameter("name", pt.getName());

                try {
                    pt = (PackageType) q.getSingleResult();
                    pt.setResourceType(rt); // we don't fetch join this, but we already know it, so just set it
                    knownPackageTypes.put(pt, pt); // cache it so we don't have to keep querying the DB
                } catch (NoResultException nre) {
                    log.warn("Content source adapter found a package of an unknown package type ["
                        + key.getPackageTypeName() + "|" + rt + "] Skipping it.");
                    continue; // skip this one but move on to the next
                }
            } else {
                pt = knownPackageTypes.get(pt);
            }

            // create the new package, if one does not already exist
            // we don't bother caching these - we won't have large amounts of the same packages
            q = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_RESOURCE_TYPE);
            q.setParameter("name", newDetails.getName());
            q.setParameter("packageTypeName", newDetails.getPackageTypeName());
            q.setParameter("resourceTypeId", rt.getId());
            Package pkg;
            try {
                pkg = (Package) q.getSingleResult();
            } catch (NoResultException nre) {
                pkg = new Package(newDetails.getName(), pt);
                pkg.setClassification(newDetails.getClassification());
                // we would have liked to rely on merge cascading when we merge the PV
                // but we need to watch out for the fact that we could be running at the
                // same time an agent sent us a content report that wants to create the same package.
                // if this is too hard a hit on performance, we can comment out the below line
                // and just accept the fact we might fail if the package is created underneath us,
                // which would cause our tx to rollback. the next sync should help us survive this failure.
                pkg = this.contentManager.persistOrMergePackageSafely(pkg);
            }

            // find and, if necessary create, the architecture
            Architecture arch = new Architecture(newDetails.getArchitectureName());

            if (!knownArchitectures.containsKey(arch)) {
                q = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
                q.setParameter("name", arch.getName());

                try {
                    arch = (Architecture) q.getSingleResult();
                    knownArchitectures.put(arch, arch); // cache it so we don't have to keep querying the DB
                } catch (NoResultException nre) {
                    log.info("Content source adapter found a previously unknown architecture [" + arch
                        + "] - it will be added to the list of known architectures");
                }
            } else {
                arch = knownArchitectures.get(arch);
            }

            // now finally create the new package version - this cascade-persists down several levels
            // note that other content sources might already be previously defined this, so only
            // persist it if it does not yet exist
            PackageVersion pv = new PackageVersion(pkg, newDetails.getVersion(), arch);
            pv.setDisplayName(newDetails.getDisplayName());
            pv.setDisplayVersion(newDetails.getDisplayVersion());
            pv.setExtraProperties(newDetails.getExtraProperties());
            pv.setFileCreatedDate(newDetails.getFileCreatedDate());
            pv.setFileName(newDetails.getFileName());
            pv.setFileSize(newDetails.getFileSize());
            pv.setLicenseName(newDetails.getLicenseName());
            pv.setLicenseVersion(newDetails.getLicenseVersion());
            pv.setLongDescription(newDetails.getLongDescription());
            pv.setMD5(newDetails.getMD5());
            pv.setMetadata(newDetails.getMetadata());
            pv.setSHA256(newDetails.getSHA265());
            pv.setShortDescription(newDetails.getShortDescription());

            q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            q.setParameter("packageName", newDetails.getName());
            q.setParameter("packageTypeName", pt.getName());
            q.setParameter("resourceTypeId", rt.getId());
            q.setParameter("architectureName", arch.getName());
            q.setParameter("version", newDetails.getVersion());

            try {
                PackageVersion pvExisting = (PackageVersion) q.getSingleResult();

                // the PackageVersion already exists, which is OK, another content source already defined it
                // but, let's make sure the important pieces of data are the same, otherwise, two content
                // sources SAY they have the same PackageVersion, but they really don't. We warn in the log
                // but the new data will overwrite the old.
                packageVersionAttributeCheck(pvExisting, pvExisting.getExtraProperties(), pv, pv.getExtraProperties(),
                    "ExtraProps");
                packageVersionAttributeCheck(pvExisting, pvExisting.getFileSize(), pv, pv.getFileSize(), "FileSize");
                packageVersionAttributeCheck(pvExisting, pvExisting.getFileName(), pv, pv.getFileName(), "FileName");
                packageVersionAttributeCheck(pvExisting, pvExisting.getMD5(), pv, pv.getMD5(), "MD5");
                packageVersionAttributeCheck(pvExisting, pvExisting.getSHA256(), pv, pv.getSHA256(), "SHA256");
                // what about metadata? test that for length only? string comparision check?

                pv = pvExisting;
            } catch (NoResultException nre) {
                // this is fine, its the first time we've seen this PV, let merge just create a new one
            }

            // we normally would want to do this:
            //    pv = entityManager.merge(pv);
            // but we have to watch out for an agent sending us a content report at the same time that
            // would create this PV concurrently.  If the below line takes too hard a hit on performance,
            // we can replace it with the merge call mentioned above and hope this concurrency doesn't happen.
            // if it does happen, we will rollback our tx and we'll have to wait for the next sync to fix it.
            pv = contentManager.persistOrMergePackageVersionSafely(pv);

            // For each resource version that is supported, make sure we have an entry for that in product version
            Set<String> resourceVersions = newDetails.getResourceVersions();
            if (resourceVersions != null) {
                Map<String, ProductVersion> cachedProductVersions = knownProductVersions.get(rt); // we are guaranteed that this returns non-null
                for (String version : resourceVersions) {
                    ProductVersion productVersion = cachedProductVersions.get(version);
                    if (productVersion == null) {
                        productVersion = productVersionManager.addProductVersion(rt, version);
                        cachedProductVersions.put(version, productVersion);
                    }

                    ProductVersionPackageVersion mapping = new ProductVersionPackageVersion(productVersion, pv);
                    entityManager.merge(mapping); // use merge just in case this mapping somehow already exists
                }
            }

            // now create the mapping between the package version and content source
            // now that if the mapping already exists, we overwrite it (a rare occurrence, but could happen)
            PackageVersionContentSource newPvcs = new PackageVersionContentSource(pv, contentSource, newDetails
                .getLocation());
            newPvcs = entityManager.merge(newPvcs);

            // for all channels that are associated with this content source, add this package version directly to them
            if (associatedChannels == null) {
                q = entityManager.createNamedQuery(Channel.QUERY_FIND_BY_CONTENT_SOURCE_ID_FETCH_CCS);
                q.setParameter("id", contentSource.getId());
                associatedChannels = q.getResultList();
            }

            for (Channel associatedChannel : associatedChannels) {
                ChannelPackageVersion mapping = new ChannelPackageVersion(associatedChannel, pv);
                entityManager.merge(mapping); // use merge just in case this mapping somehow already exists
            }

            if ((++flushCount % 100) == 0) {
                knownResourceTypes.clear();
                knownPackageTypes.clear();
                knownArchitectures.clear();
                associatedChannels = null;
                knownProductVersions.clear();
                entityManager.flush();
                entityManager.clear();
            }

            if ((++addCount % 100) == 0) {
                progress.append("...").append(addCount);
                syncResults.setResults(progress.toString());
                syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);
            }
        }

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ContentSourceSyncResults _mergeContentSourceSyncReportUPDATE(ContentSource contentSource,
        PackageSyncReport report, Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous,
        ContentSourceSyncResults syncResults, StringBuilder progress) {
        progress.append(new Date()).append(": ").append("Updating");
        syncResults.setResults(progress.toString());
        syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);

        int flushCount = 0; // used to know when we should flush the entity manager - for performance purposes
        int updateCount = 0;

        // update all changed packages that are still available on the remote repository but whose information is different
        //
        // for each updated package, we have to find its resource type
        // (which must exist, or we abort that package and move on to the next);
        // then we have to get the current PVCS and merge its updates

        for (ContentSourcePackageDetails updatedDetails : report.getUpdatedPackages()) {
            ContentSourcePackageDetailsKey key = updatedDetails.getContentSourcePackageDetailsKey();

            PackageVersionContentSource previousPvcs = previous.get(key);
            PackageVersionContentSource attachedPvcs; // what we will find in the DB, in jpa session

            attachedPvcs = entityManager.find(PackageVersionContentSource.class, previousPvcs
                .getPackageVersionContentSourcePK());
            if (attachedPvcs == null) {
                log.warn("Content source adapter reported that a non-existing package was updated, adding it [" + key
                    + "]");
                // I should probably not rely on persist cascade and use contentmanager.persistOrMergePackageVersionSafely
                // however, this rarely will occur (should never occur really) so I won't worry about it
                entityManager.persist(previousPvcs);
                attachedPvcs = previousPvcs;
            }

            PackageVersion pv = previousPvcs.getPackageVersionContentSourcePK().getPackageVersion();
            pv.setDisplayName(updatedDetails.getDisplayName());
            pv.setDisplayVersion(updatedDetails.getDisplayVersion());
            pv.setExtraProperties(updatedDetails.getExtraProperties());
            pv.setFileCreatedDate(updatedDetails.getFileCreatedDate());
            pv.setFileName(updatedDetails.getFileName());
            pv.setFileSize(updatedDetails.getFileSize());
            pv.setLicenseName(updatedDetails.getLicenseName());
            pv.setLicenseVersion(updatedDetails.getLicenseVersion());
            pv.setLongDescription(updatedDetails.getLongDescription());
            pv.setMD5(updatedDetails.getMD5());
            pv.setMetadata(updatedDetails.getMetadata());
            pv.setSHA256(updatedDetails.getSHA265());
            pv.setShortDescription(updatedDetails.getShortDescription());

            // we normally would want to do this:
            //    pv = entityManager.merge(pv);
            // but we have to watch out for an agent sending us a content report at the same time that
            // would create this PV concurrently.  If the below line takes too hard a hit on performance,
            // we can replace it with the merge call mentioned above and hope this concurrency doesn't happen.
            // if it does happen, we will rollback our tx and we'll have to wait for the next sync to fix it.
            pv = contentManager.persistOrMergePackageVersionSafely(pv);

            attachedPvcs.setLocation(updatedDetails.getLocation());

            if ((++flushCount % 200) == 0) {
                entityManager.flush();
                entityManager.clear();
            }

            if ((++updateCount % 200) == 0) {
                progress.append("...").append(updateCount);
                syncResults.setResults(progress.toString());
                syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);
            }
        }

        progress.append("...").append(updateCount).append('\n');
        syncResults.setResults(progress.toString());
        syncResults = contentSourceManager.mergeContentSourceSyncResults(syncResults);

        return syncResults;
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(int resourceId, PageControl pc) {
        pc.initDefaultOrderingField("pv.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_METADATA_BY_RESOURCE_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersion.QUERY_FIND_METADATA_BY_RESOURCE_ID);

        query.setParameter("resourceId", resourceId);
        countQuery.setParameter("resourceId", resourceId);

        List<PackageVersionMetadataComposite> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<PackageVersionMetadataComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public String getResourceSubscriptionMD5(int resourceId) {
        MD5Generator md5Generator = new MD5Generator();

        Query q = entityManager.createNamedQuery(Channel.QUERY_FIND_CHANNELS_BY_RESOURCE_ID);
        q.setParameter("resourceId", resourceId);
        List<Channel> channels = q.getResultList();

        for (Channel channel : channels) {
            long modifiedTimestamp = channel.getLastModifiedDate();
            Date modifiedDate = new Date(modifiedTimestamp);
            md5Generator.add(Integer.toString(modifiedDate.hashCode()).getBytes());
        }

        String digestString = md5Generator.getDigestString();
        return digestString;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(1000 * 60 * 30)
    public long outputPackageVersionBitsGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream) {
        return outputPackageVersionBitsRangeGivenResource(resourceId, packageDetailsKey, outputStream, 0, -1);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(1000 * 60 * 30)
    public long outputPackageBitsForChildResource(int parentResourceId, String resourceTypeName,
        PackageDetailsKey packageDetailsKey, OutputStream outputStream) {
        Resource parentResource = entityManager.find(Resource.class, parentResourceId);
        ResourceType parentResourceType = parentResource.getResourceType();

        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_PARENT_AND_NAME);
        query.setParameter("parent", parentResourceType);
        query.setParameter("name", resourceTypeName);

        ResourceType childResourceType = (ResourceType) query.getSingleResult();

        query = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
        query.setParameter("packageName", packageDetailsKey.getName());
        query.setParameter("packageTypeName", packageDetailsKey.getPackageTypeName());
        query.setParameter("architectureName", packageDetailsKey.getArchitectureName());
        query.setParameter("version", packageDetailsKey.getVersion());
        query.setParameter("resourceTypeId", childResourceType.getId());
        PackageVersion packageVersion = (PackageVersion) query.getSingleResult();

        return outputPackageVersionBitsRangeHelper(parentResourceId, packageDetailsKey, outputStream, 0, -1,
            packageVersion.getId());
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(1000 * 60 * 30)
    public long outputPackageVersionBitsRangeGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte) {
        if (startByte < 0) {
            throw new IllegalArgumentException("startByte[" + startByte + "] < 0");
        }

        if ((endByte > -1) && (endByte < startByte)) {
            throw new IllegalArgumentException("endByte[" + endByte + "] < startByte[" + startByte + "]");
        }

        // what package version?
        Query query = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_ID_BY_PACKAGE_DETAILS_KEY_AND_RES_ID);
        query.setParameter("packageName", packageDetailsKey.getName());
        query.setParameter("packageTypeName", packageDetailsKey.getPackageTypeName());
        query.setParameter("architectureName", packageDetailsKey.getArchitectureName());
        query.setParameter("version", packageDetailsKey.getVersion());
        query.setParameter("resourceId", resourceId);
        int packageVersionId = ((Integer) query.getSingleResult()).intValue();

        return outputPackageVersionBitsRangeHelper(resourceId, packageDetailsKey, outputStream, startByte, endByte,
            packageVersionId);
    }

    private long outputPackageVersionBitsRangeHelper(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte, int packageVersionId) {

        // TODO: Should we make sure the resource is subscribed/allowed to receive the the package version?
        //       Or should we not bother to perform this check?  if the caller knows the PV ID, it
        //       probably already got it through its channels

        Query query = entityManager.createNamedQuery(PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID);
        query.setParameter("id", packageVersionId);
        LoadedPackageBitsComposite composite = (LoadedPackageBitsComposite) query.getSingleResult();

        boolean packageBitsAreAvailable = composite.isPackageBitsAvailable();
        if (packageBitsAreAvailable) {
            // it says the package bits are available, but if its stored on the filesystem, we should
            // make sure no one deleted the file.  If the file is deleted, let's simply download it again.
            if (!composite.isPackageBitsInDatabase()) {
                try {
                    File bitsFile = getPackageBitsLocalFilesystemFile(composite.getPackageVersionId(), composite
                        .getFileName());
                    if (!bitsFile.exists()) {
                        log.warn("Package version [" + packageDetailsKey + "] has had its bits file [" + bitsFile
                            + "] deleted. Will attempt to download it again.");
                        packageBitsAreAvailable = false;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Package version [" + packageDetailsKey
                        + "] has had its bits file deleted but cannot download it again.", e);
                }
            }
        }

        PackageVersionContentSource pvcs = null; // will be non-null only if package bits were not originally available

        if (!packageBitsAreAvailable) {
            if (resourceId == -1) {
                throw new IllegalStateException("Package bits must be inserted prior to the agent asking for them "
                    + "during a cotent-based resource creation");
            }
            // if we got here, the package bits have not been downloaded yet.  This eliminates the
            // possibility that the package version were directly uploaded by a user
            // or auto-discovered by a resource and attached to a channel. So, that leaves
            // the only possibility - the package version comes from a content source and therefore has
            // a PackageVersionContentSource mapping.  Let's find that mapping.
            Query q2 = entityManager.createNamedQuery(PackageVersionContentSource.QUERY_FIND_BY_PKG_VER_ID_AND_RES_ID);
            q2.setParameter("resourceId", resourceId);
            q2.setParameter("packageVersionId", packageVersionId);
            List<PackageVersionContentSource> pvcss = q2.getResultList();

            // Note that its possible more than one content source can deliver a PV - if a resource is subscribed
            // to channel(s) that contain multiple content sources that can deliver a PV, we just take
            // the first one we find.

            if (pvcss.size() == 0) {
                throw new RuntimeException("Resource [" + resourceId + "] cannot access package version ["
                    + packageDetailsKey + "] - no content source exists to deliver it");
            }

            pvcs = pvcss.get(0);

            // Make it a true EJB call so we suspend our tx and get a new tx.
            // This way, we start with a fresh tx timeout when downloading and this
            // won't affect the time we are in in this method's tx (I hope that's how it works).
            // This is because this method itself may take a long time to send the data to the output stream
            // and we don't want out tx timing out due to the time it takes downloading.
            PackageBits bits = null;
            bits = contentSourceManager.downloadPackageBits(subjectManager.getOverlord(), pvcs);

            if (bits != null) {
                // rerun the query just to make sure we really downloaded it successfully
                query.setParameter("id", pvcs.getPackageVersionContentSourcePK().getPackageVersion().getId());
                composite = (LoadedPackageBitsComposite) query.getSingleResult();

                if (!composite.isPackageBitsAvailable()) {
                    throw new RuntimeException("Failed to download package bits [" + packageDetailsKey
                        + "] for resource [" + resourceId + "]");
                }
            } else {
                // package bits are not loaded and never will be loaded due to content source's download mode == NEVER
                composite = null;
            }
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet results = null;
        InputStream bitsStream = null;

        try {
            if (composite == null) {
                // this is DownloadMode.NEVER and we are really in pass-through mode, stream directly from adapter
                ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
                ContentSourceAdapterManager adapterMgr = pc.getAdapterManager();
                int contentSourceId = pvcs.getPackageVersionContentSourcePK().getContentSource().getId();
                bitsStream = adapterMgr.loadPackageBits(contentSourceId, pvcs.getLocation());
            } else {
                if (composite.isPackageBitsInDatabase()) {
                    // this is  DownloadMode.DATABASE - put the bits in the database
                    conn = dataSource.getConnection();
                    ps = conn.prepareStatement("SELECT BITS FROM " + PackageBits.TABLE_NAME + " WHERE ID = ?");

                    ps.setInt(1, composite.getPackageBitsId());
                    results = ps.executeQuery();
                    results.next();
                    bitsStream = results.getBinaryStream(1);
                    if (bitsStream == null) {
                        throw new RuntimeException("Got null for package bits stream from DB for [" + packageDetailsKey
                            + "]");
                    }
                } else {
                    // this is  DownloadMode.FILESYSTEM - put the bits on the filesystem
                    File bitsFile = getPackageBitsLocalFilesystemFile(composite.getPackageVersionId(), composite
                        .getFileName());
                    if (!bitsFile.exists()) {
                        throw new RuntimeException("Package bits at [" + bitsFile + "] are missing for ["
                            + packageDetailsKey + "]");
                    }

                    bitsStream = new FileInputStream(bitsFile);
                }
            }

            // the magic happens here - outputStream is probably a remote stream down to the agent
            long bytesRetrieved;

            if (endByte < 0) {
                if (startByte > 0) {
                    bitsStream.skip(startByte);
                }

                bytesRetrieved = StreamUtil.copy(bitsStream, outputStream, false);
            } else {
                BufferedInputStream bis = new BufferedInputStream(bitsStream);
                long length = (endByte - startByte) + 1;
                bytesRetrieved = StreamUtil.copy(bis, outputStream, startByte, length);
            }

            // close our stream but leave the output stream open
            try {
                bitsStream.close();
            } catch (Exception closeError) {
                log.warn("Failed to close the bits stream", closeError);
            }

            bitsStream = null;

            log.debug("Retrieved and sent [" + bytesRetrieved + "] bytes for [" + packageDetailsKey + "]");

            return bytesRetrieved;
        } catch (SQLException sql) {
            throw new RuntimeException("Did not download the package bits to the DB for [" + packageDetailsKey + "]",
                sql);
        } catch (Exception e) {
            throw new RuntimeException("Could not stream package bits for [" + packageDetailsKey + "]", e);
        } finally {
            if (bitsStream != null) {
                try {
                    bitsStream.close();
                } catch (IOException e) {
                    log.warn("Failed to close bits stream for: " + packageDetailsKey);
                }
            }

            if (results != null) {
                try {
                    results.close();
                } catch (SQLException e) {
                    log.warn("Failed to close result set from jdbc blob query for: " + packageDetailsKey);
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.warn("Failed to close prepared statement from jdbc blob query for: " + packageDetailsKey);
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Failed to close prepared statement from jdbc blob query for: " + packageDetailsKey);
                }
            }
        }
    }

    /**
     * Compares an attribute o1 from a package version pv1 with the same attribute whose value is o2 from a package
     * version pv2. First checks that o1 is not <code>null</code>, if it is, returns <code>true</code> if o2 is <code>
     * null</code>. Otherwise, just calls o1.equals(o2). Will issue a WARN log message if they are not equal.
     *
     * @param  pv1
     * @param  o1
     * @param  pv2
     * @param  o2
     * @param  logMsg
     *
     * @return if o1 and o2 are equal
     */
    private boolean packageVersionAttributeCheck(PackageVersion pv1, Object o1, PackageVersion pv2, Object o2,
        String logMsg) {
        boolean same;

        if (o1 == null) {
            same = (o2 == null);
        } else {
            same = o1.equals(o2);
        }

        if (!same) {
            StringBuilder str = new StringBuilder();
            str.append("A new package version has data that is different than a previous package version. ");
            str.append("The new package version data will take effect and overwrite the old version: ");
            str.append(logMsg);
            str.append(": package-version1=[").append(pv1);
            str.append("] value1=[").append(o1);
            str.append("; package-version2=[").append(pv2);
            str.append("] value2=[").append(o2);
            str.append("]");
            log.warn(str.toString());
        }

        return same;
    }

    private File getPackageBitsLocalFilesystemFile(int packageVersionId, String fileName) throws Exception {
        final String filesystemProperty = "rhq.server.content.filesystem";
        String filesystem = System.getProperty(filesystemProperty);

        if (filesystem == null) {
            throw new IllegalStateException("Server is misconfigured - missing system property '" + filesystemProperty
                + "'. Don't know where package bits are stored.");
        }

        // allow the configuration to use ${} system property replacement strings
        filesystem = StringPropertyReplacer.replaceProperties(filesystem);

        // to avoid putting everything in one big directory, but to also avoid requiring
        // knowlege of the content source that downloaded the bits, let's put bits in
        // groups based on their package version IDs so we have no more than 2000 files
        // in any one group directory.  If you know the package version, you will be able to
        // uniquely identify where in the filesystem the package bits are.
        String idGroup = String.valueOf(packageVersionId / 2000);

        // technically I don't need the file name - the package version id is the unique part.
        // but I envision for support or debug purposes, we'll want to see the filename. "real"
        // file systems have a limit on the size of the filename of 255, so we'll make sure
        // they are never more than that.
        StringBuilder bitsFileName = new StringBuilder();
        bitsFileName.append(packageVersionId).append('-').append(fileName);
        if (bitsFileName.length() > 255) {
            bitsFileName.setLength(255);
        }

        File parentDir = new File(filesystem, idGroup);
        File packageBitsFile = new File(parentDir, bitsFileName.toString());

        if (!parentDir.isDirectory()) {
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (!parentDir.isDirectory()) {
                throw new Exception("Cannot create content filesystem directory [" + parentDir
                    + "] for package bits storage.");
            }
        }

        return packageBitsFile;
    }
}