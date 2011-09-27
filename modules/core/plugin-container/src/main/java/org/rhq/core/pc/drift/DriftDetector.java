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
        log.debug("Starting drift detection..");
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

            DriftConfiguration driftConfig = schedule.getDriftConfiguration();
            DriftChangeSetCategory changeSetType = null;
            int changes = 0;

            try {
                if (changeSetMgr.changeSetExists(schedule.getResourceId(), createHeaders(schedule, COVERAGE, 0))) {
                    changeSetType = DRIFT;
                    changes = generateDriftChangeSet(schedule);
                } else {
                    changeSetType = COVERAGE;
                    generateSnapshot(schedule);
                }
            } catch (IOException e) {
                // TODO Call ChangeSetManager here to rollback any thing that was written to disk.
                log.error("An error occurred while scanning for drift", e);
            }

            if (changeSetType == COVERAGE || changes > 0) {
                driftClient.sendChangeSetToServer(schedule.getResourceId(), driftConfig, changeSetType);
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

    private int generateDriftChangeSet(final DriftDetectionSchedule schedule) throws IOException {
        log.debug("Generating drift change set for " + schedule);

        final File basedir = new File(basedir(schedule.getResourceId(), schedule.getDriftConfiguration()));
        final ChangeSetReader coverageReader = changeSetMgr.getChangeSetReader(schedule.getResourceId(), schedule
            .getDriftConfiguration().getName());
        final Set<File> processedFiles = new HashSet<File>();
        final List<FileEntry> snapshotEntries = new LinkedList<FileEntry>();
        final List<FileEntry> deltaEntries = new LinkedList<FileEntry>();
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
            return 0;
        }

        File snapshotFile = changeSetMgr.findChangeSet(schedule.getResourceId(),
            schedule.getDriftConfiguration().getName(), COVERAGE);
        File backupSnapshotFile = new File(snapshotFile.getParentFile(), snapshotFile.getName() + ".bak");
        copyFile(snapshotFile, snapshotFile);

        Headers deltaHeaders = createHeaders(schedule, DRIFT, newVersion);
        Headers snapshotHeaders = createHeaders(schedule, COVERAGE, newVersion);

        ChangeSetWriter deltaWriter = changeSetMgr.getChangeSetWriter(schedule.getResourceId(), deltaHeaders);
        ChangeSetWriter snapshotWriter = changeSetMgr.getChangeSetWriter(schedule.getResourceId(), snapshotHeaders);

        for (FileEntry entry : deltaEntries) {
            deltaWriter.write(entry);
        }
        deltaWriter.close();

        for (FileEntry entry : snapshotEntries) {
            snapshotWriter.write(entry);
        }
        snapshotWriter.close();
        backupSnapshotFile.delete();

        return deltaEntries.size();
    }

    private void generateSnapshot(final DriftDetectionSchedule schedule) throws IOException {
        log.debug("Generating coverage change set for " + schedule);

        final ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(schedule.getResourceId(), createHeaders(
            schedule, COVERAGE, 0));
        final DriftConfiguration config = schedule.getDriftConfiguration();
        final File basedir = new File(basedir(schedule.getResourceId(), config));
        if (basedir.isDirectory()) {

            forEachFile(basedir, new FilterFileVisitor(basedir, config.getIncludes(), config.getExcludes(),
                new FileVisitor() {
                    @Override
                    public void visit(File file) {
                        try {
                            if (log.isInfoEnabled()) {
                                log
                                    .info("Adding " + file.getAbsolutePath() + " to coverage change set for "
                                        + schedule);
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

}