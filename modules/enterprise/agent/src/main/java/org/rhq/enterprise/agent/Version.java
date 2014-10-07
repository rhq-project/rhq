/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.agent;

import java.io.InputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

/**
 * Utility that can be used to determine the version of the software.
 *
 * @author John Mazzitelli
 */
public class Version {
    /**
     * Version property whose value is the product name.
     */
    public static final String PROP_PRODUCT_NAME = "Product-Name";

    /**
     * Version property whose value is the product version.
     */
    public static final String PROP_PRODUCT_VERSION = "Product-Version";

    /**
     * Version property whose value is the source code revision number used to make the build.
     */
    public static final String PROP_BUILD_NUMBER = "Build-Number";

    /**
     * Version property whose value is the date when this version of the product was built.
     */
    public static final String PROP_BUILD_DATE = "Build-Date";

    /**
     * Version property whose value is the vendor of the JDK that built this version of the product.
     */
    public static final String PROP_BUILD_JDK_VENDOR = "Build-Jdk-Vendor";

    /**
     * Version property whose value is the version of the JDK that built this version of the product.
     */
    public static final String PROP_BUILD_JDK_VERSION = "Build-Jdk";

    /**
     * Version property whose value identifies the build machine's operating system.
     */
    public static final String PROP_BUILD_OS_NAME = "Build-OS-Name";

    /**
     * Version property whose value is the build machine's operating system version.
     */
    public static final String PROP_BUILD_OS_VERSION = "Build-OS-Version";

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
                } else if (PROP_BUILD_DATE.equals(key)) {
                    Date date = getVersionPropertyAsDate(value);
                    if (date != null) {
                        value = date.toString();
                    }
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

        //Ex. GA[RHQ 4.9.0.JON320GA [734bd56] or Update 02[RHQ 4.9.0.JON320GA Update 02 [734bd56]]
        if (updatePortion.trim().length() == 0){
             return "" + name + " " + version;
        } else {
          String[] versionElements = version.split(" ");
          if (versionElements.length==2){
                return "" + name + " " + versionElements[0] + " " + updatePortion + " " + versionElements[1];
          }else{
                return "" + name + " " + version + " " + updatePortion;
          }
        }
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
     * @return product build date, or <code>null</code> if the date could not be determined due to an invalid date
     *         format
     */
    public static Date getBuildDate() {
        Properties props = getVersionProperties();
        String build_date = props.getProperty(PROP_BUILD_DATE);
        Date date = getVersionPropertyAsDate(build_date);
        return date;
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
     * @throws RuntimeException if there is no VERSION file found in the current thread's class loader
     */
    public static Properties getVersionProperties() {
        if (propertiesCache == null) {
            ClassLoader cl = Version.class.getClassLoader();
            InputStream stream = cl.getResourceAsStream("rhq-agent-version.properties");

            Properties newProps = new Properties();

            try {
                try {
                    newProps.load(stream);
                } finally {
                    stream.close();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
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
        Date date = getBuildDate();

        if (date != null) {
            props.setProperty(PROP_BUILD_DATE, date.toString()); // just puts date in a more appropriate form
        }

        String updateVersion = getUpdateVersion();
        if ((updateVersion != null) && (!updateVersion.trim().isEmpty())) {
            String current = props.getProperty("Module-Version");
            props.setProperty("Module-Version", current + " " + updateVersion); // just postPends Ex. Update 02
        }

        return StringUtil.justifyKeyValueStrings(props);
    }

    /**
     * When a version property represents a date, this method will take that date string and convert it to a <code>
     * java.util.Date</code> object.
     *
     * <p>Assumes the date string is in the en.US locale based form of <code>dd.MMM.yyyy HH.mm.ss z</code></p>
     *
     * @param  date_string the version property date string to convert
     *
     * @return the converted <code>dataString</code> as a <code>java.util.Date</code> object or <code>null</code> if the
     *         date string was in an invalid format
     */
    public static Date getVersionPropertyAsDate(String date_string) {
        Date ret_date = null;

        if (date_string != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd.MMM.yyyy HH.mm.ss z", Locale.US);
            ret_date = format.parse(date_string, new ParsePosition(0));
        }

        return ret_date;
    }

    // Update property which records update/patch version: Ex. update-1, cp1, etc.
    public static String getUpdateVersion() {
        return "";
    }
}
