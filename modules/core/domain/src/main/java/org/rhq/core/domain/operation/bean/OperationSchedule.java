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
package org.rhq.core.domain.operation.bean;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.HistoryJobId;
import org.rhq.core.domain.operation.JobId;

/**
 * A simple non-entity POJO that describes a particular scheduled operation.
 *
 * @author John Mazzitelli
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class OperationSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    private String jobName;
    private String jobGroup;
    private String operationName;
    private String operationDisplayName;
    private Configuration parameters;
    private Subject subject;
    private String description;

    public OperationSchedule() {
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getOperationDisplayName() {
        /*
         * this conditional is for "legacy" purposes, so that pre-existing operation schedules don't return blank
         * displayNames, they will default to returning the operationDefinition.name instead
         */
        if ((operationDisplayName == null) || operationDisplayName.equals("")) {
            return getOperationName();
        }

        return operationDisplayName;
    }

    public void setOperationDisplayName(String operationDisplayName) {
        this.operationDisplayName = operationDisplayName;
    }

    /**
     * Returns the option parameters that are passed to the operation invocation.
     *
     * @return the parameters, or <code>null</code> if no parameters are to be passed
     */
    public Configuration getParameters() {
        return parameters;
    }

    public void setParameters(Configuration parameters) {
        this.parameters = parameters;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        if (subject == null) {
            throw new NullPointerException("Cannot schedule an operation without a user associated with it");
        }

        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The single job ID which identifies the operation. It can later be parsed via {@link JobId#JobId(String)}. Note
     * that this job ID only identifies the scheduled operation; it does not identify any specific invocation of that
     * operation (see {@link HistoryJobId} for that).
     *
     * @return job ID
     */
    @XmlElement
    public JobId getJobId() {
        return new JobId(this.jobName, this.jobGroup);
    }

    /* 
     * there may be some operations whose parameters are sensitive values, such as passwords - do not show them
     * as part of the toString.  they can still be gotten by explicitly calling getParameters()
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("job-name=[" + this.jobName);
        str.append("], job-group=[" + this.jobGroup);
        str.append("], operation-name=[" + this.operationName);
        str.append("], subject=[" + this.subject);
        str.append("], description=[" + this.description);
        str.append("]");
        return str.toString();
    }
}