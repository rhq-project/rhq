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
package org.rhq.plugins.apache;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.apache.util.HttpdConfParser;
import org.rhq.plugins.apache.util.WWWUtils;

/**
 * Discovery of an installed mod_jk module in the parents component apache server
 *
 * @author Heiko W. Rupp
 */
public class ModJKDiscoveryComponent implements ResourceDiscoveryComponent<ApacheServerComponent> {

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Try to detect a mod_jk. We do this by looking in httpd.conf if the module should be loaded
     * @param context The context that provides metadata
     * @return A potentially empty set of details
     * @throws InvalidPluginConfigurationException
     * @throws Exception
     */
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ApacheServerComponent> context) throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(1);

        Configuration parentConfig = context.getParentResourceContext().getPluginConfiguration();

        // Try to determine the presence of mod_jk and its version via a http request
        PropertySimple urlString = parentConfig.getSimple(ApacheServerConfiguration.PLUGIN_CONFIG_PROP_URL);
        String modJkVersion = null;
        if (urlString!=null) {
            try {
                URL url = new URL(urlString.getStringValue());
                String header = WWWUtils.getServerHeader(url);
                if (header!=null) {
                    if (header.contains("mod_jk")) {
                        header = header.substring(header.indexOf("mod_jk")+7);
                        int blankPos = header.indexOf(" ");
                        if (blankPos>0)
                            modJkVersion = header.substring(0,header.indexOf(" "));
                        else
                            modJkVersion = header;
                    }
                }
            }
            catch (MalformedURLException e) {
                // nothing to do
            }
        }

        PropertySimple confPathProp = parentConfig.getSimple(ApacheServerConfiguration.PLUGIN_CONFIG_PROP_HTTPD_CONF);
        if (confPathProp == null || confPathProp.getStringValue() == null) {
            log.error("Path to httpd.conf is not given - can't discover mod_jk");
            return null;
        }

        //TODO the can be simplified using augeas...
        String confPath = confPathProp.getStringValue();
        if (!confPath.startsWith("/")) { // TODO implement for Windows too
            String basePath = context.getParentResourceComponent().getServerConfiguration().getServerRoot().getAbsolutePath();
            confPath = basePath + "/" + confPath;
        }

        HttpdConfParser parser = new HttpdConfParser();
        if (parser.parse(confPath) && parser.isModJkInstalled()) {

            Configuration config = context.getDefaultPluginConfiguration();
            if (parser.getWorkerPropertiesFile()!=null) {
                PropertySimple workers = new PropertySimple("workerFile", parser.getWorkerPropertiesFile());
                config.put(workers);
            }
            if (parser.getUriWorkerLocation()!=null) {
                PropertySimple workers = new PropertySimple("uriWorkerFile", parser.getUriWorkerLocation());
                config.put(workers);
            }


            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(),
                    confPath,"mod_jk",modJkVersion,"Mod_JK", config, null);
            details.add(detail);
            return details;
        }

        return null;
    }

}
