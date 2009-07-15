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
package org.rhq.enterprise.server.resource.group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerException;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.domain.util.QueryGenerator;
import org.rhq.core.domain.util.QueryGenerator.AuthorizationTokenType;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.exception.CreateException;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.UnscheduleException;
import org.rhq.enterprise.server.exception.UpdateException;
import org.rhq.enterprise.server.operation.GroupOperationSchedule;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * A manager that provides methods for creating, updating, deleting, and querying
 * {@link org.rhq.core.domain.resource.group.ResourceGroup}s.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class ResourceGroupManagerBean implements ResourceGroupManagerLocal, ResourceGroupManagerRemote {
    private final Log log = LogFactory.getLog(ResourceGroupManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    @IgnoreDependency
    private OperationManagerLocal operationManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    @IgnoreDependency
    private ResourceTypeManagerLocal resourceTypeManager;
    @EJB
    @IgnoreDependency
    private ResourceManagerLocal resourceManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;
    private DatabaseType dbType;

    @PostConstruct
    public void init() {
        Connection conn = null;
        try {
            conn = rhqDs.getConnection();
            dbType = DatabaseTypeFactory.getDatabaseType(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            JDBCUtil.safeClose(conn);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup createResourceGroup(Subject user, ResourceGroup group) throws CreateException {
        try {
            /*
            GH: We are now allowing Groups where names collide... TODO, should this only be allowed for cluster auto backing groups?
            Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_NAME);
            query.setParameter("name", group.getName());

            List<ResourceGroup> groups = query.getResultList();
            if (groups.size() != 0) {
                throw new ResourceGroupAlreadyExistsException("ResourceGroup with name " + group.getName()
                    + " already exists");
            }*/

            long time = System.currentTimeMillis();
            group.setCtime(time);
            group.setMtime(time);
            group.setModifiedBy(user);

            entityManager.persist(group);
        } catch (Exception e) {
            throw new CreateException(e);
        }

        return group;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup updateResourceGroup(Subject user, ResourceGroup group, RecursivityChangeType changeType)
        throws ResourceGroupAlreadyExistsException, ResourceGroupUpdateException {

        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_NAME);
        query.setParameter("name", group.getName());

        int groupId = group.getId();
        try {
            ResourceGroup foundGroup = (ResourceGroup) query.getSingleResult();
            if (foundGroup.getId() == groupId) {
                // user is updating the same group and hasn't changed the name, this is OK
            } else {
                //  user is updating the group name to the name of an existing group - this is bad
                throw new ResourceGroupAlreadyExistsException("ResourceGroup with name " + group.getName()
                    + " already exists");
            }
        } catch (NoResultException e) {
            // user is changing the name of the group, this is OK
        }

        if (changeType == null) {
            ResourceGroup attachedGroup = entityManager.find(ResourceGroup.class, groupId);
            changeType = RecursivityChangeType.None;
            if (attachedGroup.isRecursive() == true && group.isRecursive() == false) {
                // making a recursive group into a "normal" group 
                changeType = RecursivityChangeType.RemovedRecursion;
            } else if (attachedGroup.isRecursive() == false && group.isRecursive() == true) {
                // making a "normal" group into a recursive group
                changeType = RecursivityChangeType.AddedRecursion;
            } else {
                // recursive bit didn't change
            }
        }

        long time = System.currentTimeMillis();
        group.setMtime(time);
        group.setModifiedBy(user);

        ResourceGroup newlyAttachedGroup = entityManager.merge(group);
        if (changeType == RecursivityChangeType.AddedRecursion) {
            newlyAttachedGroup.setRecursive(true);
            enableRecursivityForGroup(user, groupId);
        } else if (changeType == RecursivityChangeType.RemovedRecursion) {
            newlyAttachedGroup.setRecursive(false);
            clearImplicitResources(groupId);
            makeImplicitMirrorExplicit(groupId);
        }
        return newlyAttachedGroup;
    }

    private void clearImplicitResources(int resourceGroupId) throws ResourceGroupUpdateException {
        Connection conn = null;
        PreparedStatement removeImplicitStatement = null;
        try {
            conn = rhqDs.getConnection();

            removeImplicitStatement = conn.prepareStatement(ResourceGroup.QUERY_UPDATE_REMOVE_IMPLICIT);
            removeImplicitStatement.setInt(1, resourceGroupId);
            removeImplicitStatement.executeUpdate();
        } catch (SQLException sqle) {
            log.error("Error removing implicit resources from group[id=" + resourceGroupId + "]: ", sqle);
            throw new ResourceGroupUpdateException("Error removing implicit resources from group[id=" + resourceGroupId
                + "]: " + sqle.getMessage());
        } finally {
            JDBCUtil.safeClose(removeImplicitStatement);
            JDBCUtil.safeClose(conn);
        }
    }

    private void makeImplicitMirrorExplicit(int resourceGroupId) throws ResourceGroupUpdateException {
        Connection conn = null;
        PreparedStatement updateImplicitMirrorExplicitStatement = null;
        try {
            conn = rhqDs.getConnection();

            updateImplicitMirrorExplicitStatement = conn
                .prepareStatement(ResourceGroup.QUERY_UPDATE_IMPLICIT_MIRROR_EXPLICIT);
            updateImplicitMirrorExplicitStatement.setInt(1, resourceGroupId);
            updateImplicitMirrorExplicitStatement.executeUpdate();
        } catch (SQLException sqle) {
            log.error("Error making implicit resources mirror explicit resources for group[id=" + resourceGroupId
                + "]: ", sqle);
            throw new ResourceGroupUpdateException(
                "Error making implicit resources mirror explicit resources for group[id=" + resourceGroupId + "]: "
                    + sqle.getMessage());
        } finally {
            JDBCUtil.safeClose(updateImplicitMirrorExplicitStatement);
            JDBCUtil.safeClose(conn);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteResourceGroup(Subject user, int groupId) throws DeleteException {
        try {
            ResourceGroup group = getResourceGroupById(user, groupId, null);

            // for compatible groups, first recursively remove any referring backing groups for auto-clusters
            if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {
                for (ResourceGroup referringGroup : group.getClusterBackingGroups()) {
                    deleteResourceGroup(user, referringGroup.getId());
                }
            }

            // unschedule all jobs for this group (only compatible groups have operations, mixed do not)
            if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {
                Subject overlord = subjectManager.getOverlord();
                try {
                    List<GroupOperationSchedule> ops = operationManager.findScheduledGroupOperations(overlord, groupId);

                    for (GroupOperationSchedule schedule : ops) {
                        try {
                            operationManager
                                .unscheduleGroupOperation(overlord, schedule.getJobId().toString(), groupId);
                        } catch (UnscheduleException e) {
                            log.warn("Failed to unschedule job [" + schedule + "] for a group being deleted [" + group
                                + "]", e);
                        }
                    }
                } catch (SchedulerException e1) {
                    log.warn("Failed to get jobs for a group being deleted [" + group
                        + "]; will not attempt to unschedule anything", e1);
                }
            }

            for (Role doomedRoleRelationship : group.getRoles()) {
                group.removeRole(doomedRoleRelationship);
                entityManager.merge(doomedRoleRelationship);
            }

            // remove all resources in the group
            resourceGroupManager.removeAllResourcesFromGroup(user, groupId);

            // break resource and plugin configuration update links in order to preserve individual change history
            Query q = null;

            q = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_DELETE_GROUP_UPDATES_FOR_GROUP);
            q.setParameter("groupId", groupId);
            q.executeUpdate();

            q = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_DELETE_GROUP_UPDATES_FOR_GROUP);
            q.setParameter("groupId", groupId);
            q.executeUpdate();

            entityManager.remove(group);
        } catch (Exception e) {
            throw new DeleteException(e);
        }
    }

    public ResourceGroup getResourceGroupById(Subject user, int id, GroupCategory category)
        throws ResourceGroupNotFoundException {
        ResourceGroup group = entityManager.find(ResourceGroup.class, id);

        if (group == null) {
            throw new ResourceGroupNotFoundException(id);
        }

        if (!authorizationManager.canViewGroup(user, group.getId())) {
            throw new PermissionException("You do not have permission to view this resource group");
        }

        // null category means calling context doesn't care about category
        if ((category != null) && (category != group.getGroupCategory())) {
            throw new ResourceGroupNotFoundException("Expected group to belong to '" + category + "' category, "
                + "it belongs to '" + group.getGroupCategory() + "' category instead");
        }

        initLazyFields(group);

        return group;
    }

    private void initLazyFields(ResourceGroup group) {
        /*
         * initialize modifiedBy field, which is now a lazily- loaded relationship to speed up the GroupHub stuff
         */
        if (group.getModifiedBy() != null) {
            group.getModifiedBy().getId();
        }
    }

    public int getResourceGroupCountByCategory(Subject subject, GroupCategory category) {
        Query queryCount;

        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_BY_CATEGORY_COUNT_admin);
        } else {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_BY_CATEGORY_COUNT);
            queryCount.setParameter("subject", subject);
        }

        queryCount.setParameter("category", category);

        long count = (Long) queryCount.getSingleResult();

        return (int) count;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void enableRecursivityForGroup(Subject subject, int groupId) throws ResourceGroupNotFoundException,
        ResourceGroupUpdateException {

        // step 1: clear the implicit and preparation for adding a different set of resources to it
        clearImplicitResources(groupId);

        // step 2: prepare the list of resources to be used to pass to the method that does the recursive logic
        List<Integer> explicitResourceIdList = resourceManager.findExplicitResourceIdsByResourceGroup(groupId);

        // step 3: add the explicit resources back, this time with the recursive bit flipped on
        addResourcesToGroupImplicit(subject, groupId, explicitResourceIdList, false, true);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds) throws UpdateException {
        Integer[] ids = ArrayUtils.wrapInArray(resourceIds);
        if (ids == null || ids.length == 0) {
            return;
        }

        try {
            boolean isRecursive = isRecursive(groupId); // will perform check for group existence

            // batch the removes to prevent the ORA error about IN clauses containing more than 1000 items
            for (int batchIndex = 0; batchIndex < ids.length; batchIndex += 1000) {
                Integer[] batchIdArray = ArrayUtils.copyOfRange(ids, batchIndex, batchIndex + 1000);
                List<Integer> batchIds = Arrays.asList(batchIdArray);

                addResourcesToGroupImplicit(subject, groupId, batchIds, true, isRecursive);
                addResourcesToGroupExplicit(subject, groupId, batchIds, isRecursive);
            }
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }

    private void addResourcesToGroupExplicit(Subject subject, Integer groupId, List<Integer> resourceIds,
        boolean isRecursive) throws ResourceGroupUpdateException {
        // nothing to add
        if (resourceIds == null || resourceIds.size() == 0) {
            return;
        }

        List<Integer> nonMemberResources = getNonMemberExplicitResources(groupId, resourceIds);
        if (nonMemberResources.size() == 0) {
            // everybody was already a member
            return;
        }
        int[] resourceIdsToAdd = ArrayUtils.unwrapCollection(nonMemberResources);

        Connection conn = null;
        PreparedStatement insertExplicitStatement = null;
        try {
            conn = rhqDs.getConnection();

            // insert explicit resources
            String insertExplicitQueryString = JDBCUtil
                .transformQueryForMultipleInParameters(ResourceGroup.QUERY_NATIVE_ADD_RESOURCES_TO_GROUP_EXPLICIT,
                    "@@RESOURCE_IDS@@", resourceIdsToAdd.length);
            insertExplicitStatement = conn.prepareStatement(insertExplicitQueryString);
            insertExplicitStatement.setInt(1, groupId);
            JDBCUtil.bindNTimes(insertExplicitStatement, resourceIdsToAdd, 2);
            insertExplicitStatement.executeUpdate();
        } catch (SQLException sqle) {
            log.error("Error adding resources to group[id=" + groupId + "]: ", sqle);
            throw new ResourceGroupUpdateException("Error adding resources from group[id=" + groupId + "]: "
                + sqle.getMessage());
        } finally {
            JDBCUtil.safeClose(insertExplicitStatement);
            JDBCUtil.safeClose(conn);
        }
        return;
    }

    private void addResourcesToGroupImplicit(Subject subject, Integer groupId, List<Integer> resourceIds,
        boolean filterByExplicitMembership, boolean isRecursive) throws ResourceGroupUpdateException {
        if (resourceIds == null || resourceIds.size() == 0) {
            // nothing to add
            return;
        }

        int[] resourceIdsToAdd;
        if (filterByExplicitMembership) {
            List<Integer> nonMemberResources = getNonMemberExplicitResources(groupId, resourceIds);
            if (nonMemberResources.size() == 0) {
                // everybody was already a member
                return;
            }
            resourceIdsToAdd = ArrayUtils.unwrapCollection(nonMemberResources);
        } else {
            resourceIdsToAdd = ArrayUtils.unwrapCollection(resourceIds);
        }

        Connection conn = null;
        PreparedStatement insertExplicitStatement = null;
        PreparedStatement insertImplicitStatement = null;
        try {
            conn = rhqDs.getConnection();

            // insert implicit resources
            if (isRecursive) {
                insertImplicitStatement = conn
                    .prepareStatement(ResourceGroup.QUERY_NATIVE_ADD_RESOURCES_TO_GROUP_IMPLICIT_RECURSIVE);
                insertImplicitStatement.setInt(1, groupId);
                insertImplicitStatement.setInt(9, groupId);
                for (int resourceId : resourceIdsToAdd) {
                    insertImplicitStatement.setInt(2, resourceId);
                    insertImplicitStatement.setInt(3, resourceId);
                    insertImplicitStatement.setInt(4, resourceId);
                    insertImplicitStatement.setInt(5, resourceId);
                    insertImplicitStatement.setInt(6, resourceId);
                    insertImplicitStatement.setInt(7, resourceId);
                    insertImplicitStatement.setInt(8, resourceId);
                    insertImplicitStatement.executeUpdate();
                }
            } else {
                String insertImplicitQueryString = JDBCUtil.transformQueryForMultipleInParameters(
                    ResourceGroup.QUERY_NATIVE_ADD_RESOURCES_TO_GROUP_IMPLICIT, "@@RESOURCE_IDS@@",
                    resourceIdsToAdd.length);
                insertImplicitStatement = conn.prepareStatement(insertImplicitQueryString);
                insertImplicitStatement.setInt(1, groupId);
                JDBCUtil.bindNTimes(insertImplicitStatement, resourceIdsToAdd, 2);
                insertImplicitStatement.executeUpdate();
            }
        } catch (SQLException sqle) {
            log.error("Error adding resources to group[id=" + groupId + "]: ", sqle);
            throw new ResourceGroupUpdateException("Error adding resources from group[id=" + groupId + "]: "
                + sqle.getMessage());
        } finally {
            JDBCUtil.safeClose(insertExplicitStatement);
            JDBCUtil.safeClose(insertImplicitStatement);
            JDBCUtil.safeClose(conn);
        }
        return;
    }

    private boolean isRecursive(int groupId) {
        Subject overlord = subjectManager.getOverlord();
        ResourceGroup attachedGroup = getResourceGroupById(overlord, groupId, null);
        return attachedGroup.isRecursive();
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getNonMemberExplicitResources(int groupId, List<Integer> resourceIds) {
        if (resourceIds == null || resourceIds.size() == 0) {
            return Collections.emptyList();
        }
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_RESOURCE_IDS_NOT_IN_GROUP_EXPLICIT);
        query.setParameter("groupId", groupId);
        query.setParameter("resourceIds", resourceIds);
        List<Integer> nonMemberResources = query.getResultList();
        return nonMemberResources;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds) throws UpdateException {
        Integer[] ids = ArrayUtils.wrapInArray(resourceIds);
        if (ids == null || ids.length == 0) {
            return;
        }

        try {
            boolean isRecursive = isRecursive(groupId); // will perform check for group existence

            // batch the removes to prevent the ORA error about IN clauses containing more than 1000 items
            for (int batchIndex = 0; batchIndex < ids.length; batchIndex += 1000) {
                Integer[] batchIdArray = ArrayUtils.copyOfRange(ids, batchIndex, batchIndex + 1000);

                removeResourcesFromGroup_helper(subject, groupId, batchIdArray, isRecursive);
            }
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }

    private void removeResourcesFromGroup_helper(Subject subject, Integer groupId, Integer[] resourceIds,
        boolean isRecursive) throws ResourceGroupNotFoundException, ResourceGroupUpdateException {

        List<Integer> nonMembersToBeRemoved = getNonMemberExplicitResources(groupId, Arrays.asList(resourceIds));
        if (nonMembersToBeRemoved.size() != 0) {
            throw new ResourceGroupUpdateException("Can not remove resources[" + nonMembersToBeRemoved
                + "] which are not part of the group[id=" + groupId + "]");
        }

        int[] resourceIdsToRemove = ArrayUtils.unwrapArray(resourceIds);

        Connection conn = null;
        PreparedStatement deleteExplicitStatement = null;
        PreparedStatement deleteImplicitStatement = null;
        try {
            conn = rhqDs.getConnection();

            // insert implicit resources, must occur before deleting explicit
            if (isRecursive) {
                deleteImplicitStatement = conn
                    .prepareStatement(ResourceGroup.QUERY_NATIVE_REMOVE_RESOURCES_FROM_GROUP_IMPLICIT_RECURSIVE);
                deleteImplicitStatement.setInt(1, groupId);
                deleteImplicitStatement.setInt(9, groupId);
                for (int resourceId : resourceIdsToRemove) {
                    // no-op if this resource's ancestor is also in the explicit list
                    List<Integer> lineage = resourceManager.getResourceIdLineage(resourceId);
                    List<Integer> nonMembers = getNonMemberExplicitResources(groupId, lineage);
                    if (lineage.size() != nonMembers.size()) {
                        // one or more of my parents were in the explicit list, no-op to remove me
                        continue;
                    }
                    deleteImplicitStatement.setInt(2, resourceId);
                    deleteImplicitStatement.setInt(3, resourceId);
                    deleteImplicitStatement.setInt(4, resourceId);
                    deleteImplicitStatement.setInt(5, resourceId);
                    deleteImplicitStatement.setInt(6, resourceId);
                    deleteImplicitStatement.setInt(7, resourceId);
                    deleteImplicitStatement.setInt(8, resourceId);
                    deleteImplicitStatement.setInt(10, resourceId);
                    deleteImplicitStatement.executeUpdate();
                }
            } else {
                String deleteImplicitQueryString = JDBCUtil.transformQueryForMultipleInParameters(
                    ResourceGroup.QUERY_NATIVE_REMOVE_RESOURCES_FROM_GROUP_IMPLICIT, "@@RESOURCE_IDS@@",
                    resourceIdsToRemove.length);
                deleteImplicitStatement = conn.prepareStatement(deleteImplicitQueryString);
                deleteImplicitStatement.setInt(1, groupId);
                JDBCUtil.bindNTimes(deleteImplicitStatement, resourceIdsToRemove, 2);
                deleteImplicitStatement.executeUpdate();
            }

            // delete explicit resources
            String deleteExplicitQueryString = JDBCUtil.transformQueryForMultipleInParameters(
                ResourceGroup.QUERY_NATIVE_REMOVE_RESOURCES_FROM_GROUP_EXPLICIT, "@@RESOURCE_IDS@@",
                resourceIdsToRemove.length);
            deleteExplicitStatement = conn.prepareStatement(deleteExplicitQueryString);
            deleteExplicitStatement.setInt(1, groupId);
            JDBCUtil.bindNTimes(deleteExplicitStatement, resourceIdsToRemove, 2);
            deleteExplicitStatement.executeUpdate();
        } catch (SQLException sqle) {
            log.error("Error removing resources from group[id=" + groupId + "]: ", sqle);
            throw new ResourceGroupUpdateException("Error removing resources from group[id=" + groupId + "]: "
                + sqle.getMessage());
        } finally {
            JDBCUtil.safeClose(deleteExplicitStatement);
            JDBCUtil.safeClose(deleteImplicitStatement);
            JDBCUtil.safeClose(conn);
        }
        return;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeAllResourcesFromGroup(Subject subject, int groupId) throws ResourceGroupDeleteException {
        Connection conn = null;
        PreparedStatement explicitStatement = null;
        PreparedStatement implicitStatement = null;
        try {
            conn = rhqDs.getConnection();

            explicitStatement = conn
                .prepareStatement("delete from rhq_resource_group_res_exp_map where resource_group_id = ?");
            implicitStatement = conn
                .prepareStatement("delete from rhq_resource_group_res_imp_map where resource_group_id = ?");

            explicitStatement.setInt(1, groupId);
            implicitStatement.setInt(1, groupId);

            explicitStatement.executeUpdate();
            implicitStatement.executeUpdate();
        } catch (SQLException sqle) {
            log.error("Error removing group resources", sqle);
            throw new ResourceGroupDeleteException("Error removing group resources: " + sqle.getMessage());
        } finally {
            JDBCUtil.safeClose(explicitStatement);
            JDBCUtil.safeClose(implicitStatement);
            JDBCUtil.safeClose(conn);
        }
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    @SuppressWarnings("unchecked")
    public PageList<ResourceGroup> findAvailableResourceGroupsForRole(Subject subject, int roleId, int[] excludeIds,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("rg.name");

        List<Integer> excludeList = ArrayUtils.wrapInList(excludeIds);

        final String queryName;
        if ((excludeList != null) && (excludeList.size() != 0)) {
            queryName = ResourceGroup.QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE_WITH_EXCLUDES;
        } else {
            queryName = ResourceGroup.QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE;
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pageControl);

        if ((excludeList != null) && (excludeList.size() != 0)) {
            queryCount.setParameter("excludeIds", excludeList);
            query.setParameter("excludeIds", excludeList);
        }

        queryCount.setParameter("roleId", roleId);
        query.setParameter("roleId", roleId);

        long count = (Long) queryCount.getSingleResult();
        List<ResourceGroup> groups = query.getResultList();

        return new PageList<ResourceGroup>(groups, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceGroup> findResourceGroupByIds(Subject subject, int[] resourceGroupIds,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("rg.name");

        List<Integer> groupIdList = ArrayUtils.wrapInList(resourceGroupIds);

        if ((groupIdList == null) || (groupIdList.size() == 0)) {
            return new PageList<ResourceGroup>(pageControl);
        }

        Query queryCount = null;
        Query query = null;

        if (authorizationManager.isInventoryManager(subject)) {
            final String queryName = ResourceGroup.QUERY_FIND_BY_IDS_admin;
            queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pageControl);
        } else {
            final String queryName = ResourceGroup.QUERY_FIND_BY_IDS;
            queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pageControl);
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("ids", groupIdList);
        query.setParameter("ids", groupIdList);

        long count = (Long) queryCount.getSingleResult();
        List<ResourceGroup> groups = query.getResultList();

        return new PageList<ResourceGroup>(groups, (int) count, pageControl);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @SuppressWarnings("unchecked")
    public void updateImplicitGroupMembership(Subject subject, Resource resource) {
        /*
         * Get all the groups the parent of this resource is implicitly in. This will tell us which we need to update
         * (because we added new descendants).
         */
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_IMPLICIT_BY_RESOURCE_ID);
        query.setParameter("id", resource.getParentResource().getId());
        List<ResourceGroup> groups = query.getResultList();

        // the resource isn't currently in a group -> no work to do
        if (groups.size() == 0) {
            return;
        }

        List<ResourceGroup> recursiveGroups = new ArrayList<ResourceGroup>(groups.size());
        for (ResourceGroup group : groups) {
            if (group.isRecursive()) {
                recursiveGroups.add(group);
            }
        }

        /*
         * BFS-construct the resource tree
         */
        List<Resource> resourceTree = new ArrayList<Resource>();
        List<Resource> toBeSearched = new LinkedList<Resource>();
        toBeSearched.add(resource);
        while (toBeSearched.size() > 0) {
            Resource next = toBeSearched.remove(0);
            resourceTree.add(next);
            toBeSearched.addAll(next.getChildResources());
        }

        /*
         * We should add this resource and all of its descendants to whatever recursive groups this resource's parent is
         * in
         */
        for (ResourceGroup implicitRecursiveGroup : recursiveGroups) {
            for (Resource implicitResource : resourceTree) {
                // cardinal rule, add the relationship in both directions
                implicitRecursiveGroup.addImplicitResource(implicitResource);
                implicitResource.getImplicitGroups().add(implicitRecursiveGroup);
            }

            /*
             * when automatically updating recursive groups during inventory sync we need to make sure that we also
             * update the resourceGroup; this will realistically only happen when a recursive group definition is
             * created, but which initially creates one or more resource groups of size 1; if this happens, the
             * group will be created as a compatible group, if resources are later added via an inventory sync, and
             * if this compatible group's membership changes, we need to recalculate the group category
             */
            setResourceType(implicitRecursiveGroup.getId());
        }

        /*
         * Merge all the participants after the relationships are constructed, and save the return values so callers can
         * work with attached objects.
         */
        for (ResourceGroup implicitRecursiveGroup : recursiveGroups) {
            entityManager.merge(implicitRecursiveGroup);
        }

        for (Resource implicitResource : resourceTree) {
            entityManager.merge(implicitResource);
        }
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal#findResourcesForAutoGroup(org.jboss.on.domain.auth.Subject,
     * int, int)
     */
    @SuppressWarnings("unchecked")
    public List<Resource> findResourcesForAutoGroup(Subject subject, int autoGroupParentResourceId,
        int autoGroupChildResourceTypeId) {
        List<Resource> resources;
        try {
            Query q = entityManager.createNamedQuery(Resource.QUERY_FIND_FOR_AUTOGROUP);
            q.setParameter("type", autoGroupChildResourceTypeId);
            q.setParameter("parent", autoGroupParentResourceId);
            q.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
            //         q.setParameter("subject", subject);
            resources = q.getResultList();
        } catch (PersistenceException pe) {
            return new ArrayList<Resource>();
        }

        return resources;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal#findResourcesForResourceGroup(org.jboss.on.domain.auth.Subject,
     * int)
     */
    @SuppressWarnings("unchecked")
    public List<Resource> findResourcesForResourceGroup(Subject subject, int groupId, GroupCategory category) {
        ResourceGroup group = getResourceGroupById(subject, groupId, category);
        Set<Resource> res = group.getExplicitResources();
        if (res != null && res.size() > 0) {
            List<Resource> resources = PersistenceUtility.getHibernateSession(entityManager).createFilter(res,
                "where this.inventoryStatus = :inventoryStatus").setParameter("inventoryStatus",
                InventoryStatus.COMMITTED).list();

            return resources;
        } else {

            List<Resource> ret = new ArrayList<Resource>(res.size());
            ret.addAll(res);

            return ret;
        }
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal#findDefinitionsForAutoGroup(org.jboss.on.domain.auth.Subject,
     * int, int)
     */
    public int[] findDefinitionsForAutoGroup(Subject subject, int autoGroupParentResourceId,
        int autoGroupChildResourceTypeId, boolean displayTypeSummaryOnly) {
        int[] ret;
        try {
            ResourceType rt = entityManager.find(ResourceType.class, autoGroupChildResourceTypeId);
            ret = getMeasurementDefinitionIdsForResourceType(rt, displayTypeSummaryOnly);
        } catch (EntityNotFoundException enfe) {
            ret = new int[0];
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal#findDefinitionsForCompatibleGroup(org.jboss.on.domain.auth.Subject,
     * int) TODO rework
     */
    public int[] findDefinitionsForCompatibleGroup(Subject subject, int groupId, boolean displayTypeSummaryOnly) {
        int[] ret = new int[0];
        try {
            ResourceGroup group = getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);
            Set<Resource> resources = group.getExplicitResources();
            if ((resources != null) && (resources.size() > 0)) {
                Resource resource = resources.iterator().next();
                ResourceType type = resource.getResourceType();
                ret = getMeasurementDefinitionIdsForResourceType(type, displayTypeSummaryOnly);
            }
        } catch (ResourceGroupNotFoundException e) {
            log.debug("Resources for groupID: " + groupId + " not found " + e);
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    private int[] getMeasurementDefinitionIdsForResourceType(ResourceType type, boolean summariesOnly) {
        String queryString = "" //
            + "SELECT id " //
            + "  FROM MeasurementDefinition md " //
            + " WHERE md.resourceType.id = :resourceTypeId ";

        queryString += " AND md.dataType = :dataType";
        if (summariesOnly) {
            queryString += " AND md.displayType = :dispType";
        }

        // should respect the ordering
        queryString += " ORDER BY md.displayOrder, md.displayName";

        Query q = entityManager.createQuery(queryString);
        q.setParameter("resourceTypeId", type.getId());
        q.setParameter("dataType", DataType.MEASUREMENT);
        if (summariesOnly) {
            q.setParameter("dispType", DisplayType.SUMMARY);
        }

        List<Integer> res = q.getResultList();
        int[] ret = new int[res.size()];
        int i = 0;
        for (Integer r : res) {
            ret[i++] = r;
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public ResourceGroup getByGroupDefinitionAndGroupByClause(int groupDefinitionId, String groupByClause) {
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_GROUP_DEFINITION_AND_EXPRESSION);

        /*
         * since oracle interprets empty strings as null, let's cleanse the
         * groupByClause so that the processing is identical on postgres
         */
        if (groupByClause.equals("")) {
            groupByClause = null;
        }
        query.setParameter("groupDefinitionId", groupDefinitionId);
        query.setParameter("groupByClause", groupByClause);

        List<ResourceGroup> groups = query.getResultList();

        if (groups.size() == 1) {
            // fyi, database constraints prevent dups on these two attributes
            ResourceGroup group = groups.get(0);
            return group;
        } else // if ( groups.size() == 0 )
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public void setResourceType(int resourceGroupId) {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_GET_EXPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP);
        query.setParameter("groupId", resourceGroupId);

        Subject overlord = subjectManager.getOverlord();
        ResourceGroup resourceGroup = getResourceGroupById(overlord, resourceGroupId, null);

        List results = query.getResultList();
        if (results.size() == 1) {
            Object[] info = (Object[]) results.get(0);
            int resourceTypeId = (Integer) info[0];

            try {
                ResourceType type = resourceTypeManager.getResourceTypeById(overlord, resourceTypeId);

                resourceGroup.setResourceType(type);
            } catch (ResourceTypeNotFoundException rtnfe) {
                // we just got the resourceTypeId from the database, so it will exist
                // but let's set some reasonable implementation anyway
                resourceGroup.setResourceType(null);
            }
        } else {
            resourceGroup.setResourceType(null);
        }
    }

    public int getImplicitGroupMemberCount(int resourceGroupId) {
        Query countQuery = entityManager
            .createNamedQuery(Resource.QUERY_FIND_IMPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN);
        countQuery.setParameter("groupId", resourceGroupId);
        long count = (Long) countQuery.getSingleResult();
        return (int) count;
    }

    // note, resourceId and groupId can both be NULL, and so must use the numeric wrapper classes
    public PageList<ResourceGroupComposite> findResourceGroupComposites(Subject subject, GroupCategory groupCategory,
        ResourceCategory resourceCategory, ResourceType resourceType, String nameFilter, Integer resourceId,
        Integer groupId, PageControl pc) {

        String query = ResourceGroup.QUERY_NATIVE_FIND_FILTERED_MEMBER;
        if (authorizationManager.isInventoryManager(subject)) {
            query = query.replace("%SECURITY_FRAGMENT_JOIN%", "");
            query = query.replace("%SECURITY_FRAGMENT_WHERE%", "");
        } else {
            // add the security fragments when not the inventory manager
            query = query.replace("%SECURITY_FRAGMENT_JOIN%",
                ResourceGroup.QUERY_NATIVE_FIND_FILTERED_MEMBER_SECURITY_FRAGMENT_JOIN);
            query = query.replace("%SECURITY_FRAGMENT_WHERE%",
                ResourceGroup.QUERY_NATIVE_FIND_FILTERED_MEMBER_SECURITY_FRAGMENT_WHERE);
        }

        if (resourceId != null) {
            query = query.replace("%RESOURCE_FRAGMENT_WHERE%",
                ResourceGroup.QUERY_NATIVE_FIND_FILTERED_MEMBER_RESOURCE_FRAGMENT_WHERE);
        } else {
            query = query.replace("%RESOURCE_FRAGMENT_WHERE%", "");
        }

        pc.initDefaultOrderingField("groupName");
        pc.truncateOrderingFields(1); // remove all but the primary sort
        OrderingField primary = pc.getOrderingFields().get(0);
        String field = primary.getField();
        if (field.endsWith("Avail")) {
            String prefix = field.substring(0, field.length() - 5);
            String secondaryField = prefix + "Count";
            pc.addDefaultOrderingField(secondaryField, primary.getOrdering());
        }
        if (field.equals("groupName") == false) {
            pc.addDefaultOrderingField("groupName");
        }
        nameFilter = PersistenceUtility.formatSearchParameter(nameFilter);

        Connection conn = null;
        PreparedStatement stmt = null;

        List<Object[]> rawResults = new ArrayList<Object[]>();
        try {
            conn = rhqDs.getConnection();

            if (groupId == null) {
                // only filter by visibility if the user isn't selecting a group directly
                if (this.dbType instanceof PostgresqlDatabaseType || this.dbType instanceof H2DatabaseType) {
                    query = query.replace("%GROUP_AND_VISIBILITY_FRAGMENT_WHERE%", "rg.visible = TRUE");
                } else if (this.dbType instanceof OracleDatabaseType || this.dbType instanceof SQLServerDatabaseType) {
                    query = query.replace("%GROUP_AND_VISIBILITY_FRAGMENT_WHERE%", "rg.visible = 1");
                } else {
                    throw new RuntimeException("Unknown database type: " + this.dbType);
                }
            } else {
                // otherwise filter by the passed groupId
                query = query.replace("%GROUP_AND_VISIBILITY_FRAGMENT_WHERE%", "rg.id = ?");
            }

            if (this.dbType instanceof PostgresqlDatabaseType) {
                query = PersistenceUtility.addPostgresNativePagingSortingToQuery(query, pc);
            } else if (this.dbType instanceof OracleDatabaseType) {
                query = PersistenceUtility.addOracleNativePagingSortingToQuery(query, pc);
            } else if (this.dbType instanceof H2DatabaseType) {
                query = PersistenceUtility.addH2NativePagingSortingToQuery(query, pc);
            } else if (this.dbType instanceof SQLServerDatabaseType) {
                query = PersistenceUtility.addSQLServerNativePagingSortingToQuery(query, pc);
            } else {
                throw new RuntimeException("Unknown database type: " + this.dbType);
            }

            stmt = conn.prepareStatement(query);

            String search = nameFilter;
            Integer resourceTypeId = resourceType == null ? null : resourceType.getId();
            String resourceCategoryName = resourceCategory == null ? null : resourceCategory.name();
            String groupCategoryName = groupCategory == null ? null : groupCategory.name();

            int i = 0;
            if (resourceId != null) {
                stmt.setInt(++i, resourceId);
            }
            if (groupId != null) {
                stmt.setInt(++i, groupId);
            }
            stmt.setString(++i, search);
            stmt.setString(++i, search);
            stmt.setString(++i, search);
            if (resourceTypeId == null) {
                stmt.setNull(++i, Types.INTEGER);
                stmt.setNull(++i, Types.INTEGER);
            } else {
                stmt.setInt(++i, resourceTypeId);
                stmt.setInt(++i, resourceTypeId);
            }
            stmt.setString(++i, resourceCategoryName);
            stmt.setString(++i, resourceCategoryName);
            if (resourceTypeId == null) {
                stmt.setNull(++i, Types.INTEGER);
            } else {
                stmt.setInt(++i, resourceTypeId);
            }
            stmt.setString(++i, resourceCategoryName);
            stmt.setString(++i, groupCategoryName);
            stmt.setString(++i, groupCategoryName);

            if (authorizationManager.isInventoryManager(subject) == false) {
                stmt.setInt(++i, subject.getId());
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long explicitCount = rs.getLong(1);
                double explicitAvail = rs.getDouble(2);
                long implicitCount = rs.getLong(3);
                double implicitAvail = rs.getDouble(4);
                int groupKey = rs.getInt(5);
                Object[] next = new Object[] { explicitCount, explicitAvail, implicitCount, implicitAvail, groupKey };
                rawResults.add(next);
            }
        } catch (Throwable t) {
            log.error("Could not execute groups query [ " + query + " ]: ", t);
            return new PageList<ResourceGroupComposite>();
        } finally {
            JDBCUtil.safeClose(conn, stmt, null);
        }

        Query queryCount = null;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_FILTERED_COUNT_ADMIN);
        } else {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_FILTERED_COUNT);
            queryCount.setParameter("subject", subject);
        }

        queryCount.setParameter("groupCategory", groupCategory);
        queryCount.setParameter("category", resourceCategory);
        queryCount.setParameter("resourceType", resourceType);
        queryCount.setParameter("search", nameFilter);
        queryCount.setParameter("resourceId", resourceId);
        queryCount.setParameter("groupId", groupId);

        long count = (Long) queryCount.getSingleResult();

        List<Integer> groupIds = new ArrayList<Integer>();
        for (Object[] row : rawResults) {
            groupIds.add(((Number) row[4]).intValue());
        }
        Map<Integer, ResourceGroup> groupMap = getIdGroupMap(groupIds);

        List<ResourceGroupComposite> results = new ArrayList<ResourceGroupComposite>(rawResults.size());
        int i = 0;
        for (Object[] row : rawResults) {
            long explicitCount = (Long) row[0];
            double explicitAvail = (Double) row[1];
            long implicitCount = (Long) row[2];
            double implicitAvail = (Double) row[3];
            ResourceGroup group = groupMap.get(groupIds.get(i++));
            ResourceType type = group.getResourceType();
            ResourceFacets facets;
            if (type == null) {
                // mixed group
                facets = ResourceFacets.NONE;
            } else {
                // compatible group
                facets = resourceTypeManager.getResourceFacets(group.getResourceType().getId());
            }
            ResourceGroupComposite composite = new ResourceGroupComposite(explicitCount, explicitAvail, implicitCount,
                implicitAvail, group, facets);
            results.add(composite);
        }

        return new PageList<ResourceGroupComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, ResourceGroup> getIdGroupMap(List<Integer> groupIds) {
        if (groupIds == null || groupIds.size() == 0) {
            return new HashMap<Integer, ResourceGroup>();
        }

        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_IDS_admin);
        query.setParameter("ids", groupIds);
        List<ResourceGroup> groups = query.getResultList();

        Map<Integer, ResourceGroup> results = new HashMap<Integer, ResourceGroup>();
        for (ResourceGroup group : groups) {
            results.put(group.getId(), group);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    // returns a subset of the passed groupIds which no longer exist 
    public List<Integer> findDeletedResourceGroupIds(int[] possibleGroupIds) {
        List<Integer> groupIds = ArrayUtils.wrapInList(possibleGroupIds);
        if (groupIds == null || groupIds.size() == 0) {
            return Collections.emptyList();
        }

        String queryString = "" //
            + "SELECT rg.id " //
            + "  FROM ResourceGroup rg " //
            + " WHERE rg.id IN ( :groupIds ) ";

        Query query = entityManager.createQuery(queryString);
        query.setParameter("groupIds", groupIds);
        List<Integer> validIds = query.getResultList();

        groupIds.removeAll(validIds);
        return groupIds;
    }

    public void ensureMembershipMatches(Subject subject, int groupId, int[] resourceIds) throws UpdateException {
        //throws ResourceGroupUpdateException {
        List<Integer> currentMembers = resourceManager.findExplicitResourceIdsByResourceGroup(groupId);

        List<Integer> newMembers = ArrayUtils.wrapInList(resourceIds); // members needing addition
        newMembers.removeAll(currentMembers);
        if (newMembers.size() > 0) {
            addResourcesToGroup(subject, groupId, ArrayUtils.unwrapCollection(newMembers));
        }

        List<Integer> extraMembers = new ArrayList<Integer>(currentMembers); // members needing removal
        extraMembers.removeAll(ArrayUtils.wrapInList(resourceIds));
        if (extraMembers.size() > 0) {
            removeResourcesFromGroup(subject, groupId, ArrayUtils.unwrapCollection(extraMembers));
        }
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // Remote interface impl
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public ResourceGroup getResourceGroup( //
        Subject subject, //
        int groupId) throws FetchException {
        try {
            return getResourceGroupById(subject, groupId, null);
        } catch (Exception e) {
            throw new FetchException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public ResourceGroupComposite getResourceGroupComposite(Subject subject, int groupId) throws FetchException {
        try {
            // Auto cluster backing groups have a special security allowance that let's a non-inventory-manager
            // view them even if they aren't directly in one of their roles, by nature of the fact that the user has
            // the parent cluster group in one of its roles.

            if (!authorizationManager.canViewGroup(subject, groupId)) {
                throw new PermissionException("You do not have permission to view this resource group");
            }

            String queryString = "SELECT \n" //
                + "  (SELECT count(er) "
                + "       FROM ResourceGroup g JOIN g.explicitResources er where g.id = :groupId),\n"
                + "  (SELECT avg(er.currentAvailability.availabilityType) "
                + "       FROM ResourceGroup g JOIN g.explicitResources er where g.id = :groupId) AS eavail,\n"
                + "  (SELECT count(ir) "
                + "       FROM ResourceGroup g JOIN g.implicitResources ir where g.id = :groupId),\n"
                + "  (SELECT avg(ir.currentAvailability.availabilityType) "
                + "       FROM ResourceGroup g JOIN g.implicitResources ir where g.id = :groupId), g \n"
                + "FROM ResourceGroup g where g.id = :groupId";

            Query query = entityManager.createQuery(queryString);
            query.setParameter("groupId", groupId);
            List<Object[]> results = (List<Object[]>) query.getResultList();

            if (results.size() == 0) {
                throw new ResourceGroupNotFoundException(groupId);
            }

            Object[] data = results.get(0);

            ResourceGroup group = (ResourceGroup) data[4];
            ResourceType type = group.getResourceType();
            ResourceFacets facets;
            if (type == null) {
                // mixed group
                facets = ResourceFacets.NONE;
            } else {
                // compatible group
                facets = resourceTypeManager.getResourceFacets(group.getResourceType().getId());
            }

            ResourceGroupComposite composite = null;
            if (((Number) data[2]).longValue() > 0) {
                composite = new ResourceGroupComposite( //
                    ((Number) data[0]).longValue(), //
                    ((Number) data[1]).doubleValue(), //
                    ((Number) data[2]).longValue(), //
                    ((Number) data[3]).doubleValue(), //
                    group, facets);
            } else {
                composite = new ResourceGroupComposite(0, 0, 0, 0, group, facets);
            }
            group.getModifiedBy().getFirstName();

            return composite;
        } catch (Exception e) {
            throw new FetchException(e);
        }
    }

    @SuppressWarnings("unchecked")
    // if a user doesn't have MANAGE_SETTINGS, they can only see groups under their own roles
    public PageList<ResourceGroup> findResourceGroupsForRole(Subject subject, int roleId, PageControl pc)
        throws FetchException {
        try {
            pc.initDefaultOrderingField("rg.name");

            String queryName = null;
            if (authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS)) {
                queryName = ResourceGroup.QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE_admin;
            } else {
                queryName = ResourceGroup.QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE;
            }
            Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
            Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

            if (authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS) == false) {
                queryCount.setParameter("subjectId", subject.getId());
                query.setParameter("subjectId", subject.getId());
            }
            queryCount.setParameter("id", roleId);
            query.setParameter("id", roleId);

            long count = (Long) queryCount.getSingleResult();
            List<ResourceGroup> groups = query.getResultList();

            return new PageList<ResourceGroup>(groups, (int) count, pc);
        } catch (Exception e) {
            throw new FetchException(e);
        }

    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceGroup> findResourceGroups( //
        Subject subject, //
        ResourceGroup criteria, //
        PageControl pc) throws FetchException {
        try {
            QueryGenerator generator = new QueryGenerator(criteria, pc);
            if (authorizationManager.isInventoryManager(subject) == false) {
                generator.setAuthorizationResourceFragment(AuthorizationTokenType.GROUP, null, subject.getId());
            }

            Query query = generator.getQuery(entityManager);
            Query countQuery = generator.getCountQuery(entityManager);

            long count = (Long) countQuery.getSingleResult();
            List<ResourceGroup> results = query.getResultList();

            return new PageList<ResourceGroup>(results, (int) count, pc);
        } catch (Exception e) {
            throw new FetchException(e.getMessage());
        }
    }

    // note: QueryGenerator does not yet support generated queries for composites, only actual entities
    public PageList<ResourceGroupComposite> findResourceGroupComposites( //
        Subject subject, //
        ResourceGroup criteria, //
        PageControl pc) throws FetchException {
        try {
            GroupCategory groupCategory = criteria.getGroupCategory();
            ResourceType resourceType = criteria.getResourceType();
            ResourceCategory resourceCategory = resourceType == null ? null : criteria.getResourceType().getCategory();
            String nameFilter = criteria.getName();

            return findResourceGroupComposites(subject, groupCategory, resourceCategory, resourceType, nameFilter,
                null, null, pc);
        } catch (Exception e) {
            throw new FetchException(e);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void setRecursive( //
        Subject subject, //
        int groupId, //
        boolean isRecursive) throws UpdateException {
        ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
        if (group == null) {
            throw new UpdateException("Can not change recursivity of unknown group[" + groupId + "]");
        }
        updateResourceGroup(subject, group, isRecursive ? RecursivityChangeType.AddedRecursion
            : RecursivityChangeType.RemovedRecursion);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup updateResourceGroup(Subject subject, ResourceGroup group) throws UpdateException {
        try {
            return updateResourceGroup(subject, group, null);
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }
}