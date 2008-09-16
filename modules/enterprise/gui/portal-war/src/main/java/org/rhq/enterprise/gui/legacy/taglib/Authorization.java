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
package org.rhq.enterprise.gui.legacy.taglib;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class Authorization extends ConditionalTagSupport {

    private static final long serialVersionUID = 1L;

    private String permission;

    private enum Context {
        Resource, //
        ResourceGroup, // 
        None;
    }

    @Override
    protected boolean condition() throws JspTagException {
        try {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

            Subject user = RequestUtils.getSubject(request);
            Permission permission = getPermissionEnum(request);

            int objectId = 0;
            Context context = Context.None;
            try {
                objectId = getResourceId(request);
                context = Context.Resource;
            } catch (Exception e) {
                // might be OK, if this is some other context
            }

            try {
                objectId = getResourceGroupId(request);
                context = Context.ResourceGroup;
            } catch (Exception e) {
                // might be OK, if this is some other context
            }

            AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
            if (context == Context.Resource) {
                return authorizationManager.hasResourcePermission(user, permission, objectId);
            } else if (context == Context.ResourceGroup) {
                return authorizationManager.hasResourcePermission(user, permission, objectId);
            } else if (context == Context.None) {
                throw new JspTagException("Trying to determine permissions in an unknown context");
            } else {
                throw new JspTagException("Authorization tag does not yet support the context[" + context + "]");
            }

        } catch (JspTagException jte) {
            throw jte; // pass-through
        } catch (Exception e) {
            throw new JspTagException(e);
        }
    }

    private int getResourceId(HttpServletRequest request) throws JspTagException {
        try {
            return RequestUtils.getResourceId(request);
        } catch (Exception innerE) {
            throw new JspTagException("Trying to evaluate resource-level permissions in a non-resource context");
        }
    }

    private int getResourceGroupId(HttpServletRequest request) throws JspTagException {
        try {
            return RequestUtils.getGroupId(request);
        } catch (Exception innerE) {
            throw new JspTagException(
                "Trying to evaluate resource group-level permissions in a non-resource group context");
        }
    }

    private Permission getPermissionEnum(HttpServletRequest request) throws JspTagException {
        String permissionName = getPermission();

        try {
            return Permission.valueOf(permissionName.toUpperCase());
        } catch (Exception innerE) {
            throw new JspTagException("Invalid permission[" + permissionName + "]");
        }
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

}
