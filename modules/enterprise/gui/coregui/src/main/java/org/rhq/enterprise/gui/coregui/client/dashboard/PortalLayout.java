/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.dashboard;

import com.smartgwt.client.util.EventHandler;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DropEvent;
import com.smartgwt.client.widgets.events.DropHandler;

import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Greg Hinkle
 */
public class PortalLayout extends LocatableHLayout {

    private DashboardView dashboardView;

    public PortalLayout(String locatorId, DashboardView dashboardView, int numColumns) {
        super(locatorId);

        this.dashboardView = dashboardView;

        setMembersMargin(6);
        for (int i = 0; i < numColumns; i++) {
            final PortalColumn column = new PortalColumn();
            if (i == 0) {
                column.setWidth("30%");
            }
            addMember(column);

            final int columnNumber = i;
            column.addDropHandler(new DropHandler() {
                public void onDrop(DropEvent dropEvent) {

                    int dropPosition = column.getDropPosition();
                    int dropColumn = columnNumber;

                    Canvas target = EventHandler.getDragTarget();
                    ((PortletWindow) target).getDashboardPortlet().setIndex(dropPosition);
                    ((PortletWindow) target).getDashboardPortlet().setColumn(dropColumn);

                    int colNum = 0;
                    for (Canvas pcc : getMembers()) {

                        PortalColumn pc = (PortalColumn) pcc;

                        int i = 0;
                        for (Canvas c : pc.getMembers()) {
                            if (colNum == dropColumn && i == dropPosition) {
                                i++;
                            }

                            if (c instanceof PortletWindow) {
                                DashboardPortlet dp = ((PortletWindow) c).getDashboardPortlet();
                                dp.setIndex(i++);
                            }
                        }

                        colNum++;
                    }
                    save();
                    com.allen_sauer.gwt.log.client.Log.info("Rearranged column indexes");
                }
            });
        }
    }

    public PortalColumn getPortalColumn(int column) {
        return ((PortalColumn) getMember(column));
    }

    public int addPortlet(PortletWindow portlet) {
        int fewestPortletsColumnIndex = -1;
        int fewestPortletsColumnCount = Integer.MAX_VALUE;
        for (int i = 0, numColumns = getMembers().length; (i < numColumns); ++i) {
            PortalColumn portletColumn = (PortalColumn) getMember(i);
            int memberCount = portletColumn.getMembers().length;
            if (fewestPortletsColumnCount > memberCount) {
                fewestPortletsColumnIndex = i;
                fewestPortletsColumnCount = memberCount;
            }
        }

        addPortlet(portlet, fewestPortletsColumnIndex);

        return fewestPortletsColumnIndex;
    }

    public PortalColumn addPortlet(PortletWindow portlet, int columnIndex) {

        PortalColumn portalColumn = (PortalColumn) getMember(columnIndex);
        portalColumn.addMember(portlet);

        return portalColumn;
    }

    public void save() {
        this.dashboardView.save();
    }

    public void resize() {
        for (Canvas c : getMembers()) {
            PortalColumn column = (PortalColumn) c;

            for (Canvas p : column.getMembers()) {
                if (p instanceof PortletWindow) {
                    PortletWindow portlet = (PortletWindow) p;

                    p.setWidth(column.getWidth());

                }
            }
        }
    }
}