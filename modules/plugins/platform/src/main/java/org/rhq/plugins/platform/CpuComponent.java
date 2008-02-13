/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.platform;

import java.util.Set;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
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
        return AvailabilityType.UP;
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
                    double value = ((Number) ObjectUtil.lookupAttributeProperty(cpu, property)).doubleValue();
                    report.addData(new MeasurementDataNumeric(request, value));
                } else if (property.startsWith("CpuPerc.")) {
                    if (cpuInfo == null) {
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
                }
            }
        }
    }
}