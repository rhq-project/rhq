/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.bindings.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.LoaderClassPath;

/**
 * This class is used to create Javassist's classpools usable in RHQ on both client and server side. This only exists to
 * centralize the initialization code for the pools.
 *
 * <p> This is to ensure that Javassist can locate the classes in various classloading "schemes" - the traditional
 * application classloader used in the CLI client and the JBoss Modules classloading used on the server.
 *
 * @author Lukas Krejci
 */
public class ClassPoolFactory {

    /**
     * A simple extension of a standard Javassist's class pool that makes sure that the classes are defined using a
     * specified classloader.
     *
     * <p> The default implementation always defines a class in the current context classloader, which theoretically
     * might not be the same as the classloader that we requested the class pool for in the {@link
     * #getClassPoolForCurrentContextClassLoader()} method.
     *
     * <p>This class pool searches for classes using the provided class loader by default.
     */
    private static class ClassLoaderBoundClassPool extends ClassPool {

        private WeakReference<ClassLoader> classLoader;

        public ClassLoaderBoundClassPool(ClassLoader classLoader) {
            this.classLoader = new WeakReference<ClassLoader>(classLoader);
            this.insertClassPath(new LoaderClassPath(classLoader));
        }

        @Override
        public ClassLoader getClassLoader() {
            ClassLoader cl = classLoader.get();
            if (cl == null) {
                throw new IllegalStateException("The bound classloader has been garbage collected.");
            }
            return cl;
        }
    }

    /**
     * A cache of ClassPools, each for a classloader
     */
    private static final WeakHashMap<ClassLoader, ClassPool> CLASS_POOL_PER_CLASS_LOADER = new WeakHashMap<ClassLoader, ClassPool>();

    private ClassPoolFactory() {

    }

    /**
     * Returns a class pool that uses the provided class loader as its source for the "class path". Each class loader
     * will have exactly one class pool and the class pool will use the class loader to define the classes. This is to
     * ensure that there exists a consistent correspondence between the classes cached by the class pool with the ones
     * defined by the class loader and no linkage error can occur due to the possible attempts to define a single class
     * multiple times using a single class loader.
     *
     * <p> If the provided class loader is null, a default instance is used.
     *
     * @param classLoader the class loader to return the class pool for
     * @return
     */
    public static ClassPool getClassPool(ClassLoader classLoader) {
        synchronized (CLASS_POOL_PER_CLASS_LOADER) {
            ClassPool ret = CLASS_POOL_PER_CLASS_LOADER.get(classLoader);
            if (ret == null) {
                ret = classLoader == null ? new ClassPool() : new ClassLoaderBoundClassPool(classLoader);
                initClassPool(ret);
                CLASS_POOL_PER_CLASS_LOADER.put(classLoader, ret);
            }

            return ret;
        }
    }

    /**
     * Unlike the <code>ClassPool.getDefault()</code> method that only ever returns a single instance of the pool, this
     * factory may return different instances depending on the context classloader of the current thread. The returned
     * class pool is using the current thread context class loader to locate the classes but also is using the default
     * resource lookup of the <code>Class</code> class, in addition to just the system class path as detected by
     * Javassist (which is the only one used in the class pool returned by <code>ClassPool.getDefault()</code>).
     *
     * @return the class pool instance for the current context classloader
     * @see #getClassPool(ClassLoader)
     */
    public static ClassPool getClassPoolForCurrentContextClassLoader() {
        return getClassPool(Thread.currentThread().getContextClassLoader());
    }

    private static void initClassPool(ClassPool classPool) {
        classPool.appendClassPath(new ClassClassPath(ClassPoolFactory.class));
        classPool.appendSystemPath();
    }
}
