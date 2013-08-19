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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertCriteria;
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
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.authz.RequiredPermissions;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.rest.reporting.MeasurementConverter;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerLocal;
import org.rhq.enterprise.server.storage.StorageNodeOperationsHandlerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;

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
    private ResourceTypeManagerLocal resourceTypeManager;

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
    private ResourceManagerLocal resourceManager;

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
            StorageNode storageNode = findStorageNodeByAddress(InetAddress.getByName(address));

            if (storageNode != null) {
                if (log.isInfoEnabled()) {
                    log.info(storageNode + " is an existing storage node. No cluster maintenance is necessary.");
                }
                storageNode.setResource(resource);
                storageNode.setOperationMode(OperationMode.NORMAL);
                initClusterSettingsIfNecessary(pluginConfig);
            } else {
                storageNode = createStorageNode(resource);

                if (log.isInfoEnabled()) {
                    log.info("Scheduling cluster maintenance to deploy " + storageNode + " into the storage cluster...");
                }
                deployStorageNode(subjectManager.getOverlord(), storageNode);
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not resolve address [" + address + "]. The resource " + resource +
                " cannot be linked to a storage node", e);
        }
    }

    private void initClusterSettingsIfNecessary(Configuration pluginConfig) {
        // TODO Need to handle non-repeatable reads here (probably a post 4.9 task)
        //
        // If a user deploys two storage nodes prior to installing the RHQ server, then we
        // could end up in this method concurrently for both storage nodes. The settings
        // would be committed for each node with the second commit winning. The problem is
        // that is the cluster settings differ for the two nodes, it will be silently
        // ignored. This scenario will happen infrequently so it should be sufficient to
        // resolve it with optimistic locking. The second writer should fail with an
        // OptimisticLockException.

        log.info("Initializing storage cluster settings");

        StorageClusterSettings clusterSettings = storageClusterSettingsManager.getClusterSettings(subjectManager
            .getOverlord());
        if (clusterSettings != null) {
            log.info("Cluster settings have already been set. Skipping initialization.");
            return;
        }
        clusterSettings = new StorageClusterSettings();
        clusterSettings.setCqlPort(Integer.parseInt(pluginConfig.getSimpleValue(RHQ_STORAGE_CQL_PORT_PROPERTY)));
        clusterSettings.setGossipPort(Integer.parseInt(pluginConfig.getSimpleValue(RHQ_STORAGE_GOSSIP_PORT_PROPERTY)));
        storageClusterSettingsManager.setClusterSettings(subjectManager.getOverlord(), clusterSettings);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public StorageNode createStorageNode(Resource resource) {
        Configuration pluginConfig = resource.getPluginConfiguration();

        StorageNode storageNode = new StorageNode();
        storageNode.setAddress(pluginConfig.getSimpleValue(RHQ_STORAGE_ADDRESS_PROPERTY));
        storageNode.setCqlPort(Integer.parseInt(pluginConfig.getSimpleValue(RHQ_STORAGE_CQL_PORT_PROPERTY)));
        storageNode.setJmxPort(Integer.parseInt(pluginConfig.getSimpleValue(RHQ_STORAGE_JMX_PORT_PROPERTY)));
        storageNode.setResource(resource);
        storageNode.setOperationMode(OperationMode.INSTALLED);

        entityManager.persist(storageNode);

        return storageNode;
    }

    @Override
    public void deployStorageNode(Subject subject, StorageNode storageNode) {
        storageNode = entityManager.find(StorageNode.class, storageNode.getId());

        switch (storageNode.getOperationMode()) {
            case INSTALLED:
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
            default:
                // TODO what do we do with/about maintenance mode?

                // We do not want to deploying a node that is in the process of being
                // undeployed. It is too hard to make sure we are in an inconsistent state.
                // Instead finishe the undeployment and redeploy the storage node.
                throw new RuntimeException("Cannot deploy " + storageNode);
        }
    }

    @Override
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
        for (StorageNode storageNode : getStorageNodes()) {
            storageNode.setErrorMessage(null);
            storageNode.setFailedOperation(null);
        }
    }

    private List<StorageNode> combine(List<StorageNode> storageNodes, StorageNode storageNode) {
        List<StorageNode> newList = new ArrayList<StorageNode>(storageNodes.size() + 1);
        newList.addAll(storageNodes);
        newList.add(storageNode);

        return newList;
    }

    private PropertyList createPropertyListOfAddresses(String propertyName, List<StorageNode> nodes) {
        PropertyList list = new PropertyList(propertyName);
        for (StorageNode storageNode : nodes) {
            list.add(new PropertySimple("address", storageNode.getAddress()));
        }
        return list;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime) {
        int resourceId = getResourceIdFromStorageNode(node);
        Map<String, Integer> scheduleIdsMap = new HashMap<String, Integer>();

        for (Object[] tupple : getStorageServiceScheduleIds(resourceId)) {
            String definitionName = (String) tupple[0];
            Integer scheduleId = (Integer) tupple[2];
            scheduleIdsMap.put(definitionName, scheduleId);
        }
        for (Object[] tupple : getMemorySubsystemScheduleIds(resourceId)) {
            String definitionName = (String) tupple[0];
            Integer scheduleId = (Integer) tupple[2];
            scheduleIdsMap.put(definitionName, scheduleId);
        }

        StorageNodeLoadComposite result = new StorageNodeLoadComposite(node, beginTime, endTime);
        MeasurementAggregate totalDiskUsedAggregate = new MeasurementAggregate(0d, 0d, 0d);
        Integer scheduleId = null;

        // find the aggregates and enrich the result instance
        if (!scheduleIdsMap.isEmpty()) {
            if ((scheduleId = scheduleIdsMap.get(METRIC_TOKENS)) != null) {
                MeasurementAggregate tokensAggregate = measurementManager.getAggregate(subject, scheduleId, beginTime,
                    endTime);
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
                MeasurementAggregate freeDiskToDataRatioAggregate = measurementManager.getAggregate(subject,
                    scheduleId, beginTime, endTime);
                result.setFreeDiskToDataSizeRatio(freeDiskToDataRatioAggregate);
            }

            if ((scheduleId = scheduleIdsMap.get(METRIC_LOAD)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits loadAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                result.setLoad(loadAggregateWithUnits);

                updateAggregateTotal(totalDiskUsedAggregate, loadAggregateWithUnits.getAggregate());
            }
            //            if ((scheduleId = scheduleIdsMap.get(METRIC_KEY_CACHE_SIZE)) != null) {
            //                updateAggregateTotal(totalDiskUsedAggregate,
            //                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            //            }
            //            if ((scheduleId = scheduleIdsMap.get(METRIC_ROW_CACHE_SIZE)) != null) {
            //                updateAggregateTotal(totalDiskUsedAggregate,
            //                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            //            }
            //            if ((scheduleId = scheduleIdsMap.get(METRIC_TOTAL_COMMIT_LOG_SIZE)) != null) {
            //                updateAggregateTotal(totalDiskUsedAggregate,
            //                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            //            }

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
                    subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime,
                    endTime);
                result.setHeapPercentageUsed(heapUsedPercentageAggregateWithUnits);
            }
        }

        return result;
    }

    private List<Object[]> getStorageServiceScheduleIds(int storageNodeResourceId) {
        // get the schedule ids for Storage Service resource
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, Object[].class);
        query.setParameter("parrentId", storageNodeResourceId).setParameter(
            "metricNames",
            Arrays.asList(METRIC_TOKENS, METRIC_OWNERSHIP,
                METRIC_LOAD/*, METRIC_KEY_CACHE_SIZE, METRIC_ROW_CACHE_SIZE, METRIC_TOTAL_COMMIT_LOG_SIZE*/,
                METRIC_DATA_DISK_USED_PERCENTAGE, METRIC_TOTAL_DISK_USED_PERCENTAGE, METRIC_FREE_DISK_TO_DATA_RATIO));
        return query.getResultList();
    }

    private List<Object[]> getMemorySubsystemScheduleIds(int storageNodeResourceId) {
        // get the schedule ids for Memory Subsystem resource
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_GRANDPARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES,
            Object[].class);
        query.setParameter("grandparrentId", storageNodeResourceId).setParameter("metricNames",
            Arrays.asList(METRIC_HEAP_COMMITED, METRIC_HEAP_USED, METRIC_HEAP_USED_PERCENTAGE));
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
    public PageList<StorageNodeLoadComposite> getStorageNodeComposites() {
        List<StorageNode> nodes = getStorageNodes();
        PageList<StorageNodeLoadComposite> result = new PageList<StorageNodeLoadComposite>();
        long endTime = System.currentTimeMillis();
        long beginTime = endTime - (8 * 60 * 60 * 1000);
        for (StorageNode node : nodes) {
            if (node.getOperationMode() != OperationMode.INSTALLED) {
                StorageNodeLoadComposite composite = getLoad(subjectManager.getOverlord(), node, beginTime, endTime);
                int unackAlerts = findNotAcknowledgedStorageNodeAlerts(subjectManager.getOverlord(), node).size();
                composite.setUnackAlerts(unackAlerts);
                result.add(composite);
            } else { // newly installed node
                result.add(new StorageNodeLoadComposite(node, beginTime, endTime));
            }
            
        }
        return result;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<StorageNode> runner = new CriteriaQueryRunner<StorageNode>(criteria, generator,
            entityManager);
        return runner.execute();
    }

    public StorageNode findStorageNodeByAddress(InetAddress address) {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS,
            StorageNode.class);
        query.setParameter("address", address.getHostAddress());
        List<StorageNode> result = query.getResultList();

        if (result != null && result.size() > 0) {
            return result.get(0);
        }

        return null;
    }

    @Override
    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
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
        MeasurementAggregate measurementAggregate = measurementManager.getAggregate(subject, schedId, beginTime,
            endTime);
        StorageNodeLoadComposite.MeasurementAggregateWithUnits measurementAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
            measurementAggregate, units);
        measurementAggregateWithUnits.setFormattedValue(getSummaryString(measurementAggregate, units));
        return measurementAggregateWithUnits;
    }

    private int getResourceIdFromStorageNode(StorageNode storageNode) {
        int resourceId;
        if (storageNode.getResource() == null) {
            storageNode = entityManager.find(StorageNode.class, storageNode.getId());
            if (storageNode.getResource() == null) { // no associated resource
                throw new IllegalStateException("This storage node [" + storageNode.getId() + "] has no associated resource.");
            }
        }
        resourceId = storageNode.getResource().getId();
        return resourceId;
    }

    @Override
    public void runReadRepair() {
        // TODO Re-implement using work flow similar to how we deploy new nodes

//        ResourceGroup storageNodeGroup = getStorageNodeGroup();
//
//        if (storageNodeGroup.getExplicitResources().size() < 2) {
//            log.info("Skipping read repair since this is a single-node cluster");
//            return;
//        }
//
//        log.info("Scheduling read repair maintenance for storage cluster");
//
//        GroupOperationSchedule schedule = new GroupOperationSchedule();
//        schedule.setGroup(storageNodeGroup);
//        schedule.setHaltOnFailure(false);
//        schedule.setExecutionOrder(new ArrayList<Resource>(storageNodeGroup.getExplicitResources()));
//        schedule.setJobTrigger(JobTrigger.createNowTrigger());
//        schedule.setSubject(subjectManager.getOverlord());
//        schedule.setOperationName("readRepair");
//        schedule.setDescription("Run scheduled read repair on storage node");
//
//        operationManager.scheduleGroupOperation(subjectManager.getOverlord(), schedule);
    }

    @Override
    public PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject) {
        return findStorageNodeAlerts(subject, false, null);
    }

    @Override
    public PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject, StorageNode storageNode) {
        return findStorageNodeAlerts(subject, false, storageNode);
    }

    @Override
    public PageList<Alert> findAllStorageNodeAlerts(Subject subject) {
        return findStorageNodeAlerts(subject, true, null);
    }

    @Override
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

    @Override
    public Integer[] findResourcesWithAlertDefinitions() {
        return this.findResourcesWithAlertDefinitions(null);
    }

    @Override
    public Integer[] findResourcesWithAlertDefinitions(StorageNode storageNode) {
        List<StorageNode> initialStorageNodes = getStorageNodes();
        if (storageNode == null) {
            initialStorageNodes = getStorageNodes();
        } else {
            initialStorageNodes = Arrays.asList(storageNode.getResource() == null ? entityManager.find(
                StorageNode.class, storageNode.getId()) : storageNode);
        }

        Queue<Resource> unvisitedResources = new LinkedList<Resource>();
        for (StorageNode initialStorageNode : initialStorageNodes) {
            if (initialStorageNode.getResource() != null) {
                unvisitedResources.add(initialStorageNode.getResource());
            }
        }

        List<Integer> resourceIdsWithAlertDefinitions = new ArrayList<Integer>();
        while (!unvisitedResources.isEmpty()) {
            Resource resource = unvisitedResources.poll();
            if (resource.getAlertDefinitions() != null) {
                resourceIdsWithAlertDefinitions.add(resource.getId());
            }

            Set<Resource> childResources = resource.getChildResources();
            if (childResources != null) {
                for (Resource child : childResources) {
                    unvisitedResources.add(child);
                }
            }
        }

        return resourceIdsWithAlertDefinitions.toArray(new Integer[resourceIdsWithAlertDefinitions.size()]);
    }

    @Override
    public StorageNodeConfigurationComposite retrieveConfiguration(Subject subject, StorageNode storageNode) {
        StorageNodeConfigurationComposite configuration = new StorageNodeConfigurationComposite(storageNode);

        if (storageNode != null && storageNode.getResource() != null) {
            Resource storageNodeResource = storageNode.getResource();
            Configuration storageNodeConfiguration = configurationManager.getResourceConfiguration(subject,
                storageNodeResource.getId());

            configuration.setHeapSize(storageNodeConfiguration.getSimpleValue("maxHeapSize"));
            configuration.setHeapNewSize(storageNodeConfiguration.getSimpleValue("heapNewSize"));
            configuration.setThreadStackSize(storageNodeConfiguration.getSimpleValue("threadStackSize"));
            configuration.setJmxPort(storageNode.getJmxPort());
        }

        return configuration;
    }

    @Override
    @Asynchronous
    public void updateConfigurationAsync(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration) {
        updateConfiguration(subject, storageNodeConfiguration);
    }

    @Override
    public boolean updateConfiguration(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration) {
        try {
            StorageNode storageNode = findStorageNodeByAddress(InetAddress.getByName(
                storageNodeConfiguration.getStorageNode().getAddress()));

            if (storageNode != null && storageNode.getResource() != null) {
                Configuration parameters = new Configuration();
                parameters.setSimpleValue("jmxPort", storageNodeConfiguration.getJmxPort() + "");
                if (storageNodeConfiguration.getHeapSize() != null) {
                    parameters.setSimpleValue("heapSize", storageNodeConfiguration.getHeapSize() + "");
                }
                if (storageNodeConfiguration.getHeapNewSize() != null) {
                    parameters.setSimpleValue("heapNewSize", storageNodeConfiguration.getHeapNewSize() + "");
                }
                if (storageNodeConfiguration.getThreadStackSize() != null) {
                    parameters.setSimpleValue("threadStackSize", storageNodeConfiguration.getThreadStackSize() + "");
                }
                parameters.setSimpleValue("restartIfRequired", "true");

                Resource storageNodeResource = storageNode.getResource();

                boolean result = runOperationAndWaitForResult(subject, storageNodeResource, UPDATE_CONFIGURATION_OPERATION,
                    parameters);

                if (result) {
                    //2. Update the JMX port
                    //this is a fast operation compared to the restart
                    storageNode.setJmxPort(storageNodeConfiguration.getJmxPort());
                    entityManager.merge(storageNode);

                    //3. Update the plugin configuration to talk with the new server
                    Configuration storageNodePluginConfig = configurationManager.getPluginConfiguration(subject,
                        storageNodeResource.getId());

                    String existingJMXPort = storageNodePluginConfig.getSimpleValue("jmxPort");
                    String newJMXPort = storageNodeConfiguration.getJmxPort() + "";

                    if (!existingJMXPort.equals(newJMXPort)) {
                        storageNodePluginConfig.setSimpleValue("jmxPort", newJMXPort);

                        String existingConnectionURL = storageNodePluginConfig.getSimpleValue("connectorAddress");
                        String newConnectionURL = existingConnectionURL.replace(":" + existingJMXPort + "/", ":"
                            + storageNodeConfiguration.getJmxPort() + "/");
                        storageNodePluginConfig.setSimpleValue("connectorAddress", newConnectionURL);

                        configurationManager.updatePluginConfiguration(subject, storageNodeResource.getId(),
                            storageNodePluginConfig);
                    }

                    return result;
                }
            }

            return false;
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to resolve address for " + storageNodeConfiguration, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void scheduleOperationInNewTransaction(Subject subject, ResourceOperationSchedule schedule) {
        operationManager.scheduleResourceOperation(subject, schedule);
    }

    @Override
    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
    public Map<String, List<MeasurementDataNumericHighLowComposite>> findStorageNodeLoadDataForLast(Subject subject,
        StorageNode node, long beginTime, long endTime, int numPoints) {
        int storageNodeResourceId = getResourceIdFromStorageNode(node);
        Map<String, List<MeasurementDataNumericHighLowComposite>> result = new LinkedHashMap<String, List<MeasurementDataNumericHighLowComposite>>();

        List<Object[]> tupples = getStorageServiceScheduleIds(storageNodeResourceId);
        List<String> defNames = new ArrayList<String>();
        int[] definitionIds = new int[tupples.size()];
        int resId = -1;
        int index = 0;
        for (Object[] tupple : tupples) {
            String defName = (String) tupple[0];
            int definitionId = (Integer) tupple[1];
            resId = (Integer) tupple[3];
            defNames.add(defName);
            definitionIds[index++] = definitionId;
        }
        List<List<MeasurementDataNumericHighLowComposite>> storageServiceData = measurementManager.findDataForResource(
            subject, resId, definitionIds, beginTime, endTime, numPoints);
        for (int i = 0; i < storageServiceData.size(); i++) {
            List<MeasurementDataNumericHighLowComposite> oneRecord = storageServiceData.get(i);
            result.put(defNames.get(i), oneRecord);
        }

        tupples = getMemorySubsystemScheduleIds(storageNodeResourceId);
        defNames = new ArrayList<String>();
        definitionIds = new int[tupples.size()];
        resId = -1;
        index = 0;
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
            result.put(defNames.get(i), oneRecord);
        }

        return result;
    }

    private boolean runOperationAndWaitForResult(Subject subject, Resource storageNodeResource, String operationToRun,
        Configuration parameters) {

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
        while (iteration < MAX_ITERATIONS && !successResultFound) {
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
                    Thread.sleep(OPERATION_QUERY_TIMEOUT);
                } catch (Exception e) {
                    log.error(e);
                }
            }

            iteration++;
        }

        return successResultFound;
    }

}
