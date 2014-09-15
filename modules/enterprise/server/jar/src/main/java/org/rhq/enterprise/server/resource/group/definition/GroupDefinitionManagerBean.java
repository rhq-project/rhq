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
import java.util.Collections;
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.plugin.CannedGroupExpression;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.RecursivityChangeType;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionCreateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionDeleteException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionNotFoundException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionUpdateException;
import org.rhq.enterprise.server.resource.group.definition.framework.ExpressionEvaluator;
import org.rhq.core.domain.resource.group.InvalidExpressionException;
import org.rhq.enterprise.server.resource.group.definition.mbean.GroupDefinitionRecalculationThreadMonitor;
import org.rhq.enterprise.server.resource.group.definition.mbean.GroupDefinitionRecalculationThreadMonitorMBean;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

@Stateless
public class GroupDefinitionManagerBean implements GroupDefinitionManagerLocal, GroupDefinitionManagerRemote {
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

    public GroupDefinition updateGroupDefinition(Subject subject, GroupDefinition groupDefinition)
        throws GroupDefinitionAlreadyExistsException, GroupDefinitionUpdateException, InvalidExpressionException,
        ResourceGroupUpdateException {
        // whenever DG is updated from UI or remotely we detach it from cannedExpression
        return updateGroupDefinition(subject, groupDefinition, true);
    }
    
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    // required for the recalculation thread (same like calculateGroupMembership) this fixes BZ 976265
    private GroupDefinition updateGroupDefinition(Subject subject, GroupDefinition groupDefinition, boolean detachFromCannedExpression)
        throws GroupDefinitionAlreadyExistsException, GroupDefinitionUpdateException, InvalidExpressionException,
        ResourceGroupUpdateException {

        boolean nameChanged = false;
        try {
            nameChanged = validate(groupDefinition, groupDefinition.getId());
        } catch (GroupDefinitionException gde) {
            throw new GroupDefinitionUpdateException(gde.getMessage());
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

        // do not call entityManager.merge, it could overwrite managed fields
        // merge fields explicitly to control precisely which fields get updated
        attachedGroupDefinition.setName(groupDefinition.getName());
        attachedGroupDefinition.setDescription(groupDefinition.getDescription());
        attachedGroupDefinition.setRecursive(groupDefinition.isRecursive());
        attachedGroupDefinition.setExpression(groupDefinition.getExpression());
        attachedGroupDefinition.setRecalculationInterval(groupDefinition.getRecalculationInterval());
        if (detachFromCannedExpression) {
            attachedGroupDefinition.setCannedExpression(null);
        }
        return attachedGroupDefinition;
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
        if (name.contains("<") || name.contains("$") || name.contains("'") || name.contains("{") || name.contains("[")) {
            throw new GroupDefinitionException("Name must not contain <,$,',[,{ characters");
        }
        if (definition.getRecalculationInterval() < 0) {
            throw new GroupDefinitionException("Recalculation interval cannot be negative");
        }
        if (definition.getRecalculationInterval() > 0 && definition.getRecalculationInterval() < 60 * 1000) {
            throw new GroupDefinitionException(
                "Recalculation interval cannot be a positive number lower than 1 minute (60000ms)");
        }
        if (definition.getExpression() == null || definition.getExpression().isEmpty()) {
            throw new GroupDefinitionException("Expression is empty");
        }
        
        try {
            ExpressionEvaluator evaluator = new ExpressionEvaluator();
            for (String expression : definition.getExpressionAsList()) {
                evaluator.addExpression(expression);
            }
        } catch (InvalidExpressionException e) {
            throw new GroupDefinitionException("Cannot parse the expression: " + e.getMessage());
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
    public void calculateGroupMembership(Subject subject, int groupDefinitionId) throws ResourceGroupDeleteException,
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
        groupDefinition.setLastCalculationTime(System.currentTimeMillis()); // we're calculating now

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
             * as a result of recalculation, the membership may have changed such that a group which was previously
             * marked as compatible now becomes a mixed group.  if that happens, then the GroupCategory needs to be
             * updated and any compatible group constructs need to be removed from this group.  the following method
             * will achieve both of those goals
             */
            resourceGroupManager.setResourceTypeInNewTx(nextResourceGroupId);

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

        long endTime = System.currentTimeMillis();

        log.debug("calculateGroupMembership took " + (endTime - startTime) + " millis");
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Integer calculateGroupMembership_helper(Subject overlord, int groupDefinitionId,
        ExpressionEvaluator.Result result) throws ResourceGroupDeleteException, GroupDefinitionNotFoundException,
        GroupDefinitionNotFoundException {
        long startTime = System.currentTimeMillis();

        GroupDefinition groupDefinition = getById(groupDefinitionId);

        String groupByClause = result.getGroupByClause();
        ResourceGroup resourceGroup = resourceGroupManager.getByGroupDefinitionAndGroupByClause(
            groupDefinition.getId(), groupByClause);
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

        long endTime = System.currentTimeMillis();

        log.debug("calculateGroupMembership_helper took " + (endTime - startTime) + " millis");

        return resourceGroupId;
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

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<GroupDefinition> findGroupDefinitionsByCriteria(Subject subject,
        ResourceGroupDefinitionCriteria criteria) {
        if (authorizationManager.isInventoryManager(subject) == false) {
            if (criteria.isInventoryManagerRequired()) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] requires InventoryManager permission for requested query criteria.");
            }
        }
        
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<GroupDefinition> queryRunner = new CriteriaQueryRunner<GroupDefinition>(criteria,
            generator, entityManager);

        return queryRunner.execute();
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

    private void updateGroupProperties(GroupDefinition gd, CannedGroupExpression cge) {
        gd.setName(cge.getName());
        gd.setDescription(cge.getDescription());
        gd.setExpression(StringUtils.join(cge.getExpression(), "\n"));
        gd.setRecalculationInterval(cge.getRecalcInMinutes() * 60 * 1000L);
        gd.setRecursive(cge.isRecursive());
        gd.setCannedExpression(cge.getGroupDefinitionReferenceKey());
    }
    
    private GroupDefinition getByName(String name) {
        Query query = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_BY_NAME);
        query.setParameter("name", name);

        try {
            GroupDefinition found = (GroupDefinition) query.getSingleResult();
            return found;
        } catch (NoResultException e) {
            return null;
        }
    }

    private void updateDefinitionByCannedExpresssion(CannedGroupExpression cge) {
        Query query = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_BY_CANNED_EXPR_NAME);
        query.setParameter("cannedExpression", cge.getGroupDefinitionReferenceKey());
        @SuppressWarnings("unchecked")
        List<GroupDefinition> result = query.getResultList();
        if (cge.isCreateByDefault()) {
            // let's deploy dyna-group
            if (result.isEmpty()) {
                GroupDefinition sameName = getByName(cge.getName());
                if (sameName != null) {
                    log.info("Not creating DynaGroup based on ["+cge.getPlugin()+"] "+cge+" DynaGroup with same name already exists");
                    return;
                }
                log.info("Creating dynaGroup based on ["+cge.getPlugin()+"] "+cge);
                GroupDefinition gd = new GroupDefinition(cge.getName());
                updateGroupProperties(gd, cge);
                try {
                    createGroupDefinition(subjectManager.getOverlord(), gd);
                } catch (Exception ex) {
                   log.error(ex);
                }
            }
            else {
                log.info("Updating dynaGroup based on ["+cge.getPlugin()+"] "+cge);
                GroupDefinition gd = result.get(0);
                updateGroupProperties(gd, cge);
                try {
                    updateGroupDefinition(subjectManager.getOverlord(), gd, false);
                } catch (Exception ex) {
                   log.error(ex);
                }
            }
        } else {
            if (!result.isEmpty()) {
                // this might be upgrade
                // we'd like to delete referenced groupDefinition
                log.info("Purging "+result.get(0)+" because CannedExpression was upgraded in plugin with createByDefault=false");
                try {
                    removeGroupDefinition(subjectManager.getOverlord(), result.get(0).getId());
                } catch (Exception ex) {
                   log.error(ex);
                }
            }
        }
    }

    public void updateGroupsByCannedExpressions(String plugin, List<CannedGroupExpression> expressions) {
        if (expressions == null) {
            expressions = Collections.emptyList();
        }
        // create or update dyna groups based on our expressions
        for (CannedGroupExpression cge : expressions) {
            updateDefinitionByCannedExpresssion(cge);
        }
        Query query = entityManager.createNamedQuery(GroupDefinition.QUERY_FIND_LIKE_EXPR_NAME);
        query.setParameter("cannedExpression", plugin+":%"); // include separator ':' !
        @SuppressWarnings("unchecked")
        List<GroupDefinition> result = query.getResultList();
        Map<String,CannedGroupExpression> map = new HashMap<String, CannedGroupExpression>();
        for (CannedGroupExpression e : expressions) {
            map.put(e.getGroupDefinitionReferenceKey(), e);
        }
        for (GroupDefinition gd : result) {
            if (!map.containsKey(gd.getCannedExpression())) {
                log.info("Purging "+gd+" because base CannedExpression does not exist anymore");
                try {
                    removeGroupDefinition(subjectManager.getOverlord(), gd.getId());
                } catch (Exception ex) {
                   log.error(ex);
                }
            }
        }
    }
}