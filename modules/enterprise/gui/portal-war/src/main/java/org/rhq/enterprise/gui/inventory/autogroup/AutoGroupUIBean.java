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
package org.rhq.enterprise.gui.inventory.autogroup;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.common.EntityContext;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

public class AutoGroupUIBean {
    public static final String MANAGED_BEAN_NAME = "AutoGroupUIBean";

    EntityContext context;
    private Resource parentResource;
    private ResourceType childResourceType;
    private Subject subject;

    public AutoGroupUIBean() {
        subject = EnterpriseFacesContextUtility.getSubject();
        context = WebUtility.getEntityContext();
        parentResource = LookupUtil.getResourceManager().getResourceById(subject, context.getParentResourceId());
        try {
            childResourceType = LookupUtil.getResourceTypeManager().getResourceTypeById(subject,
                context.getResourceTypeId());
        } catch (ResourceTypeNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public int getParentResourceId() {
        return this.parentResource.getId();
    }

    public String getParentResourceName() {
        return this.parentResource.getName();
    }

    public int getChildResourceTypeId() {
        return this.childResourceType.getId();
    }

    public String getChildResourceTypeName() {
        return this.childResourceType.getName();
    }

}