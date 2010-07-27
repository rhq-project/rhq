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

        List<String> emails = AlertSender.unfence(emailAddressString, String.class, ",");
        try {
            Set<String> uniqueEmails = new HashSet<String>(emails);
            Collection<String> badEmails = LookupUtil.getAlertManager()
                .sendAlertNotificationEmails(alert, uniqueEmails);

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
