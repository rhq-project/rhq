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
package org.rhq.enterprise.gui.legacy.portlet.savedqueries;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility.ChartUtility;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class ModifyAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        PropertiesForm pForm = (PropertiesForm) form;
        ActionForward forward = checkSubmit(request, mapping, form);
        String returnString = RetCodeConstants.SUCCESS_URL;
        if (forward != null) {
            return forward;
        }

        if (!pForm.isDisplayOnDash()) {
            DashboardUtils.removePortlet(user, pForm.getPortletName());
        }

        String[] charts = pForm.getCharts();

        if (charts != null) {
            ChartUtility chartUtility = new ChartUtility( user );
//            String userCharts = user.getPreference(Constants.USER_DASHBOARD_CHARTS);
            for (int i = 0; i < charts.length; i++) {
                chartUtility.remove( charts[i]);
            }

            returnString = "remove";
        }

        user.persistPreferences();
        return mapping.findForward(returnString);
    }
}