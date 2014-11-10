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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * EJB interface to an SMTP email system.
 *
 * @author John Mazzitelli
 */
@Stateless
public class EmailManagerBean implements EmailManagerLocal {
    private static final Log LOG = LogFactory.getLog(EmailManagerBean.class);

    /**
     * The token string found in the email template file that will be replaced with a resource name.
     */
    private static final String TEMPLATE_TOKEN_RESOURCE_NAME = "@@@RESOURCE_NAME@@@";

    /**
     * The token string found in the email template file that will be replaced with an ascii tree structure containing
     * the names of resources up to the corresponding platform
     */
    private static final String TEMPLATE_TOKEN_RESOURCE_HIERARCHY = "@@@FULL_RESOURCE_HIERARCHY@@@";

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

    private static final String TEMPLATE_TOKEN_PRODUCT_NAME = "@@@PRODUCT_NAME@@@";

    @Resource(mappedName = "java:jboss/mail/Default")
    private Session mailSession;

    /**
     * Send email to the addressses passed in toAddresses with the passed subject and body. Invalid emails will
     * be reported back. This can only catch sender errors up to the first smtp gateway.
     * @param  toAddresses list of email addresses to send to
     * @param  messageSubject subject of the email sent
     * @param  messageBody body of the email to be sent
     *
     * @return list of email receivers for which initial delivery failed.
     */
    public Collection<String> sendEmail(Collection<String> toAddresses, String messageSubject, String messageBody) {

        MimeMessage mimeMessage = new MimeMessage(mailSession);
        try {
            String fromAddress = System.getProperty("rhq.server.email.from-address");
            if (fromAddress!=null) {
                InternetAddress from = new InternetAddress(fromAddress);
                mimeMessage.setFrom(from);
            } else {
                LOG.warn("No email sender address set in rhq-server.properties [rhq.server.email.from-address]");
            }

            mimeMessage.setSubject(messageSubject);
            mimeMessage.setContent(messageBody, "text/plain");
        } catch (MessagingException e) {
            e.printStackTrace(); // TODO: Customise this generated block
            return toAddresses;
        }

        Exception error = null;
        Collection<String> badAdresses = new ArrayList<String>(toAddresses.size());

        // Send to each recipient individually, do not throw exceptions until we try them all
        for (String toAddress : toAddresses) {
            try {
                LOG.debug("Sending email [" + messageSubject + "] to recipient [" + toAddress + "]");
                InternetAddress recipient = new InternetAddress(toAddress);
                Transport.send(mimeMessage, new InternetAddress[] { recipient });
            } catch (Exception e) {
                LOG.error("Failed to send email [" + messageSubject + "] to recipient [" + toAddress + "]: "
                    + e.getMessage());
                badAdresses.add(toAddress);

                // Remember the first error - in case its due to a session initialization problem,
                // we don't want to lose the first error.
                if (error == null) {
                    error = e;
                }
            }
        }

        if (error != null) {
            LOG.error("Sending of emails failed for this reason: " + error.getMessage());
        }

        return badAdresses;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> getAlertEmailMessage(String resourceHierarchy, String resourceName, String alertName,
        String priority, String timestamp, String conditionLogs, String alertUrl) {
        InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream("alert-email-template.txt");
        String template = new String(StreamUtil.slurp(templateStream));

        String productName = LookupUtil.getSystemManager().getProductInfo(LookupUtil.getSubjectManager().getOverlord())
            .getFullName();

        // the resource hierarchy could have backslash characters from new lines and/or resource names
        template = template.replaceAll(TEMPLATE_TOKEN_RESOURCE_HIERARCHY, cleanse(resourceHierarchy,
            "?Unknown Resource Hierarchy?"));

        // resource names will have backslashes in them when they represent some windows file system service
        template = template.replaceAll(TEMPLATE_TOKEN_RESOURCE_NAME, cleanse(resourceName, "?Unknown Resource?"));

        // nothing preventing a user from creating an alert definition named "my\cool?definition"
        template = template.replaceAll(TEMPLATE_TOKEN_ALERT_NAME, cleanse(alertName, "?Unknown Alert?"));

        //if the priority enum for alerts changes in the future, we'll be safe
        template = template.replaceAll(TEMPLATE_TOKEN_PRIORITY, cleanse(priority, "Medium"));

        // better to be paranoid and on the safe side than risk it just to save one line of code
        template = template.replaceAll(TEMPLATE_TOKEN_TIMESTAMP, cleanse(timestamp, new Date().toString()));

        // if replacements lookup from the message bundle fails, these will look like "?some.dot.delimited.property?"
        template = template.replaceAll(TEMPLATE_TOKEN_CONDITIONS, cleanse(conditionLogs, "?Unknown Condition Logs?"));

        // better to be paranoid and on the safe side than risk it just to save one line of code
        template = template.replaceAll(TEMPLATE_TOKEN_ALERT_URL, cleanse(alertUrl, "?Unknown URL?"));

        template = template.replaceAll(TEMPLATE_TOKEN_PRODUCT_NAME, cleanse(productName, "RHQ"));

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

    /*
     * if we don't escape the regex special characters '\' and '$', they will be interpreted differently
     * than desired; quoteReplacement was specifically written to help alleviate this common scenario:
     *
     *    http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6523151
     */
    private String cleanse(String passedValue, String defaultValue) {
        String results = passedValue;
        if (results == null) {
            results = defaultValue;
        }
        // cleanse no matter what, because it's possible the defaultValue has invalid characters too
        return Matcher.quoteReplacement(results);
    }
}