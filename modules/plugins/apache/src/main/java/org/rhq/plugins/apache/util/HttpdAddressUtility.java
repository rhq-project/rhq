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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;

/**
 * Utility class to extract various HTTP addresses from Augeas loaded Apache configuration.
 * 
 * @author Lukas Krejci
 */
public enum HttpdAddressUtility {

    APACHE_1_3 {
        
        public List<Address> getAllMainServerAddresses(ApacheDirectiveTree ag, boolean substituteWildcards) {
            try {
                List<ApacheDirective> ports = ag.search("/Port");
                List<ApacheDirective> bindAddresses = ag.search("/BindAddress");
                List<ApacheDirective> listens = ag.search("/Listen");

                String port = "80"; //this is the default in apache 1.3
                String bindAddress = null;
                
                List<Address> addresses = new ArrayList<Address>();
                
                if (ports.size() > 0) {
                    List<String>values = ports.get(0).getValues();
                    if (values.size()>0)                           
                       port =  values.get(0);
                }
                
                if (bindAddresses.size() > 0) {
                    List<String>values = bindAddresses.get(0).getValues();
                    if (values.size()>0)                        
                    bindAddress =  values.get(0);
                }
                
                //listen directives take precedence over port/bindaddress combo
                if (listens.size() > 0) {
                    for(ApacheDirective l : listens) {
                        addresses.add(parseListen(l.getValues().get(0)));
                    }
                } else {
                    addresses.add(new Address(bindAddress, Integer.parseInt(port)));
                }
                
                for (Address address : addresses) {
                    
                    if (!address.isPortDefined()) {
                        address.port = 80;
                    }
                    
                    if (substituteWildcards) {
                        substituteWildcards(ag, address);
                    }
                }
                
                return addresses;
            } catch (Exception e) {
                log.warn("Failed to obtain main server address.", e);

                return null;
            }
        }
    },
    APACHE_2_x {
        
        public List<Address> getAllMainServerAddresses(ApacheDirectiveTree ag, boolean substituteWildcards) {
            try {
                List<Address> ret = new ArrayList<Address>();
                
                for(ApacheDirective n : ag.search("/Listen")) {
                    Address addr = parseListen(n.getValues().get(0));
                    
                    if (substituteWildcards) {
                        substituteWildcards(ag, addr);
                    }
                    
                    ret.add(addr);
                }
                
                return ret;
            } catch (Exception e) {
                log.warn("Failed to obtain main server address.", e);

                return null;
            }
        }
    };

    private static final Log log = LogFactory.getLog(HttpdAddressUtility.class);

    public static final String BOGUS_HOST_WITHOUT_FORWARD_DNS = "bogus_host_without_forward_dns";
    public static final String BOGUS_HOST_WITHOUT_REVERSE_DNS = "bogus_host_without_reverse_dns";
    
    public static HttpdAddressUtility get(String version) {
        return version.startsWith("1.") ? APACHE_1_3 : APACHE_2_x;
    }
    
    public static class Address {
        public String host;
        public int port = -1;
        public String scheme = "http";
        
        public static final String WILDCARD = "*";
        public static final String DEFAULT_HOST = "_default_";
        public static final int PORT_WILDCARD_VALUE = 0;
        public static final int NO_PORT_SPECIFIED_VALUE = -1;
        
