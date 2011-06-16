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
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId, driftConfiguration("test", "server"));

        assertNull(reader, "Expect null for the reader when no change set exists for the drift configuration.");
    }

    @Test
    public void returnReaderForRequestedChangeSet() throws Exception {
        int resourceId = 1;
        DriftConfiguration drfitConfig = driftConfiguration("test-1", "server");

        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, "test-1");

        assertTrue(changeSetDir.mkdirs(), "Failed to create change set directory: " + changeSetDir.getAbsolutePath());

        List<String> changeSet = asList(
            "server/conf 1",
            "8f26ac3d 0 myconf.conf A"
        );
        writeLines(new File(changeSetDir, "changeset.txt"), changeSet);

        ChangeSetManager changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId, drfitConfig);

        assertNotNull(reader, "Expected to get a change set reader when change set exists");

        DirectoryEntry dirEntry = reader.readDirectoryEntry();
        assertNotNull(dirEntry, "Expected reader to return a directory entry. Was the correct change set located?");

        assertEquals(dirEntry.getDirectory(), "server/conf", "The directory entry path is wrong");
        assertEquals(dirEntry.getNumberOfFiles(), 1, "The number of files for the directory entry is wrong");
    }

    DriftConfiguration driftConfiguration(String name, String basedir) {
        Configuration config = new Configuration();
        config.put(new PropertySimple("name", name));
        config.put(new PropertySimple("basedir", basedir));

        return new DriftConfiguration(config);
    }

}
