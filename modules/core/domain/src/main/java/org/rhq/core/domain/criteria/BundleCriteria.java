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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleCriteria extends TaggedCriteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private List<Integer> filterBundleVersionIds; // needs overrides    
    private Integer filterBundleTypeId; // needs override
    private String filterBundleTypeName; // needs override    
    private String filterDescription;
    private List<Integer> filterDestinationIds; // needs overrides
    private String filterName;
    private Integer filterPackageTypeId; // needs override
    private String filterPackageTypeName; // needs override    

    private boolean fetchBundleVersions;
    private boolean fetchDestinations;
    private boolean fetchPackageType;
    private boolean fetchRepo;

    private PageOrdering sortName;
    private PageOrdering sortDescription;

    public BundleCriteria() {
        filterOverrides.put("bundleVersionIds", "" //
            + "id IN ( SELECT bv.bundle.id " //
            + "          FROM BundleVersion bv " //
            + "         WHERE bv.id IN ( ? ) )");
        filterOverrides.put("bundleTypeId", "bundleType.id = ?");
        filterOverrides.put("bundleTypeName", "bundleType.name like ?");
        filterOverrides.put("destinationIds", "" //
            + "id IN ( SELECT bd.bundle.id " //
            + "          FROM BundleDestination bd " //
            + "         WHERE bd.id IN ( ? ) )");
        filterOverrides.put("packageTypeId", "packageType.id = ?");
        filterOverrides.put("packageTypeName", "packageType.name like ?");
    }

    @Override
    public Class<Bundle> getPersistentClass() {
        return Bundle.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    /** Convenience routine calls addFilterBundleVersionIds */
    public void addFilterBundleVersionId(Integer filterBundleVersionId) {
        List<Integer> ids = new ArrayList<Integer>(1);
        ids.add(filterBundleVersionId);
        this.addFilterBundleVersionIds(ids);
    }

    public void addFilterBundleVersionIds(List<Integer> filterBundleVersionIds) {
        this.filterBundleVersionIds = filterBundleVersionIds;
    }

    public void addFilterBundleTypeId(Integer filterBundleTypeId) {
        this.filterBundleTypeId = filterBundleTypeId;
    }

    public void addFilterBundleTypeName(String filterBundleTypeName) {
        this.filterName = filterBundleTypeName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    /** Convenience routine calls addFilterDestinationIds */
    public void addFilterDestinationId(Integer filterDestinationId) {
        List<Integer> ids = new ArrayList<Integer>(1);
        ids.add(filterDestinationId);
        this.addFilterDestinationIds(ids);
    }

    public void addFilterDestinationIds(List<Integer> filterDestinationIds) {
        this.filterDestinationIds = filterDestinationIds;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterPackageTypeId(Integer filterPackageTypeId) {
        this.filterPackageTypeId = filterPackageTypeId;
    }

    public void addFilterPackageTypeName(String filterPackageTypeName) {
        this.filterPackageTypeName = filterPackageTypeName;
    }

    public void fetchBundleVersions(boolean fetchBundleVersions) {
        this.fetchBundleVersions = fetchBundleVersions;
    }

    public void fetchDestinations(boolean fetchDestinations) {
        this.fetchDestinations = fetchDestinations;
    }

    public void fetchPackageType(boolean fetchPackageType) {
        this.fetchPackageType = fetchPackageType;
    }

    public void fetchRepo(boolean fetchRepo) {
        this.fetchRepo = fetchRepo;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortDescription(PageOrdering sortDescription) {
        addSortField("description");
        this.sortDescription = sortDescription;
    }
}
