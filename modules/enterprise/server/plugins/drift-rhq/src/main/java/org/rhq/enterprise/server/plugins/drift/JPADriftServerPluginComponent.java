/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.plugins.drift;

import static org.rhq.enterprise.server.util.LookupUtil.getJPADriftServer;
import static org.rhq.enterprise.server.util.LookupUtil.getPurgeManager;

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
    private static final Log LOG = LogFactory.getLog(JPADriftServerPluginComponent.class);

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The RHQ Drift plugin has been initialized!!! : " + this);
        }
    }

    @Override
    public void start() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The RHQ Drift plugin has started!!! : " + this);
        }
    }

    @Override
    public void stop() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The RHQ Drift plugin has stopped!!! : " + this);
        }
    }

    @Override
    public void shutdown() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The RHQ Drift plugin has been shut down!!! : " + this);
        }
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
    public String persistChangeSet(Subject subject, DriftChangeSet<?> changeSet) {
        return getJPADriftServer().persistChangeSet(subject, changeSet);
    }

    @Override
    public String copyChangeSet(Subject subject, String changeSetId, int driftDefId, int resourceId) {
        return getJPADriftServer().copyChangeSet(subject, changeSetId, driftDefId, resourceId);
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
    public void purgeByDriftDefinitionName(Subject subject, int resourceId, String driftDefName) throws Exception {
        getJPADriftServer().purgeByDriftDefinitionName(subject, resourceId, driftDefName);
    }

    @Override
    public int purgeOrphanedDriftFiles(Subject subject, long purgeMillis) {
        return getPurgeManager().purgeOrphanedDriftFilesInDatabase(purgeMillis);
    }

    @Override
    public String getDriftFileBits(Subject subject, String hash) {
        return getJPADriftServer().getDriftFileBits(hash);
    }

    @Override
    public byte[] getDriftFileAsByteArray(Subject subject, String hash) {
        return getJPADriftServer().getDriftFileAsByteArray(hash);
    }
}
