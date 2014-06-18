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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.hibernate.LazyInitializationException;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.plugin.pc.generic.TestGenericServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.generic.GenericPluginDescriptorType;

/**
 * @author John Mazzitelli
 */
@Test
public class ServerPluginsBeanTest extends AbstractEJB3Test {

    private static final String TEST_PLUGIN_NAME_PREFIX = "serverplugintest";

    private ServerPluginManagerLocal serverPluginsBean;

    @Override
    protected void beforeMethod() {
        TestGenericServerPluginService pluginService;
        pluginService = new TestGenericServerPluginService(getTempDir());
        prepareCustomServerPluginService(pluginService);

        serverPluginsBean = LookupUtil.getServerPluginManager();
    }

    @Override
    protected void afterMethod() {

        try {
            unprepareServerPluginService();
        } catch (Exception e) {
        }

        try {
            getTransactionManager().begin();
            em = getEntityManager();

            Query q = em
                .createQuery("SELECT p FROM ServerPlugin p LEFT JOIN FETCH p.pluginConfiguration ppc LEFT JOIN FETCH p.scheduledJobsConfiguration psjc WHERE p.name LIKE 'serverplugintest%'");
            List<ServerPlugin> doomed = q.getResultList();
            for (ServerPlugin plugin : doomed) {
                em.remove(em.getReference(ServerPlugin.class, plugin.getId()));
            }

            getTransactionManager().commit();

        } catch (Exception e) {
            try {
                System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }
        }
    }

    public void testGetPlugins() throws Exception {
        ServerPlugin p1 = registerPlugin(1);
        ServerPlugin p2 = registerPlugin(2);
        PluginKey p1key = new PluginKey(p1);
        PluginKey p2key = new PluginKey(p2);
        List<PluginKey> pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(true);
        assert pluginKeys.contains(p1key) : pluginKeys;
        assert pluginKeys.contains(p2key) : pluginKeys;
        pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(false);
        assert !pluginKeys.contains(p1key) : pluginKeys;
        assert !pluginKeys.contains(p2key) : pluginKeys;

        ServerPlugin plugin = this.serverPluginsBean.getServerPlugin(p1key);
        assert plugin.getId() == p1.getId() : plugin;
        assert plugin.getName().equals(p1.getName()) : plugin;
        assetLazyInitializationException(plugin);

        plugin = this.serverPluginsBean.getServerPluginRelationships(plugin);
        assert plugin.getPluginConfiguration() != null;
        assert plugin.getScheduledJobsConfiguration() != null;
        assert plugin.getPluginConfiguration().equals(p1.getPluginConfiguration());
        assert plugin.getScheduledJobsConfiguration().equals(p1.getScheduledJobsConfiguration());

        List<ServerPlugin> plugins = this.serverPluginsBean.getServerPlugins();
        assert plugins.contains(p1) : plugins;
        assert plugins.contains(p2) : plugins;
        assetLazyInitializationException(plugins.get(plugins.indexOf(p1)));
        assetLazyInitializationException(plugins.get(plugins.indexOf(p2)));

        plugins = this.serverPluginsBean.getAllServerPlugins();
        assert plugins.contains(p1) : plugins;
        assert plugins.contains(p2) : plugins;
        assetLazyInitializationException(plugins.get(plugins.indexOf(p1)));
        assetLazyInitializationException(plugins.get(plugins.indexOf(p2)));

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(p1.getId());
        ids.add(p2.getId());
        plugins = this.serverPluginsBean.getServerPluginsById(ids);
        assert plugins.size() == 2 : plugins;
        assert plugins.contains(p1) : plugins;
        assert plugins.contains(p2) : plugins;
        assetLazyInitializationException(plugins.get(plugins.indexOf(p1)));
        assetLazyInitializationException(plugins.get(plugins.indexOf(p2)));

        long lastTimestamp;
        lastTimestamp = this.serverPluginsBean.getLastConfigurationChangeTimestamp(p1.getId());
        assert lastTimestamp == getConfigurationLastModifiedTimestamp(p1);
        lastTimestamp = this.serverPluginsBean.getLastConfigurationChangeTimestamp(p2.getId());
        assert lastTimestamp == getConfigurationLastModifiedTimestamp(p2);
    }

