/**
 * 
 */
package org.rhq.test.arquillian;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
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

    static {
        File f = null;
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
            throw new IllegalStateException("Could not instantiate an modified PluginContainer");
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
            throw new DeploymentException("Could not delete the RHQ plugin '" + plugin.getName());
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

    private File getDeploymentPath(Archive<?> plugin) {
        return new File(configuration.getPluginDirectory(), plugin.getName());
    }

    private void finalizeConfiguration(RhqAgentPluginContainerConfiguration config) {
        String deploymentName = container.get().getName();
        String subDir = deploymentName == null ? UUID.randomUUID().toString() : deploymentName;
        config.setContainerName(deploymentName);
        
        deploymentDirectory = new File(DEPLOYMENT_ROOT, subDir);

        File pluginsDir = new File(deploymentDirectory, PLUGINS_DIR_NAME);
        pluginsDir.mkdirs();
        File dataDir = new File(deploymentDirectory, DATA_DIR_NAME);
        dataDir.mkdirs();
        File tmpDir = new File(deploymentDirectory, TMP_DIR_NAME);
        tmpDir.mkdirs();

        config.setPluginDirectory(pluginsDir);
        config.setDataDirectory(dataDir);
        config.setTemporaryDirectory(tmpDir);
        
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
