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
package org.rhq.enterprise.server.discovery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.util.Random;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

import org.jboss.mx.util.MBeanServerLocator;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsService;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class DiscoveryBossBeanTest extends AbstractEJB3Test {
    private DiscoveryBossLocal discoveryBoss;
    private MBeanServer dummyJBossMBeanServer;

    private ResourceType platformType;

    private ResourceType serverType;

    private ResourceType serviceType1;

    private ResourceType serviceType2;

    private Agent agent;

    @BeforeClass
    public void beforeClass() throws Exception {
        discoveryBoss = LookupUtil.getDiscoveryBoss();

        dummyJBossMBeanServer = MBeanServerFactory.createMBeanServer("jboss");
        MBeanServerLocator.setJBoss(dummyJBossMBeanServer);
        dummyJBossMBeanServer.registerMBean(new ServerCommunicationsService(),
                ServerCommunicationsServiceMBean.OBJECT_NAME);
    }

    @AfterClass
    public void afterClass() {
        MBeanServerFactory.releaseMBeanServer(dummyJBossMBeanServer);
    }

    @BeforeMethod
    public void setupTestData() throws Exception {
        initDB();
        platformType = getEntityManager().find(ResourceType.class, 1);
        serverType = getEntityManager().find(ResourceType.class, 2);
        serviceType1 = getEntityManager().find(ResourceType.class, 3);
        serviceType2 = getEntityManager().find(ResourceType.class, 4);
        agent = getEntityManager().find(Agent.class, 1);
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

        platform.setUuid("" + new Random().nextInt());
        server.setUuid("" + new Random().nextInt());
        service1.setUuid("" + new Random().nextInt());
        service2.setUuid("" + new Random().nextInt());

        inventoryReport.addAddedRoot(platform);

        ResourceSyncInfo syncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert syncInfo != null;
    }

    @Test(groups = "integration.ejb3")
    public void testUpdateInventoryReport() throws Exception {
        // First just submit the platform
        InventoryReport inventoryReport = new InventoryReport(agent);
        Resource platform = new Resource("alpha", "platform", platformType);
        platform.setUuid("" + new Random().nextInt());
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

        server.setUuid("" + new Random().nextInt());
        service1.setUuid("" + new Random().nextInt());
        service2.setUuid("" + new Random().nextInt());

        inventoryReport.addAddedRoot(server);

        syncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
        assert syncInfo != null;
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
            DatabaseOperation.CLEAN_INSERT.execute(dbUnitConnection, getDataSet());
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