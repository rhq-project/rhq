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

 import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
 import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.domain.resource.CreateResourceStatus;
 import org.rhq.core.pc.PluginContainer;
 import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
 import org.rhq.core.pluginapi.inventory.CreateResourceReport;
 import org.rhq.core.util.exception.ThrowableUtil;

 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;

 import java.util.concurrent.Callable;

 /**
 * Runnable implementation to thread create request requests.
 *
 * @author Jason Dobies
 */
public class CreateResourceRunner implements Callable, Runnable {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(CreateResourceRunner.class);

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

    public void run() {
        try {
            call();
        } catch (Exception e) {
            log.error("Error while chaining run to call", e);
        }
    }

    // Callable Implementation  --------------------------------------------

    public Object call() throws Exception {
        log.info("Creating resource through report: " + report);

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
                log.warn("Plugin did not indicate the result of the request: " + requestId);
                errorMessage = "Plugin did not indicate the result of the resource creation attempt.";
                status = CreateResourceStatus.FAILURE;
            }

            // Ensure a resource key was returned from the plugin if the plugin reports the create was successful
            if ((isSuccessStatus(reportedStatus)) && (resourceKey == null)) {
                log.warn("Plugin did not indicate the resource key for this request: " + requestId);
                errorMessage = "Plugin did not indicate a resource key for this request.";
                status = CreateResourceStatus.FAILURE;
            }

            // RHQ-666 - The plugin should provide a resource name if the create was successful
            if ((isSuccessStatus(reportedStatus)) && (resourceName == null)) {
                log.warn("Plugin did not indicate a resource name for the request: " + requestId);
                errorMessage = "Plugin did not indicate a resource name for this request.";
                status = CreateResourceStatus.FAILURE;
            }

            Throwable throwable = report.getException();
            if (throwable != null) {
                if (log.isDebugEnabled())
                    log.debug("Throwable was found in creation report for request [" + requestId + "].", throwable);
                else
                    log.warn("Throwable was found in creation report for request [" + requestId + "]: " + throwable
                           + " - Enable DEBUG logging to see the stack trace.");
                status = CreateResourceStatus.FAILURE;
                String messages = ThrowableUtil.getAllMessages(throwable);
                // If we still don't have an error message, populate it from the exception
                errorMessage = (errorMessage != null) ? (errorMessage + " - Cause: " + messages) : messages;
            }
        } catch (Throwable t) {
            errorMessage = ThrowableUtil.getStackAsString(t);
            status = CreateResourceStatus.FAILURE;
        }

        // Send results back to the server
        CreateResourceResponse response =
            new CreateResourceResponse(requestId, resourceName, resourceKey, status, errorMessage, configuration);

        log.info("Sending create response to server: " + response);
        ResourceFactoryServerService serverService = resourceFactoryManager.getServerService();
        if (serverService != null) {
            try {
                serverService.completeCreateResource(response);
            } catch (Throwable throwable) {
                log.error("Error received while attempting to complete report for request: " + requestId, throwable);
            }
        }

        // Trigger a service scan on the parent resource to have the newly created resource discovered if the plugin
        // said the create was successful
        if (runRuntimeScan && isSuccessStatus(status)) {
            log.info("Scanning for newly created Resource...");
            try {
                InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
                inventoryManager.performServiceScan(parentResourceId);
            } catch (Throwable t) {
                log.error("Error received while attempting runtime scan for newly created Resource...");
            }
        }

        return response;
    }

     private static boolean isSuccessStatus(CreateResourceStatus status) {
         return (status == CreateResourceStatus.SUCCESS) || (status == CreateResourceStatus.INVALID_CONFIGURATION)
                 || (status == CreateResourceStatus.INVALID_ARTIFACT);
     }
 }