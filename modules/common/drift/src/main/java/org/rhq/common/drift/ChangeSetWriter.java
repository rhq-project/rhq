package org.rhq.common.drift;

import java.io.IOException;

/**
 * Generates a drift change set file. A change set is stored as a UTF-8 text file. It
 * consists of file paths with some associated meta data. Files are grouped by directory.
 * The meta data includes the SHA-256 hash, the old SHA-256 hash (if one exists), the
 * file name, and a single character code indicating whether the file has been added,
 * changed, or removed in this change set. The codes are taken from
 * {@link org.rhq.core.domain.drift.DriftCategory}.
 * <br/>
 * <br/>
 * The current format of the change set file is:
 * <pre>
 * HEADER
 * DRIFT_ENTRY
 *     FILE_ENTRY (1..N)
 * EMPTY_LINE
 * </pre>
 * where HEADER has three fields that are terminated by newline characters. Those fields are:
 * <ul>
 *   <li>drift configuration name</li>
 *   <li>drift configuration base directory</li>
 *   <li>flag indicating whether or not this is a coverage changeset</li>
 * </ul>
 * <br/>
 * and where DRIFT_ENTRY has two field that are space delimited. The first is the directory path
 * and the second is the number of files included in DRIFT_ENTRY. Note that the last FILE_ENTRY
 * in DRIFT_ENTRY is followed by a new line.
 * <br/>
 * FILE_ENTRY has four, space-delimited fields - new_sha, old_sha, file_name, type.<br/>
 * Here is an example change set:
 *
 * <pre>
 * /var/lib/myserver/lib 2
 * 2c4edd 5bd37a foo.jar C
 * d72c54 0 bar.jar A
 *
 * /var/lib/myserver/conf 1
 * 0 c31d46 foo.conf R
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
