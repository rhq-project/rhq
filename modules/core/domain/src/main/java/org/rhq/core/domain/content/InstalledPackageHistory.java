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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Date;
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
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.rhq.core.domain.configuration.Configuration;

/**
 * Audit trail entity for tracking the results of an individual package request made on a resource. Once created, these
 * objects are effectively immutable; if the state of a package request changes a new entity will be created.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = InstalledPackageHistory.QUERY_FIND_CONFIG_BY_PACKAGE_VERSION_AND_REQ, query = "SELECT dcv FROM InstalledPackageHistory iph JOIN iph.deploymentConfigurationValues dcv "
        + "WHERE iph.packageVersion = :packageVersion "
        + "AND iph.contentServiceRequest = :contentServiceRequest "
        + "ORDER BY iph.timestamp DESC"),
    @NamedQuery(name = InstalledPackageHistory.QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID, query = "SELECT iph FROM InstalledPackageHistory iph "
        + "WHERE iph.contentServiceRequest.id = :contentServiceRequestId "
        + "AND iph.packageVersion.id = :packageVersionId " + "ORDER BY timestamp DESC") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_INSTALLED_PKG_HIST_ID_SEQ")
@Table(name = "RHQ_INSTALLED_PKG_HIST")
public class InstalledPackageHistory implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_CONFIG_BY_PACKAGE_VERSION_AND_REQ = "InstalledPackageHistory.findConfigByPackageVersionAndReq";
    public static final String QUERY_FIND_BY_CSR_ID_AND_PKG_VER_ID = "InstalledPackageHistory.findByCsrIdAndPkgVerId";

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
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
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    /**
     * Values that correspond to the deployment time properties that are defined by the {@link PackageType}. This may
     * not be known or only partially populated if the package was installed on the server through some external means
     * (it depends on the plugin's ability to detect these values on discovery). This will be <code>null</code> in the
     * case that the package type does not define any deploy time properties.
     */
    @JoinColumn(name = "DEPLOYMENT_CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Configuration deploymentConfigurationValues;

    @JoinColumn(name = "CONTENT_SERVICE_REQUEST_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private ContentServiceRequest contentServiceRequest;

    // Public  --------------------------------------------

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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
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

    /**
     * Convienence method that sets the error message to the given throwable's stack trace dump. If the given throwable
     * is <code>null</code>, the error message will be set to <code>null</code> as if passing <code>null</code> to
     * {@link #setErrorMessage(String)}.
     *
     * @param t throwable whose message and stack trace will make up the error message (may be <code>null</code>)
     */
    public void setErrorMessageFromThrowable(Throwable t) {
        if (t != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            setErrorMessage(baos.toString());
        } else {
            setErrorMessage(null);
        }
    }

    public Configuration getDeploymentConfigurationValues() {
        return deploymentConfigurationValues;
    }

    public void setDeploymentConfigurationValues(Configuration deploymentConfigurationValues) {
        this.deploymentConfigurationValues = deploymentConfigurationValues;
    }

    public ContentServiceRequest getContentServiceRequest() {
        return contentServiceRequest;
    }

    public void setContentServiceRequest(ContentServiceRequest contentServiceRequest) {
        this.contentServiceRequest = contentServiceRequest;
    }
}