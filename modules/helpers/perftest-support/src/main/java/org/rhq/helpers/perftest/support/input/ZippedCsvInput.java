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

package org.rhq.helpers.perftest.support.input;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dbunit.dataset.stream.IDataSetProducer;

/**
 * Represents a zipped CSV input.
 * 
 * @author Lukas Krejci
 */
public class ZippedCsvInput extends CsvInput {

    private ZipInputStreamProviderDecorator sourceFile;
    
    /**
     * 
     * @param zipFileProvider a provider that creates a stream to read the zip file.
     * @throws IOException
     */
    public ZippedCsvInput(ZipInputStreamProviderDecorator zipFileProvider) throws IOException {
        super(createTempDirectory());
        sourceFile = zipFileProvider;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        delete(getDirectory());
    }

    @Override
    public IDataSetProducer getProducer() throws Exception {
        if (getCsvProducer() == null) {
            unzip(sourceFile.createInputStream(), getDirectory());
        }
        return super.getProducer();
    }
    
    private static File createTempDirectory() throws IOException {
        File tmpFile = File.createTempFile("perftest-support-csv-output", null);
        tmpFile.delete();
        tmpFile.mkdir();

        return tmpFile;
    }    
    
    private static void unzip(ZipInputStream zipFile, File target) throws RuntimeException, IOException {
        ZipEntry entry = null;
        while((entry = zipFile.getNextEntry()) != null) {
            File f = new File(target, entry.getName());
            
            if (entry.isDirectory()) {
                f.mkdirs();
            } else {
                String parentDirectory = f.getParent();
                File parentDirectoryFile = new File(parentDirectory);
                parentDirectoryFile.mkdirs();

                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(f));
                try {
                    copy(zipFile, outputStream);
                } finally {
                    outputStream.close();
                }
            }
        }
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
}
