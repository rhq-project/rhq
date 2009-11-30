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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
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

    public List<ServerPlugin> getServerPlugins() {
        Query q = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_ALL_INSTALLED);
        return q.getResultList();
    }

    public ServerPlugin getServerPlugin(PluginKey key) {
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_BY_NAME);
        query.setParameter("name", key.getPluginName());
        ServerPlugin plugin = (ServerPlugin) query.getSingleResult();
        return plugin;
    }

    public ServerPlugin getServerPluginRelationships(ServerPlugin plugin) {
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_BY_NAME);
        query.setParameter("name", plugin.getName());
        plugin = (ServerPlugin) query.getSingleResult();

        Configuration config = plugin.getPluginConfiguration();
        if (config != null) {
            config = entityManager.find(Configuration.class, config.getId());
            plugin.setPluginConfiguration(config.deepCopy());
        }

        config = plugin.getScheduledJobsConfiguration();
        if (config != null) {
            config = entityManager.find(Configuration.class, config.getId());
            plugin.setScheduledJobsConfiguration(config.deepCopy());
        }

        return plugin;
    }

    public List<ServerPlugin> getServerPluginsById(List<Integer> pluginIds) {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<ServerPlugin>(); // nothing to do
        }
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_BY_IDS);
        query.setParameter("ids", pluginIds);
        return query.getResultList();
    }

    public ServerPluginDescriptorType getServerPluginDescriptor(PluginKey pluginKey) throws Exception {
        ServerPlugin plugin = getServerPlugin(pluginKey);
        File pluginsDir = LookupUtil.getServerPluginService().getServerPluginsDirectory();
        File pluginJar = new File(pluginsDir, plugin.getPath());
        URL url = pluginJar.toURI().toURL();
        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
        return descriptor;
    }

    public List<PluginKey> getServerPluginKeysByEnabled(boolean enabled) {
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_GET_NAMES_BY_ENABLED);
        query.setParameter("enabled", Boolean.valueOf(enabled));
        return query.getResultList();
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void enableServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }

        serverPluginsBean.setServerPluginEnabledFlag(subject, pluginIds, true);

        // only restart the master if it was started to begin with
        ServerPluginServiceManagement serverPluginService = LookupUtil.getServerPluginService();
        if (serverPluginService.isMasterPluginContainerStarted()) {
            serverPluginService.restartMasterPluginContainer();
        }
        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public List<PluginKey> disableServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<PluginKey>(); // nothing to do
        }

        serverPluginsBean.setServerPluginEnabledFlag(subject, pluginIds, false);

        ServerPluginServiceManagement serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();

        List<PluginKey> doomedPlugins = new ArrayList<PluginKey>();

        for (Integer pluginId : pluginIds) {
            ServerPlugin doomedPlugin = null;
            try {
                doomedPlugin = entityManager.getReference(ServerPlugin.class, pluginId);
            } catch (Exception ignore) {
            }
            if (doomedPlugin != null) {
                PluginKey pluginKey = new PluginKey(doomedPlugin);
                doomedPlugins.add(pluginKey);
                if (master != null) {
                    AbstractTypeServerPluginContainer pc = master.getPluginContainerByPlugin(pluginKey);
                    if (pc != null) {
                        try {
                            pc.unschedulePluginJobs(pluginKey);
                        } catch (Exception e) {
                            log.warn("Failed to unschedule jobs for plugin [" + pluginKey + "]", e);
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
    public List<PluginKey> undeployServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<PluginKey>(); // nothing to do
        }

        serverPluginsBean.setServerPluginEnabledFlag(subject, pluginIds, false);

        ServerPluginServiceManagement serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();

        List<PluginKey> doomedPlugins = new ArrayList<PluginKey>();

        for (Integer pluginId : pluginIds) {
            ServerPlugin doomedPlugin = null;
            try {
                doomedPlugin = entityManager.getReference(ServerPlugin.class, pluginId);
            } catch (Exception ignore) {
            }
            if (doomedPlugin != null) {
                PluginKey pluginKey = new PluginKey(doomedPlugin);
                doomedPlugins.add(pluginKey);
                if (master != null) {
                    AbstractTypeServerPluginContainer pc = master.getPluginContainerByPlugin(pluginKey);
                    if (pc != null) {
                        try {
                            pc.unschedulePluginJobs(pluginKey);
                        } catch (Exception e) {
                            log.warn("Failed to unschedule jobs for plugin [" + pluginKey + "]", e);
                        }
                    }
                }

                // if this plugin ever gets re-installed, let's support the use-case where the new plugin
                // will have different config metadata. Here we null out the old config so the new
                // config can be set to the new cofig definition's default values.
                if (doomedPlugin.getPluginConfiguration() != null) {
                    entityManager.remove(doomedPlugin.getPluginConfiguration());
                    doomedPlugin.setPluginConfiguration(null);
                }
                if (doomedPlugin.getScheduledJobsConfiguration() != null) {
                    entityManager.remove(doomedPlugin.getScheduledJobsConfiguration());
                    doomedPlugin.setScheduledJobsConfiguration(null);
                }
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

        log.info("Server plugins " + doomedPlugins + " have been undeployed");

        // only restart if the master was started to begin with; otherwise, leave it down
        if (master != null) {
            serverPluginService.restartMasterPluginContainer();
        }

        return doomedPlugins;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setServerPluginEnabledFlag(Subject subject, List<Integer> pluginIds, boolean enabled) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }
        Query q = entityManager.createNamedQuery(ServerPlugin.UPDATE_PLUGINS_ENABLED_BY_IDS);
        q.setParameter("ids", pluginIds);
        q.setParameter("enabled", Boolean.valueOf(enabled));
        int count = q.executeUpdate();
        if (count != pluginIds.size()) {
            throw new Exception("Failed to update [" + pluginIds.size() + "] plugins. Count was [" + count + "]");
        }

        log.info((enabled ? "Enabling" : "Disabling") + " server plugins with plugin IDs of " + pluginIds);
        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setServerPluginStatus(Subject subject, List<Integer> pluginIds, PluginStatusType status)
        throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }
        List<ServerPlugin> plugins = getServerPluginsById(pluginIds);
        for (ServerPlugin plugin : plugins) {
            plugin.setStatus(status);
            updateServerPluginExceptContent(subject, plugin);
        }
        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public ServerPlugin registerServerPlugin(Subject subject, ServerPlugin plugin, File pluginFile) throws Exception {

        if (plugin.getDeployment() != PluginDeploymentType.SERVER) {
            throw new IllegalArgumentException("Plugin [" + plugin.getName()
                + "] must be a server plugin to be registered");
        }

        ServerPlugin existingPlugin = null;
        boolean newOrUpdated = false;
        try {
            existingPlugin = getServerPlugin(new PluginKey(plugin));
        } catch (NoResultException nre) {
            newOrUpdated = true; // this is expected for new plugins
        }

        if (existingPlugin != null) {
            if (existingPlugin.getStatus() == PluginStatusType.DELETED) {
                throw new IllegalArgumentException("Cannot register plugin [" + plugin.getName()
                    + "], it has been marked as deleted");
            }
            ServerPlugin obsolete = ServerPluginDescriptorUtil.determineObsoletePlugin(plugin, existingPlugin);
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
                PluginStatusType status = getServerPluginStatus(new PluginKey(plugin));
                if (PluginStatusType.DELETED == status) {
                    throw new IllegalArgumentException("Cannot register plugin [" + plugin.getName()
                        + "], it has been previously marked as deleted.");
                }
                entityManager.persist(plugin);
            } else {
                plugin = updateServerPluginExceptContent(subject, plugin);
            }

            if (pluginFile != null) {
                entityManager.flush();
                streamPluginFileContentToDatabase(plugin.getId(), pluginFile);
            }
            log.info("Server plugin [" + plugin.getName() + "] has been registered in the database");
        }

        return plugin;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void purgeServerPlugin(Subject subject, PluginKey pluginKey) {
        Query q = this.entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_ANY_BY_NAME);
        q.setParameter("name", pluginKey.getPluginName());
        ServerPlugin doomed = (ServerPlugin) q.getSingleResult();

        // get the reference to attach to em and use the em.remove. this cascade deletes too.
        doomed = this.entityManager.getReference(ServerPlugin.class, doomed.getId());
        this.entityManager.remove(doomed);

        log.debug("Server plugin [" + pluginKey + "] has been purged from the db");
        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public ServerPlugin updateServerPluginExceptContent(Subject subject, ServerPlugin plugin) throws Exception {

        // this method is here because we need a way to update the plugin's information
        // without blowing away the content data. Because we do not want to load the
        // content blob in memory, the plugin's content field will be null - if we were
        // to entityManager.merge that plugin POJO, it would null out that blob column.

        if (plugin.getId() == 0) {
            throw new IllegalArgumentException("Plugin must already exist to update it");
        } else {
            // make sure we create (if necessary) and attach the configs
            Configuration config = plugin.getPluginConfiguration();
            if (config != null) {
                config = entityManager.merge(config);
                plugin.setPluginConfiguration(config);
            }
            config = plugin.getScheduledJobsConfiguration();
            if (config != null) {
                config = entityManager.merge(config);
                plugin.setScheduledJobsConfiguration(config);
            }

            ServerPlugin pluginEntity = entityManager.getReference(ServerPlugin.class, plugin.getId());
            pluginEntity.setName(plugin.getName());
            pluginEntity.setPath(plugin.getPath());
            pluginEntity.setDisplayName(plugin.getDisplayName());
            pluginEntity.setEnabled(plugin.isEnabled());
            pluginEntity.setStatus(plugin.getStatus());
            pluginEntity.setMd5(plugin.getMD5());
            pluginEntity.setVersion(plugin.getVersion());
            pluginEntity.setAmpsVersion(plugin.getAmpsVersion());
            pluginEntity.setDeployment(plugin.getDeployment());
            pluginEntity.setPluginConfiguration(plugin.getPluginConfiguration());
            pluginEntity.setScheduledJobsConfiguration(plugin.getScheduledJobsConfiguration());
            pluginEntity.setType(plugin.getType());
            pluginEntity.setDescription(plugin.getDescription());
            pluginEntity.setHelp(plugin.getHelp());
            pluginEntity.setMtime(plugin.getMtime());

            try {
                entityManager.flush(); // make sure we push this out to the DB now
            } catch (Exception e) {
                throw new Exception("Failed to update a plugin that matches [" + plugin + "]", e);
            }
        }
        return plugin;
    }

    public PluginStatusType getServerPluginStatus(PluginKey pluginKey) {
        Query q = entityManager.createNamedQuery(ServerPlugin.QUERY_GET_STATUS_BY_NAME);
        q.setParameter("name", pluginKey.getPluginName());
        PluginStatusType status;
        try {
            status = (PluginStatusType) q.getSingleResult();
        } catch (NoResultException nre) {
            status = null; // doesn't exist in the DB, tell the caller this by returning null
        }
        return status;
    }

    // we need this method to talk to the master plugin container because the plugin data model
    // does not contain information as to what type a plugin is. It only has information about
    // where the plugin is deployed (agent or server) but it doesn't indicate if a plugin
    // if of type "alert plugin" or "generic plugin". That is only known when the plugin descriptor
    // is parsed, which happens when the master plugin container is initialized. Therefore, this
    // needs to get plugin info from the master plugin container while the master is running.
    public Map<ServerPluginType, List<PluginKey>> getAllPluginsGroupedByType() {
        Map<ServerPluginType, List<PluginKey>> allPlugins = new HashMap<ServerPluginType, List<PluginKey>>();

        MasterServerPluginContainer master = LookupUtil.getServerPluginService().getMasterPluginContainer();
        if (master != null) {
            List<ServerPluginType> types = master.getServerPluginTypes();
            for (ServerPluginType type : types) {
                List<PluginKey> plugins = master.getAllPluginsByPluginType(type);
                allPlugins.put(type, plugins);
            }
        }

        return allPlugins;
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
            ps = conn.prepareStatement("UPDATE " + ServerPlugin.TABLE_NAME + " SET CONTENT = ? WHERE ID = ?");
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
