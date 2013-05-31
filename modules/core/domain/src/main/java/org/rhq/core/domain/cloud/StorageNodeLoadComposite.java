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

    public MeasurementAggregateWithUnits getHeapCommitted() {
        return heapCommitted;
    }

    public void setHeapCommitted(MeasurementAggregateWithUnits heapCommitted) {
        this.heapCommitted = heapCommitted;
    }
    
    public MeasurementAggregateWithUnits getHeapUsed() {
        return heapUsed;
    }

    public void setHeapUsed(MeasurementAggregateWithUnits heapUsed) {
        this.heapUsed = heapUsed;
    }

    public MeasurementAggregateWithUnits getHeapPercentageUsed() {
        return heapPercentageUsed;
    }

    public void setHeapPercentageUsed(MeasurementAggregateWithUnits heapPercentageUsed) {
        this.heapPercentageUsed = heapPercentageUsed;
    }
    
    public MeasurementAggregateWithUnits getLoad() {
        return load;
    }

    public void setLoad(MeasurementAggregateWithUnits load) {
        this.load = load;
    }

    public MeasurementAggregate getTokens() {
        return tokens;
    }

    public void setTokens(MeasurementAggregate tokens) {
        this.tokens = tokens;
    }

    public MeasurementAggregateWithUnits getActuallyOwns() {
        return actuallyOwns;
    }

    public void setActuallyOwns(MeasurementAggregateWithUnits actuallyOwns) {
        this.actuallyOwns = actuallyOwns;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        // gwt doesn't support String.format
//        builder.append("average values for last ");
//        builder.append((endtime - begintime) / (1000 * 60 * 60));
//        builder.append(" hours");
//        builder.append("\naddress        load       tokens  owns (effective)\n");
//        builder.append(string.format("%15s", storagenode.getaddress()));
//        builder.append(string.format("%11s", load.getaggregate().getavg())).append(" ").append(load.getunits().getname());
//        builder.append(string.format("%8s", tokens.getavg()));
//        builder.append(string.format("%16s", actuallyowns.getavg()));
        
        builder.append("storageNode.addresss=").append(storageNode.getAddress()).append(", ");
        builder.append("beginTime=").append(beginTime).append(", ");
        builder.append("heapCommitted=").append(heapCommitted).append(", ");
        builder.append("heapUsed=").append(heapUsed).append(", ");
        builder.append("heapPercentageUsed=").append(heapPercentageUsed).append(", ");
        builder.append("load=").append(load).append(", ");
        builder.append("tokens=").append(tokens).append(", ");
        builder.append("actuallyOwns=").append(actuallyOwns);
        return builder.toString();
    }

    
    public static class MeasurementAggregateWithUnits implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final MeasurementAggregate aggregate;
        private final MeasurementUnits units;
        private String formattedValue;

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
