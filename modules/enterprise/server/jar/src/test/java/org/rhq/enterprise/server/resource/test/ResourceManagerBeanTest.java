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

import java.util.List;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.discovery.DiscoveryServerServiceImpl;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.metadata.test.UpdateSubsytemTestBase;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test for {@link ResourceManagerLocal} SLSB.
 */
@Test
public class ResourceManagerBeanTest extends UpdateSubsytemTestBase {
    private ResourceManagerLocal resourceManager;
    private Subject superuser;
    private Resource newResource;

    TestServerCommunicationsService agentServiceContainer;

    @Override
    @BeforeClass
    public void beforeClass() {
        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.measurementService = new MockAgentService();

        prepareScheduler();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        resourceManager = LookupUtil.getResourceManager();
        superuser = LookupUtil.getSubjectManager().getOverlord();
        newResource = createNewResource();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        deleteNewResource(newResource);
    }

    public void testResourceErrors() {
        ResourceError error;
        List<ResourceError> errors;
        DiscoveryServerServiceImpl serverService = new DiscoveryServerServiceImpl();

        errors = resourceManager.getResourceErrors(superuser, newResource.getId(),
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        assert errors.size() == 0;

        error = new ResourceError(newResource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, "test summary",
            "test detail", 12345);

        // simulate the agent notifying the server about an error
        // this will exercise the addResourceError in the SLSB
        serverService.setResourceError(error);
        errors = resourceManager.getResourceErrors(superuser, newResource.getId(),
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
        errors = resourceManager.getResourceErrors(superuser, newResource.getId(),
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        assert errors.size() == 1;
        error = errors.get(0);
        assert error.getId() > 0;
        assert error.getSummary().equals("another summary");
        assert error.getDetail().equals("another detail");
        assert error.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
        assert error.getTimeOccurred() == 567890;

        resourceManager.deleteResourceError(superuser, error.getId());
        errors = resourceManager.getResourceErrors(superuser, newResource.getId(),
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        assert errors.size() == 0;
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                    ResourceCategory.PLATFORM, null);

                em.persist(resourceType);

                Agent agent = new Agent("testagent", "testaddress", 1, "", "testtoken");
                em.persist(agent);
                em.flush();

                resource = new Resource("reskey" + System.currentTimeMillis(), "resname", resourceType);
                resource.setAgent(agent);
                em.persist(resource);
            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (resource != null) {
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            try {
                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Resource res = em.find(Resource.class, resource.getId());
                Agent agent = em.find(Agent.class, resource.getAgent().getId());

                List<Integer> deletedIds = resourceManager.deleteResource(superuser, res.getId());
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.deleteSingleResourceInNewTransaction(superuser, deletedResourceId);
                }
                em.remove(agent);
                em.remove(type);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }
            } finally {
                em.close();
            }
        }
    }
}