    public void testUpdatePlugins() throws Exception {
        ServerPlugin p1 = registerPlugin(1);
        PluginKey p1key = new PluginKey(p1);

        p1 = this.serverPluginsBean.getServerPlugin(p1key);
        p1 = this.serverPluginsBean.getServerPluginRelationships(p1);

        ServerPlugin p1update = this.serverPluginsBean.updateServerPluginExceptContent(getOverlord(), p1);
        p1update = this.serverPluginsBean.getServerPluginRelationships(p1update);

        assert p1update.getId() == p1.getId() : p1update;
        assert p1update.getId() == p1.getId() : p1update;
        assert p1update.getName().equals(p1.getName()) : p1update;
        assert p1update.getName().equals(p1.getName()) : p1update;
        assert p1update.getPluginConfiguration().equals(p1.getPluginConfiguration()) : p1update;
        assert p1update.getScheduledJobsConfiguration().equals(p1.getScheduledJobsConfiguration()) : p1update;
        p1update.getPluginConfiguration().equals(p1.getPluginConfiguration());
        p1update.getScheduledJobsConfiguration().equals(p1.getScheduledJobsConfiguration());
    }

    public void testDisableEnablePlugins() throws Exception {
        ServerPlugin p1 = registerPlugin(1);
        ServerPlugin p2 = registerPlugin(2);
        PluginKey p1key = new PluginKey(p1);
        PluginKey p2key = new PluginKey(p2);

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(p1.getId());
        ids.add(p2.getId());
        List<PluginKey> disabled = this.serverPluginsBean.disableServerPlugins(getOverlord(), ids);
        assert disabled.size() == 2 : disabled;
        assert disabled.contains(p1key) : disabled;
        assert disabled.contains(p2key) : disabled;
        ;
        assert this.serverPluginsBean.getServerPlugin(p1key).getStatus() == PluginStatusType.INSTALLED; // still installed
        assert this.serverPluginsBean.getServerPlugin(p2key).getStatus() == PluginStatusType.INSTALLED; // still installed
        List<PluginKey> pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(false);
        assert pluginKeys.contains(p1key) : pluginKeys;
        assert pluginKeys.contains(p2key) : pluginKeys;
        pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(true);
        assert !pluginKeys.contains(p1key) : pluginKeys;
        assert !pluginKeys.contains(p2key) : pluginKeys;

        // re-enable them
        this.serverPluginsBean.enableServerPlugins(getOverlord(), ids);
        pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(true);
        assert pluginKeys.contains(p1key) : pluginKeys;
        assert pluginKeys.contains(p2key) : pluginKeys;
        pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(false);
        assert !pluginKeys.contains(p1key) : pluginKeys;
        assert !pluginKeys.contains(p2key) : pluginKeys;

        // make sure none of these enable/disable settings lost our config
        ServerPlugin plugin = this.serverPluginsBean.getServerPlugin(p1key);
        plugin = this.serverPluginsBean.getServerPluginRelationships(plugin);
        assert plugin.getPluginConfiguration() != null; // no LazyInitException should be thrown!
        assert plugin.getPluginConfiguration().equals(p1.getPluginConfiguration());
        plugin = this.serverPluginsBean.getServerPlugin(p2key);
        plugin = this.serverPluginsBean.getServerPluginRelationships(plugin);
        assert plugin.getPluginConfiguration() != null; // no LazyInitException should be thrown!
        assert plugin.getPluginConfiguration().equals(p1.getPluginConfiguration());
    }

