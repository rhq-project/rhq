/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.criteria;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class RoleCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterName;
    private String filterDescription;
    private Integer filterSubjectId;

    private boolean fetchSubjects;
    private boolean fetchResourceGroups;
    private boolean fetchPermissions;
    private boolean fetchRoleNotifications;

    private PageOrdering sortName;

    public RoleCriteria() {
        super(Role.class);

        filterOverrides.put("subjectId", "id in (select sr.id from Role sr JOIN sr.subjects s where s.id = :id)");

    }

    @Override
    public Class getPersistentClass() {
        return Role.class;
    }

    public Integer getFilterId() {
        return this.filterId;
    }

    public void setFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void setFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void setFilterSubjectId(Integer filterSubjectId) {
        this.filterSubjectId = filterSubjectId;
    }

    public boolean getFetchSubjects() {
        return this.fetchSubjects;
    }

    /**
     * Requires MANAGE_SECURITY
     * @param fetchSubjects
     */
    public void setFetchSubjects(boolean fetchSubjects) {
        this.fetchSubjects = fetchSubjects;
    }

    /**
     * Requires MANAGE_SECURITY
     * @param fetchResourceGroups
     */
    public void setFetchResourceGroups(boolean fetchResourceGroups) {
        this.fetchResourceGroups = fetchResourceGroups;
    }

    public void setFetchPermissions(boolean fetchPermissions) {
        this.fetchPermissions = fetchPermissions;
    }

    public void setFetchRoleNotifications(boolean fetchRoleNotifications) {
        this.fetchRoleNotifications = fetchRoleNotifications;
    }

    public void setSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    /** subclasses should override as necessary */
    public boolean isSecurityManagerRequired() {
        return (this.fetchSubjects || this.fetchResourceGroups);
    }

}
