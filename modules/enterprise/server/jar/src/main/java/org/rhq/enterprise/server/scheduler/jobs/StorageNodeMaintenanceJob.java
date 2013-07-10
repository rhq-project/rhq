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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
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

    public static final String JOB_DATA_PROPERTY_CLUSTER_SIZE = "clusterSize";

    public static final String JOB_DATA_PROPERTY_TOPOLOGY_CHANGED = "topologyChanged";

    private final static int MAX_ITERATIONS = 5;
    private final static int TIMEOUT = 10000;
    private final static String STORAGE_SERVICE = "Storage Service";
    private final static String LOAD_MAP_PROPERTY = "LoadMap";
    private final static String ENDPOINT_PROPERTY = "endpoint";
    private final static String MAINTENANCE_OPERATION = "addNodeMaintenance";
    private final static String MAINTENANCE_OPERATION_NOTE = "Topology change maintenance.";
    private final static String RUN_REPAIR_PROPERTY = "runRepair";
    private final static String UPDATE_SEEDS_LIST = "updateSeedsList";
    private final static String SEEDS_LIST = "seedsList";
    private final static String SUCCEED_PROPERTY = "succeed";
    private static final String USERNAME_PROP = "rhq.cassandra.username";
    private static final String PASSWORD_PROP = "rhq.cassandra.password";

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        int clusterSize = Integer.parseInt(jobDataMap.getString(JOB_DATA_PROPERTY_CLUSTER_SIZE));

        //1. Wait for resouces to be linked to node storage nodes
        waitForResouceLinks();

        //2. Drop any storage nodes not linked to resources from the list of available nodes
        //   (if storage nodes are not linked to resources that means they are not yet managed)
        List<StorageNode> storageNodes = getOnlyResourceLinkedStorageNodes();

        //3. Wait for the all storage nodes to be part of the same cluster
        storageNodes = waitForClustering(storageNodes);

        boolean isReadRepairNeeded;

        if (clusterSize >= 4) {
            // At 4 nodes we increase the RF to 3. We are not increasing the RF beyond
            // that for additional nodes; so, there is no need to run repair if we are
            // expanding from a 4 node cluster since the RF remains the same.
            isReadRepairNeeded = false;
        } else if (clusterSize == 1) {
            // The RF will increase since we are going from a single to a multi-node
            // cluster; therefore, we want to run repair.
            isReadRepairNeeded = true;
        } else if (clusterSize == 2) {
            if (storageNodes.size() > 3) {
                // If we go from 2 to > 3 nodes we will increase the RF to 3; therefore
                // we want to run repair.
                isReadRepairNeeded = true;
            } else {
                // If we go from 2 to 3 nodes, we keep the RF at 2 so there is no need
                // to run repair.
                isReadRepairNeeded = false;
            }
        } else if (clusterSize == 3) {
            // We are increasing the cluster size > 3 which means the RF will be
            // updated to 3; therefore, we want to run repair.
            isReadRepairNeeded = true;
        } else {
            // If we cluster size of zero, then something is really screwed up. It
            // should always be > 0.
            log.error("The job data property [" + JOB_DATA_PROPERTY_CLUSTER_SIZE + "] should always be greater " +
                "than zero. This may be a bug in the code that scheduled this job.");
            isReadRepairNeeded = storageNodes.size() > 1;
        }

        if (isReadRepairNeeded) {
            updateTopology(storageNodes);
        }

        //5. run maintenance on each node
        List<String> seedList = new ArrayList<String>();
        for (StorageNode storageNode : storageNodes) {
            seedList.add(storageNode.getAddress());
        }

        runNodeMaintenance(seedList, isReadRepairNeeded);
    }

    private boolean updateTopology(List<StorageNode> storageNodes) throws JobExecutionException {
        String username = getRequiredStorageProperty(USERNAME_PROP);
        String password = getRequiredStorageProperty(PASSWORD_PROP);
        SchemaManager schemaManager = new SchemaManager(username, password, storageNodes);
        try{
            return schemaManager.updateTopology(false);
        } catch (Exception e) {
            log.error("An error occurred while applying schema topology changes", e);
        }

        return false;
    }

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
                    ResourceCriteria c = new ResourceCriteria();
                    c.addFilterParentResourceId(resource.getId());
                    List<Resource> childResources = LookupUtil.getResourceManager().findResourcesByCriteria(
                        LookupUtil.getSubjectManager().getOverlord(), c);



                    for (Resource childResource : childResources) {
                        if (STORAGE_SERVICE.equals(childResource.getName())) {
                            try {
                                PropertyList propertyList = LookupUtil
                                    .getConfigurationManager()
                                    .getLiveResourceConfiguration(LookupUtil.getSubjectManager().getOverlord(),
                                        childResource.getId(), true).getList(LOAD_MAP_PROPERTY);

                                List<Property> actualList = propertyList.getList();
                                for (Property property : actualList) {
                                    PropertyMap map = (PropertyMap) property;
                                    endpoints.add(map.getSimpleValue(ENDPOINT_PROPERTY, null));
                                }
                            } catch (Exception e) {
                                log.error("Error fetching live configuration for resource " + resource.getId());
                            }

                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("An exception occurred while waiting for nodes to cluster", e);
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

    private void runNodeMaintenance(List<String> seedList, boolean runRepair) {
        OperationManagerLocal operationManager = LookupUtil.getOperationManager();
        StorageNodeManagerLocal storageNodeManager = LookupUtil.getStorageNodeManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        ResourceGroup storageNodeGroup = storageNodeManager.getStorageNodeGroup();

        GroupOperationSchedule schedule = new GroupOperationSchedule();
        schedule.setGroup(storageNodeGroup);
        schedule.setHaltOnFailure(false);
        schedule.setExecutionOrder(new ArrayList<Resource>(storageNodeGroup.getExplicitResources()));
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subjectManager.getOverlord());
        schedule.setOperationName(MAINTENANCE_OPERATION);
        schedule.setDescription(MAINTENANCE_OPERATION_NOTE);

        List<Property> properties = new ArrayList<Property>();
        properties.add(new PropertySimple(RUN_REPAIR_PROPERTY, runRepair));
        properties.add(new PropertySimple(UPDATE_SEEDS_LIST, Boolean.TRUE));

        PropertyList seedListProperty = new PropertyList(SEEDS_LIST);
        for (String seed : seedList) {
            seedListProperty.add(new PropertySimple("seed", seed));
        }
        properties.add(seedListProperty);

        Configuration config = new Configuration();
        config.setProperties(properties);

        schedule.setParameters(config);

        operationManager.scheduleGroupOperation(subjectManager.getOverlord(), schedule);
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

    private String getRequiredStorageProperty(String property) throws JobExecutionException {
        String value = System.getProperty(property);
        if (StringUtil.isEmpty(property)) {
            throw new JobExecutionException("The system property [" + property + "] is not set. The RHQ "
                + "server will not be able connect to the RHQ storage node(s). This property should be defined "
                + "in rhq-server.properties.");
        }
        return value;
    }
}
