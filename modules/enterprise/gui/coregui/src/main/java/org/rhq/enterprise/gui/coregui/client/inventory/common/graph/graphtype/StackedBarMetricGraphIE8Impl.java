/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype;

/**
 * Contains the javascript chart definition for an IE8 d3 Stacked Bar graph chart.
 * Curr
 *
 * @author Mike Thompson
 */
public final class StackedBarMetricGraphIE8Impl extends StackedBarMetricGraphImpl {

    /**
     * General constructor for stacked bar graph when you have all the data needed to produce the graph. (This is true
     * for all cases but the dashboard portlet).
     */
    public StackedBarMetricGraphIE8Impl() {
        super();
    }


    /**
     * Empty implementation for IE8.
     */
    @Override
    public native void drawJsniChart() /*-{
        // no-op implementation for ie8
    }-*/;

}
