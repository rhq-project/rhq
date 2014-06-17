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
package org.rhq.enterprise.server.core;

import java.util.Collection;
import java.util.Map;
import javax.ejb.Local;

/**
 * Local interface that allows clients to send SMTP email messages.
 *
 * @author John Mazzitelli
 */
@Local
public interface EmailManagerLocal {
    /**
     * Send email to the addressses passed in toAddresses with the passed subject and body. Invalid emails will
     * be reported back. This can only catch sender errors up to the first smtp gateway.
     * @param  toAddresses list of email addresses to send to
     * @param  messageSubject subject of the email sent
     * @param  messageBody body of the email to be sent
     *
     * @return list of email receivers for which initial delivery failed.
     */
    Collection<String> sendEmail(Collection<String> toAddresses, String messageSubject, String messageBody);

    /**
     * This returns an email message based on the alert email template with its replacement tokens replaced with the
     * given strings. The returns Map has size 1 - the key is the subject and the value is the body of the message.
     *
     * @param  resourceHierarchy ascii tree structure containing the names of resources up to the corresponding platform
     * @param  resourceName      name of the resource that triggered the alert
     * @param  alertName         name of the alert that was triggered
     * @param  priority          the priority of the alert
     * @param  timestamp         the date/time when the alert was triggered
     * @param  conditionLogs     that conditionLogs that were met that caused the alert to trigger
     * @param  alertUrl          URL to the GUI page of the alert that was triggered
     *
     * @param templatePath
     * @return alert email message (key=subject, value=body)
     */
    Map<String, String> getAlertEmailMessage(String resourceHierarchy, String resourceName, String alertName,
                                             String priority, String timestamp, String conditionLogs, String alertUrl,
                                             String templatePath);
}