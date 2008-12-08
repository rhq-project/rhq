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
package org.rhq.enterprise.gui.legacy;

import org.rhq.enterprise.server.measurement.util.NumberConstants;

/**
 * Manifest constants for the UI of the HQ application. The constants are actually organized into more digestable chunks
 * in logical interfaces, e.g. attributes are in AttributeConstants and numbers are in NumberConstants
 *
 * @deprecated
 */
@Deprecated
public final class Constants implements RetCodeConstants, NumberConstants, MessageConstants, DefaultConstants,
    TypeConstants, AttrConstants, ParamConstants, KeyConstants, StringConstants {
    @Deprecated
    public static final String APP_VERSION = "HQVersion";

    public static final String INVENTORY_LOC_TYPE = "Inventory.do";

    public static final String MONITOR_VISIBILITY_LOC = "monitor/Visibility.do";

    public static final String MONITOR_CONFIG_LOC = "monitor/Config.do";

    public static final String CONTROL_LOC = "/Control.do";

    /**
     * These two locations are not handled similar to the rest of the above URL
     */
    public static final String ALERT_LOC = "alerts/Alerts.do";

    public static final String ALERT_CONFIG_LOC = "alerts/Config.do";

    /**
     * Magic name of transfer file action that causes the "transfer file" div to appear in server and group controls.
     * See mockup 2.4.7.1
     */
    public static final String TRANSFER_ACTION_NAME = "transfer";
}