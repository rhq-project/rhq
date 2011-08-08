/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * RHQ Criteria query support for DriftConfiguration.  This is not drift server plugin supported, DriftConfiguration
 * is a native entity.
 * 
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class DriftConfigurationCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterName;
    private List<Integer> filterResourceIds; // needs override    

    private Boolean fetchConfiguration;
    private Boolean fetchResource;

    private PageOrdering sortName;

    public DriftConfigurationCriteria() {
        filterOverrides.put("resourceIds", "resource.id IN ( ? )");
    }

    @Override
    public Class<DriftConfiguration> getPersistentClass() {
        return DriftConfiguration.class;
    }

    public void addFilterId(String filterId) {
        if (filterId != null) {
            this.filterId = Integer.parseInt(filterId);
        }
    }

    public void addFilterName(String name) {
        this.filterName = name;
    }

    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = CriteriaUtils.getListIgnoringNulls(filterResourceIds);
    }

    public void fetchConfiguration(Boolean fetchConfiguration) {
        this.fetchConfiguration = fetchConfiguration;
    }

    public void fetchResource(Boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }
}
