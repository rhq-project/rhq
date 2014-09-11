/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.util.PageOrdering;

@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class InstalledPackageCriteria extends Criteria {
    private static final long serialVersionUID = 2L;

    private Long filterInstallationTimeMinimum; // requires overrides
    private Long filterInstallationTimeMaximum; // requires overrides
    private Integer filterPackageVersionId; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterUserId; // requires overrides

    private boolean fetchPackageVersion;
    private boolean fetchResource;
    private boolean fetchUser;

    private PageOrdering sortInstallationDate;

    public InstalledPackageCriteria() {
        filterOverrides.put("installationTimeMinimum", "installationDate >= ?");
        filterOverrides.put("installationTimeMaximum", "installationDate <= ?");
        filterOverrides.put("packageVersionId", "packageVersion.id = ? ");
        filterOverrides.put("resourceId", "resource.id = ? ");
        filterOverrides.put("userId", "user.id = ? ");
    }

    @Override
    public Class<InstalledPackage> getPersistentClass() {
        return InstalledPackage.class;
    }

    public void addFilterPackageVersionId(Integer filterPackageVersionId) {
        this.filterPackageVersionId = filterPackageVersionId;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public Integer getFilterResourceId() {
        return filterResourceId;
    }

    public void addFilterSubjectId(Integer filterSubjectId) {
        this.filterUserId = filterSubjectId;
    }

    public void addFilterInstallationTimeMinimum(Long filterInstallationTimeMinimum) {
        this.filterInstallationTimeMinimum = filterInstallationTimeMinimum;
    }

    public void addFilterInstallationTimeMaximum(Long filterInstallationTimeMaximum) {
        this.filterInstallationTimeMaximum = filterInstallationTimeMaximum;
    }

    public void fetchResource(boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void fetchPackageVersion(boolean fetchPackageVersion) {
        this.fetchPackageVersion = fetchPackageVersion;
    }

    public void fetchSubject(boolean fetchSubject) {
        this.fetchUser = fetchSubject;
    }

    public void addSortInstallationTime(PageOrdering sortInstallationDate) {
        addSortField("installationDate");
        this.sortInstallationDate = sortInstallationDate;
    }

}
