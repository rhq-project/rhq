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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.core.pc.inventory;

 import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.util.exception.ThrowableUtil;

 import static java.util.concurrent.TimeUnit.SECONDS;

 /**
 * Runnable implementation to process Resource create requests.
 *
 * @author Jason Dobies
 */
public class CreateResourceRunner implements Callable, Runnable {

     private static final Log LOG = LogFactory.getLog(CreateResourceRunner.class);

     private static final int SERVICE_SCAN_MAX_RETRY = 10;
     private static final long SERVICE_SCAN_RETRY_PAUSE = SECONDS.toMillis(30);

     // Attributes  --------------------------------------------

    /**
     * Handle to the manager that will do most of the logic.
     */
    private ResourceFactoryManager resourceFactoryManager;

    /**
     * Parent resource on which the child will be created.
     */
    private int parentResourceId;

    /**
     * Indicates whether or not to execute a runtime scan after the create.
     */
    private boolean runRuntimeScan;

    /**
     * ID of the request being processed. This ID will be used when the response is sent back to the caller.
     */
    private int requestId;

    /**
     * Facet to use to make the call against the plugin.
     */
    private CreateChildResourceFacet facet;

    /**
     * Report to send to the facet as part of the call.
     */
    private CreateResourceReport report;

    // Constructors  --------------------------------------------

    public CreateResourceRunner(ResourceFactoryManager resourceFactoryManager, int parentResourceId,
        CreateChildResourceFacet facet, int requestId, CreateResourceReport report, boolean runRuntimeScan) {
        this.resourceFactoryManager = resourceFactoryManager;
        this.parentResourceId = parentResourceId;
        this.facet = facet;
        this.requestId = requestId;
        this.report = report;
        this.runRuntimeScan = runRuntimeScan;
    }

    // Runnable Implementation  --------------------------------------------

    @Override
    public void run() {
        try {
            call();
        } catch (Exception e) {
            LOG.error("Error while chaining run to call", e);
        }
    }

    // Callable Implementation  --------------------------------------------

    @Override
    public Object call() throws Exception {
        LOG.info("Creating resource through report: " + report);

        String resourceName = null;
        String resourceKey = null;
        String errorMessage;
        CreateResourceStatus status;
        Configuration configuration = null;

        try {
            // Make the create call to the plugin
            report = facet.createResource(report);

            // Pull out the plugin populated parts of the report and add to the request
            resourceName = report.getResourceName();
            resourceKey = report.getResourceKey();
            errorMessage = report.getErrorMessage();
            status = report.getStatus();
            configuration = report.getResourceConfiguration();

            // Validate the status returned from the plugin
            CreateResourceStatus reportedStatus = report.getStatus();
            if ((reportedStatus == null) || (reportedStatus == CreateResourceStatus.IN_PROGRESS)) {
                LOG.warn("Plugin did not indicate the result of the request: " + requestId);
                errorMessage = "Plugin did not indicate the result of the resource creation attempt.";
                status = CreateResourceStatus.FAILURE;
            }

            // Ensure a resource key was returned from the plugin if the plugin reports the create was successful
            if ((isSuccessStatus(reportedStatus)) && (resourceKey == null)) {
                LOG.warn("Plugin did not indicate the resource key for this request: " + requestId);
                errorMessage = "Plugin did not indicate a resource key for this request.";
                status = CreateResourceStatus.FAILURE;
            }

            // RHQ-666 - The plugin should provide a resource name if the create was successful
            if ((isSuccessStatus(reportedStatus)) && (resourceName == null)) {
                LOG.warn("Plugin did not indicate a resource name for the request: " + requestId);
                errorMessage = "Plugin did not indicate a resource name for this request.";
                status = CreateResourceStatus.FAILURE;
            }

            Throwable throwable = report.getException();
            if (throwable != null) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Throwable was found in creation report for request [" + requestId + "].", throwable);
                else
                    LOG.warn("Throwable was found in creation report for request [" + requestId + "]: " + throwable
                           + " - Enable DEBUG logging to see the stack trace.");
                status = CreateResourceStatus.FAILURE;
                String messages = ThrowableUtil.getAllMessages(throwable);
                // If we still don't have an error message, populate it from the exception
                errorMessage = (errorMessage != null) ? (errorMessage + " - Cause: " + messages) : messages;
            }
        } catch(TimeoutException e) {
            status = CreateResourceStatus.TIMED_OUT;
            errorMessage = "The time out has been exceeded; however, the deployment may have been successful. You " +
                "may want to run a discovery scan to see if the deployment did complete successfully. Also consider " +
                "using a higher time out value for future deployments.";

            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to create resource for " + report + ". " + errorMessage, e);
            } else {
                LOG.info("Failed to create resource for " + report + ". " + errorMessage, e);
            }

