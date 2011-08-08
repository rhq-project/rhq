/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.core.pc.drift;

import java.io.File;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;

import static java.util.Arrays.asList;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ChangeSetManagerImplTest extends DriftTest {

    @Test
    public void returnNullReaderWhenNoChangeSetExists() throws Exception {
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId(), "test");
        assertNull(reader, "Expect null for the reader when no change set exists for the drift configuration.");
    }

    @Test
    public void returnReaderForRequestedChangeSet() throws Exception {
        String config = "return-reader-for-existing-changeset-test";

        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");
        String serverConfHash = sha256(serverConf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(config, COVERAGE));
        writer.writeDirectoryEntry(new DirectoryEntry("conf").add(addedFileEntry("server.conf", serverConfHash)));
        writer.close();

        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId(), config);

        assertNotNull(reader, "Expected to get a change set reader when change set exists");
        assertReaderOpenedOnChangeSet(reader, asList("conf", "1"));
    }

    @Test
    public void verifyChangeSetExists() throws Exception {
        String config = "changeset-exists-test";

        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");

        Headers headers = createHeaders(config, COVERAGE);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), headers);
        writer.writeDirectoryEntry(new DirectoryEntry("conf").add(addedFileEntry("server.conf", sha256(serverConf))));
        writer.close();

        assertTrue(changeSetMgr.changeSetExists(resourceId(), headers), "Expected to find change set file.");
    }

    @Test
    public void verifyChangeSetDoesNotExist() throws Exception {
        String config = "changeset-does-not-exist";
        assertFalse(changeSetMgr.changeSetExists(resourceId(), createHeaders(config, DRIFT)),
            "Did not expect to find change set file.");
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

    Headers createHeaders(String driftConfigName, DriftChangeSetCategory type) {
        Headers headers = new Headers();
        headers.setResourceId(resourceId());
        headers.setDriftCofigurationId(1);
        headers.setDriftConfigurationName(driftConfigName);
        headers.setBasedir(resourceDir.getPath());
        headers.setType(type);

        return headers;
    }

}
