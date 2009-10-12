/*
 * Jopr Management Platform
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

package org.jboss.on.plugins.jbossOsgi.JBossOSGi;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * Discovery class for JBossOSGi servers
 * @author Heiko W. Rupp
 *
 * implements ResourceDiscoveryComponent<JBossOsgiServerComponent>
 */
public class JBossOsgiServerDiscovery implements ResourceDiscoveryComponent {


    private final Log log = LogFactory.getLog(this.getClass());
    public static final String MANAGER_OBJECT = "jboss.osgi:service=ManagedFramework";

    /**
     * Run the discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext)  {

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        String remote = "service:jmx:rmi://127.0.0.1/jndi/rmi://127.0.0.1:1090/jmxconnector";
        String connector = "org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor";


        Configuration c = new Configuration();
        c.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,remote));
        c.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,connector));
        c.put(new PropertySimple("objectName", MANAGER_OBJECT));

        boolean found = testConnection(c);
        if (found) {
            String simplifiedRemote = simplifyUrl(remote);
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails( discoveryContext.getResourceType(),
                    remote, "JBossOSGi @ " + simplifiedRemote , null, "JBossOSGi Server", c, null);


            // Add to return values
            discoveredResources.add(detail);
            log.info("Discovered new JBossOSGi server :" + remote);
        }
        return discoveredResources;

    }

    /**
     * Fiddle host:port from the service string for the resource name that will be displayed. If input is null,
     * null will be returned.
     * @param remote Input jmx-remote service url.
     * @return short version of the input if anything goes wrong
     */
    private String simplifyUrl(String remote) {
        if (remote==null)
            return null;

        if (remote.indexOf("rmi://")==-1) // Nothing to do
            return remote;

        int i = remote.lastIndexOf("rmi://");
        i+=6;
        if (i>remote.length())
            return remote;

        String res = remote.substring(i);
        i = res.indexOf("/");
        if (i>0)
            res = res.substring(0,i);

        return res;
    }

    /**
     * Try to establish a connection to the given remote and search for the managerObject
     * @param config Configuration data to set up the remote connection
     * @return true if the connetion is successful and the MBean could be found
     */
    private boolean testConnection(Configuration config) {

        boolean res=false;

        try {
            JBossOsgiServerComponent c = new JBossOsgiServerComponent();
            EmsConnection conn = c.getEmsConnection(config);
            EmsBean managerBean = conn.getBean(MANAGER_OBJECT);
            managerBean.getAttribute("bundles").refresh();
            res = true;
        }
        catch (Exception e) {
            // Nothing to do
        }

        return res;
    }
}