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
import java.util.Arrays;
import java.util.List;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;

@Test(groups = "integration.ejb3")
public class PluginTest extends AbstractEJB3Test {
    public void testUpdate() throws Throwable {
        boolean done = false;
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "PluginTest-testUpdate";
            String path = "/test/Update";
            String displayName = "Plugin Test - testUpdate";
            boolean enabled = true;
            PluginStatusType status = PluginStatusType.INSTALLED;
            String md5 = "abcdef";
            byte[] content = "the content is here".getBytes();

            Plugin plugin = new Plugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setStatus(status);
            plugin.setMD5(md5);
            plugin.setVersion(null);
            plugin.setDescription(null);
            plugin.setHelp(null);
            plugin.setContent(content);

            Query q = em.createNamedQuery(Plugin.QUERY_GET_STATUS_BY_NAME);
            q.setParameter("name", plugin.getName());
            assert q.getResultList().size() == 0; // not in the db yet

            em.persist(plugin);
            id = plugin.getId();
            assert id > 0;

            q = em.createNamedQuery(Plugin.QUERY_GET_STATUS_BY_NAME);
            q.setParameter("name", plugin.getName());
            assert ((PluginStatusType) q.getSingleResult()) == PluginStatusType.INSTALLED;

            plugin = em.find(Plugin.class, id);
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
            assert plugin.getDeployment() == PluginDeploymentType.AGENT;
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
            PluginDeploymentType deployment = PluginDeploymentType.AGENT;

            em.close();
            getTransactionManager().commit(); // we will be doing an update - needs to be in own tx
            getTransactionManager().begin();
            em = getEntityManager();

            em.flush(); // gotta get those two persists to flush to the DB

            Plugin pluginEntity = em.getReference(Plugin.class, plugin.getId());
            pluginEntity.setName(name);
            pluginEntity.setPath(path);
            pluginEntity.setDisplayName(displayName);
            pluginEntity.setEnabled(enabled);
            pluginEntity.setStatus(status);
            pluginEntity.setMd5(md5);
            pluginEntity.setVersion(version);
            pluginEntity.setAmpsVersion(ampsVersion);
            pluginEntity.setDeployment(deployment);
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

            plugin = em.find(Plugin.class, id);
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
            assert plugin.getDeployment() == PluginDeploymentType.AGENT;
            assert plugin.getHelp().equals(help);
            // and what we really want to test - ensure the content remained intact after the update
            assert new String(plugin.getContent()).equals(new String(content));

            // clean up - delete our test plugin
            em.close();
            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();
            q = em.createNamedQuery(Plugin.QUERY_FIND_ANY_BY_NAME);
            q.setParameter("name", plugin.getName());
            Plugin doomed = (Plugin) q.getSingleResult();
            doomed = em.getReference(Plugin.class, doomed.getId());
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
            String name = "PluginTest-testPersist";
            String path = "/test/Persist";
            String displayName = "Plugin Test - testPersist";
            boolean enabled = true;
            PluginStatusType status = PluginStatusType.INSTALLED;
            String md5 = "abcdef";

            Plugin plugin = new Plugin(name, path);
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

            plugin = em.find(Plugin.class, plugin.getId());
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
            assert plugin.getDeployment() == PluginDeploymentType.AGENT;
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

    public void testPersistFull() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            Query query = em.createNamedQuery(Plugin.QUERY_FIND_ALL_INSTALLED);
            int originalNumberOfPlugins = query.getResultList().size();

            String name = "PluginTest-testPersist";
            String path = "/test/Persist";
            String displayName = "Plugin Test - testPersist";
            boolean enabled = true;
            String version = "1.0";
            String description = "the test description is here";
            String help = "the test help string is here";
            byte[] content = "this is the test content".getBytes();
            String md5 = MessageDigestGenerator.getDigestString(new String(content));
            PluginDeploymentType deployment = PluginDeploymentType.AGENT;
            String ampsVersion = "1.2";

            Configuration pluginConfig = new Configuration();
            Configuration jobsConfig = new Configuration();
            pluginConfig.put(new PropertySimple("first", "last"));
            jobsConfig.put(new PropertySimple("aaa", "bbb"));

            Plugin plugin = new Plugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setMD5(md5);
            plugin.setVersion(version);
            plugin.setAmpsVersion(ampsVersion);
            plugin.setDescription(description);
            plugin.setHelp(help);
            plugin.setContent(content);
            plugin.setDeployment(deployment);

            em.persist(plugin);
            assert plugin.getId() > 0;

            plugin = em.find(Plugin.class, plugin.getId());
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
            assert plugin.getHelp().equals(help);
            assert new String(plugin.getContent()).equals(new String(content));

            // test our queries that purposefully do not load in the content blob
            query = em.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
            query.setParameter("name", name);
            plugin = (Plugin) query.getSingleResult();
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
            assert plugin.getHelp().equals(help);
            assert plugin.getContent() == null;

            query = em.createNamedQuery(Plugin.QUERY_FIND_BY_IDS);
            query.setParameter("ids", Arrays.asList(Integer.valueOf(plugin.getId())));
            plugin = (Plugin) query.getSingleResult();
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
            assert plugin.getHelp().equals(help);
            assert plugin.getContent() == null;

            query = em.createNamedQuery(Plugin.QUERY_FIND_ALL_INSTALLED);
            List<Plugin> all = query.getResultList();
            boolean got_it = false;
            for (Plugin p : all) {
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
                    assert p.getHelp().equals(help);
                    assert p.getContent() == null;
                    break;
                }
            }
            assert got_it : "findAll query failed to get our plugin";

