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
package org.rhq.enterprise.client.utility;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.URL;

/**
 * @author Greg Hinkle
 */
public class PackageFinder {



    public List<String> findPackages(String packageRoot) {
        ArrayList<String> found = new ArrayList<String>();

        String cwd = System.getProperty("user.dir");
        File libDir = new File(cwd, "lib");
        File[] jars;

        if (libDir.exists()) {
            jars = libDir.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".jar");
                }
            });
        }
        else {
            jars = loadJARsFromClassPath(packageRoot);
        }


        for (File jar : jars) {
            findPackages(packageRoot, found, jar);
        }

        return found;
    }

    private File[] loadJARsFromClassPath(String pkgRoot) {
        try {
            List<File> jarFiles = new ArrayList<File>();
            String pkgPath = pkgRoot.replaceAll("\\.", "/");
            Enumeration<URL> resources = getClass().getClassLoader().getResources(pkgPath);
            URL resource = null;

            while (resources.hasMoreElements()) {
                resource = resources.nextElement();
                if (resource.toString().startsWith("jar:file")) {
                    String jarFilePath = getJARFilePath(resource);
                    jarFiles.add(new File(jarFilePath));
                }
            }

            return jarFiles.toArray(new File[] {});
        }
        catch (IOException e) {
            e.printStackTrace();
            return new File[] {};
        }

    }

    private String getJARFilePath(URL resource) {
        int startIndex = "jar:file:".length();
        String string = resource.toString().substring(startIndex);
        int endIndex = string.indexOf("!");

        return string.substring(0, endIndex);
    }

    private void findPackages(String packageRoot, List<String> list, File jar) {

        Set<String> paths = new HashSet<String>();
        JarFile jf = null;
        try {
            jf = new JarFile(jar);

            String packagePath = packageRoot.replaceAll("\\.", "/");

            Enumeration<JarEntry> entries = jf.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().startsWith(packagePath)) {
                    String match = entry.getName().substring(0, entry.getName().lastIndexOf("/"));
                    paths.add(match.replaceAll("/", "\\."));
                }
            }
            list.addAll(paths);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try {
                if (jf != null) {
                    jf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }





    public static void main(String[] args) {
        new PackageFinder().findPackages("org.rhq.core.domain");
    }

}
