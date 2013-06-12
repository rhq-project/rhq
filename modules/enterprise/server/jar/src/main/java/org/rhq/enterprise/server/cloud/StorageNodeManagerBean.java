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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.jetbrains.annotations.Nullable;

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
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.rest.reporting.MeasurementConverter;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.server.metrics.CQLException;

@Stateless
public class StorageNodeManagerBean implements StorageNodeManagerLocal, StorageNodeManagerRemote {

    private static final String RHQ_STORAGE_RESOURCE_TYPE = "RHQ Storage Node";
    private static final String RHQ_STORAGE_PLUGIN = "RHQStorage";

    private static final String RHQ_STORAGE_CQL_PORT_PROPERTY = "nativeTransportPort";
    private static final String RHQ_STORAGE_JMX_PORT_PROPERTY = "jmxPort";
    private static final String RHQ_STORAGE_ADDRESS_PROPERTY = "host";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private MeasurementDataManagerLocal measurementManager;

    @EJB
    private MeasurementScheduleManagerLocal scheduleManager;

    @EJB
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    @Override
    public void scanForStorageNodes() {
        try {
            String seedProp = System.getProperty("rhq.cassandra.seeds");
            if (seedProp == null) {
                throw new CQLException("The rhq.cassandra.seeds property is null. Cannot create session.");
            }

            List<StorageNode> storageNodes = this.getStorageNodes();
            if (storageNodes == null) {
                storageNodes = new ArrayList<StorageNode>();
            }

            Map<String, StorageNode> storageNodeMap = new HashMap<String, StorageNode>();
            for (StorageNode storageNode : storageNodes) {
                storageNode.setOperationMode(OperationMode.DOWN);
                storageNodeMap.put(storageNode.getAddress(), storageNode);
            }

            String[] seeds = seedProp.split(",");
            for (int i = 0; i < seeds.length; ++i) {
                StorageNode discoveredStorageNode = new StorageNode();
                discoveredStorageNode.parseNodeInformation(seeds[i]);

                //Mark the node as down for now since no information is available.
                //This will change to the correct value once a corresponding resource
                //is found in the inventory
                discoveredStorageNode.setOperationMode(OperationMode.DOWN);

                if (storageNodeMap.containsKey(discoveredStorageNode.getAddress())) {
                    StorageNode existingStorageNode = storageNodeMap.get(discoveredStorageNode.getAddress());
                    existingStorageNode.setJmxPort(discoveredStorageNode.getJmxPort());
                    existingStorageNode.setCqlPort(discoveredStorageNode.getCqlPort());
                } else {
                    storageNodeMap.put(discoveredStorageNode.getAddress(), discoveredStorageNode);
                }
            }

            this.discoverResourceInformation(storageNodeMap);

            this.updateStorageNodeList(storageNodeMap.values());
        } catch (Exception e) {
            throw new CQLException("Unable to create session", e);
        }
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

                //schedule the quartz job here....
            }
        }
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
            String host = resourceConfiguration.getSimpleValue("host");

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

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime) {
        StorageNodeLoadComposite result = new StorageNodeLoadComposite(node, beginTime, endTime);
        final String tokensMetric = "Tokens", ownershipMetric = "Ownership", loadMetric = "Load";
        final String heapCommittedMetric = "{HeapMemoryUsage.committed}", heapUsedMetric = "{HeapMemoryUsage.used}", heapUsedPercentageMetric = "Calculated.HeapUsagePercentage";

        int resourceId;
        if (node.getResource() == null) {
            Resource res = entityManager.merge(node).getResource();
            if (res == null) { // no associated resource
                throw new IllegalStateException("This storage node [" + node.getId() +"] has no associated resource.");
            }
            resourceId = res.getId();
        } else {
            resourceId = node.getResource().getId();
        }

        // get the schedule ids for Storage Service resource
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, Object[].class);
        query.setParameter("parrentId", resourceId).setParameter("metricNames",
            Arrays.asList(tokensMetric, ownershipMetric, loadMetric));
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
                MeasurementAggregate ownershipAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(ownershipMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits ownershipAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    ownershipAggregate, MeasurementUnits.PERCENTAGE);
                ownershipAggregateWithUnits.setFormattedValue(getSummaryString(ownershipAggregate,
                        MeasurementUnits.PERCENTAGE));
                result.setActuallyOwns(ownershipAggregateWithUnits);
            }
            if (scheduleIdsMap.get(loadMetric) != null) {
                MeasurementAggregate loadAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(loadMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits loadAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    loadAggregate, MeasurementUnits.BYTES);
                loadAggregateWithUnits.setFormattedValue(getSummaryString(loadAggregate, MeasurementUnits.BYTES));
                result.setLoad(loadAggregateWithUnits);
            }

            if (scheduleIdsMap.get(heapCommittedMetric) != null) {
                MeasurementAggregate heapCommittedAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(heapCommittedMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapCommittedAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    heapCommittedAggregate, MeasurementUnits.BYTES);
                heapCommittedAggregateWithUnits.setFormattedValue(getSummaryString(heapCommittedAggregate,
                        MeasurementUnits.BYTES));
                result.setHeapCommitted(heapCommittedAggregateWithUnits);
            }
            if (scheduleIdsMap.get(heapUsedMetric) != null) {
                MeasurementAggregate heapUsedAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(heapUsedMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    heapUsedAggregate, MeasurementUnits.BYTES);
                heapUsedAggregateWithUnits
                    .setFormattedValue(getSummaryString(heapUsedAggregate, MeasurementUnits.BYTES));
                result.setHeapUsed(heapUsedAggregateWithUnits);
            }
            if (scheduleIdsMap.get(heapUsedPercentageMetric) != null) {
                MeasurementAggregate heapUsedPercentageAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(heapUsedPercentageMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedPercentageAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    heapUsedPercentageAggregate, MeasurementUnits.PERCENTAGE);
                heapUsedPercentageAggregateWithUnits.setFormattedValue(getSummaryString(heapUsedPercentageAggregate,
                    MeasurementUnits.PERCENTAGE));
                result.setHeapPercentageUsed(heapUsedPercentageAggregateWithUnits);
            }
        }

        return result;
    }

    @Nullable
    public List<StorageNode> getStorageNodes() {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_ALL,
            StorageNode.class);
        return query.getResultList();
    }

    public void updateStorageNodeList(Collection<StorageNode> storageNodes) {
        for (StorageNode storageNode : storageNodes) {
            entityManager.persist(storageNode);
        }
        entityManager.flush();
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<StorageNode> runner = new CriteriaQueryRunner<StorageNode>(criteria, generator,
            entityManager);
        return runner.execute();
    }

    private String getSummaryString(MeasurementAggregate aggregate, MeasurementUnits units) {
        String formattedValue = "Min: "
            + MeasurementConverter.format(aggregate.getMin(), units, true)
            + ", Max: "
            + MeasurementConverter.format(aggregate.getMax(), units, true)
            + ", Avg: "
            + MeasurementConverter.format(aggregate.getAvg(), units, true);
        return formattedValue;
    }
}
