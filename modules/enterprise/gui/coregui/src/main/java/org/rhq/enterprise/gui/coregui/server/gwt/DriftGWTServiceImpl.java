/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.drift.DriftServerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jay Shaughnessy
 */
public class DriftGWTServiceImpl extends AbstractGWTServiceImpl implements DriftGWTService {
    private static final long serialVersionUID = 1L;

    private DriftManagerLocal driftManager = LookupUtil.getDriftManager();

    private DriftServerLocal driftServer = LookupUtil.getDriftServer();

    @Override
    public int deleteDrifts(String[] driftIds) throws RuntimeException {
        try {
            return this.driftManager.deleteDrifts(getSessionSubject(), driftIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int deleteDriftsByContext(EntityContext entityContext) throws RuntimeException {
        try {
            return this.driftManager.deleteDriftsByContext(getSessionSubject(), entityContext);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int deleteDriftConfigurations(int[] driftConfigIds) throws RuntimeException {
        try {
            // TODO
            //return this.driftManager.deleteDriftConfigurations(getSessionSubject(), driftConfigIds);
            return 0;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int deleteDriftConfigurationsByContext(EntityContext entityContext) throws RuntimeException {
        try {
            // TODO
            //return this.driftManager.deleteDriftConfigurationsByContext(getSessionSubject(), entityContext);
            return 0;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void detectDrift(EntityContext entityContext, DriftConfiguration driftConfig) {
        try {
            this.driftServer.detectDrift(getSessionSubject(), entityContext, driftConfig);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<DriftChangeSet> findDriftChangeSetsByCriteria(DriftChangeSetCriteria criteria) {
        try {
            PageList<DriftChangeSet> results = driftServer.findDriftChangeSetsByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftChangeSetsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<Drift> findDriftsByCriteria(DriftCriteria criteria) throws RuntimeException {
        try {
            PageList<Drift> results = driftServer.findDriftsByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public DriftConfiguration getDriftConfiguration(EntityContext entityContext, int driftConfigId)
        throws RuntimeException {
        try {
            DriftConfiguration driftConfig = driftServer.getDriftConfiguration(getSessionSubject(), entityContext,
                driftConfigId);
            return SerialUtility.prepare(driftConfig, "DriftService.getDriftConfiguration");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updateDriftConfiguration(EntityContext entityContext, DriftConfiguration driftConfig) {
        try {
            this.driftServer.updateDriftConfiguration(getSessionSubject(), entityContext, driftConfig);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}