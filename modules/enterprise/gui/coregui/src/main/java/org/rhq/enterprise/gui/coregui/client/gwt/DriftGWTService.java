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
package org.rhq.enterprise.gui.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

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

/**
 * @author Jay Shaughnessy
 */
public interface DriftGWTService extends RemoteService {

    DriftDefinitionTemplate createTemplate(int resourceTypeId, DriftDefinition definition) throws RuntimeException;

    /**
     * Delete all named drift definitions for the specified context if the current user has permission to do so (i.e. either
     * the MANAGE_INVENTORY global permission, or the MANAGE_DRIFT permission for all corresponding resources).
     *
     * @param entityContext the context for deletion
     * @param driftDefNames the names of the definitions to delete
     * @return the number of drift definisions deleted
     */
    int deleteDriftDefinitionsByContext(EntityContext entityContext, String[] driftDefNames) throws RuntimeException;

    /**
     * One time on-demand request to detect drift on the specified entities, using the supplied def.
     * 
     * @param entityContext
     * @param driftDef
     * @throws RuntimeException
     */
    void detectDrift(EntityContext entityContext, DriftDefinition driftDef) throws RuntimeException;

    /**
     * Find all drift changesets that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return all drift changesets that matches the specified criteria
     */
    PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(GenericDriftChangeSetCriteria criteria)
        throws RuntimeException;

    PageList<DriftComposite> findDriftCompositesByCriteria(GenericDriftCriteria criteria) throws RuntimeException;

    /**
     * Find all drift definitions that match the specified criteria and returns composites inclusing the
     * requested DriftDefinition objects and supplemental data.
     *
     * @param criteria the criteria
     *
     * @return all drift definition composistes that matches the specified criteria
     */
    PageList<DriftDefinitionComposite> findDriftDefinitionCompositesByCriteria(DriftDefinitionCriteria criteria)
        throws RuntimeException;

    /**
     * Find all drift definitions that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return all drift definitions that matches the specified criteria
     */
    PageList<DriftDefinition> findDriftDefinitionsByCriteria(DriftDefinitionCriteria criteria) throws RuntimeException;

    /**
     * Find all drift definition templates that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return all drift definition templates that matches the specified criteria
     */
    PageList<DriftDefinitionTemplate> findDriftDefinitionTemplatesByCriteria(DriftDefinitionTemplateCriteria criteria)
        throws RuntimeException;

    /**
     * Find all drifts that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return all drifts that match the specified criteria
     */
    PageList<? extends Drift<?, ?>> findDriftsByCriteria(GenericDriftCriteria criteria) throws RuntimeException;

    /**
     * Get the specified drift definition.
     * 
     * @param driftDefId
     * @return
     * @throws RuntimeException
     */
    DriftDefinition getDriftDefinition(int driftDefId) throws RuntimeException;

    DriftDetails getDriftDetails(String driftId) throws RuntimeException;

    String getDriftFileBits(String hash) throws RuntimeException;

    DriftSnapshot getSnapshot(DriftSnapshotRequest request) throws RuntimeException;

    FileDiffReport generateUnifiedDiff(Drift<?, ?> drift) throws RuntimeException;

    FileDiffReport generateUnifiedDiffByIds(String driftId1, String driftId2) throws RuntimeException;

    boolean isBinaryFile(Drift<?, ?> drift) throws RuntimeException;

    void pinSnapshot(int driftDefId, int version) throws RuntimeException;

    void pinTemplate(int templateId, int snapshotDriftDefId, int snapshotVersion) throws RuntimeException;

    /**
     * Update the provided driftDef (identified by name) on the specified EntityContext.  If it exists it will be 
     * replaced. If not it will be added.  Agents, if available, will be notified of the change. 
     * 
     * @param entityContext
     * @param driftDef
     */
    void updateDriftDefinition(EntityContext entityContext, DriftDefinition driftDef) throws RuntimeException;

}