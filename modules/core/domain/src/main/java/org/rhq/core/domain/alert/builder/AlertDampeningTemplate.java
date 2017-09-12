/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.alert.builder;

import java.io.Serializable;

import org.rhq.core.domain.alert.AlertDampening;

/**
 * Construct AlertDampening rules
 *
 * @author Michael Burman
 */
public class AlertDampeningTemplate implements Serializable {

    private AlertDampening.Category category = null;
    private Integer value = null; // Occurences
    private AlertDampening.TimeUnits timeUnits = null; // Time
    private Integer period = null; // Evaluations or Time period

    public AlertDampeningTemplate category(AlertDampening.Category category) {
        this.category = category;
        return this;
    }

    public AlertDampeningTemplate occurences(Integer occurences) {
        this.value = occurences;
        return this;
    }

    public AlertDampeningTemplate period(Integer period) {
        this.period = period;
        return this;
    }

    public AlertDampeningTemplate time(AlertDampening.TimeUnits unit) {
        this.timeUnits = unit;
        return this;
    }

    AlertDampening getAlertDampening() {
        AlertDampening alertDampening = new AlertDampening(this.category);

        if(this.period != null) {
            alertDampening.setPeriod(this.period.intValue());
        }
        alertDampening.setPeriodUnits(this.timeUnits);

        if(this.value != null) {
            alertDampening.setValue(this.value.intValue());
        }

        // Where is this used?
//        alertDampening.setValueUnits();
        return alertDampening;
    }
}
