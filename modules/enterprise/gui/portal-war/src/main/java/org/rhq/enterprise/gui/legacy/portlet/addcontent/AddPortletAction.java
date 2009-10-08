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
package org.rhq.enterprise.gui.legacy.portlet.addcontent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.DashboardPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class AddPortletAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        PropertiesForm pForm = (PropertiesForm) form;
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getWebPreferences();

        if ((pForm.getPortlet() == null) || "bad".equals(pForm.getPortlet())) {
            return mapping.findForward(RetCodeConstants.SUCCESS_URL);
        }

        // add some portlet to the preferences
        DashboardPreferences dashboardPreferences = preferences.getDashboardPreferences();
        dashboardPreferences.addPortlet(pForm.getPortlet(), !pForm.isWide());
        preferences.setDashboardPreferences(dashboardPreferences);

        session.removeAttribute(Constants.USERS_SES_PORTAL);

        return mapping.findForward(RetCodeConstants.SUCCESS_URL);
    }
}