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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.seam.annotations.Create;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing Bean for the subjects sender alert sender plugin custom UI
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class SubjectsBackingBean extends CustomAlertSenderBackingBean {

    private final Log log = LogFactory.getLog(SubjectsBackingBean.class);

    private List<Subject> allSubjects;

    private Map<String, String> subjectsMap;
    private List<String> currentSubjects;
    private List<String> subjectsToRemove;
    private static final String SUBJECT_ID = "subjectId";
    private boolean isDebug;

    @Create
    public void init() {

        if (log.isDebugEnabled())
            isDebug = true;

        getAllSubjects();

        getSelectableSubjectsMap();
        fillSubjectsFromAlertParameters();
    }

    private void getAllSubjects() {
        SubjectManagerLocal mgr = LookupUtil.getSubjectManager();
        allSubjects = mgr.findAllSubjects(new PageControl());
    }

    private void fillSubjectsFromAlertParameters() {
        String rolesString = alertParameters.getSimpleValue(SUBJECT_ID,"");
        String[] subjects = rolesString.split(",");
        if (subjects.length==0)
            return;

        if (currentSubjects==null)
            currentSubjects = new ArrayList<String>();
        currentSubjects.addAll(Arrays.asList(subjects));
    }

    public List<String> getCurrentSubjects() {
        if (currentSubjects==null)
            fillSubjectsFromAlertParameters();
        return currentSubjects;
    }

    public void setCurrentSubjects(List<String> currentSubjects) {
        this.currentSubjects = currentSubjects;
    }

    public List<String> getSubjectsToRemove() {
        return subjectsToRemove;
    }

    public void setSubjectsToRemove(List<String> subjectsToRemove) {
        this.subjectsToRemove = subjectsToRemove;
    }


    public Map<String, String> getSelectableSubjectsMap() {

        if (subjectsMap == null) {
            subjectsMap = new HashMap<String, String>();

            if (allSubjects==null)
                getAllSubjects();

            if (currentSubjects==null)
                fillSubjectsFromAlertParameters();

            for (Subject subject : allSubjects) {
                String subjectId = String.valueOf(subject.getId());
                if (currentSubjects==null || !currentSubjects.contains(subjectId)) {
                    subjectsMap.put(subject.getName(), subjectId);
                }
            }
        }
        return subjectsMap;

    }

    public Map<String, String> getCurrentSubjectsMap() {

        Map<String,String> ret = new HashMap<String, String>();
        if (currentSubjects==null)
            return ret;

        for (Subject subject:allSubjects) {
            String subjectId = String.valueOf(subject.getId());
            if (currentSubjects.contains(subjectId))
                ret.put(subject.getName(), subjectId);
        }
        return ret;

    }

    public String addSubjects() {

        if (isDebug)
            log.debug("Selected subjects: " + currentSubjects);
        if (currentSubjects.isEmpty())
            return "ALERT_NOTIFICATIONS";

        String subjects="";
        for (String subject : currentSubjects) {
            subjects += subject;
            subjects += ",";
        }
        if (subjects.endsWith(","))
                subjects = subjects.substring(0,subjects.length()-1);

        PropertySimple p = alertParameters.getSimple(SUBJECT_ID);
        if (p==null) {
                p = new PropertySimple(SUBJECT_ID,subjects);
                alertParameters.put(p);
        }
        else
            p.setStringValue(subjects);

        alertParameters = persistConfiguration(alertParameters);

        fillSubjectsFromAlertParameters();

        return "ALERT_NOTIFICATIONS";
    }

    public String removeSubjects() {
        if (isDebug)
            log.debug("In remove subjects, " + subjectsToRemove);

        String subjects="";
        List<String> resulting = new ArrayList<String>(currentSubjects);
        resulting.removeAll(subjectsToRemove);

        for (String subject : resulting) {
            subjects += subject;
            subjects += ",";
        }

        if (subjects.endsWith(","))
            subjects = subjects.substring(0,subjects.length()-1);

        PropertySimple p = alertParameters.getSimple(SUBJECT_ID);
        if (p==null) {
            if (!resulting.isEmpty()) {
                p = new PropertySimple(SUBJECT_ID,subjects);
                alertParameters.put(p);
            }
        }
        else
            p.setStringValue(subjects);

        alertParameters = persistConfiguration(alertParameters);

        currentSubjects = resulting;

        fillSubjectsFromAlertParameters();

        return "ALERT_NOTIFICATIONS";
    }
}
