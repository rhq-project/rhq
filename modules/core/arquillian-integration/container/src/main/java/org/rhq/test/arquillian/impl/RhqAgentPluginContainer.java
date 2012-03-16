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
import java.util.HashMap;
import java.util.Map;
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
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.file.FileUtil;
import org.rhq.test.arquillian.impl.util.SigarInstaller;
import org.rhq.test.shrinkwrap.FilteredView;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * 
 * @author Lukas Krejci
 */
public class RhqAgentPluginContainer implements DeployableContainer<RhqAgentPluginContainerConfiguration> {

    private static final AtomicInteger CONTAINER_COUNT = new AtomicInteger(0);
    private static final File DEPLOYMENT_ROOT;
    private static final File ROOT;
    private static final String PLUGINS_DIR_NAME = "plugins";
    private static final String DATA_DIR_NAME = "data";
    private static final String TMP_DIR_NAME = "tmp";
    
    private static final Map<String, Boolean> NATIVE_SYSTEM_INFO_ENABLEMENT_PER_PC = new HashMap<String, Boolean>();
    
    static {
        File root;
        File deployments;
        File sigar;
        try {
            root = FileUtil.createTempDirectory("TEST_RHQ_PC_DEPLOYMENTS", null, null);
            deployments = new File(root, "pcs");
            deployments.mkdir();
            
            sigar = new File(root, "sigar");
            sigar.mkdir();
        } catch (IOException e) {
            root = null;
            deployments = null;
            throw new IllegalStateException(
                "Could not create the root directory for RHQ plugin container test deployments");
        }

        ROOT = root;
        DEPLOYMENT_ROOT = deployments;
        
        //install sigar if available
        SigarInstaller installer = new SigarInstaller(sigar);
        if (installer.isSigarAvailable()) {
            installer.installSigarNativeLibraries();
        }
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

    public static void init() {
        //this is just a dummy method that other classes can call to force the static
        //initialization of this class.
    }
    
    public static PluginContainer switchPluginContainer(String deploymentName) throws Exception {
        Method setInstance = PluginContainer.class.getMethod("setContainerInstance", String.class);
        setInstance.invoke(null, deploymentName);
              
        Boolean enableNativeInfo = NATIVE_SYSTEM_INFO_ENABLEMENT_PER_PC.get(deploymentName);
        
        if (enableNativeInfo == null || !enableNativeInfo.booleanValue()) {
            SystemInfoFactory.disableNativeSystemInfo();
        } else {
            SystemInfoFactory.enableNativeSystemInfo();
        }
        
        LOG.info("Switched PluginContainer to '" + deploymentName + "'.");
        
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
    }

    @Override
    public void start() throws LifecycleException {
        CONTAINER_COUNT.incrementAndGet();
        try {
            switchPcInstance();
        } catch (Exception e) {
            throw new LifecycleException("Failed to switch plugin container.", e);
        }
        
        LOG.info("Starting PluginContainer " + container.get().getName());
        
        startPc();
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            switchPcInstance();
        } catch (Exception e) {
            throw new LifecycleException("Failed to switch plugin container.", e);
        }
        
        LOG.info("Stopping PluginContainer " + container.get().getName());
        
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
        LOG.info("Deploying " + archive + " to PluginContainer " + container.get().getName());
        
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

        LOG.info("Done deploying " + archive + " to PluginContainer " + container.get().getName());
        
        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        LOG.info("Undeploying " + archive + " from PluginContainer " + container.get().getName());
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
        
        LOG.info("Done undeploying " + archive + " from PluginContainer " + container.get().getName());        
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

        NATIVE_SYSTEM_INFO_ENABLEMENT_PER_PC.put(arquillianContainerName, config.isNativeSystemInfoEnabled());
                
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

    private static void purgePcDeployments() {
        FileUtil.purge(ROOT, true);
    }

    /**
     * Starts the plugin container.
     * @return true if the plugin container needed to be started (i.e. was not running before), false otherwise.
     */
    private boolean startPc() {
        LOG.debug("Starting PluginContainer on demand");
        
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
        LOG.debug("Stopping PluginContainer on demand");
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
