/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.resource.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;

@Test(groups = "integration.ejb3")
public class ServerPluginTest extends AbstractEJB3Test {
    public void testUpdate() throws Throwable {
        boolean done = false;
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "ServerPluginTest-testUpdate";
            String path = "/test/Update";
            String displayName = "Server Plugin Test - testUpdate";
            boolean enabled = true;
            PluginStatusType status = PluginStatusType.INSTALLED;
            String md5 = "abcdef";
            byte[] content = "the content is here".getBytes();

            ServerPlugin plugin = new ServerPlugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setStatus(status);
            plugin.setMD5(md5);
            plugin.setVersion(null);
            plugin.setDescription(null);
            plugin.setHelp(null);
            plugin.setContent(content);

            Query q = em.createNamedQuery(ServerPlugin.QUERY_GET_STATUS_BY_NAME);
            q.setParameter("name", plugin.getName());
            assert q.getResultList().size() == 0; // not in the db yet

            em.persist(plugin);
            id = plugin.getId();
            assert id > 0;
            assert plugin.getPluginConfiguration() == null : "there was no config that should have been here";
            assert plugin.getScheduledJobsConfiguration() == null : "there was no config that should have been here";

            q = em.createNamedQuery(ServerPlugin.QUERY_GET_CONFIG_MTIMES);
            q.setParameter("id", plugin.getId());
            Object[] times = (Object[]) q.getSingleResult();
            assert times[0] == null;
            assert times[1] == null;

            q = em.createNamedQuery(ServerPlugin.QUERY_GET_STATUS_BY_NAME);
            q.setParameter("name", plugin.getName());
            assert ((PluginStatusType) q.getSingleResult()) == PluginStatusType.INSTALLED;

            plugin = em.find(ServerPlugin.class, id);
            assert plugin != null;
            assert plugin.getId() == id;
            assert plugin.getName().equals(name);
            assert plugin.getPath().equals(path);
            assert plugin.getDisplayName().equals(displayName);
            assert plugin.isEnabled() == enabled;
            assert plugin.getStatus() == PluginStatusType.INSTALLED;
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion() == null;
            assert plugin.getDescription() == null;
            assert plugin.getDeployment() == PluginDeploymentType.SERVER;
            assert plugin.getPluginConfiguration() == null;
            assert plugin.getScheduledJobsConfiguration() == null;
            assert plugin.getHelp() == null;
            assert new String(plugin.getContent()).equals(new String(content));

            // everything persisted fine, let's update it and see the content is left alone
            name = name + "-UPDATED";
            path = path + "-UPDATED";
            displayName = displayName + "-UPDATED";
            enabled = !enabled;
            md5 = md5 + "00000";
            String version = "version-UPDATED";
            String ampsVersion = "2.1";
            String description = "description-UPDATED";
            String help = "help-UPDATED";
            PluginDeploymentType deployment = PluginDeploymentType.SERVER;
            Configuration pluginConfig = new Configuration();
            Configuration jobsConfig = new Configuration();
            pluginConfig.put(new PropertySimple("first", "last"));
            jobsConfig.put(new PropertySimple("aaa", "bbb"));

            em.close();
            getTransactionManager().commit(); // we will be doing an update - needs to be in own tx
            getTransactionManager().begin();
            em = getEntityManager();

            em.persist(pluginConfig);
            em.persist(jobsConfig);
            em.flush(); // gotta get those two persists to flush to the DB

            // do what ServerPluginsBean.updateServerPluginExceptContent does
            Configuration config = plugin.getPluginConfiguration();
            if (config != null) {
                config = em.merge(config);
                plugin.setPluginConfiguration(config);
            }
            config = plugin.getScheduledJobsConfiguration();
            if (config != null) {
                config = em.merge(config);
                plugin.setScheduledJobsConfiguration(config);
            }

            ServerPlugin pluginEntity = em.getReference(ServerPlugin.class, plugin.getId());
            pluginEntity.setName(name);
            pluginEntity.setPath(path);
            pluginEntity.setDisplayName(displayName);
            pluginEntity.setEnabled(enabled);
            pluginEntity.setStatus(status);
            pluginEntity.setMd5(md5);
            pluginEntity.setVersion(version);
            pluginEntity.setAmpsVersion(ampsVersion);
            pluginEntity.setDeployment(deployment);
            pluginEntity.setPluginConfiguration(pluginConfig);
            pluginEntity.setScheduledJobsConfiguration(jobsConfig);
            pluginEntity.setDescription(description);
            pluginEntity.setHelp(help);
            pluginEntity.setMtime(System.currentTimeMillis());

            try {
                em.flush(); // make sure we push this out to the DB now
            } catch (Exception e) {
                throw new Exception("Failed to update a plugin that matches [" + plugin + "]", e);
            }

            em.close();
            getTransactionManager().commit(); // must commit now
            getTransactionManager().begin();
            em = getEntityManager();

            plugin = em.find(ServerPlugin.class, id);
            assert plugin != null;
            assert plugin.getId() == id;
            assert plugin.getName().equals(name);
            assert plugin.getPath().equals(path);
            assert plugin.getDisplayName().equals(displayName);
            assert plugin.isEnabled() == enabled;
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion().equals(version);
            assert plugin.getAmpsVersion().equals(ampsVersion);
            assert plugin.getDescription().equals(description);
            assert plugin.getDeployment() == PluginDeploymentType.SERVER;
            assert plugin.getPluginConfiguration().equals(pluginConfig);
            assert plugin.getScheduledJobsConfiguration().equals(jobsConfig);
            assert plugin.getHelp().equals(help);
            // and what we really want to test - ensure the content remained intact after the update
            assert new String(plugin.getContent()).equals(new String(content));

            // clean up - delete our test plugin
            em.close();
            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();
            q = em.createNamedQuery(ServerPlugin.QUERY_FIND_ANY_BY_NAME);
            q.setParameter("name", plugin.getName());
            ServerPlugin doomed = (ServerPlugin) q.getSingleResult();
            doomed = em.getReference(ServerPlugin.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the plugin";
            em.close();
            getTransactionManager().commit();
            done = true;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            if (!done) {
                getTransactionManager().rollback();
            }
        }
    }

