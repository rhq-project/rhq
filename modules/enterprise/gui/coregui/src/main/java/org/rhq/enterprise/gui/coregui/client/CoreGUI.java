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
package org.rhq.enterprise.gui.coregui.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window.Location;
import com.smartgwt.client.core.KeyIdentifier;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.KeyCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.bundle.BundleTopView;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardsView;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupTopView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTopView;
import org.rhq.enterprise.gui.coregui.client.menu.MenuBarView;
import org.rhq.enterprise.gui.coregui.client.report.ReportTopView;
import org.rhq.enterprise.gui.coregui.client.report.tag.TaggedView;
import org.rhq.enterprise.gui.coregui.client.test.TestTopView;
import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;
import org.rhq.enterprise.gui.coregui.client.util.WidgetUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageBar;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * The GWT {@link EntryPoint entry point} to the RHQ GUI.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class CoreGUI implements EntryPoint, ValueChangeHandler<String> {
    private static final Messages MSG = CoreGUI.getMessages();

    private static final String DEFAULT_VIEW_PATH = DashboardsView.VIEW_ID.getName();

    // just to avoid constructing this over and over
    private static final String TREE_NAV_VIEW_PATTERN = "(" + ResourceTopView.VIEW_ID + "|"
        + ResourceGroupTopView.VIEW_ID + "|" + ResourceGroupDetailView.AUTO_GROUP_VIEW_PATH + ")/[^/]*";

    public static final String CONTENT_CANVAS_ID = "BaseContent";

    private static ErrorHandler errorHandler = new ErrorHandler();

    private static MessageBar messageBar;

    private static BreadcrumbTrailPane breadCrumbTrailPane;

    private static MessageCenter messageCenter;

    private static String currentPath;

    @SuppressWarnings("unused")
    private static Canvas content;

    private RootCanvas rootCanvas;

    private static ViewPath currentViewPath;

    private static CoreGUI coreGUI;

    public static Messages messages = GWT.create(Messages.class);

    private static Message pendingMessage;

    public void onModuleLoad() {
        String hostPageBaseURL = GWT.getHostPageBaseURL();
        if (hostPageBaseURL.indexOf("/coregui/") == -1) {
            Log.info("Suppressing load of CoreGUI module");
            return; // suppress loading this module if not using the new GWT app
        }

        String enableLocators = Location.getParameter("enableLocators");
        if ((null != enableLocators) && Boolean.parseBoolean(enableLocators)) {
            SeleniumUtility.setUseDefaultIds(false);
        }

        coreGUI = this;

        if (isDebugMode()) {
            KeyIdentifier debugKey = new KeyIdentifier();
            debugKey.setCtrlKey(true);
            debugKey.setKeyName("D");
            Page.registerKey(debugKey, new KeyCallback() {
                public void execute(String keyName) {
                    SC.showConsole();
                }
            });
        }

        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable e) {
                getErrorHandler().handleError(MSG.view_core_uncaught(), e);
            }
        });

        messageCenter = new MessageCenter();

        UserSessionManager.login();

        // removing loading image, which can be seen if LoginView doesn't completely cover it
        Element loadingPanel = DOM.getElementById("Loading-Panel");
        loadingPanel.removeFromParent();
    }

    public static CoreGUI get() {
        return coreGUI;
    }

    public void buildCoreUI() {
        // If the core gui is already built (eg. from previous login, just refire event)
        if (rootCanvas == null) {
            MenuBarView menuBarView = new MenuBarView("TopMenu");
            menuBarView.setWidth("100%");

            messageBar = new MessageBar();

            breadCrumbTrailPane = new BreadcrumbTrailPane();

            Canvas canvas = new Canvas(CONTENT_CANVAS_ID);
            canvas.setWidth100();
            canvas.setHeight100();

            rootCanvas = new RootCanvas();
            rootCanvas.setOverflow(Overflow.HIDDEN);
            rootCanvas.addMember(menuBarView);

            rootCanvas.addMember(messageBar);
            //rootCanvas.addMember(breadCrumbTrailPane);
            rootCanvas.addMember(canvas);
            rootCanvas.addMember(new Footer());
            rootCanvas.draw();

            History.addValueChangeHandler(this);
        }

        if (History.getToken().equals("") || History.getToken().equals("LogOut")) {
            // go to default view if user doesn't specify a history token
            History.newItem(getDefaultView());
        } else {
            // otherwise just fire an event for the bookmarked URL they are returning to
            History.fireCurrentHistoryState();
        }
    }

    public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
        currentPath = URL.decodeComponent(stringValueChangeEvent.getValue());
        Log.debug("Handling history event for path: " + currentPath);

        currentViewPath = new ViewPath(currentPath);
        coreGUI.rootCanvas.renderView(currentViewPath);
    }

    public static void refresh() {
        currentViewPath = new ViewPath(currentPath, true);
        coreGUI.rootCanvas.renderView(currentViewPath);
    }

    public Canvas createContent(String breadcrumbName) {
        Canvas canvas;

        if (breadcrumbName.equals(AdministrationView.VIEW_ID.getName())) {
            canvas = new AdministrationView();
        } else if (breadcrumbName.equals(InventoryView.VIEW_ID.getName())) {
            canvas = new InventoryView();
        } else if (breadcrumbName.equals(ResourceTopView.VIEW_ID.getName())) {
            canvas = new ResourceTopView(breadcrumbName);
        } else if (breadcrumbName.equals(ResourceGroupTopView.VIEW_ID.getName())) {
            canvas = new ResourceGroupTopView(breadcrumbName);
        } else if (breadcrumbName.equals(DashboardsView.VIEW_ID.getName())) {
            canvas = new DashboardsView(breadcrumbName);
        } else if (breadcrumbName.equals(BundleTopView.VIEW_ID.getName())) {
            canvas = new BundleTopView(breadcrumbName);
        } else if (breadcrumbName.equals("LogOut")) {
            // TODO: don't make LogOut a history event, just perform the logout action by responding to click event
            LoginView logoutView = new LoginView("Login");
            canvas = logoutView;
            UserSessionManager.logout();
            logoutView.showLoginDialog();
        } else if (breadcrumbName.equals(TaggedView.VIEW_ID.getName())) {
            canvas = new TaggedView(breadcrumbName);
        } else if (breadcrumbName.equals("Subsystems")) {
            canvas = new AlertHistoryView("Alert");
        } else if (breadcrumbName.equals(ReportTopView.VIEW_ID.getName())) {
            canvas = new ReportTopView();
        } else if (breadcrumbName.equals(TestTopView.VIEW_ID.getName())) {
            canvas = new TestTopView();
        } else {
            canvas = null;
        }
        return canvas;
    }

    // -------------------- Static application utilities ----------------------

    public static MessageCenter getMessageCenter() {
        return messageCenter;
    }

    public static ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public static void printWidgetTree() {
        WidgetUtility.printWidgetTree(coreGUI.rootCanvas);
    }

    private static String getDefaultView() {
        // TODO: should this be Dashboard or a User Preference?
        return DEFAULT_VIEW_PATH;
    }

    public static void setContent(Canvas newContent) {
        Canvas contentCanvas = Canvas.getById(CONTENT_CANVAS_ID);
        for (Canvas child : contentCanvas.getChildren()) {
            child.destroy();
        }
        if (newContent != null) {
            content = newContent;
            contentCanvas.addChild(newContent);
        }
        contentCanvas.markForRedraw();
    }

    public static void goToView(String viewPath) {
        goToView(viewPath, null);
    }

    public static void goToView(String viewPath, Message message) {
        pendingMessage = message;

        // if path starts with "#" (e.g. if caller used LinkManager to obtain some of the path), strip it off 
        if (viewPath.charAt(0) == '#') {
            viewPath = viewPath.substring(1);
        }

        String currentViewPath = History.getToken();
        if (currentViewPath.equals(viewPath)) {
            // We're already there - just refresh the view.
            refresh();
        } else {
            if (viewPath.matches(TREE_NAV_VIEW_PATTERN)) {
                // e.g. "Resource/10001" or "Resource/AutoGroup/10003"
                if (!currentViewPath.startsWith(viewPath)) {
                    // The Node that was selected is not the same Node that was previously selected - it
                    // may not even be the same node type. For example, the user could have moved from a
                    // resource to an autogroup in the same tree. Try to keep the tab selection sticky as best as
                    // possible while moving from one view to another by grabbing the end portion of the previous
                    // history URL and append it to the new history URL.  The suffix is assumed to follow the
                    // ID (numeric) portion of the currentViewPath. 
                    String suffix = currentViewPath.replaceFirst("\\D*[^/]*", "");
                    // make sure we're not *too* sticky, stop no deeper than the subtab level. This prevents
                    // trying to render non-applicable detail views.  We'll do this by chopping at the start 
                    // of any other numeric in the path
                    suffix = suffix.replaceFirst("\\d.*", "");
                    viewPath += suffix;
                }
            }
            History.newItem(viewPath);
        }
    }

    public static void refreshBreadCrumbTrail() {
        breadCrumbTrailPane.refresh(currentViewPath);
    }

    public static Messages getMessages() {
        return messages;
    }

    private class RootCanvas extends VLayout implements BookmarkableView {
        private ViewId currentViewId;
        private Canvas currentCanvas;

        private RootCanvas() {
            setWidth100();
            setHeight100();
        }

        public void renderView(final ViewPath viewPath) {
            if (viewPath.isEnd()) {
                // default view
                History.newItem(DEFAULT_VIEW_PATH);
            } else {
                messageBar.clearMessage();
                if (pendingMessage != null) {
                    getMessageCenter().notify(pendingMessage);
                    pendingMessage = null;
                }

                ViewId topLevelViewId = viewPath.getCurrent(); // e.g. Administration
                if (!topLevelViewId.equals(this.currentViewId)) {
                    this.currentViewId = topLevelViewId;
                    this.currentCanvas = createContent(this.currentViewId.getPath());
                    setContent(this.currentCanvas);
                }

                if (this.currentCanvas instanceof BookmarkableView) {
                    if (this.currentCanvas instanceof InitializableView) {
                        final InitializableView initializableView = (InitializableView) this.currentCanvas;
                        final long startTime = System.currentTimeMillis();
                        final Timer timer = new Timer() {
                            public void run() {
                                if (initializableView.isInitialized()) {
                                    ((BookmarkableView) currentCanvas).renderView(viewPath.next());
                                } else {
                                    long elapsedMillis = System.currentTimeMillis() - startTime;
                                    if (elapsedMillis < 5000) {
                                        // Reschedule the timer.
                                        schedule(100);
                                    }
                                }
                            }
                        };
                        if (initializableView.isInitialized()) {
                            ((BookmarkableView) currentCanvas).renderView(viewPath.next());
                        } else {
                            timer.schedule(100);
                        }
                    } else {
                        ((BookmarkableView) currentCanvas).renderView(viewPath.next());
                    }
                }

                refreshBreadCrumbTrail();
            }
        }
    }

    public static boolean isDebugMode() {
        return !GWT.isScript();
    }
}
