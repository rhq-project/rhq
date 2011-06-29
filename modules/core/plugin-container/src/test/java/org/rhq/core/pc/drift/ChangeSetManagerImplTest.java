package org.rhq.core.pc.drift;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfiguration;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.writeLines;
import static org.testng.Assert.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;

public class ChangeSetManagerImplTest {

    File changeSetsDir;

    @BeforeClass
    public void resetChangeSetsDir() throws Exception {
        File dataDir = new File("target", getClass().getSimpleName());
        deleteDirectory(dataDir);

        changeSetsDir = new File(dataDir, "changesets");
        assertTrue(changeSetsDir.mkdirs(), "Failed to create " + changeSetsDir.getAbsolutePath());
        changeSetsDir = new File(dataDir, "changesets");
    }

    @Test
    public void returnNullReaderWhenNoChangeSetExists() throws Exception {
        int resourceId = -1;
        ChangeSetManager changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId, "test");

        assertNull(reader, "Expect null for the reader when no change set exists for the drift configuration.");
    }

    @Test
    public void returnReaderForRequestedChangeSet() throws Exception {
        int resourceId = 1;

        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, "test-1");

        assertTrue(changeSetDir.mkdirs(), "Failed to create change set directory: " + changeSetDir.getAbsolutePath());

        List<String> changeSet = asList(
            "test-1",
            "server",
            "D",
            "server/conf 1",
            "8f26ac3d 0 myconf.conf A"
        );
        writeLines(new File(changeSetDir, "changeset.txt"), changeSet);

        ChangeSetManager changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId, "test-1");

        assertNotNull(reader, "Expected to get a change set reader when change set exists");
        assertReaderOpenedOnChangeSet(reader, asList("server/conf", "1"));
    }

    /**
     * Verifies that a {@link ChangeSetReader} has been opened on the expected change set.
     * This method first verifies that the reader is not null. It then reads the first
     * {@link DirectoryEntry} from the reader and verifies that the directory and
     * numberOfFiles properties match the expected values specified in dirEntry.
     * <p/>
     * This method does not rigorously check the entire contents of the change set file
     * because that {@link ChangeSetReader} tests; rather, it aims to inspect just enough
     * info to verify that the reader is opened on the correct change set.
     *
     * @param reader The ChangeSetReader returned from the ChangeSetManager under test
     * @param dirEntry A list of strings representing the first line of a directory entry.
     * The list should consist of two elements. The first being the directory path and the
     * second being the number of files in the entry.
     * @throws Exception
     */
    void assertReaderOpenedOnChangeSet(ChangeSetReader reader, List<String> dirEntry) throws Exception {
        assertNotNull(reader, "The " + ChangeSetReader.class.getSimpleName() + " should not be null.");

        DirectoryEntry actual = reader.readDirectoryEntry();
        assertNotNull(actual, "Expected to find a directory entry");
        assertEquals(actual.getDirectory(), dirEntry.get(0), "The directory entry path is wrong");
        assertEquals(Integer.toString(actual.getNumberOfFiles()), dirEntry.get(1),
            "The number of files for the directory entry is wrong");
    }

}
