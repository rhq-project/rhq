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

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleDeploymentCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Integer filterBundleDeployDefinitionId; // requires override   
    private String filterBundleDeployDefinitionName; // requires override
    private Integer filterBundleGroupDeploymentId; // requires override    
    private Integer filterResourceId; // requires override
    private String filterResourceName; // requires override
    private BundleDeploymentStatus filterStatus;

    private boolean fetchBundleDeployDefinition;
    private boolean fetchResource;
    private boolean fetchHistories;

    public BundleDeploymentCriteria() {
        filterOverrides.put("bundleDeployDefinitionId", "parentResource.id = ?");
        filterOverrides.put("bundleDeployDefinitionName", "parentResource.name like ?");
        filterOverrides.put("bundleGroupDeploymentId", "bundleGroupDeployment.id = ?");
        filterOverrides.put("resourceId", "resource.id = ?");
        filterOverrides.put("resourceName", "resource.name like ?");
    }

    @Override
    public Class<?> getPersistentClass() {
        return BundleDeployment.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterBundleDeployDefinitionId(Integer filterBundleDeployDefinitionId) {
        this.filterBundleDeployDefinitionId = filterBundleDeployDefinitionId;
    }

    public void addFilterBundleDeployDefinitionName(String filterBundleDeployDefinitionName) {
        this.filterBundleDeployDefinitionName = filterBundleDeployDefinitionName;
    }

    public void addFilterBundleGroupDeploymentId(Integer filterBundleGroupDeploymentId) {
        this.filterBundleGroupDeploymentId = filterBundleGroupDeploymentId;
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

    public void fetchBundleDeployDefinition(boolean fetchBundleDeployDefinition) {
        this.fetchBundleDeployDefinition = fetchBundleDeployDefinition;
    }

    /**
     * Requires MANAGE_INVENTORY
     *
     * @param fetchResource
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
