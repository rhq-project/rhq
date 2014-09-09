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

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public abstract class AbstractGroupConfigurationUpdateCriteria extends AbstractConfigurationUpdateCriteria {
    private static final long serialVersionUID = 1L;

    public static final String FETCH_FIELD_CONFIGURATION_UPDATES = "configurationUpdates";

    private List<Integer> filterResourceGroupIds; // requires override

    private boolean fetchConfigurationUpdates;

    public AbstractGroupConfigurationUpdateCriteria() {
        filterOverrides.put("resourceGroupIds", "group.id IN ( ? )");
    }

    public void addFilterResourceGroupIds(List<Integer> filterResourceGroupIds) {
        this.filterResourceGroupIds = filterResourceGroupIds;
    }

    /**
     * @param fetchConfiguration If true then fetch the group-level configuration to be applied to each group member.
     */
    @Override
    public void fetchConfiguration(boolean fetchConfiguration) {
        super.fetchConfiguration(fetchConfiguration);
    }

    /**
     * Fetch the current resource configurations for the group members.   The "current" configuration is the one most
     * recently reported and stored server-side, the agent is not queried.
     *
     * @param fetchConfigurationUpdates
     */
    public void fetchConfigurationUpdates(boolean fetchConfigurationUpdates) {
        this.fetchConfigurationUpdates = fetchConfigurationUpdates;
    }
}
