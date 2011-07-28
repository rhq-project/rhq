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

package org.rhq.core.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.testng.annotations.Test;

import org.rhq.core.util.ZipUtil.ZipEntryVisitor;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

@Test
public class ZipUtilTest {
    public void testBasicZip() throws Exception {
        // just make sure we don't blow up, zip up something and see that the file was created.
        File zipFile = File.createTempFile("testZip", ".zip");
        zipFile.delete(); // I want to see it created by the ZipUtil

        File unzipDir = FileUtil.createTempDirectory("testUnzip", ".dir", null);

        try {
            ZipUtil.zipFileOrDirectory(new File("target/test-classes"), zipFile);
            assert zipFile.exists();
            assert zipFile.length() > 0;

            ZipUtil.unzipFile(zipFile, unzipDir);
            assert unzipDir.listFiles().length > 0;
        } finally {
            zipFile.delete();
            FileUtil.purge(unzipDir, true);
        }
    }

    public void testVisitor() throws Exception {
        File zipFile = File.createTempFile("testZipVisitor", ".zip");

        final File unzipDir = FileUtil.createTempDirectory("testUnzip", ".dir", null);
        assert unzipDir.listFiles().length == 0 : "failed sanity check - this should be a new, empty dir";

        try {
            ZipUtil.zipFileOrDirectory(new File("target/test-classes"), zipFile);
            assert zipFile.exists();
            assert zipFile.length() > 0;

            ZipEntryVisitor visitor = new ZipUtil.ZipEntryVisitor() {
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    File destFile = new File(unzipDir, entry.getName());
                    if (entry.isDirectory()) {
                        destFile.mkdirs();
                    } else {
                        destFile.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(destFile);
                        StreamUtil.copy(stream, fos, false);
                        fos.close();
                    }
                    return true;
                }
            };

            ZipUtil.walkZipFile(zipFile, visitor);
            assert unzipDir.listFiles().length > 0;
        } finally {
            zipFile.delete();
            FileUtil.purge(unzipDir, true);
        }
    }

    public void testError() throws Exception {
        try {
            ZipUtil.zipFileOrDirectory(new File("target/test-classes"), new File("target/test-classes/foo.zip"));
            assert false : "Should not be allowed to create a zip file in the same directory being zipped";
        } catch (Exception ok) {
        }

        try {
            ZipUtil.zipFileOrDirectory(new File("target"), new File("target/test-classes/foo.zip"));
            assert false : "Should not be allowed to create a zip file under the directory being zipped";
        } catch (Exception ok) {
        }
    }
}
