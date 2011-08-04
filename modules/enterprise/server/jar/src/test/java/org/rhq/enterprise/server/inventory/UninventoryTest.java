/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.inventory;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfiguration.BaseDirectory;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test to see that uninventorying works.
 */
@Test
public class UninventoryTest extends AbstractEJB3Test {
    private Resource newResource;

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        if (newResource != null) {
            deleteNewResource(newResource);
            newResource = null;
        }
    }

    public void testDriftConfigRemoval() throws Exception {
        ResourceTypeCreator rtCreator = new ResourceTypeCreator() {
            public void modifyResourceTypeToPersist(ResourceType resourceType, EntityManager em) {
                ConfigurationTemplate template = new ConfigurationTemplate("drift1", "drift config template");
                Configuration config = new Configuration();
                DriftConfiguration driftConfig = new DriftConfiguration(config);
                driftConfig.setBasedir(new BaseDirectory(BaseDirValueContext.fileSystem, "/"));
                driftConfig.setName("drift1");
                template.setConfiguration(driftConfig.getConfiguration());
                resourceType.addDriftConfigurationTemplate(template);
            }
        };
        ResourceCreator rCreator = new ResourceCreator() {
            public void modifyResourceToPersist(Resource resource, EntityManager em) {
                Configuration config = new Configuration();
                DriftConfiguration driftConfig = new DriftConfiguration(config);
                driftConfig.setBasedir(new BaseDirectory(BaseDirValueContext.fileSystem, "/boo"));
                driftConfig.setName("drift-config-name");
                Set<Configuration> driftConfigSet = new HashSet<Configuration>(1);
                driftConfigSet.add(driftConfig.getConfiguration());
                resource.setDriftConfigurations(driftConfigSet);
            }
        };

        Resource resource = createNewResource(rtCreator, rCreator);

        int templateId = resource.getResourceType().getDriftConfigurationTemplates().iterator().next().getId();
        int driftConfigId = resource.getDriftConfigurations().iterator().next().getId();

        // sanity check, make sure things are in the DB now
        Query qTemplate;
        Query qConfig;
        String qTemplateString = "select ct from ConfigurationTemplate ct where ct.id = :id";
        String qConfigString = "select c from Configuration c where c.id = :id";

        getTransactionManager().begin();
        try {
            qTemplate = getEntityManager().createQuery(qTemplateString).setParameter("id", templateId);
            qConfig = getEntityManager().createQuery(qConfigString).setParameter("id", driftConfigId);
            assertEquals("drift template didn't get added", 1, qTemplate.getResultList().size());
            assertEquals("drift template config didn't get added", 1, qConfig.getResultList().size());
        } finally {
            getTransactionManager().commit();
        }

        // uninventory the resource
        deleteNewResource(resource);

        // make sure things purged
        getTransactionManager().begin();
        try {
            qTemplate = getEntityManager().createQuery(qTemplateString).setParameter("id", templateId);
            qConfig = getEntityManager().createQuery(qConfigString).setParameter("id", driftConfigId);
            assertEquals("drift template didn't get purged", 0, qTemplate.getResultList().size());
            assertEquals("drift template config didn't get purged", 0, qConfig.getResultList().size());
        } finally {
            getTransactionManager().commit();
        }
    }

    private Resource createNewResource(ResourceTypeCreator rtCreator, ResourceCreator rCreator) throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                    ResourceCategory.PLATFORM, null);

                if (rtCreator != null) {
                    rtCreator.modifyResourceTypeToPersist(resourceType, em);
                }

                em.persist(resourceType);

                Agent agent = new Agent("testagent", "testaddress", 1, "", "testtoken");
                em.persist(agent);
                em.flush();

                resource = new Resource("reskey" + System.currentTimeMillis(), "resname", resourceType);
                resource.setUuid("" + new Random().nextInt());
                resource.setAgent(agent);
                resource.setInventoryStatus(InventoryStatus.COMMITTED);
                if (rCreator != null) {
                    rCreator.modifyResourceToPersist(resource, em);
                }
                em.persist(resource);

            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            em.flush();
            getTransactionManager().commit();
            newResource = resource;
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (null != resource) {
            EntityManager em = null;

            try {
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

                // invoke bulk delete on the resource to remove any dependencies not defined in the hibernate entity model
                // perform in-band and out-of-band work in quick succession
                Subject overlord = LookupUtil.getSubjectManager().getOverlord();
                List<Integer> deletedIds = resourceManager.uninventoryResource(overlord, resource.getId());
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.uninventoryResourceAsyncWork(overlord, deletedResourceId);
                }

                // now dispose of other hibernate entities
                getTransactionManager().begin();
                em = getEntityManager();

                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Agent agent = em.find(Agent.class, resource.getAgent().getId());
                if (null != agent) {
                    em.remove(agent);
                }
                if (null != type) {
                    em.remove(type);
                }

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST (" + this.getClass().getSimpleName() + ") Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }
            } finally {
                if (null != em) {
                    em.close();
                }
            }
        }
    }

    private interface ResourceTypeCreator {
        void modifyResourceTypeToPersist(ResourceType resourceType, EntityManager em);
    }

    private interface ResourceCreator {
        void modifyResourceToPersist(Resource resource, EntityManager em);
    }
}