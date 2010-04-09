/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pluginapi.bundle;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.content.PackageVersion;

/**
 * A request to deploy a bundle.
 *
 * @author John Mazzitelli
 */
public class BundleDeployRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private BundleDeployment bundleDeployment;
    private BundleManagerProvider bundleManagerProvider;
    private File bundleFilesLocation;
    private Map<PackageVersion, File> packageVersionFiles;

    public BundleDeployRequest() {
    }

    /**
     * This returns the location where the plugin container has downloaded the bundle files.
     * 
     * @return the location where the bundle files have been downloaded
     */
    public File getBundleFilesLocation() {
        return this.bundleFilesLocation;
    }

    public void setBundleFilesLocation(File bundleFilesLocation) {
        this.bundleFilesLocation = bundleFilesLocation;
    }

    /**
     * Maps all the package versions associated with the bundle and the locations
     * where the package version contents can be found. These will all be located
     * somewhere under the {@link #getBundleFilesLocation() bundle files location}.
     * 
     * @return packages and their locations on the local file system
     */
    public Map<PackageVersion, File> getPackageVersionFiles() {
        return this.packageVersionFiles;
    }

    public void setPackageVersionFiles(Map<PackageVersion, File> packageVersionFiles) {
        this.packageVersionFiles = packageVersionFiles;
    }

    public BundleDeployment getBundleDeployment() {
        return bundleDeployment;
    }

    public void setBundleDeployment(BundleDeployment bundleDeployment) {
        this.bundleDeployment = bundleDeployment;
    }

    public BundleManagerProvider getBundleManagerProvider() {
        return this.bundleManagerProvider;
    }

    public void setBundleManagerProvider(BundleManagerProvider provider) {
        this.bundleManagerProvider = provider;
    }

}
