 /*
  * Jopr Management Platform
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
package com.jboss.jbossnetwork.product.jbpm.handlers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jbpm.graph.exe.ExecutionContext;

import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;

/**
 * JBPM action handler that is responsible for downloading the bits for the patch and writing them out to a temporary
 * file.
 *
 * @author Jason Dobies
 */
public class JONServerDownloadActionHandler extends BaseHandler {
    private String destinationFileLocation;

    public void run(ExecutionContext executionContext) {
        try {
            PackageDetailsKey key = (PackageDetailsKey) executionContext
                .getVariable(ContextVariables.PACKAGE_DETAILS_KEY);
            ContentContext contentContext = (ContentContext) executionContext
                .getVariable(ContextVariables.CONTENT_CONTEXT);

            downloadBits(key, contentContext);

            complete(executionContext, "Successfully downloaded file to ["
                + HandlerUtils.formatPath(destinationFileLocation) + "].");
        } catch (Throwable t) {
            error(executionContext, t, "Failed to download file to [" +
                  HandlerUtils.formatPath(destinationFileLocation) + "].", TRANSITION_ERROR);
        }
    }

    public void downloadBits(PackageDetailsKey key, ContentContext contentContext) throws IOException,
        ActionHandlerException {
        ContentServices contentServices = contentContext.getContentServices();

        // Open a stream to where the downloaded file should go
        FileOutputStream output = new FileOutputStream(destinationFileLocation);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output, 4096);

        // Request the bits from the server
        try {
            contentServices.downloadPackageBits(contentContext, key, bufferedOutput, true);
            bufferedOutput.close();

            // Verify the file was created correctly
            File downloadedFile = new File(destinationFileLocation);
            if (!downloadedFile.exists()) {
                throw new ActionHandlerException("File to download [" + destinationFileLocation + "] does not exist");
            }

            if (downloadedFile.length() == 0) {
                throw new ActionHandlerException("File [" + destinationFileLocation + "] is empty");
            }
        } finally {
            // Close the stream if there was an error thrown from downloadPackageBits
            try {
                bufferedOutput.close();
            } catch (IOException e1) {
                logger.error("Error closing output stream to [" + destinationFileLocation + "] after exception", e1);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Download file from the server and save it to [" + HandlerUtils.formatPath(destinationFileLocation)
            + "].";
    }

    @Override
    public void setPropertyDefaults() {
        if (destinationFileLocation == null) {
            destinationFileLocation = "#{downloadFolder}/#{software.filename}";
        }
    }

    @Override
    protected void checkProperties() throws ActionHandlerException {
        HandlerUtils.checkIsSet("destinationFileLocation", destinationFileLocation);
    }

    @Override
    public void substituteVariables(ExecutionContext executionContext) throws ActionHandlerException {
        destinationFileLocation = substituteVariable(destinationFileLocation, executionContext);
    }

    public String getDestinationFileLocation() {
        return destinationFileLocation;
    }

    public void setDestinationFileLocation(String destinationFileLocation) {
        this.destinationFileLocation = destinationFileLocation;
    }
}