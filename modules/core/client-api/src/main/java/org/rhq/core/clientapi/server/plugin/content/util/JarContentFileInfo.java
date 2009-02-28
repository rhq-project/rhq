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
package org.rhq.core.clientapi.server.plugin.content.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

/**
 * Return the version of the jar file by inspecting the Manifest.
 * The file does not necessarily need to be a jar but rather can be
 * any archive satisfying jar file structure (e.g. an .ear or .war), exploded or not.  
 *
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class JarContentFileInfo extends ContentFileInfo {

    private final Attributes mainAttributes;

    public JarContentFileInfo(File file) {
        super(file);

        Manifest manifest = getManifest();
        if (manifest != null) {
            this.mainAttributes = manifest.getMainAttributes();
        } else {
            this.mainAttributes = null;
        }
    }

    /**
     * Determines if the content file is a valid jar file (i.e. one that has a manifest).
     * Even if this returns <code>true</code>, you still are not guaranteed the jar
     * file has a non-<code>null</code> {@link #getVersion(String) version} because there may not
     * be a version attribute in the manifest.
     * 
     * @return <code>true</code> if the {@link #getContentFile() file} is a valid jar file with a manifest
     */
    public boolean isValid() {
        return null != this.mainAttributes;
    }

    /**
     * Returns the version of the Jar file (or archive satisfying Jar file structure) by inspecting the Manifest.
     * The returned version will be the following, with this preference:<br>
     * <br>
     * Specification-Version (Implementation-Version)<br>
     * Implementation-Version<br> 
     * Specification-Version<br>
     *  
     * @param defaultValue the default value
     * @return the version or the default if it cannot be determined
     */
    public String getVersion(String defaultValue) {
        String result = defaultValue;

        if (null != this.mainAttributes) {
            try {
                String specVersion = getAttributeValue(Attributes.Name.SPECIFICATION_VERSION, null);
                String implVersion = getAttributeValue(Attributes.Name.IMPLEMENTATION_VERSION, null);
                if ((null != specVersion) && (null != implVersion)) {
                    result = specVersion + " (" + implVersion + ")";
                } else {
                    if (null != implVersion) {
                        result = implVersion;
                    } else if (null != specVersion) {
                        result = specVersion;
                    }
                }
            } catch (Exception e) {
                result = defaultValue;
            }
        }
        return result;
    }

    /**
     * Return a description of the Jar file (or archive satisfying Jar file structure) by inspecting the Manifest.
     * The returned description will be the value of one of the following attributes, with this preference:<br>
     * <br>
     * Implementation-Title<br>
     * Specification-Title<br> 
     * 
     * @param defaultValue the default value
     * @return description or <code>null</code> if it cannot be determined
     */
    public String getDescription(String defaultValue) {
        String result = null;

        if (null != this.mainAttributes) {
            result = getAttributeValue(Attributes.Name.IMPLEMENTATION_TITLE, null);
            if (result == null) {
                result = getAttributeValue(Attributes.Name.SPECIFICATION_TITLE, null);
            }
        }

        return (result != null) ? result : defaultValue;
    }

    /**
     * Returns an attribute value as found in the manifest.
     * 
     * @param attributeName the attribute name
     * @param defaultValue the default if the attribute does not exist in the manifest 
     *
     * @return the attribute value or the default if it doesn't exist or cannot be retrieved
     */
    public String getAttributeValue(Name attributeName, String defaultValue) {
        String result = defaultValue;

        if (null != this.mainAttributes) {
            try {
                String val = this.mainAttributes.getValue(attributeName);
                if (null != val) {
                    result = val;
                }
            } catch (Exception e) {
                result = defaultValue;
            }
        }

        return result;
    }

    private Manifest getManifest() {
        Manifest manifest = null;
        try {
            File file = getContentFile();

            if (!file.isDirectory()) {
                JarFile jarFile = new JarFile(file);
                if (null != jarFile) {
                    manifest = jarFile.getManifest();
                }
            } else {
                File manifestFile = new File(file, "/META-INF/MANIFEST.MF");
                if (manifestFile.exists()) {
                    InputStream is = null;
                    try {
                        is = new FileInputStream(manifestFile);
                        manifest = new Manifest(is);
                    } finally {
                        if (null != is)
                            is.close();
                    }
                }
            }
        } catch (Exception ignore) {
        }

        return manifest;
    }
}
