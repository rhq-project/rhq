/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.resource;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.ProductVersionPackageVersion;

/**
 * Used as a mapping between a resource and a package version. The resource will reference an instance of this entity to
 * indicate the version of the resource. A package version can optionally reference an entity of this class to indicate
 * it can only be installed on resources of a specific version.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries( { @NamedQuery(name = ProductVersion.QUERY_FIND_BY_RESOURCE_TYPE_AND_VERSION, query = "SELECT pv FROM ProductVersion AS pv WHERE pv.resourceType = :resourceType AND pv.version = :version") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PRD_VER_ID_SEQ")
@Table(name = "RHQ_PRD_VER")
public class ProductVersion implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_RESOURCE_TYPE_AND_VERSION = "ProductVersion.findByResourceTypeAndVersion";

    // Attributes --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "VERSION", nullable = false)
    private String version;

    @JoinColumn(name = "RES_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ResourceType resourceType;

    @OneToMany(mappedBy = "productVersion", fetch = FetchType.LAZY)
    private Set<Resource> resources;

    @OneToMany(mappedBy = "productVersion", fetch = FetchType.LAZY)
    private Set<ProductVersionPackageVersion> productVersionPackageVersions;

    // Constructors  --------------------------------------------

    public ProductVersion() {
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getPackageVersions()
     */
    public Set<ProductVersionPackageVersion> getProductVersionPackageVersions() {
        return productVersionPackageVersions;
    }

    /**
     * The package versions that this product version is associated with.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated package versions,
     * use {@link #getProductVersionPackageVersions()} or {@link #addPackageVersion(PackageVersion)},
     * {@link #removePackageVersion(PackageVersion)}.</p>
     */
    public Set<PackageVersion> getPackageVersions() {
        HashSet<PackageVersion> packageVersions = new HashSet<PackageVersion>();

        if (productVersionPackageVersions != null) {
            for (ProductVersionPackageVersion pvpv : productVersionPackageVersions) {
                packageVersions.add(pvpv.getProductVersionPackageVersionPK().getPackageVersion());
            }
        }

        return packageVersions;
    }

    /**
     * Directly assign a package version to this product version.
     *
     * @param  packageVersion
     *
     * @return the mapping that was added
     */
    public ProductVersionPackageVersion addPackageVersion(PackageVersion packageVersion) {
        if (this.productVersionPackageVersions == null) {
            this.productVersionPackageVersions = new HashSet<ProductVersionPackageVersion>();
        }

        ProductVersionPackageVersion mapping = new ProductVersionPackageVersion(this, packageVersion);
        this.productVersionPackageVersions.add(mapping);
        return mapping;
    }

    /**
     * Removes the package version from this product version, if it exists. If it does exist, the mapping that was
     * removed is returned; if the given package version did not exist as one that is a member of this product version,
     * <code>null</code> is returned.
     *
     * @param  packageVersion the package version to remove
     *
     * @return the mapping that was removed or <code>null</code> if the package version was not mapped to this product
     *         version
     */
    public ProductVersionPackageVersion removePackageVersion(PackageVersion packageVersion) {
        if ((this.productVersionPackageVersions == null) || (packageVersion == null)) {
            return null;
        }

        ProductVersionPackageVersion doomed = null;

        for (ProductVersionPackageVersion pvpv : this.productVersionPackageVersions) {
            if (packageVersion.equals(pvpv.getProductVersionPackageVersionPK().getPackageVersion())) {
                doomed = pvpv;
                break;
            }
        }

        if (doomed != null) {
            this.productVersionPackageVersions.remove(doomed);
        }

        return doomed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProductVersion)) {
            return false;
        }

        ProductVersion that = (ProductVersion) o;

        if (!resourceType.equals(that.resourceType)) {
            return false;
        }

        if (!version.equals(that.version)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = version.hashCode();
        result = (31 * result) + resourceType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ProductVersion[Version=" + version + ", " + resourceType + "]";
    }
}