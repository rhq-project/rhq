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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class RoleCriteria extends Criteria {

    private static final long serialVersionUID = 2L;

    private String filterDescription;
    private String filterName;
    private Integer filterSubjectId;     // needs overrides
    private Integer filterLdapSubjectId; // needs overrides
    private List<Permission> filterPermissions; // needs override

    private boolean fetchPermissions;
    private boolean fetchResourceGroups;
    private boolean fetchRoleNotifications;
    private boolean fetchSubjects;
    private boolean fetchLdapGroups;
    private boolean fetchBundleGroups;

    private PageOrdering sortName;

    public RoleCriteria() {
        filterOverrides.put("subjectId", "" //
            + "id IN ( SELECT innerRole1.id " //
            + "          FROM Role innerRole1 " //
            + "          JOIN innerRole1.subjects innerSubject1 " // 
            + "         WHERE innerSubject1.id = ? )");

        filterOverrides.put("ldapSubjectId", "" //
            + "id IN ( SELECT innerRole2.id " //
            + "          FROM Role innerRole2 " //
            + "          JOIN innerRole2.ldapSubjects innerSubject2 " //
            + "         WHERE innerSubject2.id = ? )");

        filterOverrides.put("permissions", "" //
            + "id IN ( SELECT innerRole3.id " //
            + "          FROM Role innerRole3 " //
            + "          JOIN innerRole3.permissions perm " //
            + "         WHERE perm IN ( ? ) )");
    }

    @Override
    public Class<Role> getPersistentClass() {
        return Role.class;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterSubjectId(Integer filterSubjectId) {
        this.filterSubjectId = filterSubjectId;
    }

    public void addFilterLdapSubjectId(Integer filterLdapSubjectId) {
        this.filterLdapSubjectId = filterLdapSubjectId;
    }

    public void addFilterPermissions(List<Permission> filterPermissions) {
        this.filterPermissions = filterPermissions;
    }

    /**
     * Requires MANAGE_SECURITY
     *
     * @param fetchSubjects
     */
    public void fetchSubjects(boolean fetchSubjects) {
        this.fetchSubjects = fetchSubjects;
    }

    /**
     * Specify whether or not LDAP groups should be fetched. Requires MANAGE_SECURITY.
     *
     * @param fetchLdapGroups true if LDAP groups should be fetched
     */
    public void fetchLdapGroups(boolean fetchLdapGroups) {
        this.fetchLdapGroups = fetchLdapGroups;
    }

    /**
     * Requires MANAGE_SECURITY
     *
     * @param fetchResourceGroups
     */
    public void fetchResourceGroups(boolean fetchResourceGroups) {
        this.fetchResourceGroups = fetchResourceGroups;
    }

    public void fetchBundleGroups(boolean fetchBundleGroups) {
        this.fetchBundleGroups = fetchBundleGroups;
    }

    public void fetchPermissions(boolean fetchPermissions) {
        this.fetchPermissions = fetchPermissions;
    }

    public void fetchRoleNotifications(boolean fetchRoleNotifications) {
        this.fetchRoleNotifications = fetchRoleNotifications;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    /** subclasses should override as necessary */
    public boolean isSecurityManagerRequired() {
        return (this.fetchSubjects || this.fetchResourceGroups || this.fetchLdapGroups || this.fetchBundleGroups);
    }

}
