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
/*
 * GroupMonitoringConfigForm.java
 */

package org.rhq.enterprise.gui.legacy.action.resource.group.monitor.config;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.config.MonitoringConfigForm;

/**
 * Form for setting the collection interval for metrics in resource/group/monitoring/configuration areas of the
 * application, and for adding metrics to a group.
 */
public class GroupMonitoringConfigForm extends MonitoringConfigForm {
    /**
     * Holds value of property availabilityThreshold.
     */
    private String availabilityThreshold;

    /**
     * Holds value of property unavailabilityThreshold.
     */
    private String unavailabilityThreshold;

    /**
     * Creates new MonitoringConfigForm
     */
    public GroupMonitoringConfigForm() {
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.availabilityThreshold = "100";
        this.unavailabilityThreshold = "0";
        ;
        super.reset(mapping, request);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("availability: ").append(availabilityThreshold);
        buf.append(" unvailabilityThreshold: ").append(unavailabilityThreshold);
        return super.toString() + buf.toString();
    }

    /**
     * Getter for property availabilityThreshold.
     *
     * @return Value of property availabilityThreshold.
     */
    public String getAvailabilityThreshold() {
        return this.availabilityThreshold;
    }

    /**
     * Setter for property availabilityThreshold.
     *
     * @param availabilityThreshold New value of property availabilityThreshold.
     */
    public void setAvailabilityThreshold(String availabilityThreshold) {
        this.availabilityThreshold = availabilityThreshold;
    }

    /**
     * Getter for property unavailabilityThreshold.
     *
     * @return Value of property unavailabilityThreshold.
     */
    public String getUnavailabilityThreshold() {
        return this.unavailabilityThreshold;
    }

    /**
     * Setter for property unavailabilityThreshold.
     *
     * @param unavailabilityThreshold New value of property unavailabilityThreshold.
     */
    public void setUnavailabilityThreshold(String unavailabilityThreshold) {
        this.unavailabilityThreshold = unavailabilityThreshold;
    }
}