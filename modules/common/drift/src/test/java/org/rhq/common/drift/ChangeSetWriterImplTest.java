package org.rhq.common.drift;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfiguration;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.IOUtils.lineIterator;
import static org.apache.commons.io.IOUtils.readLines;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ChangeSetWriterImplTest {

    File changeSetsDir = new File("target", "changesets");

    @BeforeClass
    public void setupChangesetsDir() throws Exception {
        deleteDirectory(changeSetsDir);
        assertTrue(changeSetsDir.mkdirs(), "Failed to create " + changeSetsDir.getPath());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeDirectoryEntryWithAddedFile() throws Exception {
        File changeSetFile = new File(changeSetsDir, "added-file-test");
        DriftConfiguration driftConfig = driftConfiguration("added-file-test", "myresource");
        boolean coverageChangeSet = true;

        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changeSetFile, driftConfig, coverageChangeSet);

        writer.writeDirectoryEntry(new DirectoryEntry("myresource/conf").add(
            addedFileEntry("myconf.conf", "a34ef6")));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 6, "Expected to find six lines in " + metaDataFile.getPath() +
            " - three for the header followed by three for a directory entry.");

        assertHeaderEquals(lines, "added-file-test", "myresource", "true");

        assertEquals(lines.get(3), "myresource/conf 1", "The first line for a directory entry should specify the " +
            "directory path followed by the number of lines in the entry.");

        assertFileEntryEquals(lines.get(4), "a34ef6 0 myconf.conf A");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeDirectoryEntryWithRemovedFile() throws Exception {
        File changeSetFile = new File(changeSetsDir, "removed-file-test");
        DriftConfiguration driftConfig = driftConfiguration("removed-file-test", "myresource");
        boolean coverageChangeSet = true;

        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changeSetFile, driftConfig, coverageChangeSet);

        writer.writeDirectoryEntry(new DirectoryEntry("myresource/conf").add(
            removedFileEntry("myconf.conf", "a34ef6")));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 6, "Expected to find six lines in " + metaDataFile.getPath() +
            " - three for the header followed by three for a directory entry.");

        assertHeaderEquals(lines, "removed-file-test", "myresource", "true");

        assertEquals(lines.get(3), "myresource/conf 1", "The first line for a directory entry should specify the " +
            "directory path followed by the number of lines in the entry.");

        assertFileEntryEquals(lines.get(4), "0 a34ef6 myconf.conf R");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeDirectoryEntryWithChangedFile() throws Exception {
        File changeSetFile = new File(changeSetsDir, "changed-file-test");
        DriftConfiguration driftConfig = driftConfiguration("changed-file-test", "myresource");
        boolean coverageChangeSet = true;

        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changeSetFile, driftConfig, coverageChangeSet);

        writer.writeDirectoryEntry(new DirectoryEntry("myresource/conf").add(
            changedFileEntry("myconf.conf", "a34ef6", "c2d55f")));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 6, "Expected to find six lines in " + metaDataFile.getPath() +
            " - three for the header followed by three for a directory entry.");

        assertHeaderEquals(lines, "changed-file-test", "myresource", "true");

        assertEquals(lines.get(3), "myresource/conf 1", "The first line for a directory entry should specify the " +
            "directory path followed by the number of lines in the entry.");

        assertFileEntryEquals(lines.get(4), "c2d55f a34ef6 myconf.conf C");
    }

    /**
     * Verifies that <code>lines</code>, which is assumed to represent the entire changeset
     * file, contains the expected header. The header contains three entries which are
     * written out one entry per line. The header is the first three lines of the changeset
     * file and contains the following entries:
     *
     * <ul>
     *   <li>drift configuration name</li>
     *   <li>drift configuration base directory</li>
     *   <li>boolean flag indicating whether or not this is an initial changeset</li>
     * </ul>
     * @param lines
     * @param expected
     */
    void assertHeaderEquals(List<String> lines, String... expected) {
        assertEquals(lines.get(0), expected[0], "The first header entry should be the drift configuration name.");
        assertEquals(lines.get(1), expected[1], "The second header entry should be the base directory.");
        assertEquals(lines.get(2), expected[2], "The second header entry should be a flag indicating whether or not " +
            "this is a coverage change set");
    }

    /**
     * Verifies that a file entry matches an expected value. A file entry consists of
     * four, space-delimited fields and is terminated by a newline character. Those fields
     * are new_sha, old_sha, file_name, type.
     *
     * @param actual
     * @param expected
     */
    void assertFileEntryEquals(String actual, String expected) {
        String[] expectedFields = expected.split(" ");
        String[] actualFields = actual.split(" ");

        assertEquals(4, expectedFields.length, "<" + expected + "> should contain 4 fields");
        assertEquals(4, actualFields.length, "<" + expected + "> should contain 4 fields");

        assertEquals(actualFields[0], expectedFields[0], "The first column, the SHA-256, is wrong");
        assertEquals(actualFields[1], expectedFields[1], "The second column, the old SHA-256, is wrong");
        assertEquals(actualFields[2], expectedFields[2], "The third column, the file name, is wrong");
        assertEquals(actualFields[3], expectedFields[3], "The fourth column, the type, is wrong");
    }

    DriftConfiguration driftConfiguration(String name, String basedir) {
        Configuration config = new Configuration();
        config.put(new PropertySimple("name", name));
        config.put(new PropertySimple("basedir", basedir));

        return new DriftConfiguration(config);
    }

}
