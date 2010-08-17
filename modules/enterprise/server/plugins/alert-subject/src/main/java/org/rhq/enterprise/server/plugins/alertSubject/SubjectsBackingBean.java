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
package org.rhq.enterprise.server.plugins.alertSubject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing Bean for the subjects sender alert sender plugin custom UI
 * @author Joseph Marques
 */
public class SubjectsBackingBean extends CustomAlertSenderBackingBean {

    private Map<String, String> available;
    private List<String> currentSubjects;
    private static final String SUBJECT_ID = "subjectId";

    @Override
    public void loadView() {
        // get available/all subjects
        List<Subject> allSubjects = LookupUtil.getSubjectManager().findAllSubjects(new PageControl());
        available = new HashMap<String, String>();
        for (Subject subject : allSubjects) {
            String subjectId = String.valueOf(subject.getId());
            available.put(subject.getName(), subjectId);
        }

        // get current subjects
        String subjectString = alertParameters.getSimpleValue(SUBJECT_ID, "");
        currentSubjects = AlertSender.unfence(subjectString, String.class);
    }

    @Override
    public void saveView() {
        String subjectIds = AlertSender.fence(currentSubjects);

        PropertySimple p = alertParameters.getSimple(SUBJECT_ID);
        if (p == null) {
            p = new PropertySimple(SUBJECT_ID, subjectIds);
            alertParameters.put(p);
        } else {
            p.setStringValue(subjectIds);
        }

        alertParameters = persistConfiguration(alertParameters);
    }

    public List<String> getCurrentSubjects() {
        return currentSubjects;
    }

    public void setCurrentSubjects(List<String> currentSubjects) {
        this.currentSubjects = currentSubjects;
    }

    public Map<String, String> getAvailableSubjectsMap() {
        return available;
    }

}
