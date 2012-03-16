/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.pc.inventory.getnativeprocess.testplugin;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class TestDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> context)
        throws InvalidPluginConfigurationException, Exception {
        
        if (context.getAutoDiscoveredProcesses().size() != 1) {
            throw new IllegalStateException("Only a single test process is expected but there are " + context.getAutoDiscoveredProcesses().size());
        }

        TestComponent.DISCOVERY_CALLS_COUNT.incrementAndGet();
                
        ProcessInfo pinfo = context.getAutoDiscoveredProcesses().get(0).getProcessInfo();
        return Collections.singleton(new DiscoveredResourceDetails(context.getResourceType(), "KEY", "NAME", "VERSION", null, context.getDefaultPluginConfiguration(), pinfo));
    }

}
