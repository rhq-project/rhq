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

import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;

/**
 * An <code>Action</code> that retrieves data from the BizApp to facilitate display of the various pages that provide
 * metrics summaries.
 */
public class MetricDisplayRangeFormPrepareAction extends WorkflowPrepareAction {
    protected final Log log = LogFactory.getLog(MetricDisplayRangeFormPrepareAction.class);

    /**
     * Retrieve data needed to display a Metrics Display Form. Respond to certain button clicks that alter the form
     * display.
     */
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        MetricDisplayRangeForm rangeForm = (MetricDisplayRangeForm) form;
        try {
            WebUser user = SessionUtils.getWebUser(request.getSession());
            MeasurementPreferences preferences = user.getMeasurementPreferences();
            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

            if (rangeForm.isResetClicked()) {
                rangeForm.setRn(rangePreferences.lastN);
                rangeForm.setRu(rangePreferences.unit);
                if (rangePreferences.readOnly) {
                    rangeForm.setA(MetricDisplayRangeForm.ACTION_DATE_RANGE);
                } else {
                    rangeForm.setA(MetricDisplayRangeForm.ACTION_LASTN);
                }
            }

            rangeForm.populateStartDate(new Date(rangePreferences.begin), request.getLocale());
            rangeForm.populateEndDate(new Date(rangePreferences.end), request.getLocale());
        } catch (IllegalArgumentException ioe) {
            throw new ServletException(ioe);
        }

        // blank range number if it's set to 0
        if ((rangeForm.getRn() != null) && (rangeForm.getRn() == 0)) {
            rangeForm.setRn(null);
        }

        return null;
    }
}