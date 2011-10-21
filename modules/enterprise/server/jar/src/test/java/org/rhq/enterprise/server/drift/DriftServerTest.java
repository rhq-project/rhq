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

import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.enterprise.server.util.LookupUtil.getSubjectManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.TransactionCallback;

@Test(groups = "drift")
public class DriftServerTest extends AbstractEJB3Test {

    protected final String RESOURCE_TYPE_NAME = getClass().getSimpleName() + "_RESOURCE_TYPE";

    protected final String AGENT_NAME = getClass().getSimpleName() + "_AGENT";

    protected final String RESOURCE_NAME = getClass().getSimpleName() + "_RESOURCE";

    private ServerPluginsLocal serverPluginsMgr;

    protected DriftServerPluginService driftServerPluginService;

    protected TestServerCommunicationsService agentServiceContainer;

    protected ResourceType resourceType;

    protected Agent agent;

    protected Resource resource;

    @BeforeMethod
    public void initServices(Method testMethod) throws Exception {
        initDriftServer();
        initAgentServices();

        InitDB annotation = testMethod.getAnnotation(InitDB.class);
        if (annotation == null || annotation.value()) {
            initDB();
        }
    }

    private void initDriftServer() throws Exception {
        driftServerPluginService = new DriftServerPluginService();
        prepareCustomServerPluginService(driftServerPluginService);
        driftServerPluginService.masterConfig.getPluginDirectory().mkdirs();

        File jpaDriftPlugin = new File("../plugins/drift-rhq/target/rhq-serverplugin-drift-4.1.0-SNAPSHOT.jar");
        assertTrue("Drift server plugin JAR file not found at" + jpaDriftPlugin.getPath(), jpaDriftPlugin.exists());
        FileUtils.copyFileToDirectory(jpaDriftPlugin, driftServerPluginService.masterConfig.getPluginDirectory());

        driftServerPluginService.startMasterPluginContainer();

        serverPluginsMgr = LookupUtil.getServerPlugins();
    }

    private void initAgentServices() {
        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.driftService = new TestDefService();
    }

    @AfterMethod(inheritGroups = true)
    public void shutDownServices() throws Exception {
        shutDownDriftServer();
        shutDownAgentServices();
    }

    private void shutDownDriftServer() throws Exception {
        unprepareServerPluginService();
        driftServerPluginService.stopMasterPluginContainer();
    }

    private void shutDownAgentServices() {
        agentServiceContainer = null;
        unprepareForTestAgents();
    }

    @AfterClass(inheritGroups = true)
    public void cleanUpDB() throws Exception {
        purgeDB();
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB(getEntityManager());
            }
        });
    }

    private void initDB() throws Exception {
        purgeDB();
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                initResourceType();
                initAgent();
                initResource();

                em.persist(resourceType);
                em.persist(agent);
                resource.setAgent(agent);
                em.persist(resource);

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

                deleteEntity(Resource.class, RESOURCE_NAME, em);
                deleteEntity(Agent.class, AGENT_NAME, em);
                deleteEntity(ResourceType.class, RESOURCE_TYPE_NAME, em);

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

    protected void initResourceType() {
        resourceType = new ResourceTypeBuilder().createResourceType()
            .withId(0)
            .withName(RESOURCE_TYPE_NAME)
            .withCategory(SERVER)
            .withPlugin(RESOURCE_TYPE_NAME.toLowerCase())
            .build();
    }

    protected void initAgent() {
        agent = new Agent(AGENT_NAME, "localhost", 1, "", AGENT_NAME + "_TOKEN");
    }

    protected void initResource() {
        resource = new ResourceBuilder().createResource()
            .withId(0)
            .withName(RESOURCE_NAME)
            .withResourceKey(RESOURCE_NAME)
            .withRandomUuid()
            .withResourceType(resourceType)
            .build();
    }

    protected Subject getOverlord() {
        return getSubjectManager().getOverlord();
    }

    protected String toString(DriftDefinition def) {
        return "DriftDefinition[id: " + def.getId() + ", name: " + def.getName() + "]";
    }

    protected String toString(DriftDefinitionTemplate template) {
        return DriftDefinitionTemplate.class.getSimpleName() + "[id: " + template.getId() + ", name: " +
            template.getName() + "]";
    }

    protected Drift findDriftByPath(List<? extends Drift> drifts, String path) {
        for (Drift drift : drifts) {
            if (drift.getPath().equals(path)) {
                return drift;
            }
        }
        return null;
    }
}
