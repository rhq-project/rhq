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
package org.rhq.coregui.client.dashboard;

import java.util.ArrayList;
import java.util.Set;

import com.smartgwt.client.types.DragAppearance;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HeaderControl;
import com.smartgwt.client.widgets.HeaderControl.HeaderIcon;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.DragResizeStopEvent;
import com.smartgwt.client.widgets.events.DragResizeStopHandler;
import com.smartgwt.client.widgets.events.MinimizeClickEvent;
import com.smartgwt.client.widgets.events.MinimizeClickHandler;
import com.smartgwt.client.widgets.events.MouseOverEvent;
import com.smartgwt.client.widgets.events.MouseOverHandler;
import com.smartgwt.client.widgets.events.RestoreClickEvent;
import com.smartgwt.client.widgets.events.RestoreClickHandler;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.coregui.client.components.table.Table;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
public class PortletWindow extends Window {

    private static final String RSS = "Rss";

    // The dashboard in which this window is diplayed
    private DashboardView dashboardView;

    // A reference to the stored/persisted DashboardPortlet, so changes made to this will
    // get persisted when the dashboard is persisted.
    private DashboardPortlet storedPortlet;
    private HeaderControl headerIcon;

    // The actual portlet content view
    private Portlet view;

    // The context applied to the portlet instance 
    private EntityContext context;

    private static final ClickHandler NO_OP_HANDLER = new ClickHandler() {
        public void onClick(ClickEvent clickEvent) {
        }
    };

    private ClickHandler settingsHandlerDelegate = NO_OP_HANDLER;

    private ClickHandler helpHandlerDelegate = NO_OP_HANDLER;

    private ClickHandler rssHandlerDelegate = NO_OP_HANDLER;

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

    private ClickHandler rssHandler = new ClickHandler() {
        public void onClick(ClickEvent clickEvent) {
            rssHandlerDelegate.onClick(clickEvent);
        }
    };

    private ClickHandler refreshHandler = new ClickHandler() {

        @SuppressWarnings("unchecked")
        public void onClick(ClickEvent clickEvent) {
            if (PortletWindow.this.view instanceof AutoRefreshPortlet) {
                ((AutoRefreshPortlet) PortletWindow.this.view).refresh();
            } else if (PortletWindow.this.view instanceof Table) {
                ((Table) PortletWindow.this.view).refresh();
            } else {
                ((Canvas) PortletWindow.this.view).redraw();
            }
        }
    };

    private HeaderControl maximizeHeaderControl = new HeaderControl(HeaderControl.MAXIMIZE, new ClickHandler() {
        public void onClick(ClickEvent event) {
            PortletWindow.this.dashboardView.maximizePortlet(PortletWindow.this);
        }
    });

