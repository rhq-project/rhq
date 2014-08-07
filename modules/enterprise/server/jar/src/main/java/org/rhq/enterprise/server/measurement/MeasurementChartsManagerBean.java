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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.ui.MetricDisplayConstants;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.ui.MetricDisplayValue;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.common.PerformanceMonitorInterceptor;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * @author Joseph Marques
 */
@Stateless
@Interceptors(PerformanceMonitorInterceptor.class)
public class MeasurementChartsManagerBean implements MeasurementChartsManagerLocal {

    private final Log log = LogFactory.getLog(MeasurementChartsManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource rhqDs;

    @EJB
    //@IgnoreDependency
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;
    @EJB
    //@IgnoreDependency
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
    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @Deprecated
    public List<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(Subject subject,
        int autoGroupParentResourceId, int autoGroupChildResourceTypeId, int[] measurementDefinitionIds, long begin,
        long end, boolean enabledOnly) throws MeasurementException {

        List<MetricDisplaySummary> ret = getAggregateMetricDisplaySummaries(subject,
            EntityContext.forAutoGroup(autoGroupParentResourceId, autoGroupChildResourceTypeId),
            measurementDefinitionIds, begin, end, enabledOnly);
        for (MetricDisplaySummary tmp : ret) {
            tmp.setParentId(autoGroupParentResourceId);
            tmp.setChildTypeId(autoGroupChildResourceTypeId);
        }

        return ret;
    }

    public List<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(Subject subject,
        EntityContext context, int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly)
        throws MeasurementException {

        List<MetricDisplaySummary> ret = getAggregateMetricDisplaySummaries(subject, context, measurementDefinitionIds,
            begin, end, enabledOnly);

        int groupId = context.getGroupId();
        for (MetricDisplaySummary tmp : ret) {
            tmp.setGroupId(groupId);
        }

        return ret;
    }

    @Deprecated
    public List<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(Subject subject, int parent, int type,
        String viewName) {
        MeasurementPreferences preferences = new MeasurementPreferences(subject);
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        long begin = rangePreferences.begin;
        long end = rangePreferences.end;

        int[] measurementDefinitionIds;
        try {
            EntityContext context = new EntityContext(-1, -1, parent, type);
            measurementDefinitionIds = fillDefinitionIdsFromUserPreferences(context, viewName, subject);
        } catch (IllegalArgumentException iae) {
            // If we can't get stuff from preferences, get the defaults.
            measurementDefinitionIds = resourceGroupManager.findDefinitionsForAutoGroup(subject, parent, type, true);
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

    public List<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(Subject subject,
        EntityContext context, String viewName) {

        MeasurementPreferences preferences = new MeasurementPreferences(subject);
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        long begin = rangePreferences.begin;
        long end = rangePreferences.end;

        /* Fiddle the metrics from the | delimited stored ones and only display those.
         * Use the default list as fall back if we don't have them in preferences
         */
        int[] measurementDefinitionIds;
        try {
            measurementDefinitionIds = fillDefinitionIdsFromUserPreferences(context, viewName, subject);
        } catch (IllegalArgumentException iae) {
            // If we can't get stuff from preferences, get the defaults.
            measurementDefinitionIds = resourceGroupManager.findDefinitionsForCompatibleGroup(subject,
                context.getGroupId(), true);
        }

        List<MetricDisplaySummary> summaries;
        try {
            summaries = getMetricDisplaySummariesForCompatibleGroup(subject, context, measurementDefinitionIds, begin,
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
    private int[] fillDefinitionIdsFromUserPreferences(EntityContext context, String viewName, Subject subject) {
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

    // TODO: jmarques - combine this into one query to the backend, and sort the data at the object layer
    public List<MetricDisplaySummary> getMetricDisplaySummariesForResource(Subject subject, int resourceId,
        int[] measurementScheduleIds, long beginTime, long endTime) throws MeasurementException {
        List<MetricDisplaySummary> allMeasurementData = new ArrayList<MetricDisplaySummary>(
            measurementScheduleIds.length);
        List<Integer> scheduleIds = new ArrayList<Integer>(measurementScheduleIds.length);
        for (int measurementScheduleId : measurementScheduleIds) {
            MeasurementSchedule schedule = null;
            try {
                schedule = scheduleManager.getScheduleById(subject, measurementScheduleId);
                scheduleIds.add(schedule.getId());
            } catch (MeasurementNotFoundException mnfe) {
                throw new MeasurementException(mnfe);
            }

            MetricDisplaySummary summary = getMetricDisplaySummary(subject, schedule, beginTime, endTime, false);
            if (summary != null) {
                summary.setUnits(schedule.getDefinition().getUnits().name());
                // TODO: jmarques - should we add summary.setResourceId(resourceId) here?
                allMeasurementData.add(summary);
            }
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(beginTime, endTime, scheduleIds);
        for (MetricDisplaySummary sum : allMeasurementData) {
            sum.setAlertCount(alerts.get(sum.getScheduleId()));
        }

        return allMeasurementData;
    }

    public List<MetricDisplaySummary> getMetricDisplaySummariesForResource(Subject subject, int resourceId,
        String viewName) throws MeasurementException {
        List<MeasurementSchedule> scheds;
        /*
         * Try to get the schedules for this view from the preferences and extract the
         * schedule ids from it. If this fails, fall back to defaults.
         */
        try {
            EntityContext context = new EntityContext(resourceId, -1, -1, -1);
            List<String> charts = viewManager.getCharts(subject, context, viewName);

            int[] schIds = new int[charts.size()];
            int i = 0;
            for (String metric : charts) {
                metric = metric.split(",")[1];
                int schedId = Integer.parseInt(metric);
                schIds[i++] = schedId;
            }
            scheds = scheduleManager.findSchedulesByIds(schIds);
            // sort the schedules returned in the order they had in the tokens.
            // the backend unfortunately looses that information
            List<MeasurementSchedule> tmp = new ArrayList<MeasurementSchedule>(scheds.size());
            for (int id : schIds) {
                for (MeasurementSchedule sch : scheds) {
                    if (sch.getId() == id) {
                        tmp.add(sch);
                        break;
                    }
                }
            }
            scheds = tmp;
        } catch (MeasurementViewException mve) {
            // No metrics in preferences? Use defaults for the resource (DisplayType==SUMMARY)
            scheds = scheduleManager.findSchedulesForResourceAndType(subject, resourceId, DataType.MEASUREMENT,
                DisplayType.SUMMARY, false);
        }

        int[] scheduleIds = new int[scheds.size()];
        int index = 0;
        for (MeasurementSchedule sched : scheds) {
            scheduleIds[index++] = sched.getId();
        }

        MeasurementPreferences preferences = new MeasurementPreferences(subject);
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        long begin = rangePreferences.begin;
        long end = rangePreferences.end;

        List<MetricDisplaySummary> idss = getMetricDisplaySummariesForResource(subject, resourceId, scheduleIds, begin,
            end);
        return idss;
    }

    // used for ListChildrenAction
    public List<MetricDisplaySummary> getMetricDisplaySummariesForMetrics(Subject subject, int resourceId,
        DataType dataType, long begin, long end, boolean narrowed, boolean enabledOnly) throws MeasurementException {
        List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();

        List<MeasurementSchedule> scheds = scheduleManager.findSchedulesForResourceAndType( // TODO only get ids
            subject, resourceId, dataType, null, enabledOnly); //null -> don't filter, we want everything

        List<Integer> scheduleIds = new ArrayList<Integer>(scheds.size());

        for (MeasurementSchedule schedule : scheds) {
            MetricDisplaySummary summary = getMetricDisplaySummary(subject, schedule, begin, end, narrowed);
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
    private MetricDisplaySummary getMetricDisplaySummary(Subject subject, MeasurementSchedule schedule, long begin,
        long end, boolean narrowed) throws MeasurementException {
        MetricDisplaySummary summary = new MetricDisplaySummary();
        summary.setScheduleId(schedule.getId());
        summary.setBeginTimeFrame(begin);
        summary.setEndTimeFrame(end);

        MeasurementDefinition definition = schedule.getDefinition();
        summary.setDefinitionId(definition.getId());
        summary.setMetricName(definition.getName());
        summary.setLabel(definition.getDisplayName());
        summary.setDescription(definition.getDescription());
        summary.setMetricSource(schedule.getResource().getName());
        summary.setMetricSourceId(schedule.getResource().getId());

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
                MeasurementAggregate compositeHighLow = dataManager.getMeasurementAggregate(subject, schedule.getId(),
                    begin, end);
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

    private List<MetricDisplaySummary> getAggregateMetricDisplaySummaries(Subject subject, EntityContext context,
        int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly) throws MeasurementException {

        List<MetricDisplaySummary> data = new ArrayList<MetricDisplaySummary>(measurementDefinitionIds.length);

        if (measurementDefinitionIds.length == 0) {
            return data;
        }

        if (context.type == EntityContext.Type.Resource) {
            if (authorizationManager.canViewResource(subject, context.resourceId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view metric display summaries for resource[id="
                    + context.resourceId + "]");
            }
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (authorizationManager.canViewGroup(subject, context.groupId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view metric display summaries for resourceGroup[id="
                    + context.groupId + "]");
            }
        } else if (context.type == EntityContext.Type.AutoGroup) {
            if (authorizationManager.canViewAutoGroup(subject, context.parentResourceId, context.resourceTypeId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view metric display summaries for autoGroup[parentResourceId="
                    + context.parentResourceId + ", resourceTypeId=" + context.resourceTypeId + "]");
            }
        }

        MeasurementDataManagerUtility dataUtil = MeasurementDataManagerUtility.getInstance(rhqDs);

        // Loop over the definitions, find matching schedules and create a MetricDisplaySummary for each definition
        for (int definitionId : measurementDefinitionIds) {

            MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
            if (context.type == EntityContext.Type.Resource) {
                criteria.addFilterResourceId(context.resourceId);
            } else if (context.type == EntityContext.Type.ResourceGroup) {
                criteria.addFilterResourceGroupId(context.groupId);
            } else if (context.type == EntityContext.Type.AutoGroup) {
                criteria.addFilterAutoGroupParentResourceId(context.parentResourceId);
                criteria.addFilterAutoGroupResourceTypeId(context.resourceTypeId);
            }
            criteria.addFilterDefinitionIds(definitionId);
            criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

            PageList<MeasurementSchedule> theSchedules = scheduleManager.findSchedulesByCriteria(subject, criteria);
            int totalScheduleCount = theSchedules.getTotalSize();

            criteria.addFilterEnabled(true);
            criteria.setPageControl(PageControl.getSingleRowInstance()); // get single row only, we want totalSize
            theSchedules = scheduleManager.findSchedulesByCriteria(subject, criteria);
            int collecting = theSchedules.getTotalSize();

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
            summary.setAlertCount(getAlertCountForContext(definitionId, context, begin, end));
            summary.setEndTimeFrame(end);

            MeasurementDefinition definition = entityManager.find(MeasurementDefinition.class, definitionId);
            summary.setDefinitionId(definition.getId());
            summary.setUnits(definition.getUnits().getName());
            summary.setDescription(definition.getDescription());
            summary.setMetricName(definition.getName());
            summary.setLabel(definition.getDisplayName());
            summary.setDescription(definition.getDescription());
            summary.setMetricSource(definition.getResourceType().getName());

            /*
             * Get the aggregate data from the backend and check if it is empty or not. If it is empty (for all members
             * of the group), skip over this metric.
             */
            MeasurementAggregate aggregate;
            if (totalScheduleCount == 0) {
                aggregate = new MeasurementAggregate(null, null, null);
                log.warn("No metric schedules found for def=[" + definition + "] and " + context
                    + ", using empty aggregate");
            } else {
                if (context.type == EntityContext.Type.ResourceGroup
                    && definition.getDataType() == DataType.MEASUREMENT) {
                    aggregate = dataManager.getAggregate(subject, context.getGroupId(), definitionId, begin, end);
                } else {
                    aggregate = dataUtil.getAggregateByDefinitionAndContext(begin, end, definitionId, context);
                }
            }
            if (aggregate.isEmpty()) {
                if (log.isTraceEnabled()) {
                    log.warn("No metric data found for def=[" + definition + "] and " + context + " in the timeframe ["
                        + new Date(begin) + ", " + new Date(end) + "]");
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
        Subject subject, int[] resourceIds, int[] definitionIds, long begin, long end) throws MeasurementException {
        // Getting all the Resource objects in one call, and caching here for the rest of this method
        PageList<Resource> resources = resourceManager.findResourceByIds(subject, resourceIds, true,
            PageControl.getUnlimitedInstance());

        // I want to only get the definition objects once for each ID, and cache here for the rest of this method
        Map<Integer, MeasurementDefinition> measurementDefinitionsMap = new HashMap<Integer, MeasurementDefinition>(
            definitionIds.length);

        // Eliminate definitions not collecting
        List<Integer> collectingDefIdList = new ArrayList<Integer>();
        for (int definitionId : definitionIds) {
            if (isMetricCollecting(subject, resourceIds, definitionId)) {
                collectingDefIdList.add(definitionId);
            }
        }

        Integer[] collectingDefIds = collectingDefIdList.toArray(new Integer[collectingDefIdList.size()]);
        // annoying, we also need an int[]
        int[] collectingDefIdArr = new int[collectingDefIds.length];
        for (int i = 0; (i < collectingDefIds.length); ++i) {
            collectingDefIdArr[i] = collectingDefIds[i];
        }

        List<MeasurementDefinition> definitions = measurementDefinitionManager.findMeasurementDefinitionsByIds(subject,
            collectingDefIds);
        for (MeasurementDefinition definition : definitions) {
            measurementDefinitionsMap.put(definition.getId(), definition);
        }

        Map<MeasurementDefinition, List<MetricDisplaySummary>> compareMetrics = new HashMap<MeasurementDefinition, List<MetricDisplaySummary>>();
        for (Resource resource : resources) {
            List<MeasurementSchedule> scheds = scheduleManager.findSchedulesByResourceIdAndDefinitionIds(subject,
                resource.getId(), collectingDefIdArr);
            int[] schedIds = new int[scheds.size()];
            for (int i = 0; (i < schedIds.length); ++i) {
                schedIds[i] = scheds.get(i).getId();
            }

            List<MetricDisplaySummary> resourceMDS = getMetricDisplaySummariesForResource(subject, resource.getId(),
                schedIds, begin, end);
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
                summary.setMetricName(definition.getName());
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

    private boolean isMetricCollecting(Subject subject, int[] resourceIds, int definitionId) {
        boolean isCollecting = false;
        List<MeasurementSchedule> schedules = scheduleManager.findSchedulesByResourceIdsAndDefinitionId(subject,
            resourceIds, definitionId);
        for (MeasurementSchedule schedule : schedules) {
            if (schedule.isEnabled()) {
                isCollecting = true;
                break;
            }
        }

        return isCollecting;
    }

    private int getAlertCountForContext(int measurementDefinitionId, EntityContext context, long begin, long end) {
        if (context.type == EntityContext.Type.AutoGroup) {
            return alertManager.getAlertCountByMeasurementDefinitionAndAutoGroup(measurementDefinitionId,
                context.getParentResourceId(), context.getResourceTypeId(), begin, end);
        } else if (context.type == EntityContext.Type.Resource) {
            return alertManager.getAlertCountByMeasurementDefinitionAndResource(measurementDefinitionId,
                context.getResourceId(), begin, end);
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            return alertManager.getAlertCountByMeasurementDefinitionAndResourceGroup(measurementDefinitionId,
                context.getGroupId(), begin, end);
        }
        return 0;
    }

}
