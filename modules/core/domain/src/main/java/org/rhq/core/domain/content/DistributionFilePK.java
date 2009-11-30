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

public class DistributionFilePK implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings here,
     * even though this class is an @IdClass and it should not need the mappings here.  The mappings belong in the
     * entity itself.
     */
    @JoinColumn(name = "DISTRIBUTION_ID", referencedColumnName = "ID", nullable = false)
    private Distribution distribution;

    public DistributionFilePK() {
    }

    public DistributionFilePK(Distribution dist) {
        this.distribution = dist;
    }

    public Distribution getDistribution() {
        return distribution;
    }

    public void setDistribution(Distribution kstree) {
        this.distribution = kstree;
    }

    @Override
    public String toString() {
        return "DistributionFilePK: Distribution=[" + distribution + "]";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((getDistribution() == null) ? 0 : getDistribution().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof DistributionFilePK))) {
            return false;
        }

        final DistributionFilePK other = (DistributionFilePK) obj;

        if (getDistribution() == null) {
            if (other.getDistribution() != null) {
                return false;
            }
        } else if (!getDistribution().equals(other.getDistribution())) {
            return false;
        }

        return true;
    }
}
