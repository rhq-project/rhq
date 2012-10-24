/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.cassandra;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UNKNOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.core.system.OperatingSystemType.WINDOWS;
import static org.rhq.plugins.cassandra.CassandraUtil.getCluster;

import java.io.File;
import java.util.Set;

import me.prettyprint.hector.api.Cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.SigarException;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jmx.JMXServerComponent;

/**
 * @author John Sanda
 */
public class CassandraNodeComponent extends JMXServerComponent<ResourceComponent<?>> implements MeasurementFacet,
    OperationFacet {

    private Log log = LogFactory.getLog(CassandraNodeComponent.class);

    @Override
    public AvailabilityType getAvailability() {
        ResourceContext<?> context = getResourceContext();
        ProcessInfo processInfo = context.getNativeProcess();

        if (processInfo == null) {
            return UNKNOWN;
        } else if (processInfo.isRunning()) {
            return UP;
        } else {
            return DOWN;
        }
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {

        if (name.equals("shutdown")) {
            return shutdownNode();
        } else if (name.equals("start")) {
            return startNode();
        } else if (name.equals("restart")) {
            return restartNode();
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    protected OperationResult shutdownNode() {
        ResourceContext<?> context = getResourceContext();

        if (log.isInfoEnabled()) {
            log.info("Starting shutdown operation on " + CassandraNodeComponent.class.getName() +
                " with resource key " + context.getResourceKey());
        }
        EmsConnection emsConnection = getEmsConnection();
        EmsBean storageService = emsConnection.getBean("org.apache.cassandra.db:type=StorageService");
        Class[] emptyParams = new Class[0];

        if (log.isDebugEnabled()) {
            log.debug("Disabling thrift...");
        }
        EmsOperation operation = storageService.getOperation("stopRPCServer", emptyParams);
        operation.invoke((Object[]) emptyParams);

        if (log.isDebugEnabled()) {
            log.debug("Disabling gossip...");
        }
        operation = storageService.getOperation("stopGossiping", emptyParams);
        operation.invoke((Object[]) emptyParams);

        if (log.isDebugEnabled()) {
            log.debug("Initiating drain...");
        }
        operation = storageService.getOperation("drain", emptyParams);
        operation.invoke((Object[]) emptyParams);

        ProcessInfo process = context.getNativeProcess();
        long pid = process.getPid();
        try {
            process.kill("KILL");
            return new OperationResult("Successfully shut down Cassandra daemon with pid " + pid);
        } catch (SigarException e) {
            log.warn("Failed to shut down Cassandra node with pid " + pid, e);
            OperationResult failure = new OperationResult("Failed to shut down Cassandra node with pid " + pid);
            failure.setErrorMessage(ThrowableUtil.getAllMessages(e));
            return failure;
        }
    }

    protected OperationResult startNode() {
        ResourceContext<?> context = getResourceContext();
        Configuration pluginConfig = context.getPluginConfiguration();
        String baseDir = pluginConfig.getSimpleValue("baseDir");
        File binDir = new File(baseDir, "bin");
        File startScript = new File(binDir, getStartScript());

        ProcessExecution scriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
        SystemInfo systemInfo = context.getSystemInformation();
        ProcessExecutionResults results = systemInfo.executeProcess(scriptExe);

        if  (results.getError() == null) {
            return new OperationResult("Successfully started Cassandra daemon");
        } else {
            OperationResult failure = new OperationResult("Failed to start Cassandra daemon");
            failure.setErrorMessage(ThrowableUtil.getAllMessages(results.getError()));
            return failure;
        }
    }

    protected OperationResult restartNode() {
        OperationResult result = shutdownNode();

        if (result.getErrorMessage() == null) {
            result = startNode();
        }

        return result;
    }

    private String getStartScript() {
        ResourceContext<?> context = getResourceContext();
        SystemInfo systemInfo = context.getSystemInformation();

        if (systemInfo.getOperatingSystemType() == WINDOWS) {
            return "cassandra.bat";
        } else {
            return "cassandra";
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        MeasurementScheduleRequest scheduleRequest = null;
        for (MeasurementScheduleRequest r : metrics) {
            if (r.getName().equals("cluster")) {
                scheduleRequest = r;
                break;
            }
        }

        if (scheduleRequest == null) {
            return;
        }

        Cluster cluster = getCluster();
        report.addData(new MeasurementDataTrait(scheduleRequest, cluster.describeClusterName()));
    }
}
