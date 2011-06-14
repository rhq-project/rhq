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
        for (String file : dirEntry) {
            metaDataWriter.write(file + "\n");
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
