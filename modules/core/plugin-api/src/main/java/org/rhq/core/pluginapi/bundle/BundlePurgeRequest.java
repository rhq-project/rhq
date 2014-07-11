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
import java.net.URI;

import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.configuration.Configuration;

/**
 * A request to purge a bundle, essentially erasing the bundle files from the destination.
 *
 * @since 4.0
 * @author John Mazzitelli
 */
public class BundlePurgeRequest implements Serializable {
    private static final long serialVersionUID = 2L;

    private BundleResourceDeployment resourceDeployment;
    private BundleManagerProvider bundleManagerProvider;
    private URI destinationTarget;
    private Configuration referencedConfiguration;

    public BundlePurgeRequest() {
    }

    /**
     * Returns the full, absolute directory as found on the local machine's file system
     * where the bundle should be deployed. This is the bundle destination's
     * {@link BundleDestination#getDeployDir() relative destination directory} under the
     * {@link BundleDestination#getDestinationBaseDirectoryName() destination base directory}.
     *
     * @since 4.1
     * @deprecated since 4.13, use {@link #getDestinationTarget()} instead
     */
    @Deprecated
    public File getAbsoluteDestinationDirectory() {
        if (destinationTarget == null) {
            return null;
        }

        if (!"file".equals(destinationTarget.getScheme())) {
            return null;
        } else {
            return new File(destinationTarget.getPath());
        }
    }

    /**
     * @since 4.1
     *
     * @param absoluteDestDir
     *
     * @deprecated since 4.13, use {@link #setDestinationTarget(java.net.URI)} instead
     */
    @Deprecated
    public void setAbsoluteDestinationDirectory(File absoluteDestDir) {
        destinationTarget = absoluteDestDir.toURI();
    }

    /**
     * For filesystem-bound bundle destinations, this URI will have the {@code file} scheme and will be the absolute
     * directory as found on the local machine's file system where the bundle should be deployed. In another words,
     * for the filesystem-bound bundle destinations, this is the bundle destination's
     * {@link BundleDestination#getDeployDir() relative destination directory} under the
     * {@link BundleDestination#getDestinationBaseDirectoryName() destination base directory}.
     * <p/>
     * For bundle destinations defined using the bundle locations (which are just generalized expressions that should
     * form a valid URI), the URI will be processed from the templated destination location provided in the bundle
     * target of the resource's type.
     *
     * @since 4.13
     */
    public URI getDestinationTarget() {
        return destinationTarget;
    }

    /**
     * @see #getDestinationTarget()
     * @since 4.13
     */
    public void setDestinationTarget(URI destinationTarget) {
        this.destinationTarget = destinationTarget;
    }

    /**
     * This is information about the last known live resource deployment - this is to be purged.
     *
     * @return information about the live resource deployment that is to be purged
     */
    public BundleResourceDeployment getLiveResourceDeployment() {
        return resourceDeployment;
    }

    public void setLiveResourceDeployment(BundleResourceDeployment resourceDeployment) {
        this.resourceDeployment = resourceDeployment;
    }

    public BundleManagerProvider getBundleManagerProvider() {
        return this.bundleManagerProvider;
    }

    public void setBundleManagerProvider(BundleManagerProvider provider) {
        this.bundleManagerProvider = provider;
    }

    /**
     * The destination specification can pass over some info to the bundle handler by using references to configuration
     * or metric data. A destination using this approach should usually be dedicated to a single bundle handler (as of
     * now, there is no enforcement/validation in place that would ensure that only a bundle handler a destination spec
     * is dedicated for will be used to install data to the destination).
     *
     * @return a configuration object containing data from the resource that is required by the bundle handler.
     * @since 4.13
     */
    public Configuration getReferencedConfiguration() {
        return referencedConfiguration;
    }

    /**
     * @see #getReferencedConfiguration()
     * @since 4.13
     */
    public void setReferencedConfiguration(Configuration referencedConfiguration) {
        this.referencedConfiguration = referencedConfiguration;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getClass() + ": ");
        str.append("live-deployment-to-be-purge=[").append(resourceDeployment.toString()).append("], ");
        str.append("target=[").append(destinationTarget.toString()).append("]");
        return str.toString();
    }
}
