/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.util;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A set of utility methods for working with zip files.
 * 
 * @author Ian Springer
 */
public abstract class ZipUtil {
    public static void zipFileOrDirectory(File fileOrDirToZip, File zipFile) throws IOException {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            zipFileOrDirectory(fileOrDirToZip, zos);
        }
        finally {
            if (zos != null)
                zos.close();
        }
    }

    public static void zipFileOrDirectory(File fileOrDirToZip, ZipOutputStream zos) throws IOException {
        if (fileOrDirToZip.isDirectory()) {
            File[] files = fileOrDirToZip.listFiles();
            for (File f : files)
                zipFileOrDirectory(f, zos); // recurse
        } else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fileOrDirToZip);
                ZipEntry zipEntry = new ZipEntry(fileOrDirToZip.getPath());
                zos.putNextEntry(zipEntry);
                byte[] readBuffer = new byte[4096];
                int bytesIn;
                while((bytesIn = fis.read(readBuffer)) != -1)
                    zos.write(readBuffer, 0, bytesIn);
            }
            finally {
                if (fis != null)
                    fis.close();
            }
        }
    }

    /**
     * Writes the content out to the specified directory as an unzipped file. The input stream should contain a valid
     * ZIP archive that will be extracted. NOTE: content will be closed by this method.
     *
     * @param  zipContent   stream containing the content to write; should be formatted as a ZIP file
     * @param  outputDir directory to which
     *
     * @throws IOException if any errors occur during the reading or writing
     */
    public static void unzipFile(InputStream zipContent, File outputDir) throws IOException {
        // First step is to create the output directory into which to unzip the content stream
        if (outputDir.exists() && !outputDir.isDirectory()) {
            throw new RuntimeException("Output directory already exists, but is a file, not a directory. File: "
                + outputDir.getAbsolutePath());
        }

        if (!outputDir.exists()) {
            boolean directoryMade = outputDir.mkdir();
            if (!directoryMade) {
                throw new RuntimeException("Could not create output directory for unzipped artifact: "
                    + outputDir.getAbsolutePath());
            }
        }

        ZipInputStream zis = new ZipInputStream(zipContent);
        ZipEntry e;

        while ((e = zis.getNextEntry()) != null) {
            String entryFileName = e.getName();
            File entryFile = new File(outputDir, entryFileName);

            if (e.isDirectory()) {
                entryFile.mkdirs();
            } else {

                // JBNADM-3287 - For each file, make sure the directory it is to reside in is created.
                String parentDirectory = entryFile.getParent();
                File parentDirectoryFile = new File(parentDirectory);
                parentDirectoryFile.mkdirs();

                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(entryFile));
                byte[] data = new byte[4096];
                int byteCount;
                while ((byteCount = zis.read(data)) != -1)
                    outputStream.write(data, 0, byteCount);
                outputStream.close();
            }
        }

        zipContent.close();
    }

    // prevent instantiation
    private ZipUtil() {
    }
}
