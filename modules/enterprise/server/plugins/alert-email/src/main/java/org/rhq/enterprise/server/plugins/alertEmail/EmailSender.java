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

import java.util.Arrays;
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
 * Class to send emails. Actually it does not do the work
 * itself, but just prepares the input which is then passed to
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

        // TODO shall we validate the emails here and return failure if they look
        // invalid? But then do we know what mail setup a user has and what may be
        // illegal in his case?

        String[] emails = emailAddress.split(",");

        return new SenderResult(ResultState.DEFERRED_EMAIL, "Emails prepared for sending", Arrays.asList(emails));
    }
}
