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
import org.rhq.plugins.jmx.JMXComponent;
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
public class TomcatConnectorDiscoveryComponent extends MBeanResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(this.getClass());

    private static final String LOCAL_IP = "0.0.0.0";

    // MBeanResourceDiscoveryComponent Overridden Methods  --------------------------------------------

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {

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

        // Map <port, scheme>
        Map<String, String> schemeMap = new HashMap<String, String>(beans.size());

        // TODO: Does Tomcat really provide address optionally in the object name? If not this logic is not needed.
        // Map <port, address>
        Map<String, String> addressMap = new HashMap<String, String>(beans.size());

        for (EmsBean bean : beans) {
            EmsBeanName eName = bean.getBeanName();
            String oName = eName.getKeyProperty("name");
            String[] tokens = oName.split("-");
            if (tokens.length == 2) {
                schemeMap.put(tokens[1], tokens[0]);
            } else if (tokens.length == 3) {
                schemeMap.put(tokens[2], tokens[0]);
                addressMap.put(tokens[2], tokens[1]);
            } else {
                log.warn("Unknown ObjectName for GlobalRequestProcessor: " + oName);
            }
        }

        for (DiscoveredResourceDetails resource : resourceDetails) {
            Configuration pluginConfiguration = resource.getPluginConfiguration();

            String port = pluginConfiguration.getSimple(TomcatConnectorComponent.PROPERTY_PORT).getStringValue();
            String scheme = schemeMap.get(port);
            String address = addressMap.get(port);

            pluginConfiguration.put(new PropertySimple(TomcatConnectorComponent.PROPERTY_SCHEME, scheme));
            if (null != address) {
                pluginConfiguration.put(new PropertySimple(TomcatConnectorComponent.PROPERTY_ADDRESS, address));
            }

            if (log.isDebugEnabled()) {
                log.debug("Found a connector: " + scheme + "-" + ((null != address) ? address : LOCAL_IP) + "-" + port);
            }
        }

        return resourceDetails;
    }
}