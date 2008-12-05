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

package org.rhq.enterprise.server.resource.cluster;

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
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * Test for {@link ClusterManagerBean} SLSB.
 * @author jshaughnessy
 *
 */
@Test(groups = { "resource-manager" })
public class ClusterManagerBeanTest extends AbstractEJB3Test {
    static private final boolean TESTS_ENABLED = true;

    private ClusterManagerLocal clusterManager;
    private ResourceGroupManagerLocal resourceGroupManager;
    private ResourceManagerLocal resourceManager;
    private RoleManagerLocal roleManager;
    private SubjectManagerLocal subjectManager;

    @BeforeSuite
    @SuppressWarnings( { "unused" })
    private void init() {
        clusterManager = LookupUtil.getClusterManager();
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        resourceManager = LookupUtil.getResourceManager();
        roleManager = LookupUtil.getRoleManager();
        subjectManager = LookupUtil.getSubjectManager();
    }

    @Test(enabled = TESTS_ENABLED)
    public void testClusterKey() throws Throwable {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            // setup simple test structures
            Subject subject = SessionTestHelper.createNewSubject(em, "subject-ClusterManagerBeanTest");
            Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "role-ClusterManagerBeanTest",
                Permission.MANAGE_INVENTORY);

            ResourceGroup clusterGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, role,
                "clusterGroup-ClusterManagerBeanTest");

            // setup two test resource trees to make into a resource cluster. Two compatible root nodes
            // with the exact same child hierarchy
            List<Resource> treeA = ResourceTreeHelper.createTree(em,
                "A=d1c1,d1c2; d1c1=d2c1,d2c2; d2c1=d3c1,d3c2; d2c2=d3c3,d3c4");
            List<Resource> treeB = ResourceTreeHelper.createTree(em,
                "B=d1c1,d1c2; d1c1=d2c1,d2c2; d2c1=d3c1,d3c2; d2c2=d3c3,d3c4");

            // test simple implicit resources
            Resource nodeA = ResourceTreeHelper.findNode(treeA, "A");
            Resource nodeB = ResourceTreeHelper.findNode(treeB, "B");

            resourceGroupManager.addResourcesToGroup(subject, clusterGroup.getId(), new Integer[] { nodeA.getId(),
                nodeB.getId() });

            PageList<Resource> AChildren = resourceManager.getChildResources(subject, nodeA, PageControl
                .getUnlimitedInstance());
            assert !AChildren.isEmpty() : "Expected Children for Node A";

            int AChildResourceTypeId = AChildren.get(0).getResourceType().getId();
            String AChildResourceKey = AChildren.get(0).getResourceKey();

            ClusterKey clusterKey = new ClusterKey(clusterGroup.getId(), AChildResourceTypeId, AChildResourceKey);
            assert clusterKey.getClusterGroupId() == clusterGroup.getId();
            assert clusterKey.getDepth() == 1;
            assert AChildResourceKey.equals(clusterKey.getHierarchy().get(0).getResourceKey());
            assert AChildResourceTypeId == clusterKey.getHierarchy().get(0).getResourceTypeId();
            assert AChildResourceTypeId == ClusterKey.getResourceType(clusterKey);
            System.out.println("      clusterKey=" + clusterKey.toString());
            System.out.println("named clusterKey=" + clusterKey.toNamedString(em));
            assert ClusterKey.valueOf(clusterKey.toString()) != null;

            List<Resource> resources = clusterManager.getAutoClusterResources(subject, clusterKey);
            /*
            assert resources.size() == 2;
            assert resources.get(0).getId() != resources.get(0).getId();
            assert resources.get(0).getParentResource().getId() != resources.get(1).getParentResource().getId();
            assert resources.get(0).getParentResource().getId() == nodeA.getId()
                || resources.get(0).getParentResource().getId() == nodeB.getId();
            assert resources.get(1).getParentResource().getId() == nodeA.getId()
                || resources.get(1).getParentResource().getId() == nodeB.getId();
            */

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }
}
