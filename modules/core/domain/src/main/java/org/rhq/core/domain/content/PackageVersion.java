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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.util.OSGiVersionComparator;

/**
 * Represents a specific version of a {@link Package}. This does <i>not</i> represent an installed package found
 * deployed in any platform or on a resource (that's {@link InstalledPackage}). This object refers to a known version of
 * a particular package that can be installed on applicable resources.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries({
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_PACKAGE_VERSION, query = "" //
        + "SELECT pv FROM PackageVersion AS pv " //
        + " WHERE pv.generalPackage.id = :packageId " //
        + "   AND pv.version = :version "//
        + " ORDER BY pv.id DESC "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_PACKAGE_VER_ARCH, query = "SELECT pv FROM PackageVersion AS pv "
        + " WHERE pv.generalPackage.name = :name " + "   AND pv.generalPackage.packageType.id = :packageTypeId "
        + "   AND pv.architecture.id = :architectureId " + "   AND pv.version = :version "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY_WITH_NON_NULL_RESOURCE_TYPE, query = "SELECT pv FROM PackageVersion AS pv "
        + " WHERE pv.generalPackage.name = :packageName "
        + "   AND pv.generalPackage.packageType.name = :packageTypeName "
        + "   AND pv.generalPackage.packageType.resourceType.id = :resourceTypeId "
        + "   AND pv.architecture.name = :architectureName " + "   AND pv.version = :version "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY, query = "SELECT pv FROM PackageVersion AS pv "
        + " WHERE pv.generalPackage.name = :packageName "
        + "   AND pv.generalPackage.packageType.name = :packageTypeName "
        + "   AND pv.generalPackage.packageType.resourceType = :resourceType "
        + "   AND pv.architecture.name = :architectureName " + "   AND pv.version = :version "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_ID_BY_PACKAGE_DETAILS_KEY_AND_RES_ID, query = "SELECT pv.id "
        + "  FROM PackageVersion AS pv " + "       JOIN pv.generalPackage.packageType.resourceType.resources r "
        + " WHERE pv.generalPackage.name = :packageName "
        + "   AND pv.generalPackage.packageType.name = :packageTypeName " + "   AND r.id = :resourceId "
        + "   AND pv.architecture.name = :architectureName " + "   AND pv.version = :version "),

    // DO NOT SELECT DISTINCT IN OUTER SELECT - Oracle bombs on PackageVersion.metadata BLOB column
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_REPO_ID, query = "SELECT pv " + "  FROM PackageVersion pv "
        + " WHERE pv.id IN (SELECT DISTINCT pv1.id " + "                   FROM PackageVersion pv1 "
        + "                        LEFT JOIN pv1.repoPackageVersions cpv "
        + "                  WHERE cpv.repo.id = :repoId) "),

    // DO NOT SELECT DISTINCT IN OUTER SELECT - Oracle bombs on PackageVersion.metadata BLOB column
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_REPO_ID_FILTERED, query = "SELECT pv "
        + "  FROM PackageVersion pv " + " WHERE pv.id IN (SELECT DISTINCT pv1.id "
        + "                   FROM PackageVersion pv1 "
        + "                        LEFT JOIN pv1.repoPackageVersions cpv "
        + "                  WHERE cpv.repo.id = :repoId"
        + "                          AND (UPPER(pv1.displayName) LIKE :filter "
        + "                             OR :filter IS NULL)) "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_PACKAGE_ID, query = "SELECT pv " + "  FROM PackageVersion pv "
        + " WHERE pv.generalPackage.id = :packageId "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE, query = "SELECT pv "
        + "  FROM PackageVersion pv " + "       LEFT JOIN FETCH pv.generalPackage "
        + " WHERE pv.id IN (SELECT DISTINCT pv1.id " + "                   FROM PackageVersion pv1 "
        + "                        LEFT JOIN pv1.repoPackageVersions cpv "
        + "                  WHERE cpv.repo.id = :repoId) "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE_FILTERED, query = "SELECT pv "
        + "  FROM PackageVersion pv " + "       LEFT JOIN FETCH pv.generalPackage "
        + " WHERE pv.id IN (SELECT DISTINCT pv1.id " + "                   FROM PackageVersion pv1 "
        + "                        LEFT JOIN pv1.repoPackageVersions cpv "
        + "                  WHERE cpv.repo.id = :repoId"
        + "                          AND (UPPER(pv1.displayName) LIKE :filter ESCAPE :escapeChar "
        + "                             OR :filter IS NULL)) "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_METADATA_BY_RESOURCE_ID, query = "SELECT new org.rhq.core.domain.content.composite.PackageVersionMetadataComposite "
        + "                ( "
        + "                   pv.id, "
        + "                   pv.metadata, "
        + "                   pv.generalPackage.name, "
        + "                   pv.version, "
        + "                   pv.generalPackage.packageType.name, "
        + "                   pv.architecture.name "
        + "                ) "
        + "  FROM PackageVersion pv "
        + " WHERE pv.id IN (SELECT DISTINCT pv1.id "
        + "                   FROM PackageVersion pv1 "
        + "                        LEFT JOIN pv1.repoPackageVersions cpv "
        + "                        LEFT JOIN cpv.repo.resourceRepos rc "
        + "                  WHERE rc.resource.id = :resourceId) "),
    @NamedQuery(name = PackageVersion.QUERY_GET_PKG_BITS_LENGTH_BY_PKG_DETAILS_AND_RES_ID, query = "SELECT pv.fileSize "
        + "  FROM PackageVersion AS pv "
        + "       JOIN pv.generalPackage.packageType.resourceType.resources r "
        + " WHERE pv.generalPackage.name = :packageName "
        + "   AND pv.generalPackage.packageType.name = :packageTypeName "
        + "   AND r.id = :resourceId "
        + "   AND pv.architecture.name = :architectureName " + "   AND pv.version = :version "),

    // deletes orphaned package versions - that is, if they have no associated content sources or repos and is not installed anywhere
    @NamedQuery(name = PackageVersion.DELETE_IF_NO_CONTENT_SOURCES_OR_REPOS, query = "DELETE PackageVersion pv "
        + " WHERE pv.id NOT IN (SELECT pvcs.packageVersion.id " //
        + "                       FROM PackageVersionContentSource pvcs) " //
        + "   AND pv.repoPackageVersions IS EMPTY " //
        + "   AND pv.installedPackages IS EMPTY " //
        + "   AND pv.installedPackageHistory IS EMPTY "),

    @NamedQuery(name = PackageVersion.DELETE_SINGLE_IF_NO_CONTENT_SOURCES_OR_REPOS, query = "DELETE PackageVersion pv "
        + " WHERE pv.id = :packageVersionId" //
        + "   AND pv.repoPackageVersions IS EMPTY " //
        + "   AND pv.installedPackages IS EMPTY " //
        + "   AND pv.installedPackageHistory IS EMPTY "),
    @NamedQuery(name = PackageVersion.DELETE_MULTIPLE_IF_NO_CONTENT_SOURCES_OR_REPOS, query = "DELETE PackageVersion pv "
        + " WHERE pv.id IN ( :packageVersionIds )" //
        + "   AND pv.repoPackageVersions IS EMPTY " //
        + "   AND pv.installedPackages IS EMPTY " //
        + "   AND pv.installedPackageHistory IS EMPTY "),
    // the bulk delete that removes the PVPV mapping from orphaned package versions
    @NamedQuery(name = PackageVersion.DELETE_PVPV_IF_NO_CONTENT_SOURCES_OR_REPOS, query = "DELETE ProductVersionPackageVersion pvpv "
        + " WHERE pvpv.packageVersion.id NOT IN (SELECT pvcs.packageVersion.id "
        + "                                        FROM PackageVersionContentSource pvcs) "
        + "   AND pvpv.packageVersion.repoPackageVersions IS EMPTY "
        + "   AND pvpv.packageVersion.installedPackages IS EMPTY "
        + "   AND pvpv.packageVersion.installedPackageHistory IS EMPTY "),

    // finds all orphaned PVs that have extra props configurations (so the configs can be deleted)
    @NamedQuery(name = PackageVersion.FIND_EXTRA_PROPS_IF_NO_CONTENT_SOURCES_OR_REPOS, query = "SELECT pv "
        + "  FROM PackageVersion pv LEFT JOIN FETCH pv.extraProperties "
        + " WHERE pv.id NOT IN (SELECT pvcs.packageVersion.id "
        + "                       FROM PackageVersionContentSource pvcs) " + "   AND pv.repoPackageVersions IS EMPTY "
        + "   AND pv.installedPackages IS EMPTY " + "   AND pv.installedPackageHistory IS EMPTY "
        + "   AND pv.extraProperties IS NOT NULL "),

    // finds all orphaned PVs that have its bits loaded on the filesystem
    @NamedQuery(name = PackageVersion.FIND_FILES_IF_NO_CONTENT_SOURCES_OR_REPOS, query = "SELECT new org.rhq.core.domain.content.composite.PackageVersionFile( "
        + "          pv.id, "
        + "          pv.fileName "
        + "       ) "
        + "  FROM PackageVersion pv JOIN pv.packageBits pb "
        + " WHERE pv.id NOT IN (SELECT pvcs.packageVersion.id "
        + "                       FROM PackageVersionContentSource pvcs) "
        + "   AND pv.repoPackageVersions IS EMPTY "
        + "   AND pv.installedPackages IS EMPTY "
        + "   AND pv.installedPackageHistory IS EMPTY "
        + "   AND pb.blob.bits IS NULL "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_COMPOSITE_BY_ID, query = "SELECT new org.rhq.core.domain.content.composite.PackageVersionComposite( "
        + "          pv, "
        + "          pv.generalPackage.packageType.name, "
        + "          pv.generalPackage.packageType.category, "
        + "          pv.generalPackage.name, "
        + "          pv.architecture.name, "
        + "          pv.generalPackage.classification, "
        + "          pv.packageBits.id, "
        + "          (SELECT count(pb.id) FROM pv.packageBits pb WHERE pb.blob.bits IS NOT NULL) "
        + "       ) "
        + "  FROM PackageVersion pv WHERE pv.id = :id "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_COMPOSITE_BY_ID_WITH_PROPS, query = "SELECT new org.rhq.core.domain.content.composite.PackageVersionComposite( "
        + "          pv, "
        + "          (SELECT c FROM Configuration c WHERE c.id = pv.extraProperties.id), "
        + "          pv.generalPackage.packageType.name, "
        + "          pv.generalPackage.packageType.category, "
        + "          pv.generalPackage.name, "
        + "          pv.architecture.name, "
        + "          pv.generalPackage.classification, "
        + "          pv.packageBits.id, "
        + "          (SELECT count(pb.id) FROM pv.packageBits pb WHERE pb.blob.bits IS NOT NULL) "
        + "       ) "
        + "  FROM PackageVersion pv WHERE pv.id = :id"),
    @NamedQuery(name = PackageVersion.QUERY_FIND_COMPOSITES_BY_IDS, query = "SELECT new org.rhq.core.domain.content.composite.PackageVersionComposite( "
        + "          pv, "
        + "          pv.generalPackage.packageType.name, "
        + "          pv.generalPackage.packageType.category, "
        + "          pv.generalPackage.name, "
        + "          pv.architecture.name, "
        + "          pv.generalPackage.classification "
        + "       ) "
        + "  FROM PackageVersion pv WHERE pv.id IN (:ids) "),
    @NamedQuery(name = PackageVersion.QUERY_FIND_COMPOSITE_BY_FILTERS, query = "SELECT new org.rhq.core.domain.content.composite.PackageVersionComposite( "
        + "          pv, "
        + "          pv.generalPackage.packageType.displayName, "
        + "          pv.generalPackage.packageType.category, "
        + "          pv.generalPackage.name, "
        + "          pv.architecture.name, "
        + "          pv.generalPackage.classification "
        + "       ) "
        + "  FROM PackageVersion pv "
        + "  JOIN pv.repoPackageVersions cpv "
        + "  JOIN cpv.repo.resourceRepos rc "
        + "  LEFT JOIN pv.productVersionPackageVersions pvpv "
        + " WHERE rc.resource.id = :resourceId "
        + "   AND cpv.repo.id = rc.repo.id "
        + "   AND (UPPER(pv.displayName) LIKE :filter "
        + "        OR :filter IS NULL) "
        + "   AND (pv.productVersionPackageVersions IS EMPTY "
        + "        OR (pv.productVersionPackageVersions IS NOT EMPTY "
        + "            AND pvpv.productVersion = rc.resource.productVersion)) "
        + "   AND (pv.id NOT IN "
        + "            (SELECT pv1.id FROM PackageVersion pv1 "
        + "             WHERE pv1.id IN "
        + "                 (SELECT ip1.packageVersion.id FROM InstalledPackage ip1 WHERE ip1.resource.id = :resourceId)"
        + "            )" + "       )"),
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_ID, query = "SELECT pv FROM PackageVersion pv WHERE pv.id = :id"),
    @NamedQuery(name = PackageVersion.QUERY_FIND_PACKAGEVERSION_BY_FILENAME, query = "SELECT pv FROM PackageVersion AS pv WHERE pv.fileName = :rpmName)"),
    @NamedQuery(name = PackageVersion.QUERY_FIND_BY_PACKAGE_AND_REPO_ID, query = "SELECT pv"
        + " FROM PackageVersion pv" + " JOIN pv.repoPackageVersions rpv" + " WHERE pv.generalPackage.id = :packageId"
        + "     AND rpv.repo.id = :repoId"),
    @NamedQuery(name = PackageVersion.QUERY_FIND_DELETEABLE_IDS_IN_REPO, query = "SELECT pv.id FROM PackageVersion pv"
        + " WHERE (pv.id, 1) IN"
        + "   (SELECT pv2.id, (SELECT COUNT(rpv) FROM RepoPackageVersion rpv WHERE rpv.packageVersion.id = pv2.id)"
        + "    FROM PackageVersion pv2" + "    WHERE pv2.id IN ( :packageVersionIds )"
        + "      AND pv2.id IN (SELECT rpv.packageVersion.id FROM RepoPackageVersion rpv WHERE rpv.repo.id = :repoId))") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_PACKAGE_VERSION_ID_SEQ", sequenceName = "RHQ_PACKAGE_VERSION_ID_SEQ")
@Table(name = "RHQ_PACKAGE_VERSION")
public class PackageVersion implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_PACKAGE_VERSION = "PackageVersion.findByPackageVersion";
    public static final String QUERY_FIND_BY_PACKAGE_VER_ARCH = "PackageVersion.findByPackageVerArch";

    public static final String QUERY_FIND_BY_PACKAGE_SHA_RES_TYPE = "PackageVersion.findByPackageShaResType";
    public static final String QUERY_FIND_BY_PACKAGE_DETAILS_KEY_WITH_NON_NULL_RESOURCE_TYPE = "PackageVersion.findByPackageDetailsKeyWithNonNullResourceType";
    public static final String QUERY_FIND_BY_PACKAGE_DETAILS_KEY = "PackageVersion.findByPackageDetailsKey";
    public static final String QUERY_FIND_BY_PACKAGE_DETAILS_SHA = "PackageVersion.findByPackageDetailsSha";
    public static final String QUERY_FIND_ID_BY_PACKAGE_DETAILS_KEY_AND_RES_ID = "PackageVersion.findIdByPackageDetailsKeyAndResId";
    public static final String QUERY_FIND_BY_REPO_ID = "PackageVersion.findByRepoId";
    public static final String QUERY_FIND_BY_REPO_ID_FILTERED = "PackageVersion.findByRepoIdFiltered";
    public static final String QUERY_FIND_BY_PACKAGE_ID = "PackageVersion.findByPackageId";
    public static final String QUERY_FIND_BY_PACKAGE_AND_REPO_ID = "PackageVersion.findByPackageAndRepoId";
    public static final String QUERY_FIND_BY_REPO_ID_WITH_PACKAGE = "PackageVersion.findByRepoIdWithPackage";
    public static final String QUERY_FIND_BY_REPO_ID_WITH_PACKAGE_FILTERED = "PackageVersion.findByRepoIdWithPackageFiltered";
    public static final String QUERY_FIND_METADATA_BY_RESOURCE_ID = "PackageVersion.findMetadataByResourceId";
    public static final String QUERY_GET_PKG_BITS_LENGTH_BY_PKG_DETAILS_AND_RES_ID = "PackageVersion.getPkgBitsLengthByPkgDetailsAndResId";
    public static final String DELETE_IF_NO_CONTENT_SOURCES_OR_REPOS = "PackageVersion.deleteIfNoContentSourcesOrRepos";
    public static final String DELETE_SINGLE_IF_NO_CONTENT_SOURCES_OR_REPOS = "PackageVersion.deleteSingleIfNoContentSourcesOrRepos";
    public static final String DELETE_MULTIPLE_IF_NO_CONTENT_SOURCES_OR_REPOS = "PackageVersion.deleteMultipleIfNoContentSourcesOrRepos";
    public static final String DELETE_PVPV_IF_NO_CONTENT_SOURCES_OR_REPOS = "PackageVersion.deletePVPVIfNoContentSourcesOrRepos";
    public static final String FIND_EXTRA_PROPS_IF_NO_CONTENT_SOURCES_OR_REPOS = "PackageVersion.findOrphanedExtraProps";
    public static final String FIND_FILES_IF_NO_CONTENT_SOURCES_OR_REPOS = "PackageVersion.findOrphanedFiles";
    public static final String QUERY_FIND_COMPOSITE_BY_ID = "PackageVersion.findCompositeById";
    public static final String QUERY_FIND_COMPOSITE_BY_ID_WITH_PROPS = "PackageVersion.findCompositeByIdWithProps";
    public static final String QUERY_FIND_COMPOSITES_BY_IDS = "PackageVersion.findCompositesByIds";
    public static final String QUERY_FIND_COMPOSITE_BY_FILTERS = "PackageVersion.findCompositeByFilters";
    public static final String QUERY_FIND_BY_ID = "PackageVersion.findById";
    public static final String QUERY_FIND_PACKAGEVERSION_BY_FILENAME = "PackageVersion.findPackageVersionByFilename";
    public static final String QUERY_FIND_DELETEABLE_IDS_IN_REPO = "PackageVersion.findDeleteableVersionIds";

    /**
     * This is a default {@link Comparator} implementation for package versions.
     * If the package versions being compared both have non-null {@link PackageVersion#getVersion() versions}
     * an {@link OSGiVersionComparator} is used to compare them. If it fails or if one of the versions
     * is null the package versions are compared by {@link PackageVersion#getFileCreatedDate() file created date}.
     * If the creation date is not specified for one of the package versions, they proclaimed equal.
     * <p>
     * Note that this comparator is *INCONSISTENT* with <code>equals()</code> and therefore care should be taken
     * when using it with sets and maps (read this {@link Comparator documentation}).
     *
     * @author Lukas Krejci
     */
    public static class DefaultPackageVersionComparator implements Comparator<PackageVersion>, Serializable {
        private static final long serialVersionUID = 1L;

        public int compare(PackageVersion p1, PackageVersion p2) {
            String v1 = p1.getVersion();
            String v2 = p2.getVersion();

            OSGiVersionComparator c = new OSGiVersionComparator();

            if (v1 != null && v2 != null) {
                try {
                    return c.compare(v1, v2);
                } catch (IllegalArgumentException e) {
                    //well, this can happen.. not all packages have OSGi type versions.
                }
            }

            if (p1.getFileCreatedDate() != null && p2.getFileCreatedDate() != null) {
                return p1.getFileCreatedDate().compareTo(p2.getFileCreatedDate());
            }

            //hmm... there's actually nothing we can sort these two by..
            //let's compare them by id - the one inserted sooner will have a lower id

            return Integer.valueOf(p1.getId()).compareTo(p2.getId());
        }
    };

    /**
     * @see DefaultPackageVersionComparator
     */
    public static final DefaultPackageVersionComparator DEFAULT_COMPARATOR = new DefaultPackageVersionComparator();

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_PACKAGE_VERSION_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "PACKAGE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, optional = false)
    private Package generalPackage;

    @Column(name = "DISPLAY_NAME", nullable = true)
    private String displayName;

    @Column(name = "SHORT_DESCRIPTION", nullable = true)
    private String shortDescription;

    @Column(name = "LONG_DESCRIPTION", nullable = true)
    private String longDescription;

    /**
     * This is basically an enhanced SHA, and will be unique across PVs for a package.  Architecture is no longer
     * a required differentiator.
     */
    @Column(name = "VERSION", nullable = false)
    private String version;

    @Column(name = "DISPLAY_VERSION", nullable = true)
    private String displayVersion;

    @JoinColumn(name = "ARCHITECTURE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST, optional = false)
    private Architecture architecture;

    @Column(name = "FILE_NAME", nullable = true)
    private String fileName;

    @Column(name = "FILE_SIZE", nullable = true)
    private Long fileSize;

    @Column(name = "FILE_MD5", nullable = true)
    private String md5;

    @Column(name = "FILE_SHA256", nullable = true)
    private String sha256;

    @Column(name = "FILE_CREATION_TIME", nullable = true)
    private Long fileCreatedDate;

    @Column(name = "LICENSE_NAME", nullable = true)
    private String licenseName;

    @Column(name = "LICENSE_VERSION", nullable = true)
    private String licenseVersion;

    @Column(name = "METADATA", nullable = true)
    private byte[] metadata;

    @JoinColumn(name = "CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Configuration extraProperties;

    @OneToMany(mappedBy = "packageVersion", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<RepoPackageVersion> repoPackageVersions;

    // this mapping is here mainly to support our JPA queries
    @OneToMany(mappedBy = "packageVersion", fetch = FetchType.LAZY)
    private Set<InstalledPackage> installedPackages;

    // this mapping is here mainly to support our JPA queries
    @OneToMany(mappedBy = "packageVersion", fetch = FetchType.LAZY)
    private Set<InstalledPackageHistory> installedPackageHistory;

    // No longer use cascade PERSIST on this.  We'll associate it manually due to intracacies in blob handling
    @JoinColumn(name = "PACKAGE_BITS_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY, optional = true)
    @XmlTransient
    private PackageBits packageBits;

    @OneToMany(mappedBy = "packageVersion", cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY)
    private Set<ProductVersionPackageVersion> productVersionPackageVersions;

    // Constructor ----------------------------------------

    public PackageVersion() {
        // for JPA use
    }

    public PackageVersion(Package pkg, String version, Architecture arch) {
        setGeneralPackage(pkg);
        setVersion(version);
        setArchitecture(arch);
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Package that this version applies to.
     */
    public Package getGeneralPackage() {
        return generalPackage;
    }

    public void setGeneralPackage(Package generalPackage) {
        this.generalPackage = generalPackage;
    }

    /**
     * Name of this package version that is suitable for display to the user in the UI.
     */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Short free text description of this version of the package. In other words, a summary of the package.
     */
    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    /**
     * Long free text description of this version of the package.
     */
    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    /**
     * The machine understandable version of the package. The format of this attribute will vary based on the package.
     * It should be possible by simply sorting package versions on this column to determine which package version is
     * newer or older than another. Anyone that creates package versions must be able to generate valid version strings
     * that support those semantics.
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * A version string suitable for displaying to a user. It may or may not be the same as {#getVersion()}.
     */
    public String getDisplayVersion() {
        return (displayVersion != null) ? displayVersion : version;
    }

    public void setDisplayVersion(String displayVersion) {
        this.displayVersion = displayVersion;
    }

    /**
     * Architecture of this package version which tells you the kinds of platforms (e.g. operating system or hardware)
     * that this package version can be installed on.
     */
    public Architecture getArchitecture() {
        return architecture;
    }

    public void setArchitecture(Architecture architecture) {
        this.architecture = architecture;
    }

    /**
     * Name of the file with the contents of this package.
     */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Size of the package's file.
     */
    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * The MD5 hash of this package version's file contents.
     */
    public String getMD5() {
        return md5;
    }

    public void setMD5(String md5) {
        this.md5 = md5;
    }

    /**
     * The SHA-256 hash of this package version's file contents.
     */
    public String getSHA256() {
        return sha256;
    }

    public void setSHA256(String sha256) {
        this.sha256 = sha256;
    }

    /**
     * Timestamp indicating when the package's file was created.
     */
    public Long getFileCreatedDate() {
        return fileCreatedDate;
    }

    public void setFileCreatedDate(Long fileCreatedDate) {
        this.fileCreatedDate = fileCreatedDate;
    }

    /**
     * Name of the license under which the package falls.
     *
     * @see #getLicenseVersion()
     */
    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    /**
     * Version of the package's {@link #getLicenseName() license}.
     */
    public String getLicenseVersion() {
        return licenseVersion;
    }

    public void setLicenseVersion(String licenseVersion) {
        this.licenseVersion = licenseVersion;
    }

    /**
     * Optional blob of metadata that is only meaningful to the plugin that is responsible for installing this package
     * version. This may be <code>null</code> if this package version has no applicable metadata for it. Typical usages
     * of this is to provide instructional information describing the install steps the plugin needs to perform to
     * install the package; or yum XML information on rpms.
     */
    public byte[] getMetadata() {
        return metadata;
    }

    public void setMetadata(byte[] metadata) {
        this.metadata = metadata;
    }

    /**
     * Values to further describe this package version. Values in this object will adhere to the configuration defined
     * by the associated package type as found in this object's {@link #getGeneralPackage() package}.
     *
     * @see PackageType#getPackageExtraPropertiesDefinition()
     */
    public Configuration getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Configuration extraProperties) {
        this.extraProperties = extraProperties;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getRepos()
     */
    public Set<RepoPackageVersion> getRepoPackageVersions() {
        return repoPackageVersions;
    }

    /**
     * The set of repos that can serve up this package version.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated repos, use
     * {@link #getRepoPackageVersions()} or {@link #addRepo(Repo)}, {@link #removeRepo(Repo)}.</p>
     */
    public Set<Repo> getRepos() {
        HashSet<Repo> repos = new HashSet<Repo>();

        if (repoPackageVersions != null) {
            for (RepoPackageVersion cpv : repoPackageVersions) {
                repos.add(cpv.getRepoPackageVersionPK().getRepo());
            }
        }

        return repos;
    }

    /**
     * Directly assign this package version to the given repo.
     *
     * @param repo
     *
     * @return the mapping that was added
     */
    public RepoPackageVersion addRepo(Repo repo) {
        if (this.repoPackageVersions == null) {
            this.repoPackageVersions = new HashSet<RepoPackageVersion>();
        }

        RepoPackageVersion mapping = new RepoPackageVersion(repo, this);
        this.repoPackageVersions.add(mapping);
        repo.addPackageVersion(this);
        return mapping;
    }

    public Set<InstalledPackage> getInstalledPackages() {
        return installedPackages;
    }

    public Set<InstalledPackageHistory> getInstalledPackageHistory() {
        return installedPackageHistory;
    }

    /**
     * Removes the repo as one that this package version is related to. The mapping that was removed is returned; if
     * the given package version was not a member of the repo, <code>null</code> is returned.
     *
     * @param  repo
     *
     * @return the mapping that was removed or <code>null</code> if this package version was not mapped to the given
     *         repo
     */
    public RepoPackageVersion removeRepo(Repo repo) {
        if ((this.repoPackageVersions == null) || (repo == null)) {
            return null;
        }

        RepoPackageVersion doomed = null;

        for (RepoPackageVersion cpv : this.repoPackageVersions) {
            if (repo.equals(cpv.getRepoPackageVersionPK().getRepo())) {
                doomed = cpv;
                repo.removePackageVersion(this);
                break;
            }
        }

        if (doomed != null) {
            this.repoPackageVersions.remove(doomed);
        }

        return doomed;
    }

    /**
     * If this package version's actual contents (its "bits") have been inventoried, this will return a
     * {@link PackageBits} object representing that data. If there is no content available yet, this will return <code>
     * null</code>. When <code>null</code> is returned, it means it has to be pulled down, usually via a
     * {@link ContentSource}. <b>WARNING!</b> You most likely do <i>not</i> want to call this method if inside the scope
     * of an entity manager session. If you do, this will load in the {@link PackageBits} object which potentially will
     * load the entire package's contents in memory (and thus, if large enough, will cause an OutOfMemoryError).
     */
    @XmlTransient
    public PackageBits getPackageBits() {
        return packageBits;
    }

    public void setPackageBits(PackageBits packageBits) {
        this.packageBits = packageBits;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getProductVersions()
     */
    public Set<ProductVersionPackageVersion> getProductVersionPackageVersions() {
        return productVersionPackageVersions;
    }

    /**
     * The product versions that this package version is associated with.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated product versions,
     * use {@link #getProductVersionPackageVersions()} or {@link #addProductVersion(ProductVersion)},
     * {@link #removeProductVersion(ProductVersion)}.</p>
     */
    public Set<ProductVersion> getProductVersions() {
        HashSet<ProductVersion> productVersions = new HashSet<ProductVersion>();

        if (productVersionPackageVersions != null) {
            for (ProductVersionPackageVersion pvpv : productVersionPackageVersions) {
                productVersions.add(pvpv.getProductVersionPackageVersionPK().getProductVersion());
            }
        }

        return productVersions;
    }

    /**
     * Directly assign a product version to this package version.
     *
     * @param  productVersion
     *
     * @return the mapping that was added
     */
    public ProductVersionPackageVersion addProductVersion(ProductVersion productVersion) {
        if (this.productVersionPackageVersions == null) {
            this.productVersionPackageVersions = new HashSet<ProductVersionPackageVersion>();
        }

        ProductVersionPackageVersion mapping = new ProductVersionPackageVersion(productVersion, this);
        this.productVersionPackageVersions.add(mapping);
        return mapping;
    }

    /**
     * Removes the product version from this package version, if it exists. If it does exist, the mapping that was
     * removed is returned; if the given product version did not exist as one that is a member of this package version,
     * <code>null</code> is returned.
     *
     * @param  productVersion the product version to remove
     *
     * @return the mapping that was removed or <code>null</code> if the product version was not mapped to this package
     *         version
     */
    public ProductVersionPackageVersion removeProductVersion(ProductVersion productVersion) {
        if ((this.productVersionPackageVersions == null) || (productVersion == null)) {
            return null;
        }

        ProductVersionPackageVersion doomed = null;

        for (ProductVersionPackageVersion pvpv : this.productVersionPackageVersions) {
            if (productVersion.equals(pvpv.getProductVersionPackageVersionPK().getProductVersion())) {
                doomed = pvpv;
                break;
            }
        }

        if (doomed != null) {
            this.productVersionPackageVersions.remove(doomed);
        }

        return doomed;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        StringBuffer toString = new StringBuffer();
        toString.append("PackageVersion[");
        toString.append("package=").append(generalPackage).append(",");
        toString.append("version=").append(version).append(",");
        toString.append("architecture=").append(architecture);
        toString.append("]");

        return toString.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof PackageVersion)) {
            return false;
        }

        PackageVersion that = (PackageVersion) o;

        if ((architecture != null) ? (!architecture.equals(that.architecture)) : (that.architecture != null)) {
            return false;
        }

        if ((generalPackage != null) ? (!generalPackage.equals(that.generalPackage)) : (that.generalPackage != null)) {
            return false;
        }

        if ((version != null) ? (!version.equals(that.version)) : (that.version != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = ((generalPackage != null) ? generalPackage.hashCode() : 0);
        result = (31 * result) + ((version != null) ? version.hashCode() : 0);
        result = (31 * result) + ((architecture != null) ? architecture.hashCode() : 0);
        return result;
    }
}