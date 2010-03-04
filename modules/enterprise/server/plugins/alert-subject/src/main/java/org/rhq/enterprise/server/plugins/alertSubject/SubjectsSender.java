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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 */
public class SubjectsSender extends AlertSender {

    private final Log log = LogFactory.getLog(SubjectsSender.class);

    @Override
    public SenderResult send(Alert alert) {

        SenderResult noSubjects = new SenderResult(ResultState.FAILURE,"No subjects defined");

        PropertySimple subjectIdProp = alertParameters.getSimple("subjectId");
        if (subjectIdProp==null )
            return noSubjects;

        String subjectIdString = subjectIdProp.getStringValue();
        if (subjectIdString==null)
            return noSubjects;

        String[] subjectIds = subjectIdString.split(",");

        List<String> emails = new ArrayList<String>(subjectIds.length);
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        for (String subjectId : subjectIds ) {
            Subject subject = subjectMgr.getSubjectById(Integer.parseInt(subjectId));
            String emailAddress = subject.getEmailAddress();
            if (emailAddress!=null)
                emails.add(emailAddress);
            else
                if (log.isDebugEnabled())
                    log.debug("Subject " + subject.getId() + " has no email associated ");
        }

        return new SenderResult(ResultState.DEFERRED_EMAIL,"Sending to subject ids " + subjectIdString , emails);
    }
}
