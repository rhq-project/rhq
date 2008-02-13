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
package org.rhq.enterprise.gui.util;

import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class EnterpriseFacesContextUtility {
    public static WebUser getWebUser() {
        FacesContext facesContext = FacesContextUtility.getFacesContext();
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> sessionMap = externalContext.getSessionMap();
        return (WebUser) sessionMap.get(AttrConstants.WEBUSER_SES_ATTR);
    }

    /**
     * Returns information on the user that submitted the currently executing request. If none, an exception is thrown.
     *
     * @return Subject containing the user information
     */
    @NotNull
    public static Subject getSubject() {
        return getWebUser().getSubject();
    }

    /**
     * Returns the {@link Resource} associated with the current request. If none, an exception is thrown.
     *
     * @return resource associated with the request.
     */
    @NotNull
    public static Resource getResource() {
        ExternalContext externalContext = FacesContextUtility.getFacesContext().getExternalContext();
        Resource resource = (Resource) externalContext.getRequestMap().get(AttrConstants.RESOURCE_ATTR);
        if (resource == null) {
            int resourceId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.RESOURCE_ID_PARAM,
                Integer.class);

            // TODO: Instead call a manager method that returns a ResourceComposite, so we can stick the
            //       ResourceComposite in the request map, rather than just the Resource.
            resource = LookupUtil.getResourceManager().getResourceById(EnterpriseFacesContextUtility.getSubject(),
                resourceId);
            externalContext.getRequestMap().put(AttrConstants.RESOURCE_ATTR, resource);
        }

        return resource;
    }

    /**
     * Returns the {@link Resource} associated with the current request. If none, an exception is thrown.
     *
     * @return resource associated with the request.
     */
    @NotNull
    public static ResourceGroup getResourceGroup() {
        ExternalContext externalContext = FacesContextUtility.getFacesContext().getExternalContext();
        ResourceGroup resourceGroup = (ResourceGroup) externalContext.getRequestMap().get(
            AttrConstants.RESOURCE_GROUP_ATTR);
        if (resourceGroup == null) {
            int resourceGroupId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.GROUP_ID_PARAM,
                Integer.class);

            resourceGroup = LookupUtil.getResourceGroupManager().getResourceGroupById(getSubject(), resourceGroupId,
                null);
            externalContext.getRequestMap().put(AttrConstants.RESOURCE_GROUP_ATTR, resourceGroup);
        }

        return resourceGroup;
    }

    /**
     * Returns the {@link Resource} associated with the current request or <code>null</code> if the request is not
     * associated with a resource.
     *
     * @return resource associated with the request or <code>null</code> if none
     */
    @Nullable
    public static Resource getResourceIfExists() {
        try {
            return getResource();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the {@link ResourceType} associated with the current request. If none, an exception is thrown.
     *
     * @return ResourceType associated with the request.
     */
    @NotNull
    public static ResourceType getResourceType() {
        ExternalContext externalContext = FacesContextUtility.getFacesContext().getExternalContext();
        ResourceType resourceType = (ResourceType) externalContext.getRequestMap()
            .get(AttrConstants.RESOURCE_TYPE_ATTR);
        if (resourceType == null) {
            int resourceTypeId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.RESOURCE_TYPE_ID_PARAM,
                Integer.class);
            try {
                Subject subject = EnterpriseFacesContextUtility.getSubject();
                resourceType = LookupUtil.getResourceTypeManager().getResourceTypeById(subject, resourceTypeId);
                externalContext.getRequestMap().put(AttrConstants.RESOURCE_TYPE_ATTR, resourceType);
            } catch (ResourceTypeNotFoundException rtnfe) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "ResourceType with id=" + resourceTypeId
                    + " does not exist");
            }
        }

        return resourceType;
    }

    /**
     * Returns the {@link ResourceType} associated with the current request or <code>null</code> if the request is not
     * associated with a ResourceType.
     *
     * @return ResourceType associated with the request or <code>null</code> if none
     */
    @Nullable
    public static ResourceType getResourceTypeIfExists() {
        try {
            return getResourceType();
        } catch (Exception e) {
            return null;
        }
    }
}