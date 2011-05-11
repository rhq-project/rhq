package org.rhq.enterprise.server.drift;

import java.io.InputStream;

import javax.ejb.Local;

import org.rhq.core.clientapi.server.drift.SnapshotReport;

@Local
public interface DriftManagerLocal {
    void uploadSnapshot(int resourceId, long metadataSize, InputStream metadataStream, long dataSize,
        InputStream dataStream) throws Exception;
}
