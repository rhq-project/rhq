/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.alert;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.QueryImpl;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceAncestryFormat;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.server.MeasurementConverter;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.i18n.AlertI18NFactory;
import org.rhq.enterprise.server.alert.i18n.AlertI18NResourceKeys;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.common.ApplicationException;
import org.rhq.enterprise.server.core.EmailManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.plugin.pc.alert.AlertServerPluginContainer;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.BatchIterator;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 * @author Ian Springer
 */
@Stateless
public class AlertManagerBean implements AlertManagerLocal, AlertManagerRemote {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private final Log log = LogFactory.getLog(AlertManagerBean.class);

    @EJB
    //@IgnoreDependency
    private AlertConditionLogManagerLocal alertConditionLogManager;
    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    private AlertManagerLocal alertManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    //@IgnoreDependency
    private ResourceManagerLocal resourceManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private SystemManagerLocal systemManager;
    @EJB
    //@IgnoreDependency
    private OperationManagerLocal operationManager;
    @EJB
    private EmailManagerLocal emailManager;

    @Override
    public int deleteAlerts(Subject user, int[] alertIds) {
        if (alertIds == null || alertIds.length == 0) {
            return 0;
        }

        List<Integer> alertIdList = ArrayUtils.wrapInList(alertIds);

        checkAlertsPermission(user, alertIdList);

        Query deleteConditionLogsQuery = entityManager.createNamedQuery(AlertConditionLog.QUERY_DELETE_BY_ALERT_IDS);
        Query deleteNotifLogsQuery = entityManager.createNamedQuery(AlertNotificationLog.QUERY_DELETE_BY_ALERT_IDS);
        Query deleteAlertsQuery = entityManager.createNamedQuery(Alert.QUERY_DELETE_BY_IDS);

        int updated = 0;
        BatchIterator<Integer> batchIter = new BatchIterator<Integer>(alertIdList);
        for (List<Integer> nextBatch : batchIter) {
            // need to delete related objects before deleting alerts
            deleteConditionLogsQuery.setParameter("alertIds", nextBatch);
            deleteConditionLogsQuery.executeUpdate();
            deleteNotifLogsQuery.setParameter("alertIds", nextBatch);
            deleteNotifLogsQuery.executeUpdate();

            // now we can delete alerts
            deleteAlertsQuery.setParameter("alertIds", nextBatch);
            updated += deleteAlertsQuery.executeUpdate();
        }
        return updated;
    }

