package org.rhq.enterprise.server.resource.metadata;

import static java.util.Arrays.asList;

import java.util.List;

import javax.ejb.EJBException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScanner;
import org.rhq.enterprise.server.inventory.InventoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob;
import org.rhq.enterprise.server.scheduler.jobs.PurgeResourceTypesJob;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Unit tests for {@link PluginManagerBean}.
 */
@Test(groups = { "plugin.metadata", "PluginManagerBean" }, priority = 100000)
public class PluginManagerBeanTest extends MetadataBeanTest {

    private SubjectManagerLocal subjectMgr;
    private PluginManagerLocal pluginMgr;

    // Our test class wants to deploy plugins that other test methods use.
    // We need to keep intact our plugin scanner throughout our methods
    // so we keep our own copy here.
    private PluginDeploymentScanner pluginScanner;

    @AfterClass
    public void afterClass() throws Exception {
        super.afterClass();
        this.pluginScanner = null;
    }

    @Override
    protected void preparePluginScannerService() {
        if (this.pluginScanner == null) {
            this.pluginScanner = new PluginDeploymentScanner();
        }
        super.preparePluginScannerService(this.pluginScanner);
    }

    public void registerPlugins() throws Exception {
        subjectMgr = LookupUtil.getSubjectManager();
        pluginMgr = LookupUtil.getPluginManager();

        String query = "from Plugin where name IN ('PluginManagerBeanTestPlugin1', 'PluginManagerBeanTestPlugin2', " +
            "'PluginManagerBeanTestPlugin3', 'PluginManagerBeanTestPlugin3.1')";
        List<Plugin> plugins = getEntityManager().createQuery(query).getResultList();
        if (!plugins.isEmpty()) {
            System.out.println("Purging plugins " + plugins + "...");
            for (Plugin plugin : plugins) {
                pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
                pluginMgr.markPluginsForPurge(subjectMgr.getOverlord(), asList(plugin.getId()));
            }
            new PurgeResourceTypesJob().execute(null);
            new PurgePluginsJob().execute(null);
        }

        createPlugin("test-plugin1", "1.0", "plugin_1.xml");
        createPlugin("test-plugin2", "1.0", "plugin_2.xml");
        createPlugin("test-plugin3", "1.0", "plugin_3.xml");
        createPlugin("test-plugin3.1", "1.0", "plugin_3.1.xml");
    }

    @Test(dependsOnMethods = { "registerPlugins" })
    public void disablePlugin() throws Exception {
        Plugin plugin = getPlugin("PluginManagerBeanTestPlugin3");

        pluginMgr.disablePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
        plugin = pluginMgr.getPlugin("PluginManagerBeanTestPlugin3");

        assertFalse("Failed to disable plugin", plugin.isEnabled());
    }

    @Test(dependsOnMethods = { "registerPlugins" })
    public void doNotDisablePluginIfDependentPluginsAreNotAlsoDisabled() throws Exception {
        Plugin plugin = getPlugin("PluginManagerBeanTestPlugin1");
        EJBException exception = null;

        try {
            pluginMgr.disablePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
        } catch (EJBException e) {
            exception = e;
        }

        assertNotNull("Expected exception to be thrown when trying to disable a plugin that has dependent plugins",
            exception);
        assertTrue("Expected an IllegalArgumentException when trying to disable a plugin with dependent plugins",
            exception.getCausedByException() instanceof IllegalArgumentException);
    }

    @Test(dependsOnMethods = { "doNotDisablePluginIfDependentPluginsAreNotAlsoDisabled" })
    public void disablePluginAndDependentPlugins() throws Exception {
        Plugin plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        Plugin plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        pluginMgr.disablePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        assertFalse("Failed to disable plugin", plugin1.isEnabled());
        assertFalse("Failed to disable plugin", plugin2.isEnabled());
    }

    @Test(groups = { "plugin.metadata", "PluginManagerBean" }, dependsOnMethods = { "disablePluginAndDependentPlugins" })
    public void enablePlugins() throws Exception {
        Plugin plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        Plugin plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        pluginMgr.enablePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        assertTrue("Failed to enable plugin", plugin1.isEnabled());
        assertTrue("Failed to enable plugin", plugin2.isEnabled());
    }

