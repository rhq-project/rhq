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
import java.util.List;

import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SigarAccess;
import org.rhq.core.system.SystemInfoException;
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
     * @param process a java process
     * @return the JMX service URL that can be used to obtain the java process' MBeanServer, or null if connecting to
     *         the process is not possible
     */
    public static JMXServiceURL extractJMXServiceURL(ProcessInfo process) {
        if (!attachApiAvailable) {
            LOG.debug("Returning null, since the Attach API is not available...");
            return null;
        }

        JMXServiceURL url;
        try {
            VirtualMachine vm = attachToVirtualMachine(process);

            if (vm == null) {
                return null;
            }

            String jmxConnectorAddress = getJmxConnectorAddress(vm);

            try {
                vm.detach();
            } catch (Exception e) {
                // We already succeeded in obtaining the connector address, so just log this, rather than throwing an exception.
                LOG.error("Failed to detach from JVM [" + vm + "].", e);
            }

            url = new JMXServiceURL(jmxConnectorAddress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract JMX service URL for process with PID [" + process.getPid()
                + "].", e);
        }

        LOG.debug("JMX service URL for java process with PID [" + process.getPid() + "]: " + url);
        return url;
    }

    private static VirtualMachine attachToVirtualMachine(ProcessInfo process) throws AttachNotSupportedException,
        IOException {
        VirtualMachine vm = null;
        List<VirtualMachineDescriptor> vmDescriptors = VirtualMachine.list();
        for (VirtualMachineDescriptor vmDescriptor : vmDescriptors) {
            if (Long.valueOf(vmDescriptor.id()) == process.getPid()) {

                boolean attachPossible = false;

                String vmUserName = process.getCredentialsName().getUser();
                String agentUserName = System.getProperty("user.name");

                if(vmUserName == null || agentUserName == null) {
                    // By default this would make more sense, but lets not break any existing behavior
                    long targetEuid = process.freshSnapshot().getCredentials().getEuid();
                    long agentEuid = getAgentProcessUid();
                    if(agentEuid > 0 && targetEuid == agentEuid) {
                        attachPossible = true;
                    }
                } else {
                    attachPossible = agentUserName.equals(vmUserName);
                }

                if (attachPossible) {
                    LOG.debug("Attaching to JVM for java process with PID [" + process.getPid() + "]...");
                    vm = VirtualMachine.attach(vmDescriptor);
                    LOG.debug("Attached to JVM [" + vm + "].");
                } else {
                    LOG.debug("Cannot attach to JVM for java process with PID [" + process.getPid()
                        + "], because it is running as a different user (" + vmUserName
                        + ") than the user the Agent is running as (" + agentUserName + ").");
                }
                break;
            }
        }
        return vm;
    }

    private static long getAgentProcessUid() {
        try {
            SigarProxy sigar = SigarAccess.getSigar();
            ProcCred procCred = sigar.getProcCred(sigar.getPid());
            return procCred.getEuid();
        } catch (SystemInfoException e) {
            return 0;
        } catch (SigarException e) {
            return 0;
        }
    }

    private static String getJmxConnectorAddress(VirtualMachine vm) throws IOException {
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
        return jmxConnectorAddress;
    }

    private JvmUtility() {
    }

}
