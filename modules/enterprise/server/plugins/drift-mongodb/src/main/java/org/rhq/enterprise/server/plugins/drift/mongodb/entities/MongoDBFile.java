package org.rhq.enterprise.server.plugins.drift.mongodb.entities;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;

@Entity
public class MongoDBFile implements DriftFile {

    @Id
    private String hash;

    @Override
    public String getHashId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setHashId(String hashId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Long getCtime() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Long getDataSize() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDataSize(Long size) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DriftFileStatus getStatus() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setStatus(DriftFileStatus status) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
