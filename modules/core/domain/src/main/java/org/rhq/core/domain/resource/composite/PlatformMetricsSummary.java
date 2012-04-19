/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.core.domain.resource.composite;

import java.io.Serializable;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.resource.Resource;

/**
 * @author jsanda
 */
public class PlatformMetricsSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    public static enum MemoryMetric {
        ActualUsed("Native.MemoryInfo.actualUsed"), Used("Native.MemoryInfo.used"),
        ActualFree("Native.MemoryInfo.actualFree"), Free("Native.MemoryInfo.free"), Total("Native.MemoryInfo.total");

        private final String property;

        MemoryMetric(String property) {
            this.property = property;
        }

        @SuppressWarnings("unused")
        public String getProperty() {
            return property;
        }
    }

    public static enum CPUMetric {
        Idle("CpuPerc.idle"), System("CpuPerc.sys"), User("CpuPerc.user"), Wait("CpuPerc.wait");

        private final String property;

        CPUMetric(String property) {
            this.property = property;
        }

        @SuppressWarnings("unused")
        public String getProperty() {
            return property;
        }
    }

    public static enum SwapMetric {
        Used("Native.SwapInfo.used"), Free("Native.SwapInfo.free"), Total("Native.SwapInfo.total");

        private final String property;

        SwapMetric(String property) {
            this.property = property;
        }

        @SuppressWarnings("unused")
        public String getProperty() {
            return property;
        }
    }

    private Resource resource;

    private boolean metricsAvailable = true;

    private MeasurementData freeMemory;

    private MeasurementData actualFreeMemory;

    private MeasurementData usedMemory;

    private MeasurementData actualUsedMemory;

    private MeasurementData totalMemory;

    private MeasurementData freeSwap;

    private MeasurementData usedSwap;

    private MeasurementData totalSwap;

    private MeasurementData idleCPU;

    private MeasurementData systemCPU;

    private MeasurementData userCPU;

    private MeasurementData waitCPU;

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public boolean isMetricsAvailable() {
        return metricsAvailable;
    }

    public void setMetricsAvailable(boolean metricsAvailable) {
        this.metricsAvailable = metricsAvailable;
    }

    public MeasurementData getActualFreeMemory() {
        return actualFreeMemory;
    }

    public void setActualFreeMemory(MeasurementData actualFreeMemory) {
        this.actualFreeMemory = actualFreeMemory;
    }

    public MeasurementData getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(MeasurementData freeMemory) {
        this.freeMemory = freeMemory;
    }

    public MeasurementData getActualUsedMemory() {
        return actualUsedMemory;
    }

    public void setActualUsedMemory(MeasurementData actualUsedMemory) {
        this.actualUsedMemory = actualUsedMemory;
    }

    public MeasurementData getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(MeasurementData usedMemory) {
        this.usedMemory = usedMemory;
    }

    public MeasurementData getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(MeasurementData totalMemory) {
        this.totalMemory = totalMemory;
    }

    public MeasurementData getFreeSwap() {
        return freeSwap;
    }

    public void setFreeSwap(MeasurementData freeSwap) {
        this.freeSwap = freeSwap;
    }

    public MeasurementData getUsedSwap() {
        return usedSwap;
    }

    public void setUsedSwap(MeasurementData usedSwap) {
        this.usedSwap = usedSwap;
    }

    public MeasurementData getTotalSwap() {
        return totalSwap;
    }

    public void setTotalSwap(MeasurementData totalSwap) {
        this.totalSwap = totalSwap;
    }

    public MeasurementData getIdleCPU() {
        return idleCPU;
    }

    public void setIdleCPU(MeasurementData idleCPU) {
        this.idleCPU = idleCPU;
    }

    public MeasurementData getSystemCPU() {
        return systemCPU;
    }

    public void setSystemCPU(MeasurementData systemCPU) {
        this.systemCPU = systemCPU;
    }

    public MeasurementData getUserCPU() {
        return userCPU;
    }

    public void setUserCPU(MeasurementData userCPU) {
        this.userCPU = userCPU;
    }

    public MeasurementData getWaitCPU() {
        return waitCPU;
    }

    public void setWaitCPU(MeasurementData waitCPU) {
        this.waitCPU = waitCPU;
    }
}
