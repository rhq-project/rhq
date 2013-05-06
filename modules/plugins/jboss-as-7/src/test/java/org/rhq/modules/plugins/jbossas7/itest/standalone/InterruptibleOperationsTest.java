/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.when;
import static org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP;
import static org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration.Property.HOSTNAME;
import static org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration.Property.MANAGEMENT_CONNECTION_TIMEOUT;
import static org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration.Property.PASSWORD;
import static org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration.Property.PORT;
import static org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration.Property.USER;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.StringUtil;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Check that management operations that might be interrupted by AS server (connection closed before the http response
 * is sent) don't make RHQ operations fail.
 *
 * See https://bugzilla.redhat.com/show_bug.cgi?id=911321
 *
 * @author Thomas Segismont
 */
public class InterruptibleOperationsTest {

    private static final Log LOG = LogFactory.getLog(InterruptibleOperationsTest.class);

    private static final String HTTP_HOST = "localhost";

    private static final int HTTP_PORT = 31159;

    private static final String HTTP_PORT_VARIABLE = "as7.itest.mock.http.management.server.port";

    @Mock
    private ResourceContext resourceContext;

    private StandaloneASComponent serverComponent;

    private Server jettyServer;

    private ExecutorService executorService;

    @BeforeMethod
    private void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        serverComponent = new TestStandaloneASComponent();
        int httpPort = getJettyPort();
        setupResourceContext(httpPort);
        setupJettyServer(httpPort);
        jettyServer.start();
        executorService = Executors.newSingleThreadExecutor();
        serverComponent.start(resourceContext);
    }

    private void setupJettyServer(int httpPort) {
        jettyServer = new Server(new InetSocketAddress(HTTP_HOST, httpPort));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        jettyServer.setHandler(context);
        HttpServlet testServlet = new TestServlet();
        context.addServlet(new ServletHolder(testServlet), "/*");
    }

    private static int getJettyPort() {
        String httpPortVariable = System.getProperty(HTTP_PORT_VARIABLE);
        if (StringUtil.isNotBlank(httpPortVariable)) {
            try {
                int port = Integer.parseInt(httpPortVariable);
                LOG.info("Using port " + httpPortVariable + " for http server");
                return port;
            } catch (NumberFormatException e) {
                LOG.warn("Invalid port variable: " + httpPortVariable);
            }
        }
        LOG.info("Using default port " + String.valueOf(HTTP_PORT) + " for http server");
        return HTTP_PORT;
    }

    private void setupResourceContext(int httpPort) {
        when(resourceContext.getPluginConfiguration()).thenReturn(pluginConfig(httpPort));
        when(resourceContext.getResourceKey()).thenReturn("/TestServer");
        when(resourceContext.getSystemInformation()).thenReturn(SystemInfoFactory.createJavaSystemInfo());
        when(resourceContext.getAvailabilityContext()).thenReturn(Mockito.mock(AvailabilityContext.class));
    }

    private Configuration pluginConfig(int httpPort) {
        Configuration pluginConfig = new Configuration();
        pluginConfig.setSimpleValue(HOSTNAME, "localhost");
        pluginConfig.setSimpleValue(PORT, String.valueOf(httpPort));
        pluginConfig.setSimpleValue(USER, "pipo");
        pluginConfig.setSimpleValue(PASSWORD, "molo");
        pluginConfig.setSimpleValue(MANAGEMENT_CONNECTION_TIMEOUT, "-1");
        pluginConfig.getMap().put(LOG_EVENT_SOURCES_CONFIG_PROP, new PropertyList());
        return pluginConfig;
    }

    @AfterMethod
    private void tearDown() throws Exception {
        LOG.info("Shutting down Jetty test server");
        try {
            if (jettyServer != null) {
                jettyServer.stop();
            }
        } catch (Exception ignore) {
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test(timeOut = 60 * 1000)
    public void testReloadOperation() throws Exception {
        OperationResult operationResult = serverComponent.invokeOperation("reload", new Configuration());
        assertEquals(operationResult.getSimpleResult(), "Success");
    }

    @Test(timeOut = 60 * 1000)
    public void testShutdown() throws Exception {
        OperationResult operationResult = serverComponent.invokeOperation("shutdown", new Configuration());
        assertEquals(operationResult.getSimpleResult(), "Success");
    }

    private class RestartJetty implements Runnable {
        @Override
        public void run() {
            try {
                jettyServer.stop();
            } catch (Exception ignore) {
            }
            try {
                Thread.sleep(SECONDS.toMillis(2));
            } catch (Exception ignore) {
            }
            try {
                jettyServer.start();
            } catch (Exception ignore) {
            }
        }
    }

    private class TestServlet extends HttpServlet {

        private ObjectMapper objectMapper = new ObjectMapper();

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Operation operation = objectMapper.readValue(req.getInputStream(), Operation.class);
            // Check if we recevied an operation which a real http management interface might interrupt 
            if (operation.getOperation().equals("reload") || operation.getOperation().equals("shutdown")) {
                // Schedule a Jetty restart 
                executorService.submit(new RestartJetty());
                // Then wait until Jetty is shutdown
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ignore) {
                }
                return;
            }
            // Standard operation. Return simple success
            Result result = new Result();
            result.setOutcome("success");
            objectMapper.writeValue(resp.getOutputStream(), result);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            doPost(req, resp);
        }

    }

    // Tweak StandaloneASComponent implementation for this test case
    private class TestStandaloneASComponent extends StandaloneASComponent {

        @Override
        public AvailabilityType getAvailability() {
            // Avoid various management requests when component is started  
            return AvailabilityType.DOWN;
        }

        @Override
        protected boolean waitUntilDown() throws InterruptedException {
            // Standard implementation relies on discovery of some resource properties
            return true;
        }
    }

}
