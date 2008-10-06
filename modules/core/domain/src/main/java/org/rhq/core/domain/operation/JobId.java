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

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Simple object that encapsulates the individual pieces of data that make up an operation job ID.
 *
 * <p>This class has some JPA annotations to allow it to be an entity ID specified via @IdClass.</p>
 *
 * @author John Mazzitelli
 */
@MappedSuperclass
public class JobId implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String JOB_ID_SEPARATOR = "_=_";

    @Column(name = "JOB_NAME", nullable = false)
    private String jobName;

    @Column(name = "JOB_GROUP", nullable = false)
    private String jobGroup;

    public JobId(String jobName, String jobGroup) {
        this.jobName = jobName;
        this.jobGroup = jobGroup;
    }

    public JobId(String jobIdString) {
        this(splitJobIdStringIntoParts(jobIdString));
    }

    protected JobId(String[] jobIdParts) {
        this(jobIdParts[0], jobIdParts[1]);
    }

    /**
     * The job name makes the job unique among all other jobs in the system.
     *
     * @return job name string
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * This class is designed as a read-only immutable, however, this protected method will allow you to subclass it to
     * make it mutable.
     *
     * @param jobName
     */
    protected void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * The job group allows jobs to be grouped across an individual resource of group. The job group string is unique to
     * a resource or group.
     *
     * @return the job's group string
     */
    public String getJobGroup() {
        return jobGroup;
    }

    /**
     * This class is designed as a read-only immutable, however, this protected method will allow you to subclass it to
     * make it mutable.
     *
     * @param jobGroup
     */
    protected void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    /**
     * Returns the single string that identifies the unique job ID.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return joinPartsIntoJobIdString(jobName, jobGroup);
    }

    /**
     * Returns the hash code of the string that is returned by {@link #toString()}.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Compares the strings returned by {@link #toString()}, assuming <code>obj</code> is another {@link JobId}.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof JobId)) {
            return false;
        }

        return this.toString().equals(obj.toString());
    }

    protected static String[] splitJobIdStringIntoParts(String jobId) {
        return jobId.split(JOB_ID_SEPARATOR);
    }

    protected static String joinPartsIntoJobIdString(String... parts) {
        String jobIdString = parts[0];

        for (int i = 1; i < parts.length; i++) {
            jobIdString += JOB_ID_SEPARATOR + parts[i];
        }

        return jobIdString;
    }
}