/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.gwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;

@RemoteServiceRelativePath("MeasurementChartsGWTService")
public interface MeasurementChartsGWTService extends RemoteService {

    ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(EntityContext context, String viewName)
        throws RuntimeException;

    ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(EntityContext context,
        int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly) throws RuntimeException;

    ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForResource(int resourceId, String viewName)
        throws RuntimeException;

    ArrayList<MetricDisplaySummary> getMetricDisplaySummariesForResource(int resourceId, int[] measurementScheduleIds,
        long begin, long end) throws RuntimeException;

    Map<MeasurementDefinition, List<MetricDisplaySummary>> getMetricDisplaySummariesForMetricsCompare(
        int[] resourceIds, int[] measurementDefinitionIds, long begin, long end) throws RuntimeException;

}
