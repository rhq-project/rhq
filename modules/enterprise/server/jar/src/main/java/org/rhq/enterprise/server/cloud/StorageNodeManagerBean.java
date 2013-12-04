/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
package org.rhq.enterprise.server.cloud;

import static java.util.Arrays.asList;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.rest.reporting.MeasurementConverter;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.storage.StorageClientManagerBean;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerLocal;
import org.rhq.enterprise.server.storage.StorageNodeOperationsHandlerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 *
 * @author Stefan Negrea, Jiri Kremser
 */
@Stateless
public class StorageNodeManagerBean implements StorageNodeManagerLocal, StorageNodeManagerRemote {

    private final Log log = LogFactory.getLog(StorageNodeManagerBean.class);

    private static final String RHQ_STORAGE_CQL_PORT_PROPERTY = "nativeTransportPort";
    private static final String RHQ_STORAGE_GOSSIP_PORT_PROPERTY = "storagePort";
    private static final String RHQ_STORAGE_JMX_PORT_PROPERTY = "jmxPort";
    private static final String RHQ_STORAGE_ADDRESS_PROPERTY = "host";

    private static final int OPERATION_QUERY_TIMEOUT = 20000;
    private static final int MAX_ITERATIONS = 10;
    private static final String UPDATE_CONFIGURATION_OPERATION = "updateConfiguration";
    private static final String RESTART_OPERATION = "restart";

    // metric names on Storage Service resource
    private static final String METRIC_TOKENS = "Tokens", METRIC_OWNERSHIP = "Ownership";
    private static final String METRIC_DATA_DISK_USED_PERCENTAGE = "Calculated.DataDiskUsedPercentage";
    private static final String METRIC_TOTAL_DISK_USED_PERCENTAGE = "Calculated.TotalDiskUsedPercentage";
    private static final String METRIC_FREE_DISK_TO_DATA_RATIO = "Calculated.FreeDiskToDataSizeRatio";
    private static final String METRIC_LOAD = "Load", METRIC_KEY_CACHE_SIZE = "KeyCacheSize",
        METRIC_ROW_CACHE_SIZE = "RowCacheSize", METRIC_TOTAL_COMMIT_LOG_SIZE = "TotalCommitlogSize";

    //metric names on Memory Subsystem resource
    private static final String METRIC_HEAP_COMMITED = "{HeapMemoryUsage.committed}",
        METRIC_HEAP_USED = "{HeapMemoryUsage.used}", METRIC_HEAP_USED_PERCENTAGE = "Calculated.HeapUsagePercentage";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private MeasurementDataManagerLocal measurementManager;

    @EJB
    private SchedulerLocal quartzScheduler;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private OperationManagerLocal operationManager;

    @EJB
    private AlertManagerLocal alertManager;

    @EJB
    private ConfigurationManagerLocal configurationManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManger;

    @EJB
    private StorageClientManagerBean storageClientManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private StorageClusterSettingsManagerLocal storageClusterSettingsManager;

    @EJB
    private StorageNodeOperationsHandlerLocal storageNodeOperationsHandler;

