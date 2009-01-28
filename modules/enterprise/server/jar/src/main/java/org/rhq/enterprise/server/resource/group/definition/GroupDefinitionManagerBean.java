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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupAlreadyExistsException;
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
public class GroupDefinitionManagerBean implements GroupDefinitionManagerLocal {
    private final Log log = LogFactory.getLog(GroupDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private GroupDefinitionManagerLocal groupDefinitionManager; // self, for xactional purposes

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

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
                int size = getManagedResourceGroupIdsForGroupDefinition(groupDefinitionId).size();
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
        try {
            if (getById(groupDefinition.getId()).isRecursive() != groupDefinition.isRecursive()) {
                throw new GroupDefinitionUpdateException("Can not change the "
                    + (groupDefinition.isRecursive() ? "" : "non-") + "recursive nature of this group definition");
            }
        } catch (GroupDefinitionNotFoundException gdnfe) {
            throw new GroupDefinitionUpdateException(gdnfe.getMessage());
        }

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

        if (nameChanged) {
            String oldGroupDefinitionName = null;
            GroupDefinition attachedGroupDefinition = null;
            try {
                attachedGroupDefinition = getById(groupDefinition.getId());
                oldGroupDefinitionName = attachedGroupDefinition.getName();
            } catch (GroupDefinitionNotFoundException gdnfe) {
                throw new GroupDefinitionUpdateException(gdnfe.getMessage());
            }

            Subject overlord = subjectManager.getOverlord();
            for (ResourceGroup dynaGroup : attachedGroupDefinition.getManagedResourceGroups()) {
                String dynaGroupName = dynaGroup.getName();
                String newDynaGroupName = updateDynaGroupName(oldGroupDefinitionName, groupDefinition.getName(),
                    dynaGroupName);
                dynaGroup.setName(newDynaGroupName);
                resourceGroupManager.updateResourceGroup(overlord, dynaGroup);
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
    public void calculateGroupMembership(Subject subject, int groupDefinitionId) throws GroupDefinitionDeleteException,
        GroupDefinitionNotFoundException, InvalidExpressionException, ResourceGroupUpdateException {
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
            Integer nextResourceGroupId = calculateGroupMembership_helper(subject, groupDefinitionId, result);

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
            removeManagedResource_helper(subject, groupDefinitionId, doomedGroupId);
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
        ExpressionEvaluator.Result result) throws ResourceGroupAlreadyExistsException, ResourceGroupUpdateException,
        GroupDefinitionNotFoundException {
        long startTime = System.currentTimeMillis();

        GroupDefinition groupDefinition = getById(groupDefinitionId);

        String groupByClause = result.getGroupByClause();
        ResourceGroup resourceGroup = resourceGroupManager.findByGroupDefinitionAndGroupByClause(groupDefinition
            .getId(), groupByClause);
        int resourceGroupId = 0;
        if (resourceGroup == null) {
            String newDynamicGroupName = getDynamicGroupName(groupDefinition.getName(), groupByClause);

            resourceGroup = new ResourceGroup(newDynamicGroupName);
            resourceGroupId = resourceGroupManager.createResourceGroup(overlord, resourceGroup);

            resourceGroup.setRecursive(groupDefinition.isRecursive());
            resourceGroup.setGroupByClause(groupByClause);
            groupDefinition.addResourceGroup(resourceGroup);
        } else {
            resourceGroupId = resourceGroup.getId();
        }

        /*
         * group additions/deletions are actions made to the explicit group, the implicit group is populate (based on
         * the recursive bit) by the existing code in the resourceGroupManager
         *
         * use resourceManager.getExplicitResourceIdsByResourceGroup instead of resourceGroup.getExplicitResources to keep
         * the data we need to pull across the line from the database as small as possible
         */
        Collection<Integer> existingResourceIds = resourceManager.getExplicitResourceIdsByResourceGroup(resourceGroup
            .getId());

        Set<Integer> idsToAdd = new HashSet<Integer>(result.getData());
        idsToAdd.removeAll(existingResourceIds);

        Set<Integer> idsToRemove = new HashSet<Integer>(existingResourceIds);
        idsToRemove.removeAll(result.getData());

        resourceGroupManager.addResourcesToGroup(overlord, resourceGroupId, idsToAdd.toArray(new Integer[0]));
        resourceGroupManager.removeResourcesFromGroup(overlord, resourceGroupId, idsToRemove.toArray(new Integer[0]));

        try {
            resourceGroupManager.setResourceType(resourceGroupId);
        } catch (ResourceTypeNotFoundException rtnfe) {
            throw new ResourceGroupUpdateException("Could not set resourceType filter for this compatible group: ",
                rtnfe);
        }

        entityManager.flush();
        entityManager.clear();

        long endTime = System.currentTimeMillis();

        log.debug("calculateGroupMembership_helper took " + (endTime - startTime) + " millis");

        return resourceGroupId;
    }

    @SuppressWarnings( { "unchecked" })
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ResourceGroupComposite> getManagedResourceGroups(Subject subject, int groupDefinitionId,
        PageControl pc) {
        pc.initDefaultOrderingField("rg.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, GroupDefinition.QUERY_FIND_MEMBERS, pc);
        Query queryCount = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_MEMBERS_count);

        query.setParameter("groupDefinitionId", groupDefinitionId);
        queryCount.setParameter("groupDefinitionId", groupDefinitionId);

        long count = (Long) queryCount.getSingleResult();
        List<ResourceGroupComposite> results = query.getResultList();

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
        Collection<Integer> managedResourceIds = getManagedResourceGroupIdsForGroupDefinition(groupDefinitionId);
        Subject overlord = subjectManager.getOverlord();
        for (Integer managedGroupId : managedResourceIds) {
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

    @SuppressWarnings( { "unchecked" })
    private List<Integer> getManagedResourceGroupIdsForGroupDefinition(int groupDefinitionId) {
        Query query = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_MANAGED_RESOURCE_GROUP_IDS_ADMIN);
        query.setParameter("groupDefinitionId", groupDefinitionId);

        List<Integer> results = query.getResultList();
        return results;
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