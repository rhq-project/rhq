package org.rhq.core.domain.drift;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Snapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private SnapshotMetadata metadata = new SnapshotMetadata();

    private InputStream data;

    public SnapshotMetadata getMetadata() {
        return metadata;
    }

    public InputStream getData() {
        return data;
    }

    public void setData(InputStream data) {
        this.data = data;
    }
}
