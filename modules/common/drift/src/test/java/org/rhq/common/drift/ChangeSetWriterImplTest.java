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

        File metaDataFile = writer.getMetaDataFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 3, "Expected to find two lines in " + metaDataFile.getPath());
        assertEquals(lines.get(0), "myresource/conf 1", "The first line for a directory entry should specify the " +
            "directory path followed by the number of lines in the entry.");

        assertFileEntryEquals(lines.get(1), "a34ef6 0 myconf.conf A");
    }

//    @Test
//    @SuppressWarnings("unchecked")
//    public void writeOneDirectoryEntry() throws Exception {
//        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changesetsDir, "test-1");
//        writer.writeDirectoryEntry(new DirectoryEntry("myresource/conf").add("myconf1.conf").add("myconf2.xml"));
//        writer.close();
//
//        File metaDataFile = writer.getMetaDataFile();
//        List<String> lines = readLines(new FileInputStream(metaDataFile));
//
//        assertEquals(lines.size(), 4, "Expected to find three lines in " + metaDataFile.getPath());
//
//        assertEquals(lines.get(0), "myresource/conf 2", "The first line for a directory entry should specify the " +
//            "directory path followed by the number of files in the entry.");
//        assertEquals(lines.get(1), "myconf1.conf", "The file entry is wrong");
//        assertEquals(lines.get(2), "myconf2.xml", "The file entry is wrong");
//        assertEquals(lines.get(3), "", "A directory entry should be followed by a blank line, i.e., \"\\n\"");
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    public void writeMultipleDirectoryEntries() throws Exception {
//        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changesetsDir, "test-2");
//
//        writer.writeDirectoryEntry(new DirectoryEntry("myresource/conf").add("myconf.conf"));
//        writer.writeDirectoryEntry(new DirectoryEntry("myresource/lib").add("mylib.jar"));
//        writer.close();
//
//        File metaDataFile = writer.getMetaDataFile();
//        List<String> lines = readLines(new FileInputStream(metaDataFile));
//
//        assertEquals(lines.size(), 6, "Expected to find six lines in " + metaDataFile.getPath());
//        assertEquals(lines.get(0), "myresource/conf 1", "The first line for a directory entry should specify the " +
//            "directory path followed by the number of files in the entry.");
//        assertEquals(lines.get(1), "myconf.conf", "The file entry is wrong");
//        assertEquals(lines.get(2), "", "A directory entry should be followed by a blank line, i.e., \"\\n\"");
//        assertEquals(lines.get(3), "myresource/lib 1", "The first line for a directory entry should specify the " +
//            "directory path followed by the number of files in the entry.");
//        assertEquals(lines.get(4), "mylib.jar", "The file entry is wrong");
//        assertEquals(lines.get(5), "", "A directory entry should be followed by a blank line, i.e., \"\\n\"");
//    }

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
