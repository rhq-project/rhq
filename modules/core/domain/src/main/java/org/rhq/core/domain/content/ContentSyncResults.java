package org.rhq.core.domain.content;

public interface ContentSyncResults {

    /**
     * Get the SyncStatus for these results.
     * @return ContentSyncStatus enum
     */
    ContentSyncStatus getStatus();

    long getStartTime();

    void setStatus(ContentSyncStatus status);

    void setEndTime(Long endTime);

    void setResults(String string);
}
