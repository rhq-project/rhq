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
        try {
            if (log.isInfoEnabled()) {
                log.info("Preparing to send content to server for " + defToString());
            }

            File changeSet = changeSetMgr.findChangeSet(resourceId, headers.getDriftDefinitionName());
            File contentDir = new File(changeSet.getParentFile(), "content");
            contentDir.mkdir();

            for (DriftFile driftFile : driftFiles) {
                File file = find(driftFile);
                if (file == null || !file.exists()) {
                    log.warn("Unable to find file for " + driftFile);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Copying " + file.getPath() + " to " + contentDir.getPath());
                    }
                    StreamUtil.copy(new BufferedInputStream(new FileInputStream(file)), new BufferedOutputStream(
                        new FileOutputStream(new File(contentDir, driftFile.getHashId()))), true);
                }
            }

            File[] content = contentDir.listFiles();
            if (content == null || content.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not find any of the requested content for " + defToString());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Sending " + content.length + " files to the server for " + defToString());
                }
                driftClient.sendChangeSetContentToServer(resourceId, headers.getDriftDefinitionName(), contentDir);
            }
        } catch (IOException e) {
            log.error("Failed to send drift files.", e);
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

    private String defToString() {
        return "[resourceId: " + resourceId + ", driftDefinitionId: " + headers.getDriftDefinitionId() +
                ", driftDefinitionName: " + headers.getDriftDefinitionName() + "]";
    }
}
