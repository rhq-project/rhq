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

import static java.util.Arrays.asList;

import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftTemplateManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.JPADriftChangeSetCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComplianceStatus;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.drift.JPADriftSet;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility.SerializationType;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.test.AssertUtils;
import org.testng.annotations.Test;

@Test
public class ManageDriftDefinitionsTest extends AbstractDriftServerTest {

    private final String DRIFT_NOT_SUPPORTED_TYPE = NAME_PREFIX + "DRIFT_NOT_SUPPORTED_RESOURCE_TYPE";

    private final String DRIFT_NOT_SUPPORTED_RESOURCE = NAME_PREFIX + "DRIFT_NOT_SUPPORTED_RESOURCE";

    private DriftManagerLocal driftMgr;

    private DriftTemplateManagerLocal templateMgr;

    private ResourceType driftNotSupportedType;

    private Resource driftNotSupportedResource;

    @Override
    protected void beforeMethod(Method testMethod) throws Exception {
        super.beforeMethod(testMethod);

        driftMgr = getDriftManager();
        templateMgr = getDriftTemplateManager();
    }

    @Override
    protected void purgeDB() {
        super.purgeDB();

        removeEntity(Resource.class, DRIFT_NOT_SUPPORTED_RESOURCE);
        removeEntity(ResourceType.class, DRIFT_NOT_SUPPORTED_TYPE);
    }

    public void createDefinitionFromUnpinnedTemplate() throws Exception {
        // first create a template
        final DriftDefinition templateDef = new DriftDefinition(new Configuration());
        templateDef.setName(NAME_PREFIX + "createUnpinnedDefinition");
        templateDef.setEnabled(true);
        templateDef.setDriftHandlingMode(normal);
        templateDef.setInterval(2400L);
        templateDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        // persist the template
        DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(), resourceType.getId(), false,
            templateDef);

