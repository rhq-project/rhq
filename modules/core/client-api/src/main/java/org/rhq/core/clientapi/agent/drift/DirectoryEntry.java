package org.rhq.core.clientapi.agent.drift;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DirectoryEntry implements Iterable<String> {

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
