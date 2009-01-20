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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipInputStream;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
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
import org.jetbrains.annotations.NotNull;
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
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;
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

    private static final String PRODUCT = "RHQ";
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

    private File pluginDir = null;
    private String licenseFile = null;

    /** Map of plugin names to the corresponding plugins' JBAS deployment infos */
    private Map<String, DeploymentInfo> deploymentInfos = new HashMap<String, DeploymentInfo>();
    /** Map of plugin names to the corresponding plugins' JAXB plugin descriptors */
    private Map<String, PluginDescriptor> pluginDescriptors = new HashMap<String, PluginDescriptor>();
    /** Map of plugin names to the corresponding plugins' versions */
    private Map<String, ComparableVersion> pluginVersions = new HashMap<String, ComparableVersion>();
    private boolean isStarted = false;
    private boolean isReady = false;
    private NotificationBroadcasterSupport broadcaster = new NotificationBroadcasterSupport();
    private AtomicLong notifSequence = new AtomicLong(0);
    private Set<String> namesOfPluginsToBeRegistered = new HashSet<String>();

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
    public void setPluginDir(String pluginDirString) {
        this.pluginDir = new File(pluginDirString);

        // this directory should always exist, but just in case it doesn't, create it
        if (!this.pluginDir.exists()) {
            this.pluginDir.mkdirs();
        }
    }

    /**
     * @see org.rhq.enterprise.server.core.plugin.ProductPluginDeployerMBean#getPluginDir()
     */
    public String getPluginDir() {
        return this.pluginDir.getAbsolutePath();
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

        if (!urlFile.endsWith("jar")) {
            return false;
        }

        File deploymentDirectory = new File(urlFile).getParentFile();

        if (deploymentDirectory.getName().equals(this.pluginDir.getName())) {
            log.debug("accepting agent plugin=" + urlFile);
            return true;
        }

        return false;
    }

    /**
     * @see org.rhq.enterprise.server.core.plugin.ProductPluginDeployerMBean#startDeployer()
     */
    public void startDeployer() {
        pluginNotify("deployer", DEPLOYER_READY);

        // Do startup license checking. TODO: Move this code somewhere else.
        LicenseManager licenseManager = LicenseManager.instance();
        licenseManager.doStartupCheck(this.licenseFile);

        registerPlugins();

        pluginNotify("deployer", DEPLOYER_CLEARED);

        this.isReady = true;
        return;
    }

    // NOTE: This method must only be called after this.isReady == true!
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
            // TODO: Doesn't it make more sense to throw an exception here?
            log.error(errorBuffer.toString());
            log.error(dependencyGraph.toString());
            return;
        }
        registerPlugins(dependencyGraph);
        this.namesOfPluginsToBeRegistered.clear();

        // Trigger vacuums on some tables as the initial deployment might have changed a lot of things.
        // There are probably more tables involved though.
        // First wait to give Hibernate a chance to close all transactions etc.
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException ignored) {
            // no problem
        }
        Subject superuser = LookupUtil.getSubjectManager().getOverlord();
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        systemManager.vacuum(superuser, new String[] { "RHQ_MEASUREMENT_DEF", "RHQ_CONFIG_DEF", "RHQ_RESOURCE_TYPE",
            "RHQ_RESOURCE_TYPE_PARENTS" });
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

    private boolean isNewOrUpdated(String pluginName) {
        DeploymentInfo deploymentInfo = this.deploymentInfos.get(pluginName);
        if (deploymentInfo == null)
            throw new IllegalStateException("DeploymentInfo was not found for plugin [" + pluginName
                + " ] - it should have been initialized by preprocessPlugin().");
        String md5 = null;
        try {
            md5 = MD5Generator.getDigestString(deploymentInfo.url.openStream());
        } catch (IOException e) {
            log.error("Error generating MD5 for plugin [" + pluginName + "].");
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

        log.debug("Registered [" + dependencyGraph.getPlugins().size() + "] plugins in "
            + (endDeployTime - startDeployTime) + " millis.");
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
        String pluginJar = deploymentInfo.url.getFile();

        if (pluginDescriptor == null) {
            log.error("Could not find a valid plugin descriptor -- is this a plugin archive at " + pluginJar + " ?");
            return;
        }

        try {
            String pluginName = pluginDescriptor.getName();
            String pluginNameDisplayName = pluginName + " (" + pluginDescriptor.getDisplayName() + ")";
            ComparableVersion comparableVersion = this.pluginVersions.get(pluginName);
            String version = (comparableVersion != null) ? comparableVersion.toString() : null;
            log.debug("Registering RHQ plugin " + pluginNameDisplayName + ", "
                + ((version != null) ? "version " + version : "undefined version") + "...");
            checkVersionCompatibility(pluginDescriptor.getAmpsVersion());

            // make sure the path is only the filename
            String filename = new File(deploymentInfo.url.getPath()).getName();
            Plugin plugin = new Plugin(pluginName, filename);
            plugin.setDisplayName((pluginDescriptor.getDisplayName() != null) ? pluginDescriptor.getDisplayName()
                : pluginName);
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

            // stream the actual plugin jar content into the database
            streamPluginFileContentToDatabase(plugin.getName(), new File(this.pluginDir, plugin.getPath()));

            pluginNotify(pluginNameDisplayName, PLUGIN_REGISTERED);
        } catch (Exception e) {
            log.error("Failed to register RHQ plugin [" + pluginJar + "]", e);
        }
    }

    private void checkVersionCompatibility(String version) throws RuntimeException {
        /*if (new OSGiVersionComparator().compare((String) version, (String) AMPS_VERSION) < 0)
         * { throw new RuntimeException("Plugin AMPS requirement " + version + " not compatible with server's AMPS
         * version " + AMPS_VERSION);}*/
    }

    private PluginDescriptor getPluginDescriptor(DeploymentInfo di) throws DeploymentException {
        URL descriptorURL = di.localCl.findResource(DEFAULT_PLUGIN_DESCRIPTOR_PATH);
        if (descriptorURL == null) {
            throw new DeploymentException("Could not load " + DEFAULT_PLUGIN_DESCRIPTOR_PATH
                + " from plugin jar file [" + di.url + "]");
        }

        PluginDescriptor pluginDescriptor;
        ValidationEventCollector vec;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable schema validation. (see http://jira.jboss.com/jira/browse/JBNADM-1539)
            URL pluginSchemaURL = getClass().getClassLoader().getResource("rhq-plugin.xsd");
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
            throw new DeploymentException("Failed to parse plugin descriptor for plugin jar file [" + di.url + "].", e);
        }

        for (ValidationEvent event : vec.getEvents()) {
            log.warn(event.getSeverity() + ":" + event.getMessage() + "    " + event.getLinkedException());
        }

        return pluginDescriptor;
    }

    private void checkDeploymentIsValidZipFile(DeploymentInfo deploymentInfo) throws DeploymentException {
        if (deploymentInfo.isDirectory)
            return;
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(deploymentInfo.url.openStream());
            zipInputStream.getNextEntry();
        } catch (IOException e) {
            throw new DeploymentException("File [" + deploymentInfo.url + "] is not a valid jarfile - "
                + " perhaps the file has not been fully written yet.", e);
        } finally {
            if (zipInputStream != null)
                try {
                    zipInputStream.close();
                } catch (IOException e) {
                    log.error("Failed to close zip input stream for file [" + deploymentInfo.url + "].");
                }
        }
    }

    /**
     * MBean Service start method. This method is called when JBoss is deploying the MBean.
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        if (!isStarted) {
            extractPluginFilesFromDatabase();
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
            this.deploymentInfos.clear();
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
            // yes we really need to call start() - you'd think the app server would call it earlier but it doesn't sometimes
            this.start();
        } catch (Exception e) {
            throw new DeploymentException(e);
        }

        if (isLicenseFile(deploymentInfo))
            return;

        String pluginJarFileName = deploymentInfo.url.getFile();
        log.debug("start(): " + pluginJarFileName);
        String pluginName = preprocessPlugin(deploymentInfo);
        if (this.isReady) {
            // isReady == true means startDeployer() has already been called, so this is a hot deploy.
            // Call registerPlugins() ourselves.
            log.debug("Hot deploying plugin [" + pluginName + "]...");
            try {
                registerPlugins();
            } catch (Exception e) {
                throw new DeploymentException("Unable to deploy RHQ plugin [" + pluginName + "]", e);
            }
        } else {
            // Otherwise, startDeployer() has not been called yet. Once it is called, it will take care
            // of calling registerPlugins().
            log.debug("Performing initial deploy of plugin [" + pluginName + "]...");
        }
        return;
    }

    /**
     * @see org.jboss.deployment.SubDeployerSupport#stop(org.jboss.deployment.DeploymentInfo)
     */
    @Override
    public void stop(DeploymentInfo deploymentInfo) throws DeploymentException {
        if (isLicenseFile(deploymentInfo))
            return;

        String pluginJarFileName = deploymentInfo.url.getFile();
        log.debug("stop: " + pluginJarFileName);

        try {
            // TODO: Actually undeploy the plugin from the DB (this will be a lot of work...).
            pluginNotify(new File(pluginJarFileName).getName(), PLUGIN_UNDEPLOYED);
        } catch (Exception e) {
            throw new DeploymentException(e);
        }

        return;
    }

    private void pluginNotify(String name, String notifType) {
        String action = notifType.substring(notifType.lastIndexOf(".") + 1);
        String msg = "Plugin " + name + " " + action + ".";
        Notification notif = new Notification(notifType, this, this.notifSequence.incrementAndGet(), msg);
        log.debug(msg);
        broadcaster.sendNotification(notif);
    }

    private static boolean isLicenseFile(DeploymentInfo deploymentInfo) {
        String name = LicenseManager.getLicenseFileName();
        return deploymentInfo.url.getFile().endsWith(name);
    }

    /**
     * This will extract the contents of all agent plugins from the database and write the
     * plugin jar files on the file system.
     */
    private void extractPluginFilesFromDatabase() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        // This map contains the names/paths of plugins that are missing their content in the database.
        // This map will only have entries if this server was recently upgraded from an older version
        // that did not support database-stored plugin content.
        Map<String, String> pluginsMissingContent = new HashMap<String, String>();

        try {
            DataSource ds = LookupUtil.getDataSource();
            conn = ds.getConnection();
            ps = conn.prepareStatement("SELECT NAME, PATH, MD5, CONTENT FROM " + Plugin.TABLE_NAME);
            rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String path = rs.getString(2);
                String md5 = rs.getString(3);
                InputStream content = rs.getBinaryStream(4);
                if (content != null) {
                    writePluginContentToFileIfAppropriate(name, new File(this.pluginDir, path), md5, content);
                } else {
                    pluginsMissingContent.put(name, path);
                }
            }
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);
        }

        // to support the use case when our server has recently been upgraded, we need to put
        // the content of the original plugins into the database.
        for (Map.Entry<String, String> entry : pluginsMissingContent.entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue();
            File pluginFile = new File(this.pluginDir, path);
            if (pluginFile.exists()) {
                streamPluginFileContentToDatabase(name, pluginFile);
            } else {
                throw new Exception("The database knows of a plugin named [" + name + "] with path [" + path
                    + "] but the content is missing. This server does not have this plugin at [" + pluginFile
                    + "] so the database cannot be updated with the content.");
            }
        }

        return;
    }

    private void writePluginContentToFileIfAppropriate(String name, File pluginFile, String md5, InputStream content)
        throws Exception {
        // if the plugin file already exists, check its md5 and if it is the same, do nothing and return
        if (pluginFile.exists()) {
            String fileMD5 = MD5Generator.getDigestString(pluginFile);
            if (fileMD5.equals(md5)) {
                log.debug("Plugin file [" + pluginFile + "] hasn't changed; leaving it as-is");
                return;
            }
        }

        log.info("Plugin file [" + pluginFile + "] will be overwritten with the new content found in the database");
        FileOutputStream fos = new FileOutputStream(pluginFile);
        StreamUtil.copy(content, fos); // note that this closes our "content" stream parameter for us
        return;
    }

    private void streamPluginFileContentToDatabase(String name, File file) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        TransactionManager tm = null;
        FileInputStream fis = new FileInputStream(file);

        try {
            tm = LookupUtil.getTransactionManager();
            tm.begin();
            DataSource ds = LookupUtil.getDataSource();
            conn = ds.getConnection();
            ps = conn.prepareStatement("UPDATE " + Plugin.TABLE_NAME + " SET CONTENT = ? WHERE NAME = ?");
            ps.setBinaryStream(1, new BufferedInputStream(fis), (int) file.length());
            ps.setString(2, name);
            int updateResults = ps.executeUpdate();
            if (updateResults != 1) {
                throw new Exception("Failed to update content for plugin [" + name + "] from [" + file + "]");
            }
        } catch (Exception e) {
            tm.rollback();
            tm = null;
            throw e;
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);

            try {
                fis.close();
            } catch (Throwable t) {
            }

            if (tm != null) {
                tm.commit();
            }
        }
        return;
    }

    /**
     * Process the specified plugin jar to figure out the plugin name and version. If it is the only plugin with this
     * name, or if it has the newest version among other plugins with the same name, then add it to our master set of
     * plugins to be registered. Once all EJBs are started, {@link #startDeployer()} will be called and will take care
     * of registering the plugins.
     */
    private String preprocessPlugin(DeploymentInfo deploymentInfo) throws DeploymentException {
        checkDeploymentIsValidZipFile(deploymentInfo);
        PluginDescriptor descriptor = getPluginDescriptor(deploymentInfo);
        String pluginName = descriptor.getName();
        boolean initialDeploy = !this.deploymentInfos.containsKey(pluginName);
        ComparableVersion version = getPluginVersion(deploymentInfo, descriptor);

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

    private boolean isNewestVersion(String pluginName, ComparableVersion version) {
        boolean newestVersion;
        ComparableVersion existingVersion = this.pluginVersions.get(pluginName);
        if (existingVersion != null) {
            newestVersion = (version.compareTo(existingVersion) >= 0);
            if (newestVersion)
                log.debug("Newer version of '" + pluginName + "' plugin found (version " + version
                    + ") - older version (" + existingVersion + ") will be ignored.");
        } else {
            newestVersion = false;
        }
        return newestVersion;
    }

    @NotNull
    private ComparableVersion getPluginVersion(DeploymentInfo deploymentInfo, PluginDescriptor descriptor)
        throws DeploymentException {
        // First, see if a version is defined in the plugin descriptor.
        String version = descriptor.getVersion();
        if (version == null) {
            // If not, check the plugin jar's MANIFEST.MF file.
            Manifest manifest = getManifest(deploymentInfo);
            version = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        }
        String pluginJarFileName = deploymentInfo.url.getFile();
        ComparableVersion comparableVersion;
        if (version == null) {
            throw new DeploymentException("No version is defined for plugin jar '" + pluginJarFileName
                + "'. A version must be defined either via the MANIFEST.MF '" + Attributes.Name.IMPLEMENTATION_VERSION
                + "' attribute or via the plugin descriptor 'version' attribute.");
        }
        try {
            comparableVersion = new ComparableVersion(version);
        } catch (RuntimeException e) {
            throw new DeploymentException("Failed to parse version (" + version + ") for plugin jar '"
                + pluginJarFileName + "'.", e);
        }
        return comparableVersion;
    }

    // Use this method rather than calling deploymentInfo.getManifest()
    // (workaround for https://jira.jboss.org/jira/browse/JBAS-6266).
    private static Manifest getManifest(DeploymentInfo deploymentInfo) {
        try {
            File file = new File(deploymentInfo.localUrl.getFile());
            Manifest manifest;
            if (file.isDirectory()) {
                FileInputStream fis = new FileInputStream(new File(file, "META-INF/MANIFEST.MF"));
                manifest = new Manifest(fis);
                fis.close();
            } else { // a jar
                JarFile jarFile = new JarFile(file);
                manifest = jarFile.getManifest();
                jarFile.close();
            }
            return manifest;
        }
        // It is ok to barf at any time in the above, means no manifest
        catch (Exception ignored) {
            return null;
        }
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
}