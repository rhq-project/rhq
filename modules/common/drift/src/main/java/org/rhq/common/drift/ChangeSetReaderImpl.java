package org.rhq.common.drift;

import static java.lang.Integer.parseInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.drift.DriftChangeSetCategory;

public class ChangeSetReaderImpl implements ChangeSetReader {

    private Pattern DIRECTORY_PATTERN = Pattern.compile("^\\s*(.*?)\\s+(\\d+)\\s*$");
    private BufferedReader reader;

    private File metaDataFile;

    private Headers headers;

    public ChangeSetReaderImpl(File metaDataFile) throws IOException {
        this.metaDataFile = metaDataFile;
        reader = new BufferedReader(new FileReader(this.metaDataFile));
        readHeaders();
    }

    public ChangeSetReaderImpl(Reader metaDataFile) throws Exception {
        reader = new BufferedReader(metaDataFile);
        readHeaders();
    }

    private void readHeaders() throws IOException {
        String name = reader.readLine();
        String basedir = reader.readLine();
        DriftChangeSetCategory type = DriftChangeSetCategory.fromCode(reader.readLine());

        headers = new Headers(name, basedir, type);
    }

    @Override
    public Headers getHeaders() throws IOException {
        return headers;
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

    @Override
    public Iterator<DirectoryEntry> iterator() {

        return new Iterator<DirectoryEntry>() {

            private DirectoryEntry next;

            {
                try {
                    next = readDirectoryEntry();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create iterator: " + e);
                }
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public DirectoryEntry next() {
                try {
                    DirectoryEntry previous = next;
                    next = readDirectoryEntry();
                    return previous;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to get next " + DirectoryEntry.class.getName() + ": " + e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
