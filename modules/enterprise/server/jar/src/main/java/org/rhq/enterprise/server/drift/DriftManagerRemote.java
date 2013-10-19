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

/**
 * The public API for Drift Management.
 */
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
     * @throws Exception
     */
    PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject, DriftChangeSetCriteria criteria)
        throws Exception;

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<DriftDefinition> findDriftDefinitionsByCriteria(Subject subject, DriftDefinitionCriteria criteria);

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The Drifts matching the criteria
     * @throws Exception
     */
    PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) throws Exception;

    /**
     * Generates a unified diff of the two files references by drift. In the case of a
     * modified file, a Drift object references the current and previous versions of the
     * file. This method generates a diff of the two versions.
     *
     * @param subject
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
     * @param subject
     * @param drift1 References the first file to be compared
     * @param drift2 References the second file to be compared
     * @return A report containing a unified diff of the two files compared
     */
    FileDiffReport generateUnifiedDiff(Subject subject, Drift<?, ?> drift1, Drift<?, ?> drift2);

    /**
     * Generates a unified diff of the two file versions referenced by drift ids.
     * @param subject
     * @param driftId1 the "new" version of the first drift
     * @param driftId2 the "new" version of the second drift
     * @return A report containing a unified diff of the two versions of the file
     * referenced by drift
     */
    FileDiffReport generateUnifiedDiffByIds(Subject subject, String driftId1, String driftId2);

    /**
     * Returns the content associated with the specified hash as a string
     *
     * @param subject
     * @param hash The hash the uniquely identifies the requested content
     * @return The content as a string
     */
    String getDriftFileBits(Subject subject, String hash);

    /**
     * @param subject
     * @param hash
     * @return the byteArray, not null
     */
    byte[] getDriftFileAsByteArray(Subject subject, String hash);

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

    /**
     * @param subject
     * @param driftDefId
     * @param snapshotVersion
     */
    void pinSnapshot(Subject subject, int driftDefId, int snapshotVersion);

    /**
     * <p>
     * Saves or updates the provided drift definition. If the definition, identified by name, already exists, an update
     * is performed; otherwise, a new drift definition is saved. Agents if available will be notified of the change.
     * If agents are unreachable, the definition will still be saved/updated. Changes will then propagate to agents
     * the next time they do an inventory sync.
     * </p>
     * <p>
     * Several validation checks are performed before the definition is persisted. If it is a new definition, the
     * following checks are performed:
     * <ul>
     *   <li>Verify that the resource supports drift management</li>
     *   <li>Verify that the template (if specified) already exists in the database</li>
     *   <li>Verify that the template (if specified) belongs to the correct resource type</li>
     * </ul>
     * For new and existing definitions these additional checks are performed:
     * <ul>
     *   <li>Verify that the definition name does not contain illegal characters</li>
     *   <li>Verify that the base directory does not contain illegal characters</li>
     *   <li>Verify that filters do not contain illegal characters</li>
     * </ul>
     * </p>
     *
     * @param subject
     * @param entityContext
     * @param driftDef
     */
    void updateDriftDefinition(Subject subject, EntityContext entityContext, DriftDefinition driftDef);
}
