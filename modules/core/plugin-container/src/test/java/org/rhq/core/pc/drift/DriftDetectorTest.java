package org.rhq.core.pc.drift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftConfiguration;

import static org.apache.commons.io.FileUtils.touch;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertNotNull;
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
        touch(new File(confDir, "server.conf"));

        DriftConfiguration driftConfig = driftConfiguration("coverage-test", resourceDir.getAbsolutePath());

        scheduleQueue.enqueue(new DriftDetectionSchedule(resourceId(), driftConfig));
        detector.run();

        File changeSet = changeSet(driftConfig.getName(), COVERAGE);

        assertHeaderEquals(changeSet, new Headers(driftConfig.getName(), resourceDir.getAbsolutePath(), COVERAGE));
        assertThatChangeSetDoesNotContainEmptyDirs(changeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void usePeriodAsNameOfBasedirDirectoryEntry() throws Exception {
        File data1 = new File(resourceDir, "data-1.txt");
        touch(data1);
        File data2 = new File(resourceDir, "data-2.txt");
        touch(data2);

        DriftConfiguration driftConfig = driftConfiguration("basedir-entry-test", resourceDir.getAbsolutePath());

        scheduleQueue.enqueue(new DriftDetectionSchedule(resourceId(), driftConfig));
        detector.run();

        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), driftConfig);

        scheduleQueue.enqueue(schedule);
        detector.run();

        assertChangeSetContainsDirEntry(changeSet(driftConfig.getName(), COVERAGE),
            new DirectoryEntry(".")
                .add(addedFileEntry("data-1.txt", sha256(data1)))
                .add(addedFileEntry("data-2.txt", sha256(data2))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeMultipleFilesInDirInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = new File(confDir, "server-1.conf");
        touch(server1Conf);
        File server2Conf = new File(confDir, "server-2.conf");
        touch(server2Conf);

        DriftConfiguration config = driftConfiguration("multiple-files-test", resourceDir.getAbsolutePath());

        scheduleQueue.enqueue(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File changeSet = changeSet(config.getName(), COVERAGE);

        assertHeaderEquals(changeSet, new Headers(config.getName(), resourceDir.getAbsolutePath(), COVERAGE));
        assertChangeSetContainsDirEntry(changeSet,
            new DirectoryEntry("conf")
                .add(addedFileEntry("server-1.conf", sha256(server1Conf)))
                .add(addedFileEntry("server-2.conf", sha256(server2Conf))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includedSiblingDirsInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File serverConf = new File(confDir, "server.conf");
        touch(serverConf);

        File libDir = mkdir(resourceDir, "lib");
        File serverLib = new File(libDir, "server.jar");
        touch(serverLib);

        DriftConfiguration config = driftConfiguration("sibling-dirs-test", resourceDir.getAbsolutePath());

        scheduleQueue.enqueue(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File changeSet = changeSet(config.getName(), COVERAGE);

        assertHeaderEquals(changeSet, new Headers(config.getName(), resourceDir.getAbsolutePath(), COVERAGE));

        assertChangeSetContainsDirEntry(changeSet,
            new DirectoryEntry("conf").add(addedFileEntry("server.conf", sha256(serverConf))));
        assertChangeSetContainsDirEntry(changeSet,
            new DirectoryEntry("lib").add(addedFileEntry("server.jar", sha256(serverLib))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeNestedDirsInCoverageChangeSet() throws Exception {
        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = new File(confDir, "server-1.conf");
        touch(server1Conf);

        File subConfDir = mkdir(confDir, "subconf");
        File server2Conf = new File(subConfDir, "server-2.conf");
        touch(server2Conf);

        DriftConfiguration config = driftConfiguration("nested-dirs-test", resourceDir.getAbsolutePath());

        scheduleQueue.enqueue(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File changeSet = changeSet(config.getName(), COVERAGE);

        assertHeaderEquals(changeSet, new Headers("nested-dirs-test", resourceDir.getAbsolutePath(), COVERAGE));

        assertChangeSetContainsDirEntry(changeSet,
            new DirectoryEntry("conf").add(addedFileEntry("server-1.conf", sha256(server1Conf))));
        assertChangeSetContainsDirEntry(changeSet,
            new DirectoryEntry("conf/subconf").add(addedFileEntry("server-2.conf", sha256(server2Conf))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeAddedFileInDriftChangeSet() throws Exception {
        DriftConfiguration config = driftConfiguration("file-added-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = new File(confDir, "server-1.conf");
        touch(server1Conf);

        File changeSetDir = changeSetDir(config.getName());

        // Generate the initial, coverage change set
        writeChangeSet(changeSetDir,
            config.getName(),
            resourceDir.getAbsolutePath(),
            COVERAGE.code(),
            "conf 1",
            sha256(server1Conf) + " 0 server-1.conf A",
            ""
        );

        // Create some drift
        File server2Conf = new File(confDir, "server-2.conf");
        touch(server2Conf);

        scheduleQueue.enqueue(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File driftChangeSet = changeSet(config.getName(), DRIFT);

        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());

        // verify that the drift change set was generated
        assertHeaderEquals(driftChangeSet, new Headers(config.getName(), resourceDir.getAbsolutePath(), DRIFT));
        assertChangeSetContainsDirEntry(driftChangeSet, new DirectoryEntry("conf")
            .add(addedFileEntry("server-2.conf", sha256(server2Conf))));

        File coverageChangeSet = changeSet(config.getName(), COVERAGE);

        // verify that the coverage change set was updated
        assertHeaderEquals(coverageChangeSet, new Headers(config.getName(), resourceDir.getAbsolutePath(), COVERAGE));
        assertChangeSetContainsDirEntry(coverageChangeSet, new DirectoryEntry("conf")
            .add(addedFileEntry("server-1.conf", sha256(server1Conf)))
            .add(addedFileEntry("server-2.conf", sha256(server2Conf))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void includeRemovedFileInDriftChangeSet() throws Exception {
        DriftConfiguration config = driftConfiguration("file-removed-drift-test", resourceDir.getAbsolutePath());

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = new File(confDir, "server-1.conf");
        touch(server1Conf);
        File server2Conf = new File(confDir, "server-2.conf");
        touch(server2Conf);

        String server2ConfHash = sha256(server2Conf);
        File changeSetDir = changeSetDir(config.getName());

        // generate the initial, coverage change set
        writeChangeSet(changeSetDir,
            config.getName(),
            resourceDir.getAbsolutePath(),
            COVERAGE.code(),
            "conf 2",
            sha256(server1Conf) + " 0 server-1.conf A",
            server2ConfHash + " 0 server-2.conf A",
            ""
        );

        // create some drift
        server2Conf.delete();

        scheduleQueue.enqueue(new DriftDetectionSchedule(resourceId(), config));
        detector.run();

        File driftChangeSet = changeSet(config.getName(), DRIFT);

        // verify that the drift change set was generated
        assertTrue(driftChangeSet.exists(), "Expected to find drift change set " + driftChangeSet.getPath());
        assertHeaderEquals(driftChangeSet, new Headers(config.getName(), resourceDir.getAbsolutePath(), DRIFT));
        assertChangeSetContainsDirEntry(driftChangeSet, new DirectoryEntry("conf")
            .add(removedFileEntry("server-2.conf", server2ConfHash)));

        // verify that the coverage change set was updated
        File coverageChangeSet = changeSet(config.getName(), COVERAGE);

        assertHeaderEquals(coverageChangeSet, new Headers(config.getName(), resourceDir.getAbsolutePath(), COVERAGE));
        assertChangeSetContainsDirEntry(coverageChangeSet, new DirectoryEntry("conf")
            .add(addedFileEntry("server-1.conf", sha256(server1Conf))));
    }

    void assertHeaderEquals(File changeSet, Headers expected) throws Exception {
        ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new FileReader(changeSet)));
        Headers actual = reader.getHeaders();
        assertPropertiesMatch(expected, actual, "Headers for " + changeSet.getPath() + " do not match " +
            "expected values");
    }

    void assertThatChangeSetDoesNotContainEmptyDirs(File changeSet) throws Exception {
        ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new FileReader(changeSet)));
        DirectoryEntry dirEntry = reader.readDirectoryEntry();

        while (dirEntry != null) {
            Assert.assertTrue(dirEntry.getNumberOfFiles() > 0, "The change set file should not include empty "
                + "directories");
            dirEntry = reader.readDirectoryEntry();
        }
    }

    void assertChangeSetContainsDirEntry(File changeSet, DirectoryEntry expected) throws Exception {
        ChangeSetReader reader = new ChangeSetReaderImpl(changeSet);
        DirectoryEntry actual = null;

        for (DirectoryEntry entry : reader) {
            if (entry.getDirectory().equals(expected.getDirectory())) {
                actual = entry;
                break;
            }
        }

        assertNotNull(actual, "Failed to find " + expected + " in " + changeSet.getPath());
        assertCollectionMatchesNoOrder(fileEntries(expected), fileEntries(actual), "File entries for " +
            expected + " in change set " + changeSet.getPath() + " do not match");
    }

    Collection<FileEntry> fileEntries(DirectoryEntry dirEntry) {
        List<FileEntry> list = new ArrayList<FileEntry>();
        for (FileEntry entry : dirEntry) {
            list.add(entry);
        }
        return list;
    }

}
