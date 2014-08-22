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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.plugin.ServerPluginManagerLocal;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * This looks at both the file system and the database for new server plugins.
 *
 * @author John Mazzitelli
 */
public class ServerPluginScanner {
    private Log log = LogFactory.getLog(ServerPluginScanner.class);

    /** a list of server plugins found on previous scans that have not yet been processed */
    private List<File> scanned = new ArrayList<File>();

    /** Maintains a cache of what we had on the filesystem during the last scan */
    private Map<File, PluginWithDescriptor> serverPluginsOnFilesystem = new HashMap<File, PluginWithDescriptor>();

    /** directory where the server plugin jar files are found */
    private File serverPluginDir;

    public ServerPluginScanner() {
    }

    public File getServerPluginDir() {
        return this.serverPluginDir;
    }

    public void setServerPluginDir(File dir) {
        this.serverPluginDir = dir;
    }

    /**
     * This should be called after a call to {@link #serverPluginScan()} to register
     * plugins that were found in the scan.
     * 
     * This will also check to see if previously registered plugins changed their state.
     *
     * @throws Exception on unexpected errors
     */
    void registerServerPlugins() throws Exception {
        for (File file : this.scanned) {
            log.debug("Deploying server plugin [" + file + "]...");
            registerServerPlugin(file);
        }
        this.scanned.clear();

        // we now have to see if the state of existing plugins have changed in the DB
        ServerPluginManagerLocal serverPluginsManager = LookupUtil.getServerPluginManager();
        List<ServerPlugin> allPlugins = serverPluginsManager.getAllServerPlugins();
        List<ServerPlugin> installedPlugins = new ArrayList<ServerPlugin>();
        List<ServerPlugin> undeployedPlugins = new ArrayList<ServerPlugin>();
        for (ServerPlugin plugin : allPlugins) {
            if (plugin.getStatus() == PluginStatusType.INSTALLED) {
                installedPlugins.add(plugin);
            } else {
                undeployedPlugins.add(plugin);
            }
        }

        // first, have any plugins been undeployed since we last checked?
        for (ServerPlugin undeployedPlugin : undeployedPlugins) {
            File undeployedFile = new File(this.getServerPluginDir(), undeployedPlugin.getPath());
            PluginWithDescriptor pluginWithDescriptor = this.serverPluginsOnFilesystem.get(undeployedFile);
            if (pluginWithDescriptor != null) {
                try {
                    log.info("Plugin file [" + undeployedFile + "] has been undeployed. It will be deleted.");
                    List<Integer> id = Arrays.asList(undeployedPlugin.getId());
                    serverPluginsManager.deleteServerPlugins(LookupUtil.getSubjectManager().getOverlord(), id);
                } finally {
                    this.serverPluginsOnFilesystem.remove(undeployedFile);
                }
            }
        }

        // now see if any plugins changed state from enabled->disabled or vice versa
        // this also checks to see if the plugin configuration has changed since it was last loaded 
        MasterServerPluginContainer master = LookupUtil.getServerPluginService().getMasterPluginContainer();
        if (master != null) {
            for (ServerPlugin installedPlugin : installedPlugins) {
                PluginKey key = PluginKey.createServerPluginKey(installedPlugin.getType(), installedPlugin.getName());
                AbstractTypeServerPluginContainer pc = master.getPluginContainerByPlugin(key);
                if (pc != null && pc.isPluginLoaded(key)) {
                    boolean needToReloadPlugin = false;
                    boolean currentlyEnabled = pc.isPluginEnabled(key);
                    if (installedPlugin.isEnabled() != currentlyEnabled) {
                        log.info("Detected a state change to plugin [" + key + "]. It will now be "
                            + ((installedPlugin.isEnabled()) ? "[enabled]" : "[disabled]"));
                        needToReloadPlugin = true;
                    } else {
                        Long pluginLoadTime = pc.getPluginLoadTime(key);
                        if (pluginLoadTime != null) {
                            long configChangeTimestamp = serverPluginsManager
                                .getLastConfigurationChangeTimestamp(installedPlugin.getId());
                            if (configChangeTimestamp > pluginLoadTime) {
                                // since the last time the plugin was loaded, its configuration has changed, reload it to pick up the new config 
                                log.info("Detected a config change to plugin [" + key + "]. It will be reloaded and "
                                    + ((installedPlugin.isEnabled()) ? "[enabled]" : "[disabled]"));
                                needToReloadPlugin = true;
                            }
                        }
                    }
                    if (needToReloadPlugin) {
                        pc.reloadPlugin(key, installedPlugin.isEnabled());
                    }
                }
            }
        }

        return;
    }

