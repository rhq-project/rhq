package org.rhq.enterprise.server.drift;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;

import static org.rhq.enterprise.server.util.LookupUtil.getCoreServer;

@Stateless
public class DriftManagerBean implements DriftManagerLocal {
    @EJB
    private AgentManagerLocal agentMgr;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public void uploadSnapshot(int resourceId, long metadataSize, InputStream metadataStream, long dataSize,
        InputStream dataStream) throws Exception {
        File snapshotsDir = getSnapshotsDir();
        File destDir = new File(snapshotsDir, Integer.toString(resourceId));
        destDir.mkdir();

        StreamUtil.copy(metadataStream, new BufferedOutputStream(new FileOutputStream(
            new File(destDir, "metadata.txt"))), false);
        StreamUtil.copy(dataStream, new BufferedOutputStream(new FileOutputStream(
            new File(destDir, "data.zip"))), false);
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
