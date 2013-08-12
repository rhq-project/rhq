package org.rhq.enterprise.server.storage;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
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
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.RHQConstants;
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
    private StorageClusterSettingsManagerBean storageClusterSettingsManager;

    @EJB
    private StorageClientManagerBean storageClientManager;

    @Override
    public void announceNewStorageNode(StorageNode newStorageNode, StorageNode clusterNode, PropertyList addresses) {
        if (log.isInfoEnabled()) {
            log.info("Announcing new storage node " + newStorageNode + " to cluster node " + clusterNode);
        }
        Subject overlord = subjectManager.getOverlord();
        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(clusterNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(overlord);
        schedule.setOperationName("updateKnownNodes");
        Configuration parameters = new Configuration();
        parameters.put(addresses);
        schedule.setParameters(parameters);

        operationManager.scheduleResourceOperation(overlord, schedule);
    }

    @Override
    public void performAddNodeMaintenance(InetAddress storageNodeAddress) {
        StorageNode storageNode = entityManager.createNamedQuery(StorageNode.QUERY_FIND_BY_ADDRESS,
            StorageNode.class).setParameter("address", storageNodeAddress.getHostAddress()).getSingleResult();
        storageNode.setOperationMode(StorageNode.OperationMode.ADD_NODE_MAINTENANCE);

        List<StorageNode> clusterNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.ADD_NODE_MAINTENANCE)
            .getResultList();

        boolean runRepair = updateSchemaIfNecessary(clusterNodes);

        performAddNodeMaintenance(storageNode, runRepair, createPropertyListOfAddresses(SEEDS_LIST, clusterNodes));
    }

    private void performAddNodeMaintenance(StorageNode storageNode, boolean runRepair, PropertyList seedsList) {
        if (log.isInfoEnabled()) {
            log.info("Running addNodeMaintenance for storage node " + storageNode);
        }

        Subject overlord = subjectManager.getOverlord();

        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(storageNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(overlord);
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
                log.error("The operation [prepareForBootstrap] was canceled for " + newStorageNode +
                    ". Deployment of the new storage node cannot proceed.");
                // TODO update workflow status (the status needs to be accessible in the UI)
                return;
            case FAILURE:
                log.error("The operation [preparedForBootstrap] failed for " + newStorageNode + ". The reported " +
                    "failure is: " + resourceOperationHistory.getErrorMessage());
                log.error("Deployment of the new storage node cannot proceed.");
                // TODO update workflow status (the status needs to be accessible in the UI)
                return;
            default:  // SUCCESS
                // Nothing to do because we wait for the C* driver to notify us that the
                // storage node has joined the cluster before we proceed with the work flow.
        }
    }

    private void handleUpdateKnownNodes(ResourceOperationHistory resourceOperationHistory) {
        StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
        switch (resourceOperationHistory.getStatus()) {
            case INPROGRESS:
                // nothing to do here
                return;
            case CANCELED:
                log.error("The operation [updateKnownNodes] was canceled for " + storageNode +
                    ". Deployment of the new storage node cannot proceed.");
                // TODO update workflow status (the status needs to be accessible in the UI)
                return;
            case FAILURE:
                log.error("The operation [updateKnownNodes] failed for " + storageNode + ". The reported " +
                    "failure is: " + resourceOperationHistory.getErrorMessage());
                log.error("Deployment of the new storage node cannot proceed.");
                // TODO update workflow status (the status needs to be accessible in the UI)
                return;
            default:  // SUCCESS
                if (log.isInfoEnabled()) {
                    log.info("Finished announcing cluster nodes to " + storageNode);
                }
                storageNode.setOperationMode(StorageNode.OperationMode.ADD_NODE_MAINTENANCE);
                Configuration parameters = resourceOperationHistory.getParameters();
                PropertyList addresses = parameters.getList("addresses");
                StorageNode nextNode = takeFromQueue(storageNode, StorageNode.OperationMode.ANNOUNCE);

                if (nextNode == null) {
                    log.info("Successfully announced new storage node to cluster");
                    StorageNode installedNode = findStorageNodeToPrepareForBootstrap(addresses);
                    // Pass a copy of addresses to avoid a TransientObjectException
                    prepareNodeForBootstrap(installedNode, addresses.deepCopy(false));
                } else {
                    announceNewStorageNode(storageNode, nextNode, addresses.deepCopy(false));
                }
        }
    }

    private void handleAddNodeMaintenance(ResourceOperationHistory resourceOperationHistory) {
        StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
        switch (resourceOperationHistory.getStatus()) {
            case INPROGRESS:
                // nothing to do here
                return;
            case CANCELED:
                log.error("The operation [addNodeMaintenance] was canceled for " + storageNode + ". This operation " +
                    "needs to be run on each storage node when a new node is added to the cluster.");
                    // TODO update workflow status (the status needs to be accessible in the UI)
                return;
            case FAILURE:
                log.error("The operation [addNodeMaintenance] failed for " + storageNode + ". This operation " +
                    "needs to be run on each storage node when a new node is added to the cluster. The reported " +
                    "failure is: " + resourceOperationHistory.getErrorMessage());
                // TODO update workflow status (the status needs to be accessible in the UI)
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
                    performAddNodeMaintenance(nextNode, runRepair, seedsList);
                }
        }
    }

    private StorageNode findStorageNode(Resource resource) {
        for (StorageNode storageNode : storageNodeManager.getStorageNodes()) {
            if (storageNode.getResource().getId() == resource.getId()) {
                return storageNode;
            }
        }
        return null;
    }

    private StorageNode findStorageNodeToPrepareForBootstrap(PropertyList addressList) {
        // It is possible that we could have more that one INSTALLED node. We want to make
        // sure we grab the one that was just announced to the cluster.
        Set<String> addresses = toSet(addressList);
        List<StorageNode> installedNodes = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL_BY_MODE,
            StorageNode.class).setParameter("operationMode", StorageNode.OperationMode.INSTALLED).getResultList();

        for (StorageNode installedNode : installedNodes) {
            if (addresses.contains(installedNode.getAddress())) {
                return installedNode;
            }
        }
        // TODO What should we do in the very unlikely event that we do not find the IP address?
        throw new IllegalStateException("Failed to find storage node to be bootstrapped.");
    }

    private Set<String> toSet(PropertyList propertyList) {
        Set<String> set = new HashSet<String>();
        for (Property property : propertyList.getList()) {
            PropertySimple simple = (PropertySimple) property;
            set.add(simple.getStringValue());
        }
        return set;
    }

    private void prepareNodeForBootstrap(StorageNode storageNode, PropertyList addresses) {
        if (log.isInfoEnabled()) {
            log.info("Preparing to bootstrap " + storageNode + " into cluster...");
        }

        ResourceOperationSchedule schedule = new ResourceOperationSchedule();
        schedule.setResource(storageNode.getResource());
        schedule.setJobTrigger(JobTrigger.createNowTrigger());
        schedule.setSubject(subjectManager.getOverlord());
        schedule.setOperationName("prepareForBootstrap");

        StorageClusterSettings clusterSettings = storageClusterSettingsManager.getClusterSettings(
            subjectManager.getOverlord());
        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("cqlPort", clusterSettings.getCqlPort()));
        parameters.put(new PropertySimple("gossipPort", clusterSettings.getGossipPort()));
        parameters.put(addresses);

        schedule.setParameters(parameters);

        operationManager.scheduleResourceOperation(subjectManager.getOverlord(), schedule);
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

}