    /**
     * This method just scans the filesystem and DB for server plugin changes but makes
     * no attempt to register the plugins.
     * 
     * @throws Exception on unexpected errors
     */
    void serverPluginScan() throws Exception {
        log.debug("Scanning for server plugins");

        if (this.getServerPluginDir() == null || !this.getServerPluginDir().isDirectory()) {
            // nothing to do since there is no plugin directory configured
            return;
        }

        // ensure that the filesystem and database are in a consistent state
        List<File> updatedFiles1 = serverPluginScanFilesystem();
        List<File> updatedFiles2 = serverPluginScanDatabase();

        // process any newly detected plugins
        List<File> allUpdatedFiles = new ArrayList<File>();
        allUpdatedFiles.addAll(updatedFiles1);
        allUpdatedFiles.addAll(updatedFiles2);

        for (File updatedFile : allUpdatedFiles) {
            log.debug("Scan detected server plugin [" + updatedFile + "]...");
            this.scanned.add(updatedFile);
        }

        ServerPluginManagerLocal pluginMgr = LookupUtil.getServerPluginManager();
        ServerManagerLocal serverMgr = LookupUtil.getServerManager();
        Server thisServer = serverMgr.getServer();

        pluginMgr.acknowledgeDeletedPluginsBy(thisServer.getId());
    }

    /**
     * This is called when a server plugin jar has been found on the filesystem that hasn't been seen yet
     * during this particular lifetime of the scanner. This does not necessarily mean its a new plugin jar,
     * it only means this is the first time we've seen it since this object has been instantiated.
     * This method will check to see if the database record matches the new plugin file and if so, does nothing.
     * 
     * @param pluginFile the new server plugin file
     */
    private void registerServerPlugin(File pluginFile) {
        try {
            ServerPluginDescriptorType descriptor;
            descriptor = this.serverPluginsOnFilesystem.get(pluginFile).descriptor;

            String pluginName = descriptor.getName();
            String displayName = descriptor.getDisplayName();

            ComparableVersion version; // this must be non-null, the next line ensures this
            version = ServerPluginDescriptorUtil.getPluginVersion(pluginFile, descriptor);

            log.debug("Registering server plugin [" + pluginName + "], version " + version);

            ServerPlugin plugin = new ServerPlugin(pluginName, pluginFile.getName());
            plugin.setDisplayName((displayName != null) ? displayName : pluginName);
            plugin.setEnabled(!descriptor.isDisabledOnDiscovery());
            plugin.setDescription(descriptor.getDescription());
            plugin.setMtime(pluginFile.lastModified());
            plugin.setVersion(version.toString());
            plugin.setAmpsVersion(descriptor.getApiVersion());
            plugin.setMD5(MessageDigestGenerator.getDigestString(pluginFile));
            plugin.setPluginConfiguration(getDefaultPluginConfiguration(descriptor));
            plugin.setScheduledJobsConfiguration(getDefaultScheduledJobsConfiguration(descriptor));
            plugin.setType(new ServerPluginType(descriptor).stringify());

            if (descriptor.getHelp() != null && !descriptor.getHelp().getContent().isEmpty()) {
                plugin.setHelp(String.valueOf(descriptor.getHelp().getContent().get(0)));
            }

            ServerPluginManagerLocal serverPluginsManager = LookupUtil.getServerPluginManager();

            // see if this plugin has been deleted previously; if so, don't register and delete the file
            PluginKey newPluginKey = new PluginKey(plugin);
            PluginStatusType status = serverPluginsManager.getServerPluginStatus(newPluginKey);

            if (PluginStatusType.DELETED == status) {
                log.warn("Plugin file [" + pluginFile + "] has been detected but that plugin with name [" + pluginName
                    + "] was previously undeployed. Will not re-register that plugin and the file will be deleted.");
                boolean succeeded = pluginFile.delete();
                if (!succeeded) {
                    log.error("Failed to delete obsolete plugin file [" + pluginFile + "].");
                }
            } else {
                // now attempt to register the plugin. "dbPlugin" will be the new updated plugin; but if
                // the scanned plugin does not obsolete the current plugin, then dbPlugin will be the old, still valid, plugin.
                SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
                ServerPlugin dbPlugin = serverPluginsManager.registerServerPlugin(subjectManager.getOverlord(), plugin,
                    pluginFile);
                log.info("Registered server plugin [" + dbPlugin.getName() + "], version " + dbPlugin.getVersion());
            }
        } catch (Exception e) {
            log.error("Failed to register server plugin file [" + pluginFile + "]", e);
        }
        return;
    }

