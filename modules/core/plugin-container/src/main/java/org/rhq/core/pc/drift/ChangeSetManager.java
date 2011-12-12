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

package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;

public interface ChangeSetManager {

    boolean changeSetExists(int resourceId, Headers headers);

    /**
     * Locates the latest coverage change set for the specified resource id and drift
     * definition.
     *
     * @param resourceId The id of the resource to which the change set belongs
     * @param driftDefinitionName The name of the drift definition for which the
     * change set was generated
     * @return The change set file or null if it is not found
     * @throws IOException
     */
    File findChangeSet(int resourceId, String driftDefinitionName) throws IOException;

    File findChangeSet(int resourceId, String name, DriftChangeSetCategory type);

    /**
     * Locates the latest change set for the given resource and drift definition and
     * returns a ChangeSetReader for that change set. Note that a resource can have
     * multiple drift definitions; so, both the resource id and the drift definition
     * are required to uniquely identify a particular change set.
     *
     * @param resourceId The id of the resource to which the change set belongs
     * @param driftDefinitionName The name of the drift definition for which the
     * change set was generated
     * @return A ChangeSetReader that is open on the change set identified by resourceId
     * and driftDefinition. Returns null if no change set has previously been generated.
     * @see ChangeSetReader
     */
    ChangeSetReader getChangeSetReader(int resourceId, String driftDefinitionName) throws IOException;

    ChangeSetReader getChangeSetReader(File changeSetFile) throws IOException;

    ChangeSetWriter getChangeSetWriter(int resourceId, Headers headers) throws IOException;

    ChangeSetWriter getChangeSetWriter(File changeSetFile, Headers headers) throws IOException;
}
