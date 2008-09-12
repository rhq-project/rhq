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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

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
( { @NamedQuery(name = PartitionEvent.QUERY_FIND_ALL, query = "SELECT pe FROM PartitionEvent pe")
    })
@SequenceGenerator(name = "id", sequenceName = "RHQ_PARTITION_EVENT_ID_SEQ")
@Table(name = "RHQ_PARTITION_EVENT")
public class PartitionEvent implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "PartitionEvent.findAll";

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

    @OneToMany(mappedBy = "partitionEvent")
    private List<PartitionEventDetails> eventDetails = new ArrayList<PartitionEventDetails>();

    // required for JPA
    protected PartitionEvent() {
    }

    public PartitionEvent(String subjectName, PartitionEventType eventType) {
        this.subjectName = subjectName;
        this.eventType = eventType;
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