            // mark a plugin deleted - all of our queries should then never see it
            plugin.setStatus(PluginStatusType.DELETED);
            plugin = em.merge(plugin);

            query = em.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
            query.setParameter("name", name);
            List<?> results = query.getResultList();
            assert results.size() == 0;

            query = em.createNamedQuery(Plugin.QUERY_FIND_BY_IDS);
            query.setParameter("ids", Arrays.asList(Integer.valueOf(plugin.getId())));
            results = query.getResultList();
            assert results.size() == 0;

            query = em.createNamedQuery(Plugin.QUERY_FIND_ALL_INSTALLED);
            results = query.getResultList();
            assert results.size() == originalNumberOfPlugins;

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
            String name = "PluginTest-testPersist";
            String path = "/test/Persist";
            String displayName = "Plugin Test - testPersist";
            boolean enabled = true;
            String version = "1.0";
            String description = "the test description is here";
            String help = "the test help string is here";
            byte[] content = "this is the test content".getBytes();
            String md5 = MessageDigestGenerator.getDigestString(new String(content));

            // persist the plugin, but without any content
            Plugin plugin = new Plugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setMD5(md5);
            plugin.setVersion(version);
            plugin.setDescription(description);
            plugin.setHelp(help);

            em.persist(plugin);
            assert plugin.getId() > 0;

            // verify we have a content-less plugin in the db
            plugin = em.find(Plugin.class, plugin.getId());
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
            ps = conn.prepareStatement("UPDATE " + Plugin.TABLE_NAME + " SET CONTENT = ? WHERE ID = ?");
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
            plugin = em.find(Plugin.class, plugin.getId());
            assert new String(plugin.getContent()).equals(new String(content));

            em.close();
            getTransactionManager().commit();
            getTransactionManager().begin();

            // verify the content made it into the database via jdbc streaming
            conn = ds.getConnection();
            ps = conn.prepareStatement("SELECT CONTENT FROM " + Plugin.TABLE_NAME + " WHERE ID = ?");
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
            Query q = em.createNamedQuery(Plugin.QUERY_FIND_ANY_BY_NAME);
            q.setParameter("name", plugin.getName());
            Plugin doomed = (Plugin) q.getSingleResult();
            doomed = em.getReference(Plugin.class, doomed.getId());
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
            Plugin plugin = new Plugin("PluginTest-testPersist", path);
            plugin.setDisplayName("Plugin Test - testPersist");
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
            ps = conn.prepareStatement("SELECT PATH, CONTENT FROM " + Plugin.TABLE_NAME + " WHERE ID = ?");
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
            ps = conn.prepareStatement("UPDATE " + Plugin.TABLE_NAME + " SET CONTENT = ? WHERE ID = ?");
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
            ps = conn.prepareStatement("SELECT PATH, CONTENT FROM " + Plugin.TABLE_NAME + " WHERE ID = ?");
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
            Query q = em.createNamedQuery(Plugin.QUERY_FIND_ANY_BY_NAME);
            q.setParameter("name", plugin.getName());
            Plugin doomed = (Plugin) q.getSingleResult();
            doomed = em.getReference(Plugin.class, doomed.getId());
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
