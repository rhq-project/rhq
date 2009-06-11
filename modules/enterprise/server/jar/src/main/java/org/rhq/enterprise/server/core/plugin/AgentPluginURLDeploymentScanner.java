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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployment.scanner.URLDeploymentScanner;
import org.jboss.mx.util.ObjectNameFactory;

import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.util.MD5Generator;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is an extended URL deployment scanner so this can look at both the
 * file system and the database for new agent plugins.
 *
 * If an agent plugin is different in the database than on the filesystem,
 * this scanner will stream the plugin's content to the filesystem. This
 * allows for the normal file system scanning to occur (the normal file
 * system scanning processor will see the new plugin from the database
 * now in the file system and will process it normally, as if someone
 * hand-copied that plugin to the file system).
 *
 * So the job of this scanner is merely to look at the database and
 * reconcile the file system so the file system has the most up-to-date
 * plugins. Any old plugins will be deleted from the file system.
 *
 * @author John Mazzitelli
 */
public class AgentPluginURLDeploymentScanner extends URLDeploymentScanner {

    /**
     * The name of this MBean when deployed.
     */
    public static ObjectName OBJECT_NAME = ObjectNameFactory
        .create("rhq:service=AgentPluginURLDeploymentScanner,type=DeploymentScanner,flavor=URL");

    private Log log = LogFactory.getLog(AgentPluginURLDeploymentScanner.class);

    private DatabaseType dbType = null;

    /** The location where the user stores agent plugin files */
    private File pluginDirectory;

    /** Maintains a cache of what we had on the filesystem during the last scan */
    private Map<File, Plugin> pluginsOnFilesystem = new HashMap<File, Plugin>();

    /**
     * This will scan the database for new/updated plugins and if it finds
     * any, will write the content as plugin files in the file system. This
     * will also delete old, obsolete plugins.
     */
    @Override
    public synchronized void scan() throws Exception {
        scanFilesystem();
        scanDatabase();
        super.scan();
        return;
    }

    /**
     * This will check to see if there are any plugin records in the database
     * that do not have content associated with them and if so, will stream
     * the content from the file system to the database. This is needed only
     * in the case when this server has recently been upgraded from an old
     * version of the software that did not originally have content stored in the DB.
     */
    @Override
    public void startService() throws Exception {
        // the first URL in the scanner's list must be the agent plugin directory
        setPluginDirectory(new File(((URL) getURLList().get(0)).toURI()));

        fixMissingPluginContent();

        super.startService();
    }

    protected void setPluginDirectory(File pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
    }

