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

import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_CHANGED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_REMOVED;

import java.io.Serializable;
import java.util.Date;

import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.util.file.FileUtil;

public class FileEntry implements Serializable, Comparable<FileEntry> {

    private static final long serialVersionUID = 1L;

    public static FileEntry removedFileEntry(String file, String sha) {
        FileEntry entry = new FileEntry();
        entry.file = FileUtil.useForwardSlash(file);
        entry.oldSHA = sha;
        entry.newSHA = "0";
        entry.type = FILE_REMOVED;
        entry.lastModified = -1L;
        entry.size = -1L;

        return entry;
    }

    public static FileEntry addedFileEntry(String file, String sha, Long lastModified, Long size) {
        FileEntry entry = new FileEntry();
        entry.file = FileUtil.useForwardSlash(file);
        entry.oldSHA = "0";
        entry.newSHA = sha;
        entry.type = FILE_ADDED;
        entry.lastModified = lastModified;
        entry.size = size;

        return entry;
    }

    public static FileEntry changedFileEntry(String file, String oldSHA, String newSHA, Long lastModified, Long size) {
        FileEntry entry = new FileEntry();
        entry.file = FileUtil.useForwardSlash(file);
        entry.oldSHA = oldSHA;
        entry.newSHA = newSHA;
        entry.type = FILE_CHANGED;
        entry.lastModified = lastModified;
        entry.size = size;

        return entry;
    }

    private String file;

    private String oldSHA;

    private String newSHA;

    private DriftCategory type;

    private Long lastModified;

    private Long size;

    private FileEntry() {
    }

    public FileEntry(String newSHA, String oldSHA, String file, String type, Long lastModified, Long size) {
        this.newSHA = newSHA;
        this.oldSHA = oldSHA;
        this.file = file;
        this.type = DriftCategory.fromCode(type);
        this.lastModified = lastModified;
        this.size = size;
    }

    public String getFile() {
        return file;
    }

    public String getOldSHA() {
        return oldSHA;
    }

    public String getNewSHA() {
        return newSHA;
    }

    public DriftCategory getType() {
        return type;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public Long getSize() {
        return size;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[newSHA: " + newSHA + ", oldSHA: " + oldSHA + ", file: " + file
            + ", type: " + type.code() + ", lastModified: " + new Date(lastModified) + ", size: " + size + "]";
    }

    // Support Sets and Ordering by deferring to a String compare on the file path
    @Override
    public int compareTo(FileEntry o) {
        return this.file.compareTo(o.getFile());
    }
}
