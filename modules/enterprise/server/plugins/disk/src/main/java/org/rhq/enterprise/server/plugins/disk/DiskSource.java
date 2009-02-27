/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.disk;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.clientapi.server.plugin.content.ContentSourceAdapter;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.clientapi.server.plugin.content.util.ContentJarFileInfo;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.util.MD5Generator;

/**
 * This is the most basic <i>reference</i> implementation of a content source. It provides primative package
 * synchronization with file-system based source. It is anticipated that more content aware subclasses will provide more
 * useful functionality.
 *
 * @author jortel
 * @author John Mazzitelli
 */
public class DiskSource implements ContentSourceAdapter {

    /**
     * The root path (directory) from which to synchronize content.
     */
    private File rootDirectory;

    /**
     * We cache the root dir's absolute path as a string since we need it often.
     */
    private String rootDirectoryAbsolutePath;

    /**
     * Map of all supported package types keyed on filename filter regex's that define
     * which files match to which package types.
     */
    private Map<String, SupportedPackageType> supportedPackageTypes;

    protected File getRootDirectory() {
        return this.rootDirectory;
    }

    protected void setRootDirectory(File path) {
        this.rootDirectory = path;
        this.rootDirectoryAbsolutePath = this.rootDirectory.getAbsolutePath();
    }

    protected Map<String, SupportedPackageType> getSupportedPackageTypes() {
        return this.supportedPackageTypes;
    }

    protected void setSupportedPackageTypes(Map<String, SupportedPackageType> supportedPackageTypes) {
        this.supportedPackageTypes = supportedPackageTypes;
    }

    public void initialize(Configuration configuration) throws Exception {
        initializePackageTypes(configuration);
        String pathString = configuration.getSimpleValue("rootDirectory", null);
        setRootDirectory(new File(pathString));
        testConnection();
    }

    public void shutdown() {
        this.rootDirectory = null;
        this.rootDirectoryAbsolutePath = null;
        this.supportedPackageTypes = null;
    }

    public void synchronizePackages(PackageSyncReport report, Collection<ContentSourcePackageDetails> existingPackages) throws Exception {

        // put all existing packages in a "to be deleted" list. As we sync, we will remove
        // packages from this list that still exist on the file system. Any leftover in the list
        // are packages that no longer exist on the file system and should be removed from the server inventory.
        List<ContentSourcePackageDetails> deletedPackages = new ArrayList<ContentSourcePackageDetails>();
        deletedPackages.addAll(existingPackages);

        // sync now
        long before = System.currentTimeMillis();
        syncPackages(report, deletedPackages, getRootDirectory());
        long elapsed = System.currentTimeMillis() - before;

        // if there are packages that weren't found on the file system, tell server to remove them from inventory
        for (ContentSourcePackageDetails p : deletedPackages) {
            report.addDeletePackage(p);
        }

        report.setSummary("Synchronized [" + getRootDirectory() + "]. Elapsed time=[" + elapsed + "] ms");

        return;
    }

    public void testConnection() throws Exception {
        File root = getRootDirectory();

        if (!root.exists()) {
            throw new Exception("Disk source [" + root + "] does not exist");
        }

        if (!root.canRead()) {
            throw new Exception("Not permitted to read disk source [" + root + "] ");
        }

        if (!root.isDirectory()) {
            throw new Exception("Disk source [" + root + "] is not a directory");
        }

        return; // good
    }

    public InputStream getInputStream(String location) throws Exception {
        return new FileInputStream(new File(getRootDirectory(), location));
    }

