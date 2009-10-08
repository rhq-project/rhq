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
package org.rhq.core.db.ant.dbupgrade;

import java.util.StringTokenizer;

/**
 * Object that encapsulates and compares database schema version strings. A version string follows the format
 * major[.minor[.patch]] where the minor and patch numbers are optional. Examples are <code>1</code>, <code>1.0</code>,
 * <code>2.1.1</code>.
 */
public class SchemaVersion implements Comparable {
    /**
     * Defines the development version string - this is the schema version on development builds. The development schema
     * version is considered the {@link #LATEST_VERSION latest version}.
     */
    public static final String DEV_VERSION = "@@@DB_SCHEMA_VERSION@@@";

    /**
     * Defines the version string that indicates this is the latest schema version. There are no other versions that are
     * newer than this one and thus doing a DB upgrade will have no effect on the schema.
     */
    public static final String LATEST_VERSION = "LATEST";

    /**
     * The actual schema version this object contains.
     */
    private String versionString = null;

    // these are the individual version components as parsed from the versionString
    private int majorVersion = 0;
    private int minorVersion = 0;
    private int patchVersion = 0;
    private boolean isLatest = false;

    /**
     * Creates a new {@link SchemaVersion} object. If the <code>version_string</code> is <code>null</code>, it will be
     * considered the {@link #LATEST_VERSION latest} version.
     *
     * @param  version_string
     *
     * @throws IllegalArgumentException if the string was not a valid version
     */
    public SchemaVersion(String version_string) throws IllegalArgumentException {
        versionString = version_string;

        if ((versionString == null) || versionString.equals(DEV_VERSION)) {
            versionString = LATEST_VERSION;
        }

        if (versionString.equalsIgnoreCase(LATEST_VERSION)) {
            isLatest = true;
            majorVersion = 1234567890;
            minorVersion = 1234567890;
            patchVersion = 1234567890;
        } else {
            isLatest = false;

            StringTokenizer strtok = new StringTokenizer(versionString, ".");
            if (!strtok.hasMoreTokens()) {
                throw new IllegalArgumentException("Invalid version: " + versionString);
            }

            majorVersion = parseInt(strtok.nextToken());

            if (strtok.hasMoreTokens()) {
                minorVersion = parseInt(strtok.nextToken());
                if (strtok.hasMoreTokens()) {
                    patchVersion = parseInt(strtok.nextToken());
                }
            }
        }
    }

    /**
     * The major version number, which is the left most number in the X.X.X version string.
     *
     * @return major version number
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * The minor version number, which is the second number in the X.X.X version string.
     *
     * @return minor version number
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * The patch version number, which is the third number in the X.X.X version string.
     *
     * @return patch version number
     */
    public int getPatchVersion() {
        return patchVersion;
    }

    /**
     * Returns <code>true</code> if this version is to be considered the latest schema version; no other schema versions
     * are considered newer than the version this object represents.
     *
     * @return <code>true</code> if this version string represents the newest version.
     */
    public boolean getIsLatest() {
        return isLatest;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("o==null");
        }

        if (!(o instanceof SchemaVersion)) {
            throw new IllegalArgumentException(o.getClass().getName() + " != " + SchemaVersion.class.getName());
        }

        SchemaVersion v = (SchemaVersion) o;

        int num;

        if (this.getMajorVersion() != v.getMajorVersion()) {
            num = (this.getMajorVersion() - v.getMajorVersion());
        } else if (this.getMinorVersion() != v.getMinorVersion()) {
            num = (this.getMinorVersion() - v.getMinorVersion());
        } else {
            num = (this.getPatchVersion() - v.getPatchVersion());
        }

        return num;
    }

    /**
     * Same as <code>Integer.parseInt</code> except it throws an <code>IllegalArgumentException</code> rather than
     * <code>NumberFormatException</code>.
     *
     * @param  number_string
     *
     * @return the number
     *
     * @throws IllegalArgumentException if the string could not be parsed as a valid number
     */
    private int parseInt(String number_string) throws IllegalArgumentException {
        try {
            return Integer.parseInt(number_string);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(nfe);
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return versionString;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if ((o != null) && (o instanceof SchemaVersion)) {
            SchemaVersion compare_me = (SchemaVersion) o;
            return (this.majorVersion == compare_me.majorVersion) && (this.minorVersion == compare_me.minorVersion)
                && (this.patchVersion == compare_me.patchVersion);
        }

        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + majorVersion;
        result = (prime * result) + minorVersion;
        result = (prime * result) + patchVersion;
        return result;
    }

    /**
     * Determines if this version is newer than <code>start_exclusive</code> but equal to or older than <code>
     * end_inclusive</code>.
     *
     * @param  start_exclusive the version that this version should be newer than
     * @param  end_inclusive   the version that this version should equal or be older than
     *
     * @return <code>true</code> if this version is between the two given versions
     */
    public boolean between(SchemaVersion start_exclusive, SchemaVersion end_inclusive) {
        return ((compareTo(start_exclusive) > 0) && (compareTo(end_inclusive) <= 0));
    }
}