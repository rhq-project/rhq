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

 import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
 import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
 import org.rhq.core.domain.resource.DeleteResourceStatus;
 import org.rhq.core.pc.PluginContainer;
 import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
 import org.rhq.core.util.exception.ThrowableUtil;

 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;

 import java.util.concurrent.Callable;

 /**
 * Runnable implementation to thread delete resource requests.
 *
 * @author Jason Dobies
 */
public class DeleteResourceRunner implements Callable, Runnable {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(DeleteResourceRunner.class);

    /**
     * Handle to the manager that will do most of the logic.
     */
    private ResourceFactoryManager resourceFactoryManager;

    /**
     * Facet against which the delete resource operation should be executed.
     */
    private DeleteResourceFacet facet;

    /**
     * ID of the original request that was used to trigger this delete.
     */
    private int requestId;

    /**
     * ID of the resource being deleted.
     */
    private int resourceId;

    // Constructors  --------------------------------------------

    /**
     * Creates a new instance of the runner that will execute the specified request.
     *
     * @param resourceFactoryManager handle to the manager for any business logic required
     * @param facet                  facet against which to perform the delete operation
     * @param requestId              ID of the request, used when submitting the response
     * @param resourceId             ID of the resource being deleted
     */
    public DeleteResourceRunner(ResourceFactoryManager resourceFactoryManager, DeleteResourceFacet facet,
        int requestId, int resourceId) {
        this.resourceFactoryManager = resourceFactoryManager;
        this.facet = facet;
        this.requestId = requestId;
        this.resourceId = resourceId;
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
        log.info("Deleting resource from request: " + requestId);

        DeleteResourceStatus status = null;
        String errorMessage = null;

        try {
            // TODO: Doesn't support resources with children
            facet.deleteResource();

            PluginContainer.getInstance().getInventoryManager().uninventoryResource(resourceId);

            status = DeleteResourceStatus.SUCCESS;
        } catch (Throwable t) {
            errorMessage = ThrowableUtil.getStackAsString(t);
            status = DeleteResourceStatus.FAILURE;
        }

        DeleteResourceResponse response = new DeleteResourceResponse(requestId, status, errorMessage);

        ResourceFactoryServerService serverService = resourceFactoryManager.getServerService();
        if (serverService != null) {
            try {
                serverService.completeDeleteResourceRequest(response);
            } catch (Throwable throwable) {
                log.error("Error received while attempting to complete report for request: " + requestId, throwable);
            }
        }

        return response;
    }
}