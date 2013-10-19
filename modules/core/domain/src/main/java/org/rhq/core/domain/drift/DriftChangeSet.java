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
package org.rhq.core.domain.drift;

import java.util.Set;

import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;

/**
 * A DriftChangeSet is somewhat similar to a commit in version control systems. It contains
 * information about files being monitored or tracked for drift detection. A change set
 * tells us about files that are added, modified, and deleted.
 * <p/>
 * Agents generate and send change set reports to the RHQ server. This interface provides
 * the contract required for defining and persisting a DriftChangeSet. Drift server plugins
 * are responsible for managing persistence.
 * <p/>
 * Each DriftChangeSet belongs to a particular {@link DriftDefinition} which specifies
 * the rules for how drift detection is performed (by the agent). A DriftDefinition is
 * in turn owned by a {@link org.rhq.core.domain.resource.Resource Resource} and a resource
 * can have multiple drift definitions.
 *
 * @param <D> A server plugin's Drift implementation
 *  
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
public interface DriftChangeSet<D extends Drift<?, ?>> {
    /**
     * Returns a string that uniquely identifies the change set. The format of the string
     * is implementation dependent and as such there is no requirement that the id be
     * stored as a string, only that it has a string representation.
     *
     * @return A unique id as a string
     */
    String getId();

    /**
     * Sets the change set id which should be unique. Implementations are free to store the
     * id in any format.
     *
     * @param id The change set identifier that should be unique
     */
    void setId(String id);

    /** @return The time that the change set was created */
    Long getCtime();

    /**
     * Every change set must be assigned a version that is unique within the context of the
     * owning {@link DriftDefinition}. This is analgous to a revision number in a
     * version control system like SVN.
     *
     * @return The change set version number that is unique within the context of its
     * owning {@link DriftDefinition}.
     */
    int getVersion();

    /**
     * Sets the change set version number. Note that change set version numbers
     * <strong>must</strong> be unique within the context of the owning
     * {@link DriftDefinition}. Change sets should be assigned version numbers in
     * increasing order. The first change set saved should have a version of N, the second
     * should have a value of N + 1, etc.
     *
     * @param version The version number.
     */
    void setVersion(int version);

    /**
     * Returns the category that identifies the change set type.
     * @return The change set category
     * @see DriftChangeSetCategory
     */
    DriftChangeSetCategory getCategory();

    /**
     * Sets the change set category.
     *
     * @param category The category that identifies the change set type
     * @see DriftChangeSetCategory
     */
    void setCategory(DriftChangeSetCategory category);

    /**
     * Returns the drift handling mode of the owning drift definition at the time of change
     * set creation.  Because the value set on the owning drift definition can change, the 
     * value must be stored with the change set such that it can be applied consistently when 
     * handling the associated drift.
     *
     * @return The {@link DriftHandlingMode} mode
     */
    DriftHandlingMode getDriftHandlingMode();

    /**
     * Sets the drift handling mode for the change set. It should be set to the owning drift
     * definition's drift handling mode at the time of change set creation. Because the 
     * value set on the owning drift definition can change, the value must be stored with 
     * the change set such that it can be applied consistently when handling the associated drift.
     *
     * @param driftHandlingMode The {@link DriftHandlingMode} mode
     */
    void setDriftHandlingMode(DriftHandlingMode driftHandlingMode);

    /**
     * Returns the id of the owning drift definition. Note that while server plugins are
     * responsible for managing the persistence of change sets, the RHQ server manages the
     * persistence of the owning drift definition.
     *
     * @return The {@link DriftDefinition} id
     */
    int getDriftDefinitionId();

    /**
     * Sets the id of the owning drift definition. Note that while server plugins are
     * responsible for managing the persistence of change sets, the RHQ server manages the
     * persistence of the drift definition.
     *
     * @param id The {@link DriftDefinition} id
     */
    void setDriftDefinitionId(int id);

    /**
     * Returns the id of the resource to which the drift definition is assigned. Note
     * that the RHQ server and not drift server plugins manage the persistence of the
     * resource.
     *
     * @return The id of the resource to which the drift definition belongs
     */
    int getResourceId();

    /**
     * Sets the id of the resource to which the drift definition is assigned.
     * Note that the RHQ server and not the drift server plugins manage the persistence
     * of the resource
     *
     * @param id The {@link org.rhq.core.domain.resource.Resource Resource} id
     */
    void setResourceId(int id);

    /** @return The entries that comprise this change set */
    Set<D> getDrifts();

    /** @param drifts The entries that make up this change set */
    void setDrifts(Set<D> drifts);
}
