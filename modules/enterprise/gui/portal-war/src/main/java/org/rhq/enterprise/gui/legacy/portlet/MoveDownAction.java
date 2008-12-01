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
package org.rhq.enterprise.gui.legacy.portlet;

import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class MoveDownAction extends BaseAction {
    public static final String delim = DashboardUtils.DASHBOARD_DELIMITER;

    private static final Log log = LogFactory.getLog(MoveDownAction.class.getName());

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        String portlet = request.getParameter("portletName");

        if (!tryMoveDown(user, portlet, session, Constants.USER_PORTLETS_FIRST)
            && !tryMoveDown(user, portlet, session, Constants.USER_PORTLETS_SECOND)) {
            // Just log the error and don't do anything.
            log.error("Didn't find portlet " + portlet + " in any column");
        }

        return mapping.findForward(Constants.AJAX_URL);
    }

    private boolean tryMoveDown(WebUser user, String portlet, HttpSession session, String columnKey) throws Exception {
        WebUserPreferences preferences = user.getPreferences();

        String portlets = preferences.getPreferences().getSimple(columnKey).getStringValue();

        // portlet is not in this column
        if (portlets.indexOf(portlet) == -1) {
            return false;
        }

        // tokenize and reshuffle
        StringBuffer newColumn = new StringBuffer();
        StringTokenizer st = new StringTokenizer(portlets, delim);
        String token;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if (token.equals(portlet)) {
                if (!st.hasMoreTokens()) {
                    log.error("Cannot move portlet " + portlet + " any further down");
                    return true;
                }

                String nextToken = st.nextToken();
                newColumn.append(delim).append(nextToken).append(delim).append(token);
            } else {
                newColumn.append(delim).append(token);
            }
        }

        preferences.setPreference(columnKey, newColumn.toString());
        preferences.persistPreferences();
        session.removeAttribute(Constants.USERS_SES_PORTAL);
        return true;
    }
}