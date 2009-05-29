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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.pc.PluginContainer;

/**
 * Classloader for the plugin jar itself and any embedded lib/* jars.
 */
public class PluginClassLoader extends URLClassLoader {
    private final Log log = LogFactory.getLog(this.getClass());

    private File embeddedJarsDirectory;
    private String stringValue;

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void destroy() {
        try {
            FileUtils.purge(embeddedJarsDirectory, true);
        } catch (IOException e) {
            log.warn("Failed to purge embedded jars directory. Cause: " + e);
        }

        // help GC
        LogFactory.release(this);
    }

    public static PluginClassLoader create(String pluginJarName, URL pluginUrl, boolean unpackNestedJars,
        ClassLoader parent, File tmpDirectory) throws PluginContainerException {
        return create(pluginJarName, new URL[] { pluginUrl }, unpackNestedJars, parent, tmpDirectory);
    }

    public static PluginClassLoader create(String pluginJarName, URL[] pluginUrls, boolean unpackNestedJars,
        ClassLoader parent, File tmpDirectory) throws PluginContainerException {
        List<URL> classpathUrlList = new ArrayList<URL>();
        File unpackedDirectory = null;

        for (URL pluginUrl : pluginUrls) {
            classpathUrlList.add(pluginUrl);

            if (unpackNestedJars) {
                try {
                    unpackedDirectory = unpackEmbeddedJars(pluginJarName, pluginUrl, classpathUrlList, tmpDirectory);
                } catch (Exception e) {
                    throw new PluginContainerException("Failed to unpack embedded JARs within: " + pluginUrl, e);
                }
            }
        }

        URL[] classpath = classpathUrlList.toArray(new URL[classpathUrlList.size()]);

        PluginClassLoader newLoader;
        // TODO (ips): Remove this temporary hack once http://jira.rhq-project.org/browse/RHQ-2059 has been
        //             implemented.
        if (PluginContainer.getInstance().isInsideAgent() && pluginJarName.matches("jopr-jboss-as-5-plugin-.+\\.jar")) {
            // Use a child first plugin classloader for the as5 plugin but only if we're running in the enterprise Agent.
            // Don't use it in embedded - it's not necessary since there are no conflicting Agent jars (jboss-remoting
            // etc.), and furthermore, it breaks things. For more info on this temporary hack, see
            // http://jira.rhq-project.org/browse/RHQ-2059.
            newLoader = new ChildFirstPluginClassLoader(classpath, parent);
        } else {
            newLoader = new PluginClassLoader(classpath, parent);
        }
        newLoader.embeddedJarsDirectory = unpackedDirectory;

        return newLoader;
    }

    /**
     * Unpacks all lib/* resources into a temporary directory, adds URLs to those newly extracted resources and returns
     * the directory where the jars were extracted. This will actually create a unique subdirectory under the given
     * <code>tmpDirectory</code>) which is where the extracted resources will be placed. If the give <code>
     * tmpDirectory</code> is <code>null</code>, the standard platform's tmp directory will be used.
     *
     * @param  pluginJarName name of the main plugin jar, used as part of the name to the tmp directory
     * @param  pluginUrl     the URL to the main plugin jar we are unpacking
     * @param  urls          the URLs to the tmp directory resources that were unpacked
     * @param  tmpDirectory  the parent directory that will contain the child directory which will contain all extracted
     *                       resources
     *
     * @return the location where all the extract files are now located
     *
     * @throws IOException       If any IO goes wrong
     */
    private static File unpackEmbeddedJars(String pluginJarName, URL pluginUrl, List<URL> urls, File tmpDirectory)
        throws IOException {
        InputStream pluginStream = pluginUrl.openStream();
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(pluginStream));
        ZipEntry entry;
        File extractionDirectory = null; // this is where we will actually store the files we extract

        try {
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Only care about entries in the lib directory
                if (entryName.startsWith("lib") && (entryName.length() > 4)) {
                    if (extractionDirectory == null) {
                        extractionDirectory = createTempDirectory(tmpDirectory, pluginJarName);
                    }

                    int i = entryName.lastIndexOf('/');
                    if (i < 0) {
                        i = entryName.lastIndexOf('\\');
                    }

                    String s = entryName.substring(i + 1);

                    File file = null;
                    try {
                        if (s.endsWith(".jar")) {
                            file = File.createTempFile(s, null, extractionDirectory);
                            urls.add(file.toURI().toURL());
                        } else {
                            // All non-jar files are extracted as-is with the
                            // same filename.
                            file = new File(extractionDirectory, s);

                            // since we have a regular file, we need to make sure the tmp dir is in classpath so it can be found
                            URL tmpUrl = extractionDirectory.toURI().toURL();
                            if (!urls.contains(tmpUrl)) {
                                urls.add(tmpUrl);
                            }
                        }

                        BufferedOutputStream outputStream;
                        try {
                            outputStream = new BufferedOutputStream(new FileOutputStream(file));
                        } catch (FileNotFoundException ex) {
                            if (file.exists() && (file.length() > 0)) {
                                // e.g. on win32, agent running w/ dll loaded PluginDumper cannot overwrite file inuse.
                                continue;
                            }

                            throw ex;
                        }

                        try {
                            file.deleteOnExit();

                            BufferedInputStream inputStream = new BufferedInputStream(zis);

                            int count;
                            byte[] b = new byte[8192];
                            while ((count = inputStream.read(b)) > -1) {
                                outputStream.write(b, 0, count);
                            }
                        } finally {
                            outputStream.flush();
                            outputStream.close();
                        }
                    } catch (IOException ioe) {
                        if (file != null) {
                            file.delete();
                        }

                        throw ioe;
                    }
                }
            }
        } finally {
            try {
                zis.close();
            } catch (Exception ignored) {
            }
        }

        return extractionDirectory;
    }

    private static File createTempDirectory(File tmpDirectory, String pluginName) throws IOException {
        // Let's reuse the algorithm the JDK uses to determine a unique name:
        // 1) create a temp file to get a unique name using JDK createTempFile
        // 2) then quickly delete the file and...
        // 3) convert it to a directory

        File tmpDir = File.createTempFile(pluginName, ".classloader", tmpDirectory); // create file with unique name
        boolean deleteOk = tmpDir.delete(); // delete the tmp file and...
        boolean mkdirsOk = tmpDir.mkdirs(); // ...convert it to a directory

        if (!deleteOk || !mkdirsOk) {
            throw new IOException("Failed to create temp classloader directory named [" + tmpDir + "]");
        }

        tmpDir.deleteOnExit();

        return tmpDir;
    }

    @Override
    public String toString() {
        if (this.stringValue == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.getClass().getSimpleName()).append("[");
            stringBuilder.append("parent=").append(getParent()).append(", ");
            stringBuilder.append("urls=").append(Arrays.asList(getURLs())).append("]");
            this.stringValue = stringBuilder.toString();
        }
        return this.stringValue;
    }
}