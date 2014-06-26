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
package org.rhq.enterprise.client;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Utility that can be used to determine the version of the software.
 *
 * @author John Mazzitelli
 */
public class Version {
    /**
     * Version property whose value is the product name.
     */
    public static final String PROP_PRODUCT_NAME = "Implementation-Title";

    /**
     * Version property whose value is the product version.
     */
    public static final String PROP_PRODUCT_VERSION = "Implementation-Version";

    /**
     * Version property whose value is the source code revision number used to make the build.
     */
    public static final String PROP_BUILD_NUMBER = "Build-Number";

    /**
     * Version property whose value is the date when this version of the product was built.
     */
    public static final String PROP_BUILD_DATE = "Build-Time";

    /**
     * Version property whose value is the vendor of the JDK that built this version of the product.
     */
    public static final String PROP_BUILD_JDK_VENDOR = "Java-Vendor";

    /**
     * Version property whose value is the version of the JDK that built this version of the product.
     */
    public static final String PROP_BUILD_JDK_VERSION = "Java-Version";

    /**
     * Version property whose value identifies the build machine's operating system.
     */
    public static final String PROP_BUILD_OS_NAME = "Os-Name";

    /**
     * Version property whose value is the build machine's operating system version.
     */
    public static final String PROP_BUILD_OS_VERSION = "Os-Version";

    /**
     * Caches the version properties so we don't have to keep reading the file.
     * These properties should never change during the lifetime of the agent, so
     * we can cache these forever in memory.
     */
    private static Properties propertiesCache = null;

    /**
     * A main method that can be used to determine the version information from a command line.
     *
     * @param args the version properties to print to stdout; if no arguments are given then all version properties are
     *             printed
     */
    public static void main(String[] args) {
        System.out.println("==========");

        if (args.length == 0) {
            System.out.println(getVersionPropertiesAsString());
        } else {
            Properties props = getVersionProperties();
            for (int i = 0; i < args.length; i++) {
                String key = args[i];
                String value = props.getProperty(key);

                if (value == null) {
                    value = "<unknown>";
                }

                System.out.println(key + "=" + value);
            }
        }

        System.out.println("==========");
    }

    /**
     * Returns the product name and its version string.
     *
     * @return "productName Version"
     */
    public static String getProductNameAndVersion() {
        Properties props = getVersionProperties();
        String name = props.getProperty(PROP_PRODUCT_NAME);
        String version = props.getProperty(PROP_PRODUCT_VERSION);
        //Conditionally check for and apply update/patch version details
        String updatePortion = getUpdateVersion();
        if (updatePortion == null) {
            updatePortion = "";
        }

        //Ex. GA[RHQ Enterprise Remote CLI 4.9.0.JON320GA] or Update 01[RHQ Enterprise Remote CLI 4.9.0.JON320GA Update 01]
        return name + ' ' + version + (updatePortion.trim().length() == 0 ? "" : " " + updatePortion);
    }

    /**
     * Returns the product name and its version string, with more build details about the version.
     *
     * @return "productName Version"
     */
    public static String getProductNameAndVersionBuildInfo() {
        Properties props = getVersionProperties();
        String name = props.getProperty(PROP_PRODUCT_NAME);
        String version = props.getProperty(PROP_PRODUCT_VERSION);
        String buildNum = props.getProperty(PROP_BUILD_NUMBER);
        String buildDate = props.getProperty(PROP_BUILD_DATE);

        //Conditionally check for and apply update/patch version details
        String updatePortion = getUpdateVersion();
        if (updatePortion == null) {
            updatePortion = "";
        }

        //Ex. GA[RHQ Enterprise Remote CLI 4.9.0.JON320GA (dcb8b6f:734bd56)] or Update 01[RHQ Enterprise Remote CLI 4.9.0.JON320GA Update 01 (dcb8b6f:734bd56)]
        return name + ' ' + version + (updatePortion.trim().length() == 0 ? "" : " " + updatePortion) + " (" + buildNum
            + ") " + buildDate;
    }

    /**
     * Returns just the product name.
     *
     * @return product name
     */
    public static String getProductName() {
        Properties props = getVersionProperties();
        String name = props.getProperty(PROP_PRODUCT_NAME);
        return name;
    }

    /**
     * Returns just the product version.
     *
     * @return product version
     */
    public static String getProductVersion() {
        Properties props = getVersionProperties();
        String version = props.getProperty(PROP_PRODUCT_VERSION);
        return version;
    }

    /**
     * Returns just the product build date.
     *
     * @return product build date
     */
    public static String getBuildDate() {
        Properties props = getVersionProperties();
        String build_date = props.getProperty(PROP_BUILD_DATE);
        return build_date;
    }

    /**
     * Returns just the product build number.
     *
     * @return product build number
     */
    public static String getBuildNumber() {
        Properties props = getVersionProperties();
        String build_num = props.getProperty(PROP_BUILD_NUMBER);
        return build_num;
    }

    /**
     * Returns a set of properties that can be used to identify this version of the product.
     *
     * @return properties identifying this version
     *
     * @throws RuntimeException if there is no version info found in the current thread's class loader
     */
    public static Properties getVersionProperties() {
        if (propertiesCache == null) {
            Manifest manifest;
            try {
                URL jarUrl = Version.class.getProtectionDomain().getCodeSource().getLocation();
                JarFile jarFile = new JarFile(new File(jarUrl.toURI()));
                try {
                    manifest = jarFile.getManifest();
                } finally {
                    jarFile.close();
                }
            } catch (Exception e) {
                return new Properties();
            }

            Attributes mainAttributes = manifest.getMainAttributes();
            Properties newProps = new Properties();
            for (Map.Entry<Object, Object> entry : mainAttributes.entrySet()) {
                String name = entry.getKey().toString();
                String value = entry.getValue().toString();
                newProps.setProperty(name, value);
            }
            propertiesCache = newProps;
        }

        Properties retProps = new Properties();
        retProps.putAll(propertiesCache);
        return retProps;
    }

    /**
     * Returns the version properties in a single string with all properties on a single line separated with a newline.
     *
     * @return the version properties in one big string
     */
    public static String getVersionPropertiesAsString() {
        Properties props = getVersionProperties();
        StringBuilder str = new StringBuilder();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            str.append(entry.getKey().toString() + '=' + entry.getValue().toString() + '\n');
        }
        return str.toString();
    }

    // Update property which records update/patch version: Ex. update-1, cp1, etc.
    public static String getUpdateVersion() {
        return "";
    }
}