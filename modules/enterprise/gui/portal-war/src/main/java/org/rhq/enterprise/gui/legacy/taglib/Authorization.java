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
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class Authorization extends ConditionalTagSupport {

    private static final long serialVersionUID = 1L;

    private String permission;

    private enum Context {
        Group, Resource, Global;
    }

    @Override
    protected boolean condition() throws JspTagException {
        try {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
            Subject user = WebUtility.getSubject(request);

            if (isSuperuserCheck()) {
                return authorizationManager.isSystemSuperuser(user);
            }

            Permission permission = getPermissionEnum();

            if (user == null) {
                return false; // cannot authorize a non-authenticated user
            }

            Context context = Context.Global;
            int resourceId = getResourceId(request);
            if (resourceId != 0) {
                context = Context.Resource;
            }
            int groupId = getResourceGroupId(request);
            if (groupId != 0) {
                context = Context.Group;
            }

            if (context == Context.Resource) {
                return authorizationManager.hasResourcePermission(user, permission, resourceId);
            } else if (context == Context.Group) {
                return authorizationManager.hasGroupPermission(user, permission, groupId);
            } else if (context == Context.Global) {
                return authorizationManager.hasGlobalPermission(user, permission);
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
        Integer id = WebUtility.getResourceId(request);
        if (id == null) {
            return 0;
        }
        return id.intValue();
    }

    private int getResourceGroupId(HttpServletRequest request) throws JspTagException {
        Integer groupId = WebUtility.getResourceGroupId(request);
        if (groupId == null) {
            return 0;
        }
        return groupId.intValue();
    }

    private boolean isSuperuserCheck() {
        return permission != null && permission.toLowerCase().equals("superuser");
    }

    private Permission getPermissionEnum() throws JspTagException {
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
