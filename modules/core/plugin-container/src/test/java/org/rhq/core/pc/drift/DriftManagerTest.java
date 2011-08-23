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

public class DriftManagerTest extends DriftTest {

    private File tmpDir;

    private TestDriftServerService driftServerService;

    private PluginContainerConfiguration pcConfig;

//    private DriftManager driftMgr;

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
    }

    @Test
    public void sendContentToServerInZipFile() throws Exception {
        String configName = "send-content-in-zip";
        File changeSetDir = changeSetDir(configName);
        File contentDir = mkdir(changeSetDir, "content");

        File content1 = createRandomFile(contentDir, "content-1");
        File content2 = createRandomFile(contentDir, "content-2");

        DriftManager driftMgr = new DriftManager();
        driftMgr.setConfiguration(pcConfig);

        driftMgr.sendChangeSetContentToServer(resourceId(), configName, contentDir);

        File outputDir = new File(tmpDir, "output");
        outputDir.mkdir();
        unzipFile(driftServerService.filesZipStream, outputDir);

        assertEquals(outputDir.listFiles().length, 2, "Expected the zip file to contain two entries");
        assertZipFileMatches(outputDir, content1, content2);
    }

//    @Test
//    public void cleanUpAfterSendingContentToServer() throws Exception {
//        String configName = "clean-up-after-sending-content";
//        File changeSetDir = changeSetDir(configName);
//        File contentDir = mkdir(changeSetDir, "content");
//    }

    /**
     * This method first verifies that each of the expected files is contained in the the
     * zip file. Then it verifies that the content for each file in the zip file matches
     * the expected files by comparing their SHA-256 hashes.
     *
     * @param zipDir
     * @param expectedFiles
     * @throws IOException
     */
    private void assertZipFileMatches(File zipDir, File... expectedFiles) throws IOException {
        for (File expectedFile : expectedFiles) {
            File actualFile = findFile(zipDir, expectedFile);
            assertNotNull(actualFile, "Expected zip file to contain " + expectedFile.getName());

            String expectedHash = sha256(expectedFile);
            String actualHash = sha256(actualFile);

            assertEquals(actualHash, expectedHash, "The zip file content is wrong. The SHA-256 hash does not match " +
                "for " + expectedFile.getName());
        }
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

    private static class TestDriftServerService implements DriftServerService {

        public int filesZipResourceId;
        public long filesZipSize;
        public InputStream filesZipStream;

        @Override
        public void sendChangesetZip(int resourceId, long zipSize, InputStream zipStream) {
        }

        @Override
        public void sendFilesZip(int resourceId, long zipSize, InputStream zipStream) {
            filesZipResourceId = resourceId;
            filesZipSize = zipSize;
            filesZipStream = zipStream;
        }
    }

}
