/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.test.arquillian.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyBuilder;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.util.file.FileUtil;
import org.rhq.test.shrinkwrap.FilteredView;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * 
 * @author Lukas Krejci
 */
public class RhqAgentPluginContainer implements DeployableContainer<RhqAgentPluginContainerConfiguration> {

    private static final AtomicInteger CONTAINER_COUNT = new AtomicInteger(0);
    private static final File DEPLOYMENT_ROOT;

    private static final String PLUGINS_DIR_NAME = "plugins";
    private static final String DATA_DIR_NAME = "data";
    private static final String TMP_DIR_NAME = "tmp";
    private static final String LIB_DIR_NAME = "lib";

    static {
        File f;
        try {
            f = FileUtil.createTempDirectory("TEST_RHQ_PC_DEPLOYMENTS", null, null);
        } catch (IOException e) {
            f = null;
            throw new IllegalStateException(
                "Could not create the root directory for RHQ plugin container test deployments");
        }

        DEPLOYMENT_ROOT = f;
    }

    private static class ExcludeDirectory implements Filter<ArchivePath> {

        private ArchivePath root;

        public ExcludeDirectory(ArchivePath root) {
            this.root = root;
        }

        @Override
        public boolean include(ArchivePath object) {
            return !object.get().startsWith(root.get());
        }

    }

    private static final Log LOG = LogFactory.getLog(RhqAgentPluginContainer.class);

    private RhqAgentPluginContainerConfiguration configuration;
    private File deploymentDirectory;

    @Inject
    private Instance<Container> container;

    @Override
    public Class<RhqAgentPluginContainerConfiguration> getConfigurationClass() {
        return RhqAgentPluginContainerConfiguration.class;
    }

    public static PluginContainer switchPluginContainer(String deploymentName) throws Exception {
        Method setInstance = PluginContainer.class.getMethod("setContainerInstance", String.class);
        setInstance.invoke(null, deploymentName);

        return PluginContainer.getInstance();
    }

    public static PluginContainer getPluginContainer(String deploymentName) throws Exception {
        Method getInstance = PluginContainer.class.getMethod("getContainerInstance", String.class);

        return (PluginContainer) getInstance.invoke(null, deploymentName);
    }

    @Override
    public void setup(RhqAgentPluginContainerConfiguration configuration) {
        this.configuration = configuration;
        finalizeConfiguration(this.configuration);

        try {
            //just try it out early
            switchPcInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Could not instantiate a modified PluginContainer");
        }
    }

    @Override
    public void start() throws LifecycleException {
        CONTAINER_COUNT.incrementAndGet();
        try {
            switchPcInstance();
        } catch (Exception e) {
            throw new LifecycleException("Failed to switch plugin container.", e);
        }
        startPc();
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            switchPcInstance();
        } catch (Exception e) {
            throw new LifecycleException("Failed to switch plugin container.", e);
        }

        stopPc();

