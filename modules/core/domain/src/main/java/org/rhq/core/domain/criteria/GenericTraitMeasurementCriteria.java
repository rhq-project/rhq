/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.core.domain.criteria;

import java.io.Serializable;

/**
 * @author John Sanda
 */
public class GenericTraitMeasurementCriteria implements TraitMeasurementCriteria, Serializable {

    private static final long serialVersionUID = 1L;

    private Integer resourceId;
    private Integer groupId;
    private Integer definitionId;
    private boolean filterMaxTimestamp;

    public GenericTraitMeasurementCriteria() {
    }

    public GenericTraitMeasurementCriteria(TraitMeasurementCriteria criteria) {
        resourceId = criteria.getFilterResourceId();
        groupId = criteria.getFilterGroupId();
        definitionId = criteria.getFilterDefinitionId();
        filterMaxTimestamp = criteria.isFilterMaxTimestamp();
    }

    @Override
    public void addFilterResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public Integer getFilterResourceId() {
        return resourceId;
    }

    @Override
    public void addFilterGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    @Override
    public Integer getFilterGroupId() {
        return groupId;
    }

    @Override
    public void addFilterDefinitionId(Integer definitionId) {
        this.definitionId = definitionId;
    }

    @Override
    public Integer getFilterDefinitionId() {
        return definitionId;
    }

    @Override
    public void addFilterMaxTimestamp() {
        filterMaxTimestamp = true;
    }

    @Override
    public boolean isFilterMaxTimestamp() {
        return filterMaxTimestamp;
    }

    @Override
    public String toString() {
        return "GenericTriatMeasurementCriteria[filterResourceId: " + resourceId + ", filterGroupId:" + groupId +
            ", filterDefinitionId: " + definitionId + ", filterMaxTimestamp: " + filterMaxTimestamp + "]";
    }
}
