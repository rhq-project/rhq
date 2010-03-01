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

import org.rhq.core.domain.bundle.BundleDeployDefinition;

/**
 * A request to deploy a bundle.
 *
 * @author John Mazzitelli
 */
public class BundleDeployRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private BundleDeployDefinition bundleDeployDefinition;
    private BundleManagerProvider bundleManager;

    public BundleDeployDefinition getBundleDeployDefinition() {
        return bundleDeployDefinition;
    }

    public void setBundleDeployDefinition(BundleDeployDefinition bundleDeployDefinition) {
        this.bundleDeployDefinition = bundleDeployDefinition;
    }

    public BundleManagerProvider getBundleManagerProvider() {
        return bundleManager;
    }

    public void setBundleManagerProvider(BundleManagerProvider provider) {
        this.bundleManager = provider;
    }

}
