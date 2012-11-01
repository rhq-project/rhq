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

import static me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality.EQUAL;
import static me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality.LESS_THAN_EQUAL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Hours;
import org.joda.time.Minutes;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.SliceQuery;

/**
 * @author John Sanda
 */
public class MetricsServer {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final Log log = LogFactory.getLog(MetricsServer.class);

    private Cluster cluster;

    private String keyspaceName;

    private String rawMetricsDataCF;

    private String oneHourMetricsDataCF;

    private String sixHourMetricsDataCF;

    private String twentyFourHourMetricsDataCF;

    private String metricsQueueCF;

    private String traitsCF;

    private String resourceTraitsCF;

    private Keyspace keyspace;

    private DateTimeService dateTimeService = new DateTimeService();

    private DataSource cassandraDS;

    // These property getters/setters are here right now primarily to facilitate
    // testing.

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public String getRawMetricsDataCF() {
        return rawMetricsDataCF;
    }

    public void setRawMetricsDataCF(String rawMetricsDataCF) {
        this.rawMetricsDataCF = rawMetricsDataCF;
    }

    public String getOneHourMetricsDataCF() {
        return oneHourMetricsDataCF;
    }

    public void setOneHourMetricsDataCF(String oneHourMetricsDataCF) {
        this.oneHourMetricsDataCF = oneHourMetricsDataCF;
    }

    public String getSixHourMetricsDataCF() {
        return sixHourMetricsDataCF;
    }

    public void setSixHourMetricsDataCF(String sixHourMetricsDataCF) {
        this.sixHourMetricsDataCF = sixHourMetricsDataCF;
    }

    public String getTwentyFourHourMetricsDataCF() {
        return twentyFourHourMetricsDataCF;
    }

    public void setTwentyFourHourMetricsDataCF(String twentyFourHourMetricsDataCF) {
        this.twentyFourHourMetricsDataCF = twentyFourHourMetricsDataCF;
    }

    public String getMetricsQueueCF() {
        return metricsQueueCF;
    }

    public void setMetricsQueueCF(String metricsQueueCF) {
        this.metricsQueueCF = metricsQueueCF;
    }

    public String getTraitsCF() {
        return traitsCF;
    }

    public void setTraitsCF(String traitsCF) {
        this.traitsCF = traitsCF;
    }

    public String getResourceTraitsCF() {
        return resourceTraitsCF;
    }

    public void setResourceTraitsCF(String resourceTraitsCF) {
        this.resourceTraitsCF = resourceTraitsCF;
    }

    public Keyspace getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    public void setCassandraDS(DataSource dataSource) {
        cassandraDS = dataSource;
    }

    public List<MeasurementDataNumericHighLowComposite> findDataForContext(Subject subject, EntityContext entityContext,
        MeasurementSchedule schedule, long beginTime, long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            return findRawDataForContext(schedule, beginTime, endTime);
        }

        if (dateTimeService.isIn1HourDataRange(begin)) {
            return findAggregateDataForContext(schedule, beginTime, endTime, oneHourMetricsDataCF);
        }

        if (dateTimeService.isIn6HourDataRnage(begin)) {
            return findAggregateDataForContext(schedule, beginTime, endTime, sixHourMetricsDataCF);
        }

