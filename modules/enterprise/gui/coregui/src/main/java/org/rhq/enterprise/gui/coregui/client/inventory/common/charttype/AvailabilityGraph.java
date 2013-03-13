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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.common.charttype;

/**
 * Interface for AvailabilityGraphs. Implementations of this interface
 * always need to define the capability of creating a graph marker
 * - some kind of div with an id that d3 can bind to and able to
 * drawJsniChart to actually draw(render) the d3 graph.
 *
 * @author Mike Thompson
 */
public interface AvailabilityGraph {

    /**
     * Step 1 create the graph marker to allow d3 to bind.
     */
    void createGraphMarker();

    /**
     * Step 2 bind to the above div#id and render the chart.
     */
    void drawJsniChart();

}
