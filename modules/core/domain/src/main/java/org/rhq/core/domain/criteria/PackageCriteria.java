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

package org.rhq.core.domain.criteria;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.util.PageOrdering;

/**
 * Criteria object for querying {@link Package}s.
 *
 * @author Lukas Krejci
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class PackageCriteria extends Criteria {

    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterName;
    private String filterClassification;
    private Integer filterPackageTypeId;
    
    private boolean fetchVersions;
    
    private PageOrdering sortName;
    
    public PackageCriteria() {
        filterOverrides.put("packageTypeId", "packageType.id = ? "); 
    }
    
    public Class<?> getPersistentClass() {
        return Package.class;
    }

    public void addFilterId(Integer id) {
        this.filterId = id;
    }
    
    public void addFilterName(String name) {
        this.filterName = name;
    }
    
    public void addFilterClassification(String classification) {
        this.filterClassification = classification;
    }
    
    public void addFilterPackageTypeId(Integer packageTypeId) {
        this.filterPackageTypeId = packageTypeId;
    }
    
    public void fetchVersions(boolean fetchVersions) {
        this.fetchVersions = fetchVersions;
    }
    
    public void addSortName(PageOrdering sort) {
        addSortField("name");
        this.sortName = sort;
    }
    
    public boolean isInventoryManagerRequired() {
        return fetchVersions;
    }
}
