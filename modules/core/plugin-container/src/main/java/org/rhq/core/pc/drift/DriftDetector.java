package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Stack;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.util.MessageDigestGenerator;

import static java.io.File.separator;
import static java.util.Collections.EMPTY_LIST;

public class DriftDetector implements Runnable {
    private Log log = LogFactory.getLog(DriftDetector.class);

    private ScheduleQueue scheduleQueue;

    private ChangeSetManager changeSetMgr;

    private MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    private DriftManager driftMgr;

    public void setScheduleQueue(ScheduleQueue queue) {
        scheduleQueue = queue;
    }

    public void setChangeSetManager(ChangeSetManager changeSetManager) {
        changeSetMgr = changeSetManager;
    }

    public void setDriftManager(DriftManager driftManager) {
        driftMgr = driftManager;
    }

    @Override
    public void run() {
        DriftDetectionSchedule schedule = scheduleQueue.dequeue();
        if (schedule == null) {
            return;
        }

        try {
            ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(schedule.getResourceId(),
            schedule.getDriftConfiguration());

            DirectoryScanner scanner = new DirectoryScanner(schedule.getDriftConfiguration(), writer);
            scanner.scan();
        } catch (IOException e) {
            // TODO Call ChangeSetManager here to rollback any thing that was written to disk.
            log.error("An error occurred while scanning for drift", e);
        }

        schedule.updateShedule();
        scheduleQueue.enqueue(schedule);
        driftMgr.sendChangeSetToServer(schedule.getResourceId(), schedule.getDriftConfiguration());
    }

    private String relativePath(File basedir, File file) {
        if (basedir.equals(file)) {
            return basedir.getPath();
        }
        return FilenameUtils.getName(basedir.getAbsolutePath()) + separator +
            file.getAbsolutePath().substring(basedir.getAbsolutePath().length() + 1);
    }

    private String sha256(File file) throws IOException {
        return digestGenerator.calcDigestString(file);
    }

    // TODO Do not use DirectoryWalker
    // Want to do the file scan iteratively to keep memory overhead as low as possible.
    private class DirectoryScanner extends DirectoryWalker {

        DriftConfiguration driftConfig;
        ChangeSetWriter writer;
        Stack<DirectoryEntry> stack = new Stack<DirectoryEntry>();

        public DirectoryScanner(DriftConfiguration driftConfig, ChangeSetWriter writer) {
            this.driftConfig = driftConfig;
            this.writer = writer;
        }

        public void scan() throws IOException {
            walk(new File(driftConfig.getBasedir()), EMPTY_LIST);
        }

        @Override
        protected void handleDirectoryStart(File directory, int depth, Collection results) throws IOException {
            stack.push(new DirectoryEntry(relativePath(new File(driftConfig.getBasedir()), directory)));
        }

        @Override
        protected void handleDirectoryEnd(File directory, int depth, Collection results) throws IOException {
            DirectoryEntry dirEntry = stack.pop();
            writer.writeDirectoryEntry(dirEntry);


        }

        @Override
        protected void handleFile(File file, int depth, Collection results) throws IOException {
            DirectoryEntry dirEntry = stack.peek();
            dirEntry.add(FileEntry.addedFileEntry(file.getName(), sha256(file)));
        }

        @Override
        protected void handleEnd(Collection results) throws IOException {
            writer.close();
        }
    }

}