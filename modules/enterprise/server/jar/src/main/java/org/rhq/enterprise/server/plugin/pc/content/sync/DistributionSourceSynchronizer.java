/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.enterprise.server.plugin.pc.content.sync;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.DistributionManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.DistributionDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionFileDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSource;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.SyncTracker;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Holds the methods necessary to interact with a plugin and execute its distribution related
 * synchronization tasks.
 *
 * @author Jason Dobies
 */
public class DistributionSourceSynchronizer {

    private final Log log = LogFactory.getLog(this.getClass());

    private RepoManagerLocal repoManager;
    private ContentSourceManagerLocal contentSourceManager;
    private SubjectManagerLocal subjectManager;

    private Repo repo;
    private ContentSource source;
    private ContentProvider provider;

    public DistributionSourceSynchronizer(Repo repo, ContentSource source, ContentProvider provider) {
        this.repo = repo;
        this.source = source;
        this.provider = provider;

        repoManager = LookupUtil.getRepoManagerLocal();
        contentSourceManager = LookupUtil.getContentSourceManager();
        subjectManager = LookupUtil.getSubjectManager();
    }

    public SyncTracker synchronizeDistributionMetadata(SyncTracker tracker) throws Exception {
        if (!(provider instanceof DistributionSource)) {
            return tracker;
        }

        DistributionSource distributionSource = (DistributionSource) provider;

        String msg = "Synchronize Distributions: [" + source.getName() + "]: syncing repo [" + repo.getName() + "]";
        log.info(msg);
        tracker.getRepoSyncResults().appendResults(msg);

        // Load existing distributions to send to source
        // --------------------------------------------
        long start = System.currentTimeMillis();

        PageControl pc = PageControl.getUnlimitedInstance();
        Subject overlord = subjectManager.getOverlord();
        List<Distribution> dists = repoManager.findAssociatedDistributions(overlord, repo.getId(), pc);
        log.debug("Found " + dists.size() + " distributions for repo " + repo.getId());

        DistributionSyncReport distReport = new DistributionSyncReport(repo.getId());
        List<DistributionDetails> distDetails = new ArrayList<DistributionDetails>(dists.size());
        translateDomainToDto(dists, distDetails);

        log.info("Synchronize Distributions: [" + repo.getName() + "]: loaded existing list of size=[" + dists.size()
            + "] (" + (System.currentTimeMillis() - start) + ")ms");

        // Ask source to do the sync
        // --------------------------------------------
        start = System.currentTimeMillis();

        distributionSource.synchronizeDistribution(repo.getName(), distReport, distDetails);

        log.info("Synchronize Distributions: [" + repo.getName() + "]: got sync report from adapter=[" + distReport
            + "] (" + (System.currentTimeMillis() - start) + ")ms");

        RepoSyncResults syncResults = contentSourceManager.mergeDistributionSyncReport(source, distReport, tracker
            .getRepoSyncResults());
        tracker.setRepoSyncResults(syncResults);
        tracker.getProgressWatcher().finishWork(provider.getSyncProgressWeight().getDistribtutionBitsWeight());
        return tracker;
    }

    public SyncTracker synchronizeDistributionBits(SyncTracker tracker) throws Exception {
        if (!(provider instanceof DistributionSource)) {
            return tracker;
        }

        Subject overlord = subjectManager.getOverlord();
        tracker.getRepoSyncResults().appendResults(
            "Synchronize Distributions: [" + repo.getName() + " Starting Distribution bits download.");
        tracker.setRepoSyncResults(repoManager.mergeRepoSyncResults(tracker.getRepoSyncResults()));
        contentSourceManager.downloadDistributionBits(overlord, source);
        tracker.getRepoSyncResults().appendResults(
            "Synchronize Distributions: [" + repo.getName() + " finished bits download.");
        tracker.getProgressWatcher().finishWork(provider.getSyncProgressWeight().getDistribtutionBitsWeight());

        return tracker;
    }

    private void translateDomainToDto(List<Distribution> dists, List<DistributionDetails> distDetails) {
        DistributionManagerLocal distManager = LookupUtil.getDistributionManagerLocal();

        for (Distribution d : dists) {
            DistributionDetails detail = new DistributionDetails(d.getLabel(), d.getDistributionType().getName());
            detail.setLabel(d.getLabel());
            detail.setDistributionPath(d.getBasePath());
            detail.setDescription(d.getDistributionType().getDescription());
            List<DistributionFile> files = distManager.getDistributionFilesByDistId(d.getId());
            for (DistributionFile f : files) {
                DistributionFileDetails dfd = new DistributionFileDetails(f.getRelativeFilename(), f.getLastModified(),
                    f.getMd5sum());
                detail.addFile(dfd);
            }
            distDetails.add(detail);
        }
    }

}
