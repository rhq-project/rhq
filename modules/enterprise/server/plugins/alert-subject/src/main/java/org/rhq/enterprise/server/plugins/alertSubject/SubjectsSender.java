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
package org.rhq.enterprise.server.plugins.alertSubject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * AlertSender that notifies RHQ subjects by gathering the email addresses of the
 * subjects and putting them into the SenderResult object for later delivery
 * by the system.
 *
 * @author Heiko W. Rupp
 * @author Joseph Marques
 */
public class SubjectsSender extends AlertSender {

    @Override
    public SenderResult send(Alert alert) {
        List<Integer> subjectIds = getSubjectIdsFromConfiguration();
        if (subjectIds == null) {
            return SenderResult.getSimpleFailure("No subjects defined");
        }

        List<String> names = getSubjectNames(subjectIds);
        List<String> emails = getSubjectEmails(subjectIds);

        try {
            Set<String> uniqueEmails = new HashSet<String>(emails);
            Collection<String> badEmails = LookupUtil.getAlertManager()
                .sendAlertNotificationEmails(alert, uniqueEmails, null);

            List<String> goodEmails = new ArrayList<String>(uniqueEmails);
            goodEmails.removeAll(badEmails);

            SenderResult result = new SenderResult();
            result.setSummary("Target subjects were: " + names);
            if (goodEmails.size() > 0) {
                result.addSuccessMessage("Successfully sent to: " + goodEmails);
            }
            if (badEmails.size() > 0) {
                result.addFailureMessage("Failed to send to: " + badEmails);
            }
            return result;
        } catch (Throwable t) {
            return SenderResult.getSimpleFailure("Error sending subject notifications to " + names + ", cause: "
                + t.getMessage());
        }
    }

    @Override
    public String previewConfiguration() {
        List<Integer> subjectIds = getSubjectIdsFromConfiguration();
        if (subjectIds == null || subjectIds.size() == 0) {
            return "<empty>";
        }

        List<String> names = getSubjectNames(subjectIds);
        String nameString = names.toString();
        return nameString.substring(1, nameString.length() - 1);
    }

    private List<String> getSubjectNames(List<Integer> subjectIds) {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        List<String> results = new ArrayList<String>();
        for (Integer nextSubjectId : subjectIds) {
            Subject nextSubject = subjectManager.getSubjectById(nextSubjectId);
            if (nextSubject != null) { // handle unknown subject ids
                results.add(nextSubject.getName());
            }
        }

        return results;
    }

    private List<String> getSubjectEmails(List<Integer> subjectIds) {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        List<String> results = new ArrayList<String>();
        for (Integer nextSubjectId : subjectIds) {
            Subject nextSubject = subjectManager.getSubjectById(nextSubjectId);
            if (nextSubject != null) { // handle unknown subject ids
                String nextEmail = nextSubject.getEmailAddress();
                if (nextEmail != null) {
                    results.add(nextEmail);
                }
            }
        }

        return results;
    }

    private List<Integer> getSubjectIdsFromConfiguration() {
        PropertySimple subjectIdProperty = alertParameters.getSimple("subjectId");
        if (subjectIdProperty == null) {
            return null;
        }

        String subjectIdString = subjectIdProperty.getStringValue();
        if (subjectIdString == null || subjectIdString.trim().equals("")) {
            return null;
        }

        return AlertSender.unfence(subjectIdString, Integer.class);
    }
}
