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
package org.rhq.core.pc.content;

import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackagesRequest;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;

/**
 * Runnable to allow threaded creation of a content.
 *
 * @author Jason Dobies
 */
public class CreateContentRunner implements Runnable {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(CreateContentRunner.class);

    private ContentManager contentManager;

    /**
     * Request being executed by this instance.
     */
    private DeployPackagesRequest request;

    // Constructors  --------------------------------------------

    public CreateContentRunner(ContentManager contentManager, DeployPackagesRequest request) {
        this.contentManager = contentManager;
        this.request = request;
    }

    // Runnable Implementation  --------------------------------------------

    public void run() {
        DeployPackagesResponse response;
        try {
            response = contentManager.performPackageDeployment(request.getResourceId(), request.getPackages());
        } catch (Throwable throwable) {
            response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setErrorMessageFromThrowable(throwable);
        }

        // We don't rely on the plugin to map up the response to the request ID, so we do it here
        response.setRequestId(request.getRequestId());

        // Request a new discovery so this content is put in the PC inventory for the resource
        // When the new content is picked up from that discovery, its first revision will be
        // discovered as well
        Set<DeployIndividualPackageResponse> packageResponses = response.getPackageResponses();
        if (packageResponses != null) {
            try {
                for (DeployIndividualPackageResponse individualResponse : packageResponses) {
                    contentManager.executeResourcePackageDiscoveryImmediately(request.getResourceId(),
                        individualResponse.getKey().getName());
                }
            } catch (Throwable throwable) {
                log.error("Error occurred on content discovery request" + throwable);
            }
        }

        // Contact the server service if one exists
        ContentServerService serverService = contentManager.getContentServerService();
        if (serverService != null) {
            serverService.completeDeployPackageRequest(response);
        }
    }
}