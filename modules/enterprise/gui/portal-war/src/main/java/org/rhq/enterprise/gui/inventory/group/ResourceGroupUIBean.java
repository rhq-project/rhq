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
package org.rhq.enterprise.gui.inventory.group;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.action.resource.common.QuickFavoritesUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;

public class ResourceGroupUIBean {
    public static final String MANAGED_BEAN_NAME = "ResourceGroupUIBean";

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

    private ResourceGroup resourceGroup;
    private Double availability;
    private long count;
    private ResourcePermission permissions;
    private ResourceFacets facets; // this will only be non-null for compat groups
    private Boolean isFavorite; // true if this resource has been added to the favorites dashboard portlet
    private ResourceError invalidPluginConfigurationError;
    private Subject subject;

    public ResourceGroupUIBean() {
        this(lookupResourceGroup());
    }

    public ResourceGroupUIBean(ResourceGroupComposite resourceGroupComposite) {
        this(resourceGroupComposite, EnterpriseFacesContextUtility.getSubject());
    }

    public ResourceGroupUIBean(ResourceGroupComposite resourceGroupComposite, Subject subject) {
        this.resourceGroup = resourceGroupComposite.getResourceGroup();
        this.count = resourceGroupComposite.getMemberCount();
        this.availability = resourceGroupComposite.getAvailability();
        this.subject = subject;
        Set<Permission> permissions = LookupUtil.getAuthorizationManager().getImplicitGroupPermissions(subject,
            this.resourceGroup.getId());
        this.permissions = new ResourcePermission(permissions);
        if (this.resourceGroup.getGroupCategory() == GroupCategory.COMPATIBLE) {
            try {
                this.facets = this.resourceTypeManager.getResourceFacets(subject,
                        this.resourceGroup.getResourceType().getId());
            } catch (ResourceTypeNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getGroupId() {
        return this.resourceGroup.getId();
    }

    @NotNull
    public String getName() {
        return this.resourceGroup.getName();
    }

    @NotNull
    public String getGroupCategoryName() {
        return this.resourceGroup.getGroupCategory().name();
    }

    public String getDescription() {
        return this.resourceGroup.getDescription();
    }

    public long getCtime() {
        return this.resourceGroup.getCtime();
    }

    public long getMtime() {
        return this.resourceGroup.getMtime();
    }

    public Subject getModifiedBy() {
        return this.resourceGroup.getModifiedBy();
    }

    public String getLocation() {
        return this.resourceGroup.getLocation();
    }

    public Boolean getRecursive() {
        return this.resourceGroup.isRecursive();
    }

    public GroupDefinition getGroupDefinition() {
        return this.resourceGroup.getGroupDefinition();
    }

    public ResourcePermission getPermissions() {
        return this.permissions;
    }

    public ResourceFacets getFacets()
    {
        return this.facets;
    }

    @Nullable
    public ResourceError getInvalidPluginConfigurationError() {
        return this.invalidPluginConfigurationError;
    }

    public Double getAvailability() {
        return this.availability;
    }

    public ResourceType getResourceType() {
        return resourceGroup.getResourceType();
    }

    public long getGroupCount() {
        return this.count;
    }

    public Subject getOwner() {
        // @Todo add owner to the RHQ_resource_group_table
        //return this.resourceGroup.getOwner();

        // This is a temp impl, will use the line above when the table is updated
        return this.subject;
    }

    public boolean isFavorite() {
        if (this.isFavorite == null) {
            this.isFavorite = QuickFavoritesUtil.determineIfFavoriteGroup(FacesContextUtility.getRequest(),
                getGroupId());
        }

        return this.isFavorite;
    }

    private static ResourceGroupComposite lookupResourceGroup() {
        int resourceGroupId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.GROUP_ID_PARAM,
            Integer.class);
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        return LookupUtil.getResourceGroupManager().getResourceGroupWithAvailabilityById(subject, resourceGroupId);
    }
}