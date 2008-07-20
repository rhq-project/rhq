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
package org.rhq.enterprise.server.resource.group.test;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.rhq.enterprise.server.util.SessionTestHelper;

public class RecursiveResourceGroupTest extends AbstractEJB3Test {
    private static ResourceGroupManagerLocal resourceGroupManager;
    private static ResourceManagerLocal resourceManager;

    @BeforeSuite
    @SuppressWarnings("unused")
    private void init() {
        try {
            resourceGroupManager = LookupUtil.getResourceGroupManager();
            resourceManager = LookupUtil.getResourceManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test(groups = "integration.session")
    public void testImplicitGroupMembershipFromInventoryUpdate() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            // setup simple test structures
            Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
            Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role");
            ResourceGroup recursiveGroup = SessionTestHelper.createNewMixedGroupForRole(em, role, "fake group", true);

            // setup the test tree
            List<Resource> fullTree = getSimpleTree(em);

            // test simple implicit resources
            Resource nodeOne = ResourceTreeHelper.findNode(fullTree, "1");
            resourceGroupManager
                .addResourcesToGroup(subject, recursiveGroup.getId(), new Integer[] { nodeOne.getId() });

            // test update implicit resources
            ResourceType type = new ResourceType("testType", "testPlugin", ResourceCategory.PLATFORM, null);
            em.persist(type);
            Resource newNodeOneChild = new Resource("new nodeOne child", "new nodeOne child", type);
            resourceManager.createResource(subject, newNodeOneChild, nodeOne.getId());

            List<Resource> updatedImplicitResources = resourceManager.getImplicitResourcesByResourceGroup(subject,
                recursiveGroup, PageControl.getUnlimitedInstance());
            List<Resource> updatedNodeOneSubtree = ResourceTreeHelper.getSubtree(nodeOne);
            assert (updatedNodeOneSubtree.containsAll(updatedImplicitResources) && updatedImplicitResources
                .containsAll(updatedNodeOneSubtree)) : "Failed: simple implicit resources";
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.session")
    public void testImplicitGroupMembershipFromComplexGroupUpdates() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            // setup simple test structures
            Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
            Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role");
            ResourceGroup recursiveGroup = SessionTestHelper.createNewMixedGroupForRole(em, role, "fake group", true);

            // setup the test tree
            List<Resource> fullTree = getSimpleTree(em);

            // get the resources from the tree we want to explicitly add
            Resource nodeBigA = ResourceTreeHelper.findNode(fullTree, "A");
            Resource nodeOne = ResourceTreeHelper.findNode(fullTree, "1");
            Resource nodeThree = ResourceTreeHelper.findNode(fullTree, "3");
            Resource nodeLittleA = ResourceTreeHelper.findNode(fullTree, "a");
            Resource nodeTripleLittleI = ResourceTreeHelper.findNode(fullTree, "iii");

            // adding nodeLittleA should give us the subtree under nodeLittleA
            List<Resource> expectedNodeLittleA = ResourceTreeHelper.getSubtree(nodeLittleA);
            implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeLittleA, expectedNodeLittleA);

            // adding nodeThree should give us the union of the subtrees under nodeLittleA and nodeThree
            List<Resource> expectedNodeThree = ResourceTreeHelper.getSubtree(nodeThree);
            expectedNodeThree.addAll(expectedNodeLittleA);
            implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeThree, expectedNodeThree);

            // adding nodeBigA should give us the union of the entire A tree with the nodeThree subtree
            List<Resource> expectedNodeBigA = ResourceTreeHelper.getSubtree(nodeBigA);
            expectedNodeBigA.addAll(ResourceTreeHelper.getSubtree(nodeThree));
            implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeBigA, ResourceTreeHelper.getSubtree(nodeBigA));

            // adding nodeOne, which is a child of nodeBigA, shouldn't effect the expected results
            implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeOne, ResourceTreeHelper.getSubtree(nodeBigA));

            // adding nodeTripleLittleI shouldn't effect the expected results either
            implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeTripleLittleI, ResourceTreeHelper
                .getSubtree(nodeBigA));

            // remove a node that wasn't in the group - negative testing
            try {
                // passing the "real" expected list for the results; this way, if the exception doesn't happen, the helper returns true
                Resource nodeBigB = ResourceTreeHelper.findNode(fullTree, "B");
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigB, ResourceTreeHelper
                    .getSubtree(nodeBigA));
                assert false : "Failed: removed non-existent successfully: node = " + nodeBigB.getName();
            } catch (ResourceGroupNotFoundException rgnfe) {
                // expected
            }

            // removing the disconnect subtree nodeTripleLittleI (it's parent isn't in the explicit set), to test proper recursion up the ancestry
            implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeTripleLittleI, ResourceTreeHelper
                .getSubtree(nodeBigA));

            // removing the wandering nodeThree, so that results will be a subtree (proper or not) of nodeBigA
            implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeThree, ResourceTreeHelper
                .getSubtree(nodeBigA));

            // removing a descendant of a node that is also in the explicit list should be a no-op
            implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeOne, ResourceTreeHelper
                .getSubtree(nodeBigA));

            // removing a root node should remove all descendants that aren't still in the explicit list
            implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigA, ResourceTreeHelper
                .getSubtree(nodeLittleA));

            // remove a node that wasn't in the group - negative testing
            try {
                // passing the "real" expected list for the results; this way, if the exception doesn't happen, the helper returns true
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigA, ResourceTreeHelper
                    .getSubtree(nodeLittleA));
                assert false : "Failed: removed non-existent successfully: node = " + nodeBigA.getName();
            } catch (ResourceGroupNotFoundException rgnfe) {
                // expected
            }

            // removing the last resource should leave an empty list
            implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeLittleA, new ArrayList<Resource>());
        } finally {
            getTransactionManager().rollback();
        }
    }

    private void implicitGroupMembershipAddHelper(Subject subject, ResourceGroup recursiveGroup, Resource node,
        List<Resource> expectedResults) throws Exception {
        resourceGroupManager.addResourcesToGroup(subject, recursiveGroup.getId(), new Integer[] { node.getId() });
        List<Resource> implicitResources = resourceManager.getImplicitResourcesByResourceGroup(subject, recursiveGroup,
            PageControl.getUnlimitedInstance());
        assert (expectedResults.containsAll(implicitResources) && implicitResources.containsAll(expectedResults)) : "Failed: complex implicit add: node = "
            + node.getName();
    }

    private void implicitGroupMembershipRemoveHelper(Subject subject, ResourceGroup recursiveGroup, Resource node,
        List<Resource> expectedResults) throws Exception {
        resourceGroupManager.removeResourcesFromGroup(subject, recursiveGroup.getId(), new Integer[] { node.getId() });
        List<Resource> implicitResources = resourceManager.getImplicitResourcesByResourceGroup(subject, recursiveGroup,
            PageControl.getUnlimitedInstance());
        assert (expectedResults.containsAll(implicitResources) && implicitResources.containsAll(expectedResults)) : "Failed: complex implicit remove: node = "
            + node.getName();
    }

    private List<Resource> getSimpleTree(EntityManager em) {
        return ResourceTreeHelper.createTree(em, "A=1,2; 1=a,b; a=i,ii; b=iii,iv; B=3");
    }
}