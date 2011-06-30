package org.rhq.core.pc.drift;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftFile;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.io.IOUtils.writeLines;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DriftFilesSenderTest {

    File changeSetsDir;

    File resourcesDir;

    ChangeSetManager changeSetMgr;

    String driftConfigName = "test";

    File server1Dir;

    @BeforeClass
    public void init() throws Exception {
        File basedir = new File("target", getClass().getSimpleName());
        deleteDirectory(basedir);
        basedir.mkdir();

        changeSetsDir = mkdir(basedir, "changesets");
        resourcesDir = mkdir(basedir, "resources");
    }

    @BeforeMethod
    public void setUp() {
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
    }

    @Test
    public void sendFilesInOneDirectory() throws Exception {
        String driftConfigName = "single-directory-test";

        File server1Dir = mkdir(resourcesDir, "server-1");
        File confDir = mkdir(server1Dir, "conf");
        touch(new File(confDir, "server-1.conf"));
        touch(new File(confDir, "server-2.conf"));

        int resourceId = 1;
        File changeSetDir = createChangeSetDir(resourceId, driftConfigName);

        // Each item in changeSet is listed by the line it will appear in the actual file
        // in an attemp to make it more self-documenting.
        writeChangeSet(changeSetDir, asList(
            driftConfigName,
            server1Dir.getAbsolutePath(),
            COVERAGE.code(),
            "conf 2",
            "2e345df 0 server-1.conf A",
            "a5d8c3e 0 server-2.conf A",
            ""
        ));

        DriftFilesSender sender = new DriftFilesSender();
        sender.setDriftClient(new DriftClientTestStub());
        sender.setChangeSetManager(changeSetMgr);
        sender.setResourceId(resourceId);
        sender.setDriftFiles(driftFiles("2e345df", "a5d8c3e"));
        sender.setHeaders(new Headers(driftConfigName, server1Dir.getAbsolutePath(), COVERAGE));
        sender.run();

        File contentDir = mkdir(changeSetDir, "content");

        assertEquals(contentDir.list().length, 2, "Expected to find two files in " + contentDir.getAbsolutePath());
        assertFileCopiedToContentDir(contentDir, "2e345df");
        assertFileCopiedToContentDir(contentDir, "a5d8c3e");
    }

    @Test
    public void sendFilesInMultipleDirectories() throws Exception {
        String driftConfigName = "multiple-directories-test";

        File server2Dir = mkdir(resourcesDir, "server-2");
        File confDir = mkdir(server2Dir, "conf");
        File libDir = mkdir(server2Dir, "lib");
        touch(new File(confDir, "server-1.conf"));
        touch(new File(confDir, "server-2.conf"));
        touch(new File(libDir, "server-1.jar"));
        touch(new File(libDir, "server-2.jar"));

        int resourceId = 2;
        File changeSetDir = createChangeSetDir(resourceId, driftConfigName);

        // Each item in changeSet is listed by the line it will appear in the actual file
        // in an attemp to make it more self-documenting.
        writeChangeSet(changeSetDir, asList(
            driftConfigName,
            server2Dir.getAbsolutePath(),
            COVERAGE.code(),
            "conf 2",
            "2e345df 0 server-1.conf A",
            "a5d8c3e 0 server-2.conf A",
            "",
            "lib 2",
            "91d4abb 0 server-1.jar A",
            "92c4abb 0 server-2.jar A",
            ""
        ));

        DriftFilesSender sender = new DriftFilesSender();
        sender.setDriftClient(new DriftClientTestStub());
        sender.setChangeSetManager(changeSetMgr);
        sender.setResourceId(resourceId);
        // Note that the order of the drift files is random. When the server sends a request
        // for files we cannot assume that the files will be in any particular order.
        sender.setDriftFiles(driftFiles("2e345df", "91d4abb", "a5d8c3e", "92c4abb"));
        sender.setHeaders(new Headers(driftConfigName, server2Dir.getAbsolutePath(), COVERAGE));
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

    File mkdir(File parent, String name) {
        File dir = new File(parent, name);
        dir.mkdirs();
        return dir;
    }

    File createChangeSetDir(int resourceId, String driftConfigName) {
        File dir = new File(new File(changeSetsDir, Integer.toString(resourceId)), driftConfigName);
        dir.mkdirs();
        return dir;
    }

    void writeChangeSet(File changeSetDir, List<String> changeSet) throws Exception {
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(changeSetDir,
            "changeset.txt")));
        writeLines(changeSet, "\n", stream);
        stream.close();
    }

    List<DriftFile> driftFiles(String... hashes) {
        List<DriftFile> files = new ArrayList<DriftFile>();
        for (String hash : hashes) {
            files.add(new DriftFile(hash));
        }
        return files;
    }

}
