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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * @author Joseph Marques
 */

@Stateless
public class AlertTemplateManagerBean implements AlertTemplateManagerLocal {
    @SuppressWarnings("unused")
    private static final Log LOG = LogFactory.getLog(AlertTemplateManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    private AlertTemplateManagerLocal alertTemplateManager;
    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;
    @EJB
    private SubjectManagerLocal subjectManager;

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinition> getAlertTemplates(Subject user, int resourceTypeId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("ctime", PageOrdering.DESC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            AlertDefinition.QUERY_FIND_BY_RESOURCE_TYPE);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            AlertDefinition.QUERY_FIND_BY_RESOURCE_TYPE, pageControl);

        queryCount.setParameter("typeId", resourceTypeId);
        query.setParameter("typeId", resourceTypeId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<AlertDefinition> list = query.getResultList();

        return new PageList<AlertDefinition>(list, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getAlertDefinitionIdsByTemplateId(Subject user, int alertTemplateId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_BY_ALERT_TEMPLATE_ID);
        query.setParameter("alertTemplateId", alertTemplateId);

        List<Integer> list = query.getResultList();
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getCommittedResourceIdsWithNoDefinitionFromThisTemplate(Subject user, int alertTemplateId,
        int resourceTypeId) {
        Query query = entityManager
            .createNamedQuery(AlertDefinition.QUERY_FIND_RESOURCE_IDS_WITH_NO_ACTIVE_TEMPLATE_DEFINITION);
        query.setParameter("alertTemplateId", alertTemplateId);
        query.setParameter("resourceTypeId", resourceTypeId);
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        List<Integer> list = query.getResultList();
        return list;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public int createAlertTemplate(Subject user, AlertDefinition alertTemplate, Integer resourceTypeId, boolean cascade)
        throws InvalidAlertDefinitionException, ResourceTypeNotFoundException {
        ResourceType type = resourceTypeManager.getResourceTypeById(user, resourceTypeId);

        alertTemplate.setResourceType(type); // mark this as an alert "template" definition
        int alertTemplateId = alertDefinitionManager.createAlertDefinition(user, alertTemplate, null);

        if (cascade) {
            try {
                int definitionCount = 0;
                for (Resource resource : type.getResources()) {
                    if (resource.getInventoryStatus() != InventoryStatus.COMMITTED) {
                        continue;
                    }

                    // make sure we perform the system side-effects as the overlord
                    updateAlertDefinitionsForResource(subjectManager.getOverlord(), alertTemplate, resource.getId());

                    /* 
                     * flush/clear after only 5 definitions (as opposed to a larger batch) because an alert
                     * definition is actual many objects: the definition, the condition set, the notification
                     * set, the alert dampening rules 
                     */
                    if (++definitionCount % 5 == 0) {
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
        }

        return alertTemplateId;
    }

    public void updateAlertDefinitionsForResource(Subject user, Integer resourceId)
        throws AlertDefinitionCreationException, InvalidAlertDefinitionException {

        updateAlertDefinitionsForResource(user, resourceId, false);
    }

    public void updateAlertDefinitionsForResource(Subject user, int resourceId, boolean descendants)
        throws AlertDefinitionCreationException, InvalidAlertDefinitionException {
        if (0 == resourceId) {
            throw new AlertDefinitionCreationException("Unexpected resourceId = 0");
        }

        if (authorizationManager.isOverlord(user) == false) {
            throw new AlertDefinitionCreationException(
                "Updating the alert definitions for a resource is an implicit system operation and must only be performed by the overlord");
        }

        Resource resource = entityManager.find(Resource.class, resourceId);

        if (descendants) {
            for (Resource child : resource.getChildResources()) {
                updateAlertDefinitionsForResource(user, child.getId(), descendants);
            }
        }

        ResourceType resourceType = resource.getResourceType();

        List<AlertDefinition> templates = getAlertTemplates(user, resourceType.getId(), PageControl
            .getUnlimitedInstance());

        for (AlertDefinition template : templates) {
            alertTemplateManager.updateAlertDefinitionsForResource(user, template, resource.getId());
        }
    }

    public void updateAlertDefinitionsForResource(Subject user, AlertDefinition alertTemplate, Integer resourceId)
        throws AlertDefinitionCreationException, InvalidAlertDefinitionException {
        if (authorizationManager.isOverlord(user) == false) {
            throw new AlertDefinitionCreationException("Updating the alert definitions for a resource "
                + "is an implicit system operation " + "and must only be performed by the overlord");
        }

        // construct the child
        AlertDefinition childAlertDefinition = new AlertDefinition(alertTemplate);
        childAlertDefinition.setParentId(alertTemplate.getId());

        // persist the child
        alertDefinitionManager.createAlertDefinition(user, childAlertDefinition, resourceId);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void removeAlertTemplates(Subject user, Integer[] alertTemplateIds, boolean cascade) {
        for (Integer alertTemplateId : alertTemplateIds) {
            alertDefinitionManager.removeAlertDefinitions(user, new Integer[] { alertTemplateId });

            if (cascade) {
                // cascading is a system side effects, and so should be performed by the overlord
                List<Integer> alertDefinitions = getAlertDefinitionIdsByTemplateId(user, alertTemplateId);
                alertDefinitionManager.removeAlertDefinitions(subjectManager.getOverlord(), alertDefinitions
                    .toArray(new Integer[0]));
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void enableAlertTemplates(Subject user, Integer[] alertTemplateIds, boolean cascade) {
        for (Integer alertTemplateId : alertTemplateIds) {
            alertDefinitionManager.enableAlertDefinitions(user, new Integer[] { alertTemplateId });

            if (cascade) {
                // cascading is a system side effects, and so should be performed by the overlord
                List<Integer> alertDefinitions = getAlertDefinitionIdsByTemplateId(user, alertTemplateId);
                alertDefinitionManager.enableAlertDefinitions(subjectManager.getOverlord(), alertDefinitions
                    .toArray(new Integer[0]));
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void disableAlertTemplates(Subject user, Integer[] alertTemplateIds, boolean cascade) {
        for (Integer alertTemplateId : alertTemplateIds) {
            alertDefinitionManager.disableAlertDefinitions(user, new Integer[] { alertTemplateId });

            if (cascade) {
                // cascading is a system side effects, and so should be performed by the overlord
                List<Integer> alertDefinitions = getAlertDefinitionIdsByTemplateId(user, alertTemplateId);
                alertDefinitionManager.disableAlertDefinitions(subjectManager.getOverlord(), alertDefinitions
                    .toArray(new Integer[0]));
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public AlertDefinition updateAlertTemplate(Subject user, AlertDefinition alertTemplate, boolean cascade,
        boolean purgeInternals) throws InvalidAlertDefinitionException {
        AlertDefinition updatedTemplate = null;

        try {
            updatedTemplate = alertDefinitionManager.updateAlertDefinition(user, alertTemplate.getId(), alertTemplate,
                purgeInternals); // do not allow direct undeletes of an alert definition
        } catch (AlertDefinitionUpdateException adue) {
            /* jmarques (Oct 10, 2007)
             *
             * this should never happen, there is currently no way to update a deleted alert template via the JON UI
             */
            LOG.error("Attempt to update a deleted template " + alertTemplate.toSimpleString());
        }

        if (cascade) {
            Subject overlord = subjectManager.getOverlord();
            int definitionCount = 0;

            /*
             * update all of the definitions that were spawned from alert templates
             */
            List<Integer> alertDefinitions = getAlertDefinitionIdsByTemplateId(overlord, alertTemplate.getId());
            for (Integer alertDefinitionId : alertDefinitions) {
                try {
                    alertDefinitionManager.updateAlertDefinition(overlord, alertDefinitionId, alertTemplate,
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
                    LOG.error("Attempt to update a deleted template " + alertTemplate.toSimpleString());
                }

                /* 
                 * flush/clear after only 5 definitions (as opposed to a larger batch) because an alert
                 * definition is actual many objects: the definition, the condition set, the notification
                 * set, the alert dampening rules 
                 */
                if (++definitionCount % 5 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }

            /*
             * if the user deleted the alert definition spawned from a template, a cascade update will recreate it
             */
            List<Integer> resourceIds = getCommittedResourceIdsWithNoDefinitionFromThisTemplate(overlord, alertTemplate
                .getId(), alertTemplate.getResourceType().getId());
            try {
                for (Integer resourceId : resourceIds) {
                    updateAlertDefinitionsForResource(overlord, alertTemplate, resourceId);

                    /* 
                     * flush/clear after only 5 definitions (as opposed to a larger batch) because an alert
                     * definition is actual many objects: the definition, the condition set, the notification
                     * set, the alert dampening rules 
                     */
                    if (++definitionCount % 5 == 0) {
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
        }

        return updatedTemplate;
    }
}