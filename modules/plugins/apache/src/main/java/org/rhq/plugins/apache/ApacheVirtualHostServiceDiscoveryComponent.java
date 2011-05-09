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
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.plugins.apache.parser.ApacheConfigReader;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParser;
import org.rhq.plugins.apache.parser.ApacheParserImpl;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.HttpdAddressUtility.Address;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.apache.util.OsProcessUtility;
import org.rhq.plugins.apache.util.RuntimeApacheConfiguration;
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
public class ApacheVirtualHostServiceDiscoveryComponent implements ResourceDiscoveryComponent<ApacheServerComponent> {

    private static final String COULD_NOT_DETERMINE_THE_VIRTUAL_HOST_ADDRESS = "*** Could not determine the virtual host address ***";

    public static final String LOGS_DIRECTORY_NAME = "logs";

    private static final String RT_LOG_FILE_NAME_SUFFIX = "_rt.log";

    private static final Log log = LogFactory.getLog(ApacheVirtualHostServiceDiscoveryComponent.class);
    
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ApacheServerComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();

        //BZ 612189 - prepare for the legacy overrides. We need to revert to the old-style resource keys until
        //resource upgrade functionality is ready.
        SnmpWwwServiceIndexes snmpDiscoveries = getSnmpDiscoveries(context.getParentResourceComponent());
        
        ApacheServerComponent serverComponent = context.getParentResourceComponent();
        ApacheDirectiveTree tree = serverComponent.loadParser();
        
        tree = RuntimeApacheConfiguration.extract(tree, serverComponent.getCurrentProcessInfo(), serverComponent.getCurrentBinaryInfo(), serverComponent.getModuleNames());
        
        //first define the root server as one virtual host
        discoverMainServer(context, discoveredResources, snmpDiscoveries, tree);

        ResourceType resourceType = context.getResourceType();