        public Address(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public Address(String scheme, String host, int port) {
            this(host, port);
            this.scheme = scheme;
        }
        /**
         * A simple parser of the provided address into host and port
         * sections.
         * 
         * @param address the address to parse
         * @return an instance of Address with host and port set accordingly
         */
        public static Address parse(String address) {
            String scheme = "http";
            int schemeSpecIdx = address.indexOf("://");
            if (schemeSpecIdx >= 0) {
                scheme = address.substring(0, schemeSpecIdx);
                address = address.substring(schemeSpecIdx + "://".length());
            }
            
            int lastColonIdx = address.lastIndexOf(':');
            if (lastColonIdx == -1) {
                return new Address(address, NO_PORT_SPECIFIED_VALUE);
            } else {
                int lastRightBracketPos = address.lastIndexOf(']');
                if (lastColonIdx > lastRightBracketPos) {
                    String host = address.substring(0, lastColonIdx);
                    String portSpec = address.substring(lastColonIdx + 1);
                    
                    int port = NO_PORT_SPECIFIED_VALUE;
                    if (WILDCARD.equals(portSpec)) {
                        port = PORT_WILDCARD_VALUE;
                    } else {
                        port = Integer.parseInt(portSpec);
                    }
                    
                    return new Address(scheme, host, port);
                } else {
                    //this is an IP6 address without a port spec
                    return new Address(scheme, address, NO_PORT_SPECIFIED_VALUE);
                }
            }
        }

        public boolean isPortWildcard() {
            return port == PORT_WILDCARD_VALUE;
        }
        
        public boolean isPortDefined() {
            return port != NO_PORT_SPECIFIED_VALUE;
        }
        
        public boolean isHostWildcard() {
            return WILDCARD.equals(host);
        }
        
        public boolean isHostDefault() {
            return DEFAULT_HOST.equals(host);
        }
        
        @Override
        public int hashCode() {
            int hash = port;
            if (host != null)
                hash *= host.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Address))
                return false;

            Address o = (Address) other;

            return safeEquals(host, o.host) && this.port == o.port;
        }
        
        /**
         * This differs from equals in the way that it considers wildcard values:
         * <ul>
         * <li>wildcard host matches any host
         * <li>default host matches default host
         * <li>wildcard port matches any port
         * <li>undefined port matches undefined port
         * </ul>
         * The addresses match if both address and port match.
         * 
         * @param other the address to match
         * @param whether to match the scheme as well
         * @return true if the addresses match according to the rules described above, false otherwise
         */
        public boolean matches(Address other, boolean matchSchemes) {
            if (matchSchemes && !safeEquals(scheme, other.scheme)) {
                return false;
            }
            
            if (!WILDCARD.equals(host) && !WILDCARD.equals(other.host) && !safeEquals(host, other.host)) {
                return false;
            }
            
            if (PORT_WILDCARD_VALUE != port && PORT_WILDCARD_VALUE != other.port && port != other.port) {
                return false;
            }
            
            return true;
        }
        
        @Override
        public String toString() {
            return toString(true, true);
        }
        
        public String toString(boolean includeScheme, boolean interpretWildcardPort) {
            StringBuilder bld = new StringBuilder();
            
            if (includeScheme) {
                bld.append(scheme).append("://");
            }

            bld.append(host);

            if (port != NO_PORT_SPECIFIED_VALUE) {
                bld.append(":");
                
                if (port == PORT_WILDCARD_VALUE && interpretWildcardPort) {
                    bld.append(WILDCARD);
                } else {
                    bld.append(port);
                }
            }
            
            return bld.toString();
        }
        
