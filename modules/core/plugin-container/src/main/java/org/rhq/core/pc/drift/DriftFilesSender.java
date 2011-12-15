package org.rhq.core.pc.drift;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.util.stream.StreamUtil;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DriftFilesSender implements Runnable {

    private Log log = LogFactory.getLog(DriftFilesSender.class);

    private int resourceId;

    private Headers headers;

    private List<? extends DriftFile> driftFiles;

    private ChangeSetManager changeSetMgr;

    private DriftClient driftClient;

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public void setDriftFiles(List<? extends DriftFile> driftFiles) {
        this.driftFiles = driftFiles;
    }

    public void setDriftClient(DriftClient driftClient) {
        this.driftClient = driftClient;
    }

    public void setChangeSetManager(ChangeSetManager changeSetManager) {
        changeSetMgr = changeSetManager;
    }

    @Override
    public void run() {
        ZipOutputStream stream = null;
        try {
            if (log.isInfoEnabled()) {
                log.info("Preparing to send content to server for " + defToString());
            }
            long startTime = System.currentTimeMillis();
            File changeSet = changeSetMgr.findChangeSet(resourceId, headers.getDriftDefinitionName());
            File changeSetDir = changeSet.getParentFile();

            // Note that the content file has a specific format that the server
            // expects. The file name is of the form content_<token>.zip where
            // token is a unique string that the agent can use to identify the
            // content zip file when the server sends an acknowledgement. The
            // token is necessary because it is possible, albeit not likely,
            // for a a particular definition to wind up with multiple content
            // zip files and there is no guarantee that they will be acked in
            // the order in which they were sent. The name of each file in the
            // zip file should be the SHA for that file. The server uses that
            // SHA to look up the DriftFile object it has already created for
            // the content.
            //
            // jsanda

            String timestamp = Long.toString(System.currentTimeMillis());
            String contentFileName = "content_" + timestamp + ".zip";
            final File zipFile = new File(changeSetDir, contentFileName);
            stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

            if (driftFiles.size() == 1) {
                DriftFile driftFile = driftFiles.get(0);
                File file = find(driftFile);
                if (file == null || !file.exists()) {
                    log.warn("Unable to find file for " + driftFile);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Adding " + file.getPath() + " to " + contentFileName);
                    }
                    addFileToContentZipFile(stream, driftFile, file);
                }
            } else {
                Map<String, FileEntry> fileEntries = createSnapshotIndex();

                for (DriftFile driftFile : driftFiles) {
                    FileEntry entry = fileEntries.get(driftFile.getHashId());
                    if (entry == null) {
                        continue;
                    }
                    File file = new File(headers.getBasedir(), entry.getFile());
                    if (file == null || !file.exists()) {
                        log.warn("Unable to find file for " + driftFile);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Adding " + file.getPath() + " to " + contentFileName);
                        }
                        addFileToContentZipFile(stream, driftFile, file);
                    }
                }
            }

            stream.close();
            stream = null;

            driftClient.sendChangeSetContentToServer(resourceId, headers.getDriftDefinitionName(), zipFile);

            if (log.isInfoEnabled()) {
                long endTime = System.currentTimeMillis();
                log.info("Finished submitting request to send content to server in " + (endTime - startTime) +
                        " ms");
            }

        } catch (IOException e) {
            log.error("Failed to send drift files.", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void addFileToContentZipFile(ZipOutputStream stream, DriftFile driftFile, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            stream.putNextEntry(new ZipEntry(driftFile.getHashId()));
            StreamUtil.copy(fis, stream, false);
        } finally {
            fis.close();
        }
    }

    private File find(DriftFile driftFile) throws IOException {
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId, headers.getDriftDefinitionName());

        try {
            for (FileEntry entry : reader) {
                if (entry.getNewSHA().equals(driftFile.getHashId())) {
                    return new File(headers.getBasedir(), entry.getFile());
                }
            }
            return null;
        } finally {
            reader.close();
        }
    }

    private Map<String, FileEntry> createSnapshotIndex() throws IOException {
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId, headers.getDriftDefinitionName());
        try {
            Map<String, FileEntry> map = new TreeMap<String, FileEntry>();
            for (FileEntry entry : reader) {
                map.put(entry.getNewSHA(), entry);
            }
            return map;
        } finally {
            reader.close();
        }
    }

    private String defToString() {
        return "[resourceId: " + resourceId + ", driftDefinitionId: " + headers.getDriftDefinitionId() +
                ", driftDefinitionName: " + headers.getDriftDefinitionName() + "]";
    }
}
