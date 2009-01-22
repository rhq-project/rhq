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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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
import org.rhq.enterprise.server.core.concurrency.LatchedServiceCircularityException;
import org.rhq.enterprise.server.core.concurrency.LatchedServiceController;
import org.rhq.enterprise.server.core.concurrency.LatchedServiceException;
import org.rhq.enterprise.server.license.LicenseManager;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * ProductPlugin deployer responsible for detecting agent plugin jars on the filesystem.
 * Note that this class is the only one that should care about the agent jar files on the
 * filesystem.  The database will contain the plugin jar contents for other objects to use.
 */
public class ProductPluginDeployer extends SubDeployerSupport implements ProductPluginDeployerMBean,
    NotificationBroadcaster {

    public static final String AMPS_VERSION = "2.0";

    private static final String DEFAULT_PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";
    private static final String PLUGIN_DESCRIPTOR_SCHEMA = "rhq-plugin.xsd";

    private static final String DEPLOYER_READY = "RHQ.plugin.deployer.ready";
    private static final String DEPLOYER_SUSPENDED = "RHQ.plugin.deployer.suspended";
    private static final String DEPLOYER_CLEARED = "RHQ.plugin.deployer.cleared";
    private static final String PLUGIN_REGISTERED = "RHQ.plugin.registered";
    private static final String PLUGIN_DEPLOYED = "RHQ.plugin.deployed";
    private static final String PLUGIN_UNDEPLOYED = "RHQ.plugin.undeployed";
    private static final String[] NOTIF_TYPES = new String[] { DEPLOYER_READY, DEPLOYER_SUSPENDED, DEPLOYER_CLEARED,
        PLUGIN_REGISTERED, PLUGIN_DEPLOYED, PLUGIN_UNDEPLOYED, };

    private Log log = LogFactory.getLog(ProductPluginDeployer.class.getName());
    private NotificationBroadcasterSupport broadcaster = new NotificationBroadcasterSupport();
    private AtomicLong notifSequence = new AtomicLong(0);

    private File pluginDir = null;
    private String licenseFile = null;

    /** Map of plugin names to the corresponding plugins' JBAS deployment infos */
    private Map<String, DeploymentInfo> deploymentInfos = new HashMap<String, DeploymentInfo>();
    /** Map of plugin names to the corresponding plugins' JAXB plugin descriptors */
    private Map<String, PluginDescriptor> pluginDescriptors = new HashMap<String, PluginDescriptor>();
    /** Map of plugin names to the corresponding plugins' versions */
    private Map<String, ComparableVersion> pluginVersions = new HashMap<String, ComparableVersion>();
    /** Set of plugins that have been accepted but need to be registered (useful during hot-deployment) */
    private Set<String> namesOfPluginsToBeRegistered = new HashSet<String>();

    private boolean isStarted = false;
    private boolean isReady = false;

    public ProductPluginDeployer() {
        // intentionally left blank
    }

    @Override
    protected void processNestedDeployments(DeploymentInfo di) throws DeploymentException {
        if (di.isDirectory) {
            super.processNestedDeployments(di);
        }
    }

    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        this.broadcaster.addNotificationListener(listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        this.broadcaster.removeNotificationListener(listener);
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[] { new MBeanNotificationInfo(NOTIF_TYPES, Notification.class.getName(),
            "Product Plugin Notifications"), };
    }

    public void setPluginDir(String pluginDirString) {
        this.pluginDir = new File(pluginDirString);

        // this directory should always exist, but just in case it doesn't, create it
        if (!this.pluginDir.exists()) {
            this.pluginDir.mkdirs();
        }
    }

    public String getPluginDir() {
        return this.pluginDir.getAbsolutePath();
    }

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
     * This is called when this deployer service itself is starting up.
     */
    @Override
    public void startService() throws Exception {
        if (!isStarted) {
            isStarted = true;
            super.startService();
        }
    }

    @Override
    public void stopService() throws Exception {
        if (isStarted) {
            super.stopService();
            pluginNotify("deployer", DEPLOYER_SUSPENDED);
            this.deploymentInfos.clear();
            this.pluginDescriptors.clear();
            this.pluginVersions.clear();
            this.namesOfPluginsToBeRegistered.clear();

            isStarted = false;
            isReady = false;
        }
    }

    /**
     * This method is called after a plugin file has been accepted and can be registered.
     * This is called as part of the JBossAS main deployer and may be called even before
     * the server is fully initialized (i.e. the EJB3 SLSBs may not be ready yet, in which case
     * {@link #isReady} will be <code>false</code>).
     * 
     * @param deploymentInfo information about the plugin file that has been accepted
     * @throws DeploymentException
     */
    @Override
    public void start(DeploymentInfo deploymentInfo) throws DeploymentException {
        if (isLicenseFile(deploymentInfo))
            return;

        log.debug("starting the deployment of agent plugin: " + getPluginJarFilename(deploymentInfo));
        String pluginName = preprocessPlugin(deploymentInfo);

        // isReady == true means startDeployer() has already been called, so this is a hot deploy.
        if (this.isReady) {
            log.debug("Hot deploying plugin [" + pluginName + "]...");
            try {
                registerPlugins(); // we are ready to hot-deploy so we can register immediately
            } catch (Exception e) {
                throw new DeploymentException("Unable to deploy RHQ plugin [" + pluginName + "]", e);
            }
        } else {
            // startDeployer() has not been called yet so we are holding off registering until then
            log.debug("Initial deploy of plugin [" + pluginName + "]...");
        }
        return;
    }

    @Override
    public void stop(DeploymentInfo deploymentInfo) throws DeploymentException {
        if (isLicenseFile(deploymentInfo))
            return;

        String pluginJarFileName = getPluginJarFilename(deploymentInfo);
        log.debug("agent plugin has been removed from the filesystem: " + pluginJarFileName);

        try {
            // TODO: Actually undeploy the plugin from the DB (this will be a lot of work...).
            pluginNotify(new File(pluginJarFileName).getName(), PLUGIN_UNDEPLOYED);
        } catch (Exception e) {
            throw new DeploymentException(e);
        }

        return;
    }

    /**
     * When this is called, the JBossAS main deployer is telling us it detected a new or changed file
     * that may be a new/updated plugin jar file (or license file). As part of this call, the main deployer
     * will have just previously copied the jar file into a "localUrl" which is at server/default/tmp/deploy.
     * 
     * @param di the deployment information of the detected file (which is probably an agent plugin file)
     * @return <code>true</code> if the deployment info represents an agent plugin file
     */
    @Override
    public boolean accepts(DeploymentInfo di) {
        if (di.isDirectory) {
            return false;
        }

        String urlString = di.url.getFile(); // must use url, not localurl - we want the path that has the true filename

        // yes, we also handle the license file too - we should move this in its own deployer someday
        if (isLicenseFile(di)) {
            licenseFile = urlString;
            return true;
        }

        if (!urlString.endsWith(".jar")) {
            return false;
        }

        File deploymentDirectory = new File(urlString).getParentFile();

        if (deploymentDirectory.getName().equals(this.pluginDir.getName())) {
            log.debug("accepting agent plugin=" + urlString);
            return true;
        }

        return false;
    }

    /**
     * This is called by the server's startup servlet which essentially informs us that
     * the server's internal EJB/SLSBs are ready and can be called. This means we are
     * allowed to begin registering types from deployed plugins.
     */
    public void startDeployer() {
        pluginNotify("deployer", DEPLOYER_READY);

        // Do startup license checking.
        LicenseManager licenseManager = LicenseManager.instance();
        licenseManager.doStartupCheck(this.licenseFile);

        registerPlugins();

        pluginNotify("deployer", DEPLOYER_CLEARED);

        this.isReady = true; // this now enables hot-deployment
        return;
    }

    /**
     * Process the specified plugin jar to figure out the plugin name and version. If it is the only plugin with this
     * name, or if it has the newest version among other plugins with the same name, then add it to our master set of
     * plugins to be registered. Once all EJBs are started, {@link #startDeployer()} will be called and will take care
     * of registering the plugins.
     */
    private String preprocessPlugin(DeploymentInfo deploymentInfo) throws DeploymentException {
        File pluginFile = new File(deploymentInfo.localUrl.getFile());
        ensureDeploymentIsValid(pluginFile);
        PluginDescriptor descriptor = getPluginDescriptor(deploymentInfo);
        String pluginName = descriptor.getName();
        boolean initialDeploy = !this.deploymentInfos.containsKey(pluginName);
        ComparableVersion version = getPluginVersion(pluginFile, descriptor);

        if (initialDeploy) {
            log.info("Deploying RHQ plugin [" + pluginName + "]...");
        } else {
            log.info("Redeploying RHQ plugin [" + pluginName + "]...");
        }

        if (initialDeploy || isNewestVersion(pluginName, version)) {
            this.deploymentInfos.put(pluginName, deploymentInfo);
            this.pluginDescriptors.put(pluginName, descriptor);
            this.pluginVersions.put(pluginName, version);
            this.namesOfPluginsToBeRegistered.add(pluginName);
        }
        return pluginName;
    }

    private PluginDescriptor getPluginDescriptor(DeploymentInfo di) throws DeploymentException {
        URL descriptorURL = di.localCl.findResource(DEFAULT_PLUGIN_DESCRIPTOR_PATH);
        if (descriptorURL == null) {
            throw new DeploymentException("Could not load " + DEFAULT_PLUGIN_DESCRIPTOR_PATH
                + " from plugin jar file [" + di.localUrl + "]");
        }

        PluginDescriptor pluginDescriptor;
        ValidationEventCollector vec;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable schema validation. (see http://jira.jboss.com/jira/browse/JBNADM-1539)
            URL pluginSchemaURL = getClass().getClassLoader().getResource(PLUGIN_DESCRIPTOR_SCHEMA);
            Schema pluginSchema;
            try {
                pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL);
            } catch (SAXException e) {
                throw new DeploymentException("Schema is invalid: " + e.getMessage());
            }

            unmarshaller.setSchema(pluginSchema);

            vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);

            pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorURL);
        } catch (JAXBException e) {
            throw new DeploymentException("Failed to parse descriptor found in plugin [" + di.localUrl + "]", e);
        }

        for (ValidationEvent event : vec.getEvents()) {
            log.warn(event.getSeverity() + ":" + event.getMessage() + "    " + event.getLinkedException());
        }

        return pluginDescriptor;
    }

    /**
     * Returns the version for the plugin represented by the given descriptor/file.
     * If the descriptor defines a version, that is considered the version of the plugin.
     * However, if the plugin descriptor does not define a version, the plugin jar's manifest
     * is searched for an implementation version string and if one is found that is the version
     * of the plugin. If the manifest entry is also not found, the plugin does not have a version
     * associated with it, which causes this method to throw an exception.
     * 
     * @param pluginFile the plugin jar
     * @param descriptor the plugin descriptor as found in the plugin jar
     * @return the version of the plugin
     * @throws DeploymentException if there is no version for the plugin or the version string is invalid
     */
    private ComparableVersion getPluginVersion(File pluginFile, PluginDescriptor descriptor) throws DeploymentException {

        String version = descriptor.getVersion();
        if (version == null) {
            Manifest manifest = getManifest(pluginFile);
            version = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        }

        if (version == null) {
            throw new DeploymentException("No version is defined for plugin jar [" + pluginFile
                + "]. A version must be defined either via the MANIFEST.MF [" + Attributes.Name.IMPLEMENTATION_VERSION
                + "] attribute or via the plugin descriptor 'version' attribute.");
        }

        try {
            return new ComparableVersion(version);
        } catch (RuntimeException e) {
            throw new DeploymentException("Version [" + version + "] for [" + pluginFile + "] did not parse", e);
        }
    }

    /**
     * Registers plugins and their types.
     * Only call this method when {@link #isReady} is true.
     */
    private void registerPlugins() {
        for (Iterator<String> it = this.namesOfPluginsToBeRegistered.iterator(); it.hasNext();) {
            String pluginName = it.next();
            if (!isNewOrUpdated(pluginName)) {
                log.debug("Plugin [" + pluginName + "] has not been updated.");
                it.remove();
            }
        }

        if (this.namesOfPluginsToBeRegistered.isEmpty()) {
            log.info("All plugins were already up to date in the database.");
            return;
        }

        log.info("Registering the following new or updated plugins: " + this.namesOfPluginsToBeRegistered);
        PluginDependencyGraph dependencyGraph = buildDependencyGraph();
        StringBuilder errorBuffer = new StringBuilder();
        if (!dependencyGraph.isComplete(errorBuffer)) {
            log.error(errorBuffer.toString());
            log.error(dependencyGraph.toString());
            return; // should we throw an exception here?
        }
        registerPlugins(dependencyGraph);
        this.namesOfPluginsToBeRegistered.clear();

        // Trigger vacuums on some tables as the initial deployment might have changed a lot of things.
        // There are probably more tables involved though.
        // First wait to give Hibernate a chance to close all transactions etc.
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException ignored) {
        }
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        systemManager.vacuum(superuser, new String[] { "RHQ_MEASUREMENT_DEF", "RHQ_CONFIG_DEF", "RHQ_RESOURCE_TYPE",
            "RHQ_RESOURCE_TYPE_PARENTS", Plugin.TABLE_NAME });

        return;
    }

    private boolean isNewestVersion(String pluginName, ComparableVersion version) {
        boolean newestVersion;
        ComparableVersion existingVersion = this.pluginVersions.get(pluginName);
        if (existingVersion != null) {
            newestVersion = (version.compareTo(existingVersion) >= 0);
            if (newestVersion)
                log.debug("Newer version of [" + pluginName + "] plugin found (version " + version
                    + ") - older version (" + existingVersion + ") will be ignored.");
        } else {
            newestVersion = false;
        }
        return newestVersion;
    }

    private boolean isNewOrUpdated(String pluginName) {
        DeploymentInfo deploymentInfo = this.deploymentInfos.get(pluginName);
        if (deploymentInfo == null) {
            throw new IllegalStateException("DeploymentInfo was not found for plugin [" + pluginName
                + " ] - it should have been initialized by preprocessPlugin().");
        }

        String md5 = null;
        try {
            md5 = MD5Generator.getDigestString(new File(deploymentInfo.localUrl.toURI()));
        } catch (Exception e) {
            log.error("Error generating MD5 for plugin [" + pluginName + "]. Cause: " + e);
        }

        ResourceMetadataManagerLocal metadataManager = LookupUtil.getResourceMetadataManager();
        Plugin plugin;
        try {
            plugin = metadataManager.getPlugin(pluginName);
        } catch (RuntimeException e) {
            log.debug("New plugin [" + pluginName + "] detected.");
            return true;
        }
        if (!plugin.getMd5().equals(md5)) {
            log.debug("Updated plugin [" + pluginName + "] detected.");
            return true;
        }
        return false;
    }

    private PluginDependencyGraph buildDependencyGraph() {
        PluginDependencyGraph dependencyGraph = new PluginDependencyGraph();
        for (String pluginName : this.deploymentInfos.keySet()) {
            PluginDescriptor descriptor = this.pluginDescriptors.get(pluginName);
            List<PluginDependencyGraph.PluginDependency> dependencies = new ArrayList<PluginDependencyGraph.PluginDependency>();
            for (PluginDescriptor.Depends dependency : descriptor.getDepends()) {
                dependencies.add(new PluginDependencyGraph.PluginDependency(dependency.getPlugin(), dependency
                    .isUseClasses()));
            }
            dependencyGraph.addPlugin(pluginName, dependencies);
        }
        log.debug("Dependency graph deployment order: " + dependencyGraph.getDeploymentOrder());
        return dependencyGraph;
    }

    private void registerPlugins(PluginDependencyGraph dependencyGraph) {
        Map<String, LatchedPluginDeploymentService> latchedDependencyMap = new HashMap<String, LatchedPluginDeploymentService>();
        for (String pluginName : this.namesOfPluginsToBeRegistered) {
            LatchedPluginDeploymentService service = getServiceIfExists(pluginName, latchedDependencyMap);
            // We need to register dependencies also even if they aren't new or updated. This is because
            // PluginMetadataManager requires dependency plugins to be loaded in its pluginsByParser map.
            // ResourceMetadataManagerBean.register() will be smart enough to pass these plugins to
            // PluginMetadataManager to be parsed, but not to unnecessarily merge their types into the DB.
            for (String dependencyPluginName : dependencyGraph.getPluginDependencies(pluginName)) {
                LatchedPluginDeploymentService dependencyService = getServiceIfExists(dependencyPluginName,
                    latchedDependencyMap);
                service.addDependency(dependencyService);
            }
        }

        long startDeployTime = System.currentTimeMillis();
        LatchedServiceController controller = new LatchedServiceController(latchedDependencyMap.values());
        try {
            controller.executeServices();
        } catch (LatchedServiceCircularityException lsce) {
            log.error(lsce.getMessage());
        }
        long endDeployTime = System.currentTimeMillis();

        log.debug("Registered [" + dependencyGraph.getPlugins().size() + "] plugins in ["
            + (endDeployTime - startDeployTime) + "]ms");
    }

    private LatchedPluginDeploymentService getServiceIfExists(String pluginName,
        Map<String, LatchedPluginDeploymentService> latchedServiceMap) {
        LatchedPluginDeploymentService result = latchedServiceMap.get(pluginName);
        if (result == null) {
            DeploymentInfo deploymentInfo = this.deploymentInfos.get(pluginName);
            PluginDescriptor descriptor = this.pluginDescriptors.get(pluginName);
            result = new LatchedPluginDeploymentService(pluginName, deploymentInfo, descriptor);
            latchedServiceMap.put(pluginName, result);
        }
        return result;
    }

    /**
     * This is the mechanism to kick off the registration of a new plugin. You must ensure you call this at the
     * appropriate time such that the plugin getting registered already has its dependencies registered.
     */
    private void registerPluginJar(PluginDescriptor pluginDescriptor, DeploymentInfo deploymentInfo) {
        if (pluginDescriptor == null) {
            log.error("Missing plugin descriptor; is [" + deploymentInfo.localUrl + "] a valid plugin?");
            return;
        }

        try {
            File localPluginFile = new File(deploymentInfo.localUrl.toURI());

            String pluginName = pluginDescriptor.getName();
            String displayName = pluginDescriptor.getDisplayName();
            String pluginNameDisplayName = pluginName + " (" + displayName + ")";
            ComparableVersion comparableVersion = this.pluginVersions.get(pluginName);
            String version = (comparableVersion != null) ? comparableVersion.toString() : null;
            log.debug("Registering RHQ plugin " + pluginNameDisplayName + ", "
                + ((version != null) ? "version " + version : "undefined version") + "...");
            checkVersionCompatibility(pluginDescriptor.getAmpsVersion());

            String filename = getPluginJarFilename(deploymentInfo); // make sure this is only the filename
            Plugin plugin = new Plugin(pluginName, filename);
            plugin.setDisplayName((displayName != null) ? displayName : pluginName);
            plugin.setEnabled(true);
            plugin.setDescription(pluginDescriptor.getDescription());

            // get the last modified of the "real" plugin jar since that's the one the user touches
            long mtime = deploymentInfo.url.openConnection().getLastModified();
            plugin.setMtime(mtime);

            if (pluginDescriptor.getHelp() != null && !pluginDescriptor.getHelp().getContent().isEmpty()) {
                plugin.setHelp(String.valueOf(pluginDescriptor.getHelp().getContent().get(0)));
            }

            plugin.setVersion(version);
            plugin.setMD5(MD5Generator.getDigestString(localPluginFile));

            // this manager is responsible for handling the munging of plugins that depend on other plugins
            // since we assume we are called in the proper deployment order, this should not fail
            // if we are called when hot-deploying a plugin whose dependencies aren't deployed, this will fail
            ResourceMetadataManagerLocal metadataManager = LookupUtil.getResourceMetadataManager();
            metadataManager.registerPlugin(plugin, pluginDescriptor, localPluginFile);

            pluginNotify(pluginNameDisplayName, PLUGIN_REGISTERED);
        } catch (Exception e) {
            log.error("Failed to register RHQ plugin file [" + deploymentInfo.shortName + "] at ["
                + deploymentInfo.localUrl + "]", e);
        }
    }

    private void checkVersionCompatibility(String version) throws RuntimeException {
        /*if (new OSGiVersionComparator().compare((String) version, (String) AMPS_VERSION) < 0)
         * { throw new RuntimeException("Plugin AMPS requirement " + version + " not compatible with server's AMPS
         * version " + AMPS_VERSION);}*/
    }

    private void ensureDeploymentIsValid(File pluginFile) throws DeploymentException {

        // try a few times (sleeping between retries)
        // if the zip file still isn't valid, its probably corrupted and not simply due to the file still being written out
        int retries = 4;
        while (!isDeploymentValidZipFile(pluginFile)) {
            if (--retries <= 0) {
                throw new DeploymentException("File [" + pluginFile + "] is not a valid jarfile - "
                    + " it is either corrupted or file has not been fully written yet.");
            }
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                break;
            }
        }
        return;
    }

    private boolean isDeploymentValidZipFile(File pluginFile) {
        boolean isValid;
        JarFile jarFile = null;
        try {
            // Try to access the plugin jar using the JarFile API.
            // Any weird errors usually mean the file is currently being written but isn't finished yet.
            // Errors could also mean the file is simply corrupted.
            jarFile = new JarFile(pluginFile);
            if (jarFile.size() <= 0) {
                throw new Exception("There are no entries in the plugin file");
            }
            JarEntry entry = jarFile.entries().nextElement();
            entry.getName();
            isValid = true;
        } catch (Exception e) {
            log.info("File [" + pluginFile + "] is not a valid jarfile - "
                + " the file may not have been fully written yet. Cause: " + e);
            isValid = false;
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Exception e) {
                    log.error("Failed to close jar file [" + pluginFile + "]");
                }
            }
        }
        return isValid;
    }

    private void pluginNotify(String name, String notifType) {
        String action = notifType.substring(notifType.lastIndexOf(".") + 1);
        String msg = "Plugin " + name + " " + action + ".";
        Notification notif = new Notification(notifType, this, this.notifSequence.incrementAndGet(), msg);
        log.debug(msg);
        broadcaster.sendNotification(notif);
    }

    /**
     * Obtains the manifest of the plugin file represented by the given deployment info.
     * Use this method rather than calling deploymentInfo.getManifest()
     * (workaround for https://jira.jboss.org/jira/browse/JBAS-6266).
     * 
     * @param pluginFile the plugin file
     * @return the deployed plugin's manifest
     */
    private Manifest getManifest(File pluginFile) {
        try {
            JarFile jarFile = new JarFile(pluginFile);
            try {
                Manifest manifest = jarFile.getManifest();
                return manifest;
            } finally {
                jarFile.close();
            }
        } catch (Exception ignored) {
            return null; // this is OK, it just means we do not have a manifest
        }
    }

    /**
     * This returns the name of the plugin file that is represented by the given
     * deployment info. This returns just the name of the plugin file, without
     * any parent directory information.
     * 
     * @param di the deployment info of the plugin file that is deployed
     * @return the name of the plugin file
     */
    private String getPluginJarFilename(DeploymentInfo di) {
        return new File(di.url.getPath()).getName(); // use url, not localurl, because we want the 'real' name
    }

    private boolean isLicenseFile(DeploymentInfo deploymentInfo) {
        String name = LicenseManager.getLicenseFileName();
        return deploymentInfo.url.getFile().endsWith(name); // use url (not localurl), want to use the actual license file
    }

    class LatchedPluginDeploymentService extends LatchedServiceController.LatchedService {
        private final DeploymentInfo pluginDeploymentInfo;
        private final PluginDescriptor pluginDescriptor;

        public LatchedPluginDeploymentService(String pluginName, DeploymentInfo di, PluginDescriptor descriptor) {
            super(pluginName);
            this.pluginDeploymentInfo = di;
            this.pluginDescriptor = descriptor;
        }

        @Override
        public void executeService() throws LatchedServiceException {
            try {
                registerPluginJar(this.pluginDescriptor, this.pluginDeploymentInfo);
            } catch (Throwable t) {
                throw new LatchedServiceException(t);
            }
        }
    }
}