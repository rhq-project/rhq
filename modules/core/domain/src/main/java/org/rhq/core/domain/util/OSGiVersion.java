/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.domain.util;

/**
 * A simple representation of an OSGi formatted version string.
 * 
 * All fields except the "major" part of the version can be undefined.
 *
 * @author Lukas Krejci
 */
public class OSGiVersion implements Comparable<OSGiVersion> {
    private int major;
    private Integer minor;
    private Integer micro;
    private String qualifier;

    public OSGiVersion() {
        
    }
    
    public static boolean isValid(String version) {
        try {
            new OSGiVersion(version);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Creates new OSGiVersion instance from the version string.
     * 
     * @param version
     * 
     * @throws IllegalArgumentException if the version string isn't a well-formed OSGi version string.
     */
    public OSGiVersion(String version) {
        String[] parts = version.split("\\.");

        try {
            switch (parts.length) {
            case 4: {
                qualifier = parts[3];
            }

            case 3: {
                micro = Integer.parseInt(parts[2]);
            }

            case 2: {
                minor = Integer.parseInt(parts[1]);
            }

            case 1: {
                major = Integer.parseInt(parts[0]);
                break;
            }

            default: {
                throw new IllegalArgumentException("Malformed version string [" + version + "]");
            }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed version string [" + version + "]");
        }
    }

    /**
     * @return the major
     */
    public int getMajor() {
        return major;
    }

    /**
     * @param major the major to set
     */
    public void setMajor(int major) {
        this.major = major;
    }

    /**
     * @return the minor
     */
    public Integer getMinorIfDefined() {
        return minor;
    }

    public int getMinor() {
        return minor == null ? 0 : minor;
    }
    
    /**
     * @param minor the minor to set
     */
    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    /**
     * @return the micro
     */
    public int getMicro() {
        return micro == null ? 0 : micro;
    }

    public Integer getMicroIfDefined() {
        return micro;
    }
    
    /**
     * @param micro the micro to set
     */
    public void setMicro(Integer micro) {
        this.micro = micro;
    }

    /**
     * @return the qualifier
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * @param qualifier the qualifier to set
     */
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public int compareTo(OSGiVersion o) {
        int result = this.getMajor() - o.getMajor();
        if (result == 0) {
            result = this.getMinor() - o.getMinor();
            if (result == 0) {
                result = this.getMicro() - o.getMicro();
                if (result == 0) {
                    if (this.getQualifier() != null) {
                        if (o.getQualifier() == null) {
                            result = 1;
                        } else {
                            result = this.getQualifier().compareTo(o.getQualifier());
                        }
                    } else {
                        if (o.getQualifier() == null) {
                            result = 0;
                        } else {
                            result = -1;
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append(major);
        
        if (minor != null) {
            bld.append(".").append(minor);
        }
        
        if (micro != null) {
            bld.append(".").append(micro);
        }
        
        if (qualifier != null) {
            bld.append(".").append(qualifier);
        }
        
        return bld.toString();
    }
}
