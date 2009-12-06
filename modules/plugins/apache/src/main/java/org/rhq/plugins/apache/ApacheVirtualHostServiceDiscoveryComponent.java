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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Lukas Krejci
 */
public class ApacheVirtualHostServiceDiscoveryComponent implements ResourceDiscoveryComponent<ApacheServerComponent> {

    public static final String MAIN_SERVER_RESOURCE_KEY = "MainServer";
    
    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ApacheServerComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();

        //first define the root server as one virtual host
        ResourceType resourceType = context.getResourceType();

        DiscoveredResourceDetails mainServer = new DiscoveredResourceDetails(resourceType, MAIN_SERVER_RESOURCE_KEY, "Main Server",
            null, null, new Configuration(), null);
        discoveredResources.add(mainServer);

        //read the virtual hosts from augeas
        AugeasTree ag = context.getParentResourceComponent().getAugeasTree();

        List<AugeasNode> virtualHosts = ag.matchRelative(ag.getRootNode(), "VirtualHost");

        for (AugeasNode node : virtualHosts) {
            List<AugeasNode> hosts = ag.matchRelative(node, "address");
            String firstAddress = hosts.get(0).getValue();
            
            StringBuilder keyBuilder = new StringBuilder(firstAddress);
            Iterator<AugeasNode> it = hosts.iterator();
            it.next();
            
            while (it.hasNext()) {
                keyBuilder.append(" ").append(it.next().getValue());
            }
            
            String resourceKey = keyBuilder.toString();
            
            Address address = getVirtualHostSampleAddress(ag, firstAddress);
            String url = "http://" + address.host + ":" + address.port + "/";
            
            Configuration pluginConfiguration = new Configuration();
            PropertySimple urlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, url);
            pluginConfiguration.put(urlProp);
            
            //TODO there is no simple way how to determine the RT log file. The server can listen on multiple
            //ports and vhost can be configured to listen on all of them. Also the server can listen on multiple
            //IPs and virtual host can also listen on all of them. Thus the host_port_rt.log is rather non-deterministic.
            
            discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, "Virtual Host " + resourceKey, null,
                null, new Configuration(), null));
        }

        return discoveredResources;
    }
    
    private Address getVirtualHostSampleAddress(AugeasTree ag, String virtualHost) {
        if (virtualHost.startsWith("[")) {
            //TODO IPv6 address
            return null;
        } else {
            String[] hostPort = virtualHost.split(":");
            if (hostPort.length == 1) {
                //just address specified
                Address addr = getMainServerSampleAddress(ag);
                
                addr.host = hostPort[0];
                return addr;
            } else {
                String host = hostPort[0];
                if ("*".equals(host)) {
                    host = getMainServerSampleAddress(ag).host;
                }
                String port = hostPort[1];
                return  new Address(host, Integer.parseInt(port));
            }
        }
    }
    
    /**
     * This just constructs a first available address under which the server or one of its virtual hosts can be reached.
     * 
     * @param ag the tree of the httpd configuration
     * @return the address
     */
    private Address getMainServerSampleAddress(AugeasTree ag) {
        //there has to be at least one Listen directive
        AugeasNode listen = ag.matchRelative(ag.getRootNode(), "Listen").get(0);
        
        String host = null;
        String port = null;
        for(AugeasNode child : listen.getChildNodes()) {
            if (child.getLabel().equals("ip")) {
                host = child.getValue();
            } else if (child.getLabel().equals("port")) {
                port = child.getValue();
            }
        }
        
        if (host != null) {
            if (host.startsWith("[")) {
                host = host.substring(1, host.length() - 1);
            }
        }
        return new Address(host, Integer.parseInt(port));
    }    
    
    private static class Address {
        public String host;
        public int port;
        
        public Address(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}