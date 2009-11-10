/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.xmlschema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.LifecycleListenerType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ScheduleType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.generic.GenericPluginDescriptorType;

/**
 * Tests that we can parse server-side plugin descriptors.
 * 
 * @author John Mazzitelli
 */
@Test
public class ServerPluginDescriptorUtilTest {

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {
        deleteAllTestPluginJars();
    }

    public void testGenericPluginDescriptorInJar() throws Exception {
        File jar = createPluginJar("test-serverplugin-generic.jar", "test-serverplugin-generic.xml");
        URL url = jar.toURI().toURL();
        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
        assert descriptor != null;
        assert descriptor instanceof GenericPluginDescriptorType;
        assert descriptor.getApiVersion().equals("1.2");
        assert descriptor.getVersion().equals("2.3");
        assert descriptor.getName().equals("generic name");
        assert descriptor.getDisplayName().equals("generic display");
        assert descriptor.getDescription().equals("generic description");
        assert descriptor.getPackage().equals("generic.package");

        LifecycleListenerType lifecycleListener = descriptor.getLifecycleListener();
        ScheduleType schedule = lifecycleListener.getSchedule();
        assert lifecycleListener.getClazz().equals("generic.plugin.lifecycle.listener");
        assert schedule.getCron().equals("0 15 10 ? * MON-FRI");
        assert schedule.getPeriod() == null;

        ConfigurationDescriptor config = descriptor.getPluginConfiguration();
        assert config != null;
        assert config.getConfigurationProperty().get(0).getValue().getName().equals("prop1");

        return;
    }

    public void testAlertPluginDescriptor() throws Exception {
        String testXml = "test-serverplugin-alert.xml";
        ServerPluginDescriptorType data = parseTestXml(testXml);
        assert data instanceof AlertPluginDescriptorType;
        AlertPluginDescriptorType descriptor = (AlertPluginDescriptorType) data;

        assert descriptor.getApiVersion().equals("11.22");
        assert descriptor.getVersion().equals("100.999");
        assert descriptor.getName().equals("alert plugin name");
        assert descriptor.getDisplayName().equals("alert plugin display name");
        assert descriptor.getDescription().equals("alert plugin wotgorilla?");
        assert descriptor.getPackage().equals("org.alert.package.name.here");

        LifecycleListenerType lifecycleListener = descriptor.getLifecycleListener();
        ScheduleType schedule = lifecycleListener.getSchedule();
        assert lifecycleListener.getClazz().equals("alertPluginLifecycleListener");
        assert schedule.getCron() == null;
        assert schedule.getPeriod().longValue() == 1000L;

        ConfigurationDescriptor config = descriptor.getPluginConfiguration();
        assert config != null;
        assert config.getConfigurationProperty().get(0).getValue().getName().equals("alertprop1");
    }

    private ServerPluginDescriptorType parseTestXml(String testXml) throws Exception {
        Unmarshaller unmarshaller = ServerPluginDescriptorUtil.getServerPluginDescriptorUnmarshaller();
        URL url = this.getClass().getClassLoader().getResource(testXml);
        JAXBElement<?> ele = (JAXBElement<?>) unmarshaller.unmarshal(url);
        assert ele != null : "Invalid server plugin descriptor: " + testXml;

        Object type = ele.getValue();
        assert type instanceof ServerPluginDescriptorType : (testXml + ": invalid server plugin descriptor: " + type);
        return (ServerPluginDescriptorType) type;
    }

    private File createPluginJar(String jarName, String descriptorXmlFilename) throws Exception {
        FileOutputStream stream = null;
        JarOutputStream out = null;
        InputStream in = null;

        try {
            File pluginDir = getPluginDirectory();
            pluginDir.mkdirs();
            File jarFile = new File(pluginDir, jarName);
            stream = new FileOutputStream(jarFile);
            out = new JarOutputStream(stream);

            // Add archive entry for the descriptor
            JarEntry jarAdd = new JarEntry("META-INF/rhq-serverplugin.xml");
            jarAdd.setTime(System.currentTimeMillis());
            out.putNextEntry(jarAdd);

            // Write the descriptor - note that we assume the xml file is in the test classloader
            in = getClass().getClassLoader().getResourceAsStream(descriptorXmlFilename);
            StreamUtil.copy(in, out, false);

            return jarFile;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }

    private void deleteAllTestPluginJars() {
        File[] files = getPluginDirectory().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".jar")) {
                    file.delete();
                }
            }
        }
        return;
    }

    private File getPluginDirectory() {
        return new File(System.getProperty("java.io.tmpdir"), "xml-schemas-test");
    }
}