    /**
     * Acknowledge alert(s) so that administrators know who is working on remedying the underlying
     * condition(s) that caused the alert(s) in the first place.
     *
     * @param subject calling user
     * @param alertIds PKs of the alerts to acknowledge
     * @return number of alerts acknowledged
     */
    @Override
    public int acknowledgeAlerts(Subject subject, int[] alertIds) {
        if (alertIds == null || alertIds.length == 0) {
            return 0;
        }

        List<Integer> alertIdList = ArrayUtils.wrapInList(alertIds);

        checkAlertsPermission(subject, alertIdList);

        Query ackAlertsQuery = entityManager.createNamedQuery(Alert.QUERY_ACKNOWLEDGE_BY_IDS);
        ackAlertsQuery.setParameter("subjectName", subject.getName());
        ackAlertsQuery.setParameter("ackTime", System.currentTimeMillis());

        int modified = 0;
        BatchIterator<Integer> batchIter = new BatchIterator<Integer>(alertIdList);
        for (List<Integer> nextBatch : batchIter) {
            ackAlertsQuery.setParameter("alertIds", nextBatch);
            modified += ackAlertsQuery.executeUpdate();
        }
        return modified;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public int deleteAlertsByContext(Subject subject, EntityContext context) {
        Query deleteConditionLogsQuery = null;
        Query deleteNotificationLogsQuery = null;
        Query deleteAlertsQuery = null;

        if (context.type == EntityContext.Type.Resource) {
            if (!authorizationManager.hasResourcePermission(subject, Permission.MANAGE_ALERTS, context.resourceId)) {
                throw new PermissionException("Can not delete alerts - " + subject + " lacks "
                    + Permission.MANAGE_ALERTS + " for resource[id=" + context.resourceId + "]");
            }
            deleteConditionLogsQuery = entityManager.createNamedQuery(AlertConditionLog.QUERY_DELETE_BY_RESOURCES);
            deleteConditionLogsQuery.setParameter("resourceIds", Arrays.asList(context.resourceId));

            deleteNotificationLogsQuery = entityManager
                .createNamedQuery(AlertNotificationLog.QUERY_DELETE_BY_RESOURCES);
            deleteNotificationLogsQuery.setParameter("resourceIds", Arrays.asList(context.resourceId));

            deleteAlertsQuery = entityManager.createNamedQuery(Alert.QUERY_DELETE_BY_RESOURCES);
            deleteAlertsQuery.setParameter("resourceIds", Arrays.asList(context.resourceId));

        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (!authorizationManager.hasGroupPermission(subject, Permission.MANAGE_ALERTS, context.groupId)) {
                throw new PermissionException("Can not delete alerts - " + subject + " lacks "
                    + Permission.MANAGE_ALERTS + " for group[id=" + context.groupId + "]");
            }
            deleteConditionLogsQuery = entityManager
                .createNamedQuery(AlertConditionLog.QUERY_DELETE_BY_RESOURCE_GROUPS);
            deleteConditionLogsQuery.setParameter("groupIds", Arrays.asList(context.groupId));

            deleteNotificationLogsQuery = entityManager
                .createNamedQuery(AlertNotificationLog.QUERY_DELETE_BY_RESOURCE_GROUPS);
            deleteNotificationLogsQuery.setParameter("groupIds", Arrays.asList(context.groupId));

            deleteAlertsQuery = entityManager.createNamedQuery(Alert.QUERY_DELETE_BY_RESOURCE_GROUPS);
            deleteAlertsQuery.setParameter("groupIds", Arrays.asList(context.groupId));

        } else if (context.type == EntityContext.Type.SubsystemView) {
            if (!authorizationManager.isInventoryManager(subject)) {
                throw new PermissionException("Can not delete alerts - " + subject + " lacks "
                    + Permission.MANAGE_INVENTORY + " for global alerts history");
            }
            deleteConditionLogsQuery = entityManager.createNamedQuery(AlertConditionLog.QUERY_DELETE_ALL);
            deleteNotificationLogsQuery = entityManager.createNamedQuery(AlertNotificationLog.QUERY_DELETE_ALL);
            deleteAlertsQuery = entityManager.createNamedQuery(Alert.QUERY_DELETE_ALL);
        } else if (context.type == EntityContext.Type.ResourceTemplate) {
            // TODO Need to determine what security check(s) need to be performed here
            deleteAlertsQuery = entityManager.createNamedQuery(Alert.QUERY_DELETE_BY_RESOURCE_TEMPLATE);
            deleteAlertsQuery.setParameter("resourceTypeId", context.resourceTypeId);

            deleteConditionLogsQuery = entityManager
                .createNamedQuery(AlertConditionLog.QUERY_DELETE_BY_RESOURCE_TEMPLATE);
            deleteConditionLogsQuery.setParameter("resourceTypeId", context.resourceTypeId);

            deleteNotificationLogsQuery = entityManager
                .createNamedQuery(AlertNotificationLog.QUERY_DELETE_BY_RESOURCE_TEMPLATE);
            deleteNotificationLogsQuery.setParameter("resourceTypeId", context.resourceTypeId);

        } else {
            throw new IllegalArgumentException("No support for deleting alerts for " + context);
        }

        deleteConditionLogsQuery.executeUpdate();
        deleteNotificationLogsQuery.executeUpdate();
        int affectedRows = deleteAlertsQuery.executeUpdate();
        return affectedRows;
    }

    @Override
    public int acknowledgeAlertsByContext(Subject subject, EntityContext context) {
        Query query = null;
        if (context.type == EntityContext.Type.Resource) {
            if (!authorizationManager.hasResourcePermission(subject, Permission.MANAGE_ALERTS, context.resourceId)) {
                throw new PermissionException("Can not acknowledge alerts - " + subject + " lacks "
                    + Permission.MANAGE_ALERTS + " for resource[id=" + context.resourceId + "]");
            }
            query = entityManager.createNamedQuery(Alert.QUERY_ACKNOWLEDGE_BY_RESOURCES);
            query.setParameter("resourceIds", Arrays.asList(context.resourceId));

        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (!authorizationManager.hasGroupPermission(subject, Permission.MANAGE_ALERTS, context.groupId)) {
                throw new PermissionException("Can not acknowledge alerts - " + subject + " lacks "
                    + Permission.MANAGE_ALERTS + " for group[id=" + context.groupId + "]");
            }
            query = entityManager.createNamedQuery(Alert.QUERY_ACKNOWLEDGE_BY_RESOURCE_GROUPS);
            query.setParameter("groupIds", Arrays.asList(context.groupId));

        } else if (context.type == EntityContext.Type.SubsystemView) {
            if (!authorizationManager.isInventoryManager(subject)) {
                throw new PermissionException("Can not acknowledge alerts - " + subject + " lacks "
                    + Permission.MANAGE_INVENTORY + " for global alerts history");
            }
            query = entityManager.createNamedQuery(Alert.QUERY_ACKNOWLEDGE_ALL);
        } else {
            throw new IllegalArgumentException("No support for acknowledging alerts for " + context);
        }

        query.setParameter("subjectName", subject.getName());
        query.setParameter("ackTime", System.currentTimeMillis());

        int affectedRows = query.executeUpdate();
        return affectedRows;
    }

    // TODO: if user passes an alertId that doesn't exist, it will generate a permission exception
    //       because the query will think the user does not have access to the corresponding resource.
    //       we need another check that ensures all alertIds exist first, or perhaps code that removes
    //       and/or gracefully ignores the ones that don't exist
    //
    // TODO: need to break up this query and iterate in blocks of 1000 ids at a time, to avoid oracle
    //       in-clause issues
    private void checkAlertsPermission(Subject subject, List<Integer> alertIds) {
        if (authorizationManager.isInventoryManager(subject)) {
            return; // inventory manager
        }

        long canModifyCount = checkAuthz(subject, alertIds);
        long canNotModifyCount = alertIds.size() - canModifyCount;

        if (canNotModifyCount != 0) {
            /*
             * implies one of two things:
             *    1) user does not have permission to modify alerts for some of the corresponding resources
             *    2) some of the passed alertIds do not exist
             *
             * to remedy this, let's remove alertIds that no longer exist.  if the new list is smaller than the
             * original list, we know that the list DID contain non-existent entries and we should perform the authz
             * check again.  however, if all of the elements in the original list existed, then we know that the
             * original authz check was valid, and we should throw the necessary PermissionException
             */

            List<Integer> validAlertIds = removeNonExistent(alertIds);
            if (validAlertIds.size() == alertIds.size()) {
                throw new PermissionException(subject + " does not have permission to delete " + canNotModifyCount
                    + " of the " + alertIds.size() + " passsed alertIds");
            } else {
                canModifyCount = checkAuthz(subject, alertIds);
                canNotModifyCount = alertIds.size() - canModifyCount;
                if (canNotModifyCount != 0) {
                    throw new PermissionException(subject + " does not have permission to delete " + canNotModifyCount
                        + " of the " + alertIds.size() + " passsed alertIds");
                }
            }

        }
    }

    private long checkAuthz(Subject subject, List<Integer> alertIds) {
        /*
         * get the count of the number of these alerts for which user
         * has MANAGE_ALERTS permission on the corresponding resource
         */
        Query authzQuery = entityManager.createNamedQuery(Alert.QUERY_CHECK_PERMISSION_BY_IDS);
        authzQuery.setParameter("subjectId", subject.getId());
        authzQuery.setParameter("permission", Permission.MANAGE_ALERTS);

        long canModifyCount = 0;
        BatchIterator<Integer> batchIter = new BatchIterator<Integer>(alertIds);
        for (List<Integer> nextBatch : batchIter) {
            authzQuery.setParameter("alertIds", nextBatch);
            canModifyCount += (Long) authzQuery.getSingleResult();
        }

        return canModifyCount;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> removeNonExistent(List<Integer> alertIds) {
        /*
         * get the count of the number of these alerts for which user
         * has MANAGE_ALERTS permission on the corresponding resource
         */
        Query authzQuery = entityManager.createNamedQuery(Alert.QUERY_RETURN_EXISTING_IDS);

        List<Integer> existingAlertIds = new ArrayList<Integer>();
        BatchIterator<Integer> batchIter = new BatchIterator<Integer>(alertIds);
        for (List<Integer> nextBatch : batchIter) {
            authzQuery.setParameter("alertIds", nextBatch);
            existingAlertIds.addAll(authzQuery.getResultList());
        }
        return existingAlertIds;
    }

    @Override
    public int getAlertCountByMeasurementDefinitionId(Integer measurementDefinitionId, long begin, long end) {
        Query query = PersistenceUtility.createCountQuery(entityManager, Alert.QUERY_FIND_BY_MEASUREMENT_DEFINITION_ID);
        query.setParameter("measurementDefinitionId", measurementDefinitionId);
        query.setParameter("begin", begin);
        query.setParameter("end", end);
        long count = (Long) query.getSingleResult();
        return (int) count;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Alert fireAlert(int alertDefinitionId) {
        if (log.isDebugEnabled()) {
            log.debug("Firing an alert for alertDefinition with id=" + alertDefinitionId + "...");
        }

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
        entityManager.persist(newAlert);
        if (log.isDebugEnabled()) {
            log.debug("New alert identifier=" + newAlert.getId());
        }

        //        AlertNotificationLog alertNotifLog = new AlertNotificationLog(newAlert);  TODO - is that all?
        //        entityManager.persist(alertNotifLog);

        List<AlertConditionLog> unmatchedConditionLogs = alertConditionLogManager
            .getUnmatchedLogsByAlertDefinitionId(alertDefinitionId);
        for (AlertConditionLog unmatchedLog : unmatchedConditionLogs) {
            if (log.isDebugEnabled()) {
                log.debug("Matched alert condition log for alertId=" + newAlert.getId() + ": " + unmatchedLog);
            }
            newAlert.addConditionLog(unmatchedLog); // adds both relationships
        }

        // process recovery actions
        processRecovery(alertDefinition);

        return newAlert;
    }

    /**
     * This is the core of the alert sending process. For each AlertNotification that is hanging
     * on the alerts definition, the sender is instantiated and its send() method called. If a sender
     * returns a list of email addresses, those will be collected and sent at the end.
     * @param alert the fired alert
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendAlertNotificationsNSTx(Alert alert) {
        /*
         * make this method public in case we show the notification failure to the user in the UI in the future and want
         * to give them some way to explicitly try to re-send the notification for some client-side auditing purposes
         */
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending alert notifications for " + alert.toSimpleString() + "...");
            }
            List<AlertNotification> alertNotifications = alert.getAlertDefinition().getAlertNotifications();

            if (alertNotifications != null && alertNotifications.size() > 0) {
                AlertSenderPluginManager alertSenderPluginManager = getAlertPluginManager();

                for (AlertNotification alertNotification : alertNotifications) {
                    try {
                        AlertNotificationLog notificationLog = null;

                        String senderName = alertNotification.getSenderName();
                        if (alertSenderPluginManager == null) {
                            notificationLog = new AlertNotificationLog(alert, senderName, ResultState.FAILURE,
                                "Notification was not sent as alert sender plugins are not yet initialized ");

                        } else if (senderName == null) {
                            notificationLog = new AlertNotificationLog(alert, senderName, ResultState.FAILURE,
                                "Sender '" + senderName + "' is not defined");

                        } else {
                            AlertSender<?> notificationSender = alertSenderPluginManager
                                .getAlertSenderForNotification(alertNotification);
                            if (notificationSender == null) {
                                notificationLog = new AlertNotificationLog(alert, senderName, ResultState.FAILURE,
                                    "Failed to obtain a sender with given name");
                            } else {
                                try {
                                    SenderResult result = notificationSender.send(alert);
                                    if (log.isDebugEnabled()) {
                                        log.debug(result);
                                    }

                                    if (result == null) {
                                        notificationLog = new AlertNotificationLog(alert, senderName,
                                            ResultState.UNKNOWN, "Sender did not return any result");
                                    } else {
                                        notificationLog = new AlertNotificationLog(alert, senderName, result);
                                    }
                                } catch (Throwable t) {
                                    log.error("Notification processing terminated abruptly" + t.getMessage());
                                    notificationLog = new AlertNotificationLog(alert, senderName, ResultState.FAILURE,
                                        "Notification processing terminated abruptly, cause: " + t.getMessage());
                                }
                            }
                        }

                        alertManager.addNotificationLog(alert.getId(), notificationLog);

                    } catch (Throwable t) {
                        log.error(
                            "Failed to send notification [" + alertNotification + "] for [" + alert.toSimpleString()
                                + "].", t);
                    }
                }
            }
        } catch (Throwable t) {
            log.error("Failed to send all notifications for [" + alert.toSimpleString() + "].", t);
        }
    }

