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

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.cassandra.CassandraException;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.bundle.DeploymentOptions;
import org.rhq.cassandra.bundle.EmbeddedDeployer;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHost;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.cassandra.service.KeyIterator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

/**
 * @author John Sanda
 */
public class MetricsServerTest {

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    private final String RAW_METRIC_DATA_CF = "raw_metrics";

    private final String ONE_HOUR_METRIC_DATA_CF = "one_hour_metric_data";

    private final String SIX_HOUR_METRIC_DATA_CF = "six_hour_metric_data";

    private final String TWENTY_FOUR_HOUR_METRIC_DATA_CF = "twenty_four_hour_metric_data";

    private final String METRICS_WORK_QUEUE_CF = "metrics_work_queue";

    private final String TRAITS_CF = "traits";

    private final String RESOURCE_TRAITS_CF = "resource_traits";

    private MetricsServer metricsServer;

    private Keyspace keyspace;

    @BeforeClass
    public void deployCluster() throws CassandraException {
        File basedir = new File("target");
        File clusterDir = new File(basedir, "cassandra");
        int numNodes = 2;

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir.getAbsolutePath());
        deploymentOptions.setNumNodes(numNodes);
        deploymentOptions.setLoggingLevel("DEBUG");

        EmbeddedDeployer deployer = new EmbeddedDeployer();
        deployer.setDeploymentOptions(deploymentOptions);
        deployer.deploy();

        List<CassandraHost> hosts = asList(new CassandraHost("127.0.0.1", 9160), new CassandraHost("127.0.0.2", 9160));
        ClusterInitService initService = new ClusterInitService();

