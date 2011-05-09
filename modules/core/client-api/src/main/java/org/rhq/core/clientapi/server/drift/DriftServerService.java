package org.rhq.core.clientapi.server.drift;

public interface DriftServerService {
    void uploadSnapshotReport(SnapshotReport report) throws Exception;
}
