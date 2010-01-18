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
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing Bean for the subjects sender alert sender plugin custom UI
 * @author Heiko W. Rupp
 */
public class SubjectsBackingBean  {

    private final Log log = LogFactory.getLog(SubjectsBackingBean.class);

    private Map<String, String> subjectsMap;
    private Set<String> currentSubjects;

    public Set<String> getCurrentSubjects() {
        return currentSubjects;
    }

    public void setCurrentSubjects(Set<String> currentSubjects) {
        this.currentSubjects = currentSubjects;
    }

    public Map<String, String> getSubjectsMap() {

        if (subjectsMap == null) {
            subjectsMap = new HashMap<String, String>();


            SubjectManagerLocal mgr = LookupUtil.getSubjectManager();
            PageList<Subject> subjectList = mgr.findAllSubjects(new PageControl());
            Map<String,String> ret = new HashMap<String, String>();
            for (Subject subject : subjectList) {
                subjectsMap.put(subject.getName(),subject.getName());
            }
        }
        return subjectsMap;

    }

    public String submit() {
        // TODO: Customise this generated block

        return "ALERT_NOTIFICATIONS";
    }
}
