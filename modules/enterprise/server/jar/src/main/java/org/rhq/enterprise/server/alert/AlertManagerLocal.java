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
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 */
@Local
public interface AlertManagerLocal {
    Alert createAlert(Alert alert);

    Alert updateAlert(Alert alert);

    void deleteAlerts(Subject user, int resourceId, Integer[] ids);

    int deleteAlerts(Subject user, int resourceId);

    int deleteAlerts(long beginTime, long endTime);

    Alert getById(int alertId);

    int getAlertCount(Integer alertDefId);

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

    void triggerOperation(AlertDefinition alertDefinition);

    public int getAlertCountByMeasurementDefinitionAndResources(int measurementDefinitionId,
        Collection<Resource> resources, long beginDate, long endDate);

    public Map<Integer, Integer> getAlertCountForSchedules(long begin, long end, List<Integer> scheduleIds);
}