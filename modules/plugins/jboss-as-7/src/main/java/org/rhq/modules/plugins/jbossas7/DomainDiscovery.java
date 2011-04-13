/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

/**
 * Discover the domain. This is done by scanning for host controllers.
 * If they are also DC, the domain-controller element points to local.
 *
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class DomainDiscovery extends AbstractBaseDiscovery<BaseComponent>  {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent> context)
            throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(1);


        List<ProcessScanResult> scans = context.getAutoDiscoveredProcesses();

        for (ProcessScanResult psr : scans) {

            // get the HostController, to find host.xml
            String psName = psr.getProcessScan().getName();
            if (!psName.equals("HostController"))
                continue;

            // Now we have the host controller, lets get the host.xml file
            // and obtain the domain controller info from there
            ProcessInfo processInfo = psr.getProcessInfo();
            readHostXml(processInfo);
            HostPort dcHp = getDomainControllerFromHostXml();

            if (!dcHp.isLocal) {
                log.info("Domain controller is not local, but at " + dcHp);
                continue;
            }

            // Ok, this is a domain controller, so we can return a Domain resource.

            // Get the management port and save for later use
            HostPort managementHostPort = getManagementPortFromHostXml();
            Configuration config = context.getDefaultPluginConfiguration();
            config.put(new PropertySimple("port",managementHostPort.port));
            config.put(new PropertySimple("hostname",managementHostPort.host));

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    context.getResourceType(), // DataType
                    "Domain", // Key
                    "Domain", // Name
                    null, // Version
                    context.getResourceType().getDescription(), // Description
                    config,
                    null);
            details.add(detail);
        }
        return details;
    }
}