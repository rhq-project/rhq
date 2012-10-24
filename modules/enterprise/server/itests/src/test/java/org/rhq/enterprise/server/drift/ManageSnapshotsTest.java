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
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftTemplateManager;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.drift.JPADriftSet;
import org.rhq.core.domain.server.EntitySerializer;
import org.rhq.core.domain.util.PageList;
import org.rhq.test.AssertUtils;
import org.rhq.enterprise.server.test.TransactionCallback;

@Test(dependsOnGroups = "pinning")
public class ManageSnapshotsTest extends AbstractDriftServerTest {

    private DriftManagerLocal driftMgr;

    private DriftTemplateManagerLocal templateMgr;

    @BeforeClass
    public void initClass() throws Exception {
        driftMgr = getDriftManager();
        templateMgr = getDriftTemplateManager();
    }

    public void pinningSnapshotShouldSetDriftDefAsPinned() {
        final DriftDefinition driftDef = createAndPersistDriftDef("test::setPinnedFlag");

        // create initial change set
        final JPADriftChangeSet changeSet = new JPADriftChangeSet(resource, 0, COVERAGE, driftDef);

        final JPADriftFile driftFile1 = new JPADriftFile("a1b2c3");
        JPADrift drift = new JPADrift(changeSet, "drift.1", FILE_ADDED, null, driftFile1);

        final JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(driftFile1);
                em.persist(changeSet);
                em.persist(driftSet);
                changeSet.setInitialDriftSet(driftSet);
                em.merge(changeSet);
            }
        });

        driftMgr.pinSnapshot(getOverlord(), driftDef.getId(), 0);
        DriftDefinition updatedDriftDef = driftMgr.getDriftDefinition(getOverlord(), driftDef.getId());

        assertNotNull("Failed to get " + toString(driftDef), updatedDriftDef);
        assertTrue("Failed to set pinned flag of " + toString(driftDef), updatedDriftDef.isPinned());
    }

    @SuppressWarnings("unchecked")
    public void pinningSnapshotShouldMakeSnapshotTheInitialChangeSet() throws Exception {
        final DriftDefinition driftDef = createAndPersistDriftDef("test::makeSnapshotVersionZero");

        // create initial change set
        final JPADriftChangeSet changeSet0 = new JPADriftChangeSet(resource, 0, COVERAGE, driftDef);

        final JPADriftFile driftFile1 = new JPADriftFile("a1b2c3");
        JPADrift drift1 = new JPADrift(changeSet0, "drift.1", FILE_ADDED, null, driftFile1);

        final JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift1);

        // create change set v1
        final JPADriftFile driftFile2 = new JPADriftFile("1a2b3c");
        final JPADriftChangeSet changeSet1 = new JPADriftChangeSet(resource, 1, DRIFT, driftDef);
        final JPADrift drift2 = new JPADrift(changeSet1, "drift.2", FILE_ADDED, null, driftFile2);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(driftFile1);
                em.persist(driftFile2);
                em.persist(changeSet0);
                em.persist(driftSet);
                changeSet0.setInitialDriftSet(driftSet);
                em.merge(changeSet0);
                em.persist(changeSet1);
                em.persist(drift2);
            }
        });

        driftMgr.pinSnapshot(getOverlord(), driftDef.getId(), 1);

        // Verify that there is now only one change set for the drift def
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(driftDef.getId());

        PageList<? extends DriftChangeSet<?>> changeSets = driftMgr.findDriftChangeSetsByCriteria(getOverlord(),
            criteria);
        assertEquals("All change sets except the change set representing the pinned snapshot should be removed",
            1, changeSets.size());
        DriftChangeSet<?> changeSet = changeSets.get(0);

        assertEquals("The pinned snapshot version should be reset to zero", 0, changeSet.getVersion());
        assertEquals("The change set category is wrong", COVERAGE, changeSet.getCategory());

        JPADriftChangeSet expectedChangeSet = new JPADriftChangeSet(resource, 1, COVERAGE, driftDef);
        List<? extends Drift> expectedDrifts = asList(
            new JPADrift(expectedChangeSet, drift1.getPath(), FILE_ADDED, null, driftFile1),
            new JPADrift(expectedChangeSet, drift2.getPath(), FILE_ADDED, null, driftFile2));

        List<? extends Drift> actualDrifts = new ArrayList(changeSet.getDrifts());

        AssertUtils.assertCollectionMatchesNoOrder(
            "Expected to find drifts from change sets 1 and 2 in the new initial change set",
            (List<Drift>) expectedDrifts, (List<Drift>) actualDrifts, "id", "ctime", "changeSet", "newDriftFile");

        // we need to compare the newDriftFile properties separately because
        // assertCollectionMatchesNoOrder compares properties via equals() and JPADriftFile
        // does not implement equals.
        assertPropertiesMatch(drift1.getNewDriftFile(), findDriftByPath(actualDrifts, "drift.1").getNewDriftFile(),
            "The newDriftFile property was not set correctly for " + drift1);
        assertPropertiesMatch(drift2.getNewDriftFile(), findDriftByPath(actualDrifts, "drift.2").getNewDriftFile(),
            "The newDriftFile property was not set correctly for " + drift1);
    }

    public void pinningSnapshotShouldSendRequestToAgent() {
        final DriftDefinition driftDef = createAndPersistDriftDef("test::setPinnedFlag");

        // create initial change set
        final JPADriftChangeSet changeSet = new JPADriftChangeSet(resource, 0, COVERAGE, driftDef);

        final JPADriftFile driftFile1 = new JPADriftFile("a1b2c3");
        JPADrift drift = new JPADrift(changeSet, "drift.1", FILE_ADDED, null, driftFile1);

        final JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(driftFile1);
                em.persist(changeSet);
                em.persist(driftSet);
                changeSet.setInitialDriftSet(driftSet);
                em.merge(changeSet);
            }
        });

        final AtomicBoolean agentInvoked = new AtomicBoolean(false);
        agentServiceContainer.driftService = new TestDefService() {
            @Override
            public void pinSnapshot(int resourceId, String configName, DriftSnapshot snapshot) {
                try {
                    agentInvoked.set(true);
                    // serialize the method arguments here to more closely simulate what
                    // happens during the call. We cannot send hibernate-proxied objects
                    // to the agent. This is an attempt to catch that.
                    ObjectOutputStream stream = new ObjectOutputStream(new ByteArrayOutputStream());
                    EntitySerializer.writeExternalRemote(resourceId, stream);
                    EntitySerializer.writeExternalRemote(configName, stream);
                    EntitySerializer.writeExternalRemote(snapshot, stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        driftMgr.pinSnapshot(getOverlord(), driftDef.getId(), 0);

        assertTrue("Failed to send request to agent to pin snapshot", agentInvoked.get());
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "Cannot repin.*definition.*")
    public void doNotAllowSnapshotToBePinnedWhenDefinitionIsAttachedToPinnedTemplate() {
        // First create the template
        final DriftDefinition templateDef = new DriftDefinition(new Configuration());
        templateDef.setName("Template-Pinned_Test");
        templateDef.setEnabled(true);
        templateDef.setDriftHandlingMode(normal);
        templateDef.setInterval(2400L);
        templateDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        final DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(), resourceType.getId(), true,
            templateDef);

        // Now we will pin the template. We are going to take a bit of a short cut
        // here. Pinning a template requires a drift definition with at least one
        // snapshot. For the purposes of this test we can simply set the
        // changeSetId field of the template to indicate that it is pinned.
        template.setChangeSetId("1234");

        // Next create a resource-level definition from the template.
        final DriftDefinition driftDef = template.createDefinition();
        driftDef.setResource(resource);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.merge(template);
                em.persist(driftDef);
            }
        });

        // Now try resource-level pinning, i.e., pin a snapshot to the definition
        driftMgr.pinSnapshot(getOverlord(), driftDef.getId(), 0);
    }

    private DriftDefinition createAndPersistDriftDef(String name) {
        final DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName(name);
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(1800L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                driftDef.setResource(resource);
                getEntityManager().persist(driftDef);
            }
        });

        return driftDef;
    }

}
