package org.rhq.core.pc.drift;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.Test;

import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.test.AssertUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.addAll;
import static org.apache.commons.io.FileUtils.touch;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;

public class DirectoryAnalyzerTest extends DriftTest {

    @SuppressWarnings("unchecked")
    @Test
    public void detectAddedFile() throws Exception {
        DriftConfiguration config = driftConfiguration("added-files-test", resourceDir.getAbsolutePath());
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

        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(resourceDir, new DirectoryEntry("conf")
            .add(addedFileEntry(server1Conf.getName(), sha256(server1Conf))));
        analyzer.run();

        assertCollectionMatchesNoOrder(asList(addedFileEntry("server-2.conf", sha256(server2Conf))),
            analyzer.getFilesAdded(), "Failed to detect added file.");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void detectRemovedFile() throws Exception {
        DriftConfiguration config = driftConfiguration("added-files-test", resourceDir.getAbsolutePath());
        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = new File(confDir, "server-1.conf");
        touch(server1Conf);
        File server2Conf = new File(confDir, "server-2.conf");
        touch(server2Conf);

        String server2ConfHash = sha256(server2Conf);

        File changeSetDir = changeSetDir(config.getName());

        // Generate the initial, coverage change set
        writeChangeSet(changeSetDir,
            config.getName(),
            resourceDir.getAbsolutePath(),
            COVERAGE.code(),
            "conf 1",
            sha256(server1Conf) + " 0 server-1.conf A",
            server2ConfHash + " 0 server-2.conf A",
            ""
        );

        // create some drift
        server2Conf.delete();

        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(resourceDir, new DirectoryEntry("conf")
            .add(addedFileEntry("server-1.conf", sha256(server1Conf)))
            .add(addedFileEntry("server-2.conf", server2ConfHash)));
        analyzer.run();

        assertCollectionMatchesNoOrder(asList(removedFileEntry("server-2.conf", server2ConfHash)),
            analyzer.getFilesRemoved(), "Failed to detect removed file.");
    }
}
