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
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

/**
 * This is the many-to-many entity that correlates an advisory with a package.
 *
 * @author Pradeep Kilambi
 */

@Entity
@NamedQueries( {
    @NamedQuery(name = AdvisoryPackage.FIND_PACKAGES_BY_ADV_ID, query = "SELECT ap FROM AdvisoryPackage AS ap "
        + "WHERE ap.advisory.id = :advId"),
    @NamedQuery(name = AdvisoryPackage.DELETE_PACKAGES_BY_ADV_ID, query = "DELETE AdvisoryPackage ap WHERE ap.advisory.id = :advId"),
    @NamedQuery(name = AdvisoryPackage.FIND_ADVISORY_PACKAGE, query = "SELECT ap FROM AdvisoryPackage AS ap "
        + "WHERE ap.advisory.id = :advId AND ap.pkg.id = :pkgVerId") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_ADVISORY_PACKAGE_ID_SEQ")
@Table(name = "RHQ_ADVISORY_PACKAGE")
public class AdvisoryPackage implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String FIND_PACKAGES_BY_ADV_ID = "AdvisoryPackage.findPackagesByAdvId";
    public static final String DELETE_PACKAGES_BY_ADV_ID = "AdvisoryPackage.deletePackagesByAdvId";
    public static final String FIND_ADVISORY_PACKAGE = "AdvisoryPackage.findAdvisoryPackage";

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @XmlTransient
    @ManyToOne
    @JoinColumn(name = "ADVISORY_ID", referencedColumnName = "ID", nullable = false)
    private Advisory advisory;

    @ManyToOne
    @JoinColumn(name = "PACKAGE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    private PackageVersion pkg;

    @Column(name = "LAST_MODIFIED", nullable = false)
    private long lastModifiedDate;

    protected AdvisoryPackage() {
    }

    public AdvisoryPackage(Advisory adv, PackageVersion pkg) {
        this.advisory = adv;
        this.pkg = pkg;
    }

    public Advisory getAdvisory() {
        return advisory;
    }

    public void setAdvisory(Advisory advisory) {
        this.advisory = advisory;
    }

    public PackageVersion getPkg() {
        return pkg;
    }

    public void setPkg(PackageVersion pkg) {
        this.pkg = pkg;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @PrePersist
    void onPersist() {
        this.lastModifiedDate = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("AdvisoryPackage: ");
        str.append(", Advisory=[").append(this.advisory).append("]");
        str.append(", Package=[").append(this.pkg).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((advisory == null) ? 0 : advisory.hashCode());
        result = (31 * result) + ((pkg == null) ? 0 : pkg.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof AdvisoryPackage))) {
            return false;
        }

        final AdvisoryPackage other = (AdvisoryPackage) obj;

        if (advisory == null) {
            if (other.advisory != null) {
                return false;
            }
        } else if (!advisory.equals(other.advisory)) {
            return false;
        }

        if (pkg == null) {
            if (other.pkg != null) {
                return false;
            }
        } else if (!pkg.equals(other.pkg)) {
            return false;
        }

        return true;
    }
}