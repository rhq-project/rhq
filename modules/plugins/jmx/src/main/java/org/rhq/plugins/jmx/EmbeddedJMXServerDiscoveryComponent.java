/*
 * RHQ Management Platform
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
package org.rhq.plugins.jmx;

import java.util.HashSet;
import java.util.Set;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

/**
 * This discovery component can be used to include JVM information under a parent Process oriented server that supports
 * JMX. The parent resource type's component must implement JMXComponent.
 *
 * @author Greg Hinkle
 */
public class EmbeddedJMXServerDiscoveryComponent implements ResourceDiscoveryComponent<JMXComponent> {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {

        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();

        Configuration configuration = context.getDefaultPluginConfiguration();
        EmsConnection emsConnection = context.getParentResourceComponent().getEmsConnection();

        if (emsConnection.getConnectionProvider().getConnectionSettings().getConnectionType() instanceof InternalVMTypeDescriptor) {
            // If our parent is internal, it may have chosen a specific local mbean server (as in the jboss server)
            // so we will look our own up
            configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
                "Local Connection"));
            configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE, InternalVMTypeDescriptor.class
                .getName()));
        } else {

            boolean isJmxRemote = false;
            String jmxRemotePort = "3001";
            ProcessInfo nativeProcess = context.getParentResourceContext().getNativeProcess();
            if (nativeProcess != null) {
                String[] commandLine = nativeProcess.getCommandLine();
                for (String item : commandLine) {
                    if (item.contains("jmxremote.port")) {
                        isJmxRemote = true;
                        jmxRemotePort = item.substring(item.indexOf('=') + 1);
                    }
                    // TODO get user / password 
                }
            }

            // with an external parent, we still need to check, if jmxremote (for jconsole) is enabled or not
            if (isJmxRemote) {
                configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                    J2SE5ConnectionTypeDescriptor.class.getName()));

                J2SE5ConnectionTypeDescriptor desc = new J2SE5ConnectionTypeDescriptor();
                String url = desc.getDefaultServerUrl();
                url = url.replace("8999", jmxRemotePort);
                configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY, url));
            } else {
                configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                    JMXDiscoveryComponent.PARENT_TYPE));
            }
        }

        if (emsConnection.getBean("java.lang:type=OperatingSystem") != null) {
            // Only inventory a VM that has the platform mbean's exposed and available
            DiscoveredResourceDetails s = new DiscoveredResourceDetails(context.getResourceType(), "JVM", context
                .getResourceType().getName(), System.getProperty("java.version"), context.getResourceType()
                .getDescription(), configuration, null);

            found.add(s);
        }

        return found;
    }
}