    private Configuration getDefaultPluginConfiguration(ServerPluginDescriptorType descriptor) throws Exception {
        Configuration defaults = null;
        ConfigurationDefinition def = ServerPluginDescriptorMetadataParser.getPluginConfigurationDefinition(descriptor);
        if (def != null) {
            defaults = createDefaultConfiguration(def);
        }
        return defaults;
    }

    private Configuration getDefaultScheduledJobsConfiguration(ServerPluginDescriptorType descriptor) throws Exception {
        Configuration defaults = null;
        ConfigurationDefinition def = ServerPluginDescriptorMetadataParser.getScheduledJobsDefinition(descriptor);
        if (def != null) {
            defaults = createDefaultConfiguration(def);
        }
        return defaults;
    }

    private Configuration createDefaultConfiguration(ConfigurationDefinition def) {
        ConfigurationTemplate defaultTemplate = def.getDefaultTemplate();
        return (defaultTemplate != null) ? defaultTemplate.createConfiguration() : new Configuration();
    }

    /**
     * Scans the plugin directory and updates our cache of known plugin files.
     * This will purge any old plugins that are deemed obsolete.
     * 
     * @return a list of files that appear to be new or updated and should be deployed
     */
    private List<File> serverPluginScanFilesystem() {
        List<File> updated = new ArrayList<File>();

        // get the current list of plugins deployed on the filesystem
        File[] pluginJars = this.getServerPluginDir().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        // refresh our cache so it reflects what is currently on the filesystem
        // first we remove any jar files in our cache that we no longer have on the filesystem
        ArrayList<File> doomedPluginFiles = new ArrayList<File>();
        for (File cachedPluginFile : this.serverPluginsOnFilesystem.keySet()) {
            boolean existsOnFileSystem = false;
            for (File filesystemPluginFile : pluginJars) {
                if (cachedPluginFile.equals(filesystemPluginFile)) {
                    existsOnFileSystem = true;
                    break; // our cached jar still exists on the file system
                }
            }
            if (!existsOnFileSystem) {
                doomedPluginFiles.add(cachedPluginFile); // this plugin file has been deleted from the filesystem, remove it from the cache
            }
        }
        for (File deletedPluginFile : doomedPluginFiles) {
            this.serverPluginsOnFilesystem.remove(deletedPluginFile);
        }

        // now insert new cache items representing new jar files and update existing ones as appropriate
        for (File pluginJar : pluginJars) {
            String md5 = null;

            PluginWithDescriptor pluginWithDescriptor = this.serverPluginsOnFilesystem.get(pluginJar);
            ServerPlugin plugin = null;
            if (pluginWithDescriptor != null) {
                plugin = pluginWithDescriptor.plugin;
            }

            try {
                if (plugin != null) {
                    if (pluginJar.lastModified() == 0L) {
                        // for some reason the operating system can't give us the last mod time, we need to do MD5 check
                        md5 = MessageDigestGenerator.getDigestString(pluginJar);
                        if (!md5.equals(plugin.getMd5())) {
                            plugin = null; // this plugin jar has changed - force it to refresh the cache.
                        }
                    } else if (pluginJar.lastModified() != plugin.getMtime()) {
                        plugin = null; // this plugin jar has changed - force it to refresh the cache.
                    }
                }

                if (plugin == null) {
                    cacheFilesystemServerPluginJar(pluginJar, md5);
                    updated.add(pluginJar);
                }
            } catch (Exception e) {
                log.warn("Failed to scan server plugin [" + pluginJar + "] found on filesystem. Skipping. Cause: " + e);
                this.serverPluginsOnFilesystem.remove(pluginJar); // act like we never saw it
                updated.remove(pluginJar);
            }
        }

        // Let's check to see if there are any obsolete plugins that need to be deleted.
        // This is needed if plugin-A-1.0.jar exists and someone deployed plugin-A-1.1.jar but fails to delete plugin-A-1.0.jar.
        doomedPluginFiles.clear();
        HashMap<String, ServerPlugin> pluginsByName = new HashMap<String, ServerPlugin>(); // key on (name+type), not just name
        for (Entry<File, PluginWithDescriptor> currentPluginFileEntry : this.serverPluginsOnFilesystem.entrySet()) {
            ServerPlugin currentPlugin = currentPluginFileEntry.getValue().plugin;
            ServerPlugin existingPlugin = pluginsByName.get(currentPlugin.getName() + currentPlugin.getType());
            if (existingPlugin == null) {
                // this is the usual case - this is the only plugin with the given name we've seen
                pluginsByName.put(currentPlugin.getName() + currentPlugin.getType(), currentPlugin);
            } else {
                ServerPlugin obsolete = ServerPluginDescriptorUtil.determineObsoletePlugin(currentPlugin,
                    existingPlugin);
                if (obsolete == null) {
                    obsolete = currentPlugin; // both were identical, but we only want one file so pick one to get rid of
                }
                doomedPluginFiles.add(new File(this.getServerPluginDir(), obsolete.getPath()));
                if (obsolete == existingPlugin) { // yes use == for reference equality!
                    pluginsByName.put(currentPlugin.getName() + currentPlugin.getType(), currentPlugin); // override the original one we saw with this latest one
                }
            }
        }

        // now we need to actually delete any obsolete plugin files from the file system
        for (File doomedPluginFile : doomedPluginFiles) {
            if (doomedPluginFile.delete()) {
                log.info("Deleted an obsolete server plugin file: " + doomedPluginFile);
                this.serverPluginsOnFilesystem.remove(doomedPluginFile);
                updated.remove(doomedPluginFile);
            } else {
                log.warn("Failed to delete what was deemed an obsolete server plugin file: " + doomedPluginFile);
            }
        }

        return updated;
    }

