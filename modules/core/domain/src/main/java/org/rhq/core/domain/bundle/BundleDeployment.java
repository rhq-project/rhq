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

import org.rhq.core.domain.resource.Resource;

/**
 * This is the many-to-many entity that correlates a bundle configuration with a resource where
 * that bundle config is installed. A bundle configuration is essentially a bundle version with
 * the custom configuration settings that were used to determine how/where that bundle version
 * was deployed to the resource.
 *
 * @author John Mazzitelli
 */
@Entity
@IdClass(BundleDeploymentPK.class)
@NamedQueries( {
    @NamedQuery(name = BundleDeployment.QUERY_FIND_BY_BUNDLE_CONFIG_ID_NO_FETCH, query = "SELECT bd FROM BundleDeployment bd WHERE bd.bundleConfig.id = :id "),
    @NamedQuery(name = BundleDeployment.QUERY_FIND_BY_RESOURCE_ID_NO_FETCH, query = "SELECT bd FROM BundleDeployment bd WHERE bd.resource.id = :id ") })
@Table(name = "RHQ_BUNDLE_DEPLOYMENT")
public class BundleDeployment implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_BUNDLE_CONFIG_ID_NO_FETCH = "BundleDeployment.findByBundleConfigIdNoFetch";
    public static final String QUERY_FIND_BY_RESOURCE_ID_NO_FETCH = "BundleDeployment.findByResourceIdNoFetch";

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "BUNDLE_VERSION_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private BundleConfig bundleConfig;

    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "REPO_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Resource resource;

    protected BundleDeployment() {
    }

    public BundleDeployment(BundleConfig bundleConfig, Resource repo) {
        this.bundleConfig = bundleConfig;
        this.resource = repo;
    }

    public BundleDeploymentPK getBundleVersionRepoPK() {
        return new BundleDeploymentPK(bundleConfig, resource);
    }

    public void setBundleVersionRepoPK(BundleDeploymentPK pk) {
        this.bundleConfig = pk.getBundleConfig();
        this.resource = pk.getResource();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("BundleDeployment: ");
        str.append(", bc=[").append(this.bundleConfig).append("]");
        str.append(", resource=[").append(this.resource).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((bundleConfig == null) ? 0 : bundleConfig.hashCode());
        result = (31 * result) + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof BundleDeployment))) {
            return false;
        }

        final BundleDeployment other = (BundleDeployment) obj;

        if (bundleConfig == null) {
            if (bundleConfig != null) {
                return false;
            }
        } else if (!bundleConfig.equals(other.bundleConfig)) {
            return false;
        }

        if (resource == null) {
            if (resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }

        return true;
    }
}