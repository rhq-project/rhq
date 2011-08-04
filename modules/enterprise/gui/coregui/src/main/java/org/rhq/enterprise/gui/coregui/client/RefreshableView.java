/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
 * A view that can refresh its content.
 *
 * @author Ian Springer
 */
public interface RefreshableView {

    /**
     * Refresh this view, i.e. reload its data from the Server.  When implementing refresh keep in mind that
     * the refresh may be called when revisiting an existing canvas. For example, when revisiting a subtab in
     * resource or group detail view. The data on that existing canvas may be displaying stale data. Especially
     * for anynchronous refresh of data,  the user experience may benefit from the stale data being
     * destroyed/wiped/hidden prior to the async call.  This can avoid having the user briefly see the
     * stale data before the refreshed data is rendered.
     */
    void refresh();

}