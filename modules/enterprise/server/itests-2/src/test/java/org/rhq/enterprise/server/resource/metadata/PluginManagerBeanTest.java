/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import static java.util.Arrays.asList;

import java.io.File;
import java.util.List;

import javax.ejb.EJBException;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.criteria.ServerCriteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
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

    private static final String PLUGIN_1 = "PluginManagerBeanTestPlugin1";
    private static final String PLUGIN_2 = "PluginManagerBeanTestPlugin2";
    private static final String PLUGIN_3 = "PluginManagerBeanTestPlugin3";
    private static final String PLUGIN_3_1 = "PluginManagerBeanTestPlugin3.1";

    private SubjectManagerLocal subjectMgr;
    private PluginManagerLocal pluginMgr;

    @Override
    protected void beforeMethod() throws Exception {
        super.beforeMethod();

        subjectMgr = LookupUtil.getSubjectManager();
        pluginMgr = LookupUtil.getPluginManager();

        getPluginScannerService().startDeployment();
    }

    @Override
    protected void afterMethod() throws Exception {
        FileUtil.purge(new File(getPluginScannerService().getAgentPluginDir()), true);

        unpreparePluginScannerService();

        super.afterMethod();
    }

    public void registerPlugins() throws Exception {

        List<Plugin> plugins = getEntityManager()
            .createQuery(
                "from Plugin where name IN ('PluginManagerBeanTestPlugin1', 'PluginManagerBeanTestPlugin2', 'PluginManagerBeanTestPlugin3')")
            .getResultList();
        if (!plugins.isEmpty()) {
            System.out.println("Purging plugins " + plugins + "...");
            for (Plugin plugin : plugins) {
                pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
            }
            new PurgeResourceTypesJob().execute(null);
            new PurgePluginsJob().execute(null);
        }

        createPluginJarFile("test-plugin1.jar", "plugin_1.xml");
        createPluginJarFile("test-plugin2.jar", "plugin_2.xml");
        createPluginJarFile("test-plugin3.jar", "plugin_3.xml");
        createPluginJarFile("test-plugin3.1.jar", "plugin_3.1.xml");

        getPluginScannerService().scanAndRegister();

        // ignore some types to make sure the plugin manager can still work with ignored types
        ignoreType("TestServer1Ignored", PLUGIN_1);
        ignoreType("TestServer2Ignored", PLUGIN_2);
        ignoreType("TestServer3Ignored", PLUGIN_3);
        ignoreType("TestServer3.1Ignored", PLUGIN_3_1);

        return;
    }

    @Test(dependsOnMethods = { "registerPlugins" })
    public void scanAndRegisterTest() throws Exception {
        Plugin plugin = getPlugin(PLUGIN_1);
        assertNotNull(plugin);
        pluginDeployed(PLUGIN_1);

        plugin = getPlugin(PLUGIN_2);
        assertNotNull(plugin);
        pluginDeployed(PLUGIN_2);

        plugin = getPlugin(PLUGIN_3);
        assertNotNull(plugin);
        pluginDeployed(PLUGIN_3);

        plugin = getPlugin(PLUGIN_3_1);
        assertNotNull(plugin);
        pluginDeployed(PLUGIN_3_1);
    }

    @Test(dependsOnMethods = { "registerPlugins" })
    public void disablePlugin() throws Exception {
        Plugin plugin = getPlugin(PLUGIN_3);
        assertTrue("Plugin should not already be disabled", plugin.isEnabled());

        pluginMgr.disablePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
        plugin = pluginMgr.getPlugin(PLUGIN_3);

        assertNotNull(plugin);
        assertFalse("Failed to disable plugin", plugin.isEnabled());
    }

    @Test(dependsOnMethods = { "registerPlugins" })
    public void doNotDisablePluginIfDependentPluginsAreNotAlsoDisabled() throws Exception {
        Plugin plugin = getPlugin(PLUGIN_1);
        assertTrue("Plugin should not already be disabled", plugin.isEnabled());
        Plugin plugin2 = getPlugin(PLUGIN_2);
        assertTrue("Plugin should not already be disabled", plugin.isEnabled());

        Exception exception = null;

        try {
            pluginMgr.disablePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
        } catch (Exception e) {
            exception = e;
        }

        plugin = getPlugin(PLUGIN_1);
        assertTrue("Plugin should not have been disabled", plugin.isEnabled());

        assertNotNull("Expected exception to be thrown when trying to disable a plugin that has dependent plugins",
            exception);
        assertTrue("Expected an IllegalArgumentException when trying to disable a plugin with dependent plugins",
            exception.getCause() instanceof IllegalArgumentException);
    }

    @Test(dependsOnMethods = { "doNotDisablePluginIfDependentPluginsAreNotAlsoDisabled" })
    public void disablePluginAndDependentPlugins() throws Exception {
        Plugin plugin1 = getPlugin(PLUGIN_1);
        Plugin plugin2 = getPlugin(PLUGIN_2);

        pluginMgr.disablePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin(PLUGIN_1);
        plugin2 = getPlugin(PLUGIN_2);

        assertFalse("Failed to disable plugin", plugin1.isEnabled());
        assertFalse("Failed to disable plugin", plugin2.isEnabled());
    }

    @Test(groups = { "plugin.metadata", "PluginManagerBean" }, dependsOnMethods = { "disablePluginAndDependentPlugins" })
    public void enablePlugins() throws Exception {
        Plugin plugin1 = getPlugin(PLUGIN_1);
        Plugin plugin2 = getPlugin(PLUGIN_2);

        pluginMgr.enablePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin(PLUGIN_1);
        plugin2 = getPlugin(PLUGIN_2);

        assertTrue("Failed to enable plugin", plugin1.isEnabled());
        assertTrue("Failed to enable plugin", plugin2.isEnabled());
    }

    @Test(dependsOnMethods = { "enablePlugins" })
    public void doNotDeletePluginIfDependentPluginIsNotAlsoDeleted() throws Exception {
        Plugin plugin = getPlugin(PLUGIN_1);
        Exception exception = null;

        try {
            pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin.getId()));
            fail("Expected an IllegalArgumentException when trying to delete a plugin with dependent plugins");

        } catch (IllegalArgumentException e) {
            // expected
        } catch (EJBException ee) {
            if (ee.getCause() == null || !(ee.getCause() instanceof IllegalArgumentException)) {
                fail("Expected an IllegalArgumentException when trying to delete a plugin with dependent plugins, got: "
                    + ee);
            }

        } catch (Throwable t) {
            fail("Expected an IllegalArgumentException when trying to delete a plugin with dependent plugins, got: "
                + t);
        }
    }

    @Test(dependsOnMethods = { "doNotDeletePluginIfDependentPluginIsNotAlsoDeleted" })
    public void deletePlugins() throws Exception {
        Plugin plugin1 = getPlugin(PLUGIN_1);
        Plugin plugin2 = getPlugin(PLUGIN_2);

        pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin1.getId(), plugin2.getId()));

        plugin1 = getPlugin(PLUGIN_1, "Deleting a plugin should not remove it from the database");
        plugin2 = getPlugin(PLUGIN_2, "Deleting a plugin should not remove it from the database");

        assertTrue("Expected plugin status to be set to DELETED", plugin1.getStatus() == PluginStatusType.DELETED);
        assertTrue("Expected plugin status to be set to DELETED", plugin2.getStatus() == PluginStatusType.DELETED);
    }

    @Test(dependsOnMethods = { "deletePlugins" })
    public void isPluginReadyForPurge() throws Exception {
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        InventoryManagerLocal inventoryManager = LookupUtil.getInventoryManager();

        Plugin plugin = getDeletedPlugin(PLUGIN_1);
        if (plugin == null) {
            //ok so there's no delete plugin like that. Let's check that there's no installed plugin either
            plugin = pluginMgr.getPlugin(PLUGIN_1);
            if (plugin != null) {
                fail(PLUGIN_1 + "should have been deleted in PluginManagerBeanTest#deletePlugins()");
            }

            //So there's no such plugin at all. This means that some other test intertwined between this test and
            //deletePlugins.
            //
            //Because tests are configured to clean up after themselves (mainly in the afterClassWork() method)
            //it may happen that the plugin we marked for deletion in deletePlugins has actually been deleted
            //by the clean up methods.
            //
            //The point of this test is in that case fulfilled anyway because by the plugin disappearing,
            //we proved that at some point in time between deletePlugins and this test it was indeed purgeable.
            return;
        }

        List<ResourceType> resourceTypes = resourceTypeManager.getResourceTypesByPlugin(plugin.getName());
        List<ResourceType> deletedTypes = inventoryManager.getDeletedTypes();

        boolean resourceTypesPurged = resourceTypes.isEmpty() && deletedTypes.isEmpty();

        //ack the plugins as deleted so that the only remaining condition for their
        //purge-ability is the disappearance of their resource types.
        ackDeletedPlugins();

        assertTrue("A plugin is not ready to be purged until all of its resource types have already been purged "
                + "and until the plugin itself has been acked for deletion by all servers",
            resourceTypesPurged == pluginMgr.isReadyForPurge(plugin));
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

    @Test(dependsOnMethods = { "registerPlugins", "isPluginReadyForPurge" })
    public void pluginPurgeCheckShouldUseExactMatchesInQuery() throws Exception {
        // See https://bugzilla.redhat.com/show_bug.cgi?id=845700 for details on this test

        InventoryManagerLocal inventoryManager = LookupUtil.getInventoryManager();
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

        Plugin plugin3 = getPlugin(PLUGIN_3);
        ResourceType resourceType = resourceTypeManager
            .getResourceTypeByNameAndPlugin("TestServer3", plugin3.getName());
        ResourceType resourceTypeIgnored = resourceTypeManager.getResourceTypeByNameAndPlugin("TestServer3Ignored",
            plugin3.getName());

        assertNotNull("Failed to find resource type. Did the resource type name in the plugin descriptor change?",
            resourceType);
        assertNotNull("Failed to find ignored resource type. Did the type name in the plugin descriptor change?",
            resourceTypeIgnored);

        pluginMgr.deletePlugins(subjectMgr.getOverlord(), asList(plugin3.getId()));
        inventoryManager.purgeDeletedResourceType(resourceType);
        inventoryManager.purgeDeletedResourceType(resourceTypeIgnored);

        ackDeletedPlugins();

        assertTrue("Expected " + plugin3 + " to be ready for purge since all its resource types have been purged "
            + "and the servers acked its deletion", pluginMgr.isReadyForPurge(plugin3));
    }

    // this needs to be the last test executed in the class, it does cleanup
    @Test(priority = 10, alwaysRun = true, dependsOnMethods = { "pluginPurgeCheckShouldUseExactMatchesInQuery" })
    public void afterClassWorkTest() throws Exception {
        afterClassWork();
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
