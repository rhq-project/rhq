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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A template that consists of multiple notifications. This can be used to create
 * 'bundles' of preconfigured alert notifications which can then be applied to
 * AlertDefinitions directly or via AlertTemplates.
 *
 * @author Heiko W. Rupp
 */

@NamedQueries({
        @NamedQuery(name = NotificationTemplate.FIND_BY_NAME, query = "SELECT t FROM NotificationTemplate t WHERE t.name = :name"),
        @NamedQuery(name = NotificationTemplate.FIND_ALL, query = "SELECT t FROM NotificationTemplate t LEFT JOIN FETCH t.notifications")
})
@Entity
@Table(name="RHQ_ALERT_NOTIF_TEMPL")
@SequenceGenerator(name="RHQ_ALERT_NOTIF_TEMPL_ID_SEQ", sequenceName = "RHQ_ALERT_NOTIF_TEMPL_ID_SEQ")
public class NotificationTemplate implements Serializable {

    public static final String FIND_BY_NAME = "NotificationTemplate.findByName";
    public static final String FIND_ALL = "NotificationTemplate.findAll";


    @Column(name="ID", nullable = false)
    @GeneratedValue(generator = "RHQ_ALERT_NOTIF_TEMPL_ID_SEQ")
    @Id
    private int id;

    @Column(name="NAME")
    private String name;

    @Column(name="DESCRIPTION")
    private String description;


    @OneToMany(mappedBy = "notificationTemplate", cascade = CascadeType.ALL)
    List<AlertNotification> notifications = new ArrayList<AlertNotification>();

    @SuppressWarnings("unused")
    protected NotificationTemplate() {
        // for JPA
    }

    public NotificationTemplate(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationTemplate that = (NotificationTemplate) o;

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
