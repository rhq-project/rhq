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

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void destroy() {

        // XXX: major hack to fix a VM bug and workaround Windows filelocking
        tryToCloseAllJarFiles();

        try {
            FileUtils.purge(embeddedJarsDirectory, true);
        } catch (IOException e) {
            log.warn("Failed to purge embedded jars directory.", e);
        }
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

                            int count = 0;
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
            } catch (Exception e) {
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

    // For why I'm trying to do this, read:
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5041014 
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4299094
    // This is a solution only for SUN VMs. This will do nothing on other VMs.
    // This was tested on SUN's Java5. If their internals change, we'll have to do different
    // things based on which version we are running on. This method never throws exceptions; it
    // will always return normally, no matter that VM we are running in.
    private static final List<Object> doNotGarbageCollectThese = new ArrayList<Object>();

    @SuppressWarnings("unchecked")
    private void tryToCloseAllJarFiles() {

        if ("true".equals(System.getProperty("rhq.agent.fix.sun.classloader.bugs", "true"))) {
            try {
                // first close the 'normal' jar files
                Class<?> clazz = URLClassLoader.class;
                java.lang.reflect.Field ucp = clazz.getDeclaredField("ucp");
                ucp.setAccessible(true);
                Object sun_misc_URLClassPath = ucp.get(this);
                java.lang.reflect.Field loaders = sun_misc_URLClassPath.getClass().getDeclaredField("loaders");
                loaders.setAccessible(true);
                Object java_util_Collection = loaders.get(sun_misc_URLClassPath);
                for (Object sun_misc_URLClassPath_JarLoader : ((java.util.Collection) java_util_Collection).toArray()) {
                    try {
                        java.lang.reflect.Field loader = sun_misc_URLClassPath_JarLoader.getClass().getDeclaredField(
                            "jar");
                        loader.setAccessible(true);
                        Object java_util_jar_JarFile = loader.get(sun_misc_URLClassPath_JarLoader);
                        ((java.util.jar.JarFile) java_util_jar_JarFile).close(); // FINALLY! CLOSE THIS TO UNLOCK THE FILE!!!
                    } catch (Throwable t) {
                        // if we got this far, this is just not a JAR loader, we can skip it
                    }
                }

                // now do native libraries
                clazz = ClassLoader.class;
                java.lang.reflect.Field nativeLibraries = clazz.getDeclaredField("nativeLibraries");
                nativeLibraries.setAccessible(true);
                java.util.Vector java_lang_ClassLoader_NativeLibrary = (java.util.Vector) nativeLibraries.get(this);
                for (Object lib : java_lang_ClassLoader_NativeLibrary) {
                    doNotGarbageCollectThese.add(lib); // call finalize twice seems to crash the VM, so keep a ref so we don't GC
                    java.lang.reflect.Method finalize = lib.getClass().getDeclaredMethod("finalize");
                    finalize.setAccessible(true);
                    finalize.invoke(lib);
                }
                if (java_lang_ClassLoader_NativeLibrary != null) {
                    java.lang.reflect.Method clear;
                    clear = java_lang_ClassLoader_NativeLibrary.getClass().getDeclaredMethod("clear");
                    clear.setAccessible(true);
                    clear.invoke(java_lang_ClassLoader_NativeLibrary);
                }
            } catch (Throwable t) {
                // probably not a SUN VM, oh, well, if on Windows, your files are now locked
            }
        }
        return;
    }
}