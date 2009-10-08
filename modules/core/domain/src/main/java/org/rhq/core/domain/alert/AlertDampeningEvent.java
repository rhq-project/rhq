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
package org.rhq.core.domain.alert;

import java.io.Serializable;
import java.text.SimpleDateFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@NamedQueries( { @NamedQuery(name = AlertDampeningEvent.QUERY_FIND_LATEST_BY_ALERT_DEFINITION_ID, query = "" //
    + "SELECT ade " //
    + "  FROM AlertDampeningEvent ade " //
    + " WHERE ade.id = " //
    + "       ( SELECT max(iade.id) FROM AlertDampeningEvent iade " //
    + "         WHERE iade.alertDefinition.id = :alertDefinitionId )"), //
    // do NOT change the ORDER BY clause of this query, the SLSB layer above it depends on the oldest event being first
    @NamedQuery(name = AlertDampeningEvent.QUERY_FIND_BY_TIME_AND_TYPES, query = "" //
        + "SELECT ade " //
        + "  FROM AlertDampeningEvent ade " //
        + " WHERE ade.alertDefinition.id = :alertDefinitionId " //
        + "   AND ade.eventType IN (:eventTypes) " //
        + "   AND ade.eventTime > :oldestEventTime " //
        + " ORDER BY ade.eventTime ASC"), //
    @NamedQuery(name = AlertDampeningEvent.QUERY_FIND_BY_ALERT_DEFINITION_ID, query = "" //
        + "SELECT ade " //
        + "  FROM AlertDampeningEvent ade " //
        + " WHERE ade.alertDefinition.id = :alertDefinitionId " //
        + " ORDER BY ade.eventTime DESC "), //
    @NamedQuery(name = AlertDampeningEvent.QUERY_DELETE_BY_RESOURCES, query = "" //
        + "DELETE AlertDampeningEvent ade " //
        + " WHERE ade.alertDefinition IN " //
        + "       ( SELECT ad FROM AlertDefinition ad " //
        + "          WHERE ad.resource.id IN ( :resourceIds ) )"), //
    @NamedQuery(name = AlertDampeningEvent.QUERY_DELETE_BY_TIMESTAMP, // 
    query = "DELETE AlertDampeningEvent ade " //
        + "   WHERE ade.eventTime < :oldest " //
        + "     AND ade.alertDefinition.id = :alertDefinitionId "), //
    @NamedQuery(name = AlertDampeningEvent.QUERY_DELETE_BY_ALERT_DEFINITION_ID, query = "" //
        + "DELETE AlertDampeningEvent ade " //
        + " WHERE ade.alertDefinition.id = :alertDefinitionId") })
@SequenceGenerator(name = "RHQ_ALERT_DAMPEN_EVENT_ID_SEQ", sequenceName = "RHQ_ALERT_DAMPEN_EVENT_ID_SEQ")
@Table(name = "RHQ_ALERT_DAMPEN_EVENT")
public class AlertDampeningEvent implements Serializable {
    public static final String QUERY_FIND_LATEST_BY_ALERT_DEFINITION_ID = "AlertDampeningEvent.findLatestByAlertDefinitionId";
    public static final String QUERY_FIND_BY_TIME_AND_TYPES = "AlertDampeningEvent.findByTimeAndTypes";
    public static final String QUERY_FIND_BY_ALERT_DEFINITION_ID = "AlertDampeningEvent.findByAlertDefinitionId";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertDampeningEvent.deleteByResources";
    public static final String QUERY_DELETE_BY_TIMESTAMP = "AlertDampeningEvent.deleteByTimestamp";
    public static final String QUERY_DELETE_BY_ALERT_DEFINITION_ID = "AlertDampeningEvent.deleteByAlertDefinitionId";

    public enum Type {
        POSITIVE, POSITIVE_AGAIN, NEGATIVE, UNCHANGED;
    }

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ALERT_DAMPEN_EVENT_ID_SEQ")
    @Id
    private int id;

    // bidrectional so that AlertDampeningEvent queries are simpler
    @JoinColumn(name = "ALERT_DEFINITION_ID", referencedColumnName = "ID")
    @ManyToOne
    private AlertDefinition alertDefinition;

    @Column(name = "EVENT_TYPE")
    @Enumerated(EnumType.ORDINAL)
    private Type eventType;

    @Column(name = "EVENT_TIMESTAMP")
    private long eventTime;

    protected AlertDampeningEvent() {
    } // JPA

    public AlertDampeningEvent(AlertDefinition alertDefinition, Type type) {
        this.eventType = type;
        this.alertDefinition = alertDefinition;
        this.eventTime = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    public void setAlertDefinition(AlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    public Type getEventType() {
        return eventType;
    }

    public Long getEventTime() {
        return eventTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AlertDampeningEvent)) {
            return false;
        }

        AlertDampeningEvent other = (AlertDampeningEvent) o;

        if (eventTime != other.eventTime) {
            return false;
        }

        if (eventType == null) {
            if (other.eventType != null) {
                return false;
            }
        } else if (!eventType.equals(other.eventType)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (eventTime ^ (eventTime >>> 32));
        result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.alert.AlertDampeningEvent" + "[ " + "id=" + id + ", " + "eventType=" + eventType
            + ", " + "eventTime=" + new SimpleDateFormat("MMM d, hh:mm:ss a").format(eventTime) + " ]";
    }
}