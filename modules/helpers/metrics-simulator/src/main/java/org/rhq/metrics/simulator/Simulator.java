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

package org.rhq.metrics.simulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Minutes;

import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.metrics.simulator.plan.SimulationPlan;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
public class Simulator implements ShutdownManager {

    private final Log log = LogFactory.getLog(Simulator.class);

    private boolean shutdown = false;

    public void run(SimulationPlan plan) {
        final ScheduledExecutorService aggregators = Executors.newScheduledThreadPool(1, new SimulatorThreadFactory());
        final ScheduledExecutorService collectors = Executors.newScheduledThreadPool(
            plan.getNumMeasurementCollectors(), new SimulatorThreadFactory());
        final ExecutorService aggregationQueue = Executors.newSingleThreadExecutor(new SimulatorThreadFactory());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown(collectors, "collectors", 5);
                shutdown(aggregators, "aggregators", 1);
                shutdown(aggregationQueue, "aggregationQueue", Integer.MAX_VALUE);
            }
        });

        createSchema(plan.getNodes(), plan.getCqlPort());

        Session session = createSession(plan.getNodes(), plan.getCqlPort());
        StorageSession storageSession = new StorageSession(session);

        MetricsDAO metricsDAO = new MetricsDAO(storageSession, plan.getMetricsServerConfiguration());
        MetricsServer metricsServer = new MetricsServer();
        metricsServer.setDAO(metricsDAO);
        metricsServer.setConfiguration(plan.getMetricsServerConfiguration());

        SimulatorDateTimeService dateTimeService = new SimulatorDateTimeService();
        dateTimeService.setConfiguration(plan.getMetricsServerConfiguration());
        metricsServer.setDateTimeService(dateTimeService);

        Metrics metrics = new Metrics();

        MeasurementAggregator measurementAggregator = new MeasurementAggregator(metricsServer, this, metrics,
            aggregationQueue);

        ConsoleReporter consoleReporter = createConsoleReporter(metrics, plan.getMetricsReportInterval());

        for (int i = 0; i < plan.getNumMeasurementCollectors(); ++i) {
            collectors.scheduleAtFixedRate(new MeasurementCollector(plan.getBatchSize(),
                plan.getBatchSize() * i, metrics, metricsServer, dateTimeService), 0, plan.getCollectionInterval(),
                TimeUnit.MILLISECONDS);
        }

        aggregators.scheduleAtFixedRate(measurementAggregator, 0, plan.getAggregationInterval(),
            TimeUnit.MILLISECONDS);
        try {
            Thread.sleep(Minutes.minutes(plan.getSimulationTime()).toStandardDuration().getMillis());
        } catch (InterruptedException e) {
        }
        log.info("Simulation has completed. Initiating shutdown...");
        consoleReporter.stop();
        shutdown(0);
    }

    private ConsoleReporter createConsoleReporter(Metrics metrics, int reportInterval) {
        try {
            File basedir = new File(System.getProperty("rhq.metrics.simulator.basedir"));
            File logDir = new File(basedir, "log");
            ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(metrics.registry)
                .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
                .outputTo(new PrintStream(new FileOutputStream(new File(logDir, "metrics.txt")))).build();
            consoleReporter.start(reportInterval, TimeUnit.SECONDS);
            return consoleReporter;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to create console reporter", e);
        }
    }

    @Override
    public synchronized void shutdown(int status) {
        if (shutdown) {
            return;
        }

        shutdown = true;
        log.info("Preparing to shutdown simulator...");
        System.exit(status);
    }

    private void shutdown(ExecutorService service, String serviceName, int wait) {
        log.info("Shutting down " + serviceName);
        service.shutdown();
        try {
            service.awaitTermination(wait, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        if (!service.isTerminated()) {
            log.info("Forcing " + serviceName + " shutdown.");
            service.shutdownNow();
        }
        log.info(serviceName + " shut down complete");
    }

    private void createSchema(String[] nodes, int cqlPort) {
        try {
            log.info("Creating schema");
            SchemaManager schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc",
                new String[] {"127.0.0.1"}, 9142);
            schemaManager.install();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start simulator. An error occurred during schema creation.", e);
        }
    }

    private Session createSession(String[] nodes, int cqlPort) throws NoHostAvailableException {
        try {
            Cluster cluster = new ClusterBuilder().addContactPoints(nodes).withPort(cqlPort)
                .withCredentials("rhqadmin", "rhqadmin")
                .build();

            log.debug("Created cluster object with " + cluster.getConfiguration().getProtocolOptions().getCompression()
                + " compression.");

            return initSession(cluster);
        } catch (Exception e) {
            log.error("Failed to start simulator. Unable to create " + Session.class, e);
            throw new RuntimeException("Failed to start simulator. Unable to create " + Session.class, e);
        }
    }

    @SuppressWarnings("deprecation")
    private Session initSession(Cluster cluster) {
        NodeFailureListener listener = new NodeFailureListener();
        for (Host host : cluster.getMetadata().getAllHosts()) {
            host.getMonitor().register(listener);
        }

        return cluster.connect("rhq");
    }

    private static class NodeFailureListener implements Host.StateListener {

        private Log log = LogFactory.getLog(NodeFailureListener.class);

        @Override
        public void onAdd(Host host) {
        }

        @Override
        public void onUp(Host host) {
        }

        @Override
        public void onDown(Host host) {
            log.warn("Node " + host + " has gone down.");
            log.warn("Preparing to shutdown simulator...");
            System.exit(1);
        }

        @Override
        public void onRemove(Host host) {
        }
    }

}
