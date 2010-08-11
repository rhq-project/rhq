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
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class GroupActivationContext extends AbstractResourceOrGroupActivationContext {
    private ResourceGroup group;
    transient EnumSet<ResourceTypeFacet> facets;
    transient EnumSet<Permission> resourcePermissions;

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();

    public GroupActivationContext(Subject subject, ResourceGroup group) {
        super(subject);
        this.group = group;
    }

    @Override
    public ResourceType getResourceType() {
        return this.group.getResourceType();
    }

    @Override
    public EnumSet<ResourceTypeFacet> getFacets() {
        // lazy load
        if (this.facets == null) {
            ResourceFacets resourceFacets = this.resourceTypeManager.getResourceFacets(getResourceType().getId());
            this.facets = EnumSet.copyOf(resourceFacets.getFacets());
        }
        return this.facets;
    }

    public EnumSet<Permission> getResourcePermissions() {
        // lazy load
        if (this.resourcePermissions == null) {
            Set<Permission> perms = this.authorizationManager.getImplicitGroupPermissions(getSubject(), this.group
                .getId());
            this.resourcePermissions = EnumSet.copyOf(perms);
        }
        return this.resourcePermissions;
    }

    public Set<Resource> getResources() {
        // lazy load
        if (null == this.group.getExplicitResources()) {
            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.addFilterId(this.group.getId());
            criteria.fetchExplicitResources(true);
            List<ResourceGroup> resourceGroups = LookupUtil.getResourceGroupManager().findResourceGroupsByCriteria(
                this.getSubject(), criteria);
            if (!resourceGroups.isEmpty()) {
                this.group.setExplicitResources(resourceGroups.get(0).getExplicitResources());
            }
        }
        return this.group.getExplicitResources();
    }

    public ResourceGroup getGroup() {
        return group;
    }

}