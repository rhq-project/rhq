package org.rhq.common.drift;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DirectoryEntry implements Iterable<String>, Serializable {

    private static final long serialVersionUID = 1L;

    private String directory;

    private List<String> files = new ArrayList<String>();

    public DirectoryEntry(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }

    public DirectoryEntry add(String file) {
        files.add(file);
        return this;
    }

    public int getNumberOfFiles() {
        return files.size();
    }

    public Iterator<String> iterator() {
        return files.iterator();
    }
}
