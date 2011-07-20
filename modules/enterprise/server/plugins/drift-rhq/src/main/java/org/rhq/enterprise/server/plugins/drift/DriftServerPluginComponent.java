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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.DriftJPACriteria;
import org.rhq.core.domain.criteria.DriftChangeSetJPACriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;

import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;

/**
 * A drift server-side plugin component that the server uses to process drift files.
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class DriftServerPluginComponent implements DriftServerPluginFacet {

    private final Log log = LogFactory.getLog(DriftServerPluginComponent.class);

    @SuppressWarnings("unused")
    private ServerPluginContext context;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        log.debug("The RHQ Drift plugin has been initialized!!! : " + this);
    }

    public void start() {
        log.debug("The RHQ Drift plugin has started!!! : " + this);
    }

    public void stop() {
        log.debug("The RHQ Drift plugin has stopped!!! : " + this);
    }

    public void shutdown() {
        log.debug("The RHQ Drift plugin has been shut down!!! : " + this);
    }

    @Override
    public PageList<DriftChangeSet> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria) {
        DriftChangeSetJPACriteria rhqCriteria = new DriftChangeSetJPACriteria();
        rhqCriteria.addFilterId(criteria.getFilterId());
        rhqCriteria.addFilterResourceId(criteria.getFilterResourceId());
        rhqCriteria.addFilterVersion(criteria.getFilterVersion());
        rhqCriteria.addFilterCategory(criteria.getFilterCategory());
        rhqCriteria.fetchDrifts(criteria.isFetchDrifts());

        PageList<? extends DriftChangeSet> results = getDriftManager().findDriftChangeSetsByCriteria(subject,
            rhqCriteria);
        return (PageList<DriftChangeSet>) results;
    }

    @Override
    public PageList<Drift> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {
        DriftJPACriteria rhqCriteria = new DriftJPACriteria();
        rhqCriteria.addFilterId(criteria.getFilterId());
        rhqCriteria.addFilterCategories(criteria.getFilterCategories().toArray(new DriftCategory[] {}));
        rhqCriteria.addFilterChangeSetId(criteria.getFilterChangeSetId());
        rhqCriteria.addFilterEndTime(criteria.getFilterEndTime());
        rhqCriteria.addFilterPath(criteria.getFilterPath());
        rhqCriteria.addFilterResourceIds(criteria.getFilterResourceIds().toArray(new Integer[] {}));
        rhqCriteria.addFilterStartTime(criteria.getFilterStartTime());
        rhqCriteria.fetchChangeSet(criteria.isFetchChangeSet());
        rhqCriteria.addSortCtime(criteria.getSortCtime());

        PageList<? extends Drift> results = getDriftManager().findDriftsByCriteria(subject, rhqCriteria);
        return (PageList<Drift>) results;
    }

    @Override
    public void saveChangeSet(int resourceId, File changeSetZip) throws Exception {
        DriftManagerLocal driftMgr = getDriftManager();
        driftMgr.storeChangeSet(resourceId, changeSetZip);
    }

    @Override
    public void saveChangeSetFiles(File changeSetFilesZip) throws Exception {
        DriftManagerLocal driftMgr = getDriftManager();
        driftMgr.storeFiles(changeSetFilesZip);
    }
}