    /**
     * Creates a {@link ServerPlugin} object for the given plugin jar and caches it.
     * @param pluginJar information about this plugin jar will be cached
     * @param md5 if known, this is the plugin jar's MD5, <code>null</code> if not known
     * @return the plugin jar file's information that has been cached
     * @throws Exception if failed to get information about the plugin
     */
    private ServerPlugin cacheFilesystemServerPluginJar(File pluginJar, @Nullable String md5) throws Exception {
        if (md5 == null) { // don't calculate the MD5 is we've already done it before
            md5 = MessageDigestGenerator.getDigestString(pluginJar);
        }
        URL pluginUrl = pluginJar.toURI().toURL();
        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginUrl);
        String version = ServerPluginDescriptorUtil.getPluginVersion(pluginJar, descriptor).toString();
        String name = descriptor.getName();
        ServerPlugin plugin = new ServerPlugin(name, pluginJar.getName());
        plugin.setType(new ServerPluginType(descriptor).stringify());
        plugin.setMd5(md5);
        plugin.setVersion(version);
        plugin.setMtime(pluginJar.lastModified());
        this.serverPluginsOnFilesystem.put(pluginJar, new PluginWithDescriptor(plugin, descriptor));
        return plugin;
    }

    /**
     * This method scans the database for any new or updated server plugins and make sure this server
     * has a plugin file on the filesystem for each of those new/updated server plugins.
     *
     * This also checks to see if the enabled flag changed for plugins that we already know about.
     * If it does, and its plugin container has the plugin already loaded, the plugin will be reloaded.
     *
     * @return a list of files that appear to be new or updated and should be deployed
     */
    private List<File> serverPluginScanDatabase() throws Exception {
        // these are plugins (name/path/md5/mtime) that have changed in the DB but are missing from the file system
        List<ServerPlugin> updatedPlugins = new ArrayList<ServerPlugin>();

        // the same list as above, only they are the files that are written to the filesystem and no longer missing
        List<File> updatedFiles = new ArrayList<File>();

        // process all the installed plugins
        ServerPluginManagerLocal serverPluginsManager = LookupUtil.getServerPluginManager();
        List<ServerPlugin> installedPlugins = serverPluginsManager.getServerPlugins();
        for (ServerPlugin installedPlugin : installedPlugins) {
            String name = installedPlugin.getName();
            String path = installedPlugin.getPath();
            String md5 = installedPlugin.getMd5();
            long mtime = installedPlugin.getMtime();
            String version = installedPlugin.getVersion();
            ServerPluginType pluginType = new ServerPluginType(installedPlugin.getType());

            // let's see if we have this logical plugin on the filesystem (it may or may not be under the same filename)
            File expectedFile = new File(this.getServerPluginDir(), path);
            File currentFile = null; // will be non-null if we find that we have this plugin on the filesystem already
            PluginWithDescriptor pluginWithDescriptor = this.serverPluginsOnFilesystem.get(expectedFile);

            if (pluginWithDescriptor != null) {
                currentFile = expectedFile; // we have it where we are expected to have it
                if (!pluginWithDescriptor.plugin.getName().equals(name)
                    || !pluginType.equals(pluginWithDescriptor.pluginType)) {
                    // Happens if someone wrote a plugin of one type but later changed it to a different type (or changed names)
                    log.warn("For some reason, the server plugin file [" + expectedFile + "] is plugin ["
                        + pluginWithDescriptor.plugin.getName() + "] of type [" + pluginWithDescriptor.pluginType
                        + "] but the database says it should be [" + name + "] of type [" + pluginType + "]");
                } else {
                    log.debug("File system and db agree on server plugin location for [" + expectedFile + "]");
                }
            } else {
                // the plugin might still be on the file system but under a different filename, see if we can find it
                for (Map.Entry<File, PluginWithDescriptor> cacheEntry : this.serverPluginsOnFilesystem.entrySet()) {
                    if (cacheEntry.getValue().plugin.getName().equals(name)
                        && cacheEntry.getValue().pluginType.equals(pluginType)) {
                        currentFile = cacheEntry.getKey();
                        pluginWithDescriptor = cacheEntry.getValue();
                        log.info("Filesystem has a server plugin [" + name + "] at the file [" + currentFile
                            + "] which is different than where the DB thinks it should be [" + expectedFile + "]");
                        break; // we found it, no need to continue the loop
                    }
                }
            }

            if (pluginWithDescriptor != null && currentFile != null && currentFile.exists()) {
                ServerPlugin dbPlugin = new ServerPlugin(name, path);
                dbPlugin.setType(pluginType.stringify());
                dbPlugin.setMd5(md5);
                dbPlugin.setVersion(version);
                dbPlugin.setMtime(mtime);

                ServerPlugin obsoletePlugin = ServerPluginDescriptorUtil.determineObsoletePlugin(dbPlugin,
                    pluginWithDescriptor.plugin);

                if (obsoletePlugin == pluginWithDescriptor.plugin) { // yes use == for reference equality!
                    StringBuilder logMsg = new StringBuilder();
                    logMsg.append("Found server plugin [").append(name);
                    logMsg.append("] in the DB that is newer than the one on the filesystem: ");
                    logMsg.append("DB path=[").append(path);
                    logMsg.append("]; file path=[").append(currentFile.getName());
                    logMsg.append("]; DB MD5=[").append(md5);
                    logMsg.append("]; file MD5=[").append(pluginWithDescriptor.plugin.getMd5());
                    logMsg.append("]; DB version=[").append(version);
                    logMsg.append("]; file version=[").append(pluginWithDescriptor.plugin.getVersion());
                    logMsg.append("]; DB timestamp=[").append(new Date(mtime));
                    logMsg.append("]; file timestamp=[").append(new Date(pluginWithDescriptor.plugin.getMtime()));
                    logMsg.append("]");
                    log.info(logMsg.toString());

                    updatedPlugins.add(dbPlugin);

                    if (currentFile.delete()) {
                        log.info("Deleted the obsolete server plugin file to be updated: " + currentFile);
                        this.serverPluginsOnFilesystem.remove(currentFile);
                    } else {
                        log.warn("Failed to delete the obsolete (to-be-updated) server plugin: " + currentFile);
                    }
                } else if (obsoletePlugin == null) {
                    // the db is up-to-date, but update the cache so we don't check MD5 or parse the descriptor again
                    boolean succeeded = currentFile.setLastModified(mtime);
                    if (!succeeded && log.isDebugEnabled()) {
                        log.debug("Failed to set mtime to [" + new Date(mtime) + "] on file [" + currentFile + "].");
                    }
                    pluginWithDescriptor.plugin.setMtime(mtime);
                    pluginWithDescriptor.plugin.setVersion(version);
                    pluginWithDescriptor.plugin.setMd5(md5);
                } else {
                    log.info("It appears that the server plugin [" + dbPlugin
                        + "] in the database may be obsolete. If so, it will be updated later.");
                }
            } else {
                log.info("Found server plugin in the DB that we do not yet have: " + name);
                ServerPlugin plugin = new ServerPlugin(name, path, md5);
                plugin.setType(pluginType.stringify());
                plugin.setMtime(mtime);
                plugin.setVersion(version);
                updatedPlugins.add(plugin);
                this.serverPluginsOnFilesystem.remove(expectedFile); // paranoia, make sure the cache doesn't have this
            }
        }

        // write all our updated plugins to the file system
        if (!updatedPlugins.isEmpty()) {
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                DataSource ds = LookupUtil.getDataSource();
                conn = ds.getConnection();

                ps = conn.prepareStatement("SELECT CONTENT FROM " + ServerPlugin.TABLE_NAME
                    + " WHERE DEPLOYMENT = 'SERVER' AND STATUS = 'INSTALLED' AND NAME = ? AND PTYPE = ?");
                for (ServerPlugin plugin : updatedPlugins) {
                    File file = new File(this.getServerPluginDir(), plugin.getPath());

                    ps.setString(1, plugin.getName());
                    ps.setString(2, plugin.getType());
                    rs = ps.executeQuery();
                    rs.next();
                    InputStream content = rs.getBinaryStream(1);
                    StreamUtil.copy(content, new FileOutputStream(file));
                    rs.close();
                    boolean succeeded = file.setLastModified(plugin.getMtime());// so our file matches the database mtime
                    if (!succeeded && log.isDebugEnabled()) {
                        log.debug("Failed to set mtime to [" + plugin.getMtime() + "] on file [" + file + "].");
                    }
                    updatedFiles.add(file);

                    // we are writing a new file to the filesystem, cache it since we know about it now
                    cacheFilesystemServerPluginJar(file, null);
                }
            } finally {
                JDBCUtil.safeClose(conn, ps, rs);
            }
        }

        return updatedFiles;
    }

    private class PluginWithDescriptor {
        public PluginWithDescriptor(ServerPlugin plugin, ServerPluginDescriptorType descriptor) {
            this.plugin = plugin;
            this.descriptor = descriptor;
            this.pluginType = new ServerPluginType(descriptor);
        }

        public final ServerPlugin plugin;
        public final ServerPluginDescriptorType descriptor;
        public final ServerPluginType pluginType;
    }
}
