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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.ejb.EJBException;
import javax.management.MBeanServer;
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
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
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

    private MBeanServer dummyJBossMBeanServer;

    private ResourceType platformType;

    private ResourceType serverType;

    private ResourceType serviceType1;

    private ResourceType serviceType2;

    private Agent agent;

    private TestServerCommunicationsService agentServiceContainer;

    @Override
    protected void beforeMethod() throws Exception {
        discoveryBoss = LookupUtil.getDiscoveryBoss();
        subjectManager = LookupUtil.getSubjectManager();
        resourceManager = LookupUtil.getResourceManager();

        initDB();

        platformType = getEntityManager().find(ResourceType.class, 1);
        serverType = getEntityManager().find(ResourceType.class, 2);
        serviceType1 = getEntityManager().find(ResourceType.class, 3);
        serviceType2 = getEntityManager().find(ResourceType.class, 4);
        agent = getEntityManager().find(Agent.class, 1);

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
                resource.setResourceKey(String.valueOf("key-" + randomLong));
                resource.setName("name-" + randomLong);
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

        Resource platform = new Resource("alpha", "platform", platformType);
        Resource server = new Resource("bravo", "server", serverType);
        platform.addChildResource(server);
        Resource service1 = new Resource("charlie", "service 1", serviceType1);
        Resource service2 = new Resource("delta", "service 2", serviceType2);
        server.addChildResource(service1);
        server.addChildResource(service2);

        platform.setUuid(String.valueOf(new Random().nextInt()));
        server.setUuid(String.valueOf(new Random().nextInt()));
        service1.setUuid(String.valueOf(new Random().nextInt()));
        service2.setUuid(String.valueOf(new Random().nextInt()));

        inventoryReport.addAddedRoot(platform);

        ResourceSyncInfo syncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert syncInfo != null;
    }

    @Test(groups = "integration.ejb3")
    public void testUpdateInventoryReport() throws Exception {
        // First just submit the platform
        InventoryReport inventoryReport = new InventoryReport(agent);
        Resource platform = new Resource("alpha", "platform", platformType);
        platform.setUuid(String.valueOf(new Random().nextInt()));
        inventoryReport.addAddedRoot(platform);
        ResourceSyncInfo syncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert syncInfo != null;

        platform.setId(syncInfo.getId());

        // Now submit the server and its children as an update report
        inventoryReport = new InventoryReport(agent);
        Resource server = new Resource("bravo", "server", serverType);
        platform.addChildResource(server);
        Resource service1 = new Resource("charlie", "service 1", serviceType1);
        Resource service2 = new Resource("delta", "service 2", serviceType2);
        server.addChildResource(service1);
        server.addChildResource(service2);

        server.setUuid(String.valueOf(new Random().nextInt()));
        service1.setUuid(String.valueOf(new Random().nextInt()));
        service2.setUuid(String.valueOf(new Random().nextInt()));

        inventoryReport.addAddedRoot(server);

        syncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert syncInfo != null;
    }

    @Test(groups = "integration.ejb3")
    public void testManuallyAddResource() throws Exception {
        InventoryReport inventoryReport = new InventoryReport(agent);

        Resource platform = new Resource("alpha", "platform", platformType);
        Resource server = new Resource("bravo", "server", serverType);
        platform.addChildResource(server);
        Resource service2 = new Resource("delta", "service 2", serviceType2);
        server.addChildResource(service2);

        platform.setUuid(String.valueOf(new Random().nextInt()));
        server.setUuid(String.valueOf(new Random().nextInt()));
        service2.setUuid(String.valueOf(new Random().nextInt()));

        inventoryReport.addAddedRoot(platform);

        ResourceSyncInfo syncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
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

        Resource platform = new Resource("platform", "platform", platformType);
        platform.setUuid(String.valueOf(new Random().nextInt()));
        for (int i = 0; i < 17; i++) {
            String serverString = "server " + String.valueOf(i);
            Resource server = new Resource(serverString, serverString, serverType);
            server.setUuid(String.valueOf(new Random().nextInt()));
            platform.addChildResource(server);
        }

        inventoryReport.addAddedRoot(platform);

        // Merge this inventory report
        ResourceSyncInfo platformSyncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));

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

        try {
            discoveryBoss.ignoreResources(subjectManager.getOverlord(), arrayOfServerIds);
            fail("Ignore resources should fail as platform resource has not yet been comitted");
        } catch (EJBException e) {
            assertEquals(String.valueOf(e.getCause()), IllegalStateException.class, e.getCause().getClass());
            assertTrue(String.valueOf(e.getCause()), e.getCause().getMessage().contains("has not yet been committed"));
        }
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
        discoveryBoss.unignoreResources(subjectManager.getOverlord(), arrayOfServerIds);
        discoveryBoss.importResources(subjectManager.getOverlord(), arrayOfServerIds);
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
            q = em.createQuery("SELECT r FROM Resource r WHERE r.resourceType.id <= 4 ORDER BY r.id DESC");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                Resource res = em.getReference(Resource.class, ((Resource) removeMe).getId());
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
        return getClass().getSimpleName() + ".xml";
    }

}
