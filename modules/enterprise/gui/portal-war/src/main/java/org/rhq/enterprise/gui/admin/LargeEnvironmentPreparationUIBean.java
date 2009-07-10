/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.admin;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class LargeEnvironmentPreparationUIBean {
    private final Log log = LogFactory.getLog(LargeEnvironmentPreparationUIBean.class);

    public static final String MANAGED_BEAN_NAME = "LargeEnvironmentPreparationUIBean";

    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();

    public LargeEnvironmentPreparationUIBean() {
    }

    public String disableMeasurementTemplates() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        log.warn(subject.getName() + " has requested to disable measurement templates.");

        try {
            measurementScheduleManager.disableAllDefaultCollections(subject);

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "Collection intervals for all Measurement Templates have been disabled. "
                    + "This means any new resources that will be imported in the future "
                    + "will, by default, not collect measurements. Current resources already "
                    + "in inventory have not been affected.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to disable all measurement templates. Cause: " + ThrowableUtil.getAllMessages(e));
        }

        return "success";
    }

    public String disableMeasurementSchedules() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        log.warn(subject.getName() + " has requested to disable current measurement schedules.");

        try {
            measurementScheduleManager.disableAllSchedules(subject);

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "Collection intervals for all current resources in inventory have been disabled. "
                    + "This means all resources will no longer collect measurements. "
                    + "Resources that you import into inventory in the future will collect measurements "
                    + "unless you also disabled the templates.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to disable all resource measurement schedules. Cause: " + ThrowableUtil.getAllMessages(e));
        }

        return "success";
    }
}