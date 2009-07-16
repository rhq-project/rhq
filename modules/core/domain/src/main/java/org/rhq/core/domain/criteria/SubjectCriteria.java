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

import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public class SubjectCriteria extends Criteria {

    private Integer filterId;
    private String filterName;
    private String filterFirstName;
    private String filterLastName;
    private String filterEmailAddress;
    private String filterSmsAddress;
    private String filterPhoneNumber;
    private String filterDepartment;
    private Boolean filterFactive;

    private boolean fetchConfiguration;
    private boolean fetchRoles;
    private boolean fetchSubjectNotifications;

    private PageOrdering sortName;
    private PageOrdering sortFirstName;
    private PageOrdering sortLastName;
    private PageOrdering sortEmailAddress;
    private PageOrdering sortSmsAddress;
    private PageOrdering sortPhoneNumber;
    private PageOrdering sortDepartment;

    public Integer getFilterId() {
        return filterId;
    }

    public void setFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterFirstName() {
        return filterFirstName;
    }

    public void setFilterFirstName(String filterFirstName) {
        this.filterFirstName = filterFirstName;
    }

    public String getFilterLastName() {
        return filterLastName;
    }

    public void setFilterLastName(String filterLastName) {
        this.filterLastName = filterLastName;
    }

    public String getFilterEmailAddress() {
        return filterEmailAddress;
    }

    public void setFilterEmailAddress(String filterEmailAddress) {
        this.filterEmailAddress = filterEmailAddress;
    }

    public String getFilterSmsAddress() {
        return filterSmsAddress;
    }

    public void setFilterSmsAddress(String filterSmsAddress) {
        this.filterSmsAddress = filterSmsAddress;
    }

    public String getFilterPhoneNumber() {
        return filterPhoneNumber;
    }

    public void setFilterPhoneNumber(String filterPhoneNumber) {
        this.filterPhoneNumber = filterPhoneNumber;
    }

    public String getFilterDepartment() {
        return filterDepartment;
    }

    public void setFilterDepartment(String filterDepartment) {
        this.filterDepartment = filterDepartment;
    }

    public Boolean getFilterFactive() {
        return filterFactive;
    }

    public void setFilterFactive(Boolean filterFactive) {
        this.filterFactive = filterFactive;
    }

    public boolean isFetchConfiguration() {
        return fetchConfiguration;
    }

    public void setFetchConfiguration(boolean fetchConfiguration) {
        this.fetchConfiguration = fetchConfiguration;
    }

    public boolean isFetchRoles() {
        return fetchRoles;
    }

    public void setFetchRoles(boolean fetchRoles) {
        this.fetchRoles = fetchRoles;
    }

    public boolean isFetchSubjectNotifications() {
        return fetchSubjectNotifications;
    }

    public void setFetchSubjectNotifications(boolean fetchSubjectNotifications) {
        this.fetchSubjectNotifications = fetchSubjectNotifications;
    }

    public PageOrdering getSortName() {
        return sortName;
    }

    public void setSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public PageOrdering getSortFirstName() {
        return sortFirstName;
    }

    public void setSortFirstName(PageOrdering sortFirstName) {
        addSortField("firstName");
        this.sortFirstName = sortFirstName;
    }

    public PageOrdering getSortLastName() {
        return sortLastName;
    }

    public void setSortLastName(PageOrdering sortLastName) {
        addSortField("lastName");
        this.sortLastName = sortLastName;
    }

    public PageOrdering getSortEmailAddress() {
        return sortEmailAddress;
    }

    public void setSortEmailAddress(PageOrdering sortEmailAddress) {
        addSortField("emailAddress");
        this.sortEmailAddress = sortEmailAddress;
    }

    public PageOrdering getSortSmsAddress() {
        return sortSmsAddress;
    }

    public void setSortSmsAddress(PageOrdering sortSmsAddress) {
        addSortField("smsAddress");
        this.sortSmsAddress = sortSmsAddress;
    }

    public PageOrdering getSortPhoneNumber() {
        return sortPhoneNumber;
    }

    public void setSortPhoneNumber(PageOrdering sortPhoneNumber) {
        addSortField("phoneNumber");
        this.sortPhoneNumber = sortPhoneNumber;
    }

    public PageOrdering getSortDepartment() {
        return sortDepartment;
    }

    public void setSortDepartment(PageOrdering sortDepartment) {
        addSortField("department");
        this.sortDepartment = sortDepartment;
    }

}
