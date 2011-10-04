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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Note that this implementation does not do any validation. It assumes that each file entry
 * contains valid, 64 character SHA-256 hashes. it also assumes paths are not null and that
 * directory entry paths are in relative paths.
 */
public class ChangeSetWriterImpl implements ChangeSetWriter {

    private Writer writer;

    private File changeSetFile;

    public ChangeSetWriterImpl(File changeSetFile, Headers headers)
        throws IOException {

        this.changeSetFile = changeSetFile;
        writer = new BufferedWriter(new FileWriter(this.changeSetFile));

        writeHeaders(headers);
    }

    private void writeHeaders(Headers headers) throws IOException {
        writer.write(headers.getResourceId() + "\n");
        writer.write(headers.getDriftDefinitionId() + "\n");
        writer.write(headers.getDriftDefinitionName() + "\n");
        writer.write(headers.getBasedir() + "\n");
        writer.write(headers.getType().code() + "\n");
        writer.write(headers.getVersion() + "\n");
    }

    public void write(FileEntry entry) throws IOException {
        switch (entry.getType()) {
        case FILE_ADDED:
            writer.write(entry.getType().code() + " " + entry.getNewSHA() + " 0 " + entry.getFile() + "\n");
            break;
        case FILE_CHANGED:
            writer.write(entry.getType().code() + " " + entry.getNewSHA() + " " + entry.getOldSHA() + " " +
                entry.getFile() + "\n");
            break;
        case FILE_REMOVED:
            writer.write(entry.getType().code() + " 0 " + entry.getOldSHA() + " " + entry.getFile() + "\n");
            break;
        }
    }

    File getChangeSetFile() {
        return changeSetFile;
    }

    public void close() throws IOException {
        writer.close();
    }
}
