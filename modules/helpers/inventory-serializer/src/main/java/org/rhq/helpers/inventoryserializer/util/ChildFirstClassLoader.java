/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.helpers.inventoryserializer.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ChildFirstClassLoader extends URLClassLoader {

    /**
     * @param urls
     * @param parent
     * @param factory
     */
    public ChildFirstClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    /**
     * @param urls
     * @param parent
     */
    public ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * @param urls
     */
    public ChildFirstClassLoader(URL[] urls) {
        super(urls);
    }
    
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        
        Class<?> c = findLoadedClass(name);

        //try to find the class among the child classloaders
        if (c == null) {
            try {            
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                //ignore
            }
        }
        
        if (c == null) {
            //ok, delegate to parent now
            try {
                if (getParent() != null) {
                    c = getParent().loadClass(name);
                } else {
                    c = getSystemClassLoader().loadClass(name);
                }                
            } catch (ClassNotFoundException e) {
                throw e;
            }
        }

        if (resolve) {
            resolveClass(c);
        }

        return c;
    }

    @Override
    public URL getResource(String name) {
        URL ret = findResource(name);

        if (ret == null) {
            if (getParent() != null) {
                ret = getParent().getResource(name);
            } else {
                ret = getSystemClassLoader().getResource(name);
            }
        }

        return ret;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> ret = findResources(name);

        if (ret == null || !ret.hasMoreElements()) {
            if (getParent() != null) {
                ret = getParent().getResources(name);
            } else {
                ret = getSystemClassLoader().getResources(name);
            }
        }

        return ret;
    }

}
