package org.rhq.core.pc.drift;

import java.io.File;
import java.io.Serializable;

/**
 * A SnapshotHandle is a handle or pointer to a snapshot on disk. It does not contain an actual Snapshot object.
 */
public class SnapshotHandle implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The path of the snapshot meta data file */
    private File metadataFile;

    /** The path of the snapshot data file */
    private File dataFile;

    public SnapshotHandle(File dataFile, File metadataFile) {
        this.dataFile = dataFile;
        this.metadataFile = metadataFile;
    }

    /** @return The path of the snapshot data file */
    public File getDataFile() {
        return dataFile;
    }

    /** @return The path of the snapshot meta data file */
    public File getMetadataFile() {
        return metadataFile;
    }
}
