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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.alert.notification.EmailNotification;
import org.rhq.core.domain.alert.notification.RoleNotification;
import org.rhq.core.domain.alert.notification.SnmpNotification;
import org.rhq.core.domain.alert.notification.SubjectNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertDefinitionEvent;
import org.rhq.enterprise.server.alert.i18n.AlertI18NFactory;
import org.rhq.enterprise.server.alert.i18n.AlertI18NResourceKeys;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.EmailManagerLocal;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.measurement.util.MeasurementFormatter;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 * @author Ian Springer
 */
@Stateless
public class AlertManagerBean implements AlertManagerLocal {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(AlertManagerBean.class);

    @EJB
    @IgnoreDependency
    private AlertConditionCacheManagerLocal alertConditionCacheManager;
    @EJB
    private AlertConditionLogManagerLocal alertConditionLogManager;
    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    @IgnoreDependency
    private ResourceManagerLocal resourceManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private SystemManagerLocal systemManager;
    @EJB
    @IgnoreDependency
    private OperationManagerLocal operationManager;

    /**
     * Persist a detached alert.
     *
     * @return an alert
     */
    public Alert createAlert(Alert alert) {
        entityManager.persist(alert);
        return alert;
    }

    public Alert updateAlert(Alert alert) {
        return entityManager.merge(alert);
    }

    /**
     * Remove the alerts with the specified id's.
     */
    public void deleteAlerts(Integer[] ids) {
        for (Integer id : ids) {
            Alert alert = entityManager.find(Alert.class, id);
            if (alert != null) {
                entityManager.remove(alert);
            }
        }
    }

