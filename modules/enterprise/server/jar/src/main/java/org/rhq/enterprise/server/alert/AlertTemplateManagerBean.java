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
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility.SerializationType;

/**
 * @author Joseph Marques
 */
@Stateless
public class AlertTemplateManagerBean implements AlertTemplateManagerLocal {

    private static final Log LOG = LogFactory.getLog(AlertTemplateManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
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
    private List<Integer> getChildrenAlertDefinitionIds(Subject user, int alertTemplateId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_BY_ALERT_TEMPLATE_ID);
        query.setParameter("alertTemplateId", alertTemplateId);

        List<Integer> list = query.getResultList();
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getCommittedResourceIdsNeedingTemplateApplication(Subject user, int alertTemplateId,
        int resourceTypeId) {
        Query query = entityManager
            .createNamedQuery(AlertDefinition.QUERY_FIND_RESOURCE_IDS_NEEDING_TEMPLATE_APPLICATION);
        query.setParameter("alertTemplateId", alertTemplateId);
        query.setParameter("resourceTypeId", resourceTypeId);
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        List<Integer> list = query.getResultList();
        return list;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int createAlertTemplate(Subject user, AlertDefinition alertTemplate, Integer resourceTypeId)
        throws InvalidAlertDefinitionException, ResourceTypeNotFoundException, AlertDefinitionCreationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("createAlertTemplate: " + alertTemplate);
        }

        ResourceType type = resourceTypeManager.getResourceTypeById(user, resourceTypeId);

        alertTemplate.setResourceType(type); // mark this as an alert "template" definition

        AlertDefinition persistedAlertTemplate = null;
        try {
            persistedAlertTemplate = alertDefinitionManager.createAlertDefinitionInNewTransaction(user, alertTemplate,
                null, true);
        } catch (Throwable t) {
            throw new AlertDefinitionCreationException("Could not create alertTemplate for " + type + " with data "
                + alertTemplate.toSimpleString(), t);
        }

        Throwable firstThrowable = null;

        List<Integer> resourceIdsForType = getCommittedResourceIdsNeedingTemplateApplication(user,
            persistedAlertTemplate.getId(), resourceTypeId);
        List<Integer> resourceIdsInError = new ArrayList<Integer>();
        for (Integer resourceId : resourceIdsForType) {
            try {
                // construct the child
                AlertDefinition childAlertDefinition = new AlertDefinition(persistedAlertTemplate);
                childAlertDefinition.setParentId(alertTemplate.getId());

                // persist the child as a dependent alert definition
                alertDefinitionManager.createDependentAlertDefinition(user, childAlertDefinition, resourceId);
            } catch (Throwable t) {
                // continue on error, create as many as possible
                if (firstThrowable == null) {
                    firstThrowable = t;
                }
                resourceIdsInError.add(resourceId);
            }
        }
        if (firstThrowable != null) {
            throw new AlertDefinitionCreationException("Could not create child alert definition for Resources "
                + resourceIdsInError + " with template" + alertTemplate.toSimpleString(), firstThrowable);
        }

        return persistedAlertTemplate.getId();
    }

