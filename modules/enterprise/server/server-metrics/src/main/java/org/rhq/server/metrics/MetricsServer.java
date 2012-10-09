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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.joda.time.DateTime;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;

/**
 * @author John Sanda
 */
public class MetricsServer {

    private static final int DEFAULT_PAGE_SIZE = 200;

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

    public void addNumericData(Set<MeasurementDataNumeric> dataSet) {
        Map<Integer, DateTime> updates = new TreeMap<Integer, DateTime>();
        Mutator<Integer> mutator = HFactory.createMutator(keyspace, IntegerSerializer.get());

        for (MeasurementDataNumeric data : dataSet) {
            updates.put(data.getScheduleId(), new DateTime(data.getTimestamp()).hourOfDay().roundFloorCopy());
            mutator.addInsertion(
                data.getScheduleId(),
                rawMetricsDataCF,
                HFactory.createColumn(data.getTimestamp(), data.getValue(), DateTimeService.SEVEN_DAYS,
                    LongSerializer.get(), DoubleSerializer.get()));
        }

        mutator.execute();

        updateMetricsQueue(oneHourMetricsDataCF, updates);
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
        Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());

        for (Integer scheduleId : updates.keySet()) {
            DateTime collectionTime = new DateTime(updates.get(scheduleId));
            Composite composite = new Composite();
            composite.addComponent(collectionTime.getMillis(), LongSerializer.get());
            composite.addComponent(scheduleId, IntegerSerializer.get());
            HColumn<Composite, Integer> column = HFactory.createColumn(composite, 0, CompositeSerializer.get(),
                IntegerSerializer.get());
            mutator.addInsertion(columnFamily, metricsQueueCF, column);
        }

        return mutator.execute();
    }

}
