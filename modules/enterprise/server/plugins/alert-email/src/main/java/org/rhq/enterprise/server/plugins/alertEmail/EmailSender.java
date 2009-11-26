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

    @Override
    public SenderResult send(Alert alert) {

        String emailAddress = alertParameters.getSimpleValue("emailAddress",null);
        if (emailAddress==null) {
            log.warn("Email address was null, should not happen");
            return new SenderResult(ResultState.FAILURE,"No email address given");
        }

        String mailserver = preferences.getSimpleValue("mailserver","localhost"); // TODO get RHQ default one
        String senderAddress = preferences.getSimpleValue("senderEmail","rhqadmin@localhost"); // TODO get RHQ default one

        Properties props = new Properties();
        props.put("mail.smtp.host",mailserver);
        Session session = Session.getDefaultInstance(props); // TODO pass authenticator
        Message message = new SMTPMessage(session);
        try {
            message.setFrom(new InternetAddress(senderAddress));
            message.setRecipient(Message.RecipientType.TO,new InternetAddress("hwr@redhat.com"));
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
