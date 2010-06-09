/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.platform;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.util.ObjectUtil;
import org.rhq.core.system.CpuInformation;

/**
 * A Resource component representing a CPU core.
 */
public class CpuComponent implements ResourceComponent<PlatformComponent>, MeasurementFacet {
    private CpuInformation cpuInformation = null;
    private CpuEntry startCpuEntry = null;

    /** 
     * A Map of cpu metric names to the last (Sigar) Cpu record used in calculations for that metric in getValues.
     * This allows us to take proper cpu usage deltas for each metric, on different schedules. See
     * RHQ-245 for why this is necessary.
     */
    private Map<String, CpuEntry> cpuCache;

    public void start(ResourceContext<PlatformComponent> resourceContext) {
        if (resourceContext.getSystemInformation().isNative()) {
            cpuInformation = resourceContext.getSystemInformation().getCpu(
                Integer.parseInt(resourceContext.getResourceKey()));
            cpuCache = new HashMap<String, CpuEntry>();
            startCpuEntry = new CpuEntry(cpuInformation.getCpu());
        }
        return;
    }

    public void stop() {
        return;
    }

    public AvailabilityType getAvailability() {
        if (this.cpuInformation != null) {
            this.cpuInformation.refresh();
            return (this.cpuInformation.isEnabled()) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } else {
            return AvailabilityType.UP;
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        if (cpuInformation != null) {
            cpuInformation.refresh();

            Cpu cpu = null;
            CpuEntry currentCpu = null;
            CpuInfo cpuInfo = null; // this is probably gonna be used for traits only

            for (MeasurementScheduleRequest request : metrics) {
                String property = request.getName();

                if (property.startsWith("Cpu.")) {
                    // Grab the current cpu info from SIGAR, only once for this cpu for all processed schedules
                    if (cpu == null) {
                        cpu = cpuInformation.getCpu();
                    }

                    property = property.substring(property.indexOf(".") + 1);
                    Long longValue = (Long) ObjectUtil.lookupAttributeProperty(cpu, property);
                    // A value of -1 indicates SIGAR does not support the metric on the Agent platform type.
                    if (longValue != null && longValue != -1) {
                        report.addData(new MeasurementDataNumeric(request, longValue.doubleValue()));
                    }
                } else if (property.startsWith("CpuPerc.")) {

                    /*
                     * ! we no longer use the SIGAR CpuPerc object to report cpu percentage metrics. See
                     * ! RHQ-245 for an explanation.  We now calculate our own percentages using
                     * ! the current raw cpu info from Sigar and the previous cpu numbers, cached per metric.
                     * ! This allows us to avoid the problem in RHQ-245 while handling perCpu-perMetric schedule
                     * ! granularity. 
                     */

                    // Grab the current cpu info from SIGAR, only once for this cpu for all processed schedules
                    if (null == cpu) {
                        cpu = cpuInformation.getCpu();
                    }
                    // Create a Cpu cacheEntry only once for this cpu for all processed schedules
                    if (null == currentCpu) {
                        currentCpu = new CpuEntry(cpu);
                    }

                    // Get the previous cpu numbers to be used for this metric and update the cache for the next go.
                    CpuEntry previousCpu = cpuCache.put(property, currentCpu);
                    previousCpu = (null == previousCpu) ? startCpuEntry : previousCpu;

                    // if for some reason the delta time is excessive then toss
                    // the metric, since it depends on a reasonable interval between
                    // prev and curr. This can happen due to avail down or a newly
                    // activated metric. Allow up to twice the metric interval.
                    Number num = null;
                    long deltaTime = currentCpu.getTimestamp() - previousCpu.getTimestamp();

                    if (deltaTime <= (2 * request.getInterval())) {
                        // Use the same calculation that SIGAR uses to generate the percentages. The difference is that
                        // we use a safe "previous" cpu record.                      
                        num = getPercentage(previousCpu.getCpu(), cpu, property);
                    }

                    // Uncomment to see details about the calculations.
                    //System.out.println("\nCPU-" + cpuInformation.getCpuIndex() + " Interval="
                    //    + ((currentCpu.getTimestamp() - previousCpu.getTimestamp()) / 1000) + " " + property + "="
                    //    + num + "\n   Prev=" + previousCpu + "\n   Curr=" + currentCpu);

                    if (num != null) {
                        report.addData(new MeasurementDataNumeric(request, num.doubleValue()));
                    }
                } else if (property.startsWith("CpuInfo.")) {
                    if (cpuInfo == null) {
                        cpuInfo = cpuInformation.getCpuInfo();
                    }

                    property = property.substring(property.indexOf(".") + 1);
                    Number num = ((Number) ObjectUtil.lookupAttributeProperty(cpuInfo, property));
                    if (num != null) {
                        report.addData(new MeasurementDataNumeric(request, num.doubleValue()));
                    }
                } else if (property.startsWith("CpuTrait.")) {
                    if (cpuInfo == null) {
                        cpuInfo = cpuInformation.getCpuInfo();
                    }
                    property = property.substring(property.indexOf(".") + 1);
                    Object o = ObjectUtil.lookupAttributeProperty(cpuInfo, property);
                    if (o != null) {
                        String res;
                        if ("model".equals(property) || "vendor".equals(property)) {
                            res = (String) o;
                        } else {
                            res = String.valueOf(o);
                        }
                        report.addData(new MeasurementDataTrait(request, res));
                    }
                }
            }
        }
        return;
    }

    /**
     * Calculate the percentage of CPU taken up by the requested cpu property (metric) for the interval
     * defined by the prev and curr cpu numbers.  This algorithm is taken from SIGAR. See
     * http://svn.hyperic.org/projects/sigar/trunk/src/sigar_format.c ( sigar_cpu_perc_calculate() ) 
     */
    private Number getPercentage(Cpu prev, Cpu curr, String property) {
        Number result = 0.0;

        double diff_user, diff_sys, diff_nice, diff_idle;
        double diff_wait, diff_irq, diff_soft_irq, diff_stolen;
        double diff_total;

        diff_user = curr.getUser() - prev.getUser();
        diff_sys = curr.getSys() - prev.getSys();
        diff_nice = curr.getNice() - prev.getNice();
        diff_idle = curr.getIdle() - prev.getIdle();
        diff_wait = curr.getWait() - prev.getWait();
        diff_irq = curr.getIrq() - prev.getIrq();
        diff_soft_irq = curr.getSoftIrq() - prev.getSoftIrq();
        diff_stolen = curr.getStolen() - prev.getStolen();

        // It's not exactly clear to me what SIGAR is doing here. It may be to handle a rollover of
        // the cumulative cpu usage numbers. If so, this code could maybe be improved to still
        // determine the total usage.
        diff_user = diff_user < 0 ? 0 : diff_user;
        diff_sys = diff_sys < 0 ? 0 : diff_sys;
        diff_nice = diff_nice < 0 ? 0 : diff_nice;
        diff_idle = diff_idle < 0 ? 0 : diff_idle;
        diff_wait = diff_wait < 0 ? 0 : diff_wait;
        diff_irq = diff_irq < 0 ? 0 : diff_irq;
        diff_soft_irq = diff_soft_irq < 0 ? 0 : diff_soft_irq;
        diff_stolen = diff_stolen < 0 ? 0 : diff_stolen;

        diff_total = diff_user + diff_sys + diff_nice + diff_idle + diff_wait + diff_irq + diff_soft_irq + diff_stolen;

        property = property.substring(property.lastIndexOf(".") + 1, property.length());
        if ("idle".equals(property)) {
            result = diff_idle / diff_total;
        } else if ("sys".equals(property)) {
            result = diff_sys / diff_total;
        } else if ("user".equals(property)) {
            result = diff_user / diff_total;
        } else if ("wait".equals(property)) {
            result = diff_wait / diff_total;
        } else if ("nice".equals(property)) {
            result = diff_nice / diff_total;
        } else if ("irq".equals(property)) {
            result = diff_irq / diff_total;
        } else if ("softIrq".equals(property)) {
            result = diff_soft_irq / diff_total;
        } else if ("stolen".equals(property)) {
            result = diff_stolen / diff_total;
        }

        return result;
    }

    /**
     * Just a private, immutable, utility class for associating a CPU and timestamp with the raw Cpu object from SIGAR.
     */
    private class CpuEntry {
        private Cpu cpu;
        private long timestamp;

        public CpuEntry(Cpu cpu) {
            this.cpu = cpu;
            this.timestamp = System.currentTimeMillis();
        }

        public Cpu getCpu() {
            return cpu;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String toString() {
            return "CPU-" + cpuInformation.getCpuIndex() + "[" + new SimpleDateFormat("HH:mm:ss").format(timestamp)
                + "] = " + cpu;
        }
    }
}
