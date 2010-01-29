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
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.ResultState;
import org.rhq.enterprise.server.plugin.pc.alert.SenderResult;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * AlertSender that notifies RHQ roles by gathering the email addresses of the
 * members of the given roles and putting them into the SenderResult object for
 * later delivery by the system.
 *
 * @author Heiko W. Rupp
 */
public class RolesSender extends AlertSender {

    private final Log log = LogFactory.getLog(RolesSender.class);

    @Override
    public SenderResult send(Alert alert) {

        SenderResult noRecipients = new SenderResult(ResultState.FAILURE, "No recipient roles defined");

        PropertySimple rolesIdProp = alertParameters.getSimple("roleId");
        if (rolesIdProp==null)
            return noRecipients;

        String rolesIds = rolesIdProp.getStringValue();
        if (rolesIds==null)
            return noRecipients;

        String[] roles = rolesIds.split(",");
        if (roles.length==0)
            return noRecipients;

        List<String> emails = new ArrayList<String>();
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        for (String r: roles) {

            Role role = roleManager.getRole(overlord,Integer.parseInt(r));
            Set<Subject> subjects = role.getSubjects();
            for (Subject subject : subjects) {
                String email = subject.getEmailAddress();
                if (email!=null)
                    emails.add(email);
                else
                if (log.isDebugEnabled())
                    log.debug("Subject " + subject.getId() + " has no email associated ");
            }
        }

        return new SenderResult(ResultState.DEFERRED_EMAIL,"Sending to roles " + rolesIds,emails);
    }
}
