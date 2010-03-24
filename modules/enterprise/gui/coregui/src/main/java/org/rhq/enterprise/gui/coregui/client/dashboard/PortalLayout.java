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

import com.smartgwt.client.widgets.layout.HLayout;

/**
 * @author Greg Hinkle
 */
public class PortalLayout extends HLayout {
    public PortalLayout(int numColumns) {
        setMembersMargin(6);
        for (int i = 0; i < numColumns; i++) {
            PortalColumn column = new PortalColumn();
            if (i == 0) {
                column.setWidth("30%");
            }
            addMember(column);
        }
    }

    public PortalColumn addPortlet(Portlet portlet) {
        // find the column with the fewest portlets
        int fewestPortlets = Integer.MAX_VALUE;
        PortalColumn fewestPortletsColumn = null;
        for (int i = 0; i < getMembers().length; i++) {
            int numPortlets = ((PortalColumn) getMember(i)).getMembers().length;
            if (numPortlets < fewestPortlets) {
                fewestPortlets = numPortlets;
                fewestPortletsColumn = (PortalColumn) getMember(i);
            }
        }
        fewestPortletsColumn.addMember(portlet);
        return fewestPortletsColumn;
    }
}