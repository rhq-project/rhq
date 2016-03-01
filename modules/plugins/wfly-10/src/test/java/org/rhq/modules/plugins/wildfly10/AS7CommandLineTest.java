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
package org.rhq.modules.plugins.wildfly10;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.stream.StreamUtil;

/**
 * A unit test for {@link AS7CommandLine}.
 */
public class AS7CommandLineTest {
    private static final File FAKE_JBOSS_HOME = new File(".");

    private static final String FILE_URL_PREFIX = (File.separatorChar == '/') ? "file://" : "file:///";

    private interface CommandLineProducer {
        AS7CommandLine get();
    }

    public static class FakeServerProcess {
        ProcessInfo processInfo;
        File fakeJBossModulesJar;

        void cleanUp() throws Exception {
            processInfo.kill("KILL");
            fakeJBossModulesJar.delete();
        }

        //this used as a main class of the process that fakes a running AS7 server
        //It just sits idle until killed (a featureful application server, indeed :) ).
        public static synchronized void main(String[] args) throws Exception {
            FakeServerProcess.class.wait();
        }
    }

    @Test
    public void testSysPropsWithAbsolutePathAndFileProtocolWithoutProcess() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties");
        File propsFile2 = File.createTempFile("jboss2-", ".properties");

        String propsFile1Path = FILE_URL_PREFIX + propsFile1;
        String propsFile2Path = FILE_URL_PREFIX + propsFile2;

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME,
                getAs7CommandLine(propsFile1Path, propsFile2Path, FAKE_JBOSS_HOME));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
        }
    }

    @Test
    public void testSysPropsWithRelativePathAndFileProtocolWithoutProcess() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties", new File("."));
        File propsFile2 = File.createTempFile("jboss2-", ".properties", new File("."));

        //create the bin subdirectory so that the path deduced by AS7CommandLine can be traversed
        new File("bin").mkdir();

        //without the process, the AS7CommandLine tries to figure out the path as relative to $JBOSS_HOME/bin
        //since we're assigning jboss home to "." and creating the file in ".", too, we need to prepend the
        //path with a ".." to jump out of the assumed "bin".
        String propsFile1Path = "file:../" + propsFile1.getName();
        String propsFile2Path = "file:../" + propsFile2.getName();

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME,
                getAs7CommandLine(propsFile1Path, propsFile2Path, FAKE_JBOSS_HOME));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            new File("bin").delete();
        }
    }

    @Test
    public void testSysPropsWithAbsolutePathWithoutProcess() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties");
        File propsFile2 = File.createTempFile("jboss2-", ".properties");

        String propsFile1Path = propsFile1.getAbsolutePath();
        String propsFile2Path = propsFile2.getAbsolutePath();

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME,
                getAs7CommandLine(propsFile1Path, propsFile2Path, FAKE_JBOSS_HOME));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
        }
    }

    @Test
    public void testSysPropsWithRelativePathWithoutProcess() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties", new File("."));
        File propsFile2 = File.createTempFile("jboss2-", ".properties", new File("."));

        //create the bin subdirectory so that the path deduced by AS7CommandLine can be traversed
        new File("bin").mkdir();

        //without the process, the AS7CommandLine tries to figure out the path as relative to $JBOSS_HOME/bin
        //since we're assigning jboss home to "." and creating the file in ".", too, we need to prepend the
        //path with a ".." to jump out of the assumed "bin".
        String propsFile1Path = "../" + propsFile1.getName();
        String propsFile2Path = "../" + propsFile2.getName();

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME,
                getAs7CommandLine(propsFile1Path, propsFile2Path, FAKE_JBOSS_HOME));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            new File("bin").delete();
        }
    }

    @Test
    public void testSysPropsWithAbsolutePathWithProcess() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties");
        File propsFile2 = File.createTempFile("jboss2-", ".properties");

        String propsFile1Path = propsFile1.getAbsolutePath();
        String propsFile2Path = propsFile2.getAbsolutePath();

        FakeServerProcess fakeServer = startFakeServer(FAKE_JBOSS_HOME, FAKE_JBOSS_HOME, propsFile1Path, propsFile2Path);

        Assert.assertNotNull(fakeServer, "Failed to start or find the fake server process.");

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME, getAs7CommandLine(fakeServer));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            fakeServer.cleanUp();
        }
    }

    @Test
    public void testSysPropsWithAbsolutePathWithProcessFromDifferentDirectory() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties");
        File propsFile2 = File.createTempFile("jboss2-", ".properties");

        String propsFile1Path = propsFile1.getAbsolutePath();
        String propsFile2Path = propsFile2.getAbsolutePath();

        FakeServerProcess fakeServer = startFakeServer(new File(".."), FAKE_JBOSS_HOME, propsFile1Path, propsFile2Path);

        Assert.assertNotNull(fakeServer, "Failed to start or find the fake server process.");

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME, getAs7CommandLine(fakeServer));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            fakeServer.cleanUp();
        }
    }

    @Test
    public void testSysPropsWithAbsolutePathAndFileProtocolWithProcess() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties");
        File propsFile2 = File.createTempFile("jboss2-", ".properties");

        String propsFile1Path = FILE_URL_PREFIX + propsFile1;
        String propsFile2Path = FILE_URL_PREFIX + propsFile2;

        FakeServerProcess fakeServer = startFakeServer(FAKE_JBOSS_HOME, FAKE_JBOSS_HOME, propsFile1Path, propsFile2Path);

        Assert.assertNotNull(fakeServer, "Failed to start or find the fake server process.");

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME, getAs7CommandLine(fakeServer));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            fakeServer.cleanUp();
        }
    }

    @Test
    public void testSysPropsWithAbsolutePathAndFileProtocolWithProcessFromDifferentDirectory() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties");
        File propsFile2 = File.createTempFile("jboss2-", ".properties");

        String propsFile1Path = FILE_URL_PREFIX + propsFile1;
        String propsFile2Path = FILE_URL_PREFIX + propsFile2;

        FakeServerProcess fakeServer = startFakeServer(new File(".."), FAKE_JBOSS_HOME, propsFile1Path, propsFile2Path);

        Assert.assertNotNull(fakeServer, "Failed to start or find the fake server process.");

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME, getAs7CommandLine(fakeServer));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            fakeServer.cleanUp();
        }
    }

    @Test
    public void testSysPropsWithRelativePathAndFileProtocolWithProcess() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties", FAKE_JBOSS_HOME);
        File propsFile2 = File.createTempFile("jboss2-", ".properties", FAKE_JBOSS_HOME);

        String propsFile1Path = "file:./" + propsFile1.getName();
        String propsFile2Path = "file:./" + propsFile2.getName();

        FakeServerProcess fakeServer = startFakeServer(FAKE_JBOSS_HOME, FAKE_JBOSS_HOME, propsFile1Path, propsFile2Path);

        Assert.assertNotNull(fakeServer, "Failed to start or find the fake server process.");

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME, getAs7CommandLine(fakeServer));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            fakeServer.cleanUp();
        }
    }

    @Test
    public void testSysPropsWithRelativePathAndFileProtocolWithProcessFromDifferentDirectory() throws Exception {
        File cwd = new File("..");
        File propsFile1 = File.createTempFile("jboss1-", ".properties", cwd);
        File propsFile2 = File.createTempFile("jboss2-", ".properties", cwd);

        String propsFile1Path = "file:./" + propsFile1.getName();
        String propsFile2Path = "file:./" + propsFile2.getName();

        FakeServerProcess fakeServer = startFakeServer(cwd, FAKE_JBOSS_HOME, propsFile1Path, propsFile2Path);

        Assert.assertNotNull(fakeServer, "Failed to start or find the fake server process.");

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME, getAs7CommandLine(fakeServer));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            fakeServer.cleanUp();
        }
    }

    @Test
    public void testSysPropsWithRelativePathWithProcess() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties", FAKE_JBOSS_HOME);
        File propsFile2 = File.createTempFile("jboss2-", ".properties", FAKE_JBOSS_HOME);

        String propsFile1Path = propsFile1.getName();
        String propsFile2Path = propsFile2.getName();

        FakeServerProcess fakeServer = startFakeServer(FAKE_JBOSS_HOME, FAKE_JBOSS_HOME, propsFile1Path, propsFile2Path);

        Assert.assertNotNull(fakeServer, "Failed to start or find the fake server process.");

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME, getAs7CommandLine(fakeServer));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            fakeServer.cleanUp();
        }
    }

    @Test
    public void testSysPropsWithRelativePathWithProcessFromDifferentDirectory() throws Exception {
        File cwd = new File("..");
        File propsFile1 = File.createTempFile("jboss1-", ".properties", cwd);
        File propsFile2 = File.createTempFile("jboss2-", ".properties", cwd);

        String propsFile1Path = propsFile1.getName();
        String propsFile2Path = propsFile2.getName();

        FakeServerProcess fakeServer = startFakeServer(cwd, FAKE_JBOSS_HOME, propsFile1Path, propsFile2Path);

        Assert.assertNotNull(fakeServer, "Failed to start or find the fake server process.");

        try {
            testSysProps(propsFile1, propsFile2, FAKE_JBOSS_HOME, getAs7CommandLine(fakeServer));
        } finally {
            propsFile1.delete();
            propsFile2.delete();
            fakeServer.cleanUp();
        }
    }

    private CommandLineProducer getAs7CommandLine(final String propsFile1Path, final String propsFile2Path,
        final File jbossHome) {
        return new CommandLineProducer() {
            @Override
            public AS7CommandLine get() {
                return new AS7CommandLine(getCommandLine(jbossHome, propsFile1Path, propsFile2Path));
            }
        };
    }

    private CommandLineProducer getAs7CommandLine(final FakeServerProcess serverProcess) {
        return new CommandLineProducer() {
            @Override
            public AS7CommandLine get() {
                return new AS7CommandLine(serverProcess.processInfo);
            }
        };
    }

    private void testSysProps(File propsFile1, File propsFile2, File jbossHome, CommandLineProducer commandLine)
        throws Exception {
        PropertiesFileUpdate propsFile1Updater = new PropertiesFileUpdate(propsFile1.getPath());
        propsFile1Updater.update("prop1", "delta");
        propsFile1Updater.update("prop4", "epsilon");

        PropertiesFileUpdate propsFile2Updater = new PropertiesFileUpdate(propsFile1.getPath());
        propsFile2Updater.update("prop2", "zeta");
        propsFile2Updater.update("prop5", "eta");

        Map<String, String> sysprops = commandLine.get().getSystemProperties();

        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.size(), 8);
        Assert.assertEquals(sysprops.get("[Standalone]"), "");
        Assert.assertEquals(sysprops.get("prop1"), "delta");
        Assert.assertEquals(sysprops.get("prop2"), "zeta");
        Assert.assertEquals(sysprops.get("prop3"), "gamma");
        Assert.assertEquals(sysprops.get("prop4"), "epsilon");
        Assert.assertEquals(sysprops.get("prop5"), "eta");
        Assert.assertEquals(sysprops.get("jboss.home.dir"), jbossHome.getAbsolutePath());
        Assert.assertEquals(sysprops.get("jboss.server.base.dir"), new File(jbossHome, "standalone").getAbsolutePath());
    }

    private FakeServerProcess startFakeServer(File cwd, File jbossHome, String propsFile1Path, String propsFile2Path)
        throws Exception {
        //prepare the fake jboss-modules.jar
        Class<?> mainClass = FakeServerProcess.class;
        File jbossModulesJar = File.createTempFile("jboss-modules-fake", ".jar");

        ShrinkWrap.create(JavaArchive.class).addClass(mainClass)
            .setManifest(new StringAsset("Main-Class: " + mainClass.getName() + "\n")).as(ZipExporter.class)
            .exportTo(jbossModulesJar, true);

        String[] commandLine = getCommandLine(jbossModulesJar, jbossHome, propsFile1Path, propsFile2Path);

        ProcessBuilder pb = new ProcessBuilder(commandLine);
        Process process = pb.directory(cwd).start();

        try {
            int exitValue = process.exitValue();
            String stdout = StreamUtil.slurp(new InputStreamReader(process.getInputStream()));
            String stderr = StreamUtil.slurp(new InputStreamReader(process.getErrorStream()));

            Assert
                .fail("The fake jboss as server process has finished even though it should keep running. The exit value was "
                    + exitValue + ". Stdout was:\n" + stdout + "\n\nStderr was:\n" + stderr);
        } catch (IllegalThreadStateException e) {
            //expected
        }

        SystemInfo sysInfo = SystemInfoFactory.createSystemInfo();
        List<ProcessInfo> processes = sysInfo.getAllProcesses();

        for (ProcessInfo pi : processes) {
            String[] cl = pi.getCommandLine();
            if (Arrays.equals(cl, commandLine)) {
                FakeServerProcess ret = new FakeServerProcess();
                ret.fakeJBossModulesJar = jbossModulesJar;
                ret.processInfo = pi;
                return ret;
            }
        }

        return null;
    }

    private String[] getCommandLine(File jbossHome, String propsFile1Path, String propsFile2Path) {
        return getCommandLine(new File(jbossHome, "jboss-modules.jar"), jbossHome, propsFile1Path, propsFile2Path);
    }

    private String[] getCommandLine(File jbossModules, File jbossHome, String propsFile1Path, String propsFile2Path) {
        return new String[] { //
        "java", //
            "-D[Standalone]", //
            "-server", //
            "-Dprop1=alpha", //
            "-Dprop2=beta", //
            "-Dprop3=gamma", //
            "-jar", jbossModules.getAbsolutePath(), //
            "-mp", new File(jbossHome, "modules").getAbsolutePath(), //
            "-jaxpmodule", "javax.xml.jaxp-provider", //
            "org.jboss.as.standalone", //
            "-Djboss.home.dir=" + jbossHome.getAbsolutePath(), //
            "-Djboss.server.base.dir=" + new File(jbossHome, "standalone").getAbsolutePath(), //
            "-P", propsFile1Path, //
            "--properties=" + propsFile2Path };
    }

    public static synchronized void main(String[] args) throws Exception {
        AS7CommandLineTest.class.wait();
    }
}
