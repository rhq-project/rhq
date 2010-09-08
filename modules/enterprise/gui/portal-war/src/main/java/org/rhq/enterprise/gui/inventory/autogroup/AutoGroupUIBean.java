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

import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

public class AutoGroupUIBean {
    public static final String MANAGED_BEAN_NAME = "AutoGroupUIBean";

    EntityContext context;
    private Resource parentResource;
    private ResourceType childResourceType;
    private Subject subject;
    private boolean measurementManager;

    public AutoGroupUIBean() {
        subject = EnterpriseFacesContextUtility.getSubject();
        context = WebUtility.getEntityContext();
        parentResource = LookupUtil.getResourceManager().getResourceById(subject, context.getParentResourceId());
        try {
            childResourceType = LookupUtil.getResourceTypeManager().getResourceTypeById(subject, context.getResourceTypeId());
        } catch (ResourceTypeNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        this.measurementManager = determineMeasurementManager();
    }

    public boolean isMeasurementManager() {
        return this.measurementManager;
    }

    public Resource getParentResource() {
        return parentResource;
    }
    
    public ResourceType getChildResourceType() {
        return childResourceType;
    }
    
    /**
     * To manipulate autogroup schedules the subject must have measurement perms for each of the AG's
     * resources.  There is no simple query for this. Since this is rare and AG size is usually fairly small
     * we'll use a brute force way to determine this, get the AG resources and test each individually.<BR>
     * <BR>
     * Another approach would be to create and perform two queries, the first just a count of AG members, the
     * second a count of AG members with the measurement permission, then compare the counts.
     * 
     *  
     * @return
     */
    private boolean determineMeasurementManager() {
        AuthorizationManagerLocal authManager = LookupUtil.getAuthorizationManager();

        if (authManager.isInventoryManager(subject)) {
            return true;
        }

        List<Resource> resources = LookupUtil.getResourceGroupManager().findResourcesForAutoGroup(this.subject, this.getParentResource().getId(),
            this.getChildResourceType().getId());
        // Note, authManager does offer a single query solution for this but it has limits and is inefficient, for now let's
        // opt for more round trips on the assumption that the AG size is typically small.          
        for (Resource resource : resources) {
            if (!authManager.hasResourcePermission(subject, Permission.MANAGE_MEASUREMENTS, resource.getId())) {
                return false;
            }
        }

        return true;
    }
}