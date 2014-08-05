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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.RuntimeApacheConfiguration;
import org.rhq.plugins.apache.util.VHostSpec;
import org.rhq.plugins.apache.util.VirtualHostLegacyResourceKeyUtil;
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
public class ApacheVirtualHostServiceDiscoveryComponent implements ResourceDiscoveryComponent<ApacheServerComponent>,
    ResourceUpgradeFacet<ApacheServerComponent> {

    private static final String COULD_NOT_DETERMINE_THE_VIRTUAL_HOST_ADDRESS =
        "*** Could not determine the virtual host address ***";

    public static final String LOGS_DIRECTORY_NAME = "logs";

    private static final String RT_LOG_FILE_NAME_SUFFIX = "_rt.log";

    private static final String LEGACY_SNMP_SERVICE_INDEX_CONFIG_PROP = "snmpWwwServiceIndex";

    private static final Log log = LogFactory.getLog(ApacheVirtualHostServiceDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ApacheServerComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();

        ApacheServerComponent serverComponent = context.getParentResourceComponent();
        ApacheDirectiveTree tree = serverComponent.parseRuntimeConfiguration(false);

        //first define the root server as one virtual host
        discoverMainServer(context, tree, discoveredResources);

        ResourceType resourceType = context.getResourceType();

        File configPath = serverComponent.getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);

        for (VHostSpec vhost : VHostSpec.detect(tree)) {

            String firstAddress = vhost.hosts.get(0);

            String resourceKey = createResourceKey(vhost.serverName, vhost.hosts);
            String resourceName = resourceKey;

            Configuration pluginConfiguration = context.getDefaultPluginConfiguration();

            Address address =
                serverComponent.getAddressUtility().getVirtualHostSampleAddress(tree, firstAddress, vhost.serverName,
                    false);
            if (address != null) {
                String scheme = address.scheme;
                String hostToPing = address.host;
                int portToPing = address.port;
                if (address.isPortWildcard() || !address.isPortDefined()) {
                    Address serverAddress =
                        serverComponent.getAddressUtility().getMainServerSampleAddress(tree, hostToPing, 0);
                    if (serverAddress != null) {
                        portToPing = serverAddress.port;
                    } else {
                        portToPing = Address.PORT_WILDCARD_VALUE;
                    }
                }
                if (address.isHostDefault() || address.isHostWildcard()) {
                    Address serverAddress =
                        serverComponent.getAddressUtility().getMainServerSampleAddress(tree, null, portToPing);

                    if (serverAddress != null) {
                        hostToPing = serverAddress.host;
                    } else {
                        hostToPing = null;
                    }
                }

                String url;
                if (hostToPing != null && portToPing != Address.PORT_WILDCARD_VALUE
                    && portToPing != Address.NO_PORT_SPECIFIED_VALUE) {
                    url = scheme + "://" + hostToPing + ":" + portToPing + "/";
                } else {
                    url = COULD_NOT_DETERMINE_THE_VIRTUAL_HOST_ADDRESS;
                }

                PropertySimple urlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, url);
                pluginConfiguration.put(urlProp);

                File rtLogFile = new File(logsDir, address.host + address.port + RT_LOG_FILE_NAME_SUFFIX);

                PropertySimple rtLogProp =
                    new PropertySimple(ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP,
                        rtLogFile.toString());
                pluginConfiguration.put(rtLogProp);

                if(vhost.serverName == null) {
                    resourceName = createResourceKey(address.toString(false, true), vhost.hosts);
                }
            }

            discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null, null,
                pluginConfiguration, null));
        }

        return discoveredResources;
    }

    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<ApacheServerComponent> inventoriedResource) {
        String resourceKey = inventoriedResource.getResourceKey();

        if (ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY.equals(resourceKey) || resourceKey.contains("|")) {
            //a new style resource key. we're done.
            return null;
        }

        String newResourceKey = null;

        ApacheServerComponent serverComponent = inventoriedResource.getParentResourceComponent();

        ApacheDirectiveTree tree = serverComponent.parseRuntimeConfiguration(false);

        List<VHostSpec> vhosts = VHostSpec.detect(tree);
        VirtualHostLegacyResourceKeyUtil legacyResourceKeyUtil =
            new VirtualHostLegacyResourceKeyUtil(serverComponent, tree);

        //first, let's see if the inventoried resource has the snmpWwwServiceIndex property set
        //if it does, use that to determine what vhost this corresponds to.
        String snmpServiceIndexString =
            inventoriedResource.getPluginConfiguration().getSimpleValue(LEGACY_SNMP_SERVICE_INDEX_CONFIG_PROP, null);
        if (snmpServiceIndexString != null) {
            Integer snmpServiceIndex = null;
            try {
                snmpServiceIndex = Integer.parseInt(snmpServiceIndexString);
            } catch (NumberFormatException e) {
                log.warn("Invalid format of the " + LEGACY_SNMP_SERVICE_INDEX_CONFIG_PROP
                    + " property value. It should be an integer but is '" + snmpServiceIndexString
                    + "'. The upgrade will continue using the resource key matching.", e);
            }

            if (snmpServiceIndex != null) {
                if (snmpServiceIndex > 0) {
                    //yay, we can use the snmpService index to determine which vhost we're dealing with
                    if (snmpServiceIndex == 1) {
                        //k, looks the vhost was representing the main server. Let's do a cross-check.
                        Set<String> legacyResourceKeys = legacyResourceKeyUtil.getLegacyMainServerResourceKeys();
                        if (legacyResourceKeys.contains(resourceKey)) {
                            newResourceKey = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;
                        } else {
                            log.debug("The cross-check of the SNMP WWW Service Index value and resource key failed for virtual host with old resource key: "
                                + resourceKey + ". The upgrade will continue using resource key matching.");
                        }
                    } else {
                        if (vhosts.size() + 1 < snmpServiceIndex) {
                            log.debug("The "
                                + LEGACY_SNMP_SERVICE_INDEX_CONFIG_PROP
                                + " property contains incorrect value ("
                                + snmpServiceIndex
                                + "), which is larger than the total number of active virtual hosts in the configuration files ("
                                + (vhosts.size() + 1) + ". The upgrade will continue using the resource key matching.");
                        } else {
                            //k, this seems to be a correct value
                            //the SNMP indices are in the reverse order of the definitions in the config files
                            //+1, where the main server is always on index 1.
                            VHostSpec vhost = vhosts.get(vhosts.size() - snmpServiceIndex + 1);

                            //right, let's do a cross-check before we actually create the resource key so
                            //that we catch user-generated errors.
                            Set<String> legacyResourceKeys =
                                legacyResourceKeyUtil.getLegacyVirtualHostResourceKeys(vhost);
                            if (legacyResourceKeys.contains(resourceKey)) {
                                newResourceKey = createResourceKey(vhost.serverName, vhost.hosts);
                            } else {
                                log.debug("The cross-check of the SNMP WWW Service Index value and resource key failed for virtual host with old resource key: "
                                    + resourceKey + ". The upgrade will continue using resource key matching.");
                            }
                        }
                    }
                } else {
                    log.warn("The " + LEGACY_SNMP_SERVICE_INDEX_CONFIG_PROP
                        + " property should be a positive integer greater than zero but is " + snmpServiceIndex
                        + " instead. The upgrade will continue using the resource key matching.");
                }
            }
        }

        if (newResourceKey != null) {
            ResourceUpgradeReport report = new ResourceUpgradeReport();
            report.setNewResourceKey(newResourceKey);

            return report;
        }

        Map<String, Set<VHostSpec>> possibleMatchesPerRK = new HashMap<String, Set<VHostSpec>>();
        for (VHostSpec vhost : vhosts) {
            Set<String> legacyResourceKeys = legacyResourceKeyUtil.getLegacyVirtualHostResourceKeys(vhost);

            for (String legacyRK : legacyResourceKeys) {
                addPossibleRKMatch(legacyRK, vhost, possibleMatchesPerRK);
            }
        }

        for (String legacyRK : legacyResourceKeyUtil.getLegacyMainServerResourceKeys()) {
            addPossibleRKMatch(legacyRK, null, possibleMatchesPerRK);
        }

        Set<VHostSpec> matchingVhosts = possibleMatchesPerRK.get(resourceKey);
        if (matchingVhosts == null || matchingVhosts.isEmpty()) {
            throw new IllegalArgumentException(
                "Failed to identify the vhost resource with the old-style resource key '"
                    + resourceKey
                    + "' with any of the vhosts in the apache configuration files. This means that the vhost resource is stale and you can safely uninventory it.");
        } else if (matchingVhosts.size() > 1) {
            String message =
                "Failed to uniquely identify the vhost from the old-style resource key. The old resource key is '"
                    + resourceKey
                    + "' which could be matched with any of the following possible new-style resource keys: "
                    + matchingVhosts
                    + ". The plugin does not have enough information to successfully upgrade this resource."
                    + " Please take note of any alert definitions or operation schedules that you have defined for this resource and manually uninventory it.";

            throw new IllegalArgumentException(message);
        } else {
            VHostSpec vhost = matchingVhosts.iterator().next();
            if (vhost == null) {
                newResourceKey = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;
            } else {
                newResourceKey = createResourceKey(vhost.serverName, vhost.hosts);
            }
        }

        ResourceUpgradeReport report = new ResourceUpgradeReport();
        report.setNewResourceKey(newResourceKey);

        return report;
    }

    private void discoverMainServer(ResourceDiscoveryContext<ApacheServerComponent> context,
        ApacheDirectiveTree runtimeConfig, Set<DiscoveredResourceDetails> discoveredResources) throws Exception {

        ResourceType resourceType = context.getResourceType();
        Configuration mainServerPluginConfig = context.getDefaultPluginConfiguration();

        File configPath = context.getParentResourceComponent().getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);

        String mainServerUrl =
            context.getParentResourceContext().getPluginConfiguration()
                .getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_URL).getStringValue();

        if (mainServerUrl == null || mainServerUrl.trim().isEmpty()) {
            HttpdAddressUtility.Address addr =
                context.getParentResourceComponent().getAddressUtility()
                    .getMainServerSampleAddress(runtimeConfig, null, 0);
            mainServerUrl = addr.toString();
        }

        PropertySimple mainServerUrlProp =
            new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, mainServerUrl);

        mainServerPluginConfig.put(mainServerUrlProp);

        URI mainServerUri = new URI(mainServerUrl);
        String host = mainServerUri.getHost();
        int port = mainServerUri.getPort();
        if (port == -1) {
            port = 80;
        }

        File rtLogFile = new File(logsDir, host + port + RT_LOG_FILE_NAME_SUFFIX);

        PropertySimple rtLogProp =
            new PropertySimple(ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP,
                rtLogFile.toString());
        mainServerPluginConfig.put(rtLogProp);

        String key = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;

        DiscoveredResourceDetails mainServer =
            new DiscoveredResourceDetails(resourceType, key, "Main", null, null, mainServerPluginConfig, null);
        discoveredResources.add(mainServer);
    }

    public static String createResourceKey(String serverName, List<String> hosts) {
        StringBuilder keyBuilder = new StringBuilder();
        if (serverName != null) {
            keyBuilder.append(serverName);
        }
        keyBuilder.append("|"); //always do this so that we have a clear distinction between old and new style resource keys
        keyBuilder.append(hosts.get(0));

        for (int i = 1; i < hosts.size(); ++i) {
            keyBuilder.append(" ").append(hosts.get(i));
        }

        return keyBuilder.toString();
    }

    private static void addPossibleRKMatch(String resourceKey, VHostSpec vhost,
        Map<String, Set<VHostSpec>> possibleMatches) {
        Set<VHostSpec> matches = possibleMatches.get(resourceKey);
        if (matches == null) {
            matches = new HashSet<VHostSpec>();
            possibleMatches.put(resourceKey, matches);
        }

        matches.add(vhost);
    }
}
