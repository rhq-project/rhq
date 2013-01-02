/*
 * RHQ Management Platform
 * Copyright (C) 2011-2012 Red Hat, Inc.
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

package org.rhq.core.domain.drift;

import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;

import java.util.List;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.core.domain.shared.TransactionCallback;

public class JPADriftChangeSetTest extends DriftDataAccessTest {

    private final String RESOURCE_TYPE_NAME = JPADriftChangeSetTest.class.getName();

    private final String DRIFT_DEFINITION_NAME = JPADriftChangeSetTest.class.getName();

    private ResourceType resourceType;

    private Resource resource;

    private int resourceCount;

    private DriftDefinition definition;

    @BeforeMethod(groups = {"JPADriftChangeSet", "drift.ejb"})
    public void init() {
        if (!inContainer()) {
            return;
        }

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                try {
                    purgeDB();

                    EntityManager em = getEntityManager();

                    resourceType = createResourceType();
                    em.persist(resourceType);

                    resource = createResource(resourceType);

                    definition = createDriftDefinition();
                    resource.addDriftDefinition(definition);
                    getEntityManager().persist(resource);

                } catch (Exception e) {
                    System.out.println("BEFORE METHOD FAILURE, TEST DID NOT RUN!!!");
                    e.printStackTrace();
                    throw e;
                }
            }
        });
    }

    private void purgeDB() {
        EntityManager em = getEntityManager();

        List<Availability> avails = (List<Availability>) em.createQuery("SELECT a FROM Availability a").getResultList();
        for (Availability a : avails) {
            em.remove(a);
        }

        List<Resource> resources = (List<Resource>) em.createQuery("from Resource where resourceType.name = :name")
            .setParameter("name", RESOURCE_TYPE_NAME).getResultList();
        for (Resource resource : resources) {
            em.remove(resource);
        }

        List<ResourceType> resourceTypes = (List<ResourceType>) em.createQuery("from ResourceType where name = :name")
            .setParameter("name", RESOURCE_TYPE_NAME).getResultList();
        for (ResourceType type : resourceTypes) {
            em.remove(type);
        }
    }

    private ResourceType createResourceType() {
        return new ResourceTypeBuilder().createResourceType().withId(0).withName(JPADriftChangeSetTest.class.getName())
            .withCategory(SERVER).withPlugin(JPADriftChangeSetTest.class.getName().toLowerCase()).build();
    }

    private Resource createResource(ResourceType type) {
        return new ResourceBuilder().createResource().withId(0)
            .withName(JPADriftChangeSetTest.class.getSimpleName() + "_" + resourceCount++)
            .withResourceKey(JPADriftChangeSetTest.class.getSimpleName() + "_" + resourceCount)
            .withUuid(JPADriftChangeSetTest.class.getSimpleName() + "_" + resourceCount)
            .withResourceType(type)
            .build();
    }

    private DriftDefinition createDriftDefinition() {
        DriftDefinition def = new DriftDefinition(new Configuration());
        def.setName(DRIFT_DEFINITION_NAME);
        def.setEnabled(true);
        def.setDriftHandlingMode(normal);
        def.setInterval(1800L);
        def.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        return def;
    }

    @Test(groups = {"JPADriftChangeSet", "drift.ejb"})
    public void saveAndLoadInitialChangeSet() {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                JPADriftChangeSet changeSet = new JPADriftChangeSet(resource, 0, COVERAGE, definition);
                changeSet.setDriftHandlingMode(DriftHandlingMode.normal);

                em.persist(changeSet);
                em.flush();

                JPADriftSet driftSet = new JPADriftSet();
                driftSet.addDrift(new JPADrift(changeSet, "drift.1", FILE_ADDED, null, null));

                em.persist(driftSet);
                changeSet.setInitialDriftSet(driftSet);
                em.merge(changeSet);
                em.flush();
                em.clear();


                JPADriftChangeSet savedChangeSet = em
                    .find(JPADriftChangeSet.class, Integer.parseInt(changeSet.getId()));
                assertNotNull("Failed to persist change set", savedChangeSet);

                JPADriftSet savedDriftSet = savedChangeSet.getInitialDriftSet();
                assertNotNull("Failed to persist drift set", savedDriftSet);
                assertEquals("Failed to persist drift belonging to drift set", 1, savedDriftSet.getDrifts().size());
            }
        });
    }
}
