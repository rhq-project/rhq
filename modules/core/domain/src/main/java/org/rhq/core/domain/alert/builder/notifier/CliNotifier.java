/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.alert.builder.notifier;

import org.rhq.core.domain.alert.builder.AlertNotificationTemplate;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Michael Burman
 */
public class CliNotifier extends AlertNotificationTemplate {

    private Integer packageId = null;
    private Integer repositoryId = null;
    private Integer userId = null;
    private String userName = null;
    private String userPassword = null;

    public CliNotifier packageId(Integer packageId) {
        this.packageId = packageId;
        return this;
    }

    public CliNotifier repositoryId(Integer repositoryId) {
        this.repositoryId = repositoryId;
        return this;
    }

    public CliNotifier userId(Integer userId) {
        this.userId = userId;
        return this;
    }

    public CliNotifier userName(String userName) {
        this.userName = userName;
        return this;
    }

    public CliNotifier password(String userPassword) {
        this.userPassword = userPassword;
        return this;
    }

    @Override
    public AlertNotification getAlertNotification() {
        sender("CliSender");

        Configuration configuration = new Configuration();

        if(userName != null) {
            if(userPassword == null) {
                throw new IllegalArgumentException("Password must be specified if custom user is given");
            }
            configuration.setSimpleValue("userName", userName);
            configuration.setSimpleValue("userPassword", userPassword);
        } else {
            PropertySimple user = new PropertySimple("userId", userId);
            configuration.put(user);
        }

        if(repositoryId == null || packageId == null) {
            throw new IllegalArgumentException("Need a repository and a package to run");
        } else {
            PropertySimple repoId = new PropertySimple("repoId", repositoryId);
            PropertySimple packId = new PropertySimple("packageId", packageId);
            configuration.put(repoId);
            configuration.put(packId);
        }

        return super.getAlertNotification();
    }
}
