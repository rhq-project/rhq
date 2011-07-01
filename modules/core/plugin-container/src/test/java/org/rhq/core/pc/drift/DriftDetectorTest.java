package org.rhq.core.pc.drift;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftConfiguration;

import static org.apache.commons.io.IOUtils.readLines;
import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.unitils.thirdparty.org.apache.commons.io.FileUtils.touch;

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

        File changeSetDir = changeSetDir(driftConfig.getName());
        File changeSet = new File(changeSetDir, "changeset.txt");
        List<String> lines = readLines(new BufferedInputStream(new FileInputStream(changeSet)));

        assertHeaderEquals(lines, driftConfig.getName(), driftConfig.getBasedir().getValueName(), COVERAGE.code());
        assertThatChangeSetDoesNotContainEmptyDirs(changeSet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void usePeriodAsNameOfBasedirDirectoryEntry() throws Exception {
        touch(new File(resourceDir, "data-1.txt"));
        touch(new File(resourceDir, "data-2.txt"));

        DriftConfiguration driftConfig = driftConfiguration("basedir-entry-test", resourceDir.getAbsolutePath());

        scheduleQueue.enqueue(new DriftDetectionSchedule(resourceId(), driftConfig));
        detector.run();

        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId(), driftConfig);

        scheduleQueue.enqueue(schedule);
        detector.run();

        File changeSetDir = changeSetDir(driftConfig.getName());
        File changeSet = new File(changeSetDir, "changeset.txt");
        List<String> lines = readLines(new BufferedInputStream(new FileInputStream(changeSet)));

        assertEquals(lines.size(), 7, "Expected " + changeSet.getPath() + " to have seven lines.");
        assertEquals(". 2", lines.get(3), "The directory header name is wrong for change set " +
            changeSet.getAbsolutePath());
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

        File changesetDir = changeSetDir(config.getName());
        File changeSet = new File(changesetDir, "changeset.txt");
        List<String> lines = readLines(new BufferedInputStream(new FileInputStream(changeSet)));

        assertEquals(lines.size(), 9, "Expected " + changeSet.getPath() + " to have 9 lines");

        assertHeaderEquals(lines, "nested-dirs-test", resourceDir.getAbsolutePath(), "C");
//        assertChangeSetContainsDirEntry(lines,
//            "conf 1",
//            sha256(server1Conf) + " 0 " + server1Conf.getName() + " " + FILE_ADDED
//        );
    }

    void assertHeaderEquals(List<String> lines, String... expected) {
        assertEquals(lines.get(0), expected[0], "The first header entry should be the drift configuration name.");
        assertEquals(lines.get(1), expected[1], "The second header entry should be the base directory.");
        assertEquals(lines.get(2), expected[2], "The third header entry should be a flag indicating the change set "
            + "type.");
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

//    void assertChangeSetContainsDirEntry(List<String> changeSet, String... dirEntry) {
//        String dirEntryHeader = dirEntry[0];
//        int i = -1;
//        boolean found = false;
//
//        for (String line : changeSet) {
//            if (line.equals(dirEntryHeader)) {
//                found = true;
//            }
//            ++i;
//            if (found) {
//                break;
//            }
//        }
//    }
//
//    void assertChangeSetContainsDirEntry(File changeSet, String... dirEntry) throws Exception {
//        ChangeSetReader reader = new ChangeSetReaderImpl(changeSet);
//        DirectoryEntry actualDirEntry = reader.readDirectoryEntry();
//
//        while (dir)
//    }

    DriftConfiguration driftConfiguration(String name, String basedir) {
        DriftConfiguration config = new DriftConfiguration(new Configuration());
        config.setName(name);
        config.setBasedir(new DriftConfiguration.BaseDirectory(fileSystem, basedir));

        return config;
    }
}
