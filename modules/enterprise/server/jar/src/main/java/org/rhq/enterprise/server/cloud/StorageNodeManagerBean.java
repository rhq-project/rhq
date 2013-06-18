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
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.rest.reporting.MeasurementConverter;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.scheduler.jobs.StorageNodeMaintenanceJob;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 *
 * @author Stefan Negrea, Jiri Kremser
 */
@Stateless
public class StorageNodeManagerBean implements StorageNodeManagerLocal, StorageNodeManagerRemote {

    private final Log log = LogFactory.getLog(StorageNodeManagerBean.class);

    private static final String RHQ_STORAGE_RESOURCE_TYPE = "RHQ Storage Node";
    private static final String RHQ_STORAGE_PLUGIN = "RHQStorage";

    private static final String RHQ_STORAGE_CQL_PORT_PROPERTY = "nativeTransportPort";
    private static final String RHQ_STORAGE_JMX_PORT_PROPERTY = "jmxPort";
    private static final String RHQ_STORAGE_ADDRESS_PROPERTY = "host";

    private static final String SEEDS_PROP = "rhq.cassandra.seeds";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private MeasurementDataManagerLocal measurementManager;

    @EJB
    private SchedulerLocal quartzScheduler;

    public synchronized List<StorageNode> scanForStorageNodes() {
        List<StorageNode> existingStorageNodes = getStorageNodes();
        if (log.isDebugEnabled()) {
            log.debug("Found existing storage nodes [" + StringUtil.listToString(existingStorageNodes)
                + "] in the database");
        }

        String seeds = System.getProperty(SEEDS_PROP);

        if (StringUtil.isEmpty(seeds) && existingStorageNodes.isEmpty()) {
            // We need to find storage node connection info from one or the other but not
            // necessarily both. If this is a single server deployment where the storage
            // node(s) is running on a separate machine, then SEEDS_PROP will have to be set
            // manually. And in this scenario during the initial deployment, there will not
            // be any storage nodes in the db. In a HA deployment, where there are already
            // storage nodes in the db, an RHQ server does not have to have SEEDS_PROP set
            // since it can obtain connection info from the storage node table.
            throw new IllegalStateException("There are no existing storage nodes defined in the RHQ database and "
                + "the system property [" + SEEDS_PROP + "] is not set. The RHQ server will not be able to connect "
                + "to the RHQ storage node(s). The [" + SEEDS_PROP + "] property should be defined in "
                + "rhq-server.properties.");
        }

        List<StorageNode> seedNodes = parseSeedsProperty(seeds);
        boolean clusterMaintenanceNeeded = false;
        List<StorageNode> newNodes = null;
        if (existingStorageNodes.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("No storage node entities exist in the database");
                log.debug("Persisting seed nodes [" + StringUtil.listToString(seedNodes) + "]");
            }
        } else {
            newNodes = findNewStorageNodes(existingStorageNodes, seedNodes);
            if (!newNodes.isEmpty()) {
                log.info("Detected topology change. New seed nodes will be persisted.");
                if (log.isDebugEnabled()) {
                    log.debug("Persisting new seed nodes [" + StringUtil.listToString(newNodes));
                }

                clusterMaintenanceNeeded = true;
            }
        }

        Map<String, StorageNode> storageNodeMap = new HashMap<String, StorageNode>(existingStorageNodes.size()
            + seedNodes.size());
        for (StorageNode existingStorageNode : existingStorageNodes) {
            storageNodeMap.put(existingStorageNode.getAddress(), existingStorageNode);
        }
        // possibly overide the existing storage nodes with up to date data
        for (StorageNode seedNode : seedNodes) {
            StorageNode existing = storageNodeMap.get(seedNode.getAddress());
            if (existing != null) {
                if (existing.getJmxPort() != seedNode.getJmxPort() || existing.getCqlPort() != seedNode.getCqlPort()
                    || existing.getResource() != seedNode.getResource()) {
                    existing.setMtime(new Date().getTime());
                }
                seedNode.setResource(existing.getResource());
            }
            storageNodeMap.put(seedNode.getAddress(), seedNode);
        }

        this.discoverResourceInformation(storageNodeMap);
        this.updateStorageNodes(storageNodeMap);

        if (clusterMaintenanceNeeded) {
            this.scheduleQuartzJob();
        }

