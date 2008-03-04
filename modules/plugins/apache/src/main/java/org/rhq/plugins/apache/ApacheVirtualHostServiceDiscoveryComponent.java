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

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;

/**
 * @author Ian Springer
 */
public class ApacheVirtualHostServiceDiscoveryComponent implements ResourceDiscoveryComponent<ApacheServerComponent> {
    private static final String RT_LOG_FILE_NAME_SUFFIX = "_rt.log";

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Discovers the VirtualHosts that are deployed on the specified Apache server and creates and returns corresponding
     * JON services.
     *
     * @see ResourceDiscoveryComponent#discoverResources(ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheServerComponent> discoveryContext) throws Exception {
        SNMPSession snmpSession = discoveryContext.getParentResourceComponent().getSNMPSession();

        List<SNMPValue> nameValues;
        List<SNMPValue> portValues;
        SNMPValue descValue;

        try {
            nameValues = snmpSession.getColumn(SNMPConstants.COLUMN_VHOST_NAME);
        } catch (SNMPException e) {
            throw new Exception(
                "Error getting SNMP column: " + SNMPConstants.COLUMN_VHOST_NAME + ": " + e.getMessage(), e);
        }

        try {
            portValues = snmpSession.getColumn(SNMPConstants.COLUMN_VHOST_PORT);
        } catch (SNMPException e) {
            throw new Exception(
                "Error getting SNMP column: " + SNMPConstants.COLUMN_VHOST_PORT + ": " + e.getMessage(), e);
        }

        try {
            // Just get the first one - they are all the same.
            descValue = snmpSession.getNextValue(SNMPConstants.COLUMN_VHOST_DESC);
        } catch (SNMPException e) {
            throw new Exception("Error getting SNMP value: " + SNMPConstants.COLUMN_VHOST_DESC + ": " + e.getMessage(),
                e);
        }

        ApacheServerComponent parentApacheComponent = discoveryContext.getParentResourceComponent();
        File configPath = parentApacheComponent.getServerRoot();
        File logsDir = new File(configPath, "logs");

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();
        for (int i = 0; i < nameValues.size(); i++) {
            SNMPValue nameValue = nameValues.get(i);
            String host = nameValue.toString();
            SNMPValue portValue = portValues.get(i);
            String fullPort = portValue.toString();

            // The port value will be in the form "1.3.6.1.2.1.6.XXXXX",
            // where "1.3.6.1.2.1.6" represents the TCP protocol ID,
            // and XXXXX is the actual port number
            String port = fullPort.substring(fullPort.lastIndexOf(".") + 1);
            String key = host + ":" + port;
            String name = "Virtual Host " + key;
            String version = null; // virtualhosts don't have versions.
            String desc = descValue.toString();
            DiscoveredResourceDetails resourceDetails = new DiscoveredResourceDetails(discoveryContext
                .getResourceType(), key, name, version, desc, null, null);

            // Init the plugin config...
            Configuration pluginConfig = resourceDetails.getPluginConfiguration();

            // The VirtualHost ResourceComponent will need the wwwService index to construct SNMP OIDs for metrics.
            String nameOID = nameValue.getOID();
            String wwwServiceIndex = nameOID.substring(nameOID.lastIndexOf(".") + 1);

            Property wwwServiceIndexProp = new PropertySimple(
                ApacheVirtualHostServiceComponent.SNMP_WWW_SERVICE_INDEX_CONFIG_PROP, wwwServiceIndex);
            pluginConfig.put(wwwServiceIndexProp);

            String url = "http://" + host + ":" + port + "/";
            Property urlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, url);
            pluginConfig.put(urlProp);

            String rtLogFileName = host + port + RT_LOG_FILE_NAME_SUFFIX;
            File rtLogFile = new File(logsDir, rtLogFileName);
            pluginConfig.put(new PropertySimple(ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP,
                rtLogFile));

            log.debug("Plugin config: " + pluginConfig);

            discoveredResources.add(resourceDetails);
        }

        return discoveredResources;
    }
}