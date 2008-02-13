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
package org.rhq.enterprise.server.agent;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This is the classloader used by the embedded JON Agent. This class loader is absolutely and completely isolated from
 * any other classloader in the system except for the top-most system classloader. There is no application server
 * classes or loaders accessible to this class loader.
 *
 * @author John Mazzitelli
 */
public class EmbeddedAgentClassLoader extends URLClassLoader {
    private final URL[] nativeLibraryUrls;

    public EmbeddedAgentClassLoader(URL[] classUrls, URL[] nativeUrls) {
        super(classUrls, getTopMostClassLoader());
        nativeLibraryUrls = (nativeUrls != null) ? nativeUrls : new URL[0];
    }

    protected String findLibrary(String libname) {
        String platformLibraryName = System.mapLibraryName(libname);

        // see if we can find the platform-specific library filename anywhere in our native library directories
        for (URL nativeLibraryUrl : nativeLibraryUrls) {
            try {
                // look at each file in the directory and see if it matches the platform-specific library filename
                File nativeLibraryDir = new File(nativeLibraryUrl.toURI());
                File[] files = nativeLibraryDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isDirectory() && file.getName().equals(platformLibraryName)) {
                            return file.getAbsolutePath();
                        }
                    }
                }
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Bad embedded native library URL: " + nativeLibraryUrl, e);
            }
        }

        // we couldn't find it - see if the default mechanism can (via java.library.path)
        return super.findLibrary(libname);
    }

    private static final ClassLoader getTopMostClassLoader() {
        ClassLoader top = ClassLoader.getSystemClassLoader();

        while (top.getParent() != null) {
            top = top.getParent();
        }

        return top;
    }
}