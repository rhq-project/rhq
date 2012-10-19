/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

/**
 * @author Ian Springer
 */
public class JvmResourceKey {

    private String mainClassName;
    private String explicitValue;
    private Integer jmxRemotingPort;
    private String connectorAddress;
    private transient Type type;
    
    public static JvmResourceKey fromExplicitValue(String mainClassName, String explicitValue) {
        JvmResourceKey instance = new JvmResourceKey(mainClassName);
        instance.explicitValue = explicitValue;
        instance.type = Type.Explicit;
        return instance;
    }

    public static JvmResourceKey fromJmxRemotingPort(String mainClassName, int jmxRemotingPort) {
        JvmResourceKey instance = new JvmResourceKey(mainClassName);
        instance.jmxRemotingPort = jmxRemotingPort;
        instance.type = (mainClassName != null) ? Type.JmxRemotingPort : Type.Legacy;
        return instance;
    }

    public static JvmResourceKey fromConnectorAddress(String connectorAddress) {
        JvmResourceKey instance = new JvmResourceKey(null);
        instance.connectorAddress = connectorAddress;
        instance.type = Type.ConnectorAddress;
        return instance;
    }
    
    public static JvmResourceKey valueOf(String string) {
        JvmResourceKey instance;
        if (string.contains("{") && string.endsWith("}")) {            
            String mainClassName = string.substring(0, string.indexOf('{'));
            String explicitValue = string.substring(string.indexOf('{') + 1, string.length() - 1);
            instance = JvmResourceKey.fromExplicitValue(mainClassName, explicitValue);
        } else if (string.contains("(") && string.endsWith(")")) {
            String mainClassName = string.substring(0, string.indexOf('('));
            String value = string.substring(string.indexOf('(') + 1, string.length() - 1);
            int jmxRemotingPort = Integer.parseInt(value);
            instance = JvmResourceKey.fromJmxRemotingPort(mainClassName, jmxRemotingPort);
        } else {
            
            try {
                int jmxRemotingPort = Integer.parseInt(string);
                // It's a legacy key, e.g. "9999".
                instance = JvmResourceKey.fromJmxRemotingPort(null, jmxRemotingPort);
            } catch (NumberFormatException e) {
                // At this point, assume it's a connector address, e.g.
                // "service:jmx:iiop://127.0.0.1:7001/jndi/weblogic.management.mbeanservers.runtime".
                instance = JvmResourceKey.fromConnectorAddress(string);
            }
        }
        return instance;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public String getExplicitValue() {
        return explicitValue;
    }

    public Integer getJmxRemotingPort() {
        return jmxRemotingPort;
    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JvmResourceKey that = (JvmResourceKey) o;

        if (connectorAddress != null ? !connectorAddress.equals(that.connectorAddress) : that.connectorAddress != null)
            return false;
        if (explicitValue != null ? !explicitValue.equals(that.explicitValue) : that.explicitValue != null)
            return false;
        if (jmxRemotingPort != null ? !jmxRemotingPort.equals(that.jmxRemotingPort) : that.jmxRemotingPort != null)
            return false;
        if (mainClassName != null ? !mainClassName.equals(that.mainClassName) : that.mainClassName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mainClassName != null ? mainClassName.hashCode() : 0;
        result = 31 * result + (explicitValue != null ? explicitValue.hashCode() : 0);
        result = 31 * result + (jmxRemotingPort != null ? jmxRemotingPort.hashCode() : 0);
        result = 31 * result + (connectorAddress != null ? connectorAddress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String string;
        switch (this.type) {
            case Legacy:
                string = this.jmxRemotingPort.toString();
                break;
            case ConnectorAddress:
                string = this.connectorAddress;
                break;
            case JmxRemotingPort:
                string = this.mainClassName + "(" + this.jmxRemotingPort + ")";
                break;
            case Explicit:
                string = this.mainClassName + "{" + this.explicitValue + "}";
                break;
            default:
                throw new IllegalStateException("Unsupported key type: " + this.type);
        }
        return string;
    }

    public enum Type {
        /**
         * The legacy format is a simple integer representing the JVM's JMX remoting port, e.g. "9999"
         */
        Legacy,
        /**
         * Manually added JVM's use the JMX connector address as the key, e.g. 
         * "service:jmx:iiop://127.0.0.1:7001/jndi/weblogic.management.mbeanservers.runtime"
         */
        ConnectorAddress,
        /**
         * The successor of the legacy format; includes the main class name in addition to the JMX remoting port,
         * e.g. "org.example.Main(9999)"
         */
        JmxRemotingPort,
        /**
         * This format is used when a key is explicitly specified on the JVM's command line via the org.rhq.resourceKey
         * sysprop; the main class name is also included, e.g. "org.example.Main{foo}"
         */
        Explicit
    }
    
    private JvmResourceKey(String mainClassName) {
        this.mainClassName = mainClassName;
    }

}
