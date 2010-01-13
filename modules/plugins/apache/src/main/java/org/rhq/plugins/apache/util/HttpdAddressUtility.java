/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.apache.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;

/**
 * Utility class to extract various HTTP addresses from Augeas loaded Apache configuration.
 * 
 * @author Lukas Krejci
 */
public class HttpdAddressUtility {

    private static final Log log = LogFactory.getLog(HttpdAddressUtility.class);
    
    private HttpdAddressUtility() {
        
    }
    
    public static class Address {
        public String host;
        public int port = -1;
        
        public Address(String host, int port) {
            this.host = host;
            this.port = port;
        }
        
        /**
         * A simple parser of the provided address into host and port
         * sections.
         * 
         * @param address the address to parse
         * @return an instance of Address with host and port set accordingly
         */
        public static Address parse(String address) {
            int lastColonIdx = address.lastIndexOf(':');
            if (lastColonIdx == -1) {
                return new Address(address, -1);
            } else {
                int lastRightBracketPos = address.lastIndexOf(']');
                if (lastColonIdx > lastRightBracketPos) {
                    String host = address.substring(0, lastColonIdx);
                    int port = Integer.parseInt(address.substring(lastColonIdx + 1));
                    return new Address(host, port);
                } else {
                    //this is an IP6 address without a port spec
                    return new Address(address, -1);
                }
            }
        }
        
        @Override
        public int hashCode() {
            int hash = port;
            if (host != null) hash *= host.hashCode();
            return hash;
        }
        
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Address)) return false;
            
            Address o = (Address)other;
            
            if (this.host == null) {
                return o.host == null && this.port == o.port;
            } else {
                return this.host.equals(o.host) && this.port == o.port;
            }
        }
    }    
    
    /**
     * This just constructs a first available address under which the server or one of its virtual hosts can be reached.
     * 
     * @param ag the tree of the httpd configuration
     * @return the address or null on failure
     */
    public static Address getMainServerSampleAddress(AugeasTree ag) {
        try {
            //there has to be at least one Listen directive
            AugeasNode listen = ag.matchRelative(ag.getRootNode(), "Listen").get(0);
            
            List<AugeasNode> params = listen.getChildByLabel("param");
            String address = params.get(0).getValue();
            
            Address addr = Address.parse(address);
            
            if (addr.port == -1) {
                addr.port = Integer.parseInt(addr.host);
                try {
                    addr.host = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    throw new IllegalStateException("Unable to get the localhost address.", e);
                }
            }
            
            return addr;
        } catch (Exception e) {
            log.info("Failed to obtain main server address. Is augeas installed and correct lens in use?");
            
            return null;
        }
    }    
    
    /**
     * This constructs an address on which given virtual host can be accessed.
     * 
     * @param ag the augeas tree of the httpd configuration
     * @param virtualHost the port or address:port of the virtual host
     * @param serverName the server name for the namebased virtual hosts (or null if the virtual host is ip based)
     * 
     * @return the address on which the virtual host can be accessed or null on error
     */
    public static Address getVirtualHostSampleAddress(AugeasTree ag, String virtualHost, String serverName) {
        Address addr = Address.parse(virtualHost);
        if (addr.port == -1) {
            //just port specified
            Address serverAddr = getMainServerSampleAddress(ag);
            
            if (serverAddr == null) return null;
            
            addr.port = Integer.parseInt(addr.host);
            addr.host = serverAddr.host;
        } else {
            String host = addr.host;
            if ("*".equals(host) || "_default_".equals(host)) {
                Address serverAddr = getMainServerSampleAddress(ag);
                if (serverAddr == null) return null;
                host = serverAddr.host;
            }
            addr.host = host;
        }
        
        if (serverName != null) {
            int colonIdx = serverName.indexOf(':');
            if (colonIdx >= 0) {
                addr.host = serverName.substring(0, colonIdx);
                addr.port = Integer.parseInt(serverName.substring(colonIdx + 1));
            } else {
                addr.host = serverName;
            }
            addr.host = addr.host.replaceAll("\\*", "replaced-wildcard");
        }
        
        return addr;
    }    
}
