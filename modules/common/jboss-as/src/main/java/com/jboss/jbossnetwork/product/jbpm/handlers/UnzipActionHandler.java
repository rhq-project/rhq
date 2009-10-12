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

import java.io.File;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * JBPM handler that is responsible for unzipping a file.
 *
 * @author Jason Dobies
 */
public class UnzipActionHandler extends BaseHandler {
    /**
     * Location of the file being unzipped.
     */
    private String fileToBeUnzippedLocation;

    /**
     * Location into which the files will be unzipped. This must be a directory.
     */
    private String destinationDirectoryLocation;

    public void run(ExecutionContext executionContext) {
        try {
            HandlerUtils.checkFilenameExists(fileToBeUnzippedLocation);
            HandlerUtils.checkFilenameIsAFile(fileToBeUnzippedLocation);
            HandlerUtils.checkFilenameIsReadable(fileToBeUnzippedLocation);

            HandlerUtils.checkFilenameExists(destinationDirectoryLocation);
            HandlerUtils.checkFilenameIsADirectory(destinationDirectoryLocation);
            HandlerUtils.checkFilenameIsWriteable(destinationDirectoryLocation);

            unzip();

            complete(executionContext, "Successfully unzipped [" + HandlerUtils.formatPath(fileToBeUnzippedLocation)
                + "] to [" + HandlerUtils.formatPath(destinationDirectoryLocation) + "].");
        } catch (Throwable t) {
            error(executionContext, t, MESSAGE_NO_CHANGES, TRANSITION_ERROR);
        }
    }

    public String getDescription() {
        return "Unzip [" + HandlerUtils.formatPath(fileToBeUnzippedLocation) + "] into ["
            + HandlerUtils.formatPath(destinationDirectoryLocation) + "].";
    }

    protected void checkProperties() throws ActionHandlerException {
        HandlerUtils.checkIsSet("destinationDirectoryLocation", destinationDirectoryLocation);
        HandlerUtils.checkIsSet("fileToBeUnzippedLocation", fileToBeUnzippedLocation);
    }

    public void substituteVariables(ExecutionContext executionContext) throws ActionHandlerException {
        setFileToBeUnzippedLocation(substituteVariable(fileToBeUnzippedLocation, executionContext));
        setDestinationDirectoryLocation(substituteVariable(destinationDirectoryLocation, executionContext));
    }

    private void unzip() throws ActionHandlerException {
        try {
            // Use the ant unzip wrappers for simplicity of code
            Expand expander = new Expand();

            // Needed in order to get the ant logging (really notification of listeners) setup correctly,
            // i.e. so that it won't throw NPE's
            expander.setProject(new Project());

            expander.setSrc(new File(fileToBeUnzippedLocation));
            expander.setDest(new File(destinationDirectoryLocation));

            expander.execute();
        } catch (Exception e) {
            throw new ActionHandlerException("Failed trying to unzip ["
                + HandlerUtils.formatPath(fileToBeUnzippedLocation) + "] to ["
                + HandlerUtils.formatPath(destinationDirectoryLocation) + "].", e);
        }
    }

    public String getFileToBeUnzippedLocation() {
        return fileToBeUnzippedLocation;
    }

    public void setFileToBeUnzippedLocation(String fileToBeUnzippedLocation) {
        this.fileToBeUnzippedLocation = fileToBeUnzippedLocation;
    }

    public String getDestinationDirectoryLocation() {
        return destinationDirectoryLocation;
    }

    public void setDestinationDirectoryLocation(String destinationDirectoryLocation) {
        this.destinationDirectoryLocation = destinationDirectoryLocation;
    }
}