        if (CONTAINER_COUNT.decrementAndGet() == 0) {
            purgePcDeployments();
        }
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Local");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        try {
            switchPcInstance();
        } catch (Exception e) {
            throw new DeploymentException("Failed to switch plugin container.", e);
        }

        RhqAgentPluginArchive plugin = archive.as(RhqAgentPluginArchive.class);

        Node descriptor = plugin.get(ArchivePaths.create("META-INF/rhq-plugin.xml"));
        if (descriptor == null) {
            throw new DeploymentException("Archive [" + archive + "] doesn't specify an RHQ plugin descriptor.");
        }

        boolean wasStarted = stopPc();

        deployPlugin(plugin);

        if (wasStarted) {
            startPc();
        }

        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        try {
            switchPcInstance();
        } catch (Exception e) {
            throw new DeploymentException("Failed to switch plugin container.", e);
        }

        RhqAgentPluginArchive plugin = archive.as(RhqAgentPluginArchive.class);

        boolean wasStarted = stopPc();

        File pluginDeploymentPath = getDeploymentPath(plugin);

        if (!pluginDeploymentPath.delete()) {
            if (File.separatorChar == '/') {
                throw new DeploymentException("Could not delete the RHQ plugin '" + plugin.getName());
            } else {
                // TODO: file locking, probably due to 
                // http://management-platform.blogspot.com/2009/01/classloaders-keeping-jar-files-open.html,
                // is not allowing deletion. Perhaps this can be fixed at some point.                
            }
        }

        if (wasStarted) {
            startPc();
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    public RhqAgentPluginContainerConfiguration getConfiguration() {
        return configuration;
    }

    private File getDeploymentPath(Archive<?> plugin) {
        return new File(configuration.getPluginDirectory(), plugin.getName());
    }

    private void finalizeConfiguration(RhqAgentPluginContainerConfiguration config) {
        String arquillianContainerName = container.get().getName();
        String pluginContainerName = (arquillianContainerName) != null ? arquillianContainerName : UUID.randomUUID()
            .toString();
        config.setContainerName(pluginContainerName);

        deploymentDirectory = new File(DEPLOYMENT_ROOT, pluginContainerName);

        File pluginsDir = new File(deploymentDirectory, PLUGINS_DIR_NAME);
        pluginsDir.mkdirs();
        File dataDir = new File(deploymentDirectory, DATA_DIR_NAME);
        dataDir.mkdirs();
        File tmpDir = new File(deploymentDirectory, TMP_DIR_NAME);
        tmpDir.mkdirs();

        config.setPluginDirectory(pluginsDir);
        config.setDataDirectory(dataDir);
        config.setTemporaryDirectory(tmpDir);

        if (config.isNativeSystemInfoEnabled()) {
            // Setup the lib dir.
            File libDir = new File(deploymentDirectory, LIB_DIR_NAME);
            installSigarNativeLibraries(libDir);
            // The Sigar class uses the below sysprop to locate the SIGAR native libraries.
            System.setProperty("org.hyperic.sigar.path", libDir.getPath());
        }

        if (config.getServerServicesImplementationClassName() != null) {
            try {
                Class<?> serverServicesClass = Class.forName(config.getServerServicesImplementationClassName());
                ServerServices serverServices = (ServerServices) serverServicesClass.newInstance();

                config.setServerServices(serverServices);
            } catch (Exception e) {
                throw new IllegalArgumentException("The serverServicesImplementationClassName property is invalid", e);
            }
        }
    }

    private void installSigarNativeLibraries(File targetDir) {
        LOG.debug("Installing SIGAR native libraries to [" + targetDir + "]...");
        MavenDependencyResolver mavenDependencyResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        // TODO: Don't hard-code the SIGAR version.
        MavenDependencyBuilder sigarDistArtifact = mavenDependencyResolver.loadEffectivePom("pom.xml").artifact(
            "org.hyperic:sigar-dist:zip:1.6.5.132");
        JavaArchive sigarDistArchive = sigarDistArtifact.resolveAs(JavaArchive.class).iterator().next();
        File tempDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tempDir.mkdirs();
        String explodedDirName = "sigar-dist";
        sigarDistArchive.as(ExplodedExporter.class).exportExploded(tempDir, explodedDirName);
        // TODO: Don't hard-code the SIGAR version.
        File sigarLibDir = new File(tempDir, explodedDirName + "/hyperic-sigar-1.6.5/sigar-bin/lib");
        // Make sure the target dir does not exist, since FileUtil.copyDirectory() requires that to be the case.
        FileUtil.purge(targetDir, true);
        try {
            FileUtil.copyDirectory(sigarLibDir, targetDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy SIGAR shared libraries from [" + sigarLibDir + "] to ["
                + targetDir + "].", e);
        } finally {
            FileUtil.purge(tempDir, true);
        }
    }

    private static void purgePcDeployments() {
        FileUtil.purge(DEPLOYMENT_ROOT, true);
    }

    /**
     * Starts the plugin container.
     * @return true if the plugin container needed to be started (i.e. was not running before), false otherwise.
     */
    private boolean startPc() {
        PluginContainer pc = PluginContainer.getInstance();
        if (pc.isStarted()) {
            return false;
        }

        //always refresh the plugin finder so that it reports all the plugins
        //each time (and doesn't remember the plugins from previous PC runs)
        configuration.setPluginFinder(new FileSystemPluginFinder(configuration.getPluginDirectory()));

        pc.setConfiguration(configuration);
        pc.initialize();
        return true;
    }

    /**
     * Stops the plugin container.
     * @return true if PC was running before this call, false otherwise
     */
    private boolean stopPc() {
        PluginContainer pc = PluginContainer.getInstance();
        if (pc.isStarted()) {
            pc.shutdown();
            return true;
        }

        return false;
    }

    private void deployPlugin(RhqAgentPluginArchive plugin) {
        if (plugin.getRequiredPlugins() != null) {
            for (Archive<?> a : plugin.getRequiredPlugins()) {
                RhqAgentPluginArchive p = a.as(RhqAgentPluginArchive.class);
                deployPlugin(p);
            }
        }

        File pluginDeploymentPath = getDeploymentPath(plugin);
        plugin.as(FilteredView.class).filterContents(new ExcludeDirectory(plugin.getRequiredPluginsPath()))
            .as(ZipExporter.class).exportTo(pluginDeploymentPath, true);
    }

    private PluginContainer switchPcInstance() throws Exception {
        return switchPluginContainer(container.get().getName());
    }

}
