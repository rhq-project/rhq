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
package org.rhq.core.clientapi.server.inventory;

import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.communications.command.annotation.Asynchronous;

/**
 * Callback interface for resource factory operations. Implementations of this interface, once registered with the
 * plugin container, will received the requests that were issued updated with the operation results.
 *
 * @author Jason Dobies
 */
public interface ResourceFactoryServerService {
    /**
     * Tells the server that a creation request has completed. The request is identified by the requestId parameter,
     * which correlates to the requestId passed into the original agent service call. If the request was successful, the
     * resource key of the newly created resource should be returned as well.
     *
     * It needs to be synchronous because we want the server to update the CreateResourceHistory entity
     * <strong>BEFORE</strong> a new service discovery is started by the PC (the entity will be read when the inventory
     * report is merged).
     *
     * @param response carries information on the status and results of the create call
     */
    void completeCreateResource(CreateResourceResponse response);

    /**
     * Tells the server that a delete request has completed. The request is identified by the requestId parameter, which
     * correlates to the requestId passed into the original agent service call.
     *
     * @param response carries information on the status and results of the delete call
     */
    @Asynchronous(guaranteedDelivery = true)
    void completeDeleteResourceRequest(DeleteResourceResponse response);
}
