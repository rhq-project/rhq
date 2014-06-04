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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.CannedGroupAddition;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * ProductPlugin deployer responsible for detecting agent plugin jars on the filesystem.
 */
public class ProductPluginDeployer {

    private Log log = LogFactory.getLog(ProductPluginDeployer.class.getName());
    private File pluginDir = null;
    private boolean isStarted = false;
    private boolean isReady = false;

    /** Map of plugin names to the corresponding plugins' deployment infos */
    private Map<String, DeploymentInfo> deploymentInfos = new HashMap<String, DeploymentInfo>();
    /** Map of plugin names to the corresponding plugins' versions */
    private Map<String, ComparableVersion> pluginVersions = new HashMap<String, ComparableVersion>();
    /** Set of plugins that have been accepted but need to be registered (useful during hot-deployment) */
    private Set<String> namesOfPluginsToBeRegistered = new HashSet<String>();
    /** Metadata cache for all JAXB plugin descriptors and resource types of all plugins */
    private PluginMetadataManager metadataManager = new PluginMetadataManager();

    public ProductPluginDeployer() {
        // intentionally left blank
    }

    public File getPluginDir() {
        return this.pluginDir;
    }

    public void setPluginDir(File pluginDir) {
        this.pluginDir = pluginDir;

        // this directory should always exist, but just in case it doesn't, create it
        if (!this.pluginDir.exists()) {
            this.pluginDir.mkdirs();
        }
    }

    public PluginMetadataManager getPluginMetadataManager() {
        return this.metadataManager;
    }

    /**
     * This is called by the server's startup servlet which essentially informs us that
     * the server's internal EJB/SLSBs are ready and can be called. This means we are
     * allowed to begin registering types from deployed plugins.
     */
    public void startDeployment() {
        // we can now register our initial set of plugins (This may be a no-op at this point)
        registerPlugins();

        // indicate that we are now ready for hot-deployment of new plugins
        this.isReady = true;
    }

    /**
     * This is called when this deployer service itself is starting up.
     */
    public void start() throws Exception {
        if (!isStarted) {
            isStarted = true;
        }
    }

    public void stop() {
        if (isStarted) {
            this.deploymentInfos.clear();
            this.pluginVersions.clear();
            this.namesOfPluginsToBeRegistered.clear();
            this.metadataManager = new PluginMetadataManager();
            isStarted = false;
            isReady = false;
        }
    }

    /**
     * This is called when a new or updated plugin is brought online.
     * This just marks the plugin as being needed to be registered. Caller
     * must ensure that {@link #registerPlugins()} is called afterwards
     * to fully process the detected plugin.
     * 
     * @param deploymentInfo information on the newly detected plugin
     */
    public void pluginDetected(DeploymentInfo deploymentInfo) throws Exception {
        if (!accepts(deploymentInfo)) {
            return;
        }

        // don't cache deployment infos across starts, so if we've seen this deployment info before,
        // take the current one we were just given and use it to replace the old info
        String key = null;
        for (Map.Entry<String, DeploymentInfo> entry : this.deploymentInfos.entrySet()) {
            if (entry.getValue().equals(deploymentInfo)) {
                key = entry.getKey();
                break;
            }
        }
        if (key != null) {
            this.deploymentInfos.put(key, deploymentInfo);
        }

        String name = preprocessPlugin(deploymentInfo);

        // isReady == true means startDeployer() has already been called, so this is a hot deploy.
        // (if the EJB3 SLSBs are not ready yet, isReady will be false.
        if (this.isReady) {
            log.debug("Will hot deploy plugin [" + name + "] from [" + deploymentInfo.url + "]");
            // do NOT register plugins yet - the dependency graph might not be complete, let the caller call registerPlugins
        } else {
            // startDeployer() has not been called yet so we are holding off registering until then
            log.debug("Not ready yet - will deploy plugin [" + name + "] from [" + deploymentInfo.url + "] later");
        }
        return;
    }