    @SuppressWarnings("unchecked")
    public void updateAlertDefinitionsForResource(Subject user, Integer resourceId)
        throws AlertDefinitionCreationException, InvalidAlertDefinitionException {
        if (authorizationManager.isOverlord(user) == false) {
            throw new AlertDefinitionCreationException(
                "Updating the alert definitions for a resource is an implicit system operation; "
                    + "It can only be performed by the overlord");
        }

        // get list of AlertTemplates that should be, but haven't already been, applied to this resource
        Query query = entityManager.createQuery("" //
            + " SELECT template " //
            + "   FROM AlertDefinition template, Resource res " //
            + "  WHERE template.resourceType.id = res.resourceType.id " //
            + "    AND res.id = :resourceId " //
            + "    AND template.deleted = false " //
            + "    AND template.id NOT IN ( SELECT ad.id " //
            + "                               FROM AlertDefinition ad " //
            + "                              WHERE ad.resource.id = :resourceId " //
            + "                                AND ad.deleted = false ) " //
            + " ORDER BY template.recoveryId ASC "); // process recovered defs first, so recovery defs can reference them
        query.setParameter("resourceId", resourceId);
        List<AlertDefinition> unappliedTemplates = query.getResultList();

        for (AlertDefinition template : unappliedTemplates) {
            // construct the child, note that the proxy will pull in lazy data during the copy
            AlertDefinition childAlertDefinition = new AlertDefinition(template);
            childAlertDefinition.setParentId(template.getId());

            // persist the child, user is known to be overlord at this point for this system side-effect
            try {
                // convert the childAlertDef to a pojo, remove all proxies left over from the copy. We can't
                // pass entities across a Tx boundary and the call to create the alert def is executed in a new trans.
                HibernateDetachUtility
                    .nullOutUninitializedFields(childAlertDefinition, SerializationType.SERIALIZATION);
                alertDefinitionManager.createAlertDefinitionInNewTransaction(user, childAlertDefinition, resourceId,
                    false);
            } catch (Throwable t) {
                throw new AlertDefinitionCreationException("Failed to create child AlertDefinition for Resource[id="
                    + resourceId + "] with template " + template.toSimpleString(), t);
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void removeAlertTemplates(Subject user, Integer[] alertTemplateIds) {
        if (null == alertTemplateIds || alertTemplateIds.length == 0) {
            return;
        }

        for (Integer alertTemplateId : alertTemplateIds) {
            AlertDefinition template = entityManager.find(AlertDefinition.class, alertTemplateId);
            if (null == template || template.getDeleted()) {
                continue;
            }

            // remove the template
            template.setDeleted(true);

            // remove the child resource-level defs
            Subject overlord = subjectManager.getOverlord();
            List<Integer> childDefinitionIds = getChildrenAlertDefinitionIds(user, alertTemplateId);
            if (childDefinitionIds.isEmpty()) {
                continue;
            }
            alertDefinitionManager.removeResourceAlertDefinitions(overlord,
                ArrayUtils.unwrapCollection(childDefinitionIds));

            // finally, detach protected alert defs
            Query query = entityManager
                .createNamedQuery(AlertDefinition.QUERY_UPDATE_DETACH_PROTECTED_BY_ALERT_TEMPLATE_ID);
            query.setParameter("alertTemplateId", alertTemplateId);
            int numDetached = query.executeUpdate();
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void enableAlertTemplates(Subject subject, Integer[] alertTemplateIds) {
        if (null == alertTemplateIds || alertTemplateIds.length == 0) {
            return;
        }

        for (Integer alertTemplateId : alertTemplateIds) {
            AlertDefinition template = entityManager.find(AlertDefinition.class, alertTemplateId);
            if (null == template || template.getEnabled() || template.getDeleted()) {
                continue;
            }

            // enable the template
            template.setEnabled(true);

            // enable the child resource-level defs
            Subject overlord = subjectManager.getOverlord();
            List<Integer> childDefinitionIds = getChildrenAlertDefinitionIds(overlord, alertTemplateId);
            if (childDefinitionIds.isEmpty()) {
                continue;
            }
            alertDefinitionManager.enableResourceAlertDefinitions(overlord,
                ArrayUtils.unwrapCollection(childDefinitionIds));
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void disableAlertTemplates(Subject subject, Integer[] alertTemplateIds) {
        if (null == alertTemplateIds || alertTemplateIds.length == 0) {
            return;
        }

        for (Integer alertTemplateId : alertTemplateIds) {
            AlertDefinition template = entityManager.find(AlertDefinition.class, alertTemplateId);
            if (null == template || !template.getEnabled() || template.getDeleted()) {
                continue;
            }

            // disable the template
            template.setEnabled(false);

            // enable the child resource-level defs
            Subject overlord = subjectManager.getOverlord();
            List<Integer> childDefinitionIds = getChildrenAlertDefinitionIds(overlord, alertTemplateId);
            if (childDefinitionIds.isEmpty()) {
                continue;
            }
            alertDefinitionManager.disableAlertDefinitions(overlord, ArrayUtils.unwrapCollection(childDefinitionIds));
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AlertDefinition updateAlertTemplate(Subject user, AlertDefinition alertTemplate, boolean resetMatching)
        throws InvalidAlertDefinitionException, AlertDefinitionUpdateException, AlertNotificationValidationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("updateAlertTemplate: " + alertTemplate);
        }

        // first update the actual alert template
        AlertDefinition updated = null;
        try {
            updated = alertDefinitionManager.updateAlertDefinition(user, alertTemplate.getId(), alertTemplate,
                resetMatching); // do not allow direct undeletes of an alert definition
        } catch (Throwable t) {
            throw new AlertDefinitionUpdateException("Failed to update an AlertTemplate "
                + alertTemplate.toSimpleString(), t);
        }

        // overlord will be used for all system-side effects as a result of updating this alert template
        Subject overlord = subjectManager.getOverlord();
        Throwable firstThrowable = null;

        // update all of the definitions that were spawned from alert templates
        List<Integer> alertDefinitions = getChildrenAlertDefinitionIds(overlord, alertTemplate.getId());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Need to update the following children alert definition ids: " + alertDefinitions);
        }
        List<Integer> alertDefinitionIdsInError = new ArrayList<Integer>();
        for (Integer alertDefinitionId : alertDefinitions) {
            try {
                alertDefinitionManager.updateDependentAlertDefinition(user, alertDefinitionId, updated, resetMatching);
            } catch (Throwable t) {
                // continue on error, update as many as possible
                if (firstThrowable == null) {
                    firstThrowable = t;
                }
                alertDefinitionIdsInError.add(alertDefinitionId);
            }
        }

        // if the user deleted the alert definition spawned from a template, a cascade update will recreate it
        List<Integer> resourceIds = getCommittedResourceIdsNeedingTemplateApplication(overlord, alertTemplate.getId(),
            getResourceTypeIdForAlertTemplateId(alertTemplate.getId()));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Need to re-create alert definitions for the following resource ids: " + resourceIds);
        }
        List<Integer> resourceIdsInError = new ArrayList<Integer>();
        for (Integer resourceId : resourceIds) {
            try {
                // construct the child
                AlertDefinition childAlertDefinition = new AlertDefinition(updated);
                childAlertDefinition.setParentId(alertTemplate.getId());

                // persist the child
                alertDefinitionManager.createAlertDefinitionInNewTransaction(overlord, childAlertDefinition,
                    resourceId, false);
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
                error.append("Failed to update child AlertDefinitions " + alertDefinitionIdsInError + "; ");
            }
            if (resourceIdsInError.size() != 0) {
                error.append("Failed to re-create child AlertDefinition for Resources " + resourceIdsInError + "; ");
            }
            throw new AlertDefinitionUpdateException(error.toString(), firstThrowable);
        }

        return updated;
    }

    private int getResourceTypeIdForAlertTemplateId(int alertTemplateId) {
        Query query = entityManager.createQuery("" //
            + "SELECT template.resourceType.id " //
            + "  FROM AlertDefinition template " //
            + " WHERE template.id = :alertTemplateId");
        query.setParameter("alertTemplateId", alertTemplateId);
        int typeId = ((Number) query.getSingleResult()).intValue();
        return typeId;
    }
}