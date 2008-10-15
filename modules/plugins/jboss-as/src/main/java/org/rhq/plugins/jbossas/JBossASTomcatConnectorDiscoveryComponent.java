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
package org.rhq.plugins.jbossas;

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
 * @author Jason Dobies
 * @author Ian Springer
 */
public class JBossASTomcatConnectorDiscoveryComponent extends MBeanResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(this.getClass());

    private static final String LOCAL_IP = "0.0.0.0";

    // MBeanResourceDiscoveryComponent Overridden Methods  --------------------------------------------

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {
        Set<DiscoveredResourceDetails> resourceDetails = super.discoverResources(context);

        /*
         * Get the connectors, pull the schema from them and set them later. 
         * jk-8009               JBoss 3.2, 4.0 (AJP connector) 
         * http-0.0.0.0-8080     JBoss 3.2, 4.0, 4.2 (http + https connector) 
         * ajp-0.0.0.0-9009      JBoss 4.2 (AJP connector)
         */
        EmsConnection connection = context.getParentResourceComponent().getEmsConnection();
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(
            "jboss.web:type=GlobalRequestProcessor,name=%name%");
        List<EmsBean> beans = connection.queryBeans(queryUtility.getTranslatedQuery());

        // We can't populate the name and schema in the plugin config if the GlobalRequestProcessor MBeans aren't
        // deployed yet, so just abort and try again the next time the PC calls us.
        if (beans.size() != resourceDetails.size()) {
            if (log.isDebugEnabled())
                log.debug("jboss.web:type=GlobalRequestProcessor,name=* MBeans are not fully deployed yet - aborting...");
            return Collections.emptySet();
        }
        // Map <port, schema>
        Map<String, String> schemaMap = new HashMap<String, String>(beans.size());

        // Map <port, addressPresent>
        Map<String, Boolean> addressPresentMap = new HashMap<String, Boolean>(beans.size());

        for (EmsBean bean : beans) {
            EmsBeanName eName = bean.getBeanName();
            String oName = eName.getKeyProperty("name");
            String[] comps = oName.split("-");
            if (comps.length == 2) {
                schemaMap.put(comps[1], comps[0]);
                addressPresentMap.put(comps[1], false);
            } else if (comps.length == 3) {
                schemaMap.put(comps[2], comps[0]);
                addressPresentMap.put(comps[2], true);
            } else {
                log.warn("Unknown ObjectName for GlobalRequestProcessor: " + oName);
            }
        }

        // The address may be read with %2F at the start of it. Parse that out before returning the resources.
        // Also set the schema for the connectors as the non-http connectors use a different naming schema.
        for (DiscoveredResourceDetails resource : resourceDetails) {
            Configuration pluginConfiguration = resource.getPluginConfiguration();

            String dirtyAddress = pluginConfiguration.getSimple(JBossASTomcatConnectorComponent.PROPERTY_ADDRESS)
                .getStringValue();
            String port = pluginConfiguration.getSimple(JBossASTomcatConnectorComponent.PROPERTY_PORT).getStringValue();
            boolean portHasAddress = (Boolean.TRUE.equals(addressPresentMap.get(port)));

            if (dirtyAddress.startsWith("%2F")) {
                String cleanAddress = dirtyAddress.substring(3);

                // Update the plugin configuration property for address or remove it if not needed
                if (portHasAddress) {
                    pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_ADDRESS,
                        cleanAddress));
                    pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_DASH, "-"));
                } else {
                    pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_ADDRESS, ""));
                    pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_DASH, ""));
                }

                // The resource key contains the address, so update that too
                String dirtyResourceKey = resource.getResourceKey();
                String cleanResourceKey = dirtyResourceKey.replace(dirtyAddress, cleanAddress);
                resource.setResourceKey(cleanResourceKey);
                String resourceName = resource.getResourceName();
                if (!cleanAddress.equals(LOCAL_IP)) {
                    resourceName = resourceName.replace("(", " (" + cleanAddress + ":");
                    resource.setResourceName(resourceName);
                }
            } else { // Address not dirty
                if (portHasAddress) {
                    pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_DASH, "-"));
                } else {
                    pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_ADDRESS, ""));
                    pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_DASH, ""));
                }
            }

            String schema = schemaMap.get(port);
            pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_SCHEMA, schema));
            //         if (log.isDebugEnabled())
            log.debug("Found a connector: " + schema + "-" + dirtyAddress + ":" + port);
        }

        return resourceDetails;
    }
}