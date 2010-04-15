/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.util.updater;

import java.io.File;
import java.util.Set;
import java.util.regex.Pattern;

import org.rhq.core.template.TemplateEngine;

/**
 * This deploys a bundle of files within a zip archive to a managed directory.
 * 
 * @author John Mazzitelli
 */
public class Deployer {
    private final DeploymentProperties deploymentProps;
    private final File zipFile;
    private final File destDir;
    private final Set<String> filesToRealize;
    private final TemplateEngine templateEngine;
    private final Pattern ignoreRegex;
    private final DeploymentsMetadata deploymentsMetadata;

    /**
     * Constructors that prepares this object to deploy the given archive's content to the destination directory.
     *  
     * @param deploymentProps metadata about this deployment
     * @param zipFile the archive containing the content to be deployed
     * @param destDir the root directory where the content is to be deployed
     * @param filesToRealize the files (whose paths are relative to destDir) that must have replacement variables
     *                       within them replaced with values obtained via the given template engine
     * @param templateEngine if one or more filesToRealize are specified, this template engine is used to determine
     *                       the values that should replace all replacement variables found in those files
     * @param ignoreRegex the files/directories to ignore when updating an existing deployment
     */
    public Deployer(DeploymentProperties deploymentProps, File zipFile, File destDir, Set<String> filesToRealize,
        TemplateEngine templateEngine, Pattern ignoreRegex) {

        if (deploymentProps == null) {
            throw new IllegalArgumentException("deploymentProps == null");
        }
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile == null");
        }
        if (destDir == null) {
            throw new IllegalArgumentException("destDir == null");
        }

        this.deploymentProps = deploymentProps;
        this.zipFile = zipFile;
        this.destDir = destDir;
        this.ignoreRegex = ignoreRegex;

        if (filesToRealize == null || filesToRealize.size() == 0 || templateEngine == null) {
            // we don't need these if there is nothing to realize or we have no template engine to obtain replacement values
            this.filesToRealize = null;
            this.templateEngine = null;
        } else {
            this.filesToRealize = filesToRealize;
            this.templateEngine = templateEngine;
        }

        this.deploymentsMetadata = new DeploymentsMetadata(destDir);
        return;
    }

    public void deploy() {
        if (!this.deploymentsMetadata.isManaged()) {
            // the destination directory has not yet been used to deploy a bundle - this is our first deployment here

        }
    }
}
