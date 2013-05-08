/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.metrics.simulator;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleAuthInfoProvider;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Minutes;


import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.metrics.simulator.plan.ClusterConfig;
import org.rhq.metrics.simulator.plan.ScheduleGroup;
import org.rhq.metrics.simulator.plan.SimulationPlan;
import org.rhq.metrics.simulator.stats.Stats;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsServer;

/**
 * @author John Sanda
 */
public class Simulator implements ShutdownManager {

    private final Log log = LogFactory.getLog(Simulator.class);

    private boolean shutdown = false;

    private CassandraClusterManager ccm;

    public void run(SimulationPlan plan) {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(plan.getThreadPoolSize(),
            new SimulatorThreadFactory());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown(executorService);
            }
        });

        List<StorageNode> nodes = initCluster(plan);

        createSchema(nodes);

        Session session;
        if (plan.getClientCompression() == null) {
            session = createSession(nodes);
        } else {
            ProtocolOptions.Compression compression = Enum.valueOf(ProtocolOptions.Compression.class,
                plan.getClientCompression().toUpperCase());
            session = createSession(nodes, compression);
        }

        MetricsServer metricsServer = new MetricsServer();
        metricsServer.setSession(session);
        metricsServer.setConfiguration(plan.getMetricsServerConfiguration());

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(plan.getMetricsServerConfiguration());
        metricsServer.setDateTimeService(dateTimeService);

        Set<Schedule> schedules = initSchedules(plan.getScheduleSets().get(0));
        PriorityQueue<Schedule> queue = new PriorityQueue<Schedule>(schedules);
        ReentrantLock queueLock = new ReentrantLock();

        MeasurementAggregator measurementAggregator = new MeasurementAggregator();
        measurementAggregator.setMetricsServer(metricsServer);
        measurementAggregator.setShutdownManager(this);

        Stats stats = new Stats();
        StatsCollector statsCollector = new StatsCollector(stats);

        log.info("Starting executor service");
        executorService.scheduleAtFixedRate(statsCollector, 0, 1, TimeUnit.MINUTES);


        for (int i = 0; i < plan.getNumMeasurementCollectors(); ++i) {
            MeasurementCollector measurementCollector = new MeasurementCollector();
            measurementCollector.setMetricsServer(metricsServer);
            measurementCollector.setQueue(queue);
            measurementCollector.setQueueLock(queueLock);
            measurementCollector.setStats(stats);
            measurementCollector.setShutdownManager(this);

            executorService.scheduleAtFixedRate(measurementCollector, 0, plan.getCollectionInterval(),
                TimeUnit.MILLISECONDS);
        }

        executorService.scheduleAtFixedRate(measurementAggregator, 0, plan.getAggregationInterval(),
            TimeUnit.MILLISECONDS);

        try {
            Thread.sleep(Minutes.minutes(plan.getSimulationTime()).toStandardDuration().getMillis());
        } catch (InterruptedException e) {
        }
        statsCollector.reportSummaryStats();
        log.info("Simulation has completed. Initiating shutdown...");
        shutdown(0);
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

    private void shutdown(ScheduledExecutorService executorService) {
        log.info("Shutting down executor service");
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        if (!executorService.isTerminated()) {
            log.info("Forcing executor service shutdown.");
            executorService.shutdownNow();
        }
        shutdownCluster();
        log.info("Shut down complete");
    }

    private List<StorageNode> initCluster(SimulationPlan plan) {
        try {
            List<StorageNode> nodes = deployCluster(plan.getClusterConfig());
            waitForClusterToInitialize(nodes);
            return nodes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start simulator. Cluster initialization failed.", e);
        }
    }

    private List<StorageNode> deployCluster(ClusterConfig clusterConfig) throws IOException {
        File clusterDir = new File(clusterConfig.getClusterDir(), "cassandra");
        log.info("Deploying cluster to " + clusterDir);
        clusterDir.mkdirs();

        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir.getAbsolutePath());
        deploymentOptions.setNumNodes(clusterConfig.getNumNodes());
        deploymentOptions.setHeapSize(clusterConfig.getHeapSize());
        deploymentOptions.setHeapNewSize(clusterConfig.getHeapNewSize());
        if (clusterConfig.getStackSize() != null) {
            deploymentOptions.setStackSize(clusterConfig.getStackSize());
        }
        deploymentOptions.setLoggingLevel("INFO");
        deploymentOptions.load();

        ccm = new CassandraClusterManager(deploymentOptions);
        List<StorageNode> nodes = ccm.createCluster();
        ccm.startCluster(false);

        return nodes;
    }

    private void shutdownCluster() {
        log.info("Shutting down cluster");
        ccm.shutdownCluster();
    }

    private void waitForClusterToInitialize(List<StorageNode> nodes) {
        log.info("Waiting for cluster to initialize");
        ClusterInitService clusterInitService = new ClusterInitService();
        clusterInitService.waitForClusterToStart(nodes, nodes.size(), 1500, 20, 2);
    }

    private void createSchema(List<StorageNode> nodes) {
        try {
            log.info("Creating schema");
            SchemaManager schemaManager = new SchemaManager("rhqadmin", "rhqadmin", nodes);
            schemaManager.createSchema();
            schemaManager.updateSchema();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start simulator. An error occurred during schema creation.", e);
        }
    }

    private Session createSession(List<StorageNode> nodes) throws NoHostAvailableException {
        try {
            SimpleAuthInfoProvider authInfoProvider = new SimpleAuthInfoProvider();
            authInfoProvider.add("username", "rhqadmin").add("password", "rhqadmin");

            Cluster cluster = new ClusterBuilder()
                .addContactPoints(getHostNames(nodes))
                .withAuthInfoProvider(authInfoProvider)
                .build();

            log.debug("Created cluster object with " + cluster.getConfiguration().getProtocolOptions().getCompression()
                + " compression.");

            return initSession(cluster);
        } catch (Exception e) {
            log.error("Failed to start simulator. Unable to create " + Session.class, e);
            throw new RuntimeException("Failed to start simulator. Unable to create " + Session.class, e);
        }
    }

    private Session createSession(List<StorageNode> nodes, ProtocolOptions.Compression compression)
        throws NoHostAvailableException {
        try {
            log.debug("Creating session using " + compression.name() + " compression");

            SimpleAuthInfoProvider authInfoProvider = new SimpleAuthInfoProvider();
            authInfoProvider.add("username", "rhqadmin").add("password", "rhqadmin");

            Cluster cluster = new ClusterBuilder()
                .addContactPoints(getHostNames(nodes))
                .withAuthInfoProvider(authInfoProvider)
                .withCompression(compression)
                .build();

            log.debug("Created cluster object with " + cluster.getConfiguration().getProtocolOptions().getCompression()
                + " compression.");

            return initSession(cluster);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start simulator. Unable to create " + Session.class, e);
        }
    }

    private Session initSession(Cluster cluster) {
        NodeFailureListener listener = new NodeFailureListener();
        for (Host host : cluster.getMetadata().getAllHosts()) {
            host.getMonitor().register(listener);
        }

        return cluster.connect("rhq");
    }

    private String[] getHostNames(List<StorageNode> nodes) {
        String[] hostnames = new String[nodes.size()];
        for (int i = 0; i < hostnames.length; ++i) {
            hostnames[i] = nodes.get(i).getAddress();
        }
        return hostnames;
    }

    private Set<Schedule> initSchedules(ScheduleGroup scheduleSet) {
        long nextCollection = System.currentTimeMillis();
        Set<Schedule> schedules = new HashSet<Schedule>();
        for (int i = 0; i < scheduleSet.getCount(); ++i) {
            Schedule schedule = new Schedule(i);
            schedule.setInterval(scheduleSet.getInterval());
            schedule.setNextCollection(nextCollection);
            schedules.add(schedule);
        }
        return schedules;
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
