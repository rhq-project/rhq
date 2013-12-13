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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.coregui.client.inventory.resource.detail.monitoring.table;

import static org.rhq.core.domain.measurement.ui.MetricDisplayConstants.AVERAGE_KEY;
import static org.rhq.core.domain.measurement.ui.MetricDisplayConstants.MAX_KEY;
import static org.rhq.core.domain.measurement.ui.MetricDisplayConstants.MIN_KEY;

import org.rhq.coregui.client.CoreGUI;

/**
 * Typesafe field names used in consolidated metrics screen grids for Resource and ResourceGroup.
 * Also associates the proper label with the value.
 *
 * @author  Mike Thompson
 */
@SuppressWarnings("GwtInconsistentSerializableClass")
public enum MetricsGridFieldName {

    SPARKLINE("sparkline"), METRIC_LABEL("label", CoreGUI.getMessages().common_title_name()), ALERT_COUNT("alertCount",
        CoreGUI.getMessages().common_title_alerts()), MAX_VALUE(MAX_KEY, CoreGUI.getMessages()
        .common_title_monitor_maximum()), MIN_VALUE(MIN_KEY, CoreGUI.getMessages().common_title_monitor_minimum()), AVG_VALUE(
        AVERAGE_KEY, CoreGUI.getMessages().common_title_monitor_average()), METRIC_DEF_ID("defId"), METRIC_SCHEDULE_ID(
        "schedId"), METRIC_UNITS("units"), METRIC_NAME("name"), RESOURCE_GROUP_ID("resourceGroupId"),
    RESOURCE_ID("resourceId"), LIVE_VALUE("live", CoreGUI.getMessages().view_resource_monitor_table_live());

    private final String value;
    private final String label;

    MetricsGridFieldName(String value, String label) {
        this.value = value;
        this.label = label;
    }

    MetricsGridFieldName(String value) {
        this.value = value;
        this.label = "";
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
