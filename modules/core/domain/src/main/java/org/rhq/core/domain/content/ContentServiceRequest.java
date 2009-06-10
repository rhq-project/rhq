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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
import javax.persistence.OneToMany;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Resource;

/**
 * Represents a user initiated request against an agent's content subsystem. This instance will be used to convey both
 * the request parameters as well as any response values that are provided from the operation. Additionally, the state
 * in this instance will indicate success or failure of the request. This object represents both the status of currently
 * executing requests as well as providing a history of all previous requests for conetent subsystem operations.
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = ContentServiceRequest.QUERY_FIND_WITH_STATUS, query = "SELECT csr FROM ContentServiceRequest AS csr WHERE csr.status = :status"),
    @NamedQuery(name = ContentServiceRequest.QUERY_FIND_BY_RESOURCE_WITH_STATUS, query = "SELECT csr FROM ContentServiceRequest AS csr WHERE csr.resource.id = :resourceId AND csr.status = :status"),
    @NamedQuery(name = ContentServiceRequest.QUERY_FIND_BY_RESOURCE_WITH_NOT_STATUS, query = "SELECT csr FROM ContentServiceRequest AS csr WHERE csr.resource.id = :resourceId AND csr.status <> :status"),
    @NamedQuery(name = ContentServiceRequest.QUERY_FIND_BY_RESOURCE, query = "SELECT csr FROM ContentServiceRequest AS csr WHERE csr.resource.id = :resourceId"),

    @NamedQuery(name = ContentServiceRequest.QUERY_FIND_BY_ID, query = "SELECT csr "
        + "  FROM ContentServiceRequest AS csr WHERE csr.id = :id"),

    @NamedQuery(name = ContentServiceRequest.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM ContentServiceRequest csr WHERE csr.resource IN (:resources))") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONTENT_REQ_ID_SEQ")
@Table(name = "RHQ_CONTENT_REQ")
public class ContentServiceRequest implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_WITH_STATUS = "ContentServiceRequest.findWithStatus";
    public static final String QUERY_FIND_BY_RESOURCE_WITH_STATUS = "ContentServiceRequest.findByResourceWithStatus";
    public static final String QUERY_FIND_BY_RESOURCE_WITH_NOT_STATUS = "ContentServiceRequest.findByResourceWithNotStatus";
    public static final String QUERY_FIND_BY_RESOURCE = "ContentServiceRequest.findByResource";
    public static final String QUERY_FIND_BY_ID = "ContentServiceRequest.findById";
    public static final String QUERY_DELETE_BY_RESOURCES = "ContentServiceRequest.deleteByResources";

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Resource resource;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContentRequestStatus status = ContentRequestStatus.IN_PROGRESS;

    @Column(name = "ERROR_MESSAGE", nullable = true)
    private String errorMessage;

    @Column(name = "NOTES", nullable = true)
    private String notes;

    @Column(name = "SUBJECT_NAME", nullable = false)
    private String subjectName;

    @Column(name = "CTIME", nullable = false)
    private long ctime = System.currentTimeMillis();

    @Column(name = "MTIME", nullable = false)
    private long mtime = System.currentTimeMillis();

    @Column(name = "REQUEST_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContentRequestType contentRequestType;

    @OneToMany(mappedBy = "contentServiceRequest", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<InstalledPackageHistory> installedPackageHistory;

    // Constructors  --------------------------------------------

    public ContentServiceRequest() {
    }

    public ContentServiceRequest(Resource resource, String subjectName, ContentRequestType contentRequestType) {
        this.resource = resource;
        this.subjectName = subjectName;
        this.contentRequestType = contentRequestType;
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public ContentRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ContentRequestStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }

        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public long getCtime() {
        return ctime;
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

    /**
     * Describes the type of service being tracked by this request.
     */
    public ContentRequestType getContentRequestType() {
        return contentRequestType;
    }

    public void setContentRequestType(ContentRequestType contentRequestType) {
        this.contentRequestType = contentRequestType;
    }

    /**
     * Conveys which packages are being manipulated in this request.
     */
    public Set<InstalledPackageHistory> getInstalledPackageHistory() {
        return installedPackageHistory;
    }

    public void addInstalledPackageHistory(InstalledPackageHistory installedPackage) {
        if (this.installedPackageHistory == null) {
            this.installedPackageHistory = new HashSet<InstalledPackageHistory>();
        }

        this.installedPackageHistory.add(installedPackage);
    }

    public void setInstalledPackageHistory(Set<InstalledPackageHistory> installedPackageHistory) {
        this.installedPackageHistory = installedPackageHistory;
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

        if ((status == null) || (status == ContentRequestStatus.IN_PROGRESS)) {
            end = System.currentTimeMillis();
        }

        return end - start;
    }

    // Package  --------------------------------------------

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

        if ((obj == null) || !(obj instanceof ContentServiceRequest)) {
            return false;
        }

        final ContentServiceRequest other = (ContentServiceRequest) obj;

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