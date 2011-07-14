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

import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageVersion;

/**
 * This composite holds a general package together with a package version that is found
 * to be the "latest" using the version comparison.
 *
 * @author Lukas Krejci
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PackageAndLatestVersionComposite implements Serializable {

    
    private static final long serialVersionUID = 1L;
    
    private Package generalPackage;
    private PackageVersion latestPackageVersion;
    
    public PackageAndLatestVersionComposite() {
        
    }
    
    public PackageAndLatestVersionComposite(Package generalPackage) {
        this.generalPackage = generalPackage;
    }
    
    public PackageAndLatestVersionComposite(Package generalPakcage, PackageVersion packageVersion) {
        this.generalPackage = generalPakcage;
        this.latestPackageVersion = packageVersion;
    }
    
    /**
     * @return the generalPackage
     */
    public Package getGeneralPackage() {
        return generalPackage;
    }
    
    /**
     * @param generalPackage the generalPackage to set
     */
    public void setGeneralPackage(Package generalPackage) {
        this.generalPackage = generalPackage;
    }
    
    /**
     * @return the latestPackageVersion
     */
    public PackageVersion getLatestPackageVersion() {
        return latestPackageVersion;
    }
    
    /**
     * @param latestPackageVersion the latestPackageVersion to set
     */
    public void setLatestPackageVersion(PackageVersion latestPackageVersion) {
        this.latestPackageVersion = latestPackageVersion;
    }
}
