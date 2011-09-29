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

package org.rhq.common.drift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.drift.DriftChangeSetCategory;

import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;

/**
 * Note that this implementation does not do any validation for the most part. It assumes
 * that entries in the change set file contain valid, 64 character SHA-256 hashes. It
 * assumes that all headers are present in the file and in the correct order. It assume
 * that all paths are non-null.
 * <p/>
 * The one thing that the read does check is the drift category code for each file entry.
 * If the code is not valid, an error message is logged and a {@link ChangeSetReaderException}
 * is thrown.
 */
public class ChangeSetReaderImpl implements ChangeSetReader {

    private Log log = LogFactory.getLog(ChangeSetReaderImpl.class);

    private BufferedReader reader;

    private File metaDataFile;

    private Headers headers;

    private boolean closeStream;

    public ChangeSetReaderImpl(File metaDataFile) throws ChangeSetReaderException {
        try {
            this.metaDataFile = metaDataFile;
            reader = new BufferedReader(new FileReader(this.metaDataFile));
            readHeaders();
        } catch (IOException e) {
            log.error("Unable to read headers from " + metaDataFile.getAbsolutePath() + ": " + e.getMessage());
            throw new ChangeSetReaderException("Unable to read headers from " + metaDataFile.getAbsolutePath(), e);
        } catch (IllegalArgumentException e) {
            log.error("Unable to read headers from " + metaDataFile.getAbsolutePath() + ": " + e.getMessage());
            throw new ChangeSetReaderException("Unable to read headers from " + metaDataFile.getAbsolutePath(), e);
        }
    }

    public ChangeSetReaderImpl(Reader metaDataFile) throws Exception {
        this(metaDataFile, false);
    }

    /**
     * Creates a new change set reader. This constructor takes a boolean argument that can
     * be used to prevent the reader from closing the stream when using its iterator. Note
     * that calling {@link #close()} will close the stream regardless of the value of
     * closeStream.
     *
     * @param metaDataFile
     * @param closeStream
     * @throws Exception
     */
    public ChangeSetReaderImpl(Reader metaDataFile, boolean closeStream) throws Exception {
        reader = new BufferedReader(metaDataFile);
        readHeaders();
        this.closeStream = closeStream;
    }

    private void readHeaders() throws IOException {
        headers = new Headers();
        try {
            headers.setResourceId(Integer.parseInt(reader.readLine()));
            headers.setDriftCofigurationId(Integer.parseInt(reader.readLine()));
            headers.setDriftConfigurationName(reader.readLine());
            headers.setBasedir(reader.readLine());
            headers.setType(DriftChangeSetCategory.fromCode(reader.readLine()));
            headers.setVersion(Integer.parseInt(reader.readLine()));
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Invalid changeset headers, could not parse: ", t);
        }
    }

    @Override
    public Headers getHeaders() throws ChangeSetReaderException {
        return headers;
    }

    @Override
    public FileEntry read() throws ChangeSetReaderException {
        try {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }

            if (line.charAt(0) == 'A') { // file added
                String sha = line.substring(2, 66);
                String fileName = line.substring(69);
                return addedFileEntry(fileName, sha);
            }
            if (line.charAt(0) == 'C') { // file modified
                String newSha = line.substring(2, 66);
                String oldSha = line.substring(67, 131);
                String fileName = line.substring(132);
                return changedFileEntry(fileName, oldSha, newSha);
            }
            if (line.charAt(0) == 'R') { // file deleted
                String sha = line.substring(4, 68);
                String fileName = line.substring(69);
                return removedFileEntry(fileName, sha);
            }

            log.error("An error occurred while parsing " + metaDataFile.getAbsolutePath() + ": " + line.charAt(0)
                + " is not a recognized drift change set category code.");
            throw new ChangeSetReaderException(line.charAt(0) + " is not a recognized drift change set category code.");
        } catch (IOException e) {
            log.error("An error ocurred while parsing " + metaDataFile.getAbsolutePath() + ": " + e.getMessage());
            throw new ChangeSetReaderException("An error ocurred while parsing " + metaDataFile.getAbsolutePath(), e);
        }
    }

    @Override
    public void close() throws ChangeSetReaderException {
        try {
            reader.close();
        } catch (IOException e) {
            log
                .warn("An error ocurred while trying to close " + metaDataFile.getAbsolutePath() + ": "
                    + e.getMessage());
            throw new ChangeSetReaderException("An error ocurred while trying to close "
                + metaDataFile.getAbsolutePath(), e);
        }
    }

    @Override
    public Iterator<FileEntry> iterator() {

        return new Iterator<FileEntry>() {

            private FileEntry next;

            {
                try {
                    next = read();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create iterator: " + e);
                }
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public FileEntry next() {
                try {
                    FileEntry previous = next;
                    next = read();
                    if (next == null && closeStream) {
                        close();
                    }
                    return previous;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to get next " + FileEntry.class.getName() + ": " + e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
