package org.rhq.enterprise.server.storage;

import java.net.InetAddress;
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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SessionManager;
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

    private final Log log  = LogFactory.getLog(StorageNodeOperationsHandlerBean.class);

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
    private StorageClientManagerBean storageClientManager;

    @EJB
    private StorageNodeOperationsHandlerLocal storageNodeOperationsHandler;

    @EJB
    private ResourceManagerLocal resourceManager;

    @Override
    public void announceStorageNode(Subject subject, StorageNode storageNode) {
        if (log.isInfoEnabled()) {
            log.info("Announcing " + storageNode + " to storage node cluster.");
        }
        List<StorageNode> clusterNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL).getResultList();
        List<StorageNode> allNodes = new ArrayList<StorageNode>(clusterNodes);
        allNodes.add(storageNode);

        for (StorageNode clusterNode : clusterNodes) {
            clusterNode.setMaintenancePending(true);
        }

        announceStorageNode(subject, storageNode, clusterNodes.get(0), createPropertyListOfAddresses("addresses",
            allNodes));

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
    public void unannounceStorageNode(Subject subject, StorageNode storageNode) {
        log.info("Unannouncing " + storageNode);

        List<StorageNode> clusterNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL).getResultList();
        for (StorageNode clusterNode : clusterNodes) {
            clusterNode.setMaintenancePending(true);
        }
        unannounceStorageNode(subject, clusterNodes.get(0), createPropertyListOfAddresses("addresses", clusterNodes));
    }

    private void unannounceStorageNode(Subject subject, StorageNode clusterNode, PropertyList addresses) {
        Configuration parameters = new Configuration();
        parameters.put(addresses);

        scheduleOperation(subject, clusterNode, parameters, "unannounce");
    }

    @Override
    public void uninstall(Subject subject, StorageNode storageNode) {
        log.info("Uninstalling " + storageNode);

        if (storageNode.getResource() == null) {
            finishUninstall(subject, storageNode);
        } else {
            scheduleOperation(subject, storageNode, new Configuration(), "uninstall");
        }
    }

    private void finishUninstall(Subject subject, StorageNode storageNode) {
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
    public void decommissionStorageNode(Subject subject, StorageNode storageNode) {
        log.info("Preparing to decommission " + storageNode);

        storageNode = storageNodeOperationsHandler.setMode(storageNode, StorageNode.OperationMode.DECOMMISSION);
        List<StorageNode> storageNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL).getResultList();

        boolean runRepair = updateSchemaIfNecessary(storageNodes.size() + 1, storageNodes.size());
        // This is a bit of a hack since the maintenancePending flag is really intended to
        // queue up storage nodes during cluster maintenance operations.
        storageNode.setMaintenancePending(runRepair);
        storageNode = entityManager.merge(storageNode);
        scheduleOperation(subject, storageNode, new Configuration(), "decommission");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logError(StorageNode.OperationMode newStorageNodeOperationMode, String error, Exception e) {
        try {
            StorageNode newStorageNode = findStorageNodeByMode(newStorageNodeOperationMode);
            newStorageNode.setErrorMessage(error + " Check the server log for details. Root cause: " +
                ThrowableUtil.getRootCause(e).getMessage());
        } catch (Exception e1) {
            log.error("Failed to log error against storage node", e);
        }
    }

    @Override
    public void performAddNodeMaintenanceIfNecessary(InetAddress storageNodeAddress) {
        try {
            StorageNode storageNode = entityManager.createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS,
                StorageNode.class).setParameter("address", storageNodeAddress.getHostAddress()).getSingleResult();

            if (storageNode.getOperationMode() == StorageNode.OperationMode.BOOTSTRAP) {
                // TODO need to add support for HA deployments
                // If multiple RHQ servers are running, they will all receive the event
                // notification that the node is up and will all wind up calling this method.
                storageNode = storageNodeOperationsHandler.setMode(storageNode, StorageNode.OperationMode.ADD_MAINTENANCE);
                performAddNodeMaintenance(subjectManager.getOverlord(), storageNode);
            } else {
                log.info(storageNode + " has already been bootstrapped. Skipping add node maintenance.");
            }
        } catch (Exception e) {
            String msg = "Aborting storage node deployment due to unexpected error while performing add node " +
                "maintenance.";
            log.error(msg, e);
            storageNodeOperationsHandler.logError(StorageNode.OperationMode.ADD_MAINTENANCE, msg, e);
        }
    }

    @Override
    public void performAddNodeMaintenance(Subject subject, StorageNode storageNode) {
        try {
            List<StorageNode> clusterNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
                StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL)
                .getResultList();
            for (StorageNode node : clusterNodes) {
                node.setMaintenancePending(true);
            }
            storageNode.setMaintenancePending(true);
            clusterNodes.add(storageNode);
            boolean runRepair = updateSchemaIfNecessary(clusterNodes.size() - 1, clusterNodes.size());
            performAddNodeMaintenance(subject, storageNode, runRepair, createPropertyListOfAddresses(SEEDS_LIST,
                clusterNodes));
        } catch (Exception e) {
            String msg = "Aborting storage node deployment due to unexpected error while performing add node " +
                "maintenance.";
            log.error(msg, e);
            storageNodeOperationsHandler.logError(StorageNode.OperationMode.ADD_MAINTENANCE, msg, e);
        }
    }

    private void performAddNodeMaintenance(Subject subject, StorageNode storageNode, boolean runRepair,
        PropertyList seedsList) {
        if (log.isInfoEnabled()) {
            log.info("Running addNodeMaintenance for storage node " + storageNode);
        }
        Configuration params = new Configuration();
        params.put(seedsList);
        params.put(new PropertySimple(RUN_REPAIR_PROPERTY, runRepair));
        params.put(new PropertySimple(UPDATE_SEEDS_LIST, Boolean.TRUE));

        scheduleOperation(subject, storageNode, params, "addNodeMaintenance");
    }

    @Override
    public void performRemoveNodeMaintenanceIfNecessary(InetAddress storageNodeAddress) {
        try {
            StorageNode storageNode = entityManager.createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS,
                StorageNode.class).setParameter("address", storageNodeAddress.getHostAddress()).getSingleResult();

            if (storageNode.getOperationMode() == StorageNode.OperationMode.DECOMMISSION) {
                // TODO need to add support for HA deployments
                // If multiple RHQ servers are running, they will all receive the event
                // notification that the node is up and will all wind up calling this method.
                storageNode = storageNodeOperationsHandler.setMode(storageNode,
                    StorageNode.OperationMode.REMOVE_MAINTENANCE);
                performRemoveNodeMaintenance(subjectManager.getOverlord(), storageNode);
            } else {
                log.info("Remove node maintenance has already been run for " + storageNode);
            }
        } catch (Exception e) {
            String msg = "Aborting undeployment due to unexpected error while performing remove node maintenance for " +
                storageNodeAddress.getHostAddress();
            log.error(msg, e);
            storageNodeOperationsHandler.logError(StorageNode.OperationMode.REMOVE_MAINTENANCE, msg, e);
        }
    }

    @Override
    public void performRemoveNodeMaintenance(Subject subject, StorageNode storageNode) {
        try {
            List<StorageNode> clusterNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
                StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL)
                .getResultList();
            for (StorageNode node : clusterNodes) {
                node.setMaintenancePending(true);
            }
            boolean runRepair = storageNode.isMaintenancePending();
            performRemoveNodeMaintenance(subject, clusterNodes.get(0), runRepair,
                createPropertyListOfAddresses(SEEDS_LIST, clusterNodes));
        } catch (Exception e) {
            String msg = "Aborting undeployment due to unexpected error while performing remove node maintenance.";
            log.error(msg, e);
            storageNodeOperationsHandler.logError(StorageNode.OperationMode.REMOVE_MAINTENANCE, msg, e);
        }
    }

    private void performRemoveNodeMaintenance(Subject subject, StorageNode storageNode, boolean runRepair,
        PropertyList seedsList) {
        if (log.isInfoEnabled()) {
            log.info("Running remove node maintenance for storage node " + storageNode);
        }
        Configuration params = new Configuration();
        params.put(seedsList);
        params.put(new PropertySimple(RUN_REPAIR_PROPERTY, runRepair));
        params.put(new PropertySimple(UPDATE_SEEDS_LIST, true));

        scheduleOperation(subject, storageNode, params, "removeNodeMaintenance");
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
                log.error(msg, e);
                storageNodeOperationsHandler.logError(StorageNode.OperationMode.ANNOUNCE, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("prepareForBootstrap")) {
            try {
                storageNodeOperationsHandler.handlePrepareForBootstrap(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting storage node deployment due to unexpected error while bootstrapping new node.";
                log.error(msg, e);
                storageNodeOperationsHandler.logError(StorageNode.OperationMode.BOOTSTRAP, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("addNodeMaintenance")) {
            try {
                storageNodeOperationsHandler.handleAddNodeMaintenance(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting storage node deployment due to unexpected error while performing add node " +
                    "maintenance.";
                log.error(msg, e);
                storageNodeOperationsHandler.logError(StorageNode.OperationMode.ADD_MAINTENANCE, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("decommission")) {
            try {
                storageNodeOperationsHandler.handleDecommission(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting undeployment due to unexpected error while decommissioning storage node.";
                log.error(msg, e);
                storageNodeOperationsHandler.logError(StorageNode.OperationMode.DECOMMISSION, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("removeNodeMaintenance")) {
            try {
                storageNodeOperationsHandler.handleRemoveNodeMaintenance(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting undeployment due to unexpected error while performing remove node maintenance.";
                log.error(msg, e);
                storageNodeOperationsHandler.logError(StorageNode.OperationMode.REMOVE_MAINTENANCE, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("unannounce")) {
            try {
                storageNodeOperationsHandler.handleUnannounce(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting undeployment due to unexpected error while performing unannouncement.";
                log.error(msg, e);
                storageNodeOperationsHandler.logError(StorageNode.OperationMode.UNANNOUNCE, msg, e);
            }
        } else if (operationHistory.getOperationDefinition().getName().equals("uninstall")) {
            try {
                storageNodeOperationsHandler.handleUninstall(resourceOperationHistory);
            } catch (Exception e) {
                String msg = "Aborting undeployment due to unexpected error while uninstalling.";
                log.error(msg, e);
                storageNodeOperationsHandler.logError(StorageNode.OperationMode.UNINSTALL, msg, e);
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

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleAnnounce(ResourceOperationHistory resourceOperationHistory) {
        StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
        StorageNode newStorageNode = null;
        switch (resourceOperationHistory.getStatus()) {
        case INPROGRESS:
            // nothing to do here
            return;
        case CANCELED:
            newStorageNode = findStorageNodeByMode(StorageNode.OperationMode.ANNOUNCE);
            deploymentOperationCanceled(storageNode, resourceOperationHistory, newStorageNode);
        case FAILURE:
            newStorageNode = findStorageNodeByMode(StorageNode.OperationMode.ANNOUNCE);
            deploymentOperationFailed(storageNode, resourceOperationHistory, newStorageNode);
            return;
        default:  // SUCCESS
            storageNode.setMaintenancePending(false);
            Configuration parameters = resourceOperationHistory.getParameters();
            PropertyList addresses = parameters.getList("addresses");
            StorageNode nextNode = takeFromMaintenanceQueue();

            newStorageNode = findStorageNodeByMode(StorageNode.OperationMode.ANNOUNCE);
            Subject subject = getSubject(resourceOperationHistory);

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
        StorageNode removedStorageNode = findStorageNodeByMode(StorageNode.OperationMode.UNANNOUNCE);
        switch (operationHistory.getStatus()) {
            case INPROGRESS:
                // nothing to do here
                break;
            case CANCELED:
                undeploymentOperationCanceled(storageNode, operationHistory, removedStorageNode);
                break;
            case FAILURE:
                deploymentOperationFailed(storageNode, operationHistory, removedStorageNode);
                break;
            default:  // SUCCESS
                storageNode.setMaintenancePending(false);

                StorageNode nextNode = takeFromMaintenanceQueue();
                Subject subject = getSubject(operationHistory);

                if (nextNode == null) {
                    log.info("Successfully unannounced " + removedStorageNode + " to storage cluster");
                    removedStorageNode = storageNodeOperationsHandler.setMode(removedStorageNode,
                        StorageNode.OperationMode.UNINSTALL);
                    uninstall(getSubject(operationHistory), removedStorageNode);
                } else {
                    Configuration params = operationHistory.getParameters();
                    PropertyList addresses = params.getList("addresses");
                    unannounceStorageNode(subject, nextNode, addresses.deepCopy(false));
                }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handlePrepareForBootstrap(ResourceOperationHistory resourceOperationHistory) {
        StorageNode newStorageNode = findStorageNode(resourceOperationHistory.getResource());
        switch (resourceOperationHistory.getStatus()) {
            case INPROGRESS:
                // nothing to do here
                return;
            case CANCELED:
                // TODO Verify whether or not the node has been bootstrapped
                // If the operation is canceled the plugin will get an InterruptedException.
                // The actual bootstrapping may very well complete so we need to add in some
                // checks to find out if the node is up and part of the cluster.
                deploymentOperationCanceled(newStorageNode, resourceOperationHistory);
                return;
            case FAILURE:
                deploymentOperationFailed(newStorageNode, resourceOperationHistory);
                return;
            default:  // SUCCESS
                // Nothing to do because we wait for the C* driver to notify us that the
                // storage node has joined the cluster before we proceed with the work flow.
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleAddNodeMaintenance(ResourceOperationHistory resourceOperationHistory) {
        StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
        StorageNode newStorageNode = findStorageNodeByMode(StorageNode.OperationMode.ADD_MAINTENANCE);
        switch (resourceOperationHistory.getStatus()) {
            case INPROGRESS:
                // nothing to do here
                return;
            case CANCELED:
                deploymentOperationCanceled(storageNode, resourceOperationHistory, newStorageNode);
                return;
            case FAILURE:
                deploymentOperationFailed(storageNode, resourceOperationHistory, newStorageNode);
                return;
            default:  // SUCCESS
                log.info("Finished running add node maintenance for " + storageNode);
                storageNode.setMaintenancePending(false);
                StorageNode nextNode = takeFromMaintenanceQueue();

                if (nextNode == null) {
                    log.info("Finished running add node maintenance on all cluster nodes");
                    storageNodeOperationsHandler.setMode(newStorageNode, StorageNode.OperationMode.NORMAL);
                } else {
                    Configuration parameters = resourceOperationHistory.getParameters();
                    boolean runRepair = parameters.getSimple(RUN_REPAIR_PROPERTY).getBooleanValue();
                    PropertyList seedsList = parameters.getList(SEEDS_LIST).deepCopy(false);
                    Subject subject = getSubject(resourceOperationHistory);
                    performAddNodeMaintenance(subject, nextNode, runRepair, seedsList);
                }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void bootstrapStorageNode(Subject subject, StorageNode storageNode) {
        List<StorageNode> clusterNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL).getResultList();
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
        StorageNode removedStorageNode = findStorageNodeByMode(StorageNode.OperationMode.REMOVE_MAINTENANCE);
        switch (operationHistory.getStatus()) {
            case INPROGRESS:
                // nothing to do here
                break;
            case CANCELED:
                undeploymentOperationCanceled(storageNode, operationHistory, removedStorageNode);
                break;
            case FAILURE:
                undeploymentOperationFailed(storageNode, operationHistory, removedStorageNode);
                break;
            default:  // SUCCESS
                log.info("Finished remove node maintenance for " + storageNode);
                storageNode.setMaintenancePending(false);
                StorageNode nextNode = takeFromMaintenanceQueue();

                if (nextNode == null) {
                    log.info("Finished running remove node maintenance on all cluster nodes");
                    removedStorageNode = storageNodeOperationsHandler.setMode(removedStorageNode,
                        StorageNode.OperationMode.UNANNOUNCE);
                    unannounceStorageNode(getSubject(operationHistory), removedStorageNode);
                } else {
                    Configuration parameters = operationHistory.getParameters();
                    boolean runRepair = parameters.getSimple(RUN_REPAIR_PROPERTY).getBooleanValue();
                    PropertyList seedsList = parameters.getList(SEEDS_LIST).deepCopy(false);
                    Subject subject = getSubject(operationHistory);
                    performRemoveNodeMaintenance(subject, nextNode, runRepair, seedsList);
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
        scheduleOperation(subject, storageNode, new Configuration(), "repair");
    }

    private void runRepair(Subject subject, StorageNode storageNode) {
        scheduleOperation(subject, storageNode, new Configuration(), "repair");
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
            default:  // SUCCESS
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
            default:  // SUCCESS
                log.info("Successfully decommissioned " + storageNode);
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
            default:  // SUCCESS
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
        Subject subject = subjectManager.getSubjectByName(resourceOperationHistory.getSubjectName());
        return SessionManager.getInstance().put(subject);
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
        log.error(opType + " has been aborted due to canceled operation [" +
            operationHistory.getOperationDefinition().getDisplayName() + " on " + storageNode.getResource() +
            ": " + operationHistory.getErrorMessage());

        movingNode.setErrorMessage(opType + " has been aborted due to canceled resource operation on " +
            storageNode.getAddress());
        storageNode.setErrorMessage(opType + " of " + movingNode.getAddress() + " has been aborted due " +
            "to cancellation of resource operation [" + operationHistory.getOperationDefinition().getDisplayName() +
            "].");
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
        log.error(opType + " has been aborted due to canceled operation [" +
            operationHistory.getOperationDefinition().getDisplayName() + " on " + storageNode.getResource() +
            ": " + operationHistory.getErrorMessage());

        storageNode.setErrorMessage(opType + " has been aborted due to canceled resource operation [" +
            operationHistory.getOperationDefinition().getDisplayName() + "].");
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
        log.error(opType + " has been aborted due to failed operation [" +
            operationHistory.getOperationDefinition().getDisplayName() + "] on " + storageNode.getResource() +
            ": " + operationHistory.getErrorMessage());

        storageNode.setErrorMessage(opType + " has been aborted due to failed resource operation [" +
            operationHistory.getOperationDefinition().getDisplayName() + "].");
        storageNode.setFailedOperation(operationHistory);
    }

    private void operationFailed(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode movingNode, String opType) {
            log.error(opType + " has been aborted due to failed operation [" +
                operationHistory.getOperationDefinition().getDisplayName() + "] on " + storageNode.getResource() +
                ": " + operationHistory.getErrorMessage());

            movingNode.setErrorMessage(opType + " has been aborted due to failed resource operation on " +
                storageNode.getAddress());
            storageNode.setErrorMessage(opType + " of " + movingNode.getAddress() + " has been aborted due " +
                "to failed resource operation [" + operationHistory.getOperationDefinition().getDisplayName() + "].");
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
        List<StorageNode> storageNodes = entityManager.createQuery("SELECT s FROM StorageNode s WHERE " +
            "s.operationMode = :operationMode AND s.maintenancePending = :maintenancePending", StorageNode.class)
            .setParameter("operationMode", StorageNode.OperationMode.NORMAL).setParameter("maintenancePending", true)
            .getResultList();

        if (storageNodes.isEmpty()) {
            return null;
        }
        return storageNodes.get(0);
    }

    private StorageNode findStorageNodeByMode(StorageNode.OperationMode operationMode) {
        return entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE, StorageNode.class)
                .setParameter("operationMode", operationMode).getSingleResult();
    }

    private boolean isStorageNodeOperation(ResourceOperationHistory operationHistory) {
        if (operationHistory == null) {
            return false;
        }

        ResourceType resourceType = operationHistory.getOperationDefinition().getResourceType();
        return resourceType.getName().equals(STORAGE_NODE_TYPE_NAME) &&
            resourceType.getPlugin().equals(STORAGE_NODE_PLUGIN_NAME);
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
            throw new IllegalStateException("The absolute difference between previousClusterSize[" +
                previousClusterSize + "] and newClusterSize[" + newClusterSize + "] must be 1");
        }

        if (newClusterSize == 1) {
            isRepairNeeded = false;
            replicationFactor = 1;
        } else if (previousClusterSize > 4 && newClusterSize == 4) {
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
        } else if (previousClusterSize == 4 && newClusterSize > 4) {
            isRepairNeeded = false;
        } else {
            throw new IllegalStateException("previousClusterSize[" + previousClusterSize + "] and newClusterSize[" +
                newClusterSize + "] is not supported");
        }




//        if (newClusterSize == 1) {
//            isRepairNeeded = false;
//            replicationFactor = 1;
//        }  else if (previousClusterSize >= 4) {
//            // At 4 nodes we increase the RF to 3. We are not increasing the RF beyond
//            // that for additional nodes; so, there is no need to run repair if we are
//            // expanding from a 4 node cluster since the RF remains the same.
//            isRepairNeeded = false;
//        } else if (previousClusterSize == 1) {
//            // The RF will increase since we are going from a single to a multi-node
//            // cluster; therefore, we want to run repair.
//            isRepairNeeded = true;
//            replicationFactor = 2;
//        } else if (previousClusterSize == 2) {
//            if (storageNodes.size() > 3) {
//                // If we go from 2 to > 3 nodes we will increase the RF to 3; therefore
//                // we want to run repair.
//                isRepairNeeded = true;
//                replicationFactor = 3;
//            } else {
//                // If we go from 2 to 3 nodes, we keep the RF at 2 so there is no need
//                // to run repair.
//                isRepairNeeded = false;
//            }
//        } else if (previousClusterSize == 3) {
//            // We are increasing the cluster size > 3 which means the RF will be
//            // updated to 3; therefore, we want to run repair.
//            isRepairNeeded = true;
//            replicationFactor = 3;
//        } else {
//            // If we cluster size of zero, then something is really screwed up. It
//            // should always be > 0.
//            throw new RuntimeException("The previous cluster size should never be zero at this point");
//        }

        if (isRepairNeeded) {
            updateReplicationFactor(replicationFactor);
            if (previousClusterSize == 1) {
                updateGCGraceSeconds(691200);  // 8 days
            }
        } else if (newClusterSize == 1) {
            updateReplicationFactor(1);
            updateGCGraceSeconds(0);
        }

        return isRepairNeeded;
    }

    private void updateReplicationFactor(int replicationFactor) {
        StorageSession session = storageClientManager.getSession();
        session.execute("ALTER KEYSPACE rhq WITH replication = {'class': 'SimpleStrategy', 'replication_factor': " +
            replicationFactor + "}");
        session.execute("ALTER KEYSPACE system_auth WITH replication = {'class': 'SimpleStrategy', " +
            "'replication_factor': " + replicationFactor + "}");
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

    private String getRequiredStorageProperty(String property) {
        String value = System.getProperty(property);
        if (StringUtil.isEmpty(property)) {
            throw new IllegalStateException("The system property [" + property + "] is not set. The RHQ "
                + "server will not be able connect to the RHQ storage node(s). This property should be defined "
                + "in rhq-server.properties.");
        }
        return value;
    }

    private void scheduleOperation(Subject subject, StorageNode storageNode, Configuration parameters,
        String operation) {
        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(storageNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subject);
        schedule.setOperationName(operation);
        schedule.setParameters(parameters);

        operationManager.scheduleResourceOperation(subject, schedule);

    }

    private PropertyList createPropertyListOfAddresses(String propertyName, List<StorageNode> nodes) {
        PropertyList list = new PropertyList(propertyName);
        for (StorageNode storageNode : nodes) {
            list.add(new PropertySimple("address", storageNode.getAddress()));
        }
        return list;
    }

}
