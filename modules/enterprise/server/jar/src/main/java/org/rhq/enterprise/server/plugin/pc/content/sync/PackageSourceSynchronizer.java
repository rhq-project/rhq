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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.PackageVersionContentSourcePK;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetailsKey;
import org.rhq.enterprise.server.plugin.pc.content.PackageSource;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Holds the methods necessary to interact with a plugin and execute its package related
 * synchronization tasks.
 *
 * @author Jason Dobies
 */
public class PackageSourceSynchronizer {

    private final Log log = LogFactory.getLog(this.getClass());

    private ContentSourceManagerLocal contentSourceManager;
    private RepoManagerLocal repoManager;
    private SubjectManagerLocal subjectManager;

    private Repo repo;
    private ContentSource source;
    private ContentProvider provider;

    public PackageSourceSynchronizer(Repo repo, ContentSource source, ContentProvider provider) {
        this.repo = repo;
        this.source = source;
        this.provider = provider;

        contentSourceManager = LookupUtil.getContentSourceManager();
        subjectManager = LookupUtil.getSubjectManager();
        repoManager = LookupUtil.getRepoManagerLocal();
    }

    public RepoSyncResults synchronizePackageMetadata(RepoSyncResults syncResults) throws Exception {
        if (!(provider instanceof PackageSource)) {
            return syncResults;
        }

        PackageSource packageSource = (PackageSource) provider;

        // Load packages to send to package source
        // --------------------------------------------
        long start = System.currentTimeMillis();

        List<PackageVersionContentSource> existingPVCS; // already know about this source
        Set<ContentProviderPackageDetails> allDetails; // send to plugin
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> keyPVCSMap;

        Subject overlord = subjectManager.getOverlord();
        existingPVCS = contentSourceManager.getPackageVersionsFromContentSourceForRepo(overlord, source.getId(), repo
            .getId());

        int existingCount = existingPVCS.size();
        keyPVCSMap = new HashMap<ContentProviderPackageDetailsKey, PackageVersionContentSource>(existingCount);
        allDetails = new HashSet<ContentProviderPackageDetails>(existingCount);

        translateDomainToDto(existingPVCS, allDetails, keyPVCSMap);

        log.info("Synchronize Packages: [" + source.getName() + "]: loaded existing list of size=[" + existingCount
            + "] (" + (System.currentTimeMillis() - start) + ")ms");

        // Ask source to do the sync
        // --------------------------------------------
        start = System.currentTimeMillis();

        PackageSyncReport report = new PackageSyncReport();
        packageSource.synchronizePackages(repo.getName(), report, allDetails);

        log.info("Synchronize Packages: [" + source.getName() + "]: got sync report from adapter=[" + report + "] ("
            + (System.currentTimeMillis() - start) + ")ms");

        // Merge in the results of the synchronization
        // --------------------------------------------
        start = System.currentTimeMillis();
        // RepoSyncResults syncResults = new RepoSyncResults(repo);
        syncResults = contentSourceManager.mergePackageSyncReport(source, repo, report, keyPVCSMap, syncResults);

        log.info("Synchronize Packages: [" + source.getName() + "]: merged sync report=("
            + (System.currentTimeMillis() - start) + ")ms");
        return syncResults;
    }

