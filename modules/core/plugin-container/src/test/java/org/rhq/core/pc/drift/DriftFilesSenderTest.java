package org.rhq.core.pc.drift;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftFile;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.io.IOUtils.writeLines;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DriftFilesSenderTest extends DriftTest {

    DriftClient driftClient;

    DriftFilesSender sender;

    @BeforeMethod
    public void initSender() {
        driftClient = new DriftClientTestStub();

        sender = new DriftFilesSender();
        sender.setDriftClient(driftClient);
        sender.setChangeSetManager(changeSetMgr);
        sender.setResourceId(resourceId());
    }

    @Test
    public void sendFilesInOneDirectory() throws Exception {
        String driftConfigName = "single-directory-test";

        File confDir = mkdir(resourceDir, "conf");
        touch(new File(confDir, "server-1.conf"));
        touch(new File(confDir, "server-2.conf"));

        File changeSetDir = changeSetDir(driftConfigName);

        // Each item in changeSet is listed by the line it will appear in the actual file
        // in an attemp to make it more self-documenting.
        writeChangeSet(changeSetDir,
            driftConfigName,
            resourceDir.getAbsolutePath(),
            COVERAGE.code(),
            "conf 2",
            "2e345df 0 server-1.conf A",
            "a5d8c3e 0 server-2.conf A",
            ""
        );

        sender.setDriftFiles(driftFiles("2e345df", "a5d8c3e"));
        sender.setHeaders(new Headers(driftConfigName, resourceDir.getAbsolutePath(), COVERAGE));
        sender.run();

        File contentDir = mkdir(changeSetDir, "content");

        assertEquals(contentDir.list().length, 2, "Expected to find two files in " + contentDir.getAbsolutePath());
        assertFileCopiedToContentDir(contentDir, "2e345df");
        assertFileCopiedToContentDir(contentDir, "a5d8c3e");
    }

    @Test
    public void sendFilesInMultipleDirectories() throws Exception {
        String driftConfigName = "multiple-directories-test";

        File confDir = mkdir(resourceDir, "conf");
        File libDir = mkdir(resourceDir, "lib");
        touch(new File(confDir, "server-1.conf"));
        touch(new File(confDir, "server-2.conf"));
        touch(new File(libDir, "server-1.jar"));
        touch(new File(libDir, "server-2.jar"));

        File changeSetDir = changeSetDir(driftConfigName);

        // Each item in changeSet is listed by the line it will appear in the actual file
        // in an attemp to make it more self-documenting.
        writeChangeSet(changeSetDir,
            driftConfigName,
            resourceDir.getAbsolutePath(),
            COVERAGE.code(),
            "conf 2",
            "2e345df 0 server-1.conf A",
            "a5d8c3e 0 server-2.conf A",
            "",
            "lib 2",
            "91d4abb 0 server-1.jar A",
            "92c4abb 0 server-2.jar A",
            ""
        );

        // Note that the order of the drift files is random. When the server sends a request
        // for files we cannot assume that the files will be in any particular order.
        sender.setDriftFiles(driftFiles("2e345df", "91d4abb", "a5d8c3e", "92c4abb"));
        sender.setHeaders(new Headers(driftConfigName, resourceDir.getAbsolutePath(), COVERAGE));
        sender.run();

        File contentDir = mkdir(changeSetDir, "content");

        assertEquals(contentDir.list().length, 4, "Expected to find four files in " + contentDir.getAbsolutePath());
        assertFileCopiedToContentDir(contentDir, "2e345df");
        assertFileCopiedToContentDir(contentDir, "a5d8c3e");
        assertFileCopiedToContentDir(contentDir, "91d4abb");
        assertFileCopiedToContentDir(contentDir, "92c4abb");
    }

    /**
     * This method only verifies that a file having a particular hash or SHA-256 has been
     * copied to the specified content directory.
     *
     * @param contentDir The directory to which the file should have been copied
     *
     * @param fileHash The file hash which is expected to be the name of the file in the
     * content directory.
     */
    void assertFileCopiedToContentDir(File contentDir, String fileHash) {
        File file = new File(contentDir, fileHash);
        assertTrue(file.exists(), "Expected to find file named " + file.getName() + " in content directory " +
            contentDir.getPath() + ". The SHA-256 hash should be used as the file name.");
    }

    List<DriftFile> driftFiles(String... hashes) {
        List<DriftFile> files = new ArrayList<DriftFile>();
        for (String hash : hashes) {
            files.add(new DriftFile(hash));
        }
        return files;
    }
}
