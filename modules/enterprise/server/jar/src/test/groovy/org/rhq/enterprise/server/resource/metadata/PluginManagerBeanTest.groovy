package org.rhq.enterprise.server.resource.metadata

import org.testng.annotations.Test
import org.rhq.enterprise.server.util.LookupUtil
import org.testng.annotations.BeforeMethod
import org.rhq.enterprise.server.auth.SubjectManagerLocal
import org.rhq.core.domain.plugin.Plugin
import javax.ejb.EJBException
import org.rhq.core.domain.plugin.PluginDeploymentType
import org.rhq.core.domain.plugin.PluginStatusType
import org.testng.annotations.BeforeGroups

class PluginManagerBeanTest extends MetadataTest {

  SubjectManagerLocal subjectMgr

  PluginManagerLocal pluginMgr

  @Test(groups = ['plugin.metadata', 'PluginManagerBean'])
  void registerPlugins() {
    subjectMgr = LookupUtil.subjectManager
    pluginMgr = LookupUtil.pluginManager

    setupDB()

    def pluginDescriptor1 =
    """
    <plugin name="PluginManagerBeanTestPlugin1"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="TestServer1"/>
    </plugin>
    """

    createPlugin("test-plugin1", "1.0", pluginDescriptor1)

    def pluginDescriptor2 =
    """
    <plugin name="PluginManagerBeanTestPlugin2"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">

      <depends plugin="PluginManagerBeanTestPlugin1" useClasses="true"/>

      <server name="TestServer2"/>
    </plugin>
    """

    createPlugin("test-plugin2", "1.0", pluginDescriptor2)

    def pluginDescriptor3 =
    """
    <plugin name="PluginManagerBeanTestPlugin3"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">

      <server name="TestServer3"/>
    </plugin>
    """

    createPlugin("test-plugin3", "1.0", pluginDescriptor3)
  }

  @Test(groups = ['plugin.metadata', 'PluginManagerBean'], dependsOnMethods = ['registerPlugins'])
  void disablePlugin() {
    def plugin = getPlugin('PluginManagerBeanTestPlugin3')

    pluginMgr.disablePlugins subjectMgr.overlord, [plugin.id]
    plugin = pluginMgr.getPlugin('PluginManagerBeanTestPlugin3')

    assertFalse 'Failed to disable plugin', plugin.enabled
  }

  @Test(groups = ['plugin.metadata', 'PluginManagerBean'], dependsOnMethods = ['registerPlugins'])
  void doNotDisablePluginIfDependentPluginsAreNotAlsoDisabled() {
    def plugin = getPlugin('PluginManagerBeanTestPlugin1')
    def exception = null

    try {
      pluginMgr.disablePlugins subjectMgr.overlord, [plugin.id]
    } catch (EJBException e) {
      exception = e
    }

    assertNotNull 'Expected exception to be thrown when trying to disable a plugin that has dependent plugins', exception
    assertTrue(
        'Expected an IllegalArgumentException when trying to disable a plugin with dependent plugins',
        exception.causedByException instanceof IllegalArgumentException
    )
  }

  @Test(groups = ['plugin.metadata', 'PluginManagerBean'], dependsOnMethods = ['doNotDisablePluginIfDependentPluginsAreNotAlsoDisabled'])
  void disablePluginAndDependentPlugins() {
    def plugin1 = getPlugin('PluginManagerBeanTestPlugin1')
    def plugin2 = getPlugin('PluginManagerBeanTestPlugin2')

    pluginMgr.disablePlugins subjectMgr.overlord, [plugin1.id, plugin2.id]

    plugin1 = getPlugin('PluginManagerBeanTestPlugin1')
    plugin2 = getPlugin('PluginManagerBeanTestPlugin2')

    assertFalse 'Failed to disable plugin', plugin1.enabled
    assertFalse 'Failed to disable plugin', plugin2.enabled
  }

  @Test(groups = ['plugin.metadata', 'PluginManagerBean'], dependsOnMethods = ['disablePluginAndDependentPlugins'])
  void enablePlugins() {
    def plugin1 = getPlugin('PluginManagerBeanTestPlugin1')
    def plugin2 = getPlugin('PluginManagerBeanTestPlugin2')

    pluginMgr.enablePlugins subjectMgr.overlord, [plugin1.id, plugin2.id]

    plugin1 = getPlugin('PluginManagerBeanTestPlugin1')
    plugin2 = getPlugin('PluginManagerBeanTestPlugin2')

    assertTrue 'Failed to enable plugin', plugin1.enabled
    assertTrue 'Failed to enable plugin', plugin2.enabled
  }

  @Test(groups = ['plugin.metadata', 'PluginManagerBean'], dependsOnMethods = ['enablePlugins'])
  void doNotDeletePluginIfDependentPluginIsNotAlsoDeleted() {
    def plugin = getPlugin('PluginManagerBeanTestPlugin1')
    def exception = null

    try {
      pluginMgr.deletePlugins subjectMgr.overlord, [plugin.id]
    } catch (EJBException e) {
      exception = e
    }

    assertNotNull 'Expected exception to be thrown when trying to delete a plugin that has dependent plugins', exception
    assertTrue(
        'Expected an IllegalArgumentException when trying to delete a plugin with dependent plugins',
        exception.causedByException instanceof IllegalArgumentException
    )
  }

  @Test(groups = ['plugin.metadata', 'PluginManagerBean'], dependsOnMethods = ['doNotDeletePluginIfDependentPluginIsNotAlsoDeleted'])
  void deletePlugins() {
    def plugin1 = getPlugin('PluginManagerBeanTestPlugin1')
    def plugin2 = getPlugin('PluginManagerBeanTestPlugin2')

    pluginMgr.deletePlugins subjectMgr.overlord, [plugin1.id, plugin2.id]

    plugin1 = getPlugin('PluginManagerBeanTestPlugin1', 'Deleting a plugin should not remove it from the database')
    plugin2 = getPlugin('PluginManagerBeanTestPlugin2', 'Deleting a plugin should not remove it from the database')

    assertTrue 'Expected plugin status to be set to DELETED', plugin1.status == PluginStatusType.DELETED
    assertTrue 'Expected plugin status to be set to DELETED', plugin2.status == PluginStatusType.DELETED
  }

  @Test(groups = ['plugin.metadata', 'PluginManagerBean'], dependsOnMethods = ['deletePlugins'])
  void purgePlugins() {
    def plugin1 = getPlugin('PluginManagerBeanTestPlugin1', 'Deleting a plugin should not remove it from the database')
    def plugin2 = getPlugin('PluginManagerBeanTestPlugin2', 'Deleting a plugin should not remove it from the database')

    pluginMgr.purgePlugins subjectMgr.overlord, [plugin1.id, plugin2.id]

    assertEquals('Failed to purge plugins from the database', 1, pluginMgr.getPlugins().size())
  }

  Plugin getPlugin(String name) {
    def plugin = pluginMgr.getPlugin(name)
    assertNotNull "Failed to find plugin <$name>", plugin

    return plugin
  }

  Plugin getPlugin(String name, String msg) {
    def plugins = entityManager.createQuery("from Plugin where name = :name").setParameter('name', name).resultList
    assertTrue "Failed to find plugin <$name>: $msg", plugins.size() == 1

    return plugins[0]
  }

}
