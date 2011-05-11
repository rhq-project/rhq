package org.rhq.enterprise.server.drift;

import java.io.InputStream;

import org.rhq.core.clientapi.server.drift.DriftServerService;

import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;

public class DriftServerServiceImpl implements DriftServerService {
    @Override
    public void uploadSnapshot(int resourceId, long metadataSize, InputStream metadataStream, long dataSize,
        InputStream dataStream) {
        try {
            DriftManagerLocal driftMgr = getDriftManager();
            driftMgr.uploadSnapshot(resourceId, metadataSize, metadataStream, dataSize, dataStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
