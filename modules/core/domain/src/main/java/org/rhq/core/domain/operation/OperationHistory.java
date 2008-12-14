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
package org.rhq.core.domain.operation;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
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

/**
 * The entity that represents an operation invocation that is either in progress or has completed. This is the
 * superclass to both the the individual resource history object (that encapsulates the results) and the group history
 * object (for group invocations across multiple resources).
 *
 * @author John Mazzitelli
 */

@DiscriminatorColumn(name = "DTYPE")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NamedQueries( {
    @NamedQuery(name = OperationHistory.QUERY_FIND_BY_JOB_ID, query = "SELECT h FROM OperationHistory h LEFT JOIN FETCH h.parameters "
        + " WHERE h.jobName = :jobName " + "   AND h.jobGroup = :jobGroup " + "   AND h.createdTime = :createdTime"),
    @NamedQuery(name = OperationHistory.QUERY_GET_RECENTLY_COMPLETED_RESOURCE_ADMIN, query = "SELECT DISTINCT new org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite( "
        + "    ro.id, "
        + "    ro.operationDefinition.displayName, "
        + "    ro.createdTime, "
        + "    ro.status, "
        + "    ro.resource.id, "
        + "    ro.resource.name, "
        + "    ro.resource.resourceType.name) "
        + " FROM ResourceOperationHistory ro " + " WHERE ro.status != 'INPROGRESS' "),
    @NamedQuery(name = OperationHistory.QUERY_GET_RECENTLY_COMPLETED_RESOURCE, query = "SELECT DISTINCT new org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite( "
        + "    ro.id, "
        + "    ro.operationDefinition.displayName, "
        + "    ro.createdTime, "
        + "    ro.status, "
        + "    ro.resource.id, "
        + "    ro.resource.name, "
        + "    ro.resource.resourceType.name) "
        + " FROM ResourceOperationHistory ro JOIN ro.resource.implicitGroups g JOIN g.roles r JOIN r.subjects s "
        + " WHERE ro.status != 'INPROGRESS' AND s = :subject "),
    @NamedQuery(name = OperationHistory.QUERY_GET_RECENTLY_COMPLETED_GROUP_ADMIN, query = "SELECT DISTINCT new org.rhq.core.domain.operation.composite.GroupOperationLastCompletedComposite( "
        + "    go.id, "
        + "    go.operationDefinition.displayName, "
        + "    go.createdTime, "
        + "    go.status, "
        + "    go.group.id, "
        + "    go.group.name, "
        + "    go.group.resourceType.name) "
        + " FROM GroupOperationHistory go " + " WHERE go.status != 'INPROGRESS' "),
    @NamedQuery(name = OperationHistory.QUERY_GET_RECENTLY_COMPLETED_GROUP, query = "SELECT DISTINCT new org.rhq.core.domain.operation.composite.GroupOperationLastCompletedComposite( "
        + "    go.id, "
        + "    go.operationDefinition.displayName, "
        + "    go.createdTime, "
        + "    go.status, "
        + "    go.group.id, "
        + "    go.group.name, "
        + "    go.group.resourceType.name) "
        + " FROM GroupOperationHistory go JOIN go.group g JOIN g.roles r JOIN r.subjects s "
        + " WHERE go.status != 'INPROGRESS' AND s = :subject "),
    @NamedQuery(name = OperationHistory.QUERY_GET_PARAMETER_CONFIGURATION_IDS, query = "" //
        + "SELECT params.id " //
        + "  FROM ResourceOperationHistory roh " //
        + "  JOIN roh.parameters params " //
        + " WHERE roh.id = :historyId"), //
    @NamedQuery(name = OperationHistory.QUERY_GET_RESULT_CONFIGURATION_IDS, query = "" //
        + "SELECT res.id " //
        + "  FROM ResourceOperationHistory roh " //
        + "  JOIN roh.results res " //
        + " WHERE roh.id = :historyId"), //
    @NamedQuery(name = OperationHistory.QUERY_DELETE_BY_HISTORY_IDS, query = "" //
        + "DELETE FROM ResourceOperationHistory roh " //
        + " WHERE roh.id = :historyId ") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_OPERATION_HISTORY_ID_SEQ")
@Table(name = "RHQ_OPERATION_HISTORY")
public abstract class OperationHistory implements Serializable {
    public static final String QUERY_FIND_BY_JOB_ID = "OperationHistory.findByJobId";

    public static final String QUERY_GET_RECENTLY_COMPLETED_RESOURCE = "OperationHistory.getRecentlyCompletedResource";
    public static final String QUERY_GET_RECENTLY_COMPLETED_RESOURCE_ADMIN = "OperationHistory.getRecentlyCompletedResource_admin";
    public static final String QUERY_GET_RECENTLY_COMPLETED_GROUP = "OperationHistory.getRecentlyCompletedGroup";
    public static final String QUERY_GET_RECENTLY_COMPLETED_GROUP_ADMIN = "OperationHistory.getRecentlyCompletedGroup_admin";

    public static final String QUERY_GET_PARAMETER_CONFIGURATION_IDS = "OperationHistory.getParameterConfigurationIds";
    public static final String QUERY_GET_RESULT_CONFIGURATION_IDS = "OperationHistory.getResultConfigurationIds";
    public static final String QUERY_DELETE_BY_HISTORY_IDS = "OperationHistory.deleteByHistoryIds";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID")
    @GeneratedValue(generator = "SEQ", strategy = GenerationType.SEQUENCE)
    @Id
    private int id;

    @Column(name = "JOB_NAME", nullable = false)
    private String jobName;

    @Column(name = "JOB_GROUP", nullable = false)
    private String jobGroup;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationRequestStatus status;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "SUBJECT_NAME", nullable = false)
    private String subjectName;

    @Column(name = "CTIME", nullable = false)
    private long createdTime = System.currentTimeMillis();

    @Column(name = "STIME", nullable = false)
    private long startedTime;

    @Column(name = "MTIME", nullable = false)
    private long modifiedTime = System.currentTimeMillis();

    @JoinColumn(name = "OPERATION_DEF_ID", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER)
    private OperationDefinition operationDefinition;

    @JoinColumn(name = "PARAMETERS_CONFIG_ID", referencedColumnName = "ID")
    @OneToOne(cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY)
    private Configuration parameters;

    protected OperationHistory() {
    }

    protected OperationHistory(String jobName, String jobGroup, String subjectName,
        OperationDefinition operationDefinition, Configuration parameters) {
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.subjectName = subjectName;
        this.operationDefinition = operationDefinition;
        this.parameters = parameters;
        this.status = OperationRequestStatus.INPROGRESS;
    }

    /**
     * The single job ID which identifies this specific history item. It can later be parsed via
     * {@link HistoryJobId#HistoryJobId(String)}.
     *
     * @return job ID
     */
    public HistoryJobId getJobId() {
        return new HistoryJobId(this.jobName, this.jobGroup, this.createdTime);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJobName() {
        return this.jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return this.jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public OperationDefinition getOperationDefinition() {
        return operationDefinition;
    }

    public void setOperationDefinition(OperationDefinition operationDefinition) {
        this.operationDefinition = operationDefinition;
    }

    public Configuration getParameters() {
        return parameters;
    }

    public void setParameters(Configuration parameters) {
        this.parameters = parameters;
    }

    /**
     * The status of the operation which indicates that the invocation is either still in progress, or it has completed
     * and either succeeded or failed. This may return <code>null</code> if the job hasn't even been triggered yet.
     *
     * @return the request status
     */
    public OperationRequestStatus getStatus() {
        return status;
    }

    public void setStatus(OperationRequestStatus status) {
        if (status == null) {
            throw new NullPointerException("status==null");
        }

        this.status = status;
    }

    /**
     * If not <code>null</code>, this is an error message (possibly a full stack trace) to indicate the overall error
     * that occurred when the operation failed. This will normally be <code>null</code> unless the
     * {@link #getStatus() status} indicates a {@link OperationRequestStatus#FAILURE}.
     *
     * @return overall error that occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Calling this method with a non-<code>null</code> error message implies that the request's status is
     * {@link OperationRequestStatus#FAILURE}. The inverse is <i>not</i> true - that is, if you set the error message to
     * <code>null</code>, the status is left as-is; it will not assume that a <code>null</code> error message means the
     * status is successful.
     *
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;

        if (this.errorMessage != null) {
            setStatus(OperationRequestStatus.FAILURE);
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
     * For auditing purposes, this method tells you the username of the person that invoked the operation. This is not a
     * relationship to an actual Subject because we want to maintain the audit trail, even if a Subject has been deleted
     * from the database.
     *
     * @return the actual name string of the submitter of the operation invocation
     */
    public String getSubjectName() {
        return subjectName;
    }

    /**
     * The time this entity was originally created.  This may, but will not necessarily, be similar or nearly identical 
     * to the {@link #getStartedTime()} value.  If this is an operation on a single resource, then these two figures
     * will be very similar because resource operations are started immediately after the history element is created
     * for them.  If this is an operation on a resource group, especially a large group, then these two figures might
     * diverge considerably because all history elements are created up front, and then the process begins executing
     * each in turn.
     *
     * @return creation time, in epoch milliseconds
     * @see #getStartedTime()
     */
    public long getCreatedTime() {
        return this.createdTime;
    }

    /**
     * The time this entity was last modified. This is the last time the status was updated. If the status has never
     * been updated, this will be the {@link #getCreatedTime() created time}.
     *
     * @return last modified time, in epoch milliseconds
     */
    public long getModifiedTime() {
        return this.modifiedTime;
    }

    /**
     * This method MUST be called when the corresponding operation is triggered, but before the request is sent down to
     * the agent.  The started time is used in the calculation of {@link #getDuration()}, which is in turn used by the
     * business layer to reason whether an operation has timed out.  If this method is never called, and if there are 
     * any issues executing the corresponding operation, this history element will never time out and will forever stay
     * in the {@link OperationRequestStatus#INPROGRESS} state. 
     * 
     * @throws IllegalArgumentException if an attempt is made to start this object more than once 
     * @see #getCreatedTime()
     */
    public void setStartedTime() {
        if (this.startedTime != 0) {
            throw new IllegalArgumentException("Can only start an operation once");
        }
        this.startedTime = System.currentTimeMillis();
    }

    /**
     * The time when corresponding operation was started.  If the corresponding operation has not yet been started, 
     * this method will return 0.
     * 
     * @return started time, in epoch millis
     */
    public long getStartedTime() {
        return this.startedTime;
    }

    /**
     * The duration of the operation invocation which simply is the difference between the {@link #getCreatedTime()} and
     * the {@link #getModifiedTime()}. If the operation hasn't completed yet, this will be the difference between the
     * current time and the created time.
     *
     * @return the duration of time that the operation took or is taking to complete
     */
    public long getDuration() {
        // by definition, the duration is 0 if the corresponding operation hasn't begun
        if (this.startedTime == 0) {
            return 0;
        }

        long start = this.startedTime;
        long end = this.modifiedTime;

        if ((status == null) || (status == OperationRequestStatus.INPROGRESS)) {
            end = System.currentTimeMillis();
        }

        return end - start;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("id=[" + this.id);
        buf.append("], job-name=[" + this.jobName);
        buf.append("], job-group=[" + this.jobGroup);
        buf.append("], status=[" + this.status);
        buf.append("], subject-name=[" + this.subjectName);

        // there may be some operations whose parameters are sensitive values, like passwords
        // do not show them by default - but allow testers to see them via a system property
        if (Boolean.getBoolean("rhq.test.operation.show-values")) {
            buf.append("], parameters=[" + this.parameters);
        }

        buf.append("], ctime=[" + new java.util.Date(this.createdTime));
        buf.append("], mtime=[" + new java.util.Date(this.modifiedTime));
        buf.append("], duration-millis=[" + getDuration());

        String err = this.errorMessage;
        if ((err != null) && (err.indexOf('\n') > -1)) {
            err = err.substring(0, err.indexOf('\n')) + "...";
        }

        buf.append("], error-message=[" + err);

        return buf.toString();
    }

    @Override
    public int hashCode() {
        return getJobId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof OperationHistory)) {
            return false;
        }

        final OperationHistory other = (OperationHistory) obj;

        return this.getJobId().equals(other.getJobId());
    }

    @PrePersist
    void onPersist() {
        this.modifiedTime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.modifiedTime = System.currentTimeMillis();
    }
}