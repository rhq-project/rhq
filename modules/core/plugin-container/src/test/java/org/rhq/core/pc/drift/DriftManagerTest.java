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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.util.stream.StreamUtil;

import static org.rhq.core.util.ZipUtil.unzipFile;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class DriftManagerTest extends DriftTest {

    private File tmpDir;

    private TestDriftServerService driftServerService;

    private PluginContainerConfiguration pcConfig;

    private DriftManager driftMgr;

    @BeforeMethod
    public void initTest() throws Exception {
        tmpDir = mkdir(basedir(), "tmp");

        pcConfig = new PluginContainerConfiguration();
        ServerServices serverServices = new ServerServices();
        driftServerService = new TestDriftServerService();
        serverServices.setDriftServerService(driftServerService);
        pcConfig.setServerServices(serverServices);
        pcConfig.setDataDirectory(basedir());
        pcConfig.setTemporaryDirectory(tmpDir);

        driftMgr = new DriftManager();
        driftMgr.setConfiguration(pcConfig);
    }

    @Test
    public void writeContentZipFileToChangeSetContentDirectory() throws Exception {
        String configName = "send-content-in-zip";
        File changeSetDir = changeSetDir(configName);
        final File contentDir = mkdir(changeSetDir, "content");

        createRandomFile(contentDir, "content-1");
        createRandomFile(contentDir, "content-2");

        setDriftServiceCallback(new DriftServiceCallback() {
            @Override
            public void execute() {
                File contentZip = new File(contentDir, "content.zip");
                assertTrue(contentZip.exists(), "Expected content zip file to be written to the change set content " +
                    "directory: " + contentDir.getPath());
            }
        });

        driftMgr.sendChangeSetContentToServer(resourceId(), configName, contentDir);
    }

    @Test
    public void sendContentToServerInZipFile() throws Exception {
        String configName = "send-content-in-zip";
        File changeSetDir = changeSetDir(configName);
        File contentDir = mkdir(changeSetDir, "content");

        final File content1 = createRandomFile(contentDir, "content-1");
        final File content2 = createRandomFile(contentDir, "content-2");

        setDriftServiceCallback(new DriftServiceCallback() {
            @Override
            public void execute() {
                assertZipFileMatches(driftServerService.filesZipStream, content1, content2);
            }
        });

        driftMgr.sendChangeSetContentToServer(resourceId(), configName, contentDir);
    }

    @Test
    public void cleanUpAfterSendingContentToServer() throws Exception {
        String configName = "clean-up-after-sending-content";
        File changeSetDir = changeSetDir(configName);
        File contentDir = mkdir(changeSetDir, "content");

        createRandomFile(contentDir, "content-1");
        createRandomFile(contentDir, "content-2");

        driftMgr.sendChangeSetContentToServer(resourceId(), configName, contentDir);

        assertThatDirectoryIsEmpty(contentDir);
    }

    /**
     * This method first verifies that each of the expected files is contained in the the
     * zip file. Then it verifies that the content for each file in the zip file matches
     * the expected files by comparing their SHA-256 hashes.
     *
     * @param zipStream
     * @param expectedFiles
     * @throws IOException
     */
    private void assertZipFileMatches(InputStream zipStream, File... expectedFiles) {
        File zipDir = new File(tmpDir, "output");
        try {
            unzipFile(zipStream, zipDir);
        } catch (IOException e) {
            fail("Failed to unzip zip file from intput stream into " + zipDir.getPath(), e);
        }

        for (File expectedFile : expectedFiles) {

            File actualFile = findFile(zipDir, expectedFile);
            assertNotNull(actualFile, "Expected zip file to contain " + expectedFile.getName());

            String expectedHash = sha256(expectedFile);
            String actualHash = sha256(actualFile);

            assertEquals(actualHash, expectedHash, "The zip file content is wrong. The SHA-256 hash does not match " +
                "for " + expectedFile.getName());
        }
    }

    private void assertThatDirectoryIsEmpty(File dir) {
        assertEquals(dir.listFiles().length, 0, "Expected " + dir.getPath() + " to be empty");
    }

    private File findFile(File dir, File file) {
        for (File f : dir.listFiles()) {
            if (f.getName().equals(file.getName())) {
                return f;
            }
        }
        return null;
    }

    private void addToZipFile(File file, ZipOutputStream stream) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        stream.putNextEntry(new ZipEntry(file.getName()));
        StreamUtil.copy(fis, stream, false);
        fis.close();
    }

    /**
     * Sets a callback that will be invoked immediately after DriftManager calls
     * {@link DriftServerService#sendChangesetZip(int, long, java.io.InputStream)} or
     * {@link DriftServerService#sendFilesZip(int, long, java.io.InputStream)}. The callback
     * can perform any verification as necessary, and that will happen before the call to
     * to DriftServerService returns.
     *
     * @param callback
     */
    private void setDriftServiceCallback(DriftServiceCallback callback) {
        driftServerService.callback = callback;
    }

    private static class TestDriftServerService implements DriftServerService {

        public int filesZipResourceId;
        public long filesZipSize;
        public InputStream filesZipStream;

        public DriftServiceCallback callback;

        @Override
        public void sendChangesetZip(int resourceId, long zipSize, InputStream zipStream) {
        }

        @Override
        public void sendFilesZip(int resourceId, long zipSize, InputStream zipStream) {
            filesZipResourceId = resourceId;
            filesZipSize = zipSize;
            filesZipStream = zipStream;

            if (callback != null) {
                callback.execute();
            }
        }
    }

    /**
     * This callback interface provides a hook for doing any verification immediately after
     * DriftManager calls DriftServerService.
     */
    private static interface DriftServiceCallback {
        void execute();
    }

}
