package org.rhq.common.drift;

import java.io.StringReader;

import org.testng.annotations.Test;

import org.rhq.core.domain.drift.DriftChangeSetCategory;

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.testng.Assert.*;

public class ChangeSetReaderImplTest {
    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileAdded() throws Exception {
        String changeset = "file-added-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "myresource/conf 1\n" +
                           "a34ef6 0 myconf.conf A";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers("file-added-test", "myresource", COVERAGE);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertDirectoryEntryEquals(dirEntry, "myresource/conf", 1);

        FileEntry actualFileEntry = dirEntry.iterator().next();
        FileEntry expectedFileEntry = new FileEntry("a34ef6", "0", "myconf.conf", "A");

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileRemoved() throws Exception {
        String changeset = "file-removed-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "myresource/conf 1\n" +
                           "0 a34ef6 myconf.conf R";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers("file-removed-test", "myresource", COVERAGE);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertDirectoryEntryEquals(dirEntry, "myresource/conf", 1);

        FileEntry actualFileEntry = dirEntry.iterator().next();
        FileEntry expectedFileEntry = new FileEntry("0", "a34ef6", "myconf.conf", "R");

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileChanged() throws Exception {
        String changeset = "file-changed-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "myresource/conf 1\n" +
                           "a34ef6 c41b8 myconf.conf C";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers("file-changed-test", "myresource", COVERAGE);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertDirectoryEntryEquals(dirEntry, "myresource/conf", 1);

        FileEntry actualFileEntry = dirEntry.iterator().next();
        FileEntry expectedFileEntry = new FileEntry("a34ef6", "c41b8", "myconf.conf", "C");

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    void assertHeadersEquals(Headers actual, Headers expected) {
        assertEquals(actual.getDriftConfigurationName(), expected.getDriftConfigurationName(),
            "The drift configuration name, which should be the first header, is wrong.");
        assertEquals(actual.getBasedir(), expected.getBasedir(), "The drift configuration base directory, which " +
            "should be the second header, is wrong.");
        assertEquals(actual.getType(), expected.getType(), "The change set type flag, " +
            "which should be the third header, is wrong.");
    }

    /**
     * Verifies that the directory entry name (i.e., path) and number of files match the
     * specified values.
     *
     * @param dirEntry
     * @param path
     * @param numberOfFiles
     */
    void assertDirectoryEntryEquals(DirectoryEntry dirEntry, String path, int numberOfFiles) {
        assertNotNull(dirEntry, "Expected non-null directory entry");
        assertEquals(dirEntry.getDirectory(), path, "The directory name/path is wrong");
        assertEquals(dirEntry.getNumberOfFiles(), numberOfFiles, "The number of files for the directory entry is " +
            "wrong");
    }

    void assertFileEntryEquals(FileEntry actual, FileEntry expected) {
        assertEquals(actual.getNewSHA(), expected.getNewSHA(), "The first column, the new SHA-256, is wrong");
        assertEquals(actual.getOldSHA(), expected.getOldSHA(), "The second column, the old SHA-256, is wrong");
        assertEquals(actual.getFile(), expected.getFile(), "The third column, the file name, is wrong");
        assertEquals(actual.getType(), expected.getType(), "The fourth column, the entry type, is wrong");
    }

}
