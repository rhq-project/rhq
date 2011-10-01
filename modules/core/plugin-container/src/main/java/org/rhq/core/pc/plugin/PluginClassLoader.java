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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.pluginapi.util.FileUtils;

/**
 * Classloader for the plugin jar itself and any embedded lib/* jars.
 */
public class PluginClassLoader extends URLClassLoader {
    private final Log log = LogFactory.getLog(this.getClass());

    private File embeddedJarsDirectory;
    private String stringValue;

    protected PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // this method is here simply to log success or failure so it is logging along side RootPluginClassLoader.
        // Both these logs and  RootPluginClassLoader logs helps determine where a class is being loaded from.
        try {
            Class<?> clazz = super.loadClass(name, resolve);
            if (log.isTraceEnabled()) {
                log.trace("Plugin class loaded: " + name);
            }
            return clazz;
        } catch (ClassNotFoundException cnfe) {
            if (log.isTraceEnabled()) {
                log.trace("Plugin class not found: " + name);
            }
            throw cnfe;
        }
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

    /**
     * Creates a classloader for the given named plugin whose plugin jar is found at the given URL.
     * 
     * @param pluginJarName the logical name of the plugin
     * @param pluginUrl the location where the plugin jar can be found
     * @param unpackNestedJars if <code>true</code>, any lib/*.jar files found in the plugin jar
     *                         are unpacked and put in the classloader
     * @param parent the parent classloader for the new classloader being created
     * @param tmpDirectory the directory where the unpacked nested jars are placed
     *
     * @return the new plugin classloader
     *
     * @throws PluginContainerException
     */
    public static PluginClassLoader create(String pluginJarName, URL pluginUrl, boolean unpackNestedJars,
        ClassLoader parent, File tmpDirectory) throws PluginContainerException {
        return create(pluginJarName, new URL[] { pluginUrl }, unpackNestedJars, parent, tmpDirectory);
    }

    /**
     * Creates a classloader for the given named plugin whose plugin jar is found at the URL found
     * in the first index of the given URL array. The rest of the URLs in the array are to be added
     * to the classloader as additional jars.
     * 
     * @param pluginJarName the logical name of the plugin
     * @param pluginUrls the first element is the location where the plugin jar can be found, the remaining
     *                   are additional URLs to jars that will be added to the new classloader 
     * @param unpackNestedJars if <code>true</code>, any lib/*.jar files found in the plugin jar
     *                         are unpacked and put in the classloader. The additional jars are NEVER unpacked.
     * @param parent the parent classloader for the new classloader being created
     * @param tmpDirectory the directory where the unpacked nested jars are placed
     *
     * @return the new plugin classloader
     *
     * @throws PluginContainerException
     */
    public static PluginClassLoader create(String pluginJarName, URL[] pluginUrls, boolean unpackNestedJars,
        ClassLoader parent, File tmpDirectory) throws PluginContainerException {
        List<URL> classpathUrlList = new ArrayList<URL>();
        File unpackedDirectory = null;
        boolean processedPluginJar = false; // after the first URL is processed (which is the plugin jar) this will be true

        for (URL pluginUrl : pluginUrls) {
            classpathUrlList.add(pluginUrl);

            // note that we only ever unpacked the plugin jar itself
            if (!processedPluginJar && unpackNestedJars) {
                try {
                    unpackedDirectory = unpackEmbeddedJars(pluginJarName, pluginUrl, classpathUrlList, tmpDirectory);
                } catch (Exception e) {
                    throw new PluginContainerException("Failed to unpack embedded JARs within: " + pluginUrl, e);
                }
            }

            processedPluginJar = true;
        }

        URL[] classpath = classpathUrlList.toArray(new URL[classpathUrlList.size()]);
        PluginClassLoader newLoader = new PluginClassLoader(classpath, parent);
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

                        FileOutputStream fileOutputStream;
                        try {
                            fileOutputStream = new FileOutputStream(file);
                        } catch (FileNotFoundException ex) {
                            if (file.exists() && (file.length() > 0)) {
                                // e.g. on win32, agent running w/ dll loaded PluginDumper cannot overwrite file inuse.
                                continue;
                            }
                            throw ex;
                        }
                        try {
                            BufferedOutputStream outputStream = new BufferedOutputStream(fileOutputStream);
                            try {
                                file.deleteOnExit();

                                // do NOT close this inputStream since it is buffering the ZipInputStream
                                // and we are going to still process that input stream later. We close
                                // this ZipInputStream down below in the outer most try-finally block.
                                BufferedInputStream inputStream = new BufferedInputStream(zis);

                                int count;
                                byte[] b = new byte[8192];
                                while ((count = inputStream.read(b)) > -1) {
                                    outputStream.write(b, 0, count);
                                }
                            } finally {
                                outputStream.close(); // this also closes the fileOutputStream
                            }
                        } finally {
                            fileOutputStream.close();
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
            URL[] urls = getURLs();
            String dir = "<>";
            if (this.embeddedJarsDirectory != null) {
                dir = this.embeddedJarsDirectory.toURI().toString();
            }

            StringBuilder stringBuilder = new StringBuilder(this.getClass().getSimpleName());
            stringBuilder.append('@').append(Integer.toHexString(this.hashCode())).append("[");
            stringBuilder.append("parent=").append(getParent()).append(",");
            stringBuilder.append("embedded-dir=[").append(dir).append("],");
            stringBuilder.append("urls=[");
            if (urls != null) {
                for (int i = 0; i < urls.length; i++) {
                    if (i != 0) {
                        stringBuilder.append(',');
                    }
                    if (urls[i].toString().startsWith(dir)) {
                        stringBuilder.append(new File(urls[i].getPath()).getName()); // convert to file just so we parse out only the filename
                    } else {
                        stringBuilder.append(urls[i]); // must be the plugin jar itself or an additional jar that is somewhere else
                    }
                }
            }
            stringBuilder.append("]]");
            this.stringValue = stringBuilder.toString();
        }
        return this.stringValue;
    }
}