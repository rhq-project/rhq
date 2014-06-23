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

/**
 * A log record for a triggered action and/or notification taken for a fired alert.
 * 
 * @author Joseph Marques
 */
@Entity
@NamedQueries({
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_ALL, //
    query = "DELETE AlertNotificationLog anl " //
        + "   WHERE anl.alert.id IN ( SELECT alert.id " //
        + "                             FROM Alert alert )"),
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_ALERT_IDS, //
    query = "DELETE AlertNotificationLog anl " //
        + "   WHERE anl.id IN ( SELECT an.id " //
        + "                       FROM Alert a " //
        + "                       JOIN a.alertNotificationLogs an" //
        + "                      WHERE a.id IN ( :alertIds ) )"),
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCES, //
    query = "DELETE AlertNotificationLog anl " //
        + "   WHERE anl.alert.id IN ( SELECT alert.id " //
        + "                             FROM AlertDefinition ad " //
        + "                             JOIN ad.alerts alert " //
        + "                            WHERE ad.resource.id IN ( :resourceIds ) ))"),
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCE_TEMPLATE,
    query = "DELETE AlertNotificationLog log "
        + "  WHERE log.alert.id IN (SELECT alert.id "
        + "                         FROM   AlertDefinition alertDef "
        + "                         JOIN   alertDef.alerts alert "
        + "                         WHERE  alertDef.resourceType.id = :resourceTypeId)"),
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCE_GROUPS, //
    query = "DELETE AlertNotificationLog anl " //
        + "   WHERE anl.alert.id IN ( SELECT alert.id " //
        + "                             FROM AlertDefinition ad " //
        + "                             JOIN ad.alerts alert " //
        + "                             JOIN ad.resource res" //
        + "                             JOIN res.implicitGroups rg " //
        + "                            WHERE rg.id IN ( :groupIds ) ))"),
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_ALERT_CTIME, //
    query = "DELETE AlertNotificationLog anl " //
        + "   WHERE anl.id IN ( SELECT an.id " //
        + "                       FROM Alert a " //
        + "                       JOIN a.alertNotificationLogs an " //
        + "                      WHERE a.ctime BETWEEN :begin AND :end )") })
@SequenceGenerator(name = "RHQ_ALERT_NOTIF_LOG_ID_SEQ", sequenceName = "RHQ_ALERT_NOTIF_LOG_ID_SEQ")
@Table(name = "RHQ_ALERT_NOTIF_LOG")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlertNotificationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_ALL = "AlertNotificationLog.deleteByAll";
    public static final String QUERY_DELETE_BY_ALERT_IDS = "AlertNotificationLog.deleteByAlertIds";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertNotificationLog.deleteByResources";
    public static final String QUERY_DELETE_BY_RESOURCE_TEMPLATE = "AlertNotificationLog.deleteByResourceType";
    public static final String QUERY_DELETE_BY_RESOURCE_GROUPS = "AlertNotificationLog.deleteByResourceGroups";
    public static final String QUERY_DELETE_BY_ALERT_CTIME = "AlertNotificationLog.deleteByAlertCtime";

    public static final String QUERY_NATIVE_TRUNCATE_SQL = "TRUNCATE TABLE RHQ_ALERT_NOTIF_LOG";

    /**
     * this is a character limit, when stored certain vendors may require the string be clipped to
     * satisfy a byte limit (postgres can store the 4000 chars, oracle only 4000 bytes).
     */
    public static final int MESSAGE_MAX_LENGTH = 4000;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ALERT_NOTIF_LOG_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "ALERT_ID", referencedColumnName = "ID")
    @ManyToOne
    @XmlTransient
    private Alert alert;

    @Column(name = "SENDER")
    private String sender;

    @Column(name = "RESULT_STATE")
    @Enumerated(EnumType.STRING)
    private ResultState resultState;

    @Column(name = "MESSAGE", length = MESSAGE_MAX_LENGTH)
    private String message;

    /**
     * This is insufficient for certain db vendors as it handles only character limit, not necessarily the
     * 4000 byte limit on oracle.
     *
     * @deprecated the message should be trimmed sufficiently by the caller to meet any db vendor specific
     * byte limits.
     */
    @Deprecated
    @PrePersist
    @PreUpdate
    public void trimMessage() {
        if (message != null && message.length() > MESSAGE_MAX_LENGTH) {
            message = message.substring(0, MESSAGE_MAX_LENGTH);
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
