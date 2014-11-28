/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJBException;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceStatus;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.StandardServerPluginService;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A unit test for {@link org.rhq.enterprise.server.resource.ResourceFactoryManagerBean}.
 *
 * @author Jason Dobies
 */
public class ResourceFactoryManagerBeanTest extends AbstractEJB3Test {

    private static final String TEST_PLUGIN_NAME = "TestPlugin";

    // Attributes  --------------------------------------------

    private ResourceFactoryManagerLocal resourceFactoryManager;
    private ResourceManagerLocal resourceManager;
    private DiscoveryBossLocal discoveryBoss;
    private Subject overlord;

    private MockResourceFactoryAgentService mockAgentService = new MockResourceFactoryAgentService();

    // Populated during setupResourceEnvironment
    private Resource parentResource;
    private ResourceType parentResourceType;
    private ResourceType contentBackedChildResourceType;
    private ResourceType configBackedChildResourceType1;
    private ResourceType configBackedChildResourceType2;
    private PackageType packageType;

    // Setup  --------------------------------------------

    @Override
    protected void beforeMethod() throws Exception {
        resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        resourceManager = LookupUtil.getResourceManager();
        discoveryBoss = LookupUtil.getDiscoveryBoss();
        overlord = LookupUtil.getSubjectManager().getOverlord();

        prepareScheduler();
        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.resourceFactoryService = mockAgentService;

        //the server plugins are in play when package types are involved
        StandardServerPluginService serverPluginService = new StandardServerPluginService(getTempDir());
        prepareCustomServerPluginService(serverPluginService);
        serverPluginService.startMasterPluginContainer();

        setupResourceEnvironment();
    }

    @Override
    protected void afterMethod() throws Exception {
        unprepareForTestAgents();
        unprepareScheduler();
        unprepareServerPluginService();

        teardownResourceEnvironment();
    }

    // Test Cases  --------------------------------------------

