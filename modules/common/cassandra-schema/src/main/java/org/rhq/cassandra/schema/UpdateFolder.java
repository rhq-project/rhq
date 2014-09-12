/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.schema;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.vfs.VirtualFile;

/**
 * @author Stefan Negrea
 */
class UpdateFolder {

    private final Log log = LogFactory.getLog(UpdateFolder.class);

    private final String folder;
    private final List<UpdateFile> updateFiles;

    public UpdateFolder(String folder) throws Exception {
        this.folder = folder;
        this.updateFiles = this.loadUpdateFiles();
    }

    public String getFolder() {
        return folder;
    }

    /**
     * Removes all the update files up to and including the provided version.
     *
     * @param currentVersion current version
     */
    public void removeAppliedUpdates(int currentVersion) {
        List<UpdateFile> updateFiles = this.getUpdateFiles();
        while (!updateFiles.isEmpty()) {
            int version = updateFiles.get(0).extractVersion();
            if (version <= currentVersion) {
                updateFiles.remove(0);
            } else {
                break;
            }
        }
    }

    /**
     * Return the list of available update files.
     *
     * @return list of update files
     */
    public List<UpdateFile> getUpdateFiles() {
        return this.updateFiles;
    }

    /**
     * The version represented by the latest/highest xml update file.
     *
     * @return the version
     */
    public int getLatestVersion() {
        if (this.updateFiles != null && this.updateFiles.size() > 0) {
            return this.updateFiles.get(this.updateFiles.size() - 1).extractVersion();
        }

        return 0;
    }

    /**
     * Loads the initial set of update files based on the input folder.
     *
     * @return list of update files
     * @throws Exception
     */
    private List<UpdateFile> loadUpdateFiles() throws Exception {
        List<UpdateFile> files = new ArrayList<UpdateFile>();
        InputStream stream = null;

        try {
            URL resourceFolderURL = this.getClass().getClassLoader().getResource(folder);

            if (resourceFolderURL.getProtocol().equals("file")) {
                stream = this.getClass().getClassLoader().getResourceAsStream(folder);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                String updateFile;
                while ((updateFile = reader.readLine()) != null) {
                    files.add(new UpdateFile(folder + "/" + updateFile));
                }
            } else if (resourceFolderURL.getProtocol().equals("jar")) {
                URL jarURL = this.getClass().getClassLoader().getResources(folder).nextElement();
                JarURLConnection jarURLCon = (JarURLConnection) (jarURL.openConnection());
                JarFile jarFile = jarURLCon.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.startsWith(folder) && !entry.equals(folder) && !entry.equals(folder + "/")) {
                        files.add(new UpdateFile(entry));
                    }
                }
            } else if (resourceFolderURL.getProtocol().equals("vfs")) {
                URLConnection conn = resourceFolderURL.openConnection();
                VirtualFile virtualFolder = (VirtualFile)conn.getContent();
                for (VirtualFile virtualChild : virtualFolder.getChildren()) {
                    if (!virtualChild.isDirectory()) {
                        files.add(new UpdateFile(virtualChild.getPathNameRelativeTo(virtualFolder.getParent())));
                    }
                }
            } else {
                // In the event we get another protocol that we do not recognize, throw an
                // exception instead of failing silently.
                throw new RuntimeException("The URL protocol [" + resourceFolderURL.getProtocol() + "] is not " +
                    "supported");
            }

            Collections.sort(files, new Comparator<UpdateFile>() {
                @Override
                public int compare(UpdateFile o1, UpdateFile o2) {
                    return o1.compareTo(o2);
                }
            });
        } catch (Exception e) {
            log.error("Error reading the list of update files.", e);
            throw e;
        } finally {
            if (stream != null) {
                try{
                    stream.close();
                } catch (Exception e) {
                    log.error("Error closing the stream with the list of update files.", e);
                    throw e;
                }
            }
        }

        return files;
    }
}
