/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import static org.rhq.core.domain.util.PageOrdering.DESC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.datastax.driver.core.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Hours;
import org.joda.time.Minutes;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.server.metrics.domain.AggregatedNumericMetric;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
public class MetricsServer {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final Log log = LogFactory.getLog(MetricsServer.class);

    private DateTimeService dateTimeService = new DateTimeService();

    private Session session;

    private MetricsDAO dao;

    public void setSession(Session session) {
        this.session = session;
        dao = new MetricsDAO(session);
    }

    public RawNumericMetric findLatestValueForResource(int scheduleId) {
        List<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, DESC, 1);

        if (metrics.isEmpty()) {
            return null;
        }

        return metrics.get(0);
    }

    public List<MeasurementDataNumericHighLowComposite> findDataForResource(int scheduleId, long beginTime,
        long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            List<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
            return createRawComposites(metrics, beginTime, endTime);
        }

        MetricsTable table = getTable(begin);
        List<AggregatedNumericMetric> metrics = dao.findAggregateMetrics(table, scheduleId, beginTime, endTime);

        return createComposites(metrics, beginTime, endTime);
    }

    public List<MeasurementDataNumericHighLowComposite> findDataForGroup(List<Integer> scheduleIds, long beginTime,
        long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            List<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
            return createRawComposites(metrics, beginTime, endTime);
        }

        MetricsTable table = getTable(begin);
        List<AggregatedNumericMetric> metrics = dao.findAggregateMetrics(table, scheduleIds, beginTime, endTime);

        return createComposites(metrics, beginTime, endTime);
    }

    public AggregatedNumericMetric getSummaryAggregate(int scheduleId, long beginTime, long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            List<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
            return createSummaryRawAggregate(metrics, beginTime);
        }

        MetricsTable table = getTable(begin);
        List<AggregatedNumericMetric> metrics = dao.findAggregateMetrics(table, scheduleId, beginTime, endTime);

        return createSummaryAggregate(metrics, beginTime);
    }

    public AggregatedNumericMetric getSummaryAggregate(List<Integer> scheduleIds, long beginTime, long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(new DateTime(beginTime))) {
            List<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
            return createSummaryRawAggregate(metrics, beginTime);
        }

        MetricsTable table = getTable(begin);
        List<AggregatedNumericMetric> metrics = dao.findAggregateMetrics(table, scheduleIds, beginTime, endTime);

        return createSummaryAggregate(metrics, beginTime);
    }

    private List<MeasurementDataNumericHighLowComposite> createRawComposites(List<RawNumericMetric> metrics,
        long beginTime, long endTime) {
        Buckets buckets = new Buckets(beginTime, endTime);
        for (RawNumericMetric metric : metrics) {
            buckets.insert(metric.getTimestamp(), metric.getValue(), metric.getValue(), metric.getValue());
        }

        List<MeasurementDataNumericHighLowComposite> data = new ArrayList<MeasurementDataNumericHighLowComposite>();
        for (int i = 0; i < buckets.getNumDataPoints(); ++i) {
            Buckets.Bucket bucket = buckets.get(i);
            data.add(new MeasurementDataNumericHighLowComposite(bucket.getStartTime(), bucket.getAvg(),
                bucket.getMax(), bucket.getMin()));
        }
        return data;
    }

    private AggregatedNumericMetric createSummaryRawAggregate(List<RawNumericMetric> metrics, long beginTime) {
        if (metrics.isEmpty()) {
            // We do not care about the scheudule id here so can just use a dummy value of zero.
            return new AggregatedNumericMetric(0, Double.NaN, Double.NaN,Double.NaN, beginTime);
        }
        return calculateAggregatedRaw(metrics, beginTime);
    }

    private AggregatedNumericMetric createSummaryAggregate(List<AggregatedNumericMetric> metrics, long beginTime) {
        if (metrics.isEmpty()) {
            // We do not care about the scheudule id here so can just use a dummy value of zero.
            return new AggregatedNumericMetric(0, Double.NaN, Double.NaN,Double.NaN, beginTime);
        }
        return calculateAggregate(metrics, beginTime);
    }

    private List<MeasurementDataNumericHighLowComposite> createComposites(List<AggregatedNumericMetric> metrics,
        long beginTime, long endTime) {
        Buckets buckets = new Buckets(beginTime, endTime);
        for (AggregatedNumericMetric metric : metrics) {
            buckets.insert(metric.getTimestamp(), metric.getAvg(), metric.getMin(), metric.getMax());
        }

        List<MeasurementDataNumericHighLowComposite> data = new ArrayList<MeasurementDataNumericHighLowComposite>();
        for (int i = 0; i < buckets.getNumDataPoints(); ++i) {
            Buckets.Bucket bucket = buckets.get(i);
            data.add(new MeasurementDataNumericHighLowComposite(bucket.getStartTime(), bucket.getAvg(),
                bucket.getMax(), bucket.getMin()));
        }
        return data;
    }

    public void addNumericData(Set<MeasurementDataNumeric> dataSet) {
        Set<MeasurementDataNumeric> updates = dao.insertRawMetrics(dataSet, MetricsTable.RAW.getTTL());
        updateMetricsIndex(updates);
    }

    void updateMetricsIndex(Set<MeasurementDataNumeric> rawMetrics) {
        Map<Integer, Long> updates = new TreeMap<Integer, Long>();
        for (MeasurementDataNumeric rawMetric : rawMetrics) {
            updates.put(rawMetric.getScheduleId(), new DateTime(rawMetric.getTimestamp()).hourOfDay().roundFloorCopy()
                .getMillis());
        }
        dao.updateMetricsIndex(MetricsTable.ONE_HOUR, updates);
    }

    public List<AggregatedNumericMetric> calculateAggregates() {
        // We first query the metrics index table to determine which schedules have data to
        // be aggregated. Then we retrieve the metric data and aggregate or compress the
        // data, writing the compressed values into the next wider (i.e., longer life span
        // for data) bucket/table. At this point we remove the index entries for the data
        // that has already been processed. We currently purge the entire row in the index
        // table. We can safely do this entire work flow is single threaded. It might make
        // sense to perform the deletes in a more granular fashion to avoid concurrency
        // issues in the future. The last step in the work flow is to update the metrics
        // index for the newly persisted aggregates.

        // TODO deleteMetricsIndexEntries should take a list of schedule ids
        // MetricsDAO.deleteMetricsIndexEntries deletes the entire row, but we probably do
        // not want to delete each column unless and until we verify that the data for the
        // schedule id in that column has in fact been aggregated. It might be better for
        // deleteMetricsIndexEntries to take a list of schedule ids to purge.

        List<AggregatedNumericMetric> newOneHourAggregates = null;

        List<AggregatedNumericMetric> updatedSchedules = aggregateRawData();
        newOneHourAggregates = updatedSchedules;
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(MetricsTable.ONE_HOUR);
            updateMetricsIndex(MetricsTable.SIX_HOUR, updatedSchedules, Minutes.minutes(60 * 6));
        }

        updatedSchedules = calculateAggregates(MetricsTable.ONE_HOUR, MetricsTable.SIX_HOUR, Minutes.minutes(60 * 6));
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(MetricsTable.SIX_HOUR);
            updateMetricsIndex(MetricsTable.TWENTY_FOUR_HOUR, updatedSchedules, Hours.hours(24).toStandardMinutes());
        }

        updatedSchedules = calculateAggregates(MetricsTable.SIX_HOUR, MetricsTable.TWENTY_FOUR_HOUR, Hours.hours(24)
            .toStandardMinutes());
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(MetricsTable.TWENTY_FOUR_HOUR);
        }

        return newOneHourAggregates;
    }

    private void updateMetricsIndex(MetricsTable bucket, List<AggregatedNumericMetric> metrics, Minutes interval) {
        Map<Integer, Long> updates = new TreeMap<Integer, Long>();
        for (AggregatedNumericMetric metric : metrics) {
            updates.put(metric.getScheduleId(),
                dateTimeService.getTimeSlice(new DateTime(metric.getTimestamp()), interval).getMillis());
        }
        dao.updateMetricsIndex(bucket, updates);
    }

    private List<AggregatedNumericMetric> aggregateRawData() {
        List<MetricsIndexEntry> indexEntries = dao.findMetricsIndexEntries(MetricsTable.ONE_HOUR);
        List<AggregatedNumericMetric> oneHourMetrics = new ArrayList<AggregatedNumericMetric>();

        for (MetricsIndexEntry indexEntry : indexEntries) {
            DateTime startTime = indexEntry.getTime();
            DateTime endTime = startTime.plusMinutes(60);

            List<RawNumericMetric> rawMetrics = dao.findRawMetrics(indexEntry.getScheduleId(), startTime.getMillis(),
                endTime.getMillis());
            AggregatedNumericMetric aggregatedRaw = calculateAggregatedRaw(rawMetrics, startTime.getMillis());
            aggregatedRaw.setScheduleId(indexEntry.getScheduleId());
            oneHourMetrics.add(aggregatedRaw);
        }

        List<AggregatedNumericMetric> updatedSchedules = dao.insertAggregates(MetricsTable.ONE_HOUR, oneHourMetrics,
            MetricsTable.ONE_HOUR.getTTL());
        return updatedSchedules;
    }

    private AggregatedNumericMetric calculateAggregatedRaw(List<RawNumericMetric> rawMetrics, long timestamp) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        double value;

        for (RawNumericMetric metric : rawMetrics) {
            value = metric.getValue();
            if (count == 0) {
                min = value;
                max = min;
            }
            if (value < min) {
                min = value;
            } else if (value > max) {
                max = value;
            }
            mean.add(value);
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregatedNumericMetric(0, mean.getArithmeticMean(), min, max, timestamp);
    }

    private List<AggregatedNumericMetric> calculateAggregates(MetricsTable fromColumnFamily,
        MetricsTable toColumnFamily, Minutes nextInterval) {

        List<MetricsIndexEntry> indexEntries = dao.findMetricsIndexEntries(toColumnFamily);
        List<AggregatedNumericMetric> toMetrics = new ArrayList<AggregatedNumericMetric>();

        DateTime currentHour = getCurrentHour();
        DateTimeComparator dateTimeComparator = DateTimeComparator.getInstance();

        for (MetricsIndexEntry indexEntry : indexEntries) {
            DateTime startTime = indexEntry.getTime();
            DateTime endTime = startTime.plus(nextInterval);

            if (dateTimeComparator.compare(currentHour, endTime) < 0) {
                continue;
            }

            List<AggregatedNumericMetric> metrics = dao.findAggregateMetrics(fromColumnFamily,
                indexEntry.getScheduleId(), startTime.getMillis(), endTime.getMillis());
            AggregatedNumericMetric aggregatedMetric = calculateAggregate(metrics, startTime.getMillis());
            aggregatedMetric.setScheduleId(indexEntry.getScheduleId());
            toMetrics.add(aggregatedMetric);
        }

        List<AggregatedNumericMetric> updatedSchedules = dao.insertAggregates(toColumnFamily, toMetrics,
            toColumnFamily.getTTL());
        return updatedSchedules;
    }

    private AggregatedNumericMetric calculateAggregate(List<AggregatedNumericMetric> metrics, long timestamp) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();

        for (AggregatedNumericMetric metric : metrics) {
            if (count == 0) {
                min = metric.getMin();
                max = metric.getMax();
            }
            if (metric.getMin() < min) {
                min = metric.getMin();
            } else if (metric.getMax() > max) {
                max = metric.getMax();
            }
            mean.add(metric.getAvg());
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregatedNumericMetric(0, mean.getArithmeticMean(), min, max, timestamp);
    }

    private MetricsTable getTable(DateTime begin) {
        MetricsTable table;
        if (dateTimeService.isIn1HourDataRange(begin)) {
            table = MetricsTable.ONE_HOUR;
        } else if (dateTimeService.isIn6HourDataRnage(begin)) {
            table = MetricsTable.SIX_HOUR;
        } else {
            table = MetricsTable.TWENTY_FOUR_HOUR;
        }
        return table;
    }

