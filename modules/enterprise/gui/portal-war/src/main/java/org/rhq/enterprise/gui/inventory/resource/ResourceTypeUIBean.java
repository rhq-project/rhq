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
package org.rhq.enterprise.gui.inventory.resource;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

public class ResourceTypeUIBean {
    private ResourceType type;

    public ResourceTypeUIBean() {
        type = EnterpriseFacesContextUtility.getResourceTypeIfExists();
    }

    public String getName() {
        return (type == null) ? null : type.getName();
    }
}