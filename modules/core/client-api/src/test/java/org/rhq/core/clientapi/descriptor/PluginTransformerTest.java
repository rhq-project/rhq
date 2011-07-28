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

package org.rhq.core.clientapi.descriptor;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.Help;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.util.MessageDigestGenerator;

import java.net.URL;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.JarFile;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

public class PluginTransformerTest {

    @Test
    public void pluginNameShouldBeSetToDescriptorName() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setName("testPlugin");

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertEquals(
            plugin.getName(),
            pluginDescriptor.getName(),
            "The plugin.name property should be set to the plugin descriptor name."
        );
    }

    @Test
    public void pluginDisplayNameShouldBeSetToDescriptorDisplayName() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setName("testPlugin");
        pluginDescriptor.setDisplayName("Test Plugin");

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertEquals(
            plugin.getDisplayName(),
            pluginDescriptor.getDisplayName(),
            "The plugin.displayName property should be set to the plugin descriptor display name."
        );
    }

    @Test
    public void pluginDisplayNameShouldBeSetToDescriptorNameWhenItsDisplayNameIsNull() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setName("testPlugin");

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertEquals(
            plugin.getDisplayName(),
            pluginDescriptor.getName(),
            "The plugin.displayName property should be set to the plugin descriptor name when the descriptor display name is null."
        );
    }

    @Test
    public void pluginAmpsVersionShouldBeSetToDescriptorAmpsVersion() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setAmpsVersion("2.1");

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertEquals(
            plugin.getAmpsVersion(),
            pluginDescriptor.getAmpsVersion(),
            "The plugin.ampsVersion property should be set to the plugin descriptor ampsVersion."
        );
    }

    @Test
    public void pluginAmpsVersionShouldBeSetToDefaultWhenDescriptorAmpsVersionIsNull() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertEquals(
            plugin.getAmpsVersion(),
            "2.0",
            "The Plugin.ampsVersion property should default to 2.0 when it is not defined in the plugin descriptor"
        );
    }

    @Test
    public void pluginDescriptionShouldBeSetToDescriptorDescription() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setDescription("description");

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertEquals(
            plugin.getDescription(),
            pluginDescriptor.getDescription(),
            "The plugin.description property should be set to the plugin descriptor description."
        );
    }

    @Test
    public void pluginEnabledFlagShouldBeSetToTrue() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertTrue(plugin.isEnabled(), "The plugin.enabled property should be set to true.");
    }

    @Test
    public void pluginPathShouldBeSetToPathOfPluginURL() throws Exception {
        File jarFile = createPluginJARFile();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        URL pluginURL = toURL(jarFile);

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, pluginURL);

        assertEquals(
            plugin.getPath(),
            pluginURL.getPath(),
            "The plugin.path property should be set to the plugin url path."
        );
    }

    @Test
    public void pluginMtimeShouldBeSetToLastModificationTimeOfPluginJarFile() throws Exception {
        File jarFile = createPluginJARFile();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        URL pluginURL = toURL(jarFile);

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, pluginURL);

        assertEquals(
            plugin.getMtime(),
            jarFile.lastModified(),
            "The plugin.mtime property should be set to the last modification time of the plugin JAR file."
        );
    }

    @Test
    public void pluginHelpShouldBeSetToDescriptorHelp() throws Exception {
        Help help = new Help();
        help.getContent().add("help");

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setHelp(help);

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertEquals(
            plugin.getHelp(),
            pluginDescriptor.getHelp().getContent().get(0),
            "The plugin.help property should be set to the plugin descriptor help."
        );
    }

    @Test
    public void pluginMd5ShouldBeSetToMd5OfPluginJarFile() throws Exception {
        File jarFile = createPluginJARFile();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        URL pluginURL = toURL(jarFile);

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, pluginURL);

        assertEquals(
            plugin.getMd5(),
            MessageDigestGenerator.getDigestString(jarFile),
            "The plugin.md5 property should be set to the MD5 sum of the plugin JAR file."
        );
    }

    @Test
    public void pluginVersionShouldBeSetToPluginDescriptorVersion() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setVersion("2.1");

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(pluginDescriptor, getTestPluginURL());

        assertEquals(
            plugin.getVersion(),
            pluginDescriptor.getVersion(),
            "The plugin.version property should be set to the plugin descriptor version."
        );
    }

    @Test
    public void pluginVersionShouldBeSetToVersionInPluginJarManifestWhenDescriptorVersionIsNull() throws Exception {
        File jarFile = createPluginJARFile();
        URL pluginURL = toURL(jarFile);

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(new PluginDescriptor(), pluginURL);

        assertEquals(
            plugin.getVersion(),
            getVersionFromPluginJarManifest(jarFile),
            "The pliugin.version property should be set to the " + Attributes.Name.IMPLEMENTATION_VERSION +
                " attribute in the plugin JAR manifest when the plugin descriptor version is null."
        );
    }

    @Test(expectedExceptions = {PluginTransformException.class})
    public void exceptionShouldBeThrownWhenVersionNotFoundInDescriptorOrPluginJarManifest() throws Exception {
        File jarFile = createPluginJARFileWithoutVersionInManifest();
        URL pluginURL = toURL(jarFile);

        PluginTransformer transformer = new PluginTransformer();

        Plugin plugin = transformer.toPlugin(new PluginDescriptor(), pluginURL);
    }

    URL getTestPluginURL() throws Exception {
        File jarFile = createPluginJARFile();
        return toURL(jarFile);
    }

    File createPluginJARFile() throws Exception {
        URL url = getClass().getResource(".");

        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue(Attributes.Name.IMPLEMENTATION_VERSION.toString(), "2.1");

        File jarFile = new File(url.getPath(), "test-plugin.jar");
        JarOutputStream stream = new JarOutputStream(new FileOutputStream(jarFile), manifest);

        stream.flush();
        stream.close();

        return jarFile;
    }

    File createPluginJARFileWithoutVersionInManifest() throws Exception {
        URL url = getClass().getResource(".");

        Manifest manifest = new Manifest();

        File jarFile = new File(url.getPath(), "test-plugin-without-version.jar");
        JarOutputStream stream = new JarOutputStream(new FileOutputStream(jarFile), manifest);

        stream.flush();
        stream.close();

        return jarFile;
    }

    URL toURL(File file) throws Exception {
        return file.toURI().toURL();
    }

    String getVersionFromPluginJarManifest(File pluginJarFile) throws Exception {
        JarFile jarFile = new JarFile(pluginJarFile);
        Manifest manifest = jarFile.getManifest();
        Attributes attributes = manifest.getMainAttributes();
        jarFile.close();
        return attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    }

}
