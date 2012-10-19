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
package org.rhq.plugins.jbossas5.util;

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
import org.rhq.core.util.file.ContentFileInfo;
import org.rhq.core.util.file.JarContentFileInfo;

/*
 * @deprecated Do not use this class anymore, even for JBoss AS5 code. This was replaced by (@link org.rhq.core.pluginapi.content.JarContentDelegate). 
 * Do not update/move/remove this class. The class is still here for backwards compatibility with previous versions of the plugin container.
 */
@Deprecated
public class JarContentDelegate extends FileContentDelegate {
    private static final String MIME_TYPE_JAR = "application/java-archive";

    private final String packageTypeName;

    @Deprecated
    public JarContentDelegate(File directory, String packageTypeName) {
        super(directory, ".jar");

        this.packageTypeName = packageTypeName;
    }

    @Deprecated
    public String getPackageTypeName() {
        return packageTypeName;
    }

    @Deprecated
    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages() {
        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();

        File[] files = this.getDirectory().listFiles(new FileFilter() {
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

                ResourcePackageDetails details = new ResourcePackageDetails(new PackageDetailsKey(file.getName(),
                    getVersion(sha256), getPackageTypeName(), "noarch"));

                packages.add(details);
                details.setFileCreatedDate(file.lastModified()); // Why don't we have a last modified time?
                details.setFileName(file.getName());
                details.setFileSize(file.length());
                details.setClassification(MIME_TYPE_JAR);
                details.setSHA256(sha256);
                details.setDisplayVersion(getDisplayVersion(file));

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

    @Deprecated
    private String getVersion(String sha256) {
        return "[sha256=" + sha256 + "]";
    }

    /**
     * Retrieve the display version for the component. The display version should be stored
     * in the manifest of the application (implementation and/or specification version).
     * It will attempt to retrieve the version for both archived or exploded deployments.
     *
     * @param file component file
     * @return
     */
    @Deprecated
    private String getDisplayVersion(File file) {
        //JarContentFileInfo extracts the version from archived and exploded deployments
        ContentFileInfo contentFileInfo = new JarContentFileInfo(file);
        return contentFileInfo.getVersion(null);
    }
}
