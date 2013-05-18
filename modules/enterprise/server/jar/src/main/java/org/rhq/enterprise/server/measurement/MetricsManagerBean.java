/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.enterprise.server.measurement;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.server.cassandra.SessionManagerBean;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
@Stateless
public class MetricsManagerBean implements MetricsManagerLocal {

    @EJB
    private SessionManagerBean sessionManager;

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void addNumericData(Set<MeasurementDataNumeric> data) {
        MetricsServer metricsServer = getMetricsServer();
        metricsServer.addNumericData(data);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Iterable<AggregateNumericMetric> calculateAggregates() {
        MetricsServer metricsServer = getMetricsServer();
        return metricsServer.calculateAggregates(System.currentTimeMillis());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MeasurementDataNumeric findLatestValueForResource(int scheduleId) {
        MetricsServer metricsServer = getMetricsServer();
        RawNumericMetric metric = metricsServer.findLatestValueForResource(scheduleId);

        if (metric == null) {
            return null;
        }
        return new MeasurementDataNumeric(metric.getTimestamp(), scheduleId, metric.getValue());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Iterable<MeasurementDataNumericHighLowComposite> findDataForResource(int scheduleId, long beginTime,
        long endTime) {
        MetricsServer metricsServer = getMetricsServer();
        return metricsServer.findDataForResource(scheduleId, beginTime, endTime);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MeasurementAggregate getSummaryAggregate(int scheduleId, long beginTime, long endTime) {
        MetricsServer metricsServer = getMetricsServer();
        AggregateNumericMetric summary = metricsServer.getSummaryAggregate(scheduleId, beginTime, endTime);

        return new MeasurementAggregate(summary.getMin(), summary.getAvg(), summary.getMax());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MeasurementAggregate getSummaryAggregate(List<Integer> scheduleIds, long beginTime, long endTime) {
        MetricsServer metricsServer = getMetricsServer();
        AggregateNumericMetric summary = metricsServer.getSummaryAggregate(scheduleIds, beginTime, endTime);

        return new MeasurementAggregate(summary.getMin(), summary.getAvg(), summary.getMax());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<MeasurementDataNumericHighLowComposite> findDataForResourceGroup(List<Integer> scheduleIds,
        long beginTime, long endTime) {
        MetricsServer metricsServer = getMetricsServer();
        return metricsServer.findDataForGroup(scheduleIds, beginTime, endTime);
    }

    private MetricsServer getMetricsServer() {
        return sessionManager.getMetricsServer();
    }
}
