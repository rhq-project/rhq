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
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.test.LargeGroupTestBase;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

@Test
public class LargeGroupCriteriaTest extends LargeGroupTestBase {

    private static final boolean TEST_ENABLED = true;

    private ArrayList<LargeGroupEnvironment> env;
    private List<ResourceGroup> globalGroups;

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
    @Override
    protected void afterMethod() throws Exception {
        tearDownGroups();
        super.afterMethod();
    }

    private void tearDownGroups() throws Exception {
        if (env != null) {
            System.out.println("Tearing down groups...");
            Iterator<LargeGroupEnvironment> iter = env.iterator();
            while (iter.hasNext()) {
                LargeGroupEnvironment doomed = iter.next();
                tearDownLargeGroupWithNormalUserRoleAccess(doomed, iter.hasNext());
                SessionTestHelper.simulateLogout(doomed.normalSubject);
            }
            env = null;
        }
        if (globalGroups != null) {
            Iterator<ResourceGroup> iter = globalGroups.iterator();
            while (iter.hasNext()) {
                resourceGroupManager.deleteResourceGroup(getOverlord(), iter.next().getId());
            }
            globalGroups = null;
        }
    }

    @Test(enabled = TEST_ENABLED)
    public void testSearchBarAvailabilityQueryUP() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(5, 0, 0, 0);
        PageList<ResourceGroupComposite> pageList = testGroupQueriesWithSearchBar(gac, "availability=up");
        assert pageList.size() == 1;

