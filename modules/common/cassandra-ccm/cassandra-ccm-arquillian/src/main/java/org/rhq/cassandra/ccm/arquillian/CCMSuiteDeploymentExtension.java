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

package org.rhq.cassandra.ccm.arquillian;

import java.io.File;
import java.util.concurrent.Callable;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.event.DeployDeployment;
import org.jboss.arquillian.container.spi.event.DeploymentEvent;
import org.jboss.arquillian.container.spi.event.UnDeployDeployment;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
import org.jboss.arquillian.container.test.impl.client.deployment.event.GenerateDeployment;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStarted;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.context.ClassContext;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.cassandra.schema.SchemaManager;

/**
 * @author John Sanda
 */
public class CCMSuiteDeploymentExtension implements LoadableExtension {

    public void register(ExtensionBuilder builder) {
        builder.observer(SuiteDeployer.class);
    }

    public static class SuiteDeployer {

        private Class<?> deploymentClass;
        private DeploymentScenario suiteDeploymentScenario;
        private CassandraClusterManager ccm;

        @Inject
        @ClassScoped
        private InstanceProducer<DeploymentScenario> classDeploymentScenario;

        @Inject
        private Event<DeploymentEvent> deploymentEvent;

        @Inject
        private Event<GenerateDeployment> generateDeploymentEvent;

        @Inject
        // Active some form of ClassContext around our deployments due to assumption bug in AS7 extension.
        private Instance<ClassContext> classContext;

        public void startup(@Observes(precedence = -100)
        ManagerStarted event, ArquillianDescriptor descriptor) {
            deploymentClass = getDeploymentClass(descriptor);

            executeInClassScope(new Callable<Void>() {
                public Void call() throws Exception {
                    generateDeploymentEvent.fire(new GenerateDeployment(new TestClass(deploymentClass)));
                    suiteDeploymentScenario = classDeploymentScenario.get();
                    return null;
                }
            });
        }

        public void initCassandra(@Observes(precedence = -100)
        final BeforeStart event, ArquillianDescriptor descriptor) {

            executeInClassScope(new Callable<Void>() {
                public Void call() throws Exception {
                    boolean deploy = Boolean.valueOf(System.getProperty("rhq.storage.deploy", "true"));
                    if (!deploy) {
                        return null;
                    }

                    SchemaManager schemaManager = null;
                    try {
                        ClusterInitService clusterInitService = new ClusterInitService();

                        String[] nodes = null;
                        int[] jmxPorts = null;
                        int cqlPort = -1;

                        if (!Boolean.valueOf(System.getProperty("itest.use-external-storage-node", "false"))) {

                            DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
                            DeploymentOptions options = factory.newDeploymentOptions();
                            File basedir = new File("target");
                            File clusterDir = new File(basedir, "cassandra");

                            options.setUsername("rhqadmin");
                            options.setPassword("1eeb2f255e832171df8592078de921bc");
                            options.setClusterDir(clusterDir.getAbsolutePath());
                            options.setHeapSize("256M");
                            options.setHeapNewSize("64M");
                            options.setStartRpc(true);

                            ccm = new CassandraClusterManager(options);
                            ccm.createCluster();

                            nodes = ccm.getNodes();
                            jmxPorts = ccm.getJmxPorts();
                            cqlPort = ccm.getCqlPort();

                            ccm.startCluster(false);

                            try {
                                clusterInitService.waitForClusterToStart(nodes, jmxPorts, nodes.length, 2000, 20, 10);
                                schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc", nodes,
                                    cqlPort);
                            } catch (Exception e) {
                                if (null != ccm) {
                                    ccm.shutdownCluster();
                                }
                                throw new RuntimeException("Cassandra cluster initialization failed", e);
                            }
                        } else {
                            try {
                                String nodesString = System.getProperty("rhq.storage.nodes", "127.0.0.1");
                                nodes = nodesString.split(",");

                                String cqlPortString = System.getProperty("rhq.storage.cql-port", "9042");
                                cqlPort = Integer.parseInt(cqlPortString);

                                String jmxPortString = System.getProperty("rhq.storage.jmx-port", "7299");
                                jmxPorts = new int[] { Integer.parseInt(jmxPortString) };

                                schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc", nodes, cqlPort);

                            } catch (Exception e) {
                                throw new RuntimeException("External Cassandra initialization failed", e);
                            }
                        }
                        try {
                            schemaManager.install();
                            clusterInitService.waitForSchemaAgreement(nodes, jmxPorts);
                            schemaManager.updateTopology();
                        } catch (Exception e) {
                            if (null != ccm) {
                                ccm.shutdownCluster();
                            }
                            throw new RuntimeException("Cassandra schema initialization failed", e);
                        }
                        return null;
                    } finally {
                        if (schemaManager != null) {
                            schemaManager.shutdown();
                        }
                    }
                }
            });
        }

        public void deploy(@Observes
        final AfterStart event, final ContainerRegistry registry) {
            executeInClassScope(new Callable<Void>() {
                public Void call() throws Exception {
                    for (Deployment d : suiteDeploymentScenario.deployments()) {
                        deploymentEvent.fire(new DeployDeployment(findContainer(registry,
                            event.getDeployableContainer()), d));
                    }
                    return null;
                }
            });
        }

        public void undeploy(@Observes
        final BeforeStop event, final ContainerRegistry registry) {
            executeInClassScope(new Callable<Void>() {
                public Void call() throws Exception {
                    for (Deployment d : suiteDeploymentScenario.deployments()) {
                        deploymentEvent.fire(new UnDeployDeployment(findContainer(registry,
                            event.getDeployableContainer()), d));
                    }
                    return null;
                }
            });
        }

        public void shutdownCassandra(@Observes
        final AfterStop event, ArquillianDescriptor descriptor) {
            executeInClassScope(new Callable<Void>() {
                public Void call() throws Exception {
                    boolean shutdown = Boolean.valueOf(System.getProperty("rhq.storage.shutdown", "true"));

                    if (null != ccm && shutdown) {
                        ccm.shutdownCluster();
                    }

                    return null;
                }
            });
        }

        public void overrideBefore(@Observes
        EventContext<BeforeClass> event) {
            // Don't continue TestClass's BeforeClass context as normal.
            // No DeploymentGeneration or Deploy will take place.

            classDeploymentScenario.set(suiteDeploymentScenario);
        }

        public void overrideAfter(@Observes
        EventContext<AfterClass> event) {
            // Don't continue TestClass's AfterClass context as normal.
            // No UnDeploy will take place.
        }

        private void executeInClassScope(Callable<Void> call) {
            try {
                classContext.get().activate(deploymentClass);
                call.call();
            } catch (Exception e) {
                throw new RuntimeException("Could not invoke operation", e);
            } finally {
                classContext.get().deactivate();
            }
        }

        private Container findContainer(ContainerRegistry registry, DeployableContainer<?> deployable) {
            for (Container container : registry.getContainers()) {
                if (container.getDeployableContainer() == deployable) {
                    return container;
                }
            }
            return null;
        }

        private Class<?> getDeploymentClass(ArquillianDescriptor descriptor) {
            if (descriptor == null) {
                throw new IllegalArgumentException("Descriptor must be specified");
            }
            String className = descriptor.extension("suite").getExtensionProperties().get("deploymentClass");
            if (className == null) {
                throw new IllegalArgumentException(
                    "A extension element with property deploymentClass must be specified in arquillian.xml");
            }
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not load defined deploymentClass: " + className, e);
            }
        }
    }
}
