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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

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
import org.jboss.shrinkwrap.impl.base.path.BasicPath;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
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

    private static final ArchivePath PLUGIN_DESCRIPTOR_PATH = new BasicPath("META-INF", "rhq-plugin.xml");

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
        PluginContainer oldInstance = PluginContainer.getInstance();
        Method setInstance = PluginContainer.class.getMethod("setContainerInstance", String.class);
        setInstance.invoke(null, deploymentName);
        PluginContainer newInstance = PluginContainer.getInstance();

        if (newInstance != oldInstance) {
            Boolean enableNativeInfo = NATIVE_SYSTEM_INFO_ENABLEMENT_PER_PC.get(deploymentName);

            if (enableNativeInfo == null || !enableNativeInfo.booleanValue()) {
                SystemInfoFactory.disableNativeSystemInfo();
            } else {
                SystemInfoFactory.enableNativeSystemInfo();
            }

            LOG.info("Switched PluginContainer to '" + deploymentName + "'.");
        }
        
        return newInstance;
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
            throw new DeploymentException("Failed to switch to PluginContainer [" + container.get().getName() + "].",
                    e);
        }

        RhqAgentPluginArchive pluginArchive = archive.as(RhqAgentPluginArchive.class);

        Node descriptor = pluginArchive.get(ArchivePaths.create("META-INF/rhq-plugin.xml"));
        if (descriptor == null) {
            throw new DeploymentException("Plugin archive [" + archive + "] doesn't specify an RHQ plugin descriptor.");
        }

        boolean wasStarted = stopPc();

        deployPlugin(pluginArchive);

        if (wasStarted) {
            startPc();
        }

        PluginContainer pc = PluginContainer.getInstance();
        String pluginName = getPluginName(pluginArchive);
        PluginEnvironment plugin = pc.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            throw new RuntimeException("Failed to deploy plugin '" + pluginName + "' (" + pluginArchive.getName()
                    + ") - check the log above for an error (and big stack trace) from PluginManager.initialize().");
        }

        LOG.info("Done deploying plugin '" + pluginName + "' (" + archive + ") to PluginContainer "
                + container.get().getName() +".");
        
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

        if (pluginDeploymentPath.exists() && !pluginDeploymentPath.delete()) {
            if (File.separatorChar == '/') {
                // Unix
                throw new DeploymentException("Could not delete the RHQ plugin jar " + plugin.getName());
            } else {
                // Windows
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
        LOG.debug("Starting PluginContainer on demand...");
        
        PluginContainer pc = PluginContainer.getInstance();
        if (pc.isStarted()) {
            return false;
        }

        //always refresh the plugin finder so that it reports all the plugins
        //each time (and doesn't remember the plugins from previous PC runs)
        configuration.setPluginFinder(new FileSystemPluginFinder(configuration.getPluginDirectory()));

        if (LOG.isDebugEnabled() && (configuration.getAdditionalPackagesForRootPluginClassLoaderToExclude() != null)) {
            LOG.debug("Using root plugin classloader regex [" + configuration.getRootPluginClassLoaderRegex() + "]...");
        }

        pc.setConfiguration(configuration);
        pc.initialize();
        return true;
    }

    /**
     * Stops the plugin container.
     * @return true if PC was running before this call, false otherwise
     */
    private boolean stopPc() {
        LOG.debug("Stopping PluginContainer on demand...");
        PluginContainer pc = PluginContainer.getInstance();
        boolean wasStarted = pc.isStarted();
        if (wasStarted) {
            boolean shutdownGracefully = pc.shutdown();
            if (shutdownGracefully) {
                LOG.debug("Stopped PluginContainer gracefully.");
            } else {
                LOG.debug("Stopped PluginContainer.");
            }
        }

        FileUtil.purge(configuration.getTemporaryDirectory(), false);

        if (configuration.isClearDataOnShutdown()) {
            FileUtil.purge(configuration.getDataDirectory(), false);
        }

        return wasStarted;
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

    private static String getPluginName(Archive<?> archive) {
        InputStream is = archive.get(PLUGIN_DESCRIPTOR_PATH).getAsset().openStream();
        XMLEventReader rdr = null;
        try {
            rdr = XMLInputFactory.newInstance().createXMLEventReader(is);

            XMLEvent event = null;
            while (rdr.hasNext()) {
                event = rdr.nextEvent();
                if (event.getEventType() == XMLEvent.START_ELEMENT) {
                    break;
                }
            }

            StartElement startElement = event.asStartElement();
            String tagName = startElement.getName().getLocalPart();
            if (!"plugin".equals(tagName)) {
                throw new IllegalArgumentException("Illegal start tag found in the plugin descriptor. Expected 'plugin' but found '" + tagName + "' in the plugin '" + archive + "'.");
            }

            Attribute nameAttr = startElement.getAttributeByName(new QName("name"));

            if (nameAttr == null) {
                throw new IllegalArgumentException("Couldn't find the name attribute on the plugin tag in the plugin descriptor of plugin '" + archive + "'.");
            }

            return nameAttr.getValue();
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Failed to extract the plugin name out of the RHQ plugin archive [" + archive + "]", e);
        } catch (FactoryConfigurationError e) {
            throw new IllegalArgumentException("Failed to extract the plugin name out of the RHQ plugin archive [" + archive + "]", e);
        } finally {
            closeReaderAndStream(rdr, is, archive);
        }
    }

    private static void closeReaderAndStream(XMLEventReader rdr, InputStream str, Archive<?> archive) {
        if (rdr != null) {
            try {
                rdr.close();
            } catch (XMLStreamException e) {
                LOG.error("Failed to close the XML reader of the plugin descriptor in archive [" + archive + "]", e);
            }
        }

        try {
            str.close();
        } catch (IOException e) {
            LOG.error("Failed to close the input stream of the plugin descriptor in archive [" + archive + "]", e);
        }
    }

}