    public void testUndeployPlugins() throws Exception {

        int originalSize = this.serverPluginsBean.getAllServerPlugins().size();

        PluginStatusType status;

        PluginKey missingKey;
        missingKey = new PluginKey(PluginDeploymentType.SERVER,
            new ServerPluginType(GenericPluginDescriptorType.class).stringify(), TEST_PLUGIN_NAME_PREFIX + "1");
        status = this.serverPluginsBean.getServerPluginStatus(missingKey);
        assert status == null;
        missingKey = new PluginKey(PluginDeploymentType.SERVER,
            new ServerPluginType(GenericPluginDescriptorType.class).stringify(), TEST_PLUGIN_NAME_PREFIX + "2");
        status = this.serverPluginsBean.getServerPluginStatus(missingKey);
        assert status == null;

        ServerPlugin p1 = registerPlugin(1);
        ServerPlugin p2 = registerPlugin(2);
        PluginKey p1key = new PluginKey(p1);
        PluginKey p2key = new PluginKey(p2);

        status = this.serverPluginsBean.getServerPluginStatus(p1key);
        assert status == PluginStatusType.INSTALLED;
        status = this.serverPluginsBean.getServerPluginStatus(p2key);
        assert status == PluginStatusType.INSTALLED;

        assert this.serverPluginsBean.getServerPlugins().size() == originalSize + 2;
        assert this.serverPluginsBean.getAllServerPlugins().size() == originalSize + 2;

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(p1.getId());
        ids.add(p2.getId());
        List<PluginKey> undeployed = this.serverPluginsBean.deleteServerPlugins(getOverlord(), ids);
        assert undeployed.size() == 2 : undeployed;
        assert undeployed.contains(p1key) : undeployed;
        assert undeployed.contains(p2key) : undeployed;
        ServerPlugin p1deleted = getDeletedPluginInTx(p1.getName());
        ServerPlugin p2deleted = getDeletedPluginInTx(p2.getName());
        assert p1deleted.getStatus() == PluginStatusType.DELETED;
        assert p2deleted.getStatus() == PluginStatusType.DELETED;

        List<PluginKey> pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(false);
        assert !pluginKeys.contains(p1key) : "deleted plugins should not be returned even here" + pluginKeys;
        assert !pluginKeys.contains(p2key) : "deleted plugins should not be returned even here" + pluginKeys;
        pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(true);
        assert !pluginKeys.contains(p1.getName()) : pluginKeys;
        assert !pluginKeys.contains(p2.getName()) : pluginKeys;

        assert this.serverPluginsBean.getServerPlugins().size() == originalSize;
        assert this.serverPluginsBean.getAllServerPlugins().size() == originalSize + 2;
    }

    public void testReRegisterPlugins() throws Exception {

        PluginStatusType status;

        PluginKey missingKey;
        missingKey = new PluginKey(PluginDeploymentType.SERVER,
            new ServerPluginType(GenericPluginDescriptorType.class).stringify(), TEST_PLUGIN_NAME_PREFIX + "1");
        status = this.serverPluginsBean.getServerPluginStatus(missingKey);
        assert status == null;
        missingKey = new PluginKey(PluginDeploymentType.SERVER,
            new ServerPluginType(GenericPluginDescriptorType.class).stringify(), TEST_PLUGIN_NAME_PREFIX + "2");
        status = this.serverPluginsBean.getServerPluginStatus(missingKey);
        assert status == null;

        ServerPlugin p1 = registerPlugin(1);
        ServerPlugin p2 = registerPlugin(2);
        PluginKey p1key = new PluginKey(p1);
        PluginKey p2key = new PluginKey(p2);

        status = this.serverPluginsBean.getServerPluginStatus(p1key);
        assert status == PluginStatusType.INSTALLED;
        status = this.serverPluginsBean.getServerPluginStatus(p2key);
        assert status == PluginStatusType.INSTALLED;

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(p1.getId());
        ids.add(p2.getId());
        List<PluginKey> undeployed = this.serverPluginsBean.deleteServerPlugins(getOverlord(), ids);
        assert undeployed.size() == 2 : undeployed;
        assert undeployed.contains(p1key) : undeployed;
        assert undeployed.contains(p2key) : undeployed;
        ServerPlugin p1deleted = getDeletedPluginInTx(p1.getName());
        assert p1deleted.getStatus() == PluginStatusType.DELETED;
        assert p1deleted.getPluginConfiguration() == null; // undeploy should have removed this
        assert p1deleted.getScheduledJobsConfiguration() == null; // undeploy should have removed this
        ServerPlugin p2deleted = getDeletedPluginInTx(p1.getName());
        assert p2deleted.getStatus() == PluginStatusType.DELETED;
        assert p2deleted.getPluginConfiguration() == null; // undeploy should have removed this
        assert p2deleted.getScheduledJobsConfiguration() == null; // undeploy should have removed this

        List<PluginKey> pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(false);
        assert !pluginKeys.contains(p1key) : "deleted plugins should not be returned even here" + pluginKeys;
        assert !pluginKeys.contains(p2key) : "deleted plugins should not be returned even here" + pluginKeys;
        pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(true);
        assert !pluginKeys.contains(p1key) : pluginKeys;
        assert !pluginKeys.contains(p2key) : pluginKeys;

        // purge them completely from the DB to prepare to re-register them
        this.serverPluginsBean.purgeServerPlugin(p1.getId());
        this.serverPluginsBean.purgeServerPlugin(p2.getId());

        // we just purged the database, make sure our entity ID's are all zero, since the original IDs are gone
        p1.setId(0);
        p2.setId(0);
        p1.setPluginConfiguration(p1.getPluginConfiguration().deepCopy(false));
        p2.setPluginConfiguration(p2.getPluginConfiguration().deepCopy(false));
        p1.setScheduledJobsConfiguration(p1.getScheduledJobsConfiguration().deepCopy(false));
        p2.setScheduledJobsConfiguration(p2.getScheduledJobsConfiguration().deepCopy(false));

        // re-register them now
        ServerPlugin p1again = this.serverPluginsBean.registerServerPlugin(getOverlord(), p1, null);
        ServerPlugin p2again = this.serverPluginsBean.registerServerPlugin(getOverlord(), p2, null);

        pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(true);
        assert pluginKeys.contains(p1key) : pluginKeys;
        assert pluginKeys.contains(p2key) : pluginKeys;
        assert p1again.getStatus() == PluginStatusType.INSTALLED;
        assert p2again.getStatus() == PluginStatusType.INSTALLED;
        pluginKeys = this.serverPluginsBean.getServerPluginKeysByEnabled(false);
        assert !pluginKeys.contains(p1key) : pluginKeys;
        assert !pluginKeys.contains(p2key) : pluginKeys;
    }

