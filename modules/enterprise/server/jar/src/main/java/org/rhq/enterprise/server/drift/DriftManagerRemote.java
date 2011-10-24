/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.drift;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.util.PageList;

@Remote
public interface DriftManagerRemote {

    /**
     * Remove the provided driftDef (identified by name) on the specified entityContext.
     * Agents, if available, will be notified of the change. 
     * @param subject
     * @param entityContext
     * @param driftDefName
     */
    void deleteDriftDefinition(Subject subject, EntityContext entityContext, String driftDefName);

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The DriftChangeSets matching the criteria
     */
    PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria)
        throws Exception;

    PageList<DriftDefinition> findDriftDefinitionsByCriteria(Subject subject, DriftDefinitionCriteria criteria);

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The Drifts matching the criteria
     */
    PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) throws Exception;

    /**
     * Generates a unified diff of the two files references by drift. In the case of a
     * modified file, a Drift object references the current and previous versions of the
     * file. This method generates a diff of the two versions.
     *
     * @param drift Specifies the two files that will be compared
     * @return A report containing a unified diff of the two versions of the file
     * referenced by drift
     */
    FileDiffReport generateUnifiedDiff(Subject subject, Drift<?, ?> drift);

    /**
     * Generates a unified diff of the two files referenced by drift1 and drift2. More
     * specifically, the files referenced by {@link org.rhq.core.domain.drift.Drift#getNewDriftFile()}
     * are compared.
     *
     * @param drift1 References the first file to be compared
     * @param drift2 References the second file to be compared
     * @return A report containing a unified diff of the two files compared
     */
    FileDiffReport generateUnifiedDiff(Subject subject, Drift<?, ?> drift1, Drift<?, ?> drift2);

    /**
     * Generates a unified diff of the two file versions referenced by drift ids.
     *
     * @param drift1Id the "new" version of the first drift
     * @param drift2Id the "new" version of the second drift 
     * @return A report containing a unified diff of the two versions of the file
     * referenced by drift
     */
    FileDiffReport generateUnifiedDiffByIds(Subject subject, String driftId1, String driftId2);

    /**
     * Returns the content associated with the specified hash as a string
     *
     * @param hash The hash the uniquely identifies the requested content
     * @return The content as a string
     */
    String getDriftFileBits(Subject subject, String hash);

    /**
     * Calculate and return requested Drift Snapshot.
     *  
     * @param subject
     * @param request
     * @return The DriftSnapshot Not null but version is set to -1 if the drift definition does not yet
     * have any snapshots or there are no snapshots that meet the requested snapshot version constraints.
     * @throws IllegalArgumentException if the specified drift definition does not exist
     */
    DriftSnapshot getSnapshot(Subject subject, DriftSnapshotRequest request);

    void pinSnapshot(Subject subject, int driftDefId, int snapshotVersion);

    void updateDriftDefinition(Subject subject, EntityContext entityContext, DriftDefinition driftConfig);
}