    /**
     * Scans the plugin directory and updates our cache of known plugin files.
     * This will purge any old plugins that are deemed obsolete.
     */
    private void scanFilesystem() {
        // get the current list of plugins deployed on the filesystem
        File[] pluginJars = this.pluginDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        // refresh our cache so it reflects what is currently on the filesystem
        // first we remove any jar files in our cache that we longer have on the filesystem
        ArrayList<File> doomedPluginFiles = new ArrayList<File>();
        for (File cachedPluginFile : this.pluginsOnFilesystem.keySet()) {
            boolean existsOnFileSystem = false;
            for (File filesystemPluginFile : pluginJars) {
                if (cachedPluginFile.equals(filesystemPluginFile)) {
                    existsOnFileSystem = true;
                    continue; // our cached jar still exists on the file system
                }
            }
            if (!existsOnFileSystem) {
                doomedPluginFiles.add(cachedPluginFile); // this plugin file has been deleted from the filesystem, remove it from the cache
            }
        }
        for (File deletedPluginFile : doomedPluginFiles) {
            this.pluginsOnFilesystem.remove(deletedPluginFile);
        }

        // now insert new cache items representing new jar files and update existing ones as appropriate
        for (File pluginJar : pluginJars) {
            String md5 = null;

            Plugin plugin = this.pluginsOnFilesystem.get(pluginJar);
            try {
                if (plugin != null) {
                    if (pluginJar.lastModified() == 0L) {
                        // for some reason the operating system can't give us the last mod time, we need to do MD5 check
                        md5 = MD5Generator.getDigestString(pluginJar);
                        if (!md5.equals(plugin.getMd5())) {
                            plugin = null; // this plugin jar has changed - force it to refresh the cache.
                        }
                    } else if (pluginJar.lastModified() != plugin.getMtime()) {
                        plugin = null; // this plugin jar has changed - force it to refresh the cache.
                    }
                }

                if (plugin == null) {
                    cacheFilesystemPluginJar(pluginJar, md5);
                }
            } catch (Exception e) {
                log.warn("Failed to scan plugin [" + pluginJar + "] found on filesystem. Skipping it. Cause: " + e);
                this.pluginsOnFilesystem.remove(pluginJar); // act like we never saw it
            }
        }

        // Let's check to see if there are any obsolete plugins that need to be deleted.
        // This is needed if plugin-A-1.0.jar exists and someone deployed plugin-A-1.1.jar but fails to delete plugin-A-1.0.jar.
        doomedPluginFiles.clear();
        HashMap<String, Plugin> pluginsByName = new HashMap<String, Plugin>();
        for (Map.Entry<File, Plugin> currentPluginFileEntry : this.pluginsOnFilesystem.entrySet()) {
            Plugin currentPlugin = currentPluginFileEntry.getValue();
            Plugin existingPlugin = pluginsByName.get(currentPlugin.getName());
            if (existingPlugin == null) {
                // this is the usual case - this is the only plugin with the given name we've seen
                pluginsByName.put(currentPlugin.getName(), currentPlugin);
            } else {
                Plugin obsolete = AgentPluginDescriptorUtil.determineObsoletePlugin(currentPlugin, existingPlugin);
                if (obsolete == null) {
                    obsolete = currentPlugin; // both were identical, but we only want one file so pick one to get rid of
                }
                doomedPluginFiles.add(new File(this.pluginDirectory, obsolete.getPath()));
                if (obsolete == existingPlugin) { // yes use == for reference equality!
                    pluginsByName.put(currentPlugin.getName(), currentPlugin); // override the original one we saw with this latest one
                }
            }
        }

        // now we need to actually delete any obsolete plugin files from the file system
        for (File doomedPluginFile : doomedPluginFiles) {
            if (doomedPluginFile.delete()) {
                log.info("Deleted an obsolete plugin file: " + doomedPluginFile);
                this.pluginsOnFilesystem.remove(doomedPluginFile);
            } else {
                log.warn("Failed to delete what was deemed an obsolete plugin file: " + doomedPluginFile);
            }
        }

        return;
    }

