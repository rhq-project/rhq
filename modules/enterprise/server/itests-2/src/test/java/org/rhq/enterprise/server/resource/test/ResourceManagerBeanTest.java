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
package org.rhq.enterprise.server.resource.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

import org.jboss.remoting.InvokerLocator;

import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.server.CommandProcessorMetrics;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SessionNotFoundException;
import org.rhq.enterprise.server.core.comm.ServerConfiguration;
import org.rhq.enterprise.server.discovery.DiscoveryServerServiceImpl;
import org.rhq.enterprise.server.operation.OperationDefinitionNotFoundException;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionNotFoundException;
import org.rhq.enterprise.server.resource.metadata.test.UpdatePluginMetadataTestBase;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.test.TestServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test for {@link ResourceManagerLocal} SLSB.
 */
@Test
public class ResourceManagerBeanTest extends UpdatePluginMetadataTestBase {
    private Resource newResource;
    private ResourceGroup newGroup;
    private ResourceGroupManagerLocal groupManager;

    @Override
    protected void beforeMethod() throws Exception {
        super.beforeMethod();

        newResource = createNewResourceWithNewType();
        groupManager = LookupUtil.getResourceGroupManager();
        newGroup = createNewGroup();
    }

    public void testResourceErrors() {
        ResourceError error;
        List<ResourceError> errors;
        DiscoveryServerServiceImpl serverService = new DiscoveryServerServiceImpl();

        errors = resourceManager.findResourceErrors(getOverlord(), newResource.getId(),
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        assert errors.size() == 0;

        error = new ResourceError(newResource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, "test summary",
            "test detail", 12345);

        // simulate the agent notifying the server about an error
        // this will exercise the addResourceError in the SLSB
        serverService.setResourceError(error);
        errors = resourceManager.findResourceErrors(getOverlord(), newResource.getId(),
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        assert errors.size() == 1;
        error = errors.get(0);
        assert error.getId() > 0;
        assert error.getSummary().equals("test summary");
        assert error.getDetail().equals("test detail");
        assert error.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
        assert error.getTimeOccurred() == 12345;

        // simulate the agent notifying the server about another error.
        // there will only be a single invalid plugin config allowed; the prior one will be deleted
        // this will exercise the addResourceError and deleteResourceError in the SLSB
        error.setId(0);
        error.setTimeOccurred(567890);
        error.setSummary("another summary");
        error.setDetail("another detail");
        serverService.setResourceError(error);
        errors = resourceManager.findResourceErrors(getOverlord(), newResource.getId(),
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        assert errors.size() == 1;
        error = errors.get(0);
        assert error.getId() > 0;
        assert error.getSummary().equals("another summary");
        assert error.getDetail().equals("another detail");
        assert error.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
        assert error.getTimeOccurred() == 567890;

        resourceManager.deleteResourceError(getOverlord(), error.getId());
        errors = resourceManager.findResourceErrors(getOverlord(), newResource.getId(),
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        assert errors.size() == 0;
    }

    @Override
    protected void afterMethod() throws Exception {
        if (newGroup != null) {
            groupManager.deleteResourceGroup(getOverlord(), newGroup.getId());
        }
        deleteNewResourceAgentResourceType(newResource);

        super.afterMethod();
    }

    public void testResourceLineage() throws Exception {
        // given a resource id for the leaf resource in a resource hierarchy
        int leafResourceId = givenASampleResourceHierarchy();

        // when
        List<Resource> resourceLineage = resourceManager.getResourceLineage(leafResourceId);

        assertEquals(resourceLineage.size(), 4);

        // then
        StringBuilder stringBuilder = new StringBuilder();
        for (Resource resource : resourceLineage) {
            stringBuilder.append(resource.getName());
            if (resourceLineage.indexOf(resource) != resourceLineage.size() - 1) {
                stringBuilder.append("::");
            }
        }
        System.err.println(stringBuilder.toString());

        // cleanup the DB
        for (int i = resourceLineage.size() - 1; i >= 0; i--) {
            deleteNewResourceAgentResourceType(resourceLineage.get(i));
        }
    }

    // Make sure our application exceptions are not wrapped
    public void bz886850Test() {
        try {
            resourceManager.getResourceById(getOverlord(), 2637426);
            fail("Should have thrown a ResourceNotFoundException");
        } catch (Throwable t) {
            if (!(t instanceof ResourceNotFoundException)) {
                fail("Should have thrown a ResourceNotFoundException but got: " + t);
            }
        }
        try {
            LookupUtil.getGroupDefinitionManager().getById(3456347);
            fail("Should have thrown a GroupDefinitionNotFoundException");
        } catch (Throwable t) {
            if (!(t instanceof GroupDefinitionNotFoundException)) {
                fail("Should have thrown a GroupDefinitionNotFoundException but got: " + t);
            }
        }
        try {
            LookupUtil.getOperationManager().getOperationDefinition(getOverlord(), 3456347);
            fail("Should have thrown a OperationDefinitionNotFoundException");
        } catch (Throwable t) {
            if (!(t instanceof OperationDefinitionNotFoundException)) {
                fail("Should have thrown a OperationDefinitionNotFoundException but got: " + t);
            }
        }
        try {
            LookupUtil.getResourceTypeManager().getResourceTypeById(getOverlord(), 3456347);
            fail("Should have thrown a ResourceTypeNotFoundException");
        } catch (Throwable t) {
            if (!(t instanceof ResourceTypeNotFoundException)) {
                fail("Should have thrown a ResourceTypeNotFoundException but got: " + t);
            }
        }
        try {
            LookupUtil.getResourceGroupManager().getResourceGroup(getOverlord(), 3456347);
            fail("Should have thrown a ResourceGroupNotFoundException");
        } catch (Throwable t) {
            if (!(t instanceof ResourceGroupNotFoundException)) {
                fail("Should have thrown a ResourceGroupNotFoundException but got: " + t);
            }
        }
        try {
            SessionManager.getInstance().getSubject(3456347);
            fail("Should have thrown a SessionNotFoundException");
        } catch (Throwable t) {
            if (!(t instanceof SessionNotFoundException)) {
                fail("Should have thrown a SessionNotFoundException but got: " + t);
            }
        }
    }

    public void testAddResorceToGroup() {
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(newGroup.getId());
        criteria.fetchExplicitResources(true);
        PageList<ResourceGroup> persistedGroups = groupManager.findResourceGroupsByCriteria(getOverlord(), criteria);
        assertEquals("There should be just one group with id " + newGroup.getId(), 1,
            persistedGroups.size());
        
        // equals is based on the name of a group
        assertEquals("Persisted group should be the same as the group created in before method.", newGroup,
            persistedGroups.get(0));
        assertEquals("There should be no explicit members in the newly created group.", 0, persistedGroups.get(0)
            .getExplicitResources().size());

        // add resource to group
        groupManager.addResourcesToGroup(getOverlord(), newGroup.getId(), new int[] { newResource.getId() });
        persistedGroups = groupManager.findResourceGroupsByCriteria(getOverlord(), criteria);
        assertEquals("There should be one member in the newly created group.", 1, persistedGroups.get(0).getExplicitResources()
            .size());
    }

    public void testResourceUninventorization() {
        // partly a regression test for BZ 878117
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(newGroup.getId());
        criteria.fetchExplicitResources(true);
        PageList<ResourceGroup> persistedGroups = groupManager.findResourceGroupsByCriteria(getOverlord(), criteria);
        assertEquals("There should be just one group with id " + newGroup.getId(), 1,
            persistedGroups.size());
        assertEquals("An empty group is considered as MIXED.", GroupCategory.MIXED, persistedGroups.get(0)
            .getGroupCategory());

        // add resource to group
        groupManager.addResourcesToGroup(getOverlord(), persistedGroups.get(0).getId(), new int[] { newResource.getId() });
        persistedGroups = groupManager.findResourceGroupsByCriteria(getOverlord(), criteria);
        assertEquals("A group with just one explicit member is considered as COMPATIBLE.", GroupCategory.COMPATIBLE,
            persistedGroups.get(0).getGroupCategory());

        // now uninventorize the only resource
        resourceManager.uninventoryResource(getOverlord(), newResource.getId());
        persistedGroups = groupManager.findResourceGroupsByCriteria(getOverlord(), criteria);
        assertEquals("An empty group is considered as MIXED.", GroupCategory.MIXED, persistedGroups.get(0)
            .getGroupCategory());
    }

    public void testResourceRemovalFromGroup() {
        ResourceGroup persistedGroup = groupManager.getResourceGroup(getOverlord(), newGroup.getId());
        assertEquals("An empty group is considered as MIXED.", GroupCategory.MIXED, persistedGroup.getGroupCategory());
        
        // add resource to group
        groupManager.addResourcesToGroup(getOverlord(), persistedGroup.getId(), new int[] { newResource.getId() });
        persistedGroup = groupManager.getResourceGroup(getOverlord(), newGroup.getId());
        assertEquals("A group with just one explicit member is considered as COMPATIBLE.", GroupCategory.COMPATIBLE,
            persistedGroup.getGroupCategory());
        
        // now remove the only resource from the group
        groupManager.removeResourcesFromGroup(getOverlord(), persistedGroup.getId(), new int[] { newResource.getId() });
        persistedGroup = groupManager.getResourceGroup(getOverlord(), newGroup.getId());
        assertEquals("An empty group is considered as MIXED.", GroupCategory.MIXED, persistedGroup.getGroupCategory());     
    }

    public void testLiveAvailability() throws Exception {
        agentServiceContainer.discoveryService = Mockito.mock(DiscoveryAgentService.class);

        Mockito.when(agentServiceContainer.discoveryService.getCurrentAvailability(Mockito.any(Resource.class))).then(
            new Answer<Availability>() {
                int count = 0;

                @Override
                public Availability answer(InvocationOnMock invocation) throws Throwable {
                    Resource res = (Resource) invocation.getArguments()[0];
                    AvailabilityType avail = count++ == 0 ? AvailabilityType.DOWN : AvailabilityType.UP;
                    return new Availability(res, avail);
                }
            });

        AlertDefinition alertDef = new AlertDefinition();

        AlertCondition cond = new AlertCondition(alertDef, AlertConditionCategory.AVAILABILITY);
        cond.setName(AlertConditionOperator.AVAIL_GOES_UP.name());
        alertDef.setName("liveAvailabilityTestAlert");
        alertDef.setResource(newResource);
        alertDef.setPriority(AlertPriority.MEDIUM);
        alertDef.setRecoveryId(0);
        alertDef.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
        alertDef.setConditions(Collections.singleton(cond));
        alertDef.setEnabled(true);
        alertDef.setConditionExpression(BooleanExpression.ALL);

        AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
        alertDefinitionManager.createAlertDefinitionInNewTransaction(getOverlord(), alertDef, newResource.getId(), true);

        //obvious, right? This needs to be done for the alert subsystem to become aware of the new def
        LookupUtil.getAlertConditionCacheManager().reloadAllCaches();

        ResourceCriteria crit = new ResourceCriteria();
        crit.addFilterId(newResource.getId());
        crit.fetchCurrentAvailability(true);
        Resource fromDb = resourceManager.findResourcesByCriteria(getOverlord(), crit).get(0);

        assertEquals(AvailabilityType.UNKNOWN, fromDb.getCurrentAvailability().getAvailabilityType());

        //ask for the live avail - the mock agent response will return "DOWN" the first time
        resourceManager.getLiveResourceAvailability(getOverlord(), newResource.getId());

        //check that the resource changed its avail in the db
        fromDb = resourceManager.getResource(getOverlord(), newResource.getId());
        assertEquals(AvailabilityType.DOWN, fromDb.getCurrentAvailability().getAvailabilityType());

        //ask for the live avail - the mock agent response will return "UP" the second time
        resourceManager.getLiveResourceAvailability(getOverlord(), newResource.getId());

        // wait for our JMS messages to process and see if we get any alerts
        Thread.sleep(3000);

        //check that the resource changed its avail in the db
        fromDb = resourceManager.getResource(getOverlord(), newResource.getId());
        assertEquals(AvailabilityType.UP, fromDb.getCurrentAvailability().getAvailabilityType());

        // wait for our JMS messages to process and see if we get any alerts
        Thread.sleep(3000);

        //check that the alert fired when going from DOWN to UP
        AlertCriteria aCrit = new AlertCriteria();
        aCrit.addFilterResourceIds(newResource.getId());

        List<Alert> alerts = LookupUtil.getAlertManager().findAlertsByCriteria(getOverlord(), aCrit);
        assertEquals("Unexpected number of alerts on the resource.", 1, alerts.size());
    }

    private int givenASampleResourceHierarchy() throws NotSupportedException, SystemException {
        getTransactionManager().begin();

        int leafResourceId = 0;
        try {
            ResourceType platformType = createResourceType("platform" + System.currentTimeMillis(), "test", null,
                ResourceCategory.PLATFORM);
            ResourceType appserverType = createResourceType("jboss AS 5" + System.currentTimeMillis(), "jbossas5",
                platformType, ResourceCategory.SERVER);
            ResourceType jvmType = createResourceType("JVM" + System.currentTimeMillis(), "jbossas5", appserverType,
                ResourceCategory.SERVICE);
            ResourceType memType = createResourceType("Memory Subsystem" + System.currentTimeMillis(), "jbossas5",
                jvmType, ResourceCategory.SERVICE);
            Agent agent = new Agent("agent" + System.currentTimeMillis(), "host" + System.currentTimeMillis(), 1, "",
                "token" + System.currentTimeMillis());
            em.persist(agent);
            em.flush();

            Resource platform = createResource(platformType, agent, "platformKey" + System.currentTimeMillis(),
                "host.dev.corp", null);
            Resource appserver = createResource(appserverType, agent, "JEAP" + System.currentTimeMillis(),
                "JBOSS EAP 5.1.1", platform);
            Resource jvm = createResource(jvmType, agent, "jvm" + System.currentTimeMillis(), "JBoss AS JVM", appserver);
            Resource memSubystem = createResource(memType, agent, "mem" + System.currentTimeMillis(),
                "Memory Subsystem", jvm);
            leafResourceId = memSubystem.getId();

            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                System.out.println("CANNOT Prepare TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }
        }
        return leafResourceId;
    }

    private Resource createResource(ResourceType platformType, Agent agent, String resourceKey, String resourceName,
        Resource parent) {
        Resource resource = new Resource(resourceKey, resourceName, platformType);
        resource.setUuid(UUID.randomUUID().toString());
        resource.setAgent(agent);
        resource.setParentResource(parent);
        em.persist(resource);
        return resource;
    }

    private ResourceType createResourceType(String name, String pluginName, ResourceType parentResourceType,
        ResourceCategory resourceCategory) {
        ResourceType platformType = new ResourceType(name, pluginName, resourceCategory, parentResourceType);
        ResourceType resourceType = platformType;
        em.persist(resourceType);
        return resourceType;
    }

    private Resource createNewResourceWithNewType() throws Exception {
        getTransactionManager().begin();

        Resource resource;

        try {
            ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                ResourceCategory.PLATFORM, null);

            em.persist(resourceType);

            resource = new Resource("reskey" + System.currentTimeMillis(), "resname", resourceType);
            setUpAgent(resource);

            resource.setUuid("" + new Random().nextInt());
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
            em.persist(resource);

            createServerIdentity();
        } catch (Exception e) {
            System.out.println("CANNOT PREPARE TEST: " + e);
            getTransactionManager().rollback();
            throw e;
        }

        getTransactionManager().commit();

        return resource;
    }

    private ResourceGroup createNewGroup() {
        ResourceGroup group = new ResourceGroup("testGroup");
        groupManager.createResourceGroup(getOverlord(), group);
        return group;
    }

    private void deleteNewResourceAgentResourceType(Resource resource) throws Exception {
        if (resource != null) {
            getTransactionManager().begin();

            try {
                Resource res = em.find(Resource.class, resource.getId());
                System.out.println("Removing " + res + "...");
                List<Integer> deletedIds = resourceManager.uninventoryResource(getOverlord(), res.getId());
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.uninventoryResourceAsyncWork(getOverlord(), deletedResourceId);
                }
                em.flush();

                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                System.out.println("Removing " + type + "...");
                em.remove(type);
                em.flush();

                // NOTE: No need to remove the Agent entity, since uninventorying the platform will do that automatically.

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }
            }
        }
    }
}
