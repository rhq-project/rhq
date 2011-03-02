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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;

import org.jboss.annotation.IgnoreDependency;
import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSyncStatus;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoAdvisory;
import org.rhq.core.domain.content.RepoContentSource;
import org.rhq.core.domain.content.RepoDistribution;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.core.domain.content.RepoPackageVersion;
import org.rhq.core.domain.content.RepoRelationship;
import org.rhq.core.domain.content.RepoRelationshipType;
import org.rhq.core.domain.content.RepoRepoRelationship;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.domain.content.ResourceRepo;
import org.rhq.core.domain.content.composite.RepoComposite;
import org.rhq.core.domain.content.transfer.SubscribedRepo;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderManager;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.PackageSource;
import org.rhq.enterprise.server.plugin.pc.content.PackageTypeBehavior;
import org.rhq.enterprise.server.plugin.pc.content.RepoDetails;
import org.rhq.enterprise.server.plugin.pc.content.RepoGroupDetails;
import org.rhq.enterprise.server.plugin.pc.content.RepoImportReport;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.QueryUtility;

@Stateless
public class RepoManagerBean implements RepoManagerLocal, RepoManagerRemote {

    /**
     * Refers to the default repo relationship created at DB setup time to represent a parent/child repo
     * relationship.
     * <p/>
     * This probably isn't the best place to store this, but for now this is the primary usage of this
     * relationship type.
     */
    private static final String PARENT_RELATIONSHIP_NAME = "parent";

    private final Log log = LogFactory.getLog(RepoManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authzManager;

    @IgnoreDependency
    @EJB
    private ContentSourceManagerLocal contentSourceManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private RepoManagerLocal repoManager;

    public void deleteRepo(Subject subject, int repoId) {
        
        if (!authzManager.canUpdateRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] cannot delete repository with id " + repoId);
        }
        
        log.info("User [" + subject + "] is deleting repository with id [" + repoId + "]...");

        // bulk delete m-2-m mappings to the doomed repo
        // get ready for bulk delete by clearing entity manager
        entityManager.flush();
        entityManager.clear();

        entityManager.createNamedQuery(ResourceRepo.DELETE_BY_REPO_ID).setParameter("repoId", repoId).executeUpdate();

        entityManager.createNamedQuery(RepoContentSource.DELETE_BY_REPO_ID).setParameter("repoId", repoId)
            .executeUpdate();

        entityManager.createNamedQuery(RepoPackageVersion.DELETE_BY_REPO_ID).setParameter("repoId", repoId)
            .executeUpdate();

        Repo repo = entityManager.find(Repo.class, repoId);
        if (repo != null) {
            entityManager.remove(repo);
            log.debug("User [" + subject + "] deleted repository [" + repo + "]");
        } else {
            log.debug("Repository with id [" + repoId + "] doesn't exist - nothing to delete");
        }

        // remove any unused, orphaned package versions
        contentSourceManager.purgeOrphanedPackageVersions(subject);
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void deleteRepoGroup(Subject subject, int repoGroupId) {
        RepoGroup deleteMe = getRepoGroup(subject, repoGroupId);
        entityManager.remove(deleteMe);
    }

    public boolean deletePackageVersionsFromRepo(Subject subject, int repoId, int[] packageVersionIds) {
        if (packageVersionIds == null || packageVersionIds.length == 0) {
            return true;
        }
        
        if (!authzManager.canUpdateRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] cannot update the repo with id " + repoId + " and therefore cannot delete package versions from it.");
        }
        
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for(int id : packageVersionIds) {
            ids.add(id);
        }

        Query deleteable = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_DELETEABLE_IDS_IN_REPO);
        deleteable.setParameter("repoId", repoId);
        deleteable.setParameter("packageVersionIds", ids);
        
        @SuppressWarnings("unchecked")
        List<Integer> deleteableIds = (List<Integer>) deleteable.getResultList();
        
        if (deleteableIds.isEmpty()) {
            return false;
        }
        
        Query deleteRepoPackageVersions = entityManager.createNamedQuery(RepoPackageVersion.DELETE_MULTIPLE_WHEN_NO_PROVIDER);
        deleteRepoPackageVersions.setParameter("repoId", repoId);            
        deleteRepoPackageVersions.setParameter("packageVersionIds", deleteableIds);
        
        deleteRepoPackageVersions.executeUpdate();
        
        Query deletePackageVersions = entityManager.createNamedQuery(PackageVersion.DELETE_MULTIPLE_IF_NO_CONTENT_SOURCES_OR_REPOS);
        deletePackageVersions.setParameter("packageVersionIds", deleteableIds);
        
        int deleted = deletePackageVersions.executeUpdate();
        
