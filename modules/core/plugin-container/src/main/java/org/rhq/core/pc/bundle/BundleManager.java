/*
 * RHQ Management
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.pc.bundle;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.bundle.BundleAgentService;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.bundle.BundleFacet;

/**
 * Manages the bundle subsystem, which allows bundles of content to be installed. 
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author John Mazzitelli
 */
public class BundleManager extends AgentService implements BundleAgentService, ContainerService {

    public BundleManager() {
        super(BundleAgentService.class);
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
    }

    public void initialize() {
    }

    public void shutdown() {
    }

    /* (non-Javadoc)
     * @see org.rhq.core.clientapi.agent.bundle.BundleAgentService#dummy()
     */
    public void dummy() {
        // TODO Auto-generated method stub

    }

    /**
     * Given a resource, this obtains that resource's {@link BundleFacet} interface.
     * If the resource does not support that facet, an exception is thrown.
     * The resource must be in the STARTED (i.e. connected) state.
     *
     * @param  resourceId identifies the resource that is to perform the bundle activities
     *
     * @return the resource's bundle facet interface
     *
     * @throws PluginContainerException on error
     */
    protected BundleFacet getBundleFacet(int resourceId, long facetMethodTimeout) throws PluginContainerException {
        return ComponentUtil.getComponent(resourceId, BundleFacet.class, FacetLockType.READ, facetMethodTimeout, false,
            true);
    }
}
