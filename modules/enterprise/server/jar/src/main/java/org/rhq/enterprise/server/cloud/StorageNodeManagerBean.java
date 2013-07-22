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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.authz.RequiredPermissions;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.rest.reporting.MeasurementConverter;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.scheduler.jobs.StorageNodeMaintenanceJob;
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
    private static final String RHQ_STORAGE_JMX_PORT_PROPERTY = "jmxPort";
    private static final String RHQ_STORAGE_ADDRESS_PROPERTY = "host";

    private static final int OPERATION_QUERY_TIMEOUT = 20000;
    private static final int MAX_ITERATIONS = 6;
    private static final String UPDATE_CONFIGURATION_OPERATION = "updateConfiguration";
    private static final String RESTART_OPERATION = "restart";

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
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private OperationManagerLocal operationManager;

    @EJB
    private AlertManagerLocal alertManager;

    @EJB
    private ConfigurationManagerLocal configurationManager;

    @Override
    public void linkResource(Resource resource) {
        List<StorageNode> storageNodes = this.getStorageNodes();

        Configuration resourceConfig = resource.getPluginConfiguration();
        String configAddress = resourceConfig.getSimpleValue(RHQ_STORAGE_ADDRESS_PROPERTY);

        if (configAddress != null) {
            // TODO Do not add the node to the group until we have verified it has joined the cluster
            // StorageNodeMaintenanceJob currently determines if a new node has successfully joined the cluster.
            addStorageNodeToGroup(resource);

            boolean storageNodeFound = false;
            if (storageNodes != null) {
                for (StorageNode storageNode : storageNodes) {
                    if (configAddress.equals(storageNode.getAddress())) {
                        storageNode.setResource(resource);
                        storageNode.setOperationMode(OperationMode.NORMAL);
                        storageNodeFound = true;
                        break;
                    }
                }
            }

            if (!storageNodeFound) {
                int cqlPort = Integer.parseInt(resourceConfig.getSimpleValue(RHQ_STORAGE_CQL_PORT_PROPERTY));
                int jmxPort = Integer.parseInt(resourceConfig.getSimpleValue(RHQ_STORAGE_JMX_PORT_PROPERTY));

                StorageNode storageNode = new StorageNode();
                storageNode.setAddress(configAddress);
                storageNode.setCqlPort(cqlPort);
                storageNode.setJmxPort(jmxPort);
                storageNode.setResource(resource);
                storageNode.setOperationMode(OperationMode.NORMAL);

                entityManager.persist(storageNode);

//                scheduleQuartzJob(storageNodes.size());
            }
        }
    }

    @Override
    public void createStorageNodeGroup() {
        log.info("Creating resource group [" + STORAGE_NODE_GROUP_NAME + "]");

        ResourceGroup group = new ResourceGroup(STORAGE_NODE_GROUP_NAME);

        ResourceType type = resourceTypeManager.getResourceTypeByNameAndPlugin(STORAGE_NODE_RESOURCE_TYPE_NAME,
            STORAGE_NODE_PLUGIN_NAME);
        group.setResourceType(type);
        group.setRecursive(false);

        resourceGroupManager.createResourceGroup(subjectManager.getOverlord(), group);

        addExistingStorageNodesToGroup();
    }

    private void addExistingStorageNodesToGroup() {
        log.info("Adding existing storage nodes to resource group [" + STORAGE_NODE_GROUP_NAME + "]");

        for (StorageNode node : getStorageNodes()) {
            if (node.getResource() != null) {
                addStorageNodeToGroup(node.getResource());
            }
        }
    }

    private void addStorageNodeToGroup(Resource resource) {
        if (log.isInfoEnabled()) {
            log.info("Adding " + resource + " to resource group [" + STORAGE_NODE_GROUP_NAME + "]");
        }

        ResourceGroup group = getStorageNodeGroup();
        resourceGroupManager.addResourcesToGroup(subjectManager.getOverlord(), group.getId(),
            new int[] {resource.getId()});
    }

    @Override
    public boolean storageNodeGroupExists() {
        Subject overlord = subjectManager.getOverlord();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterResourceTypeName(STORAGE_NODE_RESOURCE_TYPE_NAME);
        criteria.addFilterPluginName(STORAGE_NODE_PLUGIN_NAME);
        criteria.addFilterName(STORAGE_NODE_GROUP_NAME);

        List<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(overlord, criteria);

        return !groups.isEmpty();
    }

    @Override
    public ResourceGroup getStorageNodeGroup() {
        Subject overlord = subjectManager.getOverlord();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterResourceTypeName(STORAGE_NODE_RESOURCE_TYPE_NAME);
        criteria.addFilterPluginName(STORAGE_NODE_PLUGIN_NAME);
        criteria.addFilterName(STORAGE_NODE_GROUP_NAME);
        criteria.fetchExplicitResources(true);

        List<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(overlord, criteria);

        if (groups.isEmpty()) {
            throw new IllegalStateException("Resource group [" + STORAGE_NODE_GROUP_NAME + "] does not exist. This " +
                "group must exist in order for the server to manage storage nodes. Restart the server for the group " +
                "to be recreated.");
        }
        return groups.get(0);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime) {
        int resourceId = getResourceIdFromStorageNode(node);
        Map<String, Integer> scheduleIdsMap = new HashMap<String, Integer>();

        // get the schedule ids for Storage Service resource
        final String tokensMetric = "Tokens", ownershipMetric = "Ownership", diskUsedPercentageMetric = "Calculated.PartitionDiskUsedPercentage";
        final String loadMetric = "Load", keyCacheSize = "KeyCacheSize", rowCacheSize = "RowCacheSize", totalCommitLogSize = "TotalCommitlogSize";
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, Object[].class);
        query.setParameter("parrentId", resourceId).setParameter("metricNames",
            Arrays.asList(tokensMetric, ownershipMetric, diskUsedPercentageMetric, loadMetric, keyCacheSize,
                rowCacheSize, totalCommitLogSize));
        for (Object[] pair : query.getResultList()) {
            scheduleIdsMap.put((String) pair[0], (Integer) pair[1]);
        }

        // get the schedule ids for Memory Subsystem resource
        final String heapCommittedMetric = "{HeapMemoryUsage.committed}", heapUsedMetric = "{HeapMemoryUsage.used}", heapUsedPercentageMetric = "Calculated.HeapUsagePercentage";
        query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_GRANDPARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES,
            Object[].class);
        query.setParameter("grandparrentId", resourceId).setParameter("metricNames",
            Arrays.asList(heapCommittedMetric, heapUsedMetric, heapUsedPercentageMetric));
        for (Object[] pair : query.getResultList()) {
            scheduleIdsMap.put((String) pair[0], (Integer) pair[1]);
        }


        StorageNodeLoadComposite result = new StorageNodeLoadComposite(node, beginTime, endTime);
        MeasurementAggregate totalDiskUsedaggregate = new MeasurementAggregate(0d, 0d, 0d);
        Integer scheduleId = null;

        // find the aggregates and enrich the result instance
        if (!scheduleIdsMap.isEmpty()) {
            if ((scheduleId = scheduleIdsMap.get(tokensMetric)) != null) {
                MeasurementAggregate tokensAggregate = measurementManager.getAggregate(subject, scheduleId, beginTime,
                    endTime);
                result.setTokens(tokensAggregate);
            }
            if ((scheduleId = scheduleIdsMap.get(ownershipMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits ownershipAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                result.setActuallyOwns(ownershipAggregateWithUnits);
            }
            if ((scheduleId = scheduleIdsMap.get(diskUsedPercentageMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits diskUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime, endTime);
                result.setPartitionDiskUsedPercentage(diskUsedPercentageAggregateWithUnits);
            }

            if ((scheduleId = scheduleIdsMap.get(loadMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits loadAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                result.setLoad(loadAggregateWithUnits);

                updateAggregateTotal(totalDiskUsedaggregate, loadAggregateWithUnits.getAggregate());
            }
            if ((scheduleId = scheduleIdsMap.get(keyCacheSize)) != null) {
                updateAggregateTotal(totalDiskUsedaggregate,
                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            }
            if ((scheduleId = scheduleIdsMap.get(rowCacheSize)) != null) {
                updateAggregateTotal(totalDiskUsedaggregate,
                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            }
            if ((scheduleId = scheduleIdsMap.get(totalCommitLogSize)) != null) {
                updateAggregateTotal(totalDiskUsedaggregate,
                    measurementManager.getAggregate(subject, scheduleId, beginTime, endTime));
            }

            if (totalDiskUsedaggregate.getMax() > 0) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits totalDiskUsedAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    totalDiskUsedaggregate, MeasurementUnits.BYTES);
                totalDiskUsedAggregateWithUnits.setFormattedValue(getSummaryString(totalDiskUsedaggregate,
                    MeasurementUnits.BYTES));
                result.setDataDiskUsed(totalDiskUsedAggregateWithUnits);
            }

            if ((scheduleId = scheduleIdsMap.get(heapCommittedMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapCommittedAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                result.setHeapCommitted(heapCommittedAggregateWithUnits);
            }
            if ((scheduleId = scheduleIdsMap.get(heapUsedMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.BYTES, beginTime, endTime);
                result.setHeapUsed(heapUsedAggregateWithUnits);
            }
            if ((scheduleId = scheduleIdsMap.get(heapUsedPercentageMetric)) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleId, MeasurementUnits.PERCENTAGE, beginTime,
                    endTime);
                result.setHeapPercentageUsed(heapUsedPercentageAggregateWithUnits);
            }
        }

        return result;
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
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<StorageNode> runner = new CriteriaQueryRunner<StorageNode>(criteria, generator,
            entityManager);
        return runner.execute();
    }

    @Override
    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
    public void prepareNodeForUpgrade(Subject subject, StorageNode storageNode) {
        int storageNodeResourceId = getResourceIdFromStorageNode(storageNode);
        TopologyManagerLocal topologyManager = LookupUtil.getTopologyManager();
        ServerManagerLocal serverManager = LookupUtil.getServerManager();
        OperationManagerLocal operationManager = LookupUtil.getOperationManager();
        Server server = serverManager.getServer();
        // setting the server mode to maintenance
        topologyManager.updateServerMode(subject, new Integer[] { server.getId() }, Server.OperationMode.MAINTENANCE);

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

    private List<StorageNode> parseSeedsProperty(String seedsProperty) {
        String[] seeds = seedsProperty.split(",");
        List<StorageNode> storageNodes = new ArrayList<StorageNode>();
        for (String seed : seeds) {
            StorageNode node = new StorageNode();
            node.setOperationMode(OperationMode.INSTALLED);
            node.parseNodeInformation(seed);
            storageNodes.add(node);
        }
        return storageNodes;
    }

    private void scheduleQuartzJob(int clusterSize) {
        String jobName = StorageNodeMaintenanceJob.class.getName();
        String jobGroupName = StorageNodeMaintenanceJob.class.getName();
        String triggerName = StorageNodeMaintenanceJob.class.getName();
        Date jobTime = new Date(System.currentTimeMillis() + 30000);

        Trigger trigger = new SimpleTrigger(triggerName, jobGroupName, jobTime);
        trigger.setJobName(jobName);
        trigger.setJobGroup(jobGroupName);
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(StorageNodeMaintenanceJob.JOB_DATA_PROPERTY_CLUSTER_SIZE, Integer.toString(clusterSize));
            trigger.setJobDataMap(jobDataMap);

            quartzScheduler.scheduleJob(trigger);
        } catch (Throwable t) {
            log.warn("Unable to schedule storage node maintenance job", t);
        }
    }

    private void updateStorageNodes(Map<String, StorageNode> storageNodeMap) {
        for (Map.Entry<String, StorageNode> storageNodeEntry : storageNodeMap.entrySet()) {
            TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(
                StorageNode.QUERY_FIND_BY_ADDRESS, StorageNode.class);
            query.setParameter("address", storageNodeEntry.getKey());
            List<StorageNode> result = query.getResultList();
            if (!result.isEmpty()) {
                storageNodeEntry.getValue().setId(result.get(0).getId());
                entityManager.merge(storageNodeEntry.getValue());
            } else {
                entityManager.persist(storageNodeEntry.getValue());
            }
        }
        entityManager.flush();
    }

    private StorageNode findStorageNodeByAddress(String address) {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS,
            StorageNode.class);
        query.setParameter("address", address);
        List<StorageNode> result = query.getResultList();

        if (result != null && result.size() > 0) {
            return result.get(0);
        }

        return null;
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
        ResourceGroup storageNodeGroup = getStorageNodeGroup();

        if (storageNodeGroup.getExplicitResources().size() < 2) {
            log.info("Skipping read repair since this is a single-node cluster");
            return;
        }

        log.info("Scheduling read repair maintenance for storage cluster");

        GroupOperationSchedule schedule = new GroupOperationSchedule();
        schedule.setGroup(storageNodeGroup);
        schedule.setHaltOnFailure(false);
        schedule.setExecutionOrder(new ArrayList<Resource>(storageNodeGroup.getExplicitResources()));
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subjectManager.getOverlord());
        schedule.setOperationName("readRepair");
        schedule.setDescription("Run scheduled read repair on storage node");

        operationManager.scheduleGroupOperation(subjectManager.getOverlord(), schedule);
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
        List<StorageNode> initialStorageNodes;
        if (storageNode == null) {
            initialStorageNodes = getStorageNodes();
        } else {
            initialStorageNodes = Arrays.asList(storageNode);
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

            for (Resource child : resource.getChildResources()) {
                unvisitedResources.add(child);
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
    public boolean updateConfiguration(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration) {
        StorageNode storageNode = findStorageNodeByAddress(storageNodeConfiguration.getStorageNode().getAddress());

        if (storageNode != null && storageNode.getResource() != null) {
            Resource storageNodeResource = storageNode.getResource();
            Configuration parameters = new Configuration();
            parameters.setSimpleValue("jmxPort", storageNodeConfiguration.getJmxPort() + "");
            parameters.setSimpleValue("heapSize", storageNodeConfiguration.getHeapSize() + "");

            boolean updateConfigurationResult = runOperationAndWaitForResult(subject, storageNodeResource,
                UPDATE_CONFIGURATION_OPERATION, parameters);

            if (updateConfigurationResult) {
                boolean restartResult = runOperationAndWaitForResult(subject, storageNodeResource, RESTART_OPERATION,
                    null);

                if (restartResult) {
                    storageNode.setJmxPort(storageNodeConfiguration.getJmxPort());
                    entityManager.persist(storageNode);

                    return true;
                }
            }
        }

        return false;
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

        operationManager.scheduleResourceOperation(subject, newSchedule);
        entityManager.flush();

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