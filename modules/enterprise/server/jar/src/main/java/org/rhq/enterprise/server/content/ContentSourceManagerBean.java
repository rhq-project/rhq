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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
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
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.util.StringPropertyReplacer;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.AdvisoryBuglist;
import org.rhq.core.domain.content.AdvisoryCVE;
import org.rhq.core.domain.content.AdvisoryPackage;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.DistributionType;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageBitsBlob;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.PackageVersionContentSourcePK;
import org.rhq.core.domain.content.ProductVersionPackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoAdvisory;
import org.rhq.core.domain.content.RepoContentSource;
import org.rhq.core.domain.content.RepoDistribution;
import org.rhq.core.domain.content.RepoPackageVersion;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.domain.content.composite.LoadedPackageBitsComposite;
import org.rhq.core.domain.content.composite.PackageVersionFile;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PasswordObfuscationUtility;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryBugDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryCVEDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisorySyncReport;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderManager;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetailsKey;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.DistributionDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionFileDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSource;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.InitializationException;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.RepoDetails;
import org.rhq.enterprise.server.resource.ProductVersionManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A SLSB wrapper around our server-side content source plugin container. This bean provides access to the
 * {@link ContentSource} objects deployed in the server, thus allows the callers to access data about and from remote
 * content repositories.
 *
 * @author John Mazzitelli
 */
