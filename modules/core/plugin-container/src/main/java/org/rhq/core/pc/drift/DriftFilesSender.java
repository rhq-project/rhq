package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.DirectoryEntry;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftFile;

import static org.rhq.core.util.file.FileUtil.copyFile;

public class DriftFilesSender implements Runnable {

    private Log log = LogFactory.getLog(DriftFilesSender.class);

    private int resourceId;

    private Headers headers;

    private List<DriftFile> driftFiles;

    private ChangeSetManager changeSetMgr;

    private DriftClient driftClient;

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public void setDriftFiles(List<DriftFile> driftFiles) {
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
            File changeSet = changeSetMgr.findChangeSet(resourceId, headers.getDriftConfigurationName());
            File contentDir = new File(changeSet.getParentFile(), "content");
            contentDir.mkdir();

            for (DriftFile driftFile : driftFiles) {
                File file = find(driftFile);
                if (file == null || !file.exists()) {
                    log.warn("Unable to find file for " + driftFile);
                } else {
                    copyFile(file, new File(contentDir, driftFile.getHashId()));
                }
            }
            driftClient.sendChangeSetContentToServer(resourceId, headers.getDriftConfigurationName(), contentDir);
        } catch (IOException e) {
            log.error("Failed to send drift files.", e);
        }
    }

    private File find(DriftFile driftFile) throws IOException {
        ChangeSetReader reader = changeSetMgr.getChangeSetReader(resourceId, headers.getDriftConfigurationName());
        DirectoryEntry dirEntry = reader.readDirectoryEntry();

        while (dirEntry != null) {
            for (FileEntry fileEntry : dirEntry) {
                if (fileEntry.getNewSHA().equals(driftFile.getHashId())) {
                    File dir = new File(headers.getBasedir(), dirEntry.getDirectory());
                    return new File(dir, fileEntry.getFile());
                }
            }
            dirEntry = reader.readDirectoryEntry();
        }
        return null;
    }
}
