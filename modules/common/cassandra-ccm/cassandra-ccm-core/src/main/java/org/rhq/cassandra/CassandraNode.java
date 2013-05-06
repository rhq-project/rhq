/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.rhq.cassandra.installer.RMIContextFactory;


/**
 * @author John Sanda
 * @author Jirka Kremser
 */
public class CassandraNode {

    private String hostName;

    private int jmxPort;

    private int nativeTransportPort;
    
    private static final String JMX_CONNECTION_STRING = "service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi";

    public CassandraNode(String hostName, int jmxPort, int nativeTransportPort) {
        this.hostName = hostName;
        this.jmxPort = jmxPort;
        this.nativeTransportPort = nativeTransportPort;
    }

    public String getHostName() {
        return hostName;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    public int getNativeTransportPort() {
        return nativeTransportPort;
    }
    
    public boolean isNativeTransportRunning() throws Exception {
        Boolean nativeTransportRunning = false;
        String url = String.format(JMX_CONNECTION_STRING, getHostName(), getJmxPort());
        JMXServiceURL serviceURL = new JMXServiceURL(url);
        Map<String, String> env = new HashMap<String, String>();
        // see https://issues.jboss.org/browse/AS7-2138
        env.put(Context.INITIAL_CONTEXT_FACTORY, RMIContextFactory.class.getName());
        JMXConnector connector = null;

        try {
            connector = JMXConnectorFactory.connect(serviceURL, env);
            MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
            ObjectName storageService = new ObjectName("org.apache.cassandra.db:type=StorageService");
            nativeTransportRunning = (Boolean) serverConnection.getAttribute(storageService, "NativeTransportRunning");
        } finally {
            if (connector != null) {
                connector.close();
            }
        }
        return nativeTransportRunning;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CassandraNode that = (CassandraNode) o;

        if (jmxPort != that.jmxPort) return false;
        if (nativeTransportPort != that.nativeTransportPort) return false;
        if (!hostName.equals(that.hostName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + jmxPort;
        result = prime * result + nativeTransportPort;
        return result;
    }

    @Override
    public String toString() {
        return "CassandraNode[hostName: " + hostName + ", jmxPort: " + jmxPort + ", nativeTransportPort: " +
            nativeTransportPort + "]";
    }

    public static CassandraNode parseNode(String s) {
        String[] params = s.split("\\|");
        if (params.length != 3) {
            throw new IllegalArgumentException("Expected string of the form, hostname|jmxPort|nativeTransportPort");
        }
        return new CassandraNode(params[0], Integer.parseInt(params[1]), Integer.parseInt(params[2]));
    }
}
