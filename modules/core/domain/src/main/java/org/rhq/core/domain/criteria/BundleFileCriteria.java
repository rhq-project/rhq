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

package org.rhq.core.domain.criteria;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.bundle.BundleFile;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleFileCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Integer filterBundleVersionId; // needs override
    private Integer filterPackageVersionId; // needs override    

    private boolean fetchBundleVersion;
    private boolean fetchPackageVersion;

    public BundleFileCriteria() {
        filterOverrides.put("bundleVersionId", "bundleVersion.id = ?");
        filterOverrides.put("packageVersionId", "packageVersion.id = ?");
    }

    @Override
    public Class<BundleFile> getPersistentClass() {
        return BundleFile.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterBundleVersionId(Integer filterBundleVersionId) {
        this.filterBundleVersionId = filterBundleVersionId;
    }

    public void addFilterPackageVersionId(Integer filterPackageVersionId) {
        this.filterPackageVersionId = filterPackageVersionId;
    }

    public void fetchBundleVersion(boolean fetchBundleVersion) {
        this.fetchBundleVersion = fetchBundleVersion;
    }

    public void fetchPackageVersion(boolean fetchPackageVersion) {
        this.fetchPackageVersion = fetchPackageVersion;
    }
}
