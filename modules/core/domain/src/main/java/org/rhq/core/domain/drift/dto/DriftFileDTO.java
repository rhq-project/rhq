package org.rhq.core.domain.drift.dto;

import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;

public class DriftFileDTO implements DriftFile {

    private String hash;

    private Long ctime;

    private Long size;

    private DriftFileStatus status;

    @Override
    public String getHashId() {
        return hash;
    }

    @Override
    public void setHashId(String hashId) {
        hash = hashId;
    }

    @Override
    public Long getCtime() {
        return ctime;
    }

    @Override
    public Long getDataSize() {
        return size;
    }

    @Override
    public void setDataSize(Long size) {
        this.size = size;
    }

    @Override
    public DriftFileStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(DriftFileStatus status) {
        this.status = status;
    }
}
