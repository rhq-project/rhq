 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utilities for working with files from a plugin.
 *
 * @author Jason Dobies
 * @author Ian Springer
 */
public class FileUtils {
    /**
     * Writes the content in the input stream to the specified file.
     * NOTE: content will be closed by this.
     *
     * @param  content    stream containing the content to write
     * @param  outputFile file to which the content will be written
     *
     * @throws java.io.IOException if any errors occur during the reading or writing
     */
    public static void writeFile(InputStream content, File outputFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFile);
        BufferedOutputStream buf = new BufferedOutputStream(fos);

        byte[] data = new byte[4096];
        int len;
        while ((len = content.read(data)) > 0) {
            buf.write(data, 0, len);
        }

        content.close();
        buf.flush();
        fos.close();
    }

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

    private static void zipFileOrDirectory(File fileOrDirToZip, ZipOutputStream zos) throws IOException {
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
     * @param  content   stream containing the content to write; should be formatted as a ZIP file
     * @param  outputDir directory to which
     *
     * @throws java.io.IOException if any errors occur during the reading or writing
     */
    public static void unzipFile(InputStream content, File outputDir) throws IOException {
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

        ZipInputStream zis = new ZipInputStream(content);
        ZipEntry e;

        while ((e = zis.getNextEntry()) != null) {
            String entryFileName = e.getName();
            File entryFile = new File(outputDir + File.separator + entryFileName);

            if (e.isDirectory()) {
                entryFile.mkdirs();
            } else {

                // JBNADM-3287 - For each file, make sure the directory it is to reside in is created.
                String parentDirectory = entryFile.getParent();
                File parentDirectoryFile = new File(parentDirectory);
                parentDirectoryFile.mkdirs();

                FileOutputStream fos = new FileOutputStream(entryFile);
                BufferedOutputStream buf = new BufferedOutputStream(fos);

                byte[] data = new byte[4096];
                int len;
                while ((len = zis.read(data)) > 0) {
                    buf.write(data, 0, len);
                }

                buf.flush();
                fos.close();
            }
        }

        content.close();
    }

    /**
     * Recursively deletes a series of files. Any directories found in the list of files will be recursively deleted as
     * well.
     *
     * @param contents list of files to delete
     */
    public static void deleteDirectoryContents(File[] contents) {
        for (File file : contents) {
            if (file.isDirectory()) {
                deleteDirectoryContents(file.listFiles());
            }

            file.delete();
        }
    }
}