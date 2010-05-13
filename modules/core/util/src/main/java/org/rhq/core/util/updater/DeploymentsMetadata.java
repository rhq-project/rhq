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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
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
    public static final String PREVIOUS_DEPLOYMENT_FILE = "previous-deployment.properties";
    public static final String DEPLOYMENT_FILE = "deployment.properties";
    public static final String HASHCODES_FILE = "file-hashcodes.dat";
    public static final String BACKUP_DIR = "backup";
    public static final String EXT_BACKUP_DIR = "ext-backup";

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
        this.rootDirectory = rootDirectory.getAbsoluteFile(); // ensure it has an absolute path
    }

    @Override
    public String toString() {
        return "DeploymentMetadata [" + getRootDirectory() + "]";
    }

    /**
     * @return the root directory where the bundle deployments are and where the metadata directory is located.
     *         The returned File will have an absolute path.
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
            throw new IllegalStateException("Not a managed deployment location: " + getRootDirectory());
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
     * Returns information about the previous deployment given a specific deployment ID.
     * 
     * @param id identifies which deployment whose previous deployment props are to be returned
     * @return props with previous deployment information, or <code>null</code> if there was no previous deployment
     * @throws Exception if could not load the previous deployment information or the given deployment ID is invalid
     */
    public DeploymentProperties getPreviousDeploymentProperties(int deploymentId) throws Exception {
        try {
            File deploymentSubdir = getDeploymentMetadataDirectory(deploymentId);
            File propertiesFile = new File(deploymentSubdir, PREVIOUS_DEPLOYMENT_FILE);
            DeploymentProperties props = null;
            if (propertiesFile.exists()) {
                props = DeploymentProperties.loadFromFile(propertiesFile);
            }
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
     * @param deploymentId the ID of the deployment whose files/hashcodes are to be returned
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
            throw new IllegalStateException("Cannot determine deployment file hashcodes for [" + deploymentId + "]", e);
        }
    }

    /**
     * Returns a metadata directory that is appropriate to place backup files for the deployment.
     * Files placed here are files found in the deployment directory that needed to be backed up
     * before being overwritten or deleted.
     * 
     * @param deploymentId the ID of the deployment whose backup directory is to be returned
     * @return backup directory for the deployment
     */
    public File getDeploymentBackupDirectory(int deploymentId) throws Exception {
        try {
            File dir = getDeploymentMetadataDirectory(deploymentId);
            File backupDir = new File(dir, BACKUP_DIR);
            if (!backupDir.isDirectory()) {
                if (!backupDir.exists()) {
                    if (!backupDir.mkdirs()) {
                        throw new IllegalStateException("Failed to create backup directory: " + backupDir);
                    }
                } else {
                    throw new IllegalStateException("backup is a file but should be a directory: " + backupDir);
                }
            }
            return backupDir;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine deployment backup dir for [" + deploymentId + "]", e);
        }
    }

    /**
     * Returns a metadata directory that is appropriate to place backup files for the deployment.
     * Files placed here are files found in external directories (i.e. outside the deployment directory)
     * that needed to be backed up before being overwritten or deleted.
     * 
     * @param deploymentId the ID of the deployment whose backup directory is to be returned
     * @return backup directory for the deployment's external files
     */
    public File getDeploymentExternalBackupDirectory(int deploymentId) throws Exception {
        try {
            File dir = getDeploymentMetadataDirectory(deploymentId);
            File backupDir = new File(dir, EXT_BACKUP_DIR);
            if (!backupDir.isDirectory()) {
                if (!backupDir.exists()) {
                    if (!backupDir.mkdirs()) {
                        throw new IllegalStateException("Failed to create external backup directory: " + backupDir);
                    }
                } else {
                    throw new IllegalStateException("ext backup is a file but should be a directory: " + backupDir);
                }
            }
            return backupDir;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine deployment external backup dir for [" + deploymentId
                + "]", e);
        }
    }

    /**
     * Returns all the metadata directories that contain backup files for external directories
     * (i.e. outside the deployment directory). The returned map has driver letter root directories
     * as their keys (e.g. "C:\"); the values are the backup directories that contain files that were stored on their
     * associated drive. 
     * 
     * Obviously, this method is only appropriate to be called on Windows platforms. If this method is
     * called while the Java VM is running in a non-Windows environment, <code>null</code> is returned.
     * 
     * @param deploymentId the ID of the deployment whose backup directories are to be returned
     * @return backup directories for the deployment's external files, keyed on the drive letter root directory
     */
    public Map<String, File> getDeploymentExternalBackupDirectoriesForWindows(int deploymentId) throws Exception {

        boolean isWindows = (File.separatorChar == '\\');
        if (!isWindows) {
            return null;
        }

        try {
            // find all the direct children directories of the main external backup directory; if any of them
            // have the name "_X" where "X" is a letter from A to Z, that denotes a drive letter and
            // that directory contains all the backup files for that drive.
            Map<String, File> backupDirs = new HashMap<String, File>();
            Pattern driveLetterPattern = Pattern.compile("_([a-zA-Z])");
            File dir = getDeploymentExternalBackupDirectory(deploymentId);
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        String dirName = child.getName();
                        Matcher m = driveLetterPattern.matcher(dirName);
                        if (m.matches()) {
                            String driveLetter = m.group(1).toUpperCase();
                            backupDirs.put(driveLetter + ":\\", child);
                        }
                    }
                }
            }
            return backupDirs;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine deployment external backup dir for [" + deploymentId
                + "]", e);
        }
    }

    /**
     * Given a Windows drive letter, this will return the name of the external backup directory
     * where all backups for external files should be copied to. This method involves no relative or
     * absolute paths, this only returns the short name of the directory. The returned name should
     * be appended to the end of a {@link #getDeploymentExternalBackupDirectory(int) external backup directory}
     * to obtain the full path.
     *  
     * @param driveLetter
     * @return the name of the directory that should be used when storing external files for backup.
     */
    public String getExternalBackupDirectoryNameForWindows(String driveLetter) {
        return "_" + driveLetter.toUpperCase();
    }

    /**
     * Call this when you already know the properties, and files/hashcodes for the current live deployment.
     * This method will also mark this initialized, live deployment as the "current" deployment.
     * In addition, if <code>rememberPrevious</code> is <code>true</code>,  this will take the previous
     * deployment information and backup that data - pass <code>false</code> if you merely want to update
     * the current deployment properties without affecting anything else, specifically the backed up
     * previous deployment data.
     * 
     * @param deploymentProps identifies the deployment information for the live deployment
     * @param fileHashcodeMap the files and their hashcodes of the current live deployment
     * @param rememberPrevious if <code>true</code>, this will create a backup of the previous deployment data
     * @throws Exception if failed to write out the necessary metadata about the given live data information
     */
    public void setCurrentDeployment(DeploymentProperties deploymentProps, FileHashcodeMap fileHashcodeMap,
        boolean rememberPrevious) throws Exception {

        // determine where we need to put the metadata and create its empty directory 
        // Don't worry if the directory already exists, we probably backed up files there ahead of time.
        getMetadataDirectory().mkdirs();
        File deploymentMetadataDir = getDeploymentMetadataDirectory(deploymentProps.getDeploymentId());
        deploymentMetadataDir.mkdirs();
        if (!deploymentMetadataDir.isDirectory()) {
            throw new Exception("Failed to create deployment metadata directory: " + deploymentMetadataDir);
        }

        // store the deployment properties so we know what deployment this metadata belongs to
        deploymentProps.saveToFile(new File(deploymentMetadataDir, DEPLOYMENT_FILE));

        // write the files/hashcodes data to the proper file
        fileHashcodeMap.storeToFile(new File(deploymentMetadataDir, HASHCODES_FILE));

        // since we are being told this is the live deployment, this deployment should be considered the current one 
        File currentDeploymentPropertiesFile = new File(getMetadataDirectory(), CURRENT_DEPLOYMENT_FILE);

        if (rememberPrevious) {
            File previousDeploymentPropertiesFile = new File(deploymentMetadataDir, PREVIOUS_DEPLOYMENT_FILE);
            if (currentDeploymentPropertiesFile.exists()) {
                FileUtil.copyFile(currentDeploymentPropertiesFile, previousDeploymentPropertiesFile);
            }
        }

        deploymentProps.saveToFile(currentDeploymentPropertiesFile);

        return;
    }

    /**
     * Looks at the live deployment and takes a snapshot of it and stores its metadata in its appropriate
     * deployment metadata directory. The "live deployment" means the actual files in the root directory.
     * This method will also mark the live deployment as the "current" deployment.
     *
     * @param deploymentProps identifies the deployment information for the live data
     * @param ignoreRegex the live files/directories to ignore
     * @param ignored a set that will contain those files/directories that were ignored while scanning the deployment
     * @return the map of the files/hashcodes
     * @throws Exception if failed to calculate and store the metadata
     */
    public FileHashcodeMap snapshotLiveDeployment(DeploymentProperties deploymentProps, Pattern ignoreRegex,
        Set<String> ignored) throws Exception {

        // calculate the hashcodes from the live files and write the data to the proper file
        FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(getRootDirectory(), ignoreRegex, ignored);
        setCurrentDeployment(deploymentProps, map, true);
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
