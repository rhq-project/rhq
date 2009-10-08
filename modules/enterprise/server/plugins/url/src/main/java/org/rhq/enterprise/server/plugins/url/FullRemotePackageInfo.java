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

import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;

/**
 * Contains the full metadata about package content.
 * This encapsulates the information stored in {@link ContentSourcePackageDetails}.
 * 
 * @author John Mazzitelli
 */
public class FullRemotePackageInfo extends RemotePackageInfo {

    private final ContentSourcePackageDetails details;

    public FullRemotePackageInfo(URL url, ContentSourcePackageDetails details) {
        super(details.getLocation(), url, details.getMD5());
        this.details = details;

        // the metadata provided let's us know our package type
        SupportedPackageType type = new SupportedPackageType();
        type.packageTypeName = details.getPackageTypeName();
        type.architectureName = details.getArchitectureName();
        type.resourceTypeName = details.getContentSourcePackageDetailsKey().getResourceTypeName();
        type.resourceTypePluginName = details.getContentSourcePackageDetailsKey().getResourceTypePluginName();
        setSupportedPackageType(type);
    }

    public ContentSourcePackageDetails getContentSourcePackageDetails() {
        return details;
    }

    public String toString() {
        StringBuilder str = new StringBuilder("FullRemotePackageInfo: ");
        str.append("location=[").append(this.getLocation());
        str.append("], url=[").append(this.getUrl());
        str.append("], md5=[").append(this.getMD5());
        if (this.getSupportedPackageType() != null) {
            str.append("], supportedPackageType=[").append(this.getSupportedPackageType().packageTypeName);
            str.append(",").append(this.getSupportedPackageType().architectureName);
            str.append(",").append(this.getSupportedPackageType().resourceTypeName);
            str.append(",").append(this.getSupportedPackageType().resourceTypePluginName);
            str.append("], ");
        } else {
            str.append("], supportedPackageType=[unknown], ");
        }
        str.append("details=[" + this.details + "]");
        return str.toString();
    }
}
