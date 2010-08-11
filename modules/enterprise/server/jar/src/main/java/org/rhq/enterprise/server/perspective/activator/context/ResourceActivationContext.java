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
package org.rhq.enterprise.server.perspective.activator.context;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class ResourceActivationContext extends AbstractResourceOrGroupActivationContext {
    private Resource resource;
    private Set<Resource> resourceAsSet;
    transient EnumSet<ResourceTypeFacet> facets;
    transient EnumSet<Permission> resourcePermissions;

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();

    public ResourceActivationContext(Subject subject, Resource resource) {
        super(subject);
        this.resource = resource;
        this.resourceAsSet = new HashSet<Resource>(1);
        this.resourceAsSet.add(resource);
    }

    @Override
    public ResourceType getResourceType() {
        return this.resource.getResourceType();
    }

    @Override
    public EnumSet<ResourceTypeFacet> getFacets() {
        // lazy load
        if (this.facets == null) {
            ResourceFacets resourceFacets = this.resourceTypeManager.getResourceFacets(resource.getResourceType()
                .getId());
            this.facets = EnumSet.copyOf(resourceFacets.getFacets());
        }
        return this.facets;
    }

    @Override
    public boolean hasResourcePermission(Permission permission) {
        return hasGlobalPermission(Permission.MANAGE_INVENTORY) || getResourcePermissions().contains(permission);
    }

    @Override
    protected EnumSet<Permission> getResourcePermissions() {
        // lazy load
        if (this.resourcePermissions == null) {
            Set<Permission> perms = this.authorizationManager.getImplicitResourcePermissions(getSubject(),
                this.resource.getId());
            this.resourcePermissions = EnumSet.copyOf(perms);
        }
        return this.resourcePermissions;
    }

    public Resource getResource() {
        return resource;
    }

    public Set<Resource> getResources() {
        return this.resourceAsSet;
    }

}
