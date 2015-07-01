/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.util.PageOrdering;

/**
 * Criteria object for querying {@link StorageNode}s.
 * Only subject with MANAGE_SETTINGS can fetch these instances.
 *
 * @author Jay Shaughnessy
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class StorageNodeCriteria extends Criteria {

    private static final long serialVersionUID = 1L;

    public static final String SORT_FIELD_CTIME = "ctime";

    public static final String SORT_FIELD_ADDRESS = "address";

    private String filterAddress;
    private Integer filterJmxPort;
    private Integer filterCqlPort;
    private List<OperationMode> filterOperationMode; // requires override
    private Integer filterResourceId; // requires override
    private Integer parentResourceId; // requires override

    private boolean fetchResource;

    private PageOrdering sortCtime;
    private PageOrdering sortAddress;

    public StorageNodeCriteria() {
        filterOverrides.put("operationMode", "operationMode IN ( ? )");
        filterOverrides.put("resourceId", "resource.id = ?");
        filterOverrides.put("parentResourceId", "resource.parentResource.id = ?");
    }

    public Class<?> getPersistentClass() {
        return StorageNode.class;
    }

    public void addFilterAddress(String filterAddress) {
        this.filterAddress = filterAddress;
    }

    public void addFilterJmxPort(Integer filterJmxPort) {
        this.filterJmxPort = filterJmxPort;
    }

    public void addFilterCqlPort(Integer filterCqlPort) {
        this.filterCqlPort = filterCqlPort;
    }

    public void addFilterOperationMode(OperationMode... operationMode) {
        if (operationMode != null && operationMode.length > 0) {
            this.filterOperationMode = Arrays.asList(operationMode);
        }
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public void addFilterParentResourceId(Integer filterParentResourceId) {
        this.parentResourceId = filterParentResourceId;
    }
    
    public void fetchResource(boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void addSortCtime(PageOrdering sort) {
        addSortField(SORT_FIELD_CTIME);
        this.sortCtime = sort;
    }

    public void addSortAddress(PageOrdering sort) {
        addSortField(SORT_FIELD_ADDRESS);
        this.sortAddress = sort;
    }
}
