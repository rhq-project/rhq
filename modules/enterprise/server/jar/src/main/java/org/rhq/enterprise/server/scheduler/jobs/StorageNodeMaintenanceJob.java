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
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Quartz scheduler job that runs cluster wide maintenance. This should be
 * invoked if and only if a topology change was detected in the storage cluster.
 *
 *
 * @author Stefan Negrea
 */
public class StorageNodeMaintenanceJob extends AbstractStatefulJob {

    private final Log log = LogFactory.getLog(StorageNodeMaintenanceJob.class);

    private final static int MAX_ITERATIONS = 5;
    private final static int TIMEOUT = 10000;
    private final static String STORAGE_SERVICE = "Storage Service";
    private final static String LOAD_MAP_PROPERTY = "LoadMap";
    private final static String ENDPOINT_PROPERTY = "endpoint";
    private final static String MAINTENANCE_OPERATION = "addNodeMaintenance";
    private final static String MAINTENANCE_OPERATION_NOTE = "Topology change maintenance.";
    private final static String RUN_REPAIR_PROPERTY = "runRepair";
    private final static String UPDATE_SEEDS_LIST = "updateSeedsList";
    private final static String SUCCEED_PROPERTY = "succeed";

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void executeJobCode(JobExecutionContext arg0) throws JobExecutionException {
        //1. Wait for resouces to be linked to node storage nodes
        waitForResouceLinks();

        //2. Drop any storage nodes not linked to resources from the list of available nodes
        //   (if storage nodes are not linked to resources that means they are not yet managed)
        List<StorageNode> storageNodes = getOnlyResourceLinkedStorageNodes();

        //3. Wait for the all storage nodes to be part of the same cluster
        storageNodes = waitForClustering(storageNodes);

        //4. Run repair operation on all the storage nodes
        for (StorageNode storageNode : storageNodes) {
            Resource resource = storageNode.getResource();
            runNodeMaintenanceOperation(resource);
        }
    }

    @SuppressWarnings("unchecked")
    private List<StorageNode> waitForClustering(List<StorageNode> storageNodes) {
        List<String> existingEndpoints = new ArrayList<String>();
        for (StorageNode storageNode : storageNodes) {
            existingEndpoints.add(storageNode.getAddress());
        }
        Collections.sort(existingEndpoints);

        int iteration = 0;
        boolean allStorageNodesPartOfCluster = false;
        while (iteration < MAX_ITERATIONS) {
            for (StorageNode storageNode : storageNodes) {
                Resource resource = storageNode.getResource();
                List<String> endpoints = new ArrayList<String>();

                try {
                    //Set<Resource> childResources = resource.getChildResources();

                    Query query = LookupUtil.getEntityManager()
                        .createNamedQuery(Resource.QUERY_FIND_CHILDREN_ADMIN);
                    query.setParameter("parent", resource);
                    List<Resource> childResources = query.getResultList();


                    for (Resource childResource : childResources) {
                        if (STORAGE_SERVICE.equals(childResource.getName())) {
                            PropertyList propertyList = childResource.getResourceConfiguration().getList(
                                LOAD_MAP_PROPERTY);
                            List<Property> actualList = propertyList.getList();
                            for (Property property : actualList) {
                                PropertyMap map = (PropertyMap) property;
                                endpoints.add(map.get(ENDPOINT_PROPERTY).toString());
                            }

                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                }

                Collections.sort(endpoints);

                if (existingEndpoints.equals(endpoints)) {
                    allStorageNodesPartOfCluster = true;
                    break;
                }
            }

            if (allStorageNodesPartOfCluster == true) {
                break;
            } else {
                try {
                    Thread.sleep(TIMEOUT);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }

            iteration++;
        }

        return storageNodes;
    }

    private void runNodeMaintenanceOperation(Resource resource) {
        OperationManagerLocal operationManager = LookupUtil.getOperationManager();

        try {
            ResourceOperationSchedule newSchedule = new ResourceOperationSchedule();
            newSchedule.setJobTrigger(JobTrigger.createNowTrigger());
            newSchedule.setResource(resource);
            newSchedule.setOperationName(MAINTENANCE_OPERATION);
            newSchedule.setDescription(MAINTENANCE_OPERATION_NOTE);

            List<Property> properties = new ArrayList<Property>();
            properties.add(new PropertySimple(RUN_REPAIR_PROPERTY, Boolean.TRUE));
            properties.add(new PropertySimple(UPDATE_SEEDS_LIST, Boolean.FALSE));
            properties.add(new PropertyList("seedsList"));
            Configuration config = new Configuration();
            config.setProperties(properties);
            newSchedule.setParameters(config);

            long operationStartTime = System.currentTimeMillis();
            operationManager.scheduleResourceOperation(LookupUtil.getSubjectManager().getOverlord(), newSchedule);

            int iteration = 0;
            boolean resultFound = false;
            while (iteration < MAX_ITERATIONS && !resultFound) {
                PageList<ResourceOperationHistory> results = operationManager.findCompletedResourceOperationHistories(
                    LookupUtil.getSubjectManager().getOverlord(), resource.getId(), operationStartTime, null,
                    PageControl.getUnlimitedInstance());

                for (ResourceOperationHistory operationHistory : results) {
                    if (MAINTENANCE_OPERATION.equals(operationHistory.getOperationDefinition().getName())) {
                        if (OperationRequestStatus.SUCCESS.equals(operationHistory.getStatus())) {
                            Configuration operationResults = operationHistory.getResults();
                            if ("true".equals(operationResults.getSimpleValue(SUCCEED_PROPERTY))) {
                                resultFound = true;
                            }
                        }
                    }
                }

                if (resultFound) {
                    break;
                } else {
                    try {
                        Thread.sleep(TIMEOUT);
                    } catch (Exception e) {
                        log.error(e);
                    }
                }

                iteration++;
            }

        } catch (Exception e) {
            log.error(e);
        }
    }

    private List<StorageNode> getOnlyResourceLinkedStorageNodes() {
        StorageNodeManagerLocal storageNodeManager = LookupUtil.getStorageNodeManager();
        List<StorageNode> resourceLinkedstorageNodes = new ArrayList<StorageNode>();
        for(StorageNode storageNode : storageNodeManager.getStorageNodes()){
            if (storageNode.getResource() != null) {
                resourceLinkedstorageNodes.add(storageNode);
            }
        }

        return resourceLinkedstorageNodes;
    }

    private void waitForResouceLinks() {
        StorageNodeManagerLocal storageNodeManager = LookupUtil.getStorageNodeManager();

        boolean allResourcesLinked = true;
        int iteration = 0;
        while (iteration < MAX_ITERATIONS) {
            allResourcesLinked = true;
            List<StorageNode> t = storageNodeManager.getStorageNodes();

            for (StorageNode storageNode : t) {
                if (storageNode.getResource() == null) {
                    allResourcesLinked = false;
                }
            }
            if (allResourcesLinked) {
                break;
            } else {
                try {
                    Thread.sleep(TIMEOUT);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
            iteration++;
        }
    }
}
