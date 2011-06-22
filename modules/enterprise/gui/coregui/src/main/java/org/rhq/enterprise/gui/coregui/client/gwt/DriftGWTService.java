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

import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
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
    int deleteDrifts(int[] driftIds) throws RuntimeException;

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

}