/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.server.plugins.drift;

import static org.rhq.enterprise.server.util.LookupUtil.getJPADriftServer;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;

/**
 * A drift server-side plugin component that the server uses to process drift files.
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class JPADriftServerPluginComponent implements DriftServerPluginFacet, ServerPluginComponent {

    private final Log log = LogFactory.getLog(JPADriftServerPluginComponent.class);

    @SuppressWarnings("unused")
    private ServerPluginContext context;

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        log.debug("The RHQ Drift plugin has been initialized!!! : " + this);
    }

    @Override
    public void start() {
        log.debug("The RHQ Drift plugin has started!!! : " + this);
    }

    @Override
    public void stop() {
        log.debug("The RHQ Drift plugin has stopped!!! : " + this);
    }

    @Override
    public void shutdown() {
        log.debug("The RHQ Drift plugin has been shut down!!! : " + this);
    }

    @Override
    public DriftSnapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria) {
        return getJPADriftServer().createSnapshot(subject, criteria);
    }

    @Override
    public PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject,
        DriftChangeSetCriteria criteria) {
        PageList<JPADriftChangeSet> results = getJPADriftServer().findDriftChangeSetsByCriteria(subject, criteria);
        return results;
    }

    @Override
    public PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {
        PageList<JPADrift> results = getJPADriftServer().findDriftsByCriteria(subject, criteria);
        return results;
    }

    @Override
    public PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria) {
        return getJPADriftServer().findDriftCompositesByCriteria(subject, criteria);
    }

    @Override
    public DriftFile getDriftFile(Subject subject, String hashId) throws Exception {
        return getJPADriftServer().getDriftFile(subject, hashId);
    }

    @Override
    public DriftChangeSetSummary saveChangeSet(Subject subject, int resourceId, File changeSetZip) throws Exception {
        return getJPADriftServer().storeChangeSet(subject, resourceId, changeSetZip);
    }

    @Override
    public void saveChangeSetFiles(Subject subject, File changeSetFilesZip) throws Exception {
        getJPADriftServer().storeFiles(subject, changeSetFilesZip);
    }

    @Override
    public void purgeByDriftConfigurationName(Subject subject, int resourceId, String driftConfigName) throws Exception {
        getJPADriftServer().purgeByDriftConfigurationName(subject, resourceId, driftConfigName);
    }

    @Override
    public int purgeOrphanedDriftFiles(Subject subject, long purgeMillis) {
        return getJPADriftServer().purgeOrphanedDriftFiles(subject, purgeMillis);
    }

    @Override
    public String getDriftFileBits(String hash) {
        return getJPADriftServer().getDriftFileBits(hash);
    }
}
