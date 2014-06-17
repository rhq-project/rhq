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
package org.rhq.enterprise.server.plugins.alertRoles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * AlertSender that notifies RHQ roles by gathering the email addresses of the
 * members of the given roles and putting them into the SenderResult object for
 * later delivery by the system.
 *
 * @author Heiko W. Rupp
 * @author Joseph Marques
 */
public class RolesSender extends AlertSender {

    @Override
    public SenderResult send(Alert alert) {
        List<Integer> roleIds = getRoleIdsFromConfiguration();
        if (roleIds == null) {
            return SenderResult.getSimpleFailure("No roles defined");
        }

        List<String> names = getRoleNames(roleIds);
        List<String> emails = getRoleEmails(roleIds);

        try {
            Set<String> uniqueEmails = new HashSet<String>(emails);
            Collection<String> badEmails = LookupUtil.getAlertManager()
                .sendAlertNotificationEmails(alert, uniqueEmails, null);

            List<String> goodEmails = new ArrayList<String>(uniqueEmails);
            goodEmails.removeAll(badEmails);

            SenderResult result = new SenderResult();
            result.setSummary("Target roles were: " + names);
            if (goodEmails.size() > 0) {
                result.addSuccessMessage("Successfully sent to: " + goodEmails);
            }
            if (badEmails.size() > 0) {
                result.addFailureMessage("Failed to send to: " + badEmails);
            }
            return result;
        } catch (Throwable t) {
            return SenderResult.getSimpleFailure("Error sending role notifications to " + names + ", cause: "
                + t.getMessage());
        }
    }

    @Override
    public String previewConfiguration() {
        List<Integer> roleIds = getRoleIdsFromConfiguration();
        if (roleIds == null || roleIds.size() == 0) {
            return "<empty>";
        }

        List<String> names = getRoleNames(roleIds);
        String nameString = names.toString();
        return nameString.substring(1, nameString.length() - 1);
    }

    private List<String> getRoleNames(List<Integer> roleIds) {
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        List<String> results = new ArrayList<String>();
        for (Integer nextRoleId : roleIds) {
            Role nextRole = roleManager.getRoleById(nextRoleId);
            if (nextRole != null) { // handle unknown role ids
                results.add(nextRole.getName());
            }
        }

        return results;
    }

    private List<String> getRoleEmails(List<Integer> roleIds) {
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        List<String> results = new ArrayList<String>();
        for (Integer nextRoleId : roleIds) {
            Role nextRole = roleManager.getRoleById(nextRoleId);
            if (nextRole != null) { // handle unknown role ids
                for (Subject nextSubject : nextRole.getSubjects()) {
                    String nextEmail = nextSubject.getEmailAddress();
                    if (nextEmail != null) {
                        results.add(nextEmail);
                    }
                }
            }
        }

        return results;
    }

    private List<Integer> getRoleIdsFromConfiguration() {
        PropertySimple roleIdProperty = alertParameters.getSimple("roleId");
        if (roleIdProperty == null) {
            return null;
        }

        String roleIdString = roleIdProperty.getStringValue();
        if (roleIdString == null || roleIdString.trim().equals("")) {
            return null;
        }

        return AlertSender.unfence(roleIdString, Integer.class);
    }
}