        initService.waitForClusterToStart(hosts);
        initService.waitForSchemaAgreement("rhq", hosts);
    }

    @BeforeMethod
    public void initServer() throws Exception {
        Cluster cluster = HFactory.getOrCreateCluster("rhq", "127.0.0.1:9160");
        keyspace = HFactory.createKeyspace("rhq", cluster);

        metricsServer = new MetricsServer();
        metricsServer.setCluster(cluster);
        metricsServer.setKeyspace(keyspace);
        metricsServer.setRawMetricsDataCF(RAW_METRIC_DATA_CF);
        metricsServer.setOneHourMetricsDataCF(ONE_HOUR_METRIC_DATA_CF);
        metricsServer.setSixHourMetricsDataCF(SIX_HOUR_METRIC_DATA_CF);
        metricsServer.setTwentyFourHourMetricsDataCF(TWENTY_FOUR_HOUR_METRIC_DATA_CF);
        metricsServer.setMetricsQueueCF(METRICS_WORK_QUEUE_CF);
        metricsServer.setTraitsCF(TRAITS_CF);
        metricsServer.setResourceTraitsCF(RESOURCE_TRAITS_CF);
        purgeDB();
    }

    private void purgeDB() {
        deleteAllRows(METRICS_WORK_QUEUE_CF, StringSerializer.get());
        deleteAllRows(RAW_METRIC_DATA_CF, IntegerSerializer.get());
        deleteAllRows(ONE_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
        deleteAllRows(SIX_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
        deleteAllRows(TWENTY_FOUR_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
        deleteAllRows(TRAITS_CF, IntegerSerializer.get());
        deleteAllRows(RESOURCE_TRAITS_CF, IntegerSerializer.get());
    }

    private <K> MutationResult deleteAllRows(String columnFamily, Serializer<K> keySerializer) {
        KeyIterator<K> keyIterator = new KeyIterator<K>(keyspace, columnFamily, keySerializer);
        Mutator<K> rowMutator = HFactory.createMutator(keyspace, keySerializer);
        rowMutator.addDeletion(keyIterator, columnFamily);

        return rowMutator.execute();
    }

    @Test
    public void insertMultipleRawNumericDataForOneSchedule() {
        int scheduleId = 123;

        DateTime now = new DateTime();
        DateTime threeMinutesAgo = now.minusMinutes(3);
        DateTime twoMinutesAgo = now.minusMinutes(2);
        DateTime oneMinuteAgo = now.minusMinutes(1);

        int sevenDays = Duration.standardDays(7).toStandardSeconds().getSeconds();

        String scheduleName = getClass().getName() + "_SCHEDULE";
        long interval = MINUTE * 10;
        boolean enabled = true;
        DataType dataType = DataType.MEASUREMENT;
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(scheduleId, scheduleName, interval,
            enabled, dataType);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), request, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), request, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), request, 2.6));

        metricsServer.addNumericData(data);

        SliceQuery<Integer, Long, Double> query = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
            LongSerializer.get(), DoubleSerializer.get());
        query.setColumnFamily(RAW_METRIC_DATA_CF);
        query.setKey(scheduleId);
        query.setRange(null, null, false, 10);

        QueryResult<ColumnSlice<Long, Double>> queryResult = query.execute();
        List<HColumn<Long, Double>> actual = queryResult.get().getColumns();

        List<HColumn<Long, Double>> expected = asList(
            HFactory.createColumn(threeMinutesAgo.getMillis(), 3.2, sevenDays, LongSerializer.get(),
                DoubleSerializer.get()),
            HFactory.createColumn(twoMinutesAgo.getMillis(), 3.9, sevenDays, LongSerializer.get(),
                DoubleSerializer.get()),
            HFactory.createColumn(oneMinuteAgo.getMillis(), 2.6, sevenDays, LongSerializer.get(),
                DoubleSerializer.get())
        );

        for (int i = 0; i < expected.size(); ++i) {
            assertPropertiesMatch("The returned columns do not match", expected.get(i), actual.get(i),
                "clock");
        }

        DateTime theHour = now.hourOfDay().roundFloorCopy();
        Composite expectedComposite = new Composite();
        expectedComposite.addComponent(theHour.getMillis(), LongSerializer.get());
        expectedComposite.addComponent(scheduleId, IntegerSerializer.get());

        assert1HourMetricsQueueEquals(asList(HFactory.createColumn(expectedComposite, 0, CompositeSerializer.get(),
            IntegerSerializer.get())));
    }

    private void assert1HourMetricsQueueEquals(List<HColumn<Composite, Integer>> expected) {
        assertMetricsQueueEquals(ONE_HOUR_METRIC_DATA_CF, expected);
    }

    private void assertMetricsQueueEquals(String columnFamily, List<HColumn<Composite, Integer>> expected) {
        SliceQuery<String,Composite, Integer> sliceQuery = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
            new CompositeSerializer().get(), IntegerSerializer.get());
        sliceQuery.setColumnFamily(METRICS_WORK_QUEUE_CF);
        sliceQuery.setKey(columnFamily);

        ColumnSliceIterator<String, Composite, Integer> iterator = new ColumnSliceIterator<String, Composite, Integer>(
            sliceQuery, (Composite) null, (Composite) null, false);

        List<HColumn<Composite, Integer>> actual = new ArrayList<HColumn<Composite, Integer>>();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        assertEquals(actual.size(), expected.size(), "The number of entries in the queue do not match.");
        int i = 0;
        for (HColumn<Composite, Integer> expectedColumn :  expected) {
            HColumn<Composite, Integer> actualColumn = actual.get(i++);
            assertEquals(getTimestamp(actualColumn.getName()), getTimestamp(expectedColumn.getName()),
                "The timestamp does not match the expected value.");
            assertEquals(getScheduleId(actualColumn.getName()), getScheduleId(expectedColumn.getName()),
                "The schedule id does not match the expected value.");
        }
    }

    private void assert1HourDataEquals(int scheduleId, List<HColumn<Composite, Double>> expected) {
        assertMetricDataEquals(scheduleId, ONE_HOUR_METRIC_DATA_CF, expected);
    }

    private void assertMetricDataEquals(int scheduleId, String columnFamily, List<HColumn<Composite,
        Double>> expected) {
        SliceQuery<Integer, Composite, Double> query = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
            CompositeSerializer.get(), DoubleSerializer.get());
        query.setColumnFamily(columnFamily);
        query.setKey(scheduleId);

        ColumnSliceIterator<Integer, Composite, Double> iterator = new ColumnSliceIterator<Integer, Composite, Double>(
            query, (Composite) null, (Composite) null, false);

        List<HColumn<Composite, Double>> actual = new ArrayList<HColumn<Composite, Double>>();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        String prefix;
        if (columnFamily.equals(ONE_HOUR_METRIC_DATA_CF)) {
            prefix = "The one hour data for schedule id " + scheduleId + " is wrong.";
        } else if (columnFamily.equals(SIX_HOUR_METRIC_DATA_CF)) {
            prefix = "The six hour data for schedule id " + scheduleId + " is wrong.";
        } else if (columnFamily.equals(TWENTY_FOUR_HOUR_METRIC_DATA_CF)) {
            prefix = "The twenty-four hour data for schedule id " + scheduleId + " is wrong.";
        } else {
            throw new IllegalArgumentException(columnFamily + " is not a recognized column family");
        }

        assertEquals(actual.size(), expected.size(), prefix + " The number of columns do not match.");
        int i = 0;
        for (HColumn<Composite, Double> expectedColumn : expected) {
            HColumn<Composite, Double> actualColumn = actual.get(i++);
            assertEquals(getTimestamp(actualColumn.getName()), getTimestamp(expectedColumn.getName()),
                prefix + " The timestamp does not match the expected value.");
            assertEquals(getAggregateType(actualColumn.getName()), getAggregateType(expectedColumn.getName()),
                prefix + " The column data type does not match the expected value");
            assertEquals(actualColumn.getValue(), expectedColumn.getValue(), "The column value is wrong");
            assertEquals(actualColumn.getTtl(), expectedColumn.getTtl(), "The ttl for the column is wrong.");
        }
    }

    private Integer getScheduleId(Composite composite) {
        return composite.get(1, IntegerSerializer.get());
    }

    private Long getTimestamp(Composite composite) {
        return composite.get(0, LongSerializer.get());
    }

    private AggregateType getAggregateType(Composite composite) {
        Integer type = composite.get(1, IntegerSerializer.get());
        return AggregateType.valueOf(type);
    }

}
