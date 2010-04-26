/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.bundle.ant.task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.rhq.bundle.ant.DeployPropertyNames;

import java.io.File;
import java.util.Hashtable;

/**
 * @author Ian Springer
 */
public class BundleTask extends AbstractBundleTask {
    private String name;
    private String version;
    private String description;

    @Override
    public void maybeConfigure() throws BuildException {
        // The below call will init the attribute fields.
        super.maybeConfigure();

        validateAttributes();

        getProject().setBundleName(this.name);
        getProject().setBundleVersion(this.version);
        getProject().setBundleDescription(this.description);
    }

    @Override
    public void execute() throws BuildException {        
        Hashtable projectProps = getProject().getProperties();

        // Make sure the requires System properties are defined and valid.
        String deployDir = (String) projectProps.get(DeployPropertyNames.DEPLOY_DIR);
        if (deployDir == null) {
            throw new BuildException("Required property [" + DeployPropertyNames.DEPLOY_DIR + "] was not specified.");
        }
        File deployDirFile = new File(deployDir);
        if (!deployDirFile.isAbsolute()) {
            throw new BuildException("Value of property [" + DeployPropertyNames.DEPLOY_DIR + "] (" + deployDirFile
                + ") is not an absolute path.");
        }
        getProject().setDeployDir(deployDirFile);
        log(DeployPropertyNames.DEPLOY_DIR + "=\"" + deployDir + "\"", Project.MSG_DEBUG);

        String deploymentIdStr = (String) projectProps.get(DeployPropertyNames.DEPLOY_ID);
        if (deploymentIdStr == null) {
            throw new BuildException("Required property [" + DeployPropertyNames.DEPLOY_ID + "] was not specified.");
        }
        int deploymentId;
        try {
            deploymentId = Integer.parseInt(deploymentIdStr);
        } catch (Exception e) {
            throw new BuildException("Value of property [" + DeployPropertyNames.DEPLOY_ID + "] (" + deploymentIdStr
                + ") is not valid.", e);
        }
        getProject().setDeploymentId(deploymentId);
        log(DeployPropertyNames.DEPLOY_ID + "=\"" + deploymentId + "\"", Project.MSG_DEBUG);

        log("Executing Ant script for bundle '" + this.name + "' version " + this.version + "...");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     *
     * @throws BuildException if an error occurs
     */
    protected void validateAttributes() throws BuildException {
        if (this.name == null) {
            throw new BuildException("The 'name' attribute is required.");
        }
        if (this.name.length() == 0) {
            throw new BuildException("The 'name' attribute must have a non-empty value.");
        }
        if (this.version == null) {
            throw new BuildException("The 'version' attribute is required.");
        }
        if (this.version.length() == 0) {
            throw new BuildException("The 'version' attribute must have a non-empty value.");
        }
    }
}