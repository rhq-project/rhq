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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
        DriftDetectionSchedule schedule = scheduleQueue.getNextSchedule();
        if (schedule == null) {
            log.debug("No schedules are in the queue.");
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Processing " + schedule);
        }

        try {
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
                if (changeSetMgr.changeSetExists(schedule.getResourceId(), createHeaders(schedule, COVERAGE))) {
                    changeSetType = DRIFT;
                    changes = generateDriftChangeSet(schedule);
                } else {
                    changeSetType = COVERAGE;
                    generateCoverageChangeSet(schedule);
                }
            } catch (IOException e) {
                // TODO Call ChangeSetManager here to rollback any thing that was written to disk.
                log.error("An error occurred while scanning for drift", e);
            }

            if (changeSetType == COVERAGE || changes > 0) {
                driftClient.sendChangeSetToServer(schedule.getResourceId(), driftConfig, changeSetType);
            }
        } catch (Throwable t) {
            log.error("An unexpected error occurred during drift detection: " + t.getCause().getMessage(), t);
        } finally {
            scheduleQueue.deactivateSchedule();
        }
    }

    private int generateDriftChangeSet(final DriftDetectionSchedule schedule) throws IOException {
        log.debug("Generating drift change set for " + schedule);
        final File basedir = new File(basedir(schedule.getResourceId(), schedule.getDriftConfiguration()));

        final ChangeSetWriter driftWriter = changeSetMgr.getChangeSetWriter(schedule.getResourceId(),
            createHeaders(schedule,  DRIFT));
        final ChangeSetReader coverageReader = changeSetMgr.getChangeSetReader(schedule.getResourceId(),
            schedule.getDriftConfiguration().getName());
        final ChangeSetWriter coverageWriter = changeSetMgr.getChangeSetWriterForUpdate(schedule.getResourceId(),
            createHeaders(schedule, COVERAGE));
        final AtomicInteger changes = new AtomicInteger(0);

        final Set<File> processedFiles = new HashSet<File>();

        // First look for files that have either been modified or deleted
        for (FileEntry entry : coverageReader) {
            File file = new File(basedir, entry.getFile());
            if (!file.exists()) {
                // The file has been deleted since the last scan
                if (log.isInfoEnabled()) {
                    log.info("Detected deleted file for " + schedule + " --> " + file.getAbsolutePath());
                }
                driftWriter.write(removedFileEntry(entry.getFile(), entry.getNewSHA()));
                changes.incrementAndGet();
            } else {
                processedFiles.add(file);
                String currentSHA = sha256(file);
                if (!currentSHA.equals(entry.getNewSHA())) {
                    // The file has been updated
                    if (log.isInfoEnabled()) {
                        log.info("Detected modified file for " + schedule + " --> " + file.getAbsolutePath());
                    }
                    FileEntry modifiedEntry = changedFileEntry(entry.getFile(), entry.getNewSHA(), currentSHA);
                    driftWriter.write(modifiedEntry);
                    coverageWriter.write(modifiedEntry);
                    changes.incrementAndGet();
                } else {
                    // The file has not changed
                    coverageWriter.write(entry);
                }
            }
        }

        // Now we need to do a directory tree scan to look for newly added files
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
                    driftWriter.write(newEntry);
                    coverageWriter.write(newEntry);
                    changes.incrementAndGet();
                } catch (IOException e) {
                    log.error("An error occurred while generating a drift change set for " + schedule + ": " +
                        e.getMessage());
                    throw new DriftDetectionException("An error occurred while generating a drift change set", e);
                }
            }
        }));

        driftWriter.close();
        coverageWriter.close();
        changeSetMgr.updateChangeSet(schedule.getResourceId(), createHeaders(schedule, COVERAGE));

        return changes.get();
    }

    private void generateCoverageChangeSet(final DriftDetectionSchedule schedule) throws IOException {
        log.debug("Generating coverage change set for " + schedule);

        final ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(schedule.getResourceId(), createHeaders(schedule,
            COVERAGE));
        final DriftConfiguration config = schedule.getDriftConfiguration();
        final File basedir = new File(basedir(schedule.getResourceId(), config));

        forEachFile(basedir, new FilterFileVisitor(basedir, config.getIncludes(), config.getExcludes(),
            new FileVisitor() {
            @Override
            public void visit(File file) {
                try {
                    if (log.isInfoEnabled()) {
                        log.info("Adding " + file.getAbsolutePath() + " to coverage change set for " + schedule);
                    }
                    writer.write(addedFileEntry(relativePath(basedir, file), sha256(file)));
                } catch (IOException e) {
                    log.error("An error occurred while generating a coverage change set for " + schedule + ": " +
                        e.getMessage());
                    throw new DriftDetectionException("An error occurred while generating a coverage change set for " +
                        schedule, e);
                }
            }
        }));
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

    private Headers createHeaders(DriftDetectionSchedule schedule, DriftChangeSetCategory type) {
        Headers headers = new Headers();
        headers.setResourceId(schedule.getResourceId());
        headers.setDriftCofigurationId(schedule.getDriftConfiguration().getId());
        headers.setDriftConfigurationName(schedule.getDriftConfiguration().getName());
        headers.setBasedir(basedir(schedule.getResourceId(), schedule.getDriftConfiguration()));
        headers.setType(type);

        return headers;
    }

}