    public void testPersistMinimal() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            String name = "ServerPluginTest-testPersist";
            String path = "/test/Persist";
            String displayName = "Server Plugin Test - testPersist";
            boolean enabled = true;
            PluginStatusType status = PluginStatusType.INSTALLED;
            String md5 = "abcdef";

            ServerPlugin plugin = new ServerPlugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setStatus(status);
            plugin.setMD5(md5);

            // the following are the only nullable fields
            plugin.setVersion(null);
            plugin.setDescription(null);
            plugin.setHelp(null);
            plugin.setContent(null);

            em.persist(plugin);
            assert plugin.getId() > 0;

            plugin = em.find(ServerPlugin.class, plugin.getId());
            assert plugin != null;
            assert plugin.getId() > 0;
            assert plugin.getName().equals(name);
            assert plugin.getPath().equals(path);
            assert plugin.getDisplayName().equals(displayName);
            assert plugin.isEnabled() == enabled;
            assert plugin.getStatus() == PluginStatusType.INSTALLED;
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion() == null;
            assert plugin.getDescription() == null;
            assert plugin.getDeployment() == PluginDeploymentType.SERVER;
            assert plugin.getPluginConfiguration() == null;
            assert plugin.getScheduledJobsConfiguration() == null;
            assert plugin.getHelp() == null;
            assert plugin.getContent() == null;

            // side check - see that "deleting" a plugin also sets enabled to false
            assert plugin.isEnabled() == true;
            assert plugin.getStatus() == PluginStatusType.INSTALLED;
            plugin.setStatus(PluginStatusType.DELETED);
            assert plugin.getStatus() == PluginStatusType.DELETED;
            assert plugin.isEnabled() == false;
            plugin = em.merge(plugin);
            assert plugin.getStatus() == PluginStatusType.DELETED;
            assert plugin.isEnabled() == false;

        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    public void testPersistFull() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            Query query = em.createNamedQuery(ServerPlugin.QUERY_FIND_ALL_INSTALLED);
            int originalNumberOfPlugins = query.getResultList().size();

            String name = "ServerPluginTest-testPersist";
            String path = "/test/Persist";
            String displayName = "Server Plugin Test - testPersist";
            boolean enabled = true;
            String version = "1.0";
            String description = "the test description is here";
            String help = "the test help string is here";
            byte[] content = "this is the test content".getBytes();
            String md5 = MessageDigestGenerator.getDigestString(new String(content));
            PluginDeploymentType deployment = PluginDeploymentType.SERVER;
            String ampsVersion = "1.2";
            String type = "the-type-here";

