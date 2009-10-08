/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.legacy.action.resource.hub;

import org.rhq.core.domain.resource.group.GroupCategory;

/**
 * The form bean for the group hub page (GroupHub.jsp).
 */
public class GroupHubForm extends HubForm {
    /**
     * Group category filter (only required when viewing groups). The value corresponds to {@link GroupCategory#name()}
     * - either "COMPATIBLE", "MIXED".
     */
    private String groupCategory;

    public GroupHubForm() {
        super();
        setDefaults();
    }

    public String getGroupCategory() {
        return groupCategory;
    }

    public void setGroupCategory(String groupCategory) {
        this.groupCategory = groupCategory;
    }

    @Override
    protected void setDefaults() {
        groupCategory = GroupCategory.COMPATIBLE.name();
        view = HubView.LIST.name();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(super.toString());
        stringBuilder.append(" groupCategory=").append(groupCategory);
        return stringBuilder.toString();
    }
}