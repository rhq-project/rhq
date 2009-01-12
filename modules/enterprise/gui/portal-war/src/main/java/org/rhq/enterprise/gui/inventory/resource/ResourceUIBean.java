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

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.composite.ResourceAvailabilitySummary;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.clientapi.util.units.Formatter;
import org.rhq.core.clientapi.util.units.UnitsFormat;
import org.rhq.core.clientapi.util.units.UnitNumber;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.legacy.action.resource.common.QuickFavoritesUtil;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementFormatter;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A JSF managed bean that represents a RHQ resource. In addition to exposing the core Resource properties, it also
 * exposes the resource's parent, facets and the current user's implicit permissions for that resource. Note: This bean
 * is only intended for reading this information, not updating it.
 *
 * @author Ian Springer
 */
public class ResourceUIBean {
    public static final String MANAGED_BEAN_NAME = "ResourceUIBean";

    private Resource resource;
    private Resource parent;
    private ResourcePermission permissions;
    private ResourceFacets facets;
    private ResourceError invalidPluginConfigurationError;
    private Boolean isFavorite; // true if this resource has been added to the favorites dashboard portlet

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();

    public ResourceUIBean() {
        this(lookupResource());
    }

    public ResourceUIBean(Resource resource) {
        this(resource, EnterpriseFacesContextUtility.getSubject());
    }

    public ResourceUIBean(Resource resource, Subject subject) {
        this.resource = resource;
        this.parent = resourceManager.getParentResource(this.resource.getId());
        Set<Permission> resourcePerms = this.authorizationManager.getImplicitResourcePermissions(subject, this.resource
                .getId());
        this.permissions = new ResourcePermission(resourcePerms);
        try {
            this.facets = this.resourceTypeManager.getResourceFacets(subject, getResourceType().getId());
        } catch (ResourceTypeNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<ResourceError> errors = this.resourceManager.getResourceErrors(subject, this.resource.getId(),
                ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        if (errors.size() == 1) {
            this.invalidPluginConfigurationError = errors.get(0);
        }
    }

    public int getId() {
        return this.resource.getId();
    }

    public String getUuid() {
        return this.resource.getUuid();
    }

    @NotNull
    public String getName() {
        return this.resource.getName();
    }

    public String getResourceKey() {
        return this.resource.getResourceKey();
    }

    public ResourceType getResourceType() {
        return this.resource.getResourceType();
    }

    public InventoryStatus getInventoryStatus() {
        return this.resource.getInventoryStatus();
    }

    public boolean isConnected() {
        return this.resource.isConnected();
    }

    public String getVersion() {
        return this.resource.getVersion();
    }

    public String getDescription() {
        return this.resource.getDescription();
    }

    public long getCtime() {
        return this.resource.getCtime();
    }

    public long getMtime() {
        return this.resource.getMtime();
    }

    public Subject getModifiedBy() {
        return this.resource.getModifiedBy();
    }

    public String getLocation() {
        return this.resource.getLocation();
    }

    public Resource getParent() {
        return this.parent;
    }

    public ResourcePermission getPermissions() {
        return this.permissions;
    }

    public ResourceFacets getFacets() {
        return this.facets;
    }

    @Nullable
    public ResourceError getInvalidPluginConfigurationError() {
        return this.invalidPluginConfigurationError;
    }

    public boolean isFavorite() {
        if (this.isFavorite == null) {
            this.isFavorite = QuickFavoritesUtil.determineIfFavoriteResource(FacesContextUtility.getRequest());
        }

        return this.isFavorite;
    }

    public void setFavorite(boolean favorite) {

        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences preferences = user.getWebPreferences();

        int resourceId = EnterpriseFacesContextUtility.getResource().getId();


        String mode = FacesContextUtility.getRequiredRequestParameter("mode");

        if (mode.equals("add")) {
            if (!isFavorite) {

                // Add to favorites and save
                WebUserPreferences.FavoriteResourcePortletPreferences favoriteResourcePreferences = preferences
                        .getFavoriteResourcePortletPreferences();
                favoriteResourcePreferences.resourceIds.add(resourceId);
                preferences.setFavoriteResourcePortletPreferences(favoriteResourcePreferences);
            }
        } else if (mode.equals("remove")) {
            if (isFavorite) {
                // Remove from favorites and save
                WebUserPreferences.FavoriteResourcePortletPreferences favoriteResourcePreferences = preferences
                        .getFavoriteResourcePortletPreferences();
                favoriteResourcePreferences.resourceIds.remove(resourceId);
                preferences.setFavoriteResourcePortletPreferences(favoriteResourcePreferences);
            }
        }
        isFavorite = mode.equals("add");
        preferences.persistPreferences();
    }


    public AvailabilityType getAvailabilityType() {
        AvailabilityManagerLocal manager = LookupUtil.getAvailabilityManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        AvailabilityType avail = manager.getCurrentAvailabilityTypeForResource(subject, getId());
        return avail;
    }

    public ResourceAvailabilitySummary getAvailabilitySummary() {
        return resourceManager.getAvailabilitySummary(EnterpriseFacesContextUtility.getSubject(), getId());
    }

    private static Resource lookupResource() {
        int resourceId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.RESOURCE_ID_PARAM,
                Integer.class);

        // TODO: To be more efficient, instead call a manager method that returns a ResourceComposite.
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        return LookupUtil.getResourceManager().getResourceById(subject, resourceId);
    }
}