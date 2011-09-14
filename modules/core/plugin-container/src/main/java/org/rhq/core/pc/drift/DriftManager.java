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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfigurationComparator;
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
import org.rhq.core.util.stream.StreamUtil;

import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.rhq.core.domain.drift.DriftConfigurationComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;
import static org.rhq.core.util.file.FileUtil.purge;

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
        changeSetsDir.mkdir();
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize() {
        changeSetsDir.mkdir();
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);

        DriftDetector driftDetector = new DriftDetector();
        driftDetector.setScheduleQueue(schedulesQueue);
        driftDetector.setChangeSetManager(changeSetMgr);
        driftDetector.setDriftClient(this);

        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        long startTime = System.currentTimeMillis();
        initSchedules(inventoryMgr.getPlatform(), inventoryMgr);
        long endTime = System.currentTimeMillis();
        log.info("Finished initializing drift detection schedules in " + (endTime - startTime) + " ms");

        purgeDeletedDriftConfigDirs();

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
        for (DriftConfiguration c : container.getDriftConfigurations()) {
            try {
                syncWithServer(r, c);
                schedulesQueue.addSchedule(new DriftDetectionSchedule(r.getId(), c));
            } catch (IOException e) {
                log.error("Failed to sync with server for " + toString(r, c) + ". Drift detection will not be " +
                    "scheduled.", e);
            }
        }

        for (Resource child : r.getChildResources()) {
            initSchedules(child, inventoryMgr);
        }
    }

    private void syncWithServer(Resource resource, DriftConfiguration configuration) throws IOException {
        Headers headers = createHeaders(resource, configuration);
        if (!changeSetMgr.changeSetExists(resource.getId(), headers)) {
            log.info("No snapshot found for " + toString(resource, configuration) + ". Downloading snapshot from " +
                "server");
            DriftSnapshot snapshot = pluginContainerConfiguration.getServerServices().getDriftServerService()
                .getCurrentSnapshot(configuration.getId());

            if (snapshot.getVersion() == -1) {
                // A version of -1 indicates that no change sets have been reported
                // for this configuration. This can occur when a user creates a
                // drift configuration while the agent is offline for example. At
                // this point we just return and allow the agent to generate the
                // initial snapshot file.
                if (log.isDebugEnabled()) {
                    log.debug("The server does not have any change sets for " + toString(resource, configuration) +
                        ". An initial snapshot needs to be generated.");
                }
                return;
            }

            log.info("Preparing to write snapshot at version " + snapshot.getVersion() + " to disk for " +
                toString(resource, configuration));
            ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(resource.getId(), headers);

            for (Drift drift : snapshot.getEntries()) {
                switch (drift.getCategory()) {
                    case FILE_ADDED:
                        writer.write(addedFileEntry(drift.getPath(), drift.getNewDriftFile().getHashId()));
                        break;
                    case FILE_CHANGED:
                        writer.write(changedFileEntry(drift.getPath(), drift.getOldDriftFile().getHashId(),
                            drift.getNewDriftFile().getHashId()));
                        break;
                    default:  // FILE_REMOVED
                        writer.write(removedFileEntry(drift.getPath(), drift.getOldDriftFile().getHashId()));
                }
            }
            writer.close();
        }
    }

    private void purgeDeletedDriftConfigDirs() {
        log.info("Checking for deleted drift configurations");
        DriftConfigurationComparator comparator = new DriftConfigurationComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        for (File resourceDir : changeSetsDir.listFiles()) {
            int resourceId = Integer.parseInt(resourceDir.getName());
            for (File configDir : resourceDir.listFiles()) {
                DriftConfiguration config = new DriftConfiguration(new Configuration());
                config.setName(configDir.getName());
                if (!schedulesQueue.contains(resourceId, config)) {
                    log.info("Detected deleted drift configuration, DriftConfiguration[name: " + config.getName() +
                        ", resourceId: " + resourceId + "]");
                    log.info("Deleting drift configuration directory " + configDir.getPath());
                    purge(configDir, true);
                }
            }
        }
    }

    private String toString(Resource r, DriftConfiguration c) {
        return "DriftConfiguration[id: " + c.getId() + ", resourceId: " + r.getId() + ", name: " + c.getName() + "]";
    }

    private Headers createHeaders(Resource resource, DriftConfiguration configuration) {
        Headers headers = new Headers();
        headers.setResourceId(resource.getId());
        headers.setDriftCofigurationId(configuration.getId());
        headers.setType(DriftChangeSetCategory.COVERAGE);
        headers.setDriftConfigurationName(configuration.getName());
        headers.setBasedir(getAbsoluteBaseDirectory(resource.getId(), configuration).getAbsolutePath());
        return headers;
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
        driftThreadPool.shutdown();
        driftThreadPool = null;

        schedulesQueue.clear();
        schedulesQueue = null;

        changeSetMgr = null;
    }

    @Override
    public void sendChangeSetToServer(int resourceId, DriftConfiguration driftConfiguration, DriftChangeSetCategory type) {
        try {
            if (!schedulesQueue.contains(resourceId, driftConfiguration)) {
                return;
            }

            File changeSetFile = changeSetMgr.findChangeSet(resourceId, driftConfiguration.getName(), type);
            if (changeSetFile == null) {
                log.warn("changeset[resourceId: " + resourceId + ", driftConfiguration: " +
                    driftConfiguration.getName() + "] was not found. Cancelling request to send change set " +
                    "to server");
                return;
            }

            DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();

            // TODO Include the version in the change set file name to ensure the file name is unique
            String fileName = "changeset_" + System.currentTimeMillis() + ".zip";
            final File zipFile = new File(changeSetFile.getParentFile(), fileName);
            ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

            FileInputStream fis = new FileInputStream(changeSetFile);
            stream.putNextEntry(new ZipEntry(changeSetFile.getName()));
            StreamUtil.copy(fis, stream, false);
            fis.close();
            stream.close();

            // We want to clean up after we send the zip file to the server. We do this by
            // deleting the files in the content directory and the content zip itself. They
            // are no longer needed after being sent to the server. We cannot immediately
            // delete the content zip file though because it is sent asynchronously, and we
            // wind up deleting it before it is sent. The following approach allows us to
            // safely delete it when the comm layer closes the remote input stream.
            //
            // jsanda
            DriftInputStream inputStream = new DriftInputStream(new BufferedInputStream(new FileInputStream(zipFile)),
                new DeleteFile(zipFile));

            driftServer.sendChangesetZip(resourceId, zipFile.length(), remoteInputStream(inputStream));

        } catch (IOException e) {
            log.error("An error occurred while trying to send changeset[resourceId: " + resourceId
                + ", driftConfiguration: " + driftConfiguration.getName() + "]", e);
        }
    }

    @Override
    public void sendChangeSetContentToServer(int resourceId, String driftConfigurationName, final File contentDir) {
        try {
            String contentFileName = "content_" + System.currentTimeMillis() + ".zip";
            final File zipFile = new File(contentDir.getParentFile(), contentFileName);
            ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

            for (File file : contentDir.listFiles()) {
                FileInputStream fis = new FileInputStream(file);
                stream.putNextEntry(new ZipEntry(file.getName()));
                StreamUtil.copy(fis, stream, false);
                fis.close();
            }
            stream.close();

            // We want to clean up after we send the zip file to the server. We do this by
            // deleting the files in the content directory and the content zip itself. They
            // are no longer needed after being sent to the server. We cannot immediately
            // delete the content zip file though because it is sent asynchronously, and we
            // wind up deleting it before it is sent. The following approach allows us to
            // safely delete it when the comm layer closes the remote input stream.
            //
            // jsanda
            DriftInputStream inputStream = new DriftInputStream(new BufferedInputStream(new FileInputStream(zipFile)),
                new DeleteFile(zipFile));

            DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();
            driftServer.sendFilesZip(resourceId, zipFile.length(), remoteInputStream(inputStream));
        } catch (IOException e) {
            log.error("An error occurred while trying to send content for changeset[resourceId: " + resourceId
                + ", driftConfiguration: " + driftConfigurationName + "]", e);
        }

        for (File file : contentDir.listFiles()) {
            if (!file.delete()) {
                log.warn("Unable to clean up content directory. Failed to delete " + file.getPath());
            }
        }
    }

    @Override
    public void detectDrift(int resourceId, DriftConfiguration driftConfiguration) {
        if (log.isInfoEnabled()) {
            log.info("Received request to schedule drift detection immediately for [resourceId: " + resourceId
                + ", driftConfigurationId: " + driftConfiguration.getId() + ", driftConfigurationName: "
                + driftConfiguration.getName() + "]");
        }

        DriftDetectionSchedule schedule = schedulesQueue.remove(resourceId, driftConfiguration);
        if (schedule == null) {
            log.warn("No schedule found in the queue for [resourceId: " + resourceId + ", driftConfigurationId: "
                + driftConfiguration.getId() + ", driftConfigurationName: " + driftConfiguration.getName() + "]. No "
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
    public void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
        DriftDetectionSchedule schedule = new DriftDetectionSchedule(resourceId, driftConfiguration);
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
                container.addDriftConfiguration(driftConfiguration);
            }
        } else {
            log.warn("Failed to add " + schedule + " to " + schedulesQueue);
        }
    }

    @Override
    public boolean requestDriftFiles(int resourceId, Headers headers, List<? extends DriftFile> driftFiles) {
        if (log.isInfoEnabled()) {
            log.info("Server is requesting files for [resourceId: " + resourceId + ", driftConfigurationId: " +
                headers.getDriftCofigurationId() + ", driftConfigurationName: " + headers.getDriftConfigurationName() +
                "]");
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
    public void unscheduleDriftDetection(final int resourceId, final DriftConfiguration driftConfiguration) {
        log.info("Received request to unschedule drift detection for [resourceId:" + resourceId +
            ", driftConfigurationId: " + driftConfiguration.getId() + ", driftConfigurationName: " +
            driftConfiguration.getName() + "].");

        DriftDetectionSchedule schedule = schedulesQueue.removeAndExecute(resourceId, driftConfiguration,
            new Runnable() {
                @Override
                public void run() {
                    File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
                    File changeSetDir = new File(resourceDir, driftConfiguration.getName());
                    purge(changeSetDir, true);

                    log.debug("Removed change set directory " + changeSetDir.getAbsolutePath());
                }
            });
        if (schedule != null) {
            ResourceContainer container = getInventoryManager().getResourceContainer(resourceId);
            if (container != null) {
                container.removeDriftConfiguration(schedule.getDriftConfiguration());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Removed " + schedule + " from the queue " + schedulesQueue);
        }

    }

    @Override
    public void updateDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
        log.info("Received request to update schedule for [resourceId: " + resourceId + ", driftConfigurationId: " +
            driftConfiguration.getId() + ", driftConfigurationName: " + driftConfiguration.getName() + "]");
        DriftDetectionSchedule updatedSchedule = schedulesQueue.update(resourceId, driftConfiguration);
        if (updatedSchedule == null) {
            updatedSchedule = new DriftDetectionSchedule(resourceId, driftConfiguration);
            if (log.isInfoEnabled()) {
                log.info("No matching schedule was found in the queue. This must be a request to add a new " +
                    "schedule. Adding " + updatedSchedule + " to " + schedulesQueue);
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
        }

        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        ResourceContainer container = inventoryMgr.getResourceContainer(resourceId);
        if (container != null) {
            container.addDriftConfiguration(driftConfiguration);
        }
    }

    /**
     * Given a drift configuration, this examines the config and its associated resource to determine where exactly
     * the base directory is that should be monitoried.
     *
     * @param resourceId The id of the resource to which the config belongs
     * @param driftConfiguration describes what is to be monitored for drift
     *
     * @return absolute directory location where the drift configuration base directory is referring
     */
    @Override
    public File getAbsoluteBaseDirectory(int resourceId, DriftConfiguration driftConfiguration) {

        // get the resource entity stored in our local inventory
        InventoryManager im = getInventoryManager();
        ResourceContainer container = im.getResourceContainer(resourceId);
        Resource resource = container.getResource();

        // find out the type of base location that is specified by the drift config
        DriftConfiguration.BaseDirectory baseDir = driftConfiguration.getBasedir();
        if (baseDir == null) {
            throw new IllegalArgumentException("Missing basedir in drift config");
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

    private static class DeleteFile implements Runnable {

        private File file;

        public DeleteFile(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            file.delete();
        }
    }
}
