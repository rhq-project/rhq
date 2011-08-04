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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftChangeSetJPACriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.Snapshot;
import org.rhq.core.domain.util.PageList;

/**
 * @author Jay Shaughnessy
 */
public interface DriftGWTService extends RemoteService {

    /**
     * Delete the drifts with the specified ids if the current user has permission to do so (i.e. either
     * the MANAGE_INVENTORY global permission, or the MANAGE_DRIFT permission for all corresponding resources).
     * If the user does not have permission for all of the specified drifts, then none of the drifts will be deleted
     * and a PermissionException will be thrown.
     *
     * If any of the ids do not correspond to drift entities that exist, those ids will be gracefully ignored.
     *
     * @param driftIds the ids of the drifts to be deleted
     * @return the number of drifts deleted
     */
    int deleteDrifts(String[] driftIds) throws RuntimeException;

    /**
     * Delete all drifts for the specified context if the current user has permission to do so (i.e. either
     * the MANAGE_INVENTORY global permission, or the MANAGE_DRIFT permission for all corresponding resources).
     * If the user does not have permission for all of the specified drifts, then none of the drifts will be deleted
     * and a PermissionException will be thrown.
     *
     * If the entity does not correspond to an existing entity, it will be gracefully ignored.
     *
     * @param entityContext the context for deletion
     * @return the number of drifts deleted
     */
    int deleteDriftsByContext(EntityContext entityContext) throws RuntimeException;

    /**
     * Delete the drift configs with the specified ids if the current user has permission to do so (i.e. either
     * the MANAGE_INVENTORY global permission, or the MANAGE_DRIFT permission for all corresponding resources).
     * If the user does not have permission for all of the specified drift configs, then none of them  will be deleted
     * and a PermissionException will be thrown.
     *
     * If any of the ids do not correspond to drift entities that exist, those ids will be gracefully ignored.
     *
     * @param driftConfigIds the ids of the drift configs to be deleted
     * @return the number of drift configs deleted
     */
    int deleteDriftConfigurations(int[] driftConfigIds) throws RuntimeException;

    /**
     * Delete all drift configurations for the specified context if the current user has permission to do so (i.e. either
     * the MANAGE_INVENTORY global permission, or the MANAGE_DRIFT permission for all corresponding resources).
     * If the user does not have permission for all of the specified drifts, then none of the drifts will be deleted
     * and a PermissionException will be thrown.
     *
     * If the entity does not correspond to an existing entity, it will be gracefully ignored.
     *
     * @param entityContext the context for deletion
     * @return the number of drift configs deleted
     */
    int deleteDriftConfigurationsByContext(EntityContext entityContext) throws RuntimeException;

    /**
     * One time on-demand request to detect drift on the specified entities, using the supplied config.
     * 
     * @param entityContext
     * @param driftConfig
     * @throws RuntimeException
     */
    void detectDrift(EntityContext entityContext, DriftConfiguration driftConfig) throws RuntimeException;

    /**
     * Find all drift changesets that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return all drift changesets that matches the specified criteria
     */
    PageList<DriftChangeSet> findDriftChangeSetsByCriteria(DriftChangeSetCriteria criteria) throws RuntimeException;

    /**
     * Find all drifts that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return all drifts that match the specified criteria
     */
    PageList<Drift> findDriftsByCriteria(DriftCriteria criteria) throws RuntimeException;

    PageList<DriftComposite> findDriftCompositesByCriteria(DriftCriteria criteria);

    Snapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria);

    /**
     * Get the specified drift configuration for the specified context.
     * 
     * @param entityContext
     * @param driftConfigId
     * @return
     * @throws RuntimeException
     */
    DriftConfiguration getDriftConfiguration(EntityContext entityContext, int driftConfigId) throws RuntimeException;

    /**
     * Update the provided driftConfig (identified by name) on the specified EntityContext.  If it exists it will be replaced. If not it will
     * be added.  Agents, if available, will be notified of the change. 
     * 
     * @param entityContext
     * @param driftConfig
     */
    void updateDriftConfiguration(EntityContext entityContext, DriftConfiguration driftConfig);

}