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

import org.rhq.core.domain.bundle.BundleVersion;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleVersionCriteria extends TaggedCriteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterDescription;
    private String filterName;
    private String filterVersion;
    private Integer filterBundleId; // needs override    
    private String filterBundleName; // needs override    

    private boolean fetchBundle;
    private boolean fetchBundleDeployments;
    private boolean fetchBundleFiles;
    private boolean fetchConfigurationDefinition;

    public BundleVersionCriteria() {
        filterOverrides.put("bundleId", "bundle.id = ?");
        filterOverrides.put("bundleName", "bundle.name like ?");
    }

    @Override
    public Class<BundleVersion> getPersistentClass() {
        return BundleVersion.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterBundleId(Integer filterBundleId) {
        this.filterBundleId = filterBundleId;
    }

    public void addFilterBundleName(String filterBundleName) {
        this.filterBundleName = filterBundleName;
    }

    public void addFilterVersion(String filterVersion) {
        this.filterVersion = filterVersion;
    }

    public void fetchBundle(boolean fetchBundle) {
        this.fetchBundle = fetchBundle;
    }

    public void fetchBundleDeployments(boolean fetchBundleDeployments) {
        this.fetchBundleDeployments = fetchBundleDeployments;
    }

    public void fetchBundleFiles(boolean fetchBundleFiles) {
        this.fetchBundleFiles = fetchBundleFiles;
    }

    public void fetchConfigurationDefinition(boolean fetchConfigurationDefinition) {
        this.fetchConfigurationDefinition = fetchConfigurationDefinition;
    }

}
