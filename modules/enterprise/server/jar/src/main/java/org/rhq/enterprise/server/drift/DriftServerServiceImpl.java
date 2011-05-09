package org.rhq.enterprise.server.drift;

import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.clientapi.server.drift.SnapshotReport;
import org.rhq.enterprise.server.util.LookupUtil;

import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;

public class DriftServerServiceImpl implements DriftServerService {
    @Override
    public void uploadSnapshotReport(SnapshotReport report) throws Exception {
        DriftManagerLocal driftMgr = getDriftManager();
        driftMgr.uploadSnapshotReport(report);
    }
}
