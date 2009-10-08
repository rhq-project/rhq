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
package org.rhq.enterprise.server.discovery.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.persistence.EntityManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.jboss.mx.util.MBeanServerLocator;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsService;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class DiscoveryBossBeanTest extends AbstractEJB3Test {
    private DiscoveryBossLocal discoveryBoss;
    private MBeanServer dummyJBossMBeanServer;

    @BeforeClass
    public void beforeClass() throws Exception {
        discoveryBoss = LookupUtil.getDiscoveryBoss();

        dummyJBossMBeanServer = MBeanServerFactory.createMBeanServer("jboss");
        MBeanServerLocator.setJBoss(dummyJBossMBeanServer);
        dummyJBossMBeanServer.registerMBean(new ServerCommunicationsService(),
            ServerCommunicationsServiceMBean.OBJECT_NAME);
        return;
    }

    @AfterClass
    public void afterClass() {
        MBeanServerFactory.releaseMBeanServer(dummyJBossMBeanServer);
    }

    private ResourceType platformType;

    private ResourceType serverType;

    private ResourceType serviceType1;

    private ResourceType serviceType2;

    private Agent agent;

    @Test(groups = "integration.ejb3")
    public void testBasicInventoryReport() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            createResourceTypes(em);
            createAgent(em);
            em.flush();

            InventoryReport inventoryReport = new InventoryReport(agent);

            Resource platform = new Resource("alpha", "platform", platformType);
            Resource server = new Resource("bravo", "server", serverType);
            platform.addChildResource(server);
            Resource service1 = new Resource("charlie", "service 1", serviceType1);
            Resource service2 = new Resource("delta", "service 2", serviceType2);
            server.addChildResource(service1);
            server.addChildResource(service2);

            inventoryReport.addAddedRoot(platform);

            ResourceSyncInfo syncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
            assert syncInfo != null;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testUpdateInventoryReport() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            createResourceTypes(em);
            createAgent(em);
            em.flush();

            // First just submit the platform
            InventoryReport inventoryReport = new InventoryReport(agent);
            Resource platform = new Resource("alpha", "platform", platformType);
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

            inventoryReport.addAddedRoot(server);

            syncInfo = discoveryBoss.mergeInventoryReport(serialize(inventoryReport));
            assert syncInfo != null;
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Use this to fake like your remoting objects. Can be used to keep your own copy of objects locally transient.
     *
     * @param  object
     *
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

    private void createAgent(EntityManager em) {
        agent = new Agent("TestAgent-dbbt", "0.0.0.0", 1, "http://here", "tok");
        em.persist(agent);
    }

    private void createResourceTypes(EntityManager entityManager) {
        platformType = new ResourceType("test platform", "test", ResourceCategory.PLATFORM, null);
        serverType = new ResourceType("test server", "test", ResourceCategory.SERVER, platformType);
        serviceType1 = new ResourceType("test service 1", "test", ResourceCategory.SERVICE, serverType);
        serviceType2 = new ResourceType("test service 2", "test", ResourceCategory.SERVICE, serverType);

        entityManager.persist(platformType);
    }
}