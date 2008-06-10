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

package org.rhq.plugins.sshd;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.system.ProcessInfo;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Greg Hinkle
 */
public class OpenSSHDDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
         for (ProcessScanResult result : ((List <ProcessScanResult>)resourceDiscoveryContext.getAutoDiscoveredProcesses())) {

            ProcessInfo info  = result.getProcessInfo();

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                resourceDiscoveryContext.getResourceType(),
                    "sshd",
                    "OpenSSH sshd",
                    "1",
                    "Open SSH Daemon " + info.getName(),
                    null,
                    null);
            
            details.add(detail);
        }

        return details;
    }
}
