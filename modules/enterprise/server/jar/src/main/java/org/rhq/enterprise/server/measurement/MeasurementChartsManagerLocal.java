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
package org.rhq.enterprise.server.measurement;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;

/**
 * @author Joseph Marques
 */
@Local
public interface MeasurementChartsManagerLocal {

    /**
     * @deprecated portal-war
     */
    @Deprecated
    List<MetricDisplaySummary> getMetricDisplaySummariesForMetrics(Subject subject, int resourceId, DataType dataType,
        long begin, long end, boolean narrowed, boolean enabledOnly) throws MeasurementException;

    /**
     * Get metric display summaries for a compatible group
     *
     * @param  subject
     * @param  context A resource group context
     * @param  measurementDefinitionIds
     * @param  begin
     * @param  end
     * @param  enabledOnly              only show results for metric that are actually enabled
     *
     * @return
     *
     * @throws MeasurementException
     */
    List<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(Subject subject, EntityContext context,
        int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly) throws MeasurementException;

    List<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(Subject subject, EntityContext context,
        String viewName);

    /**
     * Get metric display summaries for an autogroup.
     *
     * @param  subject
     * @param  autoGroupParentResourceId
     * @param  autoGroupChildResourceTypeId
     * @param  measurementDefinitionIds
     * @param  begin
     * @param  end
     * @param  enabledOnly                  only show results for metric that are actually enabled
     *
     * @return
     *
     * @throws MeasurementException
     * @deprecated portal-war only
     */
    @Deprecated
    List<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(Subject subject, int autoGroupParentResourceId,
        int autoGroupChildResourceTypeId, int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly)
        throws MeasurementException;

    /**
     * @deprecated portal-war only
     */
    @Deprecated
    List<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(Subject subject, int parent, int type,
        String viewName);

    List<MetricDisplaySummary> getMetricDisplaySummariesForResource(Subject subject, int resourceId,
        int[] measurementScheduleIds, long begin, long end) throws MeasurementException;

    List<MetricDisplaySummary> getMetricDisplaySummariesForResource(Subject subject, int resourceId, String viewName)
        throws MeasurementException;

    /**
     * Get metric display summaries for the resources and measurements that are passed
     *
     * @param  subject                  subject of the caller
     * @param  resourceIds              Array of resource Ids that were selected to compare
     * @param  measurementDefinitionIds Array of measurement Ids
     * @param  begin                    begin time for the display time range
     * @param  end                      end time for the displays time range
     *
     * @return Map<MeasurementDefinition, List<MetricDisplaySummary>> Map holds the Metric in the key, then the
     *         resources values in a List for the value.
     *
     * @throws MeasurementException throws Measurement exception
     *
     */
    Map<MeasurementDefinition, List<MetricDisplaySummary>> getMetricDisplaySummariesForMetricsCompare(Subject subject,
        int[] resourceIds, int[] measurementDefinitionIds, long begin, long end) throws MeasurementException;

}
