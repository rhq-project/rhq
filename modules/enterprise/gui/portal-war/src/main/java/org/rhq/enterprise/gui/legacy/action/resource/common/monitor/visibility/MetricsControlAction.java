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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.Map;

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
import org.rhq.enterprise.gui.legacy.WebUserPreferences.MetricRangePreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.BaseActionMapping;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

/**
 * A <code>BaseAction</code> that handles metrics control form submissions.
 */
public class MetricsControlAction extends BaseAction {
    private static Log log = LogFactory.getLog(MetricsControlAction.class.getName());

    // ---------------------------------------------------- Public Methods

    /**
     * Modify the metrics summary display as specified in the given <code>MetricsControlForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        MetricsControlForm controlForm = (MetricsControlForm) form;

        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        WebUserPreferences preferences = user.getPreferences();

        // See if this is part of a workflow
        if (mapping instanceof BaseActionMapping) {
            BaseActionMapping smap = (BaseActionMapping) mapping;
            String workflow = smap.getWorkflow();
            if (workflow != null) {
                SessionUtils.pushWorkflow(session, mapping, workflow);
            }
        }

        Map forwardParams = controlForm.getForwardParams();
        if (controlForm.isEditRangeClicked()) {
            return returnEditRange(request, mapping, forwardParams);
        } else if (controlForm.isAdvancedClicked()) {
            return returnEditRange(request, mapping, forwardParams);
        } else if (controlForm.isRangeClicked()) {
            Integer lastN = controlForm.getRn();
            Integer unit = controlForm.getRu();

            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
            rangePreferences.lastN = lastN;
            rangePreferences.unit = unit;
            rangePreferences.readOnly = false;
            preferences.setMetricRangePreferences(rangePreferences);

            preferences.persistPreferences();
        } else if (controlForm.isSimpleClicked()) {

            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
            rangePreferences.readOnly = false;
            preferences.setMetricRangePreferences(rangePreferences);

            preferences.persistPreferences();
        }

        // assume the return path has been set- don't use forwardParams
        return returnSuccess(request, mapping);
    }

    // ---------------------------------------------------- Private Methods

    private ActionForward returnEditRange(HttpServletRequest request, ActionMapping mapping, Map params)
        throws Exception {
        return constructForward(request, mapping, Constants.EDIT_RANGE_URL, params, NO_RETURN_PATH);
    }
}