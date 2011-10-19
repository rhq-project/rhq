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
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.drift.DriftFileStatus.LOADED;
import static org.rhq.enterprise.server.util.LookupUtil.getJPADriftServer;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.JPADriftChangeSetCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.drift.JPADriftSet;
import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.core.domain.drift.dto.DriftDTO;
import org.rhq.core.domain.drift.dto.DriftFileDTO;
import org.rhq.core.domain.util.PageList;
import org.rhq.test.TransactionCallback;

public class JPADriftServerBeanTest extends DriftServerTest {

    private JPADriftServerLocal jpaDriftServer;

    private final String DRIFT_FILE_1_ID = "a1b2c3d4";

    private final String DRIFT_FILE_2_ID = "1ab2b3c4d";

    private JPADriftFile driftFile1;

    private JPADriftFile driftFile2;

    @BeforeClass
    public void initTests() {
        jpaDriftServer = getJPADriftServer();
    }

    @BeforeMethod
    public void persistDriftFiles() throws Exception {
        driftFile1 = jpaDriftServer.persistDriftFile(new JPADriftFile(DRIFT_FILE_1_ID));
        driftFile2 = jpaDriftServer.persistDriftFile(new JPADriftFile(DRIFT_FILE_2_ID));

        String driftFile1Content = "drift file 1 content...";
        String driftFile2Content = "drift file 2 content...";

        jpaDriftServer.persistDriftFileData(driftFile1, toInputStream(driftFile1Content), driftFile1Content.length());
        jpaDriftServer.persistDriftFileData(driftFile2, toInputStream(driftFile2Content), driftFile2Content.length());

        driftFile1 = jpaDriftServer.getDriftFile(getOverlord(), DRIFT_FILE_1_ID);
        driftFile2 = jpaDriftServer.getDriftFile(getOverlord(), DRIFT_FILE_2_ID);

        assertDriftFilePersisted(driftFile1, "driftFile1", driftFile1Content);
        assertDriftFilePersisted(driftFile2, "driftFile2", driftFile2Content);
    }