    private ServerPlugin registerPlugin(int index) throws Exception {
        ServerPlugin plugin = new ServerPlugin(0, TEST_PLUGIN_NAME_PREFIX + index, "path", "displayName", true,
            PluginStatusType.INSTALLED, "description", "help", "md5", "version", "ampsVersion",
            createPluginConfiguration(), createScheduledJobsConfiguration(), new ServerPluginType(
                GenericPluginDescriptorType.class).stringify(), System.currentTimeMillis(), System.currentTimeMillis());

        plugin = this.serverPluginsBean.registerServerPlugin(getOverlord(), plugin, null);
        assert plugin.getId() > 0;
        assert plugin.isEnabled() == true;
        assert plugin.getDeployment() == PluginDeploymentType.SERVER;
        assert plugin.getStatus() == PluginStatusType.INSTALLED;
        assert plugin.getPluginConfiguration() != null;
        assert plugin.getScheduledJobsConfiguration() != null;
        assert plugin.getType().equals(new ServerPluginType(GenericPluginDescriptorType.class).stringify());
        return plugin;
    }

    private void assetLazyInitializationException(ServerPlugin plugin) {
        try {
            plugin.getPluginConfiguration().toString();
            assert false : "Should have thrown a lazy-initialization exception - we didn't load config";
        } catch (LazyInitializationException ok) {
        }
        try {
            plugin.getScheduledJobsConfiguration().toString();
            assert false : "Should have thrown a lazy-initialization exception - we didn't load config";
        } catch (LazyInitializationException ok) {
        }
    }

    private Configuration createPluginConfiguration() {
        Configuration config = new Configuration();
        config.put(new PropertySimple("simpleprop1", "simpleprop1value"));
        return config;
    }

    private Configuration createScheduledJobsConfiguration() {
        Configuration config = new Configuration();
        PropertyMap map = new PropertyMap("jobname");
        config.put(map);
        map.put(new PropertySimple("methodName", "methodNameValue"));
        return config;
    }

    private Subject getOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }

    private ServerPlugin getDeletedPluginInTx(String pluginName) throws Exception {

        getTransactionManager().begin();
        try {
            Query q = em.createNamedQuery(ServerPlugin.QUERY_FIND_ANY_BY_NAME);
            q.setParameter("name", pluginName);
            return (ServerPlugin) q.getSingleResult();
        } finally {
            getTransactionManager().rollback();
        }
    }

    private long getConfigurationLastModifiedTimestamp(ServerPlugin plugin) {
        // determine the last time the plugin config or schedule jobs changed
        // return 0 value if plugin doesn't have any config whatsoever
        long timestamp = 0;

        Configuration config = plugin.getPluginConfiguration();
        if (config != null) {
            timestamp = config.getModifiedTime();
        }

        config = plugin.getScheduledJobsConfiguration();
        if (config != null && config.getModifiedTime() > timestamp) {
            timestamp = config.getModifiedTime();
        }

        return timestamp;
    }
}
