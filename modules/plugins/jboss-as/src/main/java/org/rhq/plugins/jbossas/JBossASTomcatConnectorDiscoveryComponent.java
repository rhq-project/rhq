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
 * Plugin discovery component for JBoss Web (embedded Tomcat) connectors. The bulk of the discovery is performed by the
 * super class. This class exists to work with the bean attribute values once they have been read.
 *
 * @author Jason Dobies
 * @author Ian Springer
 */
public class JBossASTomcatConnectorDiscoveryComponent extends MBeanResourceDiscoveryComponent<JMXComponent> {
    private final Log log = LogFactory.getLog(this.getClass());

    // MBeanResourceDiscoveryComponent Overridden Methods  --------------------------------------------

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {
        Set<DiscoveredResourceDetails> resourceDetails = super.discoverResources(context);

        /*
         * Lookup all jboss.web:type=GlobalRequestProcessor,* MBeans in order to associate them later with the
         * corresponding jboss.web:type=Connector,* primary connector MBeans.
         */
        EmsConnection connection = context.getParentResourceComponent().getEmsConnection();
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility("jboss.web:type=GlobalRequestProcessor,name=%name%");
        List<EmsBean> beans = connection.queryBeans(queryUtility.getTranslatedQuery());

        // We can't populate the name and scheme in the plugin config if the GlobalRequestProcessor MBeans aren't
        // deployed yet, so just abort and try again the next time the PC calls us.
        if (beans.size() != resourceDetails.size()) {
            if (log.isDebugEnabled())
                log.debug("jboss.web:type=GlobalRequestProcessor,name=* MBeans are not fully deployed yet - aborting...");
            return Collections.emptySet();
        }
        // Map <port, scheme>
        Map<String, String> schemeMap = new HashMap<String, String>(beans.size());

        // Map <port, addressPresent>
        Map<String, Boolean> addressPresentMap = new HashMap<String, Boolean>(beans.size());

        for (EmsBean bean : beans) {
            EmsBeanName eName = bean.getBeanName();

            // There are three possible formats for the value of the 'name' key property. When parsing, it's important
            // to remember that the hostname can potentially contain a '-'. Note, %2F is a URL-encoded '/'.
            //   1) jk-8009 (jk connectors in JBoss 3.2 only, due to what I think is a bug)
            //   2) http-192.168.10.11-8080
            //   3) http-foo-bar.example.com%2F192.168.10.11-8080
            String oName = eName.getKeyProperty("name");

            int firstDashIndex = oName.indexOf('-');
            String scheme = oName.substring(0, firstDashIndex);
            String remainder = oName.substring(firstDashIndex + 1);

            int lastDashIndex = remainder.lastIndexOf('-');
            String port = remainder.substring(lastDashIndex + 1);
            schemeMap.put(port, scheme);
            boolean addressPresent = (lastDashIndex != -1);
            addressPresentMap.put(port, addressPresent);
        }

        // The address may be read with %2F at the start of it. Parse that out before returning the resources.
        // Also set the schema for the connectors as the non-http connectors use a different naming schema.
        for (DiscoveredResourceDetails resource : resourceDetails) {
            Configuration pluginConfiguration = resource.getPluginConfiguration();

            String dirtyAddress = pluginConfiguration.getSimple(JBossASTomcatConnectorComponent.PROPERTY_ADDRESS).getStringValue();

            String cleanAddress;
            if (dirtyAddress.startsWith("%2F")) {
                cleanAddress = dirtyAddress.substring(3);

                // The resource key contains the address, so update that too
                String dirtyResourceKey = resource.getResourceKey();
                String cleanResourceKey = dirtyResourceKey.replace(dirtyAddress, cleanAddress);
                resource.setResourceKey(cleanResourceKey);
            } else {
                cleanAddress = dirtyAddress;
            }

            // Add the address to the Resource name (e.g. "Tomcat Connector (127.0.0.1:8080)").
            String resourceName = resource.getResourceName();
            resourceName = resourceName.replace("(", "(" + cleanAddress + ":");
            resource.setResourceName(resourceName);

            String port = pluginConfiguration.getSimple(JBossASTomcatConnectorComponent.PROPERTY_PORT).getStringValue();

            String scheme = schemeMap.get(port);
            pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_SCHEMA, scheme));

            // Update the "address" and "dash" plugin config props, or remove them if not needed.
            if (addressPresentMap.containsKey(port)) {
                pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_ADDRESS, cleanAddress));
                pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_DASH, "-"));
            } else {
                pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_ADDRESS, ""));
                pluginConfiguration.put(new PropertySimple(JBossASTomcatConnectorComponent.PROPERTY_DASH, ""));
            }

            log.debug("Found a JBoss Web connector: " + scheme + "-" + cleanAddress + ":" + port);
        }

        return resourceDetails;
    }
}