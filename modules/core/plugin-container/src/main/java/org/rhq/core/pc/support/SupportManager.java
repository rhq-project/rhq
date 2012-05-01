/*
 * RHQ Management Platform
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
package org.rhq.core.pc.support;

import java.io.InputStream;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.support.SupportAgentService;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.support.SnapshotReportRequest;
import org.rhq.core.pluginapi.support.SnapshotReportResults;
import org.rhq.core.pluginapi.support.SupportFacet;

/**
 * Manages the inventory's support subsystem, such as the generation of snapshot reports for all resources across all plugins.
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author John Mazzitelli
 */
public class SupportManager extends AgentService implements SupportAgentService, ContainerService {

    public SupportManager() {
        super(SupportAgentService.class);
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        return;
    }

    public void initialize() {
        return;
    }

    public void shutdown() {
        return;
    }

    public InputStream getSnapshotReport(int resourceId, String name, String description) throws Exception {
        SupportFacet facet = getSupportFacet(resourceId, 600000L); // give it enough time to zip up all the snapshot content 
        SnapshotReportRequest request = new SnapshotReportRequest(name, description);
        SnapshotReportResults results = facet.getSnapshotReport(request);
        InputStream inputStream = results.getInputStream();
        inputStream = remoteInputStream(inputStream);
        return inputStream;
    }

    /**
     * Given a resource, this obtains that resource's {@link SupportFacet} interface.
     * If the resource does not support that facet, an exception is thrown.
     * The resource must be in the STARTED (i.e. connected) state.
     *
     * @param  resourceId identifies the resource that is to be snapshotted
     *
     * @return the resource's snapshot report facet component
     *
     * @throws PluginContainerException on error
     */
    protected SupportFacet getSupportFacet(int resourceId, long facetMethodTimeout) throws PluginContainerException {

        return ComponentUtil.getComponent(resourceId, SupportFacet.class, FacetLockType.READ, facetMethodTimeout,
            false, true);
    }
}
