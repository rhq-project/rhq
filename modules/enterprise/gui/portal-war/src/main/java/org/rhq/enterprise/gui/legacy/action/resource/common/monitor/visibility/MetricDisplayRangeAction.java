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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

/**
 * A <code>BaseAction</code> that handles metrics-specific form gestures.
 */
public class MetricDisplayRangeAction extends BaseAction {
    private final Log log = LogFactory.getLog(MetricDisplayRangeAction.class.getName());

    /**
     * Modify the metrics summary display as specified in the given <code>MetricDisplayRangeForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        MetricDisplayRangeForm displayForm = (MetricDisplayRangeForm) form;

        // Redirect user back to where they came if cancelled
        if (displayForm.isCancelClicked()) {
            return returnSuccess(request, mapping);
        }

        ActionForward forward = checkSubmit(request, mapping, form, true);
        if (forward != null) {
            return forward;
        }

        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();

        if (displayForm.isLastnSelected()) {
            Integer lastN = displayForm.getRn();
            Integer unit = displayForm.getRu();

            log.trace("updating metric display .. lastN [" + lastN + "] .. unit [" + unit + "]");
            preferences.setPreference(WebUserPreferences.PREF_METRIC_RANGE_LASTN, lastN);
            preferences.setPreference(WebUserPreferences.PREF_METRIC_RANGE_UNIT, unit);
            preferences.setPreference(WebUserPreferences.PREF_METRIC_RANGE, null);

            // set simple mode
            preferences.setPreference(WebUserPreferences.PREF_METRIC_RANGE_RO, Boolean.FALSE);
        } else if (displayForm.isDateRangeSelected()) {
            Date begin = displayForm.getStartDate();
            Date end = displayForm.getEndDate();

            List<Long> range = new ArrayList<Long>();
            range.add(begin.getTime());
            range.add(end.getTime());

            log.trace("updating metric display date range [" + begin + ":" + end + "]");
            preferences.setPreference(WebUserPreferences.PREF_METRIC_RANGE, range);
            preferences.setPreference(WebUserPreferences.PREF_METRIC_RANGE_LASTN, null);
            preferences.setPreference(WebUserPreferences.PREF_METRIC_RANGE_UNIT, null);

            // set advanced mode
            preferences.setPreference(WebUserPreferences.PREF_METRIC_RANGE_RO, Boolean.TRUE);
        } else {
            throw new ServletException("invalid date range action [" + displayForm.getA() + "] selected");
        }

        log.trace("Invoking setUserPrefs" + " in MetricDisplayRangeAction " + " for " + user.getId() + " at "
            + System.currentTimeMillis() + " user.prefs = " + user.getPreferences());
        preferences.persistPreferences();

        // XXX: assume return path is set, don't use forward params
        return returnSuccess(request, mapping);
    }
}