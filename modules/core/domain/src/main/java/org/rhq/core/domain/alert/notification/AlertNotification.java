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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.configuration.Configuration;

@Entity
@NamedQueries( {
    @NamedQuery(name = AlertNotification.DELETE_BY_ID, query = "DELETE FROM AlertNotification an WHERE an.id IN ( :ids )"),
    @NamedQuery(name = AlertNotification.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM AlertNotification an WHERE an.alertDefinition IN ( SELECT ad FROM AlertDefinition ad WHERE ad.resource.id IN ( :resourceIds ) )"),
    @NamedQuery(name = AlertNotification.QUERY_DELETE_ORPHANED, query = "DELETE FROM AlertNotification an WHERE an.alertDefinition IS NULL AND an.alertNotificationTemplate IS NULL") })
@SequenceGenerator(name = "RHQ_ALERT_NOTIFICATION_ID_SEQ", sequenceName = "RHQ_ALERT_NOTIFICATION_ID_SEQ")
@Table(name = "RHQ_ALERT_NOTIFICATION")
public class AlertNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DELETE_BY_ID = "AlertNotification.deleteById";
    public static final String QUERY_DELETE_BY_RESOURCES = "AlertNotification.deleteByResources";
    public static final String QUERY_DELETE_ORPHANED = "AlertNotification.deleteOrphaned";

    @Transient
    transient int alertDefinitionId;
    @Transient
    transient int alertNotificationId;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ALERT_NOTIFICATION_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "ALERT_DEFINITION_ID")
    @ManyToOne
    private AlertDefinition alertDefinition;

    @JoinColumn(name = "TEMPLATE_ID")
    @ManyToOne
    private AlertNotificationTemplate alertNotificationTemplate;

    @JoinColumn(name = "SENDER_CONFIG_ID", referencedColumnName = "ID")
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    private Configuration configuration;

    @Column(name = "SENDER_NAME")
    private String senderName;

    protected AlertNotification() {
    } // JPA spec

    public AlertNotification(AlertDefinition alertDefinition) {
        if (alertDefinition == null) {
            throw new IllegalArgumentException("alertDefinition must be non-null.");
        }

        this.alertDefinition = alertDefinition;
    }

    public AlertNotification(AlertDefinition alertDefinition, Configuration config) {
        if (alertDefinition == null) {
            throw new IllegalArgumentException("alertDefinition must be non-null.");
        }

        this.alertDefinition = alertDefinition;
        this.configuration = config.deepCopy();
    }

    public AlertNotification(AlertNotification source, boolean copyIds) {
        if (copyIds) {
            this.id = source.id;
            this.configuration = source.configuration;
        } else {
            this.configuration = source.configuration.deepCopy(false);
        }
        this.senderName = source.senderName;
    }

    /**
     * Constructor only for transient usage
     * @param alertDefinitionId
     * @param alertNotificationId
     */
    public AlertNotification(int alertDefinitionId, int alertNotificationId) {
        this.alertDefinitionId = alertDefinitionId;
        this.alertNotificationId = alertNotificationId;
    }

    public AlertNotification(String sender) {
        this.senderName = sender;
    }

    public int getId() {
        return id;
    }

    @NotNull
    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    public void setAlertDefinition(AlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    public void prepareForOrphanDelete() {
        this.alertDefinition = null;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public AlertNotificationTemplate getAlertNotificationTemplate() {
        return alertNotificationTemplate;
    }

    public void setAlertNotificationTemplate(AlertNotificationTemplate alertNotificationTemplate) {
        this.alertNotificationTemplate = alertNotificationTemplate;
    }

    public int getAlertDefinitionId() {
        return alertDefinitionId;
    }

    public int getAlertNotificationId() {
        return alertNotificationId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AlertNotification");
        sb.append("{alertDefinitionId=").append(alertDefinitionId);
        sb.append(", alertNotificationId=").append(alertNotificationId);
        sb.append(", id=").append(id);
        sb.append(", notificationTemplate=").append(alertNotificationTemplate);
        sb.append(", senderName='").append(senderName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
