package org.rhq.core.pc.drift;

import org.apache.commons.io.DirectoryWalker;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.drift.Snapshot;
import org.rhq.core.util.MessageDigestGenerator;

public class SnapshotGenerator extends DirectoryWalker {

    private MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);


    public Snapshot generateSnapshot(File dir) throws IOException {
        List<File> files = new ArrayList<File>();
        walk(dir, files);

        Snapshot snapshot = new Snapshot();
        Map<String, String> metadata = snapshot.getMetadata();

        for (File file : files) {
            metadata.put(file.getPath(), digestGenerator.calcDigestString(file.toURI().toURL()));
        }

        return snapshot;
    }

    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
        return true;
    }

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        results.add(file);
    }

}
