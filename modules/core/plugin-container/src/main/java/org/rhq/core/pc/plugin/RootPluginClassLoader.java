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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the classloader that will be the parent to all plugin classloaders. It will be created such that
 * it essentially hides a set of excluded classes which typically means this this root classloader (and its
 * children plugin classloaders) will allow the following to be loaded:
 * <ul>
 * <li>the plugin itself (including its third-party libraries)</li>
 * <li>additional jars provided to the plugin container from a ClassLoaderFacet</li>
 * <li>core plugin container libraries required by all plugins, such as plugin API classes</li>
 * <li>plugin container jars that are configured to be unhidden</li>
 * </ul>
 * 
 * @author John Mazzitelli
 */
public class RootPluginClassLoader extends URLClassLoader {
    private final Log log = LogFactory.getLog(RootPluginClassLoader.class);

    private final Pattern classesToHideRegex;

    /**
     * Creates this classloader. <code>classesToHideRegexStr</code> is a regular expression to use to match against names
     * of classes to hide (i.e. not load). If a class that is to be loaded doesn't match the regex, it will be loaded
     * using parent-first semantics (i.e. it will first be searched in the parent classloader, and only if it isn't found
     * there will this classloader be checked for it). Otherwise, the class will be loaded using this classloader
     * only - the parent classloader will not be consulted so if this classloader does not have the class to be loaded, a
     * {@link ClassCastException} will be thrown.
     * 
     * @param urls URLs to jar files where classes can be loaded by this classloader
     * @param parent the parent to this classloader, used when loading classes via parent-first semantics
     * @param classesToHideRegexStr regular expression(s) to use to match against names of classes to load.
     *                              if <code>null</code> or empty, no classes will be hidden, the parent will always
     *                              be consulted first to load the classes.
     * @throws PatternSyntaxException if the given regex is invalid (see {@link Pattern#compile(String)})
     */
    public RootPluginClassLoader(URL[] urls, ClassLoader parent, String... classesToHideRegexStr) {
        super(urls, parent);

        Pattern pattern;

        if (classesToHideRegexStr != null && classesToHideRegexStr.length > 0) {
            StringBuilder fullPattern = new StringBuilder();
            for (String regex : classesToHideRegexStr) {
                if (fullPattern.length() > 0) {
                    fullPattern.append('|');
                }
                fullPattern.append('(').append(regex).append(')');
            }
            pattern = Pattern.compile(fullPattern.toString());
        } else {
            pattern = null;
        }

        this.classesToHideRegex = pattern;

        log.debug("Root plugin classloader: regex=[" + this.classesToHideRegex + "], urls=" + Arrays.asList(urls));
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // first see if the class has already been loaded, if so, return the cached class
        Class<?> clazz = findLoadedClass(name);

        if (clazz == null) {

            // The class has not yet been loaded.
            // Check to see if the class does not match our regex; if it does not, the class is not to be "hidden" so we
            // will try to load it using parent-first semantics. This will give our parent classloader (i.e. the
            // plugin container, the agent and/or the embedding component) first dibs to load the class. If it
            // still can't be found, child classloaders will be checked.
            //
            // If the class does match the regex, the class is to be hidden, meaning if we can't load the class
            // ourself, that class is not to be loaded from any parent classloader. Only we can supply the class.

            if (this.classesToHideRegex == null || !this.classesToHideRegex.matcher(name).matches()) {
                try {
                    clazz = super.loadClass(name, resolve);
                } catch (ClassNotFoundException cnfe) {
                    if (log.isTraceEnabled()) {
                        log.trace("Root plugin classloader cannot find unhidden class: " + name);
                    }
                    throw cnfe;
                }
            } else {
                try {
                    clazz = findClass(name);
                    if (resolve) {
                        resolveClass(clazz);
                    }
                } catch (ClassNotFoundException cnfe) {
                    if (log.isTraceEnabled()) {
                        log.trace("Root plugin classloader cannot find potentially hidden class: " + name);
                    }
                    throw cnfe;
                }
            }
        }
        return clazz;
    }

    @Override
    public URL getResource(String name) {
        URL res;

        // TODO: This doesn't follow the exact "hidden" semantics used when loading classes - is this be a problem?
        // Should we convert name to a class [name.replace('/', '.').append(".class")] and try to match it?
        if (this.classesToHideRegex == null) {
            res = super.getResource(name);
        } else {
            res = findResource(name);
            if (res == null) {
                res = super.getResource(name);
            }
        }

        return res;
    }
}
