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
package org.rhq.enterprise.gui.coregui.client;

/**
 * Defines the first step in the 2 step approach to creating graphs.
 * <ol>
 *     <li>Step 1: Create the graph marker in the page.</li>
 *     <li>Step 2: Create and Attach a graph to graph marker via HasD3MetricJsniChart.drawJsniChart</li>
 * </ol>
 *
 * @author  Mike Thompson
 */
public interface GraphMarker {
    String createGraphMarker();
}
