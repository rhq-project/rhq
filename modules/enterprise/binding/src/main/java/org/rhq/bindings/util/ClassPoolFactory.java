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

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.LoaderClassPath;

/**
 * This class is used to create Javassist's classpools usable in RHQ on both client and server side. This only exists to
 * centralize the initialization code for the pools.
 *
 * <p> Unlike the <code>ClassPool.getDefault()</code> method that only ever returns a single instance of the pool, this
 * factory returns a <b>new instance</b> every time. This is because it uses a "wider" class path, looking for classes
 * using the current thread context class loader but also using the default resource lookup of the <code>Class</code>
 * class, in addition to just the system class path as detected by Javassist (which is the only one used in the class
 * pool returned by <code>ClassPool.getDefault()</code>).
 *
 * <p> This is to ensure that Javassist can locate the classes in various classloading "schemes" - the traditional
 * application classloader used in the CLI client and the JBoss Modules classloading used on the server.
 *
 * @author Lukas Krejci
 */
public class ClassPoolFactory {

    private ClassPoolFactory() {

    }

    /**
     * @return the singleton class pool instance initialized according the {@link ClassPoolFactory rules}.
     */
    public static ClassPool newInstance() {
        ClassPool pool = new ClassPool(null);
        pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        pool.appendClassPath(new ClassClassPath(ClassPoolFactory.class));
        pool.appendSystemPath();

        return pool;
    }
}
