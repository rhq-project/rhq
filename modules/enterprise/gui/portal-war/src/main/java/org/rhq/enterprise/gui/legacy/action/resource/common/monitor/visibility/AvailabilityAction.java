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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.clientapi.util.units.UnitNumber;
import org.rhq.core.clientapi.util.units.UnitsConstants;
import org.rhq.core.clientapi.util.units.UnitsFormat;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.MetricRangePreferences;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityPoint;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>TilesAction</code> that retrieves metric data to facilitate display of the availability bar. This code was
 * moved from org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility.CurrentHealthAction
 */
public class AvailabilityAction extends TilesAction {
    protected final Log log = LogFactory.getLog(AvailabilityAction.class);

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Get the resource availability
        AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();

        int resourceId = RequestUtils.getResourceId(request);
        try {
            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

            List<AvailabilityPoint> data = availabilityManager.getAvailabilitiesForResource(user.getSubject(),
                resourceId, rangePreferences.begin, rangePreferences.end, DefaultConstants.DEFAULT_CHART_POINTS);

            request.setAttribute(AttrConstants.AVAILABILITY_METRICS_ATTR, data);
            request.setAttribute(AttrConstants.AVAIL_METRICS_ATTR, getFormattedAvailability(data));
        } catch (Exception e) {
            // No utilization metric
            log.debug(MeasurementConstants.CAT_AVAILABILITY + " not found for " + resourceId);
        }

        return null;
    }

    protected String getFormattedAvailability(List<AvailabilityPoint> values) {
        double sum = 0;
        int count = 0;

        for (AvailabilityPoint ap : values) {
            if (ap.isKnown()) {
                count++;
                if (ap.getAvailabilityType() == AvailabilityType.UP) {
                    sum++;
                }
            }
        }

        // by the logic above, if sum is zero then count is also zero
        // so, shortcut the result as 0 if this is the case, otherwise result sum / count
        double result = ((sum == 0) ? 0 : (sum / count));

        UnitNumber average = new UnitNumber(result, UnitsConstants.UNIT_PERCENTAGE);
        return UnitsFormat.format(average).toString();
    }
}