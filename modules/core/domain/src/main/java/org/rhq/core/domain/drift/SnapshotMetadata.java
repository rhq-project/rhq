package org.rhq.core.domain.drift;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SnapshotMetadata {

    private File basedir;

    private Map<String, String> metadata = new HashMap<String, String>();

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File dir) {
        basedir = dir;
    }

    public void put(String relativePath, String hash) {
        metadata.put(relativePath, hash);
    }

}
