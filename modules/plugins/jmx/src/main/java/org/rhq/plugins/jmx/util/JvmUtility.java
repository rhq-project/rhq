/*
 * RHQ Management Platform
 * Copyright (C) 2011-2012 Red Hat, Inc.
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
package org.rhq.plugins.jmx.util;

import java.io.File;
import java.io.IOException;

import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Ian Springer
 */
public class JvmUtility {

    private static final Log LOG = LogFactory.getLog(MBeanResourceComponent.class);

    private static final String AGENT_PROP_JMXREMOTE_LOCAL_CONNECTOR_ADDRESS =
        "com.sun.management.jmxremote.localConnectorAddress";

    private static boolean attachApiAvailable;

    static {
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
            attachApiAvailable = true;
        } catch (ClassNotFoundException e) {
            LOG.warn("JDK tools.jar not found on system classpath - cannot discover JVMs using Sun JVM Attach API; "
                            + "to fix this, run the RHQ Agent on a JDK, rather than a JRE.");
        }
    }
        
    /**
     * TODO
     *
     * @param pid the process ID of a JVM (java*) process
     *
     * @return
     *
     * @throws Exception
     */
    public static JMXServiceURL extractJMXServiceURL(long pid) {
        if (!attachApiAvailable) {
            LOG.debug("Returning null since the Attach API is not available...");
            return null;
        }
        LOG.debug("Attaching to JVM for java process with PID [" + pid + "]...");
        JMXServiceURL url;
        try {
            VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
            LOG.debug("Attached to JVM [" + vm + "].");

            String jmxConnectorAddress = vm.getAgentProperties().getProperty(AGENT_PROP_JMXREMOTE_LOCAL_CONNECTOR_ADDRESS);
            LOG.debug("Connector address for JVM [" + vm + "] is [" + jmxConnectorAddress + "].");
            if (jmxConnectorAddress == null) {
                // java.home always points to the jre dir (e.g. /usr/java/default/jre).
                String jreDir = vm.getSystemProperties().getProperty("java.home");
                // management-agent.jar is included with the v6 JRE, so we can rely on it always being there.
                File jmxAgentJarFile = new File(jreDir, "lib/management-agent.jar");
                String jmxAgentJar = jmxAgentJarFile.getCanonicalPath();
                try {
                    vm.loadAgent(jmxAgentJar);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load JVM agent from [" + jmxAgentJar + "].", e);
                }
                
                // JMX agent is started - get the connector address.
                LOG.debug("JMX agent started - getting the connector address...");
                jmxConnectorAddress = vm.getAgentProperties().getProperty(AGENT_PROP_JMXREMOTE_LOCAL_CONNECTOR_ADDRESS);
                if (jmxConnectorAddress == null) {
                    throw new RuntimeException("Failed to determine JMX connector address.");
                }                
            }

            try {
                vm.detach();
            } catch (Exception e) {
                // We already succeeded in obtaining the connector address, so just log this, rather than throwing an exception.
                LOG.error("Failed to detach from JVM [" + vm + "].", e);
            }

            url = new JMXServiceURL(jmxConnectorAddress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract JMX service URL for process with PID [" + pid + "].", e);
        }

        LOG.debug("JMX service URL for java process with PID [" + pid + "]: " + url);
        return url;
    }

    private JvmUtility() {
    }

}
