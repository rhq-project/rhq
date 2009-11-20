/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * A server API into the server plugin infrastructure.
 * 
 * @author John Mazzitelli
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class ServerPluginsBean implements ServerPluginsLocal {
    private final Log log = LogFactory.getLog(ServerPluginsBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource dataSource;

    @EJB
    private ServerPluginsLocal serverPluginsBean; //self

    public List<Plugin> getServerPlugins() {
        Query q = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_SERVER);
        return q.getResultList();
    }

    public Plugin getServerPlugin(String name) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
        query.setParameter("name", name);
        Plugin plugin = (Plugin) query.getSingleResult();
        if (plugin.getDeployment() != PluginDeploymentType.SERVER) {
            throw new IllegalArgumentException("Plugin named [" + name + "] is not a server plugin");
        }
        return plugin;
    }

    public Plugin getServerPluginRelationships(Plugin plugin) {
        plugin = entityManager.find(Plugin.class, plugin.getId());

        // force eager load
        plugin.getPluginConfiguration().getProperties().size();
        plugin.getScheduledJobsConfiguration().getProperties().size();

        return plugin;
    }

    public List<Plugin> getServerPluginsById(List<Integer> pluginIds) {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<Plugin>(); // nothing to do
        }
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_IDS_AND_TYPE);
        query.setParameter("ids", pluginIds);
        query.setParameter("type", PluginDeploymentType.SERVER);
        return query.getResultList();
    }

    public ServerPluginDescriptorType getServerPluginDescriptor(String pluginName) throws Exception {
        Plugin plugin = getServerPlugin(pluginName);
        File pluginsDir = LookupUtil.getServerPluginService().getServerPluginsDirectory();
        File pluginJar = new File(pluginsDir, plugin.getPath());
        URL url = pluginJar.toURI().toURL();
        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
        return descriptor;
    }

    public List<String> getPluginNamesByEnabled(boolean enabled) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_GET_NAMES_BY_ENABLED_AND_TYPE);
        query.setParameter("enabled", Boolean.valueOf(enabled));
        query.setParameter("type", PluginDeploymentType.SERVER);
        return query.getResultList();
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void enableServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }

        serverPluginsBean.setPluginEnabledFlag(subject, pluginIds, true);

        // only restart the master if it was started to begin with
        ServerPluginServiceManagement serverPluginService = LookupUtil.getServerPluginService();
        if (serverPluginService.isMasterPluginContainerStarted()) {
            serverPluginService.restartMasterPluginContainer();
        }
        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public List<Plugin> disableServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<Plugin>(); // nothing to do
        }

        serverPluginsBean.setPluginEnabledFlag(subject, pluginIds, false);

        ServerPluginServiceManagement serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();

        List<Plugin> doomedPlugins = new ArrayList<Plugin>();

        for (Integer pluginId : pluginIds) {
            Plugin doomedPlugin = entityManager.find(Plugin.class, pluginId);
            if (doomedPlugin != null) {
                doomedPlugins.add(doomedPlugin);
                if (master != null) {
                    AbstractTypeServerPluginContainer pc = master.getPluginContainer(doomedPlugin.getName());
                    if (pc != null) {
                        try {
                            pc.unschedulePluginJobs(doomedPlugin.getName());
                        } catch (Exception e) {
                            log.warn("Failed to unschedule jobs for plugin [" + doomedPlugin.getName() + "]", e);
                        }
                    }
                }
            }
        }

        // only restart if the master was started to begin with; otherwise, leave it down
        if (master != null) {
            serverPluginService.restartMasterPluginContainer();
        }

        return doomedPlugins;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public List<Plugin> undeployServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<Plugin>(); // nothing to do
        }

        serverPluginsBean.setPluginEnabledFlag(subject, pluginIds, false);

        ServerPluginServiceManagement serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();

        List<Plugin> doomedPlugins = new ArrayList<Plugin>();

        for (Integer pluginId : pluginIds) {
            Plugin doomedPlugin = entityManager.find(Plugin.class, pluginId);
            if (doomedPlugin != null) {
                doomedPlugins.add(doomedPlugin);
                if (master != null) {
                    AbstractTypeServerPluginContainer pc = master.getPluginContainer(doomedPlugin.getName());
                    if (pc != null) {
                        try {
                            pc.unschedulePluginJobs(doomedPlugin.getName());
                        } catch (Exception e) {
                            log.warn("Failed to unschedule jobs for plugin [" + doomedPlugin.getName() + "]", e);
                        }
                    }
                }

                // if this plugin ever gets re-installed, let's support the use-case where the new plugin
                // will have different config metadata. Here we null out the old config so the new
                // config can be set to the new cofig definition's default values.
                doomedPlugin.setPluginConfiguration(null);
                doomedPlugin.setScheduledJobsConfiguration(null);
                doomedPlugin.setStatus(PluginStatusType.DELETED);

                try {
                    File pluginDir = serverPluginService.getServerPluginsDirectory();
                    File currentFile = new File(pluginDir, doomedPlugin.getPath());
                    currentFile.delete();
                } catch (Exception e) {
                    log.error("Failed to delete the undeployed plugin [" + doomedPlugin.getPath() + "]. Cause: "
                        + ThrowableUtil.getAllMessages(e));
                }
            }
        }

        // only restart if the master was started to begin with; otherwise, leave it down
        if (master != null) {
            serverPluginService.restartMasterPluginContainer();
        }

        return doomedPlugins;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setPluginEnabledFlag(Subject subject, List<Integer> pluginIds, boolean enabled) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }
        Query q = entityManager.createNamedQuery(Plugin.UPDATE_PLUGINS_ENABLED_BY_IDS);
        q.setParameter("ids", pluginIds);
        q.setParameter("enabled", Boolean.valueOf(enabled));
        int count = q.executeUpdate();
        if (count != pluginIds.size()) {
            throw new Exception("Failed to update [" + pluginIds.size() + "] plugins. Count was [" + count + "]");
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setPluginStatus(Subject subject, List<Integer> pluginIds, PluginStatusType status) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }
        List<Plugin> plugins = getServerPluginsById(pluginIds);
        for (Plugin plugin : plugins) {
            plugin.setStatus(status);
            updatePluginExceptContent(subject, plugin);
        }
        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public Plugin registerPlugin(Subject subject, Plugin plugin, File pluginFile) throws Exception {

        if (plugin.getDeployment() != PluginDeploymentType.SERVER) {
            throw new IllegalArgumentException("Plugin [" + plugin.getName()
                + "] must be a server plugin to be registered");
        }

        Plugin existingPlugin = null;
        boolean newOrUpdated = false;
        try {
            existingPlugin = getServerPlugin(plugin.getName());
        } catch (NoResultException nre) {
            newOrUpdated = true; // this is expected for new plugins
        }

        if (existingPlugin != null) {
            if (existingPlugin.getStatus() == PluginStatusType.DELETED) {
                throw new IllegalArgumentException("Cannot register plugin [" + plugin.getName()
                    + "], it has been marked as deleted");
            }
            Plugin obsolete = ServerPluginDescriptorUtil.determineObsoletePlugin(plugin, existingPlugin);
            if (obsolete == existingPlugin) { // yes use == for reference equality
                newOrUpdated = true;
            }
            plugin.setId(existingPlugin.getId());
        }

        // If this is a brand new plugin, it gets "updated" too - which ends up being a simple persist.
        if (newOrUpdated) {
            if (plugin.getDisplayName() == null) {
                plugin.setDisplayName(plugin.getName());
            }

            if (plugin.getId() == 0) {
                entityManager.persist(plugin);
            } else {
                plugin = updatePluginExceptContent(subject, plugin);
            }

            if (pluginFile != null) {
                entityManager.flush();
                streamPluginFileContentToDatabase(plugin.getId(), pluginFile);
            }
            log.debug("Updated plugin entity [" + plugin + "]");
        }

        return plugin;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public Plugin updatePluginExceptContent(Subject subject, Plugin plugin) throws Exception {
        // this method is here because we need a way to update the plugin's information
        // without blowing away the content data. Because we do not want to load the
        // content blob in memory, the plugin's content field will be null - if we were
        // to entityManager.merge that plugin POJO, it would null out that blob column.
        if (plugin.getId() == 0) {
            throw new IllegalArgumentException("Plugin must already exist to update it");
        } else {
            Query q = entityManager.createNamedQuery(Plugin.UPDATE_ALL_BUT_CONTENT);
            q.setParameter("id", plugin.getId());
            q.setParameter("name", plugin.getName());
            q.setParameter("path", plugin.getPath());
            q.setParameter("displayName", plugin.getDisplayName());
            q.setParameter("enabled", plugin.isEnabled());
            q.setParameter("status", plugin.getStatus());
            q.setParameter("md5", plugin.getMD5());
            q.setParameter("version", plugin.getVersion());
            q.setParameter("ampsVersion", plugin.getAmpsVersion());
            q.setParameter("deployment", plugin.getDeployment());
            q.setParameter("pluginConfiguration", plugin.getPluginConfiguration());
            q.setParameter("scheduledJobsConfiguration", plugin.getScheduledJobsConfiguration());
            q.setParameter("description", plugin.getDescription());
            q.setParameter("help", plugin.getHelp());
            q.setParameter("mtime", plugin.getMtime());
            if (q.executeUpdate() != 1) {
                throw new Exception("Failed to update a plugin that matches [" + plugin + "]");
            }
        }
        return plugin;
    }

    /**
     * This will write the contents of the given plugin file to the database.
     * This will assume the MD5 in the database is already correct, so this
     * method will not take the time to calculate the MD5 again.
     *
     * @param id the ID of the plugin whose content is being updated
     * @param file the plugin file whose content will be streamed to the database
     *
     * @throws Exception
     */
    private void streamPluginFileContentToDatabase(int id, File file) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        FileInputStream fis = new FileInputStream(file);

        try {

            conn = this.dataSource.getConnection();
            ps = conn.prepareStatement("UPDATE " + Plugin.TABLE_NAME + " SET CONTENT = ? WHERE ID = ?");
            ps.setBinaryStream(1, new BufferedInputStream(fis), (int) file.length());
            ps.setInt(2, id);
            int updateResults = ps.executeUpdate();
            if (updateResults != 1) {
                throw new Exception("Failed to update content for plugin [" + id + "] from [" + file + "]");
            }
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);

            try {
                fis.close();
            } catch (Throwable t) {
            }
        }
        return;
    }
}
