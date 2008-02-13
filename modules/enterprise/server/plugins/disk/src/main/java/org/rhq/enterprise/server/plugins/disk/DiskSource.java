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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.rhq.core.clientapi.server.plugin.content.ContentSourceAdapter;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.configuration.Configuration;

/**
 * This is the most basic <i>reference</i> implementation of a content source. It provides primative package
 * synchronization with file-system based source. It is anticipated that more content aware subclasses will provide more
 * useful functionality.
 *
 * @author jortel
 */
public class DiskSource implements ContentSourceAdapter {
    /**
     * The root path (directory) from which to synchronize content.
     */
    protected String path;

    /**
     * Initialize the adapter. The configuration is expected to have a simple string property named: <b>path</b>.
     */
    public void initialize(Configuration configuration) throws Exception {
        path = configuration.getSimpleValue("path", null);
        testConnection();
    }

    /**
     * Shutdown the adapter.
     */
    public void shutdown() {
    }

    /**
     * Synchronize with the collection of packages specified by <i>path</i>.
     *
     * @param  report           The resulting synchronization report.
     * @param  existingPackages A list of packages already in inventory.
     *
     * @throws Exception
     */
    public void synchronizePackages(PackageSyncReport report, Collection<ContentSourcePackageDetails> existingPackages)
        throws Exception {
        File dir = new File(path);
        List<ContentSourcePackageDetails> deletedPackages = new ArrayList<ContentSourcePackageDetails>();
        deletedPackages.addAll(existingPackages);
        syncPackages(report, deletedPackages, dir);
        for (ContentSourcePackageDetails p : deletedPackages) {
            report.addDeletePackage(p);
        }
    }

    /**
     * Test the connection with equates to ensuring that the <i>path</i> references an existing directory that is
     * readable.
     *
     * @throws Exception When the path is not valid.
     */
    public void testConnection() throws Exception {
        File file = new File(path);
        if (file.exists() || file.canRead() || file.isDirectory()) {
            return; // good
        }

        throw new Exception("Path: '" + path + "' not found, not a directory or permission denied");
    }

    /**
     * Get an input stream to the specified packages bits. Uses the index first, then searches the directory tree.
     *
     * @param  @ param location The location relative to the basedir
     *
     * @return An input stream.
     *
     * @throws Exception On error.
     */
    public InputStream getInputStream(String location) throws Exception {
        return new FileInputStream(location);
    }

    /**
     * Performs the <i>heavy-lifting</i> for synchronization. Recursivly traverses directories and add packages to the
     * <i>add</i> list that aren't already in inventory, add packages with a newer file data into the <i>updated</i>
     * list and any packages that are in the existing list that aren't accounted for in on the filesystem are added to
     * the <i>deleted</i> list.
     *
     * @param  report           The resulting synchronization report.
     * @param  existingPackages A list of packages already in inventory.
     * @param  directory        The directory to process.
     *
     * @throws Exception On all errors.
     */
    private void syncPackages(PackageSyncReport report, List<ContentSourcePackageDetails> packages, File directory)
        throws Exception {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                syncPackages(report, packages, file);
                continue;
            }

            ContentSourcePackageDetails p = createPackage(file);
            ContentSourcePackageDetails existing = findPackage(packages, p);
            if (existing == null) {
                report.addNewPackage(p);
                continue;
            }

            packages.remove(existing);
            if (p.getFileCreatedDate().after(existing.getFileCreatedDate())) {
                report.addUpdatedPackage(p);
            }
        }
    }

    /**
     * Create a {@link org.rhq.core.domain.content.PackageDetails } object for the specified file object.
     *
     * @param  file A file for which to create the package specification.
     *
     * @return A {@link org.rhq.core.domain.content.PackageDetails }
     */
    private ContentSourcePackageDetails createPackage(File file) {
        String version = String.valueOf(file.lastModified());
        ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey(file.getName(), version, "rpm",
            "noarch", "Linux", "Platforms");
        ContentSourcePackageDetails pkg = new ContentSourcePackageDetails(key);
        pkg.setDisplayName(file.getName());
        pkg.setFileCreatedDate(new Date(file.lastModified()));
        pkg.setFileSize(file.length());
        pkg.setLocation(file.getAbsolutePath());
        return pkg;
    }

    /**
     * Find the specified package is the list.
     *
     * @param  packages A list of packages.
     * @param  pkg      The package to find.
     *
     * @return The package when found, else null.
     */
    private ContentSourcePackageDetails findPackage(List<ContentSourcePackageDetails> packages,
        ContentSourcePackageDetails pkg) {
        for (ContentSourcePackageDetails p : packages) {
            if (p.equals(pkg)) {
                return p;
            }
        }

        return null;
    }

    /**
     * Test harness
     *
     * @param  args
     *
     * @throws Exception TODO: REMOVE BEFORE PRODUCTION BUILD.
     */
    public static void main(String[] args) throws Exception {
        DiskSource ds = new DiskSource();
        ds.path = "/opt/yum";
        PackageSyncReport report = new PackageSyncReport();
        Set<ContentSourcePackageDetails> existingPackages = new HashSet<ContentSourcePackageDetails>();
        ds.synchronizePackages(report, existingPackages);
    }
}