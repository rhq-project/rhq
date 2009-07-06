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
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.rhq.enterprise.server.util.SessionTestHelper;

public class RecursiveResourceGroupTest extends AbstractEJB3Test {

    private ResourceGroupManagerLocal resourceGroupManager;
    private ResourceManagerLocal resourceManager;
    private RoleManagerLocal roleManager;
    private SubjectManagerLocal subjectManager;

    @BeforeSuite
    @SuppressWarnings( { "unused" })
    private void init() {
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        resourceManager = LookupUtil.getResourceManager();
        roleManager = LookupUtil.getRoleManager();
        subjectManager = LookupUtil.getSubjectManager();
    }

    @Test(groups = "integration.session")
    public void testImplicitGroupMembershipFromInventoryUpdate() throws Throwable {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            // setup simple test structures
            Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
            Role role = SessionTestHelper
                .createNewRoleForSubject(em, subject, "fake role", Permission.MANAGE_INVENTORY);

            ResourceGroup recursiveGroup = SessionTestHelper.createNewMixedGroupForRole(em, role, "fake group", true);

            // setup the test tree
            List<Resource> fullTree = getSimpleTree(em);

            // test simple implicit resources
            Resource nodeA = ResourceTreeHelper.findNode(fullTree, "A");

            List<Resource> resourcesFromTreeA = ResourceTreeHelper.getSubtree(nodeA);

            resourceGroupManager.addResourcesToGroup(subject, recursiveGroup.getId(), new Integer[] { nodeA.getId() });
            List<Resource> initialExplicitResources = resourceManager.getExplicitResourcesByResourceGroup(subject,
                recursiveGroup, PageControl.getUnlimitedInstance());

            assert initialExplicitResources.size() == 1 : "Failed: initial explicit resources, size was "
                + initialExplicitResources.size();
            assert initialExplicitResources.get(0).getId() == nodeA.getId() : "Failed: initial explicit resources id, found "
                + initialExplicitResources.get(0).getId() + ", expected " + nodeA.getId();

            List<Resource> initialImplicitResources = resourceManager.getImplicitResourcesByResourceGroup(subject,
                recursiveGroup, PageControl.getUnlimitedInstance());
            verifyEqualByIds("Failed: initial implicit resources", resourcesFromTreeA, initialImplicitResources);

            // test update implicit resources
            Resource newChildOfNodeA = new Resource("new nodeOne child", "new nodeOne child", nodeA.getResourceType());

            resourceManager.createResource(subject, newChildOfNodeA, nodeA.getId());

            List<Resource> updatedImplicitResources = resourceManager.getImplicitResourcesByResourceGroup(subject,
                recursiveGroup, PageControl.getUnlimitedInstance());

            resourcesFromTreeA.add(newChildOfNodeA);
            verifyEqualByIds("Failed: simple implicit resources", resourcesFromTreeA, updatedImplicitResources);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.session")
    public void testImplicitGroupMembershipFromComplexGroupUpdates() throws Throwable {
        Subject subject = null;
        Role role = null;
        ResourceGroup recursiveGroup = null;
        List<Resource> fullTree = null;

        Resource nodeBigA;
        Resource nodeOne;
        Resource nodeThree;
        Resource nodeLittleA;
        Resource nodeTripleLittleI;

        List<Resource> resultsExplicit = null;

        List<Resource> expectedImplicit = null;
        List<Resource> expectedExplicit = new ArrayList<Resource>();

        try {
            getTransactionManager().begin();
            try {
                EntityManager em = getEntityManager();
                // setup simple test structures
                subject = SessionTestHelper.createNewSubject(em, "fake subject");
                role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role", Permission.MANAGE_INVENTORY);
                recursiveGroup = SessionTestHelper.createNewMixedGroupForRole(em, role, "fake group", true);

                // setup the test tree
                fullTree = getSimpleTree(em);

                ResourceTreeHelper.printForest(fullTree);

                // get the resources from the tree we want to explicitly add
                nodeBigA = ResourceTreeHelper.findNode(fullTree, "A");
                nodeOne = ResourceTreeHelper.findNode(fullTree, "1");
                nodeThree = ResourceTreeHelper.findNode(fullTree, "3");
                nodeLittleA = ResourceTreeHelper.findNode(fullTree, "a");
                nodeTripleLittleI = ResourceTreeHelper.findNode(fullTree, "iii");

                // adding nodeLittleA should give us the subtree under nodeLittleA
                expectedImplicit = ResourceTreeHelper.getSubtree(nodeLittleA);
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeLittleA, expectedImplicit);
                resultsExplicit = resourceManager.getExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeLittleA);
                verifyEqualByIds("explicit add 1", expectedExplicit, resultsExplicit);

                // adding nodeThree should give us the union of the subtrees under nodeLittleA and nodeThree
                expectedImplicit.add(nodeThree);
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeThree, expectedImplicit);
                resultsExplicit = resourceManager.getExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeThree);
                verifyEqualByIds("explicit add 2", expectedExplicit, resultsExplicit);

