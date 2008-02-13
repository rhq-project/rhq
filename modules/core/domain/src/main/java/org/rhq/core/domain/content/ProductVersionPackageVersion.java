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
package org.rhq.core.domain.content;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.rhq.core.domain.resource.ProductVersion;

/**
 * This is the many-to-many entity that correlates a product version with one of the package versions that are
 * applicable to it. It is an explicit relationship mapping entity between {@link ProductVersion} and
 * {@link PackageVersion}.
 *
 * @author John Mazzitelli
 */
@Entity
@IdClass(ProductVersionPackageVersionPK.class)
@NamedQueries( { @NamedQuery(name = ProductVersionPackageVersion.DELETE_BY_PACKAGE_VERSION_ID, query = "DELETE ProductVersionPackageVersion pvpv WHERE pvpv.packageVersion.id = :packageVersionId") })
@Table(name = "RHQ_PKG_PRD_MAP")
public class ProductVersionPackageVersion implements Serializable {
    public static final String DELETE_BY_PACKAGE_VERSION_ID = "ProductVersionPackageVersion.deleteByPackageVersionId";

    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "PRD_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private ProductVersion productVersion;

    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "PKG_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private PackageVersion packageVersion;

    protected ProductVersionPackageVersion() {
    }

    public ProductVersionPackageVersion(ProductVersion productVersion, PackageVersion packageVersion) {
        this.productVersion = productVersion;
        this.packageVersion = packageVersion;
    }

    public ProductVersionPackageVersionPK getProductVersionPackageVersionPK() {
        return new ProductVersionPackageVersionPK(productVersion, packageVersion);
    }

    public void setProductVersionPackageVersionPK(ProductVersionPackageVersionPK pk) {
        this.productVersion = pk.getProductVersion();
        this.packageVersion = pk.getPackageVersion();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ChannelPV: ");
        str.append("prd=[").append(this.productVersion).append("]");
        str.append(", pkg=[").append(this.packageVersion).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((productVersion == null) ? 0 : productVersion.hashCode());
        result = (31 * result) + ((packageVersion == null) ? 0 : packageVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof ProductVersionPackageVersion))) {
            return false;
        }

        final ProductVersionPackageVersion other = (ProductVersionPackageVersion) obj;

        if (productVersion == null) {
            if (productVersion != null) {
                return false;
            }
        } else if (!productVersion.equals(other.productVersion)) {
            return false;
        }

        if (packageVersion == null) {
            if (packageVersion != null) {
                return false;
            }
        } else if (!packageVersion.equals(other.packageVersion)) {
            return false;
        }

        return true;
    }
}