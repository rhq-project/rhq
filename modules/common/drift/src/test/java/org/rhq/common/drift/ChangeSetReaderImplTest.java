package org.rhq.common.drift;

import java.io.StringReader;

import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ChangeSetReaderImplTest {

    MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileAdded() throws Exception {
        String sha = sha256("myconf.conf");
        String changeset = "1\n" +
                           "1\n" +
                           "file-added-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1 myresource/conf\n" +
                           "A " + sha + " 0 myconf.conf";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers();
        expectedHeaders.setResourceId(1);
        expectedHeaders.setDriftCofigurationId(1);
        expectedHeaders.setDriftConfigurationName("file-added-test");
        expectedHeaders.setBasedir("myresource");
        expectedHeaders.setType(COVERAGE);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertDirectoryEntryEquals(dirEntry, "myresource/conf", 1);

        FileEntry actualFileEntry = dirEntry.iterator().next();
        FileEntry expectedFileEntry = new FileEntry(sha, "0", "myconf.conf", "A");

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileRemoved() throws Exception {
        String sha = sha256("myconf.conf");
        String changeset = "1\n" +
                           "1\n" +
                           "file-removed-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1 myresource/conf\n" +
                           "R 0 " + sha + " myconf.conf";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers();
        expectedHeaders.setResourceId(1);
        expectedHeaders.setDriftCofigurationId(1);
        expectedHeaders.setDriftConfigurationName("file-removed-test");
        expectedHeaders.setBasedir("myresource");
        expectedHeaders.setType(COVERAGE);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertDirectoryEntryEquals(dirEntry, "myresource/conf", 1);

        FileEntry actualFileEntry = dirEntry.iterator().next();
        FileEntry expectedFileEntry = new FileEntry("0", sha, "myconf.conf", "R");

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileChanged() throws Exception {
        String oldSha = sha256("myconf.conf.old");
        String newSha = sha256("myconf.conf.new");
        String changeset = "1\n" +
                           "1\n" +
                           "file-changed-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1 myresource/conf\n" +
                           "C " + newSha + " " + oldSha + " myconf.conf";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers();
        expectedHeaders.setResourceId(1);
        expectedHeaders.setDriftCofigurationId(1);
        expectedHeaders.setDriftConfigurationName("file-changed-test");
        expectedHeaders.setBasedir("myresource");
        expectedHeaders.setType(COVERAGE);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertDirectoryEntryEquals(dirEntry, "myresource/conf", 1);

        FileEntry actualFileEntry = dirEntry.iterator().next();
        FileEntry expectedFileEntry = new FileEntry(newSha, oldSha, "myconf.conf", "C");

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readEntryWithAddedFileThatHasSpaces() throws Exception {
        String sha = sha256("file with spaces.conf");
        String changeset = "1\n" +
                           "1\n" +
                           "file-name-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1 myresource/conf\n" +
                           "A " + sha + " 0 file with spaces.conf";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers();
        expectedHeaders.setResourceId(1);
        expectedHeaders.setDriftCofigurationId(1);
        expectedHeaders.setDriftConfigurationName("file-name-test");
        expectedHeaders.setBasedir("myresource");
        expectedHeaders.setType(COVERAGE);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertDirectoryEntryEquals(dirEntry, "myresource/conf", 1);

        FileEntry actualFileEntry = dirEntry.iterator().next();
        FileEntry expectedFileEntry = new FileEntry(sha, "0", "file with spaces.conf", "A");

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    ////////////////////////////////////////////////////////
    //              Iterator tests                        //
    ////////////////////////////////////////////////////////

    // For iterator related tests we are only verifying the number of directory entries
    // returned to make sure that the iteration logic is correct. The actual parsing is
    // verified in other tests.
    //
    // jsanda

    @Test
    @SuppressWarnings("unchecked")
    public void iterateOverChangeSetWithMultipledDirectoryEntries() throws Exception {
        String changeset = "1\n" +
                           "1\n" +
                           "multiple-dir-entries-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1 conf\n" +
                           "A " + sha256("resource.conf") +  " 0 resource.conf\n" +
                           "1 lib\n" +
                           "A " + sha256("resource.jar") + " 0 resource.jar";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        int numDirEntries = 0;

        for (DirectoryEntry dirEntry : reader) {
            ++numDirEntries;
        }

        assertEquals(numDirEntries, 2, "Expected iterator to return two directory entries");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void iterateOverChangeSetOneDirectoryEntry() throws Exception {
        String sha = sha256("resource.conf");
        String changeset = "1\n" +
                           "1\n" +
                           "single-dir-entry-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1 conf\n" +
                           "A " + sha + " 0 resource.conf\n";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        int numDirEntries = 0;

        for (DirectoryEntry dirEntry : reader) {
            ++numDirEntries;
        }

        assertEquals(numDirEntries, 1, "Expected iterator to return one directory entry");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void iterateOverEmptyChangeSet() throws Exception {
        // Note: We shouldn't ever be working with empty change set files (excluding the
        // header) but this test is here to make sure ChangeReaderImpl is robust in handling
        // edge cases.

        String changeset = "1\n" +
                           "1\n" +
                           "empty-changeset-test\n" +
                           "myresouce\n" +
                           "C\n";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        int numDirEntries = 0;

        for (DirectoryEntry dirEntry : reader) {
            ++numDirEntries;
        }

        assertEquals(numDirEntries, 0, "Expected iterator to return zero directory entries");
    }

    void assertHeadersEquals(Headers actual, Headers expected) {
        assertPropertiesMatch(expected, actual, "Failed to parse change set headers");
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
        assertEquals(actual.getType(), expected.getType(), "The first column, the entry type, is wrong");
        assertEquals(actual.getNewSHA(), expected.getNewSHA(), "The second column, the new SHA-256, is wrong");
        assertEquals(actual.getOldSHA(), expected.getOldSHA(), "The third column, the old SHA-256, is wrong");
        assertEquals(actual.getFile(), expected.getFile(), "The fourth column, the file name, is wrong");
    }

    String sha256(String s) throws Exception {
        return digestGenerator.calcDigestString(s);
    }

}
