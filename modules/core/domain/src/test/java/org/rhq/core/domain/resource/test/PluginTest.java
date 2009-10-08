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
import java.util.List;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.testng.annotations.Test;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.core.util.MD5Generator;
import org.rhq.core.util.stream.StreamUtil;

@Test
public class PluginTest extends AbstractEJB3Test {
    public void testUpdate() throws Exception {
        boolean done = false;
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "PluginTest-testUpdate";
            String path = "/test/Update";
            String displayName = "Plugin Test - testUpdate";
            boolean enabled = true;
            String md5 = "abcdef";
            byte[] content = "the content is here".getBytes();

            Plugin plugin = new Plugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setMD5(md5);
            plugin.setVersion(null);
            plugin.setDescription(null);
            plugin.setHelp(null);
            plugin.setContent(content);

            em.persist(plugin);
            id = plugin.getId();
            assert id > 0;

            plugin = em.find(Plugin.class, id);
            assert plugin != null;
            assert plugin.getId() == id;
            assert plugin.getName().equals(name);
            assert plugin.getPath().equals(path);
            assert plugin.getDisplayName().equals(displayName);
            assert plugin.isEnabled() == enabled;
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion() == null;
            assert plugin.getDescription() == null;
            assert plugin.getHelp() == null;
            assert new String(plugin.getContent()).equals(new String(content));

            // everything persisted fine, let's update it and see the content is left alone
            name = name + "-UPDATED";
            path = path + "-UPDATED";
            displayName = displayName + "-UPDATED";
            enabled = !enabled;
            md5 = md5 + "00000";
            String version = "version-UPDATED";
            String description = "description-UPDATED";
            String help = "help-UPDATED";

            em.close();
            getTransactionManager().commit(); // we will be doing an update - needs to be in own tx
            getTransactionManager().begin();
            em = getEntityManager();

            Query q = em.createNamedQuery(Plugin.UPDATE_ALL_BUT_CONTENT);
            q.setParameter("id", id); // same as the one we just persisted
            q.setParameter("name", name);
            q.setParameter("path", path);
            q.setParameter("displayName", displayName);
            q.setParameter("enabled", enabled);
            q.setParameter("md5", md5);
            q.setParameter("version", version);
            q.setParameter("description", description);
            q.setParameter("help", help);
            q.setParameter("mtime", System.currentTimeMillis());
            assert q.executeUpdate() == 1 : "Failed to update the plugin";

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
            assert plugin.getDescription().equals(description);
            assert plugin.getHelp().equals(help);
            // and what we really want to test - ensure the content remained intact after the update
            assert new String(plugin.getContent()).equals(new String(content));

            // clean up - delete our test plugin
            em.close();
            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();
            em.createNativeQuery("DELETE FROM " + Plugin.TABLE_NAME + " WHERE ID = " + id).executeUpdate();
            em.close();
            getTransactionManager().commit();
            done = true;
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
            String md5 = "abcdef";

            Plugin plugin = new Plugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
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
            assert plugin.getMD5().equals(md5);
            assert plugin.getVersion() == null;
            assert plugin.getDescription() == null;
            assert plugin.getHelp() == null;
            assert plugin.getContent() == null;
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testPersistFull() throws Exception {
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
            String md5 = MD5Generator.getDigestString(new String(content));

            Plugin plugin = new Plugin(name, path);
            plugin.setDisplayName(displayName);
            plugin.setEnabled(enabled);
            plugin.setMD5(md5);
            plugin.setVersion(version);
            plugin.setDescription(description);
            plugin.setHelp(help);
            plugin.setContent(content);

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
            assert plugin.getDescription().equals(description);
            assert plugin.getHelp().equals(help);
            assert new String(plugin.getContent()).equals(new String(content));

            // test our queries that purposefully do not load in the content blob
            Query query = em.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
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
            assert plugin.getDescription().equals(description);
            assert plugin.getHelp().equals(help);
            assert plugin.getContent() == null;

            query = em.createNamedQuery(Plugin.QUERY_FIND_ALL);
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
                    assert p.getDescription().equals(description);
                    assert p.getHelp().equals(help);
                    assert p.getContent() == null;
                    break;
                }
            }
            assert got_it : "findAll query failed to get our plugin";

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
            String md5 = MD5Generator.getDigestString(new String(content));

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
            assert MD5Generator.getDigestString(new String(contentFromDb)).equals(md5);
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
            em.createNativeQuery("DELETE FROM " + Plugin.TABLE_NAME + " WHERE ID = " + plugin.getId()).executeUpdate();
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
            plugin.setMD5(MD5Generator.getDigestString(new String(content)));
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
            assert MD5Generator.getDigestString(new String(contentFromDb)).equals(
                MD5Generator.getDigestString(new String(content)));
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
            em.createNativeQuery("DELETE FROM " + Plugin.TABLE_NAME + " WHERE ID = " + plugin.getId()).executeUpdate();
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
}