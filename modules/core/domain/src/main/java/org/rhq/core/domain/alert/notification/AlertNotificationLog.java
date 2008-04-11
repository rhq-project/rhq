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
package org.rhq.core.domain.alert.notification;

import java.io.Serializable;
import java.util.Set;

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
        "                        WHERE a.alertDefinition.resource IN ( :resources ) )") })
@SequenceGenerator(name = "RHQ_ALERT_NOTIF_LOG_ID_SEQ", sequenceName = "RHQ_ALERT_NOTIF_LOG_ID_SEQ")
@Table(name = "RHQ_ALERT_NOTIF_LOG")
public class AlertNotificationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCE = "AlertNotificationLog.deleteByResource";
    public static final String QUERY_DELETE_BY_ALERT_CTIME = "AlertNotificationLog.deleteByAlertCtime";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertNotificationLog.deleteByResources";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_ALERT_NOTIF_LOG_ID_SEQ")
    @Id
    @SuppressWarnings( { "unused" })
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

    //@OneToOne(mappedBy = "alertNotificationLog")
    @JoinColumn(name = "ALERT_ID", referencedColumnName = "ID")
    @OneToOne
    private Alert alert;

    protected AlertNotificationLog() {
    } // JPA

    public AlertNotificationLog(Alert alert) {
        AlertDefinition alertDefinition = alert.getAlertDefinition();

        StringBuilder rolesBuilder = new StringBuilder();
        StringBuilder subjectsBuilder = new StringBuilder();
        StringBuilder emailsBuilder = new StringBuilder();

        Set<AlertNotification> currentNotifications = alertDefinition.getAlertNotifications();
        for (AlertNotification notification : currentNotifications) {
            if (notification instanceof RoleNotification) {
                if (rolesBuilder.length() != 0) {
                    rolesBuilder.append(", ");
                }

                rolesBuilder.append(((RoleNotification) notification).getRole().getName());
            } else if (notification instanceof SubjectNotification) {
                if (subjectsBuilder.length() != 0) {
                    subjectsBuilder.append(", ");
                }

                subjectsBuilder.append(((SubjectNotification) notification).getSubject().getName());
            } else if (notification instanceof EmailNotification) {
                if (emailsBuilder.length() != 0) {
                    emailsBuilder.append(", ");
                }

                emailsBuilder.append(((EmailNotification) notification).getEmailAddress());
            } else {
                // TODO: log that this type of AlertNotification is not supported yet for auditing
            }
        }

        roles = rolesBuilder.toString();
        subjects = subjectsBuilder.toString();
        emails = emailsBuilder.toString();
        this.alert = alert;
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