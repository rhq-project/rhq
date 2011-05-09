package org.rhq.core.clientapi.server.drift;

import java.io.InputStream;

public class SnapshotReport {
    private static final long serialVersionUID = 1L;

    private String metadataFileName;

    private long metadataSize;

    private InputStream metadataInputStream;

    private String dataFileName;

    private long dataSize;

    private InputStream dataInputStream;

    private int resourceId;

    public String getMetadataFileName() {
        return metadataFileName;
    }

    public void setMetadataFileName(String metadataFileName) {
        this.metadataFileName = metadataFileName;
    }

    public long getMetadataSize() {
        return metadataSize;
    }

    public void setMetadataSize(long metadataSize) {
        this.metadataSize = metadataSize;
    }

    public InputStream getMetadataInputStream() {
        return metadataInputStream;
    }

    public void setMetadataInputStream(InputStream metadataInputStream) {
        this.metadataInputStream = metadataInputStream;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public void setDataFileName(String dataFileName) {
        this.dataFileName = dataFileName;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public InputStream getDataInputStream() {
        return dataInputStream;
    }

    public void setDataInputStream(InputStream dataInputStream) {
        this.dataInputStream = dataInputStream;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }
}
