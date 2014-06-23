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
package org.rhq.core.domain.event;

import java.io.Serializable;

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
import javax.persistence.Transient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A transpired event, pertaining to a particular {@link Resource}.
 *
 * @author Ian Springer
 */
@NamedQueries( {
    @NamedQuery(name = Event.DELETE_BY_RESOURCES, query = "DELETE FROM Event ev  "
        + " WHERE ev.source IN ( SELECT evs FROM EventSource evs WHERE evs.resource.id IN ( :resourceIds ) )"),
    @NamedQuery(name = Event.DELETE_BY_EVENT_IDS, query = "DELETE FROM Event e WHERE e.id IN ( :eventIds )"),
    @NamedQuery(name = Event.DELETE_ALL_BY_RESOURCE, query = "" //
        + "DELETE FROM Event e " //
        + " WHERE e.source.id IN ( SELECT es.id " //
        + "                          FROM EventSource es " //
        + "                         WHERE es.resource.id = :resourceId )"),
    @NamedQuery(name = Event.DELETE_ALL_BY_RESOURCE_GROUP, query = "" //
        + "DELETE FROM Event e " //
        + " WHERE e.source.id IN ( SELECT es.id " //
        + "                          FROM EventSource es " //
        + "                          JOIN es.resource res " //
        + "                          JOIN res.explicitGroups ig " // use explicit here, this is not an authz check
        + "                         WHERE ig.id = :groupId )"),
    @NamedQuery(name = Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME, query = "SELECT ev FROM Event ev "
        + " JOIN ev.source evs JOIN evs.resource res WHERE res.id = :resourceId AND ev.timestamp BETWEEN :start AND :end "),
    @NamedQuery(name = Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME_SEVERITY, query = "SELECT ev FROM Event ev "
        + " JOIN ev.source evs JOIN evs.resource res WHERE res.id = :resourceId AND ev.severity = :severity "
        + " AND ev.timestamp BETWEEN :start AND :end "),
    @NamedQuery(name = Event.GET_DETAILS_FOR_EVENT_IDS, query = "SELECT "
        + " new org.rhq.core.domain.event.composite.EventComposite(ev.detail, res.id, res.name, res.ancestry, res.resourceType.id, ev.id, ev.severity, evs.location, ev.timestamp) "
        + " FROM Event ev JOIN ev.source evs JOIN evs.resource res WHERE ev.id IN (:eventIds) AND evs.id = ev.source"
        + "  AND res.id = evs.resource "), //
    @NamedQuery(name = Event.QUERY_EVENT_COUNTS_BY_SEVERITY, query = "" //
        + "  SELECT ev.severity, count(ev.severity) " //
        + "    FROM Event ev JOIN ev.source evs JOIN evs.resource res " //
        + "   WHERE res.id = :resourceId " //
        + "     AND ev.timestamp BETWEEN :start AND :end " //
        + "GROUP BY ev.severity"), //
    @NamedQuery(name = Event.QUERY_EVENT_COUNTS_BY_SEVERITY_GROUP, query = "" //
        + "  SELECT ev.severity, count(ev.severity) " //
        + "    FROM Event ev JOIN ev.source evs JOIN evs.resource res JOIN res.explicitGroups ig" //
        + "   WHERE ig.id = :groupId " //
        + "     AND ev.timestamp BETWEEN :start AND :end " //
        + "GROUP BY ev.severity") })
@Entity
@Table(name = Event.TABLE_NAME)
@SequenceGenerator(name = "idGenerator", sequenceName = Event.TABLE_NAME + "_ID_SEQ", allocationSize = 100)
public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_EVENT";

    /**
     * this is a character limit, when stored certain vendors may require the string be clipped to
     * satisfy a byte limit (postgres can store the 4000 chars, oracle only 4000 bytes).
     */
    public static final int DETAIL_MAX_LENGTH = 4000;

    public static final String DELETE_BY_RESOURCES = "Event.deleteByResources";
    public static final String DELETE_BY_EVENT_IDS = "Event.deleteByEventIds";
    public static final String DELETE_ALL_BY_RESOURCE = "Event.deleteAllByResource";
    public static final String DELETE_ALL_BY_RESOURCE_GROUP = "Event.deleteAllByResourceGroup";
    public static final String FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME = "Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME";
    public static final String FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME_SEVERITY = "Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME_Severity";
    public static final String GET_DETAILS_FOR_EVENT_IDS = "Event.GET_DETAILS_FOR_EVENT_IDS";
    public static final String QUERY_EVENT_COUNTS_BY_SEVERITY = "Event.eventCountsBySeverity";
    public static final String QUERY_EVENT_COUNTS_BY_SEVERITY_GROUP = "Event.eventCountsBySeverityGroup";

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "idGenerator")
    private int id;

    @JoinColumn(name = "EVENT_SOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EventSource source;

    @Column(name = "TIMESTAMP", nullable = false)
    private long timestamp;

    @Column(name = "SEVERITY", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EventSeverity severity;

    @Column(name = "DETAIL", length = DETAIL_MAX_LENGTH, nullable = false)
    private String detail;

    /** The event's type (i.e. the name of its {@link EventDefinition}). */
    @Transient
    private transient String type;

    /** The event's source location (i.e. the location field of its {@link EventSource}). */
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
    public Event(@NotNull String type, @NotNull String sourceLocation, @NotNull long timestamp,
        @NotNull EventSeverity severity, @NotNull String detail) {
        if (type == null)
            throw new IllegalArgumentException("type parameter must not be null.");
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
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

    public Event(@NotNull String type, @NotNull String sourceLocation, @NotNull long timestamp,
        @NotNull EventSeverity severity, @NotNull String detail, @Nullable EventSource source) {
        if (type == null)
            throw new IllegalArgumentException("type parameter must not be null.");
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
        if (severity == null)
            throw new IllegalArgumentException("severity parameter must not be null.");
        if (detail == null)
            throw new IllegalArgumentException("detail parameter must not be null.");
        this.source = source;
        this.type = type;
        this.sourceLocation = sourceLocation;
        this.timestamp = timestamp;
        this.severity = severity;
        this.detail = detail;
        if (this.detail.length() > DETAIL_MAX_LENGTH)
            this.detail = this.detail.substring(0, DETAIL_MAX_LENGTH);
    }

    /** NOTE: This method is not intended to be called by plugins or the Plugin Container. */
    public int getId() {
        return this.id;
    }

    /** NOTE: This method is not intended to be called by plugins or the Plugin Container. */
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
    public long getTimestamp() {
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof Event))
            return false;

        Event that = (Event) obj;

        if (timestamp != that.timestamp)
            return false;
        if (severity != that.severity)
            return false;
        if (!detail.equals(that.detail))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 31 + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + severity.hashCode();
        result = 31 * result + detail.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".") + 1) + "[" + "id="
            + this.id + ", " + "source=" + this.source + ", " + "timestamp=" + this.timestamp + ", " + "severity="
            + this.severity + ", " + "detail=" + this.detail + "]";

    }
}
