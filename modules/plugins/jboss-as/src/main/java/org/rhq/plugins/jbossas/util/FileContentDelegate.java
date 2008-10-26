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
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.rhq.core.domain.content.PackageDetails;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.plugins.utils.FileUtils;

/**
 * Delegate class used for manipulating artifacts in a JON plugin.
 *
 * @author Greg Hinkle
 * @author Jason Dobies
 */
public class FileContentDelegate {
    // Attributes  --------------------------------------------

    protected File directory;

    private String fileEnding;
    private String packageTypeName;

    // Constructors  --------------------------------------------

    public FileContentDelegate(File directory, String fileEnding, String packageTypeName) {
        this.directory = directory;
        this.fileEnding = fileEnding;
        this.packageTypeName = packageTypeName;
    }

    // Public  --------------------------------------------

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
     *                  content will be written to directly to a file using the package name as the file name
     */
    public void createContent(PackageDetails details, InputStream content, boolean unzip) {

        /* JBNADM-2022 - It still needs to be determined if it is the responsibility of the plugin container or the
         *               plugin to be concerned with path information in the package name. For now, it's the plugin's 
         *               reponsibility. We strip out the path information to keep control of where the JARs are 
         *               deployed to. Note: when we add support for more package types, we'll need to refactor this 
         *               out on an package type basis.      
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

        try {
            if (unzip) {
                File outputDir = new File(this.directory.getAbsolutePath() + File.separator + fileName + File.separator);
                FileUtils.unzipFile(content, outputDir);
            } else {
                File contentFile = new File(this.directory, fileName);
                FileUtils.writeFile(content, contentFile);
            }

            details.setFileName(fileName);
        } catch (IOException e) {
            throw new RuntimeException("Error creating artifact from details: " + fileName, e);
        }

    }

    /**
     * Returns a stream from which the content of the specified package can be read.
     *
     * @param  fileName package being loaded
     *
     * @return buffered input stream containing the contents of the package; will not be <code>null</code>, an
     *         exception is thrown if the content cannot be loaded
     */
    public InputStream getContent(PackageDetails details) {
        PackageDetailsKey key = details.getKey();
        File contentFile = new File(this.directory, key.getName());
        try {
            return new BufferedInputStream(new FileInputStream(contentFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Package content not found for package " + key.getName(), e);
        }
    }

    /**
     * Deletes the underlying file for the specified package.
     *
     * @param fileName package to delete
     */
    public void deleteContent(PackageDetails details) {
        PackageDetailsKey key = details.getKey();
        File contentFile = new File(this.directory, key.getName());

        if (!contentFile.exists())
            return;

        //If the artifact is a directory, its contents need to be deleted first   
        if (contentFile.isDirectory()) {
            FileUtils.deleteDirectoryContents(contentFile.listFiles());
        }

        boolean deleteResult = contentFile.delete();

        if (deleteResult == false) {
            throw new RuntimeException("Package content not succesfully deleted: " + key.getName());
        }

    }

    public Set<ResourcePackageDetails> discoverDeployedPackages() {
        /* 
         * This is a stub implementation, you need to implement a
         * discovery for artifacts of your particular content type
         */
        return null;
    }
}