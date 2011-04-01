/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.criteria;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public abstract class AbstractConfigurationUpdateCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    public static final String SORT_FIELD_CTIME = "createdTime";

    public static final String SORT_FIELD_STATUS = "status";

    private Integer filterId;
    private Long filterStartTime; // requires overrides
    private Long filterEndTime; // requires overrides
    private ConfigurationUpdateStatus filterStatus;
    private List<ConfigurationUpdateStatus> filterStatuses; // requires overrides

    private boolean fetchConfiguration;

    private PageOrdering sortCtime;
    private PageOrdering sortStatus; // requires sort override

    public AbstractConfigurationUpdateCriteria() {

        filterOverrides.put("startTime", "createdTime >= ?");
        filterOverrides.put("endTime", "createdTime <= ?");
        filterOverrides.put("statuses", "status IN ( ? )");

        sortOverrides.put(SORT_FIELD_STATUS, "status");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public void addFilterStatus(ConfigurationUpdateStatus status) {
        this.filterStatus = status;
    }

    public void addFilterStatuses(ConfigurationUpdateStatus... configUpdateStatus) {
        this.filterStatuses = CriteriaUtils.getListIgnoringNulls(configUpdateStatus);
    }

    public void fetchConfiguration(boolean configuration) {
        this.fetchConfiguration = configuration;
    }

    public void addSortCtime(PageOrdering sortCtime) {
        addSortField(SORT_FIELD_CTIME);
        this.sortCtime = sortCtime;
    }

    public void addSortStatus(PageOrdering sortStatus) {
        addSortField(SORT_FIELD_STATUS);
        this.sortStatus = sortStatus;
    }
}
