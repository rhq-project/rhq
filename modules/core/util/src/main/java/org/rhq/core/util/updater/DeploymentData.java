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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.rhq.core.template.TemplateEngine;

/**
 * Data that describes a particular deployment. In effect, this provides the
 * data needed to fully deploy something.
 * 
 * @author John Mazzitelli
 */
public class DeploymentData {

    private final DeploymentProperties deploymentProps;
    private final Set<File> zipFiles;
    private final Map<File, File> rawFiles;
    private final File destinationDir;
    private final Map<File, Pattern> zipEntriesToRealizeRegex;
    private final Set<File> rawFilesToRealize;
    private final TemplateEngine templateEngine;
    private final Pattern ignoreRegex;
    private final boolean manageRootDir;

    /**
     * Constructors that prepares this object with the data that is necessary in order to deploy archive/file content
     * a destination directory.
     *  
     * @param deploymentProps metadata about this deployment
     * @param zipFiles the archives containing the content to be deployed
     * @param rawFiles files that are to be copied into the destination directory - the keys are the current
     *                 locations of the files, the values are where the files should be copied (the values may be relative
     *                 in which case they are relative to destDir and can have subdirectories and/or a different filename
     *                 than what the file is named currently)
     * @param destinationDir the root directory where the content is to be deployed
     * @param zipEntriesToRealizeRegex the patterns of files (whose paths are relative to destDir) that
     *                                 must have replacement variables within them replaced with values
     *                                 obtained via the given template engine. The key is the name of the zip file
     *                                 that the regex must be applied to - in other words, the regex value is only applied
     *                                 to relative file names as found in their associated zip file.
     * @param rawFilesToRealize identifies the raw files that need to be realized; note that each item in this set
     *                          must match a key to a <code>rawFiles</code> entry
     * @param templateEngine if one or more filesToRealize are specified, this template engine is used to determine
     *                       the values that should replace all replacement variables found in those files
     * @param ignoreRegex the files/directories to ignore when updating an existing deployment
     * @param manageRootDir if false, the top directory where the files will be deployed (i.e. the destinationDir)
     *                      will be left alone. That is, if files already exist there, they will not be removed or
     *                      otherwise merged with this deployment's root files. If true, this top root directory
     *                      will be managed just as any subdirectory within the deployment will be managed.
     *                      The purpose of this is to be able to write files to an existing directory that has other
     *                      unrelated files in it that need to remain intact. e.g. the deploy/ directory of JBossAS. 
     *                      Note: regardless of this setting, all subdirectories under the root dir will be managed.
     */
    public DeploymentData(DeploymentProperties deploymentProps, Set<File> zipFiles, Map<File, File> rawFiles,
        File destinationDir, Map<File, Pattern> zipEntriesToRealizeRegex, Set<File> rawFilesToRealize,
        TemplateEngine templateEngine, Pattern ignoreRegex, boolean manageRootDir) {

        if (deploymentProps == null) {
            throw new IllegalArgumentException("deploymentProps == null");
        }
        if (destinationDir == null) {
            throw new IllegalArgumentException("destDir == null");
        }

        if (zipFiles == null) {
            zipFiles = new HashSet<File>();
        }
        if (rawFiles == null) {
            rawFiles = new HashMap<File, File>();
        }
        if ((zipFiles.size() == 0) && (rawFiles.size() == 0)) {
            throw new IllegalArgumentException("zipFiles/rawFiles are empty - nothing to do");
        }

        this.deploymentProps = deploymentProps;
        this.zipFiles = zipFiles;
        this.rawFiles = rawFiles;
        this.destinationDir = destinationDir;
        this.ignoreRegex = ignoreRegex;
        this.manageRootDir = manageRootDir;

        // if there is nothing to realize or we have no template engine to obtain replacement values, then we null things out
        if (templateEngine == null || (zipEntriesToRealizeRegex == null && rawFilesToRealize == null)) {
            this.zipEntriesToRealizeRegex = null;
            this.rawFilesToRealize = null;
            this.templateEngine = null;
        } else {
            this.zipEntriesToRealizeRegex = zipEntriesToRealizeRegex;
            this.rawFilesToRealize = rawFilesToRealize;
            this.templateEngine = templateEngine;
        }

        return;
    }

    public DeploymentProperties getDeploymentProps() {
        return deploymentProps;
    }

    public Set<File> getZipFiles() {
        return zipFiles;
    }

    public Map<File, File> getRawFiles() {
        return rawFiles;
    }

    public File getDestinationDir() {
        return destinationDir;
    }

    public Map<File, Pattern> getZipEntriesToRealizeRegex() {
        return zipEntriesToRealizeRegex;
    }

    public Set<File> getRawFilesToRealize() {
        return rawFilesToRealize;
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public Pattern getIgnoreRegex() {
        return ignoreRegex;
    }

    public boolean isManageRootDir() {
        return manageRootDir;
    }
}