// TODO: all authz checks need to be more fine grained... entitlements need to plug into here somehow?
@Stateless
public class ContentSourceManagerBean implements ContentSourceManagerLocal {
    /**
     * The location we store the bits and distro files
     */
    public static final String FILESYSTEM_PROPERTY = "rhq.server.content.filesystem";

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
    @EJB
    private RepoManagerLocal repoManager;

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void purgeOrphanedPackageVersions(Subject subject) {
        // get all orphaned package versions that have extra props, we need to delete the configs
        // separately. We do this using em.remove so we can get hibernate to perform the cascading for us.
        // Package versions normally do not have extra props, so we gain the advantage of using hibernate
        // to do the cascade deletes without incurring too much overhead in performing multiple removes.
        Query q = entityManager.createNamedQuery(PackageVersion.FIND_EXTRA_PROPS_IF_NO_CONTENT_SOURCES_OR_REPOS);
        List<PackageVersion> pvs = q.getResultList();
        for (PackageVersion pv : pvs) {
            entityManager.remove(pv.getExtraProperties());
            pv.setExtraProperties(null);
        }

        // Remove the bits on the filesystem if some were downloaded by a content source with DownloadMode.FILESYSTEM.
        // Query the package bits table and get the package bits where bits column is null - for those get the
        // related package versions and given the package version/filename you can get the files to delete.
        // Do not delete the files yet - just get the (package version ID, filename) composite list.
        q = entityManager.createNamedQuery(PackageVersion.FIND_FILES_IF_NO_CONTENT_SOURCES_OR_REPOS);
        List<PackageVersionFile> pvFiles = q.getResultList();

        // get ready for bulk delete by clearing entity manager
        entityManager.flush();
        entityManager.clear();

        // remove the productVersion->packageVersion mappings for all orphaned package versions
        entityManager.createNamedQuery(PackageVersion.DELETE_PVPV_IF_NO_CONTENT_SOURCES_OR_REPOS).executeUpdate();

        // remove the orphaned package versions
        int count = entityManager.createNamedQuery(PackageVersion.DELETE_IF_NO_CONTENT_SOURCES_OR_REPOS)
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
                File doomed = getPackageBitsLocalFileAndCreateParentDir(pvFile.getPackageVersionId(), pvFile
                    .getFileName());
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

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void deleteContentSource(Subject subject, int contentSourceId) {
        log.debug("User [" + subject + "] is deleting content source [" + contentSourceId + "]");

        // bulk delete m-2-m mappings to the doomed content source
        // get ready for bulk delete by clearing entity manager
        entityManager.flush();
        entityManager.clear();

        entityManager.createNamedQuery(RepoContentSource.DELETE_BY_CONTENT_SOURCE_ID).setParameter("contentSourceId",
            contentSourceId).executeUpdate();

        entityManager.createNamedQuery(PackageVersionContentSource.DELETE_BY_CONTENT_SOURCE_ID).setParameter(
            "contentSourceId", contentSourceId).executeUpdate();

        ContentSource cs = entityManager.find(ContentSource.class, contentSourceId);
        if (cs != null) {
            if (cs.getConfiguration() != null) {
                entityManager.remove(cs.getConfiguration());
            }

            List<ContentSourceSyncResults> results = cs.getSyncResults();
            if (results != null) {
                int[] ids = new int[results.size()];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = results.get(i).getId();
                }

                this.deleteContentSourceSyncResults(subject, ids);
            }

            entityManager.remove(cs);
            log.debug("User [" + subject + "] deleted content source [" + cs + "]");

            repoManager.deleteCandidatesWithOnlyContentSource(subject, contentSourceId);

            // make sure we stop its adapter and unschedule any sync job associated with it
            try {
                ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
                pc.unscheduleProviderSyncJob(cs);
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
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
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
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public PageList<ContentSource> getAvailableContentSourcesForRepo(Subject subject, Integer repoId, PageControl pc) {
        pc.initDefaultOrderingField("cs.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ContentSource.QUERY_FIND_AVAILABLE_BY_REPO_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            ContentSource.QUERY_FIND_AVAILABLE_BY_REPO_ID);

        query.setParameter("repoId", repoId);
        countQuery.setParameter("repoId", repoId);

        List<ContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ContentSource>(results, (int) count, pc);
    }

    public ContentSourceType getContentSourceType(String name) {
        Query q = entityManager.createNamedQuery(ContentSourceType.QUERY_FIND_BY_NAME_WITH_CONFIG_DEF);
        q.setParameter("name", name);
        ContentSourceType type = null;
        try {
            type = (ContentSourceType) q.getSingleResult();
        } catch (NoResultException e) {
            type = null;
        }
        return type;
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
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

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
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
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public PageList<Repo> getAssociatedRepos(Subject subject, int contentSourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Repo.QUERY_FIND_IMPORTED_BY_CONTENT_SOURCE_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            Repo.QUERY_FIND_IMPORTED_BY_CONTENT_SOURCE_ID);

        query.setParameter("id", contentSourceId);
        countQuery.setParameter("id", contentSourceId);

        List<Repo> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Repo>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public PageList<Repo> getCandidateRepos(Subject subject, int contentSourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Repo.QUERY_FIND_CANDIDATE_BY_CONTENT_SOURCE_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            Repo.QUERY_FIND_CANDIDATE_BY_CONTENT_SOURCE_ID);

        query.setParameter("id", contentSourceId);
        countQuery.setParameter("id", contentSourceId);

        @SuppressWarnings("unchecked")
        List<Repo> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Repo>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
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

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void mergeRepoImportResults(List<RepoDetails> repos) {

        Subject overlord = subjectManager.getOverlord();

        for (RepoDetails createMe : repos) {

            String repoName = createMe.getName();

            // Make sure the repo doesn't already exist. If we add twice, currently we'll get an exception
            List<Repo> existingRepo = repoManager.getRepoByName(repoName);
            if (existingRepo != null) {
                continue;
            }

            Repo repo = new Repo(repoName);
            repo.setCandidate(false);
            repo.setDescription(createMe.getDescription());

            try {
                repoManager.createRepo(overlord, repo);
            } catch (RepoException e) {
                log.error("Error creating repo [" + repo + "]", e);
            }
        }

    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void deleteContentSourceSyncResults(Subject subject, int[] ids) {
        if (ids != null) {
            for (int id : ids) {
                ContentSourceSyncResults doomed = entityManager.find(ContentSourceSyncResults.class, id);
                entityManager.remove(doomed);
            }
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public ContentSource createContentSource(Subject subject, ContentSource contentSource)
        throws ContentSourceException {

        validateContentSource(contentSource);

        log.debug("User [" + subject + "] is creating content source [" + contentSource + "]");

        // now that a new content source has been added to the system, let's start its adapter now
        try {
            ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
            pc.getAdapterManager().startAdapter(contentSource);
            // Schedule a job for the future
            pc.scheduleProviderSyncJob(contentSource);
            // Also sync immediately so we have the metadata
            pc.syncProviderNow(contentSource);

        } catch (InitializationException ie) {
            log.warn("Failed to start adapter for [" + contentSource + "]", ie);
            throw new ContentSourceException("Failed to start adapter for [" + contentSource + "]. Cause: "
                + ThrowableUtil.getAllMessages(ie));
        } catch (Exception e) {
            log.warn("Failed to start adapter for [" + contentSource + "]", e);
        }

        obfuscatePasswords(contentSource);

        entityManager.persist(contentSource);
        // these aren't cascaded during persist, but I want to set them to null anyway, just to be sure
        contentSource.setSyncResults(null);

        log.debug("User [" + subject + "] created content source [" + contentSource + "]");
        return contentSource; // now has the ID set
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public ContentSource simpleCreateContentSource(Subject subject, ContentSource contentSource)
        throws ContentSourceException {
        validateContentSource(contentSource);
        contentSource.setSyncResults(new ArrayList<ContentSourceSyncResults>());

        obfuscatePasswords(contentSource);

        entityManager.persist(contentSource);
        return contentSource;
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public ContentSource updateContentSource(Subject subject, ContentSource contentSource, boolean syncNow)
        throws ContentSourceException {

        log.debug("User [" + subject + "] is updating content source [" + contentSource + "]");

        ContentSource loaded = entityManager.find(ContentSource.class, contentSource.getId());

        obfuscatePasswords(contentSource);

        if (contentSource.getConfiguration() == null) {
            // this is a one-to-one and hibernate can't auto delete this orphan (HHH-2608), we manually do it here
            if (loaded.getConfiguration() != null) {
                entityManager.remove(loaded.getConfiguration());
            }
        }

        // before we merge the change, look to see if the name is changing because if it is
        // we need to unschedule the sync job due to the fact that the job data has the name in it.
        if (!loaded.getName().equals(contentSource.getName())) {
            log.info("Content source [" + loaded.getName() + "] is being renamed to [" + contentSource.getName()
                + "].  Will now unschedule the old sync job");
            try {
                ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
                pc.unscheduleProviderSyncJob(loaded);
            } catch (Exception e) {
                log.warn("Failed to unschedule obsolete content source sync job for [" + loaded + "]", e);
            }
        }

        // now we can merge the changes to the database
        contentSource = entityManager.merge(contentSource);
        log.debug("User [" + subject + "] updated content source [" + contentSource + "]");

        // now that the content source has been changed,
        // restart its adapter and reschedule its sync job because the config might have changed.
        try {
            ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
            pc.unscheduleProviderSyncJob(contentSource);
            pc.getAdapterManager().restartAdapter(contentSource);
            pc.scheduleProviderSyncJob(contentSource);
            if (syncNow) {
                pc.syncProviderNow(contentSource);
            }
        } catch (Exception e) {
            log.warn("Failed to restart adapter for [" + contentSource + "]", e);
        }

        return contentSource;
    }

    @SuppressWarnings("unchecked")
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

        List<ContentSource> existingMatchingContentSources = q.getResultList();
        if (existingMatchingContentSources.size() > 0) {
            throw new ContentSourceException("Content source with name [" + name + "] and of type [" + type.getName()
                + "] already exists, please specify a different name.");
        }

    }

    public void testContentSourceConnection(int contentSourceId) throws Exception {
        try {
            ContentServerPluginContainer contentServerPluginContainer = ContentManagerHelper.getPluginContainer();
            contentServerPluginContainer.getAdapterManager().testConnection(contentSourceId);
        } catch (Exception e) {
            log.info("Failed to test connection to [" + contentSourceId + "]. Cause: "
                + ThrowableUtil.getAllMessages(e));
            log.debug("Content source test connection failure stack follows for [" + contentSourceId + "]", e);
            throw e;
        }
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void synchronizeAndLoadContentSource(Subject subject, int contentSourceId) {
        try {
            ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
            ContentSource contentSource = entityManager.find(ContentSource.class, contentSourceId);

            if (contentSource != null) {
                pc.syncProviderNow(contentSource);
            } else {
                log.warn("Asked to synchronize a non-existing content source [" + contentSourceId + "]");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not spawn the sync job for content source [" + contentSourceId + "]");
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
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

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public List<PackageVersionContentSource> getPackageVersionsFromContentSourceForRepo(Subject subject,
        int contentSourceId, int repoId) {

        Query query = entityManager
            .createNamedQuery(PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_REPO_ID);
        query.setParameter("content_source_id", contentSourceId);
        query.setParameter("repo_id", repoId);

        List<PackageVersionContentSource> results = query.getResultList();

        return results;
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
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
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
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
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public PageList<PackageVersionContentSource> getUnloadedPackageVersionsFromContentSourceInRepo(Subject subject,
        int contentSourceId, int repoId, PageControl pc) {
        pc.initDefaultOrderingField("pvcs.contentSource.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_NOT_LOADED, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_NOT_LOADED_COUNT);

        query.setParameter("id", contentSourceId);
        query.setParameter("repo_id", repoId);
        countQuery.setParameter("id", contentSourceId);
        countQuery.setParameter("repo_id", repoId);

        List<PackageVersionContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();
        PageList<PackageVersionContentSource> dbList = new PageList<PackageVersionContentSource>(results, (int) count,
            pc);
        // Setup a HashSet so we can add missing files to the results list without getting dupes in the Set
        // Then translate at the end to a List.
        HashSet<PackageVersionContentSource> uniquePVs = new HashSet<PackageVersionContentSource>();
        uniquePVs.addAll(dbList);

        ContentSource contentSource = entityManager.find(ContentSource.class, contentSourceId);
        // Only check if it is a FILESYSTEM backed contentsource
        if (contentSource.getDownloadMode().equals(DownloadMode.FILESYSTEM)) {
            List<PackageVersionContentSource> allPackageVersions = contentSourceManager
                .getPackageVersionsFromContentSource(subject, contentSourceId, pc);
            for (PackageVersionContentSource item : allPackageVersions) {
                PackageVersion pv = item.getPackageVersionContentSourcePK().getPackageVersion();
                File verifyFile = getPackageBitsLocalFilesystemFile(pv.getId(), pv.getFileName());
                if (!verifyFile.exists()) {
                    log.info("Missing file from ContentProvider, adding to list: " + verifyFile.getAbsolutePath());
                    uniquePVs.add(item);
                }
            }
        }
        // Take the hit and convert to a List
        return new PageList<PackageVersionContentSource>(uniquePVs, pc);
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(45 * 60)
    public void downloadDistributionBits(Subject subject, ContentSource contentSource) {
        try {
            log.debug("downloadDistributionBits invoked");
            DistributionManagerLocal distManager = LookupUtil.getDistributionManagerLocal();
            ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
            int contentSourceId = contentSource.getId();
            ContentProviderManager cpMgr = pc.getAdapterManager();
            ContentProvider provider = cpMgr.getIsolatedContentProvider(contentSource.getId());
            if (!(provider instanceof DistributionSource)) {
                return;
            }

            DistributionSource distSource = (DistributionSource) provider;

            //
            // Following same sort of workaround done in ContentProviderManager for synchronizeContentProvider
            // Assume this will need to be updated when we place syncing in repo layer
            //
            final RepoCriteria reposForContentSource = new RepoCriteria();
            reposForContentSource.addFilterContentSourceIds(contentSourceId);
            reposForContentSource.addFilterCandidate(false); // Don't sync distributions for candidates

            final Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            //Use CriteriaQuery to automatically chunk/page through criteria query results
            CriteriaQueryExecutor<Repo, RepoCriteria> queryExecutor = new CriteriaQueryExecutor<Repo, RepoCriteria>() {
                @Override
                public PageList<Repo> execute(RepoCriteria criteria) {
                    return repoManager.findReposByCriteria(overlord, reposForContentSource);
                }
            };

            CriteriaQuery<Repo, RepoCriteria> repos = new CriteriaQuery<Repo, RepoCriteria>(reposForContentSource,
                queryExecutor);

            int repoCount = 0;
            for (Repo repo : repos) {
                repoCount++;
                log.debug("downloadDistributionBits operating on repo: " + repo.getName() + " id = " + repo.getId());
                // Look up Distributions associated with this ContentSource.
                PageControl pageControl = PageControl.getUnlimitedInstance();
                log.debug("Looking up existing distributions for repoId: " + repo.getId());
                List<Distribution> dists = repoManager.findAssociatedDistributions(overlord, repo.getId(), pageControl);
                log.debug("Found " + dists.size() + " Distributions for repoId " + repo.getId());

                for (Distribution dist : dists) {
                    log.debug("Looking up DistributionFiles for dist: " + dist);
                    List<DistributionFile> distFiles = distManager.getDistributionFilesByDistId(dist.getId());
                    log.debug("Found " + distFiles.size() + " DistributionFiles");
                    for (DistributionFile dFile : distFiles) {
                        String relPath = dist.getBasePath() + "/" + dFile.getRelativeFilename();
                        File outputFile = getDistLocalFileAndCreateParentDir(dist.getLabel(), relPath);
                        log.debug("Checking if file exists at: " + outputFile.getAbsolutePath());
                        if (outputFile.exists()) {
                            log.debug("File " + outputFile.getAbsolutePath() + " exists, checking md5sum");
                            String expectedMD5 = (dFile.getMd5sum() != null) ? dFile.getMd5sum() : "<unspecified MD5>";
                            String actualMD5 = MessageDigestGenerator.getDigestString(outputFile);
                            if (!expectedMD5.trim().toLowerCase().equals(actualMD5.toLowerCase())) {
                                log.error("Expected [" + expectedMD5 + "] versus actual [" + actualMD5
                                    + "] md5sums for file " + outputFile.getAbsolutePath() + " do not match.");
                                log.error("Need to re-fetch file.  Will download from DistributionSource"
                                    + " and overwrite local file.");
                            } else {
                                log.info(outputFile + " exists and md5sum matches [" + actualMD5
                                    + "] no need to re-download");
                                continue; // skip the download from bitsStream
                            }
                        }
                        log.debug("Attempting download of " + dFile.getRelativeFilename() + " from contentSourceId "
                            + contentSourceId);
                        String remoteFetchLoc = distSource.getDistFileRemoteLocation(repo.getName(), dist.getLabel(),
                            dFile.getRelativeFilename());
                        InputStream bitsStream = pc.getAdapterManager().loadDistributionFileBits(contentSourceId,
                            remoteFetchLoc);
                        StreamUtil.copy(bitsStream, new FileOutputStream(outputFile), true);
                        bitsStream = null;
                        log.debug("DistributionFile has been downloaded to: " + outputFile.getAbsolutePath());
                    }
                }
            }
            log.debug("downloadDistributionBits found and processed " + repoCount
                + " repos associated with this contentSourceId "
                + contentSourceId);
        } catch (Throwable t) {
            log.error(t);
            throw new RuntimeException(t);
        }
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @TransactionTimeout(90 * 60)
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
            ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
            bitsStream = pc.getAdapterManager().loadPackageBits(contentSourceId, packageVersionLocation);

            Connection conn = null;
            PreparedStatement ps = null;
            PreparedStatement ps2 = null;
            try {
                packageBits = createPackageBits(pk.getContentSource().getDownloadMode() == DownloadMode.DATABASE);

                PackageVersion pv = entityManager.find(PackageVersion.class, packageVersionId);
                pv.setPackageBits(packageBits); // associate the entities
                entityManager.flush(); // may not be necessary

                if (pk.getContentSource().getDownloadMode() == DownloadMode.DATABASE) {
                    conn = dataSource.getConnection();
                    // The blob has been initialized to EMPTY_BLOB already by createPackageBits...
                    // we need to lock the row which will be updated so we are using FOR UPDATE
                    ps = conn.prepareStatement("SELECT BITS FROM " + PackageBits.TABLE_NAME
                        + " WHERE ID = ? FOR UPDATE");
                    ps.setInt(1, packageBits.getId());
                    ResultSet rs = ps.executeQuery();
                    try {
                        while (rs.next()) {
                            //We can not create a blob directly because BlobImpl from Hibernate is not acceptable
                            //for oracle and Connection.createBlob is not working on postgres.
                            //This blob will be not empty because we saved there a bytes from String("a").
                            Blob blb = rs.getBlob(1);

                            StreamUtil.copy(bitsStream, blb.setBinaryStream(1), true);
                            ps2 = conn.prepareStatement("UPDATE " + PackageBits.TABLE_NAME
                                + " SET bits = ? where id = ?");
                            ps2.setBlob(1, blb);
                            ps2.setInt(2, packageBits.getId());
                            if (ps2.execute()) {
                                throw new Exception("Did not download the package bits to the DB for ");
                            }
                            ps2.close();
                        }
                    } finally {
                        rs.close();
                    }
                    ps.close();
                    conn.close();

                } else {
                    // store content to local file system
                    File outputFile = getPackageBitsLocalFileAndCreateParentDir(pv.getId(), pv.getFileName());
                    log.info("OutPutFile is located at: " + outputFile);
                    boolean download = false;

                    if (outputFile.exists()) {
                        // hmmm... it already exists, maybe we already have it?
                        // if the MD5's match, just ignore this download request and continue on
                        // If they are different we re-download
                        String expectedMD5 = (pv.getMD5() != null) ? pv.getMD5() : "<unspecified MD5>";
                        String actualMD5 = MessageDigestGenerator.getDigestString(outputFile);
                        if (!expectedMD5.trim().toLowerCase().equals(actualMD5.toLowerCase())) {
                            log.error("Already have package bits for [" + pv + "] located at [" + outputFile
                                + "] but the MD5 hashcodes do not match. Expected MD5=[" + expectedMD5
                                + "], Actual MD5=[" + actualMD5 + "] - redownloading package");
                            download = true;
                        } else {
                            log.info("Asked to download package bits but we already have it at [" + outputFile
                                + "] with matching MD5 of [" + actualMD5 + "]");
                            download = false;
                        }
                    } else {
                        download = true;
                    }
                    if (download) {
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

                if (ps2 != null) {
                    try {
                        ps2.close();
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

    /**
     * This creates a new PackageBits entity initialized to EMPTY_BLOB for the associated PackageBitsBlob.
     * Note that PackageBits and PackageBitsBlob are two entities that *share* the same db row.  This is
     * done to allow for Lazy load semantics on the Lob.  Hibernate does not honor field-level Lazy load
     * on a Lob unless the entity class is instrumented. We can't usethat approach because it introduces
     * hibernate imports into the domain class, and that violates our restriction of exposing hibernate
     * classes to the Agent and Remote clients.
     *
     * @return
     */
    private PackageBits createPackageBits(boolean initialize) {
        PackageBits bits = null;
        PackageBitsBlob blob = null;

        // We have to work backwards to avoid constraint violations. PackageBits requires a PackageBitsBlob,
        // so create and persist that first, getting the ID
        blob = new PackageBitsBlob();
        if (initialize) {
            blob.setBits(PackageBits.EMPTY_BLOB.getBytes());
        }
        entityManager.persist(blob);

        // Now create the PackageBits entity and assign the Id and blob.  Note, do not persist the
        // entity, the row already exists. Just perform and flush the update.
        bits = new PackageBits();
        bits.setId(blob.getId());
        bits.setBlob(blob);
        entityManager.flush();

        // return the new PackageBits and associated PackageBitsBlob
        return bits;
    }

    private InputStream preloadPackageBits(PackageVersionContentSource pvcs) throws Exception {
        PackageVersionContentSourcePK pk = pvcs.getPackageVersionContentSourcePK();
        int contentSourceId = pk.getContentSource().getId();
        int packageVersionId = pk.getPackageVersion().getId();
        String packageVersionLocation = pvcs.getLocation();

        ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
        InputStream bitsStream = pc.getAdapterManager().loadPackageBits(contentSourceId, packageVersionLocation);

        PackageVersion pv = entityManager.find(PackageVersion.class, packageVersionId);

        switch (pk.getContentSource().getDownloadMode()) {
        case NEVER: {
            return null; // no-op, our content source was told to never download package bits
        }

        case DATABASE: {
            log.debug("Downloading package bits to DB for package located at [" + packageVersionLocation
                + "] on content source [" + contentSourceId + "]");
            File tempFile = File.createTempFile("JonContent", "");
            tempFile.deleteOnExit();
            OutputStream tempStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            StreamUtil.copy(bitsStream, tempStream, true);
            InputStream inp = new BufferedInputStream(new FileInputStream(tempFile));
            return inp;
        }

        case FILESYSTEM: {
            log.debug("Downloading package bits to filesystem for package located at [" + packageVersionLocation
                + "] on content source [" + contentSourceId + "]");
            File outputFile = getPackageBitsLocalFileAndCreateParentDir(pv.getId(), pv.getFileName());
            log.info("OutPutFile is located at: " + outputFile);
            boolean download = false;

            if (outputFile.exists()) {
                // hmmm... it already exists, maybe we already have it?
                // if the MD5's match, just ignore this download request and continue on
                // If they are different we re-download
                String expectedMD5 = (pv.getMD5() != null) ? pv.getMD5() : "<unspecified MD5>";
                String actualMD5 = MessageDigestGenerator.getDigestString(outputFile);
                if (!expectedMD5.trim().toLowerCase().equals(actualMD5.toLowerCase())) {
                    log.error("Already have package bits for [" + pv + "] located at [" + outputFile
                        + "] but the MD5 hashcodes do not match. Expected MD5=[" + expectedMD5 + "], Actual MD5=["
                        + actualMD5 + "] - redownloading package");
                    download = true;
                } else {
                    log.info("Asked to download package bits but we already have it at [" + outputFile
                        + "] with matching MD5 of [" + actualMD5 + "]");
                    download = false;
                }
            } else {
                download = true;
            }
            if (download) {
                StreamUtil.copy(bitsStream, new FileOutputStream(outputFile), true);
                bitsStream = null;
            }
            break;
        }

        default: {
            throw new IllegalStateException(" Unknown download mode - this is a bug, please report it: " + pvcs);
        }
        }

        return null;
    }

    // TODO: Just noticing that this method seems pretty redundant with
    //       downloadPackageBits(Subject subject, PackageVersionContentSource pvcs) and should probably be
    //       refactored.  Also *** the transactional decls below are not being honored because the method
    //       is not being called through the Local, so not establishing a new transactional context.
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(40 * 60)
    private PackageBits preparePackageBits(Subject subject, InputStream bitsStream, PackageVersionContentSource pvcs) {
        PackageVersionContentSourcePK pk = pvcs.getPackageVersionContentSourcePK();
        int contentSourceId = pk.getContentSource().getId();
        int packageVersionId = pk.getPackageVersion().getId();
        String packageVersionLocation = pvcs.getLocation();

        PackageBits packageBits = null;

        try {

            Connection conn = null;
            PreparedStatement ps = null;
            PreparedStatement ps2 = null;
            try {
                packageBits = createPackageBits(pk.getContentSource().getDownloadMode() == DownloadMode.DATABASE);

                PackageVersion pv = entityManager.find(PackageVersion.class, packageVersionId);
                pv.setPackageBits(packageBits); // associate entities
                entityManager.flush(); // not sure this is necessary

                if (pk.getContentSource().getDownloadMode() == DownloadMode.DATABASE) {
                    packageBits = entityManager.find(PackageBits.class, packageBits.getId());

                    conn = dataSource.getConnection();
                    //we are loading the PackageBits saved in the previous step
                    //we need to lock the row which will be updated so we are using FOR UPDATE
                    ps = conn.prepareStatement("SELECT BITS FROM " + PackageBits.TABLE_NAME
                        + " WHERE ID = ? FOR UPDATE");
                    ps.setInt(1, packageBits.getId());
                    ResultSet rs = ps.executeQuery();
                    try {
                        while (rs.next()) {

                            //We can not create a blob directly because BlobImpl from Hibernate is not acceptable
                            //for oracle and Connection.createBlob is not working on postgres.
                            //This blob will be not empty because we saved there a bytes from String("a").
                            Blob blb = rs.getBlob(1);

                            StreamUtil.copy(bitsStream, blb.setBinaryStream(1), false);
                            bitsStream.close();
                            ps2 = conn.prepareStatement("UPDATE " + PackageBits.TABLE_NAME
                                + " SET bits = ? where id = ?");
                            ps2.setBlob(1, blb);
                            ps2.setInt(2, packageBits.getId());
                            if (ps2.execute()) {
                                throw new Exception("Did not download the package bits to the DB for ");
                            }
                            ps2.close();
                        }
                    } finally {
                        rs.close();
                    }
                    ps.close();
                    conn.close();

                } else {
                    //CONTENT IS ALLREADY LOADED
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

                if (ps2 != null) {
                    try {
                        ps2.close();
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
        ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
        ContentProviderManager contentProviderManager = pc.getAdapterManager();
        return contentProviderManager.synchronizeContentProvider(contentSourceId);
    }

    public ContentSourceSyncResults persistContentSourceSyncResults(ContentSourceSyncResults results) {
        ContentManagerHelper helper = new ContentManagerHelper(entityManager);
        Query q = entityManager.createNamedQuery(ContentSourceSyncResults.QUERY_GET_INPROGRESS_BY_CONTENT_SOURCE_ID);
        q.setParameter("contentSourceId", results.getContentSource().getId());

        return (ContentSourceSyncResults) helper.persistSyncResults(q, results);
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
    public RepoSyncResults mergeDistributionSyncReport(ContentSource contentSource, DistributionSyncReport report,
        RepoSyncResults syncResults) {
        try {
            StringBuilder progress = new StringBuilder();
            if (syncResults.getResults() != null) {
                progress.append(syncResults.getResults());
            }

            //////////////////
            // REMOVE
            syncResults = contentSourceManager._mergeDistributionSyncReportREMOVE(contentSource, report, syncResults,
                progress);

            //////////////////
            // ADD
            syncResults = contentSourceManager._mergeDistributionSyncReportADD(contentSource, report, syncResults,
                progress);

            // if we added/updated/deleted anything, change the last modified time of all repos
            // that get content from this content source
            if ((report.getDistributions().size() > 0) || (report.getDeletedDistributions().size() > 0)) {
                contentSourceManager._mergePackageSyncReportUpdateRepo(contentSource.getId());
            }

            // let our sync results object know that we completed the merge
            // don't mark it as successful yet, let the caller do that
            progress.append(new Date()).append(": ").append("MERGE COMPLETE.\n");
            syncResults.setResults(progress.toString());
            syncResults = repoManager.mergeRepoSyncResults(syncResults);
        } catch (Throwable t) {
            // ThrowableUtil will dump SQL nextException messages, too
            String errorMsg = "Could not process sync report from [" + contentSource + "]. Cause: "
                + ThrowableUtil.getAllMessages(t);
            log.error(errorMsg, t);
            throw new RuntimeException(errorMsg, t);
        }

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    // we really want NEVER, but support tests that might be in a tx
    public RepoSyncResults mergePackageSyncReport(ContentSource contentSource, Repo repo, PackageSyncReport report,
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> previous, RepoSyncResults syncResults) {
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
            syncResults = contentSourceManager._mergePackageSyncReportREMOVE(contentSource, repo, report, previous,
                syncResults, progress);

            //////////////////
            // ADD
            List<ContentProviderPackageDetails> newPackages;
            newPackages = new ArrayList<ContentProviderPackageDetails>(report.getNewPackages());

            int chunkSize = 200;
            int fromIndex = 0;
            int toIndex = chunkSize;
            int newPackageCount = newPackages.size();
            int addedCount = 0; // running tally of what we actually added into DB

            progress.append(new Date()).append(": ").append("Adding");
            syncResults.setResults(progress.toString());
            syncResults = repoManager.mergeRepoSyncResults(syncResults);

            while (fromIndex < newPackageCount) {
                if (toIndex > newPackageCount) {
                    toIndex = newPackageCount;
                }

                List<ContentProviderPackageDetails> pkgs = newPackages.subList(fromIndex, toIndex);
                syncResults = contentSourceManager._mergePackageSyncReportADD(contentSource, repo, pkgs, previous,
                    syncResults, progress, fromIndex);
                addedCount += pkgs.size();
                fromIndex += chunkSize;
                toIndex += chunkSize;
            }

            progress.append("...").append(addedCount).append('\n');
            syncResults.setResults(progress.toString());
            syncResults = repoManager.mergeRepoSyncResults(syncResults);

            //////////////////
            // UPDATE
            syncResults = contentSourceManager._mergePackageSyncReportUPDATE(contentSource, report, previous,
                syncResults, progress);

            // if we added/updated/deleted anything, change the last modified time of all repos
            // that get content from this content source
            if ((report.getNewPackages().size() > 0) || (report.getUpdatedPackages().size() > 0)
                || (report.getDeletedPackages().size() > 0)) {
                contentSourceManager._mergePackageSyncReportUpdateRepo(contentSource.getId());
            }

            // let our sync results object know that we completed the merge
            // don't mark it as successful yet, let the caller do that
            progress.append(new Date()).append(": ").append("MERGE COMPLETE.\n");
            syncResults.setResults(progress.toString());
            syncResults = repoManager.mergeRepoSyncResults(syncResults);
        } catch (Throwable t) {
            // ThrowableUtil will dump SQL nextException messages, too
            String errorMsg = "Could not process sync report from [" + contentSource + "]. Cause: "
                + ThrowableUtil.getAllMessages(t);
            log.error(errorMsg, t);
            throw new RuntimeException(errorMsg, t);
        }

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    // we really want NEVER, but support tests that might be in a tx
    public RepoSyncResults mergeAdvisorySyncReport(ContentSource contentSource, AdvisorySyncReport report,
        RepoSyncResults syncResults) {
        try {
            StringBuilder progress = new StringBuilder();
            if (syncResults.getResults() != null) {
                progress.append(syncResults.getResults());
            }

            syncResults = contentSourceManager._mergeAdvisorySyncReportREMOVE(contentSource, report, syncResults,
                progress);

            syncResults = contentSourceManager
                ._mergeAdvisorySyncReportADD(contentSource, report, syncResults, progress);

            // if we added/updated/deleted anything, change the last modified time of all repos
            // that get content from this content source
            if ((report.getAdvisory().size() > 0) || (report.getDeletedAdvisorys().size() > 0)) {
                contentSourceManager._mergePackageSyncReportUpdateRepo(contentSource.getId());
            }

            // let our sync results object know that we completed the merge
            // don't mark it as successful yet, let the caller do that
            progress.append(new Date()).append(": ").append("MERGE COMPLETE.\n");
            syncResults.setResults(progress.toString());
            syncResults = repoManager.mergeRepoSyncResults(syncResults);
        } catch (Throwable t) {
            // ThrowableUtil will dump SQL nextException messages, too
            String errorMsg = "Could not process sync report from [" + contentSource + "]. Cause: "
                + ThrowableUtil.getAllMessages(t);
            log.error(errorMsg, t);
            throw new RuntimeException(errorMsg, t);
        }

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults _mergeAdvisorySyncReportADD(ContentSource contentSource, AdvisorySyncReport report,
        RepoSyncResults syncResults, StringBuilder progress) {

        AdvisoryManagerLocal advManager = LookupUtil.getAdvisoryManagerLocal();
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        List<AdvisoryDetails> newDetails = report.getAdvisory();
        for (AdvisoryDetails detail : newDetails) {
            try {
                Advisory newAdv = advManager.getAdvisoryByName(detail.getAdvisory());
                if (newAdv == null) {
                    // Advisory does not exist, create a new one
                    log.debug("Attempting to create new advisory based off of: " + detail);
                    newAdv = advManager.createAdvisory(overlord, detail.getAdvisory(), detail.getAdvisory_type(),
                        detail.getSynopsis());
                    newAdv.setAdvisory_name(detail.getAdvisory_name());
                    newAdv.setAdvisory_rel(detail.getAdvisory_rel());
                    newAdv.setDescription(detail.getDescription());
                    newAdv.setSolution(detail.getSolution());
                    newAdv.setIssue_date(detail.getIssue_date());
                    newAdv.setUpdate_date(detail.getUpdate_date());
                    newAdv.setTopic(detail.getTopic());
                    entityManager.flush();
                    entityManager.persist(newAdv);
                }

                Repo repo = repoManager.getRepo(overlord, report.getRepoId());
                RepoAdvisory repoAdv = new RepoAdvisory(repo, newAdv);
                log.debug("Created new mapping of RepoAdvisory repoId = " + repo.getId() + ", distId = "
                    + newAdv.getId());
                entityManager.flush();
                entityManager.persist(repoAdv);
                // persist pkgs associated with an errata
                List<AdvisoryPackageDetails> pkgs = detail.getPkgs();

                Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_PACKAGEVERSION_BY_FILENAME);
                for (AdvisoryPackageDetails pkg : pkgs) {
                    try {
                        q.setParameter("rpmName", pkg.getRpmFilename());
                        PackageVersion pExisting = (PackageVersion) q.getSingleResult();
                        AdvisoryPackage apkg = advManager.findAdvisoryPackage(overlord, newAdv.getId(), pExisting
                            .getId());
                        if (apkg == null) {
                            apkg = new AdvisoryPackage(newAdv, pExisting);
                            entityManager.persist(apkg);
                            entityManager.flush();
                        }
                    } catch (NoResultException nre) {
                        log.info("Advisory has package thats not yet in the db [" + pkg.getRpmFilename()
                            + "] - Processing rest");
                    }
                }
                //persist cves associated with an errata
                List<AdvisoryCVEDetails> cves = detail.getCVEs();
                log.debug("list of CVEs " + cves);
                if (cves != null && cves.size() > 0) {
                    for (AdvisoryCVEDetails cve : cves) {
                        AdvisoryCVE acve = new AdvisoryCVE(newAdv, advManager.createCVE(overlord, cve.getName()));
                        entityManager.persist(acve);
                        entityManager.flush();
                    }
                }

                List<AdvisoryBugDetails> abugs = detail.getBugs();
                log.debug("list of Bugs " + abugs);
                if (abugs != null && abugs.size() > 0) {
                    for (AdvisoryBugDetails abug : abugs) {
                        AdvisoryBuglist abuglist = advManager.getAdvisoryBuglist(overlord, newAdv.getId(), abug
                            .getBugInfo());
                        if (abuglist == null) {
                            abuglist = new AdvisoryBuglist(newAdv, abug.getBugInfo());
                            entityManager.persist(abuglist);
                            entityManager.flush();
                        }
                    }
                }

            } catch (AdvisoryException e) {
                progress.append("Caught exception when trying to add: " + detail.getAdvisory() + "\n");
                progress.append("Error is: " + e.getMessage());
                syncResults.setResults(progress.toString());
                syncResults = repoManager.mergeRepoSyncResults(syncResults);
                log.error(e);
            }
        }
        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults _mergeAdvisorySyncReportREMOVE(ContentSource contentSource, AdvisorySyncReport report,
        RepoSyncResults syncResults, StringBuilder progress) {

        progress.append(new Date()).append(": ").append("Removing");
        syncResults.setResults(progress.toString());
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        AdvisoryManagerLocal advManager = LookupUtil.getAdvisoryManagerLocal();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // remove all advisories that are no longer available on the remote repository
        for (AdvisoryDetails advDetails : report.getDeletedAdvisorys()) {
            Advisory nukeAdv = advManager.getAdvisoryByName(advDetails.getAdvisory());
            advManager.deleteAdvisoryCVE(overlord, nukeAdv.getId());
            advManager.deleteAdvisoryPackage(overlord, nukeAdv.getId());
            advManager.deleteAdvisoryBugList(overlord, nukeAdv.getId());
            advManager.deleteAdvisoryByAdvId(overlord, nukeAdv.getId());

            progress.append("Removed advisory & advisory cves for: " + advDetails.getAdvisory());
            syncResults.setResults(progress.toString());
            syncResults = repoManager.mergeRepoSyncResults(syncResults);
        }

        progress.append("Finished Advisory removal...").append('\n');
        syncResults.setResults(progress.toString());
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void _mergePackageSyncReportUpdateRepo(int contentSourceId) {
        // this method should be called only after a merge of a content source
        // added/updated/removed one or more packages.  When this happens, we need to change
        // the last modified time for all repos that get content from the changed content source
        long now = System.currentTimeMillis();
        ContentSource contentSource = entityManager.find(ContentSource.class, contentSourceId);
        Set<RepoContentSource> ccss = contentSource.getRepoContentSources();
        for (RepoContentSource ccs : ccss) {
            ccs.getRepoContentSourcePK().getRepo().setLastModifiedDate(now);
        }

        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults _mergePackageSyncReportREMOVE(ContentSource contentSource, Repo repo,
        PackageSyncReport report, Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> previous,
        RepoSyncResults syncResults, StringBuilder progress) {

        progress.append(new Date()).append(": ").append("Removing");
        syncResults.setResults(progress.toString());
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        Query q;
        int flushCount = 0; // used to know when we should flush the entity manager - for performance purposes
        int removeCount = 0;

        // remove all packages that are no longer available on the remote repository
        // for each removed package, we need to purge the PVCS mapping and the PV itself

        for (ContentProviderPackageDetails doomedDetails : report.getDeletedPackages()) {

            // Delete the mapping between package version and content source
            ContentProviderPackageDetailsKey doomedDetailsKey = doomedDetails.getContentProviderPackageDetailsKey();
            PackageVersionContentSource doomedPvcs = previous.get(doomedDetailsKey);
            doomedPvcs = entityManager.find(PackageVersionContentSource.class, doomedPvcs
                .getPackageVersionContentSourcePK());
            if (doomedPvcs != null) {
                entityManager.remove(doomedPvcs);
            }

            // Delete the relationship between package and repo IF no other providers provide the
            // package
            q = entityManager.createNamedQuery(RepoPackageVersion.DELETE_WHEN_NO_PROVIDER);
            q.setParameter("repoId", repo.getId());
            q.executeUpdate();

            // Delete the package version if it is sufficiently orphaned:
            // - No repos
            // - No content sources
            // - No installed packages
            PackageVersion doomedPv = doomedPvcs.getPackageVersionContentSourcePK().getPackageVersion();
            q = entityManager.createNamedQuery(PackageVersion.DELETE_SINGLE_IF_NO_CONTENT_SOURCES_OR_REPOS);
            q.setParameter("packageVersionId", doomedPv.getId());
            q.executeUpdate();

            if ((++flushCount % 200) == 0) {
                entityManager.flush();
                entityManager.clear();
            }

            if ((++removeCount % 200) == 0) {
                progress.append("...").append(removeCount);
                syncResults.setResults(progress.toString());
                syncResults = repoManager.mergeRepoSyncResults(syncResults);
            }
        }

        progress.append("...").append(removeCount).append('\n');
        syncResults.setResults(progress.toString());
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults _mergePackageSyncReportADD(ContentSource contentSource, Repo repo,
        Collection<ContentProviderPackageDetails> newPackages,
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> previous, RepoSyncResults syncResults,
        StringBuilder progress, int addCount) {
        Query q;
        int flushCount = 0; // used to know when we should flush the entity manager - for performance purposes

        Map<ResourceType, ResourceType> knownResourceTypes = new HashMap<ResourceType, ResourceType>();
        Map<PackageType, PackageType> knownPackageTypes = new HashMap<PackageType, PackageType>();
        Map<Architecture, Architecture> knownArchitectures = new HashMap<Architecture, Architecture>();

        Map<ResourceType, Map<String, ProductVersion>> knownProductVersions = new HashMap<ResourceType, Map<String, ProductVersion>>();

        // Load this so we have the attached version in the repo <-> package mapping
        repo = entityManager.find(Repo.class, repo.getId());

        // add new packages that are new to the content source.
        // for each new package, we have to find its resource type and package type
        // (both of which must exist, or we abort that package and move on to the next);
        // then find the package and architecture, creating them if they do not exist;
        // then create the new PV as well as the new PVCS mapping.
        // if a repo is associated with the content source, the PV is directly associated with the repo.

        for (ContentProviderPackageDetails newDetails : newPackages) {
            ContentProviderPackageDetailsKey key = newDetails.getContentProviderPackageDetailsKey();

            // find the new package's associated resource type (should already exist)
            ResourceType rt = null;
            if (key.getResourceTypeName() != null && key.getResourceTypePluginName() != null) {
                rt = new ResourceType();
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
            }

            // find the new package's type (package types should already exist, agent plugin descriptors define them)
            PackageType pt = new PackageType(key.getPackageTypeName(), rt);

            if (!knownPackageTypes.containsKey(pt)) {
                if (rt != null) {
                    q = entityManager.createNamedQuery(PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_NAME);
                    q.setParameter("typeId", rt.getId());
                } else {
                    q = entityManager.createNamedQuery(PackageType.QUERY_FIND_BY_NAME_AND_NULL_RESOURCE_TYPE);
                }

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
            q.setParameter("resourceTypeId", rt != null ? rt.getId() : null);
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
            pv.setSHA256(newDetails.getSHA256());
            pv.setShortDescription(newDetails.getShortDescription());

            q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            q.setParameter("packageName", newDetails.getName());
            q.setParameter("packageTypeName", pt.getName());
            q.setParameter("resourceType", rt);
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

            // the check for null resource type shouldn't be necessary here because
            // the package shouldn't declare any resource versions if it doesn't declare a resource type.
            // Nevertheless, let's make that check just to prevent disasters caused by "malicious" content
            // providers.
            if (resourceVersions != null && rt != null) {
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
            } else if (resourceVersions != null) {
                log.info("Misbehaving content provider detected. It declares resource versions " + resourceVersions
                    + " but no resource type in package " + newDetails + ".");
            }

            // now create the mapping between the package version and content source
            // now that if the mapping already exists, we overwrite it (a rare occurrence, but could happen)
            PackageVersionContentSource newPvcs = new PackageVersionContentSource(pv, contentSource, newDetails
                .getLocation());
            newPvcs = entityManager.merge(newPvcs);

            // for all repos that are associated with this content source, add this package version directly to them
            RepoPackageVersion mapping = new RepoPackageVersion(repo, pv);
            entityManager.merge(mapping); // use merge just in case this mapping somehow already exists

            // Cleanup
            if ((++flushCount % 100) == 0) {
                knownResourceTypes.clear();
                knownPackageTypes.clear();
                knownArchitectures.clear();
                knownProductVersions.clear();
                entityManager.flush();
                entityManager.clear();
            }

            if ((++addCount % 100) == 0) {
                progress.append("...").append(addCount);
                syncResults.setResults(progress.toString());
                syncResults = repoManager.mergeRepoSyncResults(syncResults);
            }
        }

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults _mergePackageSyncReportUPDATE(ContentSource contentSource, PackageSyncReport report,
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> previous, RepoSyncResults syncResults,
        StringBuilder progress) {
        progress.append(new Date()).append(": ").append("Updating");
        syncResults.setResults(progress.toString());
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        int flushCount = 0; // used to know when we should flush the entity manager - for performance purposes
        int updateCount = 0;

        // update all changed packages that are still available on the remote repository but whose information is different
        //
        // for each updated package, we have to find its resource type
        // (which must exist, or we abort that package and move on to the next);
        // then we have to get the current PVCS and merge its updates

        for (ContentProviderPackageDetails updatedDetails : report.getUpdatedPackages()) {
            ContentProviderPackageDetailsKey key = updatedDetails.getContentProviderPackageDetailsKey();

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
            pv.setSHA256(updatedDetails.getSHA256());
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
                syncResults = repoManager.mergeRepoSyncResults(syncResults);
            }
        }

        progress.append("...").append(updateCount).append('\n');
        syncResults.setResults(progress.toString());
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults _mergeDistributionSyncReportREMOVE(ContentSource contentSource,
        DistributionSyncReport report, RepoSyncResults syncResults, StringBuilder progress) {

        progress.append(new Date()).append(": ").append("Removing");
        syncResults.setResults(progress.toString());
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        DistributionManagerLocal distManager = LookupUtil.getDistributionManagerLocal();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // remove all distributions that are no longer available on the remote repository
        for (DistributionDetails doomedDetails : report.getDeletedDistributions()) {
            Distribution doomedDist = distManager.getDistributionByLabel(doomedDetails.getLabel());
            distManager.deleteDistributionByDistId(overlord, doomedDist.getId());
            distManager.deleteDistributionFilesByDistId(overlord, doomedDist.getId());
            progress.append("Removed distribution & distribution files for: " + doomedDetails.getLabel());
            syncResults.setResults(progress.toString());
            syncResults = repoManager.mergeRepoSyncResults(syncResults);
        }

        progress.append("Finished Distribution removal...").append('\n');
        syncResults.setResults(progress.toString());
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        return syncResults;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults _mergeDistributionSyncReportADD(ContentSource contentSource, DistributionSyncReport report,
        RepoSyncResults syncResults, StringBuilder progress) {

        DistributionManagerLocal distManager = LookupUtil.getDistributionManagerLocal();
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        List<DistributionDetails> newDetails = report.getDistributions();
        for (DistributionDetails detail : newDetails) {
            try {

                log.debug("Attempting to create new distribution based off of: " + detail);
                DistributionType distType = distManager.getDistributionTypeByName(detail.getDistributionType());
                Distribution newDist = distManager.createDistribution(overlord, detail.getLabel(), detail
                    .getDistributionPath(), distType);
                log.debug("Created new distribution: " + newDist);
                Repo repo = repoManager.getRepo(overlord, report.getRepoId());
                RepoDistribution repoDist = new RepoDistribution(repo, newDist);
                log.debug("Created new mapping of RepoDistribution repoId = " + repo.getId() + ", distId = "
                    + newDist.getId());
                entityManager.persist(repoDist);
                List<DistributionFileDetails> files = detail.getFiles();
                for (DistributionFileDetails f : files) {
                    log.debug("Creating DistributionFile for: " + f);
                    DistributionFile df = new DistributionFile(newDist, f.getRelativeFilename(), f.getMd5sum());
                    df.setLastModified(f.getLastModified());
                    entityManager.persist(df);
                    entityManager.flush();
                }
            } catch (DistributionException e) {
                progress.append("Caught exception when trying to add: " + detail.getLabel() + "\n");
                progress.append("Error is: " + e.getMessage());
                syncResults.setResults(progress.toString());
                syncResults = repoManager.mergeRepoSyncResults(syncResults);
                log.error(e);
            }
        }
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
        MessageDigestGenerator md5Generator = new MessageDigestGenerator();

        Query q = entityManager.createNamedQuery(Repo.QUERY_FIND_REPOS_BY_RESOURCE_ID);
        q.setParameter("resourceId", resourceId);
        List<Repo> repos = q.getResultList();

        for (Repo repo : repos) {
            long modifiedTimestamp = repo.getLastModifiedDate();
            Date modifiedDate = new Date(modifiedTimestamp);
            md5Generator.add(Integer.toString(modifiedDate.hashCode()).getBytes());
        }

        String digestString = md5Generator.getDigestString();
        return digestString;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(45 * 60)
    public long outputPackageVersionBitsGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream) {
        return outputPackageVersionBitsRangeGivenResource(resourceId, packageDetailsKey, outputStream, 0, -1);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(45 * 60)
    public long outputPackageBitsForChildResource(int parentResourceId, String resourceTypeName,
        PackageDetailsKey packageDetailsKey, OutputStream outputStream) {
        Resource parentResource = entityManager.find(Resource.class, parentResourceId);
        ResourceType parentResourceType = parentResource.getResourceType();

        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_PARENT_AND_NAME);
        query.setParameter("parent", parentResourceType);
        query.setParameter("name", resourceTypeName);

        ResourceType childResourceType = (ResourceType) query.getSingleResult();

        query = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY_WITH_NON_NULL_RESOURCE_TYPE);
        query.setParameter("packageName", packageDetailsKey.getName());
        query.setParameter("packageTypeName", packageDetailsKey.getPackageTypeName());
        query.setParameter("architectureName", packageDetailsKey.getArchitectureName());
        query.setParameter("version", packageDetailsKey.getVersion());
        query.setParameter("resourceTypeId", childResourceType.getId());
        PackageVersion packageVersion = (PackageVersion) query.getSingleResult();

        return outputPackageVersionBitsRangeHelper(parentResourceId, packageDetailsKey, outputStream, 0, -1,
            packageVersion.getId());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(45 * 60)
    public long outputPackageVersionBitsRangeGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte) {
        if (startByte < 0L) {
            throw new IllegalArgumentException("startByte[" + startByte + "] < 0");
        }

        if ((endByte > -1L) && (endByte < startByte)) {
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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(45 * 60)
    public long outputPackageVersionBits(PackageVersion packageVersion, OutputStream outputStream) {
        // Used by export of content through http

        PackageDetailsKey packageDetailsKey = new PackageDetailsKey(packageVersion.getDisplayName(), packageVersion
            .getDisplayVersion(), packageVersion.getGeneralPackage().getPackageType().toString(), packageVersion
            .getArchitecture().toString());

        int resourceId = 0; //set to dummy value

        log.debug("Calling outputPackageVersionBitsRangeHelper() with package details: " + packageDetailsKey);
        return outputPackageVersionBitsRangeHelper(resourceId, packageDetailsKey, outputStream, 0, -1, packageVersion
            .getId());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(45 * 60)
    public long outputPackageVersionBits(PackageVersion packageVersion, OutputStream outputStream, long startByte,
        long endByte) {
        // Used by export of content through http

        PackageDetailsKey packageDetailsKey = new PackageDetailsKey(packageVersion.getDisplayName(), packageVersion
            .getDisplayVersion(), packageVersion.getGeneralPackage().getPackageType().toString(), packageVersion
            .getArchitecture().toString());

        int resourceId = 0; //set to dummy value

        log.debug("Calling outputPackageVersionBitsRangeHelper() with package details: " + packageDetailsKey);
        return outputPackageVersionBitsRangeHelper(resourceId, packageDetailsKey, outputStream, startByte, endByte,
            packageVersion.getId());
    }

    @SuppressWarnings( { "unchecked", "unused" })
    public boolean downloadPackageBits(int resourceId, PackageDetailsKey packageDetailsKey) {
        Query query = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_ID_BY_PACKAGE_DETAILS_KEY_AND_RES_ID);
        query.setParameter("packageName", packageDetailsKey.getName());
        query.setParameter("packageTypeName", packageDetailsKey.getPackageTypeName());
        query.setParameter("architectureName", packageDetailsKey.getArchitectureName());
        query.setParameter("version", packageDetailsKey.getVersion());
        query.setParameter("resourceId", resourceId);
        int packageVersionId = ((Integer) query.getSingleResult()).intValue();

        Query queryA = entityManager.createNamedQuery(PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID);
        queryA.setParameter("id", packageVersionId);

        LoadedPackageBitsComposite composite = (LoadedPackageBitsComposite) queryA.getSingleResult();

        boolean packageBitsAreAvailable = composite.isPackageBitsAvailable();
        if (packageBitsAreAvailable) {
            // it says the package bits are available, but if its stored on the filesystem, we should
            // make sure no one deleted the file.  If the file is deleted, let's simply download it again.
            if (!composite.isPackageBitsInDatabase()) {
                try {
                    File bitsFile = getPackageBitsLocalFileAndCreateParentDir(composite.getPackageVersionId(),
                        composite.getFileName());
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
            // or auto-discovered by a resource and attached to a repo. So, that leaves
            // the only possibility - the package version comes from a content source and therefore has
            // a PackageVersionContentSource mapping.  Let's find that mapping.
            Query q2 = entityManager.createNamedQuery(PackageVersionContentSource.QUERY_FIND_BY_PKG_VER_ID_AND_RES_ID);
            q2.setParameter("resourceId", resourceId);
            q2.setParameter("packageVersionId", packageVersionId);
            List<PackageVersionContentSource> pvcss = q2.getResultList();

            // Note that its possible more than one content source can deliver a PV - if a resource is subscribed
            // to repo(s) that contain multiple content sources that can deliver a PV, we just take
            // the first one we find.

            if (pvcss.size() == 0) {
                throw new RuntimeException("Resource [" + resourceId + "] cannot access package version ["
                    + packageDetailsKey + "] - no content source exists to deliver it");
            }

            pvcs = pvcss.get(0);

            try {
                // Make it a true EJB call so we suspend our tx and get a new tx.
                // This way, we start with a fresh tx timeout when downloading and this
                // won't affect the time we are in in this method's tx (I hope that's how it works).
                // This is because this method itself may take a long time to send the data to the output stream
                // and we don't want out tx timing out due to the time it takes downloading.
                InputStream stream = preloadPackageBits(pvcs);
                PackageBits bits = null;
                bits = preparePackageBits(subjectManager.getOverlord(), stream, pvcs);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private long outputPackageVersionBitsRangeHelper(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte, int packageVersionId) {

        // TODO: Should we make sure the resource is subscribed/allowed to receive the the package version?
        //       Or should we not bother to perform this check?  if the caller knows the PV ID, it
        //       probably already got it through its repos

        Query query = entityManager.createNamedQuery(PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID);
        query.setParameter("id", packageVersionId);
        LoadedPackageBitsComposite composite = (LoadedPackageBitsComposite) query.getSingleResult();

        boolean packageBitsAreAvailable = composite.isPackageBitsAvailable();
        if (packageBitsAreAvailable) {
            // it says the package bits are available, but if its stored on the filesystem, we should
            // make sure no one deleted the file.  If the file is deleted, let's simply download it again.
            if (!composite.isPackageBitsInDatabase()) {
                try {
                    File bitsFile = getPackageBitsLocalFileAndCreateParentDir(composite.getPackageVersionId(),
                        composite.getFileName());
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
            // or auto-discovered by a resource and attached to a repo. So, that leaves
            // the only possibility - the package version comes from a content source and therefore has
            // a PackageVersionContentSource mapping.  Let's find that mapping.
            Query q2 = entityManager.createNamedQuery(PackageVersionContentSource.QUERY_FIND_BY_PKG_VER_ID_AND_RES_ID);
            q2.setParameter("resourceId", resourceId);
            q2.setParameter("packageVersionId", packageVersionId);
            List<PackageVersionContentSource> pvcss = q2.getResultList();

            // Note that its possible more than one content source can deliver a PV - if a resource is subscribed
            // to repo(s) that contain multiple content sources that can deliver a PV, we just take
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
                ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
                ContentProviderManager adapterMgr = pc.getAdapterManager();
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
                    Blob blob = results.getBlob(1);

                    long bytesRetrieved = 0L;
                    if (endByte < 0L) {
                        if (startByte == 0L) {
                            bytesRetrieved = StreamUtil.copy(blob.getBinaryStream(), outputStream, false);
                        }
                    } else {
                        long length = (endByte - startByte) + 1;
                        //InputStream stream = blob.getBinaryStream(startByte, length);  // JDK 6 api
                        InputStream stream = blob.getBinaryStream();
                        bytesRetrieved = StreamUtil.copy(stream, outputStream, startByte, length);
                    }
                    log.debug("Retrieved and sent [" + bytesRetrieved + "] bytes for [" + packageDetailsKey + "]");
                    ps.close();
                    conn.close();
                    return bytesRetrieved;

                } else {
                    // this is  DownloadMode.FILESYSTEM - put the bits on the filesystem
                    File bitsFile = getPackageBitsLocalFileAndCreateParentDir(composite.getPackageVersionId(),
                        composite.getFileName());
                    if (!bitsFile.exists()) {
                        throw new RuntimeException("Package bits at [" + bitsFile + "] are missing for ["
                            + packageDetailsKey + "]");
                    }

                    bitsStream = new FileInputStream(bitsFile);
                }
            }

            // the magic happens here - outputStream is probably a remote stream down to the agent
            long bytesRetrieved;
            if (endByte < 0L) {
                if (startByte > 0L) {
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
            log.error("An error occurred while streaming package bits from the database.", sql);
            throw new RuntimeException("Did not download the package bits to the DB for [" + packageDetailsKey + "]",
                sql);
        } catch (Exception e) {
            log.error("An error occurred while streaming package bits from the database.", e);
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

    private File getDistributionFileBitsLocalFilesystemFile(String distLabel, String fileName) {
        String filesystem = System.getProperty(FILESYSTEM_PROPERTY);

        if (filesystem == null) {
            throw new IllegalStateException("Server is misconfigured - missing system property '" + FILESYSTEM_PROPERTY
                + "'. Don't know where distribution bits are stored.");
        }

        // allow the configuration to use ${} system property replacement strings
        filesystem = StringPropertyReplacer.replaceProperties(filesystem);

        String loc = "dists/" + distLabel;
        File parentDir = new File(filesystem, loc);
        File distBitsFile = new File(parentDir, fileName);
        return distBitsFile;
    }

    private File getPackageBitsLocalFilesystemFile(int packageVersionId, String fileName) {

        String filesystem = System.getProperty(FILESYSTEM_PROPERTY);

        if (filesystem == null) {
            throw new IllegalStateException("Server is misconfigured - missing system property '" + FILESYSTEM_PROPERTY
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
        return packageBitsFile;
    }

    private File getDistLocalFileAndCreateParentDir(String distLabel, String fileName) throws Exception {

        File distBitsFile = getDistributionFileBitsLocalFilesystemFile(distLabel, fileName);
        File parentDir = distBitsFile.getParentFile();

        if (!parentDir.isDirectory()) {
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (!parentDir.isDirectory()) {
                throw new Exception("Cannot create content filesystem directory [" + parentDir
                    + "] for distribution bits storage.");
            }
        }
        return distBitsFile;
    }

    private File getPackageBitsLocalFileAndCreateParentDir(int packageVersionId, String fileName) throws Exception {

        File packageBitsFile = getPackageBitsLocalFilesystemFile(packageVersionId, fileName);
        File parentDir = packageBitsFile.getParentFile();

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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(45 * 60)
    public long outputDistributionFileBits(DistributionFile distFile, OutputStream outputStream) {

        long numBytes = 0L;
        InputStream bitStream = null;
        try {
            Distribution dist = distFile.getDistribution();
            log.info("Distribution has a basePath of " + dist.getBasePath());
            String distFilePath = dist.getBasePath() + "/" + distFile.getRelativeFilename();
            File f = getDistributionFileBitsLocalFilesystemFile(dist.getLabel(), distFilePath);
            log.info("Fetching: " + distFilePath + " on local file store from: " + f.getAbsolutePath());
            bitStream = new FileInputStream(f);
            numBytes = StreamUtil.copy(bitStream, outputStream);

        } catch (Exception e) {
            log.info(e);
        } finally {
            // close our stream but leave the output stream open
            try {
                bitStream.close();
            } catch (Exception closeError) {
                log.warn("Failed to close the bits stream", closeError);
            }
        }
        log.debug("Retrieved and sent [" + numBytes + "] bytes for [" + distFile.getRelativeFilename() + "]");
        return numBytes;
    }

    private void obfuscatePasswords(ContentSource contentSource) {
        ConfigurationDefinition configurationDefinition = contentSource.getContentSourceType().getContentSourceConfigurationDefinition();
        if (configurationDefinition == null) {
            ContentSourceType attachedContentSourceType = getContentSourceType(contentSource.getContentSourceType().getName());
            configurationDefinition = attachedContentSourceType.getContentSourceConfigurationDefinition();
        }

        PasswordObfuscationUtility.obfuscatePasswords(configurationDefinition, contentSource.getConfiguration());
    }
}
