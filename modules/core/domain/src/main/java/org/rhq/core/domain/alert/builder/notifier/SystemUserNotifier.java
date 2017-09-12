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

import java.util.LinkedList;
import java.util.List;

import org.rhq.core.domain.alert.builder.AlertNotificationTemplate;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Michael Burman
 */
public class SystemUserNotifier extends AlertNotificationTemplate {
    private List<String> recipientList = new LinkedList<String>();

    public SystemUserNotifier addRecipient(String userName) {
        recipientList.add(userName);
        return this;
    }

    @Override
    public AlertNotification getAlertNotification() {
        this.sender("SubjectsSender");
        Configuration configuration = new Configuration();
        StringBuilder sb = new StringBuilder();
        for (String s : recipientList) {
            sb.append(s);
            sb.append("|");
        }
        sb.deleteCharAt(sb.length() - 1);

        configuration.setSimpleValue("subjectId", sb.toString());
        configuration(configuration);
        return super.getAlertNotification();
    }


}
