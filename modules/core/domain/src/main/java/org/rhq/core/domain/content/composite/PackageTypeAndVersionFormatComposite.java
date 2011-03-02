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

package org.rhq.core.domain.content.composite;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersionFormatDescription;

/**
 * @author Lukas Krejci
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PackageTypeAndVersionFormatComposite implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private PackageType packageType;
    private PackageVersionFormatDescription versionFormat;
    
    protected PackageTypeAndVersionFormatComposite() {
        
    }
    
    /**
     * @param packageType
     * @param versionFormat
     */
    public PackageTypeAndVersionFormatComposite(PackageType packageType, PackageVersionFormatDescription versionFormat) {
        super();
        this.packageType = packageType;
        this.versionFormat = versionFormat;
    }

    /**
     * @return the packageType
     */
    public PackageType getPackageType() {
        return packageType;
    }

    /**
     * @return the versionFormat, can be null
     */
    public PackageVersionFormatDescription getVersionFormat() {
        return versionFormat;
    }
}
