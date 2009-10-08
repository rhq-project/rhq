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

/**
 * Simple object that encapsulates the individual pieces of data that make up an individual invocation of an operation
 * job ID.
 *
 * @author John Mazzitelli
 */
public class HistoryJobId extends JobId {
    private static final long serialVersionUID = 1L;

    private final long createdTime;

    public HistoryJobId(String jobName, String jobGroup, long createdTime) {
        super(jobName, jobGroup);
        this.createdTime = createdTime;
    }

    public HistoryJobId(String jobIdString) {
        this(splitJobIdStringIntoParts(jobIdString));
    }

    protected HistoryJobId(String[] jobIdParts) {
        this(jobIdParts[0], jobIdParts[1], Long.parseLong(jobIdParts[2]));
    }

    public long getCreatedTime() {
        return createdTime;
    }

    /**
     * Returns the single string that contains the unique job ID which identifies a particular invocation of a
     * particular job. This string is used to determine this object's hash code and for equality checks. Note that
     * {@link HistoryJobId} objects are only ever equal to other {@link HistoryJobId} objects (they are never equal to
     * concrete instances of {@link JobId}).
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return joinPartsIntoJobIdString(super.toString(), Long.toString(createdTime));
    }
}