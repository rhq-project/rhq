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

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.apache.commons.io.FileUtils;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.testng.annotations.Test;

@Test(groups = "drift", singleThreaded = true)
public abstract class AbstractDriftServerTest extends AbstractEJB3Test {

    protected final String NAME_PREFIX = getClass().getSimpleName() + "_";

    protected final String RESOURCE_TYPE_NAME = NAME_PREFIX + "RESOURCE_TYPE";

    protected final String AGENT_NAME = NAME_PREFIX + "AGENT";

    protected final String RESOURCE_NAME = NAME_PREFIX + "RESOURCE";

    protected DriftServerPluginService driftServerPluginService;

    protected TestServerCommunicationsService agentServiceContainer;

    protected ResourceType resourceType;

    protected Agent agent;

    protected Resource resource;

    @Override
    protected void beforeMethod(Method testMethod) throws Exception {

        initDriftServer();
        initAgentServices();
        initDB();
    }

    @Override
    protected void afterMethod() throws Exception {
        purgeDB();
        shutDownDriftServer();
        shutDownAgentServices();
    }

    private void initDriftServer() throws Exception {
        driftServerPluginService = new DriftServerPluginService(getTempDir());
        prepareCustomServerPluginService(driftServerPluginService);
        driftServerPluginService.masterConfig.getPluginDirectory().mkdirs();

        String projectVersion = System.getProperty("project.version");

        File jpaDriftPlugin = new File("../plugins/drift-rhq/target/rhq-serverplugin-drift-" + projectVersion + ".jar");
        assertTrue("Drift server plugin JAR file not found at" + jpaDriftPlugin.getPath(), jpaDriftPlugin.exists());
        FileUtils.copyFileToDirectory(jpaDriftPlugin, driftServerPluginService.masterConfig.getPluginDirectory());

        driftServerPluginService.startMasterPluginContainer();
    }

    private void initAgentServices() {
        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.driftService = new TestDefService();
    }

    private void shutDownDriftServer() throws Exception {
        unprepareServerPluginService();
    }

    private void shutDownAgentServices() {
        agentServiceContainer = null;
        unprepareForTestAgents();
    }

    protected void initDB() throws Exception {
        purgeDB();
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                initResourceType();
                initAgent();
                initResource();

                em.persist(resourceType);
                em.persist(agent);
                resource.setAgent(agent);
                em.persist(resource);
            }
        });
    }

    protected void purgeDB() {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                String name = " '" + NAME_PREFIX + "%' ";

                em.createQuery("delete from JPADrift d where d.newDriftFile like" + name).executeUpdate();

                em.createQuery(
                    "delete from JPADriftChangeSet cs where cs.id in ( select cs1.id from JPADriftChangeSet cs1 where cs1.driftDefinition.name like"
                        + name + ")").executeUpdate();

                em.createQuery(
                    "delete from JPADriftChangeSet cs where cs.id in ( select cast(ddt.changeSetId as int) from DriftDefinitionTemplate ddt where ddt.name like"
                        + name + ")").executeUpdate();

                em.createNativeQuery("" //
                    + "delete from rhq_drift_set ds " //
                    + "  where not exists ( select * from rhq_drift d where d.drift_set_id = ds.id ) " //
                    + "    and not exists ( select * from rhq_drift_change_set cs where cs.drift_set_id = ds.id ) ")
                    .executeUpdate();

                em.createQuery("delete from JPADriftFile df where df.hashId like" + name).executeUpdate();

                removeEntities(DriftDefinition.class, name);

                removeEntities(DriftDefinitionTemplate.class, name);

                removeEntity(Resource.class, RESOURCE_NAME);
                removeEntity(Agent.class, AGENT_NAME);
                removeEntity(ResourceType.class, RESOURCE_TYPE_NAME);
            }
        });
    }

    protected void removeEntity(Class<?> clazz, String name) {
        try {
            Object entity = em
                .createQuery("select entity from " + clazz.getSimpleName() + " entity where entity.name = :name")
                .setParameter("name", name).getSingleResult();
            if (clazz.equals(Resource.class)) {
                ResourceTreeHelper.deleteResource(em, (Resource) entity);
            } else {
                em.remove(entity);
            }
        } catch (NoResultException e) {
            // we can ignore no results because this code will run when the db
            // is empty and we expect no results in that case
        } catch (NonUniqueResultException e) {
            // we will fail here to let the person running the test know that
            // the database may not be in a consistent state
            fail("Purging " + name + " failed. Expected to find one instance of " + clazz.getSimpleName()
                + " but found more than one. The database may not be in a consistent state.");
        }
    }

    @SuppressWarnings("unchecked")
    protected void removeEntities(Class<?> clazz, String name) {
        try {
            String query = "select entity from " + clazz.getSimpleName() + " entity where entity.name like :name";
            List<Object> entities = em.createQuery(query).setParameter("name", name).getResultList();
            for (Object entity : entities) {
                if (clazz.equals(Resource.class)) {
                    ResourceTreeHelper.deleteResource(em, (Resource) entity);
                } else {
                    em.remove(entity);
                }
            }
        } catch (NoResultException e) {
            // we can ignore no results because this code will run when the db
            // is empty and we expect no results in that case
        } catch (NonUniqueResultException e) {
            // we will fail here to let the person running the test know that
            // the database may not be in a consistent state
            fail("Purging " + name + " failed. Expected to find one instance of " + clazz.getSimpleName()
                    + " but found more than one. The database may not be in a consistent state.");
        }
    }

    protected void initResourceType() {
        resourceType = new ResourceTypeBuilder().createResourceType().withId(0).withName(RESOURCE_TYPE_NAME)
            .withCategory(SERVER).withPlugin(RESOURCE_TYPE_NAME.toLowerCase()).build();
    }

    protected void initAgent() {
        agent = new Agent(AGENT_NAME, AGENT_NAME, 17080, "", AGENT_NAME + "_TOKEN");
    }

    protected void initResource() {
        resource = new ResourceBuilder().createResource().withId(0).withName(RESOURCE_NAME)
            .withResourceKey(RESOURCE_NAME).withRandomUuid().withResourceType(resourceType).build();
    }

    protected Subject getOverlord() {
        return getSubjectManager().getOverlord();
    }

    protected String toString(DriftDefinition def) {
        return "DriftDefinition[id: " + def.getId() + ", name: " + def.getName() + "]";
    }

    protected String toString(DriftDefinitionTemplate template) {
        return DriftDefinitionTemplate.class.getSimpleName() + "[id: " + template.getId() + ", name: "
            + template.getName() + "]";
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
