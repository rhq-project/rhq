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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileVisitor;

import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.util.file.FileUtil.copyFile;
import static org.rhq.core.util.file.FileUtil.forEachFile;

public class DriftDetector implements Runnable {
    private Log log = LogFactory.getLog(DriftDetector.class);

    private ScheduleQueue scheduleQueue;

    private ChangeSetManager changeSetMgr;

    private MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    private DriftClient driftClient;

    public void setScheduleQueue(ScheduleQueue queue) {
        scheduleQueue = queue;
    }

    public void setChangeSetManager(ChangeSetManager changeSetManager) {
        changeSetMgr = changeSetManager;
    }

    public void setDriftClient(DriftClient driftClient) {
        this.driftClient = driftClient;
    }

    @Override
    public void run() {
        log.debug("Starting drift detection...");
        long startTime = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Fetching next schedule from " + scheduleQueue);
            }

            DriftDetectionSchedule schedule = scheduleQueue.getNextSchedule();
            if (schedule == null) {
                log.debug("No schedules are in the queue.");
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("Processing " + schedule);

            }

            if (schedule.getNextScan() > (System.currentTimeMillis() + 100L)) {
                log.debug("Skipping " + schedule + " because it is too early to do the next detection.");
                return;
            }

            if (!schedule.getDriftConfiguration().isEnabled()) {
                log.debug("Skipping " + schedule + " because the drift configuration is disabled.");
                return;
            }

            if (previousSnapshotExists(schedule)) {
                log.debug("Skipping " + schedule + " because server has not yet acked previous change set");
                return;
            }

