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
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;

public class AddPortletAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        WebUser user = (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);
        String portletName = request.getParameter(Constants.REM_PORTLET_PARAM);
        PropertiesForm pForm = (PropertiesForm) form;

        String preferences = Constants.DASHBOARD_DELIMITER;
        preferences += pForm.getPortlet();

        if ((pForm.getPortlet() == null) || "bad".equals(pForm.getPortlet())) {
            return mapping.findForward(RetCodeConstants.SUCCESS_URL);
        }

        if (pForm.isWide()) {
            user.setPreference(Constants.USER_PORTLETS_SECOND, user.getPreference(Constants.USER_PORTLETS_SECOND)
                + preferences);
        } else {
            user.setPreference(Constants.USER_PORTLETS_FIRST, user.getPreference(Constants.USER_PORTLETS_FIRST)
                + preferences);
        }

        LogFactory.getLog("user.preferences").trace(
            "Invoking setUserPrefs" + " in AddPortletAction " + " for " + user.getId() + " at "
                + System.currentTimeMillis() + " user.prefs = " + user.getPreferences());
        user.persistPreferences();

        session.removeAttribute(Constants.USERS_SES_PORTAL);

        return mapping.findForward(RetCodeConstants.SUCCESS_URL);
    }
}