    /**
     * Determines if this is a plugin we should process.
     * 
     * @param di the deployment information of the detected file (which is probably an agent plugin file)
     * @return <code>true</code> if the deployment info represents an agent plugin file
     */
    private boolean accepts(DeploymentInfo di) {
        String urlString = di.url.getFile();

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
     * Registers newly detected plugins and their types.
     * 
     * Only call this method when {@link #isReady} is true. This is a no-op if we are not ready.
     */
    public void registerPlugins() {
        if (!this.isReady) {
            return;
        }

        for (Iterator<String> it = this.namesOfPluginsToBeRegistered.iterator(); it.hasNext();) {
            String pluginName = it.next();
            if (!isNewOrUpdated(pluginName)) {
                log.debug("Plugin [" + pluginName + "] has not been updated.");
                it.remove();
            }
        }

        if (this.namesOfPluginsToBeRegistered.isEmpty()) {
            log.debug("All agent plugins were already up to date in the database.");
            return;
        }

        Set<String> pluginsToBeRegistered = new HashSet<String>(this.namesOfPluginsToBeRegistered);
        log.info("Deploying [" + pluginsToBeRegistered.size() + "] new or updated agent plugins: "
            + pluginsToBeRegistered);
        PluginDependencyGraph dependencyGraph = buildDependencyGraph();
        StringBuilder errorBuffer = new StringBuilder();
        if (!dependencyGraph.isComplete(errorBuffer)) {
            log.error(errorBuffer.toString());
            if (log.isDebugEnabled()) {
                log.debug(dependencyGraph.toString());
            }
            // reduce the graph down to only those plugins and their deps that exist and only register those
            dependencyGraph = dependencyGraph.reduceGraph();
            pluginsToBeRegistered.retainAll(dependencyGraph.getPlugins());
        }
        if (pluginsToBeRegistered.size() > 0) {
            registerPlugins(dependencyGraph, pluginsToBeRegistered);
        }
        log.info("Plugin metadata updates are complete for [" + pluginsToBeRegistered.size() + "] plugins: "
            + pluginsToBeRegistered);
        this.namesOfPluginsToBeRegistered.removeAll(pluginsToBeRegistered);

        // load resource facets cache
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();
            typeManager.reloadResourceFacetsCache();
        } catch (Throwable t) {
            log.error("Could not load ResourceFacets cache", t);
        }

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

    /**
     * Process the specified plugin jar to figure out the plugin name and version. If it is the only plugin with this
     * name, or if it has the newest version among other plugins with the same name, then add it to our master set of
     * plugins to be registered. Once all EJBs are started, {@link #startDeployment()} will be called and will take care
     * of registering the plugins.
     */
    private String preprocessPlugin(DeploymentInfo deploymentInfo) throws Exception {
        File pluginFile = new File(deploymentInfo.url.getFile());
        ensureDeploymentIsValid(pluginFile);
        PluginDescriptor descriptor = getPluginDescriptor(deploymentInfo);
        String pluginName = descriptor.getName();
        boolean initialDeploy = !this.deploymentInfos.containsKey(pluginName);
        ComparableVersion version;
        version = AgentPluginDescriptorUtil.getPluginVersion(pluginFile, descriptor);
        if (initialDeploy) {
            log.info("Discovered agent plugin [" + pluginName + "]");
        } else {
            log.info("Rediscovered agent plugin [" + pluginName + "]");
        }
        if (initialDeploy || isNewestVersion(pluginName, version)) {
            this.metadataManager.storePluginDescriptor(descriptor);
            this.deploymentInfos.put(pluginName, deploymentInfo);
            this.pluginVersions.put(pluginName, version);
            this.namesOfPluginsToBeRegistered.add(pluginName);
        }
        return pluginName;
    }

    private PluginDescriptor getPluginDescriptor(DeploymentInfo di) throws Exception {
        try {
            PluginDescriptor pluginDescriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(di.url);
            return pluginDescriptor;
        } catch (Exception e) {
            throw new Exception("Failed to parse descriptor found in plugin [" + di.url + "]", e);
        }
    }
    
    private boolean isNewestVersion(String pluginName, ComparableVersion version) {
        boolean newestVersion;
        ComparableVersion existingVersion = this.pluginVersions.get(pluginName);
        if (existingVersion != null) {
            newestVersion = (version.compareTo(existingVersion) >= 0);
            if (newestVersion)
                log.info("Newer version of [" + pluginName + "] plugin found (version " + version
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

        PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();
        Plugin plugin = pluginMgr.getPlugin(pluginName);
        if (null == plugin) {
            log.debug("New plugin [" + pluginName + "] detected.");
            return true;
        }

        String md5 = null;
        try {
            md5 = MessageDigestGenerator.getDigestString(new File(deploymentInfo.url.toURI()));
        } catch (Exception e) {
            log.error("Error generating MD5 for plugin [" + pluginName + "]. Cause: " + e);
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
            PluginDescriptor descriptor = this.metadataManager.getPluginDescriptor(pluginName);
            AgentPluginDescriptorUtil.addPluginToDependencyGraph(dependencyGraph, descriptor);
        }
        return dependencyGraph;
    }

    private void registerPlugins(PluginDependencyGraph dependencyGraph, Set<String> pluginsToBeRegistered) {
        log.debug("Dependency graph deployment order: " + dependencyGraph.getDeploymentOrder());
        Map<String, DeploymentRunnable> dependencyRunnableMap = new HashMap<String, DeploymentRunnable>();
        for (String pluginName : pluginsToBeRegistered) {
            DeploymentRunnable service = getServiceIfExists(pluginName, dependencyRunnableMap);
            if (service == null) {
                log.warn("Cannot create the initial deployment runnable for plugin [" + pluginName + "]");
            }

            // We need to register dependencies also even if they aren't new or updated. This is because
            // PluginMetadataManager requires dependency plugins to be loaded in its pluginsByParser map.
            // ResourceMetadataManagerBean.register() will be smart enough to pass these plugins to
            // PluginMetadataManager to be parsed, but not to unnecessarily merge their types into the DB.
            for (String dependencyPluginName : dependencyGraph.getPluginDependencies(pluginName)) {
                DeploymentRunnable dependencyService = getServiceIfExists(dependencyPluginName, dependencyRunnableMap);
                if (null == dependencyService) {
                    log.warn("Ignoring [" + pluginName + "] dependency on missing dependency plugin: "
                        + dependencyPluginName);
                }
            }

            // In addition, we need to register plugins that are optionally dependent on the plugins we must register
            // in order to allow the dependents to refresh themselves and add any new child types that need to be registered.
            List<String> optionalDependents = dependencyGraph.getOptionalDependents(pluginName);
            for (String dependentPluginName : optionalDependents) {
                DeploymentRunnable dependentService = getServiceIfExists(dependentPluginName, dependencyRunnableMap);
                if (null != dependentService) {
                    dependentService.setForceUpdate(true); // make sure it updates its types, even if plugin hasn't changed
                } else {
                    log.warn("Ignoring [" + pluginName + "] dependent on missing dependent plugin: "
                        + dependentPluginName);
                }
            }
        }

        // get the order in which they should be deployed
        ArrayList<DeploymentRunnable> orderedDeploymentRunnables = new ArrayList<DeploymentRunnable>();
        List<String> pluginOrder = dependencyGraph.getDeploymentOrder();
        for (String nextPlugin : pluginOrder) {
            DeploymentRunnable nextRunnable = dependencyRunnableMap.get(nextPlugin);
            if (nextRunnable != null) {
                orderedDeploymentRunnables.add(nextRunnable);
            }
        }

        // now do the actual deployments in the correct order
        long startDeployTime = System.currentTimeMillis();
        for (DeploymentRunnable currentRunnable : orderedDeploymentRunnables) {
            currentRunnable.run();
        }
        long endDeployTime = System.currentTimeMillis();

        log.debug("Registered [" + pluginsToBeRegistered.size() + "] plugins in [" + (endDeployTime - startDeployTime)
            + "]ms");
    }

    // Who needs this???
    /*
    private List<String> getRegisteredPluginNames() {
        ResourceMetadataManagerLocal metadataManager = LookupUtil.getResourceMetadataManager();
        Collection<Plugin> plugins = metadataManager.getInstalledPlugins();
        List<String> pluginNames = new ArrayList<String>();

        for (Plugin plugin : plugins) {
            pluginNames.add(plugin.getName());
        }

        return pluginNames;
    }
    */

    /**
     * This will return the deployment runnable for the associated plugin.
     * This will create a DeploymentRunnable if one doesn't yet exist.
     * If it can't create one, null is returned.
     * 
     * @param pluginName
     * @param runnableMap
     * @return the deployment runnable that can be used to deploy the plugin; null if not able to create one
     */
    private DeploymentRunnable getServiceIfExists(String pluginName, Map<String, DeploymentRunnable> runnableMap) {

        DeploymentRunnable result = runnableMap.get(pluginName);
        if (result == null) {
            DeploymentInfo deploymentInfo = this.deploymentInfos.get(pluginName);
            PluginDescriptor descriptor = this.metadataManager.getPluginDescriptor(pluginName);
            CannedGroupAddition addition = PluginAdditionsReader.getCannedGroupsAddition(deploymentInfo.url, pluginName);
            if ((null != deploymentInfo) && (null != descriptor)) {
                result = new DeploymentRunnable(pluginName, deploymentInfo, descriptor, addition);
                runnableMap.put(pluginName, result);
            }
        }
        return result;
    }

    /**
     * This is the mechanism to kick off the registration of a new plugin. You must ensure you call this at the
     * appropriate time such that the plugin getting registered already has its dependencies registered.
     */
    private void registerPluginJar(PluginDescriptor pluginDescriptor, CannedGroupAddition addition, DeploymentInfo deploymentInfo, boolean forceUpdate) {
        if (pluginDescriptor == null) {
            log.error("Missing plugin descriptor; is [" + deploymentInfo.url + "] a valid plugin?");
            return;
        }

        try {
            File localPluginFile = new File(deploymentInfo.url.toURI());

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
            plugin.setAmpsVersion(getAmpsVersion(pluginDescriptor));

            // get the last modified of the "real" plugin jar since that's the one the user touches
            long mtime = deploymentInfo.url.openConnection().getLastModified();
            plugin.setMtime(mtime);

            if (pluginDescriptor.getHelp() != null && !pluginDescriptor.getHelp().getContent().isEmpty()) {
                plugin.setHelp(String.valueOf(pluginDescriptor.getHelp().getContent().get(0)));
            }

            plugin.setVersion(version);
            plugin.setMD5(MessageDigestGenerator.getDigestString(localPluginFile));

            // this manager is responsible for handling the munging of plugins that depend on other plugins
            // since we assume we are called in the proper deployment order, this should not fail
            // if we are called when hot-deploying a plugin whose dependencies aren't deployed, this will fail
            PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();
            pluginMgr.registerPlugin(plugin, pluginDescriptor, localPluginFile, forceUpdate);
            if (addition!=null) {
                GroupDefinitionManagerLocal groupDefMgr = LookupUtil.getGroupDefinitionManager();
                groupDefMgr.updateGroupsByCannedExpressions(pluginName, addition.getExpressions());
            }
        } catch (Exception e) {
            log.error("Failed to register RHQ plugin file [" + deploymentInfo.url + "]", e);
        }
    }

    private String getAmpsVersion(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor.getAmpsVersion() == null) {
            return "2.0";
        }

        ComparableVersion version = new ComparableVersion(pluginDescriptor.getAmpsVersion());
        ComparableVersion version2 = new ComparableVersion("2.0");

        if (version.compareTo(version2) <= 0) {
            return "2.0";
        }

        return pluginDescriptor.getAmpsVersion();
    }

    private void checkVersionCompatibility(String version) throws RuntimeException {
        /*if (new OSGiVersionComparator().compare((String) version, (String) AMPS_VERSION) < 0)
         * { throw new RuntimeException("Plugin AMPS requirement " + version + " not compatible with server's AMPS
         * version " + AMPS_VERSION);}*/
    }

    private void ensureDeploymentIsValid(File pluginFile) throws Exception {

        // try a few times (sleeping between retries)
        // if the zip file still isn't valid, its probably corrupted and not simply due to the file still being written out
        int retries = 4;
        while (!isDeploymentValidZipFile(pluginFile)) {
            if (--retries <= 0) {
                throw new Exception("File [" + pluginFile + "] is not a valid jarfile - "
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

    /**
     * This returns the name of the plugin file that is represented by the given
     * deployment info. This returns just the name of the plugin file, without
     * any parent directory information.
     * 
     * @param di the deployment info of the plugin file that is deployed
     * @return the name of the plugin file
     */
    private String getPluginJarFilename(DeploymentInfo di) {
        return new File(di.url.getPath()).getName();
    }

    class DeploymentRunnable implements Runnable {
        private final DeploymentInfo pluginDeploymentInfo;
        private final PluginDescriptor pluginDescriptor;
        private final CannedGroupAddition cgAddition;
        private final String pluginName;
        private boolean forceUpdate;

        public DeploymentRunnable(String pluginName, DeploymentInfo di, PluginDescriptor descriptor, CannedGroupAddition cgAddition) {
            this.pluginName = pluginName;
            this.pluginDeploymentInfo = di;
            this.pluginDescriptor = descriptor;
            this.cgAddition = cgAddition;
            this.forceUpdate = false;
        }

        public void setForceUpdate(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
        }

        @Override
        public void run() {
            log.debug("Being asked to deploy plugin [" + this.pluginName + "]...");
            registerPluginJar(this.pluginDescriptor, this.cgAddition, this.pluginDeploymentInfo, this.forceUpdate);
        }
    }

    static class DeploymentInfo {
        public final URL url;

        public DeploymentInfo(URL url) {
            if (url == null) {
                throw new IllegalArgumentException("url == null");
            }
            this.url = url;
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || (!(obj instanceof DeploymentInfo))) {
                return false;
            }
            return url.equals(((DeploymentInfo) obj).url);
        }
    }
}