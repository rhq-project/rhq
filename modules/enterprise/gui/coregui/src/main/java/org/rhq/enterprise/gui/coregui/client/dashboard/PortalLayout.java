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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.EventHandler;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DropEvent;
import com.smartgwt.client.widgets.events.DropHandler;

import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

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

                    final PortletWindow target = (PortletWindow) EventHandler.getDragTarget();
                    target.getStoredPortlet().setIndex(dropPosition);
                    target.getStoredPortlet().setColumn(dropColumn);

                    int colNum = 0;
                    for (Canvas pcc : getMembers()) {

                        PortalColumn pc = (PortalColumn) pcc;

                        int i = 0;
                        for (Canvas c : pc.getMembers()) {
                            if (colNum == dropColumn && i == dropPosition) {
                                i++;
                            }

                            if (c instanceof PortletWindow) {
                                DashboardPortlet dp = ((PortletWindow) c).getStoredPortlet();
                                dp.setIndex(i++);
                            }
                        }

                        colNum++;
                    }

                    // drop means the portlet location has changed. The selenium testing locators include positioning
                    // info. So, in this case we have to take the hit and completely redraw the dash.
                    AsyncCallback<Dashboard> callback = SeleniumUtility.getUseDefaultIds() ? null
                        : new AsyncCallback<Dashboard>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                redraw();
                            }

                            @Override
                            public void onSuccess(Dashboard result) {
                                // for some reason the drag drop operation is leaving the target widget (the
                                // portlet window) in the DOM, detached from its original parent (the PortalLayout),
                                // and therefore not destroyed in the redraw().  So, kill it off manually to
                                // avoid ID conflicts if the portlet is again dragged back its original position. 
                                target.removeFromParent();
                                target.destroy();

                                redraw();
                            }
                        };
                    save(callback);

                    com.allen_sauer.gwt.log.client.Log.info("Rearranged column indexes");
                }
            });
        }
    }

    public PortalColumn getPortalColumn(int column) {
        return ((PortalColumn) getMember(column));
    }

    public PortalColumn addPortletWindow(PortletWindow portletWindow, int column) {

        PortalColumn portalColumn = (PortalColumn) getMember(column);
        portalColumn.addMember(portletWindow);

        return portalColumn;
    }

    public void save(AsyncCallback<Dashboard> callback) {
        this.dashboardView.save(callback);
    }

    public void redraw() {
        this.dashboardView.redraw();
    }

    public void resize() {
        for (Canvas c : getMembers()) {
            PortalColumn column = (PortalColumn) c;

            for (Canvas p : column.getMembers()) {
                if (p instanceof PortletWindow) {
                    PortletWindow portlet = (PortletWindow) p;

                    portlet.setWidth(column.getWidth());

                }
            }
        }
    }

    //    @Override
    //    protected void onDestroy() {
    //        destroyMembers();
    //        super.onDestroy();
    //    }

}