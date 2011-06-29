package org.rhq.core.pc.drift;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.IOUtils.readLines;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.testng.Assert.assertEquals;
import static org.unitils.thirdparty.org.apache.commons.io.FileUtils.touch;

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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.test.JMockTest;

public class DriftDetectorTest extends JMockTest {

    File changeSetsDir;

    File resourcesDir;

    ChangeSetManager changeSetMgr;

    ScheduleQueue scheduleQueue;

    @BeforeClass
    public void init() throws Exception {
        File basedir = new File("target", getClass().getSimpleName());
        deleteDirectory(basedir);
        basedir.mkdir();

        changeSetsDir = new File(basedir, "changesets");
        changeSetsDir.mkdir();

        resourcesDir = new File(basedir, "resources");
        resourcesDir.mkdir();
    }

    @BeforeMethod
    public void setUp() {
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
        scheduleQueue = new ScheduleQueueImpl();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void generateCoverageChangeSet() throws Exception {
        File server1Dir = new File(resourcesDir, "server-1");
        server1Dir.mkdir();

        File confDir = new File(server1Dir, "conf");
        confDir.mkdir();

        touch(new File(confDir, "server.conf"));

        DriftConfiguration driftConfig = driftConfiguration("coverage-test", server1Dir.getAbsolutePath());

        int resourceId = 1;
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId, driftConfig);

        scheduleQueue.enqueue(schedule);

        DriftClientTestStub driftClient = new DriftClientTestStub();
        driftClient.setBaseDir(server1Dir);

        DriftDetector detector = new DriftDetector();
        detector.setDriftClient(driftClient);
        detector.setChangeSetManager(changeSetMgr);
        detector.setScheduleQueue(scheduleQueue);

        detector.run();

        File changeSetDir = new File(new File(changeSetsDir, Integer.toString(resourceId)), "coverage-test");
        File changeSet = new File(changeSetDir, "changeset.txt");

        List<String> lines = readLines(new BufferedInputStream(new FileInputStream(changeSet)));

        assertHeaderEquals(lines, driftConfig.getName(), driftConfig.getBasedir().getValueName(), COVERAGE.code());
        assertThatChangeSetDoesNotContainEmptyDirs(changeSet);
    }

    DriftConfiguration driftConfiguration(String name, String basedir) {
        Configuration config = new Configuration();
        config.put(new PropertySimple("name", name));
        config.put(new PropertySimple("basedir", basedir));

        return new DriftConfiguration(config);
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

}