    @Test
    public void createResourceViaConfiguration() throws Exception {
        // Setup
        mockAgentService.setCreateReturnStatus(CreateResourceStatus.SUCCESS);

        // Assemble call parameters
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Configuration resourceConfiguration = new Configuration();
        resourceConfiguration.put(new PropertySimple("property1", "value1"));

        // Test
        resourceFactoryManager.createResource(overlord, parentResource.getId(), configBackedChildResourceType1.getId(),
            "newResource", null, resourceConfiguration);

        // Verify
        try {
            getTransactionManager().begin();

            Resource parent = em.find(Resource.class, parentResource.getId());
            List<CreateResourceHistory> historyList = parent.getCreateChildResourceRequests();

            assert historyList.size() == 1 : "Incorrect number of children found. Expected: 1, Found: "
                + historyList.size();

            CreateResourceHistory historyItem = historyList.get(0);

            assert historyItem.getStatus() == CreateResourceStatus.SUCCESS : "Incorrect status for history item. Expected: Success, Found: "
                + historyItem.getStatus();
            assert historyItem.getNewResourceKey().equals("mockResourceKey") : "Incorrect resource key for history item. Expected: mockResourceKey, Found: "
                + historyItem.getNewResourceKey();
            assert historyItem.getErrorMessage() == null : "Error message found for successful call";
            assert historyItem.getConfiguration() != null : "Null configuration found for history item";
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void createResourceViaConfigurationFailure() throws Exception {
        // Setup
        mockAgentService.setCreateReturnStatus(CreateResourceStatus.FAILURE);

        // Assemble call parameters
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Configuration resourceConfiguration = new Configuration();
        resourceConfiguration.put(new PropertySimple("property1", "value1"));

        // Test
        resourceFactoryManager.createResource(overlord, parentResource.getId(), configBackedChildResourceType1.getId(),
            "newResource", null, resourceConfiguration);

        // Verify
        try {
            getTransactionManager().begin();

            Resource parent = em.find(Resource.class, parentResource.getId());
            List<CreateResourceHistory> historyList = parent.getCreateChildResourceRequests();

            assert historyList.size() == 1 : "Incorrect number of children found. Expected: 1, Found: "
                + historyList.size();

            CreateResourceHistory historyItem = historyList.get(0);

            assert historyItem.getStatus() == CreateResourceStatus.FAILURE : "Incorrect status for history item. Expected: Failure, Found: "
                + historyItem.getStatus();
            assert historyItem.getErrorMessage().equals("errorMessage") : "Incorrect error message for history item. Expected: errorMessage, Found: "
                + historyItem.getErrorMessage();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void createSingletonResourceViaConfiguration() throws Exception {
        // Setup
        mockAgentService.setCreateReturnStatus(CreateResourceStatus.SUCCESS);

        // Assemble call parameters
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Configuration resourceConfiguration = new Configuration();
        resourceConfiguration.put(new PropertySimple("property1", "value1"));

        // Test
        CreateResourceHistory history1 = resourceFactoryManager.createResource(overlord, parentResource.getId(),
            configBackedChildResourceType2.getId(), "newResource", null, resourceConfiguration);

        // Verify
        try {
            getTransactionManager().begin();

            Resource parent = em.find(Resource.class, parentResource.getId());
            List<CreateResourceHistory> historyList = parent.getCreateChildResourceRequests();

            assert historyList.size() == 1 : "Incorrect number of children found. Expected: 1, Found: "
                + historyList.size();

            CreateResourceHistory historyItem = historyList.get(0);

            assert historyItem.getStatus() == CreateResourceStatus.SUCCESS : "Incorrect status for history item. Expected: Success, Found: "
                + historyItem.getStatus();
            assert historyItem.getNewResourceKey().equals("mockResourceKey") : "Incorrect resource key for history item. Expected: mockResourceKey, Found: "
                + historyItem.getNewResourceKey();
            assert historyItem.getErrorMessage() == null : "Error message found for successful call";
            assert historyItem.getConfiguration() != null : "Null configuration found for history item";
        } finally {
            getTransactionManager().rollback();
        }

        // Invoke the same callbacks the plugin container would to actually commit the new Resource to inventory.
        resourceFactoryManager.completeCreateResource(new CreateResourceResponse(history1.getId(), "newResource",
            "key", CreateResourceStatus.SUCCESS, null, new Configuration()));
        Resource resource = new Resource(1000000);
        resource.setUuid(UUID.randomUUID().toString());
        resource.setResourceType(configBackedChildResourceType2);
        long randomLong = UUID.randomUUID().getLeastSignificantBits();
        resource.setResourceKey(String.valueOf("key-" + randomLong));
        resource.setName("name-" + randomLong);
        resource.setParentResource(parentResource);
        discoveryBoss.addResource(resource, overlord.getId());

        // Now try to create another Resource of the same singleton type - this should fail.
        try {
            CreateResourceHistory history2 = resourceFactoryManager.createResource(overlord, parentResource.getId(),
                configBackedChildResourceType2.getId(), "newResource2", null, resourceConfiguration);
            fail("Creating a singleton that already existed succeeded: " + history2);
        } catch (EJBException e) {
            assertEquals(String.valueOf(e.getCause()), RuntimeException.class, e.getCause().getClass());
            assertTrue(String.valueOf(e.getCause()), e.getCause().getMessage().contains("singleton"));
        }
    }

    @Test
    public void createSingletonResourceViaPackage() throws Exception {
        // Setup
        mockAgentService.setCreateReturnStatus(CreateResourceStatus.SUCCESS);

        // Assemble call parameters
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        String packageName = "testDataResourcePackage";
        String packageVersion = "1.0.0";
        int architectureId = 1; // easier than loading a specific architecture from the DB, just use the first one

        Configuration deploymentTimeConfiguration = new Configuration();
        deploymentTimeConfiguration.put(new PropertySimple("testProperty", "testValue"));

        // This file should be in the classpath, so use it for the artifact content
        URL contentResource = this.getClass().getClassLoader().getResource("test-scheduler.properties");
        assert contentResource != null : "Could not load test-scheduler.properties as package content";

        InputStream packageInputStream = contentResource.openStream();

        // Test
        CreateResourceHistory history1 = resourceFactoryManager.createResource(overlord, parentResource.getId(),
            contentBackedChildResourceType.getId(), "newResource", null, packageName, packageVersion, architectureId,
            deploymentTimeConfiguration, packageInputStream);

        // Verify
        try {
            getTransactionManager().begin();

            Resource parent = em.find(Resource.class, parentResource.getId());
            List<CreateResourceHistory> historyList = parent.getCreateChildResourceRequests();

            assert historyList.size() == 1 : "Incorrect number of children found. Expected: 1, Found: "
                + historyList.size();

            CreateResourceHistory historyItem = historyList.get(0);

            assert historyItem.getStatus() == CreateResourceStatus.SUCCESS : "Incorrect status for history item. Expected: Success, Found: "
                + historyItem.getStatus();
            assert historyItem.getNewResourceKey().equals("mockResourceKey") : "Incorrect resource key for history item. Expected: mockResourceKey, Found: "
                + historyItem.getNewResourceKey();
            assert historyItem.getErrorMessage() == null : "Error message found for successful call";
        } finally {
            getTransactionManager().rollback();
        }

        // Invoke the same callbacks the plugin container would to actually commit the new Resource to inventory.
        resourceFactoryManager.completeCreateResource(new CreateResourceResponse(history1.getId(), "newResource",
            "key", CreateResourceStatus.SUCCESS, null, new Configuration()));
        Resource resource = new Resource(2000000);
        resource.setUuid(UUID.randomUUID().toString());
        resource.setResourceType(contentBackedChildResourceType);
        long randomLong = UUID.randomUUID().getLeastSignificantBits();
        resource.setResourceKey(String.valueOf("key-" + randomLong));
        resource.setName("name-" + randomLong);
        resource.setParentResource(parentResource);
        discoveryBoss.addResource(resource, overlord.getId());

        // Now try to create another Resource of the same singleton type - this should fail.
        try {
            CreateResourceHistory history2 = resourceFactoryManager.createResource(overlord, parentResource.getId(),
                contentBackedChildResourceType.getId(), "newResource2", null, packageName, packageVersion,
                architectureId, deploymentTimeConfiguration, packageInputStream);
            fail("Creating a singleton that already existed succeeded: " + history2);
        } catch (EJBException e) {
            assertEquals(String.valueOf(e.getCause()), RuntimeException.class, e.getCause().getClass());
            assertTrue(String.valueOf(e.getCause()), e.getCause().getMessage().contains("singleton"));
        }
    }

    @Test
    public void deleteResource() throws Exception {
        // Setup
        mockAgentService.setDeleteReturnStatus(DeleteResourceStatus.SUCCESS);
        Resource deleteMe = addResourceToParent();

        assert deleteMe != null : "Child resource to be deleted was not correctly added in the first place";

        // Assemble call parameters
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // Test
        resourceFactoryManager.deleteResource(overlord, deleteMe.getId());

        // Verify
        try {
            getTransactionManager().begin();

            Resource parent = em.find(Resource.class, parentResource.getId());
            Set<Resource> childResources = parent.getChildResources();

            assert childResources.size() == 0 : "Child resource not deleted";

        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void deleteResourceFailure() throws Exception {
        // Setup
        mockAgentService.setDeleteReturnStatus(DeleteResourceStatus.FAILURE);
        Resource deleteMe = addResourceToParent();

        assert deleteMe != null : "Child resource to be deleted was not correctly added in the first place";

        // Assemble call parameters
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // Test
        resourceFactoryManager.deleteResource(overlord, deleteMe.getId());

        try {
            getTransactionManager().begin();

            Resource parent = em.find(Resource.class, parentResource.getId());
            Set<Resource> childResources = parent.getChildResources();

            assert childResources.size() == 1 : "Child resource not found on the parent";

            Resource deletedResource = childResources.iterator().next();

            assert deletedResource.getInventoryStatus() == InventoryStatus.COMMITTED : "Inventory status for deleted resource was incorrect. Expected: Committed, Found: "
                + deletedResource.getInventoryStatus();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void createHistory() {
        // Setup
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        // Have to call getMap() on the configuration instances to trigger lazy instantiation of the properties map
        Configuration resourceConfiguration;

        resourceConfiguration = new Configuration();
        resourceConfiguration.getMap();
        resourceFactoryManager.createResource(overlord, parentResource.getId(), configBackedChildResourceType1.getId(),
            "resource1", (Configuration) null, resourceConfiguration);

        resourceConfiguration = new Configuration();
        resourceConfiguration.getMap();
        resourceFactoryManager.createResource(overlord, parentResource.getId(), configBackedChildResourceType1.getId(),
            "resource2", (Configuration) null, resourceConfiguration);

        resourceConfiguration = new Configuration();
        resourceConfiguration.getMap();
        resourceFactoryManager.createResource(overlord, parentResource.getId(), configBackedChildResourceType1.getId(),
            "resource3", (Configuration) null, resourceConfiguration);

        // Verify
        int numRequestsInHistory = resourceFactoryManager.getCreateChildResourceHistoryCount(parentResource.getId(),
            null, null);

        assert numRequestsInHistory == 3 : "Incorrect history count. Expected: 3, Found: " + numRequestsInHistory;

        PageControl pageControl = new PageControl(0, 10000);
        pageControl.initDefaultOrderingField("createdResourceName");
        PageList<CreateResourceHistory> historyList = resourceFactoryManager.findCreateChildResourceHistory(overlord,
            parentResource.getId(), null, null, pageControl);

        assert historyList.getTotalSize() == 3 : "Incorrect number of entries in page list. Expected: 3, Found: "
            + historyList.getTotalSize();

        assert historyList.get(0).getCreatedResourceName().equals("resource1") : "History entry 1 is invalid";
        assert historyList.get(1).getCreatedResourceName().equals("resource2") : "History entry 2 is invalid";
        assert historyList.get(2).getCreatedResourceName().equals("resource3") : "History entry 3 is invalid";
    }

    @Test
    public void deleteHistory() throws Exception {
        // Setup
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Resource resource1 = addResourceToParent();
        Resource resource2 = addResourceToParent();
        Resource resource3 = addResourceToParent();

        // Delete each one, which should add a history item
        resourceFactoryManager.deleteResource(overlord, resource1.getId());
        resourceFactoryManager.deleteResource(overlord, resource2.getId());
        resourceFactoryManager.deleteResource(overlord, resource3.getId());

        // Verify
        int numRequestsInHistory = resourceFactoryManager.getDeleteChildResourceHistoryCount(parentResource.getId(),
            null, null);

        assert numRequestsInHistory == 3 : "Incorrect history count. Expected: 3, Found: " + numRequestsInHistory;

        PageControl pageControl = new PageControl(0, 10000);
        pageControl.initDefaultOrderingField("drh.id");
        PageList<DeleteResourceHistory> historyList = resourceFactoryManager.findDeleteChildResourceHistory(overlord,
            parentResource.getId(), null, null, pageControl);

        assert historyList.getTotalSize() == 3 : "Incorrect number of entries in page list. Expected: 3, Found: "
            + historyList.getTotalSize();

        assert historyList.get(0).getResourceKey().equals(resource1.getResourceKey()) : "History entry 1 is invalid";
        assert historyList.get(1).getResourceKey().equals(resource2.getResourceKey()) : "History entry 2 is invalid";
        assert historyList.get(2).getResourceKey().equals(resource3.getResourceKey()) : "History entry 3 is invalid";
    }

    @Test
    public void getHistoryItem() throws Exception {
        // Setup
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Configuration resourceConfiguration = new Configuration();
        resourceConfiguration.getMap();
        resourceFactoryManager.createResource(overlord, parentResource.getId(), configBackedChildResourceType1.getId(),
            "NewResource", null, resourceConfiguration);
        PageList<CreateResourceHistory> historyList = resourceFactoryManager.findCreateChildResourceHistory(overlord,
            parentResource.getId(), null, null, new PageControl(0, 1000));
        int historyItemId = historyList.get(0).getId();

        // Test
        CreateResourceHistory historyItem = resourceFactoryManager.getCreateHistoryItem(historyItemId);

        // Verify
        assert historyItem != null : "Null history item returned from call";
        assert historyItem.getCreatedResourceName().equals("NewResource");
    }

    // Private  --------------------------------------------

    /**
     * Creates a test resource in the database for testing purposes.
     *
     * @return persisted but detached resource
     *
     * @throws Exception if anything goes wrong
     */
    private void setupResourceEnvironment() throws Exception {
        getTransactionManager().begin();

        try {
            // Create parent resource type
            parentResourceType = new ResourceType("platform-" + System.currentTimeMillis(), TEST_PLUGIN_NAME,
                ResourceCategory.PLATFORM, null);
            em.persist(parentResourceType);

            // Create child resource type to parent. Artifact type lives under this resource type.
            contentBackedChildResourceType = new ResourceType("service-" + System.currentTimeMillis(),
                TEST_PLUGIN_NAME, ResourceCategory.SERVICE, parentResourceType);
            contentBackedChildResourceType.setCreateDeletePolicy(CreateDeletePolicy.BOTH);
            contentBackedChildResourceType.setCreationDataType(ResourceCreationDataType.CONTENT);
            contentBackedChildResourceType.setSingleton(true);
            em.persist(contentBackedChildResourceType);

            // Create artifact type under child resource type that is marked as the creation artifact type
            packageType = new PackageType();
            packageType.setName("artifact-" + System.currentTimeMillis());
            packageType.setDescription("");
            packageType.setCategory(PackageCategory.DEPLOYABLE);
            packageType.setDisplayName("TestResourceArtifact");
            packageType.setCreationData(true);
            packageType.setResourceType(contentBackedChildResourceType);
            em.persist(packageType);

            // Link artifact type and child resource type
            contentBackedChildResourceType.addPackageType(packageType);

            configBackedChildResourceType1 = new ResourceType("service1-" + System.currentTimeMillis(),
                TEST_PLUGIN_NAME, ResourceCategory.SERVICE, parentResourceType);
            configBackedChildResourceType1.setCreateDeletePolicy(CreateDeletePolicy.BOTH);
            configBackedChildResourceType1.setCreationDataType(ResourceCreationDataType.CONFIGURATION);
            em.persist(configBackedChildResourceType1);

            configBackedChildResourceType2 = new ResourceType("service2-" + System.currentTimeMillis(),
                TEST_PLUGIN_NAME, ResourceCategory.SERVICE, parentResourceType);
            configBackedChildResourceType2.setCreateDeletePolicy(CreateDeletePolicy.BOTH);
            configBackedChildResourceType2.setCreationDataType(ResourceCreationDataType.CONFIGURATION);
            configBackedChildResourceType2.setSingleton(true);
            em.persist(configBackedChildResourceType2);

            // Create parent resource off of which to hang created resources
            parentResource = new Resource("parent" + System.currentTimeMillis(), "name", parentResourceType);
            parentResource.setUuid("" + new Random().nextInt());
            em.persist(parentResource);

        } catch (Exception e) {
            System.out.println(e);
            getTransactionManager().rollback();
            throw e;
        }

        getTransactionManager().commit();
    }

    /**
     * Deletes a previously persisted resource from the database.
     *
     * @throws Exception if anything goes wrong
     */
    private void teardownResourceEnvironment() throws Exception {
        if (parentResource != null) {
            resourceManager.uninventoryResource(overlord, parentResource.getId());
            List<Integer> deletedIds = resourceManager.findResourcesMarkedForAsyncDeletion(overlord);
            for (Integer deletedResourceId : deletedIds) {
                resourceManager.uninventoryResourceAsyncWork(overlord, deletedResourceId);
            }

            getTransactionManager().begin();

            try {
                // Remove the children first.
                ResourceType deleteMeType = em.find(ResourceType.class, contentBackedChildResourceType.getId());
                em.remove(deleteMeType);

                deleteMeType = em.find(ResourceType.class, configBackedChildResourceType1.getId());
                em.remove(deleteMeType);

                deleteMeType = em.find(ResourceType.class, configBackedChildResourceType2.getId());
                em.remove(deleteMeType);

                deleteMeType = em.find(ResourceType.class, parentResourceType.getId());
                em.remove(deleteMeType);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println(e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }

                throw e;
            }
        }
    }

    /**
     * Creates a new child resource on the test class' parent resource.
     *
     * @return the newly created resource
     *
     * @throws Exception if anything goes wrong
     */
    private Resource addResourceToParent() throws Exception {
        Resource resource = null;

        getTransactionManager().begin();
        try {
            resource = new Resource("child" + System.currentTimeMillis(), "name", parentResourceType);
            resource.setUuid("" + new Random().nextInt());
            resource.setParentResource(parentResource);
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
            resource.setResourceKey("" + new Random().nextInt());
            em.persist(resource);
        } catch (Exception e) {
            System.out.println(e);
            getTransactionManager().rollback();
        }

        getTransactionManager().commit();

        return resource;
    }

    // Inner Classes  --------------------------------------------

    /**
     * Mocks out the call to the agent. Different results can be achieved by manipulating the attributes on the instance
     * that is registered with the server communications service.
     */
    private class MockResourceFactoryAgentService implements ResourceFactoryAgentService {
        // Attributes  --------------------------------------------

        private CreateResourceStatus createReturnStatus = CreateResourceStatus.SUCCESS;

        private DeleteResourceStatus deleteReturnStatus = DeleteResourceStatus.SUCCESS;

        // Public  --------------------------------------------

        /**
         * Indicates what the result of a call to createResource should be.
         *
         * @param createReturnStatus indicates the type of response to send
         */
        public void setCreateReturnStatus(CreateResourceStatus createReturnStatus) {
            this.createReturnStatus = createReturnStatus;
        }

        /**
         * Indicates what the result of a call to deleteResource should be.
         *
         * @param deleteReturnStatus indicates the type of response to send
         */
        public void setDeleteReturnStatus(DeleteResourceStatus deleteReturnStatus) {
            this.deleteReturnStatus = deleteReturnStatus;
        }

        // ResourceFactoryAgentService Implementation  --------------------------------------------

        public void createResource(CreateResourceRequest request) throws PluginContainerException {
            CreateResourceResponse response = null;

            switch (createReturnStatus) {
            case SUCCESS: {
                response = new CreateResourceResponse(request.getRequestId(), "mockResourceName", "mockResourceKey",
                    CreateResourceStatus.SUCCESS, null, request.getResourceConfiguration());
                break;
            }

            case FAILURE: {
                response = new CreateResourceResponse(request.getRequestId(), null, null, CreateResourceStatus.FAILURE,
                    "errorMessage", request.getResourceConfiguration());
                break;
            }
            }

            ResourceFactoryManagerBeanTest.this.resourceFactoryManager.completeCreateResource(response);
        }

        public void deleteResource(DeleteResourceRequest request) throws PluginContainerException {
            DeleteResourceResponse response = null;

            switch (deleteReturnStatus) {
            case SUCCESS: {
                response = new DeleteResourceResponse(request.getRequestId(), request.getResourceId(),
                    deleteReturnStatus, null);
                break;
            }

            case FAILURE: {
                response = new DeleteResourceResponse(request.getRequestId(), request.getResourceId(),
                    deleteReturnStatus, "errorMessage");
                break;
            }
            }

            ResourceFactoryManagerBeanTest.this.resourceFactoryManager.completeDeleteResourceRequest(response);
        }

        public CreateResourceResponse executeCreateResourceImmediately(CreateResourceRequest request)
            throws PluginContainerException {
            return null;
        }

        public DeleteResourceResponse executeDeleteResourceImmediately(DeleteResourceRequest request)
            throws PluginContainerException {
            return null;
        }
    }

}