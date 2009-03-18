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
package org.rhq.enterprise.server.resource.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.agent.metadata.SubCategoriesMetadataParser;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

/**
 * This class manages the metadata for resources. Plugins are registered against this bean so that their metadata can be
 * pulled out and stored as necessary.
 *
 * @author Greg Hinkle
 * @author Heiko W. Rupp
 * @author John Mazzitelli
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
    private EventManagerLocal eventManager;

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
     * Returns the information on all plugins as found in the database.
     */
    public List<Plugin> getPlugins() {
        Query q = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL);
        return q.getResultList();
    }

    public void registerPlugin(Plugin plugin, PluginDescriptor pluginDescriptor, File pluginFile) throws Exception {
        // TODO GH: Consider how to remove features from plugins in updates without breaking everything
        Plugin existingPlugin = null;
        boolean newOrUpdated = false;
        try {
            existingPlugin = getPlugin(plugin.getName());
        } catch (NoResultException nre) {
            newOrUpdated = true; // this is expected for new plugins
        }

        if (existingPlugin != null) {
            if (!plugin.getMd5().equals(existingPlugin.getMd5())) {
                newOrUpdated = true;
            }
            plugin.setId(existingPlugin.getId());
        }

        // Notice that we only ever look at MD5 to determine if plugin entity needs to be updated.
        // If this is a brand new plugin, it gets "updated" too - which ends up being a simple persist.
        if (newOrUpdated) {
            if (plugin.getDisplayName() == null) {
                plugin.setDisplayName(plugin.getName());
            }

            plugin = updatePluginExceptContent(plugin);
            if (pluginFile != null) {
                entityManager.flush();
                streamPluginFileContentToDatabase(plugin.getName(), pluginFile);
            }
            log.debug("Updated plugin entity [" + plugin + "]");
        }

        if (newOrUpdated || !PLUGIN_METADATA_MANAGER.getPluginNames().contains(plugin.getName())) {
            Set<ResourceType> rootResourceTypes = PLUGIN_METADATA_MANAGER.loadPlugin(pluginDescriptor);
            if (rootResourceTypes == null) {
                throw new Exception("Failed to load plugin [" + plugin.getName() + "].");
            }
            if (newOrUpdated) {
                // Only merge the plugin's ResourceTypes into the DB if the plugin is new or updated.
                updateTypes(plugin.getName(), rootResourceTypes);
            }
        }

        // TODO GH: JBNADM-1310 - Push updated plugins to running agents and have them reboot their PCs
        // See also JBNADM-1630
        return;
    }

    private Plugin updatePluginExceptContent(Plugin plugin) throws Exception {
        // this method is here because we need a way to update the plugin's information
        // without blowing away the content data. Because we do not want to load the
        // content blob in memory, the plugin's content field will be null - if we were
        // to entityManager.merge that plugin POJO, it would null out that blob column.
        if (plugin.getId() == 0) {
            entityManager.persist(plugin);
        } else {
            Query q = entityManager.createNamedQuery(Plugin.UPDATE_ALL_BUT_CONTENT);
            q.setParameter("id", plugin.getId());
            q.setParameter("name", plugin.getName());
            q.setParameter("path", plugin.getPath());
            q.setParameter("displayName", plugin.getDisplayName());
            q.setParameter("enabled", plugin.isEnabled());
            q.setParameter("md5", plugin.getMD5());
            q.setParameter("version", plugin.getVersion());
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
     * @param name the name of the plugin whose content is being updated
     * @param file the plugin file whose content will be streamed to the database
     *
     * @throws Exception
     */
    private void streamPluginFileContentToDatabase(String name, File file) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        FileInputStream fis = new FileInputStream(file);

        try {
            conn = this.dataSource.getConnection();
            ps = conn.prepareStatement("UPDATE " + Plugin.TABLE_NAME + " SET CONTENT = ? WHERE NAME = ?");
            ps.setBinaryStream(1, new BufferedInputStream(fis), (int) file.length());
            ps.setString(2, name);
            int updateResults = ps.executeUpdate();
            if (updateResults != 1) {
                throw new Exception("Failed to update content for plugin [" + name + "] from [" + file + "]");
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

    private void updateTypes(String pluginName, Set<ResourceType> rootResourceTypes) throws Exception {
        for (ResourceType rootResourceType : rootResourceTypes) {
            updateType(rootResourceType);
        }
        removeObsoleteTypes(pluginName);
    }

    @SuppressWarnings("unchecked")
    private void removeObsoleteTypes(String pluginName) {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_PLUGIN);
        query.setParameter("plugin", pluginName);
        List<ResourceType> existingTypes = query.getResultList();

        if (existingTypes != null) {
            Subject overlord = subjectManager.getOverlord();
            for (Iterator<ResourceType> iter = existingTypes.iterator(); iter.hasNext();) {
                ResourceType existingType = iter.next();
                if (PLUGIN_METADATA_MANAGER.getType(existingType.getName(), existingType.getPlugin()) == null) {
                    if (entityManager.contains(existingType))
                        entityManager.refresh(existingType);
                    // This type no longer exists
                    removeFromParents(existingType);
                    // clean out the measurement stuff hanging on the type;
                    List<Resource> resources = existingType.getResources();
                    if (resources != null) {
                        Iterator<Resource> resIter = resources.iterator();
                        while (resIter.hasNext()) {
                            Resource res = resIter.next();
                            resourceManager.deleteResource(overlord, res.getId());
                            resIter.remove();
                        }
                    }
                    entityManager.flush();
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
                    // TODO clean out event definitions ?
                    entityManager.remove(existingType);
                    entityManager.flush();
                    iter.remove();
                }
            }

            // finally, its safe to remove any existing subcategories
            for (ResourceType remainingType : existingTypes) {
                ResourceType updateType = PLUGIN_METADATA_MANAGER.getType(remainingType.getName(), remainingType
                    .getPlugin());

                // if we've got a type from the descriptor which matches an existing one
                // then lets see if we need to remove any subcategories from the existing one
                if (updateType != null) {
                    removeSubCategories(updateType, remainingType);
                    entityManager.flush();
                }
            }
        }
    }

    private void removeFromParents(ResourceType typeToBeRemoved) {
        // Wrap in new HashSet to avoid ConcurrentModificationExceptions.
        Set<ResourceType> parents = new HashSet<ResourceType>(typeToBeRemoved.getParentResourceTypes());
        for (ResourceType parent : parents) {
            parent.removeChildResourceType(typeToBeRemoved);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateType(ResourceType resourceType) {
        try {
            entityManager.flush();

            // see if there is already an existing type that we need to update
            if (log.isDebugEnabled()) {
                log.debug("Updating resource type [" + resourceType.getName() + "] from plugin ["
                    + resourceType.getPlugin() + "]...");
            }

            Query q = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN);
            q.setParameter("name", resourceType.getName()).setParameter("plugin", resourceType.getPlugin());
            List<ResourceType> findTypeByNameAndPlugin = q.getResultList();

            ResourceType existingType = null;
            if (findTypeByNameAndPlugin.size() == 1) {
                existingType = findTypeByNameAndPlugin.get(0);
            }

            // Connect the parent types if they exist which they should
            // We'll do this no matter if the resourceType exists or not - but we use existing vs. resourceType appropriately
            // This is to support the case when an existing type gets a new parent resource type in <runs-inside>
            updateParentResourceTypes(resourceType, existingType);

            if (findTypeByNameAndPlugin.size() == 0) {
                throw new NoResultException(); // falls into the catch block down below. TODO refactor this stuff
            }

            // XXX: I do not think the code in this if-block is valid - see RHQ-1086 - I think it should be removed
            // see what we have. Default is 0 or 1. When hot deploying and moving a RT 2 can happen
            if (findTypeByNameAndPlugin.size() > 1) {
                // No Unique result. List what we have and bail out if more than 2 results
                if (log.isDebugEnabled()) {
                    for (ResourceType rType : findTypeByNameAndPlugin) {
                        log.debug("updateType: found: " + rType);
                    }
                }

                if (findTypeByNameAndPlugin.size() != 2) {
                    throw new IllegalArgumentException("We only expected two results here, but got "
                        + findTypeByNameAndPlugin.size());
                }

                // two results - see if one is us, then the other must be the existing type that we are looking for
                Iterator<ResourceType> iter = findTypeByNameAndPlugin.iterator();
                ResourceType rt1 = iter.next();
                ResourceType rt2 = iter.next();
                if (rt1.equals(resourceType)) {
                    existingType = rt2;
                } else if (rt2.equals(resourceType)) {
                    existingType = rt1;
                } else {
                    throw new IllegalArgumentException("Existing ResourceType not found for " + resourceType);
                }
            }

            // Type exists now, (if it didn't exist we'd fall through to the NSRE catch block and cascade persist everything

            // first add/update any subcategories on the parent before trying to update children
            // if we didn't do this the children may try to save themselves with subcategories which
            // wouldn't exist yet
            updateChildSubCategories(resourceType, existingType);

            // update children next
            for (ResourceType childType : resourceType.getChildResourceTypes()) {
                updateType(childType);
            }
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

            /*
             * We need to be careful updating the subcategory. If it is not null and the same ("equals")
             * to the new one, we need to copy over the attributes, as the existing will be kept and
             * the new one not persisted. Otherwise, we can just use the new one.
             */
            ResourceSubCategory oldSubCat = existingType.getSubCategory();
            ResourceSubCategory newSubCat = resourceType.getSubCategory();
            if (oldSubCat != null && oldSubCat.equals(newSubCat)) {
                // Subcategory hasn't changed - nothing to do (call to addAndUpdateChildSubCategories()
                // above already took care of any modifications to the ResourceSubCategorys themselves).
            } else if (newSubCat == null) {
                if (oldSubCat != null) {
                    log.debug("Metadata update: Subcategory of ResourceType [" + resourceType.getName()
                        + "] changed from " + oldSubCat + " to " + newSubCat);
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
        } catch (NoResultException nre) {
            /*
             * If the type didn't exist then we'll persist here which will cascade through
             * all child types as well as plugin and resource configs and their delegate types and
             * metric and operation definitions and their dependent types
             *
             * But first do some validity checking
             */

            // Check if the subcategories as children of resourceType are valid
            // Those are the subtacegories we offer for children of us
            checkForValidSubcategories(resourceType.getChildSubCategories());

            // Check if we have a subcategory attached that needs to be linked to one of the parents
            // This is a subcategory of our parent where we are supposed to be grouped in.
            linkSubCategoryToParents(resourceType);

            if (log.isDebugEnabled())
                log.debug("Persisting new ResourceType: " + resourceType);
            entityManager.persist(resourceType);
        } catch (NonUniqueResultException nure) {
            log.debug("Found more than one existing ResourceType for " + resourceType);
            throw new IllegalStateException(nure);
        }
    }

    private void linkSubCategoryToParents(ResourceType resourceType) {

        if (resourceType.getSubCategory()==null)
            return; // Nothing to do

        ResourceSubCategory mySubCategory = resourceType.getSubCategory();
        ResourceSubCategory existingCat = SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
                resourceType, mySubCategory.getName());
        if (existingCat!=null)
            resourceType.setSubCategory(existingCat);
        else
            throw new IllegalStateException("Subcategory " + mySubCategory.getName() + " defined on resource type " +
            resourceType.getName() + " in plugin " + resourceType.getPlugin() + " is not defined in a parent type");

    }

    private void updateParentResourceTypes(ResourceType resourceType, ResourceType existingType) {
        Set<ResourceType> types = new HashSet<ResourceType>(resourceType.getParentResourceTypes());
        resourceType.setParentResourceTypes(new HashSet<ResourceType>());
        for (ResourceType resourceTypeParent : types) {
            try {
                ResourceType realParentType = (ResourceType) entityManager.createNamedQuery(
                    ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN).setParameter("name", resourceTypeParent.getName())
                    .setParameter("plugin", resourceTypeParent.getPlugin()).getSingleResult();
                realParentType.addChildResourceType(existingType != null ? existingType : resourceType);
            } catch (NoResultException nre) {
                throw new RuntimeException("Couldn't persist type [" + resourceType
                    + "] because parent wasn't already persisted [" + resourceTypeParent + "]");
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

    /** Update the &lt;event> tags */
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
     *
     * @param resourceType
     * @param existingType
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
                configurationMetadataManager.updateConfigurationDefinition(resourceType
                    .getPluginConfigurationDefinition(), existingConfigurationDefinition);
            }
        } else {
            // resourceType.getPlu... is null -> remove the existing config
            if (existingConfigurationDefinition != null) {
                existingType.setPluginConfigurationDefinition(null);
                entityManager.remove(existingConfigurationDefinition);
            }
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

                // TODO send updates to agents ?
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
                log.debug("Metadata update: Adding new child SubCategory [" + newSubCategory.getName()
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
            log.debug("Metadata update: Adding new child SubCategory [" + newSubCat.getName() + "] to ResourceType ["
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
            log.debug("Metadata update: Adding new child SubCategory [" + newChildSubCategory.getName()
                + "] to SubCategory [" + existingSubCat.getName() + "]...");
            existingSubCat.addChildSubCategory(newChildSubCategory);
            entityManager.persist(newChildSubCategory);
        }
    }

    /**
     * Remove all sub category definitions that are in the existing type but not in the new type
     *
     * @param newType      new resource type containing updated definitions
     * @param existingType old resource type with existing definitions
     */
    private void removeSubCategories(ResourceType newType, ResourceType existingType) {
        // Remove all definitions that are in the existing type but not in the new type
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
                // remove  subcat and all its children, due to the CASCADE.DELETE
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