    /**
     * Creates a {@link Plugin} object for the given plugin jar and caches it.
     * @param pluginJar information about this plugin jar will be cached
     * @param md5 if known, this is the plugin jar's MD5, <code>null</code> if not known
     * @return the plugin jar files's information that has been cached
     * @throws Exception if failed to get information about the plugin
     */
    private Plugin cacheFilesystemPluginJar(File pluginJar, String md5) throws Exception {
        if (md5 == null) { // don't calculate the MD5 is we've already done it before
            md5 = MD5Generator.getDigestString(pluginJar);
        }
        URL pluginUrl = pluginJar.toURI().toURL();
        PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginUrl);
        String version = AgentPluginDescriptorUtil.getPluginVersion(pluginJar, descriptor).toString();
        String name = descriptor.getName();
        Plugin plugin = new Plugin(name, pluginJar.getName());
        plugin.setMd5(md5);
        plugin.setVersion(version);
        plugin.setMtime(pluginJar.lastModified());
        this.pluginsOnFilesystem.put(pluginJar, plugin);
        return plugin;
    }

    /**
     * This method scans the database for any new or updated agent plugins and make sure this server
     * has a plugin file on the filesystem for each of those new/updated agent plugins.
     */
    private void scanDatabase() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        // these are plugins (name/path/md5/mtime) that have changed in the DB but are missing from the file system
        List<Plugin> updatedPlugins = new ArrayList<Plugin>();

        try {
            DataSource ds = LookupUtil.getDataSource();
            conn = ds.getConnection();

            // get all the plugins
            ps = conn.prepareStatement("SELECT NAME, PATH, MD5, MTIME, VERSION FROM " + Plugin.TABLE_NAME
                + " WHERE ENABLED=?");
            setEnabledFlag(conn, ps, 1, true);
            rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String path = rs.getString(2);
                String md5 = rs.getString(3);
                long mtime = rs.getLong(4);
                String version = rs.getString(5);

                // let's see if we have this logical plugin on the filesystem (it may or may not be under the same filename)
                File expectedFile = new File(this.pluginDirectory, path);
                File currentFile = null; // will be non-null if we find that we have this plugin on the filesystem already
                Plugin cachedPluginOnFilesystem = this.pluginsOnFilesystem.get(expectedFile);

                if (cachedPluginOnFilesystem != null) {
                    currentFile = expectedFile; // we have it where we are expected to have it
                    if (!cachedPluginOnFilesystem.getName().equals(name)) {
                        // I have no idea when or if this would ever happen, but at least log it so we'll see it if it does happen
                        log.warn("For some reason, the plugin file [" + expectedFile + "] is plugin ["
                            + cachedPluginOnFilesystem.getName() + "] but the database says it should be [" + name
                            + "]");
                    } else {
                        log.debug("File system and database agree on a plugin location for [" + expectedFile + "]");
                    }
                } else {
                    // the plugin might still be on the file system but under a different filename, see if we can find it
                    for (Map.Entry<File, Plugin> cachePluginEntry : this.pluginsOnFilesystem.entrySet()) {
                        if (cachePluginEntry.getValue().getName().equals(name)) {
                            currentFile = cachePluginEntry.getKey();
                            cachedPluginOnFilesystem = cachePluginEntry.getValue();
                            log.info("Filesystem has a plugin [" + name + "] at the file [" + currentFile
                                + "] which is different than where the DB thinks it should be [" + expectedFile + "]");
                            break; // we found it, no need to continue the loop
                        }
                    }
                }

                if (cachedPluginOnFilesystem != null && currentFile != null && currentFile.exists()) {
                    Plugin dbPlugin = new Plugin(name, path);
                    dbPlugin.setMd5(md5);
                    dbPlugin.setVersion(version);
                    dbPlugin.setMtime(mtime);

                    Plugin obsoletePlugin = AgentPluginDescriptorUtil.determineObsoletePlugin(dbPlugin,
                        cachedPluginOnFilesystem);

                    if (obsoletePlugin == cachedPluginOnFilesystem) { // yes use == for reference equality!
                        StringBuilder logMsg = new StringBuilder();
                        logMsg.append("Found agent plugin [").append(name);
                        logMsg.append("] in the DB that is newer than the one on the filesystem: ");
                        logMsg.append("DB path=[").append(path);
                        logMsg.append("]; file path=[").append(currentFile.getName());
                        logMsg.append("]; DB MD5=[").append(md5);
                        logMsg.append("]; file MD5=[").append(cachedPluginOnFilesystem.getMd5());
                        logMsg.append("]; DB version=[").append(version);
                        logMsg.append("]; file version=[").append(cachedPluginOnFilesystem.getVersion());
                        logMsg.append("]; DB timestamp=[").append(new Date(mtime));
                        logMsg.append("]; file timestamp=[").append(new Date(cachedPluginOnFilesystem.getMtime()));
                        logMsg.append("]");
                        log.info(logMsg.toString());

                        updatedPlugins.add(dbPlugin);

                        if (currentFile.delete()) {
                            log.info("Deleted the obsolete plugin file to be updated: " + currentFile);
                            this.pluginsOnFilesystem.remove(currentFile);
                        } else {
                            log.warn("Failed to delete the obsolete (to-be-updated) plugin file: " + currentFile);
                        }
                        currentFile = null;
                    } else if (obsoletePlugin == null) {
                        // the db is up-to-date, but update the cache so we don't check MD5 or parse the descriptor again
                        currentFile.setLastModified(mtime);
                        cachedPluginOnFilesystem.setMtime(mtime);
                        cachedPluginOnFilesystem.setVersion(version);
                        cachedPluginOnFilesystem.setMd5(md5);
                    } else {
                        log.debug("It appears that the plugin [" + dbPlugin
                            + "] in the database may be obsolete. If so, it will be updated by the plugin deployer.");
                    }
                } else {
                    log.info("Found agent plugin in the DB that we do not yet have: " + name);
                    Plugin plugin = new Plugin(name, path, md5);
                    plugin.setMtime(mtime);
                    plugin.setVersion(version);
                    updatedPlugins.add(plugin);
                    this.pluginsOnFilesystem.remove(expectedFile); // paranoia, make sure the cache doesn't have this
                }
            }
            JDBCUtil.safeClose(ps, rs);

            // write all our updated plugins to the file system
            ps = conn.prepareStatement("SELECT CONTENT FROM " + Plugin.TABLE_NAME + " WHERE NAME = ? AND ENABLED = ?");
            for (Plugin plugin : updatedPlugins) {
                File file = new File(this.pluginDirectory, plugin.getPath());

                ps.setString(1, plugin.getName());
                setEnabledFlag(conn, ps, 2, true);
                rs = ps.executeQuery();
                rs.next();
                InputStream content = rs.getBinaryStream(1);
                StreamUtil.copy(content, new FileOutputStream(file));
                rs.close();
                file.setLastModified(plugin.getMtime()); // so our file matches the database mtime
            }
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);
        }

        return;
    }

    /**
     * This method will stream up plugin content if the server has a plugin file
     * but there is null content in the database (only occurs when upgrading an old server to the new
     * schema that supports database-storage for plugins). This method will be a no-op for
     * recent versions of the server because the database will no longer have null content from now on.
     */
    private void fixMissingPluginContent() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        // This map contains the names/paths of plugins that are missing their content in the database.
        // This map will only have entries if this server was recently upgraded from an older version
        // that did not support database-stored plugin content.
        Map<String, String> pluginsMissingContentInDb = new HashMap<String, String>();

        // This map contains the names/MD5s of plugins that are missing their content in the database.
        // This map will only have entries if this server was recently upgraded from an older version
        // that did not support database-stored plugin content.
        Map<String, String> pluginsMissingContentInDbMD5 = new HashMap<String, String>();

        try {
            DataSource ds = LookupUtil.getDataSource();
            conn = ds.getConnection();
            ps = conn.prepareStatement("SELECT NAME, PATH, MD5 FROM " + Plugin.TABLE_NAME
                + " WHERE CONTENT IS NULL AND ENABLED = ?");
            setEnabledFlag(conn, ps, 1, true);
            rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String path = rs.getString(2);
                String md5 = rs.getString(3);
                pluginsMissingContentInDb.put(name, path);
                pluginsMissingContentInDbMD5.put(name, md5);
            }
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);
        }

        if (!pluginsMissingContentInDb.isEmpty()) {
            // if a plugin used to exist but now doesn't, it should be deleted - we should not fail when this occurs
            List<String> pluginsToDelete = new ArrayList<String>();

            // in all likelihood, the new plugins have different filenames; but since the descriptors
            // will have the same plugin names, we'll be able to key off of plugin name
            PluginDescriptor descriptor;
            Map<String, File> existingPluginFiles = new HashMap<String, File>(); // keyed on plugin name
            for (File file : this.pluginDirectory.listFiles()) {
                if (file.getName().endsWith(".jar")) {
                    try {
                        descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(file.toURI().toURL());
                        existingPluginFiles.put(descriptor.getName(), file);
                    } catch (Exception e) {
                        log.warn("File [" + file + "] is not a valid plugin and will be ignored: " + e);
                    }
                }
            }

            // now let's take the new content and stream it into the DB
            for (Map.Entry<String, String> entry : pluginsMissingContentInDb.entrySet()) {
                String name = entry.getKey();
                String path = entry.getValue();
                String expectedMD5 = pluginsMissingContentInDbMD5.get(name);
                File pluginFile = existingPluginFiles.get(name);
                if (pluginFile != null) {
                    String newMD5 = MD5Generator.getDigestString(pluginFile);
                    boolean different = !expectedMD5.equals(newMD5);
                    streamPluginFileContentToDatabase(name, pluginFile, different);
                    log.info("Missing content for plugin [" + name + "] will be uploaded from [" + pluginFile
                        + "]. different=" + different);
                } else {
                    pluginsToDelete.add(name);
                    log.warn("The database knows of a plugin named [" + name + "] with path [" + path
                        + "] but the content is missing. This server does not have this plugin in ["
                        + this.pluginDirectory + "] so the database cannot be updated with the content."
                        + " This plugin must be installed to manage existing inventory for its resource types.");
                }
            }

            if (!pluginsToDelete.isEmpty()) {
                TransactionManager tm = LookupUtil.getTransactionManager();
                for (String pluginName : pluginsToDelete) {
                    try {
                        tm.begin();
                        DataSource ds = LookupUtil.getDataSource();
                        conn = ds.getConnection();
                        ps = conn.prepareStatement("UPDATE " + Plugin.TABLE_NAME + " SET ENABLED = ? WHERE NAME = ?");
                        setEnabledFlag(conn, ps, 1, false);
                        ps.setString(2, pluginName);
                        int updateResults = ps.executeUpdate();
                        if (updateResults == 1) {
                            log.warn("Disabled unavailable plugin [" + pluginName
                                + "] - This plugin must be provided to manage committed resources for its types."
                                + " Uninventory obsolete resources to avoid getting warnings in the server and agent.");
                        } else {
                            // TODO: should we throw an exception or is continuing the right thing to do?
                            log.error("Failed to disable unavailable plugin [" + pluginName + "].");
                        }
                    } catch (Exception e) {
                        tm.rollback();
                        tm = null;
                        throw e;
                    } finally {
                        JDBCUtil.safeClose(conn, ps, null);
                        if (tm != null) {
                            tm.commit();
                        }
                    }
                }
            }
        }

        return;
    }

    private void setEnabledFlag(Connection conn, PreparedStatement ps, int index, boolean enabled) throws Exception {
        if (null == this.dbType) {
            this.dbType = DatabaseTypeFactory.getDatabaseType(conn);
        }
        if (dbType instanceof PostgresqlDatabaseType || dbType instanceof H2DatabaseType) {
            ps.setBoolean(index, enabled);
        } else if (dbType instanceof OracleDatabaseType || dbType instanceof SQLServerDatabaseType) {
            ps.setInt(index, (enabled ? 1 : 0));
        } else {
            throw new RuntimeException("Unknown database type : " + dbType);
        }
    }

    /**
     * This will write the contents of the given plugin file to the database.
     * This will store both the contents and the MD5 in an atomic transaction
     * so they remain insync.
     *
     * When <code>different</code> is <code>false</code>, it means the original
     * plugin and the one currently found on the file system are the same.
     *
     * When <code>different</code> is <code>true</code>, it means the plugin
     * is most likely a different one than the one that originally existed.
     * When this happens, it is assumed that the {@link ProductPluginDeployer} needs
     * to see the plugin on the file system as new and needing to be processed, therefore
     * the MD5, CONTENT and MTIME columns will be updated to ensure the deployer
     * will process this plugin and thus update all the metadata for this plugin.
     *
     * @param name the name of the plugin whose content is being updated
     * @param file the plugin file whose content will be streamed to the database
     * @param different this will be <code>true</code> if the given file has a different filename
     *                  that the plugin's "path" as found in the database.
     *
     *
     * @throws Exception
     */
    private void streamPluginFileContentToDatabase(String name, File file, boolean different) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        TransactionManager tm = null;

        String sql = "UPDATE " + Plugin.TABLE_NAME + " SET CONTENT = ?, MD5 = ?, MTIME = ?, PATH = ? WHERE NAME = ?";

        // if 'different' is true, give bogus data so the plugin deployer will think the plugin on the file system is new
        String md5 = (!different) ? MD5Generator.getDigestString(file) : "TO BE UPDATED";
        long mtime = (!different) ? file.lastModified() : 0L;
        InputStream fis = (!different) ? new FileInputStream(file) : new ByteArrayInputStream(new byte[0]);
        int contentSize = (int) ((!different) ? file.length() : 0);

        try {
            tm = LookupUtil.getTransactionManager();
            tm.begin();
            DataSource ds = LookupUtil.getDataSource();
            conn = ds.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setBinaryStream(1, new BufferedInputStream(fis), contentSize);
            ps.setString(2, md5);
            ps.setLong(3, mtime);
            ps.setString(4, file.getName());
            ps.setString(5, name);
            int updateResults = ps.executeUpdate();
            if (updateResults == 1) {
                log.debug("Stored content for plugin [" + name + "] in the db. file=" + file);
            } else {
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
}
