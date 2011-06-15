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

        assertNotNull(dirEntry, "Expected reader to return a directory entry");
        assertEquals(dirEntry.getDirectory(), "myresource/conf", "The directory name is wrong");
        assertEquals(dirEntry.getNumberOfFiles(), 1, "The number of files for the directory entry is wrong");

        FileEntry fileEntry = dirEntry.iterator().next();

        assertEquals("a34ef6", fileEntry.getNewSHA(), "The first column, the new SHA-256, is wrong ");
    }
}
