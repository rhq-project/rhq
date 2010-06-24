/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertSubject;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.ResultState;
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
            return new SenderResult(ResultState.FAILURE, "No subjects defined");
        }

        List<String> emails = getSubjectEmails(subjectIds);
        List<String> names = getSubjectNames(subjectIds);
        return new SenderResult(ResultState.DEFERRED_EMAIL, "Sending to subjects: " + names, emails);
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
