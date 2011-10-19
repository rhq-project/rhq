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

import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftTemplateManager;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeClass;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.drift.JPADriftSet;
import org.rhq.core.domain.util.PageList;
import org.rhq.test.TransactionCallback;

public class ManageDriftDefinitionsTest extends DriftServerTest {

    private DriftManagerLocal driftMgr;

    private DriftTemplateManagerLocal templateMgr;

    @BeforeClass
    public void initClass() throws Exception {
        driftMgr = getDriftManager();
        templateMgr = getDriftTemplateManager();
    }

    public void createDefinitionFromUnpinnedTemplate() {
        // first create a template
        final DriftDefinition templateDef = new DriftDefinition(new Configuration());
        templateDef.setName("test_createUnpinnedDefinition");
        templateDef.setEnabled(true);
        templateDef.setDriftHandlingMode(normal);
        templateDef.setInterval(2400L);
        templateDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        // persist the template
        DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(), resourceType.getId(), false,
            templateDef);

        // create and persist the definition
        DriftDefinition definition = new DriftDefinition(template.getConfiguration());
        definition.setTemplate(template);
        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(resource.getId()), definition);

        // verify that the definition was created
        DriftDefinition newDef = loadDefinition(definition.getName());
        DriftDefinitionComparator comparator = new  DriftDefinitionComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);

        assertEquals("The drift definition was not persisted correctly", 0, comparator.compare(definition, newDef));
        assertEquals("The template association was not set on the definition", template, newDef.getTemplate());
    }

    public void createDefinitionFromPinnedTemplate() {
        // We first need to create a pinned template. Users can only create a pinned
        // template from a snapshot of an existing resource-level drift definition.
        // We are going to take a bit of a short cut though by directly creating
        // and persisting the pinned change set.

        // first create the change set
        final JPADriftFile driftFile1 = new JPADriftFile("a1b2c3");
        final JPADriftFile driftFile2 = new JPADriftFile("1a2b3c");

        JPADrift drift1 = new JPADrift(null, "drift.1", FILE_ADDED, null, driftFile1);
        JPADrift drift2 = new JPADrift(null, "drift.2", FILE_ADDED, null, driftFile2);

        JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift1);
        driftSet.addDrift(drift2);

        final JPADriftChangeSet changeSet0 = new JPADriftChangeSet(resource, 0, COVERAGE, null);
        changeSet0.setInitialDriftSet(driftSet);
        changeSet0.setDriftHandlingMode(DriftConfigurationDefinition.DriftHandlingMode.normal);

        // create the template
        final DriftDefinition templateDef = new DriftDefinition(new Configuration());
        templateDef.setName("test_createUnpinnedDefinition");
        templateDef.setEnabled(true);
        templateDef.setDriftHandlingMode(normal);
        templateDef.setInterval(2400L);
        templateDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        templateDef.setPinned(true);

        final DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(), resourceType.getId(), true,
            templateDef);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                em.persist(driftFile1);
                em.persist(driftFile2);
                em.persist(changeSet0);

                // setting the change set id on the template is the last and the
                // most important step in making the template pinned
                template.setChangeSetId(changeSet0.getId());
                em.merge(template);
            }
        });

        // Create and persist a resource-level definition.
        DriftDefinition definition = template.createDefinition();
        definition.setTemplate(template);
        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(resource.getId()), definition);

        DriftDefinition newDef = loadDefinition(definition.getName());

        // verify that the definition is marked as pinned
        assertTrue("The drift definition should be marked as pinned", newDef.isPinned());
    }

    private DriftDefinition loadDefinition(String name) {
        DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
        criteria.addFilterResourceIds(resource.getId());
        criteria.addFilterName(name);
        criteria.fetchConfiguration(true);
        criteria.fetchResource(true);
        criteria.fetchTemplate(true);

        PageList<DriftDefinition> driftDefs = driftMgr.findDriftDefinitionsByCriteria(getOverlord(), criteria);
        assertEquals("Expected to find one drift definition", 1, driftDefs.size());

        return driftDefs.get(0);
    }
}