        return null;
    }

    private List<MeasurementDataNumericHighLowComposite> findRawDataForContext(MeasurementSchedule schedule,
        long beginTime, long endTime) {
        SliceQuery<Integer, Long, Double> rawDataQuery = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
            LongSerializer.get(), DoubleSerializer.get());
        rawDataQuery.setColumnFamily(rawMetricsDataCF);
        rawDataQuery.setKey(schedule.getId());
        rawDataQuery.setRange(beginTime, endTime, false, DEFAULT_PAGE_SIZE);

        ColumnSliceIterator<Integer, Long, Double> rawDataIterator = new ColumnSliceIterator<Integer, Long, Double>(
            rawDataQuery, beginTime, endTime, false);
        Buckets buckets = new Buckets(beginTime, endTime);
        HColumn<Long, Double> rawColumn = null;

        while (rawDataIterator.hasNext()) {
            rawColumn = rawDataIterator.next();
            buckets.insert(rawColumn.getName(), rawColumn.getValue());
        }

        List<MeasurementDataNumericHighLowComposite> data = new ArrayList<MeasurementDataNumericHighLowComposite>();
        for (int i = 0; i < buckets.getNumDataPoints(); ++i) {
            Buckets.Bucket bucket = buckets.get(i);
            data.add(new MeasurementDataNumericHighLowComposite(bucket.getStartTime(), bucket.getAvg(),
                bucket.getMax(), bucket.getMin()));
        }
        return data;
    }

    private List<MeasurementDataNumericHighLowComposite> findAggregateDataForContext(MeasurementSchedule schedule,
        long beginTime, long endTime, String columnFamily) {
        SliceQuery<Integer, Composite, Double> dataQuery = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
            CompositeSerializer.get(), DoubleSerializer.get());
        dataQuery.setColumnFamily(oneHourMetricsDataCF);
        dataQuery.setKey(schedule.getId());

        Composite begin = new Composite();
        begin.addComponent(beginTime, LongSerializer.get(), ComparatorType.LONGTYPE.getTypeName(), EQUAL);

        Composite end = new Composite();
        end.addComponent(endTime, LongSerializer.get(), ComparatorType.LONGTYPE.getTypeName(), LESS_THAN_EQUAL);
        dataQuery.setRange(begin, end, true, DEFAULT_PAGE_SIZE);

        ColumnSliceIterator<Integer, Composite, Double> dataIterator = new ColumnSliceIterator<Integer, Composite, Double>(
            dataQuery, begin, end, false);
        Buckets buckets = new Buckets(beginTime, endTime);
        HColumn<Composite, Double> column = null;

        while (dataIterator.hasNext()) {
            column = dataIterator.next();
            Composite columnName = column.getName();
            if (AggregateType.valueOf(columnName.get(1, IntegerSerializer.get())) != AggregateType.AVG) {
                continue;
            }
            buckets.insert((Long) columnName.get(0, LongSerializer.get()), column.getValue());
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
        Map<Integer, DateTime> updates = new TreeMap<Integer, DateTime>();
        Mutator<Integer> mutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = cassandraDS.getConnection();
            String sql = "INSERT INTO raw_metrics (schedule_id, time, value) VALUES (?, ?, ?)";
            statement = connection.prepareStatement(sql);

            for (MeasurementDataNumeric data : dataSet) {
                statement.setInt(1, data.getScheduleId());
                statement.setDate(2, new java.sql.Date(data.getTimestamp()));
                statement.setDouble(3, data.getValue());

                statement.executeUpdate();
                updates.put(data.getScheduleId(), new DateTime(data.getTimestamp()).hourOfDay().roundFloorCopy());
                //statement.addBatch();
            }
//            statement.executeUpdate();
            updateMetricsQueue(oneHourMetricsDataCF, updates);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }

