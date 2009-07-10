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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;

/**
 * A manager for {@link MeasurementData}s.
 */
@Local
public interface MeasurementDataManagerLocal {

    int purgeTraits(long oldest);

    void mergeMeasurementReport(MeasurementReport report);

    void addNumericData(Set<MeasurementDataNumeric> data);

    void addTraitData(Set<MeasurementDataTrait> data);

    /**
     * Returns a list of numeric data point lists for the given measurement definition - one per specified resource.
     *
     * @param  subject
     * @param  resourceIds
     * @param  measurementDefinitionId measurement definition id for a numeric metric associated with the given sibling
     *                                 resources
     * @param  beginTime
     * @param  endTime
     * @param  numberOfdataPoints
     *
     * @return
     */
    List<List<MeasurementDataNumericHighLowComposite>> findDataForSiblingResources(Subject subject,
        int[] resourceIds, int measurementDefinitionId, long beginTime, long endTime, int numberOfdataPoints);

    /**
     * Returns a list of numeric data point lists for the given auto group - one per specified measurement definition.
     * The data points represent the average min/avg/max values of the members of the group.
     *
     * @param  subject
     * @param  autoGroupParentResourceId
     * @param  autoGroupChildResourceTypeId
     * @param  measurementDefinitionId      measurement definition id of numeric metrics associated with the given auto
     *                                      group
     * @param  beginTime
     * @param  endTime
     * @param  numberOfDataPoints
     * @param  aggregateOverAutoGroup       TODO
     *
     * @return
     */
    List<List<MeasurementDataNumericHighLowComposite>> findDataForAutoGroup(Subject subject,
        int autoGroupParentResourceId, int autoGroupChildResourceTypeId, int measurementDefinitionId, long beginTime,
        long endTime, int numberOfDataPoints, boolean aggregateOverAutoGroup);

    /**
     * Return the current trait value for the passed schedule
     *
     * @param  scheduleId id of a MeasurementSchedule that 'points' to a Trait
     *
     * @return One trait
     */
    public MeasurementDataTrait getCurrentTraitForSchedule(int scheduleId);

    /**
     * Return the current numeric value for the passed schedule
     *
     * @param  scheduleId id of a MeasurementSchedule that 'points' to a MeasurementDataNumeric record
     *
     * @return One MeasurementDataNumeric or null if nothing was found
     */
    public MeasurementDataNumeric getCurrentNumericForSchedule(int scheduleId);

    /**
     * Get metric display summaries for the resources of the passed compatible group, where the
     * {@link MetricDisplaySummary} only contains the metric name and number of alerts. All other fields
     * are not set.
     *
     * @param  subject subject of the caller
     * @param  group   compatible group
     *
     * @return a Map of resource id, List of summaries for this resource
     */
    Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricsDisplaySummariesForCompGroup(Subject subject,
        ResourceGroup group, long beginTime, long endTime);

    /**
     * Get the {@link MetricDisplaySummary}s for the resources passed in, that all need to be of the same
     * {@link ResourceType}. Summaries only contain a basic selection of fields for the purpose of filling the Child
     * resource popups.
     */
    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricDisplaySummariesForCompatibleResources(
        Subject subject, Collection<Resource> resources, long beginTime, long endTime);

    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricsDisplaySummariesForAutoGroup(Subject subject,
        int parentId, int cType, long beginTime, long endTime);

    /**
     * Return a map of &lt;resource id, List&lt;MetricDisplaySummary&gt;&gt;, where the list contains the
     * {@link MetricDisplaySummary} for the (enabled) schedules of the resource
     *
     * @param subject        Subject of the caller
     * @param resourceTypeId ResourceTypeId of the child resources
     * @param parentId       ID of the common parent resource
     * @param resourceIds    List of primary keys of the resources we are interested in
     * @param begin          begin time
     * @param end            end time
     */
    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricDisplaySummariesForResourcesAndParent(
        Subject subject, int resourceTypeId, int parentId, List<Integer> resourceIds, long begin, long end);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * Get the aggregate values of the numerical values for a given schedule.  This can only provide aggregates for data
     * in the "live" table
     *
     * @param subject    the user requesting the aggregate
     * @param scheduleId the id of the {@link MeasurementSchedule} for which this aggregate is being requested
     * @param start      the start time
     * @param end        the end time
     *
     * @return MeasurementAggregate bean with the data
     *
     * @throws FetchException if the schedule does not reference numerical data or if the user is not allowed to view
     *                        the {@link Resource} corresponding to this scheduleId
     */
    MeasurementAggregate getAggregate(Subject subject, int scheduleId, long startTime, long endTime)
        throws FetchException;

    /**
     * Return all known trait data for the passed schedule, defined by resourceId and definitionId
     *
     * @param  resourceId   PK of a {@link Resource}
     * @param  definitionId PK of a {@link MeasurementDefinition}
     *
     * @return a List of {@link MeasurementDataTrait} objects.
     */
    List<MeasurementDataTrait> findTraits(Subject subject, int resourceId, int definitionId) throws FetchException;

    List<MeasurementDataTrait> findCurrentTraitsForResource(Subject subject, int resourceId, DisplayType displayType)
        throws FetchException;

    /**
     * Get live metrics for a given MeasurementSchedule
     *
     * @param  sched MeasurementSchedule to obtain the data for
     *
     * @return MeasurementData for this Schedule
     */
    Set<MeasurementData> findLiveData(Subject subject, int resourceId, int[] definitionIds) throws FetchException;

    /**
     * Returns a list of numeric data point lists for the given compatible group - one per specified measurement
     * definition. The data points represent the average min/avg/max values of the members of the group.
     *
     * @param  subject
     * @param  compatibleGroupId
     * @param  measurementDefinitionId measurement definition id for numeric metric associated with the given compatible
     *                                 group
     * @param  beginTime
     * @param  endTime
     * @param  numberOfDataPoints
     * @param  aggregateOverGroup      TODO
     *
     * @return
     */
    List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(Subject subject, int groupId,
        int definitionId, long beginTime, long endTime, int numPoints, boolean groupAggregateOnly)
        throws FetchException;

    List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(Subject subject, int resourceId,
        int[] definitionIds, long beginTime, long endTime, int numPoints) throws FetchException;
}