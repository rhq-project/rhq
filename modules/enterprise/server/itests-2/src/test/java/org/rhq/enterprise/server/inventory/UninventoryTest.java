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

import java.util.List;
import java.util.Random;

import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinition.BaseDirectory;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
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

    @Override
    protected void afterMethod() throws Exception {
        if (newResource != null) {
            deleteNewResource(newResource);
            newResource = null;
        }
    }

    public void testDriftDefRemoval() throws Exception {
        ResourceTypeCreator rtCreator = new ResourceTypeCreator() {
            public void modifyResourceTypeToPersist(ResourceType resourceType) {
                DriftDefinitionTemplate template = new DriftDefinitionTemplate();
                template.setName("drift1");
                template.setDescription("drift def template");
                Configuration config = new Configuration();
                DriftDefinition driftDef = new DriftDefinition(config);
                driftDef.setBasedir(new BaseDirectory(BaseDirValueContext.fileSystem, "/"));
                driftDef.setName("drift1");
                template.setTemplateDefinition(driftDef);
                resourceType.addDriftDefinitionTemplate(template);
            }
        };
        ResourceCreator rCreator = new ResourceCreator() {
            public void modifyResourceToPersist(Resource resource) {
                Configuration config = new Configuration();
                DriftDefinition driftDef = new DriftDefinition(config);
                driftDef.setBasedir(new BaseDirectory(BaseDirValueContext.fileSystem, "/boo"));
                driftDef.setName("drift-def-name");
                resource.addDriftDefinition(driftDef);
            }
        };

        Resource resource = createNewResource(rtCreator, rCreator);

        int templateId = resource.getResourceType().getDriftDefinitionTemplates().iterator().next().getId();
        int driftDefId = resource.getDriftDefinitions().iterator().next().getId();

        // sanity check, make sure things are in the DB now
        Query qTemplate;
        Query qDef;
        String qTemplateString = "select t from DriftDefinitionTemplate t where t.id = :id";
        String qDefString = "select dc from DriftDefinition dc where dc.id = :id";

        getTransactionManager().begin();
        try {
            qTemplate = getEntityManager().createQuery(qTemplateString).setParameter("id", templateId);
            qDef = getEntityManager().createQuery(qDefString).setParameter("id", driftDefId);
            assertEquals("drift template didn't get added", 1, qTemplate.getResultList().size());
            assertEquals("drift template def didn't get added", 1, qDef.getResultList().size());
        } finally {
            getTransactionManager().commit();
        }

        // uninventory the resource
        deleteNewResource(resource);

        // make sure things purged
        getTransactionManager().begin();
        try {
            qTemplate = getEntityManager().createQuery(qTemplateString).setParameter("id", templateId);
            qDef = getEntityManager().createQuery(qDefString).setParameter("id", driftDefId);
            assertEquals("drift template didn't get purged", 0, qTemplate.getResultList().size());
            assertEquals("drift template def didn't get purged", 0, qDef.getResultList().size());
        } finally {
            getTransactionManager().commit();
        }
    }

    private Resource createNewResource(ResourceTypeCreator rtCreator, ResourceCreator rCreator) throws Exception {
        getTransactionManager().begin();

        Resource resource;

        try {
            ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                ResourceCategory.PLATFORM, null);

            if (rtCreator != null) {
                rtCreator.modifyResourceTypeToPersist(resourceType);
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
                rCreator.modifyResourceToPersist(resource);
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

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (null != resource) {

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
            }
        }
    }

    private interface ResourceTypeCreator {
        void modifyResourceTypeToPersist(ResourceType resourceType);
    }

    private interface ResourceCreator {
        void modifyResourceToPersist(Resource resource);
    }
}