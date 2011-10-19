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

package org.rhq.common.drift;

import java.io.Serializable;

import org.rhq.core.domain.drift.DriftChangeSetCategory;

/**
 * A change set meta data file produced with a {@link ChangeSetWriter} includes a set of
 * headers which are represented by this class. The headers can be obtained through
 * {@link org.rhq.common.drift.ChangeSetReader#getHeaders()}.
 */
public class Headers implements Serializable {

    private static final long serialVersionUID = 1L;

    private int driftDefinitionId;

    private String driftDefinitionName;

    private String basedir;

    private DriftChangeSetCategory type;

    private int resourceId;

    private int version;

    /**
     * This is the id of the resource to which the change set belongs
     *
     * @return The owning resource id
     */
    public int getResourceId() {
        return resourceId;
    }

    /** @param resourceId The id of the resource to which the change set belongs */
    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * @return The id of the drift definition to which the change set belongs.
     */
    public int getDriftDefinitionId() {
        return driftDefinitionId;
    }

    /**
     * @param driftDefinitionId The id of the drift def to which the change set belongs
     */
    public void setDriftDefinitionId(int driftDefinitionId) {
        this.driftDefinitionId = driftDefinitionId;
    }

    /**
     * @return The name of the drift def to which the change set belongs
     */
    public String getDriftDefinitionName() {
        return driftDefinitionName;
    }

    /**
     * @param name The name of the drift def to which the change set belongs
     */
    public void setDriftDefinitionName(String name) {
        driftDefinitionName = name;
    }

    /**
     * @return The full path of the top-level directory from which drift detection is performed
     */
    public String getBasedir() {
        return basedir;
    }

    /**
     * @param basedir The full path of the top-level directory from which drift detection is performed
     */
    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    /**
     * @return The type of change set
     */
    public DriftChangeSetCategory getType() {
        return type;
    }

    /**
     * @param type The type of change set
     */
    public void setType(DriftChangeSetCategory type) {
        this.type = type;
    }

    /**
     * @return The change set version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version The change set version
     */
    public void setVersion(int version) {
        this.version = version;
    }

}
