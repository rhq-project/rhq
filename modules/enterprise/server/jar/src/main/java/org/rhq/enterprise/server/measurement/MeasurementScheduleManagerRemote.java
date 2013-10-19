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
package org.rhq.enterprise.server.measurement;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.util.PageList;

/**
 * @author Jay Shaughnessy
 *
 */
@Remote
public interface MeasurementScheduleManagerRemote {

    /**
     * Disables all collection schedules attached to the given resource whose schedules are based off the given
     * definitions. This does not disable the "templates" (aka definitions).
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param resourceId
     */
    void disableSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds);

    /**
     * Disable the measurement schedules for the passed definitions for the resources of the passed compatible group.
     *
     * @param subject
     * @param groupId
     * @param measurementDefinitionIds
     */
    void disableSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds);

    /**
     * Requires MANAGE_SETTINGS global permission.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param updateExistingSchedules
     */
    void disableSchedulesForResourceType(Subject subject, int[] measurementDefinitionIds,
        boolean updateExistingSchedules);

    /**
     * @param subject
     * @param measurementDefinitionIds
     *
     * @deprecated use {@link #disableSchedulesForResourceType(Subject, int[], boolean)}
     */
    @Deprecated
    void disableMeasurementTemplates(Subject subject, int[] measurementDefinitionIds);

    /**
     * Enable the schedules for the provided definitions and resource
     * @param subject
     * @param measurementDefinitionIds
     * @param resourceId
     */
    void enableSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds);

    /**
     * Enable the measurement schedules for the passed definitions for the resources of the passed compatible group.
     *
     * @param subject
     * @param groupId
     * @param measurementDefinitionIds
     */
    void enableSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds);

    /**
     * Requires MANAGE_SETTINGS global permission.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param updateExistingSchedules
     */
    void enableSchedulesForResourceType(Subject subject, int[] measurementDefinitionIds, boolean updateExistingSchedules);

    /**
     * @param subject
     * @param measurementDefinitionIds
     *
     * @deprecated use {@link #enableSchedulesForResourceType(Subject, int[], boolean)}
     */
    @Deprecated
    void enableMeasurementTemplates(Subject subject, int[] measurementDefinitionIds);

    /**
     * @param subject
     * @param measurementSchedule
     */
    void updateSchedule(Subject subject, MeasurementSchedule measurementSchedule);

    /**
     * @param subject
     * @param resourceId
     * @param measurementDefinitionIds
     * @param collectionInterval
     */
    void updateSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds,
        long collectionInterval);

    /**
     * @param subject
     * @param groupId
     * @param measurementDefinitionIds
     * @param collectionInterval
     */
    void updateSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds,
        long collectionInterval);

    /**
     * Requires MANAGE_SETTINGS global permission.
     *
     * @param subject
     * @param measurementDefinitionIds
     * @param collectionInterval
     * @param updateExistingSchedules
     */
    void updateSchedulesForResourceType(Subject subject, int[] measurementDefinitionIds, long collectionInterval,
        boolean updateExistingSchedules);

    /**
     * @param subject
     * @param measurementDefinitionIds
     * @param collectionInterval
     * @deprecated use {@link #updateSchedulesForResourceType(Subject, int[], long, boolean)}
     */
    @Deprecated
    void updateMeasurementTemplates(Subject subject, int[] measurementDefinitionIds, long collectionInterval);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<MeasurementSchedule> findSchedulesByCriteria(Subject subject, MeasurementScheduleCriteria criteria);
}
