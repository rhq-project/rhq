/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.content;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.rhq.core.domain.content.ContentSyncResults;
import org.rhq.core.domain.content.ContentSyncStatus;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * ContentManagerHelper - Helper class to contain common methods needed by the Content managers.
 */
public class ContentManagerHelper {
    private EntityManager entityManager;

    public ContentManagerHelper(EntityManager managerIn) {
        this.entityManager = managerIn;
    }

    public static ContentServerPluginContainer getPluginContainer() throws Exception {
        ContentServerPluginContainer pc = null;

        try {
            ServerPluginServiceManagement mbean = LookupUtil.getServerPluginService();
            if (!mbean.isMasterPluginContainerStarted()) {
                throw new IllegalStateException("The master plugin container is not started!");
            }

            MasterServerPluginContainer master = mbean.getMasterPluginContainer();
            pc = master.getPluginContainerByClass(ContentServerPluginContainer.class);
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            throw new Exception("Cannot obtain the content source plugin container", e);
        }

        if (pc == null) {
            throw new Exception("Content source plugin container is null!");
        }

        return pc;
    }

    public static ResourcePackageDetails installedPackageToDetails(InstalledPackage installedPackage) {
        PackageVersion packageVersion = installedPackage.getPackageVersion();
        ResourcePackageDetails details = packageVersionToDetails(packageVersion);

        return details;
    }

    public static ResourcePackageDetails packageVersionToDetails(PackageVersion packageVersion) {
        Package generalPackage = packageVersion.getGeneralPackage();

        PackageDetailsKey key = new PackageDetailsKey(generalPackage.getName(), packageVersion.getVersion(),
            packageVersion.getGeneralPackage().getPackageType().getName(), packageVersion.getArchitecture().getName());
        ResourcePackageDetails details = new ResourcePackageDetails(key);

        details.setClassification(generalPackage.getClassification());
        details.setDisplayName(packageVersion.getDisplayName());
        details.setFileCreatedDate(packageVersion.getFileCreatedDate());
        details.setFileName(packageVersion.getFileName());
        details.setFileSize(packageVersion.getFileSize());
        details.setLicenseName(packageVersion.getLicenseName());
        details.setLicenseVersion(packageVersion.getLicenseVersion());
        details.setLongDescription(packageVersion.getLongDescription());
        details.setMD5(packageVersion.getMD5());
        details.setMetadata(packageVersion.getMetadata());
        details.setSHA256(packageVersion.getSHA256());
        details.setShortDescription(packageVersion.getShortDescription());
        Long created = packageVersion.getFileCreatedDate();
        if (created != null) {
            details.setInstallationTimestamp(created.longValue());
        }

        return details;
    }

    public ContentSyncResults persistSyncResults(Query inProgressQuery, ContentSyncResults results) {
        List<ContentSyncResults> inprogressList;
        try {
            inprogressList = inProgressQuery.getResultList(); // will be ordered by start time descending
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        boolean alreadyInProgress = false; // will be true if there is already a sync in progress

        if (inprogressList.size() > 0) {
            // If there is 1 in progress and we are being asked to persist one in progress,
            // then we either abort the persist if its recent, or we "kill" the old one by marking it failed.
            // We mark any others after the 1st one as a failure. How can you have more than 1 inprogress at
            // the same time? We shouldn't under normal circumstances, this is what we are trying to avoid in
            // this method - so we mark the status as failure because we assume something drastically bad
            // happened that left them in a bad state which will most likely never change unless we do it here.
            // If a content source sync takes longer than 24 hours, then we've made a bad assumption here and
            // this code needs to change - though I doubt any content source will take 24 hours to sync.
            if (results.getStatus() == ContentSyncStatus.INPROGRESS) {
                if ((System.currentTimeMillis() - inprogressList.get(0).getStartTime()) < (1000 * 60 * 60 * 24)) {
                    alreadyInProgress = true;
                    inprogressList.remove(0); // we need to leave this one as-is, so get rid of it from list
                }
            }

            // take this time to mark all old inprogress results as failed
            for (ContentSyncResults inprogress : inprogressList) {
                inprogress.setStatus(ContentSyncStatus.FAILURE);
                inprogress.setEndTime(System.currentTimeMillis());
                inprogress.setResults("This synchronization seems to have stalled or ended abnormally.");
            }
        }

        ContentSyncResults persistedResults = null; // leave it as null if something is already in progress

        if (!alreadyInProgress) {
            entityManager.persist(results);
            persistedResults = (ContentSyncResults) results;
        }

        return persistedResults;
    }
}