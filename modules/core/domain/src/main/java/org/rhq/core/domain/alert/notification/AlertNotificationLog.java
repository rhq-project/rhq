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

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;

@Entity
@NamedQueries( {
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_ALERT_CTIME, //
    query = "DELETE AlertNotificationLog anl " + //
        "     WHERE anl.id IN ( SELECT a.alertNotificationLog.id " + //
        "                         FROM Alert a " + //
        "                        WHERE a.ctime BETWEEN :begin AND :end )"),
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCE, //
    query = "DELETE AlertNotificationLog anl " + //
        "     WHERE anl.id IN ( SELECT a.alertNotificationLog.id " + //
        "                         FROM Alert a " + //
        "                        WHERE a.alertDefinition.resource.id = :resourceId )"),
    @NamedQuery(name = AlertNotificationLog.QUERY_DELETE_BY_RESOURCES, //
    query = "DELETE AlertNotificationLog anl " + //
        "     WHERE anl.id IN ( SELECT a.alertNotificationLog.id " + //
        "                         FROM Alert a " + //
        "                        WHERE a.alertDefinition.resource.id IN ( :resourceIds ) )") })
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
    @Column(name = "ROLES", nullable = false)
    private String roles;

    @Column(name = "SUBJECTS", nullable = false)
    private String subjects;

    @Column(name = "EMAILS", nullable = false)
    private String emails;

    @JoinColumn(name = "ALERT_ID", referencedColumnName = "ID")
    @OneToOne
    @XmlTransient
    private Alert alert;

    protected AlertNotificationLog() {
    } // JPA

    public AlertNotificationLog(Alert alert) {
        AlertDefinition alertDefinition = alert.getAlertDefinition();

        StringBuilder rolesBuilder = new StringBuilder();
        StringBuilder subjectsBuilder = new StringBuilder();
        StringBuilder emailsBuilder = new StringBuilder();

        List<AlertNotification> currentNotifications = alertDefinition.getAlertNotifications();
        for (AlertNotification notification : currentNotifications) {
                //((SnmpNotification)notification).
                // TODO: log that this type of AlertNotification is not supported yet for auditing
        }

        // always make sure each notification field is non-null by "fixing" it
        this.alert = alert;
    }

    private String fixup(StringBuilder builder) {
        if (builder.length() == 0) {
            return "(none)";
        } else {
            return builder.toString();
        }
    }

    public int getId() {
        return id;
    }

    public String getRoles() {
        return roles;
    }

    public String getSubjects() {
        return subjects;
    }

    public String getEmails() {
        return emails;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }
}