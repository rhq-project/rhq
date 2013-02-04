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
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A unit test for {@link JavaCommandLine}.
 *
 * @author Ian Springer
 */
@Test
public class JavaCommandLineTest {

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testNullArrayParam() throws Exception {
        new JavaCommandLine((String[])null);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testEmptyArrayParam() throws Exception {
        new JavaCommandLine(new String[0]);
    }

    public void testClass() throws Exception {
        String cp = "a.jar" + File.pathSeparator + "b.jar" + File.pathSeparator + "c.jar";
        JavaCommandLine javaCommandLine = new JavaCommandLine("java", "-Dshape=circle", "-Xmx100M", "-Dcolor=blue",
            "-ea", "-cp", cp, "org.example.Main", "-x", "blah");

        Assert.assertEquals(javaCommandLine.getJavaExecutable(), new File("java"));
        Assert.assertEquals(javaCommandLine.getJavaOptions(), Arrays.asList("-Dshape=circle", "-Xmx100M", "-Dcolor=blue", "-ea"),
                        javaCommandLine.getJavaOptions().toString());

        Map<String,String> sysprops = javaCommandLine.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.get("shape"), "circle");
        Assert.assertEquals(sysprops.get("color"), "blue");

        Assert.assertEquals(javaCommandLine.getClassPath(), Arrays.asList("a.jar", "b.jar", "c.jar"),
                javaCommandLine.getClassPath().toString());

        Assert.assertNull(javaCommandLine.getExecutableJarFile());
        Assert.assertEquals(javaCommandLine.getMainClassName(), "org.example.Main");
        Assert.assertEquals(javaCommandLine.getClassArguments(), Arrays.asList("-x", "blah"),
                javaCommandLine.getClassArguments().toString());
    }

    public void testJarFile() throws Exception {
        String cp = "a.jar" + File.pathSeparator + "b.jar" + File.pathSeparator + "c.jar";
        JavaCommandLine javaCommandLine = new JavaCommandLine("java", "-Dshape=circle", "-Xmx100M", "-Dcolor=blue",
            "-ea", "-cp", cp, "-jar", "main.jar", "-x", "blah");

        Assert.assertEquals(javaCommandLine.getJavaExecutable(), new File("java"));
        Assert.assertEquals(javaCommandLine.getJavaOptions(), Arrays.asList("-Dshape=circle", "-Xmx100M", "-Dcolor=blue", "-ea"),
                        javaCommandLine.getJavaOptions().toString());

        Map<String,String> sysprops = javaCommandLine.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.get("shape"), "circle");
        Assert.assertEquals(sysprops.get("color"), "blue");

        Assert.assertEquals(javaCommandLine.getClassPath(), Arrays.asList("a.jar", "b.jar", "c.jar"),
                javaCommandLine.getClassPath().toString());

        Assert.assertNull(javaCommandLine.getMainClassName());
        Assert.assertEquals(javaCommandLine.getExecutableJarFile(), new File("main.jar"));
        Assert.assertEquals(javaCommandLine.getClassArguments(), Arrays.asList("-x", "blah"),
                javaCommandLine.getClassArguments().toString());
    }

    public void testSysProps() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine("java", "-Dprop1=foo", "-Dprop2=", "-Dprop3", "org.example.Main", "-Dprop4=boo");
        Map<String,String> sysprops = javaCommandLine.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.size(), 3);
        Assert.assertEquals(sysprops.get("prop1"), "foo");
        Assert.assertEquals(sysprops.get("prop2"), "");
        Assert.assertEquals(sysprops.get("prop3"), "");
    }

    public void testCombinedSysProps() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine(new String[] {"java", "-Dprop1=foo", "-Dprop2=", "-Dprop3", "org.example.Main", "-Dprop4=boo"}, true);
        Map<String,String> sysprops = javaCommandLine.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.size(), 4);
        Assert.assertEquals(sysprops.get("prop1"), "foo");
        Assert.assertEquals(sysprops.get("prop2"), "");
        Assert.assertEquals(sysprops.get("prop3"), "");
        Assert.assertEquals(sysprops.get("prop4"), "boo");
    }

    public void testGetPresentClassOption() throws Exception {
        String cp = "a.jar" + File.pathSeparator + "b.jar" + File.pathSeparator + "c.jar";
        JavaCommandLine javaCommandLine = new JavaCommandLine("java", "-Dshape=circle", "-Xmx100M", "-Dcolor=blue",
            "-ea", "-cp", cp, "org.example.Main", "-x", "blah", "--novaluelong", "--long=longval", "-n");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption("x", null, false)), "");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption("x", null, true)), "blah");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption(null, "novaluelong", false)), "");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption(null, "novaluelong", true)), "");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption("l", "long", false)), "longval");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption("l", "long", true)), "longval");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption("n", null, false)), "");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption("n", null, true)), "");

        javaCommandLine = new JavaCommandLine("java", "-Dshape=circle", "-Xmx100M", "-Dcolor=blue",
            "-ea", "-cp", cp, "org.example.Main", "-x", "blah", "--novaluelong", "-l", "longval", "-n");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption("l", "long", false)), "");
        Assert.assertEquals(javaCommandLine.getClassOption(new CommandLineOption("l", "long", true)), "longval");
    }

    public void testGetAbsentClassOption() throws Exception {
        String cp = "a.jar" + File.pathSeparator + "b.jar" + File.pathSeparator + "c.jar";
        JavaCommandLine javaCommandLine = new JavaCommandLine("java", "-Dshape=circle", "-Xmx100M", "-Dcolor=blue",
            "-ea", "-cp", cp, "org.example.Main", "-x", "blah", "--novaluelong", "--long=longval", "-n");
        Assert.assertNull(javaCommandLine.getClassOption(new CommandLineOption("b", null, false)));
        Assert.assertNull(javaCommandLine.getClassOption(new CommandLineOption("b", null, true)));
        Assert.assertNull(javaCommandLine.getClassOption(new CommandLineOption(null, "bogus", false)));
        Assert.assertNull(javaCommandLine.getClassOption(new CommandLineOption(null, "bogus", true)));
        Assert.assertNull(javaCommandLine.getClassOption(new CommandLineOption("b", "bogus", false)));
        Assert.assertNull(javaCommandLine.getClassOption(new CommandLineOption("b", "bogus", true)));
    }

    public void testGetClassOptionStopProcessing() throws Exception {
        String cp = "a.jar" + File.pathSeparator + "b.jar" + File.pathSeparator + "c.jar";
        JavaCommandLine javaCommandLine = new JavaCommandLine("java", "-Dshape=circle", "-Xmx100M", "-Dcolor=blue",
            "-ea", "-cp", cp, "org.example.Main", "-x", "blah", "--novaluelong", "--long=longval", "--", "-n");
        Assert.assertNull(javaCommandLine.getClassOption(new CommandLineOption("b", "bogus")));
        Assert.assertNull(javaCommandLine.getClassOption(new CommandLineOption("n", null, false)));
    }

    public void testToString() throws Exception {
        String cp = "a.jar" + File.pathSeparator + "b.jar" + File.pathSeparator + "c.jar";
        JavaCommandLine javaCommandLine = new JavaCommandLine("java", "-Dshape=circle", "-Xmx100M", "-Dcolor=blue",
            "-ea", "-cp", cp, "org.example.Main", "-x", "blah", "--novaluelong", "--long=longval", "-n");
        Assert.assertNotNull(javaCommandLine.toString());
    }

}
