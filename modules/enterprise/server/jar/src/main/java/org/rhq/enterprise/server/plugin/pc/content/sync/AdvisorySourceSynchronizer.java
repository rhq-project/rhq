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
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.AdvisoryBuglist;
import org.rhq.core.domain.content.AdvisoryCVE;
import org.rhq.core.domain.content.AdvisoryPackage;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.AdvisoryManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryBugDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryCVEDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisorySource;
import org.rhq.enterprise.server.plugin.pc.content.AdvisorySyncReport;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Holds the methods necessary to interact with a plugin and execute its advisory related
 * synchronization tasks.
 *
 * @author Pradeep Kilambi
 */
public class AdvisorySourceSynchronizer {

    private final Log log = LogFactory.getLog(this.getClass());

    private RepoManagerLocal repoManager;
    private ContentSourceManagerLocal contentSourceManager;
    private SubjectManagerLocal subjectManager;

    private Repo repo;
    private ContentSource source;
    private ContentProvider provider;
    private PageControl pc = PageControl.getUnlimitedInstance();
    private Subject overlord = subjectManager.getOverlord();

    public AdvisorySourceSynchronizer(Repo repo, ContentSource source, ContentProvider provider) {
        this.repo = repo;
        this.source = source;
        this.provider = provider;

        repoManager = LookupUtil.getRepoManagerLocal();
        contentSourceManager = LookupUtil.getContentSourceManager();
        subjectManager = LookupUtil.getSubjectManager();
    }

    public void synchronizeAdvisoryMetadata() throws Exception {
        if (!(provider instanceof AdvisorySource)) {
            return;
        }

        AdvisorySource advisorySource = (AdvisorySource) provider;

        log.info("Synchronize Advisory: [" + source.getName() + "]: syncing repo [" + repo.getName() + "]");

        // Load existing distributions to send to source
        // --------------------------------------------
        long start = System.currentTimeMillis();

        List<Advisory> advs = repoManager.findAssociatedAdvisory(overlord, repo.getId(), pc);
        log.debug("Found " + advs.size() + " Advisory for repo " + repo.getId());

        AdvisorySyncReport advReport = new AdvisorySyncReport(repo.getId());
        List<AdvisoryDetails> advDetails = new ArrayList<AdvisoryDetails>(advs.size());
        translateDomainToDto(advs, advDetails);

        log.info("Synchronize Advisory: [" + source.getName() + "]: loaded existing list of size=[" + advs.size()
            + "] (" + (System.currentTimeMillis() - start) + ")ms");

        // Ask source to do the sync
        // --------------------------------------------
        start = System.currentTimeMillis();

        advisorySource.synchronizeAdvisory(repo.getName(), advReport, advDetails);

        log.info("Synchronize Advisory: [" + source.getName() + "]: got sync report from adapter=[" + advReport + "] ("
            + (System.currentTimeMillis() - start) + ")ms");

        ContentSourceSyncResults syncResults = new ContentSourceSyncResults(source);
        contentSourceManager.mergeAdvisorySyncReport(source, advReport, syncResults);
    }

    private void translateDomainToDto(List<Advisory> advs, List<AdvisoryDetails> advDetails) {
        AdvisoryManagerLocal advManager = LookupUtil.getAdvisoryManagerLocal();

        for (Advisory d : advs) {
            AdvisoryDetails detail = new AdvisoryDetails(d.getAdvisory(), d.getAdvisoryType(), d.getSynopsis());
            detail.setAdvisory(d.getAdvisory());
            detail.setAdvisory_name(d.getAdvisory_name());
            detail.setAdvisory_type(d.getAdvisoryType());
            detail.setDescription(d.getDescription());
            detail.setSolution(d.getSolution());
            detail.setIssue_date(d.getIssue_date());
            detail.setTopic(d.getTopic());
            detail.setUpdate_date(d.getUpdate_date());

            List<AdvisoryPackage> pkgs = advManager.findPackageByAdvisory(overlord, d.getId(), pc);

            for (AdvisoryPackage pkg : pkgs) {
                AdvisoryPackageDetails apkg = new AdvisoryPackageDetails(pkg.getAdvisory(), pkg.getPkg());
                detail.addPkg(apkg);
            }

            List<AdvisoryCVE> cves = advManager.getAdvisoryCVEByAdvId(overlord, d.getId(), pc);

            for (AdvisoryCVE cve : cves) {
                AdvisoryCVEDetails acve = new AdvisoryCVEDetails(cve.getAdvisory(), cve.getCVE());
                detail.addCVE(acve);
            }

            List<AdvisoryBuglist> abugs = advManager.getAdvisoryBuglistByAdvId(overlord, d.getId());
            for (AdvisoryBuglist abug : abugs) {
                AdvisoryBugDetails abugdetail = new AdvisoryBugDetails(abug.getAdvisory(), abug.getBugid());
                detail.addBug(abugdetail);
            }
            advDetails.add(detail);
        }
    }
}
