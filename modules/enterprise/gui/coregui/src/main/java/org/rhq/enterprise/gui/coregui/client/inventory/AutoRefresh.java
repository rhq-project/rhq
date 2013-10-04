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
package org.rhq.coregui.client.inventory;

/**
 * @author Simeon Pinder
 * @author Mike Thompson
 */
public interface AutoRefresh {

    /**
     * Each item implements to define the refresh cycle.  Note that once refresh is started it
     * should be canceled when the portlet goes out of scope (typically in an onDestroy() override).
     */
    void startRefreshCycle();

    /**
     * @return true if the widget is currently responding to a refresh (i.e. reloading data). This can be used
     * to ignore refresh requests until a prior request is completed.
     */
    boolean isRefreshing();

    /**
     * Refresh this widget, reload data, redraw widgets, whatever is needed to refresh the page
     */
    void refresh();


}
