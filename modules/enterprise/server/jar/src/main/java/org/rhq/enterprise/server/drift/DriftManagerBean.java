package org.rhq.enterprise.server.drift;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.ejb.EJB;

import org.rhq.core.clientapi.server.drift.SnapshotReport;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;

import static org.rhq.enterprise.server.util.LookupUtil.getCoreServer;

@EJB
public class DriftManagerBean implements DriftManagerLocal {
    @EJB
    private AgentManagerLocal agentMgr;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public void uploadSnapshotReport(SnapshotReport report) throws Exception {
        File snapshotsDir = getSnapshotsDir();
        File destDir = new File(snapshotsDir, Integer.toString(report.getResourceId()));
        destDir.mkdir();

        StreamUtil.copy(report.getMetadataInputStream(), new BufferedOutputStream(new FileOutputStream(
            new File(destDir, "data.zip"))));
        StreamUtil.copy(report.getDataInputStream(), new BufferedOutputStream(new FileOutputStream(
            new File(destDir, "metadata.txt"))));
    }

    private File getSnapshotsDir() throws Exception {
        File serverHomeDir = getCoreServer().getJBossServerHomeDir();
        File snapshotsDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads/snapshots");
        if (!snapshotsDir.isDirectory()) {
            snapshotsDir.mkdirs();
            if (!snapshotsDir.isDirectory()) {
                throw new FileNotFoundException("Missing snapshots directory at [" + snapshotsDir + "]");
            }
        }
        return snapshotsDir;
    }
}