    @Override
    public void linkResource(Resource resource) {
        Configuration pluginConfig = resource.getPluginConfiguration();
        String address = pluginConfig.getSimpleValue(RHQ_STORAGE_ADDRESS_PROPERTY);

        if (log.isInfoEnabled()) {
            log.info("Linking " + resource + " to storage node at " + address);
        }
        try {
            StorageNode storageNode = findStorageNodeByAddress(address);
            if (storageNode == null) {
                if (InetAddresses.isInetAddress(address)) {
                    String hostName = InetAddresses.forString(address).getHostName();
                    log.info("Did not find storage node with address [" + address + "]. Searching by hostname [" +
                        hostName + "]");
                    storageNode = findStorageNodeByAddress(hostName);
                } else {
                    String ipAddress = InetAddress.getByName(address).getHostAddress();
                    log.info("Did not find storage node with address [" + address + "] Searching by IP address [" +
                        ipAddress + "]");
                    storageNode = findStorageNodeByAddress(ipAddress);
                }
            }

            if (storageNode != null) {
                if (log.isInfoEnabled()) {
                    log.info(storageNode + " is an existing storage node. No cluster maintenance is necessary.");
                }
                storageNode.setAddress(address);
                storageNode.setResource(resource);
                storageNode.setOperationMode(OperationMode.NORMAL);
            } else {
                StorageClusterSettings clusterSettings = storageClusterSettingsManager.getClusterSettings(
                    subjectManager.getOverlord());
                storageNode = createStorageNode(resource, clusterSettings);

                if (log.isInfoEnabled()) {
                    log.info("Scheduling cluster maintenance to deploy " + storageNode + " into the storage cluster...");
                }
                if (clusterSettings.getAutomaticDeployment()) {
                    log.info("Deploying " + storageNode);
                    deployStorageNode(subjectManager.getOverlord(), storageNode);
                } else {
                    log.info("Automatic deployment is disabled. " + storageNode + " will not become part of the " +
                        "cluster until it is deployed.");
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not resolve address [" + address + "]. The resource " + resource +
                " cannot be linked to a storage node", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public StorageNode createStorageNode(Resource resource, StorageClusterSettings clusterSettings) {
        Configuration pluginConfig = resource.getPluginConfiguration();

        StorageNode storageNode = new StorageNode();
        storageNode.setAddress(pluginConfig.getSimpleValue(RHQ_STORAGE_ADDRESS_PROPERTY));
        storageNode.setCqlPort(clusterSettings.getCqlPort());
        storageNode.setResource(resource);
        storageNode.setOperationMode(OperationMode.INSTALLED);

        entityManager.persist(storageNode);

        return storageNode;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void deployStorageNode(Subject subject, StorageNode storageNode) {
        storageNode = entityManager.find(StorageNode.class, storageNode.getId());

        switch (storageNode.getOperationMode()) {
            case INSTALLED:
                storageNode.setOperationMode(OperationMode.ANNOUNCE);
            case ANNOUNCE:
                reset();
                storageNodeOperationsHandler.announceStorageNode(subject, storageNode);
                break;
            case BOOTSTRAP:
                reset();
                storageNodeOperationsHandler.bootstrapStorageNode(subject, storageNode);
                break;
            case ADD_MAINTENANCE:
                reset();
                storageNodeOperationsHandler.performAddNodeMaintenance(subject, storageNode);
                break;
            default:
                // TODO what do we do with/about maintenance mode?

                // We do not want to deploying a node that is in the process of being
                // undeployed. It is too hard to make sure we are in an inconsistent state.
                // Instead finishe the undeployment and redeploy the storage node.
                throw new RuntimeException("Cannot deploy " + storageNode);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void undeployStorageNode(Subject subject, StorageNode storageNode) {
        storageNode = entityManager.find(StorageNode.class, storageNode.getId());
        switch (storageNode.getOperationMode()) {
            case INSTALLED:
                reset();
                storageNodeOperationsHandler.uninstall(subject, storageNode);
                break;
            case ANNOUNCE:
            case BOOTSTRAP:
                reset();
                storageNodeOperationsHandler.unannounceStorageNode(subject, storageNode);
                break;
            case ADD_MAINTENANCE:
            case NORMAL:
            case DECOMMISSION:
                reset();
                storageNodeOperationsHandler.decommissionStorageNode(subject, storageNode);
                break;
            case REMOVE_MAINTENANCE:
                reset();
                storageNodeOperationsHandler.performRemoveNodeMaintenance(subject, storageNode);
                break;
            case UNANNOUNCE:
                reset();
                storageNodeOperationsHandler.unannounceStorageNode(subject, storageNode);
                break;
            case UNINSTALL:
                reset();
                storageNodeOperationsHandler.uninstall(subject, storageNode);
                break;
            default:
                // TODO what do we do with/about maintenance mode
                throw new RuntimeException("Cannot undeploy " + storageNode);
        }
    }

    private void reset() {
        for (StorageNode storageNode : getClusterNodes()) {
            storageNode.setErrorMessage(null);
            storageNode.setFailedOperation(null);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            if (!storageClientManager.isClusterAvailable()) {
                return new StorageNodeLoadComposite(node, beginTime, endTime);
            }
            int storageNodeResourceId;
            try {
                storageNodeResourceId = getResourceIdFromStorageNode(node);
            } catch (ResourceNotFoundException e) {
                log.warn(e.getMessage());
                return new StorageNodeLoadComposite(node, beginTime, endTime);
            }
            Map<String, Integer> scheduleIdsMap = new HashMap<String, Integer>();

            for (Object[] tupple : getChildrenScheduleIds(storageNodeResourceId, false)) {
                String definitionName = (String) tupple[0];
                Integer scheduleId = (Integer) tupple[2];
                scheduleIdsMap.put(definitionName, scheduleId);
            }
            for (Object[] tupple : getGrandchildrenScheduleIds(storageNodeResourceId, false)) {
                String definitionName = (String) tupple[0];
                Integer scheduleId = (Integer) tupple[2];
                scheduleIdsMap.put(definitionName, scheduleId);
            }

            StorageNodeLoadComposite result = new StorageNodeLoadComposite(node, beginTime, endTime);
            MeasurementAggregate totalDiskUsedAggregate = new MeasurementAggregate(0d, 0d, 0d);
            Integer scheduleId = null;

            // find the aggregates and enrich the result instance
            if (!scheduleIdsMap.isEmpty()) {
                try {
                    if ((scheduleId = scheduleIdsMap.get(METRIC_TOKENS)) != null) {
                        MeasurementAggregate tokensAggregate = measurementManager.getMeasurementAggregate(subject,
                            scheduleId, beginTime, endTime);
                        result.setTokens(tokensAggregate);
                    }
                    if ((scheduleId = scheduleIdsMap.get(METRIC_OWNERSHIP)) != null) {
                        StorageNodeLoadComposite.MeasurementAggregateWithUnits ownershipAggregateWithUnits = getMeasurementAggregateWithUnits(
                            subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                        result.setActuallyOwns(ownershipAggregateWithUnits);
                    }

                    //calculated disk space related metrics
                    if ((scheduleId = scheduleIdsMap.get(METRIC_DATA_DISK_USED_PERCENTAGE)) != null) {
                        StorageNodeLoadComposite.MeasurementAggregateWithUnits dataDiskUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                            subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                        result.setDataDiskUsedPercentage(dataDiskUsedPercentageAggregateWithUnits);
                    }
                    if ((scheduleId = scheduleIdsMap.get(METRIC_TOTAL_DISK_USED_PERCENTAGE)) != null) {
                        StorageNodeLoadComposite.MeasurementAggregateWithUnits totalDiskUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                            subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                        result.setTotalDiskUsedPercentage(totalDiskUsedPercentageAggregateWithUnits);
                    }
                    if ((scheduleId = scheduleIdsMap.get(METRIC_FREE_DISK_TO_DATA_RATIO)) != null) {
                        MeasurementAggregate freeDiskToDataRatioAggregate = measurementManager.getMeasurementAggregate(
                            subject, scheduleId, beginTime, endTime);
                        result.setFreeDiskToDataSizeRatio(freeDiskToDataRatioAggregate);
                    }

                    if ((scheduleId = scheduleIdsMap.get(METRIC_LOAD)) != null) {
                        StorageNodeLoadComposite.MeasurementAggregateWithUnits loadAggregateWithUnits = getMeasurementAggregateWithUnits(
                            subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                        result.setLoad(loadAggregateWithUnits);

                        updateAggregateTotal(totalDiskUsedAggregate, loadAggregateWithUnits.getAggregate());
                    }
                    if ((scheduleId = scheduleIdsMap.get(METRIC_KEY_CACHE_SIZE)) != null) {
                        updateAggregateTotal(totalDiskUsedAggregate,
                            measurementManager.getMeasurementAggregate(subject, scheduleId, beginTime, endTime));

                    }
                    if ((scheduleId = scheduleIdsMap.get(METRIC_ROW_CACHE_SIZE)) != null) {
                        updateAggregateTotal(totalDiskUsedAggregate,
                            measurementManager.getMeasurementAggregate(subject, scheduleId, beginTime, endTime));
                    }

                    if ((scheduleId = scheduleIdsMap.get(METRIC_TOTAL_COMMIT_LOG_SIZE)) != null) {
                        updateAggregateTotal(totalDiskUsedAggregate,
                            measurementManager.getMeasurementAggregate(subject, scheduleId, beginTime, endTime));
                    }
                    if (totalDiskUsedAggregate.getMax() > 0) {
                        StorageNodeLoadComposite.MeasurementAggregateWithUnits totalDiskUsedAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                            totalDiskUsedAggregate, MeasurementUnits.BYTES);
                        totalDiskUsedAggregateWithUnits.setFormattedValue(getSummaryString(totalDiskUsedAggregate,
                            MeasurementUnits.BYTES));
                        result.setDataDiskUsed(totalDiskUsedAggregateWithUnits);
                    }

                    if ((scheduleId = scheduleIdsMap.get(METRIC_HEAP_COMMITED)) != null) {
                        StorageNodeLoadComposite.MeasurementAggregateWithUnits heapCommittedAggregateWithUnits = getMeasurementAggregateWithUnits(
                            subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                        result.setHeapCommitted(heapCommittedAggregateWithUnits);
                    }
                    if ((scheduleId = scheduleIdsMap.get(METRIC_HEAP_USED)) != null) {
                        StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedAggregateWithUnits = getMeasurementAggregateWithUnits(
                            subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                        result.setHeapUsed(heapUsedAggregateWithUnits);
                    }
                    if ((scheduleId = scheduleIdsMap.get(METRIC_HEAP_USED_PERCENTAGE)) != null) {
                        StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                            subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                        result.setHeapPercentageUsed(heapUsedPercentageAggregateWithUnits);
                    }
                } catch (NoHostAvailableException nhae) {
                    // storage cluster went down while performing this method
                    return new StorageNodeLoadComposite(node, beginTime, endTime);
                }
            }

            return result;
        } finally {
            stopwatch.stop();
            log.info("Retrieved load metrics for " + node + " in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }
    

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public ListenableFuture<List<StorageNodeLoadComposite>> getLoadAsync(Subject subject, StorageNode node,
        long beginTime, long endTime) {
        Stopwatch stopwatch = new Stopwatch().start();
        final StorageNodeLoadComposite result = new StorageNodeLoadComposite(node, beginTime, endTime);
        try {
            if (!storageClientManager.isClusterAvailable()) {
                return Futures.successfulAsList(Lists.newArrayList(Futures.immediateFuture(result)));
            }
            int storageNodeResourceId;
            try {
                storageNodeResourceId = getResourceIdFromStorageNode(node);
            } catch (ResourceNotFoundException e) {
                log.warn(e.getMessage());
                return Futures.successfulAsList(Lists.newArrayList(Futures.immediateFuture(result)));
            }
            MetricsServer metricsServer = storageClientManager.getMetricsServer();
            Map<String, Integer> scheduleIdsMap = new HashMap<String, Integer>();

            for (Object[] tupple : getChildrenScheduleIds(storageNodeResourceId, true)) {
                String definitionName = (String) tupple[0];
                Integer scheduleId = (Integer) tupple[2];
                scheduleIdsMap.put(definitionName, scheduleId);
            }
            for (Object[] tupple : getGrandchildrenScheduleIds(storageNodeResourceId, true)) {
                String definitionName = (String) tupple[0];
                Integer scheduleId = (Integer) tupple[2];
                scheduleIdsMap.put(definitionName, scheduleId);
            }

            List<ListenableFuture<StorageNodeLoadComposite>> compositeFutures = new ArrayList<ListenableFuture<StorageNodeLoadComposite>>();
            final MeasurementAggregate totalDiskUsedAggregate = new MeasurementAggregate(0d, 0d, 0d);
            Integer scheduleId = null;

            // find the aggregates and enrich the result instance
            if (scheduleIdsMap.isEmpty()) {
                // no sheduled metrics yet
                return Futures.successfulAsList(Lists.newArrayList(Futures.immediateFuture(result)));
            }

            if ((scheduleId = scheduleIdsMap.get(METRIC_FREE_DISK_TO_DATA_RATIO)) != null) {
                ListenableFuture<AggregateNumericMetric> dataFuture = metricsServer.getSummaryAggregateAsync(
                    scheduleId, beginTime, endTime);
                ListenableFuture<StorageNodeLoadComposite> compositeFuture = Futures.transform(dataFuture,
                    new Function<AggregateNumericMetric, StorageNodeLoadComposite>() {
                        @Override
                        public StorageNodeLoadComposite apply(AggregateNumericMetric metric) {
                            result.setFreeDiskToDataSizeRatio(new MeasurementAggregate(metric.getMin(),
                                metric.getAvg(), metric.getMax()));
                            return result;
                        }
                    });
                compositeFutures.add(wrapFuture(compositeFuture, result, "Failed to retrieve metric ["
                    + METRIC_FREE_DISK_TO_DATA_RATIO + "] data for " + node));
            }
            if ((scheduleId = scheduleIdsMap.get(METRIC_HEAP_USED_PERCENTAGE)) != null) {
                ListenableFuture<StorageNodeLoadComposite.MeasurementAggregateWithUnits> dataFuture = getMeasurementAggregateWithUnitsAsync(
                    scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                ListenableFuture<StorageNodeLoadComposite> compositeFuture = Futures.transform(dataFuture,
                    new Function<StorageNodeLoadComposite.MeasurementAggregateWithUnits, StorageNodeLoadComposite>() {
                        @Override
                        public StorageNodeLoadComposite apply(
                            StorageNodeLoadComposite.MeasurementAggregateWithUnits metric) {
                            result.setHeapPercentageUsed(metric);
                            return result;
                        }
                    });
                compositeFutures.add(wrapFuture(compositeFuture, result, "Failed to retrieve metric ["
                    + METRIC_HEAP_USED_PERCENTAGE + "] data for " + node));
            }

            return Futures.successfulAsList(compositeFutures);
        } finally {
            stopwatch.stop();
            log.debug("Retrieved load metrics for " + node + " in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    private ListenableFuture<StorageNodeLoadComposite> wrapFuture(
        ListenableFuture<StorageNodeLoadComposite> future, final StorageNodeLoadComposite value, final String msg) {
        return Futures.withFallback(future, new FutureFallback<StorageNodeLoadComposite>() {
            @Override
            public ListenableFuture<StorageNodeLoadComposite> create(Throwable t) throws Exception {
                if (log.isDebugEnabled()) {
                    log.debug(msg, t);
                } else {
                    log.info(msg + ": " + t.getMessage());
                }
                return Futures.immediateFuture(value);
            }
        });
    }

    private List<Object[]> getChildrenScheduleIds(int storageNodeResourceId, boolean lightWeight) {
        // get the schedule ids for Storage Service resource
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, Object[].class);
        query.setParameter("parrentId", storageNodeResourceId).setParameter(
            "metricNames",
            lightWeight ? METRIC_FREE_DISK_TO_DATA_RATIO : Arrays.asList(METRIC_TOKENS, METRIC_OWNERSHIP, METRIC_LOAD,
                METRIC_KEY_CACHE_SIZE, METRIC_ROW_CACHE_SIZE, METRIC_TOTAL_COMMIT_LOG_SIZE,
                METRIC_DATA_DISK_USED_PERCENTAGE, METRIC_TOTAL_DISK_USED_PERCENTAGE, METRIC_FREE_DISK_TO_DATA_RATIO));
        return query.getResultList();
    }

    private List<Object[]> getGrandchildrenScheduleIds(int storageNodeResourceId, boolean lightWeight) {
        // get the schedule ids for Memory Subsystem resource
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_GRANDPARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES,
            Object[].class);
        query.setParameter("grandparrentId", storageNodeResourceId).setParameter(
            "metricNames",
            lightWeight ? METRIC_HEAP_USED_PERCENTAGE : Arrays.asList(METRIC_HEAP_COMMITED, METRIC_HEAP_USED,
                METRIC_HEAP_USED_PERCENTAGE));
        return query.getResultList();
    }

    /**
     * @param accumulator
     * @param input
     */
    private void updateAggregateTotal(MeasurementAggregate accumulator, MeasurementAggregate input) {
        if (accumulator != null && input != null
                && input.getMax() != null && !Double.isNaN(input.getMax())
                && input.getMin() != null && !Double.isNaN(input.getMin())
                && input.getAvg() != null && !Double.isNaN(input.getAvg())) {
            accumulator.setAvg(accumulator.getAvg() + input.getAvg());
            accumulator.setMax(accumulator.getMax() + input.getMax());
            accumulator.setMin(accumulator.getMin() + input.getMin());
        }
    }

    @Override
    public List<StorageNode> getStorageNodes() {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_ALL,
            StorageNode.class);
        return query.getResultList();
    }

    @Override
    public List<StorageNode> getClusterNodes() {
        return entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODES,
            StorageNode.class).setParameter("operationModes", asList(StorageNode.OperationMode.NORMAL,
            StorageNode.OperationMode.MAINTENANCE)).getResultList();
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<StorageNodeLoadComposite> getStorageNodeComposites(Subject subject) {
        Stopwatch stopwatch = new Stopwatch().start();
        List<StorageNode> nodes = getStorageNodes();
        final CountDownLatch latch = new CountDownLatch(nodes.size());
        final PageList<StorageNodeLoadComposite> result = new PageList<StorageNodeLoadComposite>();
        try {
            long endTime = System.currentTimeMillis();
            long beginTime = endTime - (8 * 60 * 60 * 1000);
            for (StorageNode node : nodes) {
                final StorageNode theNode = node;
                if (node.getOperationMode() != OperationMode.INSTALLED) {
                    ListenableFuture<List<StorageNodeLoadComposite>> compositesFuture = getLoadAsync(subject, node,
                        beginTime, endTime);
                    Futures.addCallback(compositesFuture, new FutureCallback<List<StorageNodeLoadComposite>>() {
                        @Override
                        public void onSuccess(List<StorageNodeLoadComposite> composites) {
                            for (StorageNodeLoadComposite composite : composites) {
                                if (composites.isEmpty()) {
                                    log.warn("The results from getLoadAsync() should not be empty. This is likely a bug.");
                                } else {
                                    result.add(composite);
                                    break;
                                }
                            }
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.warn("An error occurred while fetching load data for " + theNode, t);
                            latch.countDown();
                        }
                    });
                } else { // newly installed node
                    result.add(new StorageNodeLoadComposite(node, beginTime, endTime));
                    latch.countDown();
                }

            }
            Map<Integer, Integer> alertCounts = findUnackedAlertCounts(nodes);
            for (StorageNodeLoadComposite composite : result) {
                Integer count = alertCounts.get(composite.getStorageNode().getId());
                if (count != null) {
                    composite.setUnackAlerts(count);
                }
            }
            latch.await();
            return result;
        } catch (InterruptedException e) {
            log.info("There was an interrupt while waiting for storage node load data.", e);
            return result;
        } finally {
            stopwatch.stop();
            log.debug("Retrieved storage node composites in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<StorageNode> runner = new CriteriaQueryRunner<StorageNode>(criteria, generator,
            entityManager);
        return runner.execute();
    }

    public StorageNode findStorageNodeByAddress(String address) {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS,
            StorageNode.class);
        query.setParameter("address", address);
        List<StorageNode> result = query.getResultList();

        if (result != null && result.size() > 0) {
            return result.get(0);
        }

        return null;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void prepareNodeForUpgrade(Subject subject, StorageNode storageNode) {
        int storageNodeResourceId = getResourceIdFromStorageNode(storageNode);
        OperationManagerLocal operationManager = LookupUtil.getOperationManager();

        Configuration parameters = new Configuration();
        parameters.setSimpleValue("snapshotName", String.valueOf(System.currentTimeMillis()));
        // scheduling the operation
        operationManager.scheduleResourceOperation(subject, storageNodeResourceId, "prepareForUpgrade", 0, 0, 0, 0,
            parameters, "Run by StorageNodeManagerBean.prepareNodeForUpgrade()");
    }

    private String getSummaryString(MeasurementAggregate aggregate, MeasurementUnits units) {
        String formattedValue = "Min: " + MeasurementConverter.format(aggregate.getMin(), units, true) + ", Max: "
            + MeasurementConverter.format(aggregate.getMax(), units, true) + ", Avg: "
            + MeasurementConverter.format(aggregate.getAvg(), units, true);
        return formattedValue;
    }

    private StorageNodeLoadComposite.MeasurementAggregateWithUnits getMeasurementAggregateWithUnits(Subject subject,
        int schedId, MeasurementUnits units, long beginTime, long endTime) {
        MetricsServer metricsServer = storageClientManager.getMetricsServer();
        AggregateNumericMetric metric = metricsServer.getSummaryAggregate(schedId, beginTime, endTime);
        MeasurementAggregate measurementAggregate = new MeasurementAggregate(metric.getMin(), metric.getAvg(),
            metric.getMax());
        StorageNodeLoadComposite.MeasurementAggregateWithUnits measurementAggregateWithUnits = new
            StorageNodeLoadComposite.MeasurementAggregateWithUnits(measurementAggregate, units);
        measurementAggregateWithUnits.setFormattedValue(getSummaryString(measurementAggregate, units));
        return measurementAggregateWithUnits;
    }

    private ListenableFuture<StorageNodeLoadComposite.MeasurementAggregateWithUnits> getMeasurementAggregateWithUnitsAsync(
        int schedId, final MeasurementUnits units, long beginTime, long endTime) {
        MetricsServer metricsServer = storageClientManager.getMetricsServer();
        ListenableFuture<AggregateNumericMetric> dataFuture = metricsServer.getSummaryAggregateAsync(schedId, beginTime,
            endTime);
        return Futures.transform(dataFuture, new Function<AggregateNumericMetric, StorageNodeLoadComposite.MeasurementAggregateWithUnits>() {
            @Override
            public StorageNodeLoadComposite.MeasurementAggregateWithUnits apply(AggregateNumericMetric metric) {
                MeasurementAggregate measurementAggregate = new MeasurementAggregate(metric.getMin(), metric.getAvg(),
                    metric.getMax());
                StorageNodeLoadComposite.MeasurementAggregateWithUnits measurementAggregateWithUnits = new
                    StorageNodeLoadComposite.MeasurementAggregateWithUnits(measurementAggregate, units);
                return measurementAggregateWithUnits;
            }
        });
    }

    private int getResourceIdFromStorageNode(StorageNode storageNode) {
        int resourceId;
        int storageNodeId = storageNode.getId();
        if (storageNode.getResource() == null) {
            storageNode = entityManager.find(StorageNode.class, storageNode.getId());
            if (storageNode == null) { // no storage node with the specified id
                throw new ResourceNotFoundException("There is no storage node with id [" + storageNodeId
                    + "] stored in the database.");
            }
            if (storageNode.getResource() == null) { // no associated resource
                throw new IllegalStateException("This storage node [" + storageNode.getId()
                    + "] has no associated resource.");
            }
        }
        resourceId = storageNode.getResource().getId();
        return resourceId;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void runClusterMaintenance(Subject subject) {
        List<StorageNode> storageNodes = getClusterNodes();

        for (StorageNode storageNode : storageNodes) {
            Resource test = storageNode.getResource();

            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterParentResourceId(test.getId());
            criteria.addFilterResourceTypeName("StorageService");
            criteria.setPageControl(PageControl.getUnlimitedInstance());

            PageList<Resource> resources = resourceManager.findResourcesByCriteria(subjectManager.getOverlord(), criteria);
            if (resources.size() > 0) {
                Resource storageServiceResource = resources.get(0);

                ResourceOperationSchedule newSchedule = new ResourceOperationSchedule();
                newSchedule.setJobTrigger(JobTrigger.createNowTrigger());
                newSchedule.setResource(storageServiceResource);
                newSchedule.setOperationName("takeSnapshot");
                newSchedule.setDescription("Run by StorageNodeManagerBean");
                newSchedule.setParameters(new Configuration());

                storageNodeManger.scheduleOperationInNewTransaction(subjectManager.getOverlord(), newSchedule);
            }
        }

        if (storageNodes.size() == 1) {
            log.info("Skipping scheduled repair since this is a single-node cluster");
        } else {
            storageNodeOperationsHandler.runRepair(subjectManager.getOverlord(), storageNodes);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject) {
        return findStorageNodeAlerts(subject, false, null);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject, StorageNode storageNode) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            return findStorageNodeAlerts(subject, false, storageNode);
        } finally {
            stopwatch.stop();
            log.info("Retrieved unacked alerts for " + storageNode + " in " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<Alert> findAllStorageNodeAlerts(Subject subject) {
        return findStorageNodeAlerts(subject, true, null);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<Alert> findAllStorageNodeAlerts(Subject subject, StorageNode storageNode) {
        return findStorageNodeAlerts(subject, true, storageNode);
    }

    /**
     * Find the set of alerts related to Storage Node resources and sub-resources.
     *
     * @param subject subject
     * @param allAlerts if [true] then return all alerts; if [false] then return only alerts that are not acknowledged
     * @return alerts
     */
    private PageList<Alert> findStorageNodeAlerts(Subject subject, boolean allAlerts, StorageNode storageNode) {
        Integer[] resouceIdsWithAlertDefinitions = findResourcesWithAlertDefinitions(storageNode);
        PageList<Alert> alerts = new PageList<Alert>();

        if( resouceIdsWithAlertDefinitions != null && resouceIdsWithAlertDefinitions.length != 0 ){
            AlertCriteria criteria = new AlertCriteria();
            criteria.setPageControl(PageControl.getUnlimitedInstance());
            criteria.addFilterResourceIds(resouceIdsWithAlertDefinitions);
            criteria.addSortCtime(PageOrdering.DESC);

            alerts = alertManager.findAlertsByCriteria(subject, criteria);

            if (!allAlerts) {
                //select on alerts that are not acknowledge
                PageList<Alert> trimmedAlerts = new PageList<Alert>();
                for (Alert alert : alerts) {
                    if (alert.getAcknowledgeTime() == null || alert.getAcknowledgeTime() <= 0) {
                        trimmedAlerts.add(alert);
                    }
                }

                alerts = trimmedAlerts;
            }
        }

        return alerts;
    }

    private Map<Integer, Integer> findUnackedAlertCounts(List<StorageNode> storageNodes) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            Map<Integer, StorageNode> resourceIdToStorageNodeMap = new TreeMap<Integer, StorageNode>();
            for (StorageNode storageNode : storageNodes) {
                if (storageNode.getResource() != null) { // handling the case before the s.n. autoimport
                    resourceIdToStorageNodeMap.put(storageNode.getResource().getId(), storageNode);
                }
            }

            Map<Integer, Integer> storageNodeAlertCounts = new TreeMap<Integer, Integer>();
            Map<Integer, Integer> alertCountsByResource = findStorageNodeAlertCountsByResource();

            Integer currentResourceId;
            for (Integer resourceId : alertCountsByResource.keySet()) {
                currentResourceId = resourceId;
                while (!resourceIdToStorageNodeMap.containsKey(currentResourceId)) {
                    currentResourceId = entityManager.find(Resource.class, currentResourceId).getParentResource().getId();
                }
                Integer alertsForResource = alertCountsByResource.get(resourceId);
                StorageNode storageNode = resourceIdToStorageNodeMap.get(currentResourceId);
                Integer count = storageNodeAlertCounts.get(storageNode.getId());
                if (count == null) {
                    storageNodeAlertCounts.put(storageNode.getId(), alertsForResource);
                } else {
                    storageNodeAlertCounts.put(storageNode.getId(), count + alertsForResource);
                }
            }

            return storageNodeAlertCounts;
        } finally {
            stopwatch.stop();
            log.debug("Finished calculating storage node alert counts in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) +
                " ms");
        }
    }

    /**
     * @return A mapping of resource ids to the count of unacknowledged alerts. Each id belongs to a descendant of or
     * is a storage node resource itself.
     */
    private Map<Integer, Integer> findStorageNodeAlertCountsByResource() {
        List<Object[]> counts = entityManager.createNamedQuery(StorageNode.QUERY_FIND_UNACKED_ALERTS_COUNTS)
            .getResultList();
        Map<Integer, Integer> alertCounts = new TreeMap<Integer, Integer>();

        for (Object[] row : counts) {
            Integer resourceId = (Integer) row[0];
            Integer count = ((Long) row[1]).intValue();
            alertCounts.put(resourceId, count);
        }

        return alertCounts;
    }

    @Override
    public Map<Integer, Integer> findResourcesWithAlertDefinitions() {
        return this.findResourcesWithAlertsToStorageNodeMap(null);
    }

    @Override
    public Integer[] findResourcesWithAlertDefinitions(StorageNode storageNode) {
        Map<Integer, Integer> result = findResourcesWithAlertsToStorageNodeMap(storageNode);
        if (result != null) {
            Set<Integer> resourceIds = result.keySet();
            return resourceIds.toArray(new Integer[resourceIds.size()]);
        }
        return new Integer[0];
    }
    
    private Map<Integer, Integer> findResourcesWithAlertsToStorageNodeMap(StorageNode storageNode) {
        Stopwatch stopwatch = new Stopwatch().start();
        List<StorageNode> initialStorageNodes = getStorageNodes();
        try {
            if (storageNode == null) {
                initialStorageNodes = getStorageNodes();
            } else {
                initialStorageNodes = Arrays.asList(storageNode.getResource() == null ? entityManager.find(
                    StorageNode.class, storageNode.getId()) : storageNode);
            }

            Map<Integer, Integer> resourceIdsToStorageNodeMap = new HashMap<Integer, Integer>();
            Queue<Resource> unvisitedResources = new LinkedList<Resource>();

            // we are assuming here that the set of resources is disjunktive across different storage nodes
            for (StorageNode initialStorageNode : initialStorageNodes) {
                if (initialStorageNode.getResource() != null) {
                    unvisitedResources.add(initialStorageNode.getResource());
                    while (!unvisitedResources.isEmpty()) {
                        Resource resource = unvisitedResources.poll();
                        if (!resource.getAlertDefinitions().isEmpty()) {
                            resourceIdsToStorageNodeMap.put(resource.getId(), initialStorageNode.getId());
                        }

                        Set<Resource> childResources = resource.getChildResources();
                        if (childResources != null) {
                            for (Resource child : childResources) {
                                unvisitedResources.add(child);
                            }
                        }
                    }
                }
            }

            return resourceIdsToStorageNodeMap;
        } finally {
            stopwatch.stop();
            log.debug("Found storage node resources with alert defs in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) +
                " ms");
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageNodeConfigurationComposite retrieveConfiguration(Subject subject, StorageNode storageNode) {
        StorageNodeConfigurationComposite configuration = new StorageNodeConfigurationComposite(storageNode);

        if (storageNode != null && storageNode.getResource() != null) {
            Resource storageNodeResource = storageNode.getResource();
            ResourceConfigurationUpdate configurationUpdate = configurationManager
                .getLatestResourceConfigurationUpdate(subject, storageNodeResource.getId());
            Configuration storageNodeConfiguration = configurationUpdate.getConfiguration();

            Configuration storageNodePluginConfiguration = configurationManager.getPluginConfiguration(subject,
                storageNodeResource.getId());
            if (configurationUpdate != null) {
                configuration.setHeapSize(storageNodeConfiguration.getSimpleValue("maxHeapSize"));
                configuration.setHeapNewSize(storageNodeConfiguration.getSimpleValue("heapNewSize"));
                configuration.setThreadStackSize(storageNodeConfiguration.getSimpleValue("threadStackSize"));
            }
            configuration.setJmxPort(Integer.parseInt(storageNodePluginConfiguration
                .getSimpleValue(RHQ_STORAGE_JMX_PORT_PROPERTY)));
        }

        return configuration;
    }

    @Override
    @Asynchronous
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void updateConfigurationAsync(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration) {
        updateConfiguration(subject, storageNodeConfiguration);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public boolean updateConfiguration(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration) {
        StorageNode storageNode = findStorageNodeByAddress(storageNodeConfiguration
            .getStorageNode().getAddress());
        if (storageNode == null || storageNode.getResource() == null || !storageNodeConfiguration.validate())
            return false;

        // 1. upgrade the resource configuration if there was a change
        Resource storageNodeResource = storageNode.getResource();
        Configuration storageNodeResourceConfig = configurationManager.getResourceConfiguration(subject,
            storageNodeResource.getId());
        String existingHeapSize = storageNodeResourceConfig.getSimpleValue("maxHeapSize");
        String newHeapSize = storageNodeConfiguration.getHeapSize();
        String existingHeapNewSize = storageNodeResourceConfig.getSimpleValue("heapNewSize");
        String newHeapNewSize = storageNodeConfiguration.getHeapNewSize();
        String existingThreadStackSize = storageNodeResourceConfig.getSimpleValue("threadStackSize");
        String newThreadStackSize = storageNodeConfiguration.getThreadStackSize();

        Configuration storageNodePluginConfig = configurationManager.getPluginConfiguration(subject,
            storageNodeResource.getId());
        String existingJMXPort = storageNodePluginConfig.getSimpleValue("jmxPort");
        String newJMXPort = storageNodeConfiguration.getJmxPort() + "";

        boolean resourceConfigNeedsUpdate = !existingHeapSize.equals(newHeapSize)
            || !existingHeapNewSize.equals(newHeapNewSize) || !existingThreadStackSize.equals(newThreadStackSize)
            || !existingJMXPort.equals(newJMXPort);

        ResourceConfigurationUpdate resourceUpdate = null;
        if (resourceConfigNeedsUpdate) {
            storageNodeResourceConfig.setSimpleValue("jmxPort", storageNodeConfiguration.getJmxPort() + "");
            if (storageNodeConfiguration.getHeapSize() != null) {
                storageNodeResourceConfig.setSimpleValue("maxHeapSize", newHeapSize + "");
                storageNodeResourceConfig.setSimpleValue("minHeapSize", newHeapSize + "");
            }
            if (storageNodeConfiguration.getHeapNewSize() != null) {
                storageNodeResourceConfig.setSimpleValue("heapNewSize", newHeapNewSize + "");
            }
            if (storageNodeConfiguration.getThreadStackSize() != null) {
                storageNodeResourceConfig.setSimpleValue("threadStackSize", newThreadStackSize + "");
            }

            resourceUpdate = configurationManager.updateResourceConfiguration(subject, storageNodeResource.getId(),
                storageNodeResourceConfig);

            // initial waiting before the first check
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                // nothing
            }
            // wait for the resource config update
            ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
            criteria.addFilterId(resourceUpdate.getId());
            criteria.addFilterStartTime(System.currentTimeMillis() - (5 * 60 * 1000));
            boolean updateSuccess = waitForConfigurationUpdateToFinish(subject, criteria, 10);
            // restart the storage node and wait for it
            boolean restartSuccess = runOperationAndWaitForResult(subject, storageNodeResource, RESTART_OPERATION, null,
                5000, 15);
            if (!updateSuccess || !restartSuccess)
                return false;
        }

        if (existingJMXPort.equals(newJMXPort)) {
            // no need for plugin config update, we are done
            return true;
        }
        // 2. upgrade the plugin configuration if there was a change
        storageNodePluginConfig.setSimpleValue("jmxPort", newJMXPort);
        String existingConnectionURL = storageNodePluginConfig.getSimpleValue("connectorAddress");
        String newConnectionURL = existingConnectionURL.replace(":" + existingJMXPort + "/", ":"
            + storageNodeConfiguration.getJmxPort() + "/");
        storageNodePluginConfig.setSimpleValue("connectorAddress", newConnectionURL);

        configurationManager.updatePluginConfiguration(subject, storageNodeResource.getId(),
            storageNodePluginConfig);
        return true;
    }
    
    private boolean waitForConfigurationUpdateToFinish(Subject subject, ResourceConfigurationUpdateCriteria criteria,
        int maxAttempts) {
        if (maxAttempts == 0)
            return false;

        PageList<ResourceConfigurationUpdate> configUpdates = configurationManager
            .findResourceConfigurationUpdatesByCriteria(subject, criteria);
        switch (configUpdates.get(0).getStatus()) {
        case INPROGRESS:
            // try it again in 2.5 sec
            break;
        case FAILURE:
            return false;
        default:
            return true;
        }
        try {
            Thread.sleep(2500L);
        } catch (InterruptedException e) {
            return false;
        }
        return waitForConfigurationUpdateToFinish(subject, criteria, maxAttempts - 1);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void scheduleOperationInNewTransaction(Subject subject, ResourceOperationSchedule schedule) {
        operationManager.scheduleResourceOperation(subject, schedule);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public Map<String, List<MeasurementDataNumericHighLowComposite>> findStorageNodeLoadDataForLast(Subject subject,
        StorageNode node, long beginTime, long endTime, int numPoints) {
        // this method is called to get the data for sparkline graphs
        if (!storageClientManager.isClusterAvailable()) {
            return Collections.<String, List<MeasurementDataNumericHighLowComposite>>emptyMap();
        }
        int storageNodeResourceId;
        try {
            storageNodeResourceId = getResourceIdFromStorageNode(node);
        } catch (ResourceNotFoundException e) {
            log.warn(e.getMessage());
            return Collections.<String, List<MeasurementDataNumericHighLowComposite>>emptyMap();
        }
        Map<String, List<MeasurementDataNumericHighLowComposite>> result = new LinkedHashMap<String, List<MeasurementDataNumericHighLowComposite>>();

        List<Object[]> tupples = getChildrenScheduleIds(storageNodeResourceId, false);
        List<String> defNames = new ArrayList<String>();
        Map<Integer, List<Integer>> resourceWithDefinitionIds = new HashMap<Integer, List<Integer>>();
        for (Object[] tupple : tupples) {
            String defName = (String) tupple[0];
            int definitionId = (Integer) tupple[1];
            int resId = (Integer) tupple[3];
            defNames.add(defName);
            if (resourceWithDefinitionIds.get(resId) == null) {
                resourceWithDefinitionIds.put(resId, new ArrayList<Integer>(tupples.size()));
            }
            resourceWithDefinitionIds.get(resId).add(definitionId);
        }
        
        int defNameIndex = 0;
        for (Entry<Integer, List<Integer>> entry : resourceWithDefinitionIds.entrySet()) {
            List<List<MeasurementDataNumericHighLowComposite>> storageServiceData = measurementManager.findDataForResource(
                subject, entry.getKey(), ArrayUtils.unwrapCollection(entry.getValue()), beginTime, endTime, numPoints);    
            for (int i = 0; i < storageServiceData.size(); i++) {
                List<MeasurementDataNumericHighLowComposite> oneRecord = storageServiceData.get(i);
                result.put(defNames.get(defNameIndex++), filterNans(oneRecord));
            }
        }
        
        tupples = getGrandchildrenScheduleIds(storageNodeResourceId, false);
        defNames = new ArrayList<String>();
        int[] definitionIds = new int[tupples.size()];
        definitionIds = new int[tupples.size()];
        int resId = -1;
        int index = 0;
        for (Object[] tupple : tupples) {
            String defName = (String) tupple[0];
            int definitionId = (Integer) tupple[1];
            resId = (Integer) tupple[3];
            defNames.add(defName);
            definitionIds[index++] = definitionId;
        }
        List<List<MeasurementDataNumericHighLowComposite>> memorySubsystemData = measurementManager
            .findDataForResource(subject, resId, definitionIds, beginTime, endTime, numPoints);
        for (int i = 0; i < memorySubsystemData.size(); i++) {
            List<MeasurementDataNumericHighLowComposite> oneRecord = memorySubsystemData.get(i);
            result.put(defNames.get(i), filterNans(oneRecord));
        }

        return result;
    }
    
    private List<MeasurementDataNumericHighLowComposite> filterNans(List<MeasurementDataNumericHighLowComposite> data) {
        // NaNs are not useful for sparkline graphs, lets filter them and reduce the traffic over the wire
        if (data == null || data.isEmpty()) return Collections.<MeasurementDataNumericHighLowComposite>emptyList();
        List<MeasurementDataNumericHighLowComposite> filteredData = new ArrayList<MeasurementDataNumericHighLowComposite>(data.size());
        for (MeasurementDataNumericHighLowComposite number : data) {
            if (!Double.isNaN(number.getValue())) {
                filteredData.add(number);
            }
        }
        return filteredData;
    }

    
    private boolean runOperationAndWaitForResult(Subject subject, Resource storageNodeResource, String operationToRun,
        Configuration parameters) {
        return runOperationAndWaitForResult(subject, storageNodeResource, operationToRun,
            parameters, OPERATION_QUERY_TIMEOUT, MAX_ITERATIONS);
    }
    
    private boolean runOperationAndWaitForResult(Subject subject, Resource storageNodeResource, String operationToRun,
        Configuration parameters, long operationQueryTimeout, int maxIterations) {

        //scheduling the operation
        long operationStartTime = System.currentTimeMillis();

        ResourceOperationSchedule newSchedule = new ResourceOperationSchedule();
        newSchedule.setJobTrigger(JobTrigger.createNowTrigger());
        newSchedule.setResource(storageNodeResource);
        newSchedule.setOperationName(operationToRun);
        newSchedule.setDescription("Run by StorageNodeManagerBean");
        newSchedule.setParameters(parameters);

        storageNodeManger.scheduleOperationInNewTransaction(subject, newSchedule);

        //waiting for the operation result then return it
        int iteration = 0;
        boolean successResultFound = false;
        while (iteration < maxIterations && !successResultFound) {
            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterResourceIds(storageNodeResource.getId());
            criteria.addFilterStartTime(operationStartTime);
            criteria.addFilterOperationName(operationToRun);
            criteria.addFilterStatus(OperationRequestStatus.SUCCESS);
            criteria.setPageControl(PageControl.getUnlimitedInstance());

            PageList<ResourceOperationHistory> results = operationManager.findResourceOperationHistoriesByCriteria(
                subject, criteria);

            if (results != null && results.size() > 0) {
                successResultFound = true;
            }

            if (successResultFound) {
                break;
            } else {
                try {
                    Thread.sleep(operationQueryTimeout);
                } catch (Exception e) {
                    log.error(e);
                }
            }

            iteration++;
        }

        return successResultFound;
    }

}
