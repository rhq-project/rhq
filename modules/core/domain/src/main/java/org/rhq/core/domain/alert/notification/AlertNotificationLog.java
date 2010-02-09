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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.alert.Alert;

@Entity
@NamedQueries( {
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_ALERT_CTIME, //
            query = "DELETE AlertNotificationLog anl  WHERE anl.id IN ("  +
                    "SELECT an.id FROM Alert a JOIN a.alertNotificationLog an WHERE a.ctime BETWEEN :begin AND :end)"),

    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCE, //
            query = "DELETE AlertNotificationLog anl WHERE anl.id IN (" +
                    "SELECT an.id FROM Alert a JOIN a.alertNotificationLog an JOIN a.alertDefinition def " +
                    "WHERE def.resource.id = :resourceId)"),

    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCES, //
            query = "DELETE AlertNotificationLog anl WHERE anl.id IN (" +
                    "SELECT an.id FROM Alert a JOIN a.alertNotificationLog an JOIN a.alertDefinition def " +
                    "WHERE def.resource.id IN ( :resourceIds ) )")
  })
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

    private String sender;

    @Column(name ="RESULT")
    @Enumerated(EnumType.STRING)
    private ResultState result;

    private String message;

    @Column(name="EMAILS_SENT")
    private String goodEmails;

    @Column(name="EMAILS_FAILED")
    private String badEmails;


    protected AlertNotificationLog() {
    } // JPA

    public AlertNotificationLog(Alert alert, String sender, SenderResult senderResult) {
        this.alert = alert;
        this.sender = sender;
        this.result = senderResult.getState();
        this.message = senderResult.getMessage();
    }

    public AlertNotificationLog(Alert alert, String sender) {
        this.alert = alert;
        this.sender = sender;
        this.result = ResultState.FAILURE; // Default if nothing specified
    }

    public AlertNotificationLog(Alert alert, String senderName, ResultState state, String message) {
        this.alert = alert;
        this.sender = sender;
        this.result = state;
        this.message = message;

    }

    public int getId() {
        return id;
    }


    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public String getSender() {
        return sender;
    }

    public ResultState getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public String getGoodEmails() {
        return goodEmails;
    }

    public String getBadEmails() {
        return badEmails;
    }

    public void setResult(ResultState result) {
        this.result = result;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setGoodEmails(String goodEmails) {
        this.goodEmails = goodEmails;
    }

    public void setBadEmails(String badEmails) {
        this.badEmails = badEmails;
    }
}