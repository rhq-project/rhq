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
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * This is the many-to-many entity that correlates a package version with the content source that was responsible for
 * delivering that package. This is mainly to support the ability to see all the {@link ContentSource}s that a
 * particular {@link PackageVersion} can come from.
 *
 * @author John Mazzitelli
 */
@Entity
@IdClass(PackageVersionContentSourcePK.class)
@NamedQueries( {
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_NO_FETCH, query = "SELECT pvcs "
        + "  FROM PackageVersionContentSource pvcs " + " WHERE pvcs.contentSource.id = :id "),
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_PACKAGE_VERSION_ID_NO_FETCH, query = "SELECT pvcs "
        + "  FROM PackageVersionContentSource pvcs WHERE pvcs.packageVersion.id = :id"),
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID, query = "SELECT pvcs "
        + "  FROM PackageVersionContentSource pvcs " //
        + "       LEFT JOIN FETCH pvcs.packageVersion pv " //
        + "       LEFT JOIN FETCH pv.generalPackage gp " //
        + "       LEFT JOIN FETCH gp.packageType pt " //
        + "       LEFT JOIN FETCH pv.architecture arch " //
        + "       LEFT JOIN FETCH pv.extraProperties extra " //
        + " WHERE pvcs.contentSource.id = :id "),
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_REPO_ID, query = "SELECT pvcs "
        + "  FROM PackageVersionContentSource pvcs " //
        + "       LEFT JOIN FETCH pvcs.packageVersion pv " //
        + "       LEFT JOIN FETCH pv.generalPackage gp " //
        + "       LEFT JOIN FETCH gp.packageType pt " //
        + "       LEFT JOIN FETCH pv.architecture arch " //
        + "       LEFT JOIN FETCH pv.extraProperties extra " //
        + " WHERE pvcs.contentSource.id = :content_source_id " //
        + "   AND pvcs.packageVersion.id IN " //
        + "       ( SELECT rpv.packageVersion.id FROM RepoPackageVersion rpv " //
        + "         WHERE rpv.repo.id = :repo_id )"),
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_COUNT, query = "SELECT COUNT(pvcs.contentSource.id) "
        + "  FROM PackageVersionContentSource pvcs " + " WHERE pvcs.contentSource.id = :id "),

    // this is the same as FIND_BY_CONTENT_SOURCE_ID, but it returns for a set of content source IDs
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_ALL_CONTENT_SOURCE_IDS, query = "SELECT pvcs "
        + "  FROM PackageVersionContentSource pvcs " + "       LEFT JOIN FETCH pvcs.packageVersion pv "
        + "       LEFT JOIN FETCH pv.generalPackage gp " + "       LEFT JOIN FETCH gp.packageType pt "
        + "       LEFT JOIN FETCH pt.resourceType rt " + "       LEFT JOIN FETCH pv.architecture arch "
        + "       LEFT JOIN FETCH pv.extraProperties extra " + " WHERE pvcs.contentSource.id IN ( :ids ) "),
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_ALL_CONTENT_SOURCE_IDS_COUNT, query = "SELECT COUNT(pvcs.contentSource.id) "
        + "  FROM PackageVersionContentSource pvcs " + " WHERE pvcs.contentSource.id IN ( :ids ) "),

    // this is the same as FIND_BY_CONTENT_SOURCE_ID, but it only returns those PVs whose bits are not loaded yet
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_NOT_LOADED, query = "SELECT pvcs "
        + "  FROM PackageVersionContentSource pvcs " //
        + "       LEFT JOIN FETCH pvcs.packageVersion pv " //
        + "       LEFT JOIN FETCH pv.generalPackage gp " //
        + "       LEFT JOIN FETCH gp.packageType pt " //
        + "       LEFT JOIN FETCH pv.architecture arch " //
        + "       LEFT JOIN FETCH pv.extraProperties extra " //
        + " WHERE pvcs.contentSource.id = :id " //
        + "   AND pv.packageBits IS NULL " //
        + "   AND pv.id IN " //
        + "   (SELECT rpv.packageVersion.id FROM RepoPackageVersion rpv " //
        + "     WHERE rpv.repo.id = :repo_id)"),
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_NOT_LOADED_COUNT, query = "SELECT COUNT(pvcs.contentSource.id) "
        + "  FROM PackageVersionContentSource pvcs "
        + " WHERE pvcs.contentSource.id = :id "
        + "   AND pvcs.packageVersion.packageBits IS NULL "
        + "   AND pvcs.packageVersion.id IN " //
        + "   (SELECT rpv.packageVersion.id FROM RepoPackageVersion rpv " //
        + "     WHERE rpv.repo.id = :repo_id)"),

    // finds the set of content sources that can deliver the pkg ver to the subscribed resource
    @NamedQuery(name = PackageVersionContentSource.QUERY_FIND_BY_PKG_VER_ID_AND_RES_ID, query = "SELECT pvcs "
        + "  FROM PackageVersionContentSource pvcs " + "       LEFT JOIN pvcs.packageVersion pv "
        + "       LEFT JOIN pv.repoPackageVersions cpv " + "       LEFT JOIN cpv.repo.resourceRepos rc "
        + " WHERE rc.resource.id = :resourceId " + "   AND pv.id = :packageVersionId"),
    @NamedQuery(name = PackageVersionContentSource.DELETE_BY_CONTENT_SOURCE_ID, query = "DELETE PackageVersionContentSource pvcs WHERE pvcs.contentSource.id = :contentSourceId") })
