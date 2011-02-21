/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;

import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementChartsGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class MeasurementChartsGWTServiceImpl extends AbstractGWTServiceImpl implements MeasurementChartsGWTService {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_VIEW_NAME = "Default";

    private MeasurementChartsManagerLocal chartsManager = LookupUtil.getMeasurementChartsManager();

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(int parent, int type, String viewName)
        throws RuntimeException {
        try {
            if (viewName == null) {
                viewName = DEFAULT_VIEW_NAME;
            }
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForAutoGroup(getSessionSubject(), parent, type, viewName));
            return SerialUtility.prepare(list, "MeasurementChartsManager.getMetricDisplaySummariesForAutoGroup");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(int groupId, String viewName)
        throws RuntimeException {
        try {
            if (viewName == null) {
                viewName = DEFAULT_VIEW_NAME;
            }
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForCompatibleGroup(getSessionSubject(), groupId, viewName));
            return SerialUtility.prepare(list, "MeasurementChartsManager.getMetricDisplaySummariesForCompatibleGroup");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForResource(int resourceId, String viewName)
        throws RuntimeException {
        try {
            if (viewName == null) {
                viewName = DEFAULT_VIEW_NAME;
            }
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(chartsManager
                .getMetricDisplaySummariesForResource(getSessionSubject(), resourceId, viewName));
            return SerialUtility.prepare(list, "MeasurementChartsManager.getMetricDisplaySummariesForResource");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }
}
