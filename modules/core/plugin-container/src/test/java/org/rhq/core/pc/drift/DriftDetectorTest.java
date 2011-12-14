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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteDirectory;
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
import static org.testng.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.ChangeSetWriterImpl;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.SystemInfoFactory;

public class DriftDetectorTest extends DriftTest {

    ScheduleQueue scheduleQueue;

    DriftClientTestStub driftClient;

    DriftDetector detector;

    boolean isWindows = (SystemInfoFactory.createSystemInfo().getOperatingSystemType() == OperatingSystemType.WINDOWS);

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

    @Test
    public void excludeEmptyDirsFromCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");

        // create an empty directory
        File libDir = mkdir(resourceDir, "lib");
        assert (libDir.isDirectory());

        DriftDefinition driftDef = driftDefinition("coverage-test", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), driftDef));
        detector.run();

        File changeSet = changeSet(driftDef.getName(), COVERAGE);
        Headers headers = createHeaders(driftDef, COVERAGE);
        List<FileEntry> expected = asList(addedFileEntry("conf/server.conf", sha256(serverConf), serverConf
            .lastModified(), serverConf.length()));

        assertHeaderEquals(changeSet, headers);
        assertFileEntriesMatch("Only files should be included in a change set.", expected, changeSet);
    }

    @Test
    public void includeMultipleFilesInDirInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        File server2Conf = createRandomFile(confDir, "server-2.conf");

        DriftDefinition def = driftDefinition("multiple-files-test", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File changeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> entries = asList(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf
            .lastModified(), server1Conf.length()), addedFileEntry("conf/server-2.conf", sha256(server2Conf),
            server2Conf.lastModified(), server2Conf.length()));

        assertHeaderEquals(changeSet, createHeaders(def, COVERAGE));
        assertFileEntriesMatch("Each file in a directory should be included in a coverage change set", entries,
            changeSet);
    }

    @Test
    public void includedSiblingDirsInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");

        File libDir = mkdir(resourceDir, "lib");
        File serverLib = createRandomFile(libDir, "server.jar");

        DriftDefinition def = driftDefinition("sibling-dirs-test", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File changeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> entries = asList(addedFileEntry("conf/server.conf", sha256(serverConf), serverConf
            .lastModified(), serverConf.length()), addedFileEntry("lib/server.jar", sha256(serverLib), serverLib
            .lastModified(), serverLib.length()));

        assertHeaderEquals(changeSet, createHeaders(def, COVERAGE));
        assertFileEntriesMatch("A coverage change set should include files from multiple, sibling directories",
            entries, changeSet);
    }

    @Test
    public void includeNestedDirsInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");

        File subConfDir = mkdir(confDir, "subconf");
        File server2Conf = createRandomFile(subConfDir, "server-2.conf");

        DriftDefinition def = driftDefinition("nested-dirs-test", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File changeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> entries = asList(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf
            .lastModified(), server1Conf.length()), addedFileEntry("conf/subconf/server-2.conf", sha256(server2Conf),
            server2Conf.lastModified(), server2Conf.length()));

        assertHeaderEquals(changeSet, createHeaders(def, COVERAGE));
        assertFileEntriesMatch("A coverage change set should include files in nested sub directories", entries,
            changeSet);
    }

    @Test
    public void updateScheduleAfterGeneratingCoverageChangeSet() throws Exception {
        DriftDefinition driftDef = driftDefinition("update-schedule-after-coverage-changeset", resourceDir
            .getAbsolutePath());
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), driftDef);

        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");
        assert (serverConf.exists());

        long currentTime = System.currentTimeMillis();

        scheduleQueue.addSchedule(schedule);
        detector.run();

        assertTrue(schedule.getNextScan() >= (currentTime + driftDef.getInterval()),
            "Failed to update schedule. next scan is  " + schedule.getNextScan() + " and should be greater than "
                + (currentTime + driftDef.getInterval()));
    }

    @Test
    public void updateScheduleAfterGeneratingDriftChangeSet() throws Exception {

    }

    @Test
    public void doNotUpdateSnapshotOrGenerateDriftChangeSetIfNothingChanges() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");
        assert (serverConf.exists());

        DriftDefinition def = driftDefinition("nothing-to-update", resourceDir.getAbsolutePath());

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File changeSet = changeSet(def.getName(), COVERAGE);
        String originalHash = sha256(changeSet);

        // Reset the schedule so that detection will run again the next time we call
        // detection.run()
        DriftDetectionSchedule schedule = scheduleQueue.remove(resourceId(), def);
        schedule.resetSchedule();
        scheduleQueue.addSchedule(schedule);

        // Run the detector again. Note that nothing has changed so the snapshot should
        // remain the same and no drift change set file should be generated.
        detector.run();

        String newHash = sha256(changeSet);

        assertEquals(newHash, originalHash, "The snapshot file should not have changed since there was no drift. ");

        File driftChangeSet = changeSet(def.getName(), DRIFT);

        assertFalse(driftChangeSet.exists(), "A drift change set file should not have been generated since there was "
            + "no drift");
    }

    @Test
    public void skipDetectionForScheduledThatIsDisabled() throws Exception {
        detector.setDriftClient(new DriftClientTestStub() {
            {
                setBaseDir(resourceDir);
            }

            @Override
            public void sendChangeSetToServer(DriftDetectionSummary detectionSummary) {
                fail("Should not invoke drift client when drift definition is disabled");
            }
        });

        DriftDefinition def = driftDefinition("disabled-config-test", resourceDir.getAbsolutePath());
        def.setEnabled(false);

        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), def);
        long nextScan = schedule.getNextScan();

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = new File(confDir, "server-1.conf");
        touch(server1Conf);

        scheduleQueue.addSchedule(schedule);
        detector.run();

        // make sure that the next scan time is not updated
        assertEquals(schedule.getNextScan(), nextScan, "The next scan time for the drift detection schedule should "
            + " not get updated if drift detection does not actually run for the definition.");
    }

    @Test
    public void doNotUpdateScheduleIfItIsTooEarlyToRunDetection() throws Exception {
        DriftDefinition driftDef = driftDefinition("schedule-not-ready-test", resourceDir.getAbsolutePath());
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), driftDef);

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server.conf");
        assert (server1Conf.exists());

        scheduleQueue.addSchedule(schedule);
        detector.run();

        long nextScan = schedule.getNextScan();
        detector.run();

        assertEquals(schedule.getNextScan(), nextScan, "The next scan time for the drift detection schedule should "
            + " not get updated if drift detection does not actually run for the definition.");
    }

    @Test
    public void reportMissingBaseDirWhenNoInitialSnapshotExists() throws Exception {
        final File basedir = new File(resourceDir, "conf");
        DriftDefinition def = driftDefinition("basedir-does-not-exist", basedir.getAbsolutePath());

        driftClient.setBaseDir(basedir);

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        assertEquals(driftClient.getReportMissingBaseDirInvocationCount(), 1, "A missing base directory should be "
            + "reported to the server if no initial snapshot has already been generated.");
        assertEquals(driftClient.getSendChangeSetInvocationCount(), 0, "No initial change set should be sent to "
            + "the server if the base directory does not exist.");

        // verify that the initial change set was not generated
        File snapshot = changeSet(def.getName(), COVERAGE);
        assertFalse(snapshot.exists(), "An initial snapshot should not be written to disk if the base directory "
            + "does not exist.");
    }

    @Test
    public void skipDetectionWhenPreviousSnapshotFileExists() throws Exception {
        // The presence of a previous snapshot file means that the server has
        // not acknowledged that it has received and processed the change set.
        DriftDefinition def = driftDefinition("previous-snapshot-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        createRandomFile(confDir, "server.conf");

        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), def);
        scheduleQueue.addSchedule(schedule);
        detector.run();

        // create some drift and generate a new snapshot
        createRandomFile(confDir, "server-1.conf");
        schedule.resetSchedule();
        detector.run();

        File snapshot = changeSet(def.getName(), COVERAGE);
        String newHash = sha256(snapshot);
        File previousSnapshot = previousSnapshot(def.getName());
        String oldHash = sha256(previousSnapshot);

        // create some drift and make sure drift detection does not run.
        createRandomFile(confDir, "server-2.conf");
        schedule.resetSchedule();
        // Tell driftClient to throw an exception if detector attempts to send
        // the change set report to the server. The detector should never call
        // driftClient in this scenario.
        driftClient.setFailingOnSendChangeSet(true);
        detector.run();

        assertEquals(sha256(snapshot), newHash, "The snapshot should not have changed since the previous snapshot "
            + "is still on disk.");
        assertEquals(sha256(previousSnapshot), oldHash, "The previous snapshot should not have changed since "
            + "drift detection should not have run until the server acked the previous snapshot.");
    }

    @Test
    public void includeAddedFileInDriftChangeSet() throws Exception {
        DriftDefinition def = driftDefinition("file-added-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(def, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf.lastModified(), server1Conf
            .length()));
        writer.close();

        // Create some drift
        File server2Conf = createRandomFile(confDir, "server-2.conf");

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(addedFileEntry("conf/server-2.conf", sha256(server2Conf), server2Conf
            .lastModified(), server2Conf.length()));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf
            .lastModified(), server1Conf.length()), addedFileEntry("conf/server-2.conf", sha256(server2Conf),
            server2Conf.lastModified(), server2Conf.length()));

        // verify that the coverage change set was updated
        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @Test
    public void includeModifiedFileInDriftChangeSet() throws Exception {
        DriftDefinition def = driftDefinition("file-modified-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        String oldHash = sha256(server1Conf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(def, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", oldHash, server1Conf.lastModified(), server1Conf.length()));
        writer.close();

        // create some drift
        server1Conf.delete();
        server1Conf = createRandomFile(confDir, "server-1.conf", 48);
        String newHash = sha256(server1Conf);

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(changedFileEntry("conf/server-1.conf", oldHash, newHash, server1Conf
            .lastModified(), server1Conf.length()));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(changedFileEntry("conf/server-1.conf", oldHash, newHash, server1Conf
            .lastModified(), server1Conf.length()));

        // verify that the coverage change set was updated
        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @Test(enabled = false)
    public void includeFiledAddedInNewDirectoryInDriftChangeSet() throws Exception {
        DriftDefinition def = driftDefinition("file-added-in-new-dir", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(def, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf.lastModified(), server1Conf
            .length()));
        writer.close();

        // create some drift
        File subconfDir = mkdir(confDir, "subconf");
        File server2Conf = createRandomFile(subconfDir, "server-2.conf");

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(addedFileEntry("conf/subconf/server-2.conf", sha256(server2Conf),
            server2Conf.lastModified(), server2Conf.length()));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf
            .lastModified(), server1Conf.length()), addedFileEntry("conf/subconf/server-2.conf", sha256(server2Conf),
            server2Conf.lastModified(), server2Conf.length()));

        // verify that the coverage change set was updated
        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @Test
    public void includeRemovedFileInDriftChangeSet() throws Exception {
        DriftDefinition def = driftDefinition("file-removed-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        File server2Conf = createRandomFile(confDir, "server-2.conf");

        String server2ConfHash = sha256(server2Conf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(def, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf.lastModified(), server1Conf
            .length()));
        writer.write(addedFileEntry("conf/server-2.conf", server2ConfHash, server2Conf.lastModified(), server2Conf
            .length()));
        writer.close();

        // create some drift
        server2Conf.delete();

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(removedFileEntry("conf/server-2.conf", server2ConfHash));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        // verify that the coverage change set was updated
        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf
            .lastModified(), server1Conf.length()));

        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @Test
    public void reportDriftWhenBaseDirIsDeleted() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");
        String serverConfHash = sha256(serverConf);

        DriftDefinition def = driftDefinition("delete-basedir-test", confDir.getAbsolutePath());
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), def);

        // generate the initial snapshot
        scheduleQueue.addSchedule(schedule);
        detector.run();

        // Delete the base directory
        deleteDirectory(confDir);

        // re-run the detector
        schedule.resetSchedule();
        detector.run();

        // verify that the drift change set was generated
        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(removedFileEntry("conf/server.conf", serverConfHash));

        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        // verify that the snapshot was updated
        File currentSnapshot = changeSet(def.getName(), COVERAGE);
        List<FileEntry> snapshotEntries = emptyList();

        assertHeaderEquals(currentSnapshot, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The current snapshot was not updated as expected", snapshotEntries, currentSnapshot);
    }

    @Test
    public void reportDriftWhenBaseDirIsAdded() throws Exception {

    }

    @Test
    public void includeFilesInRemovedDirectoryInDriftChangeSet() throws Exception {
        DriftDefinition def = driftDefinition("dir-removed-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        String server1Hash = sha256(server1Conf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(def, COVERAGE));
        writer
            .write(addedFileEntry("conf/server-1.conf", server1Hash, server1Conf.lastModified(), server1Conf.length()));
        writer.close();

        // create some drift
        server1Conf.delete();
        confDir.delete();

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(removedFileEntry("conf/server-1.conf", server1Hash));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        // verify that the coverage change set was updated
        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = emptyList();

        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @Test
    public void revertToPreviousSnapshotWhenSendingChangeSetFails() throws Exception {
        DriftDefinition def = driftDefinition("revert-snapshot-test", resourceDir.getAbsolutePath());
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), def);

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server.conf");
        assert (server1Conf.exists());

        scheduleQueue.addSchedule(schedule);
        // generate the initial snapshot
        detector.run();

        // Now generate a drift change set
        createRandomFile(confDir, "server-1.conf");
        schedule.resetSchedule();
        detector.run();

        File changeSet = changeSet(def.getName(), COVERAGE);
        String currentHash = sha256(changeSet);

        // Need to delete the previous version snapshot file; otherwise, the
        // next detection run will be skipped.
        previousSnapshot(def.getName()).delete();

        // generate some more drift, and fail on sending the change set
        // to the server
        createRandomFile(confDir, "server-2.conf");
        schedule.resetSchedule();
        driftClient.setFailingOnSendChangeSet(true);
        try {
            detector.run();
        } catch (RuntimeException e) {
        }

        String newHash = sha256(changeSet);

        assertEquals(newHash, currentHash, "The snapshot file should be reverted if sending the new snapshot "
            + "to the server fails.");
        // The previous version file must be deleted on revert; otherwise, drift
        // detection will not run for the schedule if the previous version file
        // is found on disk.
        assertFalse(previousSnapshot(def.getName()).exists(), "The copy of the previous version snapshot file "
            + "should be deleted once we have reverted back to it and have a new, current snapsot file.");
    }

    @Test
    public void purgeSnapshotWhenSendingInitialChangeSetFails() throws Exception {
        // If we have just generated the initial change set and sending it to
        // the server fails, then there is no prior snapshot version to which
        // we can revert. We therefore need to purge the snapshot file and
        // allow DriftDetector to simply regenerate the initial change set again.

        DriftDefinition def = driftDefinition("purge-snapshot-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        createRandomFile(confDir, "server.conf");

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        driftClient.setFailingOnSendChangeSet(true);
        try {
            detector.run();
        } catch (RuntimeException e) {
        }

        assertFalse(changeSet(def.getName(), COVERAGE).exists(), "Snapshot file should be deleted when "
            + "only the initial change set has been generated and sending change send report to server fails");
    }

    @Test
    public void ignoreFilesThatAreNotReadableForCoverageChangeSet() throws Exception {

        DriftDefinition def = driftDefinition("nonreadable-files-coverage", resourcesDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        File server2Conf = createRandomFile(confDir, "server-2.conf");
        setNotReadable(server2Conf);

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File changeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> entries = asList(addedFileEntry("conf/server-1.conf", sha256(server1Conf), server1Conf
            .lastModified(), server1Conf.length()));

        assertHeaderEquals(changeSet, createHeaders(def, COVERAGE));
        assertFileEntriesMatch("Files that are non-readable should be skipped but other, readable file should still "
            + "be included in the change set", entries, changeSet);
    }

    @Test
    public void ignoreNewFilesThatAreNotReadableForDriftChangeSet() throws Exception {
        DriftDefinition def = driftDefinition("nonreadable-files-drfit", resourceDir.getAbsolutePath());
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), def);

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        String oldServer1Hash = sha256(server1Conf);

        scheduleQueue.addSchedule(schedule);
        detector.run();

        // create some drift that includes a new file that is not readable
        server1Conf.delete();
        server1Conf = createRandomFile(confDir, "server-1.conf", 48);
        String newServer1Hash = sha256(server1Conf);

        File server2Conf = createRandomFile(confDir, "server-2.conf");
        setNotReadable(server2Conf);

        schedule.resetSchedule();
        detector.run();

        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(changedFileEntry("conf/server-1.conf", oldServer1Hash, newServer1Hash,
            server1Conf.lastModified(), server1Conf.length()));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        // verify that the coverage change set was updated
        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(changedFileEntry("conf/server-1.conf", oldServer1Hash, newServer1Hash,
            server1Conf.lastModified(), server1Conf.length()));

        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @Test
    public void markFileUnderDriftDetectionAsRemovedWhenItIsMadeNonReadable() throws Exception {
        DriftDefinition def = driftDefinition("file-made-nonreadable", resourceDir.getAbsolutePath());
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), def);

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        String server1Hash = sha256(server1Conf);

        scheduleQueue.addSchedule(schedule);
        detector.run();

        // make the file non-readable and run the detector again
        setNotReadable(server1Conf);

        schedule.resetSchedule();
        detector.run();

        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(removedFileEntry("conf/server-1.conf", server1Hash));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        // verify that the coverage change set was updated
        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = emptyList();

        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @Test
    public void doNotModifyPinnedSnapshotWhenDriftIsDetected() throws Exception {
        DriftDefinition driftDef = driftDefinition("do-not-modify-pinned-snapshot", resourceDir.getAbsolutePath());
        driftDef.setPinned(true);
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), driftDef);

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server1.conf");

        scheduleQueue.addSchedule(schedule);
        detector.run();

        // When the initial snapshot is pinned, we need to generate it; otherwise, it should
        // be provided by the server.
        File currentSnapshot = changeSet(driftDef.getName(), COVERAGE);
        File pinnedSnapshot = new File(currentSnapshot.getParentFile(), "snapshot.pinned");
        String originalPinnedHash = sha256(pinnedSnapshot);

        // generate some drift
        File server2Conf = createRandomFile(confDir, "server2.conf");
        schedule.resetSchedule();
        detector.run();

        String newPinnedHash = sha256(pinnedSnapshot);

        assertEquals(newPinnedHash, originalPinnedHash, "When a snapshot is pinned, it should not get updated during "
            + "drift detection");

        // We always generate/update the current snapshot so we still need to verify that it
        // was generated/updated correctly
        List<FileEntry> fileEntries = asList(addedFileEntry("conf/server1.conf", sha256(server1Conf), server1Conf
            .lastModified(), server1Conf.length()), addedFileEntry("conf/server2.conf", sha256(server2Conf),
            server2Conf.lastModified(), server2Conf.length()));

        assertHeaderEquals(currentSnapshot, createHeaders(driftDef, COVERAGE, 1));
        assertFileEntriesMatch("The current snapshot file should still get updated even when using a pinned snapshot",
            fileEntries, currentSnapshot);
    }

    @Test
    public void updateCurrentSnapshotVersionNumberWhenUsingPinnedSnapshot() throws Exception {
        DriftDefinition driftDef = driftDefinition("update-snapshot-version-pinned", resourceDir.getAbsolutePath());
        driftDef.setPinned(true);

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server1.conf");

        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), driftDef);
        scheduleQueue.addSchedule(schedule);
        detector.run();

        // create some drift which should result in version 1
        File server2Conf = createRandomFile(confDir, "server2.conf");
        schedule.resetSchedule();
        detector.run();

        File currentSnapshot = changeSet(driftDef.getName(), COVERAGE);
        File previousSnapshot = new File(currentSnapshot.getParentFile(), "changeset.txt.previous");
        previousSnapshot.delete();

        // create some more drift which should result in version 2
        File server3Conf = createRandomFile(confDir, "server3.conf");
        schedule.resetSchedule();
        detector.run();

        // verify that the current snapshot was updated
        List<FileEntry> currentSnapshotEntries = asList(addedFileEntry("conf/server1.conf", sha256(server1Conf),
            server1Conf.lastModified(), server1Conf.length()), addedFileEntry("conf/server2.conf", sha256(server2Conf),
            server2Conf.lastModified(), server2Conf.length()), addedFileEntry("conf/server3.conf", sha256(server3Conf),
            server3Conf.lastModified(), server3Conf.length()));

        assertHeaderEquals(currentSnapshot, createHeaders(driftDef, COVERAGE, 2));
        assertFileEntriesMatch("The current snapshot file should still get updated even when using a pinned snapshot",
            currentSnapshotEntries, currentSnapshot);

        // verify that the the drift/delta change set was generated
        File driftChangeSet = changeSet(driftDef.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(addedFileEntry("conf/server2.conf", sha256(server2Conf), server2Conf
            .lastModified(), server2Conf.length()), addedFileEntry("conf/server3.conf", sha256(server3Conf),
            server3Conf.lastModified(), server3Conf.length()));

        assertHeaderEquals(driftChangeSet, createHeaders(driftDef, DRIFT, 2));
        assertFileEntriesMatch("The drift change set was not generated correctly when using a pinned snapshot",
            driftEntries, driftChangeSet);

    }

    @Test
    public void generatePinnedSnapshotFileWhenInitialVersionIsPinned() throws Exception {
        DriftDefinition driftDef = driftDefinition("initial-snapshot-pinned-test", resourceDir.getAbsolutePath());
        driftDef.setPinned(true);

        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), driftDef));
        detector.run();

        File changeSet = changeSet(driftDef.getName(), COVERAGE);
        File pinnedSnapshot = new File(changeSet.getParentFile(), "snapshot.pinned");
        List<FileEntry> entries = asList(addedFileEntry("conf/server.conf", sha256(serverConf), serverConf
            .lastModified(), serverConf.length()));

        assertTrue(changeSet.exists(), "An initial snapshot file should be generated even when it is pinned");
        assertHeaderEquals(changeSet, createHeaders(driftDef, COVERAGE));
        assertFileEntriesMatch("Initial snapshot entries are wrong for pinned snapshot", entries, changeSet);

        assertTrue(pinnedSnapshot.exists(), "Pinned snapshot file should be generated when initial version is pinned");
        assertEquals(sha256(changeSet), sha256(pinnedSnapshot), "The contents of the pinned snapshot file and the "
            + "initial snapshot should be identical");
    }

    @Test
    public void notifyServerOfRepeatChangeSet() throws Exception {
        final DriftDefinition driftDef = driftDefinition("repeat-changeset", resourceDir.getAbsolutePath());
        driftDef.setId(1);
        driftDef.setPinned(true);

        File confDir = mkdir(resourceDir, "conf");
        createRandomFile(confDir, "server1.conf");

        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), driftDef);

        scheduleQueue.addSchedule(schedule);
        detector.run();

        // generate some drift
        createRandomFile(confDir, "server2.conf");
        schedule.resetSchedule();
        detector.run();

        File currentSnapshot = changeSet(driftDef.getName(), COVERAGE);
        String currentSnapshotHash = sha256(currentSnapshot);

        File driftChangeSet = changeSet(driftDef.getName(), DRIFT);
        String driftChangeSetHash = sha256(driftChangeSet);

        // We have to delete the previous version snapshot so that the detector will
        // run again.
        File previousSnapshot = previousSnapshot(driftDef.getName());
        previousSnapshot.delete();

        // Now do another drift detection run. This should re-detect the same drift that
        // was previously detected. It should not however, produce a new current snapshot
        // since there are no changes on the file system.
        final AtomicBoolean repeatChangeSetCalled = new AtomicBoolean(false);
        detector.setDriftClient(new DriftClientTestStub() {
            {
                setBaseDir(resourceDir);
            }

            @Override
            public void sendChangeSetToServer(DriftDetectionSummary detectionSummary) {
                fail("Do not send repeat change set to server.");
            }

            @Override
            public void repeatChangeSet(int resourceId, String driftDefName, int version) {
                repeatChangeSetCalled.set(true);
                assertEquals(resourceId, resourceId(), "The resource id for the repeat change set is wrong");
                assertEquals(driftDefName, driftDef.getName(), "The drift definition name for the repeat change set "
                    + "is wrong");
                assertEquals(version, 1, "The snapshot version should not have changed since no new drift was detected");
            }
        });

        schedule.resetSchedule();
        detector.run();

        // verify that the current snapshot file has not changed
        assertEquals(sha256(currentSnapshot), currentSnapshotHash, "The current snapshot should not have been updated");

        // verify that the drift change set has not changed
        assertEquals(sha256(driftChangeSet), driftChangeSetHash, "The drift change set file should not have changed");

        // verify that notified the server
        assertTrue(repeatChangeSetCalled.get(), "Failed to notify server of repeat change set");

        // verify that the previous version snapshot file has been deleted
        assertFalse(previousSnapshot.exists(), "There should be no previous version snapshot file because the "
            + "server has already acknowledged the current snapshot.");

    }

    @Test
    public void detectWhenResourceComesBackIntoCompliance() throws Exception {
        DriftDefinition driftDef = driftDefinition("back-into-compliance", resourceDir.getAbsolutePath());
        driftDef.setPinned(true);

        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");
        String serverConfHash = sha256(serverConf);

        Headers headers = createHeaders(driftDef, COVERAGE);

        // generate the pinned snapshot which is version zero
        File pinnedSnapshot = pinnedSnapshot(driftDef.getName());
        ChangeSetWriter writer = new ChangeSetWriterImpl(pinnedSnapshot, headers);
        writer
            .write(addedFileEntry("conf/server.conf", serverConfHash, serverConf.lastModified(), serverConf.length()));
        writer.close();

        // generate the current snapshot file. we will take a shortcut here by
        // just copying the pinned snapshot. We can do this since the current
        // snapshot will be identical to the pinned snapshot after the current
        // snapshot is first generated.
        File currentSnapshot = changeSet(driftDef.getName(), COVERAGE);
        copyFile(pinnedSnapshot, currentSnapshot);

        // now generate some drift causing the resource to go out of compliance
        File newServerConf = createRandomFile(confDir, "new_server.conf");
        String newServerConfHash = sha256(newServerConf);
        assert (null != newServerConfHash);

        // do a drift detection run
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), driftDef);
        scheduleQueue.addSchedule(schedule);
        // this run should produce version one
        detector.run();

        // now put the resource back into compliance
        newServerConf.delete();

        // do another drift detection run but first we have to delete the
        // previous snapshot file otherwise the detection scan will not run.
        previousSnapshot(driftDef.getName()).delete();
        schedule.resetSchedule();
        // this run should produce version two
        detector.run();

        // verify that that current snapshot has been updated to reflect that
        // the resource is back in compliance.
        List<FileEntry> entries = asList(addedFileEntry("conf/server.conf", serverConfHash, serverConf.lastModified(),
            serverConf.length()));

        assertHeaderEquals(currentSnapshot, createHeaders(driftDef, COVERAGE, 2));
        assertFileEntriesMatch("The entries in the current snapshot should match those in the pinned snapshot "
            + "once the resource has gone back into compliance.", entries, currentSnapshot);
    }

    @Test
    public void updateTimestampInfoNoDriftTest() throws Exception {
        DriftDefinition def = driftDefinition("update-timestamp-nodrift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        String server1Hash = sha256(server1Conf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(def, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", server1Hash, -1L, -1L));
        writer.close();

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        // verify that no drift change set was generated
        File driftChangeSet = changeSet(def.getName(), DRIFT);
        assertFalse(driftChangeSet.exists(), "Expected no drift change set " + driftChangeSet.getPath());

        // verify that the coverage change set was updated with timestamp info, version is still 0
        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(addedFileEntry("conf/server-1.conf", server1Hash, server1Conf
            .lastModified(), server1Conf.length()));

        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 0));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    @Test
    public void updateTimestampInfoDriftTest() throws Exception {
        DriftDefinition def = driftDefinition("update-timestamp-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        String server1Hash = sha256(server1Conf);
        File server2Conf = createRandomFile(confDir, "server-2.conf");
        String server2Hash = sha256(server2Conf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(def, COVERAGE));
        writer.write(addedFileEntry("conf/server-1.conf", server1Hash, -1L, -1L));
        writer.write(addedFileEntry("conf/server-2.conf", server2Hash, -1L, -1L));
        writer.close();

        // create some drift
        server1Conf.delete();
        confDir.delete();

        scheduleQueue.addSchedule(new DriftDetectionSchedule(resourceId(), def));
        detector.run();

        File driftChangeSet = changeSet(def.getName(), DRIFT);
        List<FileEntry> driftEntries = asList(removedFileEntry("conf/server-1.conf", server1Hash));

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, createHeaders(def, DRIFT, 1));
        assertFileEntriesMatch("The drift change set does not match the expected values", driftEntries, driftChangeSet);

        // verify that the coverage change set was updated with timestamp info and incremented version
        File coverageChangeSet = changeSet(def.getName(), COVERAGE);
        List<FileEntry> coverageEntries = asList(addedFileEntry("conf/server-2.conf", server2Hash, server2Conf
            .lastModified(), server2Conf.length()));

        assertHeaderEquals(coverageChangeSet, createHeaders(def, COVERAGE, 1));
        assertFileEntriesMatch("The coverage change set was not updated as expected", coverageEntries,
            coverageChangeSet);
    }

    private void assertHeaderEquals(File changeSet, Headers expected) throws Exception {
        ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new FileReader(changeSet)));
        Headers actual = reader.getHeaders();
        assertPropertiesMatch(expected, actual, "Headers for " + changeSet.getPath() + " do not match "
            + "expected values");
    }

    private void assertFileEntriesMatch(String msg, List<FileEntry> expected, File changeSet) throws Exception {
        List<FileEntry> actual = new ArrayList<FileEntry>();
        ChangeSetReader reader = new ChangeSetReaderImpl(changeSet);

        for (FileEntry entry : reader) {
            actual.add(entry);
        }

        assertCollectionMatchesNoOrder(msg, expected, actual);
    }

    /**
     * Attempts to make a file non-readable. Windows does not support file permissions like
     * unix and linux platforms do.
     *
     * @param file The file to update
     */
    private void setNotReadable(File file) {
        boolean setToReadable = file.setReadable(false);
        // not every win os (maybe none) supports this call, perform the test anyway, as best as possible
        if (!setToReadable) {
            if (isWindows) {
                file.delete();
            } else {
                assertTrue(setToReadable, "Failed to make " + file.getPath() + " write only");
            }
        }
    }

}