    public RepoSyncResults synchronizePackageBits(RepoSyncResults syncResults) throws Exception {

        // Determine if the sync even needs to take place
        if (!(provider instanceof PackageSource)) {
            return syncResults;
        }

        if (source.getDownloadMode() == DownloadMode.NEVER) {
            String msg = "Download mode of NEVER for source [" + source.getName() + "], skipping "
                + "package bits sync for repo [" + repo.getName() + "]";
            log.info(msg);
            syncResults.appendResults(msg);
            syncResults = repoManager.mergeRepoSyncResults(syncResults);
            return syncResults;
        }

        if (source.isLazyLoad()) {
            String msg = "Lazy load enabled for source [" + source.getName() + "], skipping "
                + "package bits sync for repo [" + repo.getName() + "]";
            log.info(msg);
            syncResults.appendResults(msg);
            syncResults = repoManager.mergeRepoSyncResults(syncResults);
            return syncResults;
        }

        long start;

        // Determine which packages need bit synccing
        /* If there become performance issues here, consider paging. So we have to load them all
           in at once or can we do them in chunks?
         */
        start = System.currentTimeMillis();
        PageControl pc = PageControl.getUnlimitedInstance();
        Subject overlord = subjectManager.getOverlord();

        List<PackageVersionContentSource> packageVersionContentSources = contentSourceManager
            .getUnloadedPackageVersionsFromContentSourceInRepo(overlord, source.getId(), repo.getId(), pc);
        String msg = "Synchronize Package Bits: [" + source.getName() + "], repo [" + repo.getName()
            + "]: loaded package list for sync (" + (System.currentTimeMillis() - start) + ")ms";
        log.info(msg);
        syncResults.appendResults(msg);
        syncResults = repoManager.mergeRepoSyncResults(syncResults);

        // Download the bits for each unloaded package version. Abort the entire download if we
        // fail getting just one package.

        // Note: This can potentially take a very long time.
        for (PackageVersionContentSource item : packageVersionContentSources) {
            PackageVersionContentSourcePK pk = item.getPackageVersionContentSourcePK();

            try {
                log.info("Downloading package version [" + pk.getPackageVersion() + "] located at ["
                    + item.getLocation() + "]" + "] from [" + pk.getContentSource() + "]...");

                syncResults.appendResults("Downloading package version [" + pk.getPackageVersion() + "] located at ["
                    + item.getLocation() + "]");
                syncResults = repoManager.mergeRepoSyncResults(syncResults);

                overlord = subjectManager.getOverlord();
                contentSourceManager.downloadPackageBits(overlord, item);
            } catch (Exception e) {
                String errorMsg = "Failed to load package bits for package version [" + pk.getPackageVersion()
                    + "] from content source [" + pk.getContentSource() + "] at location [" + item.getLocation() + "]."
                    + "No more packages will be downloaded for this content source.";

                throw new Exception(errorMsg, e);
            }
        }

        log.info("All package bits for content source [" + source.getName() + "] have been downloaded."
            + "The downloads started at [" + new Date(start) + "] and ended at [" + new Date() + "]");

        return syncResults;

    }

    /**
     * Translates the domain representation of a list of packages into DTOs used in the plugin APIs.
     * During the translation the two collections (allDetails and keyPVCSMap) will be populated with
     * different views into the data.
     *
     * @param existingPVCS list of packages in the form of the wrapper object linking them to
     *                     the content source
     * @param allDetails   set of all translated package DTOs
     * @param keyPVCSMap   mapping of package version key to package domain object
     */
    private void translateDomainToDto(List<PackageVersionContentSource> existingPVCS,
        Set<ContentProviderPackageDetails> allDetails,
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> keyPVCSMap) {

        for (PackageVersionContentSource pvcs : existingPVCS) {
            PackageVersion pv = pvcs.getPackageVersionContentSourcePK().getPackageVersion();
            org.rhq.core.domain.content.Package p = pv.getGeneralPackage();
            ResourceType rt = p.getPackageType().getResourceType();

            ContentProviderPackageDetailsKey key;
            key = new ContentProviderPackageDetailsKey(p.getName(), pv.getVersion(), p.getPackageType().getName(), pv
                .getArchitecture().getName(), rt.getName(), rt.getPlugin());

            ContentProviderPackageDetails details = new ContentProviderPackageDetails(key);
            details.setClassification(pv.getGeneralPackage().getClassification());
            details.setDisplayName(pv.getDisplayName());
            details.setDisplayVersion(pv.getDisplayVersion());
            details.setExtraProperties(pv.getExtraProperties());
            details.setFileCreatedDate(pv.getFileCreatedDate());
            details.setFileName(pv.getFileName());
            details.setFileSize(pv.getFileSize());
            details.setLicenseName(pv.getLicenseName());
            details.setLicenseVersion(pv.getLicenseVersion());
            details.setLocation(pvcs.getLocation());
            details.setLongDescription(pv.getLongDescription());
            details.setMD5(pv.getMD5());
            details.setMetadata(pv.getMetadata());
            details.setSHA256(pv.getSHA256());
            details.setShortDescription(pv.getShortDescription());

            allDetails.add(details);
            keyPVCSMap.put(key, pvcs);
        }
    }

}
