/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
import java.util.Collections;
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

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.plugin.PluginConfigurationRequiredException;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceMBean;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ControlDefinition;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * A server API into the server plugin infrastructure.
 *
 * @author John Mazzitelli
 */
@Stateless
public class ServerPluginManagerBean implements ServerPluginManagerLocal, ServerPluginManagerRemote {

    private final Log log = LogFactory.getLog(ServerPluginManagerBean.class);
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @EJB
    private ServerPluginManagerLocal serverPluginsBean; //self

    @EJB
    private ServerManagerLocal serverManager;

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void restartMasterPluginContainer(Subject subject) {
        LookupUtil.getServerPluginService().restartMasterPluginContainer();
    }

    @SuppressWarnings("unchecked")
    public List<ServerPlugin> getServerPlugins() {
        Query q = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_ALL_INSTALLED);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<ServerPlugin> getAllServerPlugins() {
        Query q = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_ALL);
        return q.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ServerPlugin> getDeletedPlugins() {
        Query q = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_DELETED);
        return q.getResultList();
    }

    public ServerPlugin getServerPlugin(PluginKey key) {
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_BY_NAME);
        query.setParameter("name", key.getPluginName());
        try {
            ServerPlugin plugin = (ServerPlugin) query.getSingleResult();
            return plugin;
        } catch (NoResultException nre) {
            return null;
        }
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

    @SuppressWarnings("unchecked")
    public List<ServerPlugin> getServerPluginsById(List<Integer> pluginIds) {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<ServerPlugin>(); // nothing to do
        }
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_BY_IDS);
        query.setParameter("ids", pluginIds);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<ServerPlugin> getEnabledServerPluginsByType(String type) {
        if (type==null)
            return Collections.emptyList();

        Query query = entityManager.createQuery(
            "SELECT sp FROM ServerPlugin sp WHERE sp.enabled=true AND sp.type LIKE :stype");
        query.setParameter("stype",type);
        List result = query.getResultList();

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ServerPlugin> getAllServerPluginsById(List<Integer> pluginIds) {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<ServerPlugin>(); // nothing to do
        }
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_ALL_BY_IDS);
        query.setParameter("ids", pluginIds);
        return query.getResultList();
    }

    public long getLastConfigurationChangeTimestamp(int pluginId) {
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_GET_CONFIG_MTIMES);
        query.setParameter("id", pluginId);
        Object[] timestamps = (Object[]) query.getSingleResult();
        long lastConfigChangeTimestamp = 0L;
        for (Object timestamp : timestamps) {
            if (timestamp != null) {
                long val = ((Long) timestamp).longValue();
                if (val > lastConfigChangeTimestamp) {
                    lastConfigChangeTimestamp = val;
                }
            }
        }
        return lastConfigChangeTimestamp;
    }

    public ServerPluginDescriptorType getServerPluginDescriptor(PluginKey pluginKey) throws Exception {
        ServerPlugin plugin = getServerPlugin(pluginKey);
        if (plugin == null) {
            throw new IllegalArgumentException("Unknown plugin key: " + pluginKey);
        }

        File pluginsDir = LookupUtil.getServerPluginService().getServerPluginsDirectory();
        File pluginJar = new File(pluginsDir, plugin.getPath());
        URL url = pluginJar.toURI().toURL();
        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
        return descriptor;
    }

    @SuppressWarnings("unchecked")
    public List<PluginKey> getServerPluginKeysByEnabled(boolean enabled) {
        Query query = entityManager.createNamedQuery(ServerPlugin.QUERY_GET_KEYS_BY_ENABLED);
        query.setParameter("enabled", Boolean.valueOf(enabled));
        return query.getResultList();
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public List<PluginKey> enableServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<PluginKey>(); // nothing to do
        }

        List<PluginKey> pluginKeys = new ArrayList<PluginKey>();
        List<ServerPlugin> toEnable = new ArrayList<ServerPlugin>();
        // first we read plugins from DB and check whether they are configured
        // (such plugin would fail to load - in this case we don't enable anything and just throw PluginConfigurationRequiredException)
        for (Integer pluginId : pluginIds) {
            ServerPlugin plugin = null;
            try {
                plugin = entityManager.getReference(ServerPlugin.class, pluginId);
            } catch (Exception e) {
                log.debug("Cannot enable plugin [" + pluginId + "]. Cause: " + ThrowableUtil.getAllMessages(e));
            }
            if (plugin != null) {
                checkForRequiredConfiguration(plugin);
                toEnable.add(plugin);
            }
        }
        for (ServerPlugin serverPlugin : toEnable) {
            serverPluginsBean.setServerPluginEnabledFlag(subject, serverPlugin.getId(), true);
            PluginKey pluginKey = new PluginKey(serverPlugin);
            boolean success = enableServerPluginInMasterContainer(pluginKey);
            if (success) {
                pluginKeys.add(pluginKey);
            } else {
                // we can't enable the plugin in the container, there must be a problem with it - disable it
                serverPluginsBean.setServerPluginEnabledFlag(subject, serverPlugin.getId(), false);
            }
        }

        return pluginKeys;
    }

    /**
     * checks if plugin configuration has set all required properties 
     * @param plugin
     * @throws PluginConfigurationRequiredException if plugin is missing required configuration
     * @throws Exception in case of any other error
     */
    private void checkForRequiredConfiguration(ServerPlugin plugin) throws Exception {
        ConfigurationDefinition configDef = getServerPluginConfigurationDefinition(new PluginKey(plugin));
        Configuration configuration = plugin.getPluginConfiguration();
        for (PropertyDefinition propDef : configDef.getPropertyDefinitions().values()) {
            if (propDef.isRequired() && propDef instanceof PropertyDefinitionSimple) {
                Property prop = configuration.get(propDef.getName());
                PropertySimple simple = (PropertySimple) prop;
                if (simple == null || simple.getStringValue() == null || "".equals(simple.getStringValue())) {
                    throw new PluginConfigurationRequiredException("Plugin [" + plugin.getDisplayName()
                        + "] could not be enabled, because some required configuration fields are not set.");
                }
            }
        }
    }

    private boolean enableServerPluginInMasterContainer(PluginKey pluginKey) {
        ServerPluginServiceMBean serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();
        boolean success = true; // assume everything will be ok

        if (master != null) {
            AbstractTypeServerPluginContainer pc = master.getPluginContainerByPlugin(pluginKey);
            if (pc != null) {
                try {
                    pc.reloadPlugin(pluginKey, true);
                    try {
                        pc.schedulePluginJobs(pluginKey);
                    } catch (Exception e) {
                        // note that we still will report this plugin as enabled - its running
                        // in the plugin container, its just that we couldn't schedule its jobs
                        log.warn("Failed to schedule jobs for plugin [" + pluginKey + "]", e);
                    }
                } catch (Exception e) {
                    log.warn("Failed to enable server plugin [" + pluginKey + "]", e);
                    success = false;
                }
            }
        }

        // If the master PC was not started, we will still say it was successful - the next time someone starts
        // the master PC, they will be told then of any errors that might occur
        return success;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public List<PluginKey> disableServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<PluginKey>(); // nothing to do
        }

        List<PluginKey> pluginKeys = new ArrayList<PluginKey>();

        for (Integer pluginId : pluginIds) {
            serverPluginsBean.setServerPluginEnabledFlag(subject, pluginId, false);

            ServerPlugin plugin = null;
            try {
                plugin = entityManager.getReference(ServerPlugin.class, pluginId);
            } catch (Exception e) {
                log.debug("Cannot disable plugin [" + pluginId + "]. Cause: " + ThrowableUtil.getAllMessages(e));
            }
            if (plugin != null) {
                PluginKey pluginKey = new PluginKey(plugin);
                boolean success = disableServerPluginInMasterContainer(pluginKey);
                if (success) {
                    pluginKeys.add(pluginKey);
                }
            }
        }

        return pluginKeys;
    }

    private boolean disableServerPluginInMasterContainer(PluginKey pluginKey) {
        ServerPluginServiceMBean serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();
        boolean success = true; // assume everything will be ok

        if (master != null) {
            AbstractTypeServerPluginContainer pc = master.getPluginContainerByPlugin(pluginKey);
            if (pc != null) {
                try {
                    pc.unschedulePluginJobs(pluginKey);
                } catch (Exception e) {
                    // even though we can't unschedule jobs, keep going, assume success if the PC disabled it
                    log.warn("Failed to unschedule jobs for plugin [" + pluginKey + "]", e);
                }
                try {
                    pc.reloadPlugin(pluginKey, false);
                } catch (Exception e) {
                    log.warn("Failed to disable server plugin [" + pluginKey + "]", e);
                    success = false;
                }
            }
        }

        return success;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public List<PluginKey> deleteServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<PluginKey>(); // nothing to do
        }

        List<PluginKey> pluginKeys = new ArrayList<PluginKey>();

        for (Integer pluginId : pluginIds) {
            serverPluginsBean.setServerPluginEnabledFlag(subject, pluginId, false);

            ServerPlugin plugin = null;
            try {
                plugin = entityManager.getReference(ServerPlugin.class, pluginId);
            } catch (Exception e) {
                log.debug("Cannot undeploy plugin [" + pluginId + "]. Cause: " + ThrowableUtil.getAllMessages(e));
            }
            if (plugin != null) {
                PluginKey pluginKey = new PluginKey(plugin);
                boolean success = undeployServerPluginInMasterContainer(pluginKey);
                if (success) {
                    pluginKeys.add(pluginKey);
                }

                // if this plugin ever gets re-installed, let's support the use-case where the new plugin
                // will have different config metadata. Here we null out the old config so the new
                // config can be set to the new config definition's default values.
                if (plugin.getPluginConfiguration() != null) {
                    entityManager.remove(plugin.getPluginConfiguration());
                    plugin.setPluginConfiguration(null);
                }
                if (plugin.getScheduledJobsConfiguration() != null) {
                    entityManager.remove(plugin.getScheduledJobsConfiguration());
                    plugin.setScheduledJobsConfiguration(null);
                }
                plugin.setStatus(PluginStatusType.DELETED);

                // purge the file from the filesystem, we do not want this deployed again
                try {
                    File pluginDir = LookupUtil.getServerPluginService().getServerPluginsDirectory();
                    File currentFile = new File(pluginDir, plugin.getPath());
                    currentFile.delete();
                } catch (Exception e) {
                    log.error("Failed to delete the undeployed plugin file [" + plugin.getPath() + "]. Cause: "
                        + ThrowableUtil.getAllMessages(e));
                }
                log.info("Server plugin [" + pluginKey + "] has been undeployed");
            }
        }

        return pluginKeys;
    }

    private boolean undeployServerPluginInMasterContainer(PluginKey pluginKey) {
        ServerPluginServiceMBean serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();
        boolean success = true; // assume everything will be ok

        if (master != null) {
            AbstractTypeServerPluginContainer pc = master.getPluginContainerByPlugin(pluginKey);
            if (pc != null) {
                try {
                    pc.unschedulePluginJobs(pluginKey);
                } catch (Exception e) {
                    log.warn("Failed to unschedule jobs for server plugin [" + pluginKey + "]", e);
                }
                try {
                    pc.unloadPlugin(pluginKey);
                } catch (Exception e) {
                    log.warn("Failed to unload server plugin [" + pluginKey + "]", e);
                    success = false;
                }
            }
        }

        return success;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setServerPluginEnabledFlag(Subject subject, int pluginId, boolean enabled) throws Exception {
        Query q = entityManager.createNamedQuery(ServerPlugin.UPDATE_PLUGIN_ENABLED_BY_ID);
        q.setParameter("id", pluginId);
        q.setParameter("enabled", Boolean.valueOf(enabled));
        q.executeUpdate();
        log.info((enabled ? "Enabling" : "Disabling") + " server plugin [" + pluginId + "]");
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

        PluginKey pluginKey = new PluginKey(plugin);
        ServerPlugin existingPlugin = null;
        boolean newOrUpdated = false;

        existingPlugin = getServerPlugin(pluginKey);
        if (existingPlugin == null) {
            newOrUpdated = true; // this is expected for new plugins
        }

        if (existingPlugin != null) {
            if (existingPlugin.getStatus() == PluginStatusType.DELETED) {
                throw new IllegalArgumentException("Cannot register plugin [" + plugin.getName()
                    + "], it has been marked as deleted");
            }
            ServerPlugin obsolete = ServerPluginDescriptorUtil.determineObsoletePlugin(plugin, existingPlugin);
            if (obsolete == existingPlugin) { // yes use == for reference equality

                // in order to keep the same configuration that the old plugin had, let's set the new plugin's config objects
                // TODO: what happens if the plugin's metadata changes? we should clean the old config of old properties.
                plugin.setPluginConfiguration(existingPlugin.getPluginConfiguration());
                plugin.setScheduledJobsConfiguration(existingPlugin.getScheduledJobsConfiguration());
                newOrUpdated = true;
            }
            plugin.setId(existingPlugin.getId());
        }

        if (newOrUpdated) {
            // prepare some defaults if need be
            if (plugin.getDisplayName() == null) {
                plugin.setDisplayName(plugin.getName());
            }

            // if the ID is 0, it means this is a new plugin and we need to add a row to the db
            // otherwise, this is an update to an existing plugin so we need to update the existing db row
            if (plugin.getId() == 0) {
                entityManager.persist(plugin);
            } else {
                undeployServerPluginInMasterContainer(pluginKey); // remove the old plugin to immediately; this throws away the classloader, too
                plugin = updateServerPluginExceptContent(subject, plugin);
            }

            if (pluginFile != null) {
                entityManager.flush();
                streamPluginFileContentToDatabase(plugin.getId(), pluginFile);
                loadServerPluginInMasterContainer(pluginFile.toURI().toURL());
            }

            if (plugin.isEnabled()) {
                enableServerPluginInMasterContainer(pluginKey);
            }

            log.info("Server plugin [" + plugin.getName() + "] has been registered in the database");
        }

        return plugin;
    }

    private void loadServerPluginInMasterContainer(URL pluginUrl) throws Exception {
        ServerPluginServiceMBean serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();
        if (master != null) {
            master.loadPlugin(pluginUrl, false); // don't enable it - let the caller do that later
        }
        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void purgeServerPlugin(int pluginId) {
        // get the reference to attach to em and use the em.remove. this cascade deletes too.
        ServerPlugin doomed = this.entityManager.find(ServerPlugin.class, pluginId);
        doomed.getServersAcknowledgedDelete().clear();
        this.entityManager.remove(doomed);

        log.info("Server plugin [" + doomed + "] has been purged from the db");
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

    @SuppressWarnings("unchecked")
    public Map<ServerPluginType, List<PluginKey>> getInstalledServerPluginsGroupedByType() {
        Query q = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_ALL_INSTALLED_KEYS);
        List<PluginKey> keys = q.getResultList(); // all installed plugins, both enabled and disabled

        Map<ServerPluginType, List<PluginKey>> allPlugins = new HashMap<ServerPluginType, List<PluginKey>>();

        for (PluginKey key : keys) {
            try {
                ServerPluginType type = new ServerPluginType(key.getPluginType());
                List<PluginKey> knownPluginsForType = allPlugins.get(type);
                if (knownPluginsForType == null) {
                    knownPluginsForType = new ArrayList<PluginKey>();
                    allPlugins.put(type, knownPluginsForType);
                }
                knownPluginsForType.add(key);
            } catch (Exception e) {
                log.warn("Invalid plugin key found [" + key + "]", e);
            }
        }
        return allPlugins;
    }

    @Override
    public ConfigurationDefinition getServerPluginConfigurationDefinition(PluginKey pluginKey) throws Exception {
        ServerPluginDescriptorType descriptor = getServerPluginDescriptor(pluginKey);
        ConfigurationDefinition def;
        def = ConfigurationMetadataParser.parse("pc:" + pluginKey.getPluginName(), descriptor.getPluginConfiguration());
        return def;
    }

    @Override
    public ConfigurationDefinition getServerPluginScheduledJobsDefinition(PluginKey pluginKey) throws Exception {
        ServerPluginDescriptorType descriptor = getServerPluginDescriptor(pluginKey);
        ConfigurationDefinition def;
        def = ConfigurationMetadataParser.parse("jobs:" + pluginKey.getPluginName(), descriptor.getScheduledJobs());
        return def;
    }

    @Override
    public List<ControlDefinition> getServerPluginControlDefinitions(PluginKey pluginKey) throws Exception {

        ServerPluginServiceMBean serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();
        if (master != null) {
            AbstractTypeServerPluginContainer pc = master.getPluginContainerByPlugin(pluginKey);
            if (pc != null) {
                ServerPluginEnvironment env = pc.getPluginManager().getPluginEnvironment(pluginKey.getPluginName());
                List<ControlDefinition> defs;
                defs = ServerPluginDescriptorMetadataParser.getControlDefinitions(env.getPluginDescriptor());
                return defs;
            } else {
                throw new Exception("There is no known plugin named [" + pluginKey + "]");
            }
        } else {
            throw new Exception("Master plugin container not available - is it initialized?");
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public ControlResults invokeServerPluginControl(Subject subject, PluginKey pluginKey, String controlName,
        Configuration params) throws Exception {

        ServerPluginServiceMBean serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();
        if (master != null) {
            AbstractTypeServerPluginContainer pc = master.getPluginContainerByPlugin(pluginKey);
            if (pc != null) {
                ControlResults results = pc.invokePluginControl(pluginKey, controlName, params);
                return results;
            } else {
                throw new Exception("There is no known plugin named [" + pluginKey + "]. Cannot invoke [" + controlName
                    + "]");
            }
        } else {
            throw new Exception("Master plugin container not available - is it initialized?");
        }
    }

    @Override
    public boolean isReadyForPurge(int pluginId) {
        ServerPlugin plugin = entityManager.find(ServerPlugin.class, pluginId);

        @SuppressWarnings("unchecked")
        List<Server> allServers = entityManager.createNamedQuery(Server.QUERY_FIND_ALL).getResultList();

        for (Server s : allServers) {
            if (!plugin.getServersAcknowledgedDelete().contains(s)) {
                if (log.isDebugEnabled()) {
                    log.debug(plugin + " is not ready to be purged. Server " + s +
                        " has not acknowledged it knows about its deletion.");
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public void acknowledgeDeletedPluginsBy(int serverId) {
        Query q = entityManager.createNamedQuery(ServerPlugin.QUERY_UNACKED_DELETED_PLUGINS);
        q.setParameter("serverId", serverId);

        @SuppressWarnings("unchecked")
        List<ServerPlugin> plugins = q.getResultList();

        Server server = entityManager.find(Server.class, serverId);

        for (ServerPlugin p : plugins) {
            p.getServersAcknowledgedDelete().add(server);
            entityManager.merge(p);
        }
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
            BufferedInputStream bis = new BufferedInputStream(fis);
            try {
                ps.setBinaryStream(1, bis, (int) file.length());
                ps.setInt(2, id);
                int updateResults = ps.executeUpdate();
                if (updateResults != 1) {
                    throw new Exception("Failed to update content for plugin [" + id + "] from [" + file + "]");
                }
            } finally {
                bis.close();
            }
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);
            StreamUtil.safeClose(fis);
        }
        return;
    }

    @SuppressWarnings("unchecked")
    private List<PluginKey> getPluginKeysFromIds(List<Integer> pluginIds) {
        Query q = entityManager.createNamedQuery(ServerPlugin.QUERY_FIND_KEYS_BY_IDS);
        q.setParameter("ids", pluginIds);
        List<PluginKey> keys = q.getResultList();
        return keys;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public List<ServerPlugin> getServerPlugins(Subject subject) {
        return getServerPlugins();
    }

}
