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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayValue;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * @author Joseph Marques
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class MeasurementChartsManagerBean implements MeasurementChartsManagerLocal {

    private final Log log = LogFactory.getLog(MeasurementChartsManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;

    @EJB
    @IgnoreDependency
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;
    @EJB
    @IgnoreDependency
    private ResourceManagerLocal resourceManager;
    @EJB
    private MeasurementDataManagerLocal dataManager;
    @EJB
    private MeasurementScheduleManagerLocal scheduleManager;
    @EJB
    private AlertManagerLocal alertManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private MeasurementViewManagerLocal viewManager;

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal#getMetricDisplaySummariesForAutoGroup(org.jboss.on.domain.auth.Subject,
     * int, int, int[], long, long)
     */
    public List<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(Subject subject,
        int autoGroupParentResourceId, int autoGroupChildResourceTypeId, int[] measurementDefinitionIds, long begin,
        long end, boolean enabledOnly) throws MeasurementException {
        List<Resource> resources = resourceGroupManager.getResourcesForAutoGroup(subject, autoGroupParentResourceId,
            autoGroupChildResourceTypeId);
        List<MetricDisplaySummary> ret = getAggregateMetricDisplaySummaries(subject, resources,
            measurementDefinitionIds, begin, end, enabledOnly);
        for (MetricDisplaySummary tmp : ret) {
            tmp.setParentId(autoGroupParentResourceId);
            tmp.setChildTypeId(autoGroupChildResourceTypeId);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal#getMetricDisplaySummariesForCompatibleGroup(org.jboss.on.domain.auth.Subject,
     * int, int[], long, long)
     */
    public List<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(Subject subject, int groupId,
        int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly) throws MeasurementException {
        ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);
        Set<Resource> resources = group.getExplicitResources();
        List<Resource> resList = new ArrayList<Resource>(resources.size());
        resList.addAll(resources);

        List<MetricDisplaySummary> ret = getAggregateMetricDisplaySummaries(subject, resList, measurementDefinitionIds,
            begin, end, enabledOnly);
        for (MetricDisplaySummary tmp : ret) {
            tmp.setGroupId(groupId);
        }

        return ret;
    }

    public List<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(Subject subject, int parent, int type,
        String viewName) {
        MeasurementPreferences preferences = new MeasurementPreferences(subject);
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        long begin = rangePreferences.begin;
        long end = rangePreferences.end;

        int[] measurementDefinitionIds;
        try {
            MeasurementViewContext context = new MeasurementViewContext(-1, -1, parent, type);
            measurementDefinitionIds = fillDefinitionIdsFromUserPreferences(context, viewName, subject);
        } catch (IllegalArgumentException iae) {
            // If we can't get stuff from preferences, get the defaults.
            measurementDefinitionIds = resourceGroupManager.getDefinitionsForAutoGroup(subject, parent, type, true);
        }
        // now that we have the definitions, we can get the data from the backend.
        List<MetricDisplaySummary> summaries;
        try {
            summaries = getMetricDisplaySummariesForAutoGroup(subject, parent, type, measurementDefinitionIds, begin,
                end, false);
        } catch (MeasurementException me) {
            log.debug("Can't get ViewMetrics for autogroup: " + me);
            summaries = new ArrayList<MetricDisplaySummary>();
        }

        return summaries;
    }

    public List<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(Subject subject, int groupId,
        String viewName) {

        MeasurementPreferences preferences = new MeasurementPreferences(subject);
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        long begin = rangePreferences.begin;
        long end = rangePreferences.end;

        /* Fiddle the metrics from the | delimited stored ones and only display those. 
         * Use the default list as fall back if we don't have them in preferences
         */
        int[] measurementDefinitionIds;
        try {
            MeasurementViewContext context = new MeasurementViewContext(-1, groupId, -1, -1);
            measurementDefinitionIds = fillDefinitionIdsFromUserPreferences(context, viewName, subject);
        } catch (IllegalArgumentException iae) {
            // If we can't get stuff from preferences, get the defaults.
            measurementDefinitionIds = resourceGroupManager.getDefinitionsForCompatibleGroup(subject, groupId, true);
        }

        List<MetricDisplaySummary> summaries;
        try {
            summaries = getMetricDisplaySummariesForCompatibleGroup(subject, groupId, measurementDefinitionIds, begin,
                end, false);
        } catch (MeasurementException me) {
            log.debug("Can't get ViewMetrics for Compat Group: " + me);
            summaries = new ArrayList<MetricDisplaySummary>();
        }
        return summaries;
    }

    /**
     * Get the definition ids (for groups) from the metrics stored in the preferences, which are separated by a vertical bar.
     */
    private int[] fillDefinitionIdsFromUserPreferences(MeasurementViewContext context, String viewName, Subject subject) {
        List<String> charts;
        try {
            charts = viewManager.getCharts(subject, context, viewName);
            if (charts.isEmpty()) {
                throw new IllegalArgumentException("No metrics defined"); // Use defaults then from the caller
            }
        } catch (MeasurementViewException mve) {
            // view did not exist, same as the view being empty
            throw new IllegalArgumentException("No metrics defined"); // Use defaults then from the caller
        }

        int[] measurementDefinitionIds = new int[charts.size()];
        int i = 0;
        for (String token : charts) {
            MetricDisplaySummary tmp = MeasurementUtils.parseMetricToken(token);
            measurementDefinitionIds[i++] = tmp.getDefinitionId();
        }
        return measurementDefinitionIds;
    }

    public List<MetricDisplaySummary> getMetricDisplaySummariesForResource(Subject subject, int resourceId,
        int[] measurementDefinitionIds, long beginTime, long endTime) throws MeasurementException {
        List<MetricDisplaySummary> allMeasurementData = new ArrayList<MetricDisplaySummary>(
            measurementDefinitionIds.length);
        List<Integer> scheduleIds = new ArrayList<Integer>(measurementDefinitionIds.length);
        for (int measurementDefinitionId : measurementDefinitionIds) {
            MeasurementSchedule schedule = null;
            try {
                schedule = scheduleManager.getMeasurementSchedule(subject, measurementDefinitionId, resourceId, false);
                scheduleIds.add(schedule.getId());
            } catch (MeasurementNotFoundException mnfe) {
                throw new MeasurementException(mnfe);
            }

            MetricDisplaySummary summary = getMetricDisplaySummary(schedule, beginTime, endTime, false);
            if (summary != null) {
                summary.setUnits(schedule.getDefinition().getUnits().name());
                allMeasurementData.add(summary);
            }
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(beginTime, endTime, scheduleIds);
        for (MetricDisplaySummary sum : allMeasurementData) {
            sum.setAlertCount(alerts.get(sum.getScheduleId()));
        }

        return allMeasurementData;
    }

    /**
     * TODO hwr document me !!!
     */
    @Deprecated
    public List<MetricDisplaySummary> getMetricDisplaySummariesForSchedules(Subject subject, int resourceId,
        List<Integer> scheduleIds, long beginTime, long endTime, boolean narrowed) throws MeasurementException {
        List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();
        for (int scheduleId : scheduleIds) {
            MeasurementSchedule schedule = scheduleManager.getMeasurementScheduleById(subject, scheduleId);
            if (schedule == null)
                continue;

            MetricDisplaySummary summary = getMetricDisplaySummary(schedule, beginTime, endTime, narrowed);
            if (summary != null) {
                summary.setResourceId(resourceId);
                summaries.add(summary);
            }
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(beginTime, endTime, scheduleIds);
        for (MetricDisplaySummary sum : summaries) {
            sum.setAlertCount(alerts.get(sum.getScheduleId()));
        }

        return summaries;
    }

    public List<MetricDisplaySummary> getMetricDisplaySummariesForMetrics(Subject subject, int resourceId,
        DataType dataType, long begin, long end, boolean narrowed, boolean enabledOnly) throws MeasurementException {
        List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();

        List<MeasurementSchedule> scheds = scheduleManager.getMeasurementSchedulesForResourceAndType( // TODO only get ids
            subject, resourceId, dataType, null, enabledOnly); //null -> don't filter, we want everything

        List<Integer> scheduleIds = new ArrayList<Integer>(scheds.size());

        for (MeasurementSchedule schedule : scheds) {
            MetricDisplaySummary summary = getMetricDisplaySummary(schedule, begin, end, narrowed);
            if (summary != null) {
                summary.setResourceId(resourceId);
                summaries.add(summary);
                scheduleIds.add(schedule.getId());
            }
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(begin, end, scheduleIds);
        for (MetricDisplaySummary sum : summaries) {
            sum.setAlertCount(alerts.get(sum.getScheduleId()));
        }

        return summaries;
    }

    /**
     * get a display summary for the passed schedule
     *
     * @param narrowed if true, don't obtain the metrical values  NOTE: alertCounts need to be set by the caller.
     */
    private MetricDisplaySummary getMetricDisplaySummary(MeasurementSchedule schedule, long begin, long end,
        boolean narrowed) throws MeasurementException {
        MetricDisplaySummary summary = new MetricDisplaySummary();
        summary.setScheduleId(schedule.getId());
        summary.setBeginTimeFrame(begin);
        summary.setEndTimeFrame(end);

        MeasurementDefinition definition = schedule.getDefinition();
        summary.setDefinitionId(definition.getId());
        summary.setLabel(definition.getDisplayName());
        summary.setDescription(definition.getDescription());
        summary.setMetricSource(schedule.getResource().getName());

        switch (definition.getDataType()) {
        case MEASUREMENT: {
            MeasurementUnits units = definition.getUnits();
            summary.setUnits(units.name());

            NumericType type = definition.getNumericType();
            if (type == null) {
                throw new IllegalStateException("NumericType is null, but no traits are expected here");
            }

            int collectionType;
            switch (type) {
            case DYNAMIC: {
                collectionType = 0;
                break;
            }

            case TRENDSUP: {
                collectionType = 2;
                break;
            }

            case TRENDSDOWN: {
                collectionType = 3;
                break;
            }

            default: {
                throw new IllegalStateException("Unknown measurement type: " + type);
            }
            }

            summary.setCollectionType(collectionType);

            if (!narrowed) {
                MeasurementAggregate compositeHighLow = dataManager.getAggregate(schedule, begin, end);
                if (compositeHighLow.isEmpty()) {
                    summary.setValuesPresent(false);
                }

                Map<String, MetricDisplayValue> metricValues = new HashMap<String, MetricDisplayValue>();
                metricValues.put(MetricDisplayConstants.MIN_KEY, new MetricDisplayValue(compositeHighLow.getMin()));
                metricValues.put(MetricDisplayConstants.AVERAGE_KEY, new MetricDisplayValue(compositeHighLow.getAvg()));
                metricValues.put(MetricDisplayConstants.MAX_KEY, new MetricDisplayValue(compositeHighLow.getMax()));
                MeasurementDataNumeric currentNumeric = dataManager.getCurrentNumericForSchedule(schedule.getId());
                Double lastValue = (currentNumeric != null) ? currentNumeric.getValue() : Double.NaN;
                metricValues.put(MetricDisplayConstants.LAST_KEY, new MetricDisplayValue(lastValue));
                summary.setMetrics(metricValues);
            }

            break;
        }

        case TRAIT: {
            summary.setIsTrait(true);
            MeasurementDataTrait trait = dataManager.getCurrentTraitForSchedule(schedule.getId());
            if (trait != null) {
                summary.setValue(trait.getValue());
                summary.setTimestamp(trait.getTimestamp());
            } else {
                summary.setValue("- nothing found -");
                summary.setTimestamp(System.currentTimeMillis());
            }

            break;
        }

        case CALLTIME: {
            // TODO: ignore?
            break;
        }

        default: {
            throw new IllegalStateException("Unsupported metric data type: " + definition.getDataType());
        }
        }

        return summary;
    }

    /**
     * Get the group display summaries for the passed resources that belong to a (auto)group
     *
     * @param  subject
     * @param  resources
     * @param  measurementDefinitionIds
     * @param  begin
     * @param  end
     * @param  enabledOnly              do we want to restrict this to enabled metrics?
     *
     * @return
     *
     * @throws MeasurementException
     */
    private List<MetricDisplaySummary> getAggregateMetricDisplaySummaries(Subject subject, List<Resource> resources,
        int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly) throws MeasurementException {

        List<MetricDisplaySummary> data = new ArrayList<MetricDisplaySummary>(measurementDefinitionIds.length);

        // nothing to do, as we have no resources in that group
        if (resources.isEmpty()) {
            return data;
        }
        if (measurementDefinitionIds.length == 0) {
            return data;
        }

        MeasurementDataManagerUtility dataUtil = MeasurementDataManagerUtility.getInstance(rhqDs);

        // Loop over the definitions, find matching schedules and create a MetricDisplaySummary for each definition
        for (int definitionId : measurementDefinitionIds) {
            int collecting = 0;
            List<MeasurementSchedule> schedules = scheduleManager.getMeasurementSchedulesByDefinitionIdAndResources(
                subject, definitionId, resources);
            int[] scheduleIds = new int[schedules.size()];
            for (int i = 0; i < schedules.size(); i++) {
                MeasurementSchedule schedule = schedules.get(i);
                if (schedule.isEnabled()) {
                    collecting++;
                }

                scheduleIds[i] = schedule.getId();
            }

            /*
             * If no metric is collecting, stop here as we have nothing to contribute
             */
            if ((collecting == 0) && enabledOnly) {
                if (log.isTraceEnabled()) {
                    log.trace("There were no schedules enabled for definition id " + definitionId);
                }

                continue;
            }

            MetricDisplaySummary summary = new MetricDisplaySummary();

            // See MetricsDisplay.jsp for wrt the next two lines.
            summary.setNumberCollecting(collecting);
            summary.setShowNumberCollecting(true);

            summary.setDefinitionId(definitionId);
            summary.setBeginTimeFrame(begin);
            summary.setAlertCount(alertManager.getAlertCountByMeasurementDefinitionAndResources(definitionId,
                resources, begin, end));
            summary.setEndTimeFrame(end);

            MeasurementDefinition definition = entityManager.find(MeasurementDefinition.class, definitionId);
            summary.setDefinitionId(definition.getId());
            summary.setUnits(definition.getUnits().getName());
            summary.setDescription(definition.getDescription());
            summary.setLabel(definition.getDisplayName());
            summary.setDescription(definition.getDescription());
            summary.setMetricSource(definition.getResourceType().getName());

            /*
             * Get the aggregate data from the backend and check if it is empty or not. If it is empty (for all members
             * of the group), skip over this metric.
             */
            MeasurementAggregate aggregate;
            if (scheduleIds.length == 0) {
                aggregate = new MeasurementAggregate(null, null, null);
                log.warn("No metric schedules found for def=[" + definition + "] and resources [" + resources
                    + "], using empty aggregate");
            } else {
                aggregate = dataUtil.getAggregateByScheduleIds(begin, end, scheduleIds);
            }
            if (aggregate.isEmpty()) {
                if (log.isTraceEnabled()) {
                    log.trace("There was no measurement data available for schedules " + Arrays.toString(scheduleIds)
                        + " in the timeframe [" + new Date(begin) + ", " + new Date(end) + "]");
                }

                summary.setValuesPresent(false);
            }

            Map<String, MetricDisplayValue> metricValues = new HashMap<String, MetricDisplayValue>();
            metricValues.put(MetricDisplayConstants.MIN_KEY, new MetricDisplayValue(aggregate.getMin()));
            metricValues.put(MetricDisplayConstants.AVERAGE_KEY, new MetricDisplayValue(aggregate.getAvg()));
            metricValues.put(MetricDisplayConstants.MAX_KEY, new MetricDisplayValue(aggregate.getMax()));

            // TODO put the sum back on - JBNADM-2626
            //metricValues.put(MetricDisplayConstants.SUMMARY_KEY, new MetricDisplayValue(aggregate.getSum()));
            summary.setMetrics(metricValues);

            // TODO what else do we need ?

            data.add(summary);
        }

        return data;
    }

    public Map<MeasurementDefinition, List<MetricDisplaySummary>> getMetricDisplaySummariesForMetricsCompare(
        Subject subject, Integer[] resourceIds, int[] definitionIds, long begin, long end) throws MeasurementException {
        // Getting all the Resource objects in one call, and caching here for the rest of this method
        PageList<Resource> resources = resourceManager.getResourceByIds(subject, resourceIds, true, PageControl
            .getUnlimitedInstance());

        // I want to only get the definition objects once for each ID, and cache here for the rest of this method
        Map<Integer, MeasurementDefinition> measurementDefinitionsMap = new HashMap<Integer, MeasurementDefinition>(
            definitionIds.length);
        List<Integer> listOfDefinitionIds = new ArrayList<Integer>();
        for (int definitionId : definitionIds) {
            if (isMetricCollecting(subject, resources, definitionId)) {
                listOfDefinitionIds.add(definitionId);
            }
        }

        Integer[] defIds = listOfDefinitionIds.toArray(new Integer[listOfDefinitionIds.size()]);

        List<MeasurementDefinition> definitions = measurementDefinitionManager.getMeasurementDefinitionsByIds(subject,
            defIds);
        for (MeasurementDefinition definition : definitions) {
            measurementDefinitionsMap.put(definition.getId(), definition);
        }

        Map<MeasurementDefinition, List<MetricDisplaySummary>> compareMetrics = new HashMap<MeasurementDefinition, List<MetricDisplaySummary>>();
        for (Resource resource : resources) {
            List<MetricDisplaySummary> resourceMDS = getMetricDisplaySummariesForResource(subject, resource.getId(),
                definitionIds, begin, end);
            for (MetricDisplaySummary summary : resourceMDS) {
                Integer definitionId = summary.getDefinitionId();
                MeasurementDefinition definition = measurementDefinitionsMap.get(definitionId);

                // This isn't a collecting metric, so move along
                if (definition == null) {
                    continue;
                }

                summary.setResource(resource);
                summary.setParent(resource.getParentResource());
                summary.setUnits(definition.getUnits().getName());
                summary.setDescription(definition.getDescription());
                summary.setLabel(definition.getDisplayName());
                summary.setMetricSource(definition.getResourceType().getName());

                List<MetricDisplaySummary> metricsForDefinition = compareMetrics.get(definition);
                if (metricsForDefinition == null) {
                    metricsForDefinition = new ArrayList<MetricDisplaySummary>();
                    compareMetrics.put(definition, metricsForDefinition);
                }

                metricsForDefinition.add(summary);
            }
        }

        return compareMetrics;
    }

    private boolean isMetricCollecting(Subject subject, List<Resource> resources, int definitionId) {
        boolean isCollecting = false;
        List<MeasurementSchedule> schedules = scheduleManager.getMeasurementSchedulesByDefinitionIdAndResources(
            subject, definitionId, resources);
        for (MeasurementSchedule schedule : schedules) {
            if (schedule.isEnabled()) {
                isCollecting = true;
                break;
            }
        }

        return isCollecting;
    }

}
