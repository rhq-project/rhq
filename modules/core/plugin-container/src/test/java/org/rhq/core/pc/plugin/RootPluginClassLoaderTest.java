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

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;

import javax.management.ObjectName;
import javax.naming.InitialContext;

import org.testng.annotations.Test;

import org.rhq.core.pluginapi.measurement.MeasurementFacet;

@Test
public class RootPluginClassLoaderTest {
    // this regex will match the core classes (classloader to exclude core classes)
    private static final String CORE_REGEX = "org\\.rhq\\.(core|enterprise)\\..*";

    // this regex is designed to not match any class (classloader to load everything)
    private static final String NONE_REGEX = "this-will-match-nothing";

    // this regex is designed to match all classes except for java.* classes (classloader to exclude everything but J2SE classes)
    private static final String ALL_REGEX = "(?!java).*";

    // this regex will match JMX classes (classloader to excluded JMX classes)
    private static final String JMX_REGEX = "javax\\.management\\..*";

    // this regex will match javax.* classes (classloader to excluded only Java extension classes)
    private static final String JAVAX_REGEX = "javax\\..*";

    private ClassLoader parentClassLoader = this.getClass().getClassLoader();

    public void testChildFirst() throws Exception {
        // this will exclude all classes in the parent classloaders
        ClassLoader cl = new RootPluginClassLoader(new URL[] {}, parentClassLoader, ALL_REGEX);
        try {
            Class.forName("test.DummyObject", true, cl);
            assert false : "Failed sanity check! How did the dummy object from the test jar get into our classloader?";
        } catch (ClassNotFoundException ok) {
        }

        // we have pre-built a test jar with a test.DummyObject in it. It has this constant defined in it:
        // public static final String DUMMY_CONSTANT = "dummy constant value";
        // It also has a no-arg constructor and a "String getValue()" method that always returns a non-null string.

        URL testJar = new File("target/test-classes/classloader-test.jar").toURI().toURL();
        cl = new RootPluginClassLoader(new URL[] { testJar }, parentClassLoader, ALL_REGEX);
        Class<?> clazz = Class.forName("test.DummyObject", true, cl);
        Field field = clazz.getDeclaredField("DUMMY_CONSTANT");
        assert "dummy constant value".equals(field.get(null)) : "Failed to obtain the constant value";
        Object dummyObject = clazz.newInstance();
        assert clazz.getMethod("getValue").invoke(dummyObject) != null : "Cannot invoke method on loaded class";
    }

    public void testHiddenClasses() throws Exception {
        // this will hide all core plugin classes
        ClassLoader cl = new RootPluginClassLoader(new URL[] {}, parentClassLoader, CORE_REGEX);
        assert Class.forName(String.class.getName(), true, cl) != null : "Should always be able to get J2SE classes";

        try {
            Class.forName(MeasurementFacet.class.getName(), true, cl);
            assert false : "The class should have been hidden!";
        } catch (ClassNotFoundException ok) {
        }
    }

    public void testHideJavaLang() throws Exception {
        // just proves that we can even hide J2SE classes
        ClassLoader cl = new RootPluginClassLoader(new URL[] {}, parentClassLoader, ".*");
        try {
            Class.forName(String.class.getName(), true, cl);
            assert false : "The class should have been hidden!";
        } catch (ClassNotFoundException ok) {
        }
        try {
            Class.forName(Object.class.getName(), true, cl);
            assert false : "The class should have been hidden!";
        } catch (ClassNotFoundException ok) {
        }
    }

    public void testUnhiddenClasses() throws Exception {
        ClassLoader cl = new RootPluginClassLoader(new URL[] {}, parentClassLoader, NONE_REGEX);
        assert Class.forName(String.class.getName(), true, cl) != null : "Should always be able to get J2SE classes";
        assert Class.forName(MeasurementFacet.class.getName(), true, cl) != null;

        cl = new RootPluginClassLoader(new URL[] {}, parentClassLoader, (String[]) null); // null regex means do not hide anything
        assert Class.forName(String.class.getName(), true, cl) != null : "Should always be able to get J2SE classes";
        assert Class.forName(MeasurementFacet.class.getName(), true, cl) != null;
    }

    public void testJMXClasses() throws Exception {
        ClassLoader cl = new RootPluginClassLoader(new URL[] {}, parentClassLoader, (String[]) null); // null regex means do not hide anything
        assert Class.forName(String.class.getName(), true, cl) != null : "Should always be able to get J2SE classes";
        assert Class.forName(InitialContext.class.getName(), true, cl) != null;
        assert Class.forName(ObjectName.class.getName(), true, cl) != null;

        cl = new RootPluginClassLoader(new URL[] {}, parentClassLoader, JMX_REGEX);
        assert Class.forName(String.class.getName(), true, cl) != null : "Should always be able to get J2SE classes";
        assert Class.forName(InitialContext.class.getName(), true, cl) != null;
        try {
            Class.forName(ObjectName.class.getName(), true, cl);
            assert false : "The class should have been hidden!";
        } catch (ClassNotFoundException ok) {
        }

        cl = new RootPluginClassLoader(new URL[] {}, parentClassLoader, JAVAX_REGEX);
        assert Class.forName(String.class.getName(), true, cl) != null : "Should always be able to get J2SE classes";
        try {
            Class.forName(ObjectName.class.getName(), true, cl);
            assert false : "The JMX class should have been hidden!";
        } catch (ClassNotFoundException ok) {
        }
        try {
            Class.forName(InitialContext.class.getName(), true, cl);
            assert false : "The class should have been hidden!";
        } catch (ClassNotFoundException ok) {
        }
    }
}
