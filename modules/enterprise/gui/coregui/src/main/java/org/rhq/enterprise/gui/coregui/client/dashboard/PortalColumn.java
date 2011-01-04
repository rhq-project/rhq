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
import com.smartgwt.client.widgets.layout.VStack;

/**
 * @author Greg Hinkle
 */
public class PortalColumn extends VStack {

    public PortalColumn() {

        // leave some space between portlets
        setMembersMargin(6);
        this.setBorder("1px solid #999999");

        // enable predefined component animation
        setAnimateMembers(true);
        setAnimateMemberTime(300);

        // enable drop handling
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
    }
}