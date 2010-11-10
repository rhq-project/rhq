package org.rhq.enterprise.server.resource.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.inventory.InventoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Stateless
public class PluginManagerBean implements PluginManagerLocal {

    private final Log log = LogFactory.getLog(PluginManagerBean.class);

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private static final PluginMetadataManager PLUGIN_METADATA_MANAGER = new PluginMetadataManager();

    @EJB
    private ResourceMetadataManagerLocal resourceMetadataManager;

    @EJB
    private PluginManagerLocal pluginMgr;

    @EJB
    private InventoryManagerLocal inventoryMgr;

    @EJB
    private ResourceTypeManagerLocal resourceTypeMgr;

    /**
     * Returns the information on the given plugin as found in the database.
     * @param  name the name of a plugin
     * @return the plugin with the specified name
     * @throws javax.persistence.NoResultException when no plugin with that name exists
     */
    public Plugin getPlugin(String name) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
        query.setParameter("name", name);
        Plugin plugin = (Plugin) query.getSingleResult();
        return plugin;
    }

    @SuppressWarnings("unchecked")
    public List<Plugin> getPlugins() {
        Query q = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_INSTALLED);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Plugin> getAllPluginsById(List<Integer> pluginIds) {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<Plugin>(); // nothing to do
        }
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_BY_IDS);
        query.setParameter("ids", pluginIds);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName, ResourceCategory resourceCategory) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_RESOURCE_TYPE_AND_CATEGORY);
        query.setParameter("resourceTypeName", resourceTypeName);
        query.setParameter("resourceCategory", resourceCategory);
        List<Plugin> results = query.getResultList();
        return results;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }

        // we need to make sure that if a plugin is enabled, all of its dependencies are enabled
        PluginDependencyGraph graph = PLUGIN_METADATA_MANAGER.buildDependencyGraph();
        List<Plugin> allPlugins = getPlugins();
        Set<String> pluginsThatNeedToBeEnabled = new HashSet<String>();

        for (Integer pluginId : pluginIds) {
            Plugin plugin = getPluginFromListById(allPlugins, pluginId.intValue());
            if (plugin != null) {
                Collection<String> dependencyNames = graph.getAllDependencies(plugin.getName());
                for (String dependencyName : dependencyNames) {
                    Plugin dependencyPlugin = getPluginFromListByName(allPlugins, dependencyName);
                    if (dependencyPlugin != null && !dependencyPlugin.isEnabled()
                        && !pluginIds.contains(Integer.valueOf(dependencyPlugin.getId()))) {
                        pluginsThatNeedToBeEnabled.add(dependencyPlugin.getDisplayName()); // this isn't enabled and isn't getting enabled, but it needs to be
                    }
                }
            }
        }

        if (!pluginsThatNeedToBeEnabled.isEmpty()) {
            throw new IllegalArgumentException("You must enable the following plugin dependencies also: "
                + pluginsThatNeedToBeEnabled);
        }

        // everything is OK, we can enable them
        for (Integer pluginId : pluginIds) {
            setPluginEnabledFlag(subject, pluginId, true);
        }

        return;
    }

    @Override
    public void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }

        // we need to make sure that if a plugin is disabled, no other plugins that depend on it are enabled
        PluginDependencyGraph graph = PLUGIN_METADATA_MANAGER.buildDependencyGraph();
        List<Plugin> allPlugins = getPlugins();
        Set<String> pluginsThatNeedToBeDisabled = new HashSet<String>();

        for (Integer pluginId : pluginIds) {
            Plugin plugin = getPluginFromListById(allPlugins, pluginId.intValue());
            if (plugin != null) {
                Collection<String> dependentNames = graph.getAllDependents(plugin.getName());
                for (String dependentName : dependentNames) {
                    Plugin dependentPlugin = getPluginFromListByName(allPlugins, dependentName);
                    if (dependentPlugin != null && dependentPlugin.isEnabled()
                        && !pluginIds.contains(Integer.valueOf(dependentPlugin.getId()))) {
                        pluginsThatNeedToBeDisabled.add(dependentPlugin.getDisplayName()); // this isn't disabled and isn't getting disabled, but it needs to be
                    }
                }
            }
        }

        if (!pluginsThatNeedToBeDisabled.isEmpty()) {
            throw new IllegalArgumentException("You must disable the following dependent plugins also: "
                + pluginsThatNeedToBeDisabled);
        }

        // everything is OK, we can disable them
        for (Integer pluginId : pluginIds) {
            setPluginEnabledFlag(subject, pluginId, false);
        }

        return;
    }

    @Override
    public void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds.isEmpty()) {
            return;
        }

        PluginDependencyGraph graph = PLUGIN_METADATA_MANAGER.buildDependencyGraph();
        List<Plugin> allPlugins = getPlugins();
        Set<String> pluginsToDelete = new HashSet<String>();

        for (Integer pluginId : pluginIds) {
            Plugin plugin = getPluginFromListById(allPlugins, pluginId.intValue());
            if (plugin != null) {
                Collection<String> dependentNames = graph.getAllDependents(plugin.getName());
                for (String dependentName : dependentNames) {
                    Plugin dependentPlugin = getPluginFromListByName(allPlugins, dependentName);
                    if (dependentPlugin != null && dependentPlugin.isEnabled()
                        && !pluginIds.contains(Integer.valueOf(dependentPlugin.getId()))) {
                        pluginsToDelete.add(dependentPlugin.getDisplayName());
                    }
                }
            }
        }

        if (!pluginsToDelete.isEmpty()) {
            throw new IllegalArgumentException("You must delete the following dependent plugins also: "
                + pluginsToDelete);
        }

        List<Plugin> plugins = getAllPluginsById(pluginIds);
        for (Plugin plugin : plugins) {
            List<ResourceType> resourceTypes = resourceTypeMgr.getResourceTypesByPlugin(plugin.getName());
            Plugin managedPlugin = entityManager.merge(plugin);
            inventoryMgr.markTypesDeleted(resourceTypes);
            entityManager.remove(managedPlugin);
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setPluginEnabledFlag(Subject subject, int pluginId, boolean enabled) throws Exception {
        Query q = entityManager.createNamedQuery(Plugin.UPDATE_PLUGIN_ENABLED_BY_ID);
        q.setParameter("id", pluginId);
        q.setParameter("enabled", Boolean.valueOf(enabled));
        q.executeUpdate();
        log.info((enabled ? "Enabling" : "Disabling") + " plugin [" + pluginId + "]");
        return;
    }

    private Plugin getPluginFromListById(List<Plugin> plugins, int id) {
        for (Plugin plugin : plugins) {
            if (id == plugin.getId()) {
                return plugin;
            }
        }
        return null;
    }

    private Plugin getPluginFromListByName(List<Plugin> plugins, String name) {
        for (Plugin plugin : plugins) {
            if (name.equals(plugin.getName())) {
                return plugin;
            }
        }
        return null;
    }

    // Start with no transaction so we can control the transactional boundaries. This is important for a
    // few reasons. Registering the plugin and removing obsolete types are perfromed in different, subsequent,
    // transactions. The register may update types, and that locks various rows of the database. Those rows
    // must be unlocked before obsolete type removal.  Type removal executes (resource) bulk delete under the covers,
    // and that will deadlock with the rows locked by the type update (at least in oracle) if performed in the same
    // transaction.  Furthermore, as mentioned, obsolete type removal removes resources of the obsolete type. We
    // need to avoid an umbrella transaction for the type removal because large inventories of obsolete resources
    // will generate very large transactions. Potentially resulting in timeouts or other issues.
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void registerPlugin(Subject subject, Plugin plugin, PluginDescriptor pluginDescriptor, File pluginFile,
        boolean forceUpdate) throws Exception {

        boolean typesUpdated = pluginMgr.registerPluginTypes(subject, plugin, pluginDescriptor,
            pluginFile, forceUpdate);

        if (typesUpdated) {
            resourceMetadataManager.removeObsoleteTypes(subject, plugin.getName(), PLUGIN_METADATA_MANAGER);

//            Set<ResourceType> removedTypes = new HashSet<ResourceType>();
//            resourceMetadataManager.getPluginTypes(subject, plugin.getName(), new HashSet<ResourceType>(),
//                    removedTypes, PLUGIN_METADATA_MANAGER);
//            inventoryMgr.markTypesDeleted(new ArrayList<ResourceType>(removedTypes));
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public boolean registerPluginTypes(Subject subject, Plugin plugin, PluginDescriptor pluginDescriptor,
        File pluginFile, boolean forceUpdate) throws Exception {

        // TODO GH: Consider how to remove features from plugins in updates without breaking everything

        Plugin existingPlugin = null;
        boolean newOrUpdated = false;
        boolean typesUpdated = false;

        try {
            existingPlugin = getPlugin(plugin.getName());
        } catch (NoResultException nre) {
            newOrUpdated = true; // this is expected for new plugins
        }

        if (existingPlugin != null) {
            Plugin obsolete = AgentPluginDescriptorUtil.determineObsoletePlugin(plugin, existingPlugin);
            if (obsolete == existingPlugin) { // yes, use == for reference equality
                newOrUpdated = true;
            }
            plugin.setId(existingPlugin.getId());
            plugin.setEnabled(existingPlugin.isEnabled());
        }

        // If this is a brand new plugin, it gets "updated" too.
        if (newOrUpdated) {
            if (plugin.getDisplayName() == null) {
                plugin.setDisplayName(plugin.getName());
            }

            plugin = updatePluginExceptContent(plugin);
            if (pluginFile != null) {
                entityManager.flush();
                streamPluginFileContentToDatabase(plugin.getId(), pluginFile);
            }
            log.debug("Updated plugin entity [" + plugin + "]");
        }

        if (newOrUpdated || forceUpdate || !PLUGIN_METADATA_MANAGER.getPluginNames().contains(plugin.getName())) {
            Set<ResourceType> rootResourceTypes = PLUGIN_METADATA_MANAGER.loadPlugin(pluginDescriptor);
            if (rootResourceTypes == null) {
                throw new Exception("Failed to load plugin [" + plugin.getName() + "].");
            }
            if (newOrUpdated || forceUpdate) {
                // Only merge the plugin's ResourceTypes into the DB if the plugin is new or updated or we were forced to
                resourceMetadataManager.updateTypes(rootResourceTypes);
                typesUpdated = true;
            }
        }

        // TODO GH: JBNADM-1310/JBNADM-1630 - Push updated plugins to running agents and have them reboot their PCs
        // We probably want to be smart about this - perhaps have the agents periodically poll their server to see
        // if there are new plugins and if so download them - this of course would be configurable/disableable

        return typesUpdated;
    }

    private Plugin updatePluginExceptContent(Plugin plugin) throws Exception {
        // this method is here because we need a way to update the plugin's information
        // without blowing away the content data. Because we do not want to load the
        // content blob in memory, the plugin's content field will be null - if we were
        // to entityManager.merge that plugin POJO, it would null out that blob column.
        if (plugin.getId() == 0) {
            entityManager.persist(plugin);
        } else {
            // update all the fields except content
            Plugin pluginEntity = entityManager.getReference(Plugin.class, plugin.getId());
            pluginEntity.setName(plugin.getName());
            pluginEntity.setPath(plugin.getPath());
            pluginEntity.setDisplayName(plugin.getDisplayName());
            pluginEntity.setEnabled(plugin.isEnabled());
            pluginEntity.setStatus(plugin.getStatus());
            pluginEntity.setMd5(plugin.getMD5());
            pluginEntity.setVersion(plugin.getVersion());
            pluginEntity.setAmpsVersion(plugin.getAmpsVersion());
            pluginEntity.setDeployment(plugin.getDeployment());
            pluginEntity.setDescription(plugin.getDescription());
            pluginEntity.setHelp(plugin.getHelp());
            pluginEntity.setMtime(plugin.getMtime());

            try {
                entityManager.flush(); // make sure we push this out to the DB now
            } catch (Exception e) {
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
     * @param id the id of the plugin whose content is being updated
     * @param file the plugin file whose content will be streamed to the database
     *
     * @throws Exception on failure to update the plugin's content
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
