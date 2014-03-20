package org.rhq.enterprise.server.storage;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Hours;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
@Stateless
public class StorageNodeOperationsHandlerBean implements StorageNodeOperationsHandlerLocal {

    private final Log log = LogFactory.getLog(StorageNodeOperationsHandlerBean.class);

    private static final String STORAGE_NODE_TYPE_NAME = "RHQ Storage Node";
    private static final String STORAGE_NODE_PLUGIN_NAME = "RHQStorage";
    private final static String RUN_REPAIR_PROPERTY = "runRepair";
    private final static String UPDATE_SEEDS_LIST = "updateSeedsList";
    private final static String SEEDS_LIST = "seedsList";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    @EJB
    private OperationManagerLocal operationManager;

    @EJB
    private StorageClusterSettingsManagerLocal storageClusterSettingsManager;

    @EJB
    private StorageClientManager storageClientManager;

    @EJB
    private StorageNodeOperationsHandlerLocal storageNodeOperationsHandler;

    @EJB
    private ResourceManagerLocal resourceManager;

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void announceStorageNode(Subject subject, StorageNode storageNode) {
        if (log.isInfoEnabled()) {
            log.info("Announcing " + storageNode + " to storage node cluster.");
        }
        try {
            storageNodeOperationsHandler.setMode(storageNode, StorageNode.OperationMode.ANNOUNCE);

            storageNodeOperationsHandler.setMaintenancePending();

            List<StorageNode> clusterNodes = storageNodeOperationsHandler.setMaintenancePending();

            announceStorageNode(subject, storageNode, clusterNodes.get(0),
                createPropertyListOfAddresses("addresses", asList(storageNode)));

        } catch (IndexOutOfBoundsException e) {
            String msg = "Aborting storage node deployment due to unexpected error while announcing storage node at "
                + storageNode.getAddress();
            log.error(msg, e);
            log.error("If this error occurred with a storage node that was deployed prior to installing the server, "
                + "then this may indicate that the rhq.storage.nodes property in rhq-server.properties was not set "
                + "correctly. All nodes deployed prior to server installation should be listed in the "
                + "rhq.storage.nodes property. Please review the deployment documentation for additional details.");
            storageNodeOperationsHandler.logError(storageNode.getAddress(), msg, e);

        } catch (Exception e) {
            String msg = "Aborting storage node deployment due to unexpected error while announcing storage node at "
                + storageNode.getAddress();
            log.error(msg, e);
            storageNodeOperationsHandler.logError(storageNode.getAddress(), msg, e);
        }
    }

    private void announceStorageNode(Subject subject, StorageNode newStorageNode, StorageNode clusterNode,
        PropertyList addresses) {
        if (log.isInfoEnabled()) {
            log.info("Announcing " + newStorageNode + " to cluster node " + clusterNode);
        }

        Configuration parameters = new Configuration();
        parameters.put(addresses);

        scheduleOperation(subject, clusterNode, parameters, "announce");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void unannounceStorageNode(Subject subject, StorageNode storageNode) {
        log.info("Unannouncing " + storageNode);

        List<StorageNode> clusterNodes = storageNodeOperationsHandler.setMaintenancePending();

        unannounceStorageNode(subject, clusterNodes.get(0),
            createPropertyListOfAddresses("addresses", asList(storageNode)));
    }

    @Override
    public List<StorageNode> setMaintenancePending() {
        List<StorageNode> clusterNodes = getStorageNodesByMode(StorageNode.OperationMode.NORMAL);
        for (StorageNode clusterNode : clusterNodes) {
            clusterNode.setMaintenancePending(true);
        }

        return clusterNodes;
    }

    @Override
    public List<StorageNode> getStorageNodesByMode(StorageNode.OperationMode mode) {
        List<StorageNode> clusterNodes = entityManager
            .createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE, StorageNode.class)
            .setParameter("operationMode", mode).getResultList();

        return clusterNodes;
    }

