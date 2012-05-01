/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.inventory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;

/**
 * Plugin container manager that is responsible for handling resource factory operations (create and delete resource).
 * This manager implements the remoted <code>ResourceFactoryAgentService</code> interface.
 *
 * @author Jason Dobies
 */
public class ResourceFactoryManager extends AgentService implements ContainerService, ResourceFactoryAgentService {

    // This used to be a single value fixed at 60 seconds. But create and delete actions can very well exceed 1 minute
    // depending on the type of resource being created, or perhaps graceful shutdown of a resource being deleted. So, 
    // allow the timeout value to be overriden by editing rhq-agent-env.sh with new -D settings. Also, create separate
    // timeouts for create and delete, as their execution times really are not related. The properties are set in
    // milliseconds:
    //    rhq.agent.plugins.facet.create-child-resource.timeout
    //    rhq.agent.plugins.facet.delete-resource.timeout
    //
    // The default is still 60s.  Note that increasing this value affects all types across all plugins and should not
    // be raised unless a successful create or delete action requires the higher limit.  Realize that while a
    // create or delete is in progress that the resource is write-locked, so no other actions can take place (like
    // metric collection).
    //
    private static final int FACET_CREATE_TIMEOUT;
    private static final int FACET_DELETE_TIMEOUT;

    static {
        int timeout;
        try {
            timeout = Integer.parseInt(System.getProperty("rhq.agent.plugins.facet.create-child-resource.timeout",
                "60000"));
        } catch (Throwable t) {
            timeout = 60 * 1000;
        }
        FACET_CREATE_TIMEOUT = timeout;

        try {
            timeout = Integer.parseInt(System.getProperty("rhq.agent.plugins.facet.delete-resource.timeout", "60000"));
        } catch (Throwable t) {
            timeout = 60 * 1000;
        }
        FACET_DELETE_TIMEOUT = timeout;
    }

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(ResourceFactoryManager.class);

    /**
     * Configuration elements for the running of this manager.
     */
    private PluginContainerConfiguration configuration;

    /**
     * Executor service used to perform tasks.
     */
    private ExecutorService executor;

    /**
     * Handle to the metadata manager.
     */
    private PluginMetadataManager metadataManager;

    // Constructors  --------------------------------------------

    /**
     * Creates a new <code>ResourceFactoryManager</code> and initializes it as a remoted object.
     */
    public ResourceFactoryManager() {
        super(ResourceFactoryAgentService.class);
    }

    // ContainerService Implementation  --------------------------------------------

