package org.rhq.enterprise.server.plugins.drift.mongodb.entities;

import java.io.Serializable;

import com.google.code.morphia.annotations.Embedded;

import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;

@Embedded
public class MongoDBFile implements Serializable, DriftFile {

    private static final long serialVersionUID = 1L;

    private String hash;

    private Long ctime = System.currentTimeMillis();

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
