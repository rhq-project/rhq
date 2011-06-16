package org.rhq.common.drift;

import static java.lang.Integer.parseInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;

public class ChangeSetReaderImpl implements ChangeSetReader {

    private Pattern DIRECTORY_PATTERN = Pattern.compile("^\\s*(.*?)\\s+(\\d+)\\s*$");
    private BufferedReader reader;

    private File metaDataFile;

    public ChangeSetReaderImpl(File metaDataFile) throws IOException {
        this.metaDataFile = metaDataFile;
        reader = new BufferedReader(new FileReader(this.metaDataFile));
    }

    public ChangeSetReaderImpl(Reader metaDataFile) throws Exception {
        reader = new BufferedReader(metaDataFile);
    }

    @Override
    public DirectoryEntry readDirectoryEntry() throws IOException {
        String line = reader.readLine();
        if (null == line) {
            return null;
        }

        Matcher m = DIRECTORY_PATTERN.matcher(line);
        if (!m.matches() || 2 != m.groupCount()) {
            LogFactory.getLog(ChangeSetReaderImpl.class).error("Unexpected directory line, returning null on: " + line);
            return null;
        }

        DirectoryEntry dirEntry = new DirectoryEntry(m.group(1));
        int numFiles = parseInt(m.group(2));

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
