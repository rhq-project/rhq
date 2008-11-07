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

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
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
     * Get the aggregate values of the numerical values for a given schedule This can only provide aggregates for data
     * in the "live" table
     *
     * @param  sched The Schedule for which this data is
     * @param  start the start time
     * @param  end   the end time
     *
     * @return MeasurementAggregate bean with the data
     *
     * @throws org.rhq.enterprise.server.measurement.MeasurementException is the Schedule does not reference numerical
     *                                                                    data
     */
    MeasurementAggregate getAggregate(MeasurementSchedule sched, long start, long end) throws MeasurementException;

    /**
     * Get live metrics for a given MeasurementSchedule
     *
     * @param  sched MeasurementSchedule to obtain the data for
     *
     * @return MeasurementData for this Schedule
     */
    Set<MeasurementData> getLiveData(int resourceId, Set<Integer> definitionIds);

    /**
     * Remove gathered Measurement for the given Schedule
     *
     * @param sched The Schedule for which to remove the data
     */
    void removeGatheredMetricsForSchedule(MeasurementSchedule sched);

    /**
     * Remove gathered Measurement for the given Schedule
     *
     * @param schedules The Schedule for which to remove the data
     */
    void removeGatheredMetricsForSchedules(List<MeasurementSchedule> schedules);

    List<MetricDisplaySummary> getMetricDisplaySummariesForResource(Subject subject, int resourceId,
        int[] measurementDefinitionIds, long begin, long end) throws MeasurementException;

    List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForResource(Subject subject, int resourceId,
        int[] measurementDefinitionIds, long beginTime, long endTime, int dataPoints);

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
    List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForSiblingResources(Subject subject,
        int[] resourceIds, int measurementDefinitionId, long beginTime, long endTime, int numberOfdataPoints);

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
    List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForCompatibleGroup(Subject subject,
        int compatibleGroupId, int measurementDefinitionId, long beginTime, long endTime, int numberOfDataPoints,
        boolean aggregateOverGroup);

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
    List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForAutoGroup(Subject subject,
        int autoGroupParentResourceId, int autoGroupChildResourceTypeId, int measurementDefinitionId, long beginTime,
        long endTime, int numberOfDataPoints, boolean aggregateOverAutoGroup);

    /**
     * TODO hwr document me !!!
     *
     * @param narrowed if true, no measurement values are obtained from the database. This is useful for the List of
     *                 Children that does not need them.
     */
    @Deprecated
    List<MetricDisplaySummary> getMetricDisplaySummariesForSchedules(Subject subject, int resourceId,
        List<Integer> scheduleIds, long begin, long end, boolean narrowed) throws MeasurementException;

    List<MetricDisplaySummary> getMetricDisplaySummariesForMetrics(Subject subject, int resourceId, DataType dataType,
        long begin, long end, boolean narrowed, boolean enabledOnly) throws MeasurementException;

    /**
     * Return the Traits for the passed resource. This method will for each trait only return the 'youngest' entry. If
     * there are no traits found for that resource, an empty list is returned. If displayType is null, no displayType is
     * honoured, else the traits will be filtered for the given displayType
     *
     * @param  resourceId  Id of the resource we are interested in
     * @param  displayType A display type for filtering or null for all traits.
     *
     * @return a List of MeasurementDataTrait
     */
    @NotNull
    public List<MeasurementDataTrait> getCurrentTraitsForResource(int resourceId, DisplayType displayType);

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
     * Return all known trait data for the passed schedule, defined by resourceId and definitionId
     *
     * @param  resourceId   PK of a {@link Resource}
     * @param  definitionId PK of a {@link MeasurementDefinition}
     *
     * @return a List of {@link MeasurementDataTrait} objects.
     */
    public List<MeasurementDataTrait> getAllTraitDataForResourceAndDefinition(int resourceId, int definitionId);

    /**
     * Get metric display summaries for a compatible group
     *
     * @param  subject
     * @param  groupId
     * @param  measurementDefinitionIds
     * @param  begin
     * @param  end
     * @param  enabledOnly              only show results for metric that are actually enabled
     *
     * @return
     *
     * @throws MeasurementException
     */
    List<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(Subject subject, int groupId,
        int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly) throws MeasurementException;

    /**
     * Get metric display summaries for an autogroup.
     *
     * @param  subject
     * @param  autoGroupParentResourceId
     * @param  autoGroupChildResourceTypeId
     * @param  measurementDefinitionIds
     * @param  begin
     * @param  end
     * @param  enabledOnly                  only show results for metric that are actually enabled
     *
     * @return
     *
     * @throws MeasurementException
     */
    List<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(Subject subject, int autoGroupParentResourceId,
        int autoGroupChildResourceTypeId, int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly)
        throws MeasurementException;

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
    Map<Integer, List<MetricDisplaySummary>> getNarrowedMetricsDisplaySummaryForCompGroup(Subject subject,
        ResourceGroup group, long beginTime, long endTime);

    /**
     * Get metric display summaries for the resources and measurements that are passed
     *
     * @param  subject                  subject of the caller
     * @param  resourceIds              Array of resource Ids that were selected to compare
     * @param  measurementDefinitionIds Array of measurment Ids
     * @param  begin                    begin time for the display time range
     * @param  end                      end time for the displays time range
     *
     * @return Map<MeasurementDefinition, List<MetricDisplaySummary>> Map holds the Metric in the key, then the
     *         resources values in a List for the value.
     *
     * @throws MeasurementException throws Measurement exception
     */
    Map<MeasurementDefinition, List<MetricDisplaySummary>> getMetricDisplaySummariesForMetricsCompare(Subject subject,
        Integer[] resourceIds, int[] measurementDefinitionIds, long begin, long end) throws MeasurementException;

    /**
     * Get the {@link MetricDisplaySummary}s for the resources passed in, that all need to be of the same
     * {@link ResourceType}. Summaries only contain a basic selection of fields for the purpose of filling the Child
     * resource popups.
     */
    public Map<Integer, List<MetricDisplaySummary>> getNarrowedMetricDisplaySummaryForCompatibleResources(
        Subject subject, Collection<Resource> resources, long beginTime, long endTime);

    public Map<Integer, List<MetricDisplaySummary>> getNarrowedMetricsDisplaySummaryForAutoGroup(Subject subject,
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
    public Map<Integer, List<MetricDisplaySummary>> getNarrowedMetricDisplaySummariesForResourcesAndParent(
        Subject subject, int resourceTypeId, int parentId, List<Integer> resourceIds, long begin, long end);
}