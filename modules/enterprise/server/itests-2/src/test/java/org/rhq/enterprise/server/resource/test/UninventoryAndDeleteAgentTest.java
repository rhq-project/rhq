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
package org.rhq.enterprise.server.resource.test;

import java.util.HashMap;
import java.util.Random;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.metadata.test.UpdatePluginMetadataTestBase;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Testing the deletion of agents and uninventoring their resources, if there are any.
 * This is testing BZ 849711.
 */
@Test
public class UninventoryAndDeleteAgentTest extends UpdatePluginMetadataTestBase {
    private AgentManagerLocal agentManager;
    private ResourceGroupManagerLocal groupManager;
    private ResourceGroup newGroup;
    private ResourceType platformType;

    @Override
    protected void beforeMethod() throws Exception {
        super.beforeMethod();
        platformType = createPlatformResourceType();
        groupManager = LookupUtil.getResourceGroupManager();
        newGroup = createNewGroup();
        agentManager = LookupUtil.getAgentManager();
    }

    @Override
    protected void afterMethod() throws Exception {
        if (newGroup != null) {
            groupManager.deleteResourceGroup(getOverlord(), newGroup.getId());
        }
        cleanupResourceType(platformType.getName());
        super.afterMethod();
    }

    public void testDeletingAgents() throws Exception {
        // create three agents:
        // 1) agent is the only thing registered - has no inventory yet
        // 2) agent's platform is NEW, not yet committed to inventory yet (it is in the discovery queue)
        // 3) agent's platform is COMMITTED in inventory
        Agent agentRegistered = createOnlyAgent(1);
        HashMap<Agent, Resource> agentPlatNew = createAgentWithResource(2, InventoryStatus.NEW);
        HashMap<Agent, Resource> agentPlatCommitted = createAgentWithResource(3, InventoryStatus.COMMITTED);

        Agent agentNew = agentPlatNew.keySet().iterator().next();
        Agent agentCommitted = agentPlatCommitted.keySet().iterator().next();
        
        // just for some complexity - we'll want to add our committed resource to a group
        Resource committedResource = agentPlatCommitted.values().iterator().next();
        groupManager.addResourcesToGroup(getOverlord(), newGroup.getId(), new int[] { committedResource.getId() });

        // delete our agents
        resourceManager.uninventoryAllResourcesByAgent(getOverlord(), agentRegistered);
        resourceManager.uninventoryAllResourcesByAgent(getOverlord(), agentNew);
        resourceManager.uninventoryAllResourcesByAgent(getOverlord(), agentCommitted);
        
        assert null == agentManager.getAgentByID(agentRegistered.getId());
        assert null == agentManager.getAgentByID(agentNew.getId());
        assert null == agentManager.getAgentByID(agentCommitted.getId());

        try {
            Resource doomed = resourceManager.getResourceById(getOverlord(), committedResource.getId());
            assert doomed.getAgent() == null : "Resource should not have an agent attached, it should have been uninventoried";
            assert doomed.getInventoryStatus() == InventoryStatus.UNINVENTORIED : "Should have been uninventoried";
        } catch (ResourceNotFoundException rnfe) {
            // this could happen if the quartz job already purged the uninventoried resource; test is success if this happens
        }

        return;
    }

    private ResourceType createPlatformResourceType() throws Exception {
        ResourceType resourceType;
        getTransactionManager().begin();
        try {
            resourceType = new ResourceType("DeleteAgentTest-PlatType" + System.currentTimeMillis(), PLUGIN_NAME,
                ResourceCategory.PLATFORM, null);
            em.persist(resourceType);
        } catch (Exception e) {
            System.out.println("CANNOT PREPARE TEST: " + e);
            getTransactionManager().rollback();
            throw e;
        }
        getTransactionManager().commit();
        return resourceType;
    }

    private Agent createOnlyAgent(int index) throws Exception {
        Agent agent;
        getTransactionManager().begin();
        try {
            agent = new Agent("DeleteAgentTest-Agent" + index, "testaddr", 16163 + index, "", "testtoken" + index);
            em.persist(agent);
            em.flush();
        } catch (Exception e) {
            System.out.println("CANNOT PREPARE TEST: " + e);
            getTransactionManager().rollback();
            throw e;
        }
        getTransactionManager().commit();
        return agent;
    }

    private HashMap<Agent, Resource> createAgentWithResource(int index, InventoryStatus invStatus) throws Exception {
        HashMap<Agent, Resource> ret = new HashMap<Agent, Resource>(1);

        Agent agent = createOnlyAgent(index);
        Resource resource;

        getTransactionManager().begin();
        try {
            resource = new Resource("DeleteAgentTest-Res" + index, "DeleteAgentTest-Res" + index, platformType);
            resource.setUuid("" + new Random().nextInt());
            resource.setAgent(agent);
            resource.setInventoryStatus(invStatus);
            em.persist(resource);
        } catch (Exception e) {
            System.out.println("CANNOT PREPARE TEST: " + e);
            getTransactionManager().rollback();
            throw e;
        }
        getTransactionManager().commit();

        ret.put(agent, resource);
        return ret;
    }

    private ResourceGroup createNewGroup() {
        ResourceGroup group = new ResourceGroup("DeleteAgentTest-Group");
        groupManager.createResourceGroup(getOverlord(), group);
        return group;
    }
}
