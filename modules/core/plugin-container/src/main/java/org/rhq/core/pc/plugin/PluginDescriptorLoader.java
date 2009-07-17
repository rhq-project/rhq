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
package org.rhq.core.pc.plugin;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;

/**
 * This class handles parsing and validating an agent plugin's descriptor.
 *
 * @author John Mazzitelli
 */
public class PluginDescriptorLoader {

    private final Log log = LogFactory.getLog(PluginDescriptorLoader.class);

    private final URL pluginJarUrl;
    private final ClassLoader pluginClassLoader;

    public PluginDescriptorLoader(URL pluginJarUrl, ClassLoader pluginClassLoader) throws PluginContainerException {

        this.pluginJarUrl = pluginJarUrl;
        this.pluginClassLoader = pluginClassLoader;

        if (log.isDebugEnabled()) {
            log.debug("Created " + toString());
        }
    }

    public ClassLoader getPluginClassLoader() {
        return this.pluginClassLoader;
    }

    /**
     * Loads and validates the plugin descriptor from the plugin jar associated with this loader.
     *
     * @return the JAXB plugin descriptor object
     * @throws PluginContainerException on failure to load the descriptor
     */
    public PluginDescriptor loadPluginDescriptor() throws PluginContainerException {
        return AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(this.pluginJarUrl);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [pluginJarUrl=" + this.pluginJarUrl + ", pluginClassLoader="
            + this.pluginClassLoader + "]";
    }
}
