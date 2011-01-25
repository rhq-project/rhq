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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDampeningEvent;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertDefinitionEvent;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.cloud.StatusManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * @author Joseph Marques
 */

@Stateless
public class AlertDefinitionManagerBean implements AlertDefinitionManagerLocal, AlertDefinitionManagerRemote {

    private static final Log LOG = LogFactory.getLog(AlertDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    @IgnoreDependency
    private AlertManagerLocal alertManager;

    @EJB
    private StatusManagerLocal agentStatusManager;

    private boolean checkViewPermission(Subject subject, AlertDefinition alertDefinition) {
        if (alertDefinition.getResourceType() != null) { // an alert template
            return authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS);
        } else if (alertDefinition.getResourceGroup() != null) { // a groupAlertDefinition
            return authorizationManager.canViewGroup(subject, alertDefinition.getResourceGroup().getId());
        } else { // an alert definition
            return authorizationManager.canViewResource(subject, alertDefinition.getResource().getId());
        }
    }

    private boolean checkPermission(Subject subject, AlertDefinition alertDefinition) {
        /*
         * system side-effects are primarily call-outs from the template manager to the definition manager, when the
         * template operation is being cascaded
         */
        if (authorizationManager.isOverlord(subject)) {
            return true;
        }

        if (alertDefinition.getResourceType() != null) { // an alert template
            return authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS);
        } else if (alertDefinition.getResourceGroup() != null) { // a groupAlertDefinition
            return authorizationManager.hasGroupPermission(subject, Permission.MANAGE_ALERTS, alertDefinition
                .getResourceGroup().getId());
        } else { // an alert definition
            return authorizationManager.hasResourcePermission(subject, Permission.MANAGE_ALERTS, alertDefinition
                .getResource().getId());
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinition> findAlertDefinitions(Subject subject, int resourceId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("ctime", PageOrdering.DESC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, AlertDefinition.QUERY_FIND_BY_RESOURCE);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, AlertDefinition.QUERY_FIND_BY_RESOURCE,
            pageControl);

        queryCount.setParameter("id", resourceId);
        query.setParameter("id", resourceId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<AlertDefinition> list = query.getResultList();

        return new PageList<AlertDefinition>(list, (int) totalCount, pageControl);
    }

    public AlertDefinition getAlertDefinitionById(Subject subject, int alertDefinitionId) {
        AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefinitionId);
        if (alertDefinition == null) {
            return null; // fail-fast to avoid downstream NPEs
        }

        if (checkViewPermission(subject, alertDefinition) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view alertDefinition[id=" + alertDefinitionId + "] for resource[id="
                + alertDefinition.getResource().getId() + "]");
        }

        alertDefinition.getConditions().size();
        // this is now lazy
        for (AlertCondition cond : alertDefinition.getConditions()) {
            if (cond.getMeasurementDefinition() != null) {
                cond.getMeasurementDefinition().getId();
            }
        }
        // DO NOT LOAD ALL ALERTS FOR A DEFINITION... This would be all alerts that have been fired
        //alertDefinition.getAlerts().size();
        for (AlertNotification notification : alertDefinition.getAlertNotifications()) {
            notification.getConfiguration().getProperties().size(); // eager load configuration and properties too
            if (notification.getExtraConfiguration() != null) {
                notification.getExtraConfiguration().getProperties().size();
            }
        }

        return alertDefinition;
    }

