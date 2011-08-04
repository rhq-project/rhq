package org.rhq.common.drift;

import java.io.Serializable;

import org.rhq.core.domain.drift.DriftCategory;

import static org.rhq.core.domain.drift.DriftCategory.*;
import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_CHANGED;

public class FileEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    public static FileEntry removedFileEntry(String file, String sha) {
        FileEntry entry = new FileEntry();
        entry.file = file;
        entry.oldSHA = sha;
        entry.newSHA = "0";
        entry.type = FILE_REMOVED;

        return entry;
    }

    public static FileEntry addedFileEntry(String file, String sha) {
        FileEntry entry = new FileEntry();
        entry.file = file;
        entry.oldSHA = "0";
        entry.newSHA = sha;
        entry.type = FILE_ADDED;

        return entry;
    }

    public static FileEntry changedFileEntry(String file, String oldSHA, String newSHA) {
        FileEntry entry = new FileEntry();
        entry.file = file;
        entry.oldSHA = oldSHA;
        entry.newSHA = newSHA;
        entry.type = FILE_CHANGED;

        return entry;
    }

    private String file;

    private String oldSHA;

    private String newSHA;

    private DriftCategory type;

    private FileEntry() {
    }

    public FileEntry(String newSHA, String oldSHA, String file, String type) {
        this.newSHA = newSHA;
        this.oldSHA = oldSHA;
        this.file = file;
        this.type = DriftCategory.fromCode(type);
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[newSHA: " + newSHA + ", oldSHA: " + oldSHA + ", file: " + file +
            ", type: " + type.code() + "]";
    }
}