            DriftDetectionSummary detectionSummary = new DriftDetectionSummary();
            detectionSummary.setSchedule(schedule);
            try {
                if (changeSetMgr.changeSetExists(schedule.getResourceId(), createHeaders(schedule, COVERAGE, 0))) {
                    detectionSummary.setType(DRIFT);
                    generateDriftChangeSet(detectionSummary);
                } else {
                    detectionSummary.setType(COVERAGE);
                    generateSnapshot(detectionSummary);
                }
                if (detectionSummary.getType() == COVERAGE || detectionSummary.getDriftChangeSet() != null) {
                    driftClient.sendChangeSetToServer(detectionSummary);
                }
            } catch (IOException e) {
                log.error("Drift detection failed: " + e.getMessage(), e);
                revertSnapshot(detectionSummary);
            } catch (RuntimeException e) {
                log.error("Drift detection failed: " + e.getMessage(), e);
                revertSnapshot(detectionSummary);
            }
        } catch (Throwable t) {
            Throwable cause = t.getCause();
            String message = (null != cause) ? cause.getMessage() : t.getMessage();
            log.error("An unexpected error occurred during drift detection: " + message, t);

        } finally {
            try {
                scheduleQueue.deactivateSchedule();
                long endTime = System.currentTimeMillis();
                log.debug("Finished drift detection in " + (endTime - startTime) + " ms");

            } catch (Throwable t) {
                Throwable cause = t.getCause();
                String message = (null != cause) ? cause.getMessage() : t.getMessage();
                log.error("An unexpected error occurred while deactivating schedule: " + message, t);
            }
        }
    }

    private boolean previousSnapshotExists(DriftDetectionSchedule schedule) {
        File snapshot = changeSetMgr.findChangeSet(schedule.getResourceId(),
            schedule.getDriftConfiguration().getName(), COVERAGE);
        File previousSnapshot = new File(snapshot.getParentFile(), snapshot.getName() + ".previous");
        return previousSnapshot.exists();
    }

    private void generateDriftChangeSet(DriftDetectionSummary summary) throws IOException {
        final DriftDetectionSchedule schedule = summary.getSchedule();

        log.debug("Generating drift change set for " + schedule);

        final File basedir = new File(basedir(schedule.getResourceId(), schedule.getDriftConfiguration()));
        File currentSnapshot = changeSetMgr.findChangeSet(schedule.getResourceId(),
            schedule.getDriftConfiguration().getName(), COVERAGE);
        final ChangeSetReader coverageReader = changeSetMgr.getChangeSetReader(currentSnapshot);
        final Set<File> processedFiles = new HashSet<File>();
        final List<FileEntry> snapshotEntries = new LinkedList<FileEntry>();
        final List<FileEntry> deltaEntries = new LinkedList<FileEntry>();
        int currentVersion = coverageReader.getHeaders().getVersion();
        int newVersion = coverageReader.getHeaders().getVersion() + 1;

        // First look for files that have either been modified or deleted
        for (FileEntry entry : coverageReader) {
            File file = new File(basedir, entry.getFile());
            if (!file.exists()) {
                // The file has been deleted since the last scan
                if (log.isInfoEnabled()) {
                    log.info("Detected deleted file for " + schedule + " --> " + file.getAbsolutePath());
                }
                deltaEntries.add(removedFileEntry(entry.getFile(), entry.getNewSHA()));
            } else {
                processedFiles.add(file);
                String currentSHA = sha256(file);
                if (!currentSHA.equals(entry.getNewSHA())) {
                    if (log.isInfoEnabled()) {
                        log.info("Detected modified file for " + schedule + " --> " + file.getAbsolutePath());
                    }
                    FileEntry modifiedEntry = changedFileEntry(entry.getFile(), entry.getNewSHA(), currentSHA);
                    deltaEntries.add(modifiedEntry);
                    snapshotEntries.add(modifiedEntry);
                } else {
                    // The file has not changed
                    snapshotEntries.add(entry);
                }
            }
        }
        coverageReader.close();

        // If the basedir is still valid we need to do a directory tree scan to look for newly added files
        if (basedir.isDirectory()) {
            forEachFile(basedir, new FilterFileVisitor(basedir, schedule.getDriftConfiguration().getIncludes(),
                schedule.getDriftConfiguration().getExcludes(), new FileVisitor() {
                    @Override
                    public void visit(File file) {
                        try {
                            if (processedFiles.contains(file)) {
                                return;
                            }

                            if (log.isInfoEnabled()) {
                                log.info("Detected added file for " + schedule + " --> " + file.getAbsolutePath());
                            }

                            FileEntry newEntry = addedFileEntry(relativePath(basedir, file), sha256(file));
                            deltaEntries.add(newEntry);
                            snapshotEntries.add(newEntry);
                        } catch (IOException e) {
                            log.error("An error occurred while generating a drift change set for " + schedule + ": "
                                + e.getMessage());
                            throw new DriftDetectionException("An error occurred while generating a drift change set",
                                e);
                        }
                    }
                }));
        }

        if (deltaEntries.isEmpty()) {
            // If nothing has changed, there is no need to add/update any files
            summary.setNewSnapshot(currentSnapshot);
        } else {
            File oldSnapshot = new File(currentSnapshot.getParentFile(), currentSnapshot.getName() +
                ".previous");
            copyFile(currentSnapshot, oldSnapshot);
            currentSnapshot.delete();

            Headers deltaHeaders = createHeaders(schedule, DRIFT, newVersion);
            Headers snapshotHeaders = createHeaders(schedule, COVERAGE, newVersion);

            File driftChangeSet = changeSetMgr.findChangeSet(schedule.getResourceId(),
                schedule.getDriftConfiguration().getName(), DRIFT);
            ChangeSetWriter deltaWriter = changeSetMgr.getChangeSetWriter(driftChangeSet, deltaHeaders);

            File newSnapshot = changeSetMgr.findChangeSet(schedule.getResourceId(),
                schedule.getDriftConfiguration().getName(), COVERAGE);
            ChangeSetWriter newSnapshotWriter = changeSetMgr.getChangeSetWriter(schedule.getResourceId(),
                snapshotHeaders);

            summary.setDriftChangeSet(driftChangeSet);
            summary.setNewSnapshot(newSnapshot);
            summary.setOldSnapshot(oldSnapshot);

            for (FileEntry entry : deltaEntries) {
                deltaWriter.write(entry);
            }
            deltaWriter.close();

            for (FileEntry entry : snapshotEntries) {
                newSnapshotWriter.write(entry);
            }
            newSnapshotWriter.close();
        }
    }

    private void generateSnapshot(DriftDetectionSummary summary) throws IOException {
        final DriftDetectionSchedule schedule = summary.getSchedule();
        log.debug("Generating coverage change set for " + schedule);

        File snapshot = changeSetMgr.findChangeSet(schedule.getResourceId(), schedule.getDriftConfiguration().getName(),
            COVERAGE);
        final ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(snapshot, createHeaders(schedule, COVERAGE, 0));
        final DriftConfiguration config = schedule.getDriftConfiguration();
        final File basedir = new File(basedir(schedule.getResourceId(), config));
        if (basedir.isDirectory()) {

            forEachFile(basedir, new FilterFileVisitor(basedir, config.getIncludes(), config.getExcludes(),
                new FileVisitor() {
                    @Override
                    public void visit(File file) {
                        try {
                            if (log.isInfoEnabled()) {
                                log.info("Adding " + file.getPath() + " to coverage change set for " + schedule);
                            }
                            writer.write(addedFileEntry(relativePath(basedir, file), sha256(file)));
                        } catch (IOException e) {
                            log.error("An error occurred while generating a coverage change set for " + schedule + ": "
                                + e.getMessage());
                            throw new DriftDetectionException(
                                "An error occurred while generating a coverage change set for " + schedule, e);
                        }
                    }
                }));
        }
        writer.close();
        summary.setNewSnapshot(snapshot);
    }

    private String relativePath(File basedir, File file) {
        if (basedir.equals(file)) {
            return ".";
        }
        return file.getAbsolutePath().substring(basedir.getAbsolutePath().length() + 1);
    }

    private String sha256(File file) throws IOException {
        return digestGenerator.calcDigestString(file);
    }

    private String basedir(int resourceId, DriftConfiguration driftConfig) {
        return driftClient.getAbsoluteBaseDirectory(resourceId, driftConfig).getAbsolutePath();
    }

    private Headers createHeaders(DriftDetectionSchedule schedule, DriftChangeSetCategory type, int version) {
        Headers headers = new Headers();
        headers.setResourceId(schedule.getResourceId());
        headers.setDriftCofigurationId(schedule.getDriftConfiguration().getId());
        headers.setDriftConfigurationName(schedule.getDriftConfiguration().getName());
        headers.setBasedir(basedir(schedule.getResourceId(), schedule.getDriftConfiguration()));
        headers.setType(type);
        headers.setVersion(version);

        return headers;
    }

    private void revertSnapshot(DriftDetectionSummary summary) throws IOException {
        log.info("Reverting snapshot for " + summary.getSchedule());

        DriftDetectionSchedule scheudle = summary.getSchedule();
        File newSnapshot = changeSetMgr.findChangeSet(scheudle.getResourceId(),
            scheudle.getDriftConfiguration().getName(), COVERAGE);

        // We want to delete the snapshot file regardless of whether the drift detection
        // was for an initial coverage change set or for a drift change set. We do not know
        // the state of the snapshot file so we have to delete it. If we have only generated
        // the initial coverage change set, then it will get regenerated.
        newSnapshot.delete();

        if (summary.getType() == DRIFT) {
            File oldSnapshotBackup = summary.getOldSnapshot();
            // If we generated a drift change set, we need to check for a back up of the
            // previous snapshot. We revert to the back up. If no back up is found, we log
            // an error.
            if (oldSnapshotBackup != null && oldSnapshotBackup.exists()) {
                copyFile(oldSnapshotBackup, newSnapshot);
                // We have to delete to the previous version snapshot file; otherwise,
                // subsequent detection runs will be skipped.
                oldSnapshotBackup.delete();
            } else {
                // TODO Should we throw an exception and/or disable detection?
                // If we fall into this else block, that means we were not able to revert
                // to the previous snapshot version, and we may be in an inconsistent state.
                log.error("Cannot revert snapshot to previous version for " + summary.getSchedule() +
                    ". Snapshot back up file not found.");
            }
        }
        deleteZipFiles(newSnapshot.getParentFile());
    }

    private void deleteZipFiles(File dir) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".zip");
            }
        });
        for (File file : files) {
            file.delete();
        }
    }

}