//    public void addTraitData(Set<MeasurementDataTrait> dataSet) {
//        Mutator<Integer> mutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
//        Mutator<Integer> indexMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
//
//        for (MeasurementDataTrait trait : dataSet) {
//            mutator.addInsertion(
//                trait.getScheduleId(),
//                traitsCF,
//                HFactory.createColumn(trait.getTimestamp(), trait.getValue(), DateTimeService.ONE_YEAR,
//                    LongSerializer.get(), StringSerializer.get()));
//
//            Composite composite = new Composite();
//            composite.addComponent(trait.getTimestamp(), LongSerializer.get());
//            composite.addComponent(trait.getScheduleId(), IntegerSerializer.get());
//            composite.addComponent(trait.getDefinitionId(), IntegerSerializer.get());
//            composite.addComponent(trait.getDisplayType().ordinal(), IntegerSerializer.get());
//            composite.addComponent(trait.getDisplayName(), StringSerializer.get());
//
//            indexMutator.addInsertion(trait.getResourceId(), resourceTraitsCF,
//                HFactory.createColumn(composite, trait.getValue(), CompositeSerializer.get(), StringSerializer.get()));
//        }
//
//        mutator.execute();
//        indexMutator.execute();
//    }

//    public void addCallTimeData(Set<CallTimeData> callTimeDatas) {
//    }

    protected DateTime getCurrentHour() {
        DateTime now = new DateTime();
        return now.hourOfDay().roundFloorCopy();
    }
}
