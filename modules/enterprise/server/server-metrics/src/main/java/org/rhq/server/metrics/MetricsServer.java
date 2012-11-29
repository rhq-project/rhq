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

package org.rhq.server.metrics;

import static org.rhq.server.metrics.MetricsDAO.ONE_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.SIX_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.TWENTY_FOUR_HOUR_METRICS_TABLE;

import java.math.BigDecimal;
import java.math.MathContext;
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
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;

/**
 * @author John Sanda
 */
public class MetricsServer {

    private static final int DEFAULT_PAGE_SIZE = 200;

    public static int RAW_TTL = Days.days(7).toStandardSeconds().getSeconds();

    private final Log log = LogFactory.getLog(MetricsServer.class);

    private DateTimeService dateTimeService = new DateTimeService();

    private Session session;

    public void setSession(Session session) {
        this.session = session;
    }

    public List<MeasurementDataNumericHighLowComposite> findDataForResource(int scheduleId, long beginTime,
        long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            return findRawDataForResource(scheduleId, beginTime, endTime);
        }

        if (dateTimeService.isIn1HourDataRange(begin)) {
            return findAggregateDataForResource(scheduleId, beginTime, endTime, ONE_HOUR_METRICS_TABLE);
        }

        if (dateTimeService.isIn6HourDataRnage(begin)) {
            return findAggregateDataForResource(scheduleId, beginTime, endTime, SIX_HOUR_METRICS_TABLE);
        }

