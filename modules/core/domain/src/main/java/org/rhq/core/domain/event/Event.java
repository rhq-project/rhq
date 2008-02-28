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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.resource.Resource;

/**
 * A transpired event, pertaining to a particular {@link Resource}.
 *
 * @author Ian Springer
 */
@NamedQueries( {
    @NamedQuery(name = Event.FIND_EVENTS_FOR_RESOURCES_AND_TIME, query = "SELECT ev FROM Event ev "
        + " JOIN ev.source.resource res WHERE res IN (:resources) AND ev.timestamp BETWEEN :start AND :end "),
    @NamedQuery(name = Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME, query = "SELECT ev FROM Event ev "
        + " JOIN ev.source evs JOIN evs.resource res WHERE res.id = :resourceId AND ev.timestamp BETWEEN :start AND :end "),
    @NamedQuery(name = Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME_SEVERITY, query = "SELECT ev FROM Event ev "
        + " JOIN ev.source evs JOIN evs.resource res WHERE res.id = :resourceId AND ev.severity = :severity "
        + " AND ev.timestamp BETWEEN :start AND :end "),
    @NamedQuery(name = Event.GET_DETAILS_FOR_EVENT_IDS, query = "SELECT "
        + " new org.rhq.core.domain.event.composite.EventComposite(ev.detail, res.id, ev.id, evs.location) "
        + " FROM Event ev JOIN ev.source evs JOIN evs.resource res WHERE ev.id IN (:eventIds) AND evs.id = ev.source"
        + "  AND res.id = evs.resource ") })
@Entity
@Table(name = Event.TABLE_NAME)
@SequenceGenerator(name = "idGenerator", sequenceName = Event.TABLE_NAME + "_ID_SEQ", allocationSize = 100)
public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_EVENT";
    public static final int DETAIL_MAX_LENGTH = 4000;

    public static final String FIND_EVENTS_FOR_RESOURCES_AND_TIME = "Event.FIND_EVENTS_FOR_RESOURCES_AND_TIME";
    public static final String FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME = "Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME";
    public static final String FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME_SEVERITY = "Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME_Severity";
    public static final String GET_DETAILS_FOR_EVENT_IDS = "Event.GET_DETAILS_FOR_EVENT_IDS";

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGenerator")
    private int id;

    @JoinColumn(name = "EVENT_SOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private EventSource source;

    @Column(name = "TIMESTAMP", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "SEVERITY", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EventSeverity severity;

    @Column(name = "DETAIL", length = 4000, nullable = false)
    private String detail; // TODO lazify

    @Column(name = "ACK_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ackTime;

    @Column(name = "ACK_USER", length = 100)
    private String ackUser;

    @Transient
    private transient String detailExcerpt;

    /**
     * The event's type (i.e. the name of its {@link EventDefinition}).
     */
    @Transient
    private transient String type;

    /**
     * The event's source location (i.e. the location field of its {@link EventSource}).
     */
    @Transient
    private transient String sourceLocation;

    /* no-arg constructor required by EJB spec */
    protected Event() {
    }

    /**
     *
     * @param type the name of the {@link org.rhq.core.domain.event.EventDefinition} that defines this event's type
     * @param timestamp the date/time this event occurred
     * @param severity the severity of this event
     * @param detail the details of this event
     */
    public Event(@NotNull
    String type, @NotNull
    String sourceLocation, @NotNull
    Date timestamp, @NotNull
    EventSeverity severity, @NotNull
    String detail) {
        if (type == null)
            throw new IllegalArgumentException("type parameter must not be null.");
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
        if (timestamp == null)
            throw new IllegalArgumentException("timestamp parameter must not be null.");
        if (severity == null)
            throw new IllegalArgumentException("severity parameter must not be null.");
        if (detail == null)
            throw new IllegalArgumentException("detail parameter must not be null.");
        this.type = type;
        this.sourceLocation = sourceLocation;
        this.timestamp = timestamp;
        this.severity = severity;
        this.detail = detail;
        if (this.detail.length() > DETAIL_MAX_LENGTH)
            this.detail = this.detail.substring(0, DETAIL_MAX_LENGTH);
    }

    public int getId() {
        return this.id;
    }

    @Nullable
    public EventSource getSource() {
        return this.source;
    }

    @Nullable
    public String getType() {
        return this.type;
    }

    @Nullable
    public String getSourceLocation() {
        return this.sourceLocation;
    }

    @NotNull
    public Date getTimestamp() {
        return this.timestamp;
    }

    @NotNull
    public EventSeverity getSeverity() {
        return this.severity;
    }

    @NotNull
    public String getDetail() {
        return this.detail;
    }

    @Nullable
    public Date getAckTime() {
        return ackTime;
    }

    @Nullable
    public String getAckUser() {
        return ackUser;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Event that = (Event) obj;

        if (!timestamp.equals(that.timestamp))
            return false;
        if (severity != that.severity)
            return false;
        if (!detail.equals(that.detail))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp.hashCode();
        result = 31 * result + severity.hashCode();
        result = 31 * result + detail.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "["
                + "id=" + this.id + ", " + "source=" + this.source + ", " + "timestamp=" + this.timestamp
                + ", " + "severity=" + this.severity + ", " + "detail=" + this.detail + "]";

    }

    public String getDetailExcerpt() { // TODO get from DB
        int len = 100;
        if (detailExcerpt != null)
            return detailExcerpt;

        if (detail.length() < len)
            len = detail.length();
        detailExcerpt = detail.substring(0, len); // TODO 
        return detailExcerpt;
    }

    public void setDetailExcerpt(String detailExcerpt) {
        this.detailExcerpt = detailExcerpt;
    }
}
