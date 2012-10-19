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
import java.lang.reflect.Method;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
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
 * JMX (e.g. JBoss AS or Tomcat). The parent resource type's component must implement JMXComponent.
 * <p>
 * The discovered resource is called the same name as its type or according to the "embeddedJvmName" property
 * from the parent server's plugin configuration.
 * 
 * @author Greg Hinkle
 */
public class EmbeddedJMXServerDiscoveryComponent implements ResourceDiscoveryComponent<JMXComponent> {    
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context)
        throws Exception {
        Set<DiscoveredResourceDetails> discoveredServers = new HashSet<DiscoveredResourceDetails>();

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

            // With an external parent, we still need to check, if jmxremote (for jconsole) is enabled or not.
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

        EmsBean runtimeMBean = emsConnection.getBean("java.lang:type=Runtime");
        // Only inventory a VM that has the platform MXBeans exposed.
        if (runtimeMBean != null) {
            String version = getSystemProperty(runtimeMBean, "java.version");
            DiscoveredResourceDetails server = new DiscoveredResourceDetails(context.getResourceType(), "JVM",
                ParentDefinedJMXServerNamingUtility.getJVMName(context), version, context.getResourceType().getDescription(),
                    configuration, null);
            discoveredServers.add(server);
        }

        return discoveredServers;
    }

    private static String getSystemProperty(EmsBean runtimeMBean, String propertyName) throws Exception {
        // We must use reflection for the Open MBean classes (TabularData and CompositeData) to avoid
        // ClassCastExceptions due to EMS having used a different classloader than us to load them.
        Object tabularDataObj = runtimeMBean.getAttribute("systemProperties").refresh();
        Method getMethod = tabularDataObj.getClass().getMethod("get",
                new Class[] { Class.forName("[Ljava.lang.Object;") });
        // varargs don't work out when the arg itself is an array, so specify the parameters explicitly using arrays.
        Object compositeDataObj = getMethod.invoke(tabularDataObj, new Object[] { new Object[] { propertyName }});
        getMethod = compositeDataObj.getClass().getMethod("get", String.class);
        String version = (String)getMethod.invoke(compositeDataObj, "value");
        return version;
    }
}