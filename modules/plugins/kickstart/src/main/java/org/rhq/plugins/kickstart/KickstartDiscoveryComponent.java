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
package org.rhq.plugins.kickstart;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Looks for koan on the machine so that it can do kickstart installations.
 */
public class KickstartDiscoveryComponent implements ResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(KickstartDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        SystemInfo sysinfo = context.getSystemInformation();
        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();
        //TODO This will really only work on rpm based systems.
        if (OperatingSystemType.LINUX == sysinfo.getOperatingSystemType()) {
            if (packageExists(sysinfo, "koan")) {
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(),
                    "Kickstart", "Kickstart", "1", "Koan Kickstart Engine", null, null);

                set.add(detail);
            }
        }

        return set;
    }

    public boolean packageExists(SystemInfo sysinfo, String packageName) {
        boolean exists = false;
        ProcessExecution pe = new ProcessExecution("/bin/rpm");
        pe.setCaptureOutput(true);
        pe.setCheckExecutableExists(true);
        pe.setArguments(Arrays.asList("-qa", packageName));
        ProcessExecutionResults pr = sysinfo.executeProcess(pe);
        if (pr.getExitCode() == 0) {
            String output = pr.getCapturedOutput();
            exists = output.contains(packageName);
        }
        return exists;
    }
}