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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts;

import org.rhq.enterprise.gui.legacy.action.resource.ResourceForm;

/**
 * A subclass of <code>ResourceForm</code> representing the <em>RemoveAlert</em> form.
 */
public class RemoveForm extends ResourceForm {
    /**
     * Holds value of alerts.
     */
    private Integer[] alerts;
    private Integer ad;

    public RemoveForm() {
    }

    public String toString() {
        if (alerts == null) {
            return "empty";
        } else {
            return alerts.toString();
        }
    }

    /**
     * Getter for alerts
     *
     * @return alerts in an array
     */
    public Integer[] getAlerts() {
        return this.alerts;
    }

    /**
     * Setter for alerts
     *
     * @param alerts As an Integer array
     */
    public void setAlerts(Integer[] alerts) {
        this.alerts = alerts;
    }

    public Integer getAd() {
        return this.ad;
    }

    public void setAd(Integer ad) {
        this.ad = ad;
    }
}