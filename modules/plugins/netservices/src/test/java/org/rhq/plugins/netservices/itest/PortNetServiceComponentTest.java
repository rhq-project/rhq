/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.netservices.itest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.util.StringUtil;
import org.rhq.plugins.netservices.PortNetServiceComponent;
import org.rhq.plugins.netservices.PortNetServiceComponent.ConfigKeys;

/**
 * @author Thomas Segismont
 */
public class PortNetServiceComponentTest extends NetServiceComponentTest {

    private static final Log LOG = LogFactory.getLog(PortNetServiceComponentTest.class);

    private static final String SERVICE_NAME = "PortService";

    private static final String LOOPBACK = "127.0.0.1";

    private PortNetServiceComponent portNetServiceComponent;

    private ServerSocket serverSocket;

    private int serverSocketLocalPort;

    private Thread acceptorThread;

    private volatile boolean stopListening = false;

    @BeforeClass
    public void startSocketServer() throws Exception {
        LOG.info("Setting up a socket server");
        // Let's bind a server socket to any available port
        serverSocket = new ServerSocket(0, 50, InetAddress.getLocalHost());
        // Do not block indefinitely on call to #accept
        serverSocket.setSoTimeout(1000);
        serverSocketLocalPort = serverSocket.getLocalPort();
        acceptorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stopListening) {
                    try {
                        Socket socket = serverSocket.accept();
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        });
        acceptorThread.setDaemon(true);
        acceptorThread.start();
    }

    @AfterClass
    public void stopSocketServer() {
        LOG.info("Shutting down socket server");
        stopListening = true;
        try {
            serverSocket.close();
        } catch (IOException ignore) {
        }
    }

    @Test(dependsOnMethods = "testPluginLoad")
    public void testManualAdd() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setSimpleValue(ConfigKeys.ADDRESS, LOOPBACK);
        configuration.setSimpleValue(ConfigKeys.PORT, String.valueOf(serverSocketLocalPort));
        MergeResourceResponse response = getInventoryManager().manuallyAddResource(
            getPluginManager().getMetadataManager().getType(SERVICE_NAME, PLUGIN_NAME), getPlatform().getId(),
            configuration, -1);
        assertNotNull(response, "Manual add response is null");
        @SuppressWarnings("rawtypes")
        ResourceComponent resourceComponent = getInventoryManager().getResourceContainer(response.getResourceId())
            .getResourceComponent();
        assertEquals(resourceComponent.getClass(), PortNetServiceComponent.class);
        portNetServiceComponent = (PortNetServiceComponent) resourceComponent;
    }

    @Test(dependsOnMethods = "testManualAdd")
    public void testAvailability() throws Exception {
        assertEquals(portNetServiceComponent.getAvailability(), AvailabilityType.UP);
    }

    @Test(dependsOnMethods = "testAvailability")
    public void testMeasurement() throws Exception {
        MeasurementReport report = new MeasurementReport();
        Set<MeasurementScheduleRequest> metrics = new HashSet<MeasurementScheduleRequest>();
        int scheduleId = 1;
        metrics.add(new MeasurementScheduleRequest(scheduleId++, "ipAddress", 1000, true, DataType.TRAIT));
        metrics.add(new MeasurementScheduleRequest(scheduleId++, "hostName", 1000, true, DataType.TRAIT));
        metrics.add(new MeasurementScheduleRequest(scheduleId++, "connectTime", 1000, true, DataType.MEASUREMENT));
        portNetServiceComponent.getValues(report, metrics);
        Map<String, Object> datas = new HashMap<String, Object>();
        for (MeasurementData data : report.getNumericData()) {
            datas.put(data.getName(), data.getValue());
        }
        for (MeasurementData data : report.getTraitData()) {
            datas.put(data.getName(), data.getValue());
        }
        assertEquals(getTrait(datas, "ipAddress"), LOOPBACK);
        assertTrue(StringUtil.isNotBlank(getTrait(datas, "hostName")));
        assertTrue(getMetric(datas, "connectTime") >= 0);
    }

}