//        for (MeasurementDataNumeric data : dataSet) {
//            updates.put(data.getScheduleId(), new DateTime(data.getTimestamp()).hourOfDay().roundFloorCopy());
//            mutator.addInsertion(
//                data.getScheduleId(),
//                rawMetricsDataCF,
//                HFactory.createColumn(data.getTimestamp(), data.getValue(), DateTimeService.SEVEN_DAYS,
//                    LongSerializer.get(), DoubleSerializer.get()));
//        }
//
//        mutator.execute();
//
//        updateMetricsQueue(oneHourMetricsDataCF, updates);
    }

    public void calculateAggregates() {
        Map<Integer, DateTime> updatedSchedules = aggregateRawData();
        updateMetricsQueue(sixHourMetricsDataCF, updatedSchedules);

        updatedSchedules = calculateAggregates(oneHourMetricsDataCF, sixHourMetricsDataCF, Minutes.minutes(60 * 6),
            Hours.hours(24).toStandardMinutes(), DateTimeService.ONE_MONTH);
        updateMetricsQueue(twentyFourHourMetricsDataCF, updatedSchedules);

        calculateAggregates(sixHourMetricsDataCF, twentyFourHourMetricsDataCF, Hours.hours(24).toStandardMinutes(),
            Hours.hours(24).toStandardMinutes(), DateTimeService.ONE_YEAR);
    }

    private Map<Integer, DateTime> aggregateRawData() {
        Map<Integer, DateTime> updatedSchedules = new TreeMap<Integer, DateTime>();

        SliceQuery<String, Composite, Integer> queueQuery = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
            new CompositeSerializer().get(), IntegerSerializer.get());
        queueQuery.setColumnFamily(metricsQueueCF);
        queueQuery.setKey(oneHourMetricsDataCF);

        ColumnSliceIterator<String, Composite, Integer> queueIterator = new ColumnSliceIterator<String, Composite, Integer>(
            queueQuery, (Composite) null, (Composite) null, false);

        Mutator<Integer> mutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        Mutator<String> queueMutator = HFactory.createMutator(keyspace, StringSerializer.get());

        while (queueIterator.hasNext()) {
            HColumn<Composite, Integer> queueColumn = queueIterator.next();
            Integer scheduleId = queueColumn.getName().get(1, IntegerSerializer.get());
            Long timestamp = queueColumn.getName().get(0, LongSerializer.get());
            DateTime startTime = new DateTime(timestamp);
            DateTime endTime = new DateTime(timestamp).plus(Minutes.minutes(60));

            SliceQuery<Integer, Long, Double> rawDataQuery = HFactory.createSliceQuery(keyspace,
                IntegerSerializer.get(), LongSerializer.get(), DoubleSerializer.get());
            rawDataQuery.setColumnFamily(rawMetricsDataCF);
            rawDataQuery.setKey(scheduleId);

            ColumnSliceIterator<Integer, Long, Double> rawDataIterator = new ColumnSliceIterator<Integer, Long, Double>(
                rawDataQuery, startTime.getMillis(), endTime.getMillis(), false);
            rawDataIterator.hasNext();

            HColumn<Long, Double> rawDataColumn = rawDataIterator.next();
            double min = rawDataColumn.getValue();
            double max = min;
            double sum = max;
            int count = 1;

            while (rawDataIterator.hasNext()) {
                rawDataColumn = rawDataIterator.next();
                if (rawDataColumn.getValue() < min) {
                    min = rawDataColumn.getValue();
                } else if (rawDataColumn.getValue() > max) {
                    max = rawDataColumn.getValue();
                }
                sum += rawDataColumn.getValue();
                ++count;
            }

            double avg = sum / count;

            mutator.addInsertion(scheduleId, oneHourMetricsDataCF,
                createAvgColumn(startTime, avg, DateTimeService.TWO_WEEKS));
            mutator.addInsertion(scheduleId, oneHourMetricsDataCF,
                createMaxColumn(startTime, max, DateTimeService.TWO_WEEKS));
            mutator.addInsertion(scheduleId, oneHourMetricsDataCF,
                createMinColumn(startTime, min, DateTimeService.TWO_WEEKS));

            updatedSchedules.put(scheduleId, dateTimeService.getTimeSlice(startTime, Minutes.minutes(60 * 6)));

            queueMutator.addDeletion(oneHourMetricsDataCF, metricsQueueCF, queueColumn.getName(),
                CompositeSerializer.get());
        }
        mutator.execute();
        queueMutator.execute();

        return updatedSchedules;
    }

    private Map<Integer, DateTime> calculateAggregates(String fromColumnFamily, String toColumnFamily,
        Minutes interval, Minutes nextInterval, int ttl) {
        DateTime currentHour = getCurrentHour();
        DateTimeComparator dateTimeComparator = DateTimeComparator.getInstance();

        Map<Integer, DateTime> updatedSchedules = new TreeMap<Integer, DateTime>();

        SliceQuery<String, Composite, Integer> queueQuery = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
            new CompositeSerializer().get(), IntegerSerializer.get());
        queueQuery.setColumnFamily(metricsQueueCF);
        queueQuery.setKey(toColumnFamily);

        ColumnSliceIterator<String, Composite, Integer> queueIterator = new ColumnSliceIterator<String, Composite, Integer>(
            queueQuery, (Composite) null, (Composite) null, false);

        Mutator<Integer> mutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        Mutator<String> queueMutator = HFactory.createMutator(keyspace, StringSerializer.get());

        while (queueIterator.hasNext()) {
            HColumn<Composite, Integer> queueColumn = queueIterator.next();
            Integer scheduleId = queueColumn.getName().get(1, IntegerSerializer.get());
            Long timestamp = queueColumn.getName().get(0, LongSerializer.get());
            DateTime startTime = new DateTime(timestamp);
            DateTime endTime = new DateTime(timestamp).plus(interval);

            if (dateTimeComparator.compare(currentHour, endTime) < 0) {
                continue;
            }

            Composite startColKey = new Composite();
            startColKey.addComponent(startTime.getMillis(), LongSerializer.get());

            Composite endColKey = new Composite();
            endColKey.addComponent(endTime.getMillis(), LongSerializer.get());

            SliceQuery<Integer, Composite, Double> fromColumnFamilyQuery = HFactory.createSliceQuery(keyspace,
                IntegerSerializer.get(), CompositeSerializer.get(), DoubleSerializer.get());
            fromColumnFamilyQuery.setColumnFamily(fromColumnFamily);
            fromColumnFamilyQuery.setKey(scheduleId);

            ColumnSliceIterator<Integer, Composite, Double> fromColumnFamilyIterator = new ColumnSliceIterator<Integer, Composite, Double>(
                fromColumnFamilyQuery, startColKey, endColKey, false);
            fromColumnFamilyIterator.hasNext();

            HColumn<Composite, Double> fromColumn = null;
            double min = 0;
            double max = 0;
            double sum = 0;
            int avgCount = 0;
            int minCount = 0;
            int maxCount = 0;

            while (fromColumnFamilyIterator.hasNext()) {
                fromColumn = fromColumnFamilyIterator.next();
                AggregateType type = AggregateType.valueOf(fromColumn.getName().get(1, IntegerSerializer.get()));

                switch (type) {
                case AVG:
                    sum += fromColumn.getValue();
                    avgCount++;
                    break;
                case MIN:
                    if (minCount == 0) {
                        min = fromColumn.getValue();
                    } else if (fromColumn.getValue() < min) {
                        min = fromColumn.getValue();
                    }
                    minCount++;
                    break;
                case MAX:
                    if (maxCount == 0) {
                        max = fromColumn.getValue();
                    } else if (fromColumn.getValue() > max) {
                        max = fromColumn.getValue();
                    }
                    maxCount++;
                    break;
                }
            }

            double avg = sum / avgCount;

            mutator.addInsertion(scheduleId, toColumnFamily, createAvgColumn(startTime, avg, ttl));
            mutator.addInsertion(scheduleId, toColumnFamily, createMaxColumn(startTime, max, ttl));
            mutator.addInsertion(scheduleId, toColumnFamily, createMinColumn(startTime, min, ttl));

            updatedSchedules.put(scheduleId, dateTimeService.getTimeSlice(startTime, nextInterval));

            queueMutator.addDeletion(toColumnFamily, metricsQueueCF, queueColumn.getName(), CompositeSerializer.get());
        }
        mutator.execute();
        queueMutator.execute();

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

    private MutationResult updateMetricsQueue(String columnFamily, Map<Integer, DateTime> updates) {
//        Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
//
//        for (Integer scheduleId : updates.keySet()) {
//            DateTime collectionTime = new DateTime(updates.get(scheduleId));
//            Composite composite = new Composite();
//            composite.addComponent(collectionTime.getMillis(), LongSerializer.get());
//            composite.addComponent(scheduleId, IntegerSerializer.get());
//            HColumn<Composite, Integer> column = HFactory.createColumn(composite, 0, CompositeSerializer.get(),
//                IntegerSerializer.get());
//            mutator.addInsertion(columnFamily, metricsQueueCF, column);
//        }
//
//        return mutator.execute();
        Connection connection = null;
        String errorMsg = "Cannot update " + columnFamily + " index for schedule ids " + updates.keySet();
        try {
            connection = cassandraDS.getConnection();
        } catch (SQLException e) {
            log.error("Failed to obtain connection", e);
            log.warn(errorMsg);
            return null;
        }
        String sql = "INSERT INTO " + metricsQueueCF + " (bucket, time, schedule_id, null_col) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
        } catch (SQLException e) {
            log.error("Failed to create prepared statement [" + sql + "]", e);
            log.warn(errorMsg);
            return null;
        }
        for (Integer scheduleId : updates.keySet()) {
            try {
                statement.setString(1, columnFamily);
                statement.setDate(2, new java.sql.Date(updates.get(scheduleId).getMillis()));
                statement.setInt(3, scheduleId);
                statement.setBoolean(4, false);

                statement.executeUpdate();
            } catch (SQLException e) {
                log.warn("Failed to update " + columnFamily + " index for " + scheduleId + " at time slice " +
                    updates.get(scheduleId));
            }
        }
        return null;
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
