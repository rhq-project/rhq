/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.core.pc.drift;

import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftComplianceStatus.OUT_OF_COMPLIANCE_NO_BASEDIR;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.measurement.MeasurementManager;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

public class DriftManager extends AgentService implements DriftAgentService, DriftClient, ContainerService {

    private final Log log = LogFactory.getLog(DriftManager.class);

    private PluginContainerConfiguration pluginContainerConfiguration;

    private File changeSetsDir;

    private ScheduledThreadPoolExecutor driftThreadPool;

    private ScheduleQueue schedulesQueue = new ScheduleQueueImpl();

    private ChangeSetManager changeSetMgr;

    private boolean initialized;

    public DriftManager() {
        super(DriftAgentService.class);
    }

    @Override
    public void setConfiguration(PluginContainerConfiguration configuration) {
        pluginContainerConfiguration = configuration;
        changeSetsDir = new File(pluginContainerConfiguration.getDataDirectory(), "changesets");
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize() {
        long initStartTime = System.currentTimeMillis();
        if (!changeSetsDir.isDirectory()) {
            boolean success = changeSetsDir.mkdir();
            if (!success) {
                log.warn("Could not create change sets directory " + changeSetsDir);
                initialized = false;
                return;
            }
        }
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);

        DriftDetector driftDetector = new DriftDetector();
        driftDetector.setScheduleQueue(schedulesQueue);
        driftDetector.setChangeSetManager(changeSetMgr);
        driftDetector.setDriftClient(this);

        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        long startTime = System.currentTimeMillis();
        initSchedules(inventoryMgr.getPlatform(), inventoryMgr);
        long endTime = System.currentTimeMillis();

        if (log.isInfoEnabled()) {
            log.info("Finished initializing drift detection schedules in " + (endTime - startTime) + " ms");
        }

        scanForContentToResend();
        purgeDeletedDriftDefDirs();

        driftThreadPool = new ScheduledThreadPoolExecutor(5);

        long initialDelay = pluginContainerConfiguration.getDriftDetectionInitialDelay();
        long period = pluginContainerConfiguration.getDriftDetectionPeriod();
        if (period > 0) {
            // note that drift detection is globally disabled if the detection period is 0 or less
            driftThreadPool.scheduleAtFixedRate(driftDetector, initialDelay, period, TimeUnit.SECONDS);
        } else {
            log.info("Drift detection has been globally disabled as per plugin container configuration");
        }

        initialized = true;
        long initEndTime = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            log.info("Finished initialization in " + (initEndTime - initStartTime) + " ms");
        }
    }

    private void initSchedules(Resource r, InventoryManager inventoryMgr) {
        if (r.getId() == 0) {
            log.debug("Will not reschedule drift detection for " + r + ". It is not sync'ed yet.");
            return;
        }

        ResourceContainer container = inventoryMgr.getResourceContainer(r.getId());
        if (container == null) {
            log.debug("No resource container found for " + r + ". Unable to reschedule drift detection schedules.");
            return;
        }

        log.debug("Rescheduling drift detection for " + r);
        for (DriftDefinition d : container.getDriftDefinitions()) {
            try {
                syncWithServer(r, d);
                schedulesQueue.addSchedule(new DriftDetectionSchedule(r.getId(), d));

            } catch (Throwable t) {
                // catch throwable, don't prevent agent startup just due to a bad definition
                log.error("Failed to sync with server for " + toString(r.getId(), d) + ". Drift detection will not be "
                    + "scheduled.", t);
            }
        }

        for (Resource child : r.getChildResources()) {
            initSchedules(child, inventoryMgr);
        }
    }

    private void syncWithServer(Resource resource, DriftDefinition driftDefinition) throws IOException {
        Headers headers = createHeaders(resource.getId(), driftDefinition);
        if (!changeSetMgr.changeSetExists(resource.getId(), headers)) {
            log.info("No snapshot found for " + toString(resource.getId(), driftDefinition)
                + ". Downloading snapshot from server");
            DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();

            DriftSnapshot snapshot = driftServer.getCurrentSnapshot(driftDefinition.getId());

            if (snapshot.getVersion() == -1) {
                // A version of -1 indicates that no change sets have been reported
                // for this definition. This can occur when a user creates a
                // drift definition while the agent is offline for example. At
                // this point we just return and allow the agent to generate the
                // initial snapshot file.
                if (log.isDebugEnabled()) {
                    log.debug("The server does not have any change sets for "
                        + toString(resource.getId(), driftDefinition) + ". An initial snapshot needs to be generated.");
                }
                return;
            }

            headers.setVersion(snapshot.getVersion());

            log.info("Preparing to write snapshot at version " + snapshot.getVersion() + " to disk for "
                + toString(resource.getId(), driftDefinition));
            File currentSnapshotFile = changeSetMgr
                .findChangeSet(resource.getId(), driftDefinition.getName(), COVERAGE);
            writeSnapshotToFile(snapshot, currentSnapshotFile, headers);

            if (driftDefinition.isPinned()) {
                log.debug(driftDefinition + " is pinned. Fetching pinned snapshot...");
                // The pinned snapshot is always the initial change set and only the initial
                // change set.
                DriftSnapshot pinnedSnapshot = driftServer.getSnapshot(driftDefinition.getId(), 0, 0);
                Headers pinnedHeaders = createHeaders(resource.getId(), driftDefinition);
                File pinnedSnapshotFile = new File(currentSnapshotFile.getParent(), DriftDetector.FILE_SNAPSHOT_PINNED);
                log.info("Preparing to write pinned snapshot to disk for "
                    + toString(resource.getId(), driftDefinition));
                writeSnapshotToFile(pinnedSnapshot, pinnedSnapshotFile, pinnedHeaders);

                if (snapshot.getVersion() > 0) {
                    // Drift was previously reported. We will fetch a snapshot of the
                    // latest change set and write that to disk so that we avoid reporting
                    // drift that has already been reported to the server.
                    DriftSnapshot deltaSnapshot = driftServer.getSnapshot(driftDefinition.getId(), snapshot
                        .getVersion(), snapshot.getVersion());
                    File deltaFile = new File(currentSnapshotFile.getParentFile(), DriftDetector.FILE_CHANGESET_DELTA);
                    Headers deltaHeaders = createHeaders(resource.getId(), driftDefinition);
                    deltaHeaders.setVersion(snapshot.getVersion());
                    deltaHeaders.setType(DRIFT);
                    writeSnapshotToFile(deltaSnapshot, deltaFile, deltaHeaders);
                }
            }
        }
    }

    private void purgeDeletedDriftDefDirs() {
        log.info("Checking for deleted drift definitions");
        for (File resourceDir : changeSetsDir.listFiles()) {
            int resourceId = Integer.parseInt(resourceDir.getName());
            for (File defDir : resourceDir.listFiles()) {
                DriftDefinition driftDef = new DriftDefinition(new Configuration());
                driftDef.setName(defDir.getName());
                if (!schedulesQueue.contains(resourceId, driftDef)) {
                    log.info("Detected deleted drift definition, DriftDefinition[name: " + driftDef.getName()
                        + ", resourceId: " + resourceId + "]");
                    log.info("Deleting drift definition directory " + defDir.getPath());
                    FileUtil.purge(defDir, true);
                }
            }
        }
    }

    /**
     * Scans the changesets directory for any change set content zip files. This method
     * assumes that any content zip files found have not been received or persisted by the
     * server. Each content zip file is resent to the server.
     */
    public void scanForContentToResend() {
        log.info("Scanning for change set content to resend...");
        File[] files = changeSetsDir.listFiles();
        if (files == null) {
            return;
        }
        for (File resourceDir : files) {
            for (File defDir : resourceDir.listFiles()) {
                for (File contentZipFile : defDir.listFiles(new ZipFileNameFilter("content_"))) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resending " + contentZipFile.getPath());
                    }
                    sendChangeSetContentToServer(Integer.parseInt(resourceDir.getName()), defDir.getName(),
                            contentZipFile);
                }
            }
        }
    }

    /**
     * This method is provided as a test hook.
     *
     * @param changeSetMgr
     */
    void setChangeSetMgr(ChangeSetManager changeSetMgr) {
        this.changeSetMgr = changeSetMgr;
    }

    /**
     * This method is provided as a test hook.
     * @return The schedule queue
     */
    public ScheduleQueue getSchedulesQueue() {
        return schedulesQueue;
    }

    @Override
    public void shutdown() {
        if (driftThreadPool != null) {
            PluginContainer pluginContainer = PluginContainer.getInstance();
            // TODO (ips, 04/30/12): Is it safe to pass true here to interrupt executing threads?
            pluginContainer.shutdownExecutorService(driftThreadPool, false);
            driftThreadPool = null;
        }

        if (schedulesQueue != null) {
            schedulesQueue.clear();
            schedulesQueue = null;
        }

        changeSetMgr = null;
    }

    @Override
    public void sendChangeSetToServer(DriftDetectionSummary detectionSummary) {
        int resourceId = detectionSummary.getSchedule().getResourceId();
        DriftDefinition driftDefinition = detectionSummary.getSchedule().getDriftDefinition();

        if (!schedulesQueue.contains(resourceId, driftDefinition)) {
            return;
        }

        File changeSetFile;
        if (detectionSummary.getType() == COVERAGE) {
            changeSetFile = detectionSummary.getNewSnapshot();
        } else {
            changeSetFile = detectionSummary.getDriftChangeSet();
        }
        if (changeSetFile == null) {
            log.warn("changeset[resourceId: " + resourceId + ", driftDefinition: " + driftDefinition.getName()
                + "] was not found. Cancelling request to send change set " + "to server");
            return;
        }

        DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();

        String fileName = "changeset_" + System.currentTimeMillis() + ".zip";
        final File zipFile = new File(changeSetFile.getParentFile(), fileName);

        try {
            ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            FileInputStream fis = new FileInputStream(changeSetFile);
            stream.putNextEntry(new ZipEntry(changeSetFile.getName()));
            StreamUtil.copy(fis, stream, true);
        } catch (IOException e) {
            zipFile.delete();
            throw new DriftDetectionException("Failed to create change set zip file " + zipFile.getPath(), e);
            //        } finally {
            //            try {
            //                if (stream != null) {
            //                    stream.close();
            //                }
            //            } catch (IOException e) {
            //                log.warn("An error occurred while trying to close change set zip file output stream", e);
            //            }
        }

        try {
            driftServer.sendChangesetZip(resourceId, zipFile.length(), remoteInputStream(new BufferedInputStream(
                new FileInputStream(zipFile))));
        } catch (IOException e) {
            throw new DriftDetectionException("Failed to set change set for " + toString(resourceId, driftDefinition)
                + " to server");
        } catch (RuntimeException e) {
            throw new DriftDetectionException("Failed to set change set for " + toString(resourceId, driftDefinition)
                + " to server");
        }
    }

    @Override
    public void sendChangeSetContentToServer(int resourceId, String driftDefName, File contentZipFile) {
        try {
            int startIndex = "content_".length();
            int endIndex = contentZipFile.getName().indexOf(".");
            String token = contentZipFile.getName().substring(startIndex, endIndex);

            DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();
            driftServer.sendFilesZip(resourceId, driftDefName, token, contentZipFile.length(),
                    remoteInputStream(new BufferedInputStream(new FileInputStream(contentZipFile))));
        } catch (FileNotFoundException e) {
            log.error("An error occurred while trying to send change set content zip file " + contentZipFile.getPath()
                    + " to server.", e);
        }
    }

    @Override
    public void repeatChangeSet(int resourceId, String driftDefName, int version) {
        DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();
        driftServer.repeatChangeSet(resourceId, driftDefName, version);
    }

    @Override
    public void detectDrift(int resourceId, DriftDefinition driftDefinition) {
        if (log.isInfoEnabled()) {
            log.info("Received request to schedule drift detection immediately for [resourceId: " + resourceId
                + ", driftDefinitionId: " + driftDefinition.getId() + ", driftDefinitionName: "
                + driftDefinition.getName() + "]");
        }

        DriftDetectionSchedule schedule = schedulesQueue.remove(resourceId, driftDefinition);
        if (schedule == null) {
            log.warn("No schedule found in the queue for [resourceId: " + resourceId + ", driftDefinitionId: "
                + driftDefinition.getId() + ", driftDefinitionName: " + driftDefinition.getName() + "]. No "
                + " work will be scheduled.");
            return;
        }
        log.debug("Resetting " + schedule + " for immediate detection.");
        schedule.resetSchedule();
        boolean queueUpdated = schedulesQueue.addSchedule(schedule);

        if (queueUpdated) {
            if (log.isDebugEnabled()) {
                log.debug(schedule + " has been added to " + schedulesQueue + " for immediate detection.");
            }
        } else {
            log.warn("Failed to add " + schedule + " to " + schedulesQueue + " for immediate detection.");
        }
    }

    @Override
    public void scheduleDriftDetection(int resourceId, DriftDefinition driftDefinition) {
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId, driftDefinition);
        if (log.isInfoEnabled()) {
            log.info("Scheduling drift detection for " + schedule);
        }
        boolean added = schedulesQueue.addSchedule(schedule);

        if (added) {
            if (log.isDebugEnabled()) {
                log.debug(schedule + " has been added to " + schedulesQueue);
            }
            ResourceContainer container = getInventoryManager().getResourceContainer(resourceId);
            if (container != null) {
                container.addDriftDefinition(driftDefinition);
            }
        } else {
            log.warn("Failed to add " + schedule + " to " + schedulesQueue);
        }
    }

    @Override
    public boolean requestDriftFiles(int resourceId, Headers headers, List<? extends DriftFile> driftFiles) {
        if (log.isInfoEnabled()) {
            log.info("Server is requesting files for [resourceId: " + resourceId + ", driftDefinitionId: "
                + headers.getDriftDefinitionId() + ", driftDefinitionName: " + headers.getDriftDefinitionName() + "]");
        }
        DriftFilesSender sender = new DriftFilesSender();
        sender.setResourceId(resourceId);
        sender.setDriftClient(this);
        sender.setDriftFiles(driftFiles);
        sender.setHeaders(headers);
        sender.setChangeSetManager(changeSetMgr);

        driftThreadPool.execute(sender);

        return true;
    }

    @Override
    public void unscheduleDriftDetection(final int resourceId, final DriftDefinition driftDefinition) {
        log.info("Received request to unschedule drift detection for [resourceId:" + resourceId
            + ", driftDefinitionId: " + driftDefinition.getId() + ", driftDefinitionName: " + driftDefinition.getName()
            + "].");

        DriftDetectionSchedule schedule = schedulesQueue.removeAndExecute(resourceId, driftDefinition, new Runnable() {
            @Override
            public void run() {
                File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
                File changeSetDir = new File(resourceDir, driftDefinition.getName());
                FileUtil.purge(changeSetDir, true);

                log.debug("Removed change set directory " + changeSetDir.getAbsolutePath());
            }
        });
        if (schedule != null) {
            ResourceContainer container = getInventoryManager().getResourceContainer(resourceId);
            if (container != null) {
                container.removeDriftDefinition(schedule.getDriftDefinition());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Removed " + schedule + " from the queue " + schedulesQueue);
        }

    }

    @Override
    public void updateDriftDetection(int resourceId, DriftDefinition driftDefinition) {
        log.info("Recived request to update schedule for " + toString(resourceId, driftDefinition));

        DriftDetectionSchedule updatedSchedule = schedulesQueue.update(resourceId, driftDefinition);
        if (updatedSchedule == null) {
            updatedSchedule = new DriftDetectionSchedule(resourceId, driftDefinition);
            if (log.isInfoEnabled()) {
                log.info("No matching schedule was found in the queue. This must be a request to add a new "
                    + "schedule. Adding " + updatedSchedule + " to " + schedulesQueue);
            }
            boolean added = schedulesQueue.addSchedule(updatedSchedule);
            if (added) {
                if (log.isDebugEnabled()) {
                    log.debug(updatedSchedule + " has been added to " + schedulesQueue);
                }
            } else {
                log.warn("Failed to add " + updatedSchedule + " to " + schedulesQueue);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(updatedSchedule + " has been updated and added back to " + schedulesQueue);
            } else if (log.isInfoEnabled()) {
                log.info(updatedSchedule + " has been updated.");
            }

            if (!updatedSchedule.getDriftDefinition().isPinned()) {
                unpinDefinition(updatedSchedule);
            }
        }

        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        ResourceContainer container = inventoryMgr.getResourceContainer(resourceId);
        if (container != null) {
            container.addDriftDefinition(driftDefinition);
        }
    }

    private void unpinDefinition(final DriftDetectionSchedule schedule) {
        if (log.isDebugEnabled()) {
            log.debug("Unpinning definition for " + toString(schedule.getResourceId(), schedule.getDriftDefinition()));
        }
        schedulesQueue.removeAndExecute(schedule.getResourceId(), schedule.getDriftDefinition(), new Runnable() {
            @Override
            public void run() {
                File currentSnapshot = changeSetMgr.findChangeSet(schedule.getResourceId(), schedule
                    .getDriftDefinition().getName(), COVERAGE);
                File pinnedSnapshot = new File(currentSnapshot.getParentFile(), DriftDetector.FILE_SNAPSHOT_PINNED);
                pinnedSnapshot.delete();

                if (log.isDebugEnabled()) {
                    log.debug("Deleted pinned snapshot file " + pinnedSnapshot.getPath());
                }

                schedulesQueue.addSchedule(schedule);
            }
        });
    }

    @Override
    public void updateDriftDetection(int resourceId, DriftDefinition driftDef, DriftSnapshot driftSnapshot) {
        File currentSnapshot = changeSetMgr.findChangeSet(resourceId, driftDef.getName(), COVERAGE);
        File pinnedSnapshot = new File(currentSnapshot.getParentFile(), DriftDetector.FILE_SNAPSHOT_PINNED);
        Headers headers = createHeaders(resourceId, driftDef);

        try {
            writeSnapshotToFile(driftSnapshot, currentSnapshot, headers);
        } catch (IOException e) {
            log.error("An error occurred while writing snapshot file [" + currentSnapshot.getPath() + "] to disk", e);
            // TODO do we need to report the error to the server?
            currentSnapshot.delete();
            return;
        }

        try {
            StreamUtil.copy(new BufferedInputStream(new FileInputStream(currentSnapshot)), new BufferedOutputStream(
                new FileOutputStream(pinnedSnapshot)), true);
        } catch (IOException e) {
            log.error("An error occurred while writing snapshot file [" + pinnedSnapshot.getPath() + "] to disk", e);
            currentSnapshot.delete();
            pinnedSnapshot.delete();
            return;
        }

        updateDriftDetection(resourceId, driftDef);
    }

    @Override
    public void reportMissingBaseDir(int resourceId, DriftDefinition driftDefinition) {
        if (log.isDebugEnabled()) {
            log.debug("Reporting to server missing base directory for " + toString(resourceId, driftDefinition));
        }
        DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();
        driftServer.updateCompliance(resourceId, driftDefinition.getName(), OUT_OF_COMPLIANCE_NO_BASEDIR);
    }

    @Override
    public void pinSnapshot(final int resourceId, final String defName, final DriftSnapshot snapshot) {
        // When we pin a snapshot for an existing drift definition, we reset. We delete the
        // detection schedule and create a new schedule. We reset because the pinned
        // snapshot is always set to version zero.

        if (log.isInfoEnabled()) {
            log.info("Pinning snapshot for " + toString(resourceId, defName));
        }

        final DriftDetectionSchedule schedule = schedulesQueue.find(resourceId, defName);
        if (schedule == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to pin snapshot for " + toString(resourceId, defName) + " - no detection schedule "
                    + "found.");
            }
            return;
        }
        DriftDefinition driftDef = schedule.getDriftDefinition();
        driftDef.setPinned(true);
        unscheduleDriftDetection(resourceId, driftDef);
        updateDriftDetection(resourceId, driftDef, snapshot);
    }

    @Override
    public void ackChangeSet(int resourceId, String defName) {
        log
            .info("Received server change set ack for [resourceId: " + resourceId + ", driftDefinition:" + defName
                + "]");

        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, defName);

        if (!changeSetDir.exists()) {
            log.warn("Cannot complete acknowledgement. Change set directory " + changeSetDir.getPath()
                + " does not exist.");
            return;
        }

        try {
            File snapshot = changeSetMgr.findChangeSet(resourceId, defName, COVERAGE);
            if (null == snapshot) {
                log.warn("Cannot complete acknowledgement. Could not find coverage changeset for [" + resourceId + ","
                    + defName + "].");
                return;
            }

            File previousSnapshot = new File(snapshot.getParentFile(), snapshot.getName() + ".previous");
            previousSnapshot.delete();

        } finally {
            deleteZipFiles(changeSetDir, "changeset_");
        }
    }

    @Override
    public void ackChangeSetContent(int resourceId, String driftDefName, String token) {
        log.info("Received server change set content ack for [resourceId: " + resourceId + ", driftDefinitionName: "
            + driftDefName + "]");

        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, driftDefName);

        if (!changeSetDir.exists()) {
            log.warn("Cannot complete acknowledgement. Change set directory " + changeSetDir.getPath()
                + " does not exist.");
            return;
        }

        deleteZipFiles(changeSetDir, "content_" + token);
    }

    private void deleteZipFiles(File dir, final String prefix) {
        for (File file : dir.listFiles(new ZipFileNameFilter(prefix))) {
            file.delete();
        }
    }

    /**
     * Given a drift definition, this examines the def and its associated resource to determine where exactly
     * the base directory is that should be monitoried.
     *
     * @param resourceId The id of the resource to which the def belongs
     * @param driftDefinition describes what is to be monitored for drift
     *
     * @return absolute directory location where the drift def base directory is referring
     */
    @Override
    public File getAbsoluteBaseDirectory(int resourceId, DriftDefinition driftDefinition) {

        // get the resource entity stored in our local inventory
        InventoryManager im = getInventoryManager();
        ResourceContainer container = im.getResourceContainer(resourceId);
        Resource resource = container.getResource();

        // find out the type of base location that is specified by the drift def
        DriftDefinition.BaseDirectory baseDir = driftDefinition.getBasedir();
        if (baseDir == null) {
            throw new IllegalArgumentException("Base directory is null for drift definition ["
                + driftDefinition.getName() + "]");
        }

        // based on the type of base location, determine the root base directory
        String baseDirValueName = baseDir.getValueName(); // the name we look up in the given context
        String baseLocation;
        switch (baseDir.getValueContext()) {
        case fileSystem: {
            baseLocation = baseDirValueName; // the value name IS the absolute directory name
            if (baseLocation == null || baseLocation.trim().length() == 0) {
                baseLocation = File.separator; // paranoia, if not specified, assume the top root directory
            }
            break;
        }
        case pluginConfiguration: {
            baseLocation = resource.getPluginConfiguration().getSimpleValue(baseDirValueName, null);
            if (baseLocation == null) {
                throw new IllegalArgumentException("Cannot determine the bundle base deployment location - "
                    + "there is no plugin configuration setting for [" + baseDirValueName + "]");
            }
            break;
        }
        case resourceConfiguration: {
            baseLocation = resource.getResourceConfiguration().getSimpleValue(baseDirValueName, null);
            if (baseLocation == null) {
                throw new IllegalArgumentException("Cannot determine the bundle base deployment location - "
                    + "there is no resource configuration setting for [" + baseDirValueName + "]");
            }
            break;
        }
        case measurementTrait: {
            baseLocation = getMeasurementManager().getTraitValue(container, baseDirValueName);
            if (baseLocation == null) {
                throw new IllegalArgumentException("Cannot obtain trait [" + baseDirValueName + "] for resource ["
                    + resource.getName() + "]");
            }
            break;
        }
        default: {
            throw new IllegalArgumentException("Unknown location context: " + baseDir.getValueContext());
        }
        }

        File destDir = new File(baseLocation);

        if (!destDir.isAbsolute()) {
            throw new IllegalArgumentException("The base location path specified by [" + baseDirValueName
                + "] in the context [" + baseDir.getValueContext() + "] did not resolve to an absolute path ["
                + destDir.getPath() + "] so there is no way to know what directory to monitor for drift");
        }

        return destDir;
    }

    /**
     * Returns the manager that can provide data on the inventory. This is a separate protected method
     * so we can extend our manger class to have a mock manager for testing.
     *
     * @return the inventory manager
     */
    protected InventoryManager getInventoryManager() {
        return PluginContainer.getInstance().getInventoryManager();
    }

    /**
     * Returns the manager that can provide data on the measurements/metrics. This is a separate protected method
     * so we can extend our manger class to have a mock manager for testing.
     *
     * @return the inventory manager
     */
    protected MeasurementManager getMeasurementManager() {
        return PluginContainer.getInstance().getMeasurementManager();
    }

    private void writeSnapshotToFile(DriftSnapshot snapshot, File file, Headers headers) throws IOException {
        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(file, headers);
        try {
            for (Drift<?, ?> drift : snapshot.getDriftInstances()) {
                switch (drift.getCategory()) {
                case FILE_ADDED:
                    writer.write(addedFileEntry(drift.getPath(), drift.getNewDriftFile().getHashId(), -1L, -1L));
                    break;
                case FILE_CHANGED:
                    writer.write(changedFileEntry(drift.getPath(), drift.getOldDriftFile().getHashId(), drift
                        .getNewDriftFile().getHashId(), -1L, -1L));
                    break;
                default: // FILE_REMOVED
                    writer.write(removedFileEntry(drift.getPath(), drift.getOldDriftFile().getHashId()));
                }
            }
        } finally {
            writer.close();
        }
    }

    private String toString(int resourceId, DriftDefinition d) {
        return "DriftDefinition[id: " + d.getId() + ", name: " + d.getName() + ", resourceId: " + resourceId + "]";
    }

    private String toString(int resourceId, String defName) {
        return "[resourceId: " + resourceId + ", driftDefintionName: " + defName + "]";
    }

    private Headers createHeaders(int resourceId, DriftDefinition driftDef) {
        Headers headers = new Headers();
        headers.setResourceId(resourceId);
        headers.setDriftDefinitionId(driftDef.getId());
        headers.setType(COVERAGE);
        headers.setDriftDefinitionName(driftDef.getName());
        headers.setBasedir(getAbsoluteBaseDirectory(resourceId, driftDef).getAbsolutePath());
        return headers;
    }

    private static class ZipFileNameFilter implements FilenameFilter {
        private String prefix;

        public ZipFileNameFilter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(prefix) && name.endsWith(".zip");
        }
    }
}
