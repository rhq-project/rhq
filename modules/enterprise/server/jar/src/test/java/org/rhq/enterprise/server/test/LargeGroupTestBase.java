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
package org.rhq.enterprise.server.test;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;
import org.rhq.test.JPAUtils;
import org.rhq.test.TransactionCallbackWithContext;

public abstract class LargeGroupTestBase extends AbstractEJB3Test {
    protected static final String PROPERTY_ONE_NAME = "LargeGroupTest prop1";
    protected static final String PROPERTY_ONE_VALUE = "LargeGroupTest property one";
    protected static final String PROPERTY_TWO_NAME = "LargeGroupTest prop2";

    protected ConfigurationManagerLocal configurationManager;
    protected ResourceManagerLocal resourceManager;
    protected ResourceGroupManagerLocal resourceGroupManager;
    protected SubjectManagerLocal subjectManager;

    protected class LargeGroupEnvironment {
        public Agent agent;
        public Resource platformResource; // all if its children will be members of the compatible group
        public ResourceGroup compatibleGroup;
        public Subject normalSubject;
        public Role normalRole;
    }

    /**
     * Prepares things for the entire test class.
     */
    @BeforeClass
    public void beforeClass() {
        configurationManager = LookupUtil.getConfigurationManager();
        resourceManager = LookupUtil.getResourceManager();
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        subjectManager = LookupUtil.getSubjectManager();

        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        setupMockAgentServices(agentServiceContainer);

        prepareScheduler();
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        try {
            unprepareForTestAgents();
        } finally {
            unprepareScheduler();
        }
    }

    protected abstract void setupMockAgentServices(TestServerCommunicationsService agentServiceContainer);

    /**
     * Creates a compatible group of the given size. This also creates a normal user and a role
     * and makes sure that user can access the group that was created.
     *
     * @param groupSize the number of members to create and put into the compatible group that is created.
     * @return information about the entities that were created
     */
    protected LargeGroupEnvironment createLargeGroupWithNormalUserRoleAccess(final int groupSize) {

        System.out.println("=====Creating a group with [" + groupSize + "] members");

        final LargeGroupEnvironment lge = new LargeGroupEnvironment();
        JPAUtils.executeInTransaction(new TransactionCallbackWithContext<Object>() {
            public Object execute(TransactionManager tm, EntityManager em) throws Exception {
                // create the agent where all resources will be housed
                lge.agent = SessionTestHelper.createNewAgent(em, "GroupPluginConfigTestAgent");

                // create the platform resource type and server resource type
                // the server type will have our plugin configuration definition that we will use when testing
                ResourceType platformType = new ResourceType("GroupPluginConfigTestPlatformType", "testPlugin",
                    ResourceCategory.PLATFORM, null);
                em.persist(platformType);

                ResourceType serverType = new ResourceType("GroupPluginConfigTestServerType", "testPlugin",
                    ResourceCategory.SERVER, platformType);
                ConfigurationDefinition pluginConfigDef = new ConfigurationDefinition("GroupPluginConfigTestDef",
                    "desc");
                pluginConfigDef.put(new PropertyDefinitionSimple(PROPERTY_ONE_NAME, "prop1desc", false,
                    PropertySimpleType.STRING));
                pluginConfigDef.put(new PropertyDefinitionSimple(PROPERTY_TWO_NAME, "prop2desc", false,
                    PropertySimpleType.STRING));
                serverType.setPluginConfigurationDefinition(pluginConfigDef);
                em.persist(serverType);
                em.flush();

                // create our platform - all of our server resources will have this as their parent
                lge.platformResource = SessionTestHelper.createNewResource(em, "GroupPluginConfigTestPlatform",
                    platformType);
                lge.platformResource.setAgent(lge.agent);

                // create our subject and role
                lge.normalSubject = new Subject("GroupPluginConfigTestSubject", true, false);
                lge.normalRole = SessionTestHelper.createNewRoleForSubject(em, lge.normalSubject,
                    "GroupPluginConfigTestRole", Permission.MODIFY_RESOURCE);

                // create our compatible group
                lge.compatibleGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, lge.normalRole,
                    "GroupPluginConfigTestCompatGroup", serverType);

                // create our many server resources
                System.out.print("   creating resources, this might take some time");
                for (int i = 1; i <= groupSize; i++) {
                    System.out.print(((i % 100) == 0) ? String.valueOf(i) : ".");
                    Resource res = SessionTestHelper.createNewResourceForGroup(em, lge.compatibleGroup,
                        "GroupPluginConfigTestServer", serverType, (i % 100) == 0);
                    res.setAgent(lge.agent);
                    lge.platformResource.addChildResource(res);

                    Configuration c = new Configuration();
                    c.put(new PropertySimple(PROPERTY_ONE_NAME, PROPERTY_ONE_VALUE));
                    c.put(new PropertySimple(PROPERTY_TWO_NAME, res.getId()));
                    em.persist(c);
                    res.setPluginConfiguration(c);
                }
                System.out.println("Done.");

                em.flush();
                return null;
            }
        });