        File configPath = serverComponent.getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);

        List<ApacheDirective> virtualHosts = tree.search("/<VirtualHost");

        int currentVhostIndex = 0;
        
        for (ApacheDirective node : virtualHosts) {
            List<String> hosts = node.getValues();
            String firstAddress = hosts.get(0);

            List<ApacheDirective> serverNames = node.getChildByName("ServerName");
            String serverName = null;
            if (serverNames.size() > 0) {
                serverName = serverNames.get(0).getValuesAsString();
            }

            String resourceKey = createResourceKey(serverName, hosts, tree, serverComponent, snmpDiscoveries);
            String resourceName = resourceKey; //this'll get overridden below if we find a better value using the address variable

            Configuration pluginConfiguration = context.getDefaultPluginConfiguration();

            Address address = serverComponent.getAddressUtility().getVirtualHostSampleAddress(tree, firstAddress, serverName, false);
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
                resourceName = address.toString(false, true);
            }

            //as the last thing, let's determine the SNMP WWW Service Index of this vhost
            int snmpWwwServiceIndex = virtualHosts.size() - currentVhostIndex + 1;            
            pluginConfiguration.put(new PropertySimple(ApacheVirtualHostServiceComponent.SNMP_WWW_SERVICE_INDEX_CONFIG_PROP, snmpWwwServiceIndex));
            
            discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null, null,
                pluginConfiguration, null));
            
            currentVhostIndex++;
        }

        return discoveredResources;
    }


    private void discoverMainServer(ResourceDiscoveryContext<ApacheServerComponent> context,
        Set<DiscoveredResourceDetails> discoveredResources, SnmpWwwServiceIndexes snmpDiscoveries, ApacheDirectiveTree runtimeConfig) throws Exception {

        ResourceType resourceType = context.getResourceType();
        Configuration mainServerPluginConfig = context.getDefaultPluginConfiguration();

        File configPath = context.getParentResourceComponent().getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);

        String mainServerUrl = context.getParentResourceContext().getPluginConfiguration().getSimple(
            ApacheServerComponent.PLUGIN_CONFIG_PROP_URL).getStringValue();
        
        String key = createMainServerResourceKey(context.getParentResourceComponent(), runtimeConfig, snmpDiscoveries);
        
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
        
        //the SNMP WWW service index of the main server is always 1
        mainServerPluginConfig.put(new PropertySimple(
            ApacheVirtualHostServiceComponent.SNMP_WWW_SERVICE_INDEX_CONFIG_PROP, 1));
        
        DiscoveredResourceDetails mainServer = new DiscoveredResourceDetails(resourceType,
            key, "Main", null, null,
            mainServerPluginConfig, null);
        discoveredResources.add(mainServer);
    }
    
    /**
     * @deprecated remove this once we have resource upgrade
     * @param discoveryContext
     * @param newStyleResourceKey
     * @param snmpDiscoveries
     * @return
     */
    @Deprecated
    private static String getLegacyResourceKey(ApacheServerComponent serverComponent, String newStyleResourceKey, SnmpWwwServiceIndexes snmpDiscoveries) {
        int snmpWwwServiceIndex = getMatchingWwwServiceIndex(serverComponent, newStyleResourceKey, snmpDiscoveries.names, snmpDiscoveries.ports);
        
        if (snmpWwwServiceIndex < 1) {
            return null;
        } else {
            String host = snmpDiscoveries.names.get(snmpWwwServiceIndex - 1).toString();
            String fullPort = snmpDiscoveries.ports.get(snmpWwwServiceIndex - 1).toString();

            // The port value will be in the form "1.3.6.1.2.1.6.XXXXX",
            // where "1.3.6.1.2.1.6" represents the TCP protocol ID,
            // and XXXXX is the actual port number
            String port = fullPort.substring(fullPort.lastIndexOf(".") + 1);
            return host + ":" + port;
        }
    }
    
    public static String createMainServerResourceKey(ApacheServerComponent serverComponent, ApacheDirectiveTree runtimeConfig, SnmpWwwServiceIndexes snmpDiscoveries) throws Exception {
        String mainServerUrl = serverComponent.getServerUrl();
    
        String key = null;
        
        if (mainServerUrl != null && !"null".equals(mainServerUrl)) {
            URI mainServerUri = new URI(mainServerUrl);
            String host = mainServerUri.getHost();
            int port = mainServerUri.getPort();
            if (port == -1) {
                port = 80;
            }

            key = host + ":" + port;
        } else {
            key = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;
        }
        
        //BZ 612189 - remove this once we have resource upgrade
        if (snmpDiscoveries != null) {
            String legacyKey = getLegacyResourceKey(serverComponent, key, snmpDiscoveries);
            key = legacyKey != null ? legacyKey : key;
        }
        
        return key;
    }
    
    public static String createResourceKey(String serverName, List<String> hosts, ApacheDirectiveTree runtimeConfig, ApacheServerComponent serverComponent, SnmpWwwServiceIndexes snmpDiscoveries) {
//BZ 612189 - swap the impls once resource upgrade is in place
//        StringBuilder keyBuilder = new StringBuilder();
//        if (serverName != null) {
//            keyBuilder.append(serverName).append("|");
//        }
//        keyBuilder.append(hosts.get(0));
//
//       
//        for (int i = 1; i < hosts.size(); ++i){
//            keyBuilder.append(" ").append(hosts.get(i));
//        }
//        
//        return keyBuilder.toString();
        
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
        
        String key = hostAddr.host + ":" + hostAddr.port;

        //BZ 612189 - remove this once we have resource upgrade
        if (snmpDiscoveries != null) {
            String legacyResourceKey = getLegacyResourceKey(serverComponent, key, snmpDiscoveries);
            key = legacyResourceKey != null ? legacyResourceKey : key;
        }
    
        return key;
    }
    
    /**
     * @deprecated remove this once we have resource upgrade
     * @param discoveryContext
     * @return
     */
    @Deprecated
    public static SnmpWwwServiceIndexes getSnmpDiscoveries(ApacheServerComponent serverComponent) {
        try {
            SNMPSession snmpSession = serverComponent.getSNMPSession();
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
    
            SnmpWwwServiceIndexes ret = new SnmpWwwServiceIndexes();
            ret.names = nameValues;
            ret.ports = portValues;
            ret.desc = descValue;
            
            return ret;
        } catch (Exception e) {
            log.debug("Error while trying to contact SNMP of the apache server " + serverComponent.getResourceKey(), e);
            return null;
        }
    }
    
    /**
     * @deprecated remove this once we have resource upgrade
     *
     * @author Lukas Krejci
     */
    @Deprecated
    public static class SnmpWwwServiceIndexes {
        public List<SNMPValue> names;
        public List<SNMPValue> ports;
        public SNMPValue desc;
    }
    
    /**
     * @deprecated used only in the {@link ApacheVirtualHostServiceDiscoveryComponent#getLegacyResourceKey(ApacheServerComponent, String, SnmpWwwServiceIndexes)}
     * to figure out what the resource key should in a way compatible with the previous incarnations of the plugin in RHQ 3.
     * <p>
     * This method used to reside in the {@link ApacheVirtualHostServiceComponent} and was used to match a vhost with an SNMP index at runtime.
     * Because of the non-deterministic nature of this method, it is no longer used there but we still need it in the discovery so that we
     * maintain the same behavior and obtain the same resource keys as the previous versions of the plugin. This is to not create duplicate
     * resources after a new version of the plugin is deployed.  
     */
    @Deprecated
    private static int getMatchingWwwServiceIndex(ApacheServerComponent parent, String resourceKey, List<SNMPValue> names, List<SNMPValue> ports) {
        int ret = -1;
        Iterator<SNMPValue> namesIterator = names.iterator();
        Iterator<SNMPValue> portsIterator = ports.iterator();

        //figure out the servername and addresses of this virtual host
        //from the resource key.
        String vhostServerName = null;
        String[] vhostAddressStrings = null;
        int pipeIdx = resourceKey.indexOf('|');
        if (pipeIdx >= 0) {
            vhostServerName = resourceKey.substring(0, pipeIdx);
        }
        vhostAddressStrings = resourceKey.substring(pipeIdx + 1).split(" ");

        ApacheDirectiveTree tree = parent.loadParser(); 
        
        //convert the vhost addresses into fully qualified ip/port addresses
        List<HttpdAddressUtility.Address> vhostAddresses = new ArrayList<HttpdAddressUtility.Address>(
            vhostAddressStrings.length);

        if (vhostAddressStrings.length == 1 && ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY.equals(vhostAddressStrings[0])) {
            HttpdAddressUtility.Address serverAddr = parent.getAddressUtility().getMainServerSampleAddress(tree, null, 0);
            if (serverAddr != null) {
                vhostAddresses.add(serverAddr);
            }
        } else {
            for (int i = 0; i < vhostAddressStrings.length; ++i) {
                HttpdAddressUtility.Address vhostAddr = parent.getAddressUtility().getVirtualHostSampleAddress(tree, vhostAddressStrings[i],
                    vhostServerName, true);
                if (vhostAddr != null) {
                    vhostAddresses.add(vhostAddr);
                } else {
                    //this is not to choke on the old style resource keys for the main server. without this, we'd never be able
                    //to match the main server with its snmp index below.
                    HttpdAddressUtility.Address addr = HttpdAddressUtility.Address.parse(vhostAddressStrings[i]);
                    vhostAddr = parent.getAddressUtility().getMainServerSampleAddress(tree, addr.host, addr.port);
                    if (vhostAddr != null) {
                        vhostAddresses.add(vhostAddr);
                    }
                }
            }
        }

        //finding the snmp index that corresponds to the address(es) of the vhost isn't that simple
        //because the snmp module in apache always resolves the IPs to hostnames.
        //on the other hand, the resource key tries to be more accurate about what a 
        //vhost can actually be represented as. A vhost is represented by at most 1 hostname (i.e. ServerName)
        //and possibly multiple IP addresses.
        SNMPValue bestMatch = null;
        int bestMatchRate = 0;
        
        while (namesIterator.hasNext()) {
            SNMPValue nameValue = namesIterator.next();
            SNMPValue portValue = portsIterator.next();

            String snmpHost = nameValue.toString();
            String fullPort = portValue.toString();

            int snmpPort = Integer.parseInt(fullPort.substring(fullPort.lastIndexOf(".") + 1));
            
            HttpdAddressUtility.Address snmpAddress = new HttpdAddressUtility.Address(snmpHost, snmpPort);
        
            int matchRate = matchRate(vhostAddresses, snmpAddress);
            if (matchRate > bestMatchRate) {
                bestMatch = nameValue;
                bestMatchRate = matchRate;
            }
        }
        
        if (bestMatch != null) {
            String nameOID = bestMatch.getOID();
            ret = Integer.parseInt(nameOID.substring(nameOID.lastIndexOf(".") + 1));
        } else {
            log.debug("Unable to match the Virtual Host [" + resourceKey + "] with any of the SNMP advertised vhosts: " + names + ". The discovery will fallback to using the resource key not derived from an SNMP entry.");
        }
        return ret;
    }
    
    /**
     * @deprecated this is only used inside {@link #getMatchingWwwServiceIndex(ApacheServerComponent, String, List, List)}, which is
     * kept only for backwards compatibility reasons. Don't use it anywhere else ever! 
     */
    @Deprecated
    private static int matchRate(List<HttpdAddressUtility.Address> addresses, HttpdAddressUtility.Address addressToCheck) {
        for(HttpdAddressUtility.Address a : addresses) {
            if (HttpdAddressUtility.isAddressConforming(addressToCheck, a.host, a.port, true)) {
                return 3;
            }
        }
        
        //try to get the IP of the address to check
        InetAddress[] ipAddresses;
        try {
            ipAddresses = InetAddress.getAllByName(addressToCheck.host);
            for(InetAddress ip : ipAddresses) {
                HttpdAddressUtility.Address newCheck = new HttpdAddressUtility.Address(ip.getHostAddress(), addressToCheck.port);
                
                for(HttpdAddressUtility.Address a : addresses) {
                    if (HttpdAddressUtility.isAddressConforming(newCheck, a.host, a.port, true)) {
                        return 2;
                    }
                }
            }            
        } catch (UnknownHostException e) {
            log.debug("Unknown host encountered in the httpd configuration: " + addressToCheck.host);
            return 0;
        }
        
        //this stupid 80 = 0 rule is to conform with snmp module
        //the problem is that snmp module represents both 80 and * port defs as 0, 
        //so whatever we do, we might mismatch the vhost. But there's no working around that
        //but to modify the snmp module itself.
        
        int addressPort = addressToCheck.port;
        if (addressPort == 80) {
            addressPort = 0;
        }
        
        //ok, try the hardest...
        for(HttpdAddressUtility.Address listAddress: addresses) {
            int listPort = listAddress.port;
            if (listPort == 80) {
                listPort = 0;
            }
            
            InetAddress[] listAddresses;
            try {
                listAddresses = InetAddress.getAllByName(listAddress.host);
            } catch (UnknownHostException e) {
                log.debug("Unknown host encountered in the httpd configuration: " + listAddress.host);
                return 0;
            }
            
            for (InetAddress listInetAddr : listAddresses) {
                for (InetAddress ip : ipAddresses) {
                    if (ip.equals(listInetAddr) && addressPort == listPort) {
                        return 1;
                    }
                }
            }
        }
        
        return 0;
    }    
}