    @SuppressWarnings("unchecked")
    public List<IntegerOptionItem> findAlertDefinitionOptionItemsForResource(Subject subject, int resourceId) {
        PageControl pageControl = PageControl.getUnlimitedInstance();
        pageControl.initDefaultOrderingField("ad.name", PageOrdering.ASC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            AlertDefinition.QUERY_FIND_OPTION_ITEMS_BY_RESOURCE, pageControl);

        query.setParameter("resourceId", resourceId);
        List<IntegerOptionItem> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    public List<IntegerOptionItem> findAlertDefinitionOptionItemsForGroup(Subject subject, int groupId) {
        PageControl pageControl = PageControl.getUnlimitedInstance();
        pageControl.initDefaultOrderingField("ad.name", PageOrdering.ASC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            AlertDefinition.QUERY_FIND_OPTION_ITEMS_BY_GROUP, pageControl);

        query.setParameter("groupId", groupId);
        List<IntegerOptionItem> results = query.getResultList();

        return results;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int createAlertDefinition(Subject subject, AlertDefinition alertDefinition, Integer resourceId)
        throws InvalidAlertDefinitionException {
        checkAlertDefinition(alertDefinition, resourceId);

        // if this is an alert definition, set up the link to a resource
        if (resourceId != null) {
            // don't attach an alertTemplate or groupAlertDefinition to any particular resource
            // they should have already been attached to the resourceType or resourceGroup by the caller

            //Resource resource = LookupUtil.getResourceManager().getResourceById(user, resourceId);
            // use proxy trick to subvert having to load the entire resource into memory
            alertDefinition.setResource(new Resource(resourceId));
        }

        // after the resource is set up (in the case of non-templates), we can use the checkPermission on it
        if (checkPermission(subject, alertDefinition) == false) {
            if (alertDefinition.getResourceType() != null) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to create alert templates for type ["
                    + alertDefinition.getResourceType() + "]");
            } else if (alertDefinition.getResourceGroup() != null) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to create alert definitions for group ["
                    + alertDefinition.getResourceGroup() + "]");
            } else {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to create alert definitions for resource ["
                    + alertDefinition.getResource() + "]");
            }
        }

        /*
         * performance optimization for the common case of single-condition alerts; it's easier for the
         * out-of-band process to check whether or not ANY conditions are true rather than ALL of them
         */
        if (alertDefinition.getConditions().size() == 1) {
            alertDefinition.setConditionExpression(BooleanExpression.ANY);
        }

        fixRecoveryId(alertDefinition);

        entityManager.persist(alertDefinition);

        boolean addToCache = false;
        // don't notify on an alert template, only for those that get attached to a resource
        // Only add to the cache if the alert definition was created as active
        if ((resourceId != null) && alertDefinition.getEnabled()) {
            // if this is a recovery alert
            if (alertDefinition.getRecoveryId() != 0) {
                // only add to the cache if the to-be-recovered definition is disabled, and thus needs recovering
                // use entityManager direct to bypass security checks, we already know this user is authorized
                AlertDefinition toBeRecoveredDefinition = entityManager.find(AlertDefinition.class, alertDefinition
                    .getRecoveryId());
                if (toBeRecoveredDefinition.getEnabled() == false) {
                    addToCache = true;
                }
            } else {
                addToCache = true;
            }
        }

        if (addToCache) {
            notifyAlertConditionCacheManager(subject, "createAlertDefinition", alertDefinition,
                AlertDefinitionEvent.CREATED);
        }

        return alertDefinition.getId();
    }

    private void fixRecoveryId(AlertDefinition definition) {
        try {
            if (definition.getParentId() != 0 && definition.getRecoveryId() != 0) {
                // so we need to set the resource-level recovery id properly
                String findCorrectRecoveryId = "" //
                    + " SELECT toBeRecovered.id " //
                    + "   FROM AlertDefinition toBeRecovered " //
                    + "  WHERE toBeRecovered.resource.id = :resourceId " //
                    + "    AND toBeRecovered.parentId = :parentId ";
                Query fixRecoveryIdQuery = entityManager.createQuery(findCorrectRecoveryId);
                fixRecoveryIdQuery.setParameter("resourceId", definition.getResource().getId());
                // definition.recoveryId current points at the toBeRecovered template, we want the definition
                fixRecoveryIdQuery.setParameter("parentId", definition.getRecoveryId()); // wrong one to be replaced
                Integer correctRecoveryId = (Integer) fixRecoveryIdQuery.getSingleResult();
                definition.setRecoveryId(correctRecoveryId);
            } else if (definition.getGroupAlertDefinition() != null && definition.getRecoveryId() != 0) {
                // so we need to set the resource-level recovery id properly
                String findCorrectRecoveryId = "" //
                    + " SELECT toBeRecovered.id " //
                    + "   FROM AlertDefinition toBeRecovered " //
                    + "  WHERE toBeRecovered.resource.id = :resourceId " //
                    + "    AND toBeRecovered.groupAlertDefinition.id = :groupAlertDefinitionId ";
                Query fixRecoveryIdQuery = entityManager.createQuery(findCorrectRecoveryId);
                fixRecoveryIdQuery.setParameter("resourceId", definition.getResource().getId());
                // definition.recoveryId current points at the toBeRecovered template, we want the definition
                fixRecoveryIdQuery.setParameter("groupAlertDefinitionId", definition.getRecoveryId()); // wrong one to be replaced
                Integer correctRecoveryId = (Integer) fixRecoveryIdQuery.getSingleResult();
                definition.setRecoveryId(correctRecoveryId);
            }
        } catch (NoResultException nre) {
            // expected when the recovery ids have already been fixed
        }
    }

