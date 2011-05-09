package org.rhq.enterprise.server.drift;

import javax.ejb.Local;

import org.rhq.core.clientapi.server.drift.SnapshotReport;

@Local
public interface DriftManagerLocal {
    void uploadSnapshotReport(SnapshotReport report) throws Exception;
}
