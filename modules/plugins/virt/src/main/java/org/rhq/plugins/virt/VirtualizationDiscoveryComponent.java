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
package org.rhq.plugins.virt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Greg Hinkle
 */
public class VirtualizationDiscoveryComponent implements ResourceDiscoveryComponent {

    private Log log = LogFactory.getLog(getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        LibVirtConnection virt = new LibVirtConnection();
        int[] ids;
        try {
            ids = virt.getDomainIds();
        }
        catch (Exception e) {
            log.info(e.getMessage());
            return null;
        }
        List<String> guests = virt.getDomainNames();

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        for (int id : ids) {
            if ((id == 0 && resourceDiscoveryContext.getResourceType().getName().equals("Virtual Host"))
               || (id != 0 && !resourceDiscoveryContext.getResourceType().getName().equals("Virtual Host"))) {

                LibVirtConnection.DomainInfo domainInfo = virt.getDomainInfo(id);

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    resourceDiscoveryContext.getResourceType(), domainInfo.name, domainInfo.name, "0", // TODO - Change to domain id?
                    "Virtualization Domain", null, null);

                details.add(detail);
            }

            if (!resourceDiscoveryContext.getResourceType().getName().equals("Virtual Host")) {
                for (String guestName : guests) {
                    LibVirtConnection.DomainInfo domainInfo = virt.getDomainInfo(guestName);
                    DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                        resourceDiscoveryContext.getResourceType(), domainInfo.name, domainInfo.name, "0", // TODO - Change to domain id?
                        "Virtualization Guest Domain", null, null);

                    details.add(detail);
                }
            }
        }

        return details;
    }

    // This test hack calls virsh
    public static void main(String[] args) throws IOException {
        String virsh = "/usr/bin/virsh";

        ProcessBuilder pb = new ProcessBuilder(virsh, "list");
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        StringBuilder builder = new StringBuilder();
        input.readLine();
        input.readLine();

        while ((line = input.readLine()) != null) {
            String[] vars = line.split("\\s*");
            for (String var : vars) {
                System.out.println(var);
            }
        }

        input.close();

        System.out.println(builder.toString());
    }
}