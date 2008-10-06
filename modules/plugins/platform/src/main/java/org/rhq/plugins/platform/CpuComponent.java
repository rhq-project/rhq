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

import java.util.Set;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;

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

public class CpuComponent implements ResourceComponent<PlatformComponent>, MeasurementFacet {
    private CpuInformation cpuInformation = null;

    public void start(ResourceContext<PlatformComponent> resourceContext) {
        if (resourceContext.getSystemInformation().isNative()) {
            cpuInformation = resourceContext.getSystemInformation().getCpu(
                Integer.parseInt(resourceContext.getResourceKey()));
        }
    }

    public void stop() {
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
            CpuPerc cpuPerc = null;
            CpuInfo cpuInfo = null; // this is probably gonna be used for traits only

            for (MeasurementScheduleRequest request : metrics) {
                String property = request.getName();

                if (property.startsWith("Cpu.")) {
                    if (cpu == null) {
                        cpu = cpuInformation.getCpu();
                    }

                    property = property.substring(property.indexOf(".") + 1);
                    Long longValue = (Long) ObjectUtil.lookupAttributeProperty(cpu, property);
                    // A value of -1 indicates SIGAR does not support the metric on the Agent platform.
                    if (longValue != -1) {
                        report.addData(new MeasurementDataNumeric(request, longValue.doubleValue()));
                    }
                } else if (property.startsWith("CpuPerc.")) {
                    if (cpuPerc == null) {
                        cpuPerc = cpuInformation.getCpuPercentage();
                    }

                    property = property.substring(property.indexOf(".") + 1);
                    double value = ((Number) ObjectUtil.lookupAttributeProperty(cpuPerc, property)).doubleValue();
                    report.addData(new MeasurementDataNumeric(request, value));
                } else if (property.startsWith("CpuInfo.")) {
                    if (cpuInfo == null) {
                        cpuInfo = cpuInformation.getCpuInfo();
                    }

                    property = property.substring(property.indexOf(".") + 1);
                    double value = ((Number) ObjectUtil.lookupAttributeProperty(cpuInfo, property)).doubleValue();
                    report.addData(new MeasurementDataNumeric(request, value));
                } else if (property.startsWith("CpuTrait.")) {
                    if (cpuInfo == null) {
                        cpuInfo = cpuInformation.getCpuInfo();
                    }
                    property = property.substring(property.indexOf(".") + 1);
                    Object o = ObjectUtil.lookupAttributeProperty(cpuInfo, property);
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
}