            Configuration pluginConfig = new Configuration();
            Configuration jobsConfig = new Configuration();
            pluginConfig.put(new PropertySimple("first", "last"));
            jobsConfig.put(new PropertySimple("aaa", "bbb"));

            ServerPlugin plugin = new ServerPlugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setMD5(md5);
            plugin.setVersion(version);
            plugin.setAmpsVersion(ampsVersion);
            plugin.setDescription(description);
            plugin.setHelp(help);
            plugin.setContent(content);
            plugin.setDeployment(deployment);
            plugin.setPluginConfiguration(pluginConfig);
            plugin.setScheduledJobsConfiguration(jobsConfig);
            plugin.setType(type);

            em.persist(plugin);
            assert plugin.getId() > 0;

            plugin = em.find(ServerPlugin.class, plugin.getId());
            assert plugin != null;
            assert plugin.getId() > 0;
            assert plugin.getName().equals(name);
            assert plugin.getPath().equals(path);
            assert plugin.getDisplayName().equals(displayName);
            assert plugin.isEnabled() == enabled;
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion().equals(version);
            assert plugin.getAmpsVersion().equals(ampsVersion);
            assert plugin.getDescription().equals(description);
            assert plugin.getDeployment() == deployment;
            assert plugin.getPluginConfiguration().equals(pluginConfig);
            assert plugin.getScheduledJobsConfiguration().equals(jobsConfig);
            assert plugin.getHelp().equals(help);
            assert plugin.getType().equals(type);
            assert new String(plugin.getContent()).equals(new String(content));

