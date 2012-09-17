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

import static org.rhq.plugins.cassandra.CassandraUtil.getCluster;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.SigarException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.JMXServerComponent;

import me.prettyprint.hector.api.Cluster;

/**
 * @author John Sanda
 */
public class CassandraNodeComponent extends JMXServerComponent implements MeasurementFacet, OperationFacet {

    private Log log = LogFactory.getLog(CassandraNodeComponent.class);

//    private ResourceContext context;
//
//    private EmsConnection emsConnection;
//
//    @Override
//    public void start(ResourceContext context) throws Exception {
//        this.context = context;
//    }
//
//    @Override
//    public void stop() {
//        emsConnection = null;
//    }
//
//    @Override
//    public AvailabilityType getAvailability() {
//        if (emsConnection == null) {
//            getEmsConnection();
//        }
//        return AvailabilityType.UP;
//    }
//
//    @Override
//    public EmsConnection getEmsConnection() {
//        if (emsConnection != null) {
//            return emsConnection;
//        }
//
//        try {
//            Configuration pluginConfig = context.getPluginConfiguration();
//
//            ConnectionSettings connectionSettings = new ConnectionSettings();
//
//            String connectionTypeDescriptorClass = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
//                .getStringValue();
//            PropertySimple serverUrl = pluginConfig
//                .getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY);
//
//            connectionSettings.initializeConnectionType((ConnectionTypeDescriptor) Class.forName(
//                connectionTypeDescriptorClass).newInstance());
//            // if not provided use the default serverUrl
//            if (null != serverUrl) {
//                connectionSettings.setServerUrl(serverUrl.getStringValue());
//            }
//
//            ConnectionFactory connectionFactory = new ConnectionFactory();
//            ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
//            emsConnection = connectionProvider.connect();
//            emsConnection.loadSynchronous(false);
//
//            return emsConnection;
//        } catch (InstantiationException e) {
//            throw new RuntimeException("Failed to get EMS connection", e);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException("Failed to get EMS connection", e);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException("Failed to get EMS connection", e);
//        }
//    }


    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("shutdown")) {
            return shutdown(parameters);
        }
        return null;
    }

    private OperationResult shutdown(Configuration params) {
        ResourceContext context = getResourceContext();
        ProcessInfo process = context.getNativeProcess();
        long pid = process.getPid();
        try {
            process.kill("KILL");
            context.getAvailabilityContext().requestAvailabilityCheck();
            return new OperationResult("Successfully shut down Cassandra node with pid " + pid);
        } catch (SigarException e) {
            log.warn("Failed to shut down Cassandra node with pid " + pid, e);
            return new OperationResult("Failed to shut down Cassandra node with pid " + pid + ": " + e.getMessage());
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
