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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.server.metrics.CQLException;

@Stateless
public class StorageNodeManagerBean implements StorageNodeManagerLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    ResourceManagerLocal resourceManager;

    @PostConstruct
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

            for (StorageNode storageNode : storageNodes) {
                storageNode.setOperationMode(OperationMode.DOWN);
            }

            Map<String, StorageNode> storageNodeMap = new HashMap<String, StorageNode>();
            for (StorageNode storageNode : storageNodes) {
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

    @SuppressWarnings("unchecked")
    private void discoverResourceInformation(Map<String, StorageNode> storageNodeMap) {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN)
            .setParameter("name", "Cassandra Daemon").setParameter("plugin", "Cassandra");
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
                if(resource.getInventoryStatus() == InventoryStatus.NEW){
                    storageNode.setOperationMode(OperationMode.INSTALLED);
                } else if (resource.getInventoryStatus() == InventoryStatus.COMMITTED
                    && resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP) {
                    storageNode.setOperationMode(OperationMode.NORMAL);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public List<StorageNode> getStorageNodes() {
        Query query = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL);
        return (List<StorageNode>) query.getResultList();
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
}