        // create and persist the definition
        DriftDefinition definition = template.createDefinition();
        definition.setTemplate(template);
        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(resource.getId()), definition);

        // verify that the definition was created
        DriftDefinition newDef = loadDefinition(definition.getName());
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);

        HibernateDetachUtility.nullOutUninitializedFields(newDef, SerializationType.SERIALIZATION);
        assertEquals("The drift definition was not persisted correctly", 0, comparator.compare(definition, newDef));
        assertEquals("The template association was not set on the definition", template, newDef.getTemplate());
    }

    // The following two tests are commented out because when they are enabled
    // and all tests in the itests module are run, the @AfterClass method for
    // DriftTemplateManagerBeanTest does not run immediately after all of its
    // test methods have finished running. Instead, some of the tests in
    // ManageDriftDefinitionsTest start running. This leads to some database
    // constraint violations because of how agents are created in the parent
    // class, DriftServerTest. See http://groups.google.com/group/testng-users/browse_thread/thread/da2790679a430d51?pli=1
    // more info on the order in which TestNG executes tests.

    //    public void createEntitiesThatDoNotSupportDrift() {
    //        // first create the resource type that does not support drift
    //        driftNotSupportedType = new ResourceTypeBuilder()
    //                .createResourceType()
    //                .withId(0)
    //                .withName(DRIFT_NOT_SUPPORTED_TYPE)
    //                .withCategory(SERVER)
    //                .withPlugin(DRIFT_NOT_SUPPORTED_TYPE.toLowerCase())
    //                .build();
    //
    //        // create a resource of the type that does not support drift
    //        driftNotSupportedResource = new ResourceBuilder()
    //                .createResource()
    //                .withId(0)
    //                .withName(DRIFT_NOT_SUPPORTED_RESOURCE)
    //                .withResourceKey(DRIFT_NOT_SUPPORTED_RESOURCE)
    //                .withRandomUuid()
    //                .withResourceType(driftNotSupportedType)
    //                .build();
    //
    //        executeInTransaction(new TransactionCallback() {
    //            @Override
    //            public void execute() throws Exception {
    //                EntityManager em = getEntityManager();
    //                em.persist(driftNotSupportedType);
    //                em.persist(driftNotSupportedResource);
    //            }
    //        });
    //    }
    //
    //    @Test(dependsOnMethods = "createEntitiesThatDoNotSupportDrift",
    //            expectedExceptions = EJBException.class,
    //            expectedExceptionsMessageRegExp = ".*Cannot create drift definition.*type.*does not support drift management")
    //    @InitDB(false)
    //    public void doNotAllowDefinitionToBeCreatedForTypeThatDoesNotSupportDrift() {
    //        DriftDefinition driftDef = new DriftDefinition(new Configuration());
    //        driftDef.setName("test_typeDoesNotSupportDrift");
    //        driftDef.setEnabled(true);
    //        driftDef.setInterval(1800L);
    //        driftDef.setDriftHandlingMode(normal);
    //        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
    //
    //        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(driftNotSupportedResource.getId()),
    //                driftDef);
    //    }

    @SuppressWarnings("unchecked")
    public void createDefinitionFromPinnedTemplate() throws Exception {
        // We first need to create a pinned template. Users can only create a pinned
        // template from a snapshot of an existing resource-level drift definition.
        // We are going to take a bit of a short cut though by directly creating
        // and persisting the pinned change set.

        // first create the change set
        final JPADriftChangeSet changeSet0 = new JPADriftChangeSet(null, 0, COVERAGE, null);
        changeSet0.setDriftHandlingMode(DriftConfigurationDefinition.DriftHandlingMode.normal);

        final JPADriftFile driftFile1 = new JPADriftFile(NAME_PREFIX + "a1b2c3");
        final JPADriftFile driftFile2 = new JPADriftFile(NAME_PREFIX + "1a2b3c");

        JPADrift drift1 = new JPADrift(changeSet0, "drift.1", FILE_ADDED, null, driftFile1);
        JPADrift drift2 = new JPADrift(changeSet0, "drift.2", FILE_ADDED, null, driftFile2);

        final JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift1);
        driftSet.addDrift(drift2);

        // create the template
        final DriftDefinition templateDef = new DriftDefinition(new Configuration());
        templateDef.setName(NAME_PREFIX + "createUnpinnedDefinition");
        templateDef.setEnabled(true);
        templateDef.setDriftHandlingMode(normal);
        templateDef.setInterval(2400L);
        templateDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        templateDef.setPinned(true);

        final DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(), resourceType.getId(), true,
            templateDef);

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                em.persist(driftFile1);
                em.persist(driftFile2);
                em.persist(changeSet0);
                em.persist(driftSet);
                changeSet0.setInitialDriftSet(driftSet);
                em.merge(changeSet0);

                // setting the change set id on the template is the last and the
                // most important step in making the template pinned
                template.setChangeSetId(changeSet0.getId());
                em.merge(template);
            }
        });

        // Create and persist a resource-level definition.
        final DriftDefinition definition = template.createDefinition();
        definition.setTemplate(template);

        final AtomicBoolean agentInvoked = new AtomicBoolean(false);

        agentServiceContainer.driftService = new TestDefService() {
            @Override
            public void updateDriftDetection(int resourceId, DriftDefinition driftDef, DriftSnapshot snapshot) {
                try {
                    HibernateDetachUtility.nullOutUninitializedFields(driftDef,
                        HibernateDetachUtility.SerializationType.SERIALIZATION);
                    HibernateDetachUtility.nullOutUninitializedFields(snapshot,
                        HibernateDetachUtility.SerializationType.SERIALIZATION);
                    agentInvoked.set(true);
                    assertNotNull("Expected snapshot drift instances collection to be non-null",
                        snapshot.getDriftInstances());
                    assertEquals("Expected snapshot to contain two drift entries", 2, snapshot.getDriftInstances()
                        .size());
                } catch (Exception e) {
                    String msg = "Do not pass attached entites to agent since those entities are outside of "
                        + "Hibernate's control. The persistence context should be flushed and cleared to ensure that "
                        + "only detached objects are sent to the agent";
                    throw new RuntimeException(msg, e);
                }
            }
        };

        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(resource.getId()), definition);

        DriftDefinition newDef = loadDefinition(definition.getName());

        // verify that the definition is marked as pinned
        assertTrue("The drift definition should be marked as pinned", newDef.isPinned());

        // verify that the initial change set is generated for the definition
        JPADriftChangeSetCriteria criteria = new JPADriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(definition.getId());
        criteria.addFilterCategory(COVERAGE);
        criteria.fetchDrifts(true);

        PageList<? extends DriftChangeSet<?>> changeSets = driftMgr.findDriftChangeSetsByCriteria(getOverlord(),
            criteria);
        assertEquals("Expected to find one change set", 1, changeSets.size());

        JPADriftChangeSet expectedChangeSet = new JPADriftChangeSet(resource, 1, COVERAGE, null);
        List<? extends Drift> expectedDrifts = asList(new JPADrift(expectedChangeSet, drift1.getPath(), FILE_ADDED,
            null, driftFile1), new JPADrift(expectedChangeSet, drift2.getPath(), FILE_ADDED, null, driftFile2));

        DriftChangeSet<?> actualChangeSet = changeSets.get(0);
        List<? extends Drift> actualDrifts = new ArrayList(actualChangeSet.getDrifts());

        AssertUtils.assertCollectionMatchesNoOrder(
            "Expected to find drifts from change sets 1 and 2 in the template change set",
            (List<Drift>) expectedDrifts, (List<Drift>) actualDrifts, "id", "ctime", "changeSet", "newDriftFile");

        // lastly verify that the agent is called
        assertTrue("Failed to send drift definition along with snapshot to agent", agentInvoked.get());
    }

    public void unpinDefinition() {
        // First create the template
        final DriftDefinition templateDef = new DriftDefinition(new Configuration());
        templateDef.setName(NAME_PREFIX + "unpin_def_template");
        templateDef.setEnabled(true);
        templateDef.setDriftHandlingMode(normal);
        templateDef.setInterval(2400L);
        templateDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        final DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(), resourceType.getId(), true,
            templateDef);

        // First create the definition
        DriftDefinition definition = template.createDefinition();
        definition.setName(NAME_PREFIX + "unpin");
        definition.setEnabled(true);
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        definition.setComplianceStatus(DriftComplianceStatus.OUT_OF_COMPLIANCE_DRIFT);
        definition.setInterval(1800L);
        definition.setDriftHandlingMode(normal);
        definition.setPinned(true);

        // persist the definition
        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(resource.getId()), definition);

        // now update the definition
        DriftDefinition newDef = loadDefinition(definition.getName());
        assertNotNull("Failed to load new definition, " + toString(definition));
        newDef.setPinned(false);

        driftMgr.updateDriftDefinition(getOverlord(), EntityContext.forResource(resource.getId()), newDef);

        // now verify that the definition was updated
        DriftDefinition updatedDef = loadDefinition(definition.getName());
        assertNotNull("Failed to load updated definition, " + toString(newDef));

        assertFalse("The updated definition should be set to unpinned", updatedDef.isPinned());
        assertEquals("The updated definition should be set to in compliance", DriftComplianceStatus.IN_COMPLIANCE,
            updatedDef.getComplianceStatus());
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
