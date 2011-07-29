package org.rhq.core.domain.drift;

import java.io.Serializable;

public interface DriftFile extends Serializable {

    String getHashId();

    void setHashId(String hashId);

    Long getCtime();

    Long getDataSize();

    void setDataSize(Long size);

    DriftFileStatus getStatus();

    void setStatus(DriftFileStatus status);
}
