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
package org.rhq.enterprise.server.alert;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility.SerializationType;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * @author Joseph Marques
 */
@Stateless
public class GroupAlertDefinitionManagerBean implements GroupAlertDefinitionManagerLocal {

    private static final Log LOG = LogFactory.getLog(GroupAlertDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    //@IgnoreDependency
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private SubjectManagerLocal subjectManager;

    @SuppressWarnings("unchecked")
    @Deprecated
    // remove along with portal war
    public PageList<AlertDefinition> findGroupAlertDefinitions(Subject subject, int resourceGroupId,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("ctime", PageOrdering.DESC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            AlertDefinition.QUERY_FIND_BY_RESOURCE_GROUP);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            AlertDefinition.QUERY_FIND_BY_RESOURCE_GROUP, pageControl);

        queryCount.setParameter("groupId", resourceGroupId);
        query.setParameter("groupId", resourceGroupId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<AlertDefinition> list = query.getResultList();

        return new PageList<AlertDefinition>(list, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getChildrenAlertDefinitionIds(Subject subject, int groupAlertDefinitionId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_BY_GROUP_ALERT_DEFINITION_ID);
        query.setParameter("groupAlertDefinitionId", groupAlertDefinitionId);

        List<Integer> list = query.getResultList();
        return list;
    }

    public int removeGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds) {
        if (groupAlertDefinitionIds == null || groupAlertDefinitionIds.length == 0) {
            return 0;
        }

        int modified = 0;
        List<Integer> allChildDefinitionIds = new ArrayList<Integer>();
        Subject overlord = subjectManager.getOverlord();
        for (Integer groupAlertDefinitionId : groupAlertDefinitionIds) {
            List<Integer> childDefinitions = getChildrenAlertDefinitionIds(subject, groupAlertDefinitionId);
            allChildDefinitionIds.addAll(childDefinitions);
            modified += alertDefinitionManager.removeAlertDefinitions(subject, new int[] { groupAlertDefinitionId });
            alertDefinitionManager.removeAlertDefinitions(overlord, ArrayUtils.unwrapCollection(childDefinitions));
        }

        /*
         * break the Hibernate relationships used for navigating between the groupAlertDefinition and the
         * children alertDefinitions so that the async deletion mechanism can delete without FK violations
         */
        if (allChildDefinitionIds.size() > 0) {
            Query breakLinksQuery = entityManager.createNamedQuery(AlertDefinition.QUERY_UPDATE_SET_PARENTS_NULL);
            breakLinksQuery.setParameter("childrenDefinitionIds", allChildDefinitionIds);
            breakLinksQuery.executeUpdate();
        }

        return modified;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getCommittedResourceIdsNeedingGroupAlertDefinitionApplication(Subject subject,
        int groupAlertDefinitionId, int resourceGroupId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_RESOURCE_IDS_NEEDING_GROUP_APPLICATION);
        query.setParameter("groupAlertDefinitionId", groupAlertDefinitionId);
        query.setParameter("resourceGroupId", resourceGroupId);
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        List<Integer> list = query.getResultList();
        return list;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int createGroupAlertDefinitions(Subject subject, AlertDefinition groupAlertDefinition,
        Integer resourceGroupId) throws InvalidAlertDefinitionException, AlertDefinitionCreationException {
        ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, resourceGroupId, null);
        groupAlertDefinition.setGroup(group);

        int groupAlertDefinitionId = 0;
        try {
            groupAlertDefinitionId = alertDefinitionManager.createAlertDefinitionInNewTransaction(subject,
                groupAlertDefinition, null, true);
        } catch (Throwable t) {
            throw new AlertDefinitionCreationException("Could not create groupAlertDefinitions for " + group
                + " with data " + groupAlertDefinition.toSimpleString(), t);
        }

        //get the alert definition we just created.. this is so that we can create copies of it
        AlertDefinition persistedDefinition = alertDefinitionManager
            .getAlertDefinition(subject, groupAlertDefinitionId);

        Throwable firstThrowable = null;

        List<Integer> resourceIdsForGroup = getCommittedResourceIdsNeedingGroupAlertDefinitionApplication(subject,
            groupAlertDefinitionId, resourceGroupId);
        List<Integer> resourceIdsInError = new ArrayList<Integer>();
        for (Integer resourceId : resourceIdsForGroup) {
            try {
                // construct the child
                AlertDefinition childAlertDefinition = new AlertDefinition(persistedDefinition);
                childAlertDefinition.setGroupAlertDefinition(groupAlertDefinition);

                // persist the child
                alertDefinitionManager.createDependentAlertDefinition(subject, childAlertDefinition, resourceId);
            } catch (Throwable t) {
                // continue on error, create as many as possible
                if (firstThrowable == null) {
                    firstThrowable = t;
                }
                resourceIdsInError.add(resourceId);
            }
        }
        if (firstThrowable != null) {
            throw new AlertDefinitionCreationException("Could not create alert definition child for Resources "
                + resourceIdsInError + " with group " + groupAlertDefinition.toSimpleString(), firstThrowable);
        }

        return groupAlertDefinitionId;

    }

    public int disableGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds) {
        if (groupAlertDefinitionIds == null || groupAlertDefinitionIds.length == 0) {
            return 0;
        }

        int modified = 0;
        Subject overlord = subjectManager.getOverlord();
        for (Integer groupAlertDefinitionId : groupAlertDefinitionIds) {
            List<Integer> alertDefinitions = getChildrenAlertDefinitionIds(subject, groupAlertDefinitionId);

            modified += alertDefinitionManager.disableAlertDefinitions(subject, new int[] { groupAlertDefinitionId });
            alertDefinitionManager.disableAlertDefinitions(overlord, ArrayUtils.unwrapCollection(alertDefinitions));
        }
        return modified;
    }

