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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * The entity that represents an operation schedule as persisted in the database. This is the superclass to both the
 * individual resource schedule entity and the group schedule entity.
 *
 * <p>The purpose of this entity is simply to track schedules so we can query jobs with their associated
 * resources/groups.</p>
 *
 * @author John Mazzitelli
 */
@DiscriminatorColumn(name = "DTYPE")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NamedQueries( {
    @NamedQuery(name = OperationScheduleEntity.QUERY_FIND_BY_JOB_ID, query = "" //
        + "SELECT s " //
        + "  FROM OperationScheduleEntity s " //
        + " WHERE s.jobName = :jobName " //
        + "   AND s.jobGroup = :jobGroup "),
    @NamedQuery(name = OperationScheduleEntity.QUERY_GET_SCHEDULE_RESOURCE_ADMIN, query = "" //
        + "SELECT DISTINCT new org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite( "
        + "    ro.id, " //
        + "    ro.jobName, " //
        + "    ro.jobGroup, " //
        + "    '', " //
        + "    ro.nextFireTime, " //
        + "    ro.resource.id, " //
        + "    ro.resource.resourceType.id, " //        
        + "    ro.resource.name, " //
        + "    ro.resource.ancestry) " //
        + "  FROM ResourceOperationScheduleEntity ro " //
        + " WHERE ro.nextFireTime IS NOT NULL "), //
    @NamedQuery(name = OperationScheduleEntity.QUERY_GET_SCHEDULE_RESOURCE, query = "" //
        + "SELECT DISTINCT new org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite( "
        + "    ro.id, " //
        + "    ro.jobName, " //
        + "    ro.jobGroup, " //
        + "    '', " //
        + "    ro.nextFireTime, " //
        + "    ro.resource.id, " //
        + "    ro.resource.resourceType.id, " //        
        + "    ro.resource.name, " //
        + "    ro.resource.ancestry) " //
        + "  FROM ResourceOperationScheduleEntity ro " //
        + "  JOIN ro.resource.implicitGroups g " //
        + "  JOIN g.roles r " //
        + "  JOIN r.subjects s " //
        + " WHERE ro.nextFireTime IS NOT NULL " //
        + "   AND s = :subject "), //
    @NamedQuery(name = OperationScheduleEntity.QUERY_GET_SCHEDULE_GROUP_ADMIN, query = "" //
        + "SELECT DISTINCT new org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite( "
        + "    go.id, " //
        + "    go.jobName, " //
        + "    go.jobGroup, " //
        + "    '', " //
        + "    go.nextFireTime, " //
        + "    go.group.id, " //
        + "    go.group.name, " //
        + "    go.group.resourceType.name, "
        + "    go.group.autoGroupParentResource.id) " //
        + "  FROM GroupOperationScheduleEntity go " //
        + " WHERE go.nextFireTime IS NOT NULL "), //
    @NamedQuery(name = OperationScheduleEntity.QUERY_GET_SCHEDULE_GROUP, query = "" //
        + "SELECT DISTINCT new org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite( "
        + "    go.id, " //
        + "    go.jobName, " //
        + "    go.jobGroup, " //
        + "    '', " //
        + "    go.nextFireTime, " //
        + "    g.id, " //
        + "    g.name, " //
        + "    g.resourceType.name,"
        + "    g.autoGroupParentResource.id) " //
        + "  FROM GroupOperationScheduleEntity go " //
        + "  JOIN go.group g " //
        + "  JOIN g.roles r " //
        + "  JOIN r.subjects s " //
        + " WHERE go.nextFireTime IS NOT NULL " //
        + "   AND s = :subject ") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_OPERATION_SCHEDULE_ID_SEQ", sequenceName = "RHQ_OPERATION_SCHEDULE_ID_SEQ")
@Table(name = "RHQ_OPERATION_SCHEDULE")
public abstract class OperationScheduleEntity implements Serializable {

    public static final String QUERY_FIND_BY_JOB_ID = "OperationScheduleEntity.findByJobId";
    public static final String QUERY_GET_SCHEDULE_RESOURCE = "OperationScheduleEntity.getScheduleResource";
    public static final String QUERY_GET_SCHEDULE_RESOURCE_ADMIN = "OperationScheduleEntity.getScheduleResource_admin";
    public static final String QUERY_GET_SCHEDULE_GROUP = "OperationScheduleEntity.getScheduleGroup";
    public static final String QUERY_GET_SCHEDULE_GROUP_ADMIN = "OperationScheduleEntity.getScheduleGroup_admin";

    private static final long serialVersionUID = 2L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_OPERATION_SCHEDULE_ID_SEQ")
    @Id
    private int id;

    @Column(name = "JOB_NAME", nullable = false)
    private String jobName;

    @Column(name = "JOB_GROUP", nullable = false)
    private String jobGroup;

    @Column(name = "NEXT_FIRE_TIME", nullable = true)
    private Long nextFireTime;

    protected OperationScheduleEntity() {
    }

    protected OperationScheduleEntity(String jobName, String jobGroup, Date nextFireTime) {
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.nextFireTime = (nextFireTime == null) ? null : nextFireTime.getTime();
    }

    public int getId() {
        return id;
    }

    /**
     * The single job ID which identifies this specific schedule. It can later be parsed via
     * {@link ScheduleJobId#ScheduleJobId(String)}.
     *
     * @return job ID
     */
    public JobId getJobId() {
        return new ScheduleJobId(this.jobName, this.jobGroup);
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

    /**
     * The next time this schedule is going to fire. If <code>null</code>, it won't fire again. Usually, when a next
     * fire time is <code>null</code>, this entity is going to be deleted shortly.
     *
     * @return next scheduled trigger time, in epoch milliseconds
     */
    public Long setNextFireTime() {
        return this.nextFireTime;
    }

    /**
     * Sets the next time this schedule is going to fire.
     *
     * @param nextFireTime next scheduled trigger time, in epoch milliseconds
     */
    public void setNextFireTime(Long nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("id=" + id);
        buf.append(", jobName=" + jobName);
        buf.append(", jobGroup=" + jobGroup);
        buf.append(", nextFireTime=" + ((nextFireTime == null) ? "never" : new Date(nextFireTime)));

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

        if ((obj == null) || !(obj instanceof OperationScheduleEntity)) {
            return false;
        }

        final OperationScheduleEntity other = (OperationScheduleEntity) obj;

        return this.getJobId().equals(other.getJobId());
    }

}