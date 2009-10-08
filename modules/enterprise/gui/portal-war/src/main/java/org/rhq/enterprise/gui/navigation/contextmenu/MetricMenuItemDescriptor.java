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
package org.rhq.enterprise.gui.navigation.contextmenu;

/**
 * This class provides data for rendering a metric menu item in the nav tree context menu.
 * 
 * @author Lukas Krejci
 */
public class MetricMenuItemDescriptor extends MenuItemDescriptor {

    private String metricToken;

    /**
     * The metric tokens are used to identify the metric chart when adding such
     * chart to the indicators chart on the monitoring page.
     * 
     * The tokens have the following format (without the quotes):
     * for resource:     '<resourceId>,<scheduleId>'
     * for compatgroups: 'cg,<groupId>,<definitionId>'
     * for autogroups:   'ag,<parentId>,<definitionId>,<typeId>'
     *
     * @return the token for the this menu item
     */
    public String getMetricToken() {
        return metricToken;
    }

    public void setMetricToken(String metricToken) {
        this.metricToken = metricToken;
    }
}