    public void persistResourceChangeSet() {
        // first create and persist the drift definition
        final DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName("test::persistResourceChangeSet");
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(2400L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        driftDef.setResource(resource);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(driftDef);
            }
        });

        // create the change set to be persisted
        DriftChangeSetDTO changeSet = new DriftChangeSetDTO();
        changeSet.setCategory(COVERAGE);
        changeSet.setVersion(1);
        changeSet.setDriftDefinitionId(driftDef.getId());
        changeSet.setResourceId(resource.getId());
        changeSet.setDriftHandlingMode(normal);
        changeSet.setCtime(System.currentTimeMillis());

        DriftDTO drift1 = new DriftDTO();
        drift1.setCategory(FILE_ADDED);
        drift1.setPath("drift.1");
        drift1.setChangeSet(changeSet);
        drift1.setCtime(System.currentTimeMillis());
        drift1.setNewDriftFile(toDTo(driftFile1));

        DriftDTO drift2 = new DriftDTO();
        drift2.setCategory(FILE_ADDED);
        drift2.setPath("drift.2");
        drift2.setChangeSet(changeSet);
        drift2.setCtime(System.currentTimeMillis());
        drift2.setNewDriftFile(toDTo(driftFile2));

        Set<DriftDTO> drifts = new HashSet<DriftDTO>();
        drifts.add(drift1);
        drifts.add(drift2);
        changeSet.setDrifts(drifts);

        String newChangeSetId = jpaDriftServer.persistChangeSet(getOverlord(), changeSet);

        // verify that the change set was persisted
        JPADriftChangeSetCriteria criteria = new JPADriftChangeSetCriteria();
        criteria.addFilterId(newChangeSetId);
        criteria.fetchDrifts(true);

        PageList<JPADriftChangeSet> changeSets = jpaDriftServer.findDriftChangeSetsByCriteria(getOverlord(), criteria);
        assertEquals("Expected to find one change set", 1, changeSets.size());

        JPADriftChangeSet jpaChangeSet = changeSets.get(0);
        assertEquals("Expected change set to contain two drifts. This could be a result of the change set not being " +
            "persisted correctly or the criteria fetch being done incorrectly.", 2, jpaChangeSet.getDrifts().size());

       assertPropertiesMatch("The change set was not persisted correctly", changeSet, jpaChangeSet, "id", "drifts",
           "class", "ctime");

        List<? extends Drift> expectedDrifts = asList(drift1, drift2);
        List<? extends Drift> actualDrifts = new ArrayList(jpaChangeSet.getDrifts());

        // We ignore the id and ctime properties because those are set by JPADriftServerBean
        // and are somewhat implmentation specific. We ignore the directory property because
        // it is really a calculated property. newDriftFile has to be compared separately
        // since it does not implement equals.
        assertCollectionMatchesNoOrder("The change set drifts were not persisted correctly",
            (List<Drift>) expectedDrifts, (List<Drift>) actualDrifts, "id", "ctime", "changeSet", "directory",
            "newDriftFile", "class");

        assertPropertiesMatch("The newDriftFile property was not set correctly for " + drift1, drift1.getNewDriftFile(),
            findDriftByPath(actualDrifts, "drift.1").getNewDriftFile(), "class", "ctime") ;
        assertPropertiesMatch("The newDriftFile property was not set correctly for " + drift2, drift2.getNewDriftFile(),
            findDriftByPath(actualDrifts, "drift.2").getNewDriftFile(), "class", "ctime") ;
    }

    public void persistTemplateChangeSet() {
        // create the change set to be persisted
        //
        // Note that we do not set the drift definition id or resource id since
        // the change set is not owned by a resource. It is owned by the
        // resource type.
        DriftChangeSetDTO changeSet = new DriftChangeSetDTO();
        changeSet.setCategory(COVERAGE);
        changeSet.setVersion(1);
        changeSet.setDriftHandlingMode(normal);
        changeSet.setCtime(System.currentTimeMillis());

        DriftDTO drift1 = new DriftDTO();
        drift1.setCategory(FILE_ADDED);
        drift1.setPath("drift.1");
        drift1.setChangeSet(changeSet);
        drift1.setCtime(System.currentTimeMillis());
        drift1.setNewDriftFile(toDTo(driftFile1));

        DriftDTO drift2 = new DriftDTO();
        drift2.setCategory(FILE_ADDED);
        drift2.setPath("drift.2");
        drift2.setChangeSet(changeSet);
        drift2.setCtime(System.currentTimeMillis());
        drift2.setNewDriftFile(toDTo(driftFile2));

        Set<DriftDTO> drifts = new HashSet<DriftDTO>();
        drifts.add(drift1);
        drifts.add(drift2);
        changeSet.setDrifts(drifts);

        String newChangeSetId = jpaDriftServer.persistChangeSet(getOverlord(), changeSet);

        // verify that the change set was persisted
        JPADriftChangeSetCriteria criteria = new JPADriftChangeSetCriteria();
        criteria.addFilterId(newChangeSetId);
        criteria.fetchDrifts(true);

        PageList<JPADriftChangeSet> changeSets = jpaDriftServer.findDriftChangeSetsByCriteria(getOverlord(), criteria);
        assertEquals("Expected to find one change set", 1, changeSets.size());

        JPADriftChangeSet jpaChangeSet = changeSets.get(0);
        assertEquals("Expected change set to contain two drifts. This could be a result of the change set not being " +
            "persisted correctly or the criteria fetch being done incorrectly.", 2, jpaChangeSet.getDrifts().size());

       assertPropertiesMatch("The change set was not persisted correctly", changeSet, jpaChangeSet, "id", "drifts",
           "class", "ctime");

        List<? extends Drift> expectedDrifts = asList(drift1, drift2);
        List<? extends Drift> actualDrifts = new ArrayList(jpaChangeSet.getDrifts());

        // We ignore the id and ctime properties because those are set by JPADriftServerBean
        // and are somewhat implmentation specific. We ignore the directory property because
        // it is really a calculated property. newDriftFile has to be compared separately
        // since it does not implement equals.
        assertCollectionMatchesNoOrder("The change set drifts were not persisted correctly",
            (List<Drift>) expectedDrifts, (List<Drift>) actualDrifts, "id", "ctime", "changeSet", "directory",
            "newDriftFile", "class");

        assertPropertiesMatch("The newDriftFile property was not set correctly for " + drift1, drift1.getNewDriftFile(),
            findDriftByPath(actualDrifts, "drift.1").getNewDriftFile(), "class", "ctime") ;
        assertPropertiesMatch("The newDriftFile property was not set correctly for " + drift2, drift2.getNewDriftFile(),
            findDriftByPath(actualDrifts, "drift.2").getNewDriftFile(), "class", "ctime") ;
    }

    public void copyChangeSet() {
        // first create the change set that will be copied
        final JPADriftChangeSet changeSet = new JPADriftChangeSet(null, 0, COVERAGE, null);
        changeSet.setDriftHandlingMode(normal);

        final JPADrift drift1 = new JPADrift(changeSet, "drift.1", FILE_ADDED, null, driftFile1);
        final JPADrift drift2 = new JPADrift(changeSet, "drift.2", FILE_ADDED, null, driftFile2);

        final JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift1);
        driftSet.addDrift(drift2);

        // next create the drift definition
        final DriftDefinition driftDef = new DriftDefinition(new Configuration());
        driftDef.setName("test::copyChangeSet");
        driftDef.setEnabled(true);
        driftDef.setDriftHandlingMode(normal);
        driftDef.setInterval(2400L);
        driftDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));
        driftDef.setResource(resource);

        // persist the change set and drift definition
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(driftSet);
                changeSet.setInitialDriftSet(driftSet);
                em.persist(changeSet);
                em.persist(driftDef);
            }
        });

        jpaDriftServer.copyChangeSet(getOverlord(), changeSet.getId(), driftDef.getId(), resource.getId());

        // verify that the change set was created for the definition
        JPADriftChangeSetCriteria criteria = new JPADriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(driftDef.getId());
        criteria.addFilterCategory(COVERAGE);

        PageList<JPADriftChangeSet> changeSets = jpaDriftServer.findDriftChangeSetsByCriteria(getOverlord(), criteria);

        assertEquals("Expected to get back one change set", 1,changeSets.size());

        JPADriftChangeSet newChangeSet = changeSets.get(0);
        Set<JPADrift> expectedDrifts = new HashSet<JPADrift>(asList(drift1, drift2));
        Set<JPADrift> actualDrifts = newChangeSet.getDrifts();

        assertCollectionMatchesNoOrder("The change set drifts were not copied correctly", expectedDrifts, actualDrifts,
            "changeSet", "newDriftFile");
    }

    private DriftFileDTO toDTo(JPADriftFile driftFile) {
        DriftFileDTO dto = new DriftFileDTO();
        dto.setHashId(driftFile.getHashId());
        dto.setDataSize(driftFile.getDataSize());
        dto.setStatus(driftFile.getStatus());
        return dto;
    }


    private void assertDriftFilePersisted(JPADriftFile driftFile, String name, String content) {
        assertNotNull("Failed to get " + name + " Was it persisted?", driftFile);
        assertEquals("The content for " + name + " is wrong", content, jpaDriftServer.getDriftFileBits(
            driftFile.getHashId()));
        assertEquals("The drift file status is wrong", LOADED, driftFile.getStatus());
    }

}
