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
package org.rhq.enterprise.server.resource.group.definition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
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

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.exception.CreateException;
import org.rhq.enterprise.server.exception.UpdateException;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.RecursivityChangeType;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionCreateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionDeleteException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionNotFoundException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionUpdateException;
import org.rhq.enterprise.server.resource.group.definition.framework.ExpressionEvaluator;
import org.rhq.enterprise.server.resource.group.definition.framework.InvalidExpressionException;
import org.rhq.enterprise.server.resource.group.definition.mbean.GroupDefinitionRecalculationThreadMonitor;
import org.rhq.enterprise.server.resource.group.definition.mbean.GroupDefinitionRecalculationThreadMonitorMBean;

@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class GroupDefinitionManagerBean implements GroupDefinitionManagerLocal {
    private final Log log = LogFactory.getLog(GroupDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;
    private DatabaseType dbType;

    @EJB
    private GroupDefinitionManagerLocal groupDefinitionManager; // self, for xactional purposes

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

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

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void recalculateDynaGroups(Subject subject) {
        Query recalculationFinderQuery = entityManager
            .createNamedQuery(GroupDefinition.QUERY_FIND_IDS_FOR_RECALCULATION);
        recalculationFinderQuery.setParameter("now", System.currentTimeMillis());
        List<Integer> groupDefinitionIdsToRecalculate = recalculationFinderQuery.getResultList();

        if (groupDefinitionIdsToRecalculate.size() == 0) {
            return; // this will skip the info logging, so we only log when this method does something meaningful
        }

        GroupDefinitionRecalculationThreadMonitorMBean monitor = GroupDefinitionRecalculationThreadMonitor.getMBean();

        long totalStart = System.currentTimeMillis();
        for (Integer groupDefinitionId : groupDefinitionIdsToRecalculate) {
            long singleStart = System.currentTimeMillis();
            boolean success = false;
            try {
                groupDefinitionManager.calculateGroupMembership(subject, groupDefinitionId);
                success = true;
            } catch (Throwable t) {
                /* 
                 * be paranoid about capturing any and all kinds of errors, to give a chances for 
                 * all recalculations to complete in this (heart)beat of the recalculation thread
                 */
                log.error("Error recalculating DynaGroups for GroupDefinition[id=" + groupDefinitionId + "]", t);
            }
            long singleEnd = System.currentTimeMillis();

            try {
                GroupDefinition groupDefinition = getById(groupDefinitionId);
                int size = getManagedResourceGroupSizeForGroupDefinition(groupDefinitionId);
                monitor.updateStatistic(groupDefinition.getName(), size, success, singleEnd - singleStart);
            } catch (Throwable t) {
                log.error("Error updating DynaGroup statistics GroupDefinition[id=" + groupDefinitionId + "]", t);
                // ignore error during statistic update
            }
        }
        long totalEnd = System.currentTimeMillis();
        monitor.updateAutoRecalculationThreadTime(totalEnd - totalStart);
    }

    public GroupDefinition getById(int groupDefinitionId) throws GroupDefinitionNotFoundException {
        GroupDefinition groupDefinition = entityManager.find(GroupDefinition.class, groupDefinitionId);
        if (groupDefinition == null) {
            throw new GroupDefinitionNotFoundException("Group definition with specified id does not exist");
        }

        return groupDefinition;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public GroupDefinition createGroupDefinition(Subject subject, GroupDefinition newGroupDefinition)
        throws GroupDefinitionAlreadyExistsException, GroupDefinitionCreateException {

        try {
            validate(newGroupDefinition, null);
        } catch (GroupDefinitionException gde) {
            throw new GroupDefinitionCreateException(gde.getMessage());
        }

        try {
            entityManager.persist(newGroupDefinition);
        } catch (Exception e) {
            throw new GroupDefinitionCreateException(e);
        }

        return newGroupDefinition;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public GroupDefinition updateGroupDefinition(Subject subject, GroupDefinition groupDefinition)
        throws GroupDefinitionAlreadyExistsException, GroupDefinitionUpdateException, InvalidExpressionException,
        ResourceGroupUpdateException {

        boolean nameChanged = false;
        try {
            nameChanged = validate(groupDefinition, groupDefinition.getId());
        } catch (GroupDefinitionException gde) {
            throw new GroupDefinitionUpdateException(gde.getMessage());
        }

        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        for (String expression : groupDefinition.getExpressionAsList()) {
            evaluator.addExpression(expression);
        }

        RecursivityChangeType changeType = RecursivityChangeType.None;
        GroupDefinition attachedGroupDefinition = null;
        try {
            attachedGroupDefinition = getById(groupDefinition.getId());
        } catch (GroupDefinitionNotFoundException gdnfe) {
            throw new GroupDefinitionUpdateException(gdnfe.getMessage());
        }
        if (groupDefinition.isRecursive() == true && attachedGroupDefinition.isRecursive() == false) {
            // making a recursive group into a "normal" group 
            changeType = RecursivityChangeType.AddedRecursion;
        } else if (groupDefinition.isRecursive() == false && attachedGroupDefinition.isRecursive() == true) {
            // making a "normal" group into a recursive group
            changeType = RecursivityChangeType.RemovedRecursion;
        } else {
            // recursive bit didn't change
        }

        if (nameChanged || changeType != RecursivityChangeType.None) {
            String oldGroupDefinitionName = attachedGroupDefinition.getName();
            Subject overlord = subjectManager.getOverlord();
            for (ResourceGroup dynaGroup : attachedGroupDefinition.getManagedResourceGroups()) {
                String dynaGroupName = dynaGroup.getName();
                String newDynaGroupName = updateDynaGroupName(oldGroupDefinitionName, groupDefinition.getName(),
                    dynaGroupName);
                dynaGroup.setName(newDynaGroupName);
                // do not set recursive bit here
                // the update method will figure out whether to flip it by inspecting its managing GroupDefinition
                //dynaGroup.setRecursive(groupDefinition.isRecursive());
                resourceGroupManager.updateResourceGroup(overlord, dynaGroup, changeType);
            }
        }

        try {
            return entityManager.merge(groupDefinition);
        } catch (Exception e) {
            throw new GroupDefinitionUpdateException(e);
        }
    }

    // return boolean indicating whether the name of this group definition is changing
    private boolean validate(GroupDefinition definition, Integer id) throws GroupDefinitionException,
        GroupDefinitionAlreadyExistsException {
        String name = (definition.getName() == null ? "" : definition.getName().trim());
        String description = (definition.getDescription() == null ? "" : definition.getDescription().trim());

        if (name.equals("")) {
            throw new GroupDefinitionException("Name is a required property");
        }

        if (name.length() > 100) {
            throw new GroupDefinitionException("Name is limited to 100 characters");
        }

        if (description.length() > 100) {
            throw new GroupDefinitionException("Description is limited to 100 characters");
        }

        Query query = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_BY_NAME);
        query.setParameter("name", name);

        try {
            GroupDefinition found = (GroupDefinition) query.getSingleResult();
            if ((id == null) // null == id means creating new def - so if query has results, it's a dup
                || (found.getId() != id)) // found != id means updating def - so if query has result, only dup if ids don't match
            {
                throw new GroupDefinitionAlreadyExistsException("GroupDefinition with name " + name + " already exists");
            }
        } catch (NoResultException e) {
            // user is changing the name of the group, this is OK
            return true;
        }
        return false;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    // required for the recalculation thread
    public void calculateGroupMembership(Subject subject, int groupDefinitionId) throws CreateException,
        GroupDefinitionDeleteException, GroupDefinitionNotFoundException, InvalidExpressionException,
        ResourceGroupUpdateException {
        /*
         * even though this method declares to throw it, it should never generate an InvalidExpressionException because
         * the definition's expression set was validated before it was persisted.  conceivably, if the group definition
         * is persisted without being passed to the validating updateGroupDefinition( GroupDefinition ) method, then it
         * could throw a validation exception.
         *
         * so, callers to this method should catch InvalidExpressionException just in case, and give the user a chance to
         * correct the expression set before attempting to calculate the effective group again.
         */

        long startTime = System.currentTimeMillis();

        GroupDefinition groupDefinition = getById(groupDefinitionId);

        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        for (String expression : groupDefinition.getExpressionAsList()) {
            evaluator.addExpression(expression);
        }

        Collection<Integer> doomedResourceGroupIds = new ArrayList<Integer>();
        for (Integer managedGroupId : getManagedResourceGroupIdsForGroupDefinition(groupDefinitionId)) {
            doomedResourceGroupIds.add(managedGroupId);
        }

        for (ExpressionEvaluator.Result result : evaluator) {
            if (result == null) {
                /*
                 * skip null result elements, which represent queries that returned some null element -- this could be
                 * remedied by supporting "IS NULL" for parameter-replacement aside from just "= :bindArg" syntax
                 */
                continue;
            }

            /*
             * do one group at a time, to help prevent xaction timeouts
             * 
             * note: we don't need to pass the overlord here because all group definition / dynagroup functionality
             *       is hidden behind the MANAGE_INVENTORY permission, which is sufficient for all operations on a
             *       resource group including creation, deletion, and membership changes 
             */
            Integer nextResourceGroupId = groupDefinitionManager.calculateGroupMembership_helper(subject,
                groupDefinitionId, result);

            /*
             * remove all ids returned from the helper.  by the time we're done looping over all
             * ExpressionEvaluator.Result objects, the remaining objects in managedResourceGroupIds should represent
             * groups that no longer managed by this definition (either due to an inventory or expression change), and
             * are thus doomed
             */
            doomedResourceGroupIds.remove(nextResourceGroupId);
        }

        /*
         * and ids that are left over are doomed, but since deleting a resource group is related to the size of the
         * group, remove each group in it's own transaction
         * 
         * note: we don't need to pass the overlord here because all group definition / dynagroup functionality
         *       is hidden behind the MANAGE_INVENTORY permission, which is sufficient for all operations on a
         *       resource group including creation, deletion, and membership changes 
         */
        for (Integer doomedGroupId : doomedResourceGroupIds) {
            groupDefinitionManager.removeManagedResource_helper(subject, groupDefinitionId, doomedGroupId);
        }

        // re-attach the group, because it was cleared from the session during the callout to the helper
        groupDefinition = getById(groupDefinitionId);
        groupDefinition.setLastCalculationTime(System.currentTimeMillis());

        long endTime = System.currentTimeMillis();

        log.debug("calculateGroupMembership took " + (endTime - startTime) + " millis");
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Integer calculateGroupMembership_helper(Subject overlord, int groupDefinitionId,
        ExpressionEvaluator.Result result) throws CreateException, UpdateException, GroupDefinitionNotFoundException,
        GroupDefinitionNotFoundException {
        long startTime = System.currentTimeMillis();

        GroupDefinition groupDefinition = getById(groupDefinitionId);

        String groupByClause = result.getGroupByClause();
        ResourceGroup resourceGroup = resourceGroupManager.getByGroupDefinitionAndGroupByClause(groupDefinition
            .getId(), groupByClause);
        int resourceGroupId = 0;
        if (resourceGroup == null) {
            String newDynamicGroupName = getDynamicGroupName(groupDefinition.getName(), groupByClause);

            resourceGroup = new ResourceGroup(newDynamicGroupName);
            resourceGroupId = resourceGroupManager.createResourceGroup(overlord, resourceGroup).getId();

            resourceGroup.setRecursive(groupDefinition.isRecursive());
            resourceGroup.setGroupByClause(groupByClause);
            groupDefinition.addResourceGroup(resourceGroup);
        } else {
            resourceGroupId = resourceGroup.getId();
        }

        /*
         * group additions/deletions are actions made to the explicit group, the implicit group is modified (based on
         * the recursive bit) by the existing code in the resourceGroupManager
         *
         * use resourceManager.getExplicitResourceIdsByResourceGroup instead of resourceGroup.getExplicitResources to keep
         * the data we need to pull across the line from the database as small as possible
         */
        Collection<Integer> existingResourceIds = resourceManager.findExplicitResourceIdsByResourceGroup(resourceGroup
            .getId());

        Set<Integer> idsToAdd = new HashSet<Integer>(result.getData());
        idsToAdd.removeAll(existingResourceIds);

        Set<Integer> idsToRemove = new HashSet<Integer>(existingResourceIds);
        idsToRemove.removeAll(result.getData());

        resourceGroupManager.addResourcesToGroup(overlord, resourceGroupId, ArrayUtils.unwrapCollection(idsToAdd));
        resourceGroupManager.removeResourcesFromGroup(overlord, resourceGroupId, ArrayUtils
            .unwrapCollection(idsToRemove));

        resourceGroupManager.setResourceType(resourceGroupId);

        entityManager.flush();
        entityManager.clear();

        long endTime = System.currentTimeMillis();

        log.debug("calculateGroupMembership_helper took " + (endTime - startTime) + " millis");

        return resourceGroupId;
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

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ResourceGroupComposite> getManagedResourceGroups(Subject subject, int groupDefinitionId,
        PageControl pc) throws GroupDefinitionException {

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

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        List<Object[]> rawResults = new ArrayList<Object[]>();
        try {
            conn = rhqDs.getConnection();

            String query = GroupDefinition.QUERY_NATIVE_FIND_MEMBERS;

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
            stmt.setInt(1, groupDefinitionId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                long explicitCount = rs.getLong(1);
                double explicitAvail = rs.getDouble(2);
                long implicitCount = rs.getLong(3);
                double implicitAvail = rs.getDouble(4);
                int groupId = rs.getInt(5);
                Object[] next = new Object[] { explicitCount, explicitAvail, implicitCount, implicitAvail, groupId };
                rawResults.add(next);
            }
        } catch (Throwable t) {
            throw new GroupDefinitionException(t);
        } finally {
            JDBCUtil.safeClose(conn, stmt, rs);
        }

        //Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, GroupDefinition.QUERY_FIND_MEMBERS, pc);
        Query queryCount = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_MEMBERS_count);

        //query.setParameter("groupDefinitionId", groupDefinitionId);
        queryCount.setParameter("groupDefinitionId", groupDefinitionId);

        //List<Object[]> rawResults = (List<Object[]>) query.getResultList();
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

    @SuppressWarnings( { "unchecked" })
    public PageList<GroupDefinition> getGroupDefinitions(Subject subject, PageControl pc) {
        pc.initDefaultOrderingField("gd.name");
        if (authorizationManager.isInventoryManager(subject)) {

            Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, GroupDefinition.QUERY_FIND_ALL, pc);
            List<GroupDefinition> results = query.getResultList();

            int count = getGroupDefinitionCount(subject);

            return new PageList<GroupDefinition>(results, count, pc);
        } else {
            return new PageList<GroupDefinition>(pc);
        }
    }

    public int getGroupDefinitionCount(Subject subject) {
        if (authorizationManager.isInventoryManager(subject)) {
            Query queryCount = PersistenceUtility.createCountQuery(entityManager, GroupDefinition.QUERY_FIND_ALL);
            long count = (Long) queryCount.getSingleResult();
            return (int) count;
        } else {
            // instead of throwing an authorization exception, gracefully return 0
            return 0;
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public int getAutoRecalculationGroupDefinitionCount(Subject subject) {
        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            GroupDefinition.QUERY_FIND_ALL_RECALCULATING);
        long count = (Long) queryCount.getSingleResult();
        return (int) count;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public int getDynaGroupCount(Subject subject) {
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, GroupDefinition.QUERY_FIND_ALL_MEMBERS);
        long count = (Long) queryCount.getSingleResult();
        return (int) count;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeGroupDefinition(Subject subject, Integer groupDefinitionId)
        throws GroupDefinitionNotFoundException, GroupDefinitionDeleteException {
        Collection<Integer> managedGroupIds = getManagedResourceGroupIdsForGroupDefinition(groupDefinitionId);
        Subject overlord = subjectManager.getOverlord();
        for (Integer managedGroupId : managedGroupIds) {
            removeManagedResource_helper(overlord, groupDefinitionId, managedGroupId);
        }

        GroupDefinition groupDefinition = getById(groupDefinitionId);
        try {
            entityManager.remove(groupDefinition);
        } catch (Exception e) {
            throw new GroupDefinitionDeleteException("Error deleting groupDefinition '" + groupDefinition.getName()
                + "': ", e);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeManagedResource_helper(Subject overlord, int groupDefinitionId, Integer doomedGroupId)
        throws GroupDefinitionDeleteException, GroupDefinitionNotFoundException {
        GroupDefinition groupDefinition = getById(groupDefinitionId);
        ResourceGroup doomedGroup = entityManager.getReference(ResourceGroup.class, doomedGroupId);
        groupDefinition.removeResourceGroup(doomedGroup);

        try {
            /*
             * using the group manager's delete method ensures that auditing data,
             * such as completed operations, is correctly removed
             */
            resourceGroupManager.deleteResourceGroup(subjectManager.getOverlord(), doomedGroupId);
        } catch (Exception e) {
            throw new GroupDefinitionDeleteException("Error removing managedGroup '" + doomedGroup.getName() + "' "
                + "from groupDefinition '" + groupDefinition.getName() + "': ", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getManagedResourceGroupIdsForGroupDefinition(int groupDefinitionId) {
        Query query = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_MANAGED_RESOURCE_GROUP_IDS_ADMIN);
        query.setParameter("groupDefinitionId", groupDefinitionId);

        List<Integer> results = query.getResultList();
        return results;
    }

    private int getManagedResourceGroupSizeForGroupDefinition(int groupDefinitionId) {
        Query query = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_MANAGED_RESOURCE_GROUP_SIZE_ADMIN);
        query.setParameter("groupDefinitionId", groupDefinitionId);

        Number result = (Number) query.getSingleResult();
        return result.intValue();
    }

    private String getDynamicGroupName(String groupDefinitionName, String groupByClause) {
        String newDynamicGroupName = "DynaGroup - " + groupDefinitionName
            + (groupByClause.equals("") ? "" : (" ( " + groupByClause + " )"));
        return newDynamicGroupName;
    }

    private String updateDynaGroupName(String oldGroupDefinitionName, String updatedGroupDefinitionName,
        String dynaGroupName) throws GroupDefinitionUpdateException {
        String newGroupDefinitionName = updatedGroupDefinitionName;
        int oldGroupNameIndexStart = 12; // length of 'DynaGroup - ' prefix
        int oldGroupNameLength = oldGroupDefinitionName.length();
        return dynaGroupName.substring(0, oldGroupNameIndexStart) + //
            newGroupDefinitionName + //
            dynaGroupName.substring(oldGroupNameIndexStart + oldGroupNameLength);
    }
}