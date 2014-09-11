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
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.metrics.simulator.plan.SimulationPlan;
import org.rhq.metrics.simulator.plan.SimulationPlan.SimulationType;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
public class Simulator implements ShutdownManager {

    private final Log log = LogFactory.getLog(Simulator.class);

    private boolean shutdown = false;

    private Metrics metrics;
    private Session session;
    private StorageSession storageSession;
    private MetricsDAO metricsDAO;
    private MetricsServer metricsServer;

    public void run(SimulationPlan plan) throws Throwable {
        if (SimulationType.THREADED.equals(plan.getSimulationType())) {
            this.runThreadedSimulation(plan);
        } else if (SimulationType.SEQUENTIAL.equals(plan.getSimulationType())) {
            this.runSequentialSimulation(plan);
        } else {
            throw new UnsupportedOperationException("Simulation type " + plan.getSimulationType() + " not implemented.");
        }
    }

    private void initializeMetricsServer(SimulationPlan plan) {
        metrics = new Metrics();
        createSchema(plan.getNodes(), plan.getCqlPort());
        session = createSession(plan.getNodes(), plan.getCqlPort());
        storageSession = new StorageSession(session);
        metricsDAO = new MetricsDAO(storageSession, plan.getMetricsServerConfiguration());

        metricsServer = new MetricsServer();
        metricsServer.setDAO(metricsDAO);
        metricsServer.setConfiguration(plan.getMetricsServerConfiguration());
        metricsServer.setDateTimeService(plan.getDateTimeService());
        metricsServer.init();
    }

    /**
     * Run a sequential simulation where metrics insertion and aggregation run
     * in a single thread in sequence. The progression of time
     * is managed by the simulation. This guarantees that
     * the number of insertions and sequence of events takes
     * place in order irrespective of host limitations.
     */
    private void runSequentialSimulation(SimulationPlan plan) throws Throwable {
        this.initializeMetricsServer(plan);

        Random random = new Random();
        long timestamp = plan.getDateTimeService().nowInMillis();
        long endOfSimulation = timestamp + 24L * 60 * 60 * 1000 * plan.getSimulationTime();
        int numberOfMetrics = plan.getBatchSize() * plan.getNumMeasurementCollectors();

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>(plan.getBatchSize());

        int lastAggregationHour = new DateTime(timestamp).getHourOfDay();

        for (; timestamp < endOfSimulation; timestamp += 30 * 1000) {
            DateTime currentTime = new DateTime(timestamp);

            data.clear();
            for (int i = 0; i < numberOfMetrics; ++i) {
                data.add(new MeasurementDataNumeric(timestamp, i, random.nextDouble()));
            }

            WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());
            metricsServer.addNumericData(data, waitForRawInserts);
            waitForRawInserts.await("Failed to insert raw data at time: " + timestamp);

            if (currentTime.getHourOfDay() != lastAggregationHour) {
                lastAggregationHour = currentTime.getHourOfDay();
                metricsServer.calculateAggregates();
            }
        }

        metricsServer.shutdown();
        log.info("Simulation has completed. Initiating shutdown...");
        shutdown(0);
    }

    /**
     * Run a multi-threaded simulation where multiple threads
     * collect metrics and run aggregation.
     * The scheduling is done based on intervalType please review the
     * simulation plan for the timing.
     */
    private void runThreadedSimulation(SimulationPlan plan) {
        this.initializeMetricsServer(plan);
        final ConsoleReporter consoleReporter = createConsoleReporter(metrics, plan.getMetricsReportInterval());

        final ScheduledExecutorService aggregators = Executors.newScheduledThreadPool(1, new SimulatorThreadFactory());
        final ScheduledExecutorService collectors = Executors.newScheduledThreadPool(
            plan.getNumMeasurementCollectors(), new SimulatorThreadFactory());
        final ExecutorService aggregationQueue = Executors.newSingleThreadExecutor(new SimulatorThreadFactory());
        final ScheduledExecutorService readers = Executors.newScheduledThreadPool(plan.getReaderThreadPoolSize(),
            new SimulatorThreadFactory());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown(collectors, "collectors", 5);
                shutdown(readers, "readers", 5);
                shutdown(aggregators, "aggregators", 1);
                shutdown(aggregationQueue, "aggregationQueue", Integer.MAX_VALUE);
                metricsServer.shutdown();
                log.info("Wait for console reporter...");
                try {
                    Thread.sleep(181000);
                } catch (InterruptedException e) {
                }
                consoleReporter.stop();
            }
        });

        MeasurementAggregator measurementAggregator = new MeasurementAggregator(metricsServer, this, metrics,
            aggregationQueue, plan.getNumMeasurementCollectors() * plan.getBatchSize());

        for (int i = 0; i < plan.getNumMeasurementCollectors(); ++i) {
            collectors.scheduleAtFixedRate(new MeasurementCollector(plan.getBatchSize(),
                plan.getBatchSize() * i, metrics, metricsServer, plan.getDateTimeService()), 0,
                plan.getCollectionInterval(), TimeUnit.MILLISECONDS);
        }

        if (plan.isAggregationEnabled()) {
            aggregators.scheduleAtFixedRate(measurementAggregator, 0, plan.getAggregationInterval(),
                TimeUnit.MILLISECONDS);
        }

        for (int i = 0; i < plan.getNumReaders(); ++i) {
            MeasurementReader reader = new MeasurementReader(plan.getSimulationRate(), metrics, metricsServer,
                plan.getBatchSize() * i, plan.getBatchSize());
            readers.scheduleAtFixedRate(reader, 30, 30, TimeUnit.SECONDS);
        }

        try {
            Thread.sleep(Minutes.minutes(plan.getSimulationTime()).toStandardDuration().getMillis());
        } catch (InterruptedException e) {
        }
        log.info("Simulation has completed. Initiating shutdown...");
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
        SchemaManager schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc", nodes,
            cqlPort);
        try {
            log.info("Creating schema");
            schemaManager.install();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start simulator. An error occurred during schema creation.", e);
        } finally {
            schemaManager.shutdown();
        }
    }

    private Session createSession(String[] nodes, int cqlPort) throws NoHostAvailableException {
        try {
            Cluster cluster = new ClusterBuilder().addContactPoints(nodes).withPort(cqlPort)
                .withCredentials("rhqadmin", "rhqadmin")
                .build();
            PoolingOptions poolingOptions = cluster.getConfiguration().getPoolingOptions();
            poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, 24);
            poolingOptions.setCoreConnectionsPerHost(HostDistance.REMOTE, 24);
            poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, 32);
            poolingOptions.setMaxConnectionsPerHost(HostDistance.REMOTE, 32);

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