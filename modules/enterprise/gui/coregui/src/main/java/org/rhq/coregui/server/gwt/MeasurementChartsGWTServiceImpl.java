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
package org.rhq.coregui.server.gwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.coregui.client.gwt.MeasurementChartsGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class MeasurementChartsGWTServiceImpl extends AbstractGWTServiceImpl implements MeasurementChartsGWTService {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_VIEW_NAME = "Default";

    private MeasurementChartsManagerLocal chartsManager = LookupUtil.getMeasurementChartsManager();

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(EntityContext context,
        String viewName) throws RuntimeException {
        try {
            if (viewName == null) {
                viewName = DEFAULT_VIEW_NAME;
            }
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(
                chartsManager.getMetricDisplaySummariesForCompatibleGroup(getSessionSubject(), context, viewName));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForCompatibleGroup1");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(EntityContext context,
        int[] defIds, long begin, long end, boolean enabledOnly) throws RuntimeException {
        Long now = System.currentTimeMillis();
        if (now < end) {
            // we can't foretell the future (this may be caused by different timezone on client's)
            end = now;
        }
        try {
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(
                chartsManager.getMetricDisplaySummariesForCompatibleGroup(getSessionSubject(), context, defIds, begin,
                    end, enabledOnly));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForCompatibleGroup2");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForResource(int resourceId, String viewName)
        throws RuntimeException {
        try {
            if (viewName == null) {
                viewName = DEFAULT_VIEW_NAME;
            }
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(
                chartsManager.getMetricDisplaySummariesForResource(getSessionSubject(), resourceId, viewName));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForResource1");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForResource(int resourceId, int[] schedIds,
        long begin, long end) throws RuntimeException {
        Long now = System.currentTimeMillis();
        if (now < end) {
            // we can't foretell the future (this may be caused by different timezone on client's)
            end = now;
        }
        try {
            ArrayList<MetricDisplaySummary> list = new ArrayList<MetricDisplaySummary>(
                chartsManager.getMetricDisplaySummariesForResource(getSessionSubject(), resourceId, schedIds, begin,
                    end));
            return SerialUtility.prepare(list, "MeasurementCharts.getMetricDisplaySummariesForResource2");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Map<MeasurementDefinition, List<MetricDisplaySummary>> getMetricDisplaySummariesForMetricsCompare(
        int[] resourceIds, int[] measurementDefinitionIds, long begin, long end) throws RuntimeException {
        Long now = System.currentTimeMillis();
        if (now < end) {
            // we can't foretell the future (this may be caused by different timezone on client's)
            end = now;
        }
        try {
            Map<MeasurementDefinition, List<MetricDisplaySummary>> map = chartsManager
                .getMetricDisplaySummariesForMetricsCompare(getSessionSubject(), resourceIds, measurementDefinitionIds,
                    begin, end);
            return SerialUtility.prepare(map, "MeasurementCharts.getMetricDisplaySummariesForMetricsCompare");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}
