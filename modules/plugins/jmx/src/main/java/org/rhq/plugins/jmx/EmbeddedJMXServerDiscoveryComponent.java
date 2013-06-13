/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.util.ParentDefinedJMXServerNamingUtility;

/**
 * This discovery component can be used to include a singleton JVM Resource under a parent server Resource that supports
 * JMX (e.g. JBoss AS or Tomcat). The parent resource type's component must implement JMXComponent and should discover
 * and manage the {@link EmsConnection}, since this discovery class and the resulting JMXComponent wll both delegate
 * all JMX calls to the parent component's <tt>EmsConnection</tt>. The JVM Resource and its child Resources expose
 * various JVM metrics and operations made available by the platform MXBeans.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class EmbeddedJMXServerDiscoveryComponent implements ResourceDiscoveryComponent<JMXComponent<?>> {

    private static final String RESOURCE_KEY = "JVM";
    private static final String JAVA_VERSION_SYSPROP = "java.version";

    private final Log log = LogFactory.getLog(EmbeddedJMXServerDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent<?>> context)
        throws Exception {
        // Only inventory a JVM that has the platform MXBeans exposed.
        EmsBean runtimeMBean = getRuntimeMXBean(context);
        if (runtimeMBean == null) {
            return Collections.emptySet();
        }

        String name = ParentDefinedJMXServerNamingUtility.getJVMName(context);
        String version = getSystemProperty(runtimeMBean, JAVA_VERSION_SYSPROP);

        Configuration pluginConfig = context.getDefaultPluginConfiguration();
        pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE, JMXDiscoveryComponent.PARENT_TYPE));

        DiscoveredResourceDetails resourceDetails = new DiscoveredResourceDetails(context.getResourceType(),
                RESOURCE_KEY, name, version, context.getResourceType().getDescription(), pluginConfig, null);

        return Collections.singleton(resourceDetails);
    }

    @Nullable
    private EmsBean getRuntimeMXBean(ResourceDiscoveryContext<JMXComponent<?>> context) {
        EmsConnection emsConnection = context.getParentResourceComponent().getEmsConnection();
        if (emsConnection == null) {
            log.debug("Parent EMS connection is null for [" + context.getParentResourceContext().getResourceKey() + "] "
                                + context.getParentResourceContext().getResourceType() + " JVM.");
            return null;
        }

        // EmsConnection caches the list MBeans it previously found
        // See https://bugzilla.redhat.com/show_bug.cgi?id=924903
        emsConnection.refresh();

        EmsBean runtimeMBean = emsConnection.getBean(ManagementFactory.RUNTIME_MXBEAN_NAME);
        if (runtimeMBean == null) {
            log.debug("MBean [" + ManagementFactory.RUNTIME_MXBEAN_NAME + "] not found for ["
                    + context.getParentResourceContext().getResourceKey() + "] "
                    + context.getParentResourceContext().getResourceType() + " JVM.");
        }
        return runtimeMBean;
    }

    private static String getSystemProperty(EmsBean runtimeMBean, String propertyName) throws Exception {
        // We must use reflection for the Open MBean classes (TabularData and CompositeData) to avoid
        // ClassCastExceptions due to EMS having used a different classloader than us to load them.
        EmsAttribute systemPropertiesAttribute = runtimeMBean.getAttribute("systemProperties");
        Object tabularDataObj = systemPropertiesAttribute.refresh();
        Method getMethod = tabularDataObj.getClass().getMethod("get",
            new Class[] { Class.forName("[Ljava.lang.Object;") });
        // varargs don't work out when the arg itself is an array, so specify the parameters explicitly using arrays.
        Object compositeDataObj = getMethod.invoke(tabularDataObj, new Object[] { new Object[] { propertyName } });
        getMethod = compositeDataObj.getClass().getMethod("get", String.class);
        String version = (String) getMethod.invoke(compositeDataObj, "value");
        return version;
    }

}