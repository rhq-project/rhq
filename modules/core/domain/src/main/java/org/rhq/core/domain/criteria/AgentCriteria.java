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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageOrdering;

/**
 * Criteria object for querying {@link Server}s.
 * Only subject with MANAGE_SETTINGS can fetch these instances.
 *
 * @author Jiri Kremser
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class AgentCriteria extends Criteria {

    private static final long serialVersionUID = 1L;

    public static final String SORT_FIELD_CTIME = "ctime";

    public static final String SORT_FIELD_NAME = "name";

    private Integer filterId;
    private String filterName;
    private String filterAddress;
    private Integer filterPort;
    private String filterAgenttoken;
    private Integer filterServerId; // requires override
    private Integer filterAffinityGroupId; // requires override

    private boolean fetchServer;
    private boolean fetchAffinityGroup;

    private PageOrdering sortCtime;
    private PageOrdering sortName;

    public AgentCriteria() {
        filterOverrides.put("serverId", "server.id = ?");
        filterOverrides.put("affinityGroupId", "affinityGroup.id = ?");
    }

    public Class<?> getPersistentClass() {
        return Agent.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterAffinityGroupId(Integer filterAffinityGroupId) {
        this.filterAffinityGroupId = filterAffinityGroupId;
    }

    public void addFilterAddress(String filterAddress) {
        this.filterAddress = filterAddress;
    }

    public void addFilterPort(Integer filterPort) {
        this.filterPort = filterPort;
    }

    public void addFilterAgenttoken(String filterAgenttoken) {
        this.filterAgenttoken = filterAgenttoken;
    }

    public void addFilterServerId(Integer filterServerId) {
        this.filterServerId = filterServerId;
    }

    public void fetchAffinityGroup(boolean fetchAffinityGroup) {
        this.fetchAffinityGroup = fetchAffinityGroup;
    }

    public void fetchServer(boolean fetchServer) {
        this.fetchServer = fetchServer;
    }

    public void addSortCtime(PageOrdering sort) {
        addSortField(SORT_FIELD_CTIME);
        this.sortCtime = sort;
    }

    public void addSortName(PageOrdering sort) {
        addSortField(SORT_FIELD_NAME);
        this.sortName = sort;
    }
}
