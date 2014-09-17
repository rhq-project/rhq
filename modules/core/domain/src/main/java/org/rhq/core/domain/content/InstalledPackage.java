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
package org.rhq.core.domain.content;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;

/**
 * Represents a {@link PackageVersion specific package} that is installed on a resource. Each instance of this object is
 * associated to one and only one {@link PackageVersion}.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries({
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_SET_OF_IDS, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.id IN ( :packageIds )"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_RESOURCE_ID, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.resource.id = :resourceId"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.resource.id = :resourceId AND ip.packageVersion.id = :packageVersionId"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE, query = "SELECT new org.rhq.core.domain.content.composite.PackageListItemComposite(ip.id, gp.name, pt.displayName, ip.packageVersion.version, ip.packageVersion.displayVersion, ip.installationDate ) "
        + " FROM InstalledPackage ip JOIN ip.resource res LEFT JOIN ip.packageVersion pv LEFT JOIN pv.generalPackage gp LEFT JOIN gp.packageType pt "
        + "WHERE res.id = :resourceId "
        + "  AND (:packageTypeFilterId = pt.id OR :packageTypeFilterId is null) "
        + "  AND (:packageVersionFilter = ip.packageVersion.version OR :packageVersionFilter is null) "
        + " AND (UPPER(gp.name) LIKE :search OR :search is null) "),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_PACKAGE_LIST_TYPES, query = "SELECT DISTINCT new org.rhq.core.domain.common.composite.IntegerOptionItem(pt.id, pt.displayName) "
        + "    FROM InstalledPackage ip JOIN ip.resource res LEFT JOIN ip.packageVersion pv LEFT JOIN pv.generalPackage gp LEFT JOIN gp.packageType pt "
        + "   WHERE res.id = :resourceId " + "ORDER BY pt.displayName"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_PACKAGE_LIST_VERSIONS, query = "SELECT DISTINCT pv.version "
        + "    FROM InstalledPackage ip JOIN ip.resource res LEFT JOIN ip.packageVersion pv "
        + "   WHERE res.id = :resourceId " + "ORDER BY pv.version"),
    @NamedQuery(name = InstalledPackage.QUERY_DELETE_BY_IDS, query = "DELETE FROM InstalledPackage ip "
        + " WHERE ip.id IN ( :ids )"),
    @NamedQuery(name = InstalledPackage.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM InstalledPackage ip "
        + " WHERE ip.resource.id IN ( :resourceIds )") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_INSTALLED_PACKAGE_ID_SEQ", sequenceName = "RHQ_INSTALLED_PACKAGE_ID_SEQ")
@Table(name = "RHQ_INSTALLED_PACKAGE")
public class InstalledPackage implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_SET_OF_IDS = "InstalledPackage.findBySetOfIds";

    public static final String QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE = "InstalledPackage.findPackageListItemComposite";
    public static final String QUERY_FIND_PACKAGE_LIST_TYPES = "InstalledPackage.findPackageListTypes";
    public static final String QUERY_FIND_PACKAGE_LIST_VERSIONS = "InstalledPackage.findPackageListVersions";
    public static final String QUERY_DELETE_BY_IDS = "InstalledPackage.deleteByIds";

    // TODO: this is unindexed but used in resource bulk delete, should we add index on resId? Or maybe re-write query
    public static final String QUERY_DELETE_BY_RESOURCES = "InstalledPackage.deleteByResources";

    // Unindexed queries (used for testing)
    public static final String QUERY_FIND_BY_RESOURCE_ID = "InstalledPackage.findByResourceId";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID = "InstalledPackage.findByResourceIdAndPackageVersionId";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_INSTALLED_PACKAGE_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Resource resource;

    @JoinColumn(name = "PACKAGE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private PackageVersion packageVersion;

    @Column(name = "INSTALLATION_TIME", nullable = true)
    private Long installationDate;

    @JoinColumn(name = "SUBJECT_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private Subject user;

    public InstalledPackage() {
        // needed for JPA
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Resource where the package is installed.
     */
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * Specific version of the package installed on the {@link #getResource() resource}.
     */
    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(PackageVersion packageVersion) {
        this.packageVersion = packageVersion;
    }

    /**
     * Timestamp the installation was performed, if it is known.
     */
    public Long getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(Long installationDate) {
        this.installationDate = installationDate;
    }

    /**
     * User who performed the installation, if it is known.
     */
    public Subject getUser() {
        return user;
    }

    public void setUser(Subject user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "InstalledPackage[resource=" + resource.getName() + ",packageVersion=" + packageVersion.getDisplayName()
            + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof InstalledPackage)) {
            return false;
        }

        InstalledPackage that = (InstalledPackage) o;

        if ((packageVersion != null) ? (!packageVersion.equals(that.packageVersion)) : (that.packageVersion != null)) {
            return false;
        }

        if ((resource != null) ? (!resource.equals(that.resource)) : (that.resource != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = ((resource != null) ? resource.hashCode() : 0);
        result = (31 * result) + ((packageVersion != null) ? packageVersion.hashCode() : 0);
        return result;
    }
}