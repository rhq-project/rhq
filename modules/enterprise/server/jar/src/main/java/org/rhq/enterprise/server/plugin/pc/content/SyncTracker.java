/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.plugin.pc.content;

import org.rhq.core.domain.content.ContentSyncStatus;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.util.progresswatch.ProgressWatcher;

/**
 * Container class to hold the classes required to track the progress of a ContentProvider Sync.
 * @author mmccune
 *
 */
public class SyncTracker {

    private int repoId;
    private RepoSyncResults repoSyncResults;
    private ProgressWatcher progressWatcher;
    private int packageSyncCount;

    /**
     * @param repoSyncResults
     * @param progressWatcher
     */
    public SyncTracker(RepoSyncResults repoSyncResultsIn, ProgressWatcher progressWatcherIn) {
        super();
        this.repoId = repoSyncResultsIn.getRepo().getId();
        this.repoSyncResults = repoSyncResultsIn;
        this.progressWatcher = progressWatcherIn;
        this.progressWatcher.start();
    }

    public void addAdvisoryMetadataWork(ContentProvider provider) {
        SyncProgressWeight sw = provider.getSyncProgressWeight();
        if (this.getPackageSyncCount() == 0) {
            this.getProgressWatcher().addWork(sw.getAdvisoryWeight() * 10);
        } else {
            this.getProgressWatcher().addWork(sw.getAdvisoryWeight() * this.getPackageSyncCount());
        }

    }

    public void finishAdvisoryMetadataWork(ContentProvider provider) {
        if (this.getPackageSyncCount() == 0) {
            this.getProgressWatcher().finishWork(provider.getSyncProgressWeight().getAdvisoryWeight() * 10);
        } else {
            this.getProgressWatcher().finishWork(
                provider.getSyncProgressWeight().getAdvisoryWeight() * this.getPackageSyncCount());
        }
    }

    public void addPackageBitsWork(ContentProvider provider) {
        SyncProgressWeight sw = provider.getSyncProgressWeight();
        this.getProgressWatcher().addWork(sw.getPackageBitsWeight() * this.getPackageSyncCount());
    }

    /**
     * @return the repoSyncResults
     */
    public RepoSyncResults getRepoSyncResults() {
        return repoSyncResults;
    }

    /**
     * @return the progressWatcher
     */
    public ProgressWatcher getProgressWatcher() {
        return progressWatcher;
    }

    /**
     * Set the RepoSyncResults
     * @param syncResultsIn
     */
    public void setRepoSyncResults(RepoSyncResults syncResultsIn) {
        this.repoSyncResults = syncResultsIn;
    }

    /**
     * Set the Results field on the RepoSyncResults.

     * @param resultsIn to set
     */
    public void setResults(String resultsIn) {
        this.repoSyncResults.setResults(resultsIn);

    }

    /**
     * passthrough to RepoSyncResults.setStatus()
     * 
     * @param statusIn
     */
    public void setStatus(ContentSyncStatus statusIn) {
        this.repoSyncResults.setStatus(statusIn);

    }

    /**
     * @return the packageSyncCount
     */
    public int getPackageSyncCount() {
        return packageSyncCount;
    }

    /**
     * @param packageSyncCount the packageSyncCount to set
     */
    public void setPackageSyncCount(int packageSyncCount) {
        this.packageSyncCount = packageSyncCount;
    }

    /**
     * RepoId we are tracking
     * @return int repoId
     */
    public int getRepoId() {

        return this.repoId;
    }
}
