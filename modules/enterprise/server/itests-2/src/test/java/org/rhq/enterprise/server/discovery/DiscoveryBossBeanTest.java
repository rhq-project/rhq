/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.server.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.ejb.EJBException;
import javax.persistence.Query;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.discovery.MergeInventoryReportResults;
import org.rhq.core.domain.discovery.MergeInventoryReportResults.ResourceTypeFlyweight;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;

/**
 * A unit test for {@link DiscoveryBossBean}.
 */
public class DiscoveryBossBeanTest extends AbstractEJB3Test {

    private DiscoveryBossLocal discoveryBoss;

    private SubjectManagerLocal subjectManager;

    private ResourceManagerLocal resourceManager;

    private ResourceTypeManagerLocal resourceTypeManager;

    private ResourceType platformType;

    private ResourceType serverType;

    private ResourceType serviceType1;

    private ResourceType serviceType2;

    private ResourceType storagePlatformType;

    private ResourceType storageServerType;

    private Agent agent;

    private TestServerCommunicationsService agentServiceContainer;

    @Override
    protected void beforeMethod() throws Exception {
        discoveryBoss = LookupUtil.getDiscoveryBoss();
        subjectManager = LookupUtil.getSubjectManager();
        resourceManager = LookupUtil.getResourceManager();
        resourceTypeManager = LookupUtil.getResourceTypeManager();

        initDB();

        platformType = getEntityManager().find(ResourceType.class, 15641);
        serverType = getEntityManager().find(ResourceType.class, 15642);
        serviceType1 = getEntityManager().find(ResourceType.class, 15643);
        serviceType2 = getEntityManager().find(ResourceType.class, 15644);
        agent = getEntityManager().find(Agent.class, 15641);

        storagePlatformType = getEntityManager().find(ResourceType.class, 15651);
        storageServerType = getEntityManager().find(ResourceType.class, 15652);

        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.discoveryService = Mockito.mock(DiscoveryAgentService.class);
        when(
            agentServiceContainer.discoveryService.manuallyAddResource(any(ResourceType.class), anyInt(),
                any(Configuration.class), anyInt())).thenAnswer(new Answer<MergeResourceResponse>() {
            public MergeResourceResponse answer(InvocationOnMock invocation) throws Throwable {
                Resource resource = new Resource(1000000);
                resource.setUuid(UUID.randomUUID().toString());
                ResourceType resourceType = (ResourceType) invocation.getArguments()[0];
                resource.setResourceType(resourceType);
                long randomLong = UUID.randomUUID().getLeastSignificantBits();
                resource.setResourceKey(prefix("key-" + randomLong));
                resource.setName(prefix("name-" + randomLong));
                int parentResourceId = (Integer) invocation.getArguments()[1];
                Resource parentResource = resourceManager.getResource(subjectManager.getOverlord(), parentResourceId);
                resource.setParentResource(parentResource);
                Integer ownerSubjectId = (Integer) invocation.getArguments()[3];
                MergeResourceResponse response = discoveryBoss.addResource(resource, ownerSubjectId);
                return response;
            }
        });

        prepareScheduler();
    }

