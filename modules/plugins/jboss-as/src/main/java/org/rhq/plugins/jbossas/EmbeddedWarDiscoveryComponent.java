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

import java.io.File;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas.util.WarDiscoveryHelper;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * JON plugin discovery component for finding WARs that are nested inside of an EAR.
 *
 * @author Jason Dobies
 * @author Ian Springer
 */
public class EmbeddedWarDiscoveryComponent extends MBeanResourceDiscoveryComponent<JMXComponent> {
    // ResourceDiscoveryComponent Implementation  --------------------------------------------

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {
        // Generate object name based on the parent EAR with the following format.
        //   jboss.management.local:J2EEApplication=rhq.ear,J2EEServer=Local,j2eeType=WebModule,name=on-portal.war

        ApplicationComponent parentEarComponent = (ApplicationComponent) context.getParentResourceComponent();
        String parentEar = parentEarComponent.getApplicationName();

        String objectName = "jboss.management.local:J2EEApplication=" + parentEar + ",J2EEServer=Local,j2eeType=WebModule,name=%name%";

        // Stuff the object name into the default plugin configuration to look like any other JMX discovery
        // where the objectName is read from the default plugin configuration
        Configuration defaultPluginConfiguration = context.getDefaultPluginConfiguration();
        defaultPluginConfiguration.put(new PropertySimple(MBeanResourceDiscoveryComponent.PROPERTY_OBJECT_NAME, objectName));

        // Call the base MBean discovery method to perform the actual discovery
        Set<DiscoveredResourceDetails> resourceDetails = super.performDiscovery(defaultPluginConfiguration, parentEarComponent, context.getResourceType());

        // Once we've finished making sure the plugin configurations have the data we need:
        // 1) First the stuff generic to all WARs...
        JBossASServerComponent grandparentJBossASComponent = parentEarComponent.getParentResourceComponent();
        resourceDetails = WarDiscoveryHelper.initPluginConfigurations(grandparentJBossASComponent, resourceDetails,parentEarComponent);

        // 2) Then the stuff specific to embedded WARs...
        String parentEarFullFileName = parentEarComponent.getFileName() + File.separator;
        for (DiscoveredResourceDetails resource : resourceDetails) {
            Configuration pluginConfiguration = resource.getPluginConfiguration();
            pluginConfiguration.put(new PropertySimple(WarComponent.FILE_NAME, parentEarFullFileName + resource.getResourceName()));
        }

        return resourceDetails;
    }
}