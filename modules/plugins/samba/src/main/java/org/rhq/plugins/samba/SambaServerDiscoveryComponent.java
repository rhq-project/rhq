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
package org.rhq.plugins.samba;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;

/**
 * @author Greg Hinkle
 */
public class SambaServerDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        File confFile = new File("/etc/samba/smb.conf");

        ProcessExecution pe = new ProcessExecution("/usr/sbin/smbd");
        pe.setArguments(new String[]{"--version"});
        pe.setCaptureOutput(true);
        ProcessExecutionResults per =
                resourceDiscoveryContext.getSystemInformation().executeProcess(pe);
        String data = per.getCapturedOutput();
        String version = data.replace("Version","").trim();
        
        if (confFile.exists()) {
            DiscoveredResourceDetails resource =
                new DiscoveredResourceDetails(resourceDiscoveryContext.getResourceType(), "samba", "Samba Server",
                    version, "Samba services", null, null);

            details.add(resource);
        }

        return details;
    }
}
