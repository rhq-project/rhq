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
    private int unackAlerts;
    private String hostname;

    private MeasurementAggregateWithUnits heapCommitted;
    private MeasurementAggregateWithUnits heapUsed;
    private MeasurementAggregateWithUnits heapPercentageUsed;
    private MeasurementAggregateWithUnits load;
    private MeasurementAggregateWithUnits dataDiskUsed;
    private MeasurementAggregate tokens;

    private MeasurementAggregateWithUnits dataDiskUsedPercentage;
    private MeasurementAggregateWithUnits totalDiskUsedPercentage;
    private MeasurementAggregate freeDiskToDataSizeRatio;

    private MeasurementAggregateWithUnits actuallyOwns;

    public StorageNodeLoadComposite() {
        // GWT needs this
    }

    public StorageNodeLoadComposite(StorageNode storageNode, long beginTime, long endTime) {
        this.storageNode = storageNode;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.hostname = storageNode.getAddress();
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

    public int getUnackAlerts() {
        return unackAlerts;
    }

    public void setUnackAlerts(int unackAlerts) {
        this.unackAlerts = unackAlerts;
    }
    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
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
     * @deprecated use {@link #getDataDiskUsedPercentage() getPartitionDiskUsedPercentage()} instead
     *
     * @return partitionDiskUsedPercentage
     */
    public MeasurementAggregateWithUnits getDiskSpacePercentageUsed() {
        return getDataDiskUsedPercentage();
    }

    /**
     * @deprecated use {@link #setDataDiskUsedPercentage(MeasurementAggregateWithUnits)} instead
     *
     * @param diskUsedPercentage
     */
    public void setDiskSpacePercentageUsed(MeasurementAggregateWithUnits diskUsedPercentage) {
        setDataDiskUsedPercentage(diskUsedPercentage);
    }

    /**
     * @return A computed metric for the percentage of disk space used by data file on the corresponding partitions.
     *         If multiple data locations are configured then the aggregate is calculated.
     */
    public MeasurementAggregateWithUnits getDataDiskUsedPercentage() {
        return dataDiskUsedPercentage;
    }

    public void setDataDiskUsedPercentage(MeasurementAggregateWithUnits dataDiskUsedPercentage) {
        this.dataDiskUsedPercentage = dataDiskUsedPercentage;
    }

    /**
     * @return A computed metric for the percentage of total (system + Storage Node data file) disk space used the partitions where data files are stored.
     *         If multiple data locations are configured then the aggregate is calculated.
     */
    public MeasurementAggregateWithUnits getTotalDiskUsedPercentage() {
        return totalDiskUsedPercentage;
    }

    public void setTotalDiskUsedPercentage(MeasurementAggregateWithUnits totalDiskUsedPercentage) {
        this.totalDiskUsedPercentage = totalDiskUsedPercentage;
    }

    /**
     * @return A computed metric for the percentage of total (system + Storage Node data file) disk space used the partitions where data files are stored.
     *         If multiple data locations are configured then the aggregate is calculated.
     */
    public MeasurementAggregate getFreeDiskToDataSizeRatio() {
        return freeDiskToDataSizeRatio;
    }

    public void setFreeDiskToDataSizeRatio(MeasurementAggregate freeDiskToDataSizeRatio) {
        this.freeDiskToDataSizeRatio = freeDiskToDataSizeRatio;
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

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("storageNode.addresss=").append(storageNode.getAddress()).append(", ");
        builder.append("hostname=").append(hostname).append(", ");
        builder.append("beginTime=").append(beginTime).append(", ");
        builder.append("beginTime=").append(beginTime).append(", ");
        builder.append("unackAlerts=").append(unackAlerts).append(", ");
        builder.append("heapUsed=").append(heapUsed).append(", ");
        builder.append("heapPercentageUsed=").append(heapPercentageUsed).append(", ");
        builder.append("load=").append(load).append(", ");
        builder.append("dataUsedPercentage=").append(dataDiskUsedPercentage).append(", ");
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
