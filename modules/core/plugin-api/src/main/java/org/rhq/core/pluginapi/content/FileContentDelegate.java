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
package org.rhq.core.pluginapi.content;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.content.PackageDetails;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;

/**
* Delegate class used for manipulating artifacts in a JON plugin.
*
* @author Greg Hinkle
* @author Jason Dobies
*/
public class FileContentDelegate {

    private static final String RHQ_SHA_256 = "RHQ-Sha256";

    private final Log log = LogFactory.getLog(FileContentDelegate.class);
    private final String fileEnding;
    private final File directory;

    public FileContentDelegate() {
        this.fileEnding = null;
        this.directory = null;
    }

    public FileContentDelegate(File directory, String fileEnding) {
        this.directory = directory;
        this.fileEnding = fileEnding;
    }

    public String getFileEnding() {
        return fileEnding;
    }

    public File getDirectory() {
        return directory;
    }

    /**
     * Creates a new package described by the specified details. The destination of the content in the provided input
     * stream will be determined by the package name.
     *
     * @param  details  describes the package being created
     * @param  content  content to be written for the package. NOTE this Stream will be closed by this method.
     * @param  unzip    if <code>true</code>, the content stream will be treated like a ZIP file and be unzipped as
     *                  it is written, using the package name as the base directory; if <code>false</code> the
     * @param createBackup If <code>true</code>, the original file will be backed up to file.bak
     */
    public void createContent(PackageDetails details, File content, boolean unzip) {
        File destination = getPath(details);
        try {
            if (unzip) {
                ZipUtil.unzipFile(content, destination);
            } else {
                FileUtil.copyFile(content, destination);
            }
            details.setFileName(destination.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Error creating artifact from details: " + destination, e);
        }
    }

    /**
     * Returns a stream from which the content of the specified package can be read.
     *
     * @param details package being loaded
     *
     * @return buffered input stream containing the contents of the package; will not be <code>null</code>, an
     *         exception is thrown if the content cannot be loaded
     */
    public InputStream getContent(PackageDetails details) {
        File contentFile = getPath(details);
        try {
            return new BufferedInputStream(new FileInputStream(contentFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Package content not found for package " + contentFile, e);
        }
    }

    /**
     * Deletes the underlying file for the specified package.
     *
     * @param details package to delete
     */
    public void deleteContent(PackageDetails details) {
        File contentFile = getPath(details);
        if (!contentFile.exists())
            return;

        try {
            FileUtils.purge(contentFile, true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete underlying file [" + contentFile + "] for " + details + ".", e);
        }
    }

    /**
     * This is a stub implementation, you need to implement a
     * discovery for artifacts of your particular content type.
     * 
     * @return
     */
    public Set<ResourcePackageDetails> discoverDeployedPackages() {
        throw new UnsupportedOperationException("This method is not implemented!");
    }

    /**
     * Retrieves SHA256 for the deployment. If this is an exploded deployment
     * and SHA256 is missing from the data directory then compute the SHA256 
     * and save in the data directory. 
     * 
     * @param deployment deployment location
     * @param resourceId resource id
     * @param dataDirectory data directory
     * @return SHA256 of the package
     */
    public String retrieveDeploymentSHA(File deployment, String resourceId, File dataDirectory) {
        String sha = null;

        if (deployment.isDirectory()) {
            File propertiesFile = new File(dataDirectory, resourceId + ".sha");

            if (propertiesFile.exists()) {
                FileInputStream propertiesInputStream = null;
                try {
                    propertiesInputStream = new FileInputStream(propertiesFile);
                    Properties prop = new Properties();
                    prop.load(propertiesInputStream);
                    sha = prop.getProperty(RHQ_SHA_256);
                } catch (IOException e) {
                    throw new RuntimeException("Error retrieving artifact's SHA256.", e);
                } finally {
                    if (propertiesInputStream != null) {
                        try {
                            propertiesInputStream.close();
                        } catch (IOException e) {
                            log.error("Failed to close input stream.", e);
                        }
                    }
                }
            }

            if (sha == null) {
                sha = this.saveDeploymentSHA(deployment, resourceId, dataDirectory);
            }
        } else {
            sha = this.computeSHAForArchivedContent(deployment);
        }

        return sha;
    }

    public String saveDeploymentSHA(File originalArchive, File deployment, String resourceId, File dataDirectory) {
        String sha = null;

        if (deployment.isDirectory()) {
            sha = this.computeSHAForArchivedContent(originalArchive);
            this.saveDeploymentSHA(sha, resourceId, dataDirectory);
        } else {
            sha = this.computeSHAForArchivedContent(deployment);
        }

        return sha;
    }

    public String saveDeploymentSHA(File deployment, String resourceId, File dataDirectory) {
        String sha = null;

        if (deployment.isDirectory()) {
            sha = this.computeSHAForExplodedContent(deployment);
            this.saveDeploymentSHA(sha, resourceId, dataDirectory);
        } else {
            sha = this.computeSHAForArchivedContent(deployment);
        }

        return sha;
    }

    private void saveDeploymentSHA(String sha, String resourceId, File dataDirectory) {
        Properties prop = new Properties();
        prop.setProperty(RHQ_SHA_256, sha);

        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        File propertiesFile = new File(dataDirectory, resourceId + ".sha");
        FileOutputStream propertiesOutputStream = null;
        try {
            propertiesOutputStream = new FileOutputStream(propertiesFile);
            prop.store(propertiesOutputStream, null);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error saving artifact's SHA256.", e);
        } catch (IOException e) {
            throw new RuntimeException("Error saving artifact's SHA256.", e);
        } finally {
            if (propertiesOutputStream != null) {
                try {
                    propertiesOutputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error saving artifact's SHA256.", e);
                }
            }
        }
    }

    /**
     * Computes SHA256 for an archive.
     *
     * @param contentFile content archive
     * @return SHA256 of the archive
     */
    private String computeSHAForArchivedContent(File contentFile) {
        if (!contentFile.isDirectory()) {
            try {
                MessageDigestGenerator messageDigest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
                return messageDigest.calcDigestString(contentFile);
            } catch (Exception ex) {
                log.error("Not able to compute SHA256 for " + contentFile.getPath() + " .");
            }
        }

        return null;
    }

    private String computeSHAForExplodedContent(File deploymentDirectory) {
        String sha = null;

        try {
            if (deploymentDirectory.isDirectory()) {
                MessageDigestGenerator messageDigest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

                Stack<File> unvisitedFolders = new Stack<File>();
                unvisitedFolders.add(deploymentDirectory);
                while (!unvisitedFolders.empty()) {
                    File[] files = unvisitedFolders.pop().listFiles();
                    Arrays.sort(files, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            try {
                                return f1.getCanonicalPath().compareTo(f2.getCanonicalPath());
                            } catch (IOException e) {
                                //do nothing if the sort fails at this point
                            }

                            return 0;
                        }
                    });

                    for (File file : files) {
                        if (file.isDirectory()) {
                            unvisitedFolders.add(file);
                        } else {
                            FileInputStream inputStream = null;
                            try {
                                inputStream = new FileInputStream(file);
                                messageDigest.add(inputStream);
                            } finally {
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                            }
                        }
                    }
                }

                sha = messageDigest.getDigestString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error creating artifact for contentFile: " + deploymentDirectory, e);
        }

        return sha;
    }

    /**
     * JBNADM-2022 - It still needs to be determined if it is the responsibility of the plugin container or the
     *             plugin to be concerned with path information in the package name. For now, it's the plugin's
     *             responsibility. We strip out the path information to keep control of where the JARs are
     *             deployed to. Note: when we add support for more package types, we'll need to refactor this
     *             out on a package type basis.
     * @param details package details
     * @return destination path
     */
    private File getPath(PackageDetails details) {
        String fileName = details.getKey().getName();
        int lastPathStart = fileName.lastIndexOf(File.separatorChar);
        if (lastPathStart > -1) {
            fileName = fileName.substring(lastPathStart + 1);
        }

        if (this.fileEnding != null && !fileName.endsWith(this.fileEnding)) {
            fileName = fileName + this.fileEnding;
        }

        return new File(this.directory, fileName);
    }
}