                // adding nodeBigA should give us the union of the entire A tree with the nodeThree subtree
                expectedImplicit = ResourceTreeHelper.getSubtree(nodeBigA);
                expectedImplicit.addAll(ResourceTreeHelper.getSubtree(nodeThree));
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeBigA, expectedImplicit);
                resultsExplicit = resourceManager.getExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeBigA);
                verifyEqualByIds("explicit add 3", expectedExplicit, resultsExplicit);

                // adding nodeOne, which is a child of nodeBigA, shouldn't effect the expected results
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeOne, expectedImplicit);
                resultsExplicit = resourceManager.getExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeOne);
                verifyEqualByIds("explicit add 4", expectedExplicit, resultsExplicit);

                // adding nodeTripleLittleI shouldn't effect the expected results either
                implicitGroupMembershipAddHelper(subject, recursiveGroup, nodeTripleLittleI, expectedImplicit);
                resultsExplicit = resourceManager.getExplicitResourcesByResourceGroup(subject, recursiveGroup,
                    PageControl.getUnlimitedInstance());
                expectedExplicit.add(nodeTripleLittleI);
                verifyEqualByIds("explicit add 5", expectedExplicit, resultsExplicit);
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            } finally {
                getTransactionManager().commit();
            }

            getTransactionManager().begin();
            try {
                // removing the subtree nodeTripleLittleI shouldn't affect the expected set
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeTripleLittleI, expectedImplicit);
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            } finally {
                getTransactionManager().commit();
            }

            getTransactionManager().begin();
            try {
                // removing a descendant of a node that is also in the explicit list should be a no-op
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeOne, expectedImplicit);

                expectedImplicit.remove(nodeThree);
                // removing the wandering nodeThree, so that results will be the complete nodeBigA subtree
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeThree, expectedImplicit);

                // removing a root node should remove all descendants that aren't still in the explicit list
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigA, ResourceTreeHelper
                    .getSubtree(nodeLittleA));
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            } finally {
                getTransactionManager().commit();
            }

            getTransactionManager().begin();
            try {
                // remove a node that wasn't in the group - negative testing
                try {
                    // passing the "real" expected list for the results; this way, if the exception doesn't happen, the helper returns true
                    implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigA, ResourceTreeHelper
                        .getSubtree(nodeLittleA));
                    assert false : "Failed: removed non-existent successfully: node = " + nodeBigA.getName();
                } catch (ResourceGroupUpdateException rgue) {
                    // expected
                }
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            } finally {
                // throwing the RGUE will already mark this xaction for rollback
                //getTransactionManager().commit();
                getTransactionManager().rollback();
            }

            getTransactionManager().begin();
            try {
                // removing the last resource should leave an empty list
                implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeLittleA, new ArrayList<Resource>());
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            } finally {
                getTransactionManager().commit();
            }

            getTransactionManager().begin();
            try {
                // remove a node that wasn't in the group - negative testing
                try {
                    // passing the "real" expected list for the results; this way, if the exception doesn't happen, the helper returns true
                    Resource nodeBigB = ResourceTreeHelper.findNode(fullTree, "B");
                    implicitGroupMembershipRemoveHelper(subject, recursiveGroup, nodeBigB, ResourceTreeHelper
                        .getSubtree(nodeBigA));
                    assert false : "Failed: removed non-existent successfully: node = " + nodeBigB.getName();
                } catch (ResourceGroupUpdateException rgue) {
                    // expected
                    resultsExplicit = resourceManager.getExplicitResourcesByResourceGroup(subject, recursiveGroup,
                        PageControl.getUnlimitedInstance());
                    verifyEqualByIds("explicit remove 0", new ArrayList<Resource>(), resultsExplicit);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            } finally {
                // throwing the RGUE will already mark this xaction for rollback
                //getTransactionManager().commit();
                getTransactionManager().rollback();
            }

        } finally {
            // clean up anything that may have gotten created
            getTransactionManager().begin();
            try {
                EntityManager em = getEntityManager();

                Subject overlord = subjectManager.getOverlord();

                if (null != subject) {
                    subjectManager.deleteUsers(overlord, new int[] { subject.getId() });
                }
                if (null != role) {
                    roleManager.deleteRoles(overlord, new Integer[] { role.getId() });
                }
                if (null != recursiveGroup) {
                    resourceGroupManager.deleteResourceGroup(overlord, recursiveGroup.getId());
                }

                if (null != fullTree) {
                    ResourceTreeHelper.deleteForest(em, fullTree);
                }

            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            } finally {
                getTransactionManager().commit();
            }
        }
    }

    private void printGroup(String prefix, Subject subject, ResourceGroup group) {
        print(prefix + ": exp", resourceManager.getExplicitResourcesByResourceGroup(subject, group, PageControl
            .getUnlimitedInstance()));
        print(prefix + ": imp", resourceManager.getImplicitResourcesByResourceGroup(subject, group, PageControl
            .getUnlimitedInstance()));
    }

    private void print(String prefix, List<Resource> resources) {
        System.out.print(prefix + ": ");
        for (Resource res : resources) {
            System.out.print(res.getName() + " ");
        }
        System.out.println();
    }

    private void implicitGroupMembershipAddHelper(Subject subject, ResourceGroup recursiveGroup, Resource node,
        List<Resource> expectedResults) throws Exception {
        printGroup("complex implicit before add: node = " + node.getName() + " [" + node.getId() + "]", subject,
            recursiveGroup);
        resourceGroupManager.addResourcesToGroup(subject, recursiveGroup.getId(), new Integer[] { node.getId() });
        printGroup("complex implicit after add: node = " + node.getName() + " [" + node.getId() + "]", subject,
            recursiveGroup);
        List<Resource> implicitResources = resourceManager.getImplicitResourcesByResourceGroup(subject, recursiveGroup,
            PageControl.getUnlimitedInstance());
        verifyEqualByIds("Failed: complex implicit add: node = " + node.getName() + " [" + node.getId() + "]",
            expectedResults, implicitResources);
    }

    private void implicitGroupMembershipRemoveHelper(Subject subject, ResourceGroup recursiveGroup, Resource node,
        List<Resource> expectedResults) throws Exception {
        printGroup("complex implicit before remove: node = " + node.getName() + " [" + node.getId() + "]", subject,
            recursiveGroup);
        resourceGroupManager.removeResourcesFromGroup(subject, recursiveGroup.getId(), new Integer[] { node.getId() });
        printGroup("complex implicit after remove: node = " + node.getName() + " [" + node.getId() + "]", subject,
            recursiveGroup);
        List<Resource> implicitResources = resourceManager.getImplicitResourcesByResourceGroup(subject, recursiveGroup,
            PageControl.getUnlimitedInstance());
        verifyEqualByIds("Failed: complex implicit remove: node = " + node.getName() + " [" + node.getId() + "]",
            expectedResults, implicitResources);
    }

    private void verifyEqualByIds(String errorMessage, List<Resource> expected, List<Resource> results) {
        List<Integer> expectedIds = getIds(expected);
        List<Integer> resultsIds = getIds(results);
        assert (expectedIds.containsAll(resultsIds) && resultsIds.containsAll(expectedIds)) : errorMessage
            + "\nexpected = " + expectedIds + "\nresults = " + resultsIds;
    }

    private List<Integer> getIds(List<Resource> resources) {
        List<Integer> results = new ArrayList<Integer>();
        for (Resource res : resources) {
            results.add(res.getId());
        }
        Collections.sort(results);
        return results;
    }

    private List<Resource> getSimpleTree(EntityManager em) {
        return ResourceTreeHelper.createTree(em, "A=1,2; 1=a,b; a=i,ii; b=iii,iv; B=3");
    }
}