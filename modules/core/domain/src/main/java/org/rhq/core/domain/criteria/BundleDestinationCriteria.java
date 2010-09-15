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

import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleDestination;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleDestinationCriteria extends TaggedCriteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterName;
    private String filterDescription;
    private Integer filterBundleId; // needs override
    private Integer filterBundleVersionId; // needs override
    private BundleDeploymentStatus filterStatus; // needs override
    private String filterDeployDir;
    private Integer filterGroupId; // needs override

    private boolean fetchBundle;
    private boolean fetchDeployments;
    private boolean fetchGroup;

    public BundleDestinationCriteria() {
        filterOverrides.put("bundleId", "bundle.id = ?");
        filterOverrides.put("bundleVersionId", "deployments.bundleVersion.id = ?");
        filterOverrides.put("status", "deployment.status like ?");
        filterOverrides.put("groupId", "group.id = ?");
    }

    @Override
    public Class<BundleDestination> getPersistentClass() {
        return BundleDestination.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterBundleId(Integer filterBundleId) {
        this.filterBundleId = filterBundleId;
    }

    public void addFilterStatus(BundleDeploymentStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterBundleVersionId(Integer filterBundleVersionId) {
        this.filterBundleVersionId = filterBundleVersionId;
    }

    public void addFilterGroupId(Integer filterGroupId) {
        this.filterGroupId = filterGroupId;
    }

    public void fetchBundle(boolean fetchBundle) {
        this.fetchBundle = fetchBundle;
    }

    public void fetchDeployments(boolean fetchDeployments) {
        this.fetchDeployments = fetchDeployments;
    }

    public void fetchGroup(boolean fetchGroup) {
        this.fetchGroup = fetchGroup;
    }
}