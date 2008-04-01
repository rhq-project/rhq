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
package org.rhq.core.domain.alert;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@NamedQueries( {
    @NamedQuery(name = AlertDampeningEvent.QUERY_FIND_LATEST_BY_ALERT_DEFINITION_ID, query = "SELECT ade "
        + "FROM AlertDampeningEvent ade " + "WHERE ade.id = " + "( SELECT max(iade.id) FROM AlertDampeningEvent iade "
        + "WHERE iade.alertDefinition.id = :alertDefinitionId " + "AND iade.used = FALSE )"),
    @NamedQuery(name = AlertDampeningEvent.QUERY_FIND_BY_TIME_AND_TYPES, query = "SELECT ade "
        + "FROM AlertDampeningEvent ade " + "WHERE ade.alertDefinition.id = :alertDefinitionId "
        + "AND ade.eventType IN (:eventTypes) " + "AND ade.eventTime > :oldestEventTime " + "AND ade.used = FALSE "
        + "ORDER BY ade.eventTime ASC"),
    @NamedQuery(name = AlertDampeningEvent.QUERY_FIND_BY_ALERT_DEFINITION_ID, query = "  SELECT ade "
        + "    FROM AlertDampeningEvent ade " + "   WHERE ade.alertDefinition.id = :alertDefinitionId "
        + "     AND ade.used = false " + "ORDER BY ade.eventTime DESC "),
    @NamedQuery(name = AlertDampeningEvent.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM AlertDampeningEvent ade WHERE ade.alertDefinition IN ( SELECT ad FROM AlertDefinition ad WHERE ad.resource IN (:resources))") })
@SequenceGenerator(name = "RHQ_ALERT_DAMPEN_EVENT_ID_SEQ", sequenceName = "RHQ_ALERT_DAMPEN_EVENT_ID_SEQ")
@Table(name = "RHQ_ALERT_DAMPEN_EVENT")
public class AlertDampeningEvent implements Serializable {
    public static final String QUERY_FIND_LATEST_BY_ALERT_DEFINITION_ID = "AlertDampeningEvent.findLatestByAlertDefinitionId";
    public static final String QUERY_FIND_BY_TIME_AND_TYPES = "AlertDampeningEvent.findByTimeAndTypes";
    public static final String QUERY_FIND_BY_ALERT_DEFINITION_ID = "AlertDampeningEvent.findByAlertDefinitionId";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertDampeningEvent.deleteByResources";

    public enum Type {
        POSITIVE, POSITIVE_AGAIN, NEGATIVE, UNCHANGED;
    }

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_ALERT_DAMPEN_EVENT_ID_SEQ")
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
    @Temporal(TemporalType.TIMESTAMP)
    private Date eventTime;

    @Column(name = "USED")
    @SuppressWarnings("unused")
    private boolean used;

    protected AlertDampeningEvent() {
    } // JPA

    public AlertDampeningEvent(AlertDefinition alertDefinition, Type type) {
        this.eventType = type;
        this.alertDefinition = alertDefinition;
        this.eventTime = new Date(System.currentTimeMillis());
        this.used = false;
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

    public Date getEventTime() {
        return eventTime;
    }

    public void markUsed() {
        this.used = true;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertDampeningEvent)) return false;

        AlertDampeningEvent event = (AlertDampeningEvent) o;

        if (eventTime != null ? !eventTime.equals(event.eventTime) : event.eventTime != null) return false;
        if (eventType != event.eventType) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (eventTime != null ? eventTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.alert.AlertDampeningEvent" + "[ " + "id=" + id + ", " + "eventType=" + eventType
            + ", " + "eventTime=" + new SimpleDateFormat("MMM d, hh:mm:ss a").format(eventTime) + ", " + "used=" + used
            + " ]";
    }
}