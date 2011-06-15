package org.rhq.common.drift;

import java.io.StringReader;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ChangeSetReaderImplTest {
    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileAdded() throws Exception {
        String changeset = "myresource/conf 1\n" +
                           "a34ef6 0 myconf.conf A";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertNotNull(dirEntry, "Expected reader to return a directory entry");
        assertEquals(dirEntry.getDirectory(), "myresource/conf", "The directory name is wrong");
        assertEquals(dirEntry.getNumberOfFiles(), 1, "The number of files for the directory entry is wrong");

        FileEntry fileEntry = dirEntry.iterator().next();

        assertEquals(fileEntry.getNewSHA(), "a34ef6", "The first column, the new SHA-256, is wrong");
        assertEquals(fileEntry.getOldSHA(), "0", "The second column, the old SHA-256, is wrong");
        assertEquals(fileEntry.getFile(), "myconf.conf", "The third column, the file name, is wrong");
        assertEquals(fileEntry.getType().code(), "A", "The fourth column, the entry type, is wrong");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileRemoved() throws Exception {
        String changeset = "myresource/conf 1\n" +
                           "0 a34ef6 myconf.conf R";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertNotNull(dirEntry, "Expected reader to return a directory entry");
        assertEquals(dirEntry.getDirectory(), "myresource/conf", "The directory name is wrong");
        assertEquals(dirEntry.getNumberOfFiles(), 1, "The number of files for the directory entry is wrong");

        FileEntry fileEntry = dirEntry.iterator().next();

        assertEquals(fileEntry.getNewSHA(), "0", "The first column, the new SHA-256, is wrong");
        assertEquals(fileEntry.getOldSHA(), "a34ef6", "The second column, the old SHA-256, is wrong");
        assertEquals(fileEntry.getFile(), "myconf.conf", "The third column, the file name, is wrong");
        assertEquals(fileEntry.getType().code(), "R", "The fourth column, the entry type, is wrong");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readDirectoryEntryWithFileChanged() throws Exception {
        String changeset = "myresource/conf 1\n" +
                           "a34ef6 c41b8 myconf.conf C";

        ChangeSetReaderImpl reader = new ChangeSetReaderImpl(new StringReader(changeset));
        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        reader.close();

        assertNotNull(dirEntry, "Expected reader to return a directory entry");
        assertEquals(dirEntry.getDirectory(), "myresource/conf", "The directory name is wrong");
        assertEquals(dirEntry.getNumberOfFiles(), 1, "The number of files for the directory entry is wrong");

        FileEntry fileEntry = dirEntry.iterator().next();

        assertEquals(fileEntry.getNewSHA(), "a34ef6", "The first column, the new SHA-256, is wrong");
        assertEquals(fileEntry.getOldSHA(), "c41b8", "The second column, the old SHA-256, is wrong");
        assertEquals(fileEntry.getFile(), "myconf.conf", "The third column, the file name, is wrong");
        assertEquals(fileEntry.getType().code(), "C", "The fourth column, the entry type, is wrong");
    }

}
