/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.core.plugin;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.xml.sax.SAXException;

import org.jboss.deployment.DeploymentException;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.SubDeployerSupport;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.util.MD5Generator;
import org.rhq.enterprise.server.core.concurrency.LatchedServiceController;
import org.rhq.enterprise.server.core.concurrency.LatchedServiceException;
import org.rhq.enterprise.server.license.LicenseManager;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * ProductPlugin deployer responsible for detecting plugin jars.
 */
public class ProductPluginDeployer extends SubDeployerSupport implements ProductPluginDeployerMBean,
    NotificationBroadcaster {
    public static final String AMPS_VERSION = "2.0";

    private static final String PRODUCT = "RHQ";
    private static final String PLUGIN_DIR = "rhq-plugins";
    private static final String DEFAULT_PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";

    private static final String DEPLOYER_READY = NOTIF_TYPE("deployer.ready");
    private static final String DEPLOYER_SUSPENDED = NOTIF_TYPE("deployer.suspended");
    private static final String DEPLOYER_CLEARED = NOTIF_TYPE("deployer.cleared");
    private static final String PLUGIN_REGISTERED = NOTIF_TYPE("registered");
    private static final String PLUGIN_DEPLOYED = NOTIF_TYPE("deployed");
    private static final String PLUGIN_UNDEPLOYED = NOTIF_TYPE("undeployed");

    private static final String[] NOTIF_TYPES = new String[] { DEPLOYER_READY, DEPLOYER_SUSPENDED, DEPLOYER_CLEARED,
        PLUGIN_REGISTERED, PLUGIN_DEPLOYED, PLUGIN_UNDEPLOYED, };

    private Log log = LogFactory.getLog(ProductPluginDeployer.class.getName());

    private String pluginDir = PLUGIN_DIR;
    private String licenseFile = null;
    private LicenseManager licenseManager = null;
    /** Map of plugin names to the corresponding plugins' JBAS deployment infos */
    private Map<String, DeploymentInfo> pluginsToBeRegistered = new HashMap<String, DeploymentInfo>();
    /** Map of plugin names to the corresponding plugins' JAXB plugin descriptors */
    private Map<String, PluginDescriptor> pluginDescriptors = new HashMap<String, PluginDescriptor>();
    /** Map of plugin names to the corresponding plugins' versions */
    private Map<String, ComparableVersion> pluginVersions = new HashMap<String, ComparableVersion>();
    private boolean isStarted = false;
    private boolean isReady = false;
    private NotificationBroadcasterSupport broadcaster = new NotificationBroadcasterSupport();
    private AtomicLong notifSequence = new AtomicLong(0);

    private static String NOTIF_TYPE(String type) {
        return PRODUCT + ".plugin." + type;
    }

    /**
     * Creates a new {@link ProductPluginDeployer} object.
     */
    public ProductPluginDeployer() {
        // intentionally left blank
    }

    /**
     * @see org.jboss.deployment.SubDeployerSupport#processNestedDeployments(DeploymentInfo)
     */
    @Override
    protected void processNestedDeployments(DeploymentInfo di) throws DeploymentException {
        if (di.isDirectory) {
            super.processNestedDeployments(di);
        }
    }

    /**
     * @see org.jboss.mx.util.JBossNotificationBroadcasterSupport#addNotificationListener(NotificationListener,NotificationFilter,
     *      Object)
     */
    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        this.broadcaster.addNotificationListener(listener, filter, handback);
    }

    /**
     * @see org.jboss.mx.util.JBossNotificationBroadcasterSupport#removeNotificationListener(NotificationListener)
     */
    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        this.broadcaster.removeNotificationListener(listener);
    }

    /**
     * @see org.jboss.mx.util.JBossNotificationBroadcasterSupport#getNotificationInfo()
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[] { new MBeanNotificationInfo(NOTIF_TYPES, Notification.class.getName(),
            "Product Plugin Notifications"), };
    }

    /**
     * @see org.rhq.enterprise.server.core.plugin.ProductPluginDeployerMBean#setPluginDir(java.lang.String)
     */
    public void setPluginDir(String name) {
        this.pluginDir = name;
    }

    /**
     * @see org.rhq.enterprise.server.core.plugin.ProductPluginDeployerMBean#getPluginDir()
     */
    public String getPluginDir() {
        return this.pluginDir;
    }

    /**
     * @see org.rhq.enterprise.server.core.plugin.ProductPluginDeployerMBean#getRegisteredPluginNames()
     */
    public List<String> getRegisteredPluginNames() {
        ResourceMetadataManagerLocal metadataManager = LookupUtil.getResourceMetadataManager();
        Collection<Plugin> plugins = metadataManager.getPlugins();
        List<String> pluginNames = new ArrayList<String>();

        for (Plugin plugin : plugins) {
            pluginNames.add(plugin.getName());
        }

        return pluginNames;
    }

    /**
     * @see org.jboss.deployment.SubDeployer#accepts(org.jboss.deployment.DeploymentInfo)
     */
    @Override
    public boolean accepts(DeploymentInfo di) {
        String urlFile = di.url.getFile();

        if (isLicenseFile(di)) {
            licenseFile = urlFile;
            return true;
        }

        if (!(urlFile.endsWith("jar") || (urlFile.endsWith("test")))) {
            return false;
        }

        String urlPath = new File(urlFile).getParent();

        if (urlPath.endsWith(this.pluginDir)) {
            log.debug("accepting plugin=" + urlFile);
            return true;
        }

        return false;
    }

    /**
     * @see org.rhq.enterprise.server.core.plugin.ProductPluginDeployerMBean#startDeployer()
     */
    public void startDeployer() {
        pluginNotify("deployer", DEPLOYER_READY);

        // Do startup license checking
        this.licenseManager = LicenseManager.instance();
        this.licenseManager.doStartupCheck(this.licenseFile);

        // now that we have started, we are being told all EJBs are ready, so register
        // any plugins we've already been told about
        // note that we deploy the plugins in their proper deployment order based on dependencies
        Map<String, DeploymentInfo> pluginDeploymentInfos = new HashMap<String, DeploymentInfo>();
        PluginDependencyGraph dependencyGraph = new PluginDependencyGraph();

        for (String pluginName : this.pluginsToBeRegistered.keySet()) {
            DeploymentInfo deploymentInfo = this.pluginsToBeRegistered.get(pluginName);
            PluginDescriptor descriptor = this.pluginDescriptors.get(pluginName);
            pluginDeploymentInfos.put(descriptor.getName(), deploymentInfo);

            List<PluginDependencyGraph.PluginDependency> dependencies = new ArrayList<PluginDependencyGraph.PluginDependency>();
            for (PluginDescriptor.Depends dependency : descriptor.getDepends()) {
                dependencies.add(new PluginDependencyGraph.PluginDependency(dependency.getPlugin(), dependency
                    .isUseClasses()));
            }

            dependencyGraph.addPlugin(descriptor.getName(), dependencies);
        }

        StringBuffer error = new StringBuffer();
        if (dependencyGraph.isComplete(error)) {
            deployDependencyGraph(pluginDeploymentInfos, dependencyGraph);

            this.pluginsToBeRegistered.clear();
            this.pluginDescriptors.clear();

            //generally means we are done deploying plugins at startup.
            //but we are not "done" since a plugin can be dropped into
            //the plugins directory at anytime.
            pluginNotify("deployer", DEPLOYER_CLEARED);

            // Trigger vacuums on some tables as the initial deployment might have changed a lot of things
            // there are probably more tables involved though
            // First wait to give Hibernate a chance to close all transactions etc.
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                ; // no problem
            }

            Subject superuser = LookupUtil.getSubjectManager().getOverlord();
            SystemManagerLocal systemManager = LookupUtil.getSystemManager();
            systemManager.vacuum(superuser, new String[] { "RHQ_MEASUREMENT_DEF", "RHQ_CONFIG_DEF",
                "RHQ_RESOURCE_TYPE", "RHQ_RESOURCE_TYPE_PARENTS" });
        } else {
            log.warn(error.toString());
            log.warn(dependencyGraph.toString());
        }

        this.isReady = true;

        return;
    }

    class LatchedPluginDeploymentService extends LatchedServiceController.LatchedService {

        private final DeploymentInfo pluginDeploymentInfo;
        private final PluginDescriptor pluginDescriptor;

        public LatchedPluginDeploymentService(String pluginName, DeploymentInfo deploymentInfo,
            PluginDescriptor descriptor) {
            super(pluginName);
            this.pluginDeploymentInfo = deploymentInfo;
            this.pluginDescriptor = descriptor;
        }

        @Override
        public void executeService() throws LatchedServiceException {
            try {
                registerPluginJar(pluginDescriptor, pluginDeploymentInfo);
            } catch (Throwable t) {
                throw new LatchedServiceException(t);
            }
        }

    }

    private void deployDependencyGraph(Map<String, DeploymentInfo> pluginDeploymentInfos,
        PluginDependencyGraph dependencyGraph) {

        Map<String, LatchedPluginDeploymentService> latchedDependencyMap = new HashMap<String, LatchedPluginDeploymentService>();
        for (String name : dependencyGraph.getPlugins()) {
            LatchedPluginDeploymentService nextService = getServiceIfExists(name, pluginDeploymentInfos,
                latchedDependencyMap);
            for (String nextDependency : dependencyGraph.getPluginDependencies(name)) {
                LatchedPluginDeploymentService dependency = getServiceIfExists(nextDependency, pluginDeploymentInfos,
                    latchedDependencyMap);
                nextService.addDependency(dependency);
            }
        }

        long startDeployTime = System.currentTimeMillis();
        LatchedServiceController controller = new LatchedServiceController(latchedDependencyMap.values());
        controller.executeServices();
        long endDeployTime = System.currentTimeMillis();

        log.info("PluginDependencyGraph deploy time was " + (endDeployTime - startDeployTime) + " millis");
    }

    private LatchedPluginDeploymentService getServiceIfExists(String name,
        Map<String, DeploymentInfo> pluginDeploymentInfos, Map<String, LatchedPluginDeploymentService> latchedServiceMap) {

        LatchedPluginDeploymentService result = latchedServiceMap.get(name);

        if (result == null) {
            DeploymentInfo deploymentInfo = pluginDeploymentInfos.get(name);
            PluginDescriptor descriptor = this.pluginDescriptors.get(name);

            result = new LatchedPluginDeploymentService(name, deploymentInfo, descriptor);

            latchedServiceMap.put(name, result);
        }

        return result;
    }

    /**
     * This is the mechanism to kick off the registration of a new plugin. You must ensure you call this at the
     * appropriate time such that the plugin getting registered already has its dependencies registered.
     *
     * @param  pluginDescriptor
     * @param  deploymentInfo
     *
     * @return the name of the plugin
     */
    private String registerPluginJar(PluginDescriptor pluginDescriptor, DeploymentInfo deploymentInfo) {
        String pluginJar = deploymentInfo.url.getFile();

        try {
            String pluginName = pluginDescriptor.getName();
            String pluginNameDisplayName = pluginName + " (" + pluginDescriptor.getDisplayName() + ")";
            ComparableVersion comparableVersion = this.pluginVersions.get(pluginName);
            String version = (comparableVersion != null) ? comparableVersion.toString() : null;
            log.info("Deploying RHQ plugin " + pluginNameDisplayName + ", "
                + ((version != null) ? "version " + version : "undefined version") + "...");
            checkVersionCompatibility(pluginDescriptor.getAmpsVersion());

            // make sure the path is only the filename
            String filename = new File(deploymentInfo.url.getPath()).getName();
            Plugin plugin = new Plugin(pluginDescriptor.getName(), filename);
            plugin.setDisplayName((pluginDescriptor.getDisplayName() != null) ? pluginDescriptor.getDisplayName()
                : pluginDescriptor.getName());
            plugin.setEnabled(true);
            plugin.setDescription(pluginDescriptor.getDescription());

            if (pluginDescriptor.getHelp() != null && !pluginDescriptor.getHelp().getContent().isEmpty()) {
                plugin.setHelp(String.valueOf(pluginDescriptor.getHelp().getContent().get(0)));
            }

            plugin.setVersion(version);
            plugin.setMD5(MD5Generator.getDigestString(deploymentInfo.url.openStream()));

            // this manager is responsible for handling the munging of plugins that depend on other plugins
            // since we assume we are called in the proper deployment order, this should not fail
            // if we are called when hot-deploying a plugin whose dependencies aren't deployed, this will fail
            ResourceMetadataManagerLocal metadataManager = LookupUtil.getResourceMetadataManager();
            metadataManager.registerPlugin(plugin, pluginDescriptor);

            pluginNotify(pluginNameDisplayName, PLUGIN_REGISTERED);

            return plugin.getName();
        } catch (Exception e) {
            log.error("Unable to deploy RHQ plugin [" + pluginJar + "]", e);
            return null;
        }
    }

    private void checkVersionCompatibility(String version) throws RuntimeException {
        /*if (new OSGiVersionComparator().compare((String) version, (String) AMPS_VERSION) < 0)
         * { throw new RuntimeException("Plugin AMPS requirement " + version + " not compatible with server's AMPS
         * version " + AMPS_VERSION);}*/
    }

    private PluginDescriptor getPluginDescriptor(DeploymentInfo di) throws JAXBException {
        URL descriptorURL = di.localCl.findResource(DEFAULT_PLUGIN_DESCRIPTOR_PATH);
        if (descriptorURL == null) {
            log.warn("Could not load " + DEFAULT_PLUGIN_DESCRIPTOR_PATH + " from plugin jar file [" + di.url + "]");
            return null;
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        // Enable schema validation. (see http://jira.jboss.com/jira/browse/JBNADM-1539)
        URL pluginSchemaURL = getClass().getClassLoader().getResource("rhq-plugin.xsd");
        Schema pluginSchema;
        try {
            pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL);
        } catch (SAXException e) {
            throw new JAXBException("Schema is invalid: " + e.getMessage());
        }

        unmarshaller.setSchema(pluginSchema);

        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);

        PluginDescriptor pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorURL);

        for (ValidationEvent event : vec.getEvents()) {
            log.warn(event.getSeverity() + ":" + event.getMessage() + "    " + event.getLinkedException());
        }

        return pluginDescriptor;
    }

    /**
     * MBean Service start method. This method is called when JBoss is deploying the MBean.
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        if (!isStarted) {
            isStarted = true;
            super.start();
        }
    }

    /**
     * @see org.jboss.system.ServiceMBeanSupport#stop()
     */
    @Override
    public void stop() {
        if (isStarted) {
            super.stop();
            pluginNotify("deployer", DEPLOYER_SUSPENDED);
            this.pluginsToBeRegistered.clear();
            isStarted = false;
            isReady = false;
        }
    }

    /**
     * @see org.jboss.deployment.SubDeployerSupport#start(org.jboss.deployment.DeploymentInfo)
     */
    @Override
    public void start(DeploymentInfo deploymentInfo) throws DeploymentException {
        try {
            this.start();
        } catch (Exception e) {
            throw new DeploymentException(e);
        }

        if (isLicenseFile(deploymentInfo)) {
            return;
        }

        String pluginJarFileName = deploymentInfo.url.getFile();
        log.debug("start: " + pluginJarFileName);

        // plugin metadata cannot be deployed until EJBs are ready
        if (isReady) {
            try {
                // note that hot deploying a plugin whose dependencies aren't yet deployed will fail
                PluginDescriptor descriptor = getPluginDescriptor(deploymentInfo);
                if (registerPluginJar(descriptor, deploymentInfo) == null) {
                    throw new DeploymentException("Unable to hot deploy RHQ plugin [" + pluginJarFileName + "]");
                }
            } catch (JAXBException e) {
                throw new DeploymentException("Unable to hot deploy RHQ plugin [" + pluginJarFileName + "]", e);
            }
        } else {
            preprocessPlugin(deploymentInfo);
        }

        return;
    }

    /**
     * @see org.jboss.deployment.SubDeployerSupport#stop(org.jboss.deployment.DeploymentInfo)
     */
    @Override
    public void stop(DeploymentInfo di) throws DeploymentException {
        if (isLicenseFile(di)) {
            return;
        }

        log.debug("stop: " + di.url.getFile());

        try {
            String jar = di.url.getFile();

            pluginNotify(new File(jar).getName(), PLUGIN_UNDEPLOYED);
        } catch (Exception e) {
            throw new DeploymentException(e);
        }

        return;
    }

    private void pluginNotify(String name, String type) {
        String action = type.substring(type.lastIndexOf(".") + 1);
        String msg = "Plugin " + name + " " + action;

        Notification notif = new Notification(type, this, this.notifSequence.incrementAndGet(), msg);

        log.debug(msg);

        broadcaster.sendNotification(notif);
    }

    private boolean isLicenseFile(DeploymentInfo di) {
        String name = LicenseManager.getLicenseFileName();
        return di.url.getFile().endsWith(name);
    }

    private void preprocessPlugin(DeploymentInfo deploymentInfo) throws DeploymentException {
        String pluginJarFileName = deploymentInfo.url.getFile();
        PluginDescriptor descriptor;
        try {
            descriptor = getPluginDescriptor(deploymentInfo);
        } catch (JAXBException e) {
            throw new DeploymentException("Failed to parse plugin descriptor for plugin jar '" + pluginJarFileName
                + "'.", e);
        }
        Manifest manifest = deploymentInfo.getManifest();
        String version = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        if (version == null) {
            log.warn("'" + Attributes.Name.IMPLEMENTATION_VERSION
                + "' attribute not found in MANIFEST.MF of plugin jar '" + pluginJarFileName
                + "'. Falling back to version defined in plugin descriptor...");
            version = descriptor.getVersion();
        }
        ComparableVersion comparableVersion;
        if (version != null) {
            try {
                comparableVersion = new ComparableVersion(version);
            } catch (RuntimeException e) {
                throw new DeploymentException("Failed to parse version (" + version + ") for plugin jar '"
                    + pluginJarFileName + ".", e);
            }
        } else {
            log.warn("No version is defined for plugin jar '" + pluginJarFileName
                + "'. A version should be defined either via the MANIFEST.MF '"
                + Attributes.Name.IMPLEMENTATION_VERSION
                + "' attribute or via the plugin descriptor 'version' attribute.");
            comparableVersion = null;
        }
        String pluginName = descriptor.getName();
        ComparableVersion existingComparableVersion = this.pluginVersions.get(pluginName);
        boolean newerThanExistingVersion = false;
        if (existingComparableVersion != null) {
            if (comparableVersion != null && comparableVersion.compareTo(existingComparableVersion) > 0) {
                newerThanExistingVersion = true;
                log.debug("Newer version of '" + pluginName + "' plugin found (version " + version
                    + ") - older version (" + existingComparableVersion + ") will be ignored.");
            }
        }
        if (existingComparableVersion == null || newerThanExistingVersion) {
            this.pluginDescriptors.put(pluginName, descriptor);
            this.pluginVersions.put(pluginName, comparableVersion);
            this.pluginsToBeRegistered.put(pluginName, deploymentInfo);
        }
    }
}