/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.enterprise.server.cloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Query;
import javax.transaction.Transaction;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jirka Kremser
 */
@Test
public class StorageNodeManagerBeanTest extends AbstractEJB3Test {

    private StorageNodeManagerLocal nodeManager;
    private ResourceTypeManagerLocal typeManager;
    private Subject overlord;
    private static final String TEST_PREFIX = "test-";

    @Override
    protected void beforeMethod() throws Exception {
        nodeManager = LookupUtil.getStorageNodeManager();
        typeManager = LookupUtil.getResourceTypeManager();
        overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    @Test
    public void testInit() throws Exception {
        final String cassandraSeedsProperty = "rhq.cassandra.seeds";
        final String originalSeedValue = System.getProperty(cassandraSeedsProperty);

        try {
            prepareScheduler();
            executeInTransaction(new TransactionCallback() {

                @Override
                public void execute() throws Exception {
                    String testHostName = TEST_PREFIX + "hostname";
                    List<String> addresses = Arrays.asList(testHostName, TEST_PREFIX + "hostWithNoFoundResource",
                        TEST_PREFIX + "secondHostWithNoFoundResource");
                    System.setProperty(cassandraSeedsProperty, addresses.get(0) + "|123|123," + addresses.get(1)
                        + "|987|987," + addresses.get(2) + "|123|123");

                    cleanDatabase();

                    // create the resource type if it doesn't exist
                    ResourceType testResourceType = typeManager.getResourceTypeByNameAndPlugin("RHQ Storage Node",
                        "RHQStorage");
                    if (testResourceType == null) {
                        testResourceType = createResourceType();
                    }
                    Resource testResource = createResource(testResourceType, testHostName);

                    // finds the storage nodes and pairs them w/ the associated resources
                    nodeManager.scanForStorageNodes();

                    // get the storage nodes and checks some properties on them
                    List<StorageNode> storageNodes = nodeManager.getStorageNodes();
                    Assert.assertNotNull(storageNodes, "The list of storage nodes shouldn't be null.");
                    Assert.assertFalse(storageNodes.isEmpty(), "The list of storage nodes shouldn't be empty.");
                    Assert.assertTrue(storageNodes.size() >= addresses.size(),
                        "The size of the list of storage nodes should be at least " + addresses.size());

                    List<String> obtainedAddresses = new ArrayList<String>(storageNodes.size());
                    for (StorageNode storageNode : storageNodes) {
                        Assert.assertNotNull(storageNode.getAddress(), "Address of storage node cannot be null.");
                        obtainedAddresses.add(storageNode.getAddress());
                        if (storageNode.getAddress().equals(testHostName)) {
                            Assert.assertEquals(storageNode.getResource().getId(), testResource.getId());
                            Assert.assertNotNull(storageNode.getResource(), "Associated resource cannot be null.");
                        } else {
                            Assert.assertNull(storageNode.getResource(),
                                "The resource field should be null at this point.");
                        }
                    }

                    Assert.assertTrue(obtainedAddresses.containsAll(addresses),
                        "There are some storage nodes that should be created but were not discovered and returned."
                            + " The storage nodes that should be returned: " + addresses
                            + "  The storage nodes that were returned: " + obtainedAddresses
                            + "  (the second should be a super-set (not necessarily strict) of the first.)");

                }
            });
        } finally {
            unprepareScheduler();
            cleanDatabase();
            System.setProperty(cassandraSeedsProperty, originalSeedValue);
        }
    }

    @Test(groups = "integration.ejb3")
    public void testStorageNodeCriteriaFinder() throws Exception {

        final int storageNodeCount = 42;
        executeInTransaction(new TransactionCallback() {

            public void execute() throws Exception {
                // verify that all storage nodes objects are actually parsed.
                final Set<String> nodeAddresses = new HashSet<String>(storageNodeCount);

                final String prefix = "storage_node";
                StorageNode lastOne = null, firstOne = null;
                for (int i = 0; i < storageNodeCount; i++) {
                    String address = prefix + String.format(" %03d", i + 1) + ".domain.com";
                    StorageNode node = new StorageNode();
                    node.setAddress(address);
                    node.setOperationMode(StorageNode.OperationMode.NORMAL);
                    node.setJmxPort(7299 + i);
                    node.setCqlPort(9142 + i);
                    if (i == 0) {
                        firstOne = node;
                    } else if (i == storageNodeCount - 1) {
                        lastOne = node;
                    }

                    em.persist(node);
                    nodeAddresses.add(address);
                    em.flush();
                }
                em.flush();

                assertTrue("The number of created storage nodes should be " + storageNodeCount + ". Was: "
                    + nodeAddresses.size(), storageNodeCount == nodeAddresses.size());

                StorageNodeCriteria criteria = new StorageNodeCriteria();
                criteria.addFilterAddress(prefix);
                // use DESC just to make sure sorting on name is different than insert order
                criteria.addSortAddress(PageOrdering.DESC);
                PageList<StorageNode> list = nodeManager.findStorageNodesByCriteria(overlord, criteria);

                assertTrue("The number of found storage nodes should be " + storageNodeCount + ". Was: " + list.size(),
                    storageNodeCount == list.size());

                assertTrue("The first storage node [" + firstOne + "] should be same as the last one in the list [ "
                    + list.get(list.size() - 1) + "]", firstOne.equals(list.get(list.size() - 1)));

                assertTrue("The last storage node [" + lastOne + "] should be same as the first one in the list [ "
                    + list.get(0) + "]", lastOne.equals(list.get(0)));

                String prevAddress = null;
                for (StorageNode s : list) {
                    assert null == prevAddress || s.getAddress().compareTo(prevAddress) < 0 : "Results should be"
                        + "sorted by address DESC, something is out of order";
                    prevAddress = s.getAddress();
                    nodeAddresses.remove(s.getAddress());
                }

                // test that entire list was parsed
                assertTrue("Expected resourceNames to be empty. Still " + nodeAddresses.size() + " name(s).",
                    nodeAddresses.size() == 0);
            }
        });
    }

    private void cleanDatabase() throws Exception {
        // this method is still needed, because tests calls SLSB methods that are executed in their own transaction
        // and the  rollback performed once the TransactionCallback is finished just wont clean everything

        // pause the currently running TX
        Transaction runningTransaction = getTransactionManager().suspend();
        getTransactionManager().begin();

        Query query = getEntityManager().createQuery("DELETE FROM StorageNode s WHERE s.address LIKE (:prefix || '%')")
            .setParameter("prefix", TEST_PREFIX);

        query.executeUpdate();

        // perhaps this could be restricted by resouce ids as well not to delete all the table with existing data
        query = getEntityManager().createQuery("DELETE FROM Availability");
        query.executeUpdate();

        query = getEntityManager().createQuery("DELETE FROM Resource r WHERE r.resourceKey LIKE (:prefix || '%')")
            .setParameter("prefix", TEST_PREFIX);
        query.executeUpdate();

        query = getEntityManager().createQuery("DELETE FROM ResourceType rt WHERE rt.name = :name").setParameter(
            "name", "Cassandra Daemon");
        query.executeUpdate();
        getTransactionManager().commit();

        // resume the currently running TX
        getTransactionManager().resume(runningTransaction);
    }

    private ResourceType createResourceType() throws Exception {
        ResourceType resourceType = new ResourceType("RHQ Storage Node", "RHQStorage", ResourceCategory.SERVER, null);
        ConfigurationDefinition pluginConfigurationDefinition = new ConfigurationDefinition("config", null);
        pluginConfigurationDefinition.put(new PropertyDefinitionSimple("host", null, true, PropertySimpleType.STRING));
        resourceType.setPluginConfigurationDefinition(pluginConfigurationDefinition);
        getEntityManager().persist(resourceType);

        return resourceType;
    }

    private Resource createResource(ResourceType resourceType, String host) throws Exception {
        Resource resource = new Resource(TEST_PREFIX + "CassandraDaemon", TEST_PREFIX + "CassandraDaemon", resourceType);
        resource.setUuid(UUID.randomUUID().toString());
        resource.getPluginConfiguration().setSimpleValue("host", host);
        getEntityManager().persist(resource);
        getEntityManager().flush();

        return resource;
    }
}