        return null;
    }

    private List<MeasurementDataNumericHighLowComposite> findRawDataForResource(int scheduleId, long beginTime,
        long endTime) {
        MetricsDAO dao = new MetricsDAO(session);
        Buckets buckets = new Buckets(beginTime, endTime);

        List<RawNumericMetric> rawMetrics = dao.findRawMetrics(scheduleId, new DateTime(beginTime),
            new DateTime(endTime));
        for (RawNumericMetric rawMetric : rawMetrics) {
            buckets.insert(rawMetric.getTimestamp(), rawMetric.getValue());
        }

        List<MeasurementDataNumericHighLowComposite> data = new ArrayList<MeasurementDataNumericHighLowComposite>();
        for (int i = 0; i < buckets.getNumDataPoints(); ++i) {
            Buckets.Bucket bucket = buckets.get(i);
            data.add(new MeasurementDataNumericHighLowComposite(bucket.getStartTime(), bucket.getAvg(),
                bucket.getMax(), bucket.getMin()));
        }
        return data;
    }

    private List<MeasurementDataNumericHighLowComposite> findAggregateDataForResource(int scheduleId, long beginTime,
        long endTime, String columnFamily) {
        MetricsDAO dao = new MetricsDAO(session);
        Buckets buckets = new Buckets(beginTime, endTime);

        List<AggregatedNumericMetric> metrics = dao.findAggregateMetrics(columnFamily, scheduleId,
            new DateTime(beginTime), new DateTime(endTime));
        for (AggregatedNumericMetric metric : metrics) {
            buckets.insert(metric.getTimestamp(), metric.getAvg());
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
        MetricsDAO dao = new MetricsDAO(session);
        Set<MeasurementDataNumeric> updates = dao.insertRawMetrics(dataSet, RAW_TTL);
        updateMetricsIndex(updates);
    }

    void updateMetricsIndex(Set<MeasurementDataNumeric> rawMetrics) {
        MetricsDAO dao = new MetricsDAO(session);
        Map<Integer, DateTime> updates = new TreeMap<Integer, DateTime>();
        for (MeasurementDataNumeric rawMetric : rawMetrics) {
            updates.put(rawMetric.getScheduleId(), new DateTime(rawMetric.getTimestamp()).hourOfDay().roundFloorCopy());
        }
        dao.updateMetricsIndex(ONE_HOUR_METRICS_TABLE, updates);
    }

    public void calculateAggregates() {
        MetricsDAO dao = new MetricsDAO(session);

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

        List<AggregatedNumericMetric> updatedSchedules = aggregateRawData();
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);
            updateMetricsIndex(SIX_HOUR_METRICS_TABLE, updatedSchedules, Minutes.minutes(60 * 6));
        }

        updatedSchedules = calculateAggregates(ONE_HOUR_METRICS_TABLE, SIX_HOUR_METRICS_TABLE, Minutes.minutes(60 * 6),
            DateTimeService.ONE_MONTH);
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(SIX_HOUR_METRICS_TABLE);
            updateMetricsIndex(TWENTY_FOUR_HOUR_METRICS_TABLE, updatedSchedules, Hours.hours(24).toStandardMinutes());
        }

        updatedSchedules = calculateAggregates(SIX_HOUR_METRICS_TABLE, TWENTY_FOUR_HOUR_METRICS_TABLE,
            Hours.hours(24).toStandardMinutes(), DateTimeService.ONE_YEAR);
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(TWENTY_FOUR_HOUR_METRICS_TABLE);
        }
    }

    private void updateMetricsIndex(String bucket, List<AggregatedNumericMetric> metrics, Minutes interval) {
        MetricsDAO dao = new MetricsDAO(session);
        Map<Integer, DateTime> updates = new TreeMap<Integer, DateTime>();
        for (AggregatedNumericMetric metric : metrics) {
            updates.put(metric.getScheduleId(), dateTimeService.getTimeSlice(new DateTime(metric.getTimestamp()),
                interval));
        }
        dao.updateMetricsIndex(bucket, updates);
    }

    private List<AggregatedNumericMetric> aggregateRawData() {
       MetricsDAO dao = new MetricsDAO(session);
        List<MetricsIndexEntry> indexEntries = dao.findMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);
        List<AggregatedNumericMetric> oneHourMetrics = new ArrayList<AggregatedNumericMetric>();

        for (MetricsIndexEntry indexEntry : indexEntries) {
            DateTime startTime = indexEntry.getTime();
            DateTime endTime = startTime.plusMinutes(60);

            List<RawNumericMetric> rawMetrics = dao.findRawMetrics(indexEntry.getScheduleId(), startTime, endTime);

            double min = Double.NaN;
            double max = min;
            double sum = 0;
            int count = 0;
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
                sum += value;
                ++count;
            }
            double avg = divide(sum, count);
            oneHourMetrics.add(new AggregatedNumericMetric(indexEntry.getScheduleId(), avg, min, max,
                startTime.getMillis()));
        }

        List<AggregatedNumericMetric> updatedSchedules = dao.insertAggregates(ONE_HOUR_METRICS_TABLE,
            oneHourMetrics);
        return updatedSchedules;
    }

    private List<AggregatedNumericMetric> calculateAggregates(String fromColumnFamily, String toColumnFamily,
        Minutes nextInterval, int ttl) {

        MetricsDAO dao = new MetricsDAO(session);
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
                indexEntry.getScheduleId(), startTime, endTime);
            double min = Double.NaN;
            double max = min;
            double sum = 0;
            int count = 0;

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
                sum += metric.getAvg();
                ++count;
            }
            double avg = divide(sum, count);
            toMetrics.add(new AggregatedNumericMetric(indexEntry.getScheduleId(), avg, min, max,
                startTime.getMillis()));
        }

        List<AggregatedNumericMetric> updatedSchedules = dao.insertAggregates(toColumnFamily, toMetrics);
        return updatedSchedules;
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

    static double divide(double dividend, int divisor) {
        return new BigDecimal(Double.toString(dividend)).divide(new BigDecimal(Integer.toString(divisor)),
            MathContext.DECIMAL64).doubleValue();
    }

    double avg(double... values) {
        BigDecimal sum = new BigDecimal("0.00");
        for (double value : values) {
            sum = sum.add(new BigDecimal(Double.toString(value)));
        }
        BigDecimal avg = sum.divide(new BigDecimal(Integer.toString(values.length), MathContext.DECIMAL64));
        return avg.doubleValue();
    }

    protected DateTime getCurrentHour() {
        DateTime now = new DateTime();
        return now.hourOfDay().roundFloorCopy();
    }

    private HColumn<Composite, Double> createAvgColumn(DateTime timestamp, double value, int ttl) {
        return createAggregateColumn(AggregateType.AVG, timestamp, value, ttl);
    }

    private HColumn<Composite, Double> createMaxColumn(DateTime timestamp, double value, int ttl) {
        return createAggregateColumn(AggregateType.MAX, timestamp, value, ttl);
    }

    private HColumn<Composite, Double> createMinColumn(DateTime timestamp, double value, int ttl) {
        return createAggregateColumn(AggregateType.MIN, timestamp, value, ttl);
    }

    private HColumn<Composite, Double> createAggregateColumn(AggregateType type, DateTime timestamp, double value,
                                                             int ttl) {
        Composite composite = new Composite();
        composite.addComponent(timestamp.getMillis(), LongSerializer.get());
        composite.addComponent(type.ordinal(), IntegerSerializer.get());
        return HFactory.createColumn(composite, value, ttl, CompositeSerializer.get(), DoubleSerializer.get());
    }

}
