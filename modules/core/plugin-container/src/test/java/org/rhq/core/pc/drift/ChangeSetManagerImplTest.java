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

import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;

import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;

public class ChangeSetManagerImplTest extends DriftTest {

    @Test
    public void returnNullReaderWhenNoChangeSetExists() throws Exception {
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId(), "test");
        assertNull(reader, "Expect null for the reader when no change set exists for the drift definition.");
    }

    @Test
    public void returnReaderForRequestedChangeSet() throws Exception {
        String config = "return-reader-for-existing-changeset-test";

        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");
        String serverConfHash = sha256(serverConf);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), createHeaders(config, COVERAGE));
        writer.write(addedFileEntry("conf/server.conf", serverConfHash, serverConf.lastModified(), serverConf.length()));
        writer.close();

        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId(), config);

        assertNotNull(reader, "Expected to get a change set reader when change set exists");
        assertReaderOpenedOnChangeSet(reader);
    }

    @Test
    public void verifyChangeSetExists() throws Exception {
        String config = "changeset-exists-test";

        File confDir = mkdir(resourceDir, "conf");
        File serverConf = createRandomFile(confDir, "server.conf");

        Headers headers = createHeaders(config, COVERAGE);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), headers);
        writer.write(addedFileEntry("conf/server.conf", sha256(serverConf), serverConf.lastModified(), serverConf.length()));
        writer.close();

        assertTrue(changeSetMgr.changeSetExists(resourceId(), headers), "Expected to find change set file.");
    }

    @Test
    public void verifyChangeSetDoesNotExist() throws Exception {
        String config = "changeset-does-not-exist";
        assertFalse(changeSetMgr.changeSetExists(resourceId(), createHeaders(config, DRIFT)),
            "Did not expect to find change set file.");
    }

    void assertReaderOpenedOnChangeSet(ChangeSetReader reader) throws Exception {
        assertNotNull(reader, "The " + ChangeSetReader.class.getSimpleName() + " should not be null.");

        assertNotNull(reader.read(), "Expected to find a file entry");
    }

    Headers createHeaders(String driftDefName, DriftChangeSetCategory type) {
        Headers headers = new Headers();
        headers.setResourceId(resourceId());
        headers.setDriftDefinitionId(1);
        headers.setDriftDefinitionName(driftDefName);
        headers.setBasedir(resourceDir.getPath());
        headers.setType(type);

        return headers;
    }

}
