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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

/**
 * Represents a package inventoried in the system. A package can be created by either a {@link ContentSource} (on the
 * server side) or via a plugin discovery (on the agent side). A package has no knowledge of how it was created (i.e. it
 * doesn't know if it was pulled down from a content source or pushed in from an agent discovery).
 *
 * <p>Regardless of how a package was created, it can be placed in one or more {@link Channel}s so resources can later
 * subscribe to those channels and install different versions of the package.
 *
 * <p>
 * <p>A package can have one or more {@link PackageVersion}s associated with it. Package versions allow for things like
 * different software revisions or different architectures.</p>
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID, query = "SELECT p FROM Package AS p WHERE p.name = :name AND p.packageType.id = :packageTypeId"),
    @NamedQuery(name = Package.QUERY_FIND_BY_NAME_PKG_TYPE_RESOURCE_TYPE, query = "SELECT p FROM Package AS p "
        + "WHERE p.name = :name " + "AND p.packageType.name = :packageTypeName "
        + "AND p.packageType.resourceType.id = :resourceTypeId") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PACKAGE_ID_SEQ")
@Table(name = "RHQ_PACKAGE")
public class Package implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_NAME_PKG_TYPE_ID = "Package.findByNameAndPkgTypeId";
    public static final String QUERY_FIND_BY_NAME_PKG_TYPE_RESOURCE_TYPE = "Package.findByNamePkgTypeResourceType";

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "PACKAGE_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(cascade = CascadeType.PERSIST)
    private PackageType packageType;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CLASSIFICATION", nullable = true)
    private String classification;

    // make sure you do not make this eager load, see join fetches in queries in PackageVersionContentSource
    @OneToMany(mappedBy = "generalPackage", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private List<PackageVersion> versions;

    // Constructor ----------------------------------------

    public Package() {
    }

    public Package(String name, PackageType type) {
        setName(name);
        setPackageType(type);
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * The type definition of this package.
     */
    public PackageType getPackageType() {
        return packageType;
    }

    public void setPackageType(PackageType packageType) {
        this.packageType = packageType;
    }

    /**
     * Programmatic name of the package.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Provides a second means for indicating a package's type. The possible values for this attribute will vary based
     * on package type. For instance, two packages of the same package type may be differentiated as a "bug fix" or a
     * "security update". Another example would be to indicate a grouping means, such as "System Environment" or
     * "Applications/Editors".
     */
    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    /**
     * List of all versions of this package known to the system. The list will be in descending order, with the most
     * recent version first.
     */
    public List<PackageVersion> getVersions() {
        return versions;
    }

    public void addVersion(PackageVersion version) {
        if (this.versions == null) {
            this.versions = new ArrayList<PackageVersion>();
        }

        this.versions.add(version);
        version.setGeneralPackage(this);
    }

    public void setVersions(List<PackageVersion> versions) {
        this.versions = versions;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return "Package[name=" + name + ",packageType=" + packageType + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Package)) {
            return false;
        }

        Package aPackage = (Package) o;

        if ((name != null) ? (!name.equals(aPackage.name)) : (aPackage.name != null)) {
            return false;
        }

        if ((packageType != null) ? (!packageType.equals(aPackage.packageType)) : (aPackage.packageType != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + ((packageType == null) ? 0 : packageType.hashCode());
        return result;
    }
}