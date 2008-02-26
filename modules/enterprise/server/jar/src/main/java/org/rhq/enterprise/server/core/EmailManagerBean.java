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

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;

/**
 * EJB interface to an SMTP email system.
 *
 * @author John Mazzitelli
 */
@Stateless
public class EmailManagerBean implements EmailManagerLocal {
    private static final Log LOG = LogFactory.getLog(EmailManagerBean.class);

    /**
     * The token string found in the email template file that will be replaced with an ascii tree structure containing
     * the names of resources up to the corresponding platform
     */
    private static final String TEMPLATE_TOKEN_RESOURCE_NAME = "@@@RESOURCE_NAME@@@";

    /**
     * The token string found in the email template file that will be replaced with a resource name.
     */
    private static final String TEMPLATE_TOKEN_FULL_RESOURCE_HIERARCHY = "@@@FULL_RESOURCE_HIERARCHY@@@";

    /**
     * The token string found in the email template file that will be replaced with an alert name.
     */
    private static final String TEMPLATE_TOKEN_ALERT_NAME = "@@@ALERT_NAME@@@";

    /**
     * The token string found in the email template file that will be replaced with a priority string.
     */
    private static final String TEMPLATE_TOKEN_PRIORITY = "@@@PRIORITY@@@";

    /**
     * The token string found in the email template file that will be replaced with a timestamp.
     */
    private static final String TEMPLATE_TOKEN_TIMESTAMP = "@@@TIMESTAMP@@@";

    /**
     * The token string found in the email template file that will be replaced with a condition set string.
     */
    private static final String TEMPLATE_TOKEN_CONDITIONS = "@@@CONDITIONS@@@";

    /**
     * The token string found in the email template file that will be replaced with a URL to a specific alert.
     */
    private static final String TEMPLATE_TOKEN_ALERT_URL = "@@@ALERT_URL@@@";

    @Resource(mappedName = "java:/Mail")
    private Session mailSession;

    public void sendEmail(Collection<String> toAddresses, String messageSubject, String messageBody) throws Exception {
        MimeMessage mimeMessage = new MimeMessage(mailSession);
        mimeMessage.setSubject(messageSubject);
        mimeMessage.setContent(messageBody, "text/plain");

        Exception error = null;

        // Send to each recipient individually, do not throw exceptions until we try them all
        for (String toAddress : toAddresses) {
            try {
                LOG.debug("Sending email [" + messageSubject + "] to recipient [" + toAddress + "]");
                InternetAddress recipient = new InternetAddress(toAddress);
                Transport.send(mimeMessage, new InternetAddress[] { recipient });
            } catch (Exception e) {
                LOG.error("Failed to send email [" + messageSubject + "] to recipient [" + toAddress + "]", e);

                // Remember the first error - in case its due to a session initialization problem,
                // we don't want to lose the first error.
                if (error == null) {
                    error = e;
                }
            }
        }

        if (error != null) {
            throw error;
        }

        return;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> getAlertEmailMessage(String resourceHierarchy, String resourceName, String alertName,
        String priority, String timestamp, String conditionLogs, String alertUrl) {
        InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream("alert-email-template.txt");
        String template = new String(StreamUtil.slurp(templateStream));

        template = template.replaceAll(TEMPLATE_TOKEN_FULL_RESOURCE_HIERARCHY,
            (resourceHierarchy != null) ? resourceHierarchy : "?Unknown Resource Hierarchy?");

        template = template.replaceAll(TEMPLATE_TOKEN_RESOURCE_NAME, (resourceName != null) ? resourceName
            : "?Unknown Resource?");

        template = template.replaceAll(TEMPLATE_TOKEN_ALERT_NAME, (alertName != null) ? alertName : "?Unknown Alert?");

        template = template.replaceAll(TEMPLATE_TOKEN_PRIORITY, (priority != null) ? priority : "!! - Medium");

        template = template.replaceAll(TEMPLATE_TOKEN_TIMESTAMP, (timestamp != null) ? timestamp : new Date()
            .toString());

        template = template.replaceAll(TEMPLATE_TOKEN_CONDITIONS, (conditionLogs != null) ? conditionLogs
            : "?Unknown ConditionLogs?");

        template = template.replaceAll(TEMPLATE_TOKEN_ALERT_URL, (alertUrl != null) ? alertUrl : "?Unknown URL?");

        String subject = "[" + RHQConstants.PRODUCT_NAME + "] Alert";

        if (template.startsWith("Subject:")) {
            try {
                subject = template.substring("Subject:".length(), template.indexOf('\n'));
            } catch (RuntimeException ignore) {
                LOG.warn("Bad alert template file - can't determine the subject, using a default");
            }
        }

        Map<String, String> message = new HashMap<String, String>(1);
        message.put(subject, template);

        return message;
    }
}