    public PortletWindow(DashboardView dashboardView, DashboardPortlet dashboardPortlet, EntityContext context) {
        super();

        this.dashboardView = dashboardView;
        this.storedPortlet = dashboardPortlet;
        this.context = context;

        setEdgeSize(2);

        //        if (!showFrame) {
        //            setShowHeader(false);
        //            setShowEdges(false);

        //configure HeaderControls with toolTips
        HeaderControl RssHeader = new HeaderControl(new HeaderIcon("[SKIN]/headerIcons/clipboard.png"), rssHandler);
        RssHeader.setTooltip(RSS);

        //detect customized Header icon
        headerIcon = null;
        String portletKey = storedPortlet.getPortletKey();
        final String iconUrl = PortletFactory.getRegisteredPortletIcon(portletKey);
        if ((iconUrl != null) && (!iconUrl.trim().isEmpty())) {
            HeaderIcon icon = new HeaderIcon(iconUrl);
            headerIcon = new HeaderControl(icon);
            headerIcon.addMouseOverHandler(new MouseOverHandler() {
                @Override
                public void onMouseOver(MouseOverEvent event) {
                    headerIcon.setIcon(iconUrl);
                }
            });
        }

        // customize the appearance and order of the controls in the window header
        initHeaderControls();

        // enable predefined component animation
        setAnimateMinimize(true);

        // Window is draggable with "outline" appearance by default.
        // "target" is the solid appearance.
        setCanDrag(true);
        setDragAppearance(DragAppearance.TARGET);
        // show either a shadow, or translucency, when dragging a portlet
        // (could do both at the same time, but these are not visually compatible effects)
        // setShowDragShadow(true);
        setDragOpacity(30);
        // can be dropped on a column        
        setCanDrop(true);

        setCanDragResize(true);
        setResizeFrom("T", "B");

        setShowShadow(false);

        // these settings enable the portlet to autosize its height only to fit its contents
        // (since width is determined from the containing layout, not the portlet contents)
        //        setVPolicy(LayoutPolicy.NONE);
        setOverflow(Overflow.VISIBLE);

        addDragResizeStopHandler(new DragResizeStopHandler() {
            public void onDragResizeStop(DragResizeStopEvent dragResizeStopEvent) {

                PortletWindow.this.storedPortlet.setHeight(((Canvas) dragResizeStopEvent.getSource()).getHeight());

                PortletWindow.this.dashboardView.resize();
                save();

            }
        });

        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent closeClientEvent) {
                if (PortletWindow.this.dashboardView.isMaximized()) {
                    PortletWindow.this.dashboardView.restorePortlet();
                } else {
                    PortletWindow.this.dashboardView.removePortlet(PortletWindow.this);
                    destroy();
                }
            }
        });

        setSettingsClickHandler(settingsHandler);
        setHelpClickHandler(helpHandler);
    }

    private void initHeaderControls() {

        ArrayList<Object> headerControls = new ArrayList<Object>();

        headerControls.add(HeaderControls.MINIMIZE_BUTTON);
        addMinimizeClickHandler(new MinimizeClickHandler() {
            public void onMinimizeClick(MinimizeClickEvent event) {
                maximizeHeaderControl.hide();
            }
        });
        addRestoreClickHandler(new RestoreClickHandler() {
            public void onRestoreClick(RestoreClickEvent event) {
                maximizeHeaderControl.show();
            }
        });

        headerControls.add(maximizeHeaderControl);

        if (headerIcon != null) {
            headerControls.add(headerIcon);
        }
        headerControls.add(HeaderControls.HEADER_LABEL);
        headerControls.add(new HeaderControl(HeaderControl.REFRESH, refreshHandler));
        headerControls.add(new HeaderControl(HeaderControl.SETTINGS, settingsHandler));
        headerControls.add(new HeaderControl(HeaderControl.HELP, helpHandler));
        headerControls.add(HeaderControls.CLOSE_BUTTON);

        setHeaderControls(headerControls.toArray(new Object[headerControls.size()]));
    }

    void hideSizingHeaderControls(boolean hideControls) {

        if (hideControls) {
            setShowMinimizeButton(false);
            maximizeHeaderControl.hide();

        } else {
            setShowMinimizeButton(true);
            maximizeHeaderControl.show();
        }
    }

    @Override
    protected void onInit() {
        super.onInit();

        // each portletWindow wraps a single portlet view
        view = PortletFactory.buildPortlet(this, storedPortlet, context);

        Canvas canvas = (Canvas) view;
        addItem(canvas);

        settingsHandlerDelegate = new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new PortletSettingsWindow(PortletWindow.this, storedPortlet, view).show();
            }
        };

        helpHandlerDelegate = new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new PortletHelpWindow(storedPortlet, view).show();
            }
        };
    }

    public void setSettingsClickHandler(ClickHandler handler) {
        settingsHandlerDelegate = handler;
    }

    public void setHelpClickHandler(ClickHandler handler) {
        helpHandlerDelegate = handler;
    }

    public Portlet getView() {
        return view;
    }

    public DashboardPortlet getStoredPortlet() {
        return storedPortlet;
    }

    public void setStoredPortlet(DashboardPortlet storedPortlet) {
        this.storedPortlet = storedPortlet;
    }

    public void save() {
        this.dashboardView.save();
    }

    public Set<Permission> getGlobalPermissions() {
        return dashboardView.getGlobalPermissions();
    }

    public ResourcePermission getResourcePermissions() {

        ResourcePermission result = null;

        if (null != dashboardView.getResourceComposite()) {
            result = dashboardView.getResourceComposite().getResourcePermission();
        } else if (null != dashboardView.getGroupComposite()) {
            result = dashboardView.getGroupComposite().getResourcePermission();
        }

        return result;
    }

}
