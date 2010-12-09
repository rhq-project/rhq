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
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.HttpdAddressUtility.Address;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;

/**
 * Discovers VirtualHosts under the Apache server by reading them out from Augeas tree constructed
 * in the parent component. If Augeas is not present, an attempt is made to discover the vhosts using
 * SNMP module.
 * 
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class ApacheVirtualHostServiceDiscoveryComponent implements ResourceDiscoveryComponent<ApacheServerComponent>, ResourceUpgradeFacet<ApacheServerComponent> {

    private static final String COULD_NOT_DETERMINE_THE_VIRTUAL_HOST_ADDRESS = "*** Could not determine the virtual host address ***";

    public static final String LOGS_DIRECTORY_NAME = "logs";

    private static final String RT_LOG_FILE_NAME_SUFFIX = "_rt.log";

    private static final Log log = LogFactory.getLog(ApacheVirtualHostServiceDiscoveryComponent.class);
    
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ApacheServerComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();

        ApacheServerComponent serverComponent = context.getParentResourceComponent();
        ApacheDirectiveTree tree = serverComponent.loadParser();
        //first define the root server as one virtual host
        discoverMainServer(context, discoveredResources);

        ResourceType resourceType = context.getResourceType();

        File configPath = serverComponent.getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);

        for(VHostSpec vhost : VHostSpec.detect(tree)) {
            
            String firstAddress = vhost.hosts.get(0);

            String resourceKey = createResourceKey(vhost.serverName, vhost.hosts);
            String resourceName = resourceKey; //this'll get overridden below if we find a better value using the address variable

            Configuration pluginConfiguration = context.getDefaultPluginConfiguration();

            Address address = serverComponent.getAddressUtility().getVirtualHostSampleAddress(tree, firstAddress, vhost.serverName, false);
            if (address != null) {
                String scheme = address.scheme;
                String hostToPing = address.host;
                int portToPing = address.port;
                if (address.isPortWildcard() || !address.isPortDefined()) {
                    Address serverAddress = serverComponent.getAddressUtility().getMainServerSampleAddress(tree, hostToPing, 0);
                    if (serverAddress != null) {
                        portToPing = serverAddress.port;
                    } else {
                        portToPing = Address.PORT_WILDCARD_VALUE;
                    }
                }
                if (address.isHostDefault() || address.isHostWildcard()) {
                    Address serverAddress = serverComponent.getAddressUtility().getMainServerSampleAddress(tree, null, portToPing);
                    
                    if (serverAddress != null) {
                        hostToPing = serverAddress.host;
                    } else {
                        hostToPing = null;
                    }
                }
                
                String url;
                if (hostToPing != null && portToPing != Address.PORT_WILDCARD_VALUE && portToPing != Address.NO_PORT_SPECIFIED_VALUE) {
                    url = scheme + "://" + hostToPing + ":" + portToPing + "/";
                } else {
                    url = COULD_NOT_DETERMINE_THE_VIRTUAL_HOST_ADDRESS;
                }

                PropertySimple urlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, url);
                pluginConfiguration.put(urlProp);
                
                File rtLogFile = new File(logsDir, address.host + address.port + RT_LOG_FILE_NAME_SUFFIX);
                
                PropertySimple rtLogProp = new PropertySimple(
                    ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP, rtLogFile.toString());
                pluginConfiguration.put(rtLogProp);

                //redefine the resourcename using the virtual host sample address
                resourceName = address.toString(false);
            }
            
            discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null, null,
                pluginConfiguration, null));
        }

        return discoveredResources;
    }

    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<ApacheServerComponent> inventoriedResource) {
        String resourceKey = inventoriedResource.getResourceKey();
        
        if (ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY.equals(resourceKey) ||
            resourceKey.contains("|")) {
            //a new style resource key. we're done.
            return null;
        }
        
        String newResourceKey = null;
        
        ApacheServerComponent serverComponent = inventoriedResource.getParentResourceComponent();
        
        ApacheDirectiveTree tree = serverComponent.loadParser();
        
        List<VHostSpec> vhosts = VHostSpec.detect(tree);

        SnmpWwwServiceIndexes snmpIndexes = getSnmpDiscoveries(serverComponent, inventoriedResource.getParentResourceContext().getResourceKey());

        for (VHostSpec vhost : vhosts) {
            String legacyResourceKey = createLegacyResourceKey(serverComponent, vhost.serverName, vhost.hosts, snmpIndexes);
            if (resourceKey.equals(legacyResourceKey)) {
                newResourceKey = createResourceKey(vhost.serverName, vhost.hosts);
                break;
            }
        }
        
        if (newResourceKey == null) {
            //the last thing to check is whether the inventoried vhost isn't in fact the main server
            String serverUrl = serverComponent.getServerUrl();
            HttpdAddressUtility.Address serverAddress = HttpdAddressUtility.Address.parse(serverUrl);        
            HttpdAddressUtility.Address vhostAddress = HttpdAddressUtility.Address.parse(resourceKey);
            
            if (ApacheVirtualHostServiceComponent.matchRate(Collections.singletonList(serverAddress), vhostAddress) > 0) {
                newResourceKey = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;
            }
        }
        
        if (newResourceKey != null) {
            ResourceUpgradeReport report = new ResourceUpgradeReport();
            report.setNewResourceKey(newResourceKey);
            
            return report;
        } else {
            return null;
        }
    }
    
    private void discoverMainServer(ResourceDiscoveryContext<ApacheServerComponent> context,
        Set<DiscoveredResourceDetails> discoveredResources) throws Exception {

        ResourceType resourceType = context.getResourceType();
        Configuration mainServerPluginConfig = context.getDefaultPluginConfiguration();

        File configPath = context.getParentResourceComponent().getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);

        String mainServerUrl = context.getParentResourceContext().getPluginConfiguration().getSimple(
            ApacheServerComponent.PLUGIN_CONFIG_PROP_URL).getStringValue();
        
        if (mainServerUrl != null && !"null".equals(mainServerUrl)) {
            PropertySimple mainServerUrlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP,
                mainServerUrl);

            mainServerPluginConfig.put(mainServerUrlProp);

            URI mainServerUri = new URI(mainServerUrl);
            String host = mainServerUri.getHost();
            int port = mainServerUri.getPort();
            if (port == -1) {
                port = 80;
            }

            File rtLogFile = new File(logsDir, host + port + RT_LOG_FILE_NAME_SUFFIX);

            PropertySimple rtLogProp = new PropertySimple(
                ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP, rtLogFile.toString());
            mainServerPluginConfig.put(rtLogProp);
        }

        String key = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;
        
        DiscoveredResourceDetails mainServer = new DiscoveredResourceDetails(resourceType,
            key, "Main", null, null,
            mainServerPluginConfig, null);
        discoveredResources.add(mainServer);
    }
    
    public static String createResourceKey(String serverName, List<String> hosts) {
        StringBuilder keyBuilder = new StringBuilder();
        if (serverName != null) {
            keyBuilder.append(serverName);
        }
        keyBuilder.append("|"); //always do this so that we have a clear distinction between old and new style resource keys
        keyBuilder.append(hosts.get(0));
       
        for (int i = 1; i < hosts.size(); ++i){
            keyBuilder.append(" ").append(hosts.get(i));
        }
        
        return keyBuilder.toString();
    }
    
    private static String createLegacyResourceKey(ApacheServerComponent serverComponent, String serverName, List<String> hosts, SnmpWwwServiceIndexes snmpIndexes) {       
        if (snmpIndexes != null) {
            String newStyleResourceKey = createResourceKey(serverName, hosts);
            String legacyResourceKey = null;

            int snmpWwwServiceIndex = ApacheVirtualHostServiceComponent.getMatchingWwwServiceIndex(serverComponent, newStyleResourceKey, snmpIndexes.names, snmpIndexes.ports);
            
            if (snmpWwwServiceIndex > 0) {
                String host = snmpIndexes.names.get(snmpWwwServiceIndex - 1).toString();
                String fullPort = snmpIndexes.ports.get(snmpWwwServiceIndex - 1).toString();

                // The port value will be in the form "1.3.6.1.2.1.6.XXXXX",
                // where "1.3.6.1.2.1.6" represents the TCP protocol ID,
                // and XXXXX is the actual port number
                String port = fullPort.substring(fullPort.lastIndexOf(".") + 1);
                legacyResourceKey = host + ":" + port;
            }
            
            if (legacyResourceKey != null) {
                return legacyResourceKey;
            }
        }
        
        //try to derive the same resource key as the SNMP would have... this is to prevent the duplication of
        //vhost resources after the SNMP was configured - how I wish resource upgrade made it to 3.0 to prevent this
        //kind of guessing being necessary.
        String host = hosts.get(0);
        HttpdAddressUtility.Address hostAddr = HttpdAddressUtility.Address.parse(host);
        if (serverName != null) {
            HttpdAddressUtility.Address serverAddr = HttpdAddressUtility.Address.parse(serverName);
            hostAddr.host = serverAddr.host;
        }
        
        //the SNMP module seems to resolve the IPs to hostnames.
        try {
            InetAddress hostName = InetAddress.getByName(hostAddr.host);
            hostAddr.host = hostName.getHostName();
        } catch (UnknownHostException e) {
            log.debug("Host " + hostAddr.host + " is not resolvable.", e);
        } 
        
        return hostAddr.host + ":" + hostAddr.port;
    }
    
    /**
     * @param discoveryContext
     * @return
     */
    private static SnmpWwwServiceIndexes getSnmpDiscoveries(ApacheServerComponent serverComponent, String serverResourceKey) {
        try {
            SNMPSession snmpSession = serverComponent.getSNMPSession();
            List<SNMPValue> nameValues;
            List<SNMPValue> portValues;
            
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
            
            SnmpWwwServiceIndexes ret = new SnmpWwwServiceIndexes();
            ret.names = nameValues;
            ret.ports = portValues;
            
            return ret;
        } catch (Exception e) {
            log.debug("Error while trying to contact SNMP of the apache server " + serverResourceKey, e);
            return null;
        }
    }
    
    private static class SnmpWwwServiceIndexes {
        public List<SNMPValue> names;
        public List<SNMPValue> ports;
    }
    
    private static class VHostSpec {
        public String serverName;
        public List<String> hosts;
        
        public static List<VHostSpec> detect(ApacheDirectiveTree config) {
            List<ApacheDirective> virtualHosts = config.search("/<VirtualHost");
            
            List<VHostSpec> ret = new ArrayList<VHostSpec>(virtualHosts.size());
            
            for(ApacheDirective dir : virtualHosts) {
                ret.add(new VHostSpec(dir));
            }
            
            return ret;
        }
        
        public VHostSpec(ApacheDirective vhostDirective) {
            hosts = vhostDirective.getValues();

            List<ApacheDirective> serverNames = vhostDirective.getChildByName("ServerName");
            serverName = null;
            if (serverNames.size() > 0) {
                serverName = serverNames.get(0).getValuesAsString();
            }
        }
    }
}