            // test our queries that purposefully do not load in the content blob
            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_BY_NAME);
            query.setParameter("name", name);
            plugin = (ServerPlugin) query.getSingleResult();
            assert plugin != null;
            assert plugin.getId() > 0;
            assert plugin.getName().equals(name);
            assert plugin.getPath().equals(path);
            assert plugin.getDisplayName().equals(displayName);
            assert plugin.isEnabled() == enabled;
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion().equals(version);
            assert plugin.getAmpsVersion().equals(ampsVersion);
            assert plugin.getDescription().equals(description);
            assert plugin.getDeployment() == deployment;
            assert plugin.getPluginConfiguration().equals(pluginConfig);
            assert plugin.getScheduledJobsConfiguration().equals(jobsConfig);
            assert plugin.getHelp().equals(help);
            assert plugin.getType().equals(type);
            assert plugin.getContent() == null;

            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_BY_IDS);
            query.setParameter("ids", Arrays.asList(Integer.valueOf(plugin.getId())));
            plugin = (ServerPlugin) query.getSingleResult();
            assert plugin != null;
            assert plugin.getId() > 0;
            assert plugin.getName().equals(name);
            assert plugin.getPath().equals(path);
            assert plugin.getDisplayName().equals(displayName);
            assert plugin.isEnabled() == enabled;
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion().equals(version);
            assert plugin.getAmpsVersion().equals(ampsVersion);
            assert plugin.getDescription().equals(description);
            assert plugin.getDeployment() == deployment;
            assert plugin.getPluginConfiguration().equals(pluginConfig);
            assert plugin.getScheduledJobsConfiguration().equals(jobsConfig);
            assert plugin.getHelp().equals(help);
            assert plugin.getType().equals(type);
            assert plugin.getContent() == null;

            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_ALL_INSTALLED);
            List<ServerPlugin> all = query.getResultList();
            boolean got_it = false;
            for (ServerPlugin p : all) {
                if (p.getName().equals(name)) {
                    got_it = true;
                    assert p.getId() > 0;
                    assert p.getName().equals(name);
                    assert p.getPath().equals(path);
                    assert p.getDisplayName().equals(displayName);
                    assert p.isEnabled() == enabled;
                    assert p.getMD5().equals(md5);
                    assert p.getVersion().equals(version);
                    assert plugin.getAmpsVersion().equals(ampsVersion);
                    assert p.getDescription().equals(description);
                    assert plugin.getDeployment() == deployment;
                    assert plugin.getPluginConfiguration().equals(pluginConfig);
                    assert plugin.getScheduledJobsConfiguration().equals(jobsConfig);
                    assert p.getHelp().equals(help);
                    assert plugin.getType().equals(type);
                    assert p.getContent() == null;
                    break;
                }
            }
            assert got_it : "findAll query failed to get our plugin";

            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_ALL_INSTALLED_KEYS);
            List<PluginKey> allKeys = query.getResultList();
            assert allKeys.size() == originalNumberOfPlugins + 1;
            assert allKeys.contains(new PluginKey(plugin));

            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_KEYS_BY_IDS);
            ArrayList<Integer> ids = new ArrayList<Integer>();
            ids.add(plugin.getId());
            query.setParameter("ids", ids);
            allKeys = query.getResultList();
            assert allKeys.size() == 1;
            assert allKeys.contains(new PluginKey(plugin));

            query = em.createNamedQuery(ServerPlugin.QUERY_GET_CONFIG_MTIMES);
            query.setParameter("id", plugin.getId());
            Object[] times = (Object[]) query.getSingleResult();
            assert times[0] != null;
            assert times[1] != null;
            assert ((Long) times[0]).longValue() > 0;
            assert ((Long) times[1]).longValue() > 0;

            // mark a plugin deleted - all of our queries should then never see it
            plugin.setStatus(PluginStatusType.DELETED);
            em.merge(plugin);

            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_BY_NAME);
            query.setParameter("name", name);
            List<?> results = query.getResultList();
            assert results.size() == 0;

            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_BY_IDS);
            query.setParameter("ids", Arrays.asList(Integer.valueOf(plugin.getId())));
            results = query.getResultList();
            assert results.size() == 0;

            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_ALL_INSTALLED);
            results = query.getResultList();
            assert results.size() == originalNumberOfPlugins;

            query = em.createNamedQuery(ServerPlugin.QUERY_FIND_ALL);
            results = query.getResultList();
            assert results.size() == originalNumberOfPlugins + 1;

        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testPersistStreamContent() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean done = false;

        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            String name = "ServerPluginTest-testPersist";
            String path = "/test/Persist";
            String displayName = "Server Plugin Test - testPersist";
            boolean enabled = true;
            String version = "1.0";
            String description = "the test description is here";
            String help = "the test help string is here";
            byte[] content = "this is the test content".getBytes();
            String md5 = MessageDigestGenerator.getDigestString(new String(content));

            // persist the plugin, but without any content
            ServerPlugin plugin = new ServerPlugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setMD5(md5);
            plugin.setVersion(version);
            plugin.setDescription(description);
            plugin.setHelp(help);

            em.persist(plugin);
            assert plugin.getId() > 0;

            // verify we have a content-less plugin in the db
            plugin = em.find(ServerPlugin.class, plugin.getId());
            assert plugin != null;
            assert plugin.getId() > 0;
            assert plugin.getName().equals(name);
            assert plugin.getPath().equals(path);
            assert plugin.getDisplayName().equals(displayName);
            assert plugin.isEnabled() == enabled;
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion().equals(version);
            assert plugin.getDescription().equals(description);
            assert plugin.getHelp().equals(help);
            assert plugin.getContent() == null;

            em.close();
            getTransactionManager().commit(); // must commit since we are going to use a second connection now
            getTransactionManager().begin();

            // now stream the content into the plugin's table
            InitialContext context = getInitialContext();
            DataSource ds = (DataSource) context.lookup("java:/RHQDS");
            assert ds != null : "Could not get the data source!";
            conn = ds.getConnection();
            ps = conn.prepareStatement("UPDATE " + ServerPlugin.TABLE_NAME + " SET CONTENT = ? WHERE ID = ?");
            ps.setBinaryStream(1, new ByteArrayInputStream(content), content.length);
            ps.setInt(2, plugin.getId());
            int updateResults = ps.executeUpdate();
            assert updateResults == 1 : "Failed to stream the content blob: " + updateResults;
            ps.close();
            ps = null;
            conn.close();
            conn = null;

            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();

            // verify the content made it into the database via hibernate
            plugin = em.find(ServerPlugin.class, plugin.getId());
            assert new String(plugin.getContent()).equals(new String(content));

            em.close();
            getTransactionManager().commit();
            getTransactionManager().begin();

            // verify the content made it into the database via jdbc streaming
            conn = ds.getConnection();
            ps = conn.prepareStatement("SELECT CONTENT FROM " + ServerPlugin.TABLE_NAME + " WHERE ID = ?");
            ps.setInt(1, plugin.getId());
            rs = ps.executeQuery();
            rs.next();
            InputStream dbStream = rs.getBinaryStream(1);
            assert dbStream != null : "Could not read the plugin content stream from the db";
            byte[] contentFromDb = StreamUtil.slurp(dbStream);
            assert contentFromDb.length == content.length;
            assert new String(contentFromDb).equals(new String(content));
            assert MessageDigestGenerator.getDigestString(new String(contentFromDb)).equals(md5);
            rs.close();
            rs = null;
            ps.close();
            ps = null;
            conn.close();
            conn = null;

            // clean up - delete our test plugin
            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();
            Query q = em.createNamedQuery(ServerPlugin.QUERY_FIND_ANY_BY_NAME);
            q.setParameter("name", plugin.getName());
            ServerPlugin doomed = (ServerPlugin) q.getSingleResult();
            doomed = em.getReference(ServerPlugin.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the plugin";
            em.close();
            getTransactionManager().commit();
            done = true;

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
            if (conn != null) {
                conn.close();
            }
            if (!done) {
                getTransactionManager().rollback();
            }
        }
    }

    public void testPersistStreamContent2() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean done = false;

        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            byte[] content = "this is the test content".getBytes();
            String path = "/test/Persist";

            // persist a content-less plugin
            ServerPlugin plugin = new ServerPlugin("ServerPluginTest-testPersist", path);
            plugin.setDisplayName("Server Plugin Test - testPersist");
            plugin.setEnabled(true);
            plugin.setMD5(MessageDigestGenerator.getDigestString(new String(content)));
            em.persist(plugin);
            assert plugin.getId() > 0;

            em.close();
            getTransactionManager().commit(); // must commit since we are going to use a second connection now
            getTransactionManager().begin();

            // test that we can get a null content stream
            InitialContext context = getInitialContext();
            DataSource ds = (DataSource) context.lookup("java:/RHQDS");
            assert ds != null : "Could not get the data source!";
            conn = ds.getConnection();
            ps = conn.prepareStatement("SELECT PATH, CONTENT FROM " + ServerPlugin.TABLE_NAME + " WHERE ID = ?");
            ps.setInt(1, plugin.getId());
            rs = ps.executeQuery();
            rs.next();
            String dbPath = rs.getString(1);
            assert dbPath.equals(path);
            InputStream dbStream = rs.getBinaryStream(2);
            assert dbStream == null : "Was expecting a null stream but got a non-null stream from db";
            rs.close();
            ps.close();
            conn.close();
            rs = null;
            ps = null;
            conn = null;

            getTransactionManager().commit();
            getTransactionManager().begin();

            // now stream the content into the plugin's table
            conn = ds.getConnection();
            ps = conn.prepareStatement("UPDATE " + ServerPlugin.TABLE_NAME + " SET CONTENT = ? WHERE ID = ?");
            ps.setBinaryStream(1, new ByteArrayInputStream(content), content.length);
            ps.setInt(2, plugin.getId());
            int updateResults = ps.executeUpdate();
            assert updateResults == 1 : "Failed to stream the content blob: " + updateResults;
            ps.close();
            ps = null;
            conn.close();
            conn = null;

            getTransactionManager().commit();
            getTransactionManager().begin();

            // verify we can get the content stream along with another column in the same query
            conn = ds.getConnection();
            ps = conn.prepareStatement("SELECT PATH, CONTENT FROM " + ServerPlugin.TABLE_NAME + " WHERE ID = ?");
            ps.setInt(1, plugin.getId());
            rs = ps.executeQuery();
            rs.next();
            dbPath = rs.getString(1);
            assert dbPath.equals(path);
            dbStream = rs.getBinaryStream(2);
            assert dbStream != null : "Could not read the plugin content stream from the db";
            byte[] contentFromDb = StreamUtil.slurp(dbStream);
            assert contentFromDb.length == content.length;
            assert new String(contentFromDb).equals(new String(content));
            assert MessageDigestGenerator.getDigestString(new String(contentFromDb)).equals(
                MessageDigestGenerator.getDigestString(new String(content)));
            rs.close();
            rs = null;
            ps.close();
            ps = null;
            conn.close();
            conn = null;

            // clean up - delete our test plugin
            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();
            Query q = em.createNamedQuery(ServerPlugin.QUERY_FIND_ANY_BY_NAME);
            q.setParameter("name", plugin.getName());
            ServerPlugin doomed = (ServerPlugin) q.getSingleResult();
            doomed = em.getReference(ServerPlugin.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the plugin";
            getTransactionManager().commit();
            done = true;

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
            if (conn != null) {
                conn.close();
            }
            if (!done) {
                getTransactionManager().rollback();
            }
        }
    }

}
