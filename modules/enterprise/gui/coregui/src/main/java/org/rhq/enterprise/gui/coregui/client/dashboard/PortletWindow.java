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

import com.smartgwt.client.types.DragAppearance;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HeaderControl;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.events.DragResizeStopEvent;
import com.smartgwt.client.widgets.events.DragResizeStopHandler;

import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * @author Greg Hinkle
 */
public class PortletWindow extends Window {

    private DashboardView dashboardView;
    private DashboardPortlet dashboardPortlet;

    private Portlet view;

    private static final ClickHandler NO_OP_HANDLER = new ClickHandler() {
        public void onClick(ClickEvent clickEvent) {
        }
    };

    private ClickHandler settingsHandlerDelegate = NO_OP_HANDLER;

    private ClickHandler helpHandlerDelegate = NO_OP_HANDLER;

    private ClickHandler settingsHandler = new ClickHandler() {
        public void onClick(ClickEvent clickEvent) {
            settingsHandlerDelegate.onClick(clickEvent);
        }
    };

    private ClickHandler helpHandler = new ClickHandler() {
        public void onClick(ClickEvent clickEvent) {
            helpHandlerDelegate.onClick(clickEvent);
        }
    };

    private ClickHandler refreshHandler = new ClickHandler() {
        public void onClick(ClickEvent clickEvent) {
            if (PortletWindow.this.view instanceof Table) {
                ((Table)PortletWindow.this.view).refresh();
            } else {
                ((Canvas)PortletWindow.this.view).redraw();
            }
        }
    };

    public PortletWindow(DashboardView dashboardView, DashboardPortlet dashboardPortlet) {

        this.dashboardView = dashboardView;
        this.dashboardPortlet = dashboardPortlet;
        setEdgeSize(2);


//        if (!showFrame) {
//            setShowHeader(false);
//            setShowEdges(false);

        // customize the appearance and order of the controls in the window header
        setHeaderControls(
                HeaderControls.MINIMIZE_BUTTON,
                HeaderControls.HEADER_LABEL,
                new HeaderControl(HeaderControl.REFRESH, refreshHandler),
                new HeaderControl(HeaderControl.SETTINGS, settingsHandler),
                new HeaderControl(HeaderControl.HELP, helpHandler),
                HeaderControls.CLOSE_BUTTON
        );

        // show either a shadow, or translucency, when dragging a portlet
        // (could do both at the same time, but these are not visually compatible effects)
        // setShowDragShadow(true);
        setDragOpacity(30);

        // enable predefined component animation
        setAnimateMinimize(true);

        // Window is draggable with "outline" appearance by default.
        // "target" is the solid appearance.
        setDragAppearance(DragAppearance.TARGET);
        setCanDrop(true);

        setCanDragResize(true);
//        setResizeFrom("B");


        setShowShadow(false);

        // these settings enable the portlet to autosize its height only to fit its contents
        // (since width is determined from the containing layout, not the portlet contents)
//        setVPolicy(LayoutPolicy.NONE);
        setOverflow(Overflow.VISIBLE);


        addDragResizeStopHandler(new DragResizeStopHandler() {
            public void onDragResizeStop(DragResizeStopEvent dragResizeStopEvent) {


                PortletWindow.this.dashboardPortlet.setHeight(((Canvas) dragResizeStopEvent.getSource()).getHeight()); 

                PortletWindow.this.dashboardView.resize();
                save();

            }
        });

        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClientEvent closeClientEvent) {
                PortletWindow.this.dashboardPortlet.getDashboard().removePortlet(PortletWindow.this.dashboardPortlet);
                save();
                destroy();
            }
        });

        setSettingsClickHandler(settingsHandler);
        setHelpClickHandler(helpHandler);

    }

    @Override
    protected void onInit() {
        super.onInit();

        view = PortletFactory.buildPortlet(this, dashboardPortlet);

        Canvas canvas = (Canvas) view;
        addItem(canvas);

        settingsHandlerDelegate = new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new PortletSettingsWindow(PortletWindow.this, dashboardPortlet, view).show();
            }
        };

        helpHandlerDelegate = new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new PortletHelpWindow(dashboardPortlet, view).show();
            }
        };
    }

    public void setSettingsClickHandler(ClickHandler handler) {
        settingsHandlerDelegate = handler;
    }

    public void setHelpClickHandler(ClickHandler handler) {
        helpHandlerDelegate = handler;
    }

    public DashboardPortlet getDashboardPortlet() {
        return dashboardPortlet;
    }

    public void setDashboardPortlet(DashboardPortlet dashboardPortlet) {
        this.dashboardPortlet = dashboardPortlet;
    }

    public void save() {
        this.dashboardView.save();
    }
}
