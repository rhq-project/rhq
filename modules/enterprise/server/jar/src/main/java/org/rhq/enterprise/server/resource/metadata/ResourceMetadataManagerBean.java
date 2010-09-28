/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.agent.metadata.SubCategoriesMetadataParser;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationDefinitionUpdateReport;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * This class manages the metadata for resources. Plugins are registered against this bean so that their metadata can be
 * pulled out and stored as necessary.
 *
 * @author Greg Hinkle
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class ResourceMetadataManagerBean implements ResourceMetadataManagerLocal {
    private final Log log = LogFactory.getLog(ResourceMetadataManagerBean.class);

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource dataSource;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private static final PluginMetadataManager PLUGIN_METADATA_MANAGER = new PluginMetadataManager();

    @EJB
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    @EJB
    private MeasurementScheduleManagerLocal scheduleManager;

    @EJB
    private ConfigurationMetadataManagerLocal configurationMetadataManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private EventManagerLocal eventManager;

    @EJB
    private ResourceMetadataManagerLocal resourceMetadataManager; // self

    @SuppressWarnings("unchecked")
    public List<Plugin> getAllPluginsById(List<Integer> pluginIds) {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<Plugin>(); // nothing to do
        }
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_BY_IDS);
        query.setParameter("ids", pluginIds);
        return query.getResultList();
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
            resourceMetadataManager.setPluginEnabledFlag(subject, pluginId, true);
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
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
            resourceMetadataManager.setPluginEnabledFlag(subject, pluginId, false);
        }

        return;
    }

    private Plugin getPluginFromListByName(List<Plugin> plugins, String name) {
        for (Plugin plugin : plugins) {
            if (name.equals(plugin.getName())) {
                return plugin;
            }
        }
        return null;
    }

    private Plugin getPluginFromListById(List<Plugin> plugins, int id) {
        for (Plugin plugin : plugins) {
            if (id == plugin.getId()) {
                return plugin;
            }
        }
        return null;
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

    /**
     * Returns the information on the given plugin as found in the database.
     * @param  name the name of a plugin
     * @return the plugin with the specified name
     * @throws NoResultException when no plugin with that name exists
     */
    public Plugin getPlugin(String name) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
        query.setParameter("name", name);
        Plugin plugin = (Plugin) query.getSingleResult();
        return plugin;
    }

    /**
     * Returns the information on all agent plugins as found in the database.
     */
    @SuppressWarnings("unchecked")
    public List<Plugin> getPlugins() {
        Query q = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_INSTALLED);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName, ResourceCategory resourceCategory) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_RESOURCE_TYPE_AND_CATEGORY);
        query.setParameter("resourceTypeName", resourceTypeName);
        query.setParameter("resourceCategory", resourceCategory);
        List<Plugin> results = query.getResultList();
        return results;
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

        boolean typesUpdated = resourceMetadataManager.registerPluginTypes(subject, plugin, pluginDescriptor,
            pluginFile, forceUpdate);

        if (typesUpdated) {
            removeObsoleteTypes(subject, plugin.getName());
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
                updateTypes(rootResourceTypes);
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

    private void updateTypes(Set<ResourceType> resourceTypes) throws Exception {
        // Only process the type if it is a non-runs-inside type (i.e. not a child of some other type X at this same
        // level in the type hierarchy). runs-inside types which we skip here will get processed at the next level down
        // when we recursively process type X's children.
        Set<ResourceType> allChildren = new HashSet<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            allChildren.addAll(resourceType.getChildResourceTypes());
        }
        Set<ResourceType> nonRunsInsideResourceTypes = new LinkedHashSet<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            if (!allChildren.contains(resourceType)) {
                nonRunsInsideResourceTypes.add(resourceType);
            }
        }

        // Iterate the resource types breadth-first, so all platform types get added before any server types or platform
        // service types. This way, we'll be able to set all of the platform types as parents of the server types and
        // platform service types. It's also helpful for other types with multiple "runs-inside" parent types (e.g
        // Hibernate Entities), since it ensures the parent types will get persisted prior to the child types.
        if (log.isDebugEnabled()) {
            log.debug("Processing types: " + nonRunsInsideResourceTypes + "...");
        }
        Set<ResourceType> legitimateChildren = new HashSet<ResourceType>();
        for (ResourceType resourceType : nonRunsInsideResourceTypes) {
            updateType(resourceType);
            legitimateChildren.addAll(resourceType.getChildResourceTypes());
        }
        // Only recurse if there are actually children - this prevents infinite recursion.
        if (!legitimateChildren.isEmpty()) {
            updateTypes(legitimateChildren);
        }
    }

    // NO TRANSACTION SHOULD BE ACTIVE ON ENTRY 
    // Start with no transaction so we can control the transactional boundaries. Obsolete type removal removes
    // resources of the obsolete type. We need to avoid an umbrella transaction for the type removal because large
    // inventories of obsolete resources will generate very large transactions. Potentially resulting in timeouts
    // or other issues.    
    private void removeObsoleteTypes(Subject subject, String pluginName) {

        Set<ResourceType> obsoleteTypes = new HashSet<ResourceType>();
        Set<ResourceType> legitTypes = new HashSet<ResourceType>();

        try {
            resourceMetadataManager.getPluginTypes(subject, pluginName, legitTypes, obsoleteTypes);

            if (!obsoleteTypes.isEmpty()) {
                // TODO: Log this at DEBUG instead.
                log.info("Removing " + obsoleteTypes.size() + " obsolete types: " + obsoleteTypes + "...");
                removeResourceTypes(subject, obsoleteTypes, new HashSet<ResourceType>(obsoleteTypes));
            }

            // Now it's safe to remove any obsolete subcategories on the legit types.
            for (ResourceType legitType : legitTypes) {
                ResourceType updateType = PLUGIN_METADATA_MANAGER.getType(legitType.getName(), legitType.getPlugin());

                // If we've got a type from the descriptor which matches an existing one,
                // then let's see if we need to remove any subcategories from the existing one.
                if (updateType != null) {
                    try {
                        resourceMetadataManager.removeObsoleteSubCategories(subject, updateType, legitType);
                    } catch (Exception e) {
                        throw new Exception("Failed to delete obsolete subcategories from " + legitType + ".", e);
                    }
                }
            }
        } catch (Exception e) {
            // Catch all exceptions, so a failure here does not cause the outer tx to rollback.
            log.error("Failure during removal of obsolete ResourceTypes and Subcategories.", e);
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @SuppressWarnings("unchecked")
    public void getPluginTypes(Subject subject, String pluginName, Set<ResourceType> legitTypes,
        Set<ResourceType> obsoleteTypes) {
        try {
            Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_PLUGIN);
            query.setParameter("plugin", pluginName);
            List<ResourceType> existingTypes = query.getResultList();

            if (existingTypes != null) {

                for (ResourceType existingType : existingTypes) {
                    if (PLUGIN_METADATA_MANAGER.getType(existingType.getName(), existingType.getPlugin()) == null) {
                        // The type is obsolete - (i.e. it's no longer defined by the plugin).
                        obsoleteTypes.add(existingType);
                    } else {
                        legitTypes.add(existingType);
                    }
                }
            }
        } catch (Exception e) {
            // Catch all exceptions, so a failure here does not cause the outer tx to rollback.
            log.error("Failure during removal of obsolete ResourceTypes and Subcategories.", e);
        }
    }

    // NO TRANSACTION SHOULD BE ACTIVE ON ENTRY 
    private void removeResourceTypes(Subject subject, Set<ResourceType> candidateTypes,
        Set<ResourceType> typesToBeRemoved) throws Exception {
        for (ResourceType candidateType : candidateTypes) {
            // Remove obsolete descendant types first.
            //Set<ResourceType> childTypes = candidateType.getChildResourceTypes();
            List<ResourceType> childTypes = resourceTypeManager.getChildResourceTypes(subject, candidateType);
            if (childTypes != null && !childTypes.isEmpty()) {
                // Wrap child types in new HashSet to avoid ConcurrentModificationExceptions.
                removeResourceTypes(subject, new HashSet<ResourceType>(childTypes), typesToBeRemoved);
            }
            if (typesToBeRemoved.contains(candidateType)) {
                try {
                    removeResourceType(subject, candidateType);
                } catch (Exception e) {
                    throw new Exception("Failed to remove " + candidateType + ".", e);
                }
                typesToBeRemoved.remove(candidateType);
            }
        }
    }

    // NO TRANSACTION SHOULD BE ACTIVE ON ENTRY
    private void removeResourceType(Subject subject, ResourceType existingType) {
        log.info("Removing ResourceType [" + toConciseString(existingType) + "]...");

        // Remove all Resources that are of the type.
        ResourceCriteria c = new ResourceCriteria();
        c.addFilterResourceTypeId(existingType.getId());
        List<Resource> resources = resourceManager.findResourcesByCriteria(subject, c);
        if (resources != null) {
            Iterator<Resource> resIter = resources.iterator();
            while (resIter.hasNext()) {
                Resource res = resIter.next();
                List<Integer> deletedIds = resourceManager.uninventoryResource(subject, res.getId());
                // do this out of band because the current transaction is locking rows that due to
                // updates that may need to get deleted. If you do it here the NewTrans used below
                // may deadlock with the current transactions locks.
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.uninventoryResourceAsyncWork(subject, deletedResourceId);
                }
                resIter.remove();
            }
        }

        resourceMetadataManager.completeRemoveResourceType(subject, existingType);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void completeRemoveResourceType(Subject subject, ResourceType existingType) {
        existingType = entityManager.find(ResourceType.class, existingType.getId());

        if (entityManager.contains(existingType)) {
            entityManager.refresh(existingType);
        }

        // Completely remove the type from the type hierarchy.
        removeFromParents(existingType);
        removeFromChildren(existingType);
        entityManager.merge(existingType);

        // Remove all compatible groups that are of the type.
        List<ResourceGroup> compatGroups = existingType.getResourceGroups();
        if (compatGroups != null) {
            Iterator<ResourceGroup> compatGroupIterator = compatGroups.iterator();
            while (compatGroupIterator.hasNext()) {
                ResourceGroup compatGroup = compatGroupIterator.next();
                try {
                    resourceGroupManager.deleteResourceGroup(subject, compatGroup.getId());
                } catch (ResourceGroupDeleteException e) {
                    throw new RuntimeException(e);
                }
                compatGroupIterator.remove();
            }
        }
        entityManager.flush();

        // Remove the type's metric definitions. We do this separately, rather than just relying on cascade
        // upon deletion of the ResourceType, because the removeMeasurementDefinition() will also take care
        // of removing any associated schedules and those schedules' OOBs.
        Set<MeasurementDefinition> definitions = existingType.getMetricDefinitions();
        if (definitions != null) {
            Iterator<MeasurementDefinition> defIter = definitions.iterator();
            while (defIter.hasNext()) {
                MeasurementDefinition def = defIter.next();
                if (entityManager.contains(def)) {
                    entityManager.refresh(def);
                    measurementDefinitionManager.removeMeasurementDefinition(def);
                }
                defIter.remove();
            }
        }
        entityManager.flush();

        // TODO: Clean out event definitions?

        // Finally, remove the type itself.
        // Refresh it first to make sure any newly discovered Resources of the type get added to the persistence
        // context and hopefully get removed via cascade when we remove the type.
        entityManager.refresh(existingType);
        entityManager.remove(existingType);
        entityManager.flush();
    }

    private void removeFromParents(ResourceType typeToBeRemoved) {
        // Wrap in new HashSet to avoid ConcurrentModificationExceptions.
        Set<ResourceType> parentTypes = new HashSet<ResourceType>(typeToBeRemoved.getParentResourceTypes());
        for (ResourceType parentType : parentTypes) {
            parentType.removeChildResourceType(typeToBeRemoved);
            entityManager.merge(parentType);
        }
    }

    private void removeFromChildren(ResourceType typeToBeRemoved) {
        // Wrap in new HashSet to avoid ConcurrentModificationExceptions.
        Set<ResourceType> childTypes = new HashSet<ResourceType>(typeToBeRemoved.getChildResourceTypes());
        for (ResourceType childType : childTypes) {
            childType.removeParentResourceType(typeToBeRemoved);
            entityManager.merge(childType);
        }
    }

    private void updateType(ResourceType resourceType) {
        entityManager.flush();

        // see if there is already an existing type that we need to update
        if (log.isDebugEnabled()) {
            log.debug("Updating resource type [" + resourceType.getName() + "] from plugin ["
                + resourceType.getPlugin() + "]...");
        }

        ResourceType existingType;
        try {
            existingType = resourceTypeManager.getResourceTypeByNameAndPlugin(resourceType.getName(), resourceType
                .getPlugin());
        } catch (NonUniqueResultException nure) {
            log.debug("Found more than one existing ResourceType for " + resourceType);
            // TODO: Delete the redundant ResourceTypes to get the DB into a valid state.
            throw new IllegalStateException(nure);
        }

        // Connect the parent types if they exist, which they should.
        // We'll do this no matter if the resourceType exists or not - but we use existing vs. resourceType appropriately
        // This is to support the case when an existing type gets a new parent resource type in <runs-inside>
        updateParentResourceTypes(resourceType, existingType);

        if (existingType == null) {
            persistNewType(resourceType);
        } else {
            mergeExistingType(resourceType, existingType);
        }
    }

    private void mergeExistingType(ResourceType resourceType, ResourceType existingType) {
        log.debug("Merging type [" + resourceType + "] + into existing type [" + existingType + "]...");

        // Make sure to first add/update any subcategories on the parent before trying to update children.
        // Otherwise, the children may try to save themselves with subcategories which wouldn't exist yet.
        updateChildSubCategories(resourceType, existingType);

        entityManager.flush();

        // even though we've updated our child types to use new subcategory references, its still
        // not safe to delete the old sub categories yet, because we haven't yet deleted all of the old
        // child types which may still be referencing these sub categories

        // Update the rest of these related resources
        updatePluginConfiguration(resourceType, existingType);
        entityManager.flush();

        updateResourceConfiguration(resourceType, existingType);

        updateMeasurementDefinitions(resourceType, existingType);

        updateContentDefinitions(resourceType, existingType);

        updateOperationDefinitions(resourceType, existingType);

        updateProcessScans(resourceType, existingType);

        updateEventDefinitions(resourceType, existingType);

        // Update the type itself
        existingType.setDescription(resourceType.getDescription());
        existingType.setCreateDeletePolicy(resourceType.getCreateDeletePolicy());
        existingType.setCreationDataType(resourceType.getCreationDataType());
        existingType.setSingleton(resourceType.isSingleton());
        existingType.setSupportsManualAdd(resourceType.isSupportsManualAdd());

        // We need to be careful updating the subcategory. If it is not null and the same ("equals")
        // to the new one, we need to copy over the attributes, as the existing will be kept and
        // the new one not persisted. Otherwise, we can just use the new one.
        ResourceSubCategory oldSubCat = existingType.getSubCategory();
        ResourceSubCategory newSubCat = resourceType.getSubCategory();
        if (oldSubCat != null && oldSubCat.equals(newSubCat)) {
            // Subcategory hasn't changed - nothing to do (call to addAndUpdateChildSubCategories()
            // above already took care of any modifications to the ResourceSubCategories themselves).
        } else if (newSubCat == null) {
            if (oldSubCat != null) {
                log.debug("Metadata update: Subcategory of ResourceType [" + resourceType.getName() + "] changed from "
                    + oldSubCat + " to " + newSubCat);
                existingType.setSubCategory(null);
            }
        } else {
            // New subcategory is non-null and not equal to the old subcategory.
            ResourceSubCategory existingSubCat = SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
                existingType, newSubCat.getName());
            if (existingSubCat == null)
                throw new IllegalStateException("Resource type [" + resourceType.getName() + "] in plugin ["
                    + resourceType.getPlugin() + "] has a subcategory (" + newSubCat.getName()
                    + ") which was not defined as a child subcategory of one of its ancestor resource types.");
            log.debug("Metadata update: Subcategory of ResourceType [" + resourceType.getName() + "] changed from "
                + oldSubCat + " to " + existingSubCat);
            existingType.setSubCategory(existingSubCat);
        }

        existingType = entityManager.merge(existingType);
        entityManager.flush();
    }

    private void persistNewType(ResourceType resourceType) {
        log.info("Persisting new ResourceType [" + toConciseString(resourceType) + "]...");
        // If the type didn't exist then we'll persist here which will cascade through
        // all child types as well as plugin and resource configs and their delegate types and
        // metric and operation definitions and their dependent types,
        // but first do some validity checking.

        // Check if the subcategories as children of resourceType are valid
        // Those are the subcategories we offer for children of us
        checkForValidSubcategories(resourceType.getChildSubCategories());

        // Check if we have a subcategory attached that needs to be linked to one of the parents
        // This is a subcategory of our parent where we are supposed to be grouped in.
        linkSubCategoryToParents(resourceType);

        entityManager.persist(resourceType);
        entityManager.flush();
    }

    private void linkSubCategoryToParents(ResourceType resourceType) {
        if (resourceType.getSubCategory() == null) {
            return; // Nothing to do
        }

        ResourceSubCategory mySubCategory = resourceType.getSubCategory();
        ResourceSubCategory existingCat = SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
            resourceType, mySubCategory.getName());
        if (existingCat != null) {
            resourceType.setSubCategory(existingCat);
        } else {
            throw new IllegalStateException("Subcategory " + mySubCategory.getName() + " defined on resource type "
                + resourceType.getName() + " in plugin " + resourceType.getPlugin()
                + " is not defined in a parent type");
        }
    }

    private void updateParentResourceTypes(ResourceType newType, ResourceType existingType) {
        if (log.isDebugEnabled()) {
            if (existingType != null) {
                log.debug("Setting parent types on existing type: " + existingType + " to ["
                    + newType.getParentResourceTypes() + "] - current parent types are ["
                    + existingType.getParentResourceTypes() + "]...");
            } else {
                log.debug("Setting parent types on new type: " + newType + " to [" + newType.getParentResourceTypes()
                    + "]...");
            }
        }

        Set<ResourceType> newParentTypes = newType.getParentResourceTypes();
        newType.setParentResourceTypes(new HashSet<ResourceType>());
        Set<ResourceType> originalExistingParentTypes = new HashSet<ResourceType>();
        if (existingType != null) {
            originalExistingParentTypes.addAll(existingType.getParentResourceTypes());
        }
        for (ResourceType newParentType : newParentTypes) {
            try {
                boolean isExistingParent = originalExistingParentTypes.remove(newParentType);
                if (existingType == null || !isExistingParent) {
                    ResourceType realParentType = (ResourceType) entityManager.createNamedQuery(
                        ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN).setParameter("name", newParentType.getName())
                        .setParameter("plugin", newParentType.getPlugin()).getSingleResult();
                    ResourceType type = (existingType != null) ? existingType : newType;
                    if (existingType != null) {
                        log.info("Adding ResourceType [" + toConciseString(type) + "] as child of ResourceType ["
                            + toConciseString(realParentType) + "]...");
                    }
                    realParentType.addChildResourceType(type);
                }
            } catch (NoResultException nre) {
                throw new RuntimeException("Couldn't persist type [" + newType + "] because parent [" + newParentType
                    + "] wasn't already persisted.");
            }
        }

        for (ResourceType obsoleteParentType : originalExistingParentTypes) {
            log.info("Removing type [" + toConciseString(existingType) + "] from parent type ["
                + toConciseString(obsoleteParentType) + "]...");
            obsoleteParentType.removeChildResourceType(existingType);
            moveResourcesToNewParent(existingType, obsoleteParentType, newParentTypes);
        }

        entityManager.flush();
    }

    private static String toConciseString(ResourceType type) {
        return (type != null) ? (type.getPlugin() + ":" + type.getName() + "(id=" + type.getId() + ")") : "null";
    }

    private void moveResourcesToNewParent(ResourceType existingType, ResourceType obsoleteParentType,
        Set<ResourceType> newParentTypes) {
        Subject overlord = subjectManager.getOverlord();
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceTypeId(existingType.getId());
        criteria.addFilterParentResourceTypeId(obsoleteParentType.getId());
        List<Resource> resources = resourceManager.findResourcesByCriteria(overlord, criteria);
        for (Resource resource : resources) {
            Resource newParent = null;
            newParentTypes: for (ResourceType newParentType : newParentTypes) {
                Resource ancestorResource = resource.getParentResource();
                while (ancestorResource != null) {
                    if (ancestorResource.getResourceType().equals(newParentType)) {
                        // We found an ancestor to be the new parent of our orphaned Resource.
                        newParent = ancestorResource;
                        break newParentTypes;
                    }
                    ancestorResource = ancestorResource.getParentResource();
                }
                for (Resource childResource : resource.getChildResources()) {
                    if (childResource.getResourceType().equals(newParentType)) {
                        // We found a child to be the new parent of our orphaned Resource.
                        // TODO: Check if there are are multiple children of the new parent type. If so,
                        //       log an error and don't move the resource.
                        newParent = childResource;
                        break newParentTypes;
                    }
                }
            }
            if (newParent != null) {
                if (resource.getParentResource() != null) {
                    resource.getParentResource().removeChildResource(resource);
                }
                newParent.addChildResource(resource);
            } else {
                log.debug("We were unable to move " + resource + " from invalid parent " + resource.getParentResource()
                    + " to a new valid parent with one of the following types: " + newParentTypes);
            }
        }
    }

    private void checkForValidSubcategories(List<ResourceSubCategory> subCategories) {
        Set<String> subCatNames = new HashSet<String>();

        for (ResourceSubCategory subCategory : subCategories) {
            List<ResourceSubCategory> allSubcategories = getAllSubcategories(subCategory);
            for (ResourceSubCategory subCategory2 : allSubcategories) {
                if (subCatNames.contains(subCategory2.getName())) {
                    throw new RuntimeException("Subcategory [" + subCategory.getName() + "] is duplicated");
                }
                subCatNames.add(subCategory2.getName());
            }
        }
    }

    private List<ResourceSubCategory> getAllSubcategories(ResourceSubCategory cat) {
        List<ResourceSubCategory> result = new ArrayList<ResourceSubCategory>();

        if (cat.getChildSubCategories() != null) {
            for (ResourceSubCategory cat2 : cat.getChildSubCategories()) {
                result.addAll(getAllSubcategories(cat2));
            }
        }

        result.add(cat);
        return result;
    }

    /* Update the <event> tags */
    private void updateEventDefinitions(ResourceType newType, ResourceType existingType) {
        Set<EventDefinition> newEventDefs = newType.getEventDefinitions();
        // Loop over the newEventDefs and set the resourceTypeId, so equals() will work
        for (EventDefinition def : newEventDefs) {
            def.setResourceTypeId(existingType.getId());
        }

        Set<EventDefinition> existingEventDefs = existingType.getEventDefinitions();
        for (EventDefinition def : existingEventDefs) {
            entityManager.refresh(def);
        }

        Set<EventDefinition> toDelete = missingInFirstSet(newEventDefs, existingEventDefs);
        Set<EventDefinition> newOnes = missingInFirstSet(existingEventDefs, newEventDefs);
        Set<EventDefinition> toUpdate = intersection(newEventDefs, existingEventDefs);

        // update existing ones
        for (EventDefinition eDef : existingEventDefs) {
            for (EventDefinition nDef : toUpdate) {
                if (eDef.equals(nDef)) {
                    eDef.setDescription(nDef.getDescription());
                    eDef.setDisplayName(nDef.getDisplayName());
                }
            }
        }

        // Persist new definitions
        for (EventDefinition eDef : newOnes) {
            EventDefinition e2 = new EventDefinition(existingType, eDef.getName());
            e2.setDescription(eDef.getDescription());
            e2.setDisplayName(eDef.getDisplayName());
            entityManager.persist(e2);
            existingType.addEventDefinition(e2);
        }

        // and finally remove deleted ones. First flush the EM to be on the save side
        // for a bulk delete.
        existingEventDefs.removeAll(toDelete);
        entityManager.flush();
        for (EventDefinition eDef : toDelete) {
            // remove EventSources and events on it.
            eventManager.deleteEventSourcesForDefinition(eDef);
            entityManager.remove(eDef);
        }
    }

    /**
     * Update the stuff below a <plugin-configuration>
     */
    private void updatePluginConfiguration(ResourceType resourceType, ResourceType existingType) {
        ConfigurationDefinition existingConfigurationDefinition = existingType.getPluginConfigurationDefinition();
        if (resourceType.getPluginConfigurationDefinition() != null) {
            // all new
            if (existingConfigurationDefinition == null) {
                entityManager.persist(resourceType.getPluginConfigurationDefinition());
                existingType.setPluginConfigurationDefinition(resourceType.getPluginConfigurationDefinition());
            } else // update the configuration
            {
                ConfigurationDefinitionUpdateReport updateReport = configurationMetadataManager
                    .updateConfigurationDefinition(resourceType.getPluginConfigurationDefinition(),
                        existingConfigurationDefinition);

                if (updateReport.getNewPropertyDefinitions().size() > 0 ||
                    updateReport.getUpdatedPropertyDefinitions().size() > 0) {
                    Subject overlord = subjectManager.getOverlord();
                    ResourceCriteria criteria = new ResourceCriteria();
                    criteria.addFilterResourceTypeId(existingType.getId());
                    List<Resource> resources = resourceManager.findResourcesByCriteria(overlord, criteria);

                    for (Resource resource : resources) {
                        updateResourcePluginConfiguration(resource, updateReport);
                    }
                }
            }
        } else {
            // resourceType.getPlu... is null -> remove the existing config
            if (existingConfigurationDefinition != null) {
                existingType.setPluginConfigurationDefinition(null);
                entityManager.remove(existingConfigurationDefinition);
            }
        }
    }

    private void updateResourcePluginConfiguration(Resource resource, ConfigurationDefinitionUpdateReport updateReport) {
        Configuration pluginConfiguration = resource.getPluginConfiguration();
        boolean modified = false;
        int numberOfProperties = pluginConfiguration.getProperties().size();
        ConfigurationTemplate template = updateReport.getConfigurationDefinition().getDefaultTemplate();
        Configuration templateConfiguration = template.getConfiguration();

        for (PropertyDefinition propertyDef : updateReport.getNewPropertyDefinitions()) {
            if (propertyDef.isRequired()) {
                Property templateProperty = templateConfiguration.get(propertyDef.getName());
                pluginConfiguration.put(templateProperty.deepCopy(false));
                modified = true;
            }
        }

        for (PropertyDefinition propertyDef : updateReport.getUpdatedPropertyDefinitions()) {
            if (propertyDef.isRequired()) {
                String propertyValue = pluginConfiguration.getSimpleValue(propertyDef.getName(), null);
                if (propertyValue == null) {
                    Property templateProperty = templateConfiguration.get(propertyDef.getName());
                    pluginConfiguration.put(templateProperty.deepCopy(false));
                    modified = true;
                }
            }
        }

//        if (pluginConfiguration.getProperties().size() > numberOfProperties) {
        if (modified) {
            resource.setMtime(new Date().getTime());
        }
    }

    /**
     * Update the set of process scans for a given resource type
     *
     * @param resourceType
     * @param existingType
     */
    private void updateProcessScans(ResourceType resourceType, ResourceType existingType) {
        Set<ProcessScan> existingScans = existingType.getProcessScans();
        Set<ProcessScan> newScans = resourceType.getProcessScans();

        Set<ProcessScan> scansToPersist = missingInFirstSet(existingScans, newScans);
        Set<ProcessScan> scansToDelete = missingInFirstSet(newScans, existingScans);

        Set<ProcessScan> scansToUpdate = intersection(existingScans, newScans);

        // update scans that may have changed
        for (ProcessScan scan : scansToUpdate) {
            for (ProcessScan nScan : newScans) {
                if (scan.equals(nScan)) {
                    scan.setName(nScan.getName());
                }
            }
        }

        // persist new scans
        for (ProcessScan scan : scansToPersist) {
            existingType.addProcessScan(scan);
        }

        // remove deleted ones
        for (ProcessScan scan : scansToDelete) {
            existingScans.remove(scan);
            entityManager.remove(scan);
        }
    }

    /**
     * Update the operation definitions of existingType with the ones from resource type.
     *
     * @param resourceType New resourceType definition with operationDefinitions
     * @param existingType The existing resource type with operation Definitions
     */
    private void updateOperationDefinitions(ResourceType resourceType, ResourceType existingType) {
        Set<OperationDefinition> existingDefinitions = existingType.getOperationDefinitions();
        Set<OperationDefinition> newDefinitions = resourceType.getOperationDefinitions();

        Set<OperationDefinition> newOps = missingInFirstSet(existingDefinitions, newDefinitions);
        Set<OperationDefinition> opsToRemove = missingInFirstSet(newDefinitions, existingDefinitions);

        existingDefinitions.retainAll(newDefinitions);

        // loop over the OperationDefinitions that are neither new nor deleted
        // and update them from the resourceType
        for (OperationDefinition def : existingDefinitions) {
            for (OperationDefinition nDef : newDefinitions) {
                if (def.equals(nDef)) {
                    def.setDescription(nDef.getDescription());
                    def.setDisplayName(nDef.getDisplayName());
                    def.setParametersConfigurationDefinition(nDef.getParametersConfigurationDefinition());
                    def.setResourceVersionRange(nDef.getResourceVersionRange());
                    def.setResultsConfigurationDefinition(nDef.getResultsConfigurationDefinition());
                    def.setTimeout(nDef.getTimeout());
                }
            }
        }

        for (OperationDefinition newOp : newOps) {
            existingType.addOperationDefinition(newOp); // does the back link as well
        }

        existingDefinitions.removeAll(opsToRemove);
        for (OperationDefinition opToDelete : opsToRemove) {
            entityManager.remove(opToDelete);
        }
    }

    private void updateMeasurementDefinitions(ResourceType newType, ResourceType existingType) {
        if (newType.getMetricDefinitions() != null) {
            Set<MeasurementDefinition> existingDefinitions = existingType.getMetricDefinitions();
            if (existingDefinitions.isEmpty()) {
                // They're all new.
                for (MeasurementDefinition newDefinition : newType.getMetricDefinitions()) {
                    if (newDefinition.getDefaultInterval() < MeasurementSchedule.MINIMUM_INTERVAL) {
                        newDefinition.setDefaultInterval(MeasurementSchedule.MINIMUM_INTERVAL);
                        log.info("Definition [" + newDefinition
                            + "] has too short of a default interval, setting to minimum");
                    }
                    existingType.addMetricDefinition(newDefinition);
                    entityManager.persist(newDefinition);

                    // Now create schedules for already existing resources
                    scheduleManager.createSchedulesForExistingResources(existingType, newDefinition);
                }
            } else {
                // Update existing or add new metrics
                for (MeasurementDefinition newDefinition : newType.getMetricDefinitions()) {
                    boolean found = false;
                    for (MeasurementDefinition existingDefinition : existingDefinitions) {
                        if (existingDefinition.getName().equals(newDefinition.getName())
                            && (existingDefinition.isPerMinute() == newDefinition.isPerMinute())) {
                            found = true;

                            existingDefinition.update(newDefinition, false);

                            // we normally do not want to touch interval in case a user changed it,
                            // but we cannot allow too-short of an interval, so override it if necessary
                            if (existingDefinition.getDefaultInterval() < MeasurementSchedule.MINIMUM_INTERVAL) {
                                existingDefinition.setDefaultInterval(MeasurementSchedule.MINIMUM_INTERVAL);
                                log.info("Definition [" + existingDefinition
                                    + "] has too short of a default interval, setting to minimum");
                            }

                            entityManager.merge(existingDefinition);

                            // There is nothing in the schedules that need to be updated.
                            // We do not want to change schedules (such as collection interval)
                            // because the user might have customized them. So leave them be.

                            break;
                        }
                    }

                    if (!found) {
                        // Its new, create it
                        existingType.addMetricDefinition(newDefinition);
                        entityManager.persist(newDefinition);

                        // Now create schedules for already existing resources
                        scheduleManager.createSchedulesForExistingResources(existingType, newDefinition);
                    }
                }

                /*
                 * Now delete outdated measurement definitions. First find them ...
                 */
                List<MeasurementDefinition> definitionsToDelete = new ArrayList<MeasurementDefinition>();
                for (MeasurementDefinition existingDefinition : existingDefinitions) {
                    if (!newType.getMetricDefinitions().contains(existingDefinition)) {
                        definitionsToDelete.add(existingDefinition);
                    }
                }

                // ... and remove them
                existingDefinitions.removeAll(definitionsToDelete);
                for (MeasurementDefinition definitionToDelete : definitionsToDelete) {
                    measurementDefinitionManager.removeMeasurementDefinition(definitionToDelete);
                }
                if (!definitionsToDelete.isEmpty() && log.isDebugEnabled()) {
                    log.debug("Metadata update: Measurement definitions deleted from resource type ["
                        + existingType.getName() + "]:" + definitionsToDelete);
                }

                entityManager.flush();
            }
        }
        // TODO what if they are null? --> delete everything from existingType
        // not needed see JBNADM-1639
    }

    /**
     * Updates the database with new package definitions found in the new resource type. Any definitions not found in
     * the new type but were previously in the existing resource type will be removed. Any definitions common to both
     * will be merged.
     *
     * @param newType      new resource type containing updated package definitions
     * @param existingType old resource type with existing package definitions
     */
    private void updateContentDefinitions(ResourceType newType, ResourceType existingType) {

        // set the bundle type if one is defined
        BundleType newBundleType = newType.getBundleType();
        if (newBundleType != null) {
            BundleType existingBundleType = existingType.getBundleType();
            newBundleType.setResourceType(existingType);
            if (existingBundleType != null) {
                newBundleType.setId(existingBundleType.getId());
                newBundleType = entityManager.merge(newBundleType);
            }
            existingType.setBundleType(newBundleType);
        } else {
            existingType.setBundleType(null);
        }

        // Easy case: If there are no package definitions in the new type, null out any in the existing and return
        if ((newType.getPackageTypes() == null) || (newType.getPackageTypes().size() == 0)) {
            existingType.setPackageTypes(null);
            return;
        }

        // The new type has package definitions

        // Easy case: If the existing type did not have any package definitions, simply use the new type defs and return
        if ((existingType.getPackageTypes() == null) || (existingType.getPackageTypes().size() == 0)) {
            for (PackageType newPackageType : newType.getPackageTypes()) {
                newPackageType.setResourceType(existingType);
                entityManager.persist(newPackageType);
            }

            existingType.setPackageTypes(newType.getPackageTypes());
            return;
        }

        // Both the new and existing types have definitions, so merge
        Set<PackageType> existingPackageTypes = existingType.getPackageTypes();
        Map<String, PackageType> newPackageTypeDefinitions = new HashMap<String, PackageType>(newType.getPackageTypes()
            .size());
        for (PackageType newPackageType : newType.getPackageTypes()) {
            newPackageTypeDefinitions.put(newPackageType.getName(), newPackageType);
        }

        // Remove all definitions that are in the existing type but not in the new type
        List<PackageType> removedPackageTypes = new ArrayList<PackageType>(existingType.getPackageTypes());
        removedPackageTypes.removeAll(newType.getPackageTypes());
        for (PackageType removedPackageType : removedPackageTypes) {
            existingType.removePackageType(removedPackageType);
            entityManager.remove(removedPackageType);
        }

        // Merge definitions that were already in the existing type and again in the new type
        List<PackageType> mergedPackageTypes = new ArrayList<PackageType>(existingType.getPackageTypes());
        mergedPackageTypes.retainAll(newType.getPackageTypes());

        for (PackageType mergedPackageType : mergedPackageTypes) {
            updatePackageConfigurations(newPackageTypeDefinitions.get(mergedPackageType.getName()), mergedPackageType);
            mergedPackageType.update(newPackageTypeDefinitions.get(mergedPackageType.getName()));
            entityManager.merge(mergedPackageType);
        }

        // Persist all new definitions
        List<PackageType> newPackageTypes = new ArrayList<PackageType>(newType.getPackageTypes());
        newPackageTypes.removeAll(existingType.getPackageTypes());

        for (PackageType newPackageType : newPackageTypes) {
            newPackageType.setResourceType(existingType);
            entityManager.persist(newPackageType);
            existingPackageTypes.add(newPackageType);
        }
    }

    /**
     * Updates the database with new child subcategory definitions found in the new resource type. Any definitions
     * common to both will be merged.
     *
     * @param newType      new resource type containing updated definitions
     * @param existingType old resource type with existing definitions
     */
    private void updateChildSubCategories(ResourceType newType, ResourceType existingType) {
        // we'll do the removal of all definitions that are in the existing type but not in the new type
        // once the child resource types have had a chance to stop referencing any old subcategories

        // Easy case: If the existing type did not have any definitions, simply save the new type defs and return
        if (existingType.getChildSubCategories() == null) {
            for (ResourceSubCategory newSubCategory : newType.getChildSubCategories()) {
                log.info("Metadata update: Adding new child SubCategory [" + newSubCategory.getName()
                    + "] to ResourceType [" + existingType.getName() + "]...");
                existingType.addChildSubCategory(newSubCategory);
                entityManager.persist(newSubCategory);
            }
            return;
        }

        // Merge definitions that were already in the existing type and also in the new type
        //
        // First, put the new subcategories in a map for easier access when iterating over the existing ones
        Map<String, ResourceSubCategory> subCategoriesFromNewType = new HashMap<String, ResourceSubCategory>(newType
            .getChildSubCategories().size());
        for (ResourceSubCategory newSubCategory : newType.getChildSubCategories()) {
            subCategoriesFromNewType.put(newSubCategory.getName(), newSubCategory);
        }

        // Second, loop over the sub categories that need to be merged and update and persist them
        List<ResourceSubCategory> mergedSubCategories = new ArrayList<ResourceSubCategory>(existingType
            .getChildSubCategories());
        mergedSubCategories.retainAll(subCategoriesFromNewType.values());
        for (ResourceSubCategory existingSubCat : mergedSubCategories) {
            updateSubCategory(existingSubCat, subCategoriesFromNewType.get(existingSubCat.getName()));
            entityManager.merge(existingSubCat);
        }

        // Persist all new definitions
        List<ResourceSubCategory> newSubCategories = new ArrayList<ResourceSubCategory>(newType.getChildSubCategories());
        newSubCategories.removeAll(existingType.getChildSubCategories());
        for (ResourceSubCategory newSubCat : newSubCategories) {
            log.info("Metadata update: Adding new child SubCategory [" + newSubCat.getName() + "] to ResourceType ["
                + existingType.getName() + "]...");
            existingType.addChildSubCategory(newSubCat);
            entityManager.persist(newSubCat);
        }
    }

    private void updateSubCategory(ResourceSubCategory existingSubCat, ResourceSubCategory newSubCategory) {
        // update the basic properties
        existingSubCat.update(newSubCategory);

        // we'll do the removal of all child subcategories that are in the existing subcat but not in the new one
        // once the child resource types have had a chance to stop referencing any old subcategories

        // Easy case: If the existing sub category did not have any child sub categories,
        // simply use the ones from the new type
        if ((existingSubCat.getChildSubCategories() == null) || existingSubCat.getChildSubCategories().isEmpty()) {
            for (ResourceSubCategory newChildSubCategory : newSubCategory.getChildSubCategories()) {
                log.debug("Metadata update: Adding new child SubCategory [" + newChildSubCategory.getName()
                    + "] to SubCategory [" + existingSubCat.getName() + "]...");
                existingSubCat.addChildSubCategory(newChildSubCategory);
                entityManager.persist(newChildSubCategory);
            }
            return;
        }

        // Merge definitions that were already in the existing sub cat and also in the new one
        //
        // First, put the new child sub categories in a map for easier access when iterating over the existing ones
        Map<String, ResourceSubCategory> childSubCategoriesFromNewSubCat = new HashMap<String, ResourceSubCategory>(
            newSubCategory.getChildSubCategories().size());
        for (ResourceSubCategory newChildSubCategory : newSubCategory.getChildSubCategories()) {
            childSubCategoriesFromNewSubCat.put(newChildSubCategory.getName(), newChildSubCategory);
        }

        // Second, loop over the sub categories that need to be merged and update and persist them
        List<ResourceSubCategory> mergedChildSubCategories = new ArrayList<ResourceSubCategory>(existingSubCat
            .getChildSubCategories());
        mergedChildSubCategories.retainAll(childSubCategoriesFromNewSubCat.values());
        for (ResourceSubCategory existingChildSubCategory : mergedChildSubCategories) {
            // recursively update childSubCategory
            updateSubCategory(existingChildSubCategory, childSubCategoriesFromNewSubCat.get(existingChildSubCategory
                .getName()));
            entityManager.merge(existingChildSubCategory);
        }

        // Persist all new definitions
        List<ResourceSubCategory> newChildSubCategories = new ArrayList<ResourceSubCategory>(newSubCategory
            .getChildSubCategories());
        newChildSubCategories.removeAll(existingSubCat.getChildSubCategories());
        for (ResourceSubCategory newChildSubCategory : newChildSubCategories) {
            log.info("Metadata update: Adding new child SubCategory [" + newChildSubCategory.getName()
                + "] to SubCategory [" + existingSubCat.getName() + "]...");
            existingSubCat.addChildSubCategory(newChildSubCategory);
            entityManager.persist(newChildSubCategory);
        }
    }

    /**
     * Remove all subcategory definitions that are in the existing type but not in the new type.
     *
     * @param newType      new resource type containing updated definitions
     * @param existingType old resource type with existing definitions
     */
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void removeObsoleteSubCategories(Subject subject, ResourceType newType, ResourceType existingType) {
        // Remove all definitions that are in the existing type but not in the new type
        existingType = entityManager.find(ResourceType.class, existingType.getId());
        List<ResourceSubCategory> removedSubCategories = new ArrayList<ResourceSubCategory>(existingType
            .getChildSubCategories());
        removedSubCategories.removeAll(newType.getChildSubCategories());
        for (ResourceSubCategory removedSubCat : removedSubCategories) {
            // remove it from the resourceType too, so we dont try to persist it again
            // when saving the type
            existingType.getChildSubCategories().remove(removedSubCat);
            entityManager.remove(removedSubCat);
        }

        // now need to recursively remove any child sub categories which no longer appear
        removeChildSubCategories(existingType.getChildSubCategories(), newType.getChildSubCategories());
        entityManager.flush();
    }

    private void removeChildSubCategories(List<ResourceSubCategory> existingSubCategories,
        List<ResourceSubCategory> newSubCategories) {
        // create a map of the new sub categories, for easier retrieval
        Map<String, ResourceSubCategory> mapOfNewSubCategories = new HashMap<String, ResourceSubCategory>(
            newSubCategories.size());
        for (ResourceSubCategory newSubCategory : newSubCategories) {
            mapOfNewSubCategories.put(newSubCategory.getName(), newSubCategory);
        }

        for (ResourceSubCategory existingSubCat : existingSubCategories) {
            // Remove all definitions that are in the existing type but not in the new type
            List<ResourceSubCategory> removedChildSubCategories = new ArrayList<ResourceSubCategory>(existingSubCat
                .getChildSubCategories());
            List<ResourceSubCategory> newChildSubCategories = mapOfNewSubCategories.get(existingSubCat.getName())
                .getChildSubCategories();
            removedChildSubCategories.removeAll(newChildSubCategories);
            for (ResourceSubCategory removedChildSubCat : removedChildSubCategories) {
                // remove subcat and all its children, due to the CASCADE.DELETE
                existingSubCat.removeChildSubCategory(removedChildSubCat);
                entityManager.remove(removedChildSubCat);
            }

            // for any remaining children of this subCat, see if any of their children should be removed
            removeChildSubCategories(existingSubCat.getChildSubCategories(), newChildSubCategories);
        }
    }

    /**
     * updates both configuration definitions on PackageType
     */
    private void updatePackageConfigurations(PackageType newType, PackageType existingType) {
        ConfigurationDefinition newConfigurationDefinition = newType.getDeploymentConfigurationDefinition();
        if (newConfigurationDefinition != null) {
            if (existingType.getDeploymentConfigurationDefinition() == null) {
                // everything new
                entityManager.persist(newConfigurationDefinition);
                existingType.setDeploymentConfigurationDefinition(newConfigurationDefinition);
            } else {
                // update existing
                ConfigurationDefinition existingDefinition = existingType.getDeploymentConfigurationDefinition();
                configurationMetadataManager.updateConfigurationDefinition(newConfigurationDefinition,
                    existingDefinition);
            }
        } else {
            // newDefinition == null
            if (existingType.getDeploymentConfigurationDefinition() != null) {
                existingType.setDeploymentConfigurationDefinition(null);
            }
        }

        newConfigurationDefinition = newType.getPackageExtraPropertiesDefinition();
        if (newConfigurationDefinition != null) {
            if (existingType.getPackageExtraPropertiesDefinition() == null) {
                // everything new
                entityManager.persist(newConfigurationDefinition);
                existingType.setPackageExtraPropertiesDefinition(newConfigurationDefinition);
            } else {
                // update existing
                ConfigurationDefinition existingDefinition = existingType.getPackageExtraPropertiesDefinition();
                configurationMetadataManager.updateConfigurationDefinition(newConfigurationDefinition,
                    existingDefinition);
            }
        } else {
            // newDefinition == null
            if (existingType.getPackageExtraPropertiesDefinition() != null) {
                existingType.setPackageExtraPropertiesDefinition(null);
            }
        }
    }

    /**
     * deals with the content of <resource-configuration>
     */
    private void updateResourceConfiguration(ResourceType newType, ResourceType existingType) {
        ConfigurationDefinition newResourceConfigurationDefinition = newType.getResourceConfigurationDefinition();
        if (newResourceConfigurationDefinition != null) {
            if (existingType.getResourceConfigurationDefinition() == null) // everything new
            {
                entityManager.persist(newResourceConfigurationDefinition);
                existingType.setResourceConfigurationDefinition(newResourceConfigurationDefinition);
            } else {
                ConfigurationDefinition existingDefinition = existingType.getResourceConfigurationDefinition();
                configurationMetadataManager.updateConfigurationDefinition(newResourceConfigurationDefinition,
                    existingDefinition);
            }
        } else // newDefinition == null
        {
            if (existingType.getResourceConfigurationDefinition() != null) {
                existingType.setResourceConfigurationDefinition(null);
            }
        }
    }

    /**
     * Return a set containing those element that are in reference, but not in first. Both input sets are not modified
     *
     * @param  <T>
     * @param  first
     * @param  reference
     *
     * @return
     */
    private <T> Set<T> missingInFirstSet(Set<T> first, Set<T> reference) {
        Set<T> result = new HashSet<T>();

        if (reference != null) {
            // First collection is null -> everything is missing
            if (first == null) {
                result.addAll(reference);
                return result;
            }

            // else loop over the set and sort out the right items.
            for (T item : reference) {
                //                if (!first.contains(item)) {
                //                    result.add(item);
                //                }
                boolean found = false;
                Iterator<T> iter = first.iterator();
                while (iter.hasNext()) {
                    T f = iter.next();
                    if (f.equals(item)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    result.add(item);
            }
        }

        return result;
    }

    /**
     * Return a new Set with elements that are in the first and second passed collection.
     * If one set is null, an empty Set will be returned.
     * @param  <T>    Type of set
     * @param  first  First set
     * @param  second Second set
     *
     * @return a new set (depending on input type) with elements in first and second
     */
    private <T> Set<T> intersection(Set<T> first, Set<T> second) {
        Set<T> result = new HashSet<T>();
        if ((first != null) && (second != null)) {
            result.addAll(first);
            //            result.retainAll(second);
            Iterator<T> iter = result.iterator();
            boolean found;
            while (iter.hasNext()) {
                T item = iter.next();
                found = false;
                for (T s : second) {
                    if (s.equals(item))
                        found = true;
                }
                if (!found)
                    iter.remove();
            }
        }

        return result;
    }
}
