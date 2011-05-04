package org.rhq.core.domain.drift;

import java.util.HashMap;
import java.util.Map;

public class Snapshot {

    private Map<String, String> metadata = new HashMap<String, String>();

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

}
