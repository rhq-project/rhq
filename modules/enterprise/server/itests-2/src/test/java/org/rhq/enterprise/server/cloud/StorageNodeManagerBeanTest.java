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

package org.rhq.enterprise.server.cloud;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jirka Kremser
 */
@Test
public class StorageNodeManagerBeanTest extends AbstractEJB3Test {

    private StorageNodeManagerLocal nodeManager;
    private Subject overlord;

    @Override
    protected void beforeMethod() throws Exception {
        nodeManager = LookupUtil.getStorageNodeManager();
        overlord = LookupUtil.getSubjectManager().getOverlord();
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
                criteria.addSortAddress(PageOrdering.DESC); // use DESC just to make sure sorting on name is different than insert order
                PageList<StorageNode> list = nodeManager.findStorageNodesByCriteria(overlord, criteria);

                assertTrue("The number of found storage nodes should be " + storageNodeCount + ". Was: " + list.size(),
                    storageNodeCount == list.size());

                assertTrue("The first storage node [" + firstOne + "] should be same as the last one in the list [ "
                    + list.get(list.size() - 1) + "]", firstOne.equals(list.get(list.size() - 1)));

                assertTrue("The last storage node [" + lastOne + "] should be same as the first one in the list [ "
                    + list.get(0) + "]", lastOne.equals(list.get(0)));

                String prevAddress = null;
                for (StorageNode s : list) {
                    assert null == prevAddress || s.getAddress().compareTo(prevAddress) < 0 : "Results should be sorted by address DESC, something is out of order";
                    prevAddress = s.getAddress();
                    nodeAddresses.remove(s.getAddress());
                }

                // test that entire list was parsed
                assertTrue("Expected resourceNames to be empty. Still " + nodeAddresses.size() + " name(s).",
                    nodeAddresses.size() == 0);
            }
        });
    }
}