        return deleted == packageVersionIds.length;
    }
    
    @SuppressWarnings("unchecked")
    public PageList<Repo> findRepos(Subject subject, PageControl pc) {
        pc.initDefaultOrderingField("c.name");

        Query query = null;
        Query countQuery = null;
        
        if (authzManager.hasGlobalPermission(subject, Permission.MANAGE_REPOSITORIES)) {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Repo.QUERY_FIND_ALL_IMPORTED_REPOS_ADMIN, pc);
            countQuery = PersistenceUtility.createCountQuery(entityManager, Repo.QUERY_FIND_ALL_IMPORTED_REPOS_ADMIN);
        } else {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Repo.QUERY_FIND_ALL_IMPORTED_REPOS, pc);
            countQuery = PersistenceUtility.createCountQuery(entityManager, Repo.QUERY_FIND_ALL_IMPORTED_REPOS);
            
            query.setParameter("subject", subject);
            countQuery.setParameter("subject", subject);
        }
        
        List<Repo> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Repo>(results, (int) count, pc);
    }

    public Repo getRepo(Subject subject, int repoId) {
        if (!authzManager.canViewRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] cannot access the repo with id " + repoId);
        }
        
        Repo repo = entityManager.find(Repo.class, repoId);

        if ((repo != null) && (repo.getRepoContentSources() != null)) {
            // load content sources separately. we can't do this all at once via fetch join because
            // on Oracle we use a LOB column on a content source field and you can't DISTINCT on LOBs
            repo.getRepoContentSources().size();
        }

        return repo;
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public RepoGroup getRepoGroup(Subject subject, int repoGroupId) {
        RepoGroup repoGroup = entityManager.find(RepoGroup.class, repoGroupId);
        return repoGroup;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public PageList<ContentSource> findAssociatedContentSources(Subject subject, int repoId, PageControl pc) {
        pc.initDefaultOrderingField("cs.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, ContentSource.QUERY_FIND_BY_REPO_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, ContentSource.QUERY_FIND_BY_REPO_ID);

        query.setParameter("id", repoId);
        countQuery.setParameter("id", repoId);

        List<ContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ContentSource>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Resource> findSubscribedResources(Subject subject, int repoId, PageControl pc) {
        pc.initDefaultOrderingField("rc.resource.id");

        if (!authzManager.canViewRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] can't access repository with id " + repoId);
        }
        
        Query query = PersistenceUtility
            .createQueryWithOrderBy(entityManager, Repo.QUERY_FIND_SUBSCRIBER_RESOURCES, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Repo.QUERY_FIND_SUBSCRIBER_RESOURCES);

        query.setParameter("id", repoId);
        countQuery.setParameter("id", repoId);

        List<Resource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Resource>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    // current resource subscriptions should be viewing, but perhaps available ones shouldn't
    public PageList<RepoComposite> findResourceSubscriptions(Subject subject, int resourceId, PageControl pc) {
        if (!authzManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User [" + subject + "] can't view resource with id " + resourceId);
        }
        
        pc.initDefaultOrderingField("c.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Repo.QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID, pc);
        Query countQuery = entityManager.createNamedQuery(Repo.QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT);

        query.setParameter("resourceId", resourceId);
        countQuery.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<RepoComposite>(results, (int) count, pc);
    }

    public List<SubscribedRepo> findSubscriptions(Subject subject, int resourceId) {
        if (!authzManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User [" + subject + "] can't view resource with id " + resourceId);
        }
        
        List<SubscribedRepo> list = new ArrayList<SubscribedRepo>();
        PageControl pc = new PageControl();
        for (RepoComposite repoComposite : findResourceSubscriptions(subject, resourceId, pc)) {
            Repo repo = repoComposite.getRepo();
            SubscribedRepo summary = new SubscribedRepo(repo.getId(), repo.getName());
            list.add(summary);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<RepoComposite> findAvailableResourceSubscriptions(Subject subject, int resourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.id");

        Query query = null;
        Query countQuery = null;
        
        if (authzManager.hasGlobalPermission(subject, Permission.MANAGE_REPOSITORIES)) {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_ADMIN, pc);
            countQuery = entityManager
                .createNamedQuery(Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_ADMIN_COUNT);
        } else {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID, pc);
            countQuery = entityManager
                .createNamedQuery(Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT);
            
            query.setParameter("subject", subject);
            countQuery.setParameter("subject", subject);
        }
         
        query.setParameter("resourceId", resourceId);
        countQuery.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<RepoComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public List<RepoComposite> findResourceSubscriptions(int resourceId) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID);

        query.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<RepoComposite> findAvailableResourceSubscriptions(int resourceId) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_ADMIN);

        query.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, PageControl pc) {
        if (!authzManager.canViewRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] can't access repo with id " + repoId);
        }
        
        pc.initDefaultOrderingField("pv.generalPackage.name, pv.version");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE, pc);

        query.setParameter("repoId", repoId);

        List<PackageVersion> results = query.getResultList();
        long count = getPackageVersionCountFromRepo(subject, null, repoId);

        return new PageList<PackageVersion>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, String filter, PageControl pc) {
        if (!authzManager.canViewRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] can't access repo with id " + repoId);
        }
        
        pc.initDefaultOrderingField("pv.generalPackage.name, pv.version");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE_FILTERED, pc);

        query.setParameter("repoId", repoId);
        query.setParameter("filter", QueryUtility.formatSearchParameter(filter));
        query.setParameter("escapeChar", QueryUtility.getEscapeCharacter());

        List<PackageVersion> results = query.getResultList();
        long count = getPackageVersionCountFromRepo(subject, filter, repoId);

        return new PageList<PackageVersion>(results, (int) count, pc);
    }

    public PackageVersion getLatestPackageVersion(Subject subject, int packageId, int repoId) {
        if (!authzManager.canViewRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] cannot access the repo with id " + repoId);
        }
        
        Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_AND_REPO_ID);
        q.setParameter("packageId", packageId);
        q.setParameter("repoId", repoId);
        
        @SuppressWarnings("unchecked")
        List<PackageVersion> results = (List<PackageVersion>) q.getResultList();
        
        if (results.size() == 0) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        }
        
        PackageVersion latest = results.get(0);
        String packageTypeName = latest.getGeneralPackage().getPackageType().getName();

        Comparator<PackageVersion> versionComparator = null;
        try {
            PackageTypeBehavior behavior = ContentManagerHelper.getPackageTypeBehavior(packageTypeName);
            versionComparator = behavior.getPackageVersionComparator(packageTypeName);
        } catch (Exception e) {
            log.error("Could not get the package type behavior for package type '" + packageTypeName + "'. This should not happen.", e);
        }
        
        if (versionComparator == null) {
            versionComparator = PackageVersion.DEFAULT_COMPARATOR;
        }
        
        Iterator<PackageVersion> it = results.iterator();
        it.next(); //skip the first element, we don't have to compare it with itself
        while(it.hasNext()) {
            PackageVersion current = it.next();
            if (versionComparator.compare(latest, current) < 0) {
                latest = current;
            }
        }
        
        return latest;                    
    }
    
    public Repo updateRepo(Subject subject, Repo repo) throws RepoException {
        validateFields(repo);

        if (!authzManager.hasGlobalPermission(subject, Permission.MANAGE_REPOSITORIES)) {
            if (!authzManager.canUpdateRepo(subject, repo.getId())) {
                throw new PermissionException("User [" + subject + "] can't update repo with id " + repo.getId());
            }
            
            //only the repo manager can update the owner of a repo.
            //make sure that's the case.
            repo.setOwner(subject);
        }
        
        // HHH-2864 - Leave this in until we move to hibernate > 3.2.r14201-2
        getRepo(subject, repo.getId());

        // should we check non-null repo relationships and warn that we aren't changing them?
        log.debug("User [" + subject + "] is updating [" + repo + "]...");
        repo = entityManager.merge(repo);
        log.debug("User [" + subject + "] updated [" + repo + "].");

        try {
            ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
            pc.unscheduleRepoSyncJob(repo);
            pc.scheduleRepoSyncJob(repo);
        } catch (Exception e) {
            log.warn("Failed to reschedule repository synchronization job for [" + repo + "].", e);
        }

        return repo;
    }

    public Repo createRepo(Subject subject, Repo repo) throws RepoException {
        validateRepo(repo);
        
        if (!authzManager.hasGlobalPermission(subject, Permission.MANAGE_REPOSITORIES)) {
            //only the repo manager can update the owner of a repo.
            //make sure that's the case.
            repo.setOwner(subject);
        }
        
        log.debug("User [" + subject + "] is creating [" + repo + "]...");
        entityManager.persist(repo);
        log.info("User [" + subject + "] created [" + repo + "].");

        // If this repo is imported, schedule the repo sync job.
        if (!repo.isCandidate()) {
            try {
                ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
                pc.scheduleRepoSyncJob(repo);
            } catch (Exception e) {
                log.error("Failed to schedule repository synchronization job for [" + repo + "].", e);
                throw new RuntimeException(e);
            }
        }

        return repo; // now has the id set
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void deleteCandidatesWithOnlyContentSource(Subject subject, int contentSourceId) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_CANDIDATES_WITH_ONLY_CONTENT_SOURCE);

        query.setParameter("contentSourceId", contentSourceId);

        List<Repo> repoList = query.getResultList();

        for (Repo deleteMe : repoList) {
            deleteRepo(subject, deleteMe.getId());
        }
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void processRepoImportReport(Subject subject, RepoImportReport report, int contentSourceId,
        StringBuilder result) {

        // TODO: The below line was added to simplify things for JON (i.e. patches from JBoss CSP) - remove it if we
        //       need more flexibility for other use cases. (ips, 03/26/10)
        boolean autoImport = (report.getRepoGroups().isEmpty() && report.getRepos().size() == 1);

        // Import groups first
        List<RepoGroupDetails> repoGroups = report.getRepoGroups();

        List<RepoGroupDetails> importedRepoGroups = new ArrayList<RepoGroupDetails>();
        for (RepoGroupDetails createMe : repoGroups) {
            String name = createMe.getName();

            RepoGroup existingGroup = getRepoGroupByName(name);
            if (existingGroup == null) {
                existingGroup = new RepoGroup(name);
                existingGroup.setDescription(createMe.getDescription());

                RepoGroupType groupType = getRepoGroupTypeByName(subject, createMe.getTypeName());
                existingGroup.setRepoGroupType(groupType);

                // Don't let the whole report blow up if one of these fails,
                // but be sure to mention it in the report.
                try {
                    createRepoGroup(subject, existingGroup);
                    importedRepoGroups.add(createMe);
                } catch (RepoException e) {

                    if (e.getType() == RepoException.RepoExceptionType.NAME_ALREADY_EXISTS) {
                        result.append("Skipping existing repository group [").append(name).append("]").append('\n');
                    } else {
                        log.error("Error adding repository group [" + name + "]", e);
                        result.append("Could not add repository group [").append(name)
                            .append("]. See log for more information.").append('\n');
                    }
                }
            }
        }

        if (importedRepoGroups.isEmpty()) {
            result
                .append("There are no new repository groups since the last time this content source was synchronized.\n");
        } else {
            result.append("Imported the following [").append(importedRepoGroups.size())
                .append("] repository group(s): ").append(importedRepoGroups).append('\n');
        }

        // Hold on to all current candidate repos for the content source. If any were not present in this
        // report, remove them from the system (the rationale being, the content source no longer knows
        // about them and thus they cannot be imported).
        RepoCriteria candidateReposCriteria = new RepoCriteria();
        candidateReposCriteria.addFilterContentSourceIds(contentSourceId);
        candidateReposCriteria.addFilterCandidate(true);

        PageList<Repo> candidatesForThisProvider = findReposByCriteria(subject, candidateReposCriteria);

        // Once the groups are in the system, import any repos that were added
        List<RepoDetails> repos = report.getRepos();

        // First add repos that have no parent. We later add repos with a parent afterwards to prevent
        // issues where both the parent and child are specified in this report.
        List<RepoDetails> importedRepos = new ArrayList<RepoDetails>();
        for (RepoDetails createMe : repos) {
            if (createMe.getParentRepoName() == null) {
                try {
                    if (addCandidateRepo(contentSourceId, createMe, autoImport)) {
                        importedRepos.add(createMe);
                    }
                    removeRepoFromList(createMe.getName(), candidatesForThisProvider);
                } catch (Exception e) {
                    if (e instanceof RepoException
                        && ((RepoException) e).getType() == RepoException.RepoExceptionType.NAME_ALREADY_EXISTS) {
                        result.append("Skipping addition of existing repository [").append(createMe.getName())
                            .append("]").append('\n');
                    } else {
                        log.error("Error processing repository [" + createMe + "]", e);
                        result.append("Could not add repository [").append(createMe.getName())
                            .append("]. See log for more information.").append('\n');
                    }
                }
            }
        }

        // Take a second pass through the list checking for any repos that were created to be
        // a child of another repo.
        for (RepoDetails createMe : repos) {
            if (createMe.getParentRepoName() != null) {
                try {
                    if (addCandidateRepo(contentSourceId, createMe, autoImport)) {
                        importedRepos.add(createMe);
                    }
                    removeRepoFromList(createMe.getName(), candidatesForThisProvider);
                } catch (Exception e) {
                    log.error("Error processing repository [" + createMe + "]", e);
                    result.append("Could not add repository [").append(createMe.getName())
                        .append("]. See log for more information.").append('\n');
                }
            }
        }

        if (importedRepos.isEmpty()) {
            result.append("There are no new repositories since the last time this content source was synchronized.\n");
        } else {
            result.append("Imported the following ").append(importedRepos.size()).append(" repository(s): ")
                .append(importedRepos).append('\n');
        }

        // Any repos that haven't been removed from candidatesForThisProvider were not returned in this
        // report, so remove them from the database.
        if (!candidatesForThisProvider.isEmpty()) {
            for (Repo deleteMe : candidatesForThisProvider) {
                deleteRepo(subject, deleteMe.getId());
            }
            result.append("Deleted the following ").append(candidatesForThisProvider.size())
                .append(" obsolete repository(s): ").append(candidatesForThisProvider).append('\n');
        }
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void importCandidateRepo(Subject subject, List<Integer> repoIds) throws RepoException {
        for (Integer repoId : repoIds) {
            Repo repo = entityManager.find(Repo.class, repoId);

            if (repo == null) {
                throw new RepoException("Unable to find candidate repository with id " + repoId + " for import.");
            }

            if (!repo.isCandidate()) {
                throw new RepoException("Unable to import repository with id " + repoId
                    + ", because it is already imported.");
            }

            repo.setCandidate(false);
        }
    }

    public void removeOwnershipOfSubject(int subjectId) {
        Query q = entityManager.createNamedQuery(Repo.QUERY_UPDATE_REMOVE_OWNER_FROM_REPOS_OWNED_BY_SUBJECT);
        q.setParameter("ownerId", subjectId);
        
        q.executeUpdate();
    }
    
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public RepoGroup createRepoGroup(Subject subject, RepoGroup repoGroup) throws RepoException {
        validateRepoGroup(repoGroup);

        entityManager.persist(repoGroup);

        return repoGroup;
    }

    @SuppressWarnings("unchecked")
    public List<Repo> getRepoByName(String name) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_NAME);

        query.setParameter("name", name);
        List<Repo> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    public RepoGroup getRepoGroupByName(String name) {
        Query query = entityManager.createNamedQuery(RepoGroup.QUERY_FIND_BY_NAME);

        query.setParameter("name", name);
        List<RepoGroup> results = query.getResultList();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public RepoGroupType getRepoGroupTypeByName(Subject subject, String name) {
        Query query = entityManager.createNamedQuery(RepoGroupType.QUERY_FIND_BY_NAME);

        query.setParameter("name", name);
        List<RepoGroupType> results = query.getResultList();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void addContentSourcesToRepo(Subject subject, int repoId, int[] contentSourceIds) throws Exception {
        Repo repo = entityManager.find(Repo.class, repoId);
        if (repo == null) {
            throw new Exception("There is no repo with an ID [" + repoId + "]");
        }

        repo.setLastModifiedDate(System.currentTimeMillis());

        log.debug("User [" + subject + "] is adding content sources to repo [" + repo + "]");

        ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
        Query q = entityManager.createNamedQuery(PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_NO_FETCH);

        for (int id : contentSourceIds) {
            ContentSource contentSource = entityManager.find(ContentSource.class, id);
            if (contentSource == null) {
                throw new Exception("There is no content source with id [" + id + "]");
            }

            Set<ContentSource> alreadyAssociatedContentSources = repo.getContentSources();
            // Only add it if it's not already associated with this repo.
            if (!alreadyAssociatedContentSources.contains(contentSource)) {
                RepoContentSource repoContentSourceMapping = repo.addContentSource(contentSource);
                entityManager.persist(repoContentSourceMapping);
            }
            Set<PackageVersion> alreadyAssociatedPackageVersions = new HashSet<PackageVersion>(
                repo.getPackageVersions());

            // Automatically associate all of the content source's package versions with this repo,
            // but *skip* over the ones that are already linked to this repo from a previous association.
            q.setParameter("id", contentSource.getId());
            List<PackageVersionContentSource> pvcss = q.getResultList();
            for (PackageVersionContentSource pvcs : pvcss) {
                PackageVersion packageVersion = pvcs.getPackageVersionContentSourcePK().getPackageVersion();
                // Only add it if it's not already associated with this repo.
                if (!alreadyAssociatedPackageVersions.contains(packageVersion)) {
                    RepoPackageVersion mapping = new RepoPackageVersion(repo, packageVersion);
                    entityManager.persist(mapping);
                }
            }

            entityManager.flush();
            entityManager.clear();
        }
    }

    public void simpleAddContentSourcesToRepo(Subject subject, int repoId, int[] contentSourceIds) throws Exception {
        Repo repo = entityManager.find(Repo.class, repoId);
        if (repo == null) {
            throw new Exception("There is no repo with an ID [" + repoId + "]");
        }

        for (int id : contentSourceIds) {
            ContentSource cs = entityManager.find(ContentSource.class, id);
            if (cs == null) {
                throw new Exception("There is no content source with id [" + id + "]");
            }

            RepoContentSource ccsmapping = repo.addContentSource(cs);
            entityManager.persist(ccsmapping);
        }
    }

    public void addPackageVersionsToRepo(Subject subject, int repoId, int[] packageVersionIds) {
        if (!authzManager.canUpdateRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] can't update repo with id " + repoId);
        }
                
        Repo repo = entityManager.find(Repo.class, repoId);

        for (int packageVersionId : packageVersionIds) {
            PackageVersion packageVersion = entityManager.find(PackageVersion.class, packageVersionId);

            RepoPackageVersion mapping = new RepoPackageVersion(repo, packageVersion);
            entityManager.persist(mapping);
        }
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void removeContentSourcesFromRepo(Subject subject, int repoId, int[] contentSourceIds) throws RepoException {
        Repo repo = getRepo(subject, repoId);

        log.debug("User [" + subject + "] is removing content sources from repository [" + repo + "]");

        Set<RepoContentSource> currentSet = repo.getRepoContentSources();

        if ((currentSet != null) && (currentSet.size() > 0)) {
            Set<RepoContentSource> toBeRemoved = new HashSet<RepoContentSource>();
            for (RepoContentSource current : currentSet) {
                for (int id : contentSourceIds) {
                    if (id == current.getRepoContentSourcePK().getContentSource().getId()) {
                        toBeRemoved.add(current);
                        break;
                    }
                }
            }

            for (RepoContentSource doomed : toBeRemoved) {
                entityManager.remove(doomed);
            }

            currentSet.removeAll(toBeRemoved);
        }

        // note that we specifically do not disassociate package versions from the repo, even if those
        // package versions come from the content source that is being removed
    }

    @SuppressWarnings("unchecked")
    public void subscribeResourceToRepos(Subject subject, int resourceId, int[] repoIds) {
        if ((repoIds == null) || (repoIds.length == 0)) {
            return; // nothing to do
        }

        // make sure the user has permissions to subscribe this resource
        if (!authzManager.hasResourcePermission(subject, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("[" + subject
                + "] does not have permission to subscribe this resource to repos");
        }

        // find the resource - abort if it does not exist
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new RuntimeException("There is no resource with the ID [" + resourceId + "]");
        }

        // find all the repos and subscribe the resource to each of them
        // note that if the length of the ID array doesn't match, then one of the repos doesn't exist
        // and we abort altogether - we do not subscribe to anything unless all repo IDs are valid
        Query q = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_IDS);
        List<Integer> idList = new ArrayList<Integer>(repoIds.length);
        for (Integer id : repoIds) {
            idList.add(id);
        }

        q.setParameter("ids", idList);
        List<Repo> repos = q.getResultList();

        if (repos.size() != repoIds.length) {
            throw new RuntimeException("One or more of the repos do not exist [" + idList + "]->[" + repos + "]");
        }

        //authz check
        for(Repo repo : repos) {
            if (!authzManager.canViewRepo(subject, repo.getId())) {
                throw new PermissionException("User [" + subject + "] cannot access a repo with id " + repo.getId());
            }
        }
        
        for (Repo repo : repos) {
            ResourceRepo mapping = repo.addResource(resource);
            entityManager.persist(mapping);
        }
    }

    @SuppressWarnings("unchecked")
    public void unsubscribeResourceFromRepos(Subject subject, int resourceId, int[] repoIds) {
        if ((repoIds == null) || (repoIds.length == 0)) {
            return; // nothing to do
        }

        // make sure the user has permissions to unsubscribe this resource
        if (!authzManager.hasResourcePermission(subject, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("[" + subject
                + "] does not have permission to unsubscribe this resource from repositories");
        }

        // find the resource - abort if it does not exist
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new RuntimeException("There is no resource with the ID [" + resourceId + "]");
        }

        // find all the repos and unsubscribe the resource from each of them
        // note that if the length of the ID array doesn't match, then one of the repos doesn't exist
        // and we abort altogether - we do not unsubscribe from anything unless all repo IDs are valid
        Query q = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_IDS);
        List<Integer> idList = new ArrayList<Integer>(repoIds.length);
        for (Integer id : repoIds) {
            idList.add(id);
        }

        q.setParameter("ids", idList);
        List<Repo> repos = q.getResultList();

        if (repos.size() != repoIds.length) {
            throw new RuntimeException("One or more of the repos do not exist [" + idList + "]->[" + repos + "]");
        }

        for (Repo repo : repos) {
            ResourceRepo mapping = repo.removeResource(resource);
            entityManager.remove(mapping);
        }
    }

    public long getPackageVersionCountFromRepo(Subject subject, String filter, int repoId) {
        if (!authzManager.canViewRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] can't access repo with id " + repoId);
        }
        
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_FILTERED);

        countQuery.setParameter("repoId", repoId);
        countQuery.setParameter("filter", (filter == null) ? null : ("%" + filter.toUpperCase() + "%"));

        return ((Long) countQuery.getSingleResult()).longValue();
    }

    public long getPackageVersionCountFromRepo(Subject subject, int repoId) {
        return getPackageVersionCountFromRepo(subject, null, repoId);
    }

    @SuppressWarnings("unchecked")
    public PageList<Repo> findReposByCriteria(Subject subject, RepoCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;
        
        //TODO this needs the authz applied somehow
        
        CriteriaQueryRunner<Repo> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageVersion> findPackageVersionsInRepoByCriteria(Subject subject, PackageVersionCriteria criteria) {
        Integer repoId = criteria.getFilterRepoId();

        if ((null == repoId) || (repoId < 1)) {
            throw new IllegalArgumentException("Illegal filterResourceId: " + repoId);
        }

        if (!authzManager.canViewRepo(subject, repoId)) {
            throw new PermissionException("User [" + subject + "] can't access repo with id " + repoId);
        }
        

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;

        CriteriaQueryRunner<PackageVersion> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);

        return queryRunner.execute();
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addRepoRelationship(Subject subject, int repoId, int relatedRepoId, String relationshipTypeName) {

        Repo repo = entityManager.find(Repo.class, repoId);
        Repo relatedRepo = entityManager.find(Repo.class, relatedRepoId);

        Query typeQuery = entityManager.createNamedQuery(RepoRelationshipType.QUERY_FIND_BY_NAME);
        typeQuery.setParameter("name", relationshipTypeName);
        RepoRelationshipType relationshipType = (RepoRelationshipType) typeQuery.getSingleResult();

        RepoRelationship repoRelationship = new RepoRelationship();
        repoRelationship.setRelatedRepo(relatedRepo);
        repoRelationship.setRepoRelationshipType(relationshipType);
        repoRelationship.addRepo(repo);

        entityManager.persist(repoRelationship);
        relatedRepo.addRepoRelationship(repoRelationship);

        RepoRepoRelationship repoRepoRelationship = new RepoRepoRelationship(repo, repoRelationship);

        entityManager.persist(repoRepoRelationship);
        repo.addRepoRelationship(repoRelationship);
    }

    private void validateFields(Repo repo) throws RepoException {
        if (repo.getName() == null || repo.getName().trim().equals("")) {
            throw new RepoException("Repo name is required");
        }
        if (repo.getSyncSchedule() != null) {
            try {
                CronExpression ce = new CronExpression(repo.getSyncSchedule());
            } catch (ParseException e) {
                throw new RepoException("Repo sync schedule is not a vaild format.");
            }
        }
    }

    private void validateRepo(Repo c) throws RepoException {
        this.validateFields(c);

        List<Repo> repos = getRepoByName(c.getName());
        if (repos.size() != 0) {
            RepoException e = new RepoException("There is already a repo with the name of [" + c.getName() + "]");
            e.setType(RepoException.RepoExceptionType.NAME_ALREADY_EXISTS);
            throw e;
        }
    }

    /**
     * Tests the values of the given repo group to ensure creating the group would be a valid operation, including
     * ensuring the name is specified and there isn't already an existing group with the same name.
     *
     * @param repoGroup group to test
     * @throws RepoException if the group should not be allowed to be created
     */
    private void validateRepoGroup(RepoGroup repoGroup) throws RepoException {
        if (repoGroup.getName() == null || repoGroup.getName().trim().equals("")) {
            throw new RepoException("Repo group name is required");
        }

        RepoGroup existingRepoGroup = getRepoGroupByName(repoGroup.getName());
        if (existingRepoGroup != null) {
            RepoException e = new RepoException("There is already a repo group with the name [" + repoGroup.getName()
                + "]");
            e.setType(RepoException.RepoExceptionType.NAME_ALREADY_EXISTS);
            throw e;
        }
    }

    /**
     * Performs the necessary logic to determine if a candidate repo should be added to the system, adding it
     * in the process if it needs to. If the repo already exists in the system, associate it with the specified
     * content source.
     * <p/>
     * Calling this method with a repo that has a parent assumes the parent has already been created. This call
     * assumes the repo group has been created as well.
     *
     * @param contentSourceId identifies the content source that introduced the candidate into the system
     * @param createMe        describes the candidate to be created
     *
     * @param autoImport      whether or not to import the repo
     *
     * @throws Exception if there is an error associating the content source with the repo or if the repo
     *                   indicates a parent or repo group that does not exist
     */
    private boolean addCandidateRepo(int contentSourceId, RepoDetails createMe, boolean autoImport) throws Exception {

        Subject overlord = subjectManager.getOverlord();
        String name = createMe.getName();

        List<Repo> existingRepos = getRepoByName(name);

        if (!existingRepos.isEmpty()) {
            // The repo already exists - make sure it is associated with the specified content source.
            for (Repo existingRepo : existingRepos) {
                addContentSourcesToRepo(overlord, existingRepo.getId(), new int[] { contentSourceId });
            }
            return false;
        }

        // The repo doesn't exist yet in the system - create it.
        Repo addMe = new Repo(name);
        addMe.setCandidate(!autoImport);
        addMe.setDescription(createMe.getDescription());

        String createMeGroup = createMe.getRepoGroup();
        if (createMeGroup != null) {
            RepoGroup group = getRepoGroupByName(createMeGroup);
            addMe.addRepoGroup(group);
        }

        // Add the new candidate to the database
        addMe = createRepo(overlord, addMe);

        // Associate the content source that introduced the candidate with the repo
        addContentSourcesToRepo(overlord, addMe.getId(), new int[] { contentSourceId });

        // If the repo indicates it has a parent, create that relationship
        String parentName = createMe.getParentRepoName();
        if (parentName != null) {
            List<Repo> parentList = getRepoByName(parentName);

            if (parentList.size() == 0) {
                String error = "Attempting to create repo [" + name + "] with parent [" + parentName
                    + "] but cannot find the parent";
                log.error(error);
                throw new RepoException(error);
            } else {
                Repo parent = parentList.get(0);
                addRepoRelationship(overlord, addMe.getId(), parent.getId(), PARENT_RELATIONSHIP_NAME);
            }
        }

        return true;
    }

    private void removeRepoFromList(String repoName, List<Repo> repoList) {
        Repo deleteMe = null;
        for (Repo checkMe : repoList) {
            if (checkMe.getName().equals(repoName)) {
                deleteMe = checkMe;
                break;
            }
        }

        if (deleteMe != null) {
            repoList.remove(deleteMe);
        }
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public long getDistributionCountFromRepo(Subject subject, int repoId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, RepoDistribution.QUERY_FIND_BY_REPO_ID);

        countQuery.setParameter("repoId", repoId);

        return (Long) countQuery.getSingleResult();
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    @SuppressWarnings("unchecked")
    public PageList<Distribution> findAssociatedDistributions(Subject subject, int repoid, PageControl pc) {
        pc.setPrimarySort("rkt.id", PageOrdering.ASC);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, RepoDistribution.QUERY_FIND_BY_REPO_ID,
            pc);

        query.setParameter("repoId", repoid);

        List<RepoDistribution> results = query.getResultList();

        ArrayList<Distribution> distros = new ArrayList();
        for (RepoDistribution result : results) {
            distros.add(result.getRepoDistributionPK().getDistribution());

        }
        long count = getDistributionCountFromRepo(subject, repoid);

        return new PageList<Distribution>(distros, (int) count, pc);

    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    @SuppressWarnings("unchecked")
    public PageList<Advisory> findAssociatedAdvisory(Subject subject, int repoid, PageControl pc) {
        pc.setPrimarySort("rkt.id", PageOrdering.ASC);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, RepoAdvisory.QUERY_FIND_BY_REPO_ID, pc);

        query.setParameter("repoId", repoid);

        List<RepoAdvisory> results = query.getResultList();

        ArrayList<Advisory> advs = new ArrayList();
        for (RepoAdvisory result : results) {
            advs.add(result.getAdvisory());
        }
        log.debug("list of Advisory : " + advs + " associated to the repo: " + repoid);
        long count = getAdvisoryCountFromRepo(subject, repoid);

        return new PageList<Advisory>(advs, (int) count, pc);

    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public long getAdvisoryCountFromRepo(Subject subject, int repoId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, RepoAdvisory.QUERY_FIND_BY_REPO_ID);

        countQuery.setParameter("repoId", repoId);

        return (Long) countQuery.getSingleResult();
    }

    public String calculateSyncStatus(Subject subject, int repoId) {
        Repo found = this.getRepo(subject, repoId);
        List<RepoSyncResults> syncResults = found.getSyncResults();
        // Add the most recent sync results status
        int latestIndex = syncResults.size() - 1;
        if (!syncResults.isEmpty() && syncResults.get(latestIndex) != null) {
            RepoSyncResults results = syncResults.get(latestIndex);
            return results.getStatus().toString();
        } else {
            return ContentSyncStatus.NONE.toString();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults getMostRecentSyncResults(Subject subject, int repoId) {
        Repo found = this.getRepo(subject, repoId);
        List<RepoSyncResults> syncResults = found.getSyncResults();

        int latestIndex = syncResults.size() - 1;
        if (syncResults != null && (!syncResults.isEmpty()) && syncResults.get(latestIndex) != null) {
            return syncResults.get(latestIndex);
        } else {
            return null;
        }
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public int synchronizeRepos(Subject subject, int[] repoIds) throws Exception {
        int syncCount = 0;

        ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();

        for (int id : repoIds) {
            try {
                Repo repo = getRepo(subject, id);
                pc.syncRepoNow(repo);
                syncCount++;
            } catch (SchedulerException e) {
                log.error("Error synchronizing repo with id [" + id + "]", e);
            }
        }

        return syncCount;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    // TEMPORARY TIMEOUT DEFINED WHILE WE PROPERLY FIGURE OUT TIMEOUTS
    @TransactionTimeout(86400)
    public int internalSynchronizeRepos(Subject subject, Integer[] repoIds) throws InterruptedException {
        ContentServerPluginContainer pc;
        try {
            pc = ContentManagerHelper.getPluginContainer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ContentProviderManager providerManager = pc.getAdapterManager();

        int syncCount = 0;
        for (Integer id : repoIds) {
            boolean syncExecuted = providerManager.synchronizeRepo(id);

            if (syncExecuted) {
                syncCount++;
            }
        }

        return syncCount;
    }

    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public void cancelSync(Subject subject, int repoId) throws ContentException {
        ContentServerPluginContainer pc;
        try {
            pc = ContentManagerHelper.getPluginContainer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Repo repo = this.getRepo(subject, repoId);
        try {
            pc.cancelRepoSync(subject, repo);

        } catch (SchedulerException e) {
            throw new ContentException(e);
        }
        RepoSyncResults results = this.getMostRecentSyncResults(subject, repo.getId());
        results.setStatus(ContentSyncStatus.CANCELLING);
        repoManager.mergeRepoSyncResults(results);
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults persistRepoSyncResults(RepoSyncResults results) {

        ContentManagerHelper helper = new ContentManagerHelper(entityManager);
        Query q = entityManager.createNamedQuery(RepoSyncResults.QUERY_GET_INPROGRESS_BY_REPO_ID);
        q.setParameter("repoId", results.getRepo().getId());

        RepoSyncResults persistedSyncResults = (RepoSyncResults) helper.persistSyncResults(q, results);
        return (null != persistedSyncResults) ? persistedSyncResults : results;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_REPOSITORIES)
    public PageList<RepoSyncResults> getRepoSyncResults(Subject subject, int repoId, PageControl pc) {
        pc.initDefaultOrderingField("cssr.startTime", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            RepoSyncResults.QUERY_GET_ALL_BY_REPO_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, RepoSyncResults.QUERY_GET_ALL_BY_REPO_ID);

        query.setParameter("repoId", repoId);
        countQuery.setParameter("repoId", repoId);

        List<RepoSyncResults> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<RepoSyncResults>(results, (int) count, pc);
    }

    // we want this in its own tx so other tx's can see it immediately, even if calling method is already in a tx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RepoSyncResults mergeRepoSyncResults(RepoSyncResults results) {
        RepoSyncResults retval = entityManager.merge(results);
        return retval;
    }

    public RepoSyncResults getRepoSyncResults(int resultsId) {
        return entityManager.find(RepoSyncResults.class, resultsId);
    }

}