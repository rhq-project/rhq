package org.rhq.common.drift;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;

public class ChangeSetWriterImpl implements ChangeSetWriter {

    private Writer writer;

    private File changeSetFile;

    public ChangeSetWriterImpl(File changeSetFile, DriftConfiguration driftConfig, DriftChangeSetCategory type)
        throws IOException {

        this.changeSetFile = changeSetFile;
        writer = new BufferedWriter(new FileWriter(this.changeSetFile));

        writeHeader(driftConfig, type);
    }

    private void writeHeader(DriftConfiguration driftConfig, DriftChangeSetCategory type) throws IOException {
        writer.write(driftConfig.getName() + "\n");
        writer.write(driftConfig.getBasedir() + "\n");
        writer.write(type.code() + "\n");
    }

    public void writeDirectoryEntry(DirectoryEntry dirEntry) throws IOException {
        writer.write(dirEntry.getDirectory() + " " + dirEntry.getNumberOfFiles() + "\n");
        for (FileEntry entry : dirEntry) {
            switch (entry.getType()) {
                case FILE_ADDED:
                    writer.write(entry.getNewSHA() + " 0 " + entry.getFile() + " " + entry.getType().code() +
                        "\n");
                    break;
                case FILE_CHANGED:
                    writer.write(entry.getNewSHA() + " " + entry.getOldSHA() + " " + entry.getFile() + " " +
                        entry.getType().code() + "\n");
                    break;
                case FILE_REMOVED:
                    writer.write("0 " + entry.getOldSHA() + " " + entry.getFile() + " " +
                        entry.getType().code() + "\n");
                    break;
            }
        }
        writer.write("\n");
    }

    File getChangeSetFile() {
        return changeSetFile;
    }

    public void close() throws IOException {
        writer.close();
    }
}
