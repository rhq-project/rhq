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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.jboss.remoting.CannotConnectException;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceIdWithAgentComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.rest.ResourceHandlerBean;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.RawDataInsertedCallback;
import org.rhq.server.metrics.TraitsDAO;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * A manager for {@link MeasurementData}s.
 *
 * @author Heiko W. Rupp
 * @author Greg Hinkle
 * @author Ian Springer
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class MeasurementDataManagerBean implements MeasurementDataManagerLocal, MeasurementDataManagerRemote {

    private final Log log = LogFactory.getLog(MeasurementDataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;

    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;
    @EJB
    private AlertManagerLocal alertManager;
    @EJB
    //@IgnoreDependency
    private AgentManagerLocal agentClientManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private CallTimeDataManagerLocal callTimeDataManager;
    @EJB
    private MeasurementDataManagerLocal measurementDataManager;
    @EJB
    //@IgnoreDependency
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    @EJB
    private StorageClientManager storageClientManager;

    @EJB
    private MeasurementScheduleManagerLocal measurementScheduleManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void mergeMeasurementReport(MeasurementReport report) {
        long start = System.currentTimeMillis();
        // TODO GH: Deal with offset (this is only for situations where the clock doesn't match on the agent)

        /*
         * even if these methods check for null/empty collections, they cross the EJB boundary and so unnecessarily
         * start transactions.  by checking the null/emptiness of a collection here, by only create transactions
         * when real work will be done;
         */
        if (report.getNumericData() != null && !report.getNumericData().isEmpty()) {
            this.measurementDataManager.addNumericData(report.getNumericData());
        }
        if (report.getTraitData() != null && !report.getTraitData().isEmpty()) {
            this.measurementDataManager.addTraitData(report.getTraitData());
        }
        if (report.getCallTimeData() != null && !report.getCallTimeData().isEmpty()) {
            this.callTimeDataManager.addCallTimeData(report.getCallTimeData());
        }

        long time = System.currentTimeMillis() - start;
        MeasurementMonitor.getMBean().incrementMeasurementInsertTime(time);
        MeasurementMonitor.getMBean().incrementMeasurementsInserted(report.getDataCount());

        if (log.isDebugEnabled()) {
            log.debug("Measurement storage for [" + report.getDataCount() + "] took " + time + "ms");
        }
    }

    /**
     * Add metrics data to the database. Data that is passed can come from several Schedules, but needs to be of only
     * one type of MeasurementGathering. For good performance it is important that the agent sends batches as big as
     * possible (ok, perhaps not more than 100 items at a time).
     *
     * @param data the actual data points
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void addNumericData(final Set<MeasurementDataNumeric> data) {
        if ((data == null) || (data.isEmpty())) {
            return;
        }

        MetricsServer metricsServer = storageClientManager.getMetricsServer();
        metricsServer.addNumericData(data, new RawDataInsertedCallback() {

            private final ReentrantLock lock = new ReentrantLock();

            private final Set<MeasurementData> insertedData = new TreeSet<MeasurementData>(new Comparator<MeasurementData>() {
                @Override
                public int compare(MeasurementData d1, MeasurementData d2) {
                    return (d1.getTimestamp() < d2.getTimestamp()) ? -1 : ((d1.getTimestamp() == d2.getTimestamp()) ? 0 : 1);
                }
            });

            @Override
            public void onFinish() {
                measurementDataManager.updateAlertConditionCache("mergeMeasurementReport",
                    insertedData.toArray(new MeasurementData[insertedData.size()]));
            }

            @Override
            public void onSuccess(MeasurementDataNumeric measurementDataNumeric) {
                try {
                    lock.lock();
                    insertedData.add(measurementDataNumeric);
                }  finally {
                    lock.unlock();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
            }
        });
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void addTraitData(final Set<MeasurementDataTrait> data) {
        TraitsDAO dao = storageClientManager.getTraitsDAO();
        dao.insertTraits(data, new FutureCallback<Object>() {

            @Override
            public void onFailure(Throwable e) {
                log.error("Error persisting trait data " + data.size(), e);
            }

            @Override
            public void onSuccess(Object result) {
                notifyAlertConditionCacheManager("mergeMeasurementReport",
                        data.toArray(new MeasurementData[data.size()]));
            }
        });
    }

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
    @SuppressWarnings("unchecked")
    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricDisplaySummariesForResourcesAndParent(
        Subject subject, int resourceTypeId, int parentId, List<Integer> resourceIds, long begin, long end) {
        Map<Integer, List<MetricDisplaySummary>> sumMap = new HashMap<Integer, List<MetricDisplaySummary>>();
        if ((parentId <= 0) || (resourceIds == null) || (resourceIds.isEmpty()) || (end < begin)) {
            return sumMap;
        }

        /*
         * Get the schedule(ids) for the passed resources and types and stuff them in a MapMap to easier access them
         * afterwards.
         */
        Query q = entityManager.createNamedQuery(MeasurementSchedule.FIND_ENABLED_BY_RESOURCE_IDS_AND_RESOURCE_TYPE_ID);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setParameter("resourceTypeId", resourceTypeId);
        q.setParameter("resourceIds", resourceIds);

        // <schedId, resId, defId>
        List<Object[]> triples = q.getResultList();

        Map<Integer, Map<Integer, Integer>> resDefSchedMap = new HashMap<Integer, Map<Integer, Integer>>();

        List<Integer> scheduleIds = new ArrayList<Integer>(triples.size());
        for (Object[] triple : triples) {
            int sid = (Integer) triple[0];
            scheduleIds.add(sid);
            int res = (Integer) triple[1];
            int def = (Integer) triple[2];
            Map<Integer, Integer> defSchedMap;
            if (!resDefSchedMap.containsKey(res)) {
                defSchedMap = new HashMap<Integer, Integer>();
                resDefSchedMap.put(res, defSchedMap);
            } else {
                defSchedMap = resDefSchedMap.get(res);
            }

            defSchedMap.put(def, sid);
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(begin, end, scheduleIds);

        List<MeasurementDefinition> definitions = measurementDefinitionManager
            .findMeasurementDefinitionsByResourceType(subject, resourceTypeId, DataType.MEASUREMENT, null);
        Map<Integer, MeasurementDefinition> defMap = new HashMap<Integer, MeasurementDefinition>(definitions.size());
        for (MeasurementDefinition def : definitions) {
            defMap.put(def.getId(), def);
        }

        /*
         * Now that we have the data, loop over the data and fill in the MetricDisplaySummaries.
         */
        for (int resourceId : resourceIds) {
            List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();
            if (resDefSchedMap.containsKey(resourceId)) {
                Map<Integer, Integer> defSchedMap = resDefSchedMap.get(resourceId);
                for (int defId : defSchedMap.keySet()) {
                    if (defMap.get(defId) == null) {
                        // This is not a DataType.MEASUREMENT type measurement
                        continue;
                    }

                    int sid = defSchedMap.get(defId);
                    MetricDisplaySummary mds = new MetricDisplaySummary();
                    mds.setAlertCount(alerts.get(sid));
                    mds.setBeginTimeFrame(begin);
                    mds.setEndTimeFrame(end);
                    mds.setDefinitionId(defId);
                    mds.setMetricName(defMap.get(defId).getName());
                    mds.setLabel(defMap.get(defId).getDisplayName());
                    mds.setParentId(parentId);
                    mds.setChildTypeId(resourceTypeId);
                    summaries.add(mds);
                }
            }

            sumMap.put(resourceId, summaries);
        }

        return sumMap;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal#getNarrowedMetricsDisplaySummaryForCompGroup(org.jboss.on.domain.auth.Subject,
     * int)
     */
    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricsDisplaySummariesForCompGroup(Subject subject,
        ResourceGroup group, long beginTime, long endTime) {
        group = entityManager.merge(group);
        Set<Resource> resources = group.getExplicitResources();

        Map<Integer, List<MetricDisplaySummary>> resMap = findNarrowedMetricDisplaySummariesForCompatibleResources(
            subject, resources, beginTime, endTime);

        // loop over the map entries and set the group Id on each list element
        for (List<MetricDisplaySummary> summaries : resMap.values()) {
            for (MetricDisplaySummary sum : summaries) {
                sum.setGroupId(group.getId());
            }
        }

        return resMap;
    }

    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricsDisplaySummariesForAutoGroup(Subject subject,
        int parentId, int cType, long beginTime, long endTime) {
        List<Resource> resources = resourceGroupManager.findResourcesForAutoGroup(subject, parentId, cType);
        Set<Resource> resSet = new HashSet<Resource>(resources.size());

        Map<Integer, List<MetricDisplaySummary>> resMap = findNarrowedMetricDisplaySummariesForCompatibleResources(
            subject, resSet, beginTime, endTime);

        // loop over the map entries and set the group Id on each list element
        for (List<MetricDisplaySummary> summaries : resMap.values()) {
            for (MetricDisplaySummary sum : summaries) {
                sum.setChildTypeId(cType);
                sum.setParentId(parentId);
            }
        }

        return resMap;
    }

    /**
     * Get the {@link MetricDisplaySummary}s for the resources passed in, that all need to be of the same
     * {@link ResourceType}. Summaries only contain a basic selection of fields for the purpose of filling the Child
     * resource popups.
     */
    @SuppressWarnings("unchecked")
    public Map<Integer, List<MetricDisplaySummary>> findNarrowedMetricDisplaySummariesForCompatibleResources(
        Subject subject, Collection<Resource> resources, long beginTime, long endTime) {
        Map<Integer, List<MetricDisplaySummary>> resMap = new HashMap<Integer, List<MetricDisplaySummary>>();

        if ((resources == null) || (resources.isEmpty())) {
            return resMap;
        }

        /*
         * Get the resource type and make sure all resources are of the same type
         */
        Iterator<Resource> it = resources.iterator();
        ResourceType type = it.next().getResourceType();
        boolean found = false;
        while (it.hasNext()) {
            ResourceType tmp = it.next().getResourceType();
            if (tmp != type) {
                found = true;
                break;
            }
        }

        if (found) {
            throw new IllegalArgumentException("Resources were of different type: " + resources);
        }

        Set<MeasurementDefinition> defs = type.getMetricDefinitions();

        // get all schedules that are collecting (=enabled)
        Query q = entityManager.createNamedQuery(MeasurementSchedule.FIND_ENABLED_BY_RESOURCES_AND_RESOURCE_TYPE);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setParameter("resourceType", type);
        q.setParameter("resources", resources);
        q.setParameter("dataType", DataType.MEASUREMENT);

        // <schedId, resId, defId>
        List<Object[]> schedules = q.getResultList();

        Map<Integer, Map<Integer, Integer>> resDefSchedMap = new HashMap<Integer, Map<Integer, Integer>>();

        List<Integer> scheduleIds = new ArrayList<Integer>(schedules.size());
        for (Object[] sched : schedules) {
            int sid = (Integer) sched[0];
            scheduleIds.add(sid);
            int res = (Integer) sched[1];
            int def = (Integer) sched[2];
            Map<Integer, Integer> defSchedMap;
            if (!resDefSchedMap.containsKey(res)) {
                defSchedMap = new HashMap<Integer, Integer>();
                resDefSchedMap.put(res, defSchedMap);
            } else {
                defSchedMap = resDefSchedMap.get(res);
            }

            defSchedMap.put(def, sid);
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(beginTime, endTime, scheduleIds);

        /*
         * Loop over the resources and populate the map with the schedules for the definitions we have There won't be a
         * schedule for each combination, as the list above only contains schedules that are actually collecting. Also
         * if the schedule is not collecting, we don't need to add it to the result.
         */
        for (Resource res : resources) {
            List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();
            for (MeasurementDefinition def : defs) {
                MetricDisplaySummary sum = new MetricDisplaySummary();
                sum.setDefinitionId(def.getId());
                sum.setMetricName(def.getName());
                sum.setLabel(def.getDisplayName());
                sum.setBeginTimeFrame(beginTime);
                sum.setEndTimeFrame(endTime);

                int resId = res.getId();
                if (resDefSchedMap.containsKey(resId)) {
                    Map<Integer, Integer> defSched = resDefSchedMap.get(resId);
                    if (defSched.containsKey(def.getId())) {
                        int sid = defSched.get(def.getId());
                        sum.setScheduleId(sid);
                        sum.setAlertCount(alerts.get(sid));
                        summaries.add(sum);
                    }
                }
            }

            resMap.put(res.getId(), summaries);
        }

        return resMap;
    }

    @Nullable
    public MeasurementDataTrait getCurrentTraitForSchedule(int scheduleId) {
        MeasurementDataTrait trait = storageClientManager.getTraitsDAO().currentTrait(scheduleId);
        return trait;
    }

    @Nullable
    public MeasurementDataNumeric getCurrentNumericForSchedule(int scheduleId) {
        MetricsServer metricsServer = storageClientManager.getMetricsServer();
        RawNumericMetric metric = metricsServer.findLatestValueForResource(scheduleId);
        if(null != metric) {
            return new MeasurementDataNumeric(metric.getTimestamp(), scheduleId, metric.getValue());
        }else {
            return new MeasurementDataNumeric(System.currentTimeMillis(), scheduleId, Double.NaN);
        }
    }

    @Asynchronous
    @Override
    public void updateAlertConditionCache(String callingMethod, MeasurementData[] data) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(data);
        log.debug(callingMethod + ": " + stats.toString());
    }

    private void notifyAlertConditionCacheManager(String callingMethod, MeasurementData[] data) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(data);

        log.debug(callingMethod + ": " + stats.toString());
    }

    @Deprecated
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public org.rhq.enterprise.server.measurement.MeasurementAggregate getAggregate(Subject subject, int scheduleId, long startTime, long endTime) {
        MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
        criteria.addFilterId(scheduleId);
        criteria.fetchResource(true);

        PageList<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByCriteria(
            subjectManager.getOverlord(), criteria);
        if (schedules.isEmpty()) {
            throw new MeasurementException("Could not fine MeasurementSchedule with the id[" + scheduleId + "]");
        }
        MeasurementSchedule schedule = schedules.get(0);

        if (authorizationManager.canViewResource(subject, schedule.getResource().getId()) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view schedule[id=" + scheduleId + "]");
        }

        if (schedule.getDefinition().getDataType() != DataType.MEASUREMENT) {
            throw new IllegalArgumentException(schedule + " is not about numerical values. Can't compute aggregates");
        }

        if (startTime > endTime) {
            throw new IllegalArgumentException("Start date " + startTime + " is not before " + endTime);
        }

        MetricsServer metricsServer = storageClientManager.getMetricsServer();
        AggregateNumericMetric summary = metricsServer.getSummaryAggregate(scheduleId, startTime, endTime);

        return new org.rhq.enterprise.server.measurement.MeasurementAggregate(summary.getMin(), summary.getAvg(), summary.getMax());
    }

    public MeasurementAggregate getMeasurementAggregate(Subject subject, int scheduleId, long startTime, long endTime) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
            criteria.addFilterId(scheduleId);
            criteria.fetchResource(true);

            PageList<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByCriteria(
                subjectManager.getOverlord(), criteria);
            if (schedules.isEmpty()) {
                throw new MeasurementException("Could not fine MeasurementSchedule with the id[" + scheduleId + "]");
            }
            MeasurementSchedule schedule = schedules.get(0);

            if (authorizationManager.canViewResource(subject, schedule.getResource().getId()) == false) {
                throw new PermissionException("User[" + subject.getName()
                    + "] does not have permission to view schedule[id=" + scheduleId + "]");
            }

            if (schedule.getDefinition().getDataType() != DataType.MEASUREMENT) {
                throw new IllegalArgumentException(schedule + " is not about numerical values. Can't compute aggregates");
            }

            if (startTime > endTime) {
                throw new IllegalArgumentException("Start date " + startTime + " is not before " + endTime);
            }

            MetricsServer metricsServer = storageClientManager.getMetricsServer();
            AggregateNumericMetric summary = metricsServer.getSummaryAggregate(scheduleId, startTime, endTime);

            return new MeasurementAggregate(summary.getMin(), summary.getAvg(), summary.getMax());
        } finally {
            stopwatch.stop();
            log.debug("Finished loading measurement aggregate in " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MeasurementAggregate getAggregate(Subject subject, int groupId, int definitionId, long startTime,
        long endTime) {

        if (authorizationManager.canViewGroup(subject, groupId) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to calculate measurement aggregate for group[id=" + groupId
                + "], definition[id=" + definitionId + "]");
        }

        MeasurementDefinition def = measurementDefinitionManager.getMeasurementDefinition(subject, definitionId);
        if (def.getDataType() != DataType.MEASUREMENT) {
            throw new IllegalArgumentException(def + " is not about numerical values. Can't compute aggregates");
        }

        if (startTime > endTime) {
            throw new IllegalArgumentException("Start date " + startTime + " is not before " + endTime);
        }

        MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
        criteria.addFilterResourceGroupId(groupId);
        criteria.addFilterDefinitionIds(definitionId);
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        PageList<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByCriteria(subject,
            criteria);

        MetricsServer metricsServer = storageClientManager.getMetricsServer();
        AggregateNumericMetric summary = metricsServer.getSummaryAggregate(map(schedules), startTime, endTime);

        return new MeasurementAggregate(summary.getMin(), summary.getAvg(), summary.getMax());
    }

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
    public List<MeasurementDataTrait> findCurrentTraitsForResource(Subject subject, int resourceId,
        DisplayType displayType) {
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view traits for resource[id=" + resourceId + "]");
        }

        MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
        criteria.addFilterResourceId(resourceId);
        criteria.addFilterDisplayType(displayType);
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        PageList<MeasurementSchedule> list = measurementScheduleManager.findSchedulesByCriteria(subjectManager.getOverlord(), criteria);

        TraitsDAO dao = storageClientManager.getTraitsDAO();

        List<MeasurementDataTrait> traits = dao.queryAll(list, false);
        for (MeasurementDataTrait trait : traits) {
            MeasurementSchedule sched = entityManager.find(MeasurementSchedule.class, trait.getScheduleId());
            trait.setSchedule(sched);
            trait.setName(sched.getDefinition().getName());
        }

        Collections.sort(traits, new Comparator<MeasurementDataTrait>() {
            public int compare(MeasurementDataTrait t1, MeasurementDataTrait t2) {
                int o1 = t1.getSchedule().getDefinition().getDisplayOrder();
                int o2 = t2.getSchedule().getDefinition().getDisplayOrder();
                return o1 - o2;
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("getCurrentTraitsForResource(" + resourceId + ") -> result is " + traits);
        }

        return traits;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(Subject subject, int groupId,
        int definitionId, long beginTime, long endTime, int numPoints) {

        List<List<MeasurementDataNumericHighLowComposite>> ret = findDataForContext(subject,
            EntityContext.forGroup(groupId), definitionId, beginTime, endTime, numPoints);
        return ret;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<List<MeasurementDataNumericHighLowComposite>> findDataForContext(Subject subject,
        EntityContext context, int definitionId, long beginTime, long endTime, int numDataPoints) {
        MetricsServer metricsServer = storageClientManager.getMetricsServer();

        if (context.type == EntityContext.Type.Resource) {
            if (!authorizationManager.canViewResource(subject, context.resourceId)) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view measurement data for resource[id=" + context.resourceId + "]");
            }
            MeasurementSchedule schedule = measurementScheduleManager.getSchedule(subject, context.getResourceId(),
                definitionId, false);
            List<List<MeasurementDataNumericHighLowComposite>> data =
                new ArrayList<List<MeasurementDataNumericHighLowComposite>>();

            List<MeasurementDataNumericHighLowComposite> tempList = new ArrayList<MeasurementDataNumericHighLowComposite>();
            for (MeasurementDataNumericHighLowComposite object : metricsServer.findDataForResource(schedule.getId(),
                beginTime, endTime, numDataPoints)) {
                tempList.add(object);
            }
            data.add(tempList);

            return data;
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (!authorizationManager.canViewGroup(subject, context.groupId)) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view measurement data for resourceGroup[id=" + context.groupId
                    + "]");
            }
            MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
            criteria.addFilterResourceGroupId(context.getGroupId());
            criteria.addFilterDefinitionIds(definitionId);
            criteria.setPageControl(PageControl.getUnlimitedInstance());
            PageList<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByCriteria(subject,
                criteria);
            List<List<MeasurementDataNumericHighLowComposite>> data =
                new ArrayList<List<MeasurementDataNumericHighLowComposite>>();

            List<MeasurementDataNumericHighLowComposite> tempList = new ArrayList<MeasurementDataNumericHighLowComposite>();
            for (MeasurementDataNumericHighLowComposite object : metricsServer.findDataForGroup(map(schedules),
                beginTime, endTime,numDataPoints)) {
                tempList.add(object);
            }
            data.add(tempList);

            return data;
        } else {
            throw new UnsupportedOperationException("The findDataForContext method does not support " +
                context);
        }
    }

    private List<Integer> map(List<MeasurementSchedule> schedules) {
        List<Integer> scheduleIds = new ArrayList<Integer>(schedules.size());
        for (MeasurementSchedule schedule : schedules) {
            scheduleIds.add(schedule.getId());
        }
        return scheduleIds;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(Subject subject, int resourceId,
        int[] definitionIds, long beginTime, long endTime, int numDataPoints) {

        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view measurement data for resource[id=" + resourceId + "]");
        }

        MetricsServer metricsServer = storageClientManager.getMetricsServer();
        List<List<MeasurementDataNumericHighLowComposite>> results =
            new ArrayList<List<MeasurementDataNumericHighLowComposite>>();
        for (int nextDefinitionId : definitionIds) {
            MeasurementSchedule schedule = measurementScheduleManager.getSchedule(subject, resourceId, nextDefinitionId,
                false);

            List<MeasurementDataNumericHighLowComposite> tempList = new ArrayList<MeasurementDataNumericHighLowComposite>();
            for(MeasurementDataNumericHighLowComposite object : metricsServer.findDataForResource(schedule.getId(),
                beginTime, endTime,numDataPoints) ){
                tempList.add(object);
            }

            results.add(tempList);
        }

        return results;
    }

    @Override
    public Set<MeasurementData> findLiveData(Subject subject, int resourceId, int[] definitionIds) {
        // use default timeout
        return findLiveData(subject, resourceId, definitionIds, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<MeasurementData> findLiveData(Subject subject, int resourceId, int[] definitionIds, Long timeout) {
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view live measurement data for resource[id=" + resourceId + "]");
        }
        // return an empty collection if no definition ids were provided
        if (definitionIds == null || definitionIds.length == 0) {
            return emptySet();
        }

        Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_RESOURCE_ID);
        query.setParameter("resourceId", resourceId);
        Agent agent = (Agent) query.getSingleResult();

        // return empty data if the agent is the dummy one
        if (agent.getName().startsWith(ResourceHandlerBean.DUMMY_AGENT_NAME_PREFIX)
            && agent.getAgentToken().startsWith(ResourceHandlerBean.DUMMY_AGENT_TOKEN_PREFIX)) {
            return emptySet();
        }

        query = entityManager.createNamedQuery(MeasurementSchedule.FIND_BY_RESOURCE_IDS_AND_DEFINITION_IDS);
        query.setParameter("definitionIds", ArrayUtils.wrapInList(definitionIds));
        query.setParameter("resourceIds", Arrays.asList(resourceId));
        List<MeasurementSchedule> schedules = query.getResultList();

        Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>(schedules.size());
        for (MeasurementSchedule schedule : schedules) {
            requests.add(new MeasurementScheduleRequest(schedule));
        }

        Set<MeasurementData> result = null;
        try {
            AgentClient ac = agentClientManager.getAgentClient(agent);
            result = ac.getMeasurementAgentService(timeout).getRealTimeMeasurementValue(resourceId, requests);

        } catch (RuntimeException e) {
            if (e instanceof CannotConnectException //
                || (null != e.getCause() && (e.getCause() instanceof TimeoutException))) {

                // ignore timeouts and connect issue,  just return an empty result and keep the logs clean
            } else {
                throw e;
            }
        }

        if (result != null && !result.isEmpty()) {
            //we just got data from the agent so let's push them through the alerting
            pushToAlertSubsystem(result);
        }

        //[BZ 760139] always return non-null value even when there are errors on the server side.  Avoids cryptic
        //            Global UI Exceptions when attempting to serialize null responses.
        if (null == result) {
            result = emptySet();
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<MeasurementData> findLiveDataForGroup(Subject subject, int groupId, int resourceIds[],
        int[] definitionIds) {
        if (authorizationManager.canViewGroup(subject, groupId) == false) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view measurement data for resourceGroup[id=" + groupId + "]");
        }
        // return an empty collection if no definition ids were provided
        if (definitionIds == null || definitionIds.length == 0) {
            return emptySet();
        }

        Set<MeasurementData> values = new HashSet<MeasurementData>();
        if (resourceIds != null) {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_RESOURCE_IDS_WITH_AGENTS_BY_RESOURCE_IDS);
            query.setParameter("resourceIds", ArrayUtils.wrapInList(resourceIds));
            List<ResourceIdWithAgentComposite> resourceIdsWithAgents = query.getResultList();

            for (ResourceIdWithAgentComposite resourceIdWithAgent : resourceIdsWithAgents) {
                // return empty data if the agent is the dummy one
                if (resourceIdWithAgent.getAgent().getName().startsWith(ResourceHandlerBean.DUMMY_AGENT_NAME_PREFIX)
                    && resourceIdWithAgent.getAgent().getAgentToken()
                        .startsWith(ResourceHandlerBean.DUMMY_AGENT_TOKEN_PREFIX)) {
                    values.addAll(Collections.<MeasurementData>emptySet());
                    continue;
                }

                query = entityManager.createNamedQuery(MeasurementSchedule.FIND_BY_RESOURCE_IDS_AND_DEFINITION_IDS);
                query.setParameter("definitionIds", ArrayUtils.wrapInList(definitionIds));
                query.setParameter("resourceIds", Arrays.asList(resourceIdWithAgent.getResourceId()));
                List<MeasurementSchedule> schedules = query.getResultList();

                Map<Integer, Integer> scheduleIdToResourceIdMap = new HashMap<Integer, Integer>(schedules.size());
                Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>(schedules.size());
                for (MeasurementSchedule schedule : schedules) {
                    requests.add(new MeasurementScheduleRequest(schedule));
                    scheduleIdToResourceIdMap.put(schedule.getId(), resourceIdWithAgent.getResourceId());
                }

                AgentClient ac = agentClientManager.getAgentClient(resourceIdWithAgent.getAgent());
                Set<MeasurementData> newValues = ac.getMeasurementAgentService().getRealTimeMeasurementValue(
                    resourceIdWithAgent.getResourceId(), requests);
                values.addAll(newValues);

                // Add the resource id as a prefix of the name, because the name is not unique across different platforms
                for (MeasurementData value : newValues) {
                    value.setName(String.valueOf(scheduleIdToResourceIdMap.get(value.getScheduleId())) + ":"
                        + value.getName());
                }
            }
        }

        if (values != null && !values.isEmpty()) {
            //we just got data from the agent so let's push them through the alerting
            pushToAlertSubsystem(values);
        }


        return values;
    }

    @Override
    public List<MeasurementDataNumeric> findRawData(Subject subject, int scheduleId, long startTime, long endTime) {

        List<MeasurementDataNumeric> result = new ArrayList<MeasurementDataNumeric>();
        String table = MeasurementDataManagerUtility.getCurrentRawTable();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = rhqDs.getConnection();
            ps = connection.prepareStatement( // TODO supply real impl that spans multiple tables
                "SELECT time_stamp,value FROM " + table + " WHERE schedule_id= ? AND time_stamp BETWEEN ? AND ?");
            ps.setLong(1, scheduleId);
            ps.setLong(2, startTime);
            ps.setLong(3, endTime);
            rs = ps.executeQuery();

            while (rs.next()) {
                MeasurementDataNumeric point = new MeasurementDataNumeric(rs.getLong(1), scheduleId, rs.getDouble(2));
                result.add(point);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // TODO: Customise this generated block
        } finally {
            JDBCUtil.safeClose(connection, ps, rs);
        }

        return result;
    }

    /**
     * Return all known trait data for the passed schedule, defined by resourceId and definitionId
     *
     * @param  resourceId   PK of a {@link Resource}
     * @param  definitionId PK of a {@link MeasurementDefinition}
     *
     * @return a List of {@link MeasurementDataTrait} objects.
     */
    @SuppressWarnings("unchecked")
    public List<MeasurementDataTrait> findTraits(Subject subject, int resourceId, int definitionId) {
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view trait data for resource[id=" + resourceId
                + "] and definition[id=" + definitionId + "]");
        }

        // todo optimize this
        List<MeasurementSchedule> scheds = measurementScheduleManager.
                findSchedulesByResourceIdsAndDefinitionIds(new int[] { resourceId }, new int[] { definitionId});
        if (scheds.isEmpty()) {
            return emptyList();
        }
        MeasurementSchedule sched = scheds.get(0);
        List<MeasurementDataTrait> traits = storageClientManager.getTraitsDAO().historyFor(sched.getId());
        return traits;
    }

    public PageList<MeasurementDataTrait> findTraitsByCriteria(Subject subject, MeasurementDataTraitCriteria criteria) {

        // Query everything by schedule, then load the traits from the RHQ storage
        MeasurementScheduleCriteria scriteria = new MeasurementScheduleCriteria(criteria);
        // Used to get trait names
        scriteria.fetchDefinition(true);

        // If the query is filtered by group id, also fetch the Resource for
        // each schedule, so the results include the Resource names.
        boolean filterGroup = criteria.getFilterGroupId() != null;
        scriteria.fetchResource(filterGroup);
        PageList<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByCriteria(
            subjectManager.getOverlord(), scriteria);

        List<MeasurementDataTrait> traits;
        TraitsDAO traitsDAO = storageClientManager.getTraitsDAO();

        // for permission checking (later)
        Set<Integer> resources = new HashSet<Integer>();

        if (criteria.isFilterMaxTimestamp()) {
            traits = traitsDAO.queryAll(schedules, false);
        } else {
            traits = traitsDAO.queryAll(schedules, true);
        }

        for (MeasurementSchedule schedule : schedules) {
            int resourceId = schedule.getResource().getId();
            resources.add(resourceId);

            if (filterGroup) {
                schedule.getResource().getName();
            }
        }

        // Fetch the metric definition for each schedule, so the results include the trait names.
        for (MeasurementDataTrait trait : traits) {
            MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, trait.getScheduleId());
            trait.setSchedule(schedule);
            trait.setName(schedule.getDefinition().getName());
        }

        if (criteria.getSortTimestamp() == PageOrdering.ASC) {
            Collections.reverse(traits);
        }

        for (int resourceId : resources) {
            if (!authorizationManager.canViewResource(subject, resourceId)) {
                throw new PermissionException("User[" + subject.getName()
                    + "] cannot view resource[id=" + resourceId + "]");
            }
        }

        PageList<MeasurementDataTrait> tresults = new PageList<MeasurementDataTrait>(traits, PageControl.getUnlimitedInstance());
        tresults.setTotalSize(traits.size());
        return tresults;
    }

    private void pushToAlertSubsystem(Set<MeasurementData> data) {
        MeasurementReport fakeReport = new MeasurementReport();
        for(MeasurementData datum : data) {
            if (datum instanceof MeasurementDataTrait) {
                fakeReport.addData((MeasurementDataTrait) datum);
            } else if (datum instanceof MeasurementDataNumeric) {
                fakeReport.addData((MeasurementDataNumeric) datum);
            }
        }

        this.measurementDataManager.mergeMeasurementReport(fakeReport);
    }

    public void cleanupTraitHistory(Date after) {
        TraitsDAO dao = storageClientManager.getTraitsDAO();
        dao.cleanup(after);
    }

}
