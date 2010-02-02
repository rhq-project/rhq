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

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.rhq.core.domain.resource.Resource;

/**
 * This is the composite primary key for the {@link BundleDeployment} entity. That entity is an explicit
 * many-to-many mapping table, so this composite key is simply the foreign keys to both ends of that relationship.
 *
 * @author John Mazzitelli
 */
public class BundleDeploymentPK implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings here,
     * even though this class is an @IdClass and it should not need the mappings here.  The mappings belong in the
     * entity itself.
     */
    @JoinColumn(name = "BUNDLE_CONFIG_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    // persist so we can add the version as soon as we map it
    private BundleConfig bundleConfig;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Resource resource;

    public BundleDeploymentPK() {
    }

    public BundleDeploymentPK(BundleConfig bc, Resource r) {
        bundleConfig = bc;
        resource = r;
    }

    public BundleConfig getBundleConfig() {
        return bundleConfig;
    }

    public void setBundleConfig(BundleConfig bc) {
        this.bundleConfig = bc;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        return "BundleDeploymentPK: bundleConfig=[" + bundleConfig + "]; resource=[" + resource + "]";
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

        if (!(obj instanceof BundleDeploymentPK)) {
            return false;
        }

        final BundleDeploymentPK other = (BundleDeploymentPK) obj;

        if (bundleConfig == null) {
            if (other.bundleConfig != null) {
                return false;
            }
        } else if (!bundleConfig.equals(other.bundleConfig)) {
            return false;
        }

        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }

        return true;
    }
}