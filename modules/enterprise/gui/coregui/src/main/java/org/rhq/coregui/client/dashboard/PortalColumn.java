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
package org.rhq.coregui.client.dashboard;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VStack;

import org.rhq.coregui.client.util.enhanced.EnhancedUtility;

/**
 * @author Greg Hinkle
 */
public class PortalColumn extends VStack {

    public PortalColumn() {

        // leave some space between portlets
        setMembersMargin(4);

        // Provide a visible border for framing of columns (especially when empty) and add padding to make
        // the border easier to see for dragging
        setBorder("1px solid transparent");

        // Allow column specific vertical scrolling to see off-screen portlets. Takes up real estate but allows
        // a user to see specific portlets in each column at the same time.
        //setOverflow(Overflow.AUTO);

        // enable predefined component animation
        setAnimateMembers(true);
        setAnimateMemberTime(300);

        // enable drop handling for moving portlet windows within or between columns
        setCanAcceptDrop(true);

        // change appearance of drag placeholder and drop indicator
        setDropLineThickness(4);

        Canvas dropLineProperties = new Canvas();
        dropLineProperties.setBackgroundColor("#4A5D75");
        setDropLineProperties(dropLineProperties);

        setShowDragPlaceHolder(true);

        Canvas placeHolderProperties = new Canvas();
        placeHolderProperties.setBorder("2px solid #4A5D75");
        setPlaceHolderProperties(placeHolderProperties);

        // Allow column resizing (width only)
        setCanDragResize(true);

        // Use Left side drag resize because right side seems to conflict with vertical scroll bars.
        setResizeFrom("L");

        // Do not use the resize bar. It looks good but does not behave as well as the border dragging, and does
        // not seem to play as well with the resize handlers. Maybe in the future...
        // setShowResizeBar(true);

        // True is the default, just capturing this call here for any future tweaking
        // setRedrawOnResize(true);
    }

    @Override
    public void destroy() {
        removeFromParent();
        EnhancedUtility.destroyMembers(this);

        super.destroy();
    }
}
