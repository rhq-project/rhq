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
package org.rhq.enterprise.server.plugins.alertEmail;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import com.sun.mail.smtp.SMTPMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.ResultState;
import org.rhq.enterprise.server.plugin.pc.alert.SenderResult;

/**
 * Class to send emails.
 * @author Heiko W. Rupp
 */
public class EmailSender extends AlertSender {

    private final Log log = LogFactory.getLog(EmailSender.class);
    private static final String SMTP_HOST = "rhq.server.email.smtp-host";
    private static final String SMTP_PORT = "rhq.server.email.smtp-port";
    private static final String EMAIL_FROM = "rhq.server.email.from-address";

    @Override
    public SenderResult send(Alert alert) {

        String emailAddress = alertParameters.getSimpleValue("emailAddress",null);
        if (emailAddress==null) {
            log.warn("Email address was null, should not happen");
            return new SenderResult(ResultState.FAILURE,"No email address given");
        }

        String tmp = System.getProperty(SMTP_HOST,"localhost");
        String mailserver = preferences.getSimpleValue("mailserver",tmp);
        tmp = System.getProperty(EMAIL_FROM, "rhqadmin@localhost");
        String senderAddress = preferences.getSimpleValue("senderEmail",tmp);

        Properties props = new Properties();
        props.put("mail.smtp.host",mailserver);
        props.put("mail.transport.protocol","smtp");
        props.put("mail.host",mailserver);
        // TODO for whatever reason, the passed props are ignored and 'localhost' is used
        Session session = Session.getInstance(props); // TODO pass authenticator
        Message message = new SMTPMessage(session);
        try {
            message.setFrom(new InternetAddress(senderAddress));
            message.setRecipient(Message.RecipientType.TO,new InternetAddress(emailAddress));
            message.setSubject("Alert on " + "Dummy - TODO");
            message.setText("Li la lu, nur der Mann im Mond schaut zu");
            Transport.send(message);
        }
        catch (Exception e) {
            log.warn("Sending of email failed: " + e);
            return new SenderResult(ResultState.FAILURE,"Send failed: " + e.getMessage());
        }
        return new SenderResult(ResultState.SUCCESS, "Emails sent to XXXX");
    }
}
