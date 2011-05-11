package org.rhq.core.clientapi.server.drift;

import java.io.InputStream;

import org.rhq.core.communications.command.annotation.Asynchronous;

public interface DriftServerService {
    @Asynchronous(guaranteedDelivery = true)
    void uploadSnapshot(int resourceId, long metadataSize, InputStream metadataStream, long dataSize,
        InputStream dataStream);
}
