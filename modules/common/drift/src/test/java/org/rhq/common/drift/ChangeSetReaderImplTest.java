package org.rhq.common.drift;

import java.io.StringReader;

import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;

public class ChangeSetReaderImplTest {

    MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    @Test
    @SuppressWarnings("unchecked")
    public void readFileAddedEntry() throws Exception {
        String sha = sha256("myconf.conf");
        String changeset = "1\n" +
                           "1\n" +
                           "file-added-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1\n" +
                           "A " + sha + " 0 conf/myconf.conf";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers();
        expectedHeaders.setResourceId(1);
        expectedHeaders.setDriftDefinitionId(1);
        expectedHeaders.setDriftDefinitionName("file-added-test");
        expectedHeaders.setBasedir("myresource");
        expectedHeaders.setType(COVERAGE);
        expectedHeaders.setVersion(1);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        FileEntry expectedFileEntry = new FileEntry(sha, "0", "conf/myconf.conf", "A");
        FileEntry actualFileEntry = reader.read();
        reader.close();

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readFileRemovedEntry() throws Exception {
        String sha = sha256("myconf.conf");
        String changeset = "1\n" +
                           "1\n" +
                           "file-removed-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1\n" +
                           "R 0 " + sha + " conf/myconf.conf";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers();
        expectedHeaders.setResourceId(1);
        expectedHeaders.setDriftDefinitionId(1);
        expectedHeaders.setDriftDefinitionName("file-removed-test");
        expectedHeaders.setBasedir("myresource");
        expectedHeaders.setType(COVERAGE);
        expectedHeaders.setVersion(1);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        FileEntry expectedFileEntry = new FileEntry("0", sha, "conf/myconf.conf", "R");
        FileEntry actualFileEntry = reader.read();
        reader.close();

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readChangedFileEntry() throws Exception {
        String oldSha = sha256("myconf.conf.old");
        String newSha = sha256("myconf.conf.new");
        String changeset = "1\n" +
                           "1\n" +
                           "file-changed-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1\n" +
                           "C " + newSha + " " + oldSha + " conf/myconf.conf";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers();
        expectedHeaders.setResourceId(1);
        expectedHeaders.setDriftDefinitionId(1);
        expectedHeaders.setDriftDefinitionName("file-changed-test");
        expectedHeaders.setBasedir("myresource");
        expectedHeaders.setType(COVERAGE);
        expectedHeaders.setVersion(1);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        FileEntry expectedFileEntry = new FileEntry(newSha, oldSha, "conf/myconf.conf", "C");
        FileEntry actualFileEntry = reader.read();
        reader.close();

        assertFileEntryEquals(actualFileEntry, expectedFileEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readFileEntryWithSpacesInPath() throws Exception {
        String sha = sha256("file with spaces.conf");
        String changeset = "1\n" +
                           "1\n" +
                           "file-name-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1\n" +
                           "A " + sha + " 0 conf/file with spaces.conf";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));

        Headers actualHeaders = reader.getHeaders();
        Headers expectedHeaders = new Headers();
        expectedHeaders.setResourceId(1);
        expectedHeaders.setDriftDefinitionId(1);
        expectedHeaders.setDriftDefinitionName("file-name-test");
        expectedHeaders.setBasedir("myresource");
        expectedHeaders.setType(COVERAGE);
        expectedHeaders.setVersion(1);

        assertHeadersEquals(actualHeaders, expectedHeaders);

        FileEntry expectedFileEntry = new FileEntry(sha, "0", "conf/file with spaces.conf", "A");
        FileEntry actualFileEntry = reader.read();
        reader.close();

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
    public void iterateOverChangeSetWithMultipleEntries() throws Exception {
        String changeset = "1\n" +
                           "1\n" +
                           "multiple-dir-entries-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1\n" +
                           "A " + sha256("resource.conf") +  " 0 conf/resource.conf\n" +
                           "A " + sha256("resource.jar") + " 0 lib/resource.jar";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        int numEntries = 0;

        for (FileEntry entry : reader) {
            ++numEntries;
        }

        assertEquals(numEntries, 2, "Expected iterator to return two file entries");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void iterateOverChangeSetWithOneEntry() throws Exception {
        String sha = sha256("resource.conf");
        String changeset = "1\n" +
                           "1\n" +
                           "single-dir-entry-test\n" +
                           "myresource\n" +
                           "C\n" +
                           "1\n" +
                           "A " + sha + " 0 conf/resource.conf\n";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        int numEntries = 0;

        for (FileEntry entry : reader) {
            ++numEntries;
        }

        assertEquals(numEntries, 1, "Expected iterator to return one file entry");
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
                           "C\n" +
                           "1\n";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        int numEntries = 0;

        for (FileEntry entry : reader) {
            ++numEntries;
        }

        assertEquals(numEntries, 0, "Expected iterator to return zero file entries");
    }

    void assertHeadersEquals(Headers actual, Headers expected) {
        assertPropertiesMatch(expected, actual, "Failed to parse change set headers");
    }

    void assertFileEntryEquals(FileEntry actual, FileEntry expected) {
        assertEquals(actual.getType(), expected.getType(), "The first column, the entry type, is wrong");
        assertEquals(actual.getNewSHA(), expected.getNewSHA(), "The second column, the new SHA-256, is wrong");
        assertEquals(actual.getOldSHA (), expected.getOldSHA(), "The third column, the old SHA-256, is wrong");
        assertEquals(actual.getFile(), expected.getFile(), "The fourth column, the file name, is wrong");
    }

    String sha256(String s) throws Exception {
        return digestGenerator.calcDigestString(s);
    }

}
