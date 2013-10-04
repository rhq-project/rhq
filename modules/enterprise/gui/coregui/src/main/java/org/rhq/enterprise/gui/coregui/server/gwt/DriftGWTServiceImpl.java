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
package org.rhq.coregui.server.gwt;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComposite;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftDetails;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.gwt.DriftGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.drift.DriftTemplateManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jay Shaughnessy
 */
public class DriftGWTServiceImpl extends AbstractGWTServiceImpl implements DriftGWTService {
    private static final long serialVersionUID = 1L;

    private DriftManagerLocal driftManager = LookupUtil.getDriftManager();
    private DriftTemplateManagerLocal driftTemplateManager = LookupUtil.getDriftTemplateManager();

    @Override
    public DriftDefinitionTemplate createTemplate(int resourceTypeId, DriftDefinition definition)
        throws RuntimeException {
        try {
            DriftDefinitionTemplate result = this.driftTemplateManager.createTemplate(getSessionSubject(),
                resourceTypeId, true, definition);
            return SerialUtility.prepare(result, "DriftService.createTemplate");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public int deleteDriftDefinitionsByContext(EntityContext entityContext, String[] driftDefinitionNames)
        throws RuntimeException {
        try {
            for (String driftDefName : driftDefinitionNames) {
                this.driftManager.deleteDriftDefinition(getSessionSubject(), entityContext, driftDefName);
            }
            return driftDefinitionNames.length;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void detectDrift(EntityContext entityContext, DriftDefinition driftDef) throws RuntimeException {
        try {
            this.driftManager.detectDrift(getSessionSubject(), entityContext, driftDef);
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
    public PageList<DriftDefinition> findDriftDefinitionsByCriteria(DriftDefinitionCriteria criteria)
        throws RuntimeException {
        try {
            PageList<DriftDefinition> results = driftManager.findDriftDefinitionsByCriteria(getSessionSubject(),
                criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftDefinitionsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<DriftDefinitionComposite> findDriftDefinitionCompositesByCriteria(DriftDefinitionCriteria criteria)
        throws RuntimeException {
        try {
            PageList<DriftDefinitionComposite> results = driftManager.findDriftDefinitionCompositesByCriteria(
                getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftDefinitionCompositesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<DriftDefinitionTemplate> findDriftDefinitionTemplatesByCriteria(
        DriftDefinitionTemplateCriteria criteria) throws RuntimeException {
        try {
            PageList<DriftDefinitionTemplate> results = driftTemplateManager.findTemplatesByCriteria(
                getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "DriftService.findDriftDefinitionTemplatesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deleteDriftDefinitionTemplates(int[] templateIds) throws RuntimeException {
        try {
            for (int templateId : templateIds) {
                driftTemplateManager.deleteTemplate(getSessionSubject(), templateId);
            }
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public DriftSnapshot getSnapshot(DriftSnapshotRequest request) throws RuntimeException {
        try {
            DriftSnapshot results = driftManager.getSnapshot(getSessionSubject(), request);
            return SerialUtility.prepare(results, "DriftService.getSnapshot");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public DriftDefinition getDriftDefinition(int driftDefId) throws RuntimeException {
        try {
            DriftDefinition driftDef = driftManager.getDriftDefinition(getSessionSubject(), driftDefId);
            return SerialUtility.prepare(driftDef, "DriftService.getDriftDefinition");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public String getDriftFileBits(String hash) throws RuntimeException {
        try {
            return driftManager.getDriftFileBits(getSessionSubject(), hash);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public FileDiffReport generateUnifiedDiff(Drift<?, ?> drift) throws RuntimeException {
        try {
            return driftManager.generateUnifiedDiff(getSessionSubject(), drift);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public FileDiffReport generateUnifiedDiffByIds(String driftId1, String driftId2) throws RuntimeException {
        try {
            return driftManager.generateUnifiedDiffByIds(getSessionSubject(), driftId1, driftId2);
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
    public boolean isBinaryFile(Drift<?, ?> drift) throws RuntimeException {
        try {
            return driftManager.isBinaryFile(getSessionSubject(), drift);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void pinSnapshot(int driftDefId, int snapshotVersion) throws RuntimeException {
        try {
            driftManager.pinSnapshot(getSessionSubject(), driftDefId, snapshotVersion);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void pinTemplate(int templateId, int snapshotDriftDefId, int snapshotVersion) throws RuntimeException {

        try {
            driftTemplateManager.pinTemplate(getSessionSubject(), templateId, snapshotDriftDefId, snapshotVersion);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updateDriftDefinition(EntityContext entityContext, DriftDefinition driftDef) throws RuntimeException {
        try {
            this.driftManager.updateDriftDefinition(getSessionSubject(), entityContext, driftDef);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updateTemplate(DriftDefinitionTemplate driftDefTemplate) throws RuntimeException {
        try {
            this.driftTemplateManager.updateTemplate(getSessionSubject(), driftDefTemplate);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}