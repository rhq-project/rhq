package org.rhq.common.drift;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class ChangeSetWriterImpl implements ChangeSetWriter {

    private Writer metaDataWriter;

    private File metaDataFile;

    public ChangeSetWriterImpl(File changesetDir, String changesetName) throws IOException {
        metaDataFile = new File(changesetDir, changesetName + "-metadata.txt");
        metaDataWriter = new BufferedWriter(new FileWriter(metaDataFile));
    }

    public void writeDirectoryEntry(DirectoryEntry dirEntry) throws IOException {
        metaDataWriter.write(dirEntry.getDirectory() + " " + dirEntry.getNumberOfFiles() + "\n");
        for (FileEntry entry : dirEntry) {
            switch (entry.getType()) {
                case FILE_ADDED:
                    metaDataWriter.write(entry.getNewSHA() + " 0 " + entry.getFile() + " " + entry.getType().code() +
                        "\n");
                    break;
                case FILE_CHANGED:
                    metaDataWriter.write(entry.getNewSHA() + " " + entry.getOldSHA() + " " + entry.getFile() + " " +
                        entry.getType().code() + "\n");
                    break;
                case FILE_REMOVED:
                    metaDataWriter.write("0 " + entry.getOldSHA() + " " + entry.getFile() + " " +
                        entry.getType().code() + "\n");
                    break;
            }
        }
        metaDataWriter.write("\n");
    }

    File getMetaDataFile() {
        return metaDataFile;
    }

    public void close() throws IOException {
        metaDataWriter.close();
    }
}
