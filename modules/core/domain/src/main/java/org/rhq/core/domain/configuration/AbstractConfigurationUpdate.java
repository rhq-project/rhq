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
package org.rhq.core.domain.configuration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@MappedSuperclass
/**
 * Base class for resource configuration and plugin configuration updates (i.e. history items).
 */
public abstract class AbstractConfigurationUpdate {
    @JoinColumn(name = "CONFIGURATION_ID", referencedColumnName = "ID", nullable = false)
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    protected Configuration configuration;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    protected ConfigurationUpdateStatus status;

    @Column(name = "ERROR_MESSAGE")
    protected String errorMessage;

    @Column(name = "SUBJECT_NAME")
    protected String subjectName;

    @Column(name = "CTIME", nullable = false)
    protected long createdTime = System.currentTimeMillis();

    @Column(name = "MTIME", nullable = false)
    protected long modifiedTime = System.currentTimeMillis();

    protected AbstractConfigurationUpdate()
    {
    }

    protected AbstractConfigurationUpdate(String subjectName)
    {
        this.subjectName = subjectName;
        this.status = ConfigurationUpdateStatus.INPROGRESS;
    }

    /**
     * Contains the entire configuration that was submitted as the new configuration for a resource. If the update
     * request failed, this configuration will contain properties whose error messages are non-null - this can be used
     * to find out which properties were invalid and why.
     *
     * @return the new configuration for the updated resource
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * The status of the request which indicates that the request is either still in progress, or it has completed and
     * either succeeded or failed.
     *
     * @return the request status
     */
    public ConfigurationUpdateStatus getStatus() {
        return status;
    }

    public void setStatus(ConfigurationUpdateStatus status) {
        if (status == null) {
            throw new NullPointerException("status==null");
        }

        this.status = status;
    }

    /**
     * If not <code>null</code>, this is an error message (possibly a full stack trace) to indicate the overall error
     * that occurred when the configuration update failed. This will normally be <code>null</code> unless the
     * {@link #getStatus() status} indicates a {@link ConfigurationUpdateStatus#FAILURE}.
     *
     * <p>If the update failed because one or more configuration property values were invalid, then you can also examine
     * the error messages in each of the failed properties to find out more specific details about the errors for each
     * invalid property.</p>
     *
     * @return overall error that occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Calling this method with a non-<code>null</code> error message implies that the request's status is
     * {@link ConfigurationUpdateStatus#FAILURE}. The inverse is <i>not</i> true - that is, if you set the error message
     * to <code>null</code>, the status is left as-is; it will not assume that a <code>null</code> error message means
     * the status is successful.
     *
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;

        if (this.errorMessage != null) {
            setStatus(ConfigurationUpdateStatus.FAILURE);
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

    /**
     * For auditing purposes, this method tells you the username of the person that created the request. This is not a
     * relationship to an actual Subject because we want to maintain the audit trail, even if a Subject has been deleted
     * from the database.
     *
     * <p>If the configuration update was automatically detected by the plugin (that is, was not initiated by a JBoss ON
     * user; for example, someone modified a configuration file on disk), this will return <code>null</code> as an
     * indication that we don't know who modified it</p>
     *
     * @return the actual name string of the submitter of the request
     */
    public String getSubjectName() {
        return subjectName;
    }

    /**
     * The time this entity was originally created; in other words, when the request was originally made.
     *
     * @return creation time
     */
    public long getCreatedTime() {
        return this.createdTime;
    }

    /**
     * The time this entity was last modified. This is the last time the status was updated. If the status has never
     * been updated, this will be the {@link #getCreatedTime() created time}.
     *
     * @return last modified time
     */
    public long getModifiedTime() {
        return this.modifiedTime;
    }

    /**
     * The duration of the configuration update request which simply is the difference between the
     * {@link #getCreatedTime()} and the {@link #getModifiedTime()}. If the request hasn't completed yet, this will be
     * the difference between the current time and the created time.
     *
     * @return the duration of time that the request took or is taking to complete
     */
    public long getDuration() {
        long start = this.createdTime;
        long end;
        if ((status == null) || (status == ConfigurationUpdateStatus.INPROGRESS)) {
            end = System.currentTimeMillis();
        } else {
            end = this.modifiedTime;
        }

        return end - start;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = (PRIME * result) + (int) (createdTime ^ (createdTime >>> 32));
        result = (PRIME * result) + ((subjectName == null) ? 0 : subjectName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof AbstractConfigurationUpdate)) {
            return false;
        }

        final AbstractConfigurationUpdate other = (AbstractConfigurationUpdate) obj;

        if (this.createdTime != other.createdTime) {
            return false;
        }

        if (this.subjectName == null) {
            if (other.subjectName != null) {
                return false;
            }
        } else if (!this.subjectName.equals(other.subjectName)) {
            return false;
        }

        return true;
    }

    @PrePersist
    void onPersist() {
        // don't set createdTime - we use it in equals/hashCode - it is already set at instantiation time
        this.modifiedTime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.modifiedTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getClass().getSimpleName());
        str.append("[status=").append(this.status);
        str.append(", configuration=").append(this.configuration);
        str.append(", subjectName=").append(this.subjectName);
        str.append(", createdTime=").append(this.createdTime);
        str.append(", modifiedTime=").append(this.modifiedTime);

        String err = this.errorMessage;
        if ((err != null) && (err.indexOf('\n') > -1)) {
            err = err.substring(0, err.indexOf('\n')) + "...";
        }

        str.append(", errorMessage=").append(err);

        appendToStringInternals(str); // ask subclasses if they have anything else to add

        str.append(']');

        return str.toString();
    }

    /**
     * Subclasses can override this to add things it wants to see in the toString.
     *
     * @param str the builder to append strings to
     */
    protected void appendToStringInternals(StringBuilder str) {
        return;
    }
}