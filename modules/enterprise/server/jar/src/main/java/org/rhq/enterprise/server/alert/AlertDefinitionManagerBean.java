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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDampeningEvent;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.domain.util.QueryGenerator;
import org.rhq.core.domain.util.QueryGenerator.AuthorizationTokenType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertDefinitionEvent;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.cloud.StatusManagerLocal;
import org.rhq.enterprise.server.exception.FetchException;

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
    private StatusManagerLocal agentStatusManager;

    private boolean checkViewPermission(Subject subject, AlertDefinition alertDefinition) {
        if (alertDefinition.getResourceType() != null) // an alert template
        {
            return authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_INVENTORY);
        } else // an alert definition
        {
            return authorizationManager.hasResourcePermission(subject, Permission.MANAGE_ALERTS, alertDefinition
                .getResource().getId());
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

        if (alertDefinition.getResourceType() != null) // an alert template
        {
            return authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_INVENTORY);
        } else // an alert definition
        {
            return authorizationManager.hasResourcePermission(subject, Permission.MANAGE_ALERTS, alertDefinition
                .getResource().getId());
        }
    }

    @SuppressWarnings("unchecked")
    public List<AlertDefinition> findAllAlertDefinitionsWithConditions(int agentId, Subject user) {
        if (authorizationManager.isOverlord(user) == false) {
            throw new PermissionException("User [" + user.getName() + "] does not have permission to call "
                + "getAllAlertDefinitionsWithConditions; only the overlord has that right");
        }

        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_ALL_WITH_CONDITIONS);
        query.setParameter("agentId", agentId);
        List<AlertDefinition> list = query.getResultList();

        return list;
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
        if (checkViewPermission(subject, alertDefinition) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view alertDefinition[id=" + alertDefinitionId + "] for resource[id="
                + alertDefinition.getResource().getId() + "]");
        }

        if (alertDefinition != null) {
            // avoid NPEs if the caller passed an invalid id
            alertDefinition.getConditions().size();
            // this is now lazy
            for (AlertCondition cond : alertDefinition.getConditions()) {
                if (cond.getMeasurementDefinition() != null) {
                    cond.getMeasurementDefinition().getId();
                }
            }
            // DO NOT LOAD ALL ALERTS FOR A DEFINITION... This would be all alerts that have been fired
            //alertDefinition.getAlerts().size();
            alertDefinition.getAlertNotifications().size();
        }

        return alertDefinition;
    }

    @SuppressWarnings("unchecked")
    public List<IntegerOptionItem> findAlertDefinitionOptionItems(Subject subject, int resourceId) {
        PageControl pageControl = PageControl.getUnlimitedInstance();
        pageControl.initDefaultOrderingField("ad.name", PageOrdering.ASC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            AlertDefinition.QUERY_FIND_OPTION_ITEMS_BY_RESOURCE);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            AlertDefinition.QUERY_FIND_OPTION_ITEMS_BY_RESOURCE, pageControl);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<IntegerOptionItem> list = query.getResultList();

        return new PageList<IntegerOptionItem>(list, (int) totalCount, pageControl);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int createAlertDefinition(Subject subject, AlertDefinition alertDefinition, Integer resourceId)
        throws InvalidAlertDefinitionException {
        checkAlertDefinition(alertDefinition, resourceId);

        // if this is an alert definition, set up the link to a resource
        if (resourceId != null) {
            // don't attach an alert template to any particular resource
            // the template should have already been attached to the resourceType by the template manager

            //Resource resource = LookupUtil.getResourceManager().getResourceById(user, resourceId);
            // use proxy trick to subvert having to load the entire resource into memory
            alertDefinition.setResource(new Resource(resourceId));
        }

        // after the resource is set up (in the case of non-templates), we can use the checkPermission on it
        if (checkPermission(subject, alertDefinition) == false) {
            if (resourceId != null) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to create alert definitions" + "for resource ["
                    + alertDefinition.getResource() + "]");
            } else {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to create alert templates");
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
            notifyAlertConditionCacheManager("createAlertDefinition", alertDefinition.getId(),
                AlertDefinitionEvent.CREATED);
        }

        return alertDefinition.getId();
    }

    private void fixRecoveryId(AlertDefinition definition) {
        // this was a recovery alert created from a template
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
        }
    }

    public int removeAlertDefinitions(Subject subject, Integer[] alertDefinitionIds) {
        int modifiedCount = 0;
        boolean isAlertTemplate = false;

        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(subject, alertDefinition)) {
                alertDefinition.setDeleted(true);
                modifiedCount++;

                // There is no need to update the cache if this is removal of an alert template condition
                // because it is not associated with any resource/agent.
                isAlertTemplate = (null != alertDefinition.getResourceType());

                if (!isAlertTemplate) {
                    notifyAlertConditionCacheManager("removeAlertDefinitions", alertDefinition.getId(),
                        AlertDefinitionEvent.DELETED);
                }
            }
        }

        return modifiedCount;
    }

    public int enableAlertDefinitions(Subject subject, Integer[] alertDefinitionIds) {
        int modifiedCount = 0;
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(subject, alertDefinition)) {
                // only enable an alert if it's not currently enabled
                if (alertDefinition.getEnabled() == false) {
                    alertDefinition.setEnabled(true);
                    modifiedCount++;

                    // thus, add it to the cache since it (shouldn't) already exist
                    notifyAlertConditionCacheManager("enableAlertDefinitions", alertDefinition.getId(),
                        AlertDefinitionEvent.ENABLED);
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
        Query enabledQuery = entityManager.createNamedQuery(AlertDefinition.QUERY_IS_TEMPLATE);
        enabledQuery.setParameter("alertDefinitionId", definitionId);
        List<Integer> resultIds = enabledQuery.getResultList();
        return (resultIds.size() == 1);
    }

    public int disableAlertDefinitions(Subject subject, Integer[] alertDefinitionIds) {
        int modifiedCount = 0;
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(subject, alertDefinition)) {
                // only disable an alert if it's currently enabled
                if (alertDefinition.getEnabled() == true) {
                    alertDefinition.setEnabled(false);
                    modifiedCount++;

                    // thus, remove it from the cache since it (should) already exist
                    notifyAlertConditionCacheManager("disableAlertDefinitions", alertDefinition.getId(),
                        AlertDefinitionEvent.DISABLED);
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

                // this is a "true" copy, so update parentId, resource, and resourceType
                newAlertDefinition.setParentId(alertDefinition.getParentId());
                newAlertDefinition.setResource(alertDefinition.getResource());
                newAlertDefinition.setResourceType(alertDefinition.getResourceType());

                entityManager.persist(newAlertDefinition);

                notifyAlertConditionCacheManager("copyAlertDefinitions", alertDefinition.getId(),
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
            purgeInternals(alertDefinitionId);
        }

        /*
         * Method for catching ENABLE / DISABLE changes will use switch logic off of the delta instead of calling out to
         * the enable/disable functions
         */
        AlertDefinition oldAlertDefinition = getAlertDefinitionById(subject, alertDefinitionId);

        boolean isAlertTemplate = (oldAlertDefinition.getResourceType() != null);

        if (checkPermission(subject, oldAlertDefinition) == false) {
            if (isAlertTemplate) {
                throw new PermissionException("You do not have permission to modify this alert template");
            } else {
                throw new PermissionException("You do not have permission to modify this alert definition");
            }
        }

        /*
         * only need to check the validity of the new alert definition if the authz checks pass *and* the old definition
         * is not currently deleted
         */
        checkAlertDefinition(oldAlertDefinition, isAlertTemplate ? null : oldAlertDefinition.getResource().getId());

        /*
         * Should not be able to update an alert definition if the old alert definition is in an invalid state
         */
        if (oldAlertDefinition.getDeleted()) {
            throw new AlertDefinitionUpdateException("Can not update deleted " + oldAlertDefinition.toSimpleString());
        }

        AlertDefinitionUpdateType updateType = AlertDefinitionUpdateType.get(oldAlertDefinition, alertDefinition);

        if ((isAlertTemplate == false)
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
            notifyAlertConditionCacheManager("updateAlertDefinition", oldAlertDefinition.getId(),
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
        fixRecoveryId(oldAlertDefinition);
        AlertDefinition newAlertDefinition = entityManager.merge(oldAlertDefinition);

        if ((isAlertTemplate == false)
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
                notifyAlertConditionCacheManager("updateAlertDefinition", newAlertDefinition.getId(),
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
                if (def.getNumericType() != NumericType.DYNAMIC) {
                    throw new InvalidAlertDefinitionException("Invalid Condition: '" + def.getDisplayName()
                        + "' is a trending metric, and thus will never have baselines calculated for it.");
                }
            }
        }

        return;
    }

    private void notifyAlertConditionCacheManager(String methodName, int alertDefinitionId,
        AlertDefinitionEvent alertDefinitionEvent) {
        LOG.debug("Invoking... " + methodName);
        agentStatusManager.updateByAlertDefinition(alertDefinitionId);
    }

    private void purgeInternals(int alertDefinitionId) {
        Query alertDampeningEventPurgeQuery = entityManager
            .createNamedQuery(AlertDampeningEvent.QUERY_DELETE_BY_ALERT_DEFINITION_ID);
        Query unmatchedAlertConditionLogPurgeQuery = entityManager
            .createNamedQuery(AlertConditionLog.QUERY_DELETE_UNMATCHED_BY_ALERT_DEFINITION_ID);

        alertDampeningEventPurgeQuery.setParameter("alertDefinitionId", alertDefinitionId);
        unmatchedAlertConditionLogPurgeQuery.setParameter("alertDefinitionId", alertDefinitionId);

        int alertDampeningEventPurgeCount = alertDampeningEventPurgeQuery.executeUpdate();
        int unmatchedAlertConditionLogPurgeCount = unmatchedAlertConditionLogPurgeQuery.executeUpdate();

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

    @SuppressWarnings("unchecked")
    public int purgeUnusedAlertDefinition() {
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

    public AlertDefinition getAlertDefinition(Subject subject, int alertDefinitionId) throws FetchException {
        try {
            return getAlertDefinitionById(subject, alertDefinitionId);
        } catch (Exception e) {
            throw new FetchException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinition> findAlertDefinitions(Subject subject, AlertDefinition criteria, PageControl pc)
        throws FetchException {

        try {
            QueryGenerator generator = new QueryGenerator(criteria, pc);
            if (authorizationManager.isInventoryManager(subject) == false) {
                generator.setAuthorizationResourceFragment(AuthorizationTokenType.RESOURCE, subject.getId());
            }

            Query query = generator.getQuery(entityManager);
            Query countQuery = generator.getCountQuery(entityManager);

            long count = (Long) countQuery.getSingleResult();
            List<AlertDefinition> alertDefinitions = query.getResultList();

            return new PageList<AlertDefinition>(alertDefinitions, (int) count, pc);
        } catch (Exception e) {
            throw new FetchException(e.getMessage());
        }

    }
}