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
package org.rhq.core.domain.event;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.rhq.core.domain.resource.Resource;

/**
 * A log record for a transpired JON event.
 */
@Entity
@SequenceGenerator(name = "RHQ_RESOURCE_EVENT_ID_SEQ", sequenceName = "RHQ_RESOURCE_EVENT_ID_SEQ", allocationSize = 100)
@Table(name = "RHQ_RESOURCE_EVENT")
public class EventLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_RESOURCE_EVENT_ID_SEQ")
    @Id
    @SuppressWarnings( { "UnusedDeclaration", "UNUSED_SYMBOL", "unused" })
    private int id;

    @JoinColumn(name = "RESOURCE_ID")
    @ManyToOne
    private Resource resource;

    @Column(name = "TYPE", nullable = false)
    private String type;

    @Column(name = "DETAIL", nullable = false)
    private String detail;

    @Column(name = "TIMESTAMP", nullable = false)
    private long timestamp;

    @Column(name = "SUBJECT")
    private String subject;

    @Column(name = "STATUS")
    private String status;

    public EventLog() {
    }

    public int getId() {
        return this.id;
    }

    public String getDetail() {
        return this.detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof EventLog)) {
            return false;
        }

        EventLog that = (EventLog) obj;
        return (id == that.id) && (timestamp == that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = (31 * result) + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.event.EventLog[id=" + id + "]";
    }
}