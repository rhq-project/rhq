/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

import java.util.List;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Lukas Krejci
 * @since 4.11
 */
@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
public class PluginCriteria extends Criteria {

    private static final long serialVersionUID = 1L;

    private String filterName;
    private String filterDisplayName;
    private Boolean filterEnabled;
    private String filterVersion;
    private List<Integer> filterResourceTypeIds;

    //using integers here so that we generate Oracle compliant sql - the poor bugger doesn't have booleans
    //see the filter overrides in the constructor
    private Integer filterInstalled;
    private Integer filterDeleted;

    private PageOrdering sortName;

    public PluginCriteria() {
        filterOverrides.put("resourceTypeIds",
            "id IN (SELECT p.id FROM Plugin p, ResourceType rt WHERE rt.plugin = p.name AND rt.id IN ( ? ))");
        filterOverrides.put("installed", "id IN (SELECT p.id FROM Plugin p WHERE (? = 1 AND p.status = 'INSTALLED') OR (? = 0 AND p.status <> 'INSTALLED'))");
        filterOverrides.put("deleted", "id IN (SELECT p.id FROM Plugin p WHERE (? = 1 AND p.status = 'DELETED') OR (? = 0 AND p.status <> 'DELETED'))");

    }

    @Override
    public Class<?> getPersistentClass() {
        return Plugin.class;
    }

    public void addFilterName(String value) {
        this.filterName = value;
    }

    public void addFilterDisplayName(String value) {
        this.filterDisplayName = value;
    }

    public void addFilterEnabled(boolean value) {
        this.filterEnabled = value;
    }

    public void addFilterVersion(String value) {
        this.filterVersion = value;
    }

    public void addFilterResourceTypeIds(Integer... resourceTypeIds) {
        filterResourceTypeIds = CriteriaUtils.getListIgnoringNulls(resourceTypeIds);
    }

    public void addFilterInstalled(boolean value) {
        filterInstalled = value ? 1 : 0;
    }

    public void addFilterDeleted(boolean value) {
        filterDeleted = value ? 1 : 0;
    }
}
