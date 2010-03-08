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

import org.rhq.core.domain.bundle.Bundle;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Integer filterBundleTypeId; // needs override
    private String filterBundleTypeName; // needs override    
    private String filterName;

    private boolean fetchBundleVersions;
    private boolean fetchRepo;

    public BundleCriteria() {
        super();

        filterOverrides.put("bundleTypeId", "bundleType.id = ?");
        filterOverrides.put("bundleTypeName", "bundleType.name like ?");
    }

    public Class<Bundle> getPersistentClass() {
        return Bundle.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterBundleTypeId(Integer filterBundleTypeId) {
        this.filterBundleTypeId = filterBundleTypeId;
    }

    public void addFilterBundleTypeName(String filterBundleTypeName) {
        this.filterName = filterBundleTypeName;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void fetchBundleVersions(boolean fetchBundleVersions) {
        this.fetchBundleVersions = fetchBundleVersions;
    }

    public void fetchRepo(boolean fetchRepo) {
        this.fetchRepo = fetchRepo;
    }

}
