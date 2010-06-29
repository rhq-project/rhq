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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.StringUtils;
import org.rhq.core.server.MeasurementConverter;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.jdbc.JDBCUtil;
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
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.plugin.pc.alert.AlertServerPluginContainer;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 * @author Ian Springer
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class AlertManagerBean implements AlertManagerLocal, AlertManagerRemote {
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

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;

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
     * @param user caller
     * @param alertIds primary keys of the alerts to delete
     * @return number of alerts deleted
     */
    public int deleteAlerts(Subject user, Integer[] alertIds) {
        int count = 0;
        for (Integer nextAlertId : alertIds) {
            Alert alert = entityManager.find(Alert.class, nextAlertId);
            if (alert != null) {
                //                AlertNotificationLog anl = alert.getAlertNotificationLog();  TODO is that all?
                //                entityManager.remove(anl);
                Resource resource = alert.getAlertDefinition().getResource();
                if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_ALERTS, resource.getId())) {
                    throw new PermissionException("User [" + user.getName()
                        + "] does not have permissions to delete alerts: " + Arrays.asList(alertIds));
                }

                entityManager.remove(alert); // condition logs will be removed with entity cascading
            }
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private void checkAlertsPermission(Subject user, Integer[] alertIds) {
        Query q = entityManager.createNamedQuery(Alert.QUERY_FIND_RESOURCES);
        q.setParameter("alertIds", Arrays.asList(alertIds));
        List<Resource> resources = q.getResultList();

        List<Resource> forbiddenResources = new ArrayList<Resource>();
        for (Resource resource : resources) {
            if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_ALERTS, resource.getId())) {
                forbiddenResources.add(resource);
            }
        }
        if (!forbiddenResources.isEmpty()) {
            throw new PermissionException("User [" + user.getName() + "] does not have permissions to manage alerts "
                + "for the following Resource(s): " + forbiddenResources);
        }
    }

    public void deleteResourceAlerts(Subject user, Integer[] alertIds) {
        checkAlertsPermission(user, alertIds);

        deleteAlerts(user, alertIds);
    }

    public void deleteAlerts(Subject user, int resourceId, Integer[] ids) {
        if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_ALERTS, resourceId)) {
            throw new PermissionException("User [" + user.getName() + "] does not have permissions to delete alerts "
                + "for resourceId=" + resourceId);
        }

        deleteAlerts(user, ids);
    }

    public void deleteAlertsForResourceGroup(Subject user, int resourceGroupId, Integer[] ids) {
        if (!authorizationManager.hasGroupPermission(user, Permission.MANAGE_ALERTS, resourceGroupId)) {
            throw new PermissionException("User [" + user.getName() + "] does not have permissions to delete alerts "
                + "for groupId=" + resourceGroupId);
        }

        deleteAlerts(user, ids);
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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public int purgeAlerts() {
        long totalTime = 0;

        Connection conn = null;
        PreparedStatement truncateConditionLogsStatement = null;
        PreparedStatement truncateNotificationLogsStatement = null;
        PreparedStatement truncateAlertsStatement = null;
        try {
            conn = rhqDs.getConnection();

            truncateConditionLogsStatement = conn.prepareStatement(AlertConditionLog.QUERY_NATIVE_TRUNCATE_SQL);
            truncateNotificationLogsStatement = conn.prepareStatement(AlertNotificationLog.QUERY_NATIVE_TRUNCATE_SQL);
            truncateAlertsStatement = conn.prepareStatement(Alert.QUERY_NATIVE_TRUNCATE_SQL);

            long start = System.currentTimeMillis();
            int purgedConditions = truncateConditionLogsStatement.executeUpdate();
            long end = System.currentTimeMillis();
            log.debug("Purged [" + purgedConditions + "] alert condition logs in [" + (end - start) + "]ms");
            totalTime += (end - start);

            start = System.currentTimeMillis();
            int purgedNotifications = truncateNotificationLogsStatement.executeUpdate();
            end = System.currentTimeMillis();
            log.debug("Purged [" + purgedNotifications + "] alert notifications in [" + (end - start) + "]ms");
            totalTime += (end - start);

            start = System.currentTimeMillis();
            int purgedAlerts = truncateAlertsStatement.executeUpdate();
            end = System.currentTimeMillis();
            log.debug("Purged [" + purgedAlerts + "] alerts in [" + (end - start) + "]ms");
            totalTime += (end - start);

            MeasurementMonitor.getMBean().incrementPurgeTime(totalTime);
            MeasurementMonitor.getMBean().setPurgedAlerts(purgedAlerts);
            MeasurementMonitor.getMBean().setPurgedAlertConditions(purgedConditions);
            MeasurementMonitor.getMBean().setPurgedAlertNotifications(purgedNotifications);
            log.debug("Deleted [" + (purgedAlerts + purgedConditions + purgedNotifications) + "] "
                + "alert audit records in [" + (totalTime) + "]ms");

            return purgedAlerts;
        } catch (SQLException sqle) {
            log.error("Error purging alerts", sqle);
            throw new RuntimeException("Error purging alerts: " + sqle.getMessage());
        } finally {
            JDBCUtil.safeClose(truncateConditionLogsStatement);
            JDBCUtil.safeClose(truncateNotificationLogsStatement);
            JDBCUtil.safeClose(truncateAlertsStatement);
            JDBCUtil.safeClose(conn);
        }
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

    public int getAlertCountByMeasurementDefinitionId(Integer measurementDefinitionId, long begin, long end) {
        Query query = PersistenceUtility.createCountQuery(entityManager, Alert.QUERY_FIND_BY_MEASUREMENT_DEFINITION_ID);
        query.setParameter("measurementDefinitionId", measurementDefinitionId);
        query.setParameter("begin", begin);
        query.setParameter("end", end);
        long count = (Long) query.getSingleResult();
        return (int) count;
    }

    public int getAlertCountByMeasurementDefinitionAndResources(int measurementDefinitionId, int[] resourceIds,
        long beginDate, long endDate) {
        Query query = PersistenceUtility.createCountQuery(entityManager, Alert.QUERY_FIND_BY_MEAS_DEF_ID_AND_RESOURCES);
        query.setParameter("measurementDefinitionId", measurementDefinitionId);
        query.setParameter("startDate", beginDate);
        query.setParameter("endDate", endDate);
        query.setParameter("resourceIds", ArrayUtils.wrapInList(resourceIds));
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
        alert.getAlertNotificationLogs().size();
    }

    private void fetchCollectionFields(List<Alert> alerts) {
        for (Alert alert : alerts) {
            fetchCollectionFields(alert);
        }
    }

    /**
     * Acknowledge the alerts (that got fired) so that admins know who is working on fixing the situation.
     *
     * @param user calling user
     * @param alertIds PKs of the alerts to acknowledge
     * @return number of alerts acknowledged
     */
    public int acknowledgeAlerts(Subject user, Integer[] alertIds) {
        if (alertIds == null || alertIds.length == 0) {
            log.debug("acknowledgeAlerts: no alertIds passed");
            return 0;
        }

        checkAlertsPermission(user, alertIds);

        int count = 0;
        final long NOW = System.currentTimeMillis();
        for (int nextAlertId : alertIds) {
            Alert alert = entityManager.find(Alert.class, nextAlertId);
            if (alert == null) {
                continue;
            } else {
                count++;
            }
            alert.setAcknowledgingSubject(user.getName());
            alert.setAcknowledgeTime(NOW);
        }
        return count;
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

        //        AlertNotificationLog alertNotifLog = new AlertNotificationLog(newAlert);  TODO - is that all?
        //        entityManager.persist(alertNotifLog);

        List<AlertConditionLog> unmatchedConditionLogs = alertConditionLogManager
            .getUnmatchedLogsByAlertDefinitionId(alertDefinitionId);
        for (AlertConditionLog unmatchedLog : unmatchedConditionLogs) {
            log.debug("Matched alert condition log for alertId=" + newAlert.getId() + ": " + unmatchedLog);
            newAlert.addConditionLog(unmatchedLog); // adds both relationships
        }

        // process recovery actions
        processRecovery(alertDefinition);

        sendAlertNotifications(newAlert); // this really needs to be done async,
    }

    /**
     * This is the core of the alert sending process. For each AlertNotification that is hanging
     * on the alerts definition, the sender is instantiated and its send() method called. If a sender
     * returns a list of email addresses, those will be collected and sent at the end.
     * @param alert the fired alert
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendAlertNotifications(Alert alert) {
        /*
         * make this method public in case we show the notification failure to the user in the UI in the future and want
         * to give them some way to explicitly try to re-send the notification for some client-side auditing purposes
         */
        try {
            log.debug("Sending alert notifications for " + alert.toSimpleString() + "...");
            List<AlertNotification> alertNotifications = alert.getAlertDefinition().getAlertNotifications();
            Set<String> emailAddresses = new LinkedHashSet<String>();

            AlertSenderPluginManager alertSenderPluginManager = getAlertPluginManager();

            for (AlertNotification alertNotification : alertNotifications) {

                // Send over the new AlertSender plugins
                String senderName = alertNotification.getSenderName();
                if (senderName == null) {
                    log.error("Alert notification " + alertNotification + " has no sender name defined");
                    continue;
                }

                AlertNotificationLog alNoLo;
                AlertSender<?> sender = alertSenderPluginManager.getAlertSenderForNotification(alertNotification);

                if (sender != null) {
                    try {
                        SenderResult result = sender.send(alert);

                        if (result == null) {
                            log.warn("- !! -- sender [" + senderName
                                + "] did not return a SenderResult. Please fix this -- !! - ");
                            alNoLo = new AlertNotificationLog(alert, senderName);
                            alNoLo.setMessage("Sender did not return a SenderResult, assuming failure");

                        } else if (result.getState() == ResultState.DEFERRED_EMAIL) {

                            alNoLo = new AlertNotificationLog(alert, senderName, result);
                            if (result.getEmails() != null && !result.getEmails().isEmpty()) {
                                emailAddresses.addAll(result.getEmails());
                            }
                        } else {
                            alNoLo = new AlertNotificationLog(alert, senderName, result);
                        }
                        log.debug(result);
                    } catch (Throwable t) {
                        log.error("Sender failed: " + t.getMessage());
                        if (log.isDebugEnabled())
                            log.debug("Sender " + sender.toString() + "failed: \n", t);
                        alNoLo = new AlertNotificationLog(alert, senderName, ResultState.FAILURE,
                            "Failed with exception: " + t.getMessage());
                    }
                } else {
                    alNoLo = new AlertNotificationLog(alert, senderName, ResultState.FAILURE,
                        "Failed to obtain a sender with given name");
                }
                entityManager.persist(alNoLo);
                alert.addAlertNotificatinLog(alNoLo);
            }

            // send them off
            Collection<String> badAddresses = null;
            try {
                badAddresses = sendAlertNotificationEmails(alert, emailAddresses);
            } catch (Throwable t) {
                badAddresses = new ArrayList<String>();
                log.error("Could not send emails to " + emailAddresses + " for " + alert + ", cause:", t);
            }
            // TODO we may do the same for SMS in the future.

            // log those bad addresses to the gui and their individual senders (if possible)
            if (!badAddresses.isEmpty()) {
                for (AlertNotificationLog anl : alert.getAlertNotificationLogs()) {
                    if (!(anl.getResultState() == ResultState.DEFERRED_EMAIL))
                        continue;

                    List<String> badList = new ArrayList<String>();
                    for (String badOne : badAddresses) {
                        if (anl.getTransientEmails().contains(badOne)) {
                            anl.setResultState(ResultState.FAILED_EMAIL);
                            badList.add(badOne);
                        }
                    }
                    if (anl.getResultState() == ResultState.FAILED_EMAIL)
                        anl.setBadEmails(StringUtils.getListAsString(badList, ","));
                    if (anl.getResultState() == ResultState.DEFERRED_EMAIL && badList.isEmpty())
                        anl.setResultState(ResultState.SUCCESS);
                }
            } else { // No bad addresses
                // Only set the result state to success for email sending notifications
                // We must not set them if the notification failed.
                for (AlertNotificationLog anl : alert.getAlertNotificationLogs()) {
                    if (anl.getResultState() == ResultState.DEFERRED_EMAIL)
                        anl.setResultState(ResultState.SUCCESS);
                }
            }

        } catch (Throwable t) {
            log.error("Failed to send all notifications for " + alert.toSimpleString(), t);
        }
    }

    /**
     * Return the plugin manager that is managing alert sender plugins
     * @return The alert sender plugin manager
     */
    public AlertSenderPluginManager getAlertPluginManager() {
        MasterServerPluginContainer container = LookupUtil.getServerPluginService().getMasterPluginContainer();
        if (container == null) {
            log.warn(MasterServerPluginContainer.class.getSimpleName() + " is not started yet");
            return null;
        }
        AlertServerPluginContainer pc = container.getPluginContainerByClass(AlertServerPluginContainer.class);
        if (pc == null) {
            log.warn(AlertServerPluginContainer.class.getSimpleName() + " has not been loaded by the "
                + MasterServerPluginContainer.class.getSimpleName() + " yet");
            return null;
        }
        AlertSenderPluginManager manager = (AlertSenderPluginManager) pc.getPluginManager();
        return manager;
    }

    /**
     *
     * @param alert
     * @param emailAddresses
     * @return
     */
    private Collection<String> sendAlertNotificationEmails(Alert alert, Set<String> emailAddresses) {

        if (emailAddresses.size() == 0)
            return new ArrayList<String>(0); // No email to send -> no bad addresses

        log.debug("Sending alert notifications for " + alert.toSimpleString() + "...");

        AlertDefinition alertDefinition = alert.getAlertDefinition();
        Map<String, String> alertMessage = emailManager.getAlertEmailMessage(
            prettyPrintResourceHierarchy(alertDefinition.getResource()), alertDefinition.getResource().getName(),
            alertDefinition.getName(), alertDefinition.getPriority().toString(), new Date(alert.getCtime()).toString(),
            prettyPrintAlertConditions(alert.getConditionLogs(), false), prettyPrintAlertURL(alert));
        String messageSubject = alertMessage.keySet().iterator().next();
        String messageBody = alertMessage.values().iterator().next();

        Collection<String> badAddresses;
        badAddresses = emailManager.sendEmail(emailAddresses, messageSubject, messageBody);
        if (log.isDebugEnabled()) {
            if (badAddresses.isEmpty())
                log.debug("All notifications for " + alert.toSimpleString() + " succeeded");
            else
                log.debug("Sending email notifications for " + badAddresses + " failed");
        }
        return badAddresses;
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

    /**
     * Create a human readable description of the conditions that led to this alert.
     * @param alert Alert to create human readable condition description
     * @param shortVersion if true the messages printed are abbreviated to save space
     * @return human readable condition log
     */
    public String prettyPrintAlertConditions(Alert alert, boolean shortVersion) {
        return prettyPrintAlertConditions(alert.getConditionLogs(), shortVersion);
    }

    private String prettyPrintAlertConditions(Set<AlertConditionLog> conditionLogs, boolean shortVersion) {
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

            String format;
            if (shortVersion)
                format = AlertI18NResourceKeys.ALERT_EMAIL_CONDITION_LOG_FORMAT_SHORT;
            else
                format = AlertI18NResourceKeys.ALERT_EMAIL_CONDITION_LOG_FORMAT;
            SimpleDateFormat dateFormat;
            if (shortVersion)
                dateFormat = new SimpleDateFormat("yy/MM/dd HH:mm:ss z");
            else
                dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
            builder.append(AlertI18NFactory.getMessage(format, conditionCounter, prettyPrintAlertCondition(aLog
                .getCondition(), shortVersion), dateFormat.format(new Date(aLog.getCtime())), formattedValue));
            conditionCounter++;
        }

        return builder.toString();
    }

    private String prettyPrintAlertCondition(AlertCondition condition, boolean shortVersion) {
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
            if (category.getName() != null) // this is null for e.g. availability
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

            if (shortVersion)
                builder.append(AlertI18NFactory
                    .getMessage(AlertI18NResourceKeys.ALERT_CURRENT_LIST_VALUE_CHANGED_SHORT));
            else
                builder.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_CURRENT_LIST_VALUE_CHANGED));

        } else if (category == AlertConditionCategory.EVENT) {
            if ((condition.getOption() != null) && (condition.getOption().length() > 0)) {
                String propsCbEventSeverityRegexMatch;
                if (shortVersion)
                    propsCbEventSeverityRegexMatch = AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY_REGEX_MATCH_SHORT;
                else
                    propsCbEventSeverityRegexMatch = AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY_REGEX_MATCH;

                builder.append(AlertI18NFactory.getMessage(propsCbEventSeverityRegexMatch, condition.getName(),
                    condition.getOption()));
            } else {
                if (shortVersion)
                    builder.append(AlertI18NFactory.getMessage(
                        AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY_SHORT, condition.getName()));
                else
                    builder.append(AlertI18NFactory.getMessage(
                        AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_EVENT_SEVERITY, condition.getName()));
            }
        } else if (category == AlertConditionCategory.AVAILABILITY) {
            if (shortVersion)
                builder.append(AlertI18NFactory.getMessage(
                    AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_AVAILABILITY_SHORT, condition.getOption()));
            else
                builder.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_CONFIG_PROPS_CB_AVAILABILITY,
                    condition.getOption()));
        } else {
            // do nothing
        }

        return builder.toString();
    }

    public String prettyPrintAlertURL(Alert alert) {
        StringBuilder builder = new StringBuilder();

        String baseUrl = systemManager.getSystemConfiguration().getProperty(RHQConstants.BaseURL);
        builder.append(baseUrl);
        if (!baseUrl.endsWith("/")) {
            builder.append("/");
        }

        builder.append("alerts/Alerts.do?mode=viewAlert");

        builder.append("&id=").append(alert.getAlertDefinition().getResource().getId());
        builder.append("&a=").append(alert.getId());

        return builder.toString();
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

    /**
     * Tells us if the definition of the passed alert will be disabled after this alert was fired
     * @param alert alert to check
     * @return true if the definition got disabled
     */
    public boolean willDefinitionBeDisabled(Alert alert) {
        entityManager.refresh(alert);
        AlertDefinition firedDefinition = alert.getAlertDefinition();
        Subject overlord = subjectManager.getOverlord();
        Integer recoveryDefinitionId = firedDefinition.getRecoveryId();

        if (recoveryDefinitionId != 0) {
            AlertDefinition toBeRecoveredDefinition = alertDefinitionManager.getAlertDefinitionById(overlord,
                recoveryDefinitionId);
            boolean wasEnabled = toBeRecoveredDefinition.getEnabled();
            if (!wasEnabled)
                return false;
        } else if (firedDefinition.getWillRecover()) {
            return true;
        }
        return false; // Default is not to disable the definition
    }

    @SuppressWarnings("unchecked")
    public PageList<Alert> findAlertsByCriteria(Subject subject, AlertCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "alertDefinition.resource", subject.getId());
        }

        CriteriaQueryRunner<Alert> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        PageList<Alert> alerts = queryRunner.execute();

        fetchCollectionFields(alerts);

        return alerts;
    }
}
