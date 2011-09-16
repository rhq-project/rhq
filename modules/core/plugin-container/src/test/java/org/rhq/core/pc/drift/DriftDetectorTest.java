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

package org.rhq.core.pc.drift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.touch;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DriftDetectorTest extends DriftTest {

    ScheduleQueue scheduleQueue;

    DriftClientTestStub driftClient;

    DriftDetector detector;

    @BeforeMethod
    public void initDetector() {
        driftClient = new DriftClientTestStub();
        driftClient.setBaseDir(resourceDir);

        scheduleQueue = new ScheduleQueueImpl();

        detector = new DriftDetector();
        detector.setDriftClient(driftClient);
        detector.setChangeSetManager(changeSetMgr);
        detector.setScheduleQueue(scheduleQueue);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void excludeEmptyDirsFromCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");

        // create an empty directory
        File libDir = mkdir(resourceDir, "lib");

        DriftConfiguration driftConfig = driftConfiguration("coverage-test", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), driftConfig));
        detector.run();

        File changeSet = changeSet(driftConfig.getName(), COVERAGE);
        Headers headers = createHeaders(driftConfig, COVERAGE);
        List<FileEntry> expected = asList(addedFileEntry("conf/server.conf", sha256(serverConf)));

        assertHeaderEquals(changeSet, headers);
        assertFileEntriesMatch("Only files should be included in a change set.", expected, changeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeMultipleFilesInDirInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        File server2Conf = createRandomFile(confDir, "server-2.conf");

        DriftConfiguration config = driftConfiguration("multiple-files-test", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File changeSet = changeSet(config.getName(), COVERAGE);
        List<FileEntry> entries = asList(
            addedFileEntry("conf/server-1.conf", sha256(server1Conf)),
            addedFileEntry("conf/server-2.conf", sha256(server2Conf)));

        assertHeaderEquals(changeSet, createHeaders(config, COVERAGE));
        assertFileEntriesMatch("Each file in a directory should be included in a coverage change set", entries,
            changeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includedSiblingDirsInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");

        File libDir = mkdir(resourceDir, "lib");
        File serverLib = createRandomFile(libDir, "server.jar");

        DriftConfiguration config = driftConfiguration("sibling-dirs-test", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File changeSet = changeSet(config.getName(), COVERAGE);
        List<FileEntry> entries = asList(
            addedFileEntry("conf/server.conf", sha256(serverConf)),
            addedFileEntry("lib/server.jar", sha256(serverLib)));

        assertHeaderEquals(changeSet, createHeaders(config, COVERAGE));
        assertFileEntriesMatch("A coverage change set should include files from multiple, sibling directories",
            entries, changeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeNestedDirsInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");

        File subConfDir = mkdir(confDir, "subconf");
        File server2Conf = createRandomFile(subConfDir, "server-2.conf");

        DriftConfiguration config = driftConfiguration("nested-dirs-test", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File changeSet = changeSet(config.getName(), COVERAGE);
        List<FileEntry> entries = asList(
            addedFileEntry("conf/server-1.conf", sha256(server1Conf)),
            addedFileEntry("conf/subconf/server-2.conf", sha256(server2Conf)));

        assertHeaderEquals(changeSet, createHeaders(config, COVERAGE));
        assertFileEntriesMatch("A coverage change set should include files in nested sub directories", entries,
            changeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doNotUpdateSnapshotOrGenerateDriftChangeSetIfNothingChanges() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");

        DriftConfiguration config = driftConfiguration("nothing-to-update", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File changeSet = changeSet(config.getName(), COVERAGE);
        String originalHash = sha256(changeSet);

        // Run the detector again. Note that nothing has changed so the snapshot should
        // remain the same and no drift change set file should be generated.
        detector.run();

        String newHash = sha256(changeSet);

        assertEquals(newHash, originalHash, "The snapshot file should not have changed since there was no drift.");

        File driftChangeSet = changeSet(config.getName(), DRIFT);

        assertFalse(driftChangeSet.exists(), "A drift change set file should not have been generated since there was "
            + "no drift");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void skipScheduledThatHasConfigDisabled() throws Exception {
        detector.setDriftClient(new DriftClientTestStub() {
            {
                setBaseDir(resourceDir);
            }

            @Override
            public void sendChangeSetToServer(int resourceId, DriftConfiguration driftConfiguration,
                DriftChangeSetCategory type) {
                throw new RuntimeException("Should not invoke drift client when drift configuration is disabled");
            }
        });

        DriftConfiguration config = driftConfiguration("disabled-config-test", resourceDir.getAbsolutePath());
        config.setEnabled(false);

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = new File(confDir, "server-1.conf");
        touch(server1Conf);

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeAddedFileInDriftChangeSet() throws Exception {
        DriftConfiguration config = driftConfiguration("file-added-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(config, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", sha256(server1Conf)));
        writer.close();

        // Create some drift
        File server2Conf = createRandomFile(confDir, "server-2.conf");

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File driftChangeSet = changeSet(config.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(addedFileEntry("conf/server-2.conf", sha256(server2Conf)));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(config, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        File coverageChangeSet = changeSet(config.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(
            addedFileEntry("conf/server-1.conf", sha256(server1Conf)),
            addedFileEntry("conf/server-2.conf", sha256(server2Conf)));

        // verify that the coverage change set was updated
        assertHeaderEquals(coverageChangeSet, createHeaders(config, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeModifiedFileInDriftChangeSet() throws Exception {
        DriftConfiguration config = driftConfiguration("file-modified-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        String oldHash = sha256(server1Conf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(config, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", oldHash));
        writer.close();

        // create some drift
        server1Conf.delete();
        server1Conf = createRandomFile(confDir, "server-1.conf");
        String newHash = sha256(server1Conf);

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File driftChangeSet = changeSet(config.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(changedFileEntry("conf/server-1.conf", oldHash, newHash));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(config, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        File coverageChangeSet = changeSet(config.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(changedFileEntry("conf/server-1.conf", oldHash, newHash));

        // verify that the coverage change set was updated
        assertHeaderEquals(coverageChangeSet, createHeaders(config, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @SuppressWarnings("unchecked")
    @Test(enabled = false)
    public void includeFiledAddedInNewDirectoryInDriftChangeSet() throws Exception {
        DriftConfiguration config = driftConfiguration("file-added-in-new-dir", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(config, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", sha256(server1Conf)));
        writer.close();

        // create some drift
        File subconfDir = mkdir(confDir, "subconf");
        File server2Conf = createRandomFile(subconfDir, "server-2.conf");

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File driftChangeSet = changeSet(config.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(addedFileEntry("conf/subconf/server-2.conf", sha256(server2Conf)));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(config, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        File coverageChangeSet = changeSet(config.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(
            addedFileEntry("conf/server-1.conf", sha256(server1Conf)),
            addedFileEntry("conf/subconf/server-2.conf", sha256(server2Conf)));

        // verify that the coverage change set was updated
        assertHeaderEquals(coverageChangeSet, createHeaders(config, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeRemovedFileInDriftChangeSet() throws Exception {
        DriftConfiguration config = driftConfiguration("file-removed-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        File server2Conf = createRandomFile(confDir, "server-2.conf");

        String server2ConfHash = sha256(server2Conf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(config, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", sha256(server1Conf)));
        writer.write(addedFileEntry("conf/server-2.conf", server2ConfHash));
        writer.close();

        // create some drift
        server2Conf.delete();

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File driftChangeSet = changeSet(config.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(removedFileEntry("conf/server-2.conf", server2ConfHash));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(config, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        // verify that the coverage change set was updated
        File coverageChangeSet = changeSet(config.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(addedFileEntry("conf/server-1.conf", sha256(server1Conf)));

        assertHeaderEquals(coverageChangeSet, createHeaders(config, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeFilesInRemovedDirectoryInDriftChangeSet() throws Exception {
        DriftConfiguration config = driftConfiguration("dir-removed-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        String server1Hash = sha256(server1Conf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(config, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", server1Hash));
        writer.close();

        // create some drift
        server1Conf.delete();
        confDir.delete();

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File driftChangeSet = changeSet(config.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(removedFileEntry("conf/server-1.conf", server1Hash));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(config, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        // verify that the coverage change set was updated
        File coverageChangeSet = changeSet(config.getName(), COVERAGE);
        List<FileEntry> coverageEntries = emptyList();

        assertHeaderEquals(coverageChangeSet, createHeaders(config, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    void assertHeaderEquals(File changeSet, Headers expected) throws Exception {
        ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new FileReader(changeSet)));
        Headers actual = reader.getHeaders();
        assertPropertiesMatch(expected, actual, "Headers for " + changeSet.getPath() + " do not match " +
            "expected values");
    }

    void assertFileEntriesMatch(String msg, List<FileEntry> expected, File changeSet) throws Exception {
        List<FileEntry> actual = new ArrayList<FileEntry>();
        ChangeSetReader reader = new ChangeSetReaderImpl(changeSet);

        for (FileEntry entry : reader) {
            actual.add(entry);
        }

        assertCollectionMatchesNoOrder(msg, expected, actual);
    }

    Headers createHeaders(DriftConfiguration driftConfig, DriftChangeSetCategory type) {
        return createHeaders(driftConfig, type, 0);
    }

    Headers createHeaders(DriftConfiguration driftConfig, DriftChangeSetCategory type, int version) {
        Headers headers = new Headers();
        headers.setResourceId(resourceId());
        headers.setDriftCofigurationId(driftConfig.getId());
        headers.setDriftConfigurationName(driftConfig.getName());
        headers.setBasedir(resourceDir.getAbsolutePath());
        headers.setType(type);
        headers.setVersion(version);

        return headers;
    }

}
