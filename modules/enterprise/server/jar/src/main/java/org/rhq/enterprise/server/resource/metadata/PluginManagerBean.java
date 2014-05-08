package org.rhq.enterprise.server.resource.metadata;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import org.jboss.ejb3.annotation.TransactionTimeout;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.PluginCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.plugin.CannedGroupExpression;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.CannedGroupAddition;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.inventory.InventoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.core.plugin.PluginAdditionsReader;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.QuartzUtil;

@Stateless
public class PluginManagerBean implements PluginManagerLocal, PluginManagerRemote {

    private final Log log = LogFactory.getLog(PluginManagerBean.class);

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private ResourceMetadataManagerLocal resourceMetadataManager;

    @EJB
    private PluginManagerLocal pluginMgr;

    @EJB
    private InventoryManagerLocal inventoryMgr;

    @EJB
    private ResourceTypeManagerLocal resourceTypeMgr;

    @EJB
    private ResourceManagerLocal resourceMgr;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @EJB
    private ContentManagerLocal contentManager;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private SchedulerLocal scheduler;

    @Override
    public Plugin getPlugin(String name) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
        query.setParameter("name", name);
        Plugin result = null;
        try {
            result = (Plugin) query.getSingleResult();
        } catch (NoResultException e) {
            result = null;
        }

