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

package org.rhq.core.domain.criteria;

import org.rhq.core.domain.drift.DriftDefinitionTemplate;

@SuppressWarnings("unused")
public class DriftDefinitionTemplateCriteria extends Criteria {
    private static final long serialVersionUID = 2L;

    private String filterName;
    private Integer filterResourceTypeId;

    private boolean fetchResourceType;
    private boolean fetchDriftDefinitions;

    public DriftDefinitionTemplateCriteria() {
        filterOverrides.put("resourceTypeId", "resourceType.id = ?");
    }

    @Override
    public Class<DriftDefinitionTemplate> getPersistentClass() {
        return DriftDefinitionTemplate.class;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterResourceTypeId(Integer filterResourceTypeId) {
        this.filterResourceTypeId = filterResourceTypeId;
    }

    public void fetchResourceType(boolean fetchResourceType) {
        this.fetchResourceType = fetchResourceType;
    }

    public void fetchDriftDefinitions(boolean fetchDriftDefinitions) {
        this.fetchDriftDefinitions = fetchDriftDefinitions;
    }
}
