/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.clientapi.descriptor;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.Help;
import org.rhq.core.util.MessageDigestGenerator;

import java.net.URL;
import java.io.IOException;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Transforms some input into a {@link org.rhq.core.domain.plugin.Plugin} object.
 */
public class PluginTransformer {

    /**
     * Takes the given plugin descriptor and plugin JAR file URL and converts them into a
     * {@link org.rhq.core.domain.plugin.Plugin} object. This method does not set the <code>content</code> property
     * of the plugin.
     *
     * @param pluginDescriptor The plugin descriptor from which to create the plugin
     * @param pluginURL The URL of the plugin JAR file from which to create the plugin
     * @return A new plugin object create from the descriptor and plugin URL
     * @throws PluginTransformException if any IO errors occur trying to read the JAR file or if a version not in the
     * plugin descriptor or in the plugin JAR manifest
     */
    public Plugin toPlugin(PluginDescriptor pluginDescriptor, URL pluginURL) {
        try {
            Plugin plugin = new Plugin();
            plugin.setName(pluginDescriptor.getName());

            if (pluginDescriptor.getDisplayName() == null) {
                plugin.setDisplayName(pluginDescriptor.getName());
            }
            else {
                plugin.setDisplayName(pluginDescriptor.getDisplayName());
            }

            plugin.setAmpsVersion(getAmpsVersion(pluginDescriptor));
            plugin.setDescription(pluginDescriptor.getDescription());
            plugin.setPath(pluginURL.getPath());
            plugin.setMtime(pluginURL.openConnection().getLastModified());
            plugin.setHelp(getHelp(pluginDescriptor));
            plugin.setMd5(getMd5(pluginURL));
            plugin.setVersion(getVersion(pluginDescriptor, pluginURL));

            return plugin;
        }
        catch (IOException e) {
            throw new PluginTransformException("Failed to create plugin.", e);
        }
    }

    private String getAmpsVersion(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor.getAmpsVersion() == null) {
            return "2.0";
        }
        return pluginDescriptor.getAmpsVersion();
    }

    private String getHelp(PluginDescriptor pluginDescriptor) {
        Help help = pluginDescriptor.getHelp();
        if (help == null || help.getContent().isEmpty()) {
            return null;
        }
        return help.getContent().get(0).toString();
    }

    private String getMd5(URL pluginURL) throws IOException {
        return MessageDigestGenerator.getDigestString(pluginURL);
    }

    String getVersion(PluginDescriptor pluginDescriptor, URL pluginURL) throws IOException {
        String version = pluginDescriptor.getVersion();
        if (version == null) {
            version = getVersionFromPluginJarManifest(pluginURL);
        }
        
        if (version == null) {
            throw new PluginTransformException("No version is defined for plugin jar [" + pluginURL
                + "]. A version must be defined either via the MANIFEST.MF '" + Attributes.Name.IMPLEMENTATION_VERSION
                + "' attribute or via the plugin descriptor 'version' attribute.");
        }

        return version;
    }

    private String getVersionFromPluginJarManifest(URL pluginJarUrl) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(pluginJarUrl.openStream());
        jarInputStream.close();
        Manifest manifest = jarInputStream.getManifest();
        if (manifest != null) {
            Attributes attributes = manifest.getMainAttributes();
            return attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } else {
            return null;
        }
    }
}
