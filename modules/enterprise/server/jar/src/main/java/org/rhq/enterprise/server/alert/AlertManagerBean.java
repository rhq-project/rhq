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
import org.jboss.annotation.ejb.TransactionTimeout;

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
import org.rhq.enterprise.server.alert.i18n.AlertI18NFactory;
import org.rhq.enterprise.server.alert.i18n.AlertI18NResourceKeys;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.EmailManagerLocal;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
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

    private final Log log = LogFactory.getLog(AlertManagerBean.class);

    @EJB
    @IgnoreDependency
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
    @EJB
    private EmailManagerLocal emailManager;

    private static Date bootTime = null;

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
    private void deleteAlerts(Integer[] ids) {
        for (Integer id : ids) {
            Alert alert = entityManager.find(Alert.class, id);
            if (alert != null) {
                AlertNotificationLog anl = alert.getAlertNotificationLog();
                entityManager.remove(anl);
                entityManager.remove(alert); // condition logs will be removed with entity cascading
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
    @TransactionTimeout(30 * 60)
    public int deleteAlerts(Subject user, int resourceId) {
        if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_ALERTS, resourceId)) {
            throw new PermissionException("User [" + user.getName() + "] does not have permissions to delete alerts "
                + "for resourceId=" + resourceId);
        }

        /*
         * Since BULK delete JPQL doesn't enforce cascade options, we need to delete the logs first and then the 
         * corresponding Alerts
         */
        long totalTime = 0L;

        long start = System.currentTimeMillis();
        Query query = entityManager.createNamedQuery(AlertConditionLog.QUERY_DELETE_BY_RESOURCE);
        query.setParameter("resourceId", resourceId);
        int deletedConditionLogs = query.executeUpdate();
        long end = System.currentTimeMillis();
        totalTime += (end - start);
        log.debug("Performance: Deleted [" + deletedConditionLogs + "] AlertConditionLogs in [" + (end - start)
            + "]ms for resourceId[" + resourceId + "]");

        start = System.currentTimeMillis();
        query = entityManager.createNamedQuery(AlertNotificationLog.QUERY_DELETE_BY_RESOURCE);
        query.setParameter("resourceId", resourceId);
        int deletedNotifications = query.executeUpdate();
        end = System.currentTimeMillis();
        totalTime += (end - start);
        log.debug("Performance: Deleted [" + deletedNotifications + "] AlertNotificationLogs in [" + (end - start)
            + "]ms for resourceId[" + resourceId + "]");

        start = System.currentTimeMillis();
        query = entityManager.createNamedQuery(Alert.QUERY_DELETE_BY_RESOURCE);
        query.setParameter("resourceId", resourceId);
        int deletedAlerts = query.executeUpdate();
        end = System.currentTimeMillis();
        totalTime += (end - start);
        log.debug("Performance: Deleted [" + deletedAlerts + "] Alerts in [" + (end - start) + "]ms for resourceId["
            + resourceId + "]");

        log.debug("Performance: Deleted [" + (deletedConditionLogs + deletedNotifications + deletedAlerts)
            + "] alert audit entities in [" + (totalTime) + "]ms for resourceId[" + resourceId + "]");

        return deletedAlerts;
    }

    /**
     * Remove alerts for the specified range of time.
     */
    // gonna use bulk delete, make sure we are in new tx to not screw up caller's hibernate session
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public int deleteAlerts(long beginTime, long endTime) {
        long totalTime = 0;

        long start = System.currentTimeMillis();
        Query query = entityManager.createNamedQuery(AlertConditionLog.QUERY_DELETE_BY_ALERT_CTIME);
        query.setParameter("begin", beginTime);
        query.setParameter("end", endTime);
        int conditionsDeleted = query.executeUpdate();
        long end = System.currentTimeMillis();
        log.debug("Deleted [" + conditionsDeleted + "] alert condition logs in [" + (end - start) + "]ms");
        totalTime += (end - start);

        start = System.currentTimeMillis();
        query = entityManager.createNamedQuery(AlertNotificationLog.QUERY_DELETE_BY_ALERT_CTIME);
        query.setParameter("begin", beginTime);
        query.setParameter("end", endTime);
        int deletedNotifications = query.executeUpdate();
        end = System.currentTimeMillis();
        log.debug("Deleted [" + deletedNotifications + "] alert notifications in [" + (end - start) + "]ms");
        totalTime += (end - start);

        start = System.currentTimeMillis();
        query = entityManager.createNamedQuery(Alert.QUERY_DELETE_BY_CTIME);
        query.setParameter("begin", beginTime);
        query.setParameter("end", endTime);
        int deletedAlerts = query.executeUpdate();
        end = System.currentTimeMillis();
        log.debug("Deleted [" + deletedAlerts + "] alerts in [" + (end - start) + "]ms");
        totalTime += (end - start);

        MeasurementMonitor.getMBean().incrementPurgeTime(totalTime);
        MeasurementMonitor.getMBean().setPurgedAlerts(deletedAlerts);
        MeasurementMonitor.getMBean().setPurgedAlertConditions(conditionsDeleted);
        MeasurementMonitor.getMBean().setPurgedAlertNotifications(deletedNotifications);
        log.debug("Deleted [" + (deletedAlerts + conditionsDeleted + deletedNotifications) + "] "
            + "alert audit records in [" + (totalTime) + "]ms");

        return deletedAlerts;
    }

    /**
     * Get the alert with the specified id.
     */
    public Alert getById(int alertId) {
        Alert alert = entityManager.find(Alert.class, alertId);
        if (alert == null)
            return null;

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

    public int getAlertCountByMeasurementDefinitionAndResourceGroup(int measurementDefinitionId, int groupId,
        long beginDate, long endDate) {
        Query query = PersistenceUtility.createCountQuery(entityManager,
            Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCEGROUP);
        query.setParameter("measurementDefinitionId", measurementDefinitionId);
        query.setParameter("startDate", beginDate);
        query.setParameter("endDate", endDate);
        query.setParameter("groupId", groupId);
        long count = (Long) query.getSingleResult();
        return (int) count;
    }

    public int getAlertCountByMeasurementDefinitionAndAutoGroup(int measurementDefinitionId, int resourceParentId,
        int resourceTypeId, long beginDate, long endDate) {
        Query query = PersistenceUtility.createCountQuery(entityManager, Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_AUTOGROUP);
        query.setParameter("measurementDefinitionId", measurementDefinitionId);
        query.setParameter("startDate", beginDate);
        query.setParameter("endDate", endDate);
        query.setParameter("parentId", resourceParentId);
        query.setParameter("typeId", resourceTypeId);
        long count = (Long) query.getSingleResult();
        return (int) count;
    }

    public int getAlertCountByMeasurementDefinitionAndResource(int measurementDefinitionId, int resourceId,
        long beginDate, long endDate) {
        Query query = PersistenceUtility.createCountQuery(entityManager, Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCE);
        query.setParameter("measurementDefinitionId", measurementDefinitionId);
        query.setParameter("startDate", beginDate);
        query.setParameter("endDate", endDate);
        query.setParameter("resourceId", resourceId);
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
            int fromIndex = round * BATCH_SIZE;
            int toIndex = fromIndex + BATCH_SIZE;
            if (toIndex > numSched) // don't run over the end of the list
                toIndex = numSched;
            List<Integer> scheds = scheduleIds.subList(fromIndex, toIndex);

            if (fromIndex == toIndex)
                continue;

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

        // Now fill in those schedules without return value to have an alertCount of 0
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

    @SuppressWarnings("unchecked")
    public PageList<Alert> findAlerts(int resourceId, Integer alertDefinitionId, AlertPriority priority,
        Long beginDate, Long endDate, PageControl pageControl) {
        pageControl.initDefaultOrderingField("a.ctime", PageOrdering.DESC);

        String queryStr = Alert.QUERY_FIND_BY_RESOURCE_DATED;

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryStr);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryStr, pageControl);

        queryCount.setParameter("id", resourceId);
        query.setParameter("id", resourceId);

        queryCount.setParameter("startDate", beginDate);
        query.setParameter("startDate", beginDate);

        queryCount.setParameter("endDate", endDate);
        query.setParameter("endDate", endDate);

        queryCount.setParameter("alertDefinitionId", alertDefinitionId);
        query.setParameter("alertDefinitionId", alertDefinitionId);

        queryCount.setParameter("priority", priority);
        query.setParameter("priority", priority);

        long totalCount = (Long) queryCount.getSingleResult();

        List<Alert> alerts = query.getResultList();

        fetchCollectionFields(alerts);

        return new PageList<Alert>(alerts, (int) totalCount, pageControl);
    }

    private void fetchCollectionFields(Alert alert) {
        alert.getConditionLogs().size();
        for (AlertConditionLog log : alert.getConditionLogs()) {
            // this is now lazy
            if (log.getCondition() != null && log.getCondition().getMeasurementDefinition() != null) {
                log.getCondition().getMeasurementDefinition().getId();
            }
        }
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
         * the AlertConditionLog children objects are already in the database, we need to persist the alert first
         * to prevent:
         *
         * "TransientObjectException: object references an unsaved transient instance - save the transient instance before
         * flushing org.jboss.on.domain.event.alert.AlertConditionLog.alert -> org.jboss.on.domain.event.alert.Alert"
         */
        this.createAlert(newAlert);
        log.debug("New alert identifier=" + newAlert.getId());

        AlertNotificationLog alertNotifLog = new AlertNotificationLog(newAlert);
        entityManager.persist(alertNotifLog);

        List<AlertConditionLog> unmatchedConditionLogs = alertConditionLogManager
            .getUnmatchedLogsByAlertDefinitionId(alertDefinitionId);
        for (AlertConditionLog unmatchedLog : unmatchedConditionLogs) {
            log.debug("Matched alert condition log for alertId=" + newAlert.getId() + ": " + unmatchedLog);
            newAlert.addConditionLog(unmatchedLog); // adds both relationships
        }

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

            String formattedValue = null;

            try {
                formattedValue = MeasurementConverter.format(Double.valueOf(aLog.getValue()), aLog.getCondition()
                    .getMeasurementDefinition().getUnits(), true);
            } catch (Exception e) {
                // If the value does not parse just report the value as is.
                formattedValue = aLog.getValue();
            }

            builder.append(NEW_LINE);

            builder.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EMAIL_CONDITION_LOG_FORMAT,
                conditionCounter, prettyPrintAlertCondition(aLog.getCondition()), new SimpleDateFormat(
                    "yyyy/MM/dd HH:mm:ss z").format(new Date(aLog.getCtime())), formattedValue));
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

                OperationDefinition definition = operationManager.getOperationDefinitionByResourceTypeAndName(
                    resourceTypeId, operationName, false);
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
        } else if ((category == AlertConditionCategory.RESOURCE_CONFIG) || (category == AlertConditionCategory.CHANGE)
            || (category == AlertConditionCategory.TRAIT)) {
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

    private String prettyPrintAlertURL(Alert alert) {
        StringBuilder builder = new StringBuilder();

        String baseUrl = systemManager.getSystemConfiguration().getProperty(RHQConstants.BaseURL);
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
        List<Resource> lineage = resourceManager.getResourceLineage(alert.getAlertDefinition().getResource().getId());
        String platformName = lineage.get(0).getName();
        String conditions = prettyPrintAlertConditions(alert.getConditionLogs());
        String alertUrl = prettyPrintAlertURL(alert);
        try {
            if (bootTime == null)
                bootTime = LookupUtil.getCoreServer().getBootTime();
            result = snmpTrapSender.sendSnmpTrap(alert, snmpNotification, platformName, conditions, bootTime, alertUrl);
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
             * there's no reason to update the cache directly anymore.  even though this direct type of update is safe 
             * (because we know the AlertManager will only be executing on the same server instance that is processing
             * these recovery alerts now) it's unnecessary because changes made via the AlertDefinitionManager will  
             * update the cache indirectly via the status field on the owning agent and the periodic job that checks it. 
             */
        } else if (firedDefinition.getWillRecover()) {
            log.debug("Disabling " + firedDefinition + " until recovered manually or by recovery definition");

            /*
             * disable until recovered manually or by recovery definition
             *
             * go through the manager layer so as to update the alert cache
             */
            alertDefinitionManager.disableAlertDefinitions(overlord, new Integer[] { firedDefinition.getId() });

            /*
             * there's no reason to update the cache directly anymore.  even though this direct type of update is safe 
             * (because we know the AlertManager will only be executing on the same server instance that is processing
             * these recovery alerts now) it's unnecessary because changes made via the AlertDefinitionManager will  
             * update the cache indirectly via the status field on the owning agent and the periodic job that checks it. 
             */
        }
    }
}