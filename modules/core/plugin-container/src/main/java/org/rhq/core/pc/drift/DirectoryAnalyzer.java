package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.core.util.MessageDigestGenerator;

import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;

public class DirectoryAnalyzer {

    private File basedir;

    private DirectoryEntry dirEntry;

    private List<FileEntry> filesAdded = new ArrayList<FileEntry>();

    private List<FileEntry> filesRemoved = new ArrayList<FileEntry>();

    private List<FileEntry> filesChanged = new ArrayList<FileEntry>();

    private MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    public DirectoryAnalyzer(File basedir, DirectoryEntry directoryEntry) {
        this.basedir = basedir;
        dirEntry = directoryEntry;
    }

    public List<FileEntry> getFilesAdded() {
        return filesAdded;
    }

    public List<FileEntry> getFilesRemoved() {
        return filesRemoved;
    }

    public List<FileEntry> getFilesChanged() {
        return filesChanged;
    }

    public void run() throws IOException {
        File dir = new File(basedir, dirEntry.getDirectory());
        Set<String> files = fileNames(dir.listFiles());
        Map<String, FileEntry> fileEntries = createFileEntriesMap();

        Set<String> dirEntryFileNames = dirEntryFileNames();

        for (String file : files) {
            if (!fileEntries.containsKey(file)) {
                filesAdded.add(addedFileEntry(file, sha256(new File(dir, file))));
            }
        }

        for (String file : fileEntries.keySet()) {
            if (!files.contains(file)) {
                filesRemoved.add(removedFileEntry(file, fileEntries.get(file).getNewSHA()));
            }
        }
    }

    private Set<String> fileNames(File... files) {
        Set<String> set = new TreeSet<String>();
        for (File file : files) {
            set.add(file.getName());
        }
        return set;
    }

    private Map<String, FileEntry> createFileEntriesMap() {
        Map<String, FileEntry> map = new TreeMap<String, FileEntry>();
        for (FileEntry entry : dirEntry) {
            map.put(entry.getFile(), entry);
        }
        return map;
    }

    private Set<String> dirEntryFileNames() {
        Set<String> set = new TreeSet<String>();
        for (FileEntry entry : dirEntry) {
            set.add(entry.getFile());
        }
        return set;
    }

    private String sha256(File file) throws IOException {
        return digestGenerator.calcDigestString(file);
    }

}
