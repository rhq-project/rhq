package org.rhq.common.drift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import static java.lang.Integer.parseInt;

public class ChangeSetReaderImpl implements ChangeSetReader {

    private BufferedReader reader;

    private File metaDataFile;

    public ChangeSetReaderImpl(File metaDataFile) throws IOException {
        this.metaDataFile = metaDataFile;
        reader = new BufferedReader(new FileReader(this.metaDataFile));
    }

    ChangeSetReaderImpl(Reader metaDataFile) throws Exception {
        reader = new BufferedReader(metaDataFile);
    }

    @Override
    public DirectoryEntry readDirectoryEntry() throws IOException {
        String[] fields = reader.readLine().split(" ");
        DirectoryEntry dirEntry = new DirectoryEntry(fields[0]);
        int numFiles = parseInt(fields[1]);

        for (int i = 0; i < numFiles; ++i) {
            String[] cols = reader.readLine().split(" ");
            dirEntry.add(new FileEntry(cols[0], cols[1], cols[2], cols[3]));
        }
        // Reader the blank line that follows. Not sure if the blank line is needed since
        // the directory entry header specifies the number of lines in entry
        reader.readLine();

        return dirEntry;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