    @Override
    protected void afterMethod() throws Exception {
        try {
            cleanDB();
        } finally {
            unprepareForTestAgents();
            unprepareScheduler();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testBasicInventoryReport() throws Exception {
        InventoryReport inventoryReport = new InventoryReport(agent);

        Resource platform = new Resource(prefix("alpha"), prefix("platform"), platformType);
        Resource server = new Resource(prefix("bravo"), prefix("server"), serverType);
        platform.addChildResource(server);
        Resource service1 = new Resource(prefix("charlie"), prefix("service 1"), serviceType1);
        Resource service2 = new Resource(prefix("delta"), prefix("service 2"), serviceType2);
        server.addChildResource(service1);
        server.addChildResource(service2);

        platform.setUuid(String.valueOf(new Random().nextInt()));
        server.setUuid(String.valueOf(new Random().nextInt()));
        service1.setUuid(String.valueOf(new Random().nextInt()));
        service2.setUuid(String.valueOf(new Random().nextInt()));

        inventoryReport.addAddedRoot(platform);

        MergeInventoryReportResults results = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert results != null;
        assert results.getIgnoredResourceTypes() == null : "nothing should have been ignored in this test";
        ResourceSyncInfo syncInfo = results.getResourceSyncInfo();
        assert syncInfo != null;
    }

    @Test(groups = "integration.ejb3")
    public void testUpdateInventoryReport() throws Exception {
        // First just submit the platform
        InventoryReport inventoryReport = new InventoryReport(agent);
        Resource platform = new Resource(prefix("alpha"), prefix("platform"), platformType);
        platform.setUuid(String.valueOf(new Random().nextInt()));
        inventoryReport.addAddedRoot(platform);
        MergeInventoryReportResults results = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert results != null;
        assert results.getIgnoredResourceTypes() == null : "nothing should have been ignored in this test";
        ResourceSyncInfo syncInfo = results.getResourceSyncInfo();
        assert syncInfo != null;

        platform.setId(syncInfo.getId());

        // Now submit the server and its children as an update report
        inventoryReport = new InventoryReport(agent);
        Resource server = new Resource(prefix("bravo"), prefix("server"), serverType);
        platform.addChildResource(server);
        Resource service1 = new Resource(prefix("charlie"), prefix("service 1"), serviceType1);
        Resource service2 = new Resource(prefix("delta"), prefix("service 2"), serviceType2);
        server.addChildResource(service1);
        server.addChildResource(service2);

        server.setUuid(String.valueOf(new Random().nextInt()));
        service1.setUuid(String.valueOf(new Random().nextInt()));
        service2.setUuid(String.valueOf(new Random().nextInt()));

        inventoryReport.addAddedRoot(server);

        results = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert results != null;
        assert results.getIgnoredResourceTypes() == null : "nothing should have been ignored in this test";
        syncInfo = results.getResourceSyncInfo();
        assert syncInfo != null;
    }

    @Test(groups = "integration.ejb3")
    public void testManuallyAddResource() throws Exception {
        InventoryReport inventoryReport = new InventoryReport(agent);

        Resource platform = new Resource(prefix("alpha"), prefix("platform"), platformType);
        Resource server = new Resource(prefix("bravo"), prefix("server"), serverType);
        platform.addChildResource(server);
        Resource service2 = new Resource(prefix("delta"), prefix("service 2"), serviceType2);
        server.addChildResource(service2);

        platform.setUuid(String.valueOf(new Random().nextInt()));
        server.setUuid(String.valueOf(new Random().nextInt()));
        service2.setUuid(String.valueOf(new Random().nextInt()));

        inventoryReport.addAddedRoot(platform);

        MergeInventoryReportResults results = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert results != null;
        assert results.getIgnoredResourceTypes() == null : "nothing should have been ignored in this test";
        ResourceSyncInfo syncInfo = results.getResourceSyncInfo();
        assert syncInfo != null;

        ResourceSyncInfo serverSyncInfo = syncInfo.getChildSyncInfos().iterator().next();
        Resource resource1 = discoveryBoss.manuallyAddResource(subjectManager.getOverlord(), serviceType2.getId(),
            serverSyncInfo.getId(), new Configuration());

        try {
            Resource resource2 = discoveryBoss.manuallyAddResource(subjectManager.getOverlord(), serviceType2.getId(),
                serverSyncInfo.getId(), new Configuration());
            fail("Manually adding a singleton that already existed succeeded: " + resource2);
        } catch (EJBException e) {
            assertEquals(String.valueOf(e.getCause()), RuntimeException.class, e.getCause().getClass());
            assertTrue(String.valueOf(e.getCause()), e.getCause().getMessage().contains("singleton"));
        }
    }

    @Test(groups = "integration.ejb3")
    public void testIgnoreUnignoreAndImportResources() throws Exception {

        // First create an inventory report for a new platform with servers

        InventoryReport inventoryReport = new InventoryReport(agent);

        Resource platform = new Resource(prefix("platform"), prefix("platform"), platformType);
        platform.setUuid(String.valueOf(new Random().nextInt()));
        for (int i = 0; i < 17; i++) {
            String serverString = prefix("server " + String.valueOf(i));
            Resource server = new Resource(serverString, serverString, serverType);
            server.setUuid(String.valueOf(new Random().nextInt()));
            platform.addChildResource(server);
        }

        inventoryReport.addAddedRoot(platform);

        // Merge this inventory report
        MergeInventoryReportResults mergeResults = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert mergeResults != null;
        assert mergeResults.getIgnoredResourceTypes() == null : "nothing should have been ignored";
        ResourceSyncInfo platformSyncInfo = mergeResults.getResourceSyncInfo();
        assert platformSyncInfo != null;

        // Check merge result
        assertEquals(InventoryStatus.NEW, platformSyncInfo.getInventoryStatus());
        assertEquals(platform.getChildResources().size(), platformSyncInfo.getChildSyncInfos().size());

        // Collect the resource ids generated for the platform and the servers

        int platformId = platformSyncInfo.getId();
        List<Integer> serverIds = new LinkedList<Integer>();
        for (ResourceSyncInfo serverSyncInfo : platformSyncInfo.getChildSyncInfos()) {
            serverIds.add(serverSyncInfo.getId());
        }
        int[] arrayOfServerIds = ArrayUtils.unwrapCollection(serverIds);

        // Now test ignore, unignore and import behavior
        discoveryBoss.importResources(subjectManager.getOverlord(), new int[] { platformId });
        discoveryBoss.ignoreResources(subjectManager.getOverlord(), arrayOfServerIds);

        try {
            discoveryBoss.importResources(subjectManager.getOverlord(), arrayOfServerIds);
            fail("Import resources should fail for ignored resources");
        } catch (EJBException e) {
            assertEquals(String.valueOf(e.getCause()), IllegalArgumentException.class, e.getCause().getClass());
            assertTrue(String.valueOf(e.getCause()),
                e.getCause().getMessage().startsWith("Can only set inventory status to"));
        }

        discoveryBoss.unignoreAndImportResources(subjectManager.getOverlord(), arrayOfServerIds);

        // excursus: take this time to do a side test of the resource criteria filtering on parent inv status
        List<InventoryStatus> committedStatus = new ArrayList<InventoryStatus>(1);
        List<InventoryStatus> ignoredStatus = new ArrayList<InventoryStatus>(1);
        committedStatus.add(InventoryStatus.COMMITTED);
        ignoredStatus.add(InventoryStatus.IGNORED);
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterParentInventoryStatuses(committedStatus);
        criteria.addFilterId(serverIds.get(0));

        // excursus: look for the server with the given ID but only if the parent is committed (this should return the resource)
        List<Resource> lookup = resourceManager.findResourcesByCriteria(subjectManager.getOverlord(), criteria);
        assert 1 == lookup.size() : lookup;
        assert lookup.get(0).getId() == serverIds.get(0) : lookup;

        // excursus: look for the server with the given ID but only if the parent is ignored (this should return nothing)
        criteria.addFilterParentInventoryStatuses(ignoredStatus);
        lookup = resourceManager.findResourcesByCriteria(subjectManager.getOverlord(), criteria);
        assert lookup.isEmpty() : lookup;
    }

    @Test(groups = "integration.ejb3")
    public void testIgnoreResourceType() throws Exception {

        // ignore the server type immediately
        resourceTypeManager.setResourceTypeIgnoreFlagAndUninventoryResources(subjectManager.getOverlord(),
            serverType.getId(), true);

        // create an inventory report with a platform and a server - the server will be of the ignored type
        InventoryReport inventoryReport = new InventoryReport(agent);
        Resource platform = new Resource(prefix("platform"), prefix("platform"), platformType);
        platform.setUuid(String.valueOf(new Random().nextInt()));
        Resource server = new Resource(prefix("server0"), prefix("server0"), serverType);
        server.setUuid(String.valueOf(new Random().nextInt()));
        platform.addChildResource(server);
        inventoryReport.addAddedRoot(platform);

        // Merge this inventory report
        MergeInventoryReportResults mergeResults = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert mergeResults != null;
        ResourceSyncInfo platformSyncInfo = mergeResults.getResourceSyncInfo();
        assert platformSyncInfo != null;
        assertNotNull(mergeResults.getIgnoredResourceTypes());
        assertEquals(mergeResults.getIgnoredResourceTypes().size(), 1);
        assert mergeResults.getIgnoredResourceTypes().contains(new ResourceTypeFlyweight(serverType));

        // Check merge result - make sure we should not see any children under the platform (it should have been ignored)
        assertEquals(InventoryStatus.NEW, platformSyncInfo.getInventoryStatus());
        assertEquals(platformSyncInfo.getChildSyncInfos().size(), 0);
    }

    @Test(groups = "integration.ejb3")
    public void testReturnAllIgnoredResourceTypes() throws Exception {

        // ignore the service types immediately
        resourceTypeManager.setResourceTypeIgnoreFlagAndUninventoryResources(subjectManager.getOverlord(),
            serviceType1.getId(), true);
        resourceTypeManager.setResourceTypeIgnoreFlagAndUninventoryResources(subjectManager.getOverlord(),
            serviceType2.getId(), true);

        // create an inventory report with just a platform and a server
        InventoryReport inventoryReport = new InventoryReport(agent);
        Resource platform = new Resource(prefix("platform"), prefix("platform"), platformType);
        platform.setUuid(String.valueOf(new Random().nextInt()));
        Resource server = new Resource(prefix("server0"), prefix("server0"), serverType);
        server.setUuid(String.valueOf(new Random().nextInt()));
        platform.addChildResource(server);
        inventoryReport.addAddedRoot(platform);

        // Merge this inventory report
        MergeInventoryReportResults mergeResults = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert mergeResults != null;
        ResourceSyncInfo platformSyncInfo = mergeResults.getResourceSyncInfo();
        assert platformSyncInfo != null;

        // now see that we were told about the service types being ignored (even though we had no resources of that type in the report)
        Collection<ResourceTypeFlyweight> ignoredResourceTypes = mergeResults.getIgnoredResourceTypes();
        assertNotNull(ignoredResourceTypes);
        assertEquals(ignoredResourceTypes.size(), 2);
        assert ignoredResourceTypes.contains(new ResourceTypeFlyweight(serviceType1));
        assert ignoredResourceTypes.contains(new ResourceTypeFlyweight(serviceType2));
    }

    @Test(groups = "integration.ejb3")
    public void testIgnoreResourceTypeAndUninventoryResources() throws Exception {

        // First create an inventory report for a new platform with servers - nothing is ignored yet
        InventoryReport inventoryReport = new InventoryReport(agent);
        Resource platform = new Resource(prefix("platform"), prefix("platform"), platformType);
        platform.setUuid(String.valueOf(new Random().nextInt()));
        for (int i = 0; i < 17; i++) {
            String serverString = prefix("server " + String.valueOf(i));
            Resource server = new Resource(serverString, serverString, serverType);
            server.setUuid(String.valueOf(new Random().nextInt()));
            platform.addChildResource(server);
        }
        inventoryReport.addAddedRoot(platform);

        // Merge this inventory report to get platform and servers in NEW state
        MergeInventoryReportResults mergeResults = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert mergeResults != null;
        assert mergeResults.getIgnoredResourceTypes() == null : "nothing should have been ignored";
        ResourceSyncInfo platformSyncInfo = mergeResults.getResourceSyncInfo();
        assert platformSyncInfo != null;

        // Collect the resource ids generated for the platform and the servers
        int platformId = platformSyncInfo.getId();
        List<Integer> serverIds = new LinkedList<Integer>();
        for (ResourceSyncInfo serverSyncInfo : platformSyncInfo.getChildSyncInfos()) {
            serverIds.add(serverSyncInfo.getId());
        }
        int[] arrayOfServerIds = ArrayUtils.unwrapCollection(serverIds);

        // Now import platform and servers into inventory
        discoveryBoss.importResources(subjectManager.getOverlord(), new int[] { platformId });
        discoveryBoss.importResources(subjectManager.getOverlord(), arrayOfServerIds);

        // make sure servers are committed into inventory now
        List<Integer> serverTypeIdInList = new ArrayList<Integer>(1);
        serverTypeIdInList.add(serverType.getId());
        PageList<Resource> allServers = resourceManager.findResourceByIds(subjectManager.getOverlord(),
            arrayOfServerIds, false, PageControl.getUnlimitedInstance());
        for (Resource aServer : allServers) {
            assert aServer.getInventoryStatus() == InventoryStatus.COMMITTED : "should be committed: " + aServer;
        }
        assert allServers.getTotalSize() == arrayOfServerIds.length : "all servers were not committed into inventory";

        // now ignore the server type - this should uninventory all servers
        resourceTypeManager.setResourceTypeIgnoreFlagAndUninventoryResources(subjectManager.getOverlord(),
            serverType.getId(), true);

        // make sure all servers were uninventoried
        allServers = resourceManager.findResourceByIds(subjectManager.getOverlord(), arrayOfServerIds, false,
            PageControl.getUnlimitedInstance());
        for (Resource aServer : allServers) {
            assert aServer.getInventoryStatus() != InventoryStatus.COMMITTED : "should not be committed: " + aServer;
        }

        // Merge the inventory report again to simulate another discovery - the servers should be ignored now
        mergeResults = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert mergeResults != null;
        platformSyncInfo = mergeResults.getResourceSyncInfo();
        assert platformSyncInfo != null;
        assertNotNull(mergeResults.getIgnoredResourceTypes());
        assertEquals(mergeResults.getIgnoredResourceTypes().size(), 1);
        assert mergeResults.getIgnoredResourceTypes().contains(new ResourceTypeFlyweight(serverType));

        assertEquals(InventoryStatus.COMMITTED, platformSyncInfo.getInventoryStatus()); // notice platform is committed now
        assertEquals(platformSyncInfo.getChildSyncInfos().size(), 0); // notice there are no server children now
    }

    @Test(groups = "integration.ejb3")
    public void testAutoImportStorageNode() throws Exception {

        // create an inventory report for a storage node
        InventoryReport inventoryReport = new InventoryReport(agent);

        Resource storagePlatform = new Resource(prefix("storagePlatform"), prefix("storagePlatform"),
            storagePlatformType);
        storagePlatform.setUuid(String.valueOf(new Random().nextInt()));

        Resource storageNode = new Resource(prefix("storageNode"), prefix("storageNode"), storageServerType);
        storageNode.setUuid(String.valueOf(new Random().nextInt()));
        storagePlatform.addChildResource(storageNode);

        storageNode.setPluginConfiguration(Configuration.builder()
            .addSimple("nativeTransportPort", 9142)
            .addSimple("storagePort", 7100)
            .addSimple("host", "localhost")
            .build());

        inventoryReport.addAddedRoot(storagePlatform);

        // Merge this inventory report
        MergeInventoryReportResults mergeResults = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert mergeResults != null;
        assert mergeResults.getIgnoredResourceTypes() == null : "nothing should have been ignored";
        ResourceSyncInfo platformSyncInfo = mergeResults.getResourceSyncInfo();
        assert platformSyncInfo != null;

        // Check merge result
        assertEquals(InventoryStatus.COMMITTED, platformSyncInfo.getInventoryStatus());
        assertEquals(storagePlatform.getChildResources().size(), platformSyncInfo.getChildSyncInfos().size());

        storageNode = resourceManager.getResourceById(subjectManager.getOverlord(), platformSyncInfo
            .getChildSyncInfos().iterator().next().getId());
        assertNotNull(storageNode);
        assertEquals(InventoryStatus.COMMITTED, storageNode.getInventoryStatus());
    }

    /**
     * Use this to fake like your remoting objects. Can be used to keep your own copy of objects locally transient.
     *
     * @param object
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> T serialize(T object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            System.out.println("****** Size of serialized object: " + baos.size());

            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            Object transfered = ois.readObject();

            return (T) transfered;
        } catch (Exception e) {
            throw new RuntimeException("Failed serializing and deserializing object: " + object);
        }
    }

    public void initDB() throws Exception {
        Connection connection = null;

        try {
            connection = getConnection();
            IDatabaseConnection dbUnitConnection = new DatabaseConnection(connection);
            setDbType(dbUnitConnection);
            DatabaseOperation.REFRESH.execute(dbUnitConnection, getDataSet());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void cleanDB() throws Exception {
        Connection connection = null;

        try {
            getTransactionManager().begin();

            Query q;
            List<?> doomed;
            q = em.createQuery("" //
                + "SELECT r FROM Resource r" //
                + "    WHERE r.resourceType.name LIKE '" + getPrefix() + "%'" //
                + "       OR r.resourceType.name = 'RHQ Storage Node'" //
                + " ORDER BY r.id DESC");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                Resource res = em.getReference(Resource.class, ((Resource) removeMe).getId());
                StorageNode storageNode = findStorageNode(res);
                if (storageNode != null) {
                    storageNode.setResource(null);
                }
                System.out.println("Deleting resource " + res);
                ResourceTreeHelper.deleteResource(em, res);
            }
            em.flush();
            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }
        }

        try {
            connection = getConnection();

            IDatabaseConnection dbUnitConnection = new DatabaseConnection(connection);
            setDbType(dbUnitConnection);
            DatabaseOperation.DELETE.execute(dbUnitConnection, getDataSet());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private StorageNode findStorageNode(Resource resource) {
        List<StorageNode> storageNodes = em.createQuery("SELECT s FROM StorageNode s where s.resource = :resource",
            StorageNode.class).setParameter("resource", resource).getResultList();
        if (storageNodes.isEmpty()) {
            return null;
        }
        return storageNodes.get(0);
    }

    void setDbType(IDatabaseConnection connection) throws Exception {
        DatabaseConfig dbConfig = connection.getConfig();
        String name = connection.getConnection().getMetaData().getDatabaseProductName().toLowerCase();
        int major = connection.getConnection().getMetaData().getDatabaseMajorVersion();
        IDataTypeFactory type = null;

        if (name.contains("postgres")) {
            type = new PostgresqlDataTypeFactory();
        } else if (name.contains("oracle")) {
            if (major >= 10) {
                type = new Oracle10DataTypeFactory();
            } else {
                type = new OracleDataTypeFactory();
            }
        }

        if (type != null) {
            dbConfig.setProperty("http://www.dbunit.org/properties/datatypeFactory", type);
        }
    }

    IDataSet getDataSet() throws Exception {
        FlatXmlProducer xmlProducer = new FlatXmlProducer(new InputSource(getClass().getResourceAsStream(
            getDataSetFile())));
        xmlProducer.setColumnSensing(true);
        return new FlatXmlDataSet(xmlProducer);
    }

    String getDataSetFile() {
        return getPrefix() + ".xml";
    }

    String prefix(String suffix) {
        return getPrefix() + "-" + suffix;
    }

    String getPrefix() {
        return getClass().getSimpleName();
    }
}
