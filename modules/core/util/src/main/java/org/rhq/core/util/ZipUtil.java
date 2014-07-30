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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.rhq.core.util.stream.StreamUtil;

/**
 * A set of utility methods for working with zip files.
 * 
 * @author Ian Springer
 */
public abstract class ZipUtil {

    /**
     * Zips up the given file or directory and stores the zip file at <code>zipFile</code>.
     * Note that zipping up a directory and if the output zip file is to be located in or under
     * the directory being zipped, an exception will be thrown - you must output the zip file
     * in another location outside of the directory being zipped.
     *  
     * @param fileOrDirToZip what to zip up
     * @param zipFile where to store the zip file
     * @throws IOException
     */
    public static void zipFileOrDirectory(File fileOrDirToZip, File zipFile) throws IOException {
        if (fileOrDirToZip.isDirectory()) {
            if (zipFile.getParentFile().getAbsolutePath().startsWith(fileOrDirToZip.getAbsolutePath())) {
                // if we allowed this, we could go in an infinite loop zipping up the every growing zip file
                throw new IOException("Cannot write the zip file [" + zipFile.getAbsolutePath()
                    + "] in or under the same directory being zipped [" + fileOrDirToZip.getAbsolutePath() + "]");
            }
        }

        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            zipFileOrDirectory(fileOrDirToZip, zos);
        } finally {
            if (zos != null) {
                zos.close();
            }
        }
    }

    public static void zipFileOrDirectory(File fileOrDirToZip, ZipOutputStream zos) throws IOException {
        if (fileOrDirToZip.isDirectory()) {
            File[] files = fileOrDirToZip.listFiles();
            for (File f : files) {
                zipFileOrDirectory(f, zos); // recurse
            }
        } else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fileOrDirToZip);
                ZipEntry zipEntry = new ZipEntry(fileOrDirToZip.getPath());
                zos.putNextEntry(zipEntry);
                StreamUtil.copy(fis, zos, false);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
    }

    /**
     * Unzips the content of the given zip file to the specified directory.
     *
     * @param  zipFile the zip file to unzip
     * @param  destDir root directory where the zip files will be extracted
     *
     * @throws IOException if any errors occur during the reading or writing
     */
    public static void unzipFile(File zipFile, File destDir) throws IOException {
        destDir.mkdirs();
        InputStream is = new BufferedInputStream(new FileInputStream(zipFile));
        ZipUtil.unzipFile(is, destDir);
    }

    /**
     * Writes the zip content out to the specified directory. The input stream should contain a valid
     * ZIP archive that will be extracted. NOTE: zipContent will be closed by this method.
     *
     * @param  zipContent   stream containing the content to write; should be formatted as a ZIP file
     * @param  outputDir root directory where the zip files will be extracted
     *
     * @throws IOException if any errors occur during the reading or writing
     */
    public static void unzipFile(InputStream zipContent, File outputDir) throws IOException {
        try {
            // First step is to create the output directory into which to unzip the content stream
            if (outputDir.exists() && !outputDir.isDirectory()) {
                throw new RuntimeException("Output directory already exists, but is a file, not a directory. File: "
                    + outputDir.getAbsolutePath());
            }

            if (!outputDir.exists()) {
                boolean directoryMade = outputDir.mkdirs();
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

                    FileOutputStream fos = new FileOutputStream(entryFile);
                    try {
                        BufferedOutputStream outputStream = new BufferedOutputStream(fos);
                        try {
                            StreamUtil.copy(zis, outputStream, false);
                        } finally {
                            outputStream.close();
                        }
                    } finally {
                        fos.close();
                    }
                }
            }
        } finally {
            zipContent.close();
        }
    }

    /**
     * Walks the entries of a zip file, allowing a listener to "visit" each node and perform tasks on
     * the zip entry.
     *
     * @param zipFile the zip file to walk
     * @param visitor the object that will be notified for each entry in the zip file
     *
     * @throws Exception if any errors occur during the reading or visiting
     */
    public static void walkZipFile(File zipFile, ZipEntryVisitor visitor) throws Exception {
        FileInputStream fis = new FileInputStream(zipFile);
        try {
            InputStream zipContent = new BufferedInputStream(fis);
            try {
                walkZip(zipContent, visitor, false);
            } finally {
                zipContent.close();
            }
        } finally {
            fis.close();
        }
    }

    /**
     * Walks the provided zip file stream. Does NOT close it afterwards.
     *
     * @param zipFileStream the stream of zip file contents
     * @param visitor the visitor to call on each zip entry
     * @param readFully true if the whole zip file should be read from the stream even if visitor bailed out, false to
     *                  quit reading as soon as the visitor bails out.
     * @throws Exception
     * @since 4.13
     */
    public static void walkZip(InputStream zipFileStream, ZipEntryVisitor visitor, boolean readFully) throws Exception {
        ZipInputStream zis = new ZipInputStream(zipFileStream);

        ZipEntry e;
        boolean doVisit = true;
        while ((e = zis.getNextEntry()) != null) {
            doVisit = doVisit && visitor.visit(e, zis);
            if (!readFully && !doVisit) {
                break;
            }
        }
    }

    // prevent instantiation
    private ZipUtil() {
    }

    /**
     * Used by {@link ZipUtil#walkZipFile(File, ZipEntryVisitor)} to visit zip entries.
     */
    public static interface ZipEntryVisitor {
        /**
         * Visits a specific zip file entry. Implementations can read the entry content from the given stream but
         * must <b>not</b> close the stream - the caller of this method will handle the lifecycle of the stream.
         * 
         * @param entry the entry being visited
         * @param stream the stream containing the zip content
         * @return the visitor should return <code>true</code> if everything is OK and processing of the zip content
         *         should continue; returning <code>false</code> will tell the walker to abort further traversing
         *         of the zip content.
         * @throws Exception if the visitation failed for some reason - this will abort further walking of the zip content
         */
        boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception;
    }
}
