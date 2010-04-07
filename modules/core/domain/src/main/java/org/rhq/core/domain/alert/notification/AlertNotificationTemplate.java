/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * An {@link AlertNotificationTemplate} is a group of {@link AlertNotification}. This can be used to create
 * pre-configured sets of notifications which can then be applied to all types of {@link AlertDefinition}s -
 * those at the {@link Resource}-level, {@link ResourceGroup}-level, and {@link ResourceType}-level (a.k.a
 * Alert Template).
 *
 * @author Heiko W. Rupp
 */

@NamedQueries( {
    @NamedQuery(name = AlertNotificationTemplate.FIND_BY_NAME, query = "SELECT t FROM AlertNotificationTemplate t WHERE t.name = :name"),
    @NamedQuery(name = AlertNotificationTemplate.FIND_ALL, query = "SELECT t FROM AlertNotificationTemplate AS t") })
@Entity
@Table(name = "RHQ_ALERT_NOTIF_TEMPL")
@SequenceGenerator(name = "RHQ_ALERT_NOTIF_TEMPL_ID_SEQ", sequenceName = "RHQ_ALERT_NOTIF_TEMPL_ID_SEQ")
public class AlertNotificationTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FIND_BY_NAME = "AlertNotificationTemplate.findByName";
    public static final String FIND_ALL = "AlertNotificationTemplate.findAll";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(generator = "RHQ_ALERT_NOTIF_TEMPL_ID_SEQ")
    @Id
    private int id;

    @Column(name = "CTIME")
    private long ctime;

    @Column(name = "MTIME")
    private long mtime;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @OneToMany(mappedBy = "alertNotificationTemplate", cascade = CascadeType.ALL)
    List<AlertNotification> notifications = new ArrayList<AlertNotification>();

    protected AlertNotificationTemplate() {
        // for JPA
    }

    public AlertNotificationTemplate(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return this.id;
    }

    public long getCtime() {
        return ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<AlertNotification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<AlertNotification> notifications) {
        this.notifications = notifications;
    }

    public List<AlertNotification> addNotification(AlertNotification notification) {
        this.notifications.add(notification);
        return this.notifications;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    @PreUpdate
    void onPreUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AlertNotificationTemplate that = (AlertNotificationTemplate) o;

        if (description != null ? !description.equals(that.description) : that.description != null)
            return false;
        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NotificationTemplate");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
