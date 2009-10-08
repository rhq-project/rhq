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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.rhq.core.domain.resource.ProductVersion;

/**
 * This is the composite primary key for the {@link ProductVersionPackageVersion} entity. That entity is an explicit
 * many-to-many mapping table, so this composite key is simply the foreign keys to both ends of that relationship.
 *
 * @author John Mazzitelli
 */
public class ProductVersionPackageVersionPK implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings here,
     * even though this class is an @IdClass and it should not need the mappings here.  The mappings belong in the
     * entity itself.
     */

    @JoinColumn(name = "PRD_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ProductVersion productVersion;

    @JoinColumn(name = "PKG_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private PackageVersion packageVersion;

    public ProductVersionPackageVersionPK() {
    }

    public ProductVersionPackageVersionPK(ProductVersion prd, PackageVersion pkg) {
        this.productVersion = prd;
        this.packageVersion = pkg;
    }

    public ProductVersion getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(ProductVersion prd) {
        this.productVersion = prd;
    }

    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(PackageVersion packageVersion) {
        this.packageVersion = packageVersion;
    }

    @Override
    public String toString() {
        return "ProductVersionPackageVersionPK: productVersion=[" + productVersion + "]; packageVersion=["
            + packageVersion + "]";
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

        if ((obj == null) || (!(obj instanceof ProductVersionPackageVersionPK))) {
            return false;
        }

        final ProductVersionPackageVersionPK other = (ProductVersionPackageVersionPK) obj;

        if (productVersion == null) {
            if (other.productVersion != null) {
                return false;
            }
        } else if (!productVersion.equals(other.productVersion)) {
            return false;
        }

        if (packageVersion == null) {
            if (other.packageVersion != null) {
                return false;
            }
        } else if (!packageVersion.equals(other.packageVersion)) {
            return false;
        }

        return true;
    }
}