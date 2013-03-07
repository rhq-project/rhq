/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.rest;

/**
 * List of templates to create links to the GWT-based coregui.
 *
 * The argument of the Enum is a template pointing at the fragment
 * in coregui. If an id needs to be passed, a placeholder is used.
 * Those placeholders conform to those of {@link java.lang.String#format(String,Object...)}
 * @author Heiko W. Rupp
 */
public enum UILinkTemplate {

    RESOURCE("Resource/%d"),
    RESOURCE_ALERT_DEF("Resource/%d/Alerts/Definitions/%d"),
    RESOURCE_ALERT("Resource/%d/Alerts/%d"),
    RESOURCE_MONITORING("Resource/%d/Monitoring/Tables"),
    RESOURCE_CHILDREN("Resource/%d/Inventory/Children"),

    GROUP("ResourceGroup/%d"),
    GROUP_ALERT("ResourceGroup/%d/Alerts/%d"),
    GROUP_ALERT_DEF("ResourceGroup/%d/Alerts/Definitions/%d"),

    DYNAGROUP("Inventory/Groups/DynagroupDefinitions/%d"),

    METRIC_SCHEDULE("Resource/%d/Monitoring/Schedules"),

    TEMPLATE_ALERT_DEF("Administration/Configuration/AlertDefTemplates/%d/%d");

    private String url;

    private UILinkTemplate(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
