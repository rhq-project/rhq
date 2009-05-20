/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.jboss.on.plugins.tomcat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.EmsBeanName;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * JON plugin discovery component for Tomcat connectors. The bulk of the discovery is performed by the super class. This
 * class exists to work with the bean attribute values once they were read.
 *
 * @author Jay Shaughnessy
 * @author Jason Dobies
 * @author Ian Springer
 */
public class TomcatConnectorDiscoveryComponent extends MBeanResourceDiscoveryComponent<TomcatServerComponent> {
    private final Log log = LogFactory.getLog(this.getClass());

    // MBeanResourceDiscoveryComponent Overridden Methods  --------------------------------------------

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<TomcatServerComponent> context) {

        // Connector objectNames can have two formats:
        // Catalina:type=Connector,port=%port% (this is the default for the connector type in the plugin desc) 
        // Catalina:type=Connector,port=%port%,address=%address% (this happens if <address> is specified in connector def 
        // Get both by querying without skipping unknown props
        Set<DiscoveredResourceDetails> resourceDetails = super.discoverResources(context, false);

        // we depend on the GlobalRequestProcessor MBeans (1 for each connector) to fully define the resources, so the connector and
        // GlobalRequestProcessor MBeans must be fully deployed. If the mbeans aren't fully deployed then wait for 
        // the next go around of the PC.
        EmsConnection connection = context.getParentResourceComponent().getEmsConnection();
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(
            "Catalina:type=GlobalRequestProcessor,name=%name%");
        List<EmsBean> grpBeans = connection.queryBeans(queryUtility.getTranslatedQuery());

        if (grpBeans.size() != resourceDetails.size()) {
            if (log.isDebugEnabled())
                log.debug("Connector discovery pending jboss.web:type=GlobalRequestProcessor,name=* deployment...");
            return Collections.emptySet();
        }

        // Map <port, ConfigInfo>
        Map<String, ConfigInfo> configMap = new HashMap<String, ConfigInfo>(grpBeans.size());

        for (EmsBean bean : grpBeans) {
            ConfigInfo configInfo = new ConfigInfo(bean);
            if (null != configInfo.getPort()) {
                configMap.put(configInfo.port, configInfo);
            } else {
                log.warn("Failed to parse ObjectName for GlobalRequestProcessor: " + configInfo.getName() + ": "
                    + configInfo.getException());
            }
        }

        for (DiscoveredResourceDetails resource : resourceDetails) {
            Configuration pluginConfiguration = resource.getPluginConfiguration();

            String port = pluginConfiguration.getSimple(TomcatConnectorComponent.PLUGIN_CONFIG_PORT).getStringValue();
            ConfigInfo configInfo = configMap.get(port);

            // Set handler plugin config and update resource name 
            String handler = (null != configInfo) ? configInfo.getHandler() : TomcatConnectorComponent.UNKNOWN;
            resource.setResourceName(resource.getResourceName().replace("{handler}", handler));

            // It is unusual but possible that there is a GlobalRequestProcessor object representing a configured AJP
            // connector but with a different port.  If the configured AJP connector port is in use, Tomcat increments
            // the port number (up to maxPort) looking for a free port.  That actual listening port is used on the
            // GlobalRequestProcessor object.  This behavior seems to be, after some research, considered a bug in
            // Tomcat. So, until proven otherwise, we'll treat it as such. To bring this to the attention of the user
            // we do still discover the connector, but we'll fail the component start and provide a useful message
            // indicating that the Tomcat configuration should change. Handler being set UNKNOWN will signal the problem.
            pluginConfiguration.put(new PropertySimple(TomcatConnectorComponent.PLUGIN_CONFIG_HANDLER, handler));

            // Set the address and protocol
            //            queryUtility = new ObjectNameQueryUtility("Catalina:type=Connector,port=" + port);
            //            grpBeans = connection.queryBeans(queryUtility.getTranslatedQuery());
            //
            //            if (!grpBeans.isEmpty()) {
            //                EmsAttribute scheme = grpBeans.get(0).getAttribute("

            //                if (null != scheme) {
            //                    pluginConfiguration.put(new PropertySimple(TomcatConnectorComponent.CONFIG_SCHEME,
            //                        (String) scheme.getValue()));
            //                }
            //            }

            if (log.isDebugEnabled()) {
                log.debug("Found a connector: " + handler
                    + ((null != configInfo) ? ("-" + configInfo.getAddress()) : "") + "-" + port);
            }
        }

        return resourceDetails;
    }

    private static class ConfigInfo {
        private String name;
        private String address;
        private String handler;
        private String port;
        private Exception exception;

        public ConfigInfo(EmsBean bean) {
            EmsBeanName eName = bean.getBeanName();
            this.name = eName.getKeyProperty("name");

            /* Possible name formats:
             * 1) handler-port
             * 2) handler-ipaddress-port
             * 3) handler-host%2Fipaddress-port
             * 
             * Option 2 or 3 occurs when the <address> is explicitly defined in the <connector> element.
             * Option 3 occurs when the address is an alias.  The alias is resolved and appended to the alias separated
             * by '/' (encoded a slash comes through as %2F).  Note that the host may have itself contain dashes '-'. 
             */

            try {
                int firstDash = name.indexOf('-');
                int lastDash = name.lastIndexOf('-');
                handler = name.substring(0, firstDash);
                port = name.substring(lastDash + 1);
                // validate that the port is a valid int
                Integer.valueOf(port);

                // Check to see if an address portion exists
                if (firstDash != lastDash) {
                    // For option 3 keep the alias and we'll resolve
                    String rawAddress = name.substring(firstDash + 1, lastDash);
                    int delim = rawAddress.indexOf("%2F");
                    address = (-1 == delim) ? rawAddress : rawAddress.substring(0, delim);
                } else {
                    this.address = TomcatConnectorComponent.UNKNOWN;
                }
            } catch (Exception e) {
                port = null;
                address = null;
                handler = null;
                exception = e;
            }
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public String getHandler() {
            return handler;
        }

        public String getPort() {
            return port;
        }

        public Exception getException() {
            return exception;
        }

    }
}