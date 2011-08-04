/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.clientapi.agent.bundle;

import java.io.Serializable;

import org.rhq.core.domain.bundle.BundleResourceDeployment;

/**
 * The request that the server sends down to the agent to purge the live bundle deployment.
 * 
 * @author John Mazzitelli
 */
public class BundlePurgeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private BundleResourceDeployment resourceDeployment;

    public BundlePurgeRequest(BundleResourceDeployment resourceDeployment) {
        this.resourceDeployment = resourceDeployment;
    }

    /**
     * @return the live resource deployment that is to be purged
     */
    public BundleResourceDeployment getLiveBundleResourceDeployment() {
        return resourceDeployment;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getClass() + ": ");
        str.append("live-deployment=[").append(resourceDeployment.toString()).append("]");
        return str.toString();
    }
}
