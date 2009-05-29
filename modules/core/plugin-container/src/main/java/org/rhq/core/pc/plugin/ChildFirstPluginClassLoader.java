/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pc.plugin;

import java.net.URL;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An alternate plugin classloader that attempts to load classes from its own jar URLs prior to attempting to load them
 * via its parent classloader. It is currently only used for the jboss-as-5 plugin.
 *
 * See also: <a href="http://jira.rhq-project.org/browse/RHQ-2059">RHQ-2059</a>.
 *
 * @author Ian Springer
 */
public class ChildFirstPluginClassLoader extends PluginClassLoader {
    private final Log log = LogFactory.getLog(this.getClass());

    public ChildFirstPluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        log.debug("*** Created child-first plugin class loader for URLs: " + Arrays.asList(urls));
    }

    protected synchronized Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class clazz = findLoadedClass(name);
        if (clazz == null) {
            if (name.matches("org\\.rhq\\.(core|enterprise)\\..*")) {
                // Let our parent classloader load PC and Agent classes.
                clazz = super.loadClass(name, resolve);
            }
            else {
                // But try to load other classes ourselves before giving our parent a shot.
                try {
                    try {
                        clazz = findClass(name);
                    }
                    catch (SecurityException se) {
                        int i = name.lastIndexOf('.');
                        String packageName = name.substring(0, i);
                        // Check if package already loaded.
                        Package pkg = getPackage(packageName);
                        if (pkg == null) {
                            definePackage(packageName, null, null, null, null, null, null, null);
                            // TODO: Do clazz = findClass(name) again?
                        }
                    }
                    catch (RuntimeException re) {
                        log.error("Failed to find class '" + name + "' - cause: " + re);
                        throw re;
                    }
                    if (resolve) {
                        resolveClass(clazz);
                    }
                }
                catch (ClassNotFoundException cnfe) {
                    clazz = super.loadClass(name, resolve);
                }
            }
        }
        return clazz;
    }

    @Override
    public URL getResource(String name) {
        URL res = findResource(name);
        if (res == null) {
            res = super.getResource(name);
        }
        return res;
    }
}
