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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftConfigurationCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftDetails;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jay Shaughnessy
 */
public class DriftGWTServiceImpl extends AbstractGWTServiceImpl implements DriftGWTService {
    private static final long serialVersionUID = 1L;

    private DriftManagerLocal driftManager = LookupUtil.getDriftManager();

    @Override
    public int deleteDriftConfigurationsByContext(EntityContext entityContext, String[] driftConfigNames)
        throws RuntimeException {
        try {
            for (String driftConfigName : driftConfigNames) {
                this.driftManager.deleteDriftConfiguration(getSessionSubject(), entityContext, driftConfigName);
            }
            return driftConfigNames.length;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void detectDrift(EntityContext entityContext, DriftConfiguration driftConfig) throws RuntimeException {
        try {
            this.driftManager.detectDrift(getSessionSubject(), entityContext, driftConfig);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(GenericDriftChangeSetCriteria criteria)
        throws RuntimeException {
        try {
            PageList<? extends DriftChangeSet<?>> results = driftManager.findDriftChangeSetsByCriteria(
                getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftChangeSetsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<? extends Drift<?, ?>> findDriftsByCriteria(GenericDriftCriteria criteria) throws RuntimeException {
        try {
            PageList<? extends Drift<?, ?>> results = driftManager.findDriftsByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<DriftComposite> findDriftCompositesByCriteria(GenericDriftCriteria criteria)
        throws RuntimeException {
        try {
            PageList<DriftComposite> results = driftManager
                .findDriftCompositesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftCompositesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<DriftConfiguration> findDriftConfigurationsByCriteria(DriftConfigurationCriteria criteria)
        throws RuntimeException {
        try {
            PageList<DriftConfiguration> results = driftManager.findDriftConfigurationsByCriteria(getSessionSubject(),
                criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftConfigurationsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public DriftSnapshot createSnapshot(Subject subject, GenericDriftChangeSetCriteria criteria)
        throws RuntimeException {
        try {
            return driftManager.createSnapshot(subject, criteria);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public DriftConfiguration getDriftConfiguration(int driftConfigId) throws RuntimeException {
        try {
            DriftConfiguration driftConfig = driftManager.getDriftConfiguration(getSessionSubject(), driftConfigId);
            return SerialUtility.prepare(driftConfig, "DriftService.getDriftConfiguration");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updateDriftConfiguration(EntityContext entityContext, DriftConfiguration driftConfig)
        throws RuntimeException {
        try {
            this.driftManager.updateDriftConfiguration(getSessionSubject(), entityContext, driftConfig);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public String getDriftFileBits(String hash) throws RuntimeException {
        try {
            return driftManager.getDriftFileBits(hash);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public FileDiffReport generateUnifiedDiff(Drift drift) throws RuntimeException {
        try {
            return driftManager.generateUnifiedDiff(drift);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public DriftDetails getDriftDetails(String driftId) throws RuntimeException {
        try {
            DriftDetails details = driftManager.getDriftDetails(getSessionSubject(), driftId);
            return SerialUtility.prepare(details, "DriftService.getDriftDetails");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public boolean isBinaryFile(Drift drift) throws RuntimeException {
        try {
            return driftManager.isBinaryFile(drift);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}