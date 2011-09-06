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
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.util.PageList;

@Remote
public interface DriftManagerRemote {

    DriftSnapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria) throws Exception;

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The DriftChangeSets matching the criteria
     */
    PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject,
        DriftChangeSetCriteria criteria) throws Exception;

    /**
     * Standard criteria based fetch method
     * @param subject
     * @param criteria
     * @return The Drifts matching the criteria
     */
    PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) throws Exception;

    /**
     * Returns the content associated with the specified hash as a string
     *
     * @param hash The hash the uniquely identifies the requested content
     * @return The content as a string
     */
    String getDriftFileBits(String hash);

    /**
     * Generates a unified diff of the two files references by drift. In the case of a
     * modified file, a Drift object references the current and previous versions of the
     * file. This method generates a diff of the two versions.
     *
     * @param drift Specifies the two files that will be compared
     * @return A report containing a unified diff of the two versions of the file
     * referenced by drift
     */
    FileDiffReport generateUnifiedDiff(Drift drift);

    /**
     * Generates a unified diff of the two files referenced by drift1 and drift2. More
     * specifically, the files referenced by {@link org.rhq.core.domain.drift.Drift#getNewDriftFile()}
     * are compared.
     *
     * @param drift1 References the first file to be compared
     * @param drift2 References the second file to be compared
     * @return A report containing a unified diff of the two files compared
     */
    FileDiffReport generateUnifiedDiff(Drift drift1, Drift drift2);

}