    public void addNotificationLog(int alertId, AlertNotificationLog notificationLog) {
        Alert alert = entityManager.find(Alert.class, alertId);
        if (null == alert) {
            throw new ApplicationException("Alert not found with id [" + alertId + "].");
        }

        // make sure we don't exceed the max message length for the db vendor
        DatabaseType dbType = DatabaseTypeFactory.getDefaultDatabaseType();
        String message = dbType.getString(notificationLog.getMessage(), AlertNotificationLog.MESSAGE_MAX_LENGTH);

        if (null != message && !message.equals(notificationLog.getMessage())) {
            notificationLog = new AlertNotificationLog(notificationLog.getAlert(), notificationLog.getSender(),
                notificationLog.getResultState(), message);
        }

        entityManager.persist(notificationLog);
        alert.addAlertNotificatinLog(notificationLog);
    }

    /**
     * Return the plugin manager that is managing alert sender plugins
     * @return The alert sender plugin manager
     */
    @Override
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

    @Override
    public Collection<String> sendAlertNotificationEmails(Alert alert, Collection<String> emailAddresses) {
        if (log.isDebugEnabled()) {
            log.debug("Sending alert notifications for " + alert.toSimpleString() + "...");
        }

        if (emailAddresses.size() == 0) {
            return new ArrayList<String>(0); // No email to send -> no bad addresses
        }

        AlertDefinition alertDefinition = alert.getAlertDefinition();
        Resource resource = alertDefinition.getResource();
        Map<Integer, String> ancestry = resourceManager.getResourcesAncestry(subjectManager.getOverlord(),
            new Integer[] { resource.getId() }, ResourceAncestryFormat.VERBOSE);
        Map<String, String> alertMessage = emailManager.getAlertEmailMessage(ancestry.get(resource.getId()), //
            resource.getName(), //
            alertDefinition.getName(), //
            alertDefinition.getPriority().toString(), //
            new Date(alert.getCtime()).toString(), //
            prettyPrintAlertConditions(alert.getConditionLogs(), false), //
            prettyPrintAlertURL(alert));

        String messageSubject = alertMessage.keySet().iterator().next();
        String messageBody = alertMessage.values().iterator().next();

        Set<String> uniqueAddresses = new HashSet<String>(emailAddresses);
        Collection<String> badAddresses = emailManager.sendEmail(uniqueAddresses, messageSubject, messageBody);

        if (log.isDebugEnabled()) {
            if (badAddresses.isEmpty()) {
                log.debug("All notifications for " + alert.toSimpleString() + " succeeded");
            } else {
                log.debug("Sending email notifications for " + badAddresses + " failed");
            }
        }

        return badAddresses;
    }