        return result;
    }

    @Override
    public boolean isReadyForPurge(Plugin plugin) {
        int resourceTypeCount = getResourceTypeCount(plugin);
        if (resourceTypeCount > 0) {
            if (log.isDebugEnabled()) {
                log.debug(plugin + " is not ready to be purged. It still has " + resourceTypeCount
                    + " resource types in the database.");
            }
            return false;
        }

        //check that all the servers have acked the deletion
        Plugin inDbPlugin = entityManager.find(Plugin.class, plugin.getId());

        @SuppressWarnings("unchecked")
        List<Server> allServers = entityManager.createNamedQuery(Server.QUERY_FIND_ALL).getResultList();

        for (Server s : allServers) {
            if (!inDbPlugin.getServersAcknowledgedDelete().contains(s)) {
                if (log.isDebugEnabled()) {
                    log.debug(plugin + " is not ready to be purged. Server " + s +
                        " has not acknowledged it knows about its deletion.");
                }
                return false;
            }
        }

        return true;
    }

    private int getResourceTypeCount(Plugin plugin) {
        // this will get all types, even those deleted and ignored
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterPluginName(plugin.getName());
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        criteria.addFilterDeleted(null);
        criteria.addFilterIgnored(null);
        criteria.setStrict(true);

        PageList<ResourceType> types = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(), criteria);

        return types.getTotalSize();
    }

    @Override
    public void purgePlugins(List<Plugin> plugins) {
        for(Plugin p : plugins) {
            Plugin inDb = entityManager.find(Plugin.class, p.getId());
            inDb.getServersAcknowledgedDelete().clear();
            entityManager.remove(inDb);
        }

        if (log.isDebugEnabled()) {
            log.debug("The following plugins were purged from the database: " + plugins);
        }
    }

    @Override
    public List<Plugin> getPlugins() {
        return entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL).getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Plugin> getInstalledPlugins() {
        Query q = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_INSTALLED);
        return q.getResultList();
    }

    @Override
    public List<Plugin> findAllDeletedPlugins() {
        return entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_DELETED).getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Plugin> getAllPluginsById(List<Integer> pluginIds) {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<Plugin>(); // nothing to do
        }
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_BY_IDS);
        query.setParameter("ids", pluginIds);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName, ResourceCategory resourceCategory) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_RESOURCE_TYPE_AND_CATEGORY);
        query.setParameter("resourceTypeName", resourceTypeName);
        query.setParameter("resourceCategory", resourceCategory);
        List<Plugin> results = query.getResultList();
        return results;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }

        // we need to make sure that if a plugin is enabled, all of its dependencies are enabled
        PluginDependencyGraph graph = getPluginMetadataManager().buildDependencyGraph();
        List<Plugin> allPlugins = getInstalledPlugins();
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
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds == null || pluginIds.size() == 0) {
            return; // nothing to do
        }

        // we need to make sure that if a plugin is disabled, no other plugins that depend on it are enabled
        PluginDependencyGraph graph = getPluginMetadataManager().buildDependencyGraph();
        List<Plugin> allPlugins = getInstalledPlugins();
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
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        if (pluginIds.isEmpty()) {
            return;
        }

        PluginDependencyGraph graph = getPluginMetadataManager().buildDependencyGraph();
        List<Plugin> allPlugins = getInstalledPlugins();
        Set<String> pluginsToDelete = new HashSet<String>();

        for (Integer pluginId : pluginIds) {
            Plugin plugin = getPluginFromListById(allPlugins, pluginId.intValue());
            if (plugin != null && plugin.getStatus().equals(PluginStatusType.INSTALLED)) {
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

        // In order to avoid a large transaction issue, when deleting one or more plugins with possibly quite a large
        // resource population, perform in multiple transactions, being mindful of consistency.  Because of plugin
        // dependency, it's important that minimally all of the plugins and related types are *marked* for
        // deletion in a single transaction.  This prevents inconsistent plugin state and the method is considered a
        // success if we commit that transaction. After that, again trying to avoid an overly large umbrella
        // transaction, we mark the relevant resources for uninventory.  If that fails it will be detected by the
        // PurgeResourceTypesJob and rectified later.

        List<Plugin> plugins = pluginMgr.getAllPluginsById(pluginIds);

        // Do this in its own transaction to ensure everything gets marked.
        pluginMgr.markPluginsDeleted(subject, plugins);

        // Now, try and uninventory the resource of doomed plugin types
        try {
            for (Plugin plugin : plugins) {
                deleteResourcesForPlugin(subject, plugin);
            }

        } catch (Throwable t) {
            log.warn(
                "Failed to uninventory all resources of deleted plugins. This should fix itself automatically when the PurgeResourceTypsJob executes.",
                t);
        }
        GroupDefinitionManagerLocal groupDefMgr = LookupUtil.getGroupDefinitionManager();
        for (Plugin plugin : plugins) {
            groupDefMgr.updateGroupsByCannedExpressions(plugin.getName(), null);
        }
    }

    private void deleteResourcesForPlugin(Subject subject, Plugin plugin) throws Exception {
        // Uninventory all of the top level resources for the plugin's deleted types. The children go away automatically
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.setStrict(true);
        criteria.addFilterPluginName(plugin.getName());
        criteria.addFilterDeleted(true); // get all deleted types ...
        criteria.addFilterIgnored(null); // ... whether they are ignored or not
        criteria.addFilterParentResourceTypesEmpty(true);
        criteria.clearPaging();

        List<ResourceType> deletedServerTypes = resourceTypeMgr.findResourceTypesByCriteria(subject, criteria);

        // Do this type by type in an effort to keep chunks smaller. 
        for (ResourceType deletedServerType : deletedServerTypes) {
            deleteResourcesForType(subject, deletedServerType);
        }
    }

    private void deleteResourcesForType(Subject subject, ResourceType type) throws Exception {

        List<Integer> typeIds = new ArrayList<Integer>(1);
        typeIds.add(type.getId());

        List<Integer> resourceIds = resourceMgr.findIdsByTypeIds(typeIds);
        for (Integer resourceId : resourceIds) {
            resourceMgr.uninventoryResourceInNewTransaction(resourceId);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void markPluginsDeleted(Subject subject, List<Plugin> plugins) throws Exception {

        log.debug(subject + " preparing to delete the following plugins: " + plugins);

        for (Plugin plugin : plugins) {
            if (plugin.getStatus().equals(PluginStatusType.INSTALLED)) {
                long startTime = System.currentTimeMillis();
                List<Integer> resourceTypeIds = resourceTypeMgr.getResourceTypeIdsByPlugin(plugin.getName());

                inventoryMgr.markTypesDeleted(resourceTypeIds, false);

                plugin.setStatus(PluginStatusType.DELETED);
                entityManager.merge(plugin);

                long endTime = System.currentTimeMillis();
                log.debug("Deleted " + plugin + " in " + (endTime - startTime) + " ms");

            } else {
                log.debug("Skipping " + plugin + ". It is already deleted.");
            }
        }
    }

    @Override
    public List<PluginStats> getPluginStats(List<Integer> pluginIds) {
        List<PluginStats> stats = new ArrayList<PluginStats>();
        List<Plugin> plugins = getAllPluginsById(pluginIds);

        for (Plugin plugin : plugins) {
            List<Integer> resourceTypeIds = resourceTypeMgr.getResourceTypeIdsByPlugin(plugin.getName());
            Integer resourceCount = resourceMgr.getResourceCount(resourceTypeIds);
            stats.add(new PluginStats(plugin, resourceTypeIds.size(), resourceCount));
        }

        return stats;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void setPluginEnabledFlag(Subject subject, int pluginId, boolean enabled) throws Exception {
        Query q = entityManager.createNamedQuery(Plugin.UPDATE_PLUGIN_ENABLED_BY_ID);
        q.setParameter("id", pluginId);
        q.setParameter("enabled", Boolean.valueOf(enabled));
        q.executeUpdate();
        log.info((enabled ? "Enabling" : "Disabling") + " plugin [" + pluginId + "]");
        return;
    }

    @Override
    public File getPluginDropboxDirectory() {
        File dir = new File(LookupUtil.getPluginDeploymentScanner().getUserPluginDir());
        return dir;
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
    // few reasons. Registering the plugin and removing obsolete types are performed in different, subsequent,
    // transactions. The registration may update types, and that locks various rows of the database. Those rows
    // must be unlocked before obsolete type removal.  Type removal executes (resource) bulk delete under the covers,
    // and that will deadlock with the rows locked by the type update (at least in oracle) if performed in the same
    // transaction.  Furthermore, as mentioned, obsolete type removal removes resources of the obsolete type. We
    // need to avoid an umbrella transaction for the type removal because large inventories of obsolete resources
    // will generate very large transactions. Potentially resulting in timeouts or other issues.
    @TransactionAttribute(TransactionAttributeType.NEVER)
    @Override
    public void registerPlugin(Plugin plugin, PluginDescriptor pluginDescriptor, File pluginFile, boolean forceUpdate)
        throws Exception {

        if (isDeleted(plugin)) {
            String msg = "A deleted version of " + plugin + " already exists in the database. The plugin cannot be "
                + "installed until the deleted version is purged from the database (which should happen a couple of" +
                " minutes after all servers acknowledged the plugin was deleted). Especially, the plugin won't be" +
                " purged if ANY of the servers in the HA cloud have been down at the point in time the plugin was" +
                " deleted and haven't gone back up yet.";
            log.warn(msg);
            throw new IllegalStateException(msg);
        }

        log.debug("Registering " + plugin + "...");
        long startTime = System.currentTimeMillis();

        boolean newOrUpdated = pluginMgr.installPluginJar(plugin, pluginDescriptor, pluginFile);
        boolean typesUpdated = pluginMgr.registerPluginTypes(plugin.getName(), pluginDescriptor, newOrUpdated,
            forceUpdate);

        if (typesUpdated) {
            // There may be other types in other plugins that extended from this plugin (the "embedded" extension mechanism).
            // In this case, we have to re-deploy those plugins so those types get recreated with the new metadata.
            // We do the same thing with the extended plugins that we will do with our passed-in "parent" plugin, that is,
            // we register the extended plugins' types (we'll force it to update types since we know something changed in the parent)
            // and then we'll remove any obsoleted types from the extended plugins.
            PluginMetadataManager metadataManager = getPluginMetadataManager();
            Map<String, PluginDescriptor> extensions = metadataManager.getEmbeddedExtensions(plugin.getName());
            if (extensions != null && extensions.size() > 0) {
                for (Map.Entry<String, PluginDescriptor> entry : extensions.entrySet()) {
                    String extPluginName = entry.getKey();
                    PluginDescriptor extPluginDescriptor = entry.getValue();
                    log.debug("Plugin [" + extPluginName
                        + "] will be re-registered because it embeds types from plugin [" + plugin.getName() + "]");
                    pluginMgr.registerPluginTypes(extPluginName, extPluginDescriptor, false, true);
                    resourceMetadataManager.removeObsoleteTypes(subjectMgr.getOverlord(), extPluginName,
                        metadataManager);
                }
            }

            // now remove any obsolete types from the newly registered plugin
            resourceMetadataManager.removeObsoleteTypes(subjectMgr.getOverlord(), plugin.getName(), metadataManager);
        }

        long endTime = System.currentTimeMillis();
        log.debug("Finished registering " + plugin + " in " + (endTime - startTime) + " ms");
    }

    private boolean isDeleted(Plugin plugin) {
        for (Plugin deletedPlugins : findAllDeletedPlugins()) {
            if (deletedPlugins.equals(plugin)) {
                return true;
            }
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public boolean installPluginJar(Plugin newPlugin, PluginDescriptor pluginDescriptor, File pluginFile)
        throws Exception {

        Plugin existingPlugin = getPlugin(newPlugin.getName());
        boolean newOrUpdated = (null == existingPlugin);

        if (existingPlugin != null) {
            Plugin obsolete = AgentPluginDescriptorUtil.determineObsoletePlugin(newPlugin, existingPlugin);
            if (obsolete == existingPlugin) { // yes, use == for reference equality
                newOrUpdated = true;
            }
            newPlugin.setId(existingPlugin.getId());
            newPlugin.setEnabled(existingPlugin.isEnabled());
        }

        // If this is a brand new plugin, it gets "updated" too.
        if (newOrUpdated) {
            if (newPlugin.getDisplayName() == null) {
                newPlugin.setDisplayName(newPlugin.getName());
            }

            newPlugin = updatePluginExceptContent(newPlugin);
            if (pluginFile != null) {
                entityManager.flush();
                streamPluginFileContentToDatabase(newPlugin.getId(), pluginFile);
            }
            log.debug("Updated plugin entity [" + newPlugin + "]");
        }
        return newOrUpdated;
    }

    @Override
    @TransactionTimeout(1800)
    public boolean registerPluginTypes(String newPluginName, PluginDescriptor pluginDescriptor, boolean newOrUpdated,
        boolean forceUpdate) throws Exception {
        boolean typesUpdated = false;

        PluginMetadataManager metadataManager = getPluginMetadataManager();

        if (newOrUpdated || forceUpdate || !metadataManager.getPluginNames().contains(newPluginName)) {
            Set<ResourceType> rootResourceTypes = metadataManager.loadPlugin(pluginDescriptor);
            if (rootResourceTypes == null) {
                throw new Exception("Failed to load plugin [" + newPluginName + "].");
            }
            if (newOrUpdated || forceUpdate) {
                // Only merge the plugin's ResourceTypes into the DB if the plugin is new or updated or we were forced to
                resourceMetadataManager.updateTypes(rootResourceTypes);
                typesUpdated = true;
            }
        }

        return typesUpdated;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void update(Subject subject) throws Exception {
        PluginDeploymentScannerMBean scanner = LookupUtil.getPluginDeploymentScanner();
        scanner.scanAndRegister();
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public String schedulePluginUpdateOnAgents(Subject subject, long delayInMilliseconds) throws Exception {
        JobDetail jobDetail = UpdatePluginsOnAgentsJob.getJobDetail();
        Trigger trigger = QuartzUtil.getFireOnceOffsetTrigger(jobDetail, delayInMilliseconds);
        try {
            scheduler.scheduleJob(jobDetail, trigger);

            return jobDetail.getName();
        } catch (ObjectAlreadyExistsException e) {
            //well, there already is a plugin update job scheduled, so let's just not add another one.
            log.debug("A request to update plugins on agents seems to already be scheduled." +
                " Ignoring the current request with the error message: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public boolean isPluginUpdateOnAgentsFinished(Subject subject, String handle) {
        try {
            return scheduler.getJobDetail(handle, UpdatePluginsOnAgentsJob.class.getName()) == null;
        } catch (SchedulerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve job details while checking for active plugin update schedule, code: " + e.getErrorCode(), e);
            } else {
                log.warn("Failed to retrieve job details while checking for active plugin update schedule, code: " + e.getErrorCode() + ", message: " + e.getMessage());
            }

            return false;
        }
    }
    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public List<Plugin> deployUsingBytes(Subject subject, String pluginJarName, byte[] pluginJarBytes) throws Exception {
        File base = getPluginDropboxDirectory();

        File targetFile = new File(base, pluginJarName);
        FileOutputStream out = new FileOutputStream(targetFile);
        ByteArrayInputStream in = new ByteArrayInputStream(pluginJarBytes);

        StreamUtil.copy(in, out, true);
        return updateAndDetectChanges(subject);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public List<Plugin> deployUsingContentHandle(Subject subject, String pluginJarName, String handle) throws Exception {
        File pluginJar = contentManager.getTemporaryContentFile(handle);
        File base = getPluginDropboxDirectory();

        FileUtil.copyFile(pluginJar, new File(base, pluginJarName));

        return updateAndDetectChanges(subject);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<Plugin> findPluginsByCriteria(Subject subject, PluginCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);

        CriteriaQueryRunner<Plugin> queryRunner = new CriteriaQueryRunner<Plugin>(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    private List<Plugin> updateAndDetectChanges(Subject subject) throws Exception {
        List<Plugin> before = getPlugins();

        update(subject);

        List<Plugin> after = getPlugins();

        for (Plugin p : before) {
            after.remove(p);
        }

        return after;
    }

    public void acknowledgeDeletedPluginsBy(int serverId) {
        Query q = entityManager.createNamedQuery(Plugin.QUERY_UNACKED_DELETED_PLUGINS);
        q.setParameter("serverId", serverId);

        @SuppressWarnings("unchecked")
        List<Plugin> plugins = (List<Plugin>) q.getResultList();

        Server server = entityManager.find(Server.class, serverId);

        for (Plugin p : plugins) {
            p.getServersAcknowledgedDelete().add(server);
            entityManager.merge(p);
        }
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

    /**
     * Returns the metadata manager that will be used to obtain the resource types from
     * descriptors.
     *
     * @return metadata manager
     */
    private PluginMetadataManager getPluginMetadataManager() {
        return LookupUtil.getPluginDeploymentScanner().getPluginMetadataManager();
    }

    /**
     * gets canned expressions from all plugins by direct reading and parsing additional descriptors;
     */
    public List<CannedGroupExpression> getCannedGroupExpressions() {
        ArrayList<CannedGroupExpression> list = new ArrayList<CannedGroupExpression>();
        String pluginDir = LookupUtil.getPluginDeploymentScanner().getAgentPluginDir();
        long now = System.currentTimeMillis();
        log.debug("Reading canned expressions from all agent plugin jars");
        for (Plugin plugin : this.getInstalledPlugins()) {
            if (plugin.getDeployment().equals(PluginDeploymentType.AGENT)) {
                File pluginFile = new File(pluginDir, plugin.getPath());
                try {
                    URL pluginUrl = pluginFile.toURI().toURL();
                    CannedGroupAddition addition = PluginAdditionsReader.getCannedGroupsAddition(pluginUrl, plugin.getName());
                    if (addition != null) {
                        list.addAll(addition.getExpressions());
                    }
                } catch (Exception e) {
                    log.error("Failed to parse plugin addition found in plugin [" + pluginFile.getAbsolutePath() + "]",e);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Reading "+list.size()+" canned expressions from all plugins took "+(System.currentTimeMillis()-now)+"ms");
        }
        return list;
    }
}