        private static boolean safeEquals(Object a, Object b) {
            return a == null ? b == null : a.equals(b);
        }
    }

    /**
     * This returns all the addresses the server listens on.
     * 
     * @param ag the tree of the httpd configuration
     * @param substituteWildcards true if wildcard substitution should be made on host and port specs
     * @return the addresses or null on failure
     */
    public abstract List<Address> getAllMainServerAddresses(ApacheDirectiveTree ag, boolean substituteWildcards);
    
    /**
     * This just constructs a first available address under which the server or one of its virtual hosts can be reached.
     * 
     * @param ag the tree of the httpd configuration
     * @param limitToHost if non-null and different from {@link Address#DEFAULT_HOST} and {@link Address#WILDCARD}, 
     * the sample address is looked for only for the given host
     * @param limitToPort if > 0, the sample address is looked for only for the given port
     * @return the address or null on failure
     */
    public Address getMainServerSampleAddress(ApacheDirectiveTree ag, String limitToHost, int limitToPort) {
        List<Address> addressesToMatch = getAllMainServerAddresses(ag, false);
        
        if (addressesToMatch == null) {
            return null;
        }
        
        for (Address address : addressesToMatch) {
            if (isAddressConforming(address, limitToHost, limitToPort, false)) {
                substituteWildcards(ag, address);
                return address;
            }
        }
        
        return null;
    }

    /**
     * This constructs an address on which given virtual host can be accessed.
     * 
     * @param ag the augeas tree of the httpd configuration
     * @param virtualHost the port or address:port of the virtual host
     * @param serverName the server name for the namebased virtual hosts (or null if the virtual host is ip based)
     * @param legacyWildcardHostHandling use the legacy handling of wildcard hosts. This should always be false unless you are calling this method
     * from the code generating the legacy resource keys during vhost upgrade
     * @return the address on which the virtual host can be accessed or null on error
     */
    public Address getVirtualHostSampleAddress(ApacheDirectiveTree ag, String virtualHost, String serverName, boolean legacyWildcardHostHandling) {
        try {
            Address addr = Address.parse(virtualHost);
            if (addr.isHostDefault() || addr.isHostWildcard()) {
                Address serverAddr = null;
                if (legacyWildcardHostHandling) {
                    serverAddr = getLocalhost(addr.port);
                } else {
                    serverAddr = getMainServerSampleAddress(ag, null, addr.port);
                }
                if (serverAddr == null)
                    return null;
                addr.host = serverAddr.host;
            }
            
            if (serverName != null) {
                updateWithServerName(addr, serverName);
            }

            return addr;
        } catch (Exception e) {
            log.warn("Failed to obtain virtual host address.", e);
            return null;
        }
    }
    
    public Address getHttpdInternalMainServerAddressRepresentation(ApacheDirectiveTree runtimeConfig) {
        Address ret = null;
        
        List<ApacheDirective> serverNames = runtimeConfig.search("/ServerName");
        if (serverNames.size() == 0) {
            //no servername directive in the apache config
            ret = new Address(Address.WILDCARD, Address.NO_PORT_SPECIFIED_VALUE);
            try {
                ret.host = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                ret.host = "127.0.0.1";
            }
            
            ret.port = 0;
        } else {
            String serverName = serverNames.get(serverNames.size() - 1).getValuesAsString();
            ret = HttpdAddressUtility.Address.parse(serverName);
            if (!ret.isPortDefined()) {
                ret.port = 0;
            }
        }
        
        return ret;
    }
    
    public Address getHttpdInternalVirtualHostAddressRepresentation(ApacheDirectiveTree runtimeConfig, String virtualHost, String serverName) {
        Address ret = null;
        
        if (serverName != null) {
            ret = Address.parse(serverName);
            if (!ret.isPortDefined()) {
                ret.port = 0;
            }
            
            //servername is taken literally and no reverse dns lookup is made
            //if the servername host is an IP address. We're done here...
        } else {
            ret = Address.parse(virtualHost);
            if (!ret.isPortDefined() || ret.isPortWildcard() || ret.isHostDefault() || ret.isHostWildcard()) {
                Address mainAddress = getHttpdInternalMainServerAddressRepresentation(runtimeConfig);
                
                if (!ret.isPortDefined() || ret.isPortWildcard()) {
                    ret.port = mainAddress.port;
                }
                
                if (ret.isHostDefault() || ret.isHostWildcard()) {
                    ret.host = mainAddress.host;
                }
            }
            
            //if the vhost hostname is an IP address, a reverse dns lookup is attempted
            //to get the actual hostname.
            //the BOGUS* constants are what the apache actually uses to identify such
            //"error" conditions.
            try {
                InetAddress iAddr = InetAddress.getByName(ret.host);
                String reverseLookup = iAddr.getHostName();
                if (iAddr.getHostAddress().equals(reverseLookup)) {
                    ret.host = BOGUS_HOST_WITHOUT_REVERSE_DNS;
                } else {
                    ret.host = reverseLookup;
                }
            } catch (UnknownHostException e) {
                ret.host = BOGUS_HOST_WITHOUT_FORWARD_DNS;

                //weird, as it seems, apache uses the port of the main server
                //with the unknown host even if the port was specified in the vhost
                //definition
                Address mainAddress = getHttpdInternalMainServerAddressRepresentation(runtimeConfig);
                ret.port = mainAddress.port;
            }
        }
        
        return ret;
    }
    
    private static Address parseListen(String listenValue) {
        Address ret = Address.parse(listenValue);
        if (!ret.isPortDefined()) {
            try {
                ret.port = Integer.parseInt(ret.host);
            } catch (NumberFormatException e) {
                return null;
            }
            ret.host = null;
        }
        
        return ret;
    }
    
    private static void substituteWildcards(ApacheDirectiveTree ag, Address address) {
        if (address.isPortWildcard()) {
            address.port = 80;
        }
        
        if (address.host == null || address.isHostDefault() || address.isHostWildcard()) {
            Address localhost = getLocalhost(address.port);
            address.host = localhost.host;
        }
        
        updateWithServerName(address, ag);
        
    }
    
    /**
     * Checks that given address represents a possibly wildcarded limitingHost and limitingPort values.
     * 
     * @param listen the address to check
     * @param limitingHost the host to limit to. The null value or the {@link Address#DEFAULT_HOST} 
     * or the {@link Address#WILDCARD} are not considered limiting
     * @param limitingPort the port to limit the address to. Values &lt;= 0 are not considered limiting
     * @param snmpModuleCompatibleMode the snmp module represents both port 80 and port wildcard (*) as '0'. 
     * If this flag is set to true, this method takes that into account.
     * @return
     */
    public static boolean isAddressConforming(Address listen, String limitingHost, int limitingPort, boolean snmpModuleCompatibleMode) {
        if (Address.DEFAULT_HOST.equals(limitingHost) || Address.WILDCARD.equals(limitingHost)) {
            limitingHost = null;
        }
        
        boolean hostOk = limitingHost == null;
        boolean portOk = limitingPort <= 0;
        
        //listen.host == null means that server listens on all addresses 
        if (!hostOk && (listen.host == null || limitingHost.equals(listen.host))) {
            hostOk = true;
        }
        
        int listenPort = listen.port;
        
        //this stupid 80 = 0 rule is to conform with snmp module
        //the problem is that snmp module represents both 80 and * port defs as 0, 
        //so whatever we do, we might mismatch the vhost. But there's no working around that
        //but to modify the snmp module itself.
        if (snmpModuleCompatibleMode) {
            if (limitingPort == 80) {
                limitingPort = 0;
            }
            if (listenPort == 80) {
                listenPort = 0;
            }
        }
        
        if (!portOk && limitingPort == listenPort) {
            portOk = true;
        }
        
        return hostOk && portOk;
    }
    
    private static Address getLocalhost(int port) {
        try {
            return new Address(InetAddress.getLocalHost().getHostAddress(), port);
        } catch (UnknownHostException e) {
            //well, this is bad, we can't get address of the localhost. let's use the force...
            return new Address("127.0.0.1", port);
        }
    }
    
    private static void updateWithServerName(Address address, ApacheDirectiveTree config) {
        //check if there is a ServerName directive
        List<ApacheDirective> serverNameNodes = config.search("/ServerName");

        //if there is a ServerName directive, check that the address
        //we're returning indeed corresponds to it. This might not
        //be the case if the server listens on more than one interfaces.
        if (serverNameNodes.size() > 0) {
            String serverName = serverNameNodes.get(0).getValuesAsString();
            updateWithServerName(address, serverName);
        }
    }
    
    private static void updateWithServerName(Address address, String serverName) {
        //the configuration may be invalid and/or the hostname can be unresolvable.
        //we try to match the address with the servername first by IP address
        //but if that fails (i.e. the hostname couldn't be resolved to an IP)
        //we try to simply match the hostnames themselves.
        
        Address serverAddr = Address.parse(serverName);
        String ipFromServerName = null;
        String ipFromAddress = null;
        String hostFromServerName = null;
        String hostFromAddress = null;
        boolean lookupFailed = false;

        try {
            InetAddress addrFromServerName = InetAddress.getByName(serverAddr.host);
            ipFromServerName = addrFromServerName.getHostAddress();
            hostFromServerName = addrFromServerName.getHostName();
        } catch (UnknownHostException e) {
            ipFromServerName = serverAddr.host;
            hostFromServerName = serverAddr.host;
            lookupFailed = true;
        }
        
        try {
            InetAddress addrFromAddress = InetAddress.getByName(address.host);
            ipFromAddress = addrFromAddress.getHostAddress();
            hostFromAddress = addrFromAddress.getHostName();
        } catch (UnknownHostException e) {
            ipFromAddress = address.host;
            hostFromAddress = address.host;
            lookupFailed = true;
        }
        
        if (ipFromAddress.equals(ipFromServerName) || (lookupFailed && (hostFromAddress.equals(hostFromServerName)))) {
            address.scheme = serverAddr.scheme;
            address.host = serverAddr.host;
        }
    }
}
