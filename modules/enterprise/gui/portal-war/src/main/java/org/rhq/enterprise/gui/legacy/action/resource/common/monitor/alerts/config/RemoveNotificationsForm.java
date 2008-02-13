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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import org.rhq.enterprise.gui.legacy.action.resource.ResourceForm;

/**
 * Form for removing various notification types for alert definitions
 */
public class RemoveNotificationsForm extends ResourceForm {
    public static final String ROLES = "Roles";
    public static final String USERS = "Users";
    public static final String OTHERS = "Others";

    private Integer ad;

    private Integer[] users;
    private Integer[] roles;
    private Integer[] emails;

    public RemoveNotificationsForm() {
    }

    public Integer getAd() {
        return this.ad;
    }

    public void setAd(Integer ad) {
        this.ad = ad;
    }

    public Integer[] getUsers() {
        return this.users;
    }

    public void setUsers(Integer[] users) {
        this.users = users;
    }

    public Integer[] getRoles() {
        return this.roles;
    }

    public void setRoles(Integer[] roles) {
        this.roles = roles;
    }

    public Integer[] getEmails() {
        return this.emails;
    }

    public void setEmails(Integer[] emails) {
        this.emails = emails;
    }

    public boolean isAlertTemplate() {
        return ((getType() != null) && (getType() != 0));
    }
}