    public void initialize() {
        log.debug("Initializing...");

        // Retrieve handle to metadata manager
        metadataManager = PluginContainer.getInstance().getPluginManager().getMetadataManager();

        // Initialize thread pool for executing tasks
        int corePoolSize = configuration.getResourceFactoryCoreThreadPoolSize();
        int keepAliveTime = configuration.getResourceFactoryKeepAliveTime();
        int maxPoolSize = configuration.getResourceFactoryMaxThreadPoolSize();

        executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(10000), new LoggingThreadFactory("ResourceFactory.executor", true));
    }

    public void shutdown() {
        PluginContainer pluginContainer = PluginContainer.getInstance();
        // TODO (ips, 04/30/12): Is it safe to pass true here to interrupt executing threads?
        pluginContainer.shutdownExecutorService(executor, false);
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    // ResourceFactoryAgentService Implementation  --------------------------------------------

    @SuppressWarnings("unchecked")
    public CreateResourceResponse executeCreateResourceImmediately(CreateResourceRequest request)
        throws PluginContainerException {
        // Load the actual resource type instance to be passed to the facet
        ResourceType resourceType = metadataManager.getType(request.getResourceTypeName(), request.getPluginName());
        if (resourceType == null) {
            throw new PluginContainerException("Could not retrieve resource type for request: " + request);
        }

        String creationType = (request.getResourceConfiguration() != null) ? "configuration" : "package";
        {
            log.debug("Creating " + creationType + "-based resource of type '" + request.getResourceTypeName()
                + "' and with parent with id " + request.getParentResourceId() + "...");
        }

        // Create the report to send the plugin
        CreateResourceReport report = new CreateResourceReport(request.getResourceName(), resourceType, request
            .getPluginConfiguration(), request.getResourceConfiguration(), request.getPackageDetails());

        // Execute the create against the plugin
        CreateChildResourceFacet facet = getCreateChildResourceFacet(request.getParentResourceId(), request
            .getTimeout());

        CreateResourceRunner runner = new CreateResourceRunner(this, request.getParentResourceId(), facet, request
            .getRequestId(), report, configuration.isInsideAgent());

        CreateResourceResponse response;
        try {
            response = (CreateResourceResponse) executor.submit((Callable) runner).get();
        } catch (Exception e) {
            throw new PluginContainerException("Error during create resource callable", e);
        }

        return response;
    }

    public void createResource(CreateResourceRequest request) throws PluginContainerException {
        // Load the actual resource type instance to be passed to the facet
        ResourceType resourceType = metadataManager.getType(request.getResourceTypeName(), request.getPluginName());
        if (resourceType == null) {
            throw new PluginContainerException("Could not retrieve resource type for request: " + request);
        }

        String creationType = (request.getResourceConfiguration() != null) ? "configuration" : "package";
        {
            log.debug("Creating " + creationType + "-based resource of type '" + request.getResourceTypeName()
                + "' and with parent with id " + request.getParentResourceId() + "...");
        }

        // Create the report to send the plugin
        CreateResourceReport report = new CreateResourceReport(request.getResourceName(), resourceType, request
            .getPluginConfiguration(), request.getResourceConfiguration(), request.getPackageDetails());

        // Execute the create against the plugin
        CreateChildResourceFacet facet = getCreateChildResourceFacet(request.getParentResourceId(), request
            .getTimeout());

        CreateResourceRunner runner = new CreateResourceRunner(this, request.getParentResourceId(), facet, request
            .getRequestId(), report, configuration.isInsideAgent());
        executor.submit((Runnable) runner);
    }

    @SuppressWarnings("unchecked")
    public DeleteResourceResponse executeDeleteResourceImmediately(DeleteResourceRequest request)
        throws PluginContainerException {
        int resourceId = request.getResourceId();
        DeleteResourceFacet facet = getDeleteResourceFacet(resourceId);

        DeleteResourceRunner runner = new DeleteResourceRunner(this, facet, request.getRequestId(), resourceId);

        DeleteResourceResponse response;

        try {
            response = (DeleteResourceResponse) executor.submit((Callable) runner).get();
        } catch (Exception e) {
            throw new PluginContainerException("Error occurred in delete resource thread", e);
        }

        return response;
    }

    public void deleteResource(DeleteResourceRequest request) throws PluginContainerException {
        int resourceId = request.getResourceId();
        DeleteResourceFacet facet = getDeleteResourceFacet(resourceId);

        DeleteResourceRunner runner = new DeleteResourceRunner(this, facet, request.getRequestId(), resourceId);
        executor.submit((Runnable) runner);
    }

    // Package  --------------------------------------------

    /**
     * Returns the server service implementation used to notify of a task completion.
     *
     * @return server service if one is registered; <code>null</code> otherwise
     */
    ResourceFactoryServerService getServerService() {
        ServerServices serverServices = configuration.getServerServices();
        if (serverServices == null) {
            return null;
        }

        ResourceFactoryServerService resourceFactoryServerService = serverServices.getResourceFactoryServerService();
        return resourceFactoryServerService;
    }

    // Private  --------------------------------------------

    /**
     * Returns the component that should be used to delete the resource in the given request.
     *
     * @param  resourceId identifies the resource for which to retrieve the facet
     *
     * @return component used to delete the resource described by the request
     *
     * @throws PluginContainerException if the resource component required to delete the resource does not implement the
     *                                  correct facet
     */
    private DeleteResourceFacet getDeleteResourceFacet(int resourceId) throws PluginContainerException {
        DeleteResourceFacet facet = ComponentUtil.getComponent(resourceId, DeleteResourceFacet.class,
            FacetLockType.WRITE, FACET_DELETE_TIMEOUT, false, true);
        return facet;
    }

    /**
     * Returns the component that should be used to create the resource in the given request.
     *
     * @param  parentResourceId identifies the parent under which the new resource will be created
     * @param  timeout the agent side timeout for the resource creation. if null or unusable use FACET_CREATE_TIMEOUT. 
     *
     * @return component used to create the resource
     *
     * @throws PluginContainerException if the resource component required to create the resource does not implement the
     *                                  correct facet
     */
    private CreateChildResourceFacet getCreateChildResourceFacet(int parentResourceId, Integer timeout)
        throws PluginContainerException {
        int createTimeout = (null == timeout || timeout < 1) ? FACET_CREATE_TIMEOUT : timeout;

        CreateChildResourceFacet facet = ComponentUtil.getComponent(parentResourceId, CreateChildResourceFacet.class,
            FacetLockType.WRITE, createTimeout, false, true);
        return facet;
    }

}