    @Test(dependsOnMethods = { "enablePlugins" })
    public void doNotDeletePluginIfDependentPluginIsNotAlsoDeleted() throws Exception {
        Plugin plugin = getPlugin("PluginManagerBeanTestPlugin1");
        EJBException exception = null;

        try {
            pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
        } catch (EJBException e) {
            exception = e;
        }

        assertNotNull("Expected exception to be thrown when trying to delete a plugin that has dependent plugins",
            exception);
        assertTrue("Expected an IllegalArgumentException when trying to delete a plugin with dependent plugins",
            exception.getCausedByException() instanceof IllegalArgumentException);
    }

    @Test(dependsOnMethods = { "doNotDeletePluginIfDependentPluginIsNotAlsoDeleted" })
    public void deletePlugins() throws Exception {
        Plugin plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        Plugin plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin("PluginManagerBeanTestPlugin1", "Deleting a plugin should not remove it from the database");
        plugin2 = getPlugin("PluginManagerBeanTestPlugin2", "Deleting a plugin should not remove it from the database");

        assertTrue("Expected plugin status to be set to DELETED", plugin1.getStatus() == PluginStatusType.DELETED);
        assertTrue("Expected plugin status to be set to DELETED", plugin2.getStatus() == PluginStatusType.DELETED);
    }

    @Test(dependsOnMethods = {"deletePlugins"})
    public void isPluginReadyForPurge() throws Exception {
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        InventoryManagerLocal inventoryManager = LookupUtil.getInventoryManager();

        Plugin plugin = getDeletedPlugin("PluginManagerBeanTestPlugin1");
        List<ResourceType> resourceTypes = resourceTypeManager.getResourceTypesByPlugin(plugin.getName());
        List<ResourceType> deletedTypes = inventoryManager.getDeletedTypes();

        assertTrue("All of the resource types declared in " + plugin + " should have already been deleted",
            deletedTypes.containsAll(resourceTypes));

        assertFalse("A plugin is not ready to be purged until all of its resource types have already been purged " +
            "and until the plugin itself has been marked for purge", pluginMgr.isReadyForPurge(plugin));
    }

    private Plugin getDeletedPlugin(String name) {
        List<Plugin> deletedPlugins = pluginMgr.findAllDeletedPlugins();
        for (Plugin plugin : deletedPlugins) {
            if (plugin.getName().equals(name)) {
                return plugin;
            }
        }
        return null;
    }

    @Test(dependsOnMethods = { "registerPlugins", "isPluginReadyForPurge"})
    public void pluginPurgeCheckShouldUseExactMatchesInQuery() throws Exception {
        // See https://bugzilla.redhat.com/show_bug.cgi?id=845700 for details on this test

        InventoryManagerLocal inventoryManager = LookupUtil.getInventoryManager();
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

        Plugin plugin3 = getPlugin("PluginManagerBeanTestPlugin3");
        ResourceType resourceType = resourceTypeManager.getResourceTypeByNameAndPlugin("TestServer3",
            plugin3.getName());

        assertNotNull("Failed to find resource type. Did the resource type name in the plugin descriptor change?",
            resourceType);

        pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin3.getId()));
        inventoryManager.purgeDeletedResourceType(resourceType);
        pluginMgr.markPluginsForPurge(subjectMgr.getOverlord(), asList(plugin3.getId()));


        assertTrue("Expected " + plugin3 + " to be ready for purge since all its resource types have been purged " +
            "and the plugin has been marked for purge", pluginMgr.isReadyForPurge(plugin3));
    }

    @Test(enabled = false, dependsOnMethods = { "deletePlugins" })
    public void purgePlugins() throws Exception {
        Plugin plugin1 = getPlugin("PluginManagerBeanTestPlugin1",
            "Deleting a plugin should not remove it from the database");
        Plugin plugin2 = getPlugin("PluginManagerBeanTestPlugin2",
            "Deleting a plugin should not remove it from the database");

        pluginMgr.markPluginsForPurge(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        assertEquals("Failed to purge plugins from the database", 1, pluginMgr.getPlugins().size());
    }

    private Plugin getPlugin(String name) {
        Plugin plugin = pluginMgr.getPlugin(name);
        assertNotNull("Failed to find plugin [" + name + "].", plugin);

        return plugin;
    }

    private Plugin getPlugin(String name, String msg) {
        List<Plugin> plugins = getEntityManager().createQuery("from Plugin where name = :name")
            .setParameter("name", name).getResultList();
        assertTrue("Failed to find plugin [" + name + "]: " + msg, plugins.size() == 1);

        return plugins.get(0);
    }

}
