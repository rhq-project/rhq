package org.rhq.core.pc.drift;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import org.rhq.core.clientapi.server.drift.SnapshotReport;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;

public class DriftManager extends AgentService implements ContainerService {
    private PluginContainerConfiguration pluginContainerConfiguration;

    private File snapshotsDir;

    public DriftManager() {
        super(DriftAgentService.class);
    }

    @Override
    public void setConfiguration(PluginContainerConfiguration configuration) {
        pluginContainerConfiguration = configuration;
        snapshotsDir = new File(pluginContainerConfiguration.getDataDirectory(), "snapshots");
        snapshotsDir.mkdir();
    }

    @Override
    public void initialize() {
    }

    @Override
    public void shutdown() {
    }

    public SnapshotHandle generateSnapshot(int resourceId, File basedir) {
        SnapshotGenerator generator = new SnapshotGenerator();
        generator.setSnapshotDir(snapshotsDir);
        return generateSnapshot(resourceId, basedir);
    }

    public void sendSnapshotReport(int resourceId, SnapshotHandle handle) throws Exception {
        SnapshotReport report = new SnapshotReport();
        report.setMetadataSize(handle.getMetadataFile().length());
        report.setDataSize(handle.getDataFile().length());
        report.setMetadataInputStream(remoteInputStream(new BufferedInputStream(new FileInputStream(
            handle.getMetadataFile()))));
        report.setDataInputStream(remoteInputStream(new BufferedInputStream(new FileInputStream(
            handle.getDataFile()))));
        report.setResourceId(resourceId);
    }
}