    private void unannounceStorageNode(Subject subject, StorageNode clusterNode, PropertyList addresses) {
        Configuration parameters = new Configuration();
        parameters.put(addresses);

        scheduleOperation(subject, clusterNode, parameters, "unannounce");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void uninstall(Subject subject, StorageNode storageNode) {
        log.info("Uninstalling " + storageNode);

        if (storageNode.getResource() == null) {
            finishUninstall(subject, storageNode);
        } else {
            scheduleOperation(subject, storageNode, new Configuration(), "uninstall");
        }
    }

    @Override
    public void finishUninstall(Subject subject, StorageNode storageNode) {
        storageNode = entityManager.find(StorageNode.class, storageNode.getId());
        if (storageNode.getResource() != null) {
            log.info("Removing storage node resource " + storageNode.getResource() + " from inventory");
            Resource resource = storageNode.getResource();
            storageNodeOperationsHandler.detachFromResource(storageNode);
            resourceManager.uninventoryResource(subject, resource.getId());
        }
        log.info("Removing storage node entity " + storageNode + " from database");
        entityManager.remove(storageNode);

        log.info(storageNode + " has been undeployed");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void detachFromResource(StorageNode storageNode) {
        storageNode.setResource(null);
        storageNode.setFailedOperation(null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void decommissionStorageNode(Subject subject, StorageNode storageNode) {
        log.info("Preparing to decommission " + storageNode);

        storageNode = storageNodeOperationsHandler.setMode(storageNode, StorageNode.OperationMode.DECOMMISSION);

        storageNode = storageNodeOperationsHandler.setMaintenancePendingDecommissionStorageNode(storageNode);

        scheduleOperation(subject, storageNode, new Configuration(), "decommission");
    }

    @Override
    public StorageNode setMaintenancePendingDecommissionStorageNode(StorageNode storageNode) {
        List<StorageNode> storageNodes = getStorageNodesByMode(StorageNode.OperationMode.NORMAL);

        boolean runRepair = updateSchemaIfNecessary(storageNodes.size() + 1, storageNodes.size());
        // This is a bit of a hack since the maintenancePending flag is really intended to
        // queue up storage nodes during cluster maintenance operations.
        storageNode = entityManager.find(StorageNode.class, storageNode.getId());
        storageNode.setMaintenancePending(runRepair);

        storageNode = entityManager.merge(storageNode);

        return storageNode;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logError(String address, String error, Exception e) {
        try {
            StorageNode newStorageNode = findStorageNodeByAddress(address);
            newStorageNode.setErrorMessage(error + " Check the server log for details. Root cause: "
                + ThrowableUtil.getRootCause(e).getMessage());
        } catch (Exception e1) {
            log.error("Failed to log error against storage node", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void performAddNodeMaintenance(Subject subject, StorageNode storageNode) {
        try {
            storageNodeOperationsHandler.performAddMaintenance(subject, storageNode);
        } catch (Exception e) {
            String msg = "Aborting storage node deployment due to unexpected error while performing add node "
                + "maintenance.";
            log.error(msg, e);
            storageNodeOperationsHandler.logError(storageNode.getAddress(), msg, e);
        }
    }

    @Override
    public void performAddMaintenance(Subject subject, StorageNode storageNode) {
        List<StorageNode> clusterNodes = setMaintenancePendingPerformAddMaintenance(storageNode);

        boolean runRepair = updateSchemaIfNecessary(clusterNodes.size() - 1, clusterNodes.size());

        performAddNodeMaintenance(subject, storageNode, runRepair,
            createPropertyListOfAddresses(SEEDS_LIST, clusterNodes), storageNode.getAddress());
    }

    @Override
    public List<StorageNode> setMaintenancePendingPerformAddMaintenance(StorageNode storageNode) {
        List<StorageNode> storageNodes = getStorageNodesByMode(StorageNode.OperationMode.NORMAL);

        for (StorageNode node : storageNodes) {
            node.setMaintenancePending(true);
        }

        storageNode.setMaintenancePending(true);
        storageNode = entityManager.merge(storageNode);

        storageNodes.add(storageNode);

        return storageNodes;
    }

    private void performAddNodeMaintenance(Subject subject, StorageNode storageNode, boolean runRepair,
        PropertyList seedsList, String newNodeAddress) {
        if (log.isInfoEnabled()) {
            log.info("Running addNodeMaintenance for storage node " + storageNode);
        }
        Configuration params = new Configuration();
        params.put(seedsList);
        params.put(new PropertySimple(RUN_REPAIR_PROPERTY, runRepair));
        params.put(new PropertySimple(UPDATE_SEEDS_LIST, Boolean.TRUE));
        params.put(new PropertySimple("newNodeAddress", newNodeAddress));

        scheduleOperation(subject, storageNode, params, "addNodeMaintenance", Hours.EIGHT.toStandardSeconds()
            .getSeconds());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void performRemoveNodeMaintenance(Subject subject, StorageNode storageNode) {
        try {
            storageNodeOperationsHandler.performRemoveMaintenance(subject, storageNode);
        } catch (Exception e) {
            String msg = "Aborting undeployment due to unexpected error while performing remove node maintenance.";
            log.error(msg, e);
            storageNodeOperationsHandler.logError(storageNode.getAddress(), msg, e);
        }
    }

    @Override
    public void performRemoveMaintenance(Subject subject, StorageNode storageNode) {
        List<StorageNode> clusterNodes = setMaintenancePending();

        boolean runRepair = storageNode.isMaintenancePending();

        performRemoveNodeMaintenance(subject, clusterNodes.get(0), runRepair,
            createPropertyListOfAddresses(SEEDS_LIST, clusterNodes), storageNode.getAddress());
    }

    private void performRemoveNodeMaintenance(Subject subject, StorageNode storageNode, boolean runRepair,
        PropertyList seedsList, String removedNodeAddress) {
        if (log.isInfoEnabled()) {
            log.info("Running remove node maintenance for storage node " + storageNode);
        }
        Configuration params = new Configuration();
        params.put(seedsList);
        params.put(new PropertySimple(RUN_REPAIR_PROPERTY, runRepair));
        params.put(new PropertySimple(UPDATE_SEEDS_LIST, true));
        params.put(new PropertySimple("removedNodeAddress", removedNodeAddress));

        scheduleOperation(subject, storageNode, params, "removeNodeMaintenance", Hours.EIGHT.toStandardSeconds()
            .getSeconds());
    }

    @Override
    @Asynchronous
    public void handleOperationUpdateIfNecessary(OperationHistory operationHistory) {
        if (!(operationHistory instanceof ResourceOperationHistory)) {
            return;
        }

        ResourceOperationHistory resourceOperationHistory = (ResourceOperationHistory) operationHistory;
        if (!isStorageNodeOperation(resourceOperationHistory)) {
            return;
        }

        if (resourceOperationHistory.getOperationDefinition().getName().equals("announce")) {
            try {
                storageNodeOperationsHandler.handleAnnounce(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting storage node deployment due to unexpected error while announcing cluster nodes.";
                logError(resourceOperationHistory, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("prepareForBootstrap")) {
            try {
                storageNodeOperationsHandler.handlePrepareForBootstrap(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting storage node deployment due to unexpected error while bootstrapping new node.";
                logError(resourceOperationHistory, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("addNodeMaintenance")) {
            try {
                storageNodeOperationsHandler.handleAddNodeMaintenance(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting storage node deployment due to unexpected error while performing add node "
                    + "maintenance.";
                logError(resourceOperationHistory, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("decommission")) {
            try {
                storageNodeOperationsHandler.handleDecommission(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting undeployment due to unexpected error while decommissioning storage node.";
                logError(resourceOperationHistory, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("removeNodeMaintenance")) {
            try {
                storageNodeOperationsHandler.handleRemoveNodeMaintenance(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting undeployment due to unexpected error while performing remove node maintenance.";
                logError(resourceOperationHistory, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("unannounce")) {
            try {
                storageNodeOperationsHandler.handleUnannounce(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting undeployment due to unexpected error while performing unannouncement.";
                logError(resourceOperationHistory, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("uninstall")) {
            try {
                storageNodeOperationsHandler.handleUninstall(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting undeployment due to unexpected error while uninstalling.";
                logError(resourceOperationHistory, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("repair")) {
            try {
                storageNodeOperationsHandler.handleRepair(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Abort scheduled repair maintenance due to unexpected error.";
                log.error(msg, e);
            }
        }
    }

    private void logError(ResourceOperationHistory operationHistory, String msg, Exception e) {
        log.error(msg, e);
        StorageNode storageNode = findStorageNode(operationHistory.getResource());
        storageNodeOperationsHandler.logError(storageNode.getAddress(), msg, e);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleAnnounce(ResourceOperationHistory resourceOperationHistory) {
        StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
        Configuration parameters = resourceOperationHistory.getParameters();
        PropertyList addresses = parameters.getList("addresses");
        StorageNode newStorageNode;

        switch (resourceOperationHistory.getStatus()) {
        case INPROGRESS:
            // nothing to do here
            return;
        case CANCELED:
            newStorageNode = findStorageNodeByAddress(getAddress(addresses));
            deploymentOperationCanceled(storageNode, resourceOperationHistory, newStorageNode);
        case FAILURE:
            newStorageNode = findStorageNodeByAddress(getAddress(addresses));
            deploymentOperationFailed(storageNode, resourceOperationHistory, newStorageNode);
            return;
        default: // SUCCESS
            storageNode.setMaintenancePending(false);
            StorageNode nextNode = takeFromMaintenanceQueue();
            Subject subject = getSubject(resourceOperationHistory);
            newStorageNode = findStorageNodeByAddress(getAddress(addresses));

            if (nextNode == null) {
                log.info("Successfully announced new storage node to storage cluster");
                newStorageNode = storageNodeOperationsHandler.setMode(newStorageNode,
                    StorageNode.OperationMode.BOOTSTRAP);
                storageNodeOperationsHandler.bootstrapStorageNode(subject, newStorageNode);
            } else {
                announceStorageNode(subject, newStorageNode, nextNode, addresses.deepCopy(false));
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleUnannounce(ResourceOperationHistory operationHistory) {
        StorageNode storageNode = findStorageNode(operationHistory.getResource());
        Configuration params = operationHistory.getParameters();
        PropertyList addresses = params.getList("addresses");
        StorageNode removedStorageNode;

        switch (operationHistory.getStatus()) {
        case INPROGRESS:
            // nothing to do here
            break;
        case CANCELED:
            removedStorageNode = findStorageNodeByAddress(getAddress(addresses));
            undeploymentOperationCanceled(storageNode, operationHistory, removedStorageNode);
            break;
        case FAILURE:
            removedStorageNode = findStorageNodeByAddress(getAddress(addresses));
            deploymentOperationFailed(storageNode, operationHistory, removedStorageNode);
            break;
        default: // SUCCESS
            storageNode.setMaintenancePending(false);
            StorageNode nextNode = takeFromMaintenanceQueue();
            Subject subject = getSubject(operationHistory);
            removedStorageNode = findStorageNodeByAddress(getAddress(addresses));

            if (nextNode == null) {
                log.info("Successfully unannounced " + removedStorageNode + " to storage cluster");
                removedStorageNode = storageNodeOperationsHandler.setMode(removedStorageNode,
                    StorageNode.OperationMode.UNINSTALL);
                uninstall(getSubject(operationHistory), removedStorageNode);
            } else {
                unannounceStorageNode(subject, nextNode, addresses.deepCopy(false));
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handlePrepareForBootstrap(ResourceOperationHistory operationHistory) {
        StorageNode newStorageNode = findStorageNode(operationHistory.getResource());
        switch (operationHistory.getStatus()) {
        case INPROGRESS:
            // nothing to do here
            return;
        case CANCELED:
            deploymentOperationCanceled(newStorageNode, operationHistory);
            return;
        case FAILURE:
            deploymentOperationFailed(newStorageNode, operationHistory);
            return;
        default: // SUCCESS
            log.info("The prepare for bootstrap operation completed successfully for " + newStorageNode);
            newStorageNode = storageNodeOperationsHandler.setMode(newStorageNode,
                StorageNode.OperationMode.ADD_MAINTENANCE);
            Subject subject = getSubject(operationHistory);
            performAddMaintenance(subject, newStorageNode);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleAddNodeMaintenance(ResourceOperationHistory resourceOperationHistory) {
        StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
        Configuration parameters = resourceOperationHistory.getParameters();
        String newNodeAddress = parameters.getSimpleValue("newNodeAddress");
        StorageNode newStorageNode;

        switch (resourceOperationHistory.getStatus()) {
        case INPROGRESS:
            // nothing to do here
            return;
        case CANCELED:
            newStorageNode = findStorageNodeByAddress(newNodeAddress);
            deploymentOperationCanceled(storageNode, resourceOperationHistory, newStorageNode);
            return;
        case FAILURE:
            newStorageNode = findStorageNodeByAddress(newNodeAddress);
            deploymentOperationFailed(storageNode, resourceOperationHistory, newStorageNode);
            return;
        default: // SUCCESS
            log.info("Finished running add node maintenance for " + storageNode);
            storageNode.setMaintenancePending(false);
            StorageNode nextNode = takeFromMaintenanceQueue();
            newStorageNode = findStorageNodeByAddress(newNodeAddress);

            if (nextNode == null) {
                log.info("Finished running add node maintenance on all cluster nodes");
                storageNodeOperationsHandler.setMode(newStorageNode, StorageNode.OperationMode.NORMAL);
            } else {

                boolean runRepair = parameters.getSimple(RUN_REPAIR_PROPERTY).getBooleanValue();
                PropertyList seedsList = parameters.getList(SEEDS_LIST).deepCopy(false);
                Subject subject = getSubject(resourceOperationHistory);
                performAddNodeMaintenance(subject, nextNode, runRepair, seedsList, newNodeAddress);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void bootstrapStorageNode(Subject subject, StorageNode storageNode) {
        List<StorageNode> clusterNodes = storageNodeOperationsHandler
            .getStorageNodesByMode(StorageNode.OperationMode.NORMAL);

        clusterNodes.add(storageNode);
        prepareNodeForBootstrap(subject, storageNode, createPropertyListOfAddresses("addresses", clusterNodes));
    }

    private void prepareNodeForBootstrap(Subject subject, StorageNode storageNode, PropertyList addresses) {
        if (log.isInfoEnabled()) {
            log.info("Preparing to bootstrap " + storageNode + " into cluster...");
        }
        StorageClusterSettings clusterSettings = storageClusterSettingsManager.getClusterSettings(subject);
        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("cqlPort", clusterSettings.getCqlPort()));
        parameters.put(new PropertySimple("gossipPort", clusterSettings.getGossipPort()));
        parameters.put(addresses);

        scheduleOperation(subject, storageNode, parameters, "prepareForBootstrap");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleRemoveNodeMaintenance(ResourceOperationHistory operationHistory) {
        StorageNode storageNode = findStorageNode(operationHistory.getResource());
        Configuration parameters = operationHistory.getParameters();
        String removedNodeAddress = parameters.getSimpleValue("removedNodeAddress");
        StorageNode removedStorageNode;

        switch (operationHistory.getStatus()) {
        case INPROGRESS:
            // nothing to do here
            break;
        case CANCELED:
            removedStorageNode = findStorageNodeByAddress(removedNodeAddress);
            undeploymentOperationCanceled(storageNode, operationHistory, removedStorageNode);
            break;
        case FAILURE:
            removedStorageNode = findStorageNodeByAddress(removedNodeAddress);
            undeploymentOperationFailed(storageNode, operationHistory, removedStorageNode);
            break;
        default: // SUCCESS
            log.info("Finished remove node maintenance for " + storageNode);
            storageNode.setMaintenancePending(false);
            StorageNode nextNode = takeFromMaintenanceQueue();
            removedStorageNode = findStorageNodeByAddress(removedNodeAddress);

            if (nextNode == null) {
                log.info("Finished running remove node maintenance on all cluster nodes");
                removedStorageNode = storageNodeOperationsHandler.setMode(removedStorageNode,
                    StorageNode.OperationMode.UNANNOUNCE);
                unannounceStorageNode(getSubject(operationHistory), removedStorageNode);
            } else {
                boolean runRepair = parameters.getSimple(RUN_REPAIR_PROPERTY).getBooleanValue();
                PropertyList seedsList = parameters.getList(SEEDS_LIST).deepCopy(false);
                Subject subject = getSubject(operationHistory);
                performRemoveNodeMaintenance(subject, nextNode, runRepair, seedsList, removedNodeAddress);
            }
        }
    }

    @Override
    public void runRepair(Subject subject, List<StorageNode> clusterNodes) {
        log.info("Starting anti-entropy repair on storage cluster");

        for (StorageNode node : clusterNodes) {
            node.setErrorMessage(null);
            node.setFailedOperation(null);
            node.setMaintenancePending(true);
        }
        StorageNode storageNode = storageNodeOperationsHandler.setMode(clusterNodes.get(0),
            StorageNode.OperationMode.MAINTENANCE);
        scheduleOperation(subject, storageNode, new Configuration(), "repair", Hours.SIX.toStandardSeconds()
            .getSeconds());
    }

    private void runRepair(Subject subject, StorageNode storageNode) {
        scheduleOperation(subject, storageNode, new Configuration(), "repair", Hours.SIX.toStandardSeconds()
            .getSeconds());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleRepair(ResourceOperationHistory operationHistory) {
        StorageNode storageNode = findStorageNode(operationHistory.getResource());
        switch (operationHistory.getStatus()) {
        case INPROGRESS:
            // nothing to do here
            break;
        case CANCELED:
            repairCanceled(storageNode, operationHistory);
            break;
        case FAILURE:
            repairFailed(storageNode, operationHistory);
            break;
        default: // SUCCESS
            log.info("Finished running repair on " + storageNode);
            storageNode.setMaintenancePending(false);
            storageNodeOperationsHandler.setMode(storageNode, StorageNode.OperationMode.NORMAL);
            StorageNode nextNode = takeFromMaintenanceQueue();

            if (nextNode == null) {
                log.info("Finished running repair on storage cluster");
            } else {
                nextNode = storageNodeOperationsHandler.setMode(nextNode, StorageNode.OperationMode.MAINTENANCE);
                runRepair(getSubject(operationHistory), nextNode);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleDecommission(ResourceOperationHistory operationHistory) {
        StorageNode storageNode = findStorageNode(operationHistory.getResource());
        switch (operationHistory.getStatus()) {
        case INPROGRESS:
            // nothing do to here
            break;
        case CANCELED:
            undeploymentOperationCanceled(storageNode, operationHistory);
            break;
        case FAILURE:
            undeploymentOperationFailed(storageNode, operationHistory);
            break;
        default: // SUCCESS
            log.info("Successfully decommissioned " + storageNode);
            storageNode = storageNodeOperationsHandler.setMode(storageNode,
                StorageNode.OperationMode.REMOVE_MAINTENANCE);
            Subject subject = getSubject(operationHistory);
            performRemoveMaintenance(subject, storageNode);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleUninstall(ResourceOperationHistory operationHistory) {
        StorageNode storageNode = findStorageNode(operationHistory.getResource());
        switch (operationHistory.getStatus()) {
        case INPROGRESS:
            // nothing to do here
            break;
        case CANCELED:
            undeploymentOperationCanceled(storageNode, operationHistory);
            break;
        case FAILURE:
            undeploymentOperationFailed(storageNode, operationHistory);
            break;
        default: // SUCCESS
            log.info("Successfully uninstalled " + storageNode + " from disk");
            finishUninstall(getSubject(operationHistory), storageNode);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public StorageNode setMode(StorageNode storageNode, StorageNode.OperationMode newMode) {
        storageNode.setOperationMode(newMode);
        return entityManager.merge(storageNode);
    }

    private Subject getSubject(ResourceOperationHistory resourceOperationHistory) {
        try {
            return subjectManager.loginUnauthenticated(resourceOperationHistory.getSubjectName());
        } catch (Exception e) {
            throw new SubjectException("Not able to authenticate subject " + resourceOperationHistory.getSubjectName(),
                e);
        }
    }

    private void deploymentOperationCanceled(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode newStorageNode) {
        operationCanceled(storageNode, operationHistory, newStorageNode, "Deployment");
    }

    private void undeploymentOperationCanceled(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode removedStorageNode) {
        operationCanceled(storageNode, operationHistory, removedStorageNode, "Undeployment");
    }

    private void operationCanceled(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode movingNode, String opType) {
        log.error(opType + " has been aborted due to canceled operation ["
            + operationHistory.getOperationDefinition().getDisplayName() + " on " + storageNode.getResource() + ": "
            + operationHistory.getErrorMessage());

        movingNode.setErrorMessage(opType + " has been aborted due to canceled resource operation on "
            + storageNode.getAddress());
        storageNode.setErrorMessage(opType + " of " + movingNode.getAddress() + " has been aborted due "
            + "to cancellation of resource operation [" + operationHistory.getOperationDefinition().getDisplayName()
            + "].");
        storageNode.setFailedOperation(operationHistory);
    }

    private void deploymentOperationCanceled(StorageNode newStorageNode, ResourceOperationHistory operationHistory) {
        operationCanceled(newStorageNode, operationHistory, "Deployment");
    }

    private void undeploymentOperationCanceled(StorageNode storageNode, ResourceOperationHistory operationHistory) {
        operationCanceled(storageNode, operationHistory, "Undeployment");
    }

    private void repairCanceled(StorageNode storageNode, ResourceOperationHistory operationHistory) {
        operationCanceled(storageNode, operationHistory, "Scheduled repair");
    }

    private void operationCanceled(StorageNode storageNode, ResourceOperationHistory operationHistory, String opType) {
        log.error(opType + " has been aborted due to canceled operation ["
            + operationHistory.getOperationDefinition().getDisplayName() + " on " + storageNode.getResource() + ": "
            + operationHistory.getErrorMessage());

        storageNode.setErrorMessage(opType + " has been aborted due to canceled resource operation ["
            + operationHistory.getOperationDefinition().getDisplayName() + "].");
        storageNode.setFailedOperation(operationHistory);
    }

    private void deploymentOperationFailed(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode newStorageNode) {
        operationFailed(storageNode, operationHistory, newStorageNode, "Deployment");
    }

    private void undeploymentOperationFailed(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode removedNode) {
        operationFailed(storageNode, operationHistory, removedNode, "Undeployment");
    }

    private void deploymentOperationFailed(StorageNode storageNode, ResourceOperationHistory operationHistory) {
        operationFailed(storageNode, operationHistory, "Deployment");
    }

    private void undeploymentOperationFailed(StorageNode storageNode, ResourceOperationHistory operationHistory) {
        operationFailed(storageNode, operationHistory, "Undeployment");
    }

    private void repairFailed(StorageNode storageNode, ResourceOperationHistory operationHistory) {
        operationFailed(storageNode, operationHistory, "Scheduled repair");
    }

    private void operationFailed(StorageNode storageNode, ResourceOperationHistory operationHistory, String opType) {
        log.error(opType + " has been aborted due to failed operation ["
            + operationHistory.getOperationDefinition().getDisplayName() + "] on " + storageNode.getResource() + ": "
            + operationHistory.getErrorMessage());

        storageNode.setErrorMessage(opType + " has been aborted due to failed resource operation ["
            + operationHistory.getOperationDefinition().getDisplayName() + "].");
        storageNode.setFailedOperation(operationHistory);
    }

    private void operationFailed(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode movingNode, String opType) {
        log.error(opType + " has been aborted due to failed operation ["
            + operationHistory.getOperationDefinition().getDisplayName() + "] on " + storageNode.getResource() + ": "
            + operationHistory.getErrorMessage());

        movingNode.setErrorMessage(opType + " has been aborted due to failed resource operation on "
            + storageNode.getAddress());
        storageNode.setErrorMessage(opType + " of " + movingNode.getAddress() + " has been aborted due "
            + "to failed resource operation [" + operationHistory.getOperationDefinition().getDisplayName() + "].");
        storageNode.setFailedOperation(operationHistory);
    }

    private StorageNode findStorageNode(Resource resource) {
        for (StorageNode storageNode : storageNodeManager.getStorageNodes()) {
            if (storageNode.getResource().getId() == resource.getId()) {
                return storageNode;
            }
        }
        return null;
    }

    private StorageNode takeFromMaintenanceQueue() {
        List<StorageNode> storageNodes = entityManager
            .createQuery(
                "SELECT s FROM StorageNode s WHERE "
                    + "s.operationMode = :operationMode AND s.maintenancePending = :maintenancePending",
                StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL)
            .setParameter("maintenancePending", true).getResultList();

        if (storageNodes.isEmpty()) {
            return null;
        }
        return storageNodes.get(0);
    }

    private StorageNode findStorageNodeByAddress(String address) {
        return entityManager.createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS, StorageNode.class)
            .setParameter("address", address).getSingleResult();
    }

    private boolean isStorageNodeOperation(ResourceOperationHistory operationHistory) {
        if (operationHistory == null) {
            return false;
        }

        ResourceType resourceType = operationHistory.getOperationDefinition().getResourceType();
        return resourceType.getName().equals(STORAGE_NODE_TYPE_NAME)
            && resourceType.getPlugin().equals(STORAGE_NODE_PLUGIN_NAME);
    }

    private boolean updateSchemaIfNecessary(int previousClusterSize, int newClusterSize) {
        boolean isRepairNeeded;
        int replicationFactor = 1;

        if (previousClusterSize == 0) {
            throw new IllegalStateException("previousClusterSize cannot be 0");
        }
        if (newClusterSize == 0) {
            throw new IllegalStateException("newClusterSize cannot be 0");
        }
        if (Math.abs(newClusterSize - previousClusterSize) != 1) {
            throw new IllegalStateException("The absolute difference between previousClusterSize["
                + previousClusterSize + "] and newClusterSize[" + newClusterSize + "] must be 1");
        }

        if (newClusterSize == 1) {
            isRepairNeeded = false;
            replicationFactor = 1;
        } else if (newClusterSize >= 5) {
            isRepairNeeded = false;
        } else if (previousClusterSize == 4 && newClusterSize == 3) {
            isRepairNeeded = true;
            replicationFactor = 2;
        } else if (previousClusterSize == 3 && newClusterSize == 2) {
            isRepairNeeded = false;
        } else if (previousClusterSize == 1 && newClusterSize == 2) {
            isRepairNeeded = true;
            replicationFactor = 2;
        } else if (previousClusterSize == 2 && newClusterSize == 3) {
            isRepairNeeded = false;
        } else if (previousClusterSize == 3 && newClusterSize == 4) {
            isRepairNeeded = true;
            replicationFactor = 3;
        } else {
            throw new IllegalStateException("previousClusterSize[" + previousClusterSize + "] and newClusterSize["
                + newClusterSize + "] is not supported");
        }

        if (isRepairNeeded) {
            updateReplicationFactor(replicationFactor);
            if (previousClusterSize == 1) {
                updateGCGraceSeconds(691200); // 8 days
            }
        } else if (newClusterSize == 1) {
            updateReplicationFactor(1);
            updateGCGraceSeconds(0);
        }

        return isRepairNeeded;
    }

    private void updateReplicationFactor(int replicationFactor) {
        StorageSession session = storageClientManager.getSession();
        session.execute("ALTER KEYSPACE rhq WITH replication = {'class': 'SimpleStrategy', 'replication_factor': "
            + replicationFactor + "}");
        session.execute("ALTER KEYSPACE system_auth WITH replication = {'class': 'SimpleStrategy', "
            + "'replication_factor': " + replicationFactor + "}");
    }

    private void updateGCGraceSeconds(int seconds) {
        StorageSession session = storageClientManager.getSession();
        session.execute("ALTER COLUMNFAMILY rhq.metrics_index WITH gc_grace_seconds = " + seconds);
        session.execute("ALTER COLUMNFAMILY rhq.raw_metrics WITH gc_grace_seconds = " + seconds);
        session.execute("ALTER COLUMNFAMILY rhq.one_hour_metrics WITH gc_grace_seconds = " + seconds);
        session.execute("ALTER COLUMNFAMILY rhq.six_hour_metrics WITH gc_grace_seconds = " + seconds);
        session.execute("ALTER COLUMNFAMILY rhq.twenty_four_hour_metrics WITH gc_grace_seconds = " + seconds);
        session.execute("ALTER COLUMNFAMILY rhq.schema_version WITH gc_grace_seconds = " + seconds);
    }

    //    private String getRequiredStorageProperty(String property) {
    //        String value = System.getProperty(property);
    //        if (StringUtil.isEmpty(property)) {
    //            throw new IllegalStateException("The system property [" + property + "] is not set. The RHQ "
    //                + "server will not be able connect to the RHQ storage node(s). This property should be defined "
    //                + "in rhq-server.properties.");
    //        }
    //        return value;
    //    }

    /**
     * Optimally this should be called outside of a transaction, because when scheduling an operation we
     * currently call back into {@link StorageNodeOperationsHandlerBean#handleOperationUpdateIfNecessary(OperationHistory)}.
     * This runs the risk of locking if called inside an existing transaction.
     */
    private void scheduleOperation(Subject subject, StorageNode storageNode, Configuration parameters, String operation) {
        scheduleOperation(subject, storageNode, parameters, operation, 300);
    }

    /**
     * Optimally this should be called outside of a transaction, because when scheduling an operation we
     * currently call back into {@link StorageNodeOperationsHandlerBean#handleOperationUpdateIfNecessary(OperationHistory)}.
     * This runs the risk of locking if called inside an existing transaction.
     */
    private void scheduleOperation(Subject subject, StorageNode storageNode, Configuration parameters,
        String operation, int timeout) {
        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(storageNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subject);
        schedule.setOperationName(operation);
        parameters.setSimpleValue(OperationDefinition.TIMEOUT_PARAM_NAME, Integer.toString(timeout));
        schedule.setParameters(parameters);

        operationManager.scheduleResourceOperation(subject, schedule);

    }

    private String getAddress(PropertyList addressList) {
        List<String> list = new ArrayList<String>(addressList.getList().size());
        for (Property property : addressList.getList()) {
            PropertySimple simple = (PropertySimple) property;
            list.add(simple.getStringValue());
        }
        return list.get(0);
    }

    private PropertyList createPropertyListOfAddresses(String propertyName, List<StorageNode> nodes) {
        PropertyList list = new PropertyList(propertyName);
        for (StorageNode storageNode : nodes) {
            list.add(new PropertySimple("address", storageNode.getAddress()));
        }
        return list;
    }

}
