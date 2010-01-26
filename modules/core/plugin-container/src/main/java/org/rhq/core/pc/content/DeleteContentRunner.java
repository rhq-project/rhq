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
package org.rhq.core.pc.content;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.content.DeletePackagesRequest;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.RemoveIndividualPackageResponse;
import org.rhq.core.util.exception.ThrowableUtil;

 /**
 * Runnable to allow threaded deleting of content.
 *
 * @author Jason Dobies
 */
public class DeleteContentRunner implements Runnable {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(DeleteContentRunner.class);

    private ContentManager contentManager;

    /**
     * Request being executed by this instance.
     */
    private DeletePackagesRequest request;

    // Constructors  --------------------------------------------

    public DeleteContentRunner(ContentManager contentManager, DeletePackagesRequest request) {
        this.contentManager = contentManager;
        this.request = request;
    }

    // Runnable Implementation  --------------------------------------------

    public void run() {

        RemovePackagesResponse response;
        
        try {
            response = contentManager.performPackageDelete(request.getResourceId(), request.getPackages());
        } catch (Throwable throwable) {
            response = new RemovePackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage(ThrowableUtil.getStackAsString(throwable));
        }

        // We don't rely on the plugin to map up the response to the request ID, so we do it here
        response.setRequestId(request.getRequestId());

        // Request a new discovery so this content is removed from the PC inventory
        // This should not influence the result code of the delete request
        Set<RemoveIndividualPackageResponse> packageResponses = response.getPackageResponses();
        if (packageResponses != null) {

            // Keep a quick cache of which package types have had discoveries executed so we don't
            // unnecessarily hammer the plugin with redundant discoveries
            Set<String> packageTypeNames = new HashSet<String>();

            for (RemoveIndividualPackageResponse individualResponse : packageResponses) {
                PackageDetailsKey key = individualResponse.getKey();

                if (key == null)
                    continue;

                String packageTypeName = key.getPackageTypeName();

                // Make sure we haven't already run a discovery for this package type
                if (!packageTypeNames.contains(packageTypeName)) {
                    packageTypeNames.add(packageTypeName);
                    
                    try {
                        contentManager.executeResourcePackageDiscoveryImmediately(request.getRequestId(),
                            individualResponse.getKey().getName());
                    } catch (Throwable throwable) {
                        log.error("Error executing content discovery", throwable);
                    }
                }
            }
        }

        // Contact the server service if one exists
        ContentServerService serverService = contentManager.getContentServerService();
        if (serverService != null) {
            serverService.completeDeletePackageRequest(response);
        }
    }
}