            errorMessage += "\n\nRoot Cause:\n" + e.getMessage();
        } catch (Throwable t) {
            status = CreateResourceStatus.FAILURE;
            errorMessage = ThrowableUtil.getStackAsString(t);
        }

        // Send results back to the server
        CreateResourceResponse response =
            new CreateResourceResponse(requestId, resourceName, resourceKey, status, errorMessage, configuration);

        LOG.info("Sending create response to server: " + response);
        ResourceFactoryServerService serverService = resourceFactoryManager.getServerService();
        if (serverService != null) {
            try {
                serverService.completeCreateResource(response);
            } catch (Throwable throwable) {
                LOG.error("Error received while attempting to complete report for request: " + requestId, throwable);
            }
        }

        // Trigger a service scan on the parent resource to have the newly created resource discovered if the plugin
        // said the create was successful
        if (isSuccessStatus(status) && runRuntimeScan) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Scheduling service scan to discover newly created [" + report.getResourceType()
                    + "] managed resource with key [" + report.getResourceKey() + "]...");
            }
            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            Resource discoveredResource = null;
            for (int retry = 1; discoveredResource == null && retry <= SERVICE_SCAN_MAX_RETRY; retry++) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Service scan retry [" + retry + "] for parentResourceId [" + parentResourceId + "]");
                }
                if (retry > 1 && retry < SERVICE_SCAN_MAX_RETRY) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Pausing for [" + SERVICE_SCAN_RETRY_PAUSE
                            + "] ms before retrying service scan for parentResourceId [" + parentResourceId + "]");
                    }
                    Thread.sleep(SERVICE_SCAN_RETRY_PAUSE);
                }
                try {
                    // This will block until the service scan completes.
                    inventoryManager.performServiceScan(parentResourceId);
                } catch (Exception e) {
                    LOG.error("Failed to run service scan to discover newly created [" + report.getResourceType()
                        + "] managed resource with key [" + report.getResourceKey() + "].", e);
                }
                discoveredResource = getDiscoveredResource();
                if (discoveredResource != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Discovered " + discoveredResource + ", for a new managed resource created via RHQ.");
                    }
                } else {
                    LOG.warn("Failed to discover Resource for newly created [" + report.getResourceType()
                        + "] managed resource with key [" + report.getResourceKey() + "].");
                }
            }
        }

        return response;
    }

     private static boolean isSuccessStatus(CreateResourceStatus status) {
         return (status == CreateResourceStatus.SUCCESS) || (status == CreateResourceStatus.INVALID_CONFIGURATION)
                 || (status == CreateResourceStatus.INVALID_ARTIFACT);
     }

     private Resource getDiscoveredResource() {
         InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
         ResourceContainer parentResourceContainer = inventoryManager.getResourceContainer(parentResourceId);
         Resource parentResource = parentResourceContainer.getResource();
         for (Resource childResource : parentResource.getChildResources()) {
             if (childResource.getResourceType().equals(report.getResourceType()) && childResource.getResourceKey().equals(report.getResourceKey())) {
                 return childResource;
             }
         }
         return null;
     }

 }
