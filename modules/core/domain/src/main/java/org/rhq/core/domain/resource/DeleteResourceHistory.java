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
package org.rhq.core.domain.resource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Describes one requests that a resource be deleted. The actual resource will be deleted by the plugin.
 *
 * @author Jason Dobies
 */
@Entity(name = "DeleteResourceHistory")
@NamedQueries( {
    @NamedQuery(name = DeleteResourceHistory.QUERY_FIND_WITH_STATUS, query = "SELECT drh from DeleteResourceHistory AS drh where drh.status = :status"),
    @NamedQuery(name = DeleteResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, query = "SELECT drh from DeleteResourceHistory AS drh WHERE drh.resource.parentResource.id = :id"),
    @NamedQuery(name = DeleteResourceHistory.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM DeleteResourceHistory drh WHERE drh.resource IN (:resources))") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DELETE_RES_HIST_ID_SEQ")
@Table(name = "RHQ_DELETE_RES_HIST")
public class DeleteResourceHistory {
    // Constants  --------------------------------------------

    public static final String QUERY_FIND_WITH_STATUS = "DeleteResourceHistory.findWithStatus";
    public static final String QUERY_FIND_BY_PARENT_RESOURCE_ID = "DeleteResourceHistory.findByParentResourceId";
    public static final String QUERY_DELETE_BY_RESOURCES = "DeleteResourceHistory.deleteByResources";

    // Attributes  --------------------------------------------

    @GeneratedValue(generator = "SEQ", strategy = GenerationType.SEQUENCE)
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

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeleteResourceStatus status;

    // Parameters for plugin call ----------

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Resource resource;

    // Constructors  --------------------------------------------

    public DeleteResourceHistory() {
    }

    /**
     * Creates a new history instance representing a request to delete the specified resource.
     *
     * @param resource being deleted
     */
    public DeleteResourceHistory(Resource resource, String subjectName) {
        this.resource = resource;
        this.subjectName = subjectName;
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
            setStatus(DeleteResourceStatus.FAILURE);
        }
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

    public DeleteResourceStatus getStatus() {
        return status;
    }

    public void setStatus(DeleteResourceStatus status) {
        this.status = status;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
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

        if ((status == null) || (status == DeleteResourceStatus.IN_PROGRESS)) {
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

        if ((obj == null) || !(obj instanceof DeleteResourceHistory)) {
            return false;
        }

        final DeleteResourceHistory other = (DeleteResourceHistory) obj;

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