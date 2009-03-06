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
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
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

        // Get the connectors via the default JMX discovery for the connector mbeans
        Set<DiscoveredResourceDetails> resourceDetails = super.discoverResources(context);

        // we depend on the GlobalRequestProcessor MBeans (1 for each connector) to fully define the resources, so the connector and
        // GlobalRequestProcessor MBeans must be fully deployed. If the mbeans aren't fully deployed yet wait for 
        // the next go around of the PC.
        EmsConnection connection = context.getParentResourceComponent().getEmsConnection();
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility("Catalina:type=GlobalRequestProcessor,name=%name%");
        List<EmsBean> beans = connection.queryBeans(queryUtility.getTranslatedQuery());

        if (beans.size() != resourceDetails.size()) {
            if (log.isDebugEnabled())
                log.debug("jboss.web:type=GlobalRequestProcessor,name=* MBeans are not fully deployed yet - aborting...");
            return Collections.emptySet();
        }

        // Map <port, ConfigInfo>
        Map<String, ConfigInfo> configMap = new HashMap<String, ConfigInfo>(beans.size());

        for (EmsBean bean : beans) {
            ConfigInfo configInfo = new ConfigInfo(bean);
            if (null != configInfo.getPort()) {
                configMap.put(configInfo.port, configInfo);
            } else {
                log.warn("Unknown ObjectName for GlobalRequestProcessor: " + configInfo.getName());
            }
        }

        for (DiscoveredResourceDetails resource : resourceDetails) {
            Configuration pluginConfiguration = resource.getPluginConfiguration();

            String port = pluginConfiguration.getSimple(TomcatConnectorComponent.PLUGIN_CONFIG_PORT).getStringValue();
            ConfigInfo configInfo = configMap.get(port);

            // It is unusual but possible that there is a GlobalRequestProcessor object representing a configured AJP
            // connector but with a different port.  If the configured AJP connector port is in use, Tomcat increments
            // the port number (up to maxPort) looking for a free port.  That actual listening port is used on the
            // GlobalRequestProcessor object.  This behavior seems to be, after some research, considered a bug in
            // Tomcat. So, until proven otherwise, we'll treat it as such. To bring this to the attention of the user
            // we do still discover the connector, but we'll fail the component start and provide a useful message
            // indicating that the Tomcat configuration should change. We'll use a special scheme property value
            // to signal the problem.
            String scheme = (null != configInfo) ? configInfo.getScheme() : TomcatConnectorComponent.UNKNOWN;
            String address = (null != configInfo) ? configInfo.getAddress() : TomcatConnectorComponent.UNKNOWN;

            pluginConfiguration.put(new PropertySimple(TomcatConnectorComponent.PLUGIN_CONFIG_SCHEME, scheme));
            pluginConfiguration.put(new PropertySimple(TomcatConnectorComponent.PLUGIN_CONFIG_ADDRESS, address));
            resource.setResourceName(resource.getResourceName().replace("{scheme}", scheme));

            queryUtility = new ObjectNameQueryUtility("Catalina:type=Connector,port=" + port);
            beans = connection.queryBeans(queryUtility.getTranslatedQuery());

            if (!beans.isEmpty()) {
                EmsAttribute protocol = beans.get(0).getAttribute("protocol");
                if (null != protocol) {
                    pluginConfiguration.put(new PropertySimple(TomcatConnectorComponent.PLUGIN_CONFIG_PROTOCOL, (String) protocol.getValue()));
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Found a connector: " + scheme + "-" + address + "-" + port);
            }
        }

        return resourceDetails;
    }

    private static class ConfigInfo {
        private static final String LOCAL_IP = "0.0.0.0";

        private String name;
        private String address;
        private String scheme;
        private String port;

        public ConfigInfo(EmsBean bean) {
            EmsBeanName eName = bean.getBeanName();
            this.name = eName.getKeyProperty("name");
            String[] tokens = name.split("-");
            if (tokens.length == 2) {
                this.scheme = tokens[0];
                this.port = tokens[1];
                this.address = LOCAL_IP;
            } else if (tokens.length == 3) {
                this.scheme = tokens[0];
                this.address = tokens[1];
                this.port = tokens[2];
            }
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public String getScheme() {
            return scheme;
        }

        public String getPort() {
            return port;
        }
    }
}