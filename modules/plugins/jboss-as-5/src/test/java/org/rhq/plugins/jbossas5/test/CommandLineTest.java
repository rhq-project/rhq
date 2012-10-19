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
package org.rhq.plugins.jbossas5.test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfoException;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.plugins.jbossas5.helper.JBossInstanceInfo;

/**
 * A unit test for AS5 plugin's sysprop handling.
 */
@Test
public class CommandLineTest {

    public static class TestProcessInfo extends ProcessInfo {


        @Override
        public String getCurrentWorkingDirectory() throws SystemInfoException {
            return new File(".").getAbsolutePath();
        }

        @Override
        public String[] getCommandLine() {

            File propsFile1 = null;
            File propsFile2 = null;
            File runJar = null;
            try {
                propsFile1 = File.createTempFile("jboss1-", ".properties");

                PropertiesFileUpdate propsFile1Updater = new PropertiesFileUpdate(propsFile1.getPath());
                propsFile1Updater.update("prop1", "delta");
                propsFile1Updater.update("prop4", "epsilon");

                propsFile2 = File.createTempFile("jboss2-", ".properties");
                PropertiesFileUpdate propsFile2Updater = new PropertiesFileUpdate(propsFile1.getPath());
                propsFile2Updater.update("prop2", "zeta");
                propsFile2Updater.update("prop5", "eta");
            } catch (IOException e) {
                Assert.fail();
            }

            return new String[] { //
            "/usr/java/default/bin/java", //
                "-D[Standalone]", //
                "-server", //
                "-Dprop1=alpha", //
                "-Dprop2=beta", //
                "-Dprop3=gamma", //
                "-jar", "/home/jboss/jboss-eap-5.0/jboss-modules.jar", //
                "-mp", "/home/jboss/jboss-eap-5.0/modules", //
                "-jaxpmodule", "javax.xml.jaxp-provider", //
                "org.jboss.Main", //
                "-Djboss.home.dir=" + new File("./src/test/resources/mock-jboss-home").getAbsolutePath(), //
                "-b", "127.0.0.1", //
                "-c", "production", //                
                "-P", propsFile1.toString(), //
                "--properties=" + propsFile2.toString() //
            };
        }
    }

    public void testSysProps() throws Exception {
        JBossInstanceInfo instanceInfo = new JBossInstanceInfo(new TestProcessInfo());
        Properties sysprops = instanceInfo.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.size(), 16, sysprops.toString());
        Assert.assertEquals(sysprops.get("[Standalone]"), "");
        Assert.assertEquals(sysprops.get("prop1"), "delta");
        Assert.assertEquals(sysprops.get("prop2"), "zeta");
        Assert.assertEquals(sysprops.get("prop3"), "gamma");
        Assert.assertEquals(sysprops.get("prop4"), "epsilon");
        Assert.assertEquals(sysprops.get("prop5"), "eta");
        Assert.assertEquals(sysprops.get("jboss.home.dir"),
            new File("./src/test/resources/mock-jboss-home").getAbsolutePath());
        Assert.assertEquals(sysprops.get("jboss.bind.address"), "127.0.0.1");
        Assert.assertEquals(sysprops.get("jboss.server.name"), "production");

        // the following are generated off of the other values
        Assert.assertNotNull("jboss.server.base.url");
        Assert.assertNotNull("jboss.server.base.dir");
        Assert.assertNotNull("jboss.server.home.url");
        Assert.assertNotNull("jboss.server.home.dir");
        Assert.assertNotNull("jboss.home.url");
        Assert.assertNotNull("jgroups.bind_addr");
        Assert.assertNotNull("java.rmi.server.hostname");
    }

}
