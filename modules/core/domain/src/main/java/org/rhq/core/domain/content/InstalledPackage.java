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
import java.util.Date;
import java.util.List;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;

/**
 * Represents a {@link PackageVersion specific package} that is installed on a resource. Each instance of this object is
 * associated to one and only one {@link PackageVersion}.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_SET_OF_IDS, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.id IN ( :packageIds )"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_SET_OF_PACKAGE_VER_IDS, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.packageVersion.id IN ( :packageIds )"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_RESOURCE_ID, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.resource.id = :resourceId"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.resource.id = :resourceId AND ip.packageVersion.id = :packageVersionId"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_RESOURCE_AND_PACKAGE, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.resource.id = :resourceId AND ip.packageVersion.generalPackage.id = :packageId  "),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_BY_RESOURCE_AND_PACKAGE_VER, query = "SELECT ip FROM InstalledPackage AS ip WHERE ip.resource.id = :resourceId AND ip.packageVersion.id = :packageVersionId  "),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE, query = "SELECT new org.rhq.core.domain.content.composite.PackageListItemComposite(ip.id, gp.name, pt.displayName, ip.packageVersion.version) "
        + "FROM InstalledPackage ip JOIN ip.resource res LEFT JOIN ip.packageVersion pv LEFT JOIN pv.generalPackage gp LEFT JOIN gp.packageType pt "
        + "WHERE res.id = :resourceId"),
    @NamedQuery(name = InstalledPackage.QUERY_FIND_INSTALLED_PACKAGE_HISTORY, query = "SELECT ip "
        + "FROM InstalledPackage ip JOIN ip.resource res LEFT JOIN ip.packageVersion pv LEFT JOIN pv.generalPackage gp LEFT JOIN gp.packageType pt "
        + "WHERE res.id = :resourceId " + "  AND gp.id = :generalPackageId") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_INSTALLED_PACKAGE_ID_SEQ")
@Table(name = "RHQ_INSTALLED_PACKAGE")
public class InstalledPackage implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_SET_OF_IDS = "InstalledPackage.findBySetOfIds";
    public static final String QUERY_FIND_BY_SET_OF_PACKAGE_VER_IDS = "InstalledPackage.findBySetOfPackageVerIds";
    public static final String QUERY_FIND_BY_RESOURCE_ID = "InstalledPackage.findByResourceId";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_PKG_VER_ID = "InstalledPackage.findByResourceIdAndPackageVersionId";
    public static final String QUERY_FIND_BY_RESOURCE_AND_PACKAGE = "InstalledPackage.findByResourceAndPackage";
    public static final String QUERY_FIND_BY_RESOURCE_AND_PACKAGE_VER = "InstalledPackage.findByResourceAndPackageVer";
    public static final String QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE = "InstalledPackage.findPackageListItemComposite";
    public static final String QUERY_FIND_INSTALLED_PACKAGE_HISTORY = "InstalledPackage.findInstalledPackageHistory";

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Resource resource;

    @JoinColumn(name = "PACKAGE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private PackageVersion packageVersion;

    @JoinColumn(name = "DEPLOYMENT_CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Configuration deploymentConfigurationValues;

    @Column(name = "INSTALLATION_TIME", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date installationDate;

    @JoinColumn(name = "SUBJECT_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private Subject user;

    @OneToMany(mappedBy = "installedPackage", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private List<PackageInstallationStep> installationSteps;

    // Constructor ----------------------------------------

    public InstalledPackage() {
        // needed for JPA
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Resource where the package is installed.
     */
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * Specific version of the package installed on the {@link #getResource() resource}.
     */
    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(PackageVersion packageVersion) {
        this.packageVersion = packageVersion;
    }

    /**
     * Values that correspond to the deployment time properties that are defined by the {@link PackageType}. This may
     * not be known or only partially populated if the package was installed on the server through some external means
     * (it depends on the plugin's ability to detect these values on discovery). This will be <code>null</code> in the
     * case that the package type does not define any deploy time properties.
     *
     * @see PackageType#getDeploymentConfigurationDefinition()
     */
    public Configuration getDeploymentConfigurationValues() {
        return deploymentConfigurationValues;
    }

    public void setDeploymentConfigurationValues(Configuration deploymentConfigurationValues) {
        this.deploymentConfigurationValues = deploymentConfigurationValues;
    }

    /**
     * Timestamp the installation was performed, if it is known.
     */
    public Date getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(Date installationDate) {
        this.installationDate = installationDate;
    }

    /**
     * User who performed the installation, if it is known.
     */
    public Subject getUser() {
        return user;
    }

    public void setUser(Subject user) {
        this.user = user;
    }

    /**
     * User readable steps the plugin will perform to install the package. If specified, the UI will display these steps
     * to the user prior to executing the installation. The plugin will report on the success of each step in the
     * process as it attempts to install the package. These are optional, leaving it up to the discretion of the plugin
     * to determine how to install the package. In such a case, the plugin will simply report the success or failure of
     * the package installation.
     */
    public List<PackageInstallationStep> getInstallationSteps() {
        return installationSteps;
    }

    public void setInstallationSteps(List<PackageInstallationStep> installationSteps) {
        this.installationSteps = installationSteps;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return "InstalledPackage[resource=" + resource + ",packageVersion=" + packageVersion + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof InstalledPackage)) {
            return false;
        }

        InstalledPackage that = (InstalledPackage) o;

        if ((packageVersion != null) ? (!packageVersion.equals(that.packageVersion)) : (that.packageVersion != null)) {
            return false;
        }

        if ((resource != null) ? (!resource.equals(that.resource)) : (that.resource != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = ((resource != null) ? resource.hashCode() : 0);
        result = (31 * result) + ((packageVersion != null) ? packageVersion.hashCode() : 0);
        return result;
    }
}