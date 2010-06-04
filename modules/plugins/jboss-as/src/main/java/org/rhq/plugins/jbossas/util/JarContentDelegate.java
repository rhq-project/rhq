/*
 * Jopr Management Platform
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
package org.rhq.plugins.jbossas.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.util.MessageDigestGenerator;

/**
 * Discovers Jar files as artifacts including loading their manifest version into the artifact config.
 *
 * @author Greg Hinkle
 */
public class JarContentDelegate extends FileContentDelegate {
    public static final String MIME_TYPE_JAR = "application/java-archive";

    public JarContentDelegate(File directory, String typeName) {
        super(directory, ".jar", typeName);
    }

    /*
     * public InputStream getContent(ArtifactDetails artifactDetails) {   File contentFile = new File(this.directory,
     * artifactDetails.getArtifactKey());   try   {      return new BufferedInputStream(new
     * FileInputStream(contentFile));   }   catch (FileNotFoundException e)   {      throw new
     * RuntimeException("Artifact content not found for artifact " + artifactDetails, e);   } }
     *
     */
    /*
     * public void deleteContent(ArtifactDetails artifactDetails) {   File contentFile = new File(this.directory,
     * artifactDetails.getArtifactKey());
     *
     * if (!contentFile.exists())      return;
     *
     * // If the artifact is a directory, its contents need to be deleted first   if (contentFile.isDirectory())   {
     *  TomcatFileUtils.deleteDirectoryContents(contentFile.listFiles());   }
     *
     * boolean deleteResult = contentFile.delete();
     *
     * if (deleteResult==false)   {      throw new RuntimeException("Artifact content not succesfully deleted: " +
     * artifactDetails);   }
     *
     * }
     */

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages() {
        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();

        File[] files = this.directory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(getFileEnding()) && pathname.isFile();
            }
        });

        for (File file : files) {
            String manifestVersion = null;
            JarFile jf = null;
            try {
                Configuration config = new Configuration();
                jf = new JarFile(file);
                Manifest manifest = jf.getManifest();

                if (manifest != null) {
                    Attributes attributes = manifest.getMainAttributes();

                    manifestVersion = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);

                    config.put(new PropertySimple("version", manifestVersion));
                    config.put(new PropertySimple("title", attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE)));
                    config.put(new PropertySimple("url", attributes.getValue(Attributes.Name.IMPLEMENTATION_URL)));
                    config
                        .put(new PropertySimple("vendor", attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR)));

                    config.put(new PropertySimple("classpath", attributes.getValue(Attributes.Name.CLASS_PATH)));
                    config.put(new PropertySimple("sealed", attributes.getValue(Attributes.Name.SEALED)));
                }
                String sha256 = null;
                try {
                    sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(file);
                } catch (Exception e) {
                    // leave as null
                }
                String version = getVersion(manifestVersion, sha256);
                ResourcePackageDetails details = new ResourcePackageDetails(new PackageDetailsKey(file.getName(),
                    version, getPackageTypeName(), "noarch"));

                packages.add(details);
                details.setFileCreatedDate(file.lastModified()); // Why don't we have a last modified time?
                details.setFileName(file.getName());
                details.setFileSize(file.length());
                details.setClassification(MIME_TYPE_JAR);
                details.setSHA256(sha256);

                details.setExtraProperties(config);
            } catch (IOException e) {
                // If we can't open it, don't worry about it, we just won't know the version
            } finally {
                try {
                    if (jf != null)
                        jf.close();
                } catch (Exception e) {
                    // Nothing we can do here ...
                }
            }
        }

        return packages;
    }

    private String getVersion(String manifestVersion, String sha256) {
        // Version string in order of preference
        // manifestVersion + sha256, sha256, manifestVersion, "0"
        String version = "0";

        if ((null != manifestVersion) && (null != sha256)) {
            // this protects against the occasional differing binaries with poor manifest maintenance  
            version = manifestVersion + " [sha256=" + sha256 + "]";
        } else if (null != sha256) {
            version = "[sha256=" + sha256 + "]";
        } else if (null != manifestVersion) {
            version = manifestVersion;
        }

        return version;
    }

}