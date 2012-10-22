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

import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.core.domain.shared.TransactionCallback;

public class DriftDefinitionTemplateTest extends DriftDataAccessTest {

    private final String RESOURCE_TYPE_NAME = DriftDefinitionTemplateTest.class.getName();

    private ResourceType resourceType;

    @BeforeMethod(groups = { "DriftDefinitionTemplate", "drift.ejb" })
    public void init() {
        if (!inContainer()) {
            return;
        }

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
                createResourceType();
                em.persist(resourceType);
            }
        });
    }

    @AfterClass(groups = { "DriftDefinitionTemplate", "drift.ejb" })
    public void cleanUp() {
        if (!inContainer()) {
            return;
        }

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
            }
        });
    }

    private void purgeDB() {
        List<?> results = em.createQuery("select t from ResourceType t where t.name = :name")
            .setParameter("name", RESOURCE_TYPE_NAME).getResultList();
        if (results.isEmpty()) {
            return;
        }
        ResourceType type = (ResourceType) results.get(0);
        for (DriftDefinitionTemplate template : type.getDriftDefinitionTemplates()) {
            em.remove(template);
        }
        em.remove(type);
    }

    private void createResourceType() {
        resourceType = new ResourceTypeBuilder().createResourceType().withId(0)
            .withName(DriftDefinitionTemplateTest.class.getName()).withCategory(SERVER)
            .withPlugin(DriftDefinitionTemplateTest.class.getName().toLowerCase()).build();
    }

    @Test(groups = { "DriftDefinitionTemplate", "drift.ejb" })
    public void saveAndLoadTemplate() {
        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setResourceType(resourceType);
        template.setChangeSetId("1");

        DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName("saveAndLoadTemplate");
        driftDef.setDescription("Testing save and load");
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        template.setTemplateDefinition(driftDef);

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                em.persist(template);
                em.flush();
                em.clear();

                DriftDefinitionTemplate savedTemplate = em.find(DriftDefinitionTemplate.class, template.getId());
                Assert.assertNotNull(savedTemplate, "Failed to persist " + template.toString(false));
                assertDriftTemplateEquals("Failed to persist template", savedTemplate, template);
                assertPropertiesMatch("Failed to persist " + template.toString(false), template.getResourceType(),
                    resourceType, "driftDefinitionTemplates");
            }
        });
    }

    @Test(groups = { "DriftDefinitionTemplate", "drift.ejb" })
    public void deleteTemplate() {
        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setResourceType(resourceType);
        template.setChangeSetId("1");

        DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName("saveAndLoadTemplate");
        driftDef.setDescription("Testing save and load");
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        template.setTemplateDefinition(driftDef);

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
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
                assertNull("Deleting " + template.toString(false) + " should cascade to its "
                    + "underlying configuration object", config);

                assertNotNull("Deleting " + template.toString(false) + " should not cascade to "
                    + "its parent resource type", em.find(ResourceType.class, resourceType.getId()));
            }
        });
    }

    @Test(groups = { "DriftDefinitionTemplate", "drift.ejb" })
    public void addTemplateToResourceType() {
        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setChangeSetId("1");

        DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName("saveAndLoadTemplate");
        driftDef.setDescription("Testing save and load");
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        template.setTemplateDefinition(driftDef);

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                resourceType.addDriftDefinitionTemplate(template);
                resourceType = em.merge(resourceType);
                em.flush();
                em.clear();

                ResourceType updatedType = em.find(ResourceType.class, resourceType.getId());
                assertFalse("Failed to persist drift definition template", updatedType.getDriftDefinitionTemplates()
                    .isEmpty());

                DriftDefinitionTemplate savedTemplate = updatedType.getDriftDefinitionTemplates().iterator().next();
                assertDriftTemplateEquals("Failed to add template to existing resource type", template, savedTemplate);
            }
        });
    }

    @Test(groups = { "DriftDefinitionTemplate", "drift.ejb" })
    public void deleteResourceTypeShouldCascadeToTemplates() {
        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setResourceType(resourceType);
        template.setChangeSetId("1");

        DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName("cascadeDelete");
        driftDef.setDescription("testing cascade delete");
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        template.setTemplateDefinition(driftDef);

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

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

    @Test(groups = { "DriftDefinitionTemplate", "drift.ejb" })
    public void persistTemplateAndDefinition() {
        final DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName("addDefToTemplate");
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setName("saveAndLoadTemplate");
        template.setDescription("Testing save and load");
        template.setResourceType(resourceType);
        template.setChangeSetId("1");
        template.setTemplateDefinition(driftDef);

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                template.addDriftDefinition(driftDef);
                em.persist(template);
                em.flush();
                em.clear();

                DriftDefinitionTemplate savedTemplate = em.find(DriftDefinitionTemplate.class, template.getId());

                assertNotNull("Failed to persist template", savedTemplate);
                assertEquals("Failed to add definition to template", 1, savedTemplate.getDriftDefinitions().size());

                DriftDefinition savedDefinition = savedTemplate.getDriftDefinitions().iterator().next();
                assertPropertiesMatch("Failed to persist definition", driftDef, savedDefinition, "id", "configuration",
                    "template");
            }
        });
    }

    @Test(groups = { "DriftDefinitionTemplate", "drift.ejb" })
    public void deleteTemplateShouldNotCascadeToDefinitions() {
        final DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName("addDefToTemplate");
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        final DriftDefinitionTemplate template = new DriftDefinitionTemplate();
        template.setName("saveAndLoadTemplate");
        template.setDescription("Testing save and load");
        template.setResourceType(resourceType);
        template.setChangeSetId("1");
        template.setTemplateDefinition(driftDef);

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                em.persist(template);

                driftDef.setTemplate(template);
                em.persist(driftDef);
                em.flush();
                em.clear();

                DriftDefinitionTemplate templateToDelete = em.find(DriftDefinitionTemplate.class, template.getId());
                DriftDefinition def = em.find(DriftDefinition.class, driftDef.getId());
                def.setTemplate(null);
                em.remove(templateToDelete);
                em.flush();
                em.clear();

                Assert.assertNotNull(em.find(DriftDefinition.class, driftDef.getId()),
                    "Deleting the template should not delete its definitions");
            }
        });
    }

    private void assertDriftTemplateEquals(String msg, DriftDefinitionTemplate expected, DriftDefinitionTemplate actual) {
        assertPropertiesMatch(msg + ": basic drift definition template properties do not match", expected, actual,
            "id", "resourceType", "ctime", "templateDefinition");
        assertDriftDefEquals(msg + ": template definitions do not match", expected.getTemplateDefinition(),
            actual.getTemplateDefinition());
    }

    private void assertDriftDefEquals(String msg, DriftDefinition expected, DriftDefinition actual) {
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        assertEquals(msg, 0, comparator.compare(expected, actual));
    }
}
