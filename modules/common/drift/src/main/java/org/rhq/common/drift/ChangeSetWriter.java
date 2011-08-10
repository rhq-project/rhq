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

import java.io.IOException;

/**
 * Generates a drift change set file. A change set is stored as a UTF-8 text file. It
 * consists of file paths with some associated meta data. Files are grouped by directory.
 * The meta data includes the SHA-256 hash, the old SHA-256 hash (if one exists), the
 * file name, and a single character code indicating whether the file has been added,
 * changed, or removed in this change set. The codes are taken from
 * {@link org.rhq.core.domain.drift.DriftCategory DriftCategory}.
 * <br/>
 * <br/>
 * The current format of the change set file is:
 * <pre>
 * HEADER
 * DRIFT_ENTRY
 *     FILE_ENTRY (1..N)
 * </pre>
 * where HEADER corresponds to {@link Headers} and the header fields are written out in the
 * following order:
 * <ul>
 *   <li>resource id</li>
 *   <li>drift configuration id</li>
 *   <li>drift configuration name</li>
 *   <li>drift configuration base directory</li>
 *   <li>change set type flag indicating whether this is a coverage or drift changeset</li>
 * </ul>
 * <br/>
 * DRIFT_ENTRY has two field that are space delimited. The first is the number of files
 * included in DRIFT_ENTRY. The second is the relative path of the directory, that is
 * <br/>
 * FILE_ENTRY has four, space-delimited fields - type, new_sha, old_sha, file_name.<br/>
 * Here is an example change set:
 *
 * <pre>
 * 1001
 * 2345
 * Core Server JARs
 * /var/lib/myserver
 * 2 lib
 * C 1706b5c18e4358041b463995efc30f8f721766fab0e018d50d85978b46df013c 1536b5c18e4358041b463995efc30f8f721766fab0e018d50d85978b46df013c foo.jar
 * A 2706b5c18e4358041b463995efc30f8f721766fab0e018d50d85978b46df013a 0 bar.jar
 * 1 conf
 * R 0 2206b5af8e4358041b463995efc30f8f721766fab0e018d50d85978b46df013a foo.conf
 * </pre>
 *
 * For the first directory entry, notice that the old_sha field has a value of 0 for bar.jar.
 * This is because it is a new file, and there is no previous SHA-256 hash for the file. For
 * the second directory entry, notice that the new_sha field has a value of 0 for foo.conf.
 * This is because the file has been removed and so there is no new SHA-256 hash.
 */
public interface ChangeSetWriter {

    /**
     *
     * @param dirEntry
     * @throws IOException
     */
    void writeDirectoryEntry(DirectoryEntry dirEntry) throws IOException;

    void close() throws IOException;

}
