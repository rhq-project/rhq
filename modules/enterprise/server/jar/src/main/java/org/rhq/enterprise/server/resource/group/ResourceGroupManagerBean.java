/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.GroupAlertDefinitionManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.jaxb.adapter.ResourceGroupAdapter;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.QueryUtility;

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
    @EJB
    private GroupAlertDefinitionManagerLocal groupAlertDefinitionManager;

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

    public ResourceGroup createPrivateResourceGroup(Subject subject, //
        @XmlJavaTypeAdapter(ResourceGroupAdapter.class) ResourceGroup group) {

        group.setSubject(subject);
        group.setRecursive(false);

        return resourceGroupManager.createResourceGroup(subjectManager.getOverlord(), group);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup createResourceGroup(Subject user, //
        @XmlJavaTypeAdapter(ResourceGroupAdapter.class) ResourceGroup group) {

        // We are now allowing Groups where names collide if the group is not visible as for autogroups and clusters
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_NAME_VISIBLE_GROUP);
        query.setParameter("name", group.getName());

        List<ResourceGroup> groups = query.getResultList();
        if (groups.size() != 0) {
            throw new ResourceGroupAlreadyExistsException("ResourceGroup with name " + group.getName()
                + " already exists");
        }

        long time = System.currentTimeMillis();
        group.setCtime(time);
        group.setMtime(time);
        group.setModifiedBy(user.getName());

        entityManager.persist(group);

        return group;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup updateResourceGroup(Subject subject, ResourceGroup group) throws ResourceGroupUpdateException {
        return updateResourceGroup(subject, group, null, true);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup updateResourceGroup(Subject subject, ResourceGroup group, RecursivityChangeType changeType)
        throws ResourceGroupUpdateException {
        return updateResourceGroup(subject, group, changeType, true);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup updateResourceGroup(Subject user, ResourceGroup group, RecursivityChangeType changeType,
        boolean updateMembership) throws ResourceGroupUpdateException {

        int groupId = group.getId();
        ResourceGroup attachedGroup = entityManager.find(ResourceGroup.class, groupId);
        if (attachedGroup == null) {
            throw new ResourceGroupNotFoundException(groupId);
        }

        if (!authorizationManager.hasGroupPermission(user, Permission.MODIFY_RESOURCE, groupId)) {
            throw new PermissionException("User [" + user
                + "] does not have permission to modify Resource group with id [" + groupId + "].");
        }

        //roles are not to be updated by this call but the group entity
        //owns the relationship. Let's make sure we don't change the assigned roles here.
        group.getRoles().clear();
        group.getRoles().addAll(attachedGroup.getRoles());

        if (changeType == null) {
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

        if (!updateMembership) {
            group.setExplicitResources(attachedGroup.getExplicitResources());
            group.setImplicitResources(attachedGroup.getImplicitResources());
        }

        group.setMtime(System.currentTimeMillis());
        group.setModifiedBy(user.getName());

        ResourceGroup newlyAttachedGroup = entityManager.merge(group);
        if (changeType == RecursivityChangeType.AddedRecursion) {
            newlyAttachedGroup.setRecursive(true);
            enableRecursivityForGroup(user, groupId);
        } else if (changeType == RecursivityChangeType.RemovedRecursion) {
            newlyAttachedGroup.setRecursive(false);
            clearImplicitResources(groupId);
            makeImplicitMirrorExplicit(groupId);
        }

        if (updateMembership) {
            try {
                setResourceType(groupId);
            } catch (ResourceGroupDeleteException e) {
                throw new ResourceGroupNotFoundException(e.getMessage());
            }
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
    public void deleteResourceGroup(Subject subject, int groupId) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException {
        ResourceGroup group = getResourceGroupById(subject, groupId, null);

        for (Role doomedRoleRelationship : group.getRoles()) {
            group.removeRole(doomedRoleRelationship);
            entityManager.merge(doomedRoleRelationship);
        }

        // remove all resources in the group
        resourceGroupManager.removeAllResourcesFromGroup(subject, groupId);

        if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {
            removeCompatibleGroupConstructs(subject, group);
        }

        // break resource and plugin configuration update links in order to preserve individual change history
        Query q = null;

        q = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_DELETE_GROUP_UPDATES_FOR_GROUP);
        q.setParameter("groupId", group.getId());
        q.executeUpdate();

        q = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_DELETE_GROUP_UPDATES_FOR_GROUP);
        q.setParameter("groupId", group.getId());
        q.executeUpdate();

        entityManager.remove(group);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteResourceGroups(Subject subject, int[] groupIds) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException {
        for (int nextGroupId : groupIds) {
            deleteResourceGroup(subject, nextGroupId);
        }
    }

    /*
     * TODO: Deletion of all associated group data (except implicit/explicit resource members) should be moved here.
     *       in other words, we don't want Hibernate cascade annotations to remove that history upon deletion of an
     *       entity anymore because there are now two cases where group constructs need to be destroyed:
     *
     *          1) compatible group deletion - a group is deleted, all history removed, entity is gone from the system
     *          2) dynagroup recomputation - a group definition is recalculation, a compatible group turns into a mixed
     *                                       group, compatible constructs need to be removed, but the entity survives
     *
     *       For now, this implementation should suffice for -- https://bugzilla.redhat.com/show_bug.cgi?id=535671
     */
    private void removeCompatibleGroupConstructs(Subject subject, ResourceGroup group)
        throws ResourceGroupDeleteException {

        // for compatible groups, first recursively remove any referring backing groups for auto-clusters
        for (ResourceGroup referringGroup : group.getClusterBackingGroups()) {
            deleteResourceGroup(subject, referringGroup.getId());
        }

        Subject overlord = subjectManager.getOverlord();
        try {
            List<GroupOperationSchedule> ops = operationManager.findScheduledGroupOperations(overlord, group.getId());

            for (GroupOperationSchedule schedule : ops) {
                    operationManager.unscheduleGroupOperation(overlord, schedule.getJobId().toString(), group.getId());
            }
        } catch (Exception e) {
            throw new ResourceGroupDeleteException( "Failed to get jobs for a group being deleted [" + group
                + "]; will not attempt to unschedule anything", e);
        }

        groupAlertDefinitionManager.purgeAllGroupAlertDefinitions(subject, group.getId());
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
        group.getAlertDefinitions().size();
    }

    @SuppressWarnings("unchecked")
    public int[] getResourceGroupCountSummary(Subject user) {
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_RESOURCE_GROUP_SUMMARY_admin);
        } else {
            query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_RESOURCE_GROUP_SUMMARY);
            query.setParameter("subject", user);
        }

        int[] counts = new int[2];
        List<Object[]> resultList = query.getResultList();

        for (Object[] row : resultList) {
            switch ((GroupCategory) row[0]) {
            case MIXED:
                counts[0] = ((Long) row[1]).intValue();
                break;
            case COMPATIBLE:
                counts[1] = ((Long) row[1]).intValue();
                break;
            }
        }

        return counts;
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
    public void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds) {

        addResourcesToGroup(subject, groupId, resourceIds, true);
    }

    private void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds, boolean setType) {
        Integer[] ids = ArrayUtils.wrapInArray(resourceIds);
        if (ids == null || ids.length == 0) {
            return;
        }

        boolean isRecursive = isRecursive(groupId); // will perform check for group existence

        // batch the removes to prevent the ORA error about IN clauses containing more than 1000 items
        for (int batchIndex = 0; batchIndex < ids.length; batchIndex += 1000) {
            Integer[] batchIdArray = ArrayUtils.copyOfRange(ids, batchIndex, batchIndex + 1000);
            List<Integer> batchIds = Arrays.asList(batchIdArray);

            addResourcesToGroupImplicit(subject, groupId, batchIds, true, isRecursive);
            addResourcesToGroupExplicit(subject, groupId, batchIds, isRecursive);
        }

        if (setType) {
            try {
                setResourceType(groupId);
            } catch (ResourceGroupDeleteException e) {
                throw new ResourceGroupNotFoundException(e.getMessage());
            }
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
        groupAlertDefinitionManager.addGroupAlertDefinitions(subject, groupId, resourceIdsToAdd);

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
    public void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds) {

        removeResourcesFromGroup(subject, groupId, resourceIds, true);
    }

    private void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds, boolean setType) {
        Integer[] ids = ArrayUtils.wrapInArray(resourceIds);
        if (ids == null || ids.length == 0) {
            return;
        }

        boolean isRecursive = isRecursive(groupId); // will perform check for group existence

        // batch the removes to prevent the ORA error about IN clauses containing more than 1000 items
        for (int batchIndex = 0; batchIndex < ids.length; batchIndex += 1000) {
            Integer[] batchIdArray = ArrayUtils.copyOfRange(ids, batchIndex, batchIndex + 1000);

            removeResourcesFromGroup_helper(subject, groupId, batchIdArray, isRecursive);
        }

        if (setType) {
            try {
                setResourceType(groupId);
            } catch (ResourceGroupDeleteException e) {
                throw new ResourceGroupNotFoundException(e.getMessage());
            }
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
        groupAlertDefinitionManager.removeGroupAlertDefinitions(subject, groupId, resourceIdsToRemove);

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
        Query query = entityManager
            .createNamedQuery(ResourceGroup.QUERY_FIND_IMPLICIT_RECURSIVE_GROUP_IDS_BY_RESOURCE_ID);
        query.setParameter("id", resource.getParentResource().getId());
        List<Integer> implicitRecursiveGroupIds = query.getResultList();

        // the resource isn't currently in a group -> no work to do
        if (implicitRecursiveGroupIds.size() == 0) {
            return;
        }

        /*
         * BFS-construct the resource tree
         */
        List<Integer> resourceIdsToAdd = new ArrayList<Integer>();
        List<Resource> toBeSearched = new LinkedList<Resource>();
        toBeSearched.add(resource);
        while (toBeSearched.size() > 0) {
            Resource next = toBeSearched.remove(0);
            resourceIdsToAdd.add(next.getId());
            toBeSearched.addAll(next.getChildResources());
        }

        /*
         * now add this resource and all of its descendants to whatever recursive groups it's parent is already in
         */
        Connection conn = null;
        PreparedStatement insertImplicitStatement = null;
        try {
            conn = rhqDs.getConnection();
            for (Integer implicitRecursiveGroupId : implicitRecursiveGroupIds) {
                /*
                 * do have to worry about whether these resources are already in the explicit resource list because
                 * they are being newly committed to inventory and thus shouldn't be in any group except the work
                 * being done right now.
                 *
                 * also, since we've already computed the toAddResourceIds by recursing down the chain resource passed
                 * to this method, we can just do simple RHQ_RESOURCE_GROUP_RES_IMP_MAP table insertions
                 */
                String insertImplicitQueryString = JDBCUtil.transformQueryForMultipleInParameters(
                    ResourceGroup.QUERY_NATIVE_ADD_RESOURCES_TO_GROUP_IMPLICIT, "@@RESOURCE_IDS@@",
                    resourceIdsToAdd.size());
                insertImplicitStatement = conn.prepareStatement(insertImplicitQueryString);
                insertImplicitStatement.setInt(1, implicitRecursiveGroupId);
                JDBCUtil.bindNTimes(insertImplicitStatement, ArrayUtils.unwrapCollection(resourceIdsToAdd), 2);
                insertImplicitStatement.executeUpdate();

                /*
                 * when automatically updating recursive groups during inventory sync we need to make sure that we also
                 * update the resourceGroup; this will realistically only happen when a recursive group definition is
                 * created, but which initially creates one or more resource groups of size 1; if this happens, the
                 * group will be created as a compatible group, if resources are later added via an inventory sync, and
                 * if this compatible group's membership changes, we need to recalculate the group category
                 *
                 * NOTE: this is no longer true.  the group category used to be based off of the explicit membership,
                 *       but that assumption was changed for 1.2.0 release so we could support a compatible left-nav
                 *       tree with recursive access to descendant for easy authorization.  this method only modifies
                 *       the implicit resource membership, not the explicit, so setResourceType would be a no-op.
                 */
                //setResourceType(implicitRecursiveGroupId);
            }
        } catch (Exception e) {
            throw new ResourceGroupUpdateException("Could not add resource[id=" + resource.getId()
                + "] to necessary implicit groups", e);
        } finally {
            JDBCUtil.safeClose(insertImplicitStatement);
            JDBCUtil.safeClose(conn);
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
            List<Resource> resources = PersistenceUtility.getHibernateSession(entityManager)
                .createFilter(res, "where this.inventoryStatus = :inventoryStatus")
                .setParameter("inventoryStatus", InventoryStatus.COMMITTED).list();

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
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setResourceType(int resourceGroupId) throws ResourceGroupDeleteException {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_GET_EXPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP);
        query.setParameter("groupId", resourceGroupId);

        Subject overlord = subjectManager.getOverlord();
        ResourceGroup resourceGroup = getResourceGroupById(overlord, resourceGroupId, null);

        List results = query.getResultList();
        if (results.size() == 1) {
            Object[] info = (Object[]) results.get(0);
            int resourceTypeId = (Integer) info[0];

            ResourceType flyWeightType = new ResourceType();
            flyWeightType.setId(resourceTypeId);
            resourceGroup.setResourceType(flyWeightType);
        } else {
            if (resourceGroup.getResourceType() != null) {
                // converting compatible group to mixed group, remove all corresponding compatible constructs
                removeCompatibleGroupConstructs(overlord, resourceGroup);
            }
            resourceGroup.setResourceType(null);
        }
    }

    public int getExplicitGroupMemberCount(int resourceGroupId) {
        Query countQuery = entityManager
            .createNamedQuery(Resource.QUERY_FIND_EXPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN);
        countQuery.setParameter("groupId", resourceGroupId);
        long count = (Long) countQuery.getSingleResult();
        return (int) count;
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
        ResourceCategory resourceCategory, String resourceTypeName, String pluginName, String nameFilter,
        Integer resourceId, Integer groupId, PageControl pc) {

        if ((resourceId == null) && (groupId == null)) {
            return new PageList<ResourceGroupComposite>(0, pc);
        }

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
        nameFilter = QueryUtility.formatSearchParameter(nameFilter);

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
            String resourceCategoryName = resourceCategory == null ? null : resourceCategory.name();
            String groupCategoryName = groupCategory == null ? null : groupCategory.name();

            int i = 0;
            if (resourceId != null) {
                stmt.setInt(++i, resourceId);
            }
            if (groupId != null) {
                stmt.setInt(++i, groupId);
            }

            if (search == null) {
                stmt.setNull(++i, Types.VARCHAR);
                stmt.setNull(++i, Types.VARCHAR);
                stmt.setString(++i, QueryUtility.getEscapeCharacter());
                stmt.setNull(++i, Types.VARCHAR);
                stmt.setString(++i, QueryUtility.getEscapeCharacter());
            } else {
                stmt.setString(++i, search);
                stmt.setString(++i, search);
                stmt.setString(++i, QueryUtility.getEscapeCharacter());
                stmt.setString(++i, search);
                stmt.setString(++i, QueryUtility.getEscapeCharacter());
            }

            if (resourceTypeName == null) {
                stmt.setNull(++i, Types.VARCHAR);
                stmt.setNull(++i, Types.VARCHAR);
            } else {
                stmt.setString(++i, resourceTypeName);
                stmt.setString(++i, resourceTypeName);
            }

            if (pluginName == null) {
                stmt.setNull(++i, Types.VARCHAR);
                stmt.setNull(++i, Types.VARCHAR);
            } else {
                stmt.setString(++i, pluginName);
                stmt.setString(++i, pluginName);
            }

            if (resourceCategoryName == null) {
                stmt.setNull(++i, Types.VARCHAR);
                stmt.setNull(++i, Types.VARCHAR);
            } else {
                stmt.setString(++i, resourceCategoryName);
                stmt.setString(++i, resourceCategoryName);
            }

            if (groupCategoryName == null) {
                stmt.setNull(++i, Types.VARCHAR);
                stmt.setNull(++i, Types.VARCHAR);
            } else {
                stmt.setString(++i, groupCategoryName);
                stmt.setString(++i, groupCategoryName);
            }

            if (authorizationManager.isInventoryManager(subject) == false) {
                stmt.setInt(++i, subject.getId());
            }

            ResultSet rs = stmt.executeQuery();
            try {
                while (rs.next()) {
                    long explicitCount = rs.getLong(1);
                    long explicitDown = rs.getLong(2);
                    long explicitUnknown = rs.getLong(3);
                    long explicitDisabled = rs.getLong(4);
                    long implicitCount = rs.getLong(5);
                    long implicitDown = rs.getLong(6);
                    long implicitUnknown = rs.getLong(7);
                    long implicitDisabled = rs.getLong(8);
                    int groupKey = rs.getInt(9);
                    Object[] next = new Object[] { explicitCount, explicitDown, explicitUnknown, explicitDisabled,
                        implicitCount, implicitDown, implicitUnknown, implicitDisabled, groupKey };
                    rawResults.add(next);
                }
            } finally {
                rs.close();
            }
        } catch (Throwable t) {
            log.error("Could not execute groups query [ " + query + " ]: ", t);
            return new PageList<ResourceGroupComposite>();
        } finally {
            JDBCUtil.safeClose(conn, stmt, null);
        }

        Query queryCount;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_FILTERED_COUNT_ADMIN);
        } else {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_FILTERED_COUNT);
            queryCount.setParameter("subject", subject);
        }

        queryCount.setParameter("groupCategory", groupCategory);
        queryCount.setParameter("category", resourceCategory);
        queryCount.setParameter("resourceTypeName", resourceTypeName);
        queryCount.setParameter("pluginName", pluginName);
        queryCount.setParameter("search", nameFilter);
        queryCount.setParameter("resourceId", resourceId);
        queryCount.setParameter("groupId", groupId);

        long count = (Long) queryCount.getSingleResult();

        List<Integer> groupIds = new ArrayList<Integer>();
        for (Object[] row : rawResults) {
            groupIds.add(((Number) row[8]).intValue());
        }
        Map<Integer, ResourceGroup> groupMap = getIdGroupMap(groupIds);

        List<ResourceGroupComposite> results = new ArrayList<ResourceGroupComposite>(rawResults.size());
        int i = 0;
        for (Object[] row : rawResults) {
            long explicitCount = (Long) row[0];
            long explicitDown = (Long) row[1];
            long explicitUnknown = (Long) row[2];
            long explicitDisabled = (Long) row[3];
            long implicitCount = (Long) row[4];
            long implicitDown = (Long) row[5];
            long implicitUnknown = (Long) row[6];
            long implicitDisabled = (Long) row[7];
            ResourceGroup group = groupMap.get(groupIds.get(i++));
            ResourceType type = group.getResourceType();
            ResourceFacets facets;
            if (type == null) {
                // mixed group
                facets = ResourceFacets.NONE;
            } else {
                // compatible group
                facets = resourceTypeManager.getResourceFacets(type.getId());
            }
            ResourceGroupComposite composite = new ResourceGroupComposite(explicitCount, explicitDown, explicitUnknown,
                explicitDisabled, implicitCount, implicitDown, implicitUnknown, implicitDisabled, group, facets);
            Set<Permission> perms = authorizationManager.getImplicitGroupPermissions(subject, group.getId());
            composite.setResourcePermission(new ResourcePermission(perms));
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

    public void setAssignedResources(Subject subject, int groupId, int[] resourceIds, boolean setType)
        throws ResourceGroupDeleteException {

        ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
        if (group.isPrivateGroup()) {
            List<Integer> ids = new ArrayList<Integer>(resourceIds.length);
            for (int id : resourceIds) {
                ids.add(id);
            }
            if (!authorizationManager.canViewResources(subject, ids)) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] does not have VIEW permission for all specified resources.");
            }
        } else {
            if (!authorizationManager.isInventoryManager(subject)) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] is not authorized for [ MANAGE_INVENTORY ]. Required for changing group membership ");
            }
        }

        List<Integer> currentMembers = resourceManager.findExplicitResourceIdsByResourceGroup(groupId);

        List<Integer> newMembers = ArrayUtils.wrapInList(resourceIds); // members needing addition
        newMembers.removeAll(currentMembers);
        if (newMembers.size() > 0) {
            addResourcesToGroup(subjectManager.getOverlord(), groupId, ArrayUtils.unwrapCollection(newMembers), false);
        }

        List<Integer> extraMembers = new ArrayList<Integer>(currentMembers); // members needing removal
        extraMembers.removeAll(ArrayUtils.wrapInList(resourceIds));
        if (extraMembers.size() > 0) {
            removeResourcesFromGroup(subjectManager.getOverlord(), groupId, ArrayUtils.unwrapCollection(extraMembers),
                false);
        }

        // As a result of the membership change ensure that the group type is set correctly.
        if (setType) {
            setResourceType(groupId);
        }
    }

    public void setAssignedResourceGroupsForResource(Subject subject, int resourceId, int[] resourceGroupIds,
        boolean setType) throws ResourceGroupDeleteException {

        Resource resource = entityManager.find(Resource.class, resourceId);
        Set<ResourceGroup> currentGroups = resource.getExplicitGroups();
        List<Integer> currentGroupIds = new ArrayList<Integer>(currentGroups.size());
        for (ResourceGroup currentGroup : currentGroups) {
            currentGroupIds.add(currentGroup.getId());
        }

        int[] resourceIdArr = new int[] { resourceId };

        List<Integer> addedGroupIds = ArrayUtils.wrapInList(resourceGroupIds);
        addedGroupIds.removeAll(currentGroupIds);
        for (Integer addedGroupId : addedGroupIds) {
            addResourcesToGroup(subject, addedGroupId, resourceIdArr);
            // As a result of the membership change ensure that the group type is set correctly.
            if (setType) {
                setResourceType(addedGroupId);
            }
        }

        List<Integer> removedGroupIds = new ArrayList<Integer>(currentGroupIds); // groups needing removal
        removedGroupIds.removeAll(ArrayUtils.wrapInList(resourceGroupIds));
        for (Integer removedGroupId : removedGroupIds) {
            removeResourcesFromGroup(subject, removedGroupId, resourceIdArr);
            // As a result of the membership change ensure that the group type is set correctly.
            if (setType) {
                setResourceType(removedGroupId);
            }
        }
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // Remote interface impl
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public ResourceGroup getResourceGroup( //
        Subject subject, //
        int groupId) {
        return getResourceGroupById(subject, groupId, null);
    }

    @SuppressWarnings("unchecked")
    public ResourceGroupComposite getResourceGroupComposite(Subject subject, int groupId) {
        // Auto cluster backing groups have a special security allowance that let's a non-inventory-manager
        // view them even if they aren't directly in one of their roles, by nature of the fact that the user has
        // the parent cluster group in one of its roles.

        if (!authorizationManager.canViewGroup(subject, groupId)) {
            throw new PermissionException("You do not have permission to view this resource group");
        }

        // Could do this with two GROUP BY queries but we'll go with one RT to the db and hope that's best, despite
        // all the subselects.
        String queryString = "SELECT \n" //
            + "  (SELECT count(er) " // Total explicit
            + "       FROM ResourceGroup g JOIN g.explicitResources er where er.inventoryStatus = 'COMMITTED' and g.id = :groupId),\n"
            + "  (SELECT count(er) " // DOWN explicit
            + "       FROM ResourceGroup g JOIN g.explicitResources er where er.inventoryStatus = 'COMMITTED' and g.id = :groupId"
            + "        AND er.currentAvailability.availabilityType = 0 ),\n"
            + "  (SELECT count(er) " // UNKNOWN explicit
            + "       FROM ResourceGroup g JOIN g.explicitResources er where er.inventoryStatus = 'COMMITTED' and g.id = :groupId"
            + "        AND er.currentAvailability.availabilityType = 2 ),\n"
            + "  (SELECT count(er) " // DISABLED explicit
            + "       FROM ResourceGroup g JOIN g.explicitResources er where er.inventoryStatus = 'COMMITTED' and g.id = :groupId"
            + "        AND er.currentAvailability.availabilityType = 3 ),\n"
            + "  (SELECT count(ir) " // Total implicit
            + "       FROM ResourceGroup g JOIN g.implicitResources ir where ir.inventoryStatus = 'COMMITTED' and g.id = :groupId),\n"
            + "  (SELECT count(ir) " // DOWN implicit
            + "       FROM ResourceGroup g JOIN g.implicitResources ir where ir.inventoryStatus = 'COMMITTED' and g.id = :groupId"
            + "        AND ir.currentAvailability.availabilityType = 0 ),\n"
            + "  (SELECT count(ir) " // UNKNOWN implicit
            + "       FROM ResourceGroup g JOIN g.implicitResources ir where ir.inventoryStatus = 'COMMITTED' and g.id = :groupId"
            + "        AND ir.currentAvailability.availabilityType = 2 ),\n"
            + "  (SELECT count(ir) " // DISABLED implicit
            + "       FROM ResourceGroup g JOIN g.implicitResources ir where ir.inventoryStatus = 'COMMITTED' and g.id = :groupId"
            + "        AND ir.currentAvailability.availabilityType = 3 )\n,"
            + "    g "
            + "FROM ResourceGroup g where g.id = :groupId";

        Query query = entityManager.createQuery(queryString);
        query.setParameter("groupId", groupId);
        List<Object[]> results = (List<Object[]>) query.getResultList();

        if (results.size() == 0) {
            throw new ResourceGroupNotFoundException(groupId);
        }

        Object[] data = results.get(0);

        ResourceGroup group = (ResourceGroup) data[8];
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
        if (((Number) data[4]).longValue() > 0) {
            long explicitCount = ((Number) data[0]).longValue();
            long explicitDown = ((Number) data[1]).longValue();
            long explicitUnknown = ((Number) data[2]).longValue();
            long explicitDisabled = ((Number) data[3]).longValue();
            long implicitCount = ((Number) data[4]).longValue();
            long implicitDown = ((Number) data[5]).longValue();
            long implicitUnknown = ((Number) data[6]).longValue();
            long implicitDisabled = ((Number) data[7]).longValue();
            // In the past we had only DOWN/0 and UP/1 avails/ordinal and and the avails were just averages.
            // Now we have DISABLED and UNKNOWN. So group avail is done differently, instead of a ratio of
            // of UP vs DOWN it is now handled with counts. This is handled in the composite.

            composite = new ResourceGroupComposite(explicitCount, explicitDown, explicitUnknown, explicitDisabled,
                implicitCount, implicitDown, implicitUnknown, implicitDisabled, group, facets);
        } else {
            composite = new ResourceGroupComposite(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, group, facets);
        }

        return composite;
    }

    @SuppressWarnings("unchecked")
    // if a user doesn't have MANAGE_SETTINGS, they can only see groups under their own roles
    public PageList<ResourceGroup> findResourceGroupsForRole(Subject subject, int roleId, PageControl pc) {
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
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void setRecursive( //
        Subject subject, //
        int groupId, //
        boolean isRecursive) {
        ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
        if (group == null) {
            throw new ResourceGroupNotFoundException(groupId);
        }
        updateResourceGroup(subject, group, isRecursive ? RecursivityChangeType.AddedRecursion
            : RecursivityChangeType.RemovedRecursion);
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceGroup> findResourceGroupsByCriteria(Subject subject, ResourceGroupCriteria criteria) {

        CriteriaAuthzType authzType = getCriteriaAuthzType(subject, criteria);

        CriteriaQueryGenerator generator = getCriteriaQueryGenerator(subject, criteria, authzType);

        CriteriaQueryRunner<ResourceGroup> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);

        PageList<ResourceGroup> result = queryRunner.execute();

        return result;
    }

    /**
     * This method adheres to all of the regular semantics of {@link Criteria}-based queries.  In other words,
     * all of the methods on the {@link Criteria} object - including paging, sorting, filtering, fetching - will
     * work with this method.  The only thing that differs is the ResultSet which, instead of being a collection
     * of {@link ResourceGroup} objects is a collection of {@link ResourceGroupComposite} objects.
     *
     * The extended data in the composite object, however, is treated differently:
     *
     *   1) It is always fetched
     *   2) It can not be a candidate for filtering
     *   3) It must be sorted by using the zero-based positional ordinal within the projection
     *
     * This method offers 2 new aggregates that you can sort on.  The
     *
     * explicitCount (ordinal 0) - the count of the number of children in the group
     * implicitCount (ordinal 2) - the count of the number of descendents in the group
     */
    @SuppressWarnings("unchecked")
    public PageList<ResourceGroupComposite> findResourceGroupCompositesByCriteria(Subject subject,
        ResourceGroupCriteria criteria) {

        CriteriaAuthzType authzType = getCriteriaAuthzType(subject, criteria);

        // first select groups only, then we'll select composites
        PageList<ResourceGroup> resourceGroups = findResourceGroupsByCriteria(subject, criteria);

        if (resourceGroups.isEmpty()) {
            return new PageList<ResourceGroupComposite>();
        }

        // put group IDs to list, so we can query composites for those qroups only
        List<Integer> groupIds = new ArrayList<Integer>(resourceGroups.size());
        // create a map of groupId, it's index - in the end we'll need to re-order composites and respect ordering from criteria
        final Map<Integer, Integer> groupIndexes = new HashMap<Integer, Integer>(resourceGroups.size());
        int index = 0;
        for (ResourceGroup rg : resourceGroups) {
            groupIds.add(rg.getId());
            groupIndexes.put(rg.getId(), index);
            index++;
        }
        // JPA queries does not allow to nest SELECT (ie Select x from (Select y) ..)
        // we'd need to run 2 (or 3) queries
        // first JOIN ON implicitResources
        // then JOIN ON explicitResources
        // then (AUTO_CLUSTER) join on roles/permissions
        // we're reusing ResourceGroupComposite for selected data
        String compositeProjection = ""
            + " new org.rhq.core.domain.resource.group.composite.ResourceGroupComposite( "
            + "  SUM(CASE WHEN res.inventoryStatus = 'COMMITTED' THEN 1 ELSE 0 END), "
            + "  SUM(CASE WHEN res.inventoryStatus = 'COMMITTED' AND avail.availabilityType = 0 THEN 1 ELSE 0 END), "
            + "  SUM(CASE WHEN res.inventoryStatus = 'COMMITTED' AND avail.availabilityType = 2 THEN 1 ELSE 0 END), "
            + "  SUM(CASE WHEN res.inventoryStatus = 'COMMITTED' AND avail.availabilityType = 3 THEN 1 ELSE 0 END), "
            + "  0L,0L, 0L, 0L, %alias%) ";

        String compositeFromCause = criteria.getPersistentClass().getSimpleName()
            + " %alias% "
            + " LEFT JOIN %alias%.%membership%Resources res "
            + " LEFT JOIN res.currentAvailability avail ";

        String permissionsProjection = null;
        String permissionsFromCause = null;
        switch (authzType) {
        case NONE:
        case SUBJECT_OWNED:
            break;
        case ROLE_OWNED:
        case AUTO_CLUSTER:

            permissionsProjection = ""
                + " new org.rhq.core.domain.resource.group.composite.ResourceGroupComposite( "
                + "    0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, %alias%, "
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 8 THEN 1 ELSE 0 END), " // MANAGE_MEASUREMENTS
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 4 THEN 1 ELSE 0 END), " // MODIFY_RESOURCE
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 10 THEN 1 ELSE 0 END), " // CONTROL
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 7 THEN 1 ELSE 0 END), " // MANAGE_ALERTS
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 14 THEN 1 ELSE 0 END), " // MANAGE_EVENTS
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 13 THEN 1 ELSE 0 END), " // CONFIGURE_READ
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 11 THEN 1 ELSE 0 END), " // CONFIGURE_WRITE
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 9 THEN 1 ELSE 0 END), " // MANAGE_CONTENT
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 6 THEN 1 ELSE 0 END), " // CREATE_CHILD_RESOURCES
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 5 THEN 1 ELSE 0 END), " // DELETE_RESOURCES
                + "    SUM(CASE WHEN s.id = %subjectId% and p = 16 THEN 1 ELSE 0 END) " // MANAGE_DRIFT
                + ") ";
            permissionsProjection = permissionsProjection.replace("%subjectId%", String.valueOf(subject.getId()));
            permissionsFromCause = criteria.getPersistentClass().getSimpleName()
                + " %alias% "
                + " LEFT JOIN %permAlias%.roles r"
                + " LEFT JOIN r.subjects s"
                + " LEFT JOIN r.permissions p";

            break;
        default:
            throw new IllegalStateException("Unexpected CriteriaAuthzType: " + authzType);
        }

        String alias = criteria.getAlias();
        String permAlias = alias + ((authzType == CriteriaAuthzType.AUTO_CLUSTER) ? ".clusterResourceGroup" : "");
        String from = compositeFromCause
            .replace("%membership%", "implicit")
            .replace("%alias%", alias)
            .replace("%permAlias%", permAlias);
        String projection = compositeProjection.replace("%alias%", alias);

        String compositeQuery = "SELECT " + projection + " FROM " + from + " WHERE " + alias + ".id IN ( :ids ) GROUP BY "
            + alias + ".id";

        // query implicit group members
        List<ResourceGroupComposite> implicitResults = entityManager.createQuery(compositeQuery)
            .setParameter("ids", groupIds).getResultList();

        from = compositeFromCause.replace("%membership%", "explicit").replace("%alias%", alias)
            .replace("%permAlias%", permAlias);

        compositeQuery = "SELECT " + projection + " FROM " + from + " WHERE " + alias + ".id IN ( :ids ) GROUP BY " + alias
            + ".id";

        // query explicit members
        List<ResourceGroupComposite> explicitResults = entityManager.createQuery(compositeQuery)
            .setParameter("ids", groupIds).getResultList();

        List<ResourceGroupComposite> permissionResults = null;

        if (permissionsProjection != null) {
            from = permissionsFromCause.replace("%alias%", alias).replace("%permAlias%", permAlias);

            projection = permissionsProjection.replaceFirst("%alias%", alias);
            compositeQuery = "SELECT " + projection + " FROM " + from + " WHERE " + alias + ".id IN ( :ids ) GROUP BY "
                + alias + ".id";

            // query permissions
            permissionResults = entityManager.createQuery(compositeQuery).setParameter("ids", groupIds).getResultList();

        }
        // now "join" results together
        List<ResourceGroupComposite> results = new ArrayList<ResourceGroupComposite>(explicitResults.size());
        if (explicitResults.size() == implicitResults.size()
            && (permissionResults == null || permissionResults.size() == implicitResults.size())) {
            // in case we selected same groups sets using all 2 (or 3) queries (99% of cases) just "join" them by index
            for (int i = 0; i < explicitResults.size(); i++) {
                ResourceGroupComposite imp = implicitResults.get(i);
                ResourceGroupComposite exp = explicitResults.get(i);
                ResourceGroupComposite perm = permissionResults != null ? permissionResults.get(i) : null;
                ResourceGroupComposite composite = createComposite(imp, exp, perm);
                results.add(composite);
            }
        } else {
            // we did not get same results, this means some groups are being added/removed in the meantime
            // we must join it using resourceGroup ID (this is a bit more expensive approach)
            // map ResourceGroupID and [implicitComposite, explicitComposite, permComposite]

            Map<Integer,List<ResourceGroupComposite>> joinMap = new HashMap<Integer, List<ResourceGroupComposite>>();
            for (ResourceGroupComposite c : implicitResults) {
                if (!joinMap.containsKey(c.getResourceGroup().getId())) {
                    joinMap.put(c.getResourceGroup().getId(), new ArrayList<ResourceGroupComposite>(3));
                }
                joinMap.get(c.getResourceGroup().getId()).add(c);
            }
            for (ResourceGroupComposite c : explicitResults) {
                if (!joinMap.containsKey(c.getResourceGroup().getId())) {
                    joinMap.put(c.getResourceGroup().getId(), new ArrayList<ResourceGroupComposite>(3));
                }
                joinMap.get(c.getResourceGroup().getId()).add(c);
            }
            if (permissionResults != null) {
                for (ResourceGroupComposite c : permissionResults) {
                    if (!joinMap.containsKey(c.getResourceGroup().getId())) {
                        joinMap.put(c.getResourceGroup().getId(), new ArrayList<ResourceGroupComposite>(3));
                    }
                    joinMap.get(c.getResourceGroup().getId()).add(c);
                }
                // produce results (include permissionResults)
                for (List<ResourceGroupComposite> list : joinMap.values()) {
                    if (list.size() < 3) {
                        continue; // we did not fully select this composite with all 3 queries, ignore it
                    }
                    ResourceGroupComposite composite = createComposite(list.get(0), list.get(1), list.get(2));
                    results.add(composite);
                }
            } else {
                // prouduce results, but exclude permissionResults
                for (List<ResourceGroupComposite> list : joinMap.values()) {
                    if (list.size() < 2) {
                        continue; // we did not fully select this composite with all 2 queries, ignore it
                    }
                    ResourceGroupComposite composite = createComposite(list.get(0), list.get(1), null);
                    results.add(composite);
                }
            }
        }
        // finally sort results by the same order we got in resoureGroups PageList
        Collections.sort(results, new Comparator<ResourceGroupComposite>() {
            @Override
            public int compare(ResourceGroupComposite arg0, ResourceGroupComposite arg1) {
                Integer index0 = groupIndexes.get(arg0.getResourceGroup().getId());
                Integer index1 = groupIndexes.get(arg1.getResourceGroup().getId());
                // in theory we may not find index in map, in case we've selected more composites than groups
                if (index0 != null && index1 != null) {
                    return index0.compareTo(index1);
                }
                return 0;
            }
        });
        // and transform it to PageList and assign original ResourceGroups
        PageList<ResourceGroupComposite> resultPageList = new PageList<ResourceGroupComposite>(resourceGroups.getTotalSize(),
            resourceGroups.getPageControl());
        index = 0;
        for (ResourceGroupComposite composite : results) {
            ResourceGroup fetched = resourceGroups.get(index);
            resultPageList.add(createComposite(composite, fetched));
            index++;
        }

        resultPageList = getAuthorizedGroupComposites(subject, authzType, resultPageList);

        for (ResourceGroupComposite composite : resultPageList) {
            ResourceGroup group = composite.getResourceGroup();
            ResourceType type = group.getResourceType();
            ResourceFacets facets = (type != null) ? resourceTypeManager.getResourceFacets(type.getId())
                : ResourceFacets.NONE;

            composite.setResourceFacets(facets);
        }
        return resultPageList;
    }

    private ResourceGroupComposite createComposite(ResourceGroupComposite original, ResourceGroup group) {
        return new ResourceGroupComposite(
            original.getExplicitCount(),
            original.getExplicitDown(),
            original.getExplicitUnknown(),
            original.getExplicitDisabled(),
            original.getImplicitCount(),
            original.getImplicitDown(),
            original.getImplicitUnknown(),
            original.getImplicitDisabled(),
            group,
            null,
            original.getResourcePermission()
        );
    }

    private ResourceGroupComposite createComposite(ResourceGroupComposite implicit, ResourceGroupComposite explicit,
        ResourceGroupComposite permission) {
        return new ResourceGroupComposite(
            explicit.getExplicitCount(),
            explicit.getExplicitDown(),
            explicit.getExplicitUnknown(),
            explicit.getExplicitDisabled(),
            implicit.getExplicitCount(),
            implicit.getExplicitDown(),
            implicit.getExplicitUnknown(),
            implicit.getExplicitDisabled(),
            implicit.getResourceGroup(),
            null,
            permission != null ? permission.getResourcePermission() : new ResourcePermission()
        );
    }

    private enum CriteriaAuthzType {
        // inv manager / no auth required
        NONE,
        // standard role-subject-group auth
        ROLE_OWNED,
        // private group auth
        SUBJECT_OWNED,
        // auto cluster backing group
        AUTO_CLUSTER
    }

    private CriteriaAuthzType getCriteriaAuthzType(Subject subject, ResourceGroupCriteria criteria) {
        Set<Permission> globalUserPerms = authorizationManager.getExplicitGlobalPermissions(subject);

        if (criteria.isSecurityManagerRequired() && !globalUserPerms.contains(Permission.MANAGE_SECURITY)) {
            throw new PermissionException("Subject [" + subject.getName()
                + "] requires SecurityManager permission for requested query criteria.");
        }

        boolean isInventoryManager = globalUserPerms.contains(Permission.MANAGE_INVENTORY);

        if (criteria.isInventoryManagerRequired() && !isInventoryManager) {
            throw new PermissionException("Subject [" + subject.getName()
                + "] requires InventoryManager permission for requested query criteria.");
        }

        CriteriaAuthzType result = CriteriaAuthzType.ROLE_OWNED;

        if (isInventoryManager) {
            result = CriteriaAuthzType.NONE;
        } else if (criteria.isFilterPrivate()) {
            result = CriteriaAuthzType.SUBJECT_OWNED;
        } else if (!criteria.isFilterVisible()) {
            result = CriteriaAuthzType.AUTO_CLUSTER;
        }

        return result;
    }

    private CriteriaQueryGenerator getCriteriaQueryGenerator(Subject subject, ResourceGroupCriteria criteria,
        CriteriaAuthzType authzType) {

        // if we're searching for private groups set the subject filter to the current user's subjectId.
        // setting it here prevents the caller from spoofing a different user. This is why the subject and
        // private filters are different. The subject filter can specify any user and therefore requires
        // inventory manager.
        if (criteria.isFilterPrivate()) {
            criteria.addFilterPrivate(null);
            criteria.addFilterSubjectId(subject.getId());
        }

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (authzType != CriteriaAuthzType.NONE) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.GROUP, null,
                subject.getId());
        }

        return generator;
    }

    private PageList<ResourceGroupComposite> getAuthorizedGroupComposites(Subject subject, CriteriaAuthzType authzType,
        PageList<ResourceGroupComposite> groupComposites) {

        switch (authzType) {
        case NONE:
            // leave resourcePermissions unset on the assumption that it will not be checked for inv managers
            break;
        case ROLE_OWNED:
            // the permissions are already set by the query projection
            break;
        case AUTO_CLUSTER:
            // the permissions are already set by the query projection
            break;
        case SUBJECT_OWNED:
            Iterator<ResourceGroupComposite> iterator = groupComposites.iterator();
            while (iterator.hasNext()) {
                ResourceGroupComposite groupComposite = iterator.next();
                ResourceGroup group = groupComposite.getResourceGroup();
                Subject groupOwner = group.getSubject();
                if (null != groupOwner) {
                    // private group, we need to set the group resource permissions since we couldn't do it in
                    // the projection.
                    groupComposite.setResourcePermission(new ResourcePermission(authorizationManager
                        .getExplicitGroupPermissions(groupOwner, group.getId())));
                } else {
                    throw new IllegalStateException("Unexpected group, not subject owned: " + groupComposite);
                }
            }
            break;
        default:
            throw new IllegalStateException("Unexpected CriteriaAuthzType: " + authzType);
        }
        return groupComposites;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void uninventoryMembers(Subject subject, int groupId) {
        List<Integer> resourceMemberIds = resourceManager.findExplicitResourceIdsByResourceGroup(groupId);
        for (int doomedResourceId : resourceMemberIds) {
            resourceManager.uninventoryResource(subject, doomedResourceId);
        }
    }

    public void updateResourceGroupName(Subject subject, int groupId, String name) {
        if (name == null) {
            throw new IllegalArgumentException("Group name cannot be null.");
        }
        ResourceGroup group = getResourceGroupToBeModified(subject, groupId);
        group.setName(name);
        group.setMtime(System.currentTimeMillis());
    }

    public void updateResourceGroupDescription(Subject subject, int groupId, String description) {
        ResourceGroup group = getResourceGroupToBeModified(subject, groupId);
        group.setDescription(description);
        group.setMtime(System.currentTimeMillis());
    }

    public void updateResourceGroupLocation(Subject subject, int groupId, String location) {
        ResourceGroup group = getResourceGroupToBeModified(subject, groupId);
        group.setDescription(location);
        group.setMtime(System.currentTimeMillis());
    }

    private ResourceGroup getResourceGroupToBeModified(Subject subject, int groupId) {
        ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);

        if (group == null) {
            throw new ResourceGroupNotFoundException(groupId);
        }

        if (!authorizationManager.hasGroupPermission(subject, Permission.MODIFY_RESOURCE, groupId)) {
            throw new PermissionException("User [" + subject
                + "] does not have permission to modify Resource group with id [" + groupId + "].");
        }
        return group;
    }

}
