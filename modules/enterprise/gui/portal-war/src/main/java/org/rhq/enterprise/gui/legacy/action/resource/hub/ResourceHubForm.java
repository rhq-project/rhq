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

import org.rhq.core.domain.resource.ResourceCategory;

/**
 * The form bean for the resource hub page (ResourceHub.jsp).
 *
 * @author Ian Springer
 */
public class ResourceHubForm extends HubForm {
    /**
     * Resource category filter (only required when viewing resources). The value corresponds to
     * {@link ResourceCategory#name()} - either "PLATFORM", "SERVER, or "SERVICE".
     */
    private String resourceCategory;

    public ResourceHubForm() {
        super();
        setDefaults();
    }

    public String getResourceCategory() {
        return this.resourceCategory;
    }

    public void setResourceCategory(String resourceCategory) {
        this.resourceCategory = resourceCategory;
    }

    protected void setDefaults() {
        resourceCategory = ResourceCategory.PLATFORM.name();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(super.toString());
        stringBuilder.append(" resourceCategory=").append(resourceCategory);
        return stringBuilder.toString();
    }
}