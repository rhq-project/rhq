/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.util.updater;

import java.io.File;

import org.testng.annotations.Test;

import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;

@Test
public class DeploymentsMetadataTest {
    public void testBasic() throws Exception {
        File tmpDir = FileUtil.createTempDirectory("deploymentsMetadataTest", ".dir", null);
        try {
            ZipUtil.unzipFile(new File("target/test-classes/updater-test1.zip"), tmpDir);
            DeploymentsMetadata metadata = new DeploymentsMetadata(tmpDir);
            assert !metadata.isManaged() : "this should not be managed yet : " + metadata;

            DeploymentProperties deploymentProps = new DeploymentProperties();
            int deploymentId = 1;
            deploymentProps.setDeploymentId(deploymentId);
            deploymentProps.setBundleName("test-bundle-name");
            deploymentProps.setBundleVersion("1.0");
            deploymentProps.setDescription("test bundle description");
            FileHashcodeMap map = metadata.snapshotLiveDeployment(deploymentProps, null, null);
            assert metadata.isManaged() : "this should be managed now : " + metadata;
            assert map.size() == 5 : map; // there are 5 files in our test bundle zip
            assert map.containsKey("file0") : map;
            assert map.containsKey("dir1" + File.separator + "file1") : map;
            assert map.containsKey("dir1" + File.separator + "file2") : map;
            assert map.containsKey("dir2" + File.separator + "file3") : map;
            assert map.containsKey("dir3" + File.separator + "dir4" + File.separator + "file4") : map;

            // make sure we created our metadata
            File metadataDir = new File(tmpDir, DeploymentsMetadata.METADATA_DIR);
            assert metadataDir.exists();
            assert new File(metadataDir, DeploymentsMetadata.CURRENT_DEPLOYMENT_FILE).exists();
            File deploymentDir = new File(metadataDir, Integer.toString(deploymentId));
            assert deploymentDir.isDirectory();
            assert new File(deploymentDir, DeploymentsMetadata.DEPLOYMENT_FILE).exists();
            assert new File(deploymentDir, DeploymentsMetadata.HASHCODES_FILE).exists();

            FileHashcodeMap mapDup = FileHashcodeMap.loadFromFile(new File(deploymentDir,
                DeploymentsMetadata.HASHCODES_FILE));
            assert map.equals(mapDup) : mapDup + " is not same as " + map;

        } finally {
            FileUtil.purge(tmpDir, true);
        }
    }
}
