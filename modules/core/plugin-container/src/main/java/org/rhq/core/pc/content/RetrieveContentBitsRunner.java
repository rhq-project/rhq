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

import java.io.InputStream;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.transfer.RetrievePackageBitsRequest;

/**
 * Runnable implementation to allow threaded requests to get a package content.
 *
 * @author Jason Dobies
 */
public class RetrieveContentBitsRunner implements Runnable {
    // Attributes  --------------------------------------------

    private ContentManager contentManager;

    /**
     * Request being handled by this instance.
     */
    private RetrievePackageBitsRequest request;

    // Constructors  --------------------------------------------

    public RetrieveContentBitsRunner(ContentManager contentManager, RetrievePackageBitsRequest request) {
        this.contentManager = contentManager;
        this.request = request;
    }

    // Runnable Implementation  --------------------------------------------

    public void run() {
        // Create the response
        ContentServiceResponse response = new ContentServiceResponse(request.getRequestId());

        InputStream inputStream = null;
        try {
            // Perform the request on the plugin
            inputStream = contentManager.performGetPackageBits(request.getResourceId(), request.getPackageDetails());

            // If the input stream was gotten, we're successful. Otherwise, the call failed.
            if (inputStream == null) {
                response.setErrorMessage("Null input stream received from plugin");
                response.setStatus(ContentRequestStatus.FAILURE);
            } else {
                response.setStatus(ContentRequestStatus.SUCCESS);
            }
        } catch (Throwable throwable) {
            response.setErrorMessageFromThrowable(throwable);
            response.setStatus(ContentRequestStatus.FAILURE);
        }

        // Notify the server that the request has been completed
        ContentServerService serverService = contentManager.getContentServerService();
        if (serverService != null) {
            serverService.completeRetrievePackageBitsRequest(response, inputStream);
        }
    }
}