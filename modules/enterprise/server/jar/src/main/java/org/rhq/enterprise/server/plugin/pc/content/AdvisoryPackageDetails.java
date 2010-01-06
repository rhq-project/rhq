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
package org.rhq.enterprise.server.plugin.pc.content;

public class AdvisoryPackageDetails {
    private static final long serialVersionUID = 1L;
    private String name;
    private String version;
    private String arch;
    private String rpmFilename;

    public AdvisoryPackageDetails(String name, String version, String architectureName, String rpmname) {
        this.name = name;
        this.version = version;
        this.arch = architectureName;
        this.rpmFilename = rpmname;
    }

    public String getRpmFilename() {
        return rpmFilename;
    }

    public void setRpmFilename(String rpmFilename) {
        this.rpmFilename = rpmFilename;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    @Override
    public String toString() {
        return "AdvisoryPackageDetails[" + super.toString() + "]";
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof AdvisoryPackageDetails)) {
            return false;
        }

        return true;
    }
}
