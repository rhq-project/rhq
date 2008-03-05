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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Swap;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ObjectUtil;
import org.rhq.core.system.CpuInformation;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;

/**
 * Represents the platform resource which is the root resource for all other resources managed.
 *
 * @author John Mazzitelli
 */
public class PlatformComponent implements ResourceComponent, ConfigurationFacet, MeasurementFacet, OperationFacet {

    private final Log log = LogFactory.getLog(PlatformComponent.class);

    /**
     * This is a substring that starts all native-only measurement property names. If a measurement property name starts
     * with this, it can only be collected if we have native support for our platform.
     */
    private static final String NATIVE_INDICATOR = "Native.";

    /**
     * This is a substring that starts all trait measurement property names. If its a native trait, it will appear after
     * the {@link #NATIVE_INDICATOR}.
     */
    private static final String TRAIT_INDICATOR = "Trait.";

    // these are the "property" names for all trait metrics
    private static final String TRAIT_HOSTNAME = TRAIT_INDICATOR + "hostname";
    private static final String TRAIT_OSNAME = TRAIT_INDICATOR + "osname";
    private static final String TRAIT_OSVERSION = TRAIT_INDICATOR + "osversion";

    protected ResourceContext resourceContext;
    private SystemInfo sysinfo;

    public void start(ResourceContext context) {
        this.resourceContext = context;
        sysinfo = context.getSystemInformation();
    }

    public void stop() {
        resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        // if we are running then obviously, by definition, the platform is up
        return AvailabilityType.UP;
    }

    public Configuration loadResourceConfiguration() {
        // platform configuration is "read-only" and are implemented as metrics
        Configuration config = new Configuration();
        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // most of the platform configuration are "read-only" and
        // are implemented as metric traits nothing to update
        report.setErrorMessage("Cannot update platform resource configuration - it is read only");
        report.setStatus(ConfigurationUpdateStatus.FAILURE);
        return;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        SystemInfo info = this.resourceContext.getSystemInformation();
        boolean isNative = info.isNative();
        Mem platformMemoryInfo = null;
        Swap platformSwapInfo = null;

        for (MeasurementScheduleRequest request : metrics) {
            String property = request.getName();

            if (property.startsWith(NATIVE_INDICATOR)) {
                // we cannot collect a native measurement without native support - go on to the next
                if (!isNative) {
                    continue;
                }

                property = property.substring(NATIVE_INDICATOR.length());
            }

            if (property.startsWith(TRAIT_INDICATOR)) {
                report.addData(getMeasurementDataTrait(request));
            } else if (property.startsWith("MemoryInfo.")) {
                if (platformMemoryInfo == null) {
                    platformMemoryInfo = info.getMemoryInfo();
                }

                property = property.substring(property.indexOf(".") + 1);
                double memoryValue = ((Number) getObjectProperty(platformMemoryInfo, property)).doubleValue();
                report.addData(new MeasurementDataNumeric(request, memoryValue));
            } else if (property.startsWith("SwapInfo.")) {
                if (platformSwapInfo == null) {
                    platformSwapInfo = info.getSwapInfo();
                }

                property = property.substring(property.indexOf(".") + 1);
                double swapValue = ((Number) getObjectProperty(platformSwapInfo, property)).doubleValue();
                report.addData(new MeasurementDataNumeric(request, swapValue));
            } else if (property.startsWith("CpuPerc.")) {

                double result = 0.0;
                property = property.substring(property.indexOf(".") + 1);

                int numberOfCpus = 0;
                try {
                    numberOfCpus = sysinfo.getNumberOfCpus();
                } catch (UnsupportedOperationException uoe) {
                    if (log.isDebugEnabled())
                        log.debug("Can't get number of CPUs ignoring metric " + property + " : " + uoe.getMessage());
                }
                if (numberOfCpus > 0) {
                    for (int i = 0; i < numberOfCpus; i++) {
                        CpuInformation cpuInfo = sysinfo.getCpu(i);
                        CpuPerc cpuPerc = cpuInfo.getCpuPercentage();
                        double value = ((Number) ObjectUtil.lookupAttributeProperty(cpuPerc, property)).doubleValue();
                        result += value;
                    }

                    result /= numberOfCpus;

                    report.addData(new MeasurementDataNumeric(request, result));
                }
            }
        }
    }

    private Object getObjectProperty(Object object, String name) {
        try {
            BeanInfo info = Introspector.getBeanInfo(object.getClass());
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                if (pd.getName().equals(name)) {
                    return pd.getReadMethod().invoke(object);
                }
            }
        } catch (Exception skip) {
            if (log.isDebugEnabled())
                log.debug(skip);
        }

        return Double.NaN;
    }

    private MeasurementDataTrait getMeasurementDataTrait(MeasurementScheduleRequest request) {
        String name = request.getName();

        MeasurementDataTrait trait = new MeasurementDataTrait(request, "?");

        try {
            if (TRAIT_HOSTNAME.indexOf(name) > -1) {
                trait.setValue(sysinfo.getHostname());
            } else if (TRAIT_OSNAME.indexOf(name) > -1) {
                trait.setValue(sysinfo.getOperatingSystemName());
            } else if (TRAIT_OSVERSION.indexOf(name) > -1) {
                trait.setValue(sysinfo.getOperatingSystemVersion());
            } else {
                log.error("Being asked to collect an unknown trait measurement: " + name);
            }
        } catch (Exception skip) {
            log.debug(skip);
        }

        return trait;
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("discovery".equals(name)) {
            long start = System.currentTimeMillis();
            PluginContainer pc = PluginContainer.getInstance();
            pc.getInventoryManager().executeServerScanImmediately();
            boolean detailed = parameters.getSimple("detailedDiscovery").getBooleanValue();
            if (detailed) {
                pc.getInventoryManager().executeServiceScanImmediately();
            }

            return new OperationResult((detailed ? "Full " : "") + "Discovery run in ["
                + (System.currentTimeMillis() - start) + "]ms");
        } else if ("viewProcessList".equals(name)) {
            OperationResult result = new OperationResult();
            List<ProcessInfo> processes = this.resourceContext.getSystemInformation().getAllProcesses();
            PropertyList processList = new PropertyList("processList");
            for (ProcessInfo process : processes) {
                PropertyMap pm = new PropertyMap("process");
                pm.put(new PropertySimple("pid", process.getPid()));
                pm.put(new PropertySimple("name", process.getBaseName()));
                pm.put(new PropertySimple("size", (process.getMemory() != null) ? process.getMemory().getSize() : "0"));
                pm.put(new PropertySimple("userTime", (process.getTime() != null) ? process.getTime().getUser() : "0"));
                pm
                    .put(new PropertySimple("kernelTime", (process.getTime() != null) ? process.getTime().getSys()
                        : "0"));
                processList.add(pm);
            }

            result.getComplexResults().put(processList);
            return result;
        }

        throw new UnsupportedOperationException("Operation [" + name + "] not supported on "
            + resourceContext.getResourceType() + ".");
    }
}