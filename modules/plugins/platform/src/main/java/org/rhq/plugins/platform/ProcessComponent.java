/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.plugins.platform;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.plugins.platform.ProcessComponentConfig.createProcessComponentConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.system.AggregateProcessInfo;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.ProcessInfo.ProcessInfoSnapshot;
import org.rhq.core.system.SystemInfo;

/**
 * Monitors a generic process.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ProcessComponent implements ResourceComponent, MeasurementFacet {
    private static final Log LOG = LogFactory.getLog(ProcessComponent.class);

    private static final String PROCESS_METRIC_PREFIX = "Process.";

    private ResourceContext resourceContext;
    private ProcessComponentConfig processComponentConfig;
    private ProcessInfo process;

    @Override
    public void start(ResourceContext resourceContext) throws Exception {
        this.resourceContext = resourceContext;
        processComponentConfig = createProcessComponentConfig(resourceContext.getPluginConfiguration());
    }

    @Override
    public void stop() {
        resourceContext = null;
        processComponentConfig = null;
        process = null;

    }

    @Override
    public AvailabilityType getAvailability() {
        try {
            ProcessInfoSnapshot snapshot = getFreshSnapshot();
            return (snapshot != null && snapshot.isRunning()) ? UP : DOWN;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to get process info", e);
            }
            return DOWN;
        }
    }

    private ProcessInfoSnapshot getFreshSnapshot() throws Exception {
        ProcessInfoSnapshot snapshot = (process == null) ? null : process.freshSnapshot();
        if (snapshot == null || !snapshot.isRunning()) {
            process = findProcess(processComponentConfig, resourceContext.getSystemInformation());
            // Safe to get prior snapshot here, we've just recreated the process info instance
            snapshot = (process == null) ? null : process.priorSnaphot();
        }
        return snapshot;
    }

    static ProcessInfo findProcess(ProcessComponentConfig processComponentConfig, SystemInfo systemInfo)
        throws Exception {

        long pid;
        switch (processComponentConfig.getType()) {
        case pidFile:
            pid = getPidFromPidFile(processComponentConfig);
            break;
        case piql:
            pid = getPidFromPiqlExpression(processComponentConfig, systemInfo);
            break;
        default:
            throw new InvalidPluginConfigurationException("Unknown type: " + processComponentConfig.getType());
        }

        if (processComponentConfig.isFullProcessTree()) {
            return new AggregateProcessInfo(pid);
        } else {
            return new ProcessInfo(pid);
        }
    }

    private static long getPidFromPidFile(ProcessComponentConfig processComponentConfig) throws IOException {
        File file = new File(processComponentConfig.getPidFile());
        if (file.canRead()) {
            FileInputStream fis = new FileInputStream(file);
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(fis));
                return Long.parseLong(r.readLine());
            } finally {
                try {
                    fis.close();
                } catch (Exception ignore) {
                }
            }
        } else {
            throw new FileNotFoundException("pidfile [" + processComponentConfig.getPidFile()
                + "] does not exist or is not allowed to be read. full path=" + file.getAbsolutePath());
        }
    }

    private static long getPidFromPiqlExpression(ProcessComponentConfig processComponentConfig, SystemInfo systemInfo)
        throws Exception {
        List<ProcessInfo> processes = systemInfo.getProcesses(processComponentConfig.getPiql());
        if (processes != null && processes.size() == 1) {
            return processes.get(0).getPid();
        } else {
            throw new Exception("process query [" + processComponentConfig.getPiql()
                + "] did not return a single process: " + processes);
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        ProcessInfoSnapshot snapshot;
        try {
            snapshot = getFreshSnapshot();
            if (snapshot == null || !snapshot.isRunning()) {
                return;
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to get process info", e);
            }
            return;
        }
        for (MeasurementScheduleRequest request : metrics) {
            String propertyName = request.getName();
            if (!propertyName.startsWith(PROCESS_METRIC_PREFIX)) {
                continue;
            }
            propertyName = propertyName.substring(PROCESS_METRIC_PREFIX.length());
            StringTokenizer propertyTokenizer = new StringTokenizer(propertyName, ".");
            if (!propertyTokenizer.hasMoreTokens()) {
                continue;
            }
            String category = propertyTokenizer.nextToken();
            if (!propertyTokenizer.hasMoreTokens()) {
                continue;
            }
            String subcategory = propertyTokenizer.nextToken();
            if (category.equals("cpu")) {
                CpuMetricGatherer cpuMetricGatherer;
                if (processComponentConfig.isFullProcessTree()) {
                    cpuMetricGatherer = new AggregateProcessCpuMetricGatherer((AggregateProcessInfo) process);
                } else {
                    cpuMetricGatherer = new ProcessCpuMetricGatherer(snapshot);
                }
                addCpuMetric(subcategory, report, request, cpuMetricGatherer);
            } else if (category.equals("memory")) {
                MemoryMetricGatherer memoryMetricGatherer;
                if (processComponentConfig.isFullProcessTree()) {
                    memoryMetricGatherer = new AggregateProcessMemoryMetricGatherer((AggregateProcessInfo) process);
                } else {
                    memoryMetricGatherer = new ProcessMemoryMetricGatherer(snapshot);
                }
                addMemoryMetric(subcategory, report, request, memoryMetricGatherer);
            } else if (category.equals("fileDescriptor")) {
                FileDescriptorMetricGatherer fileDescriptorMetricGatherer;
                if (processComponentConfig.isFullProcessTree()) {
                    fileDescriptorMetricGatherer = new AggregateProcessFileDescriptorMetricGatherer(
                        (AggregateProcessInfo) process);
                } else {
                    fileDescriptorMetricGatherer = new ProcessFileDescriptorMetricGatherer(snapshot);
                }
                addFileDescriptorMetric(subcategory, report, request, fileDescriptorMetricGatherer);
            }
        }
    }

    private void addCpuMetric(String element, MeasurementReport report, MeasurementScheduleRequest request,
        CpuMetricGatherer cpuMetricGatherer) {
        if (element.equals("user")) {
            report.addData(new MeasurementDataNumeric(request, cpuMetricGatherer.getUser()));
        } else if (element.equals("sys")) {
            report.addData(new MeasurementDataNumeric(request, cpuMetricGatherer.getSys()));
        } else if (element.equals("percent")) {
            report.addData(new MeasurementDataNumeric(request, cpuMetricGatherer.getPercent()));
        }
    }

    private void addMemoryMetric(String element, MeasurementReport report, MeasurementScheduleRequest request,
        MemoryMetricGatherer memoryMetricGatherer) {
        if (element.equals("resident")) {
            report.addData(new MeasurementDataNumeric(request, memoryMetricGatherer.getResident()));
        } else if (element.equals("size")) {
            report.addData(new MeasurementDataNumeric(request, memoryMetricGatherer.getSize()));
        }
    }

    private void addFileDescriptorMetric(String element, MeasurementReport report, MeasurementScheduleRequest request,
        FileDescriptorMetricGatherer fileDescriptorMetricGatherer) {
        if (element.equals("total")) {
            report.addData(new MeasurementDataNumeric(request, fileDescriptorMetricGatherer.getTotal()));
        }
    }

    /**
     * @deprecated since RHQ4.12. It should not have been exposed.
     */
    @Deprecated
    protected static ProcessInfo getProcessForConfiguration(Configuration pluginConfig, SystemInfo systemInfo)
        throws Exception {
        return findProcess(createProcessComponentConfig(pluginConfig), systemInfo);
    }

    private interface CpuMetricGatherer {
        Double getUser();

        Double getSys();

        Double getPercent();
    }

    private static class ProcessCpuMetricGatherer implements CpuMetricGatherer {
        ProcessInfoSnapshot snapshot;

        ProcessCpuMetricGatherer(ProcessInfoSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Double getUser() {
            return (double) snapshot.getCpu().getUser();
        }

        @Override
        public Double getSys() {
            return (double) snapshot.getCpu().getSys();
        }

        @Override
        public Double getPercent() {
            return snapshot.getCpu().getPercent();
        }
    }

    private static class AggregateProcessCpuMetricGatherer implements CpuMetricGatherer {
        AggregateProcessInfo aggregateProcessInfo;

        AggregateProcessCpuMetricGatherer(AggregateProcessInfo aggregateProcessInfo) {
            this.aggregateProcessInfo = aggregateProcessInfo;
        }

        @Override
        public Double getUser() {
            return (double) aggregateProcessInfo.getAggregateCpu().getUser();
        }

        @Override
        public Double getSys() {
            return (double) aggregateProcessInfo.getAggregateCpu().getSys();
        }

        @Override
        public Double getPercent() {
            return aggregateProcessInfo.getAggregateCpu().getPercent();
        }
    }

    private interface MemoryMetricGatherer {
        Double getResident();

        Double getSize();
    }

    private static class ProcessMemoryMetricGatherer implements MemoryMetricGatherer {
        ProcessInfoSnapshot snapshot;

        ProcessMemoryMetricGatherer(ProcessInfoSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Double getResident() {
            return (double) snapshot.getMemory().getResident();
        }

        @Override
        public Double getSize() {
            return (double) snapshot.getMemory().getSize();
        }
    }

    private static class AggregateProcessMemoryMetricGatherer implements MemoryMetricGatherer {
        AggregateProcessInfo aggregateProcessInfo;

        AggregateProcessMemoryMetricGatherer(AggregateProcessInfo aggregateProcessInfo) {
            this.aggregateProcessInfo = aggregateProcessInfo;
        }

        @Override
        public Double getResident() {
            return (double) aggregateProcessInfo.getAggregateMemory().getResident();
        }

        @Override
        public Double getSize() {
            return (double) aggregateProcessInfo.getAggregateMemory().getSize();
        }
    }

    private interface FileDescriptorMetricGatherer {
        Double getTotal();
    }

    private static class ProcessFileDescriptorMetricGatherer implements FileDescriptorMetricGatherer {
        ProcessInfoSnapshot snapshot;

        ProcessFileDescriptorMetricGatherer(ProcessInfoSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Double getTotal() {
            return (double) snapshot.getFileDescriptor().getTotal();
        }
    }

    private static class AggregateProcessFileDescriptorMetricGatherer implements FileDescriptorMetricGatherer {
        AggregateProcessInfo aggregateProcessInfo;

        AggregateProcessFileDescriptorMetricGatherer(AggregateProcessInfo aggregateProcessInfo) {
            this.aggregateProcessInfo = aggregateProcessInfo;
        }

        @Override
        public Double getTotal() {
            return (double) aggregateProcessInfo.getAggregateFileDescriptor().getTotal();
        }
    }
}
