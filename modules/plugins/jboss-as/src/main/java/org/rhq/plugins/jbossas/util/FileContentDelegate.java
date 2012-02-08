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
package org.rhq.plugins.jbossas.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.content.PackageDetails;
import org.rhq.core.domain.content.PackageDetailsKey;
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
    private static final String MANIFEST_RELATIVE_PATH = "META-INF/MANIFEST.MF";

    private Log log = LogFactory.getLog(FileContentDelegate.class);

    protected File directory;

    private String fileEnding;
    private String packageTypeName;

    public FileContentDelegate(File directory, String fileEnding, String packageTypeName) {
        this.directory = directory;
        this.fileEnding = fileEnding;
        this.packageTypeName = packageTypeName;
    }

    public String getFileEnding() {
        return fileEnding;
    }

    public String getPackageTypeName() {
        return packageTypeName;
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
     * @param shaString the SHA-256 of the specified input stream
     */
    public void createContent(PackageDetails details, File sourceContentFile, boolean unzip, boolean createBackup) {
        File destinationContentFile = getPath(details);
        try {
            if (createBackup) {
                moveToBackup(destinationContentFile, ".bak");
            }
            if (unzip) {
                ZipUtil.unzipFile(sourceContentFile, destinationContentFile);
                String shaString = new MessageDigestGenerator(MessageDigestGenerator.SHA_256)
                    .calcDigestString(sourceContentFile);
                writeSHAToManifest(destinationContentFile, shaString);
            } else {
                FileUtil.copyFile(sourceContentFile, destinationContentFile);
            }
            details.setFileName(destinationContentFile.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Error creating artifact from details: " + destinationContentFile, e);
        }
    }

    /**
     * Try to move the passed contentFile to a backup named contentFile + suffix
     * @param contentFile File object pointing to the original file
     * @param suffix the suffix to tack on
     */
    private void moveToBackup(File contentFile, String suffix) {

        File backupFile = new File(contentFile.getAbsolutePath() + suffix);
        if (backupFile.exists()) {
            // remove
            if (!backupFile.delete())
                log.warn("Removing of old backup file " + backupFile + " failed ");
        }
        if (!contentFile.renameTo(backupFile)) {
            log.warn("Moving " + contentFile + " to backup " + backupFile + " failed");
        }
    }

    public File getPath(PackageDetails details) {
        /* JBNADM-2022 - It still needs to be determined if it is the responsibility of the plugin container or the
         *               plugin to be concerned with path information in the package name. For now, it's the plugin's
         *               responsibility. We strip out the path information to keep control of where the JARs are
         *               deployed to. Note: when we add support for more package types, we'll need to refactor this
         *               out on a package type basis.
         *
         * jdobies, Sep 20, 2007
         */
        PackageDetailsKey key = details.getKey();
        String fileName = key.getName();
        int lastPathStart = fileName.lastIndexOf(File.separatorChar);
        if (lastPathStart > -1) {
            fileName = fileName.substring(lastPathStart + 1);
        }

        if (!fileName.endsWith(fileEnding)) {
            fileName = fileName + fileEnding;
        }
        return new File(this.directory, fileName);
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

    public Set<ResourcePackageDetails> discoverDeployedPackages() {
        /*
         * This is a stub implementation, you need to implement a
         * discovery for artifacts of your particular content type
         */
        return null;
    }

    /**
     * Retrieves the SHA256 for a deployed application.
     * 1) If the app is exploded then return RHQ-Sha256 manifest attribute.
     *   1.1) If RHQ-Sha256 is missing then compute it, save it and return the result.
     * 2) If the app is an archive then compute SHA256 on fly and return it.
     *
     * @param deploymentFile deployment file
     * @return
     */
    public String getSHA(File deploymentFile) {
        String sha = null;
        try {
            if (deploymentFile.isDirectory()) {
                File manifestFile = new File(deploymentFile.getAbsolutePath(), MANIFEST_RELATIVE_PATH);
                if (manifestFile.exists()) {
                    InputStream manifestStream = new FileInputStream(manifestFile);
                    Manifest manifest = null;
                    try {
                        manifest = new Manifest(manifestStream);
                        sha = manifest.getMainAttributes().getValue(RHQ_SHA_256);
                    } finally {
                        manifestStream.close();
                    }
                }

                if (sha == null || sha.trim().isEmpty()) {
                    sha = computeAndSaveSHA(deploymentFile);
                }
            } else {
                sha = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(deploymentFile);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Problem calculating digest of package [" + deploymentFile.getPath() + "].", ex);
        }

        return sha;
    }

    /**
     * Compute SHA256 for the content of an exploded war deployment. This method should be used to
     * compute the SHA256 for content deployed outside RHQ or for the initial content delivered
     * with the server.
     *
     * @param deploymentDirectory app deployment folder
     * @return
     */
    private String computeAndSaveSHA(File deploymentDirectory) {
        String sha = null;
        try {
            if (deploymentDirectory.isDirectory()) {
                MessageDigestGenerator messageDigest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

                Stack<File> unvisitedFolders = new Stack<File>();
                unvisitedFolders.add(deploymentDirectory);
                while (!unvisitedFolders.empty()) {
                    for (File file : unvisitedFolders.pop().listFiles()) {
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
                writeSHAToManifest(deploymentDirectory, sha);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error creating artifact for contentFile: " + deploymentDirectory, e);
        }

        return sha;
    }

    /**
     * Write the SHA256 to the manifest using the RHQ-Sha256 attribute tag.
     *
     * @param deploymentFolder app deployment folder
     * @param sha SHA256
     * @throws IOException
     */
    private void writeSHAToManifest(File deploymentFolder, String sha) throws IOException {
        File manifestFile = new File(deploymentFolder, MANIFEST_RELATIVE_PATH);
        Manifest manifest;
        if (manifestFile.exists()) {
            FileInputStream inputStream = new FileInputStream(manifestFile);
            try {
                manifest = new Manifest(inputStream);
            } finally {
                inputStream.close();
            }
        } else {
            manifest = new Manifest();
            manifestFile.getParentFile().mkdirs();
            manifestFile.createNewFile();
        }

        Attributes attribs = manifest.getMainAttributes();

        //The main section of the manifest file does not get saved if both of
        //these two attributes are missing. Please see Attributes implementation.
        if (!attribs.containsKey(Attributes.Name.MANIFEST_VERSION.toString())
            && !attribs.containsKey(Attributes.Name.SIGNATURE_VERSION.toString())) {
            attribs.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        }

        attribs.putValue(RHQ_SHA_256, sha);

        FileOutputStream outputStream = new FileOutputStream(manifestFile);
        try {
            manifest.write(outputStream);
        } finally {
            outputStream.close();
        }
    }
}