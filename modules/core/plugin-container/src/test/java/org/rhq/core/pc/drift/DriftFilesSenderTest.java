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
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.JPADriftFile;

import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DriftFilesSenderTest extends DriftTest {

    DriftClient driftClient;

    DriftFilesSender sender;

    @BeforeMethod
    public void initSender() {
        driftClient = new DriftClientTestStub();

        sender = new DriftFilesSender();
        sender.setDriftClient(driftClient);
        sender.setChangeSetManager(changeSetMgr);
        sender.setResourceId(resourceId());
    }

    @Test
    public void sendFilesInOneDirectory() throws Exception {
        String driftDefName = "single-directory-test";

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        File server2Conf = createRandomFile(confDir, "server-2.conf");

        String server1ConfHash = sha256(server1Conf);
        String server2ConfHash = sha256(server2Conf);

        File changeSetDir = changeSetDir(driftDefName);

        Headers headers = createHeaders(driftDefName, COVERAGE);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), headers);
        writer.write(addedFileEntry("conf/server-1.conf", server1ConfHash));
        writer.write(addedFileEntry("conf/server-2.conf", server2ConfHash));
        writer.close();

        sender.setDriftFiles(driftFiles(server1ConfHash, server2ConfHash));
        sender.setHeaders(createHeaders(driftDefName, COVERAGE));
        sender.run();

        File contentDir = mkdir(changeSetDir, "content");

        assertEquals(contentDir.list().length, 2, "Expected to find two files in " + contentDir.getAbsolutePath());
        assertFileCopiedToContentDir(contentDir, server1ConfHash);
        assertFileCopiedToContentDir(contentDir, server2ConfHash);
    }

    @Test
    public void sendFilesInMultipleDirectories() throws Exception {
        String driftDefName = "multiple-directories-test";

        File confDir = mkdir(resourceDir, "conf");
        File libDir = mkdir(resourceDir, "lib");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        File server2Conf = createRandomFile(confDir, "server-2.conf");
        File server1Jar = createRandomFile(libDir, "server-1.jar");
        File server2Jar = createRandomFile(libDir, "server-2.jar");

        String server1ConfHash = sha256(server1Conf);
        String server2ConfHash = sha256(server2Conf);
        String server1JarHash = sha256(server1Jar);
        String server2JarHash = sha256(server2Jar);

        File changeSetDir = changeSetDir(driftDefName);

        Headers headers = createHeaders(driftDefName, COVERAGE);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), headers);
        writer.write(addedFileEntry("conf/server-1.conf", server1ConfHash));
        writer.write(addedFileEntry("conf/server-2.conf", server2ConfHash));
        writer.write(addedFileEntry("lib/server-1.jar", server1JarHash));
        writer.write(addedFileEntry("lib/server-2.jar", server2JarHash));
        writer.close();

        // Note that the order of the drift files is random. When the server sends a request
        // for files we cannot assume that the files will be in any particular order.
        sender.setDriftFiles(driftFiles(server1JarHash, server2ConfHash, server2JarHash, server1ConfHash));
        sender.setHeaders(createHeaders(driftDefName, COVERAGE));
        sender.run();

        File contentDir = mkdir(changeSetDir, "content");

        assertEquals(contentDir.list().length, 4, "Expected to find four files in " + contentDir.getAbsolutePath());
        assertFileCopiedToContentDir(contentDir, server1ConfHash);
        assertFileCopiedToContentDir(contentDir, server2ConfHash);
        assertFileCopiedToContentDir(contentDir, server1JarHash);
        assertFileCopiedToContentDir(contentDir, server2JarHash);
    }

    @Test
    public void checkForFilesThatHaveBeenRemoved() throws Exception {
        String driftDefName = "file-exists-test";

        File confDir = mkdir(resourceDir, "conf");
        File server1Conf = createRandomFile(confDir, "server-1.conf");
        File server2Conf = createRandomFile(confDir, "server-2.conf");

        String server1ConfHash = sha256(server1Conf);
        String server2ConfHash = sha256(server2Conf);

        File changeSetDir = changeSetDir(driftDefName);

        Headers headers = createHeaders(driftDefName, COVERAGE);

        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resourceId(), headers);
        writer.write(addedFileEntry("conf/server-1.conf", server1ConfHash));
        writer.write(addedFileEntry("conf/server-2.conf", server2ConfHash));
        writer.close();

        // It is possible that the server can request a file that has been deleted since it
        // it was initially detected during drift detection.
        server1Conf.delete();


        sender.setDriftFiles(driftFiles(server1ConfHash, server2ConfHash));
        sender.setHeaders(headers);
        sender.run();

        File contentDir = mkdir(changeSetDir, "content");

        assertEquals(contentDir.list().length, 1, "Expected to find one file in " + contentDir.getAbsolutePath());
        assertFileCopiedToContentDir(contentDir, server2ConfHash);
    }

    /**
     * This method only verifies that a file having a particular hash or SHA-256 has been
     * copied to the specified content directory.
     *
     * @param contentDir The directory to which the file should have been copied
     *
     * @param fileHash The file hash which is expected to be the name of the file in the
     * content directory.
     */
    void assertFileCopiedToContentDir(File contentDir, String fileHash) {
        File file = new File(contentDir, fileHash);
        assertTrue(file.exists(), "Expected to find file named " + file.getName() + " in content directory " +
            contentDir.getPath() + ". The SHA-256 hash should be used as the file name.");
    }

    Headers createHeaders(String configName, DriftChangeSetCategory type) {
        Headers headers = new Headers();
        headers.setResourceId(resourceId());
        headers.setDriftDefinitionId(0);
        headers.setDriftDefinitionName(configName);
        headers.setBasedir(resourceDir.getAbsolutePath());
        headers.setType(type);

        return headers;
    }

    List<DriftFile> driftFiles(String... hashes) {
        List<DriftFile> files = new ArrayList<DriftFile>();
        for (String hash : hashes) {
            files.add(new JPADriftFile(hash));
        }
        return files;
    }
}
