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

import java.util.Collections;
import java.util.List;

import javax.persistence.Query;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.test.LargeGroupTestBase;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

@Test
public class GroupWithUncommittedTest extends LargeGroupTestBase {

    private static final boolean TESTS_ENABLED = true;
    private LargeGroupEnvironment env;

    private class GroupAvailCounts {
        public final int up;
        public final int down;
        public final int unknown;
        public final int disabled;
        public final int uncommitted;
        public final int total;
        public final int visibleTotal;

        GroupAvailCounts(int up, int down, int unknown, int disabled, int uncommitted) {
            this.up = up;
            this.down = down;
            this.unknown = unknown;
            this.disabled = disabled;
            this.uncommitted = uncommitted;
            this.total = up + down + unknown + disabled + uncommitted;
            this.visibleTotal = up + down + unknown + disabled; // uncommitted is not included
            assert this.visibleTotal != this.total : "all of our tests should have at least 1 uncommitted resource";
        }
    }

    @Override
    protected void setupMockAgentServices(TestServerCommunicationsService agentServiceContainer) {
    }

    /**
     * Remove the group and all its members.
     */
    @AfterMethod(alwaysRun = true)
    public void teardownEnv() throws Exception {
        if (env != null) {
            tearDownLargeGroupWithNormalUserRoleAccess(env, false);
            SessionTestHelper.simulateLogout(env.normalSubject);
        }
        env = null;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testResourceGroupQueries() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(1, 1, 1, 1, 1);
        env = createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(null, gac.total, gac.down, gac.unknown,
            gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ);
        assert null != pickUncommittedResource(env.platformResource) : "there should have been an uncommitted resource";

        SessionTestHelper.simulateLogin(env.normalSubject);

        // these queries were tweeked to filter uncommitted - see BZ 820981
        PageList<ResourceGroupComposite> results = resourceGroupManager.findResourceGroupComposites(env.normalSubject,
            null, null,
            null, null, null, null, env.compatibleGroup.getId(), new PageControl(0, 50));
        int count = results.size();
        assert count == 1 : "results=" + results;
        ResourceGroupComposite rgc = results.get(0);
        assert rgc.getExplicitCount() == gac.visibleTotal;
        assert rgc.getImplicitCount() == gac.visibleTotal;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testGetResourceNameOptionItems() {
        GroupAvailCounts gac = new GroupAvailCounts(1, 1, 1, 1, 1);
        env = createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(null, gac.total, gac.down, gac.unknown,
            gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ);
        assert null != pickUncommittedResource(env.platformResource) : "there should have been an uncommitted resource";

        SessionTestHelper.simulateLogin(env.normalSubject);

        // this uses ResourceGroup.QUERY_FIND_RESOURCE_NAMES_BY_GROUP_ID which was tweeked due to BZ 820981
        List<IntegerOptionItem> results = LookupUtil.getOperationManager().getResourceNameOptionItems(
            env.compatibleGroup.getId());
        int count = results.size();
        assert count == gac.visibleTotal : "results=" + results;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAnotherResourceGroupQuery() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(1, 1, 1, 1, 1);
        env = createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(null, gac.total, gac.down, gac.unknown,
            gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ);
        assert null != pickUncommittedResource(env.platformResource) : "there should have been an uncommitted resource";

        SessionTestHelper.simulateLogin(env.normalSubject);

        // this query was tweeked to filter uncommitted - see BZ 820981
        // we aren't really testing recursive groups - so we don't have any resources in implicit list that isn't in explicit
        // but run this query anyway to make sure it can successfully run - we'll check that we get back 0 rows.
        Query q = getEntityManager().createNamedQuery(ResourceGroup.QUERY_FIND_RESOURCE_IDS_NOT_IN_GROUP_EXPLICIT);
        q.setParameter("groupId", env.compatibleGroup.getId());
        q.setParameter("resourceIds",
            Collections.singletonList(env.platformResource.getChildResources().iterator().next().getId())); // just pick any ID, it won't match anyway
        List<?> results = q.getResultList();
        assert results.isEmpty() : "results should have been empty" + results;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testResourceTypeQueries() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(1, 1, 1, 1, 1);
        env = createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(null, gac.total, gac.down, gac.unknown,
            gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ);
        assert null != pickUncommittedResource(env.platformResource) : "there should have been an uncommitted resource";

        SessionTestHelper.simulateLogin(env.normalSubject);

        // these queries were tweeked to filter uncommitted - see BZ 820981
        Query q = getEntityManager().createNamedQuery(ResourceType.QUERY_GET_EXPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP);
        q.setParameter("groupId", env.compatibleGroup.getId());
        List<?> results = q.getResultList();
        int count = results.size();
        assert count == 1 : "results=" + results;

        q = getEntityManager().createNamedQuery(ResourceType.QUERY_GET_IMPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP);
        q.setParameter("groupId", env.compatibleGroup.getId());
        results = q.getResultList();
        count = results.size();
        assert count == 1 : "results=" + results;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testResourceQueries() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(1, 1, 1, 1, 1);
        env = createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(null, gac.total, gac.down, gac.unknown,
            gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ);
        assert null != pickUncommittedResource(env.platformResource) : "there should have been an uncommitted resource";

        SessionTestHelper.simulateLogin(env.normalSubject);

        // these queries were tweeked to filter uncommitted - see BZ 820981
        Query q = getEntityManager().createNamedQuery(Resource.QUERY_FIND_EXPLICIT_IDS_BY_RESOURCE_GROUP_ADMIN);
        q.setParameter("groupId", env.compatibleGroup.getId());
        List<?> results = q.getResultList();
        int count = results.size();
        assert count == gac.visibleTotal : "results=" + results;

        q = getEntityManager().createNamedQuery(Resource.QUERY_FIND_IMPLICIT_IDS_BY_RESOURCE_GROUP_ADMIN);
        q.setParameter("groupId", env.compatibleGroup.getId());
        results = q.getResultList();
        count = results.size();
        assert count == gac.visibleTotal : "results=" + results;

        q = getEntityManager().createNamedQuery(Resource.QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP);
        q.setParameter("group", env.compatibleGroup);
        q.setParameter("subject", env.normalSubject);
        results = q.getResultList();
        count = results.size();
        assert count == gac.visibleTotal : "results=" + results;

        q = getEntityManager().createNamedQuery(Resource.QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP_ADMIN);
        q.setParameter("group", env.compatibleGroup);
        results = q.getResultList();
        count = results.size();
        assert count == gac.visibleTotal : "results=" + results;

        q = getEntityManager().createNamedQuery(Resource.QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP);
        q.setParameter("group", env.compatibleGroup);
        q.setParameter("subject", env.normalSubject);
        results = q.getResultList();
        count = results.size();
        assert count == gac.visibleTotal : "results=" + results;

        q = getEntityManager().createNamedQuery(Resource.QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP_ADMIN);
        q.setParameter("group", env.compatibleGroup);
        results = q.getResultList();
        count = results.size();
        assert count == gac.visibleTotal : "results=" + results;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAvailGroupQueries() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(1, 1, 1, 1, 1);
        env = createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(null, gac.total, gac.down, gac.unknown,
            gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ);
        assert null != pickUncommittedResource(env.platformResource) : "there should have been an uncommitted resource";

        SessionTestHelper.simulateLogin(env.normalSubject);

        // this query was tweeked to filter uncommitted - see BZ 820981
        Query q = getEntityManager().createNamedQuery(Availability.FIND_FOR_RESOURCE_GROUP_WITHIN_INTERVAL);
        q.setParameter("groupId", env.compatibleGroup.getId());
        q.setParameter("start", 0L);
        q.setParameter("end", System.currentTimeMillis());
        List<?> results = q.getResultList();
        int availCount = results.size();
        assert availCount == gac.visibleTotal : "availCount=" + availCount;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testConfigGroupQueries() throws Exception {
        GroupAvailCounts gac = new GroupAvailCounts(5, 4, 3, 2, 1);
        env = createLargeGroupWithNormalUserRoleAccessWithInventoryStatus(null, gac.total, gac.down, gac.unknown,
            gac.disabled, gac.uncommitted, Permission.CONFIGURE_READ);
        assert null != pickUncommittedResource(env.platformResource) : "there should have been an uncommitted resource";

        SessionTestHelper.simulateLogin(env.normalSubject);

        // these queries were tweeked to filter uncommitted - see BZ 820981
        Query resConfigCountQuery = PersistenceUtility.createCountQuery(getEntityManager(),
            Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID);
        resConfigCountQuery.setParameter("resourceGroupId", env.compatibleGroup.getId());
        long resConfigCount = (Long) resConfigCountQuery.getSingleResult();

        Query pluginConfigCountQuery = PersistenceUtility.createCountQuery(getEntityManager(),
            Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID);
        pluginConfigCountQuery.setParameter("resourceGroupId", env.compatibleGroup.getId());
        long pluginConfigCount = (Long) pluginConfigCountQuery.getSingleResult();

        assert resConfigCount == gac.visibleTotal;
        assert pluginConfigCount == gac.visibleTotal;

        int groupSize = resourceGroupManager.getExplicitGroupMemberCount(env.compatibleGroup.getId());
        assert resConfigCount == groupSize;
        assert pluginConfigCount == groupSize;

    }

    private Resource pickUncommittedResource(Resource platformResource) {
        for (Resource r : platformResource.getChildResources()) {
            if (r.getInventoryStatus() == InventoryStatus.NEW) {
                return r;
            }
        }
        return null;
    }
}