    public void deleteAlerts(Subject user, int resourceId, Integer[] ids) {
        if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_ALERTS, resourceId)) {
            throw new PermissionException("User [" + user.getName() + "] does not have permissions to delete alerts "
                + "for resourceId=" + resourceId);
        }

        deleteAlerts(ids);
    }

    // gonna use bulk delete, make sure we are in new tx to not screw up caller's hibernate session
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int deleteAlerts(Subject user, int resourceId) {
        if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_ALERTS, resourceId)) {
            throw new PermissionException("User [" + user.getName() + "] does not have permissions to delete alerts "
                + "for resourceId=" + resourceId);
        }

        /*
         * Since JPQL doesn't enforce cascade options, we need to delete the logs first and then the corresponding
         * Alerts
         */
        Query query = entityManager.createNamedQuery(AlertConditionLog.QUERY_DELETE_BY_RESOURCE);
        query.setParameter("resourceId", resourceId);
        int deletedLogs = query.executeUpdate();

        query = entityManager.createNamedQuery(Alert.QUERY_DELETE_BY_RESOURCE);
        query.setParameter("resourceId", resourceId);
        int deletedAlerts = query.executeUpdate();

        if (deletedLogs > 0 || deletedAlerts > 0) {
            query = entityManager.createNamedQuery(AlertNotificationLog.QUERY_DELETE_ORPHANED);
            query.executeUpdate();
        }

        return deletedAlerts;
    }

    /**
     * Remove alerts for the specified range of time.
     */
    // gonna use bulk delete, make sure we are in new tx to not screw up caller's hibernate session
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int  deleteAlerts(long beginTime, long endTime) {
        long start = System.currentTimeMillis();
        Query query = entityManager.createNamedQuery(AlertConditionLog.QUERY_DELETE_BY_ALERT_CTIME);
        query.setParameter("begin", beginTime);
        query.setParameter("end", endTime);
        int conditionsDeleted = query.executeUpdate();

        query = entityManager.createNamedQuery(Alert.QUERY_DELETE_BY_CTIME);
        query.setParameter("begin", beginTime);
        query.setParameter("end", endTime);
        int deletedAlerts = query.executeUpdate();

        query = entityManager.createNamedQuery(AlertNotificationLog.QUERY_DELETE_ORPHANED);
        int deletedNotifications = query.executeUpdate();

        log.info("Delete [" + deletedAlerts + "] alerts, [" + conditionsDeleted + "] conditions, and [" + deletedNotifications + "] conditions in [" + (System.currentTimeMillis() - start) + "]ms");

        return deletedAlerts;
    }

    /**
     * Get the alert with the specified id.
     */
    public Alert getById(int alertId) {
        Alert alert = entityManager.find(Alert.class, alertId);
        fetchCollectionFields(alert);
        return alert;
    }

    /**
     * Get the number of alerts for the specified alert definition id.
     */
    @SuppressWarnings("unchecked")
    public int getAlertCount(Integer alertDefId) {
        Query query = entityManager.createNamedQuery("Alert.findByAlertDefinition");
        query.setParameter("id", alertDefId);
        List alerts = query.getResultList();
        return alerts.size();
    }

    public int getAlertCountByMeasurementDefinitionId(Integer measurementDefinitionId, long begin, long end) {
        Query query = PersistenceUtility.createCountQuery(entityManager, Alert.QUERY_FIND_BY_MEASUREMENT_DEFINITION_ID);
        query.setParameter("measurementDefinitionId", measurementDefinitionId);
        query.setParameter("begin", begin);
        query.setParameter("end", end);
        long count = (Long) query.getSingleResult();
        return (int) count;
    }

    public int getAlertCountByMeasurementDefinitionAndResources(int measurementDefinitionId,
        Collection<Resource> resources, long beginDate, long endDate) {
        Query query = PersistenceUtility.createCountQuery(entityManager, Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCES);
        query.setParameter("measurementDefinitionId", measurementDefinitionId);
        query.setParameter("startDate", beginDate);
        query.setParameter("endDate", endDate);
        query.setParameter("resources", resources);
        long count = (Long) query.getSingleResult();
        return (int) count;
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Integer> getAlertCountForSchedules(long begin, long end, List<Integer> scheduleIds) {
        if ((scheduleIds == null) || (scheduleIds.size() == 0) || (end < begin)) {
            return new HashMap<Integer, Integer>();
        }

        final int BATCH_SIZE = 1000;

        int numSched = scheduleIds.size();
        int rounds = (numSched / BATCH_SIZE) + 1;
        Map<Integer, Integer> resMap = new HashMap<Integer, Integer>();

        // iterate over the passed schedules ids when we have more than 1000 of them, as some
        // databases bail out with more than 1000 resources in IN () clauses.
        for (int round = 0; round < rounds; round++) {
            int toIndex = round * BATCH_SIZE + BATCH_SIZE - 1;
            if (toIndex > numSched) // don't run over the end of the list
                toIndex = numSched;
            List<Integer> scheds = scheduleIds.subList(round * BATCH_SIZE, toIndex);

            Query q = entityManager.createNamedQuery(Alert.QUERY_GET_ALERT_COUNT_FOR_SCHEDULES);
            q.setParameter("startDate", begin);
            q.setParameter("endDate", end);
            q.setParameter("schedIds", scheds);
            List<Object[]> ret = q.getResultList();
            if (ret.size() > 0) {
                for (Object[] obj : ret) {
                    Integer scheduleId = (Integer) obj[0];
                    Long tmp = (Long) obj[1];
                    int alertCount = tmp.intValue();
                    resMap.put(scheduleId, alertCount);
                }
            }
        }

        // Now fill in those schedules without return value to have an oobCount of 0
        for (int scheduleId : scheduleIds) {
            if (!resMap.containsKey(scheduleId)) {
                resMap.put(scheduleId, 0);
            }
        }

        return resMap;
    }

    @SuppressWarnings("unchecked")
    public PageList<Alert> findAlerts(Subject subject, Integer[] resourceIds, AlertPriority priority, long timeRange,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("a.ctime", PageOrdering.DESC);

        if ((resourceIds != null) && (resourceIds.length == 0)) {
            return new PageList<Alert>(pageControl);
        }

        String queryStr;
        if (authorizationManager.isInventoryManager(subject)) {
            queryStr = ((resourceIds == null) ? Alert.QUERY_DASHBOARD_ALL_ADMIN
                : Alert.QUERY_DASHBOARD_BY_RESOURCE_IDS_ADMIN);
        } else {
            queryStr = ((resourceIds == null) ? Alert.QUERY_DASHBOARD_ALL : Alert.QUERY_DASHBOARD_BY_RESOURCE_IDS);
        }

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryStr, pageControl);
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryStr);

        if (!authorizationManager.isInventoryManager(subject)) {
            query.setParameter("subjectId", subject.getId());
            queryCount.setParameter("subjectId", subject.getId());
        }

        if (resourceIds != null) {
            List<Integer> resourceIdList = Arrays.asList(resourceIds);
            queryCount.setParameter("resourceIds", resourceIdList);
            query.setParameter("resourceIds", resourceIdList);
        }

        long startTime = System.currentTimeMillis() - timeRange;

        queryCount.setParameter("startDate", startTime);
        queryCount.setParameter("priority", priority);

        query.setParameter("startDate", startTime);
        query.setParameter("priority", priority);

        long totalCount = (Long) queryCount.getSingleResult();

        List<Alert> alerts = query.getResultList();

        fetchCollectionFields(alerts);

        return new PageList<Alert>(alerts, (int) totalCount, pageControl);
    }

    /**
     * Get a page list of alerts for the resource with the specified appdef id.
     */
    @SuppressWarnings("unchecked")
    public PageList<Alert> findAlerts(int resourceId, Date dateFilter, PageControl pageControl) {
        pageControl.initDefaultOrderingField("a.ctime", PageOrdering.DESC);

        String queryStr = ((dateFilter == null) ? Alert.QUERY_FIND_BY_RESOURCE : Alert.QUERY_FIND_BY_RESOURCE_DATED);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryStr);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryStr, pageControl);

        queryCount.setParameter("id", resourceId);
        query.setParameter("id", resourceId);

        if (dateFilter != null) {
            Date nextDay = new Date(dateFilter.getTime() + (24 * 60 * 60 * 1000));

            queryCount.setParameter("startDate", dateFilter.getTime());
            queryCount.setParameter("endDate", nextDay.getTime());

            query.setParameter("startDate", dateFilter.getTime());
            query.setParameter("endDate", nextDay.getTime());
        }

        long totalCount = (Long) queryCount.getSingleResult();

        List<Alert> alerts = query.getResultList();

        fetchCollectionFields(alerts);

        return new PageList<Alert>(alerts, (int) totalCount, pageControl);
    }

    private void fetchCollectionFields(Alert alert) {
        alert.getConditionLogs().size();
    }

    private void fetchCollectionFields(List<Alert> alerts) {
        for (Alert alert : alerts) {
            fetchCollectionFields(alert);
        }
    }

    public void fireAlert(int alertDefinitionId) {
        log.debug("Firing an alert for alertDefinition with id=" + alertDefinitionId + "...");

        Subject overlord = subjectManager.getOverlord();
        AlertDefinition alertDefinition = alertDefinitionManager.getAlertDefinitionById(overlord, alertDefinitionId);

        /*
         * creating an alert via an alertDefinition automatically creates the needed auditing data structures such as
         * alertConditionLogs and alertNotificationLogs
         */
        Alert newAlert = new Alert(alertDefinition, System.currentTimeMillis());

        /*
         * this the AlertConditionLog children objects are already in the database, we need to persist the alert first
         * to prevent:
         *
         * "TransientObjectException: object references an unsaved transient instance - save the transient instance before
         * flushing org.jboss.on.domain.event.alert.AlertConditionLog.alert -> org.jboss.on.domain.event.alert.Alert"
         */
        this.createAlert(newAlert);

        List<AlertConditionLog> unmatchedConditionLogs = alertConditionLogManager
            .getUnmatchedLogsByAlertDefinitionId(alertDefinitionId);
        for (AlertConditionLog unmatchedLog : unmatchedConditionLogs) {
            newAlert.addConditionLog(unmatchedLog); // adds both relationships
        }

        this.updateAlert(newAlert);

        // process recovery actions
        processRecovery(alertDefinition);

        sendAlertNotifications(newAlert);

        triggerOperation(alertDefinition);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void triggerOperation(AlertDefinition alertDefinition) {
        OperationDefinition operationDefinition = alertDefinition.getOperationDefinition();
        if (operationDefinition == null) {
            return;
        }

        Subject overlord = subjectManager.getOverlord(); // use overlord for system side-effects

        try {
            operationManager.scheduleResourceOperation(overlord, alertDefinition.getResource().getId(),
                operationDefinition.getName(), null, // today, only no-arg operations can be executed via alerting
                new SimpleTrigger("alerting", "alerting", new Date()), "Triggered when "
                    + alertDefinition.toSimpleString() + " fired an alert off");
        } catch (SchedulerException se) {
            // as of 10/23/2007, there's not much we can do proactively.  just log that this happened for now.
            log.error(alertDefinition.toSimpleString() + " could not successfully schedule " + "its operation "
                + operationDefinition.getName());
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendAlertNotifications(Alert alert) {
        /*
         * make this method public in case we show the notification failure to the user in the UI in the future and want
         * to give them some way to explicitly try to re-send the notification for some client-side auditing purposes
         */
        try {
            log.debug("Sending alert notifications for " + alert.toSimpleString() + "...");
            Set<AlertNotification> alertNotifications = alert.getAlertDefinition().getAlertNotifications();
            Set<String> emailAddresses = new LinkedHashSet<String>();

            for (AlertNotification alertNotification : alertNotifications) {
                if (alertNotification instanceof RoleNotification) {
                    RoleNotification roleNotification = (RoleNotification) alertNotification;
                    Set<Subject> subjects = roleNotification.getRole().getSubjects();

                    for (Subject subject : subjects) {
                        if (subject.getFsystem()) {
                            /*
                             * if a user wants to notify the superuser role, that's fine... but we shouldn't send an
                             * email to the special overlord user
                             */
                            continue;
                        }

                        String emailAddress = subject.getEmailAddress();

                        processEmailAddress(alert, emailAddress, emailAddresses);
                    }
                } else if (alertNotification instanceof SubjectNotification) {
                    SubjectNotification subjectNotification = (SubjectNotification) alertNotification;
                    String emailAddress = subjectNotification.getSubject().getEmailAddress();

                    processEmailAddress(alert, emailAddress, emailAddresses);
                } else if (alertNotification instanceof EmailNotification) {
                    EmailNotification emailNotification = (EmailNotification) alertNotification;
                    String emailAddress = emailNotification.getEmailAddress();

                    processEmailAddress(alert, emailAddress, emailAddresses);
                } else if (alertNotification instanceof SnmpNotification) {
                    SnmpNotification snmpNotification = (SnmpNotification) alertNotification;

                    sendAlertSnmpTrap(alert, snmpNotification);
                }
            }

            sendAlertNotificationEmails(alert, emailAddresses);
        } catch (Exception e) {
            log.error("Failed to send all notifications for " + alert.toSimpleString(), e);
        }
    }

    private void processEmailAddress(Alert alert, String emailAddress, Set<String> emailAddresses) {
        if (emailAddress == null) {
            return;
        }

        emailAddress = emailAddress.toLowerCase();

        if (!emailAddresses.contains(emailAddress)) {
            emailAddresses.add(emailAddress);
        }
    }

    private void sendAlertNotificationEmails(Alert alert, Set<String> emailAddresses) {
        log.debug("Sending alert notifications for " + alert.toSimpleString() + "...");

        EmailManagerLocal emailManager = LookupUtil.getEmailManagerBean();
        AlertDefinition alertDefinition = alert.getAlertDefinition();
        Map<String, String> alertMessage = emailManager.getAlertEmailMessage(
            prettyPrintResourceHierarchy(alertDefinition.getResource()), alertDefinition.getResource().getName(),
            alertDefinition.getName(), alertDefinition.getPriority().toString(), new Date(alert.getCtime()).toString(),
            prettyPrintAlertConditions(alert.getConditionLogs()), prettyPrintAlertURL(alert));
        String messageSubject = alertMessage.keySet().iterator().next();
        String messageBody = alertMessage.values().iterator().next();

        try {
            emailManager.sendEmail(emailAddresses, messageSubject, messageBody);
            log.debug("All notifications for " + alert.toSimpleString() + " succeeded");
        } catch (Throwable t) {
            log.error("One or more notifications for " + alert.toSimpleString() + " failed: " + t.getMessage());
        }
    }

    private static String NEW_LINE = System.getProperty("line.separator");

    private String prettyPrintResourceHierarchy(Resource resource) {
        StringBuilder builder = new StringBuilder();

        List<Resource> lineage = resourceManager.getResourceLineage(resource.getId());

        int depth = 0;
        for (Resource res : lineage) {
            if (depth == 0) {
                builder.append(" - ");
            } else {
                builder.append(NEW_LINE);

                for (int i = 0; i < depth; i++) {
                    builder.append("   ");
                }

                builder.append("|");
                builder.append(NEW_LINE);

                for (int i = 0; i < depth; i++) {
                    builder.append("   ");
                }

                builder.append("+- ");
            }

            builder.append(res.getName());

            depth++;
        }

        return builder.toString();
    }

    private String prettyPrintAlertConditions(Set<AlertConditionLog> conditionLogs) {
        StringBuilder builder = new StringBuilder();

        int conditionCounter = 1;
        for (AlertConditionLog aLog : conditionLogs) {
            builder.append(NEW_LINE);

            builder.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EMAIL_CONDITION_LOG_FORMAT,
                conditionCounter, prettyPrintAlertCondition(aLog.getCondition()), new SimpleDateFormat(
                    "yyyy/MM/dd HH:mm:ss z").format(new Date(aLog.getCtime())), aLog.getValue()));
            conditionCounter++;
        }

        return builder.toString();
    }

    private String prettyPrintAlertCondition(AlertCondition condition) {
        StringBuilder builder = new StringBuilder();

        AlertConditionCategory category = condition.getCategory();

        // first format the LHS of the operator
        if (category == AlertConditionCategory.CONTROL) {
            try {
                Integer resourceTypeId = condition.getAlertDefinition().getResource().getResourceType().getId();
                String operationName = condition.getName();
                OperationManagerLocal operationManager = LookupUtil.getOperationManager(); // TODO why is this here? Why not the class wide variable?

                OperationDefinition definition = operationManager.getOperationDefinitionByResourceTypeAndName(
                    resourceTypeId, operationName);
                builder.append(definition.getDisplayName()).append(' ');
            } catch (Exception e) {
                builder.append(condition.getName()).append(' ');
            }
        } else {
            builder.append(condition.getName()).append(' ');
        }

        // next format the RHS
        if (category == AlertConditionCategory.CONTROL) {
            builder.append(condition.getOption());
        } else if ((category == AlertConditionCategory.THRESHOLD) || (category == AlertConditionCategory.BASELINE)) {
            builder.append(condition.getComparator());
            builder.append(' ');

            MeasurementSchedule schedule = null;

            MeasurementUnits units;
            double value = condition.getThreshold();
            if (category == AlertConditionCategory.THRESHOLD) {
                units = condition.getMeasurementDefinition().getUnits();
            } else // ( category == AlertConditionCategory.BASELINE )
            {
                units = MeasurementUnits.PERCENTAGE;
            }

            String formatted = MeasurementConverter.format(value, units, true);
            builder.append(formatted);

            if (category == AlertConditionCategory.BASELINE) {
                builder.append(" of ");
                builder.append(MeasurementFormatter.getBaselineText(condition.getOption(), schedule));
            }
        } else if ((category == AlertConditionCategory.CONFIGURATION_PROPERTY)
            || (category == AlertConditionCategory.CHANGE) || (category == AlertConditionCategory.TRAIT)) {
            builder.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_CURRENT_LIST_VALUE_CHANGED));
        } else if (category == AlertConditionCategory.EVENT) {
            if ((condition.getOption() != null) && (condition.getOption().length() > 0)) {
                builder.append(AlertI18NFactory.getMessage(
                    AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY_REGEX_MATCH, condition.getName(),
                    condition.getOption()));
            } else {
                builder.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY,
                    condition.getName()));
            }
        } else if (category == AlertConditionCategory.AVAILABILITY) {
            builder.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_AVAILABILITY,
                condition.getOption()));
        } else {
            // do nothing
        }

        return builder.toString();
    }

    @SuppressWarnings("deprecation")
    private String prettyPrintAlertURL(Alert alert) {
        StringBuilder builder = new StringBuilder();

        String baseUrl = systemManager.getSystemConfiguration().getProperty(HQConstants.BaseURL);
        builder.append(baseUrl);
        if (baseUrl.endsWith("/") == false) {
            builder.append("/");
        }

        builder.append("alerts/Alerts.do?mode=viewAlert");

        builder.append("&id=" + alert.getAlertDefinition().getResource().getId());
        builder.append("&a=" + alert.getId());

        return builder.toString();
    }

    private void sendAlertSnmpTrap(Alert alert, SnmpNotification snmpNotification) {
        SnmpTrapSender snmpTrapSender = new SnmpTrapSender();
        log.debug("Sending SNMP trap with OID " + snmpNotification.getOid() + " to SNMP engine "
            + snmpNotification.getHost() + ":" + snmpNotification.getPort() + "...");
        String result;
        try {
            result = snmpTrapSender.sendSnmpTrap(alert, snmpNotification);
        } catch (Throwable t) {
            result = "failed - cause: " + t;
        }

        log.debug("Result of sending SNMP trap: " + result);
        // TODO: Log the action result to the DB (i.e. as an AlertNotificationLog).
        //       (see http://jira.jboss.com/jira/browse/JBNADM-1820)
    }

    private void processRecovery(AlertDefinition firedDefinition) {
        Subject overlord = subjectManager.getOverlord();
        Integer recoveryDefinitionId = firedDefinition.getRecoveryId();

        if (recoveryDefinitionId != 0) {
            log.debug("Processing recovery rules...");
            log.debug("Found recoveryDefinitionId " + recoveryDefinitionId);

            AlertDefinition toBeRecoveredDefinition = alertDefinitionManager.getAlertDefinitionById(overlord,
                recoveryDefinitionId);
            boolean wasEnabled = toBeRecoveredDefinition.getEnabled();

            log
                .debug(firedDefinition + (wasEnabled ? "does not need to recover " : "needs to recover ")
                    + toBeRecoveredDefinition
                    + (wasEnabled ? ", it was already enabled " : ", it was currently disabled "));

            if (!wasEnabled) {
                /*
                 * recover the other alert, go through the manager layer so as to update the alert cache
                 */
                alertDefinitionManager.enableAlertDefinitions(overlord, new Integer[] { recoveryDefinitionId });
            }

            /*
             * an alert definition may have several recovery definitions; if ONE of the recovery definitions fires, 
             * and thus re-enables the to-be-recovered alert definition, ALL recovery definitions (for that alert
             * definition) must be removed from cache (including the one that just fired)
             */
            List<AlertDefinition> relatedRecoveryDefinitions = alertDefinitionManager.getAllRecoveryDefinitionsById(
                overlord, toBeRecoveredDefinition.getId());
            for (AlertDefinition relatedRecoveryDefinition : relatedRecoveryDefinitions) {
                /*
                 * bypass the manager layer and reuse the AlertDefinitionEvent construct to update the cache;
                 * we don't want to actually disable these definitions, just remove them from the cache
                 */
                alertConditionCacheManager.updateConditions(relatedRecoveryDefinition, AlertDefinitionEvent.DISABLED);
            }
        } else if (firedDefinition.getWillRecover()) {
            log.debug("Disabling " + firedDefinition + " until recovered manually or by recovery definition");

            /*
             * disable until recovered manually or by recovery definition
             *
             * go through the manager layer so as to update the alert cache
             */
            alertDefinitionManager.disableAlertDefinitions(overlord, new Integer[] { firedDefinition.getId() });

            /*
             * an alert definition may have several recovery definitions; if this to-be-recovered alert definition
             * fires an alert, it will be disabled; at this point, all of its (enabled) recovery definitions must be
             * added to the cache
             */
            List<AlertDefinition> recoveryDefinitions = alertDefinitionManager.getAllRecoveryDefinitionsById(overlord,
                firedDefinition.getId());
            for (AlertDefinition recoveryDefinition : recoveryDefinitions) {
                /* 
                 * bypass the manager layer and reuse the AlertDefinitionEvent construct to update the cache; we don't
                 * want to enable these definitions (cause they are already enabled), just add them them to the cache
                 */
                alertConditionCacheManager.updateConditions(recoveryDefinition, AlertDefinitionEvent.ENABLED);
            }
        }
    }
}