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
package org.rhq.core.domain.bundle;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.rhq.core.domain.content.Repo;

/**
 * This is the many-to-many entity that correlates a bundle version with a repo that has content
 * needed by the bundle.
 *
 * @author John Mazzitelli
 */
@Entity
@IdClass(BundleVersionRepoPK.class)
@NamedQueries( {
    @NamedQuery(name = BundleVersionRepo.QUERY_FIND_BY_REPO_ID_NO_FETCH, query = "SELECT bvr FROM BundleVersionRepo bvr WHERE bvr.repo.id = :id "),
    @NamedQuery(name = BundleVersionRepo.QUERY_FIND_BY_BUNDLE_VERSION_ID_NO_FETCH, query = "SELECT bvr FROM BundleVersionRepo bvr WHERE bvr.bundleVersion.id = :id ") })
@Table(name = "RHQ_BUNDLE_VERSION_REPO")
public class BundleVersionRepo implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_REPO_ID_NO_FETCH = "BundleVersionRepo.findByRepoIdNoFetch";
    public static final String QUERY_FIND_BY_BUNDLE_VERSION_ID_NO_FETCH = "BundleVersionRepo.findByBundleVersionIdNoFetch";

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "BUNDLE_VERSION_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private BundleVersion bundleVersion;

    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "REPO_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Repo repo;

    protected BundleVersionRepo() {
    }

    public BundleVersionRepo(BundleVersion bundleVersion, Repo repo) {
        this.bundleVersion = bundleVersion;
        this.repo = repo;
    }

    public BundleVersionRepoPK getBundleVersionRepoPK() {
        return new BundleVersionRepoPK(bundleVersion, repo);
    }

    public void setBundleVersionRepoPK(BundleVersionRepoPK pk) {
        this.bundleVersion = pk.getBundleVersion();
        this.repo = pk.getRepo();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("BVR: ");
        str.append(", bv=[").append(this.bundleVersion).append("]");
        str.append(", repo=[").append(this.repo).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((bundleVersion == null) ? 0 : bundleVersion.hashCode());
        result = (31 * result) + ((repo == null) ? 0 : repo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof BundleVersionRepo))) {
            return false;
        }

        final BundleVersionRepo other = (BundleVersionRepo) obj;

        if (bundleVersion == null) {
            if (bundleVersion != null) {
                return false;
            }
        } else if (!bundleVersion.equals(other.bundleVersion)) {
            return false;
        }

        if (repo == null) {
            if (repo != null) {
                return false;
            }
        } else if (!repo.equals(other.repo)) {
            return false;
        }

        return true;
    }
}