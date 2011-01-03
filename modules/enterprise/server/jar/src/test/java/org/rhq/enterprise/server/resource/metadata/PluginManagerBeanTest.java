package org.rhq.enterprise.server.resource.metadata;

import java.util.Arrays;
import java.util.List;

import javax.ejb.EJBException;

import org.testng.annotations.Test;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import static java.util.Arrays.asList;

public class PluginManagerBeanTest extends MetadataBeanTest {

    SubjectManagerLocal subjectMgr;

    PluginManagerLocal pluginMgr;

    @Test(groups = {"plugin.metadata", "PluginManagerBean"})
    public void registerPlugins() throws Exception {
        subjectMgr = LookupUtil.getSubjectManager();
        pluginMgr = LookupUtil.getPluginManager();

        setupDB();

        createPlugin("test-plugin1", "1.0", "plugin_1.xml");
        createPlugin("test-plugin2", "1.0", "plugin_2.xml");
        createPlugin("test-plugin3", "1.0", "plugin_3.xml");
    }

    @Test(groups = {"plugin.metadata", "PluginManagerBean"}, dependsOnMethods = {"registerPlugins"})
    public void disablePlugin() throws Exception {
        Plugin plugin = getPlugin("PluginManagerBeanTestPlugin3");

        pluginMgr.disablePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
        plugin = pluginMgr.getPlugin("PluginManagerBeanTestPlugin3");

        assertFalse("Failed to disable plugin", plugin.isEnabled());
    }

    @Test(groups = {"plugin.metadata", "PluginManagerBean"}, dependsOnMethods = {"registerPlugins"})
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
        assertTrue(
            "Expected an IllegalArgumentException when trying to disable a plugin with dependent plugins",
            exception.getCausedByException() instanceof IllegalArgumentException
        );
    }

    @Test(groups = {"plugin.metadata", "PluginManagerBean"},
        dependsOnMethods = {"doNotDisablePluginIfDependentPluginsAreNotAlsoDisabled"})
    public void disablePluginAndDependentPlugins() throws Exception {
        Plugin plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        Plugin plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        pluginMgr.disablePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        assertFalse("Failed to disable plugin", plugin1.isEnabled());
        assertFalse("Failed to disable plugin", plugin2.isEnabled());
    }

    @Test(groups = {"plugin.metadata", "PluginManagerBean"}, dependsOnMethods = {"disablePluginAndDependentPlugins"})
    public void enablePlugins() throws Exception {
        Plugin plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        Plugin plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        pluginMgr.enablePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        assertTrue("Failed to enable plugin", plugin1.isEnabled());
        assertTrue("Failed to enable plugin", plugin2.isEnabled());
    }

    @Test(groups = {"plugin.metadata", "PluginManagerBean"}, dependsOnMethods = {"enablePlugins"})
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
        assertTrue(
            "Expected an IllegalArgumentException when trying to delete a plugin with dependent plugins",
            exception.getCausedByException() instanceof IllegalArgumentException
        );
      }

    @Test(groups = {"plugin.metadata", "PluginManagerBean"},
        dependsOnMethods = {"doNotDeletePluginIfDependentPluginIsNotAlsoDeleted"})
    public void deletePlugins() throws Exception {
        Plugin plugin1 = getPlugin("PluginManagerBeanTestPlugin1");
        Plugin plugin2 = getPlugin("PluginManagerBeanTestPlugin2");

        pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin("PluginManagerBeanTestPlugin1", "Deleting a plugin should not remove it from the database");
        plugin2 = getPlugin("PluginManagerBeanTestPlugin2", "Deleting a plugin should not remove it from the database");

        assertTrue("Expected plugin status to be set to DELETED", plugin1.getStatus() == PluginStatusType.DELETED);
        assertTrue("Expected plugin status to be set to DELETED", plugin2.getStatus() == PluginStatusType.DELETED);
    }

    @Test(enabled = false, groups = {"plugin.metadata", "PluginManagerBean"}, dependsOnMethods = {"deletePlugins"})
    public void purgePlugins() throws Exception {
        Plugin plugin1 = getPlugin("PluginManagerBeanTestPlugin1",
            "Deleting a plugin should not remove it from the database");
        Plugin plugin2 = getPlugin("PluginManagerBeanTestPlugin2",
            "Deleting a plugin should not remove it from the database");

        pluginMgr.purgePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        assertEquals("Failed to purge plugins from the database", 1, pluginMgr.getPlugins().size());
    }

    Plugin getPlugin(String name) {
        Plugin plugin = pluginMgr.getPlugin(name);
        assertNotNull("Failed to find plugin <$name>", plugin);

        return plugin;
    }

    Plugin getPlugin(String name, String msg) {
        List<Plugin> plugins = getEntityManager().createQuery("from Plugin where name = :name")
            .setParameter("name", name)
            .getResultList();
        assertTrue("Failed to find plugin <$name>: $msg", plugins.size() == 1);

        return plugins.get(0);
    }

}
