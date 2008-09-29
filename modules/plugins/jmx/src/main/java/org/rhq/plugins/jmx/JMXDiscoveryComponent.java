/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.jmx;

import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.system.ProcessInfo;

/**
 * This product will discover JDK 5 agents running locally that have active JSR-160 connectors defined via system
 * properties
 *
 * @author Greg Hinkle
 */
public class JMXDiscoveryComponent implements ResourceDiscoveryComponent {
    private static final Log log = LogFactory.getLog(JMXDiscoveryComponent.class);

    public static final String VMID_CONFIG_PROPERTY = "vmid";

    public static final String COMMAND_LINE_CONFIG_PROPERTY = "commandLine";

    public static final String CONNECTOR_ADDRESS_CONFIG_PROPERTY = "connectorAddress";

    public static final String INSTALL_URI = "installURI";

    public static final String CONNECTION_TYPE = "type";

    public static final String PARENT_TYPE = "PARENT";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {

        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();

        // This model of discovery is of questionable usefullness since if you restart your process you'll get a new resource
        // Works only on JDK6 and maybe some 64 bit JDK5 See JBNADM-3332.
        //
        //        Map<Integer, LocalVirtualMachine> vms;
        //
        //        try {
        //            vms = LocalVMFinder.getManageableVirtualMachines();
        //        } catch (Exception e) {
        //            log.info("JMX Platform Autodiscovery only supported on JDK6 and above");
        //            return null;
        //        }
        //
        //        if (vms != null) {
        //            for (LocalVirtualMachine vm : vms.values()) {
        //                // TODO: Might want to limit to vms already managed as the other kind are temporary connector addresses
        //                String resourceKey = (vm.getCommandLine() != null) ? vm.getCommandLine() : vm.getConnectorAddress();
        //                DiscoveredResourceDetails s = new DiscoveredResourceDetails(context.getResourceType(), resourceKey,
        //                    "Java VM [" + vm.getVmid() + "]", System.getProperty("java.version"), // TODO Get the vm's version
        //                    vm.getCommandLine(), null, null);
        //
        //                Configuration configuration = s.getPluginConfiguration();
        //                configuration.setNotes("Auto-discovered");
        //                configuration.put(new PropertySimple(VMID_CONFIG_PROPERTY, String.valueOf(vm.getVmid())));
        //                configuration.put(new PropertySimple(CONNECTOR_ADDRESS_CONFIG_PROPERTY, String.valueOf(vm
        //                    .getConnectorAddress())));
        //                configuration
        //                    .put(new PropertySimple(COMMAND_LINE_CONFIG_PROPERTY, String.valueOf(vm.getCommandLine())));
        //                configuration.put(new PropertySimple(CONNECTION_TYPE, LocalVMTypeDescriptor.class.getName()));
        //
        //                found.add(s);
        //            }
        //
        //            /* GH: Disabling discovery of the internal VM... Other plugins should probably embed via the internal
        //             * component above
        //             * DiscoveredResourceDetails localVM = new DiscoveredResourceDetails(context.getResourceType(),
        //             *                                                "InternalVM",
        //             *                "Internal Java VM",
        //             * System.getProperty("java.version"),                                                             "VM of
        //             * plugin container", null, null); Configuration configuration = localVM.getPluginConfiguration();
        //             * configuration.put(new PropertySimple(CONNECTOR_ADDRESS_CONFIG_PROPERTY, "Local Connection"));
        //             * configuration.put(new PropertySimple(CONNECTION_TYPE, InternalVMTypeDescriptor.class.getName()));
        //             *
        //             *found.add(localVM);*/
        //      }


        try {
            List<ProcessInfo> processes =
                    context.getSystemInformation().getProcesses(
                            "process|basename|match=^java.*");

            for (ProcessInfo process : processes) {
                DiscoveredResourceDetails details = discoverProcess(context, process);
                if (details != null) {
                    found.add(details);
                }
            }
        } catch (Exception e) {
            log.warn("Unable to complete base jmx server discovery - cause (enable DEBUG to see stack trace): " + e);
            log.debug("Stack trace follows...", e);
        }

        for (Configuration c : (List<Configuration>) context.getPluginConfigurations()) {
            String resourceKey = c.getSimpleValue(CONNECTOR_ADDRESS_CONFIG_PROPERTY, null);
            String connectionType = c.getSimpleValue(CONNECTION_TYPE, null);

            DiscoveredResourceDetails s = new DiscoveredResourceDetails(context.getResourceType(), resourceKey,
                    "Java VM", "?", connectionType + " [" + resourceKey + "]", null, null);

            s.setPluginConfiguration(c);

            found.add(s);
        }

        return found;
    }

    protected DiscoveredResourceDetails discoverProcess(ResourceDiscoveryContext context, ProcessInfo process) {
        String portProp = "com.sun.management.jmxremote.port";

        String port = null;
        for (String argument : process.getCommandLine()) {
            String cmdLineArg = "-D" + portProp + "=";
            if (argument.startsWith(cmdLineArg)) {
                port = argument.substring(cmdLineArg.length());
                break;
            }
        }

        if (port == null) {
            port = process.getEnvironmentVariable(portProp);
        }

        DiscoveredResourceDetails details = null;
        if (port != null) {

            String name = null;
            for (int i = 1; i < process.getCommandLine().length; i++) {
                String arg = process.getCommandLine()[i];

                if (!arg.startsWith("-")) {
                    name = arg;
                    break;
                }
            }

            name += " (" + port + ")";

            Configuration config = context.getDefaultPluginConfiguration();
            config.put(new PropertySimple(CONNECTION_TYPE,"org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor"));
            config.put(new PropertySimple(CONNECTOR_ADDRESS_CONFIG_PROPERTY, "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi"));
//            config.put(new PropertySimple(INSTALL_URI, process.getCurrentWorkingDirectory()));

            details = new DiscoveredResourceDetails(
                    context.getResourceType(),
                    port,
                    name,
                    null,
                    "Standalone JVM Process",
                    config,
                    null);
        }

        return details;
    }
}