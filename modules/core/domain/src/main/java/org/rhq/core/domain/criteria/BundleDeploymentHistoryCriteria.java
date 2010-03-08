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

import org.rhq.core.domain.bundle.BundleDeploymentHistory;

/**
 * @author Adam Young
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleDeploymentHistoryCriteria extends Criteria {

    private static final long serialVersionUID = 747793536546442610L;

    //No reason to make these private.  The setters are necessary to work with bean-api mechanisms
    public Integer filterId;
    public Integer filterPlatformResourceId;
    public Integer filterBundleId;
    public Integer filterBundleDeploymentId;
    public Integer filterBundleDeploymentDefinitionId;

    public BundleDeploymentHistoryCriteria() {
        super();

    }

    public Class<BundleDeploymentHistory> getPersistentClass() {
        return BundleDeploymentHistory.class;
    }

    public void setFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void setFilterPlatformResourceId(Integer filterPlatformResourceId) {
        this.filterPlatformResourceId = filterPlatformResourceId;
    }

    public void setFilterBundleId(Integer filterBundleId) {
        this.filterBundleId = filterBundleId;
    }

    public void setFilterBundleDeploymentId(Integer filterBundleDeploymentId) {
        this.filterBundleDeploymentId = filterBundleDeploymentId;
    }

    public void setFilterBundleDeploymentDefinitionId(Integer filterBundleDeploymentDefinitionId) {
        this.filterBundleDeploymentDefinitionId = filterBundleDeploymentDefinitionId;
    }

}
