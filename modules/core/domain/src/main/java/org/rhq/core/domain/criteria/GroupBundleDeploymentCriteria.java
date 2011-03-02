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

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Simeon Pinder
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class GroupBundleDeploymentCriteria extends BundleDeploymentCriteria {
    private static final long serialVersionUID = 1L;

    private List<Integer> filterResourceGroupIds; // requires overrides

    public GroupBundleDeploymentCriteria() {
        filterOverrides.put("resourceGroupIds", "destination.group.id IN " //
            + "( SELECT rg.id " //
            + "    FROM ResourceGroup rg " //
            + "    JOIN rg.bundleDestinations bds " //
            + "   WHERE rg.id = ? )");
    }

    public void addFilterResourceGroupIds(Integer... filterResourceGroupIds) {
        this.filterResourceGroupIds = Arrays.asList(filterResourceGroupIds);
    }

    public List<Integer> getFilterGroupIds() {
        return filterResourceGroupIds;
    }
}
