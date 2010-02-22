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

import com.smartgwt.client.types.DragAppearance;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.HeaderControl;
import com.smartgwt.client.widgets.Window;

/**
 * @author Greg Hinkle
 */
public class Portlet extends Window {

    public Portlet() {

        setShowShadow(false);

        // enable predefined component animation
        setAnimateMinimize(true);

        // Window is draggable with "outline" appearance by default.
        // "target" is the solid appearance.
        setDragAppearance(DragAppearance.OUTLINE);
        setCanDrop(true);

        // customize the appearance and order of the controls in the window header
        setHeaderControls(HeaderControls.MINIMIZE_BUTTON, HeaderControls.HEADER_LABEL, new HeaderControl(HeaderControl.SETTINGS), new HeaderControl(HeaderControl.HELP), HeaderControls.CLOSE_BUTTON);

        // show either a shadow, or translucency, when dragging a portlet
        // (could do both at the same time, but these are not visually compatible effects)
        // setShowDragShadow(true);
        setDragOpacity(30);

        // these settings enable the portlet to autosize its height only to fit its contents
        // (since width is determined from the containing layout, not the portlet contents)
//        setVPolicy(LayoutPolicy.NONE);
        setOverflow(Overflow.VISIBLE);
    }
}