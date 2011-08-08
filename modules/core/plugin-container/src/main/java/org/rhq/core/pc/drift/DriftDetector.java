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

import static java.util.Collections.EMPTY_LIST;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Stack;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.util.MessageDigestGenerator;

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
            return;
        }

        try {
            if (schedule.getNextScan() > (System.currentTimeMillis() + 100L)) {
                return;
            }

            if (!schedule.getDriftConfiguration().isEnabled()) {
                return;
            }

            DriftConfiguration driftConfig = schedule.getDriftConfiguration();
            int resourceId = schedule.getResourceId();
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
        } finally {
            scheduleQueue.deactivateSchedule();
        }
    }

    private int generateDriftChangeSet(DriftDetectionSchedule schedule) throws IOException {
        File basedir = new File(basedir(schedule.getResourceId(), schedule.getDriftConfiguration()));

        ChangeSetWriter driftWriter = changeSetMgr.getChangeSetWriter(schedule.getResourceId(), createHeaders(schedule,
            DRIFT));
        ChangeSetWriter coverageWriter = changeSetMgr.getChangeSetWriterForUpdate(schedule.getResourceId(),
            createHeaders(schedule, COVERAGE));
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(schedule.getResourceId(), schedule
            .getDriftConfiguration().getName());

        int changes = 0;

        for (DirectoryEntry dirEntry : reader) {
            DirectoryAnalyzer analyzer = new DirectoryAnalyzer(basedir, dirEntry);
            analyzer.run();

            if (analyzer.getFilesAdded().size() > 0 || analyzer.getFilesRemoved().size() > 0
                || analyzer.getFilesChanged().size() > 0) {
                DirectoryEntry driftDirEntry = new DirectoryEntry(dirEntry.getDirectory());
                DirectoryEntry coverageDirEntry = new DirectoryEntry(dirEntry.getDirectory());

                // add new files to the directory entry
                for (FileEntry entry : analyzer.getFilesAdded()) {
                    driftDirEntry.add(entry);
                    ++changes;
                    coverageDirEntry.add(entry);
                }

                // add removed files to the directory entry
                for (FileEntry entry : analyzer.getFilesRemoved()) {
                    driftDirEntry.add(entry);
                    ++changes;
                    dirEntry.remove(entry);
                }

                // add changed files to the directory entry
                for (FileEntry entry : analyzer.getFilesChanged()) {
                    driftDirEntry.add(entry);
                    ++changes;
                    dirEntry.remove(entry);
                    coverageDirEntry.add(entry);
                }

                // add the remaining unchanged files to the coverage entry
                for (FileEntry entry : dirEntry) {
                    coverageDirEntry.add(entry);
                }

                driftWriter.writeDirectoryEntry(driftDirEntry);
                coverageWriter.writeDirectoryEntry(coverageDirEntry);
            } else {
                coverageWriter.writeDirectoryEntry(dirEntry);
            }
        }
        driftWriter.close();
        coverageWriter.close();
        changeSetMgr.updateChangeSet(schedule.getResourceId(), createHeaders(schedule, COVERAGE));

        return changes;
    }

    private void generateCoverageChangeSet(DriftDetectionSchedule schedule) throws IOException {
        ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(schedule.getResourceId(), createHeaders(schedule,
            COVERAGE));

        DirectoryScanner scanner = new DirectoryScanner(schedule.getResourceId(), schedule.getDriftConfiguration(),
            writer);
        scanner.scan();
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

    // TODO Do not use DirectoryWalker
    // Want to do the file scan iteratively to keep memory overhead as low as possible.
    private class DirectoryScanner extends DirectoryWalker {

        int resourceId;
        DriftConfiguration driftConfig;
        ChangeSetWriter writer;
        Stack<DirectoryEntry> stack = new Stack<DirectoryEntry>();

        public DirectoryScanner(int resourceId, DriftConfiguration driftConfig, ChangeSetWriter writer) {
            this.resourceId = resourceId;
            this.driftConfig = driftConfig;
            this.writer = writer;
        }

        public void scan() throws IOException {
            walk(new File(basedir(resourceId, driftConfig)), EMPTY_LIST);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void handleDirectoryStart(File directory, int depth, Collection results) throws IOException {
            stack.push(new DirectoryEntry(relativePath(new File(basedir(resourceId, driftConfig)), directory)));
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void handleDirectoryEnd(File directory, int depth, Collection results) throws IOException {
            DirectoryEntry dirEntry = stack.pop();
            if (dirEntry.getNumberOfFiles() > 0) {
                writer.writeDirectoryEntry(dirEntry);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void handleFile(File file, int depth, Collection results) throws IOException {
            DirectoryEntry dirEntry = stack.peek();
            dirEntry.add(FileEntry.addedFileEntry(file.getName(), sha256(file)));
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void handleEnd(Collection results) throws IOException {
            writer.close();
        }
    }

}