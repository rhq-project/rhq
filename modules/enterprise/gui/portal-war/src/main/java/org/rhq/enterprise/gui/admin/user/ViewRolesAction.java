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
package org.rhq.enterprise.gui.admin.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves the roles for a specific user.
 */
public class ViewRolesAction extends WorkflowPrepareAction {
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(ViewRolesAction.class.getName());
        Subject user = LookupUtil.getSubjectManager().getSubjectById(RequestUtils.getUserId(request));

        log.trace("getting roles for user [" + user + "]");

        PageControl pc = WebUtility.getPageControl(request);

        PageList<Role> roles = LookupUtil.getRoleManager().getRoles(user.getId(), pc);
        request.setAttribute(Constants.USER_ROLES_ATTR, roles);

        return null;
    }
}