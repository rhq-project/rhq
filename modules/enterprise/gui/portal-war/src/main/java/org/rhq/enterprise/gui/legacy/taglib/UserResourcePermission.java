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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Check to see if the current user has a specific set of permissions relative to the current resource in scope.
 */
public class UserResourcePermission extends UserResourcePermissionParameters {
    //----------------------------------------------------instance variables
    Subject user;
    boolean debugger = false;

    private JspWriter output;

    //----------------------------------------------------constructors

    public UserResourcePermission() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Release tag state.
     */
    @Override
    public void release() {
        super.release();
    }

    //-------------------------------------------------------------  helper methods

    /**
     * evaulate the user, resource and required permission. to determine if the user had rightst ot perform permission
     * on resource populate scoped variable with true or false.
     *
     * @return
     *
     * @throws JspException
     */
    @Override
    public final int doStartTag() throws JspException {
        //make sure these attributes aren't already in the request.

        try {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            user = RequestUtils.getSubject(request);
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
            Resource resource = resourceManager.getResourceById(user, getResource());

            Set<Permission> permissions = LookupUtil.getAuthorizationManager().getImplicitResourcePermissions(user,
                resource.getId());
            ResourcePermission resourcePermission = null;
            if (LookupUtil.getAuthorizationManager().isInventoryManager(user)) {
                resourcePermission = new ResourcePermission(Permission.RESOURCE_ALL);
            } else {
                resourcePermission = new ResourcePermission(permissions);
            }

            request.setAttribute("resourcePermissions", resourcePermission);

            //have a debug option to output the values as html.
            if (debugger) {
                output = pageContext.getOut();
                output.print(resourcePermission);
            }
        } catch (Exception e) {
            throw new JspException(e);
        }

        return SKIP_BODY;
    }

    /**
     * @return
     *
     * @throws JspException
     */
    @Override
    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;
    }
}