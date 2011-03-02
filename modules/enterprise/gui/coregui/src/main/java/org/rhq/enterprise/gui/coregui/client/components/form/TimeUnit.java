/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.form;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

/**
 * A unit of time.
 *
 * @author Ian Springer
 */
public enum TimeUnit {

    MILLISECONDS,
    SECONDS,
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
    YEARS;

    private static final Messages MSG = CoreGUI.getMessages();

    public String getDisplayName() {
        String displayName;
        switch (this) {
            case MILLISECONDS:
                displayName = MSG.common_unit_milliseconds();
                break;
            case SECONDS:
                displayName = MSG.common_unit_seconds();
                break;
            case MINUTES:
                displayName = MSG.common_unit_minutes();
                break;
            case HOURS:
                displayName = MSG.common_unit_hours();
                break;
            case DAYS:
                displayName = MSG.common_unit_days();
                break;
            case WEEKS:
                displayName = MSG.common_unit_weeks();
                break;
            case MONTHS:
                displayName = MSG.common_unit_months();
                break;
            case YEARS:
                displayName = MSG.common_unit_years();
                break;
            default:
                throw new IllegalStateException("Unsupported time unit: " + this);
        }
        return displayName;
    }

}
