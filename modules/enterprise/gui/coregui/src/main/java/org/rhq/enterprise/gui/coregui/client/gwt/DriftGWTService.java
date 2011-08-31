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

/**
 * @author Jay Shaughnessy
 */
public interface DriftGWTService extends RemoteService {

    DriftSnapshot createSnapshot(Subject subject, GenericDriftChangeSetCriteria criteria) throws RuntimeException;

    /**
     * Delete all named drift configurations for the specified context if the current user has permission to do so (i.e. either
     * the MANAGE_INVENTORY global permission, or the MANAGE_DRIFT permission for all corresponding resources).
     *
     * @param entityContext the context for deletion
     * @param driftConfigNames the names of the configs to delete
     * @return the number of drift configs deleted
     */
    int deleteDriftConfigurationsByContext(EntityContext entityContext, String[] driftConfigNames)
        throws RuntimeException;

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
    PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(GenericDriftChangeSetCriteria criteria)
        throws RuntimeException;

    PageList<DriftComposite> findDriftCompositesByCriteria(GenericDriftCriteria criteria) throws RuntimeException;

    /**
     * Find all drift configurations that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return all drift configurations that matches the specified criteria
     */
    PageList<DriftConfiguration> findDriftConfigurationsByCriteria(DriftConfigurationCriteria criteria)
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
     * Get the specified drift configuration.
     * 
     * @param driftConfigId
     * @return
     * @throws RuntimeException
     */
    DriftConfiguration getDriftConfiguration(int driftConfigId) throws RuntimeException;

    /**
     * Update the provided driftConfig (identified by name) on the specified EntityContext.  If it exists it will be replaced. If not it will
     * be added.  Agents, if available, will be notified of the change. 
     * 
     * @param entityContext
     * @param driftConfig
     */
    void updateDriftConfiguration(EntityContext entityContext, DriftConfiguration driftConfig) throws RuntimeException;

    String getDriftFileBits(String hash) throws RuntimeException;

    FileDiffReport generateUnifiedDiff(Drift drift) throws RuntimeException;

    boolean isBinaryFile(Drift drift) throws RuntimeException;

    DriftDetails getDriftDetails(String driftId) throws RuntimeException;

}