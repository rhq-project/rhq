package org.rhq.enterprise.server.drift;

import java.io.InputStream;
import java.io.Serializable;

public class UploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int resourceId;

    private long metaDataSize;

    private InputStream metaDataStream;

    private long dataSize;

    private InputStream dataStream;

    public UploadRequest(int resourceId, long metaDataSize, InputStream metaDataStream, long dataSize,
                         InputStream dataStream) {
        this.resourceId = resourceId;
        this.metaDataSize = metaDataSize;
        this.metaDataStream = metaDataStream;
        this.dataSize = dataSize;
        this.dataStream = dataStream;
    }

    public int getResourceId() {
        return resourceId;
    }

    public long getMetaDataSize() {
        return metaDataSize;
    }

    public InputStream getMetaDataStream() {
        return metaDataStream;
    }

    public long getDataSize() {
        return dataSize;
    }

    public InputStream getDataStream() {
        return dataStream;
    }
}
