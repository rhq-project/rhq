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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;

/**
 * @author Joseph Marques
 */
@Local
public interface AlertManagerLocal {
    Alert createAlert(Alert alert);

    Alert updateAlert(Alert alert);

    /**
     * Delete the Resource alerts with the specified id's if the specified user has permission to do so (i.e. either
     * the MANAGE_INVENTORY global permission, or the MANAGE_ALERTS Resource permission for all associated Resources).
     * If the user does not have permission for all of the specified alerts, then none of the alerts will be deleted
     * and a PermissionException will be thrown.
     *
     * @param user the user requesting the deletion
     * @param alertIds the id's of the Resource alerts to be deleted
     */
    void deleteResourceAlerts(Subject user, Integer[] alertIds);

    void deleteAlerts(Subject user, int resourceId, Integer[] ids);

    void deleteAlertsForResourceGroup(Subject user, int resourceGroupId, Integer[] ids);

    int deleteAlerts(Subject user, int resourceId);

    int deleteAlerts(long beginTime, long endTime);

    Alert getById(int alertId);

    int getAlertCountByMeasurementDefinitionId(Integer measurementDefinitionId, long begin, long end);

    int getAlertCountByMeasurementDefinitionAndResourceGroup(int measurementDefinitionId, int groupId, long beginDate,
        long endDate);

    int getAlertCountByMeasurementDefinitionAndAutoGroup(int measurementDefinitionId, int resourceParentId,
        int resourceTypeId, long beginDate, long endDate);

    int getAlertCountByMeasurementDefinitionAndResource(int measurementDefinitionId, int resourceId, long beginDate,
        long endDate);

    // resourceIds is nullable
    PageList<Alert> findAlerts(Subject subject, Integer[] resourceIds, AlertPriority priority, long timeRange,
        PageControl pageControl);

    PageList<Alert> findAlerts(int resourceId, Integer alertDefinitionId, AlertPriority priority, Long beginDate,
        Long endDate, PageControl pageControl);

    void fireAlert(int alertDefinitionId);

    void sendAlertNotifications(Alert alert);

    int getAlertCountByMeasurementDefinitionAndResources(int measurementDefinitionId, int[] resources, long beginDate,
        long endDate);

    Map<Integer, Integer> getAlertCountForSchedules(long begin, long end, List<Integer> scheduleIds);

    /**
     * Return the plugin manager that is managing alert sender plugins
     * @return The alert sender plugin manager
     */
    AlertSenderPluginManager getAlertPluginManager();

    /**
     * Create a human readable description of the conditions that led to this alert.
     * @param alert Alert to create human readable condition description
     * @param shortVersion if true the messages printed are abbreviated to save space
     * @return human readable condition log
     */
    String prettyPrintAlertConditions(Alert alert, boolean shortVersion);

    /**
     * Tells us if the definition of the passed alert will be disabled after this alert was fired
     * @param alert alert to check
     * @return true if the definition got disabled
     */
    boolean willDefinitionBeDisabled(Alert alert);

    Collection<String> sendAlertNotificationEmails(Alert alert, Collection<String> emailAddresses);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    PageList<Alert> findAlertsByCriteria(Subject subject, AlertCriteria criteria);

    String prettyPrintAlertURL(Alert alert);

    /**
     * Acknowledge the alerts (that got fired) so that admins know who is working
     * on fixing the situation.
     * @param user calling user
     * @param alertIds PKs of the alerts to ack
     * @return number of alerts acknowledged
     */
    int acknowledgeAlerts(Subject user, Integer[] alertIds);

    /**
     * Remove the alerts with the specified id's.
     * @param user caller
     * @param ids primary keys of the alerts to delete
     * @return number of alerts deleted
     */
    int deleteAlerts(Subject user, Integer[] ids);
}