    public int removeAlertDefinitions(Subject subject, int[] alertDefinitionIds) {
        int modifiedCount = 0;
        boolean isResourceLevel = false;

        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(subject, alertDefinition)) {
                alertDefinition.setDeleted(true);
                modifiedCount++;

                // alertTemplates and groupAlertDefinitions do not need to update the cache
                isResourceLevel = (null != alertDefinition.getResource());
                if (isResourceLevel) {
                    notifyAlertConditionCacheManager(subject, "removeAlertDefinitions", alertDefinition,
                        AlertDefinitionEvent.DELETED);
                }
                if (alertDefinition.getResourceGroup() != null) {
                    alertDefinition.setResourceGroup(null); // break bonds so corresponding ResourceGroup can be purged
                }
            }
        }

        return modifiedCount;
    }

    public int enableAlertDefinitions(Subject subject, int[] alertDefinitionIds) {
        int modifiedCount = 0;
        boolean isResourceLevel = false;
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(subject, alertDefinition)) {
                // only enable an alert if it's not currently enabled
                if (alertDefinition.getEnabled() == false) {
                    alertDefinition.setEnabled(true);
                    modifiedCount++;

                    // alertTemplates and groupAlertDefinitions do not need to update the cache
                    isResourceLevel = (null != alertDefinition.getResource());
                    if (isResourceLevel) {
                        notifyAlertConditionCacheManager(subject, "enableAlertDefinitions", alertDefinition,
                            AlertDefinitionEvent.ENABLED);
                    }
                }
            }
        }

        return modifiedCount;
    }

    @SuppressWarnings("unchecked")
    public boolean isEnabled(Integer definitionId) {
        Query enabledQuery = entityManager.createNamedQuery(AlertDefinition.QUERY_IS_ENABLED);
        enabledQuery.setParameter("alertDefinitionId", definitionId);
        List<Integer> resultIds = enabledQuery.getResultList();
        return (resultIds.size() == 1);
    }

    @SuppressWarnings("unchecked")
    public boolean isTemplate(Integer definitionId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_IS_TEMPLATE);
        query.setParameter("alertDefinitionId", definitionId);
        List<Integer> resultIds = query.getResultList();
        return (resultIds.size() == 1);
    }

    @SuppressWarnings("unchecked")
    public boolean isGroupAlertDefinition(Integer definitionId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_IS_GROUP_ALERT_DEFINITION);
        query.setParameter("alertDefinitionId", definitionId);
        List<Integer> resultIds = query.getResultList();
        return (resultIds.size() == 1);
    }

    @SuppressWarnings("unchecked")
    public boolean isResourceAlertDefinition(Integer definitionId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_IS_RESOURCE_ALERT_DEFINITION);
        query.setParameter("alertDefinitionId", definitionId);
        List<Integer> resultIds = query.getResultList();
        return (resultIds.size() == 1);
    }

    public int disableAlertDefinitions(Subject subject, int[] alertDefinitionIds) {
        int modifiedCount = 0;
        boolean isResourceLevel;
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(subject, alertDefinition)) {
                // only disable an alert if it's currently enabled
                if (alertDefinition.getEnabled() == true) {
                    alertDefinition.setEnabled(false);
                    modifiedCount++;

                    // alertTemplates and groupAlertDefinitions do not need to update the cache
                    isResourceLevel = (null != alertDefinition.getResource());
                    if (isResourceLevel) {
                        notifyAlertConditionCacheManager(subject, "disableAlertDefinitions", alertDefinition,
                            AlertDefinitionEvent.DISABLED);
                    }
                }
            }
        }

        return modifiedCount;
    }

    public void copyAlertDefinitions(Subject subject, Integer[] alertDefinitionIds) {
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(subject, alertDefinition)) {
                AlertDefinition newAlertDefinition = new AlertDefinition(alertDefinition);
                newAlertDefinition.setEnabled(false);

                // this is a "true" copy, so update parentId, resource, and resourceType, group, groupAlertDefinition
                newAlertDefinition.setParentId(alertDefinition.getParentId());
                newAlertDefinition.setResource(alertDefinition.getResource());
                newAlertDefinition.setResourceType(alertDefinition.getResourceType());
                newAlertDefinition.setResourceGroup(alertDefinition.getResourceGroup());
                newAlertDefinition.setGroupAlertDefinition(alertDefinition.getGroupAlertDefinition());

                entityManager.persist(newAlertDefinition);

                notifyAlertConditionCacheManager(subject, "copyAlertDefinitions", alertDefinition,
                    AlertDefinitionEvent.CREATED);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<AlertDefinition> findAllRecoveryDefinitionsById(Subject subject, Integer alertDefinitionId) {
        if (authorizationManager.isOverlord(subject) == false) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission to call "
                + "getAllRecoveryDefinitionsById; only the overlord has that right");
        }

        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_ALL_BY_RECOVERY_DEFINITION_ID);
        query.setParameter("recoveryDefinitionId", alertDefinitionId);

        List<AlertDefinition> list = query.getResultList();
        return list;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AlertDefinition updateAlertDefinition(Subject subject, int alertDefinitionId,
        AlertDefinition alertDefinition, boolean purgeInternals) throws InvalidAlertDefinitionException,
        AlertDefinitionUpdateException {
        if (purgeInternals) {
            alertDefinitionManager.purgeInternals(alertDefinitionId);
        }

        /*
         * Method for catching ENABLE / DISABLE changes will use switch logic off of the delta instead of calling out to
         * the enable/disable functions
         */
        AlertDefinition oldAlertDefinition = entityManager.find(AlertDefinition.class, alertDefinitionId);

        if (checkPermission(subject, oldAlertDefinition) == false) {
            if (oldAlertDefinition.getResourceType() != null) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to modify alert templates for type ["
                    + oldAlertDefinition.getResourceType() + "]");
            } else if (oldAlertDefinition.getResourceGroup() != null) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to modify alert definitions for group ["
                    + oldAlertDefinition.getResourceGroup() + "]");
            } else {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to modify alert definitions for resource ["
                    + oldAlertDefinition.getResource() + "]");
            }
        }

        /*
         * only need to check the validity of the new alert definition if the authz checks pass *and* the old definition
         * is not currently deleted
         */
        boolean isResourceLevel = (oldAlertDefinition.getResource() != null);
        checkAlertDefinition(alertDefinition, isResourceLevel ? oldAlertDefinition.getResource().getId() : null);

        /*
         * Should not be able to update an alert definition if the old alert definition is in an invalid state
         */
        if (oldAlertDefinition.getDeleted()) {
            throw new AlertDefinitionUpdateException("Can not update deleted " + oldAlertDefinition.toSimpleString());
        }

        AlertDefinitionUpdateType updateType = AlertDefinitionUpdateType.get(oldAlertDefinition, alertDefinition);

        if (isResourceLevel
            && ((updateType == AlertDefinitionUpdateType.JUST_DISABLED) || (updateType == AlertDefinitionUpdateType.STILL_ENABLED))) {
            /*
             * if you were JUST_DISABLED or STILL_ENABLED, you are coming from the ENABLED state, which means you need
             * to be removed from the cache as the first half of this update
             */
            LOG.debug("Updating AlertConditionCacheManager with AlertDefinition[ id=" + oldAlertDefinition.getId()
                + " ]...DELETING");
            for (AlertCondition nextCondition : oldAlertDefinition.getConditions()) {
                LOG.debug("OldAlertCondition[ id=" + nextCondition.getId() + " ]");
            }
            notifyAlertConditionCacheManager(subject, "updateAlertDefinition", oldAlertDefinition,
                AlertDefinitionEvent.DELETED);
        }

        /*
         * performance optimization for the common case of single-condition alerts; it's easier for the
         * out-of-band process to check whether or not ANY conditions are true rather than ALL of them
         */
        if (alertDefinition.getConditions().size() == 1) {
            alertDefinition.setConditionExpression(BooleanExpression.ANY);
        }

        oldAlertDefinition.update(alertDefinition);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating: " + oldAlertDefinition);
            for (AlertCondition nextCondition : oldAlertDefinition.getConditions()) {
                LOG.debug("Condition: " + nextCondition);
            }
            for (AlertNotification nextNotification : oldAlertDefinition.getAlertNotifications()) {
                LOG.debug("Notification: " + nextNotification);
                LOG.debug("Notification-Configuration: " + nextNotification.getConfiguration().toString(true));
                if (nextNotification.getExtraConfiguration() != null) {
                    LOG.debug("Notification-Extra-Configuration: "
                        + nextNotification.getExtraConfiguration().toString(true));
                }
            }
        }

        fixRecoveryId(oldAlertDefinition);
        AlertDefinition newAlertDefinition = entityManager.merge(oldAlertDefinition);

        if (isResourceLevel
            && ((updateType == AlertDefinitionUpdateType.JUST_ENABLED) || (updateType == AlertDefinitionUpdateType.STILL_ENABLED))) {
            /*
             * if you were JUST_ENABLED or STILL_ENABLED, you are moving to the ENABLED state, which means you need to
             * be added to the cache as the last half of this update
             */

            boolean addToCache = false;
            // if this was a recovery alert, or was recently turned into one
            if (newAlertDefinition.getRecoveryId() != 0) {
                // only add to the cache if the to-be-recovered definition is disabled, and thus needs recovering
                AlertDefinition toBeRecoveredDefinition = getAlertDefinitionById(subject, newAlertDefinition
                    .getRecoveryId());
                if (toBeRecoveredDefinition.getEnabled() == false) {
                    addToCache = true;
                }
            } else {
                addToCache = true;
            }

            if (addToCache) {
                LOG.debug("Updating AlertConditionCacheManager with AlertDefinition[ id=" + newAlertDefinition.getId()
                    + " ]...CREATING");
                for (AlertCondition nextCondition : newAlertDefinition.getConditions()) {
                    LOG.debug("NewAlertCondition[ id=" + nextCondition.getId() + " ]");
                }
                notifyAlertConditionCacheManager(subject, "updateAlertDefinition", newAlertDefinition,
                    AlertDefinitionEvent.CREATED);
            }
        }

        /*
         * note, nothing is done to the cache in the STILL_DISABLED case because nothing should've been in the cache to
         * begin with, and nothing needs to be added to the cache as a result
         */

        return newAlertDefinition;
    }

    /*
     * A helper enum to make for cleaner logic in updateAlertDefinition( Subject, AlertDefinition, boolean )
     */
    enum AlertDefinitionUpdateType {
        JUST_ENABLED, JUST_DISABLED, STILL_ENABLED, STILL_DISABLED;

        public static AlertDefinitionUpdateType get(AlertDefinition oldDefinition, AlertDefinition newDefinition) {
            if ((oldDefinition.getEnabled() == false) && (newDefinition.getEnabled() == true)) {
                return AlertDefinitionUpdateType.JUST_ENABLED;
            } else if ((oldDefinition.getEnabled() == true) && (newDefinition.getEnabled() == false)) {
                return AlertDefinitionUpdateType.JUST_DISABLED;
            } else if ((oldDefinition.getEnabled() == true) && (newDefinition.getEnabled() == true)) {
                return AlertDefinitionUpdateType.STILL_ENABLED;
            } else {
                return AlertDefinitionUpdateType.STILL_DISABLED;
            }
        }
    }

    private void checkAlertDefinition(AlertDefinition alertDefinition, Integer resourceId)
        throws InvalidAlertDefinitionException {
        for (AlertCondition alertCondition : alertDefinition.getConditions()) {
            AlertConditionCategory alertConditionCategory = alertCondition.getCategory();
            if (alertConditionCategory == AlertConditionCategory.ALERT) {
                throw new InvalidAlertDefinitionException(
                    "AlertDefinitionManager does not yet support condition category: " + alertConditionCategory);
            }
            if (alertConditionCategory == AlertConditionCategory.BASELINE) {

                MeasurementDefinition def = alertCondition.getMeasurementDefinition();
                NumericType numType = def.getNumericType();
                if (numType == null) {
                    def = entityManager.getReference(MeasurementDefinition.class, def.getId());
                    numType = def.getNumericType();
                }
                if (numType != NumericType.DYNAMIC) {
                    throw new InvalidAlertDefinitionException("Invalid Condition: '" + def.getDisplayName()
                        + "' is a trending metric, and thus will never have baselines calculated for it.");
                }
            }
        }

        return;
    }

    private void notifyAlertConditionCacheManager(Subject subject, String methodName, AlertDefinition alertDefinition,
        AlertDefinitionEvent alertDefinitionEvent) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Invoking... " + methodName + " with AlertDefinitionEvent[" + alertDefinitionEvent + "]");
        }
        if (alertDefinitionEvent == AlertDefinitionEvent.CREATED) {
            if (alertDefinition.getResource() != null) {
                int resourceId = alertDefinition.getResource().getId();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invoking... agentStatusManager.updateByResource(" + resourceId + ")");
                }
                agentStatusManager.updateByResource(subject, resourceId);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("notifyAlertConditionCacheManager skipping alert template or group alert definition");
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invoking... agentStatusManager.updateByAlertDefinition(" + alertDefinition.getId() + ")");
            }
            agentStatusManager.updateByAlertDefinition(subject, alertDefinition.getId());
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void purgeInternals(int alertDefinitionId) {
        try {
            Query alertDampeningEventPurgeQuery = entityManager
                .createNamedQuery(AlertDampeningEvent.QUERY_DELETE_BY_ALERT_DEFINITION_ID);
            Query unmatchedAlertConditionLogPurgeQuery = entityManager
                .createNamedQuery(AlertConditionLog.QUERY_DELETE_UNMATCHED_BY_ALERT_DEFINITION_ID);

            alertDampeningEventPurgeQuery.setParameter("alertDefinitionId", alertDefinitionId);
            unmatchedAlertConditionLogPurgeQuery.setParameter("alertDefinitionId", alertDefinitionId);

            int alertDampeningEventPurgeCount = alertDampeningEventPurgeQuery.executeUpdate();
            int unmatchedAlertConditionLogPurgeCount = unmatchedAlertConditionLogPurgeQuery.executeUpdate();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Update to AlertDefinition[id=" + alertDefinitionId
                    + " caused a purge of internal, dampening constructs.");
                if (alertDampeningEventPurgeCount > 0) {
                    LOG.debug("Removed " + alertDampeningEventPurgeCount + " AlertDampeningEvent"
                        + (alertDampeningEventPurgeCount == 1 ? "" : "s"));
                }
                if (unmatchedAlertConditionLogPurgeCount > 0) {
                    LOG.debug("Removed " + unmatchedAlertConditionLogPurgeCount + " unmatched AlertConditionLog"
                        + (unmatchedAlertConditionLogPurgeCount == 1 ? "" : "s"));
                }
            }
        } catch (Throwable t) {
            LOG.debug("Could not purge internal alerting constructs for: " + alertDefinitionId, t);
        }
    }

    @SuppressWarnings("unchecked")
    public int purgeUnusedAlertDefinitions() {
        Query purgeQuery = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_UNUSED_DEFINITION_IDS);
        List<Integer> resultIds = purgeQuery.getResultList();

        int removed = 0;
        for (int unusedDefinitionId : resultIds) {
            AlertDefinition unusedDefinition = entityManager.find(AlertDefinition.class, unusedDefinitionId);
            if (unusedDefinition != null) {
                entityManager.remove(unusedDefinition);
                removed++;
            } else {
                LOG.warn("Could not find alertDefinition[id=" + unusedDefinitionId + "] for purge");
            }
        }

        return removed;
    }

    public AlertDefinition getAlertDefinition(Subject subject, int alertDefinitionId) {
        return getAlertDefinitionById(subject, alertDefinitionId);
    }

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinition> findAlertDefinitionsByCriteria(Subject subject, AlertDefinitionCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE, subject
                .getId());
        }

        CriteriaQueryRunner<AlertDefinition> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    public String[] getAlertNotificationConfigurationPreview(Subject sessionSubject, AlertNotification[] notifications) {
        if (notifications == null || notifications.length == 0) {
            return new String[0];
        }

        AlertSenderPluginManager alertPluginManager = alertManager.getAlertPluginManager();

        String[] previews = new String[notifications.length];
        int i = 0;
        for (AlertNotification notif : notifications) {
            AlertSender<?> sender = alertPluginManager.getAlertSenderForNotification(notif);
            if (sender != null) {
                previews[i++] = sender.previewConfiguration();
            } else {
                previews[i++] = "n/a (unknown sender)";
            }
        }

        return previews;
    }

}