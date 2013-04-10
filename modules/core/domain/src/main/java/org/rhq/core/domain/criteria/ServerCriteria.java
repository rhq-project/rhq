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

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.util.PageOrdering;

/**
 * Criteria object for querying {@link Server}s.
 * Only subject with MANAGE_SETTINGS can fetch these instances.
 *
 * @author Jiri Kremser
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ServerCriteria extends Criteria {

    private static final long serialVersionUID = 2L;

    public static final String SORT_FIELD_CTIME = "ctime";

    public static final String SORT_FIELD_NAME = "name";

    public static final String SORT_FIELD_COMPUTE_POWER = "computePower";

    private String filterName;
    private String filterAddress;
    private Integer filterPort;
    private Integer filterSecurePort;
    private List<OperationMode> filterOperationMode; // requires override
    private Integer filterComputePower;
    private Integer filterAffinityGroupId; // requires override

    private boolean fetchAgents;
    private boolean fetchAffinityGroup;

    private PageOrdering sortCtime;
    private PageOrdering sortName;
    private PageOrdering sortComputePower;

    public ServerCriteria() {
        filterOverrides.put("operationMode", "operationMode IN ( ? )");
        filterOverrides.put("affinityGroupId", "affinityGroup.id = ?");
    }

    public Class<?> getPersistentClass() {
        return Server.class;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterAddress(String filterAddress) {
        this.filterAddress = filterAddress;
    }

    public void addFilterPort(Integer filterPort) {
        this.filterPort = filterPort;
    }

    public void addFilterSecurePort(Integer filterSecurePort) {
        this.filterSecurePort = filterSecurePort;
    }

    public void addFilterOperationMode(OperationMode... operationMode) {
        if (operationMode != null && operationMode.length > 0) {
            this.filterOperationMode = Arrays.asList(operationMode);
        }
    }

    public void addFilterComputePower(Integer filterComputePower) {
        this.filterComputePower = filterComputePower;
    }

    public void addFilterAffinityGroupId(Integer filterAffinityGroupId) {
        this.filterAffinityGroupId = filterAffinityGroupId;
    }

    public void fetchAgents(boolean fetchAgents) {
        this.fetchAgents = fetchAgents;
    }

    public void fetchAffinityGroup(boolean fetchAffinityGroup) {
        this.fetchAffinityGroup = fetchAffinityGroup;
    }

    public void addSortCtime(PageOrdering sort) {
        addSortField(SORT_FIELD_CTIME);
        this.sortCtime = sort;
    }

    public void addSortName(PageOrdering sort) {
        addSortField(SORT_FIELD_NAME);
        this.sortName = sort;
    }

    public void addSortComputePower(PageOrdering sortComputePower) {
        addSortField(SORT_FIELD_COMPUTE_POWER);
        this.sortComputePower = sortComputePower;
    }
}
