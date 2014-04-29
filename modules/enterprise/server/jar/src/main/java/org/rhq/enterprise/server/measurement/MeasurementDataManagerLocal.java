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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * A manager for {@link MeasurementData}s.
 */
@Local
public interface MeasurementDataManagerLocal extends MeasurementDataManagerRemote {

    /**
     * Remove duplicate traits after this date.
     * Should be run once a week or so.
     */
    void cleanupTraitHistory(Date after);

    void mergeMeasurementReport(MeasurementReport report);

    void addNumericData(Set<MeasurementDataNumeric> data);

    void addTraitData(Set<MeasurementDataTrait> data);

    /**
     * Return the current trait value for the passed schedule.
     *
     * @param  scheduleId id of a MeasurementSchedule that 'points' to a Trait
     */
    MeasurementDataTrait getCurrentTraitForSchedule(int scheduleId);

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
     *
     * @deprecated portal-war
     */
    Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricsDisplaySummariesForCompGroup(Subject subject,
        ResourceGroup group, long beginTime, long endTime);

    /**
     * Get the {@link MetricDisplaySummary}s for the resources passed in, that all need to be of the same
     * {@link ResourceType}. Summaries only contain a basic selection of fields for the purpose of filling the Child
     * resource popups.
     *
     * @deprecated portal-war
     */
    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricDisplaySummariesForCompatibleResources(
        Subject subject, Collection<Resource> resources, long beginTime, long endTime);

    /**
     * @deprecated portal-war (it is not used at all)
     */
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
     *
     * @deprecated portal-war
     */
    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricDisplaySummariesForResourcesAndParent(
        Subject subject, int resourceTypeId, int parentId, List<Integer> resourceIds, long begin, long end);

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForContext(Subject subject,
        EntityContext context, int definitionId, long beginTime, long endTime, int numDataPoints);

    /**
     * @deprecated portal-war (it is not used at all)
     */
    List<MeasurementDataNumeric> findRawData(Subject subject, int scheduleId, long startTime, long endTime);

    /**
     * Get live metrics from the agent for a given MeasurementSchedule
     *
     * @param subject the user that is requesting the data
     * @param resourceId the id of the resource
     * @param definitionIds the array of ids of schedule definitions
     * @param timeout the amount of time in milliseconds before timing out the request. Should be > 0. If null then default
     * is applied. Default agent connection failures can be long.
     *
     * @return MeasurementData for this Schedule. Not null. Returns empty set if agent connection can not be established or
     * component fails to report live data.
     */
    Set<MeasurementData> findLiveData(Subject subject, int resourceId, int[] definitionIds, Long timeout);

    void updateAlertConditionCache(String callingMethod, MeasurementData[] data);

}