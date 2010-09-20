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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class SubjectCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterName;
    private String filterFirstName;
    private String filterLastName;
    private String filterEmailAddress;
    private String filterSmsAddress;
    private String filterPhoneNumber;
    private String filterDepartment;
    private Boolean filterFactive;
    private Integer filterRoleId;

    private boolean fetchConfiguration;
    private boolean fetchRoles;

    private PageOrdering sortName;
    private PageOrdering sortFirstName;
    private PageOrdering sortLastName;
    private PageOrdering sortEmailAddress;
    private PageOrdering sortSmsAddress;
    private PageOrdering sortPhoneNumber;
    private PageOrdering sortDepartment;

    public SubjectCriteria() {
        filterOverrides.put("roleId", "" //
            + "id IN ( SELECT innerSubject.id " //
            + "          FROM Subject innerSubject " //
            + "          JOIN innerSubject.roles innerRole " // 
            + "         WHERE innerRole.id = ? )");
    }

    @Override
    public Class<Subject> getPersistentClass() {
        return Subject.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterFirstName(String filterFirstName) {
        this.filterFirstName = filterFirstName;
    }

    public void addFilterLastName(String filterLastName) {
        this.filterLastName = filterLastName;
    }

    public void addFilterEmailAddress(String filterEmailAddress) {
        this.filterEmailAddress = filterEmailAddress;
    }

    public void addFilterSmsAddress(String filterSmsAddress) {
        this.filterSmsAddress = filterSmsAddress;
    }

    public void addFilterPhoneNumber(String filterPhoneNumber) {
        this.filterPhoneNumber = filterPhoneNumber;
    }

    public void addFilterDepartment(String filterDepartment) {
        this.filterDepartment = filterDepartment;
    }

    public void addFilterFactive(Boolean filterFactive) {
        this.filterFactive = filterFactive;
    }

    public void addFilterRoleId(Integer filterRoleId) {
        this.filterRoleId = filterRoleId;
    }

    public void fetchConfiguration(boolean fetchConfiguration) {
        this.fetchConfiguration = fetchConfiguration;
    }

    public void fetchRoles(boolean fetchRoles) {
        this.fetchRoles = fetchRoles;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortFirstName(PageOrdering sortFirstName) {
        addSortField("firstName");
        this.sortFirstName = sortFirstName;
    }

    public void addSortLastName(PageOrdering sortLastName) {
        addSortField("lastName");
        this.sortLastName = sortLastName;
    }

    public void addSortEmailAddress(PageOrdering sortEmailAddress) {
        addSortField("emailAddress");
        this.sortEmailAddress = sortEmailAddress;
    }

    public void addSortSmsAddress(PageOrdering sortSmsAddress) {
        addSortField("smsAddress");
        this.sortSmsAddress = sortSmsAddress;
    }

    public void addSortPhoneNumber(PageOrdering sortPhoneNumber) {
        addSortField("phoneNumber");
        this.sortPhoneNumber = sortPhoneNumber;
    }

    public void addSortDepartment(PageOrdering sortDepartment) {
        addSortField("department");
        this.sortDepartment = sortDepartment;
    }

}
