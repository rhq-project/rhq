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

package org.rhq.core.domain.drift;

import java.util.List;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.rhq.test.TransactionCallback;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

public class DriftDefinitionTemplateTest extends AbstractEJB3Test {

    private final String RESOURCE_TYPE_NAME = DriftDefinitionTemplateTest.class.getName();

    private ResourceType resourceType;

    @BeforeMethod(groups = "DriftDefinitionTemplate")
    public void initDB() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
                createResourceType();
                getEntityManager().persist(resourceType);
            }
        });
    }

    @AfterClass
    public void resetDB() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
            }
        });
    }

    private void purgeDB() {
        EntityManager em = getEntityManager();
         List results =  em.createQuery("select id from ResourceType where name = :name")
            .setParameter("name", RESOURCE_TYPE_NAME)
            .getResultList();
        if (results.isEmpty()) {
            return;
        }
        Integer resourceTypeId = (Integer) results.get(0);

        em.createQuery(
            "delete from DriftDefinitionTemplate template " +
                "where template.resourceType.id = :resourceTypeId")
            .setParameter("resourceTypeId", resourceTypeId)
            .executeUpdate();
        em.createQuery("delete from ResourceType where id = :id")
            .setParameter("id", resourceTypeId)
            .executeUpdate();
    }

    private void createResourceType() {
        resourceType = new ResourceTypeBuilder().createResourceType()
            .withId(0)
            .withName(DriftDefinitionTemplateTest.class.getName())
            .withCategory(SERVER)
            .withPlugin(DriftDefinitionTemplateTest.class.getName().toLowerCase())
            .build();
    }

    @Test(groups = {"DriftDefinitionTemplate", "integration.ejb3"})
    public void saveAndLoadTemplate() {
        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setName("saveAndLoadTemplate");
        template.setDescription("Testing save and load");
        template.setResourceType(resourceType);
        template.setChangeSetId("1");

        DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        template.setConfiguration(driftDef.getConfiguration());

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(template);
                em.flush();
                em.clear();

                DriftDefinitionTemplate savedTemplate = em.find(DriftDefinitionTemplate.class, template.getId());
                assertNotNull("Failed to persist " + template.toString(false), savedTemplate);
                assertPropertiesMatch("Failed to persist " + template.toString(false), template,
                    savedTemplate, "resourceType");
                assertPropertiesMatch("Failed to persist " + template.toString(false),
                    template.getResourceType(), resourceType, "driftDefinitionTemplates");
            }
        });
    }

    @Test(groups = {"DriftDefinitionTemplate", "integration.ejb3"})
    public void deleteTemplate() {
        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setName("saveAndLoadTemplate");
        template.setDescription("Testing save and load");
        template.setResourceType(resourceType);
        template.setChangeSetId("1");

        DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        template.setConfiguration(driftDef.getConfiguration());

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(template);
                em.flush();
                em.clear();

                DriftDefinitionTemplate savedTemplate = em.find(DriftDefinitionTemplate.class, template.getId());
                em.remove(savedTemplate);
                em.flush();
                em.clear();

                DriftDefinitionTemplate deletedTemplate = em.find(DriftDefinitionTemplate.class, template.getId());
                assertNull("Failed to delete" + template.toString(false), deletedTemplate);

                Configuration config = em.find(Configuration.class, template.getConfiguration().getId());
                assertNull("Deleting " + template.toString(false) + " should cascade to its " +
                    "underlying configuration object", config);

                assertNotNull("Deleting " + template.toString(false) + " should not cascade to " +
                    "its parent resource type", em.find(ResourceType.class, resourceType.getId()));
            }
        });
    }

    @Test(groups = {"DriftDefinitionTemplate", "integration.ejb3"})
    public void addTemplateToResourceType() {
        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setName("saveAndLoadTemplate");
        template.setDescription("Testing save and load");
        template.setChangeSetId("1");

        DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        template.setConfiguration(driftDef.getConfiguration());

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                resourceType.addDriftDefinitionTemplate(template);
                resourceType = em.merge(resourceType);
                em.flush();
                em.clear();

                ResourceType updatedType = em.find(ResourceType.class, resourceType.getId());

                assertCollectionMatchesNoOrder("Failed to persist drift definition template when updating resource " +
                    "type", asList(template), updatedType.getDriftDefinitionTemplates(), "id", "resourceType");
            }
        });
    }

    @Test(groups = {"DriftDefinitionTemplate", "integration.ejb3"})
    public void deleteResourceTypeShouldCascadeToTemplates() {
        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setName("saveAndLoadTemplate");
        template.setDescription("Testing save and load");
        template.setResourceType(resourceType);
        template.setChangeSetId("1");

        DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        template.setConfiguration(driftDef.getConfiguration());

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                em.persist(template);
                em.flush();
                em.clear();

                ResourceType typeToDelete = em.find(ResourceType.class, resourceType.getId());
                em.remove(typeToDelete);
                em.flush();
                em.clear();

                assertNull("Deleting " + resourceType + " should have cascaded to " + template.toString(false),
                    em.find(DriftDefinitionTemplate.class, template.getId()));
            }
        });
    }
}
