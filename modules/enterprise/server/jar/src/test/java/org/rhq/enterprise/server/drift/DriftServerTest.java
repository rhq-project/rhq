/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.drift;

import java.io.File;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;

import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.TransactionCallback;

public class DriftServerTest extends AbstractEJB3Test {

    private ServerPluginsLocal serverPluginsMgr;

    private DriftServerPluginService driftServerPluginService;

    protected TestServerCommunicationsService agentServiceContainer;

    @BeforeGroups(groups = "drift.server")
    public void initDriftServer() throws Exception {
        driftServerPluginService = new DriftServerPluginService();
        prepareCustomServerPluginService(driftServerPluginService);
        driftServerPluginService.masterConfig.getPluginDirectory().mkdirs();

        File jpaDriftPlugin = new File("../plugins/drift-rhq/target/rhq-serverplugin-drift-4.1.0-SNAPSHOT.jar");
        assertTrue("Drift server plugin JAR file not found at" + jpaDriftPlugin.getPath(), jpaDriftPlugin.exists());
        FileUtils.copyFileToDirectory(jpaDriftPlugin, driftServerPluginService.masterConfig.getPluginDirectory());

        driftServerPluginService.startMasterPluginContainer();

        serverPluginsMgr = LookupUtil.getServerPlugins();
    }

    @AfterGroups
    public void shutDownDriftServer() {
        driftServerPluginService.stopMasterPluginContainer();
    }

    @AfterClass(groups = {"drift.ejb", "drift.server"})
    public void cleanUpDB() throws Exception {
        unprepareServerPluginService();
        purgeDB();
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB(getEntityManager());
            }
        });
    }

    @BeforeMethod(groups = {"drift.ejb", "drift.server"})
    public void initDB() {
        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.driftService = new TestDefService();

        purgeDB();
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                initDB(em);
            }
        });
    }

    private void purgeDB() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                em.createQuery("delete from JPADrift ").executeUpdate();
                em.createQuery("delete from JPADriftChangeSet").executeUpdate();
                em.createQuery("delete from JPADriftSet").executeUpdate();
                em.createQuery("delete from JPADriftFile").executeUpdate();
                em.createQuery("delete from DriftDefinition").executeUpdate();
                em.createQuery("delete from DriftDefinitionTemplate").executeUpdate();

                purgeDB(em);
            }
        });
    }

    protected void purgeDB(EntityManager em) {
    }

    protected void initDB(EntityManager em) {
    }

    protected void deleteEntity(Class<?> clazz, String name, EntityManager em) {
        try {
            Object entity = em.createQuery(
                "select entity from " + clazz.getSimpleName() + " entity where entity.name = :name")
                .setParameter("name", name)
                .getSingleResult();
            em.remove(entity);
        } catch (NoResultException e) {
            // we can ignore no results because this code will run when the db
            // is empty and we expect no results in that case
        } catch (NonUniqueResultException e) {
            // we will fail here to let the person running the test know that
            // the database may not be in a consistent state
            fail("Purging " + name + " failed. Expected to find one instance of " + clazz.getSimpleName() +
                " but found more than one. The database may not be in a consistent state.");
        }
    }

}
