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
     * Sends an email message to the given a set of email addresses where the subject and body of the message are passed
     * in. The message body is considered to have a MIME type of <code>text/plain</code>.
     *
     * <p>This method will attempt to send to all addresses, even if one or more failed. The first exception that is
     * encountered will be the one that is eventually thrown.</p>
     *
     * @param  toAddresses
     * @param  messageSubject
     * @param  messageBody
     *
     * @throws Exception if failed to send to one or more email addresses
     */
    void sendEmail(Collection<String> toAddresses, String messageSubject, String messageBody) throws Exception;

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
     * @return alert email message (key=subject, value=body)
     */
    Map<String, String> getAlertEmailMessage(String resourceHierarchy, String resourceName, String alertName,
        String priority, String timestamp, String conditionLogs, String alertUrl);
}