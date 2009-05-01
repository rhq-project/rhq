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
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
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
 * scanner will then do the normal file system scanning (and will therefore
 * see the new plugin from the database now in the file system and will
 * process it normally, as if someone hand-copied that plugin to the
 * file system).
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

    /**
     * This will first scan the database for new/updated plugins and if it finds
     * any, will write the content as plugin files in the file system. After that,
     * this will tell the superclass to perform the normal file system scanning.
     */
    @Override
    public synchronized void scan() throws Exception {
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
        this.pluginDirectory = new File(((URL) getURLList().get(0)).toURI());

        fixMissingPluginContent();

        super.startService();
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
            ps = conn.prepareStatement("SELECT NAME, PATH, MD5, MTIME FROM " + Plugin.TABLE_NAME + " WHERE ENABLED=?");
            setEnabledFlag(conn, ps, 1, true);
            rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String path = rs.getString(2);
                String md5 = rs.getString(3);
                long mtime = rs.getLong(4);
                File file = new File(this.pluginDirectory, path);
                if (file.exists()) {
                    long fileMtime = file.lastModified();
                    if (fileMtime < mtime) {
                        String fileMd5 = MD5Generator.getDigestString(file);
                        if (!fileMd5.equals(md5)) {
                            log.info("Found agent plugin in the DB that is newer than the one we have [" + name
                                + "]. DB timestamp=[" + new Date(mtime) + "]; file timestamp=[" + new Date(fileMtime)
                                + "]");
                            Plugin plugin = new Plugin(name, path, md5);
                            plugin.setMtime(mtime);
                            updatedPlugins.add(plugin);
                        } else {
                            file.setLastModified(mtime); // its up-to-date, set the file mtime so we don't check MD5 again
                        }
                    }
                } else {
                    log.info("Found agent plugin in the DB that we do not yet have: " + name);
                    Plugin plugin = new Plugin(name, path, md5);
                    plugin.setMtime(mtime);
                    updatedPlugins.add(plugin);
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
        if (dbType instanceof PostgresqlDatabaseType) {
            ps.setBoolean(index, enabled);
        } else if (dbType instanceof OracleDatabaseType) {
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