    /**
     * Recursive function that drills down into subdirectories and builds up the report
     * of packages for all files found. As files are found, their associated packages
     * are removed from <code>packages</code> if they exist - leaving only packages
     * remaining that do not exist on the file system.
     * 
     * @param report the report that we are building up
     * @param packages existing packages not yet found on the file system but exist in server inventory
     * @param directory the directory (and its subdirectories) to scan
     * @throws Exception if the sync fails
     */
    protected void syncPackages(PackageSyncReport report, List<ContentSourcePackageDetails> packages, File directory) throws Exception {

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                // process the directory recursively
                syncPackages(report, packages, file);
            } else {
                // process the file
                ContentSourcePackageDetails details = createPackage(file);
                if (details != null) {
                    ContentSourcePackageDetails existing = findPackage(packages, details);
                    if (existing == null) {
                        report.addNewPackage(details);
                    } else {
                        packages.remove(existing); // it still exists, remove it from our list
                        if (details.getFileCreatedDate().compareTo(existing.getFileCreatedDate()) > 0) {
                            report.addUpdatedPackage(details);
                        }
                    }
                } else {
                    // file does not match any filter and is therefore an unknown type - ignore it
                }
            }
        }

        return;
    }

    protected ContentSourcePackageDetails createPackage(File file) throws Exception {

        SupportedPackageType supportedPackageType = determinePackageType(file);
        if (supportedPackageType == null) {
            return null; // we can't handle this file - it is an unknown/unsupported package type
        }

        ContentJarFileInfo info = new ContentJarFileInfo(file);
        String md5 = MD5Generator.getDigestString(file);
        String name = file.getName();
        String version = info.getVersion(md5);
        String packageTypeName = supportedPackageType.packageTypeName;
        String architectureName = supportedPackageType.architectureName;
        String resourceTypeName = supportedPackageType.resourceTypeName;
        String resourceTypePluginName = supportedPackageType.resourceTypePluginName;

        ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey(name, version, packageTypeName, architectureName, resourceTypeName,
            resourceTypePluginName);
        ContentSourcePackageDetails pkg = new ContentSourcePackageDetails(key);

        pkg.setDisplayName(name);
        pkg.setFileName(name);
        pkg.setFileCreatedDate(file.lastModified());
        pkg.setFileSize(file.length());
        pkg.setMD5(md5);
        pkg.setLocation(getRelativePath(file));
        pkg.setShortDescription(info.getDescription(null));

        return pkg;
    }

    protected ContentSourcePackageDetails findPackage(List<ContentSourcePackageDetails> packages, ContentSourcePackageDetails pkg) {
        for (ContentSourcePackageDetails p : packages) {
            if (p.equals(pkg)) {
                return p;
            }
        }
        return null;
    }

    protected String getRelativePath(File file) {
        String relativePath;
        String fileAbsolutePath = file.getAbsolutePath();
        int idx = fileAbsolutePath.indexOf(this.rootDirectoryAbsolutePath);
        if (idx > -1) { // this should always be the case, in fact, it should always be 0
            relativePath = fileAbsolutePath.substring(idx + this.rootDirectoryAbsolutePath.length());
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(1);
            }
        } else {
            relativePath = fileAbsolutePath; // this should never happen, but, just in case, default to the full path
        }
        return relativePath;
    }

    protected void initializePackageTypes(Configuration config) {

        Map<String, SupportedPackageType> supportedPackageTypes = new HashMap<String, SupportedPackageType>();

        /* TODO: THE UI CURRENTLY DOES NOT SUPPORT ADDING/EDITING LIST OF MAPS, SO WE CAN ONLY SUPPORT ONE PACKAGE TYPE 
         * SO FOR NOW, JUST SUPPORT A FLAT SET OF SIMPLE PROPS - WE WILL RE-ENABLE THIS CODE LATER */
        if (false) {
            // All of these properties must exist, any nulls should trigger runtime exceptions which is what we want
            // because if the configuration is bad, this content source should not initialize. 
            List<Property> packageTypesList = config.getList("packageTypes").getList();
            for (Property property : packageTypesList) {
                PropertyMap pkgType = (PropertyMap) property;
                SupportedPackageType supportedPackageType = new SupportedPackageType();
                supportedPackageType.packageTypeName = pkgType.getSimpleValue("packageTypeName", null);
                supportedPackageType.architectureName = pkgType.getSimpleValue("architectureName", null);
                supportedPackageType.resourceTypeName = pkgType.getSimpleValue("resourceTypeName", null);
                supportedPackageType.resourceTypePluginName = pkgType.getSimpleValue("resourceTypePluginName", null);

                String filenameFilter = pkgType.getSimpleValue("filenameFilter", null);
                supportedPackageTypes.put(filenameFilter, supportedPackageType);
            }
        } else {
            /* THIS CODE IS THE FLAT SET OF PROPS - USE UNTIL WE CAN EDIT MAPS AT WHICH TIME DELETE THIS ELSE CLAUSE */
            SupportedPackageType supportedPackageType = new SupportedPackageType();
            supportedPackageType.packageTypeName = config.getSimpleValue("packageTypeName", null);
            supportedPackageType.architectureName = config.getSimpleValue("architectureName", null);
            supportedPackageType.resourceTypeName = config.getSimpleValue("resourceTypeName", null);
            supportedPackageType.resourceTypePluginName = config.getSimpleValue("resourceTypePluginName", null);
            String filenameFilter = config.getSimpleValue("filenameFilter", null);
            supportedPackageTypes.put(filenameFilter, supportedPackageType);
        }

        setSupportedPackageTypes(supportedPackageTypes);
        return;
    }

    protected SupportedPackageType determinePackageType(File file) {
        String absolutePath = file.getAbsolutePath();
        for (Map.Entry<String, SupportedPackageType> entry : getSupportedPackageTypes().entrySet()) {
            if (absolutePath.matches(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null; // the file doesn't match any known types for this content source
    }

    protected class SupportedPackageType {
        public String packageTypeName;
        public String architectureName;
        public String resourceTypeName;
        public String resourceTypePluginName;
    }
}