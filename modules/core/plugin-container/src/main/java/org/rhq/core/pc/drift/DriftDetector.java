package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Stack;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.util.MessageDigestGenerator;

import static java.util.Collections.EMPTY_LIST;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;

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
        DriftDetectionSchedule schedule = scheduleQueue.dequeue();
        if (schedule == null) {
            return;
        }

        DriftConfiguration driftConfig = schedule.getDriftConfiguration();

        try {
            // TODO add logic to determine if there is an existing changeset
            // if there is no previous changeset then we need to generate the initial
            // coverage changeset
            ChangeSetWriter writer = changeSetMgr.getChangeSetWriter(schedule.getResourceId(),
                new Headers(driftConfig.getName(), basedir(driftConfig), COVERAGE));

            DirectoryScanner scanner = new DirectoryScanner(schedule.getDriftConfiguration(), writer);
            scanner.scan();
        } catch (IOException e) {
            // TODO Call ChangeSetManager here to rollback any thing that was written to disk.
            log.error("An error occurred while scanning for drift", e);
        }

        schedule.updateShedule();
        scheduleQueue.enqueue(schedule);
        driftClient.sendChangeSetToServer(schedule.getResourceId(), driftConfig);
    }

    private String relativePath(File basedir, File file) {
        if (basedir.equals(file)) {
            return basedir.getName();
        }
        return file.getAbsolutePath().substring(basedir.getAbsolutePath().length() + 1);
    }

    private String sha256(File file) throws IOException {
        return digestGenerator.calcDigestString(file);
    }

    private String basedir(DriftConfiguration driftConfig) {
        return driftClient.getAbsoluteBaseDirectory(driftConfig).getAbsolutePath();
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
            walk(new File(basedir(driftConfig)), EMPTY_LIST);
        }

        @Override
        protected void handleDirectoryStart(File directory, int depth, Collection results) throws IOException {
            stack.push(new DirectoryEntry(relativePath(new File(basedir(driftConfig)), directory)));
        }

        @Override
        protected void handleDirectoryEnd(File directory, int depth, Collection results) throws IOException {
            DirectoryEntry dirEntry = stack.pop();
            if (dirEntry.getNumberOfFiles() > 0) {
                writer.writeDirectoryEntry(dirEntry);
            }
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