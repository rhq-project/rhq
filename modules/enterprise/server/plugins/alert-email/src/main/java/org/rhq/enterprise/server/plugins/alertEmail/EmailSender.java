/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.plugins.alertEmail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Class to send emails. Actually it does not do the work
 * itself, but just prepares the input which is then passed to
 * @author Heiko W. Rupp
 * @author Joseph Marques
 */
public class EmailSender extends AlertSender {

    @Override
    public SenderResult send(Alert alert) {
        String emailAddressString = alertParameters.getSimpleValue("emailAddress", null);
        if (emailAddressString == null) {
            return SenderResult.getSimpleFailure("No email address given");
        }

        String templatePath = null;
        String templateName = alertParameters.getSimpleValue("template");
        if (templateName!=null) {
            String baseDir = System.getProperty("rhq.server.home") +
                File.separator +
                "alert-email-templates";
            templatePath = baseDir + File.separator + templateName;
        }

        List<String> emails = AlertSender.unfence(emailAddressString, String.class, ",");
        try {
            Set<String> uniqueEmails = new HashSet<String>(emails);
            Collection<String> badEmails = LookupUtil.getAlertManager()
                .sendAlertNotificationEmails(alert, uniqueEmails, templatePath);

            List<String> goodEmails = new ArrayList<String>(uniqueEmails);
            goodEmails.removeAll(badEmails);

            SenderResult result = new SenderResult();
            result.setSummary("Target addresses were: " + uniqueEmails);
            if (goodEmails.size() > 0) {
                result.addSuccessMessage("Successfully sent to: " + goodEmails);
            }
            if (badEmails.size() > 0) {
                result.addFailureMessage("Failed to send to: " + badEmails);
            }
            return result;
        } catch (Throwable t) {
            return SenderResult.getSimpleFailure("Error sending email notifications to " + emails + ", cause: "
                + t.getMessage());
        }

    }

    @Override
    public String previewConfiguration() {
        String emailAddressString = alertParameters.getSimpleValue("emailAddress", null);
        if (emailAddressString == null || emailAddressString.trim().length() == 0) {
            return "<empty>";
        }
        return emailAddressString;
    }
}
