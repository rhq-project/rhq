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

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleDeploymentCriteria extends TaggedCriteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Boolean filterIsLive;
    private String filterName;
    private String filterDescription;
    private Integer filterBundleId; // needs override
    private Integer filterBundleVersionId; // needs override    
    private Integer filterDestinationId; // needs override
    private String filterDestinationName; // needs override
    private BundleDeploymentStatus filterStatus;
    private PageOrdering sortStatus;
    private String filterSubjectName;

    private boolean fetchBundleVersion;
    private boolean fetchConfiguration;
    private boolean fetchDestination;
    private boolean fetchResourceDeployments;

    public BundleDeploymentCriteria() {
        filterOverrides.put("bundleId", "bundleVersion.bundle.id = ?");
        filterOverrides.put("bundleVersionId", "bundleVersion.id = ?");
        filterOverrides.put("destinationId", "destination.id = ?");
        filterOverrides.put("destinationName", "destination.name like ?");
    }

    @Override
    public Class<BundleDeployment> getPersistentClass() {
        return BundleDeployment.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterIsLive(Boolean filterIsLive) {
        this.filterIsLive = filterIsLive;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDestinationId(Integer filterDestinationId) {
        this.filterDestinationId = filterDestinationId;
    }

    public void addFilterDestinationName(String filterDestinationName) {
        this.filterDestinationName = filterDestinationName;
    }

    public void addFilterStatus(BundleDeploymentStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public void addFilterSubjectName(String filterSubjectName) {
        this.filterSubjectName = filterSubjectName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterBundleId(Integer filterBundleId) {
        this.filterBundleId = filterBundleId;
    }

    public void addFilterBundleVersionId(Integer filterBundleVersionId) {
        this.filterBundleVersionId = filterBundleVersionId;
    }

    public void fetchBundleVersion(boolean fetchBundleVersion) {
        this.fetchBundleVersion = fetchBundleVersion;
    }

    public void fetchConfiguration(boolean fetchConfiguration) {
        this.fetchConfiguration = fetchConfiguration;
    }

    public void fetchDestination(boolean fetchDestination) {
        this.fetchDestination = fetchDestination;
    }

    public void fetchResourceDeployments(boolean fetchResourceDeployments) {
        this.fetchResourceDeployments = fetchResourceDeployments;
    }

    public void addSortStatus(PageOrdering sortStatus) {
        addSortField("sort");
        this.sortStatus = sortStatus;
    }

}
