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
import javax.persistence.CascadeType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * This is the composite primary key for the {@link PackageVersionContentSource} entity. That entity is an explicit
 * many-to-many mapping table, so this composite key is simply the foreign keys to both ends of that relationship.
 *
 * @author John Mazzitelli
 */
public class PackageVersionContentSourcePK implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings here,
     * even though this class is an @IdClass and it should not need the mappings here.  The mappings belong in the
     * entity itself.
     */
    @JoinColumn(name = "PACKAGE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(cascade = CascadeType.PERSIST)
    // persist so we can add the version as soon as we map it
    private PackageVersion packageVersion;

    @JoinColumn(name = "CONTENT_SRC_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ContentSource contentSource;

    public PackageVersionContentSourcePK() {
    }

    public PackageVersionContentSourcePK(PackageVersion pv, ContentSource cs) {
        packageVersion = pv;
        contentSource = cs;
    }

    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(PackageVersion packageVersion) {
        this.packageVersion = packageVersion;
    }

    public ContentSource getContentSource() {
        return contentSource;
    }

    public void setContentSource(ContentSource contentSource) {
        this.contentSource = contentSource;
    }

    @Override
    public String toString() {
        return "PackageVersionContentSourcePK: packageVersion=[" + packageVersion + "]; contentSource=["
            + contentSource + "]";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((contentSource == null) ? 0 : contentSource.hashCode());
        result = (31 * result) + ((packageVersion == null) ? 0 : packageVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof PackageVersionContentSourcePK))) {
            return false;
        }

        final PackageVersionContentSourcePK other = (PackageVersionContentSourcePK) obj;

        if (contentSource == null) {
            if (other.contentSource != null) {
                return false;
            }
        } else if (!contentSource.equals(other.contentSource)) {
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