/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.bundle;

import java.io.Serializable;

import javax.persistence.CascadeType;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.content.PackageVersion;

/**
 * Defines a bundle file that is part of a bundle version.
 *
 * @author John Mazzitelli
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = BundleFile.QUERY_FIND_ALL, query = "SELECT bf FROM BundleFile bf"), //
    @NamedQuery(name = BundleFile.QUERY_FIND_BY_BUNDLE_VERSION_ID, query = "SELECT bf FROM BundleFile bf WHERE bf.bundleVersion.id = :id") //
})
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_BUNDLE_FILE_ID_SEQ")
@Table(name = "RHQ_BUNDLE_FILE")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleFile implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "BundleFile.findAll";
    public static final String QUERY_FIND_BY_BUNDLE_VERSION_ID = "BundleFile.findByBundleVersionId";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "BUNDLE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BundleVersion bundleVersion;

    // Note, currently we cascade remove the PackageVersion because BundleVersions do not share
    // PackageVersions. They create a PV versioned the same as the BV.  If this approach changes in
    // the future (e.e reuse of PVs if the sha256 is not different) then this will will have to change
    // to reflect the fact that multiple BundleFiles could reference the same PackageVersion.
    @JoinColumn(name = "PACKAGE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.REMOVE)
    private PackageVersion packageVersion;

    public BundleFile() {
        // for JPA use
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(BundleVersion bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(PackageVersion packageVersion) {
        this.packageVersion = packageVersion;
    }

    @Override
    public String toString() {
        return "BundleFile[id=" + id + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bundleVersion == null) ? 0 : bundleVersion.hashCode());
        result = prime * result + ((packageVersion == null) ? 0 : packageVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BundleFile)) {
            return false;
        }

        BundleFile other = (BundleFile) obj;

        if (bundleVersion == null) {
            if (other.bundleVersion != null) {
                return false;
            }
        } else if (!bundleVersion.equals(other.bundleVersion)) {
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