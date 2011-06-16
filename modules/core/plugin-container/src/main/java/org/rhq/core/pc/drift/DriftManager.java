package org.rhq.core.pc.drift;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;

public class DriftManager extends AgentService implements DriftAgentService, ContainerService {

    private final Log log = LogFactory.getLog(DriftManager.class);

    private PluginContainerConfiguration pluginContainerConfiguration;

    private File snapshotsDir;

    private ScheduledThreadPoolExecutor driftThreadPool;

    private ScheduleQueue schedulesQueue = new ScheduleQueueImpl();

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
        DriftDetector driftDetector = new DriftDetector();
        driftDetector.setScheduleQueue(schedulesQueue);
        //driftDetector.setChangeSetManager();

        driftThreadPool = new ScheduledThreadPoolExecutor(5);
        driftThreadPool.scheduleAtFixedRate(new DriftDetector(), 30, 1800, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        driftThreadPool.shutdown();
        driftThreadPool = null;

        schedulesQueue.clear();
        schedulesQueue = null;
    }

    @Override
    public void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
        schedulesQueue.enqueue(new DriftDetectionSchedule(resourceId, driftConfiguration));
    }

    public void sendSnapshotReport(int resourceId, SnapshotHandle handle) throws Exception {
        DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();

        driftServer.sendChangesetZip(resourceId, handle.getMetadataFile().length(),
            remoteInputStream(new BufferedInputStream(new FileInputStream(handle.getMetadataFile()))));
    }

    @Override
    public boolean requestDriftFiles(List<DriftFile> driftFiles) {
        // TODO Auto-generated method stub
        return false;
    }

}