@Table(name = "RHQ_PKG_VER_CONTENT_SRC_MAP")
public class PackageVersionContentSource implements Serializable {
    public static final String QUERY_FIND_BY_CONTENT_SOURCE_ID_NO_FETCH = "PackageVersionContentSource.findByContentSourceIdNoFetch";
    public static final String QUERY_FIND_BY_CONTENT_SOURCE_ID = "PackageVersionContentSource.findByContentSourceId";
    public static final String QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_REPO_ID = "PackageVersionContentSource.findByContentSourceIdAndRepoId";
    public static final String QUERY_FIND_BY_CONTENT_SOURCE_ID_COUNT = "PackageVersionContentSource.findByContentSourceIdCount";
    public static final String QUERY_FIND_BY_ALL_CONTENT_SOURCE_IDS = "PackageVersionContentSource.findByAllContentSourceIds";
    public static final String QUERY_FIND_BY_ALL_CONTENT_SOURCE_IDS_COUNT = "PackageVersionContentSource.findByAllContentSourceIdsCount";
    public static final String QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_NOT_LOADED = "PackageVersionContentSource.findByCSIdAndNotLoaded";
    public static final String QUERY_FIND_BY_CONTENT_SOURCE_ID_AND_NOT_LOADED_COUNT = "PackageVersionContentSource.findByCSIdAndNotLoadedCount";
    public static final String QUERY_FIND_BY_PKG_VER_ID_AND_RES_ID = "PackageVersionContentSource.findByPkgVerIdAndResId";
    public static final String DELETE_BY_CONTENT_SOURCE_ID = "PackageVersionContentSource.deleteByContentSourceId";
    public static final String QUERY_FIND_BY_PACKAGE_VERSION_ID_NO_FETCH = "PackageVersionContentSource.findByPackageVersionIdNoFetch";
    
    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "PACKAGE_VERSION_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private PackageVersion packageVersion;

    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "CONTENT_SRC_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private ContentSource contentSource;

    @Column(name = "LOCATION", nullable = false)
    private String location;

    protected PackageVersionContentSource() {
    }

    public PackageVersionContentSource(PackageVersion packageVersion, ContentSource contentSource, String location) {
        this.packageVersion = packageVersion;
        this.contentSource = contentSource;
        this.location = location;
    }

    public PackageVersionContentSourcePK getPackageVersionContentSourcePK() {
        return new PackageVersionContentSourcePK(packageVersion, contentSource);
    }

    public void setPackageVersionContentSourcePK(PackageVersionContentSourcePK pk) {
        this.packageVersion = pk.getPackageVersion();
        this.contentSource = pk.getContentSource();
    }

    /**
     * This is additional information about the relationship between the package version and the source from where it
     * came. Typically, this data helps locate the specific package version on the remote repository that the content
     * source connects to (e.g. this location can be a relative file path that is relative to a root directory of a
     * local file system content source repository).
     */
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("PVCS: ");
        str.append("location=[").append(this.location).append("]");
        str.append(", pv=[").append(this.packageVersion).append("]");
        str.append(", cs=[").append(this.contentSource).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((packageVersion == null) ? 0 : packageVersion.hashCode());
        result = (31 * result) + ((contentSource == null) ? 0 : contentSource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof PackageVersionContentSource))) {
            return false;
        }

        final PackageVersionContentSource other = (PackageVersionContentSource) obj;

        if (packageVersion == null) {
            if (other.packageVersion != null) {
                return false;
            }
        } else if (!packageVersion.equals(other.packageVersion)) {
            return false;
        }

        if (contentSource == null) {
            if (other.contentSource != null) {
                return false;
            }
        } else if (!contentSource.equals(other.contentSource)) {
            return false;
        }

        return true;
    }
}