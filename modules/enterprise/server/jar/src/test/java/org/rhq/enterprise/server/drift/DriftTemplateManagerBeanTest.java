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

import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftTemplateManager;
import static org.rhq.enterprise.server.util.LookupUtil.getSubjectManager;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.test.TransactionCallback;

public class DriftTemplateManagerBeanTest extends AbstractEJB3Test {

    private final String RESOURCE_TYPE_NAME = DriftTemplateManagerBeanTest.class.getName();

    private final String AGENT_NAME = DriftTemplateManagerBeanTest.class.getName() + "_AGENT";

    private ResourceType resourceType;

    private Agent agent;

    private DriftTemplateManagerLocal templateMgr;

    private DriftManagerLocal driftMgr;

    private List<Resource> resources;

    @BeforeClass(groups = "drift-template")
    public void initClass() {
        templateMgr = getDriftTemplateManager();
        driftMgr = getDriftManager();

        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.driftService = new TestDefService();
    }

    @BeforeMethod(groups = "drift-template")
    public void initDB() {
        resources = new LinkedList<Resource>();
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
                initResourceType();
                initAgent();

                EntityManager em = getEntityManager();
                em.persist(resourceType);
                em.persist(agent);
            }
        });
    }

    @AfterClass(groups = "drift-template")
    public void resetDB() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
            }
        });
        unprepareForTestAgents();
    }

    @SuppressWarnings("unchecked")
    private void purgeDB() {
        EntityManager em = getEntityManager();

        // purge resources
        for (Resource resource : resources) {
            em.remove(resource);
        }

        // purge resource type
        List results = em.createQuery("select t from ResourceType t where t.name = :name").setParameter("name",
            RESOURCE_TYPE_NAME).getResultList();
        if (!results.isEmpty()) {
            ResourceType type = (ResourceType) results.get(0);
            for (DriftDefinitionTemplate template : type.getDriftDefinitionTemplates()) {
                em.remove(template);
            }
            em.remove(type);
        }
        // purge agent
        List<Agent> agents = (List<Agent>) em.createQuery("select a from Agent a where a.name = :name").setParameter(
            "name", AGENT_NAME).getResultList();
        if (!agents.isEmpty()) {
            Agent agent = agents.get(0);
            em.remove(agent);
        }
    }

    private void initResourceType() {
        resourceType = new ResourceTypeBuilder().createResourceType().withId(0).withName(
            DriftTemplateManagerBeanTest.class.getName()).withCategory(SERVER).withPlugin(
            DriftTemplateManagerBeanTest.class.getName().toLowerCase()).build();
    }

    private void initAgent() {
        agent = new Agent(AGENT_NAME, "localhost", 1, "", AGENT_NAME + "_TOKEN");
    }

    @Test(groups = "drift-template")
    public void createNewTemplate() {
        final DriftDefinition definition = new DriftDefinition(new Configuration());
        definition.setName("test::createNewTemplate");
        definition.setEnabled(true);
        definition.setDriftHandlingMode(normal);
        definition.setInterval(2400L);
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        templateMgr.createTemplate(getOverlord(), resourceType.getId(), true, definition);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                ResourceType updatedType = em.find(ResourceType.class, resourceType.getId());

                assertEquals("Failed to add new drift definition to resource type", 1, updatedType
                    .getDriftDefinitionTemplates().size());

                DriftDefinitionTemplate newTemplate = updatedType.getDriftDefinitionTemplates().iterator().next();

                DriftDefinitionTemplate expectedTemplate = new DriftDefinitionTemplate();
                expectedTemplate.setTemplateDefinition(definition);

                assertDriftTemplateEquals("Failed to save template", expectedTemplate, newTemplate);
            }
        });
    }

    // Note: This test is going to change substantially in terms of the behavior that it
    // is verifying because it was written before the design has been fully flushed out.
    @Test(groups = "drift-template")
    public void updateTemplateNameAndDescription() {
        // first create a template
        final DriftDefinition definition = new DriftDefinition(new Configuration());
        definition.setName("test::updateNameAndDescription");
        definition.setDescription("testing updating template name and description");
        definition.setEnabled(true);
        definition.setDriftHandlingMode(normal);
        definition.setInterval(2400L);
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        templateMgr.createTemplate(getOverlord(), resourceType.getId(), true, definition);

        // perform the update
        DriftDefinitionTemplate template = loadTemplate(definition.getName());
        String updatedName = "UPDATED NAME";
        template.setName(updatedName);
        template.setDescription("UPDATED DESCRIPTION");

        templateMgr.updateTemplate(getOverlord(), template, false);

        // verify that the update was made
        DriftDefinitionTemplate updatedTemplate = loadTemplate(updatedName);

        assertDriftTemplateEquals("Failed to update template", template, updatedTemplate);
    }

    // Note: This test is going to change substantially in terms of the behavior that it
    // is verifying because it was written before the design has been fully flushed out.
    @Test(groups = "drift-template")
    public void updateTemplateEnabledFlagAndIntervalAndApplyToDefs() {
        // create a new template
        final DriftDefinition definition = new DriftDefinition(new Configuration());
        definition.setName("test::updateEnabledFlagAndInterval");
        definition.setDescription("test updating enabled flag and interval");
        definition.setEnabled(false);
        definition.setDriftHandlingMode(normal);
        definition.setInterval(2400L);
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        templateMgr.createTemplate(getOverlord(), resourceType.getId(), true, definition);

        // create some definitions
        DriftDefinitionTemplate template = loadTemplate(definition.getName());
        Resource resource = createResource();

        final DriftDefinition def1 = template.createDefinition();
        def1.setName("def 1");
        def1.setTemplate(template);
        def1.setResource(resource);

        final DriftDefinition def2 = template.createDefinition();
        def2.setName("def 2");
        def2.setTemplate(template);
        def2.setResource(resource);

        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(resource.getId()), def1);
        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(resource.getId()), def2);

        // perform the update
        template.getTemplateDefinition().setEnabled(false);
        template.getTemplateDefinition().setInterval(3600L);
        templateMgr.updateTemplate(getOverlord(), template, true);
    }

    private Subject getOverlord() {
        return getSubjectManager().getOverlord();
    }

    private Resource createResource() {
        int index = resources.size();
        final Resource resource = new ResourceBuilder().createResource().withId(0).withName(
            getClass().getSimpleName() + "_" + index).withResourceKey(getClass().getSimpleName() + "_" + index)
            .withUuid(getClass().getSimpleName() + "_" + index).withResourceType(resourceType).build();

        resources.add(resource);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                resource.setAgent(agent);
                em.persist(resource);
            }
        });

        return resource;
    }

    private DriftDefinitionTemplate loadTemplate(String name) {
        DriftDefinitionTemplateCriteria criteria = new DriftDefinitionTemplateCriteria();
        criteria.addFilterResourceTypeId(resourceType.getId());
        criteria.addFilterName(name);
        criteria.fetchDriftDefinitions(true);

        PageList<DriftDefinitionTemplate> templates = templateMgr.findTemplatesByCriteria(getOverlord(), criteria);
        assertEquals("Expected to find one template", 1, templates.size());

        return templates.get(0);
    }

    private void assertDriftTemplateEquals(String msg, DriftDefinitionTemplate expected, DriftDefinitionTemplate actual) {
        assertPropertiesMatch(msg + ": basic drift definition template properties do not match", expected, actual,
            "id", "resourceType", "ctime", "templateDefinition");
        assertDriftDefEquals(msg + ": template definitions do not match", expected.getTemplateDefinition(), actual
            .getTemplateDefinition());
    }

    private void assertDriftDefEquals(String msg, DriftDefinition expected, DriftDefinition actual) {
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        assertEquals(msg, 0, comparator.compare(expected, actual));
    }

}
