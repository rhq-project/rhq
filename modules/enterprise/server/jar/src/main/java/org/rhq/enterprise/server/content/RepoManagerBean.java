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

import java.util.ArrayList;
import java.util.HashSet;
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

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoContentSource;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.core.domain.content.RepoPackageVersion;
import org.rhq.core.domain.content.RepoRelationship;
import org.rhq.core.domain.content.RepoRelationshipType;
import org.rhq.core.domain.content.RepoRepoRelationship;
import org.rhq.core.domain.content.ResourceRepo;
import org.rhq.core.domain.content.composite.RepoComposite;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

@Stateless
public class RepoManagerBean implements RepoManagerLocal, RepoManagerRemote {
    private final Log log = LogFactory.getLog(RepoManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authzManager;
    @IgnoreDependency
    @EJB
    private ContentSourceManagerLocal contentSourceManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteRepo(Subject subject, int repoId) {
        log.debug("User [" + subject + "] is deleting repo [" + repoId + "]");

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
            log.debug("User [" + subject + "] deleted repo [" + repo + "]");
        } else {
            log.debug("Repo ID [" + repoId + "] doesn't exist - nothing to delete");
        }

        // remove any unused, orphaned package versions
        contentSourceManager.purgeOrphanedPackageVersions(subject);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteRepoGroup(Subject subject, int repoGroupId) {
        RepoGroup deleteMe = getRepoGroup(subject, repoGroupId);
        entityManager.remove(deleteMe);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Repo> findRepos(Subject subject, PageControl pc) {
        pc.initDefaultOrderingField("c.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Repo.QUERY_FIND_ALL_IMPORTED_REPOS, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Repo.QUERY_FIND_ALL_IMPORTED_REPOS);

        List<Repo> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Repo>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo getRepo(Subject subject, int repoId) {
        Repo repo = entityManager.find(Repo.class, repoId);

        if ((repo != null) && (repo.getRepoContentSources() != null)) {
            // load content sources separately. we can't do this all at once via fetch join because
            // on Oracle we use a LOB column on a content source field and you can't DISTINCT on LOBs
            repo.getRepoContentSources().size();
        }

        return repo;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public RepoGroup getRepoGroup(Subject subject, int repoGroupId) {
        RepoGroup repoGroup = entityManager.find(RepoGroup.class, repoGroupId);
        return repoGroup;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
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

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<RepoComposite> findAvailableResourceSubscriptions(Subject subject, int resourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID, pc);
        Query countQuery = entityManager
            .createNamedQuery(Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT);

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
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID);

        query.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, PageControl pc) {
        pc.initDefaultOrderingField("pv.generalPackage.name, pv.version");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE, pc);

        query.setParameter("repoId", repoId);

        List<PackageVersion> results = query.getResultList();
        long count = getPackageVersionCountFromRepo(subject, null, repoId);

        return new PageList<PackageVersion>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, String filter, PageControl pc) {
        pc.initDefaultOrderingField("pv.generalPackage.name, pv.version");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE_FILTERED, pc);

        query.setParameter("repoId", repoId);
        query.setParameter("filter", PersistenceUtility.formatSearchParameter(filter));

        List<PackageVersion> results = query.getResultList();
        long count = getPackageVersionCountFromRepo(subject, filter, repoId);

        return new PageList<PackageVersion>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo updateRepo(Subject subject, Repo repo) throws RepoException {
        if (repo.getName() == null || repo.getName().trim().equals("")) {
            throw new RepoException("Repo name is required");
        }

        // should we check non-null repo relationships and warn that we aren't changing them?
        log.debug("User [" + subject + "] is updating repo [" + repo + "]");
        repo = entityManager.merge(repo);
        log.debug("User [" + subject + "] updated repo [" + repo + "]");

        return repo;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo createRepo(Subject subject, Repo repo) throws RepoException {
        validateRepo(repo);

        repo.setCandidate(false);

        log.debug("User [" + subject + "] is creating repo [" + repo + "]");
        entityManager.persist(repo);
        log.debug("User [" + subject + "] created repo [" + repo + "]");

        return repo; // now has the ID set
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo createCandidateRepo(Subject subject, Repo repo) throws RepoException {
        validateRepo(repo);

        repo.setCandidate(true);

        entityManager.persist(repo);

        return repo;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
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
    @RequiredPermission(Permission.MANAGE_INVENTORY)
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
            ContentSource cs = entityManager.find(ContentSource.class, id);
            if (cs == null) {
                throw new Exception("There is no content source with id [" + id + "]");
            }

            RepoContentSource ccsmapping = repo.addContentSource(cs);
            entityManager.persist(ccsmapping);

            Set<PackageVersion> alreadyAssociatedPVs = new HashSet<PackageVersion>(repo.getPackageVersions());

            // automatically associate all of the content source's package versions with this repo
            // but, *skip* over the ones that are already linked to this repo from a previous association
            q.setParameter("id", cs.getId());
            List<PackageVersionContentSource> pvcss = q.getResultList();
            for (PackageVersionContentSource pvcs : pvcss) {
                PackageVersion pv = pvcs.getPackageVersionContentSourcePK().getPackageVersion();
                if (alreadyAssociatedPVs.contains(pv)) {
                    continue; // skip if already associated with this repo
                }
                RepoPackageVersion mapping = new RepoPackageVersion(repo, pv);
                entityManager.persist(mapping);
            }

            entityManager.flush();
            entityManager.clear();

            // ask to synchronize the content source immediately (is this the right thing to do?)
            pc.syncNow(cs);
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void addPackageVersionsToRepo(Subject subject, int repoId, int[] packageVersionIds) {
        Repo repo = entityManager.find(Repo.class, repoId);

        for (int packageVersionId : packageVersionIds) {
            PackageVersion packageVersion = entityManager.find(PackageVersion.class, packageVersionId);

            RepoPackageVersion mapping = new RepoPackageVersion(repo, packageVersion);
            entityManager.persist(mapping);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void removeContentSourcesFromRepo(Subject subject, int repoId, int[] contentSourceIds) throws RepoException {
        Repo repo = getRepo(subject, repoId);

        log.debug("User [" + subject + "] is removing content sources from repo [" + repo + "]");

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
                + "] does not have permission to unsubscribe this resource from repos");
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

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getPackageVersionCountFromRepo(Subject subject, String filter, int repoId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_FILTERED);

        countQuery.setParameter("repoId", repoId);
        countQuery.setParameter("filter", (filter == null) ? null : ("%" + filter.toUpperCase() + "%"));

        return ((Long) countQuery.getSingleResult()).longValue();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getPackageVersionCountFromRepo(Subject subject, int repoId) {
        return getPackageVersionCountFromRepo(subject, null, repoId);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Repo> findReposByCriteria(Subject subject, RepoCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<Repo> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInRepoByCriteria(Subject subject, PackageVersionCriteria criteria) {
        Integer repoId = criteria.getFilterRepoId();

        if ((null == repoId) || (repoId < 1)) {
            throw new IllegalArgumentException("Illegal filterResourceId: " + repoId);
        }

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        Query query = generator.getQuery(entityManager);
        Query countQuery = generator.getCountQuery(entityManager);

        long count = (Long) countQuery.getSingleResult();
        List<PackageVersion> packageVersions = query.getResultList();

        return new PageList<PackageVersion>(packageVersions, (int) count, criteria.getPageControl());
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
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

    private void validateRepo(Repo c) throws RepoException {
        if (c.getName() == null || c.getName().trim().equals("")) {
            throw new RepoException("Repo name is required");
        }

        List<Repo> repos = getRepoByName(c.getName());
        if (repos.size() != 0) {
            throw new RepoException("There is already a repo with the name of [" + c.getName() + "]");
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
            throw new RepoException("There is already a repo group with the name [" + repoGroup.getName() + "]");
        }
    }

}