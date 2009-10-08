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
package org.rhq.enterprise.gui.measurement.tables;

import javax.faces.application.FacesMessage;

import org.rhq.core.gui.util.FacesContextUtility;

/**
 * @author jay shaughnessy
 */
public class MetricsTableChartUIBean {

    public static final String MANAGED_BEAN_NAME = "MetricsTableChartUIBean";

    public String chartSelected() {
        String[] selectedMetrics = FacesContextUtility.getRequest().getParameterValues("selectedMetrics");

        if ((selectedMetrics == null) || (selectedMetrics.length < 1)) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "Select 1 or more metrics to chart");
        }

        return "chartMetrics";
    }

    public String getSelected() {
        String[] selectedMetrics = FacesContextUtility.getRequest().getParameterValues("selectedMetrics");

        StringBuilder sb = new StringBuilder();
        for (String scheduleIdString : selectedMetrics) {
            sb.append("&m=");
            sb.append(scheduleIdString);
        }

        return sb.toString();
    }

}
