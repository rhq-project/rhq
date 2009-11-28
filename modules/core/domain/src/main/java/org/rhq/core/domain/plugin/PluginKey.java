/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.core.domain.plugin;

/**
 * Encapsulates informaton that can uniquely identify a plugin.
 * 
 * @author John Mazzitelli
 */
public class PluginKey {
    private final PluginDeploymentType deployment;
    private final String pluginType;
    private final String pluginName;

    /**
     * Creates a plugin key that identifies an agent plugin. There is only
     * one plugin container that runs in the agent, thus there is only
     * one "type" of an agent plugin. Therefore, {@link #getPluginType()} on the
     * returned object will return <code>null</code> to signify this.
     * 
     * @param pluginName the name of the plugin
     * @return the plugin key for the agent plugin
     */
    public static PluginKey createAgentPluginKey(String pluginName) {
        return new PluginKey(PluginDeploymentType.AGENT, null, pluginName);
    }

    /**
     * Creates a plugin key that identifies a server plugin. All server plugins
     * must have a type and a name.
     * 
     * @param pluginType the type of plugin - must not be null or an empty string
     * @param pluginName the name of the plugin
     * @return the plugin key for the server plugin
     */
    public static PluginKey createServerPluginKey(String pluginType, String pluginName) {
        // for server plugins, cannot allow an empty string for plugin type
        if (pluginType == null || pluginType.length() == 0) {
            throw new IllegalArgumentException("invalid pluginType: " + pluginType);
        }
        return new PluginKey(PluginDeploymentType.SERVER, pluginType, pluginName);
    }

    /**
     * Create a plugin key that identifies the given agent plugin.
     * 
     * @param plugin agent plugin
     */
    public PluginKey(Plugin plugin) {
        this(plugin.getDeployment(), null, plugin.getName());
    }

    /**
     * Create a plugin key that identifies the given server plugin.
     * 
     * @param plugin server plugin
     */
    public PluginKey(ServerPlugin plugin) {
        this(plugin.getDeployment(), plugin.getType(), plugin.getName());
    }

    public PluginKey(PluginDeploymentType deployment, String pluginType, String pluginName) {
        if (deployment == null) {
            throw new IllegalArgumentException("deployment==null");
        }
        if (pluginType == null && deployment != PluginDeploymentType.AGENT) {
            throw new IllegalArgumentException("only agent plugins can have null type");
        }
        if (pluginType != null && deployment == PluginDeploymentType.AGENT) {
            throw new IllegalArgumentException("agent plugins must have null type");
        }
        if (pluginName == null || pluginName.length() == 0) {
            throw new IllegalArgumentException("invalid pluginName: " + pluginName);
        }

        this.deployment = deployment;
        this.pluginType = pluginType;
        this.pluginName = pluginName;
    }

    public PluginDeploymentType getDeployment() {
        return deployment;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getPluginType() {
        return pluginType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PluginKey [deployment=").append(deployment).append(", pluginType=").append(pluginType).append(
            ", pluginName=").append(pluginName).append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + deployment.hashCode();
        result = prime * result + ((pluginType == null) ? 0 : pluginType.hashCode());
        result = prime * result + pluginName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PluginKey)) {
            return false;
        }
        PluginKey other = (PluginKey) obj;
        if (!pluginName.equals(other.pluginName)) {
            return false;
        }
        if (pluginType == null) {
            if (other.pluginType != null) {
                return false;
            }
        } else if (!pluginType.equals(other.pluginType)) {
            return false;

        }
        if (!deployment.equals(other.deployment)) {
            return false;
        }
        return true;
    }
}
