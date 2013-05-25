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

package org.rhq.enterprise.server.cloud;

import org.rhq.enterprise.server.measurement.MeasurementAggregate;

/**
 * @author John Sanda
 */
public class LoadInfo {

    private String diskSpaceUsed;

    private String ringOwnership;

    private MeasurementAggregate heapCommitted;

    private MeasurementAggregate heapUsage;

    public String getDiskSpaceUsed() {
        return diskSpaceUsed;
    }

    public void setDiskSpaceUsed(String diskSpaceUsed) {
        this.diskSpaceUsed = diskSpaceUsed;
    }

    public String getRingOwnership() {
        return ringOwnership;
    }

    public void setRingOwnership(String ringOwnership) {
        this.ringOwnership = ringOwnership;
    }

    public MeasurementAggregate getHeapCommitted() {
        return heapCommitted;
    }

    public void setHeapCommitted(MeasurementAggregate heapCommitted) {
        this.heapCommitted = heapCommitted;
    }

    public MeasurementAggregate getHeapUsage() {
        return heapUsage;
    }

    public void setHeapUsage(MeasurementAggregate heapUsage) {
        this.heapUsage = heapUsage;
    }

}
