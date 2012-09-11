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
package org.rhq.plugins.cassandra;

import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * @author John Sanda
 */
public class CassandraNodeDiscoveryComponent extends JMXDiscoveryComponent {

//    @Override
//    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) throws Exception {
//        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
//        List<ProcessScanResult> processScanResults = context.getAutoDiscoveredProcesses();
//
//        for (ProcessScanResult processScanResult : processScanResults) {
//            DiscoveredResourceDetails discoveredDetails = getDetails(context, processScanResult);
//            if (discoveredDetails != null) {
//                details.add(discoveredDetails);
//            }
//        }
//
//        return details;
//    }
//
//    private DiscoveredResourceDetails getDetails(ResourceDiscoveryContext context,
//        ProcessScanResult processScanResult) {
//        ProcessInfo processInfo = processScanResult.getProcessInfo();
//        String jmxPort = null;
//
//        for (String arg : processInfo.getCommandLine()) {
//            if (arg.startsWith("-Dcom.sun.management.jmxremote.port")) {
//                String[] jmxPortArg = arg.split("=");
//                jmxPort = jmxPortArg[1];
//                break;
//            }
//        }
//
//        if (jmxPort == null) {
//            return null;
//        }
//
//        String resourceKey = "CassandraDaemon:" + jmxPort;
//        String resourceName = "CassandraDaemon";
//
//        Configuration pluginConfig = new Configuration();
//        pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
//            J2SE5ConnectionTypeDescriptor.class.getName()));
//        pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
//            //service:jmx:rmi:///jndi/rmi://127.0.0.1:7199/jmxrmi
//            "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxPort + "/jmxrmi"));
//
//        return new DiscoveredResourceDetails(context.getResourceType(), resourceKey, resourceName, null, null,
//            pluginConfig, processInfo);
//    }
}
