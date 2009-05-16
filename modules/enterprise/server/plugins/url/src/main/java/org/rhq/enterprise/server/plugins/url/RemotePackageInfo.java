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
package org.rhq.enterprise.server.plugins.url;

import java.net.URL;

public class RemotePackageInfo {
    private String location; // the path of the package relative to the root URL
    private URL url;
    private String md5;
    private SupportedPackageType supportedPackageType;

    public RemotePackageInfo(String location, URL url, String md5) {
        this.location = location;
        this.url = url;
        this.md5 = md5;
        this.supportedPackageType = null; // we don't know it
    }

    public String getLocation() {
        return this.location;
    }

    public URL getUrl() {
        return this.url;
    }

    public String getMD5() {
        return this.md5;
    }

    /**
     * If this package info knows it, this will return the package type that supports this package.
     * If this package info does not know its package type information, this will return <code>null</code>.
     * 
     * @return known package type that supports this package or <code>null</code> if not known
     */
    public SupportedPackageType getSupportedPackageType() {
        return this.supportedPackageType;
    }

    public void setSupportedPackageType(SupportedPackageType type) {
        this.supportedPackageType = type;
    }

    public String toString() {
        StringBuilder str = new StringBuilder("RemotePackageInfo: ");
        str.append("location=[").append(this.location);
        str.append("], url=[").append(this.url);
        str.append("], md5=[").append(this.md5);
        if (this.supportedPackageType != null) {
            str.append("], supportedPackageType=[").append(this.supportedPackageType.packageTypeName);
            str.append(",").append(this.supportedPackageType.architectureName);
            str.append(",").append(this.supportedPackageType.resourceTypeName);
            str.append(",").append(this.supportedPackageType.resourceTypePluginName);
            str.append("]");
        } else {
            str.append("], supportedPackageType=[unknown]");
        }
        return str.toString();
    }

    public static class SupportedPackageType {
        public String packageTypeName;
        public String architectureName;
        public String resourceTypeName;
        public String resourceTypePluginName;
    }
}
