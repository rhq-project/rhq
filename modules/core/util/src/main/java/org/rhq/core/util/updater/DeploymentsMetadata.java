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
import java.util.regex.Pattern;

import org.rhq.core.util.file.FileUtil;

/**
 * This loads and stores metadata about installs of a particular bundle deployment.
 * 
 * @author John Mazzitelli
 */
public class DeploymentsMetadata {
    public static final String METADATA_DIR = ".rhqdeployments";
    public static final String CURRENT_DEPLOYMENT_FILE = "current-deployment.properties";
    public static final String DEPLOYMENT_FILE = "deployment.properties";
    public static final String HASHCODES_FILE = "file-hashcodes.dat";

    private final File rootDirectory;

    /**
     * Creates the metadata object given the root directory where the bundle deployment is installed.
     * 
     * @param rootDirectory the location where the bundle deployments will go and where the metadata directory is located
     */
    public DeploymentsMetadata(File rootDirectory) {
        if (rootDirectory == null) {
            throw new NullPointerException("rootDirectory == null");
        }
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String toString() {
        return "DeploymentMetadata [" + getRootDirectory().getAbsolutePath() + "]";
    }

    /**
     * @return the root directory where the bundle deployments are and where the metadata directory is located.
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * If this object's {@link #getRootDirectory()} refers to a directory containing managed deployments,
     * this returns <code>true</code>. If that root location is not managed, <code>false</code> is returned.
     * 
     * @return indication if the root directory has deployments that are managed
     */
    public boolean isManaged() {
        File metaDir = getMetadataDirectory();
        if (!metaDir.isDirectory()) {
            return false; // there isn't even a top level metadata directory, we can't be managing deployments here
        }

        try {
            File currentMetaDir = getCurrentDeploymentMetadataDirectory();
            if (!currentMetaDir.isDirectory()) {
                return false; // strange, why is this missing?
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * @return the directory where the metadata does or should exist
     */
    public File getMetadataDirectory() {
        return new File(getRootDirectory(), METADATA_DIR);
    }

    /**
     * Same as {@link #getMetadataDirectory()}, however, if the directory doesn't exist, an exception is thrown.
     * @return the directory where the metadata exists
     * @throws Exception if the directory does not exist
     */
    public File getMetadataDirectoryOnlyIfExists() throws Exception {
        File metaDir = getMetadataDirectory();
        if (!metaDir.isDirectory()) {
            throw new IllegalStateException("Not a managed deployment location: "
                + getRootDirectory().getAbsolutePath());
        }
        return metaDir;
    }

    /**
     * Returns information about the current deployment.
     * 
     * @return props with current deployment information
     * @throws Exception if could not determine the current deployment
     */
    public DeploymentProperties getCurrentDeploymentProperties() throws Exception {
        try {
            File metaDir = getMetadataDirectoryOnlyIfExists();
            File propertiesFile = new File(metaDir, CURRENT_DEPLOYMENT_FILE);
            DeploymentProperties props = DeploymentProperties.loadFromFile(propertiesFile);
            return props;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine current deployment", e);
        }
    }

    /**
     * Returns information about the deployment with the given ID.
     * 
     * @param id identifies which deployment the caller wants information on
     * @return props with deployment information
     * @throws Exception if could not find the deployment information
     */
    public DeploymentProperties getDeploymentProperties(int deploymentId) throws Exception {
        try {
            File deploymentSubdir = getDeploymentMetadataDirectory(deploymentId);
            File propertiesFile = new File(deploymentSubdir, DEPLOYMENT_FILE);
            DeploymentProperties props = DeploymentProperties.loadFromFile(propertiesFile);
            return props;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot get deployment info", e);
        }
    }

    /**
     * Returns the files and their hashcodes for the current deployment. This does not
     * perform live computations of the file hashcodes, instead it reads the data out of
     * the metadata file from a previous computation when the files were initially
     * deployed. In other words, if someone has recently changed a file after it was
     * initially deployed, the returned map will not know about it.
     * 
     * @return map of files/hashcodes when the current deployment was initially deployed
     * 
     * @throws Exception
     */
    public FileHashcodeMap getCurrentDeploymentFileHashcodes() throws Exception {
        try {
            File dir = getCurrentDeploymentMetadataDirectory();
            File hashcodesFile = new File(dir, HASHCODES_FILE);
            FileHashcodeMap map = FileHashcodeMap.loadFromFile(hashcodesFile);
            return map;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine current deployment", e);
        }
    }

    /**
     * Returns the files and their hashcodes for the given deployment. This does not
     * perform live computations of the file hashcodes, instead it reads the data out of
     * the metadata file for the given deployment.
     *
     * @return map of files/hashcodes when the current deployment was initially deployed
     * 
     * @throws Exception
     */
    public FileHashcodeMap getDeploymentFileHashcodes(int deploymentId) throws Exception {
        try {
            File dir = getDeploymentMetadataDirectory(deploymentId);
            File hashcodesFile = new File(dir, HASHCODES_FILE);
            FileHashcodeMap map = FileHashcodeMap.loadFromFile(hashcodesFile);
            return map;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine current deployment", e);
        }
    }

    /**
     * Call this when you already know the properties, and files/hashcodes for the current live deployment.
     * This method will also mark this initialized, live deployment as the "current" deployment.
     *
     * @param deploymentProps identifies the deployment information for the live deployment
     * @param fileHashcodeMap the files and their hashcodes of the current live deployment
     * @throws Exception if failed to write out the necessary metadata about the given live data information
     */
    public void setCurrentDeployment(DeploymentProperties deploymentProps, FileHashcodeMap fileHashcodeMap)
        throws Exception {

        // determine where we need to put the metadata and create its empty directory 
        // TODO: if the directory exists, it means somehow we are initializing the same deployment again
        //       for now I'm just purging the old data, but is that the correct thing to do?
        getMetadataDirectory().mkdirs();
        File deploymentMetadataDir = getDeploymentMetadataDirectory(deploymentProps.getDeploymentId());
        FileUtil.purge(deploymentMetadataDir, true);
        deploymentMetadataDir.mkdirs();
        if (!deploymentMetadataDir.isDirectory()) {
            throw new Exception("Failed to create deployment metadata directory: " + deploymentMetadataDir);
        }

        // store the deployment properties so we know what deployment this metadata belongs to
        deploymentProps.saveToFile(new File(deploymentMetadataDir, DEPLOYMENT_FILE));

        // write the files/hashcodes data to the proper file
        fileHashcodeMap.storeToFile(new File(deploymentMetadataDir, HASHCODES_FILE));

        // since we are being told this is the live deployment, this deployment should be considered the current one 
        deploymentProps.saveToFile(new File(getMetadataDirectory(), CURRENT_DEPLOYMENT_FILE));

        return;
    }

    /**
     * Looks at the live deployment and takes a snapshot of it and stores its metadata in its appropriate
     * deployment metadata directory. The "live deployment" means the actual files in the root directory.
     * This method will also mark the live deployment as the "current" deployment.
     *
     * @param deploymentProps identifies the deployment information for the live data
     * @param ignoreRegex the live files/directories to ignore
     * @return the map of the files/hashcodes
     * @throws Exception if failed to calculate and store the metadata
     */
    public FileHashcodeMap snapshotLiveDeployment(DeploymentProperties deploymentProps, Pattern ignoreRegex)
        throws Exception {

        // calculate the hashcodes from the live files and write the data to the proper file
        FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(getRootDirectory(), ignoreRegex);
        setCurrentDeployment(deploymentProps, map);
        return map;
    }

    private File getCurrentDeploymentMetadataDirectory() throws Exception {
        DeploymentProperties currentDeploymentProps = getCurrentDeploymentProperties();
        return getDeploymentMetadataDirectory(currentDeploymentProps.getDeploymentId());
    }

    private File getDeploymentMetadataDirectory(int deploymentId) throws Exception {
        File metaDir = getMetadataDirectoryOnlyIfExists();
        File deploymentSubdir = new File(metaDir, Integer.toString(deploymentId));
        return deploymentSubdir;
    }
}