        return new ArrayList<StorageNode>(storageNodeMap.values());
    }

    public void linkResource(Resource resource) {
        List<StorageNode> storageNodes = this.getStorageNodes();

        Configuration resourceConfig = resource.getPluginConfiguration();
        String configAddress = resourceConfig.getSimpleValue(RHQ_STORAGE_ADDRESS_PROPERTY);

        if (configAddress != null) {
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

                scheduleQuartzJob();
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime) {
        StorageNodeLoadComposite result = new StorageNodeLoadComposite(node, beginTime, endTime);
        final String tokensMetric = "Tokens", ownershipMetric = "Ownership", loadMetric = "Load", diskUsedPercentageMetric = "Calculated.DiskSpaceUsedPercentage";
        final String heapCommittedMetric = "{HeapMemoryUsage.committed}", heapUsedMetric = "{HeapMemoryUsage.used}", heapUsedPercentageMetric = "Calculated.HeapUsagePercentage";

        int resourceId;
        if (node.getResource() == null) {
            node = entityManager.find(StorageNode.class, node.getId());
            if (node.getResource() == null) { // no associated resource
                throw new IllegalStateException("This storage node [" + node.getId() + "] has no associated resource.");
            }
        }
        resourceId = node.getResource().getId();

        // get the schedule ids for Storage Service resource
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, Object[].class);
        query.setParameter("parrentId", resourceId).setParameter("metricNames",
            Arrays.asList(tokensMetric, ownershipMetric, loadMetric, diskUsedPercentageMetric));
        List<Object[]> scheduleIds = query.getResultList();
        Map<String, Integer> scheduleIdsMap = new HashMap<String, Integer>(4);
        for (Object[] pair : scheduleIds) {
            scheduleIdsMap.put((String) pair[0], (Integer) pair[1]);
        }

        // get the schedule ids for Memory Subsystem resource
        query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_GRANDPARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES,
            Object[].class);
        query.setParameter("grandparrentId", resourceId).setParameter("metricNames",
            Arrays.asList(heapCommittedMetric, heapUsedMetric, heapUsedPercentageMetric));
        scheduleIds = query.getResultList();
        for (Object[] pair : scheduleIds) {
            scheduleIdsMap.put((String) pair[0], (Integer) pair[1]);
        }

        // find the aggregates and enrich the result instance
        if (!scheduleIdsMap.isEmpty()) {
            if (scheduleIdsMap.get(tokensMetric) != null) {
                MeasurementAggregate tokensAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(tokensMetric), beginTime, endTime);
                result.setTokens(tokensAggregate);
            }
            if (scheduleIdsMap.get(ownershipMetric) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits ownershipAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleIdsMap.get(ownershipMetric), MeasurementUnits.PERCENTAGE, beginTime, endTime);
                result.setActuallyOwns(ownershipAggregateWithUnits);
            }
            if (scheduleIdsMap.get(loadMetric) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits loadAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleIdsMap.get(loadMetric), MeasurementUnits.BYTES, beginTime, endTime);
                result.setLoad(loadAggregateWithUnits);
            }
            if (scheduleIdsMap.get(diskUsedPercentageMetric) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits diskUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleIdsMap.get(diskUsedPercentageMetric), MeasurementUnits.PERCENTAGE, beginTime,
                    endTime);
                result.setDiskSpacePercentageUsed(diskUsedPercentageAggregateWithUnits);
            }

            if (scheduleIdsMap.get(heapCommittedMetric) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapCommittedAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleIdsMap.get(heapCommittedMetric), MeasurementUnits.BYTES, beginTime, endTime);
                result.setHeapCommitted(heapCommittedAggregateWithUnits);
            }
            if (scheduleIdsMap.get(heapUsedMetric) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleIdsMap.get(heapUsedMetric), MeasurementUnits.BYTES, beginTime, endTime);
                result.setHeapUsed(heapUsedAggregateWithUnits);
            }
            if (scheduleIdsMap.get(heapUsedPercentageMetric) != null) {
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedPercentageAggregateWithUnits = getMeasurementAggregateWithUnits(
                    subject, scheduleIdsMap.get(heapUsedPercentageMetric), MeasurementUnits.PERCENTAGE, beginTime,
                    endTime);
                result.setHeapPercentageUsed(heapUsedPercentageAggregateWithUnits);
            }
        }

        return result;
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

    public List<StorageNode> getStorageNodes() {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_ALL,
            StorageNode.class);
        return query.getResultList();
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<StorageNode> runner = new CriteriaQueryRunner<StorageNode>(criteria, generator,
            entityManager);
        return runner.execute();
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

    private List<StorageNode> findNewStorageNodes(List<StorageNode> nodes, List<StorageNode> seedNodes) {
        if (log.isDebugEnabled()) {
            log.debug("Checking system property [" + SEEDS_PROP + "] for any new nodes to be persisted");
        }
        List<StorageNode> newNodes = new ArrayList<StorageNode>();
        for (StorageNode seedNode : seedNodes) {
            // The contains call should be ok even though it is an O(N) operation because
            // the number of storage nodes will be small and this is only done at start up.
            if (!nodes.contains(seedNode)) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected new storage node [" + seedNode + "]");
                }
                newNodes.add(seedNode);
            }
        }
        return newNodes;
    }

    private void scheduleQuartzJob() {
        String jobName = StorageNodeMaintenanceJob.class.getName();
        String jobGroupName = StorageNodeMaintenanceJob.class.getName();
        String triggerName = StorageNodeMaintenanceJob.class.getName();
        Date jobTime = new Date(System.currentTimeMillis() + 30000);

        Trigger trigger = new SimpleTrigger(triggerName, jobGroupName, jobTime);
        trigger.setJobName(jobName);
        trigger.setJobGroup(jobGroupName);
        try {
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

    @SuppressWarnings("unchecked")
    private void discoverResourceInformation(Map<String, StorageNode> storageNodeMap) {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN)
            .setParameter("name", RHQ_STORAGE_RESOURCE_TYPE).setParameter("plugin", RHQ_STORAGE_PLUGIN);
        List<ResourceType> resourceTypes = (List<ResourceType>) query.getResultList();

        if (resourceTypes.isEmpty()) {
            return;
        }

        query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_TYPE_ADMIN).setParameter("type",
            resourceTypes.get(0));
        List<Resource> cassandraResources = (List<Resource>) query.getResultList();

        for (Resource resource : cassandraResources) {
            Configuration resourceConfiguration = resource.getPluginConfiguration();
            String host = resourceConfiguration.getSimpleValue(RHQ_STORAGE_ADDRESS_PROPERTY);

            if (host != null && storageNodeMap.containsKey(host)) {
                StorageNode storageNode = storageNodeMap.get(host);

                storageNode.setResource(resource);
                if (resource.getInventoryStatus() == InventoryStatus.NEW) {
                    storageNode.setOperationMode(OperationMode.INSTALLED);
                } else if (resource.getInventoryStatus() == InventoryStatus.COMMITTED
                    && resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP) {
                    storageNode.setOperationMode(OperationMode.NORMAL);
                }
            }
        }
    }
}
