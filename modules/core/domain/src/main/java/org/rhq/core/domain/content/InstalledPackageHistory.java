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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;

/**
 * Audit trail entity for tracking the results of an individual package request made on a resource. Once created, these
 * objects are effectively immutable; if the state of a package request changes a new entity will be created.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries({
    @NamedQuery(name = InstalledPackageHistory.QUERY_FIND_CONFIG_BY_PACKAGE_VERSION_AND_REQ, query = "SELECT dcv FROM InstalledPackageHistory iph JOIN iph.deploymentConfigurationValues dcv "
        + "WHERE iph.packageVersion = :packageVersion "
        + "AND iph.contentServiceRequest = :contentServiceRequest "
        + "ORDER BY iph.timestamp DESC"),
    @NamedQuery(name = InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID, query = "SELECT iph FROM InstalledPackageHistory iph "
        + "WHERE iph.contentServiceRequest.id = :contentServiceRequestId "
        + "AND iph.packageVersion.id = :packageVersionId " + "ORDER BY timestamp DESC"),
    @NamedQuery(name = InstalledPackageHistory.QUERY_FIND_BY_CSR_ID, query = "SELECT iph FROM InstalledPackageHistory iph "
        + "WHERE iph.contentServiceRequest.id = :contentServiceRequestId"),
    @NamedQuery(name = InstalledPackageHistory.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_ID, query = "SELECT iph FROM InstalledPackageHistory iph "
        + "WHERE iph.packageVersion.generalPackage.id = :packageId AND iph.resource.id = :resourceId"),
    @NamedQuery(name = InstalledPackageHistory.QUERY_FIND_BY_RESOURCE_ID, query = "SELECT iph FROM InstalledPackageHistory iph "
        + "WHERE iph.resource.id = :resourceId"),
    @NamedQuery(name = InstalledPackageHistory.QUERY_FIND_BY_ID, query = "SELECT iph FROM InstalledPackageHistory iph "
        + "WHERE iph.id = :id"),
    @NamedQuery(name = InstalledPackageHistory.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM InstalledPackageHistory iph "
        + " WHERE iph.resource.id IN ( :resourceIds ) )") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_INSTALLED_PKG_HIST_ID_SEQ")
@Table(name = "RHQ_INSTALLED_PKG_HIST")
public class InstalledPackageHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_CONFIG_BY_PACKAGE_VERSION_AND_REQ = "InstalledPackageHistory.findConfigByPackageVersionAndReq";
    public static final String QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID = "InstalledPackageHistory.findByCsrIdAndPkgVerId";
    public static final String QUERY_FIND_BY_CSR_ID = "InstalledPackageHistory.findByCsrId";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_PKG_ID = "InstalledPackageHistory.findByResourceIdAndPkgId";
    public static final String QUERY_FIND_BY_RESOURCE_ID = "InstalledPackageHistory.findByResourceId";
    public static final String QUERY_FIND_BY_ID = "InstalledPackageHistory.findById";
    public static final String QUERY_DELETE_BY_RESOURCES = "InstalledPackageHistory.deleteByResources";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "PACKAGE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private PackageVersion packageVersion;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private InstalledPackageHistoryStatus status;

    /**
     * If the status is set to FAILED, this error message will be populated with the details.
     */
    @Column(name = "ERROR_MESSAGE", nullable = true)
    private String errorMessage;

    @Column(name = "HISTORY_TIMESTAMP", nullable = true)
    private Long timestamp;

    /**
     * Values that correspond to the deployment time properties that are defined by the {@link PackageType}. This may
     * not be known or only partially populated if the package was installed on the server through some external means
     * (it depends on the plugin's ability to detect these values on discovery). This will be <code>null</code> in the
     * case that the package type does not define any deploy time properties.
     */
    @JoinColumn(name = "DEPLOYMENT_CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Configuration deploymentConfigurationValues;

    /**
     * User readable steps the plugin will perform to install the package. If specified, the UI will display these steps
     * to the user prior to executing the installation. The plugin will report on the success of each step in the
     * process as it attempts to install the package. These are optional, leaving it up to the discretion of the plugin
     * to determine how to install the package. In such a case, the plugin will simply report the success or failure of
     * the package installation.
     */
    @OneToMany(mappedBy = "installedPackageHistory", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private List<PackageInstallationStep> installationSteps;

    /**
     * This history item described a package that was on this resource.
     */
    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Resource resource;

    @JoinColumn(name = "CONTENT_SERVICE_REQUEST_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private ContentServiceRequest contentServiceRequest;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(PackageVersion packageVersion) {
        this.packageVersion = packageVersion;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public InstalledPackageHistoryStatus getStatus() {
        return status;
    }

    public void setStatus(InstalledPackageHistoryStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Configuration getDeploymentConfigurationValues() {
        return deploymentConfigurationValues;
    }

    public void setDeploymentConfigurationValues(Configuration deploymentConfigurationValues) {
        this.deploymentConfigurationValues = deploymentConfigurationValues;
    }

    public List<PackageInstallationStep> getInstallationSteps() {
        return installationSteps;
    }

    public void setInstallationSteps(List<PackageInstallationStep> installationSteps) {
        this.installationSteps = installationSteps;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public ContentServiceRequest getContentServiceRequest() {
        return contentServiceRequest;
    }

    public void setContentServiceRequest(ContentServiceRequest contentServiceRequest) {
        this.contentServiceRequest = contentServiceRequest;
    }
}