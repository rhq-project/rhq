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
package org.rhq.core.domain.cluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * An object to capture when the infrastructure used for high availability
 * reconfigures itself for some reason.  This object will store the reason
 * for the change, the connection topology after the change takes place, and
 * the time it took place.
 * 
 * @author jmarques
 *
 */
@Entity(name = "PartitionEvent")
@NamedQueries //
( { @NamedQuery(name = PartitionEvent.QUERY_FIND_ALL, query = "SELECT pe FROM PartitionEvent pe"),
    @NamedQuery(name = PartitionEvent.QUERY_FIND_VIA_EXECUTION_STATUS, query = "" //
        + "SELECT pe FROM PartitionEvent pe " //
        + " WHERE pe.executionStatus = :executionStatus") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_PARTITION_EVENT_ID_SEQ")
@Table(name = "RHQ_PARTITION_EVENT")
public class PartitionEvent implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "PartitionEvent.findAll";
    public static final String QUERY_FIND_VIA_EXECUTION_STATUS = "PartitionEvent.findRequested";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @Column(name = "SUBJECT_NAME")
    protected String subjectName;

    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @Column(name = "EVENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private PartitionEventType eventType;

    @Column(name = "EXECUTION_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private PartitionEvent.ExecutionStatus executionStatus;

    @OneToMany(mappedBy = "partitionEvent", cascade = CascadeType.ALL)
    private List<PartitionEventDetails> eventDetails = new ArrayList<PartitionEventDetails>();

    // required for JPA
    protected PartitionEvent() {
    }

    public PartitionEvent(String subjectName, PartitionEventType eventType,
        PartitionEvent.ExecutionStatus executionStatus) {
        this.subjectName = subjectName;
        this.eventType = eventType;
        this.executionStatus = executionStatus;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public PartitionEventType getEventType() {
        return eventType;
    }

    public void setEventType(PartitionEventType eventType) {
        this.eventType = eventType;
    }

    public List<PartitionEventDetails> getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(List<PartitionEventDetails> eventDetails) {
        this.eventDetails = eventDetails;
    }

    public enum ExecutionStatus {

        AUDIT("This parition event is executed only as an audit activity and did not affect server list generation"), //
        COMPLETED("The request for this partition event has been completed by the cluster manager job"), //
        IMMEDIATE("This partition event was executed at creation time."), //
        REQUESTED("This partition event has been requested of the cluster manager job.");

        public final String message;

        private ExecutionStatus(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public PartitionEvent.ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(PartitionEvent.ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (ctime ^ (ctime >>> 32));
        result = prime * result + ((subjectName == null) ? 0 : subjectName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PartitionEvent)) {
            return false;
        }

        final PartitionEvent other = (PartitionEvent) obj;

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