    private static String NEW_LINE = System.getProperty("line.separator");

    /**
     * Create a human readable description of the conditions that led to this alert.
     * @param alert Alert to create human readable condition description
     * @param shortVersion if true the messages printed are abbreviated to save space
     * @return human readable condition log
     */
    @Override
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
            builder.append(AlertI18NFactory.getMessage(format, conditionCounter,
                prettyPrintAlertCondition(aLog.getCondition(), shortVersion),
                dateFormat.format(new Date(aLog.getCtime())), formattedValue));
            conditionCounter++;
        }

        return builder.toString();
    }

    private String prettyPrintAlertCondition(AlertCondition condition, boolean isShort) {
        StringBuilder str = new StringBuilder();

        AlertConditionCategory category = condition.getCategory();
        switch (category) {
        case AVAILABILITY: {
            AlertConditionOperator operator = AlertConditionOperator.valueOf(condition.getName().toUpperCase());
            String msg;
            switch (operator) {
            case AVAIL_GOES_DISABLED:
                msg = isShort ? AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_DISABLED_SHORT
                    : AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_DISABLED;
                break;
            case AVAIL_GOES_DOWN:
                msg = isShort ? AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_DOWN_SHORT
                    : AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_DOWN;
                break;
            case AVAIL_GOES_UNKNOWN:
                msg = isShort ? AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_UNKNOWN_SHORT
                    : AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_UNKNOWN;
                break;
            case AVAIL_GOES_UP:
                msg = isShort ? AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_UP_SHORT
                    : AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_UP;
                break;
            case AVAIL_GOES_NOT_UP:
            default:
                msg = isShort ? AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_NOT_UP_SHORT
                    : AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_NOT_UP;
                break;
            }
            str.append(AlertI18NFactory.getMessage(msg));

            break;
        }

        case AVAIL_DURATION: {
            AlertConditionOperator operator = AlertConditionOperator.valueOf(condition.getName().toUpperCase());
            String msg;
            switch (operator) {
            case AVAIL_DURATION_DOWN:
                msg = isShort ? AlertI18NResourceKeys.ALERT_AVAILABILITY_DURATION_DOWN_SHORT
                    : AlertI18NResourceKeys.ALERT_AVAILABILITY_DURATION_DOWN;
                break;
            case AVAIL_DURATION_NOT_UP:
            default:
                msg = isShort ? AlertI18NResourceKeys.ALERT_AVAILABILITY_DURATION_NOT_UP_SHORT
                    : AlertI18NResourceKeys.ALERT_AVAILABILITY_DURATION_NOT_UP;
                break;
            }
            str.append(AlertI18NFactory.getMessage(msg));
            str.append(" [");

            // stored in seconds but present in minutes
            String value = String.valueOf(Integer.valueOf(condition.getOption()) / 60);
            String formatted = MeasurementConverter.format(value, MeasurementUnits.MINUTES);
            str.append(formatted);
            str.append("]");

            break;
        }

        case THRESHOLD: {
            double value = condition.getThreshold();
            MeasurementUnits units = condition.getMeasurementDefinition().getUnits();
            String formatted = MeasurementConverter.format(value, units, true);

            if (condition.getOption() == null) {
                String metricName = condition.getName();
                String comparator = condition.getComparator();
                str.append(metricName).append(' ').append(comparator).append(' ').append(formatted);
            } else {
                // this is a calltime threshold condition
                String metricName = "";
                if (condition.getMeasurementDefinition() != null) {
                    metricName = condition.getMeasurementDefinition().getDisplayName();
                }

                String limit = condition.getOption(); // MIN, MAX, AVG (never null)
                String comparator = condition.getComparator(); // <, >, =

                if (condition.getName() != null && condition.getName().length() > 0) {
                    String regex = condition.getName();
                    if (isShort) {
                        str.append(AlertI18NFactory.getMessage(
                            AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_THRESHOLD_WITH_EXPR_SHORT, metricName, limit,
                            comparator, formatted, regex));
                    } else {
                        str.append(AlertI18NFactory.getMessage(
                            AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_THRESHOLD_WITH_EXPR, metricName, limit,
                            comparator, formatted, regex));
                    }
                } else {
                    if (isShort) {
                        str.append(AlertI18NFactory.getMessage(
                            AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_THRESHOLD_SHORT, metricName, limit, comparator,
                            formatted));
                    } else {
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_THRESHOLD,
                            metricName, limit, comparator, formatted));
                    }
                }
            }
            break;
        }
        case BASELINE: {
            String metricName = condition.getName();
            String comparator = condition.getComparator();
            double value = condition.getThreshold();
            MeasurementUnits units = MeasurementUnits.PERCENTAGE;
            String percentage = MeasurementConverter.format(value, units, true);
            String baselineThreshold = condition.getOption(); // mean, min, max

            if (isShort) {
                if (baselineThreshold.equalsIgnoreCase("min")) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_BASELINE_MIN_SHORT, metricName,
                        comparator, percentage));
                } else if (baselineThreshold.equalsIgnoreCase("max")) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_BASELINE_MAX_SHORT, metricName,
                        comparator, percentage));
                } else { // mean
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_BASELINE_MEAN_SHORT, metricName,
                        comparator, percentage));
                }
            } else {
                if (baselineThreshold.equalsIgnoreCase("min")) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_BASELINE_MIN, metricName,
                        comparator, percentage));
                } else if (baselineThreshold.equalsIgnoreCase("max")) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_BASELINE_MAX, metricName,
                        comparator, percentage));
                } else { // mean
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_BASELINE_MEAN, metricName,
                        comparator, percentage));
                }
            }
            break;
        }
        case CHANGE: {
            if (condition.getOption() == null) {
                String metricName = condition.getName();
                if (isShort) {
                    str.append(AlertI18NFactory
                        .getMessage(AlertI18NResourceKeys.ALERT_METRIC_CHANGED_SHORT, metricName));
                } else {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CHANGED, metricName));
                }
            } else {
                // this is a calltime change condition
                double value = condition.getThreshold();
                MeasurementUnits units = MeasurementUnits.PERCENTAGE;
                String formatted = MeasurementConverter.format(value, units, true);

                String comparator;
                if ("HI".equalsIgnoreCase(condition.getComparator())) {
                    comparator = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_GROWS);
                } else if ("LO".equalsIgnoreCase(condition.getComparator())) {
                    comparator = AlertI18NFactory
                        .getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_SHRINKS);
                } else { // CH
                    comparator = AlertI18NFactory
                        .getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_CHANGES);
                }

                String metricName = "";
                if (condition.getMeasurementDefinition() != null) {
                    metricName = condition.getMeasurementDefinition().getDisplayName();
                }

                String limit = condition.getOption(); // MIN, MAX, AVG (never null)

                if (condition.getName() != null && condition.getName().length() > 0) {
                    String regex = condition.getName();
                    if (isShort) {
                        str.append(AlertI18NFactory.getMessage(
                            AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_WITH_EXPR_SHORT, metricName, limit,
                            comparator, formatted, regex));
                    } else {
                        str.append(AlertI18NFactory.getMessage(
                            AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_WITH_EXPR, metricName, limit,
                            comparator, formatted, regex));
                    }
                } else {
                    if (isShort) {
                        str.append(AlertI18NFactory.getMessage(
                            AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_SHORT, metricName, limit, comparator,
                            formatted));
                    } else {
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE,
                            metricName, limit, comparator, formatted));
                    }
                }
            }
            break;
        }
        case TRAIT: {
            String metricName = condition.getName();
            String expression = condition.getOption();

            if (expression != null && !expression.isEmpty()) {
                if (isShort) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CHANGED_WITH_EXPR_SHORT,
                        metricName, expression));
                } else {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CHANGED_WITH_EXPR,
                        metricName, expression));
                }
            } else {
                if (isShort) {
                    str.append(AlertI18NFactory
                        .getMessage(AlertI18NResourceKeys.ALERT_METRIC_CHANGED_SHORT, metricName));
                } else {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CHANGED, metricName));
                }
            }
            break;
        }
        case CONTROL: {
            String opName;
            try {
                Integer resourceTypeId = condition.getAlertDefinition().getResource().getResourceType().getId();
                String operationName = condition.getName();

                OperationDefinition definition = operationManager.getOperationDefinitionByResourceTypeAndName(
                    resourceTypeId, operationName, false);
                opName = definition.getDisplayName();
            } catch (Exception e) {
                opName = condition.getName(); // can't look up the op display name (are we in a test?), just use the op name
            }

            String status = condition.getOption();
            if (isShort) {
                str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_OPERATION_SHORT, opName, status));
            } else {
                str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_OPERATION, opName, status));
            }

            break;
        }
        case RESOURCE_CONFIG: {
            if (isShort) {
                str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RESOURCECONFIGCHANGE_SHORT));
            } else {
                str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RESOURCECONFIGCHANGE));
            }
            break;
        }
        case EVENT: {
            String severity = condition.getName();
            if (condition.getOption() != null && condition.getOption().length() > 0) {
                String expression = condition.getOption();
                String regexEventDetails = "", regexSourceLocation = "";
                if (expression.contains(AlertCondition.ADHOC_SEPARATOR)) {
                    String[] regexes = expression.split(AlertCondition.ADHOC_SEPARATOR);
                    if (regexes.length > 0) {
                        regexEventDetails = regexes[0];
                        if (regexes.length > 1) {
                            regexSourceLocation = regexes[1];
                        }
                    }
                } else {
                    regexEventDetails = expression; // old approach -> probably working with db before rhq 4.13
                }

                if (isShort) {
                    if (!regexEventDetails.isEmpty()) {
                        if (!regexSourceLocation.isEmpty()) {
                            str.append(AlertI18NFactory.getMessage(
                                AlertI18NResourceKeys.ALERT_EVENT_WITH_EXPR_WITH_SOURCE_SHORT, severity,
                                regexEventDetails, regexSourceLocation));
                        } else {
                            str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EVENT_WITH_EXPR_SHORT,
                                severity, regexEventDetails));
                        }
                    } else if (!regexSourceLocation.isEmpty()) {
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EVENT_WITH_SOURCE_SHORT,
                            severity, regexSourceLocation));
                    } else {
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EVENT_SHORT, severity));
                    }
                } else {
                    if (!regexEventDetails.isEmpty()) {
                        if (!regexSourceLocation.isEmpty()) {
                            str.append(AlertI18NFactory.getMessage(
                                AlertI18NResourceKeys.ALERT_EVENT_WITH_EXPR_WITH_SOURCE, severity, regexEventDetails,
                                regexSourceLocation));
                        } else {
                            str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EVENT_WITH_EXPR,
                                severity, regexEventDetails));
                        }
                    } else if (!regexSourceLocation.isEmpty()) {
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EVENT_WITH_SOURCE, severity,
                            regexSourceLocation));
                    } else {
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EVENT, severity));
                    }
                }
            } else {
                if (isShort) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EVENT_SHORT, severity));
                } else {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_EVENT, severity));
                }
            }
            break;
        }
        case DRIFT: {
            String configNameRegex = condition.getName();
            String pathNameRegex = condition.getOption();
            if (isShort) {
                if (configNameRegex == null || configNameRegex.length() == 0) {
                    if (pathNameRegex == null || pathNameRegex.length() == 0) {
                        // neither a config name regex nor path regex was specified
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_DRIFT_SHORT));
                    } else {
                        // a path name regex was specified, but not a config name regex
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_DRIFT_ONLYPATHS_SHORT,
                            pathNameRegex));
                    }
                } else {
                    if (pathNameRegex == null || pathNameRegex.length() == 0) {
                        // a config name regex was specified, but not a path name regex
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_DRIFT_ONLYCONFIG_SHORT,
                            configNameRegex));
                    } else {
                        // both a config name regex and a path regex was specified
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_DRIFT_CONFIGPATHS_SHORT,
                            pathNameRegex, configNameRegex));
                    }
                }
            } else {
                if (configNameRegex == null || configNameRegex.length() == 0) {
                    if (pathNameRegex == null || pathNameRegex.length() == 0) {
                        // neither a config name regex nor path regex was specified
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_DRIFT));
                    } else {
                        // a path name regex was specified, but not a config name regex
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_DRIFT_ONLYPATHS,
                            pathNameRegex));
                    }
                } else {
                    if (pathNameRegex == null || pathNameRegex.length() == 0) {
                        // a config name regex was specified, but not a path name regex
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_DRIFT_ONLYCONFIG,
                            configNameRegex));
                    } else {
                        // both a config name regex and a path regex was specified
                        str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_DRIFT_CONFIGPATHS,
                            pathNameRegex, configNameRegex));
                    }
                }
            }
            break;
        }
        case RANGE: {
            String metricName = condition.getName();
            Double loValue = condition.getThreshold();
            String hiValueStr = condition.getOption();

            String loValueFormatted;
            String hiValueFormatted;
            MeasurementUnits units = condition.getMeasurementDefinition().getUnits();
            if (units == null) {
                loValueFormatted = "" + loValue;
                hiValueFormatted = hiValueStr;
            } else {
                loValueFormatted = MeasurementConverter.format(loValue, units, true);
                try {
                    hiValueFormatted = MeasurementConverter.format(Double.parseDouble(hiValueStr), units, true);
                } catch (Exception e) {
                    hiValueFormatted = "?[" + hiValueStr + "]?";
                }
            }

            // < means "inside the range", > means "outside the range" - exclusive
            // <= means "inside the range", >= means "outside the range" - inclusive

            if ("<".equals(condition.getComparator())) {
                if (isShort) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RANGE_INSIDE_EXCL_SHORT,
                        metricName, loValueFormatted, hiValueFormatted));
                } else {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RANGE_INSIDE_EXCL, metricName,
                        loValueFormatted, hiValueFormatted));
                }
            } else if (">".equals(condition.getComparator())) {
                if (isShort) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RANGE_OUTSIDE_EXCL_SHORT,
                        metricName, loValueFormatted, hiValueFormatted));
                } else {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RANGE_OUTSIDE_EXCL, metricName,
                        loValueFormatted, hiValueFormatted));
                }
            } else if ("<=".equals(condition.getComparator())) {
                if (isShort) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RANGE_INSIDE_INCL_SHORT,
                        metricName, loValueFormatted, hiValueFormatted));
                } else {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RANGE_INSIDE_INCL, metricName,
                        loValueFormatted, hiValueFormatted));
                }
            } else if (">=".equals(condition.getComparator())) {
                if (isShort) {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RANGE_OUTSIDE_INCL_SHORT,
                        metricName, loValueFormatted, hiValueFormatted));
                } else {
                    str.append(AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_RANGE_OUTSIDE_INCL, metricName,
                        loValueFormatted, hiValueFormatted));
                }
            } else {
                str.append("invalid range comparator [" + condition.getComparator() + "] (" + loValueFormatted + ","
                    + hiValueFormatted + ")");
            }

            break;
        }
        default: {
            str.append("unknown category [" + category.name() + "]");
            break;
        }
        }

        return str.toString();
    }

    @Override
    public String prettyPrintAlertURL(Alert alert) {
        StringBuilder builder = new StringBuilder();

        String baseUrl = systemManager.getUnmaskedSystemSettings(true).get(SystemSetting.BASE_URL);
        builder.append(baseUrl);
        if (!baseUrl.endsWith("/")) {
            builder.append("/");
        }

        builder.append("coregui/CoreGUI.html#Resource/");
        builder.append(alert.getAlertDefinition().getResource().getId());
        builder.append("/Alerts/History/");
        builder.append(alert.getId());

        return builder.toString();
    }

    private int markAlertsRecovered(Integer alertDefinitionId) {
        if (log.isDebugEnabled()) {
            log.debug("Trying to mark alertDefinitionId " + alertDefinitionId + " recovered");
        }
        Query markRecovered = entityManager.createNamedQuery(Alert.QUERY_MARK_RECOVERED_BY_DEFINITION_ID);
        markRecovered.setParameter("recoveryTime", System.currentTimeMillis());
        markRecovered.setParameter("alertDefinitionId", alertDefinitionId);
        int recoveredAlerts = markRecovered.executeUpdate();
        if (log.isDebugEnabled()) {
            log.debug("Recovered " + recoveredAlerts + " with alertDefinitionId " + alertDefinitionId);
        }
        return recoveredAlerts;
    }
   
    private void processRecovery(AlertDefinition firedDefinition) {
        Subject overlord = subjectManager.getOverlord();
        Integer recoveryDefinitionId = firedDefinition.getRecoveryId();

        if (recoveryDefinitionId != 0) {
            if (log.isDebugEnabled()) {
                log.debug("Processing recovery rules... Found recoveryDefinitionId " + recoveryDefinitionId);
            }

            AlertDefinition toBeRecoveredDefinition = alertDefinitionManager.getAlertDefinitionById(overlord,
                recoveryDefinitionId);
            boolean wasEnabled = toBeRecoveredDefinition.getEnabled();

            if (log.isDebugEnabled()) {
                log.debug(firedDefinition + (wasEnabled ? "does not need to recover " : "needs to recover ")
                    + toBeRecoveredDefinition
                    + (wasEnabled ? ", it was already enabled " : ", it was currently disabled "));
            }

            if (!wasEnabled) {
                /*
                 * - recover the other alert def, go through the manager layer so as to update the alert cache
                 * - we know this is a resource alert def so make that call directly. this is more efficient
                 *   and also avoids the issue in BZ 1126853.
                 */
                alertDefinitionManager.enableResourceAlertDefinitions(overlord, new int[] { recoveryDefinitionId });
                markAlertsRecovered(recoveryDefinitionId);
            }

            /*
             * there's no reason to update the cache directly anymore.  even though this direct type of update is safe
             * (because we know the AlertManager will only be executing on the same server instance that is processing
             * these recovery alerts now) it's unnecessary because changes made via the AlertDefinitionManager will
             * update the cache indirectly via the status field on the owning agent and the periodic job that checks it.
             */
        } else if (firedDefinition.getWillRecover()) {
            if (log.isDebugEnabled()) {
                log.debug("Disabling " + firedDefinition + " until recovered manually or by recovery definition");
            }

            /*
             * - disable alert def until recovered manually or by recovery definition.
             * - go through the manager layer so as to update the alert cache.
             * - we know this is a resource alert def so make that call directly. this is more efficient
             *   and also avoids the issue in BZ 1126853.
             */
            alertDefinitionManager.disableResourceAlertDefinitions(overlord, new int[] { firedDefinition.getId() });

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
    @Override
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

    @Override
    public PageList<Alert> findAlertsByCriteria(Subject subject, AlertCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "alertDefinition.resource", subject.getId());
        }

        Query query = generator.getQuery(entityManager);
        if (log.isDebugEnabled()) {
            QueryImpl queryImpl = (QueryImpl) query;
            PageControl pageControl = CriteriaQueryGenerator.getPageControl(criteria);
            log.debug("*Executing JPA query: " + queryImpl.getHibernateQuery().getQueryString() + ", selection=["
                + pageControl.getStartRow() + ".." + (pageControl.getStartRow() + pageControl.getPageSize() - 1)
                + "]...");
        }

        CriteriaQueryRunner<Alert> queryRunner = new CriteriaQueryRunner<Alert>(criteria, generator, entityManager);
        PageList<Alert> alerts = queryRunner.execute();

        fetchCollectionFields(alerts);

        return alerts;
    }
}
