/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.output;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Represents a CSV output compressed to a zip file.
 * 
 * @author Lukas Krejci
 */
public class ZippedCsvOutput extends CsvOutput {

    private File targetFile;

    /**
     * @param file the target zip file
     * @throws IOException 
     */
    public ZippedCsvOutput(File file) throws IOException {
        super(createTempDirectory());
        targetFile = file;
    }

    public void close() throws IOException {
        super.close();
        ZipOutputStream stream = null;
        try {
            stream = new ZipOutputStream(new FileOutputStream(targetFile));
            zipFileOrDir(super.getDirectory(), stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
            delete(super.getDirectory());
        }
    }

    private static File createTempDirectory() throws IOException {
        File tmpFile = File.createTempFile("perftest-support-csv-output", null);
        tmpFile.delete();
        tmpFile.mkdir();

        return tmpFile;
    }

    private static void zipFileOrDir(File f, ZipOutputStream zipFile) throws IOException {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                zipFileOrDir(child, zipFile);
            }
        } else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                ZipEntry zipEntry = new ZipEntry(f.getPath());
                zipFile.putNextEntry(zipEntry);
                copy(fis, zipFile);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        long numBytesCopied = 0;
        int bufferSize = 32768;

        input = new BufferedInputStream(input, bufferSize);

        byte[] buffer = new byte[bufferSize];

        for (int bytesRead = input.read(buffer); bytesRead != -1; bytesRead = input.read(buffer)) {
            output.write(buffer, 0, bytesRead);
            numBytesCopied += bytesRead;
        }

        output.flush();
    }

    private static void delete(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                delete(child);
            }
            f.delete();
        } else {
            f.delete();
        }
    }
}
