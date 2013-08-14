package org.rhq.enterprise.server.storage;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
@Stateless
public class StorageNodeOperationsHandlerBean implements StorageNodeOperationsHandlerLocal {

    private final Log log  = LogFactory.getLog(StorageNodeOperationsHandlerBean.class);

    private static final String STORAGE_NODE_TYPE_NAME = "RHQ Storage Node";
    private static final String STORAGE_NODE_PLUGIN_NAME = "RHQStorage";
    private static final String USERNAME_PROPERTY = "rhq.cassandra.username";
    private static final String PASSWORD_PROPERTY = "rhq.cassandra.password";
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

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void announceStorageNode(Subject subject, StorageNode storageNode) {
        if (log.isInfoEnabled()) {
            log.info("Announcing " + storageNode + " to storage node cluster.");
        }
        storageNode.setOperationMode(StorageNode.OperationMode.ANNOUNCE);
        List<StorageNode> clusterNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL).getResultList();
        List<StorageNode> allNodes = new ArrayList<StorageNode>(clusterNodes);
        allNodes.add(storageNode);

        announceStorageNode(subject, storageNode, createPropertyListOfAddresses("addresses", allNodes),
            getAddresses(clusterNodes));

    }

    private void announceStorageNode(Subject subject, StorageNode storageNode, PropertyList addresses,
        List<String> remainingNodes) {
        String address = remainingNodes.remove(0);
        StorageNode clusterNode = findStorageNodeByAddress(address);

        if (log.isInfoEnabled()) {
            log.info("Announcing " + storageNode + " to cluster node " + clusterNode);
        }
        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(clusterNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subject);
        schedule.setOperationName("updateKnownNodes");
        Configuration parameters = new Configuration();
        parameters.put(addresses);
        parameters.put(new PropertySimple("remainingNodes", StringUtil.listToString(remainingNodes)));
        schedule.setParameters(parameters);

        operationManager.scheduleResourceOperation(subject, schedule);
    }

    @Override
    public void performAddNodeMaintenanceIfNecessary(InetAddress storageNodeAddress) {
        StorageNode storageNode = entityManager.createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS,
            StorageNode.class).setParameter("address", storageNodeAddress.getHostAddress()).getSingleResult();

        if (storageNode.getOperationMode() == StorageNode.OperationMode.BOOTSTRAP) {
            performAddNodeMaintenance(subjectManager.getOverlord(), storageNode);
        } else {
            log.info(storageNode + " has already been bootstrapped. Skipping add node maintenance.");
        }
    }

    @Override
    public void performAddNodeMaintenance(Subject subject, StorageNode storageNode) {
        storageNode.setOperationMode(StorageNode.OperationMode.ADD_NODE_MAINTENANCE);
        List<StorageNode> clusterNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.NORMAL)
            .getResultList();
        for (StorageNode node : clusterNodes) {
            node.setOperationMode(StorageNode.OperationMode.ADD_NODE_MAINTENANCE);
        }
        clusterNodes.add(storageNode);
        boolean runRepair = updateSchemaIfNecessary(clusterNodes);
        performAddNodeMaintenance(subject, storageNode, runRepair, createPropertyListOfAddresses(SEEDS_LIST,
            clusterNodes));
    }

    private void performAddNodeMaintenance(Subject subject, StorageNode storageNode, boolean runRepair,
        PropertyList seedsList) {
        if (log.isInfoEnabled()) {
            log.info("Running addNodeMaintenance for storage node " + storageNode);
        }

        Subject overlord = subjectManager.getOverlord();

        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(storageNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subject);
        schedule.setOperationName("addNodeMaintenance");

        Configuration config = new Configuration();
        config.put(seedsList);
        config.put(new PropertySimple(RUN_REPAIR_PROPERTY, runRepair));
        config.put(new PropertySimple(UPDATE_SEEDS_LIST, Boolean.TRUE));

        schedule.setParameters(config);

        operationManager.scheduleResourceOperation(overlord, schedule);
    }

    @Override
    @Asynchronous
    public void handleOperationUpdateIfNecessary(OperationHistory operationHistory) {
        if (!(operationHistory instanceof ResourceOperationHistory)) {
            return;
        }

        ResourceOperationHistory resourceOperationHistory = entityManager.find(ResourceOperationHistory.class,
            operationHistory.getId());
        if (resourceOperationHistory == null) {
            return;
        }

        if (isStorageNodeOperation(resourceOperationHistory.getOperationDefinition())) {
            if (resourceOperationHistory.getOperationDefinition().getName().equals("updateKnownNodes")) {
                handleUpdateKnownNodes(resourceOperationHistory);
            } else if (operationHistory.getOperationDefinition().getName().equals("prepareForBootstrap")) {
                handlePrepareForBootstrap(resourceOperationHistory);
            } else if (operationHistory.getOperationDefinition().getName().equals("addNodeMaintenance")) {
                handleAddNodeMaintenance(resourceOperationHistory);
            }
        }
    }

    private void handlePrepareForBootstrap(ResourceOperationHistory resourceOperationHistory) {
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

                log.error("The operation [prepareForBootstrap] was canceled for " + newStorageNode +
                    ". Deployment of the new storage node cannot proceed.");
                operationCanceled(newStorageNode, resourceOperationHistory);
                return;
            case FAILURE:
                log.error("The operation [preparedForBootstrap] failed for " + newStorageNode + ". The reported " +
                    "failure is: " + resourceOperationHistory.getErrorMessage());
                log.error("Deployment of the new storage node cannot proceed.");
                operationFailed(newStorageNode, resourceOperationHistory);
                return;
            default:  // SUCCESS
                // Nothing to do because we wait for the C* driver to notify us that the
                // storage node has joined the cluster before we proceed with the work flow.
        }
    }

    private void handleUpdateKnownNodes(ResourceOperationHistory resourceOperationHistory) {
        StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
        StorageNode newStorageNode = null;
        switch (resourceOperationHistory.getStatus()) {
            case INPROGRESS:
                // nothing to do here
                return;
            case CANCELED:
                log.error("The operation [updateKnownNodes] was canceled for " + storageNode +
                    ". Deployment of the new storage node cannot proceed.");
                newStorageNode = findNewStorgeNode(StorageNode.OperationMode.ANNOUNCE);
                operationCanceled(storageNode, resourceOperationHistory, newStorageNode);
            case FAILURE:
                log.error("The operation [updateKnownNodes] failed for " + storageNode + ". The reported " +
                    "failure is: " + resourceOperationHistory.getErrorMessage());
                log.error("Deployment of the new storage node cannot proceed.");
                newStorageNode = findNewStorgeNode(StorageNode.OperationMode.ANNOUNCE);
                operationFailed(storageNode, resourceOperationHistory, newStorageNode);
                return;
            default:  // SUCCESS
                if (log.isInfoEnabled()) {
                    log.info("Finished announcing cluster nodes to " + storageNode);
                }
                Configuration parameters = resourceOperationHistory.getParameters();
                PropertyList addresses = parameters.getList("addresses");
                List<String> remainingNodes = getRemainingNodes(resourceOperationHistory);

                newStorageNode = findNewStorgeNode(StorageNode.OperationMode.ANNOUNCE);
                Subject subject = getSubject(resourceOperationHistory);

                if (remainingNodes.isEmpty()) {
                    log.info("Successfully announced new storage node to cluster");
                    newStorageNode.setOperationMode(StorageNode.OperationMode.BOOTSTRAP);
                    prepareNodeForBootstrap(subject, newStorageNode, addresses.deepCopy(false));
                } else {
                    announceStorageNode(subject, newStorageNode, addresses.deepCopy(false), remainingNodes);
                }
        }
    }

    private void handleAddNodeMaintenance(ResourceOperationHistory resourceOperationHistory) {
        StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
        StorageNode newStorageNode = null;
        switch (resourceOperationHistory.getStatus()) {
            case INPROGRESS:
                // nothing to do here
                return;
            case CANCELED:
                log.error("The operation [addNodeMaintenance] was canceled for " + storageNode + ". This operation " +
                    "needs to be run on each storage node when a new node is added to the cluster.");
                newStorageNode = findNewStorgeNode(StorageNode.OperationMode.ADD_NODE_MAINTENANCE);
                operationCanceled(storageNode, resourceOperationHistory, newStorageNode);
                return;
            case FAILURE:
                log.error("The operation [addNodeMaintenance] failed for " + storageNode + ". This operation " +
                    "needs to be run on each storage node when a new node is added to the cluster. The reported " +
                    "failure is: " + resourceOperationHistory.getErrorMessage());
                newStorageNode = findNewStorgeNode(StorageNode.OperationMode.ADD_NODE_MAINTENANCE);
                operationFailed(storageNode, resourceOperationHistory, newStorageNode);
                return;
            default:  // SUCCESS
                if (log.isInfoEnabled()) {
                    log.info("Finnished cluster maintenance for " + storageNode + " for addition of new node");
                }
                storageNode.setOperationMode(StorageNode.OperationMode.NORMAL);
                StorageNode nextNode = takeFromQueue(storageNode, StorageNode.OperationMode.ADD_NODE_MAINTENANCE);

                if (nextNode == null) {
                    log.info("Finished running cluster maintenance for addition of new node");
                } else {
                    Configuration parameters = resourceOperationHistory.getParameters();
                    boolean runRepair = parameters.getSimple(RUN_REPAIR_PROPERTY).getBooleanValue();
                    PropertyList seedsList = parameters.getList(SEEDS_LIST).deepCopy(false);
                    Subject subject = getSubject(resourceOperationHistory);
                    performAddNodeMaintenance(subject, nextNode, runRepair, seedsList);
                }
        }
    }

    private Subject getSubject(ResourceOperationHistory resourceOperationHistory) {
        Subject subject = subjectManager.getSubjectByName(resourceOperationHistory.getSubjectName());
        return SessionManager.getInstance().put(subject);
    }

    private void operationCanceled(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode newStorageNode) {
        newStorageNode.setErrorMessage("Deployment has been aborted due to canceled resource operation on " +
            storageNode.getAddress());
        storageNode.setErrorMessage("Deployment of " + newStorageNode.getAddress() + " has been aborted due " +
            "to cancellation of resource operation [" + operationHistory.getOperationDefinition().getDisplayName() +
            "].");
        storageNode.setFailedOperation(operationHistory);
    }

    private void operationCanceled(StorageNode newStorageNode, ResourceOperationHistory operationHistory) {
        newStorageNode.setErrorMessage("Deployment has been aborted due to canceled resource operation [" +
            operationHistory.getOperationDefinition().getDisplayName() + "].");
        newStorageNode.setFailedOperation(operationHistory);
    }

    private void operationFailed(StorageNode storageNode, ResourceOperationHistory operationHistory,
        StorageNode newStorageNode) {
        newStorageNode.setErrorMessage("Deployment has been aborted due to failed resource operation on " +
            storageNode.getAddress());
        storageNode.setErrorMessage("Deployment of " + newStorageNode.getAddress() + " has been aborted due " +
            "to failed resource operation [" + operationHistory.getOperationDefinition().getDisplayName() + "].");
        storageNode.setFailedOperation(operationHistory);
    }

    private void operationFailed(StorageNode newStorageNode, ResourceOperationHistory operationHistory) {
        newStorageNode.setErrorMessage("Deployment has been aborted due to failed resource operation [" +
            operationHistory.getOperationDefinition().getDisplayName() + "].");
        newStorageNode.setFailedOperation(operationHistory);
    }

    private StorageNode findStorageNode(Resource resource) {
        for (StorageNode storageNode : storageNodeManager.getStorageNodes()) {
            if (storageNode.getResource().getId() == resource.getId()) {
                return storageNode;
            }
        }
        return null;
    }

    @Override
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

        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(storageNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subject);
        schedule.setOperationName("prepareForBootstrap");

        StorageClusterSettings clusterSettings = storageClusterSettingsManager.getClusterSettings(subject);
        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("cqlPort", clusterSettings.getCqlPort()));
        parameters.put(new PropertySimple("gossipPort", clusterSettings.getGossipPort()));
        parameters.put(addresses);

        schedule.setParameters(parameters);

        operationManager.scheduleResourceOperation(subject, schedule);
    }

    private StorageNode takeFromQueue(StorageNode lastTaken, StorageNode.OperationMode queue) {
        List<StorageNode> nodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE_EXCLUDING,
            StorageNode.class).setParameter("operationMode", queue).setParameter("storageNode", lastTaken)
            .getResultList();

        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0);
    }

    private List<String> getRemainingNodes(ResourceOperationHistory resourceOperationHistory) {
        LinkedList<String> addresses = new LinkedList<String>();
        Configuration results = resourceOperationHistory.getResults();
        String remainingNodes = results.getSimpleValue("remainingNodes");

        if (!StringUtil.isEmpty(remainingNodes)) {
            for (String address : remainingNodes.split(",")) {
                addresses.add(address);
            }
        }
        return addresses;
    }

    private StorageNode findStorageNodeByAddress(String address) {
        try {
            return entityManager.createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS, StorageNode.class)
                .setParameter("address", address).getSingleResult();

        } catch (PersistenceException e) {
            throw new StorageNodeDeploymentException("Storage node deployment has failed! Failed to fetch the next " +
                "storage node at " + address + " to be updated.", e);
        }
    }

    private StorageNode findNewStorgeNode(StorageNode.OperationMode operationMode) {
        try {
            return entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE, StorageNode.class)
                .setParameter("operationMode", operationMode).getSingleResult();
        } catch (PersistenceException e) {
            throw new StorageNodeDeploymentException("Storage node deployment has failed! Failed to fetch the " +
                "storage node to be deployed.", e);
        }
    }

    private boolean isStorageNodeOperation(OperationDefinition operationDefinition) {
        ResourceType resourceType = operationDefinition.getResourceType();
        return resourceType.getName().equals(STORAGE_NODE_TYPE_NAME) &&
            resourceType.getPlugin().equals(STORAGE_NODE_PLUGIN_NAME);
    }

    private boolean updateSchemaIfNecessary(List<StorageNode> storageNodes) {
        // The previous cluster size will be the current size - 1 since we currently only
        // support deploying one node at a time.
        int previousClusterSize = storageNodes.size() - 1;
        boolean isRepairNeeded;
        int replicationFactor = 1;

        if (previousClusterSize >= 4) {
            // At 4 nodes we increase the RF to 3. We are not increasing the RF beyond
            // that for additional nodes; so, there is no need to run repair if we are
            // expanding from a 4 node cluster since the RF remains the same.
            isRepairNeeded = false;
        } else if (previousClusterSize == 1) {
            // The RF will increase since we are going from a single to a multi-node
            // cluster; therefore, we want to run repair.
            isRepairNeeded = true;
            replicationFactor = 2;
        } else if (previousClusterSize == 2) {
            if (storageNodes.size() > 3) {
                // If we go from 2 to > 3 nodes we will increase the RF to 3; therefore
                // we want to run repair.
                isRepairNeeded = true;
                replicationFactor = 3;
            } else {
                // If we go from 2 to 3 nodes, we keep the RF at 2 so there is no need
                // to run repair.
                isRepairNeeded = false;
            }
        } else if (previousClusterSize == 3) {
            // We are increasing the cluster size > 3 which means the RF will be
            // updated to 3; therefore, we want to run repair.
            isRepairNeeded = true;
            replicationFactor = 3;
        } else {
            // If we cluster size of zero, then something is really screwed up. It
            // should always be > 0.
            throw new RuntimeException("The previous cluster size should never be zero at this point");
        }

        if (isRepairNeeded) {
//            String username = getRequiredStorageProperty(USERNAME_PROPERTY);
//            String password = getRequiredStorageProperty(PASSWORD_PROPERTY);
//            SchemaManager schemaManager = new SchemaManager(username, password, storageNodes);
//            try{
//                schemaManager.updateTopology();
//            } catch (Exception e) {
//                log.error("An error occurred while applying schema topology changes", e);
//            }

            updateReplicationFactor(replicationFactor);
            if (previousClusterSize == 1) {
                updateGCGraceSeconds(691200);  // 8 days
            }
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

    private PropertyList createPropertyListOfAddresses(String propertyName, List<StorageNode> nodes) {
        PropertyList list = new PropertyList(propertyName);
        for (StorageNode storageNode : nodes) {
            list.add(new PropertySimple("address", storageNode.getAddress()));
        }
        return list;
    }

    private List<String> getAddresses(List<StorageNode> storageNodes) {
        List<String> addresses = new LinkedList<String>();
        for (StorageNode storageNode : storageNodes) {
            addresses.add(storageNode.getAddress());
        }
        return addresses;
    }

}
