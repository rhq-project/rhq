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
package org.rhq.core.domain.alert.notification;

import java.io.Serializable;

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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.alert.Alert;

@Entity
@NamedQueries( { @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_ALERT_CTIME, //
    query = "DELETE AlertNotificationLog anl  WHERE anl.id IN ("
        + "SELECT an.id FROM Alert a JOIN a.alertNotificationLogs an WHERE a.ctime BETWEEN :begin AND :end)"),

    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCE, //
    query = "DELETE AlertNotificationLog anl WHERE anl.id IN ("
        + "SELECT an.id FROM Alert a JOIN a.alertNotificationLogs an JOIN a.alertDefinition def "
        + "WHERE def.resource.id = :resourceId)"),

    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCES, //
    query = "DELETE AlertNotificationLog anl WHERE anl.id IN ("
        + "SELECT an.id FROM Alert a JOIN a.alertNotificationLogs an JOIN a.alertDefinition def "
        + "WHERE def.resource.id IN ( :resourceIds ) )") })
@SequenceGenerator(name = "RHQ_ALERT_NOTIF_LOG_ID_SEQ", sequenceName = "RHQ_ALERT_NOTIF_LOG_ID_SEQ")
@Table(name = "RHQ_ALERT_NOTIF_LOG")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlertNotificationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCE = "AlertNotificationLog.deleteByResource";
    public static final String QUERY_DELETE_BY_ALERT_CTIME = "AlertNotificationLog.deleteByAlertCtime";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertNotificationLog.deleteByResources";

    public static final String QUERY_NATIVE_TRUNCATE_SQL = "TRUNCATE TABLE RHQ_ALERT_NOTIF_LOG";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ALERT_NOTIF_LOG_ID_SEQ")
    @Id
    private int id;

    /*
     * note, currently there is no distinction between successful and failed notifications, but there should be in the
     * future
     */

    @JoinColumn(name = "ALERT_ID", referencedColumnName = "ID")
    @ManyToOne
    @XmlTransient
    private Alert alert;

    @Column(name = "SENDER")
    private String sender;

    @Column(name = "RESULT_STATE")
    @Enumerated(EnumType.STRING)
    private ResultState resultState;

    @Column(name = "MESSAGE")
    private String message;

    @PrePersist
    @PreUpdate
    public void trimMessage() {
        if (message != null && message.length() > 4000) {
            message = message.substring(0, 4000);
        }
    }

    protected AlertNotificationLog() {
    } // JPA

    public AlertNotificationLog(Alert alert, String sender, SenderResult senderResult) {
        this.alert = alert;
        this.sender = sender;
        this.resultState = senderResult.getState();
        this.message = getMessage(senderResult);
    }

    public AlertNotificationLog(Alert alert, String senderName, ResultState state, String message) {
        this.alert = alert;
        this.sender = senderName;
        this.resultState = state;
        this.message = message;
    }

    private String getMessage(SenderResult result) {
        StringBuilder builder = new StringBuilder();

        boolean first = true;

        if (result.getSummary() != null) {
            first = false;
            builder.append(result.getSummary());
        }

        for (String success : result.getSuccessMessages()) {
            if (first) {
                first = false;
            } else {
                builder.append("<br/>");
            }
            builder.append(success);
        }

        for (String failure : result.getFailureMessages()) {
            if (first) {
                first = false;
            } else {
                builder.append("<br/>");
            }
            builder.append(failure);
        }

        return builder.toString();
    }

    public int getId() {
        return id;
    }

    public Alert getAlert() {
        return alert;
    }

    public String getSender() {
        return sender;
    }

    public ResultState getResultState() {
        return resultState;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AlertNotificationLog");
        sb.append("{id=").append(id);
        sb.append(", alert=").append(alert);
        sb.append(", sender='").append(sender).append('\'');
        sb.append(", resultState=").append(resultState);
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}