package org.rhq.common.drift;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.IOUtils.lineIterator;
import static org.apache.commons.io.IOUtils.readLines;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ChangeSetWriterImplTest {

    File changesetsDir = new File("target", "changesets");

    @BeforeClass
    public void setupChangesetsDir() throws Exception {
        deleteDirectory(changesetsDir);
        assertTrue(changesetsDir.mkdirs(), "Failed to create " + changesetsDir.getPath());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeDirectoryEntryWithAddedFile() throws Exception {
        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changesetsDir, "added-file-test");

        writer.writeDirectoryEntry(new DirectoryEntry("myresource/conf").add(
            addedFileEntry("myconf.conf", "a34ef6")));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 3, "Expected to find three lines in " + metaDataFile.getPath());
        assertEquals(lines.get(0), "myresource/conf 1", "The first line for a directory entry should specify the " +
            "directory path followed by the number of lines in the entry.");

        assertFileEntryEquals(lines.get(1), "a34ef6 0 myconf.conf A");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeDirectoryEntryWithRemovedFile() throws Exception {
        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changesetsDir, "removed-file-test");

        writer.writeDirectoryEntry(new DirectoryEntry("myresource/conf").add(
            removedFileEntry("myconf.conf", "a34ef6")));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 3, "Expected to find three lines in " + metaDataFile.getPath());
        assertEquals(lines.get(0), "myresource/conf 1", "The first line for a directory entry should specify the " +
            "directory path followed by the number of lines in the entry.");

        assertFileEntryEquals(lines.get(1), "0 a34ef6 myconf.conf R");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeDirectoryEntryWithChangedFile() throws Exception {
        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changesetsDir, "changed-file-test");

        writer.writeDirectoryEntry(new DirectoryEntry("myresource/conf").add(
            changedFileEntry("myconf.conf", "a34ef6", "c2d55f")));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 3, "Expected to find three lines in " + metaDataFile.getPath());
        assertEquals(lines.get(0), "myresource/conf 1", "The first line for a directory entry should specify the " +
            "directory path followed by the number of lines in the entry.");

        assertFileEntryEquals(lines.get(1), "c2d55f a34ef6 myconf.conf C");
    }

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

}