        System.out.println("=====Created group [" + lge.compatibleGroup.getName() + "] with ["
            + lge.platformResource.getChildResources().size() + "] members");

        return lge;
    }

    /**
     * Purges all the entities that were created by the {@link #createLargeGroupWithNormalUserRoleAccess(int)} method.
     * This includes the user, role, agent and all resources along with the group itself.
     *
     * @param lge contains information that was created which needs to be deleted
     */
    protected void tearDownLargeGroupWithNormalUserRoleAccess(final LargeGroupEnvironment lge) throws Exception {
        System.out.println("=====Cleaning up test data from " + this.getClass().getSimpleName());

        // purge the group itself
        resourceGroupManager.deleteResourceGroup(getOverlord(), lge.compatibleGroup.getId());

        // purge all resources by performing in-band and out-of-band work in quick succession.
        // this takes a long time but trying to get this right using native queries is hard to get right so just do it this way.
        // only need to delete the platform which will delete all children servers AND the agent itself
        final List<Integer> deletedIds = resourceManager.uninventoryResource(getOverlord(),
            lge.platformResource.getId());
        for (Integer deletedResourceId : deletedIds) {
            resourceManager.uninventoryResourceAsyncWork(getOverlord(), deletedResourceId);
        }

        // purge the user and role
        JPAUtils.executeInTransaction(new TransactionCallbackWithContext<Object>() {
            public Object execute(TransactionManager tm, EntityManager em) throws Exception {
                lge.normalRole = em.getReference(Role.class, lge.normalRole.getId());
                lge.normalSubject = em.getReference(Subject.class, lge.normalSubject.getId());
                em.remove(lge.normalRole);
                em.remove(lge.normalSubject);
                return null;
            }
        });

        // purge the resource types
        JPAUtils.executeInTransaction(new TransactionCallbackWithContext<Object>() {
            public Object execute(TransactionManager tm, EntityManager em) throws Exception {
                ResourceType pType = em
                    .getReference(ResourceType.class, lge.platformResource.getResourceType().getId());
                ResourceType sType = em.getReference(ResourceType.class, lge.compatibleGroup.getResourceType().getId());
                em.remove(sType);
                em.remove(pType);
                return null;
            }
        });

        System.out.println("=====Cleaned up test data from " + this.getClass().getSimpleName());
    }

    /**
     * Given the id of a compatible group, this will return the status of the update.
     * 
     * @param groupId compatible group ID
     * @return status, or null if not known
     */
    protected ConfigurationUpdateStatus getGroupPluginConfigurationStatus(final int groupId) {
        return JPAUtils.executeInTransaction(new TransactionCallbackWithContext<ConfigurationUpdateStatus>() {
            public ConfigurationUpdateStatus execute(TransactionManager tm, EntityManager em) throws Exception {
                try {
                    Query query = em.createNamedQuery(GroupPluginConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID);
                    query.setParameter("groupId", groupId);
                    GroupPluginConfigurationUpdate latestConfigGroupUpdate = (GroupPluginConfigurationUpdate) query
                        .getSingleResult();
                    return latestConfigGroupUpdate.getStatus();
                } catch (NoResultException nre) {
                    // The group resource config history is empty, so there's obviously no update in progress.
                    return null;
                }
            }
        });
    }

    /**
     * Obtain the overlord user.
     * @return overlord
     */
    protected Subject getOverlord() {
        return subjectManager.getOverlord();
    }
}