        tearDownGroups();
        pageList = testGroupQueriesWithSearchBar(gac, "availability != up");
        assert pageList.size() == 0;
    }

    @Test(enabled = TEST_ENABLED)
    public void testSearchBarAvailabilityQueryDOWN() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(0, 5, 0, 0);
        PageList<ResourceGroupComposite> pageList = testGroupQueriesWithSearchBar(gac, "availability=down");
        assert pageList.size() == 1;

        tearDownGroups();
        pageList = testGroupQueriesWithSearchBar(gac, "availability != down");
        assert pageList.size() == 0;
    }

    @Test(enabled = TEST_ENABLED)
    public void testSearchBarAvailabilityQueryDISABLED() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(0, 0, 0, 5);
        PageList<ResourceGroupComposite> pageList = testGroupQueriesWithSearchBar(gac, "availability=disabled");
        assert pageList.size() == 1;

        tearDownGroups();
        pageList = testGroupQueriesWithSearchBar(gac, "availability != disabled");
        assert pageList.size() == 0;
    }

    @Test(enabled = TEST_ENABLED)
    public void testSearchBarAvailabilityQueryMIXED() throws Exception {
        // when a group has a mix of up/down/disabled resources, it will not be returned with the avail search expression
        GroupAvailCounts gac = new GroupAvailCounts(2, 2, 0, 2);
        PageList<ResourceGroupComposite> pageList = testGroupQueriesWithSearchBar(gac, "availability=up");
        assert pageList.size() == 0;

        tearDownGroups();
        pageList = testGroupQueriesWithSearchBar(gac, "availability=down");
        assert pageList.size() == 0;

        tearDownGroups();
        pageList = testGroupQueriesWithSearchBar(gac, "availability=disabled");
        assert pageList.size() == 0;
    }

    @Test(enabled = TEST_ENABLED)
    public void testUncommitted() throws Exception {
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

    @Test(enabled = TEST_ENABLED)
    public void testVariousCriteriaQueries() throws Exception {

        globalGroups = new ArrayList<ResourceGroup>();
        // create 10 empty groups
        executeInTransaction(false, new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                for (int i = 0; i < 10; i++) {
                    ResourceGroup group = SessionTestHelper.createNewCompatibleGroupForRole(em, null,
                        "LargeGroupTestCompatGroup", null);
                    globalGroups.add(group);
                }
            }
        });

        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();

        // test ordering
        criteria.addSortName(PageOrdering.DESC);
        PageList<ResourceGroupComposite> result = groupManager.findResourceGroupCompositesByCriteria(getOverlord(),
            criteria);
        assert result.size() == 10 : "Expected to return 10 group composites, but was " + result.size();

        // test custom page size
        criteria = new ResourceGroupCriteria();
        criteria.setPageControl(new PageControl(0, 2));
        result = groupManager.findResourceGroupCompositesByCriteria(getOverlord(), criteria);
        assert result.size() == 2 : "Expected to return 2 group composites when paging is enabled";
        assert result.getTotalSize() == 10 : "Expected 10 group composites as total pageList size";

        // test custom page size and ordering within pageControl
        criteria = new ResourceGroupCriteria();
        criteria.setPageControl(new PageControl(0, 2, new OrderingField("name", PageOrdering.DESC)));
        result = groupManager.findResourceGroupCompositesByCriteria(getOverlord(), criteria);
        assert result.size() == 2 : "Expected to return 2 group composites when paging is enabled";
        assert result.getTotalSize() == 10 : "Expected 10 group composites as total pageList size";
    }

    // test findResourcesByCriteriaBounded here instead of ResourceGroupManagerBeanTest because we want
    // to work with a decent # or resources.
    @Test(enabled = TEST_ENABLED)
    public void testResourceCriteriaBounded() throws Exception {
        ArrayList<GroupAvailCounts> gacs = new ArrayList<LargeGroupCriteriaTest.GroupAvailCounts>();
        gacs.add(new GroupAvailCounts(1100, 0, 0, 0)); // purposefully over 1,000, avails don't really matter

        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        AuthorizationManagerLocal authManager = LookupUtil.getAuthorizationManager();

        env = new ArrayList<LargeGroupEnvironment>(gacs.size());

        LargeGroupEnvironment lgeWithTypes = null;
        for (GroupAvailCounts gac : gacs) {
            env.add(createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(lgeWithTypes, gac.total, gac.down,
                gac.unknown, gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ));
            lgeWithTypes = env.get(0);
        }

        ResourceCriteria criteria = new ResourceCriteria();
        List<Resource> result;

        SessionTestHelper.simulateLogin(env.get(0).normalSubject);

        criteria.addFilterParentResourceId(lgeWithTypes.platformResource.getId());
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        result = resourceManager.findResourcesByCriteria(env.get(0).normalSubject, criteria);
        assert null != result;
        assert result.size() == 1100 : "Expected unbounded query to return all 1100 resources";

        result = resourceManager.findResourcesByCriteriaBounded(env.get(0).normalSubject, criteria, 2000, 100);
        assert null != result;
        assert result.size() == 1100 : "Expected 2000/100 bounded query to return all 1100 resources";

        result = resourceManager.findResourcesByCriteriaBounded(env.get(0).normalSubject, criteria, 1100, 100);
        assert null != result;
        assert result.size() == 1100 : "Expected 1100/100 bounded query to return all 1100 resources";

        result = resourceManager.findResourcesByCriteriaBounded(env.get(0).normalSubject, criteria, 0, 0);
        assert null != result;
        assert result.size() == 200 : "Expected default (1000/200) bounded query to return 200 resources";

        result = resourceManager.findResourcesByCriteriaBounded(env.get(0).normalSubject, criteria, 0, 500);
        assert null != result;
        assert result.size() == 500 : "Expected default (1000)/500) bounded query to return 500 resources";

        result = resourceManager.findResourcesByCriteriaBounded(env.get(0).normalSubject, criteria, 100, 200);
        assert null != result;
        assert result.size() == 100 : "Expected 100/200 bounded query to return 100 resources";
    }

    private PageList<ResourceGroupComposite> testGroupQueriesWithSearchBar(GroupAvailCounts gac, String searchExpression)
        throws Exception {
        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

        env = new ArrayList<LargeGroupEnvironment>(1);

        LargeGroupEnvironment lgeWithTypes = null;
        env.add(createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(lgeWithTypes, gac.total, gac.down,
                gac.unknown, gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ));

        ResourceGroupCriteria criteria;
        PageList<ResourceGroupComposite> pageList;
        long start;

        // test findResourceGroupCompositesByCriteria where the criteria will use the search bar feature
        SessionTestHelper.simulateLogin(env.get(0).normalSubject);
        criteria = new ResourceGroupCriteria();
        criteria.setSearchExpression(searchExpression);
        start = System.currentTimeMillis();
        pageList = groupManager.findResourceGroupCompositesByCriteria(env.get(0).normalSubject, criteria);
        System.out.println("criteria with search==>" + (System.currentTimeMillis() - start) + "ms");
        return pageList;
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
            System.out.println("findResourceGroupCompositesByCriteria #" + i + "==>"
                + (System.currentTimeMillis() - start) + "ms");
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
