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
package org.rhq.enterprise.server.legacy.events;

import org.rhq.core.clientapi.util.ArrayUtil;

/**
 * Constants used in the Events subsystem.
 */
public class EventConstants {
    public static final int TYPE_THRESHOLD = 1;
    public static final int TYPE_BASELINE = 2;
    public static final int TYPE_CONTROL = 3;
    public static final int TYPE_CHANGE = 4;
    public static final int TYPE_ALERT = 5;
    public static final int TYPE_CUST_PROP = 6;
    public static final int TYPE_LOG = 7;

    private static final String[] TYPES = { "Measurement Threshold", "Measurement Baseline", "Control Action",
        "Measurement Value Change", "Alert Fired", "Custom Property Value Change", "Log Event", };

    public static String[] getTypes() {
        return TYPES;
    }

    public static int getType(String typeStr) {
        int ind = ArrayUtil.find(TYPES, typeStr);

        if (ind > -1) {
            ind++;
        }

        return ind;
    }

    public static String getType(int type) {
        return TYPES[type - 1];
    }

    public static final String EVENTS_TOPIC = "topic/eventsTopic";

    /**
     * Constant for removing a control action from an alert definition.
     */
    public static final String CONTROL_ACTION_NONE = "none";

    public static final Integer TYPE_ALERT_DEF_ID = 0;
}