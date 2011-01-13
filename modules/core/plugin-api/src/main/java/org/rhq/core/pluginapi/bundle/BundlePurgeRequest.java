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

import java.io.Serializable;

import org.rhq.core.domain.bundle.BundleResourceDeployment;

/**
 * A request to purge a bundle, essentially erasing the bundle files from the destination.
 *
 * @author John Mazzitelli
 */
public class BundlePurgeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private BundleResourceDeployment resourceDeployment;
    private BundleManagerProvider bundleManagerProvider;

    public BundlePurgeRequest() {
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

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getClass() + ": ");
        str.append("live-deployment-to-be-purge=[").append(resourceDeployment.toString()).append("], ");
        return str.toString();
    }
}
