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

/**
 * Reads change set file in a sequential fashion. Note that class extends {@link Iterable}
 * which means you can iterate over change set files as follows:
 * <pre>
 *     ChangeSetReader reader = ...
 *     for (FileEntry entry : reader) {
 *         ...
 *     }
 * </pre>
 */
public interface ChangeSetReader extends Iterable<FileEntry> {

    /**
     * Returns the headers from change set file. This method can be called multiple times.
     * Given a properly formatted change set file, this method should never return null.
     *
     * @return The change set headers
     *
     * @throws ChangeSetReaderException If an error occurs reading the file or the headers
     * are not formatted correctly.
     */
    Headers getHeaders() throws ChangeSetReaderException;

    /**
     * Read and return the next file entry or null if the end of the file has been reached.
     *
     * @return The next file entry
     *
     * @throws ChangeSetReaderException if an IO error occurs or if the file entry is not
     * properly formatted.
     */
    FileEntry read() throws ChangeSetReaderException;

    /**
     * Closes the reader.
     *
     * @throws ChangeSetReaderException if an IO error occurs
     */
    void close() throws ChangeSetReaderException;

}
