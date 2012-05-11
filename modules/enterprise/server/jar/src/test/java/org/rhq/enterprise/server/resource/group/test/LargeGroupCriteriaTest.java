/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.util.Iterator;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.test.LargeGroupTestBase;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

@Test
public class LargeGroupCriteriaTest extends LargeGroupTestBase {

    private static final boolean TEST_ENABLED = true;

    private ArrayList<LargeGroupEnvironment> env;

    private class GroupAvailCounts {
        public final int up;
        public final int down;
        public final int unknown;
        public final int disabled;
        public final int uncommitted;
        public final int total;
        public final int visibleTotal;

        GroupAvailCounts(int up, int down, int unknown, int disabled) {
            this(up, down, unknown, disabled, 0);
        }

        GroupAvailCounts(int up, int down, int unknown, int disabled, int uncommitted) {
            this.up = up;
            this.down = down;
            this.unknown = unknown;
            this.disabled = disabled;
            this.uncommitted = uncommitted;
            this.total = up + down + unknown + disabled + uncommitted;
            this.visibleTotal = up + down + unknown + disabled; // uncommitted is not included
        }
    }

    @Override
    protected void setupMockAgentServices(TestServerCommunicationsService agentServiceContainer) {
    }

    /**
     * Remove the group and all its members.
     */
    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        if (env != null) {
            Iterator<LargeGroupEnvironment> iter = env.iterator();
            while (iter.hasNext()) {
                LargeGroupEnvironment doomed = iter.next();
                tearDownLargeGroupWithNormalUserRoleAccess(doomed, iter.hasNext());
                SessionTestHelper.simulateLogout(doomed.normalSubject);
            }
            env = null;
        }
    }

    @Test(enabled = TEST_ENABLED)
    public void deleteMe() throws Exception {
        ArrayList<GroupAvailCounts> gacs = new ArrayList<LargeGroupCriteriaTest.GroupAvailCounts>();
        gacs.add(new GroupAvailCounts(1, 1, 1, 1, 1));
        testGroupQueries(gacs);
    }

    @Test(enabled = TEST_ENABLED)
    public void testSmallGroupsWithUncommitted() throws Exception {
        ArrayList<GroupAvailCounts> gacs = new ArrayList<LargeGroupCriteriaTest.GroupAvailCounts>();
        gacs.add(new GroupAvailCounts(8, 4, 2, 1, 1));
        gacs.add(new GroupAvailCounts(2, 4, 6, 8, 10));
        testGroupQueries(gacs);
    }

    @Test(enabled = TEST_ENABLED)
    public void testSmallGroups() throws Exception {
        ArrayList<GroupAvailCounts> gacs = new ArrayList<LargeGroupCriteriaTest.GroupAvailCounts>();
        gacs.add(new GroupAvailCounts(8, 4, 2, 1));
        gacs.add(new GroupAvailCounts(2, 4, 6, 8));
        testGroupQueries(gacs);
    }

    @Test(enabled = TEST_ENABLED)
    public void testLotsOfSmallGroups() throws Exception {
        ArrayList<GroupAvailCounts> gacs = new ArrayList<LargeGroupCriteriaTest.GroupAvailCounts>();
        for (int i = 0; i < 50; i++) {
            gacs.add(new GroupAvailCounts(1, 1, 1, 1));
            gacs.add(new GroupAvailCounts(2, 2, 2, 2));
        }
        testGroupQueries(gacs);
    }

    @Test(enabled = TEST_ENABLED)
    public void testLargeGroup() throws Exception {
        ArrayList<GroupAvailCounts> gacs = new ArrayList<LargeGroupCriteriaTest.GroupAvailCounts>();
        gacs.add(new GroupAvailCounts(500, 250, 150, 110)); // purposefully over 1,000
        testGroupQueries(gacs);
    }

    @Test(enabled = TEST_ENABLED)
    public void testLotsOfLargeGroups() throws Exception {
        ArrayList<GroupAvailCounts> gacs = new ArrayList<LargeGroupCriteriaTest.GroupAvailCounts>();
        for (int i = 0; i < 5; i++) {
            gacs.add(new GroupAvailCounts(10, 10, 10, 10));
            gacs.add(new GroupAvailCounts(20, 20, 20, 20));
        }
        testGroupQueries(gacs);
    }

    private void testGroupQueries(ArrayList<GroupAvailCounts> groupAvailCounts) throws Exception {
        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
        AuthorizationManagerLocal authManager = LookupUtil.getAuthorizationManager();

        env = new ArrayList<LargeGroupEnvironment>(groupAvailCounts.size());

        LargeGroupEnvironment lgeWithTypes = null;
        for (GroupAvailCounts gac : groupAvailCounts) {
            env.add(createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(lgeWithTypes, gac.total, gac.down,
                gac.unknown, gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ));
            lgeWithTypes = env.get(0);
        }

        ResourceGroupCriteria criteria;
        PageList<ResourceGroupComposite> pageList;
        ResourceGroupComposite groupComp;
        long start;

        // test findResourceGroupCompositesByCriteria
        for (int i = 0; i < groupAvailCounts.size(); i++) {
            LargeGroupEnvironment lge = env.get(i);
            GroupAvailCounts gac = groupAvailCounts.get(i);

            SessionTestHelper.simulateLogin(lge.normalSubject);
            criteria = new ResourceGroupCriteria();
            start = System.currentTimeMillis();
            pageList = groupManager.findResourceGroupCompositesByCriteria(lge.normalSubject, criteria);
            System.out.println("findResourceGroupCompositesByCriteria #" + i + "==>" + (System.currentTimeMillis() - start) + "ms");
            assert pageList.size() == 1 : "the query should only have selected the one group for our user";
            groupComp = pageList.get(0);
            System.out.println("-->" + groupComp);
            assert groupComp.getExplicitCount() == gac.visibleTotal;
            assert groupComp.getExplicitCount() == groupComp.getImplicitCount(); // we aren't testing recursive groups
            assert groupComp.getExplicitUp() == gac.up;
            assert groupComp.getExplicitDown() == gac.down;
            assert groupComp.getExplicitUnknown() == gac.unknown;
            assert groupComp.getExplicitDisabled() == gac.disabled;
            
            // mainly to help test when there are uncommitted resources in the group - see BZ 820981
            Resource committed = pickAResourceWithInventoryStatus(lge.platformResource, InventoryStatus.COMMITTED);
            assert true == authManager.hasResourcePermission(lge.normalSubject, Permission.CONFIGURE_READ,
                Collections.singletonList(committed.getId()));
            assert false == authManager.hasResourcePermission(lge.normalSubject, Permission.CONTROL,
                Collections.singletonList(committed.getId())); // we weren't given CONTROL perms on the committed resource
            Resource uncommitted = pickAResourceWithInventoryStatus(lge.platformResource, InventoryStatus.NEW);
            if (uncommitted != null) {
                assert false == authManager.hasResourcePermission(lge.normalSubject, Permission.CONFIGURE_READ,
                    Collections.singletonList(uncommitted.getId())); // no permissions for uncommitted resource
            }
        }

        // test getResourceGroupComposite
        for (int i = 0; i < groupAvailCounts.size(); i++) {
            LargeGroupEnvironment lge = env.get(i);
            GroupAvailCounts gac = groupAvailCounts.get(i);

            SessionTestHelper.simulateLogin(lge.normalSubject);
            start = System.currentTimeMillis();
            groupComp = groupManager.getResourceGroupComposite(lge.normalSubject, lge.compatibleGroup.getId());
            System.out.println("getResourceGroupComposite #" + i + "==>" + (System.currentTimeMillis() - start) + "ms");
            System.out.println("-->" + groupComp);
            assert groupComp.getExplicitCount() == gac.visibleTotal;
            assert groupComp.getExplicitCount() == groupComp.getImplicitCount(); // we aren't testing recursive groups
            assert groupComp.getExplicitUp() == gac.up;
            assert groupComp.getExplicitDown() == gac.down;
            assert groupComp.getExplicitUnknown() == gac.unknown;
            assert groupComp.getExplicitDisabled() == gac.disabled;
        }
    }

    private Resource pickAResourceWithInventoryStatus(Resource platformResource, InventoryStatus status) {
        for (Resource r : platformResource.getChildResources()) {
            if (r.getInventoryStatus() == status) {
                return r;
            }
        }
        return null;
    }
}
