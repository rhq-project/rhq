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
import org.joda.time.Duration;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
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

    private MetricsConfiguration configuration;

    public void setSession(Session session) {
        this.session = session;
        dao = new MetricsDAO(session);
    }

    public void setConfiguration(MetricsConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public RawNumericMetric findLatestValueForResource(int scheduleId) {
        Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, DESC, 1);

        if (!metrics.iterator().hasNext()) {
            return null;
        }

        return metrics.iterator().next();
    }

    public Iterable<MeasurementDataNumericHighLowComposite> findDataForResource(int scheduleId, long beginTime,
        long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
            return createRawComposites(metrics, beginTime, endTime);
        }

        MetricsTable table = getTable(begin);
        Iterable<AggregateNumericMetric> metrics = dao.findAggregateMetrics(table, scheduleId, beginTime, endTime);

        return createComposites(metrics, beginTime, endTime);
    }

    public List<MeasurementDataNumericHighLowComposite> findDataForGroup(List<Integer> scheduleIds, long beginTime,
        long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
            return createRawComposites(metrics, beginTime, endTime);
        }

        MetricsTable table = getTable(begin);
        Iterable<AggregateNumericMetric> metrics = dao.findAggregateMetrics(table, scheduleIds, beginTime, endTime);

        return createComposites(metrics, beginTime, endTime);
    }

    public AggregateNumericMetric getSummaryAggregate(int scheduleId, long beginTime, long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
            return createSummaryRawAggregate(metrics, beginTime);
        }

        MetricsTable table = getTable(begin);
        Iterable<AggregateNumericMetric> metrics = dao.findAggregateMetrics(table, scheduleId, beginTime, endTime);

        return createSummaryAggregate(metrics, beginTime);
    }

    public AggregateNumericMetric getSummaryAggregate(List<Integer> scheduleIds, long beginTime, long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(new DateTime(beginTime))) {
            Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
            return createSummaryRawAggregate(metrics, beginTime);
        }

        MetricsTable table = getTable(begin);
        Iterable<AggregateNumericMetric> metrics = dao.findAggregateMetrics(table, scheduleIds, beginTime, endTime);

        return createSummaryAggregate(metrics, beginTime);
    }

    private List<MeasurementDataNumericHighLowComposite> createRawComposites(Iterable<RawNumericMetric> metrics,
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

    private AggregateNumericMetric createSummaryRawAggregate(Iterable<RawNumericMetric> metrics, long beginTime) {
        if (!metrics.iterator().hasNext()) {
            // We do not care about the scheudule id here so can just use a dummy value of zero.
            return new AggregateNumericMetric(0, Double.NaN, Double.NaN,Double.NaN, beginTime);
        }
        return calculateAggregatedRaw(metrics, beginTime);
    }

    private AggregateNumericMetric createSummaryAggregate(Iterable<AggregateNumericMetric> metrics, long beginTime) {
        if (!metrics.iterator().hasNext()) {
            // We do not care about the scheudule id here so can just use a dummy value of zero.
            return new AggregateNumericMetric(0, Double.NaN, Double.NaN,Double.NaN, beginTime);
        }
        return calculateAggregate(metrics, beginTime);
    }

    private List<MeasurementDataNumericHighLowComposite> createComposites(Iterable<AggregateNumericMetric> metrics,
        long beginTime, long endTime) {
        Buckets buckets = new Buckets(beginTime, endTime);
        for (AggregateNumericMetric metric : metrics) {
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
        Set<MeasurementDataNumeric> updates = dao.insertRawMetrics(dataSet, configuration.getRawTTL());
        updateMetricsIndex(updates);
    }

    void updateMetricsIndex(Set<MeasurementDataNumeric> rawMetrics) {
        Map<Integer, Long> updates = new TreeMap<Integer, Long>();
        for (MeasurementDataNumeric rawMetric : rawMetrics) {
            updates.put(rawMetric.getScheduleId(), dateTimeService.getTimeSlice(
                new DateTime(rawMetric.getTimestamp()), configuration.getRawTimeSliceDuration()).getMillis());
        }
        dao.updateMetricsIndex(MetricsTable.ONE_HOUR, updates);
    }

    public Iterable<AggregateNumericMetric> calculateAggregates() {
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

        Iterable<AggregateNumericMetric> newOneHourAggregates = null;

        Iterable<AggregateNumericMetric> updatedSchedules = aggregateRawData();
        newOneHourAggregates = updatedSchedules;
        if (updatedSchedules.iterator().hasNext()) {
            dao.deleteMetricsIndexEntries(MetricsTable.ONE_HOUR);
            updateMetricsIndex(MetricsTable.SIX_HOUR, updatedSchedules, configuration.getOneHourTimeSliceDuration());
        }

        updatedSchedules = calculateAggregates(MetricsTable.ONE_HOUR, MetricsTable.SIX_HOUR,
            configuration.getOneHourTimeSliceDuration());
        if (updatedSchedules.iterator().hasNext()) {
            dao.deleteMetricsIndexEntries(MetricsTable.SIX_HOUR);
            updateMetricsIndex(MetricsTable.TWENTY_FOUR_HOUR, updatedSchedules,
                configuration.getSixHourTimeSliceDuration());
        }

        updatedSchedules = calculateAggregates(MetricsTable.SIX_HOUR, MetricsTable.TWENTY_FOUR_HOUR,
            configuration.getSixHourTimeSliceDuration());
        if (updatedSchedules.iterator().hasNext()) {
            dao.deleteMetricsIndexEntries(MetricsTable.TWENTY_FOUR_HOUR);
        }

        return newOneHourAggregates;
    }

    private void updateMetricsIndex(MetricsTable bucket, Iterable<AggregateNumericMetric> metrics, Duration duration) {
        Map<Integer, Long> updates = new TreeMap<Integer, Long>();
        for (AggregateNumericMetric metric : metrics) {
            updates.put(metric.getScheduleId(),
                dateTimeService.getTimeSlice(new DateTime(metric.getTimestamp()), duration).getMillis());
        }
        dao.updateMetricsIndex(bucket, updates);
    }

    private Iterable<AggregateNumericMetric> aggregateRawData() {
        Iterable<MetricsIndexEntry> indexEntries = dao.findMetricsIndexEntries(MetricsTable.ONE_HOUR);
        List<AggregateNumericMetric> oneHourMetrics = new ArrayList<AggregateNumericMetric>();

        for (MetricsIndexEntry indexEntry : indexEntries) {
            DateTime startTime = indexEntry.getTime();
            DateTime endTime = startTime.plusMinutes(60);

            Iterable<RawNumericMetric> rawMetrics = dao.findRawMetrics(indexEntry.getScheduleId(),
                startTime.getMillis(), endTime.getMillis());
            AggregateNumericMetric aggregatedRaw = calculateAggregatedRaw(rawMetrics, startTime.getMillis());
            aggregatedRaw.setScheduleId(indexEntry.getScheduleId());
            oneHourMetrics.add(aggregatedRaw);
        }

        List<AggregateNumericMetric> updatedSchedules = dao.insertAggregates(MetricsTable.ONE_HOUR, oneHourMetrics,
            MetricsTable.ONE_HOUR.getTTL());
        return updatedSchedules;
    }

    private AggregateNumericMetric calculateAggregatedRaw(Iterable<RawNumericMetric> rawMetrics, long timestamp) {
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
        return new AggregateNumericMetric(0, mean.getArithmeticMean(), min, max, timestamp);
    }

    private List<AggregateNumericMetric> calculateAggregates(MetricsTable fromTable,
        MetricsTable toTable, Duration nextDuration) {

        Iterable<MetricsIndexEntry> indexEntries = dao.findMetricsIndexEntries(toTable);
        List<AggregateNumericMetric> toMetrics = new ArrayList<AggregateNumericMetric>();

        DateTime currentHour = getCurrentHour();
        DateTimeComparator dateTimeComparator = DateTimeComparator.getInstance();

        for (MetricsIndexEntry indexEntry : indexEntries) {
            DateTime startTime = indexEntry.getTime();
            DateTime endTime = startTime.plus(nextDuration);

            if (dateTimeComparator.compare(currentHour, endTime) < 0) {
                continue;
            }

            Iterable<AggregateNumericMetric> metrics = dao.findAggregateMetrics(fromTable,
                indexEntry.getScheduleId(), startTime.getMillis(), endTime.getMillis());
            AggregateNumericMetric aggregatedMetric = calculateAggregate(metrics, startTime.getMillis());
            aggregatedMetric.setScheduleId(indexEntry.getScheduleId());
            toMetrics.add(aggregatedMetric);
        }

        List<AggregateNumericMetric> updatedSchedules = dao.insertAggregates(toTable, toMetrics,
            toTable.getTTL());
        return updatedSchedules;
    }

    private AggregateNumericMetric calculateAggregate(Iterable<AggregateNumericMetric> metrics, long timestamp) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();

        for (AggregateNumericMetric metric : metrics) {
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
        return new AggregateNumericMetric(0, mean.getArithmeticMean(), min, max, timestamp);
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
