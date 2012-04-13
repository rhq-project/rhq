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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public void testNullStringParam() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine((String)null);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testEmptyStringParam() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine(" ");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testNullArrayParam() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine((String[])null);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testEmptyArrayParam() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine(new String[0]);
    }

    public void testClass() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine("java -Dshape=circle -Xmx100M -Dcolor=blue -ea -cp a.jar:b.jar:c.jar org.example.Main -x blah");
        Assert.assertEquals(javaCommandLine.getJavaExecutable(), new File("java"));
        Assert.assertNotNull(javaCommandLine.getJavaOptions());
        Set<String> javaOpts = new HashSet<String>(javaCommandLine.getJavaOptions());
        Assert.assertTrue(javaOpts.contains("-Xmx100M"));
        Assert.assertTrue(javaOpts.contains("-ea"));
        Map<String,String> sysprops = javaCommandLine.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.get("shape"), "circle");
        Assert.assertEquals(sysprops.get("color"), "blue");
        Assert.assertNotNull(javaCommandLine.getClassPath());
        Assert.assertEquals(javaCommandLine.getClassPath(), Arrays.asList("a.jar", "b.jar", "c.jar"),
                javaCommandLine.getClassPath().toString());
        Assert.assertNull(javaCommandLine.getExecutableJarFile());
        Assert.assertEquals(javaCommandLine.getMainClassName(), "org.example.Main");
        Assert.assertNotNull(javaCommandLine.getClassArguments());
        Set<String> classArgs = new HashSet<String>(javaCommandLine.getClassArguments());
        Assert.assertTrue(classArgs.contains("-x"));
        Assert.assertTrue(classArgs.contains("blah"));
    }

    public void testJarFile() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine("java -Dshape=circle -Xmx100M -Dcolor=blue -ea -classpath a.jar:b.jar:c.jar -jar main.jar -x blah");
        Assert.assertEquals(javaCommandLine.getJavaExecutable(), new File("java"));
        Assert.assertNotNull(javaCommandLine.getJavaOptions());
        Set<String> javaOpts = new HashSet<String>(javaCommandLine.getJavaOptions());
        Assert.assertTrue(javaOpts.contains("-Xmx100M"));
        Assert.assertTrue(javaOpts.contains("-ea"));
        Map<String,String> sysprops = javaCommandLine.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.get("shape"), "circle");
        Assert.assertEquals(sysprops.get("color"), "blue");
        Assert.assertNotNull(javaCommandLine.getClassPath());
        Assert.assertEquals(javaCommandLine.getClassPath(), Arrays.asList("a.jar", "b.jar", "c.jar"),
                javaCommandLine.getClassPath().toString());
        Assert.assertNull(javaCommandLine.getMainClassName());
        Assert.assertEquals(javaCommandLine.getExecutableJarFile(), new File("main.jar"));
        Assert.assertNotNull(javaCommandLine.getClassArguments());
        Set<String> classArgs = new HashSet<String>(javaCommandLine.getClassArguments());
        Assert.assertTrue(classArgs.contains("-x"));
        Assert.assertTrue(classArgs.contains("blah"));
    }

    public void testSysProps() throws Exception {
        JavaCommandLine javaCommandLine = new JavaCommandLine("java -Dprop1=foo -Dprop2= -Dprop3 org.example.Main");
        Map<String,String> sysprops = javaCommandLine.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.size(), 3);
        Assert.assertEquals(sysprops.get("prop1"), "foo");
        Assert.assertEquals(sysprops.get("prop2"), "");
        Assert.assertEquals(sysprops.get("prop3"), "");
    }

}
