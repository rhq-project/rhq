/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.dashboard;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.dashboard.DashboardPortlet;

/**
 * @author Greg Hinkle
 */
public interface Portlet {

    /**
     * Called to initially configure the portlet and may be subsequently called to update an
     * existing portlet's configuration.  A portlet's window will not change so it can be safely persisted
     * by the portlet impl.  But the storedPortlet is provided as a convenience and should not be
     * persisted. Outside of this call a portlet impl should retrieve the storedPortlet from the portletWindow,
     * if needed.    
     *  
     * @param portletWindow
     * @param storedPortlet
     */
    void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet);

    /**
     * A canvas displaying help text for the portlet. 
     * 
     * @return
     */
    Canvas getHelpCanvas();
}