    public int enableGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds) {
        if (groupAlertDefinitionIds == null || groupAlertDefinitionIds.length == 0) {
            return 0;
        }

        int modified = 0;
        Subject overlord = subjectManager.getOverlord();
        for (Integer groupAlertDefinitionId : groupAlertDefinitionIds) {
            List<Integer> alertDefinitions = getChildrenAlertDefinitionIds(subject, groupAlertDefinitionId);

            modified += alertDefinitionManager.enableAlertDefinitions(subject, new int[] { groupAlertDefinitionId });
            alertDefinitionManager.enableAlertDefinitions(overlord, ArrayUtils.unwrapCollection(alertDefinitions));
        }
        return modified;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AlertDefinition updateGroupAlertDefinitions(Subject subject, AlertDefinition groupAlertDefinition,
        boolean resetMatching) throws InvalidAlertDefinitionException, AlertDefinitionUpdateException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("updateGroupAlertDefinition: " + groupAlertDefinition);
        }

        // first update the actual alert group alert definition
        AlertDefinition updated = null;
        try {
            updated = alertDefinitionManager.updateAlertDefinition(subject, groupAlertDefinition.getId(),
                groupAlertDefinition, resetMatching); // do not allow direct undeletes of an alert definition
        } catch (Throwable t) {
            throw new AlertDefinitionUpdateException("Failed to update a GroupAlertDefinition: "
                + groupAlertDefinition.toSimpleString(), t);
        }

        // overlord will be used for all system-side effects as a result of updating this alert template
        Subject overlord = subjectManager.getOverlord();
        Throwable firstThrowable = null;

        // update all of the definitions that were spawned from this group alert definition
        List<Integer> alertDefinitions = getChildrenAlertDefinitionIds(overlord, groupAlertDefinition.getId());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Need to update the following children alert definition ids: " + alertDefinitions);
        }
        List<Integer> alertDefinitionIdsInError = new ArrayList<Integer>();

        for (Integer alertDefinitionId : alertDefinitions) {
            try {
                alertDefinitionManager.updateDependentAlertDefinition(subject, alertDefinitionId, updated,
                    resetMatching);
            } catch (Throwable t) {
                // continue on error, update as many as possible
                if (firstThrowable == null) {
                    firstThrowable = t;
                }
                alertDefinitionIdsInError.add(alertDefinitionId);
            }
        }

        // if the subject deleted the alertDefinition spawned from a groupAlertDefinition, cascade update will recreate it
        List<Integer> resourceIds = getCommittedResourceIdsNeedingGroupAlertDefinitionApplication(overlord,
            groupAlertDefinition.getId(), getResourceGroupIdForAlertDefinitionId(groupAlertDefinition.getId()));
        List<Integer> resourceIdsInError = new ArrayList<Integer>();
        for (Integer resourceId : resourceIds) {
            try {
                // construct the child
                AlertDefinition childAlertDefinition = new AlertDefinition(updated);
                childAlertDefinition.setGroupAlertDefinition(groupAlertDefinition);

                // persist the child
                alertDefinitionManager.createAlertDefinitionInNewTransaction(subject, childAlertDefinition, resourceId,
                    false);
            } catch (Throwable t) {
                // continue on error, update as many as possible
                if (firstThrowable == null) {
                    firstThrowable = t;
                }
                resourceIdsInError.add(resourceId);
            }
        }
        if (firstThrowable != null) {
            StringBuilder error = new StringBuilder();
            if (alertDefinitionIdsInError.size() != 0) {
                error.append("Failed to update child AlertDefinitions " + alertDefinitionIdsInError + " ; ");
            }
            if (resourceIdsInError.size() != 0) {
                error.append("Failed to re-create child AlertDefinition for Resources " + resourceIdsInError + "; ");
            }
            throw new AlertDefinitionUpdateException(error.toString(), firstThrowable);
        }

        return updated;
    }

    public void addGroupMemberAlertDefinitions(Subject subject, int resourceGroupId, int[] addedResourceIds)
        throws AlertDefinitionCreationException {
        if (addedResourceIds == null || addedResourceIds.length == 0) {
            return;
        }

        List<Integer> resourceIdsInError = new ArrayList<Integer>();
        Throwable firstThrowable = null;

        // We want to copy the group level AlertDefinitions, so fetch them with the relevant lazy fields, so we
        // have everything we need when calling the copy constructor, minimizing 
        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterResourceGroupIds(resourceGroupId);
        criteria.fetchGroupAlertDefinition(false);
        criteria.fetchConditions(true);
        criteria.fetchAlertNotifications(true);
        // Apply paging when optionally fetching collections, to avoid duplicates. Hibernate seems to apply DISTINCT,
        // which is what we want.  Use a huge # because we want them all.
        criteria.setPaging(0, Integer.MAX_VALUE);

        List<AlertDefinition> groupAlertDefinitions = alertDefinitionManager.findAlertDefinitionsByCriteria(subject,
            criteria);

        for (AlertDefinition groupAlertDefinition : groupAlertDefinitions) {
            for (Integer resourceId : addedResourceIds) {
                try {
                    // Construct the resource-level AlertDefinition by using the copy constructor.
                    AlertDefinition childAlertDefinition = new AlertDefinition(groupAlertDefinition);

                    // groupAlertDefinition is an attached entity. It is dangerous to pass attached entities (with
                    // Hibernate proxies for the current session) across sessions. Since the call to create the new
                    // AlertDefinition is performed in a new transaction, make sure not to pass the attached entity. 
                    // Just use a simple stand-in to create the link to the group alert definition.
                    AlertDefinition groupAlertDefinitionPojo = new AlertDefinition();
                    groupAlertDefinitionPojo.setId(groupAlertDefinition.getId());
                    childAlertDefinition.setGroupAlertDefinition(groupAlertDefinitionPojo);

                    // Persist the resource-level (child) alert definition, cleanse any further proxies left
                    // over from the copy constructor.
                    HibernateDetachUtility.nullOutUninitializedFields(childAlertDefinition,
                        SerializationType.SERIALIZATION);

                    alertDefinitionManager.createAlertDefinitionInNewTransaction(subjectManager.getOverlord(),
                        childAlertDefinition, resourceId, false);
                } catch (Throwable t) {
                    // continue on error, create as many as possible
                    if (firstThrowable == null) {
                        firstThrowable = t;
                    }
                    resourceIdsInError.add(resourceId);
                }
            }
        }
        if (firstThrowable != null) {
            throw new AlertDefinitionCreationException(
                "Could not create group alert definition children for Resources " + resourceIdsInError
                    + " under ResourceGroup[id=" + resourceGroupId + "]", firstThrowable);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void purgeAllGroupAlertDefinitions(Subject subject, int resourceGroupId) {
        Integer[] groupAlertDefinitionIdsForResourceGroup = findGroupAlertDefinitionIds(resourceGroupId);
        removeGroupAlertDefinitions(subject, groupAlertDefinitionIdsForResourceGroup);
    }

    @SuppressWarnings("unchecked")
    private Integer[] findGroupAlertDefinitionIds(int resourceGroupId) {
        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterResourceGroupIds(resourceGroupId);
        criteria.setPageControl(PageControl.getUnlimitedInstance());

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);
        generator.alterProjection("alertdefinition.id");
        Query query = generator.getQuery(entityManager);
        List<Integer> groupAlertDefinitionIds = query.getResultList();

        Integer[] results = groupAlertDefinitionIds.toArray(new Integer[groupAlertDefinitionIds.size()]);
        return results;
    }

    public void removeGroupMemberAlertDefinitions(Subject subject, int resourceGroupId, Integer[] removedResourceIds) {
        if (removedResourceIds == null || removedResourceIds.length == 0) {
            return;
        }

        // fetch the resource-level AlertDefs tied to the Group from which resources are being removed
        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterResourceIds(removedResourceIds);
        criteria.addFilterGroupAlertDefinitionGroupId(resourceGroupId);
        criteria.addFilterDeleted(false);
        criteria.clearPaging();

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<AlertDefinition> queryRunner = new CriteriaQueryRunner<AlertDefinition>(criteria,
            generator, entityManager);
        List<AlertDefinition> alertDefinitions = queryRunner.execute();

        // No group alert defs, just return
        if (alertDefinitions.isEmpty()) {
            return;
        }

        // 1) remove only the attached (i.e. non-protected) alert defs.
        // 2) break all of the FK references to the group
        List<Integer> allMemberAlertDefinitionIds = new ArrayList<Integer>(alertDefinitions.size());
        List<Integer> attachedMemberAlertDefinitionIds = new ArrayList<Integer>(alertDefinitions.size());
        for (AlertDefinition ad : alertDefinitions) {
            Integer id = ad.getId();
            allMemberAlertDefinitionIds.add(id);
            if (!ad.isReadOnly()) {
                attachedMemberAlertDefinitionIds.add(id);
            }
        }

        // 1) remove only the attached (i.e. non-protected) alert defs.
        if (!attachedMemberAlertDefinitionIds.isEmpty()) {
            int[] groupMemberAlertDefinitionIdsArray = ArrayUtils.unwrapCollection(attachedMemberAlertDefinitionIds);

            alertDefinitionManager.removeAlertDefinitions(subjectManager.getOverlord(),
                groupMemberAlertDefinitionIdsArray);
        }

        // 2) break all of the references to the group
        Query breakLinksQuery = entityManager.createNamedQuery(AlertDefinition.QUERY_UPDATE_SET_PARENTS_NULL);
        breakLinksQuery.setParameter("childrenDefinitionIds", allMemberAlertDefinitionIds);
        breakLinksQuery.executeUpdate();

        alertDefinitions.clear();
        allMemberAlertDefinitionIds.clear();
        attachedMemberAlertDefinitionIds.clear();
    }

    private int getResourceGroupIdForAlertDefinitionId(int groupAlertDefinitionId) {
        Query query = entityManager.createQuery("" //
            + "SELECT groupAlertDefinition.group.id " //
            + "  FROM AlertDefinition groupAlertDefinition " //
            + " WHERE groupAlertDefinition.id = :groupAlertDefinitionId");
        query.setParameter("groupAlertDefinitionId", groupAlertDefinitionId);
        int groupId = ((Number) query.getSingleResult()).intValue();
        return groupId;
    }
}