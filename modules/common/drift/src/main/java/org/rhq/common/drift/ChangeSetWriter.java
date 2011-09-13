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
 * Generates a change set file. A change set is stored as a UTF-8 text file. It consists of
 * file paths with some associated meta data. The meta data includes the SHA-256 hash, the
 * old SHA-256 hash (if one exists), and a single character code indicating whether the file
 * has been added, changed, or removed in the change set. The codes are taken from
 * {@link org.rhq.core.domain.drift.DriftCategory DriftCategory}.
 * <br/>
 * <br/>
 * The current format of the change set file is:
 * <pre>
 * HEADERS
 * FILE_ENTRY (1..N)
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
 * <br/>
 * FILE_ENTRY has four, space-delimited fields - type, new_sha, old_sha, file_name.<br/>
 * Here is an example change set:
 *
 * <pre>
 * 1001
 * 2345
 * Core Server JARs
 * /var/lib/myserver
 * C 1706b5c18e4358041b463995efc30f8f721766fab0e018d50d85978b46df013c 1536b5c18e4358041b463995efc30f8f721766fab0e018d50d85978b46df013c lib/foo.jar
 * A 2706b5c18e4358041b463995efc30f8f721766fab0e018d50d85978b46df013a 0 lib/bar.jar
 * R 0 2206b5af8e4358041b463995efc30f8f721766fab0e018d50d85978b46df013a conf/foo.conf
 * </pre>
 *
 * Note that the file paths in each entry are relative to the path in the base directory
 * header. For the entry with a path of lib/bar.jar notice that the old_sha field has a
 * value of 0. This is because it is a new file, and there is no previous SHA-256 hash for
 * the file. For the entry with conf/foo.conf as its path, notices that the new_sha field
 * has a value of 0 for foo.conf. This is because the file has been deleted and so there is
 * no new SHA-256 hash.
 * <p/>
 * Lastly and importantly, the format of this file is still subject to change. Additional
 * headers may be added, maybe allowing for optional headers. The format of the file entry
 * may change as well. Currently a place holder value of 0 is used to indicate the lack
 * of a SHA-256 hash. That place holder is not really needed and may go away.
 */
public interface ChangeSetWriter {

    /**
     *
     * @param entry
     * @throws IOException
     */
    void write(FileEntry entry) throws IOException;

    void close() throws IOException;

}
