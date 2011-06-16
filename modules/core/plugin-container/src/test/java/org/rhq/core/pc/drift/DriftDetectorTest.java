package org.rhq.core.pc.drift;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.jmock.Expectations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetWriterImpl;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.test.JMockTest;

import static org.apache.commons.io.FileUtils.touch;
import static org.testng.Assert.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;

public class DriftDetectorTest extends JMockTest {

    File resourcesDir;

    File changeSetsDir;

    ScheduleQueue queue;

    ChangeSetManager changeSetMgr;

    @BeforeClass
    public void resetDataDir() throws Exception {
        File basedir = new File("target", getClass().getSimpleName());
        deleteDirectory(basedir);

        resourcesDir = new File(basedir, "resources");
        changeSetsDir = new File(basedir, "changesets");

        assertTrue(resourcesDir.mkdirs(), "Failed to create " + resourcesDir.getAbsolutePath());
        assertTrue(changeSetsDir.mkdirs(), "Failed to create " + changeSetsDir.getAbsolutePath());
    }

    @BeforeMethod
    public void initTest() {
        queue = context.mock(ScheduleQueue.class);
        changeSetMgr = context.mock(ChangeSetManager.class);
    }

    @Test
    public void generateInitialChangeSet() throws Exception {
        File server = new File(resourcesDir, "server");
        File lib = new File(server, "lib");
        lib.mkdirs();

        touch(new File(lib, "mylib1.jar"));
        touch(new File(lib, "mylib2.jar"));

        File nativeLib = new File(lib, "native");
        nativeLib.mkdir();
        touch(new File(nativeLib, "my-native-lib-1.so"));
        touch(new File(nativeLib, "my-native-lib-2.so"));

        final DriftConfiguration driftConfig = driftConfiguration("test", server.getPath());

        final File changeSetDir = new File(changeSetsDir, "1-test");
        changeSetDir.mkdir();

        // Note that absence of meta data is not sufficient for determining that
        // no change sets have previously been generated. The user could have just
        // deleted the data directory. We need to track what the current change set
        // is. That probably needs to hang off of the resource so it is obtained
        // during inventory sync when the agent starts up.

        final DriftDetectionSchedule schedule = new DriftDetectionSchedule(1, driftConfig);

        context.checking(new Expectations() {{
            allowing(queue).nextSchedule(); will(returnValue(schedule));

            allowing(queue).offer(with(any(DriftDetectionSchedule.class)));

            allowing(changeSetMgr).getChangeSetReader(schedule.getResourceId(), schedule.getDriftConfiguration());
            will(returnValue(new DriftDetectionSchedule(1, driftConfig)));

            allowing(changeSetMgr).getChangeSetWriter(1, schedule.getDriftConfiguration());
            will(returnValue(new ChangeSetWriterImpl(changeSetDir, "test")));
        }});

        DriftDetector driftDetector = new DriftDetector();
        driftDetector.setScheduleQueue(queue);
        driftDetector.setChangeSetManager(changeSetMgr);

        driftDetector.run();
    }

    DriftConfiguration driftConfiguration(String name, String basedir) {
        Configuration config = new Configuration();
        config.put(new PropertySimple("name", name));
        config.put(new PropertySimple("basedir", basedir));

        return new DriftConfiguration(config);
    }

}
