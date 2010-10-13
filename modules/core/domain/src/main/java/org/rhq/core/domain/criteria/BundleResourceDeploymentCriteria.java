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
import org.rhq.core.domain.bundle.BundleResourceDeployment;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleResourceDeploymentCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Integer filterBundleDeploymentId; // requires override   
    private String filterBundleDeploymentName; // requires override    
    private Integer filterResourceId; // requires override
    private String filterResourceName; // requires override
    private BundleDeploymentStatus filterStatus;

    private boolean fetchBundleDeployment;
    private boolean fetchResource;
    private boolean fetchHistories;

    public BundleResourceDeploymentCriteria() {
        filterOverrides.put("bundleDeploymentId", "bundleDeployment.id = ?");
        filterOverrides.put("bundleDeploymentName", "bundleDeployment.name like ?");
        filterOverrides.put("resourceId", "resource.id = ?");
        filterOverrides.put("resourceName", "resource.name like ?");
    }

    @Override
    public Class<BundleResourceDeployment> getPersistentClass() {
        return BundleResourceDeployment.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterBundleDeploymentId(Integer filterBundleDeploymentId) {
        this.filterBundleDeploymentId = filterBundleDeploymentId;
    }

    public void addFilterBundleDeploymentName(String filterBundleDeploymentName) {
        this.filterBundleDeploymentName = filterBundleDeploymentName;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public void addFilterResourceName(String filterResourceName) {
        this.filterResourceName = filterResourceName;
    }

    public void addFilterStatus(BundleDeploymentStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public void fetchBundleDeployment(boolean fetchBundleDeployment) {
        this.fetchBundleDeployment = fetchBundleDeployment;
    }

    /**
     * By setting this fetch to true, it will cause {@link #isInventoryManagerRequired()} to return true as well.
     * 
     * In practice, however, MANAGE_INVENTORY is too restrictive; a bundle manager who has MANAGE_BUNDLE but not
     * MANAGE_INVENTORY would not be able to see any resource deployments.  So, until it is possible to handle granular
     * authorization checks on optionally fetched criteria data, a bundle manager will be allowed to see resource 
     * deployments to any platform.
     * 
     * @see org.rhq.enterprise.server.bundle.BundleManagerBean#findBundleResourceDeploymentsByCriteria(org.rhq.core.domain.auth.Subject, BundleResourceDeploymentCriteria)
     */
    public void fetchResource(boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void fetchHistories(boolean fetchHistories) {
        this.fetchHistories = fetchHistories;
    }

    /**
     * subclasses should override as necessary
     */
    public boolean isInventoryManagerRequired() {
        return (this.fetchResource);
    }

}
