package org.rhq.core.pc.drift;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.writeLines;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.testng.Assert.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;

public class ChangeSetManagerImplTest extends DriftTest {

    @Test
    public void returnNullReaderWhenNoChangeSetExists() throws Exception {
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId(), "test");
        assertNull(reader, "Expect null for the reader when no change set exists for the drift configuration.");
    }

    @Test
    public void returnReaderForRequestedChangeSet() throws Exception {
        String config = "return-reader-for-existing-changeset-test";
        File changeSetDir = changeSetDir(config);

        writeChangeSet(changeSetDir,
            config,
            "server",
            "D",
            "server/conf 1",
            "8f26ac3d 0 myconf.conf A"
        );

        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId(), config);

        assertNotNull(reader, "Expected to get a change set reader when change set exists");
        assertReaderOpenedOnChangeSet(reader, asList("server/conf", "1"));
    }

    @Test
    public void verifyChangeSetExists() throws Exception {
        String config = "changeset-exists-test";
        File changeSetDir = changeSetDir(config);

        writeChangeSet(changeSetDir,
            config,
            "server",
            "D",
            "server/conf 1",
            "8f26ac3d 0 myconf.conf A"
        );

        assertTrue(changeSetMgr.changeSetExists(resourceId(), new Headers(config, resourceDir.getAbsolutePath(),
            DRIFT)), "Expected to find change set file.");
    }

    @Test
    public void verifyChangeSetDoesNotExist() throws Exception {
        String config = "changeset-does-not-exist";
        assertFalse(changeSetMgr.changeSetExists(resourceId(), new Headers(config, resourceDir.getAbsolutePath(),
            DRIFT)), "Did not expect to find change set file.");
    }

    /**
     * Verifies that a {@link ChangeSetReader} has been opened on the expected change set.
     * This method first verifies that the reader is not null. It then reads the first
     * {@link DirectoryEntry} from the reader and verifies that the directory and
     * numberOfFiles properties match the expected values specified in dirEntry.
     * <p/>
     * This method does not rigorously check the entire contents of the change set file
     * because that is handled by {@link ChangeSetReader} tests; rather, it aims to inspect
     * just enough info to verify that the reader is opened on the correct change set.
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
