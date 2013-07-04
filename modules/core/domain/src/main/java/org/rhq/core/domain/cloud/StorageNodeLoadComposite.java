/*
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
 */
package org.rhq.core.domain.cloud;

import java.io.Serializable;

import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementUnits;

/**
 * @author Jiri Kremser
 */
public class StorageNodeLoadComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private StorageNode storageNode;
    private long beginTime;
    private long endTime;

    private MeasurementAggregateWithUnits heapCommitted;
    private MeasurementAggregateWithUnits heapUsed;
    private MeasurementAggregateWithUnits heapPercentageUsed;
    private MeasurementAggregateWithUnits load;
    private MeasurementAggregateWithUnits partitionDiskUsedPercentage;
    private MeasurementAggregateWithUnits dataDiskUsed;
    private MeasurementAggregate tokens;
    private MeasurementAggregateWithUnits actuallyOwns;

    public StorageNodeLoadComposite() {
        // GWT needs this
    }

    public StorageNodeLoadComposite(StorageNode storageNode, long beginTime, long endTime) {
        this.storageNode = storageNode;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    public StorageNode getStorageNode() {
        return storageNode;
    }

    public void setStorageNode(StorageNode storageNode) {
        this.storageNode = storageNode;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * @return heapCommitted A computed metric for the amount of memory that is committed for the JVM to use.
     */
    public MeasurementAggregateWithUnits getHeapCommitted() {
        return heapCommitted;
    }

    public void setHeapCommitted(MeasurementAggregateWithUnits heapCommitted) {
        this.heapCommitted = heapCommitted;
    }

    /**
     * @return A computed metric for the amount of JVM heap memory used
     */
    public MeasurementAggregateWithUnits getHeapUsed() {
        return heapUsed;
    }

    public void setHeapUsed(MeasurementAggregateWithUnits heapUsed) {
        this.heapUsed = heapUsed;
    }

    /**
     * @return A computed metric for the {@link #getHeapUsed() heapUsed} divided by the
     * {@link #getHeapCommitted() heapCommitted} expressed as a percentage.
     */
    public MeasurementAggregateWithUnits getHeapPercentageUsed() {
        return heapPercentageUsed;
    }

    public void setHeapPercentageUsed(MeasurementAggregateWithUnits heapPercentageUsed) {
        this.heapPercentageUsed = heapPercentageUsed;
    }

    /**
     * @return A computed metric for the percentage of disk space used on the partition that contains the SSTables.
     *         If multiple data locations are configured then the partition with the highest utilization will be reported.
     */
    public MeasurementAggregateWithUnits getPartitionDiskUsedPercentage() {
        return partitionDiskUsedPercentage;
    }

    public void setPartitionDiskUsedPercentage(MeasurementAggregateWithUnits partitionDiskUsedPercentage) {
        this.partitionDiskUsedPercentage = partitionDiskUsedPercentage;
    }

    /**
     * @return A computed metric for the space used on disk by all data files, commit logs, and saved caches.
     */
    public MeasurementAggregateWithUnits getDataDiskUsed() {
        return dataDiskUsed;
    }

    public void setDataDiskUsed(MeasurementAggregateWithUnits dataDiskUsed) {
        this.dataDiskUsed = dataDiskUsed;
    }

    /**
     * @return A computed metric for the the reported disk space used by all SSTables on disk for all column families.
     */
    public MeasurementAggregateWithUnits getLoad() {
        return load;
    }

    public void setLoad(MeasurementAggregateWithUnits load) {
        this.load = load;
    }

    /**
     * @return A computed metric for the number of tokens owned by this node. The range of values between two tokens is
     * the range of possible keys for that portion of the token ring.
     */
    public MeasurementAggregate getTokens() {
        return tokens;
    }

    public void setTokens(MeasurementAggregate tokens) {
        this.tokens = tokens;
    }

    /**
     * @return A computed metric for a percentage of keys owned by this node. This directly correlates to the
     * number of {@link #getTokens() tokens}. For example, if you have a a two node cluster with each node having 256
     * tokens, then with an even distribution, this metric should be right around 50%.
     */
    public MeasurementAggregateWithUnits getActuallyOwns() {
        return actuallyOwns;
    }

    public void setActuallyOwns(MeasurementAggregateWithUnits actuallyOwns) {
        this.actuallyOwns = actuallyOwns;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("storageNode.addresss=").append(storageNode.getAddress()).append(", ");
        builder.append("beginTime=").append(beginTime).append(", ");
        builder.append("heapCommitted=").append(heapCommitted).append(", ");
        builder.append("heapUsed=").append(heapUsed).append(", ");
        builder.append("heapPercentageUsed=").append(heapPercentageUsed).append(", ");
        builder.append("load=").append(load).append(", ");
        builder.append("partitionDiskUsedPercentage=").append(partitionDiskUsedPercentage).append(", ");
        builder.append("dataDiskUsed=").append(dataDiskUsed).append(", ");
        builder.append("tokens=").append(tokens).append(", ");
        builder.append("actuallyOwns=").append(actuallyOwns);
        return builder.toString();
    }


    public static class MeasurementAggregateWithUnits implements Serializable {
        private static final long serialVersionUID = 1L;

        private MeasurementAggregate aggregate;
        private MeasurementUnits units;
        private String formattedValue;

        public MeasurementAggregateWithUnits() {
            // GWT needs this
        }

        public MeasurementAggregateWithUnits(MeasurementAggregate aggregate, MeasurementUnits units) {
            this.aggregate = aggregate;
            this.units = units;
        }

        public MeasurementAggregate getAggregate() {
            return aggregate;
        }

        public MeasurementUnits getUnits() {
            return units;
        }

        public void setFormattedValue(String formattedValue) {
            this.formattedValue = formattedValue;
        }

        @Override
        public String toString() {
            if (formattedValue != null) {
                return formattedValue;
            }
            return aggregate.toString() + " (" + units.toString() + ")";
        }
    }
}
