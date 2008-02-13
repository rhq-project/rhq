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
package org.rhq.core.clientapi.agent.inventory;

import org.rhq.core.clientapi.agent.PluginContainerException;

/**
 * Plugin container interface for performing create and delete resource operations.
 *
 * @author Jason Dobies
 */
public interface ResourceFactoryAgentService {
    /**
     * Requests that a resource be created. The result of this creation will asynchronously be sent to a registered
     * instance of <code>ResourceFactoryServerService</code>. The parameter <code>requestId</code> will be used to
     * correlate the response call with this request.
     *
     * @param  request contains all of the information necessary to create the resource; cannot be <code>null</code>
     *
     * @throws PluginContainerException if the plugin container cannot be reached or fails before forwarding the
     *                                  creation request to the appropriate plugin
     */
    void createResource(CreateResourceRequest request) throws PluginContainerException;

    /**
     * Requests that a resource be created. This method will execute synchronously and return the results of the create.
     *
     * @param  request contains all of the information necessary to create the resource; cannot be <code>null</code>
     *
     * @return response object detailing the results of the operation
     *
     * @throws PluginContainerException if the plugin container cannot be reached or fails before forwarding the
     *                                  creation request to the appropriate plugin
     */
    CreateResourceResponse executeCreateResourceImmediately(CreateResourceRequest request)
        throws PluginContainerException;

    /**
     * This method is responsible for destroying an actual resource. For example, if the resource to be deleted is a
     * JBossAS 4.0 Data Source, this method will effectively delete the -ds.xml file.
     *
     * @param  request contains all of the information necessary to delete the resource; cannot be <code>null</code>
     *
     * @throws PluginContainerException if the plugin container cannot be reached or fails before forwarding the
     *                                  creation request to the appropriate plugin
     */
    void deleteResource(DeleteResourceRequest request) throws PluginContainerException;

    /**
     * Requests that a resource be destroyed. This method will execute synchronously and return the result of the delete
     * operation.
     *
     * @param  request contains all of the information necessary to delete the resource; cannot be <code>null</code>
     *
     * @return response object detailing the results of the operation
     *
     * @throws PluginContainerException if the plugin container cannot be reached or fails before forwarding the delete
     *                                  request to the appropriate plugin
     */
    DeleteResourceResponse executeDeleteResourceImmediately(DeleteResourceRequest request)
        throws PluginContainerException;
}