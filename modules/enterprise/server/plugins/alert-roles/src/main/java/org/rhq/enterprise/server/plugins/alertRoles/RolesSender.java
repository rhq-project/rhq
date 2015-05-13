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
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
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
                .sendAlertNotificationEmails(alert, uniqueEmails);

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
        List<String> results = new ArrayList<String>();

        if (null == roleIds || roleIds.isEmpty()) {
            return results;
        }

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        RoleCriteria c = new RoleCriteria();
        c.addFilterIds(roleIds.toArray(new Integer[roleIds.size()]));
        c.fetchSubjects(true);
        List<Role> roles = roleManager.findRolesByCriteria(subjectManager.getOverlord(), c);

        for (Role role : roles) {
            for (Subject subject : role.getSubjects()) {
                String email = subject.getEmailAddress();
                if (email != null) {
                    results.add(email);
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
