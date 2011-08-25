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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.measurement.MeasurementManager;
import org.rhq.core.util.stream.StreamUtil;

import static org.rhq.core.util.ZipUtil.zipFileOrDirectory;
import static org.rhq.core.util.file.FileUtil.purge;

public class DriftManager extends AgentService implements DriftAgentService, DriftClient, ContainerService {

    private final Log log = LogFactory.getLog(DriftManager.class);

    private PluginContainerConfiguration pluginContainerConfiguration;

    private File changeSetsDir;

    private ScheduledThreadPoolExecutor driftThreadPool;

    private ScheduleQueue schedulesQueue = new ScheduleQueueImpl();

    private ChangeSetManager changeSetMgr;

    public DriftManager() {
        super(DriftAgentService.class);
    }

    @Override
    public void setConfiguration(PluginContainerConfiguration configuration) {
        pluginContainerConfiguration = configuration;
        changeSetsDir = new File(pluginContainerConfiguration.getDataDirectory(), "changesets");
        changeSetsDir.mkdir();
    }

    @Override
    public void initialize() {
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);

        DriftDetector driftDetector = new DriftDetector();
        driftDetector.setScheduleQueue(schedulesQueue);
        driftDetector.setChangeSetManager(changeSetMgr);
        driftDetector.setDriftClient(this);

        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        initSchedules(inventoryMgr.getPlatform(), inventoryMgr);

        driftThreadPool = new ScheduledThreadPoolExecutor(5);

        long initialDelay = pluginContainerConfiguration.getDriftDetectionInitialDelay();
        long period = pluginContainerConfiguration.getDriftDetectionPeriod();
        if (period > 0) {
            // note that drift detection is globally disabled if the detection period is 0 or less
            driftThreadPool.scheduleAtFixedRate(driftDetector, initialDelay, period, TimeUnit.SECONDS);
        } else {
            log.info("Drift detection has been globally disabled as per plugin container configuration");
        }
    }

    private void initSchedules(Resource r, InventoryManager inventoryMgr) {
        if (r.getId() == 0) {
            log.debug("Will not reschedule drift detection schedules for " + r + ". It is not sync'ed yet.");
            return;
        }

        ResourceContainer container = inventoryMgr.getResourceContainer(r.getId());
        if (container == null) {
            log.debug("No resource container found for " + r + ". Unable to reschedule drift detection schedules.");
            return;
        }

        log.debug("Rescheduling drift detection schedules for " + r);
        Set<DriftDetectionSchedule> driftSchedules = container.getDriftSchedules();
        if (driftSchedules != null)
            for (DriftDetectionSchedule schedule : driftSchedules) {
                schedulesQueue.addSchedule(schedule);
            }

        for (Resource child : r.getChildResources()) {
            initSchedules(child, inventoryMgr);
        }
    }

    /**
     * This method is provided as a test hook.
     * @return The schedule queue
     */
    ScheduleQueue getSchedulesQueue() {
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
            File zipFile = new File(pluginContainerConfiguration.getTemporaryDirectory(), "changeset-" + resourceId
                + driftConfiguration.getName() + ".zip");
            zipFileOrDirectory(changeSetFile, zipFile);

            driftServer.sendChangesetZip(resourceId, zipFile.length(), remoteInputStream(new BufferedInputStream(
                new FileInputStream(zipFile))));
        } catch (IOException e) {
            log.error("An error occurred while trying to send changeset[resourceId: " + resourceId
                + ", driftConfiguration: " + driftConfiguration.getName() + "]", e);
        }
    }

    @Override
    public void sendChangeSetContentToServer(int resourceId, String driftConfigurationName, final File contentDir) {
        try {
            File zipFile = new File(contentDir, "content.zip");
            ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

            FilenameFilter contentZipFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return dir.equals(contentDir) && !name.equals("content.zip");
                }
            };

            for (File file : contentDir.listFiles(contentZipFilter)) {
                FileInputStream fis = new FileInputStream(file);
                stream.putNextEntry(new ZipEntry(file.getName()));
                StreamUtil.copy(fis, stream, false);
                fis.close();
            }
            stream.close();

            DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();
            driftServer.sendFilesZip(resourceId, zipFile.length(), remoteInputStream(new BufferedInputStream(
                new FileInputStream(zipFile))));
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
        if (log.isDebugEnabled()) {
            log.debug("Received request to schedule drift detection immediately for [resourceId: " + resourceId
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
        schedulesQueue.addSchedule(schedule);
    }

    @Override
    public void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
        schedulesQueue.addSchedule(new DriftDetectionSchedule(resourceId, driftConfiguration));
    }

    @Override
    public boolean requestDriftFiles(int resourceId, Headers headers, List<? extends DriftFile> driftFiles) {
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
        log.debug("Received request to unschedule drift detection for [resourceId:" + resourceId +
            ", driftConfigurationId:" + driftConfiguration.getId() + ", driftConfigurationName: " +
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

        log.debug("Removed " + schedule + " from the schedule queue.");
    }

    @Override
    public void updateDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
        DriftDetectionSchedule updatedSchedule = schedulesQueue.update(resourceId, driftConfiguration);
        if (updatedSchedule == null) {
            updatedSchedule = new DriftDetectionSchedule(resourceId, driftConfiguration);
            schedulesQueue.addSchedule(updatedSchedule);
        }

        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
        ResourceContainer container = inventoryMgr.getResourceContainer(resourceId);
        if (container != null) {
            container.getDriftSchedules().add(updatedSchedule);
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
}
