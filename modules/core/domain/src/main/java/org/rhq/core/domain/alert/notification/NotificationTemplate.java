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
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * A template that consists of multiple notifications. This can be used to create
 * 'bundles' of preconfigured alert notifications which can then be applied to
 * AlertDefinitions directly or via AlertTemplates.
 * @author Heiko W. Rupp
 */
@Entity
@Table(name="RHQ_ALERT_NOTIF_TEMPL")
@SequenceGenerator(name="RHQ_ALERT_NOTIF_SEQ", sequenceName = "RHQ_ALERT_NOTIF_SEQ")
public class NotificationTemplate implements Serializable {

    @Column(name="ID", nullable = false)
    @GeneratedValue(generator = "RHQ_ALERT_NOTIF_SEQ")
    @Id
    private int id;

    @Column(name="NAME")
    private String name;

    @Column(name="DESCRIPTION")
    private String description;


 //   @OneToMany(cascade = CascadeType.ALL)
    @Transient
    List<AlertNotification> notifications = new ArrayList<AlertNotification>();

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

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
