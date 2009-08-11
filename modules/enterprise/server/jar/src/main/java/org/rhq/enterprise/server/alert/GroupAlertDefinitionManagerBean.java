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
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;

/**
 * @author Joseph Marques
 */

@Stateless
public class GroupAlertDefinitionManagerBean implements GroupAlertDefinitionManagerLocal {

    private static final Log LOG = LogFactory.getLog(GroupAlertDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    private SubjectManagerLocal subjectManager;

    @SuppressWarnings("unchecked")
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
            modified += alertDefinitionManager
                .removeAlertDefinitions(subject, new Integer[] { groupAlertDefinitionId });
            alertDefinitionManager.removeAlertDefinitions(overlord, childDefinitions
                .toArray(new Integer[childDefinitions.size()]));
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

    public void updateAlertDefinitionsForResource(Subject subject, AlertDefinition groupAlertDefinition,
        Integer resourceId) throws AlertDefinitionCreationException, InvalidAlertDefinitionException {
        if (authorizationManager.isOverlord(subject) == false) {
            throw new AlertDefinitionCreationException("Updating the alert definitions for a resource "
                + "is an implicit system operation and must only be performed by the overlord");
        }

        // construct the child
        AlertDefinition childAlertDefinition = new AlertDefinition(groupAlertDefinition);
        childAlertDefinition.setGroupAlertDefinition(groupAlertDefinition);

        // persist the child
        alertDefinitionManager.createAlertDefinition(subject, childAlertDefinition, resourceId);
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

    public int createGroupAlertDefinitions(Subject subject, AlertDefinition groupAlertDefinition,
        Integer resourceGroupId) throws InvalidAlertDefinitionException {
        ResourceGroup group = entityManager.find(ResourceGroup.class, resourceGroupId);
        groupAlertDefinition.setResourceGroup(group);
        int groupAlertDefinitionId = alertDefinitionManager.createAlertDefinition(subject, groupAlertDefinition, null);
        group = entityManager.merge(group);

        int definitionCount = 0;
        Subject overlord = subjectManager.getOverlord();
        List<Integer> resourceIdsForGroup = getCommittedResourceIdsNeedingGroupAlertDefinitionApplication(subject,
            groupAlertDefinitionId, resourceGroupId);

        for (Integer resourceId : resourceIdsForGroup) {
            try {
                // make sure we perform the system side-effects as the overlord
                updateAlertDefinitionsForResource(overlord, groupAlertDefinition, resourceId);

                // rev2804 - flush/clear after 250 definitions for good performance
                if (++definitionCount % 250 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            } catch (AlertDefinitionCreationException adce) {
                /* should never happen because AlertDefinitionCreationException is only ever
                 * thrown if updateAlertDefinitionsForResource isn't called as the overlord
                 *
                 * but we'll log it anyway, just in case, so it isn't just swallowed
                 */
                LOG.error(adce);
            }
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

            modified += alertDefinitionManager.disableAlertDefinitions(subject,
                new Integer[] { groupAlertDefinitionId });
            alertDefinitionManager.disableAlertDefinitions(overlord, alertDefinitions
                .toArray(new Integer[alertDefinitions.size()]));
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

            modified += alertDefinitionManager
                .enableAlertDefinitions(subject, new Integer[] { groupAlertDefinitionId });
            alertDefinitionManager.enableAlertDefinitions(overlord, alertDefinitions
                .toArray(new Integer[alertDefinitions.size()]));
        }
        return modified;
    }

    public AlertDefinition updateGroupAlertDefinitions(Subject subject, AlertDefinition groupAlertDefinition,
        boolean purgeInternals) throws InvalidAlertDefinitionException {
        AlertDefinition updatedTemplate = null;

        try {
            updatedTemplate = alertDefinitionManager.updateAlertDefinition(subject, groupAlertDefinition.getId(),
                groupAlertDefinition, purgeInternals); // do not allow direct undeletes of an alert definition
        } catch (AlertDefinitionUpdateException adue) {
            /* jmarques (Oct 10, 2007)
             *
             * this should never happen, there is currently no way to update a deleted alert template via the JON UI
             */
            LOG.error("Attempt to update a deleted groupAlertDefinition " + groupAlertDefinition.toSimpleString());
        }

        Subject overlord = subjectManager.getOverlord();
        int definitionCount = 0;

        /*
         * update all of the definitions that were spawned from this groupAlertDefinition
         */
        List<Integer> alertDefinitions = getChildrenAlertDefinitionIds(overlord, groupAlertDefinition.getId());
        for (Integer alertDefinitionId : alertDefinitions) {
            try {

                alertDefinitionManager.updateAlertDefinition(overlord, alertDefinitionId, groupAlertDefinition,
                    purgeInternals); // if the child is deleted, we will undelete is as part of the update
            } catch (AlertDefinitionUpdateException adue) {
                /* jmarques (Oct 10, 2007), as of this writing...
                 *
                 * this should not happen, because the call to getAlertDefinitionIdsByTemplateId only returns
                 * non-deleted definitions.
                 *
                 * if a definition was deleted, then it will signify to the system that a new definition has to be
                 * created in its place, which is what the call out to
                 * getResourceIdsWithNoDefinitionFromThisTemplate is all about.
                 *
                 * but, it's conceivable that in the future an AlertDefinitionUpdateException can be thrown for other
                 * reasons, so we want to catch that here and, at the very least, log it.
                 */
                LOG.error("Attempt to update a deleted template " + groupAlertDefinition.toSimpleString());
            }

            // rev2804 - flush/clear after 250 definitions for good performance
            if (++definitionCount % 250 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        /*
         * if the subject deleted the alertDefinition spawned from a groupAlertDefinition, cascade update will recreate it
         */
        List<Integer> resourceIds = getCommittedResourceIdsNeedingGroupAlertDefinitionApplication(overlord,
            groupAlertDefinition.getId(), getResourceGroupIdAlertTemplateId(groupAlertDefinition.getId()));
        try {
            for (Integer resourceId : resourceIds) {
                updateAlertDefinitionsForResource(overlord, groupAlertDefinition, resourceId);

                // rev2804 - flush/clear after 250 definitions for good performance
                if (++definitionCount % 250 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        } catch (AlertDefinitionCreationException adce) {
            /* should never happen because AlertDefinitionCreationException is only ever
             * thrown if updateAlertDefinitionsForResource isn't called as the overlord
             *
             * but we'll log it anyway, just in case, so it isn't just swallowed
             */
            LOG.error(adce);
        }

        return updatedTemplate;
    }

    public void addGroupAlertDefinitions(Subject subject, int resourceGroupId, int[] resourcesIdsToAdd) {
        if (resourcesIdsToAdd == null || resourcesIdsToAdd.length == 0) {
            return;
        }

        List<AlertDefinition> groupAlertDefinitions = findGroupAlertDefinitions(subject, resourceGroupId, PageControl
            .getUnlimitedInstance());

        int definitionCount = 0;
        Subject overlord = subjectManager.getOverlord();
        for (AlertDefinition groupAlertDefinition : groupAlertDefinitions) {
            for (Integer resourceId : resourcesIdsToAdd) {
                try {
                    // make sure we perform the system side-effects as the overlord
                    updateAlertDefinitionsForResource(overlord, groupAlertDefinition, resourceId);

                    // rev2804 - flush/clear after 250 definitions for good performance
                    if (++definitionCount % 250 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                } catch (AlertDefinitionCreationException adce) {
                    /* should never happen because AlertDefinitionCreationException is only ever
                     * thrown if updateAlertDefinitionsForResource isn't called as the overlord
                     *
                     * but we'll log it anyway, just in case, so it isn't just swallowed
                     */
                    LOG.error(adce);
                } catch (InvalidAlertDefinitionException iade) {
                    /*
                     * should never happen because the alert definitions we're creating already exist on the resourceGroup
                     * as well as it's current resource members; children are spawned from the parent groupAlertDefinition,
                     * and so should also be valid
                     * 
                     *  but we'll log it anyway, just in case, so it isn't just swallowed
                     */
                    LOG.error(iade);
                }
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void purgeAllGroupAlertDefinitions(Subject subject, int resourceGroupId) {
        List<AlertDefinition> groupAlertDefinitions = findGroupAlertDefinitions(subject, resourceGroupId, PageControl
            .getUnlimitedInstance());

        int i = 0;
        Integer[] groupAlertDefinitionIds = new Integer[groupAlertDefinitions.size()];
        for (AlertDefinition groupAlertDefinition : groupAlertDefinitions) {
            groupAlertDefinitionIds[i++] = groupAlertDefinition.getId();
        }

        removeGroupAlertDefinitions(subject, groupAlertDefinitionIds);
    }

    public void removeGroupAlertDefinitions(Subject subject, int resourceGroupId, int[] resourceIdsToRemove) {
        if (resourceIdsToRemove == null || resourceIdsToRemove.length == 0) {
            return;
        }

        List<AlertDefinition> groupAlertDefinitions = findGroupAlertDefinitions(subject, resourceGroupId, PageControl
            .getUnlimitedInstance());

        List<Integer> allChildrenDefinitionIds = new ArrayList<Integer>();
        Subject overlord = subjectManager.getOverlord();
        for (AlertDefinition groupAlertDefinition : groupAlertDefinitions) {
            List<Integer> childDefinitions = getChildrenAlertDefinitionIds(subject, groupAlertDefinition.getId());
            allChildrenDefinitionIds.addAll(childDefinitions);
            alertDefinitionManager.removeAlertDefinitions(overlord, childDefinitions
                .toArray(new Integer[childDefinitions.size()]));
        }

        /*
         * break the Hibernate relationships used for navigating between the groupAlertDefinition and the
         * children alertDefinitions so that the async deletion mechanism can delete without FK violations
         */
        if (allChildrenDefinitionIds.size() > 0) {
            Query breakLinksQuery = entityManager.createNamedQuery(AlertDefinition.QUERY_UPDATE_SET_PARENTS_NULL);
            breakLinksQuery.setParameter("childrenDefinitionIds", allChildrenDefinitionIds);
            breakLinksQuery.executeUpdate();
        }
    }

    private int getResourceGroupIdAlertTemplateId(int groupAlertDefinitionId) {
        Query query = entityManager.createQuery("" //
            + "SELECT groupAlertDefinition.resourceGroup.id " //
            + "  FROM AlertDefinition groupAlertDefinition " //
            + " WHERE groupAlertDefinition.id = :groupAlertDefinitionId");
        query.setParameter("groupAlertDefinitionId", groupAlertDefinitionId);
        int groupId = ((Number) query.getSingleResult()).intValue();
        return groupId;
    }
}