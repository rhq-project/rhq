/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.util.List;
import java.util.Set;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;

/**
 * Public API for measurement data.
 */
@Remote
public interface MeasurementDataManagerRemote {

    /**
     * Get the aggregate values of the numerical values for a given schedule.  This can only provide aggregates for data
     * in the "live" table
     *
     * @param subject    the user requesting the aggregate
     * @param scheduleId the id of the {@link MeasurementSchedule} for which this aggregate is being requested
     * @param startTime
     * @param endTime
     *
     * @return MeasurementAggregate bean with the data
     *
     * @deprecated       class {@link org.rhq.enterprise.server.measurement.MeasurementAggregate} has been deprecated
     *                   since RHQ 4.8, therefore this method was deprecated as well and
     *                   replaced by {@link #getMeasurementAggregate(org.rhq.core.domain.auth.Subject,int,long,long)}
     */
    @Deprecated
    org.rhq.enterprise.server.measurement.MeasurementAggregate getAggregate(Subject subject, int scheduleId,
        long startTime, long endTime);

    /**
     * Get the aggregate values of the numerical values for a given schedule.  This can only provide aggregates for data
     * in the "live" table
     *
     * @param subject    the user requesting the aggregate
     * @param scheduleId the id of the {@link MeasurementSchedule} for which this aggregate is being requested
     * @param startTime in millis
     * @param endTime  in millis
     *
     * @return MeasurementAggregate bean with the data
     *
     * @throws MeasurementException if the schedule does not reference numerical data or if the user is not allowed to view
     *                        the {@link Resource} corresponding to this scheduleId
     *
     * @since 4.10
     */
    MeasurementAggregate getMeasurementAggregate(Subject subject, int scheduleId, long startTime, long endTime)
        throws MeasurementException;

    /**
     * Return all known trait data for the passed schedule, defined by resourceId and definitionId
     *
     * @param subject
     * @param  resourceId   PK of a {@link Resource}
     * @param  definitionId PK of a {@link MeasurementDefinition}
     *
     * @return a List of {@link MeasurementDataTrait} objects.
     */
    List<MeasurementDataTrait> findTraits(Subject subject, int resourceId, int definitionId);

    /**
     * @param subject
     * @param resourceId
     * @param displayType
     * @return not null
     */
    List<MeasurementDataTrait> findCurrentTraitsForResource(Subject subject, int resourceId, DisplayType displayType);

    /**
     * Finds traits that match the specified {@link MeasurementDataTraitCriteria criteria}.
     *
     * @param subject the user that is requesting the traits
     * @param criteria the criteria by which to filter the traits
     *
     * @return the traits that match the specified {@link MeasurementDataTraitCriteria criteria}; never null
     */
    PageList<MeasurementDataTrait> findTraitsByCriteria(Subject subject, MeasurementDataTraitCriteria criteria);

    // Deprecating this for 4.7, we don't want clients generating live data requests, it's not efficient or reliable
    // It will be moved to the Local
    /**
     * @param subject
     * @param resourceId
     * @param definitionIds
     * @return not null
     * @deprecated use {@link #findDataForResource(Subject, int, int[], long, long, int)}
     */
    @Deprecated
    Set<MeasurementData> findLiveData(Subject subject, int resourceId, int[] definitionIds);

    // Deprecating this for 4.7, we don't want clients generating live data requests, it's not efficient or reliable
    // It will be moved to the Local
    /**
     * @param subject
     * @param groupId
     * @param resourceIds
     * @param definitionIds
     * @return not null
     * @deprecated use {@link #findDataForCompatibleGroup(Subject, int, int, long, long, int)}
     */
    @Deprecated
    Set<MeasurementData> findLiveDataForGroup(Subject subject, int groupId, int[] resourceIds, int[] definitionIds);

    /**
     * Returns a list of numeric data point lists for the given compatible group - one per specified measurement
     * definition (only one). The data points represent the average min/avg/max values of the members of the group.
     *
     * @param  subject
     * @param  groupId
     * @param  definitionId measurement definition id for numeric metric associated with the given compatible group
     * @param  beginTime
     * @param  endTime
     * @param  numPoints
     *
     * @return not null
     */
    List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(Subject subject, int groupId,
        int definitionId, long beginTime, long endTime, int numPoints);

    /**
     * Returns a list of numeric data point lists for the given resource - one list per specified measurement
     * definition.
     *
     * @param  subject
     * @param  resourceId
     * @param  definitionIds measurement definition id for numeric metric associated with the given resource
     * @param  beginTime
     * @param  endTime
     * @param  numPoints
     *
     * @return not null
     */
    List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(Subject subject, int resourceId,
        int[] definitionIds, long beginTime, long endTime, int numPoints);

    /**
     * Get the aggregate values of the numerical values for a given group and metric
     * definition.
     *
     * @param subject      the user requesting the aggregate
     * @param groupId      the id of the {@link ResourceGroup} for which this aggregate is being requested
     * @param definitionId the id of the {@link MeasurementDefinition} for the metric
     *
     * @param startTime in millis
     * @param endTime  in millis
     *
     * @return MeasurementAggregate aggregate data
     *
     * @throws MeasurementException if aggregate cannot be found
     */
    MeasurementAggregate getAggregate(Subject subject, int groupId, int definitionId, long startTime, long endTime);

}
