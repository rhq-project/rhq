/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.core.domain.resource;

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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.InstalledPackage;

/**
 * Describes one request to create a new resource.
 *
 * @author Jason Dobies
 */
@Entity(name = "CreateResourceHistory")
@NamedQueries({
    @NamedQuery(name = CreateResourceHistory.QUERY_FIND_WITH_STATUS, query = "SELECT crh FROM CreateResourceHistory AS crh WHERE crh.status = :status"),
    @NamedQuery(name = CreateResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, query = "" //
        + "SELECT crh " //
        + "  FROM CreateResourceHistory AS crh " //
        + " WHERE crh.parentResource.id = :id" //
        + "   AND ( crh.ctime > :startTime OR :startTime IS NULL ) " //
        + "   AND ( crh.mtime < :endTime OR :endTime IS NULL ) "),
    @NamedQuery(name = CreateResourceHistory.QUERY_FIND_BY_ID, query = "SELECT crh FROM CreateResourceHistory AS crh WHERE crh.id = :id"),
    @NamedQuery(name = CreateResourceHistory.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM CreateResourceHistory crh WHERE crh.parentResource.id IN ( :resourceIds ) )"),
    @NamedQuery(name = CreateResourceHistory.QUERY_FIND_BY_CHILD_RESOURCE_KEY, query = "" //
        + "SELECT crh " //
        + "  FROM CreateResourceHistory AS crh " //
        + " WHERE crh.parentResource.id = :parentResourceId " //
        + "   AND crh.newResourceKey = :newResourceKey ") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_CREATE_RES_HIST_ID_SEQ", sequenceName = "RHQ_CREATE_RES_HIST_ID_SEQ")
@Table(name = "RHQ_CREATE_RES_HIST")
public class CreateResourceHistory implements Serializable {

    // Constants  --------------------------------------------
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_WITH_STATUS = "CreateResourceHistory.findWithStatus";
    public static final String QUERY_FIND_BY_PARENT_RESOURCE_ID = "CreateResourceHistory.findByParentResourceId";
    public static final String QUERY_FIND_BY_ID = "CreateResourceHistory.findById";
    public static final String QUERY_DELETE_BY_RESOURCES = "CreateResourceHistory.deleteByResources";
    public static final String QUERY_FIND_BY_CHILD_RESOURCE_KEY = "CreateResourceHistory.findByChildResourceKey";

    // Attributes  --------------------------------------------

    @GeneratedValue(generator = "RHQ_CREATE_RES_HIST_ID_SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "SUBJECT_NAME", nullable = false)
    private String subjectName;

    @Column(name = "CTIME", nullable = false)
    private long ctime = System.currentTimeMillis();

    @Column(name = "MTIME", nullable = false)
    private long mtime = System.currentTimeMillis();

    /**
     * Links to the parent under which this resource is created. There will always be a parent; resources created at the
     * highest level (i.e. "servers") will have the platform resource as their parent.
     */
    @JoinColumn(name = "PARENT_RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Resource parentResource;

    /**
     * Resource instance created on the server onto which to hang these create requests.
     */
    @Column(name = "CREATED_RESOURCE_NAME", nullable = true)
    private String createdResourceName;

    /**
     * Resource key assigned to the newly created resource by the plugin if this request is successful.
     */
    @Column(name = "NEW_RESOURCE_KEY")
    private String newResourceKey;

    /**
     * Status of this create request.
     */
    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private CreateResourceStatus status;

    /**
     * Type of resource being created.
     */
    @JoinColumn(name = "RESOURCE_TYPE_ID", referencedColumnName = "ID")
    @OneToOne(fetch = FetchType.EAGER)
    private ResourceType resourceType;

    /**
     * For configuration-backed resource creation, this instance carries the values entered by the user for the newly
     * created resource. For content-backed resource creation, this instance carries any configuration values about the
     * content itself that may be specified when the content is created.
     */
    @JoinColumn(name = "CONFIGURATION_ID", referencedColumnName = "ID")
    @OneToOne(cascade = CascadeType.ALL)
    private Configuration configuration;

    /**
     * For content backed resource creation, this indicates the type of content being created as part of this request.
     */
    @JoinColumn(name = "INSTALLED_PACKAGE_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = CascadeType.PERSIST, optional = true)
    private InstalledPackage installedPackage;

    // Constructors  --------------------------------------------

    /**
     * Creates an empty instance.
     */
    public CreateResourceHistory() {
    }

    /**
     * Helper constructor when creating a request for a configuration-backed resource.
     *
     * @param parentResource        resource under which the new resource is created
     * @param resourceType          type of resource being created
     * @param subjectName           user creating the resource
     * @param resourceConfiguration the intial configuration for the resource being created
     */
    public CreateResourceHistory(Resource parentResource, ResourceType resourceType, String subjectName,
        Configuration resourceConfiguration) {
        this.parentResource = parentResource;
        this.subjectName = subjectName;
        this.resourceType = resourceType;
        this.configuration = resourceConfiguration;
    }

    /**
     * Helper constructor when creating a request for a content-backed resource.
     *
     * @param parentResource   resource under which the new resource is created
     * @param resourceType     type of resource being created
     * @param subjectName      user creating the resource
     * @param installedPackage the package being created
     */
    public CreateResourceHistory(Resource parentResource, ResourceType resourceType, String subjectName,
        InstalledPackage installedPackage) {
        this.parentResource = parentResource;
        this.subjectName = subjectName;
        this.resourceType = resourceType;
        this.installedPackage = installedPackage;
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Resource getParentResource() {
        return parentResource;
    }

    public void setParentResource(Resource parentResource) {
        this.parentResource = parentResource;
    }

    public String getCreatedResourceName() {
        return createdResourceName;
    }

    public void setCreatedResourceName(String createdResourceName) {
        this.createdResourceName = createdResourceName;
    }

    public String getNewResourceKey() {
        return newResourceKey;
    }

    public void setNewResourceKey(String newResourceKey) {
        this.newResourceKey = newResourceKey;
    }

    public CreateResourceStatus getStatus() {
        return status;
    }

    public void setStatus(CreateResourceStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Calling this method with a non-<code>null</code> error message implies that the request's status is
     * {@link CreateResourceStatus#FAILURE}. The inverse is <i>not</i> true - that is, if you set the error message to
     * <code>null</code>, the status is left as-is; it will not assume that a <code>null</code> error message means the
     * status is successful.
     *
     * @param errorMessage description of the error
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;

        if (errorMessage != null) {
            setStatus(CreateResourceStatus.FAILURE);
        }
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public long getCreatedTime() {
        return ctime;
    }

    public Date getCreatedDate() {
        return new Date(ctime);
    }

    public long getLastModifiedTime() {
        return mtime;
    }

    public Date getLastModifiedDate() {
        return new Date(mtime);
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public InstalledPackage getInstalledPackage() {
        return installedPackage;
    }

    public void setInstalledPackage(InstalledPackage installedPackage) {
        this.installedPackage = installedPackage;
    }

    /**
     * The duration of the configuration update request which simply is the difference between the
     * {@link #getCreatedTime()} and the {@link #getLastModifiedTime()}. If the request hasn't completed yet, this will
     * be the difference between the current time and the created time.
     *
     * @return the duration of time that the request took or is taking to complete
     */
    public long getDuration() {
        long start = this.ctime;
        long end = this.mtime;

        if ((status == null) || (status == CreateResourceStatus.IN_PROGRESS)) {
            end = System.currentTimeMillis();
        }

        return end - start;
    }

    // Package  --------------------------------------------

    @PrePersist
    void onPersist() {
        this.mtime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    // Object Overridden  --------------------------------------------

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = (PRIME * result) + (int) (ctime ^ (ctime >>> 32));
        result = (PRIME * result) + ((subjectName == null) ? 0 : subjectName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof CreateResourceHistory)) {
            return false;
        }

        final CreateResourceHistory other = (CreateResourceHistory) obj;

        if (ctime != other.ctime) {
            return false;
        }

        if (subjectName == null) {
            if (other.subjectName != null) {
                return false;
            }
        } else if (!subjectName.equals(other.subjectName)) {
            return false;
        }

        return true;
    }
}
