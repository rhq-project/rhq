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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.RepoImportReport;
import org.rhq.enterprise.server.plugin.pc.content.RepoSource;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Holds the methods necessary to interact with a plugin and execute its repo-related
 * synchronization tasks.
 *
 * @author Jason Dobies
 */
public class RepoSourceSynchronizer {

    private final Log log = LogFactory.getLog(this.getClass());

    private RepoManagerLocal repoManager;
    private SubjectManagerLocal subjectManager;

    private ContentSource source;
    private ContentProvider provider;

    public RepoSourceSynchronizer(ContentSource source, ContentProvider provider) {
        this.source = source;
        this.provider = provider;

        repoManager = LookupUtil.getRepoManagerLocal();
        subjectManager = LookupUtil.getSubjectManager();
    }

    public void synchronizeCandidateRepos(StringBuilder progress) throws Exception {

        if (!(provider instanceof RepoSource)) {
            // Nothing to do.
            return;
        }

        progress.append(new Date()).append(": ");
        progress.append("Asking content source for new repositories to import...\n");

        RepoSource repoSource = (RepoSource) provider;

        long start = System.currentTimeMillis();

        // Call to the plugin
        RepoImportReport report = repoSource.importRepos();

        Subject overlord = subjectManager.getOverlord();
        repoManager.processRepoImportReport(overlord, report, source.getId(), progress);

        log.info("importRepos: [" + source.getName() + "]: report has been merged ("
            + (System.currentTimeMillis() - start) + ")ms");
    }
}
