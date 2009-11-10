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

package org.rhq.enterprise.server.plugin.pc.generic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.plugin.pc.generic.TestGenericServerPluginService.State;
import org.rhq.enterprise.server.plugin.pc.generic.TestGenericServerPluginService.TestGenericPluginManager;
import org.rhq.enterprise.server.plugin.pc.generic.TestGenericServerPluginService.TestGenericServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.generic.TestLifecycleListener.LifecycleState;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

@Test
public class GenericServerPluginTest extends AbstractEJB3Test {
    private TestGenericServerPluginService pluginService;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        this.pluginService = new TestGenericServerPluginService();
        deleteAllTestPluginJars(); // remove any old server plugins that might be still around
        prepareCustomServerPluginService(this.pluginService);
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        unprepareServerPluginService();
        deleteAllTestPluginJars(); // remove any server plugins that tests created
        this.pluginService = null;
    }

    public void testSimpleGenericPlugin() throws Exception {
        // tests that the lifecycle of the plugin container and the listener is correct
        createPluginJar("testSimpleGenericPlugin.jar", "serverplugins/simple-generic.xml");
        this.pluginService.startMasterPluginContainer();

        TestGenericServerPluginContainer pc = this.pluginService.genericPC;
        assert pc.state == State.STARTED;

        TestGenericPluginManager pm = (TestGenericPluginManager) pc.getPluginManager();
        TestLifecycleListener listener = (TestLifecycleListener) pm.listeners.values().iterator().next();
        assert listener.state == LifecycleState.STARTED;

        this.pluginService.stopMasterPluginContainer();

        assert pc.state == State.UNINITIALIZED;
        assert listener.state == LifecycleState.UNINITIALIZED;
    }

    private File createPluginJar(String jarName, String descriptorXmlFilename) throws Exception {
        FileOutputStream stream = null;
        JarOutputStream out = null;
        InputStream in = null;

        try {
            this.pluginService.masterConfig.getPluginDirectory().mkdirs();
            File jarFile = new File(this.pluginService.masterConfig.getPluginDirectory(), jarName);
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
        File[] files = this.pluginService.masterConfig.getPluginDirectory().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".jar")) {
                    file.delete();
                }
            }
        }
        return;
    }

}