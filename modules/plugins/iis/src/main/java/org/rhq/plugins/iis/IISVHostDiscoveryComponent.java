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
package org.rhq.plugins.iis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.sigar.win32.Pdh;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.iis.util.IISMetaBase;

/**
 *
 * @author Greg Hinkle
 */
public class IISVHostDiscoveryComponent implements ResourceDiscoveryComponent<IISServerComponent> {

    private static final String PDH_WEB_SERVICE = "Web Service";
    private static final String PDH_TOTAL = "_Total";

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<IISServerComponent> resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        List<String> vhosts = new ArrayList<String>();

        String[] instances = Pdh.getInstances(PDH_WEB_SERVICE);

        Set<DiscoveredResourceDetails> hostDetails = new HashSet<DiscoveredResourceDetails>();

        for (String instance : instances) {
            if (!instance.equals(PDH_TOTAL)) {
                vhosts.add(instance);
            }
        }

        Map<String, IISMetaBase> websites = IISMetaBase.getWebSites();

        for (String siteName : vhosts) {

            IISMetaBase info = websites.get(siteName);

            if (info == null) {
                continue;
            }

            Configuration config = resourceDiscoveryContext.getDefaultPluginConfiguration();

            config.put(new PropertySimple("siteName", siteName));
            config.put(new PropertySimple("port", info.getPort()));
            config.put(new PropertySimple("ipAddress", info.getIp()));
            config.put(new PropertySimple("docRoot", info.getPath()));
            config.put(new PropertySimple("hostname", info.getHostname()));
            config.put(new PropertySimple("ssl", info.isRequireSSL()));

            // Auto-configure response-time properties.  IIS 5.x and 6.x put
            // logs by default in system32.  (Even though IIS 5.x installs
            // into C:\Windows\System32\inetsrv).  Should try to get this
            // info from either metabase or the registry, though this will
            // cover most cases.
            config.put(new PropertySimple("logDirectory", "C:\\Windows\\System32\\LogFiles\\W3SVC" + info.getId()));

            DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceDiscoveryContext
                .getResourceType(), siteName, siteName, "1.0", siteName + " Virtual Host", config, null);

            hostDetails.add(details);
        }

        return hostDetails;
    }
}
