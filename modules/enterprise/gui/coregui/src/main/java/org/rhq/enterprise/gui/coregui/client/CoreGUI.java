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
import com.google.gwt.user.client.Window;
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
import org.rhq.enterprise.gui.coregui.client.help.HelpView;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupTopView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTopView;
import org.rhq.enterprise.gui.coregui.client.menu.MenuBarView;
import org.rhq.enterprise.gui.coregui.client.report.ReportTopView;
import org.rhq.enterprise.gui.coregui.client.report.tag.TaggedView;
import org.rhq.enterprise.gui.coregui.client.test.TestTopView;
import org.rhq.enterprise.gui.coregui.client.test.i18n.TestRemoteServiceStatisticsView;
import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;
import org.rhq.enterprise.gui.coregui.client.util.WidgetUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * The GWT {@link EntryPoint entry point} to the RHQ GUI.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class CoreGUI implements EntryPoint, ValueChangeHandler<String> {

    public static final String CONTENT_CANVAS_ID = "BaseContent";

    // This must come first to ensure proper I18N class loading for dev mode
    private static final Messages MSG = GWT.create(Messages.class);
    private static final MessageConstants MSGCONST = GWT.create(MessageConstants.class);

    private static final String DEFAULT_VIEW = DashboardsView.VIEW_ID.getName();
    private static final String LOGOUT_VIEW = "LogOut";

    private static String currentView;
    private static ViewPath currentViewPath;

    // just to avoid constructing this over and over. the ordering is important, more complex viewPaths first,
    // javascript will greedily match "Resource" and give up trying after that, missing "Resource/AutoGroup" for
    // example.
    private static final String TREE_NAV_VIEW_PATTERN = "(" //
        + ResourceGroupDetailView.AUTO_GROUP_VIEW + "|" //
        + ResourceGroupDetailView.AUTO_CLUSTER_VIEW //
        + ResourceGroupTopView.VIEW_ID + "|" //
        + ResourceTopView.VIEW_ID + "|" // 
        + ")/[^/]*";

    private static ErrorHandler errorHandler = new ErrorHandler();

    private static MessageCenter messageCenter;

    private static CoreGUI coreGUI;

    // store a message to be posted in the message center during the next renderView processing.
    private static Message pendingMessage;

    // store the fact that we want the ViewPath for the next renderView to have refresh on. This allows us to
    // ask for a refresh even when changing viewPaths, which can be useful when we want to say, refresh a LHS tree
    // while also changing the main content. Typically we refresh only when the path is not changed.
    private static boolean pendingRefresh = false;

    private RootCanvas rootCanvas;
    private MenuBarView menuBarView;
    private Footer footer;

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

        KeyIdentifier statisticsWindowKey = new KeyIdentifier();
        statisticsWindowKey.setCtrlKey(true);
        statisticsWindowKey.setKeyName("S");
        Page.registerKey(statisticsWindowKey, new KeyCallback() {
            public void execute(String keyName) {
                TestRemoteServiceStatisticsView.showInWindow();
            }
        });

        KeyIdentifier messageCenterWindowKey = new KeyIdentifier();
        messageCenterWindowKey.setCtrlKey(true);
        messageCenterWindowKey.setKeyName("M");
        Page.registerKey(messageCenterWindowKey, new KeyCallback() {
            public void execute(String keyName) {
                footer.getMessageCenter().showMessageCenterWindow();
            }
        });

        KeyIdentifier testTopViewKey = new KeyIdentifier();
        testTopViewKey.setCtrlKey(true);
        testTopViewKey.setAltKey(true);
        testTopViewKey.setKeyName("t");
        Page.registerKey(testTopViewKey, new KeyCallback() {
            public void execute(String keyName) {
                goToView("Test");
            }
        });

        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable e) {
                getErrorHandler().handleError(MSG.view_core_uncaught(), e);
            }
        });

        /* after having this enabled for a while, the majority opinion is that this is more annoying than helpful 
         *  
        Window.addWindowClosingHandler(new Window.ClosingHandler() {
            public void onWindowClosing(Window.ClosingEvent event) {
                event.setMessage("Are you sure you want to leave RHQ?");                              
            }
        });
        */

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
        // If the core gui is already built (eg. from previous login) skip the build and just refire event
        if (rootCanvas == null) {
            menuBarView = new MenuBarView("TopMenu");
            menuBarView.setWidth("100%");
            footer = new Footer();

            Canvas canvas = new Canvas(CONTENT_CANVAS_ID);
            canvas.setWidth100();
            canvas.setHeight100();

            rootCanvas = new RootCanvas();
            rootCanvas.setOverflow(Overflow.HIDDEN);

            rootCanvas.addMember(menuBarView);
            rootCanvas.addMember(footer);
            rootCanvas.addMember(canvas);

            rootCanvas.draw();

            History.addValueChangeHandler(this);
        }

        if (History.getToken().equals("") || History.getToken().equals(LOGOUT_VIEW)) {

            // init the rootCanvas to ensure a clean slate for a new user
            rootCanvas.initCanvas();

            // go to default view if user doesn't specify a history token
            History.newItem(getDefaultView());
        } else {

            // otherwise just fire an event for the bookmarked URL they are returning to
            History.fireCurrentHistoryState();
        }

        // The root canvas is hidden by user log out, show again if necessary
        if (!rootCanvas.isVisible()) {
            rootCanvas.show();
        }
    }

    public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
        currentView = URL.decodeComponent(stringValueChangeEvent.getValue());
        Log.debug("Handling history event for view: " + currentView);

        currentViewPath = new ViewPath(currentView);
        coreGUI.rootCanvas.renderView(currentViewPath);
    }

    public static void refresh() {
        currentViewPath = new ViewPath(currentView, true);
        coreGUI.rootCanvas.renderView(currentViewPath);
    }

    public Canvas createContent(String viewName) {
        Canvas canvas;

        if (viewName.equals(DashboardsView.VIEW_ID.getName())) {
            canvas = new DashboardsView(viewName);
        } else if (viewName.equals(InventoryView.VIEW_ID.getName())) {
            canvas = new InventoryView();
        } else if (viewName.equals(ResourceTopView.VIEW_ID.getName())) {
            canvas = new ResourceTopView(viewName);
        } else if (viewName.equals(ResourceGroupTopView.VIEW_ID.getName())) {
            canvas = new ResourceGroupTopView(viewName);
        } else if (viewName.equals(ReportTopView.VIEW_ID.getName())) {
            canvas = new ReportTopView();
        } else if (viewName.equals(BundleTopView.VIEW_ID.getName())) {
            canvas = new BundleTopView(viewName);
        } else if (viewName.equals(AdministrationView.VIEW_ID.getName())) {
            canvas = new AdministrationView();
        } else if (viewName.equals(HelpView.VIEW_ID.getName())) {
            canvas = new HelpView();
        } else if (viewName.equals(LOGOUT_VIEW)) {
            UserSessionManager.logout();
            rootCanvas.hide();

            LoginView logoutView = new LoginView("Login");
            canvas = logoutView;
            logoutView.showLoginDialog();
        } else if (viewName.equals(TaggedView.VIEW_ID.getName())) {
            canvas = new TaggedView(viewName);
        } else if (viewName.equals("Subsystems")) {
            canvas = new AlertHistoryView("Alert");
        } else if (viewName.equals(TestTopView.VIEW_ID.getName())) {
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
        return DEFAULT_VIEW;
    }

    public static void goToView(String view) {
        goToView(view, null, false);
    }

    public static void goToView(String view, Message message) {
        goToView(view, message, false);
    }

    public static void goToView(String view, boolean refresh) {
        goToView(view, null, refresh);
    }

    public static void goToView(String view, Message message, boolean refresh) {
        pendingMessage = message;
        pendingRefresh = refresh;

        // if path starts with "#" (e.g. if caller used LinkManager to obtain some of the path), strip it off 
        if (view.charAt(0) == '#') {
            view = view.substring(1);
        }

        String currentViewPath = History.getToken();
        if (currentViewPath.equals(view)) {
            // We're already there - just refresh the view.
            refresh();
        } else {
            if (view.matches(TREE_NAV_VIEW_PATTERN)) {
                // e.g. "Resource/10001" or "Resource/AutoGroup/10003"
                if (!currentViewPath.startsWith(view)) {
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
                    view += suffix;
                }
            }
            History.newItem(view);
        }
    }

    public static Messages getMessages() {
        return MSG;
    }

    public static MessageConstants getMessageConstants() {
        return MSGCONST;
    }

    private class RootCanvas extends VLayout implements BookmarkableView {
        private ViewId currentViewId;
        private Canvas currentCanvas;

        private RootCanvas() {
            setWidth100();
            setHeight100();
        }

        private String getViewPathTitle(ViewPath viewPath) {
            // default title is the path minus any IDs we find. That should give a least some nice default title.
            StringBuilder viewPathTitle = new StringBuilder();
            for (ViewId viewId : viewPath.getViewPath()) {
                // none of our path elements start with a digit that is NOT an ID; if we see an ID, skip it
                if (!Character.isDigit(viewId.getPath().charAt(0))) {
                    if (viewPathTitle.length() > 0) {
                        viewPathTitle.append(" | ");
                    }
                    viewPathTitle.append(viewId.getPath());
                }
            }
            if (viewPathTitle.length() == 0) {
                viewPathTitle.append("Core Application");
            }
            return "RHQ: " + viewPathTitle.toString();
        }

        public void initCanvas() {
            // request a redraw of the MenuBarItem to ensure the correct session info is displayed 
            getMember(0).markForRedraw();

            // remove any current viewId so the next requested view generates new content
            this.currentViewId = null;
        }

        public void renderView(ViewPath viewPath) {
            // If the session is logged out ensure that we only navigate to the log out view, otherwise keep 
            // our CoreGUI session alive by refreshing the session timer each time the user performs navigation
            if (UserSessionManager.isLoggedOut()) {
                if (!LOGOUT_VIEW.equals(viewPath.getCurrent().getPath())) {
                    History.newItem(LOGOUT_VIEW);
                }
                return;
            } else {
                UserSessionManager.refresh();
            }

            Window.setTitle(getViewPathTitle(viewPath));

            // clear any message when navigating to a new view (not refreshing), the user is probably no longer interested
            if (!viewPath.isRefresh()) {
                coreGUI.footer.getMessageBar().clearMessage(true);
            }

            if (viewPath.isEnd()) {
                // default view
                History.newItem(DEFAULT_VIEW);
            } else {
                if (pendingMessage != null) {
                    getMessageCenter().notify(pendingMessage);
                    pendingMessage = null;
                }

                if (pendingRefresh) {
                    viewPath.setRefresh(true);
                    pendingRefresh = false;
                }

                ViewId topLevelViewId = viewPath.getCurrent(); // e.g. Administration
                if (!topLevelViewId.equals(this.currentViewId)) {
                    // Destroy the current canvas before creating the new one. This helps prevent locator
                    // conflicts if the old and new content share (logical) widgets.  A call to destroy (e.g. certain
                    // IFrames/FullHTMLPane) can actually remove multiple children of the contentCanvas. As such, we
                    // need to query for the children after each destroy to ensure only valid children are in the array.
                    Canvas contentCanvas = Canvas.getById(CONTENT_CANVAS_ID);
                    Canvas[] children;
                    while ((children = contentCanvas.getChildren()).length > 0) {
                        children[0].destroy();
                    }

                    // Set the new content and redraw
                    this.currentViewId = topLevelViewId;
                    this.currentCanvas = createContent(this.currentViewId.getPath());

                    if (null != this.currentCanvas) {
                        contentCanvas.addChild(this.currentCanvas);
                    }

                    contentCanvas.markForRedraw();
                }

                if (this.currentCanvas instanceof InitializableView) {
                    final InitializableView initializableView = (InitializableView) this.currentCanvas;
                    final ViewPath initializableViewPath = viewPath;
                    new Timer() {
                        final long startTime = System.currentTimeMillis();

                        public void run() {
                            if (initializableView.isInitialized()) {
                                if (RootCanvas.this.currentCanvas instanceof BookmarkableView) {
                                    ((BookmarkableView) RootCanvas.this.currentCanvas).renderView(initializableViewPath
                                        .next());
                                } else {
                                    RootCanvas.this.currentCanvas.markForRedraw();
                                }
                            } else {
                                long elapsedMillis = System.currentTimeMillis() - startTime;
                                if (elapsedMillis < 10000) {
                                    schedule(100); // Reschedule the timer.
                                }
                            }
                        }
                    }.run(); // fire the timer immediately
                } else {
                    if (this.currentCanvas instanceof BookmarkableView) {
                        ((BookmarkableView) this.currentCanvas).renderView(viewPath.next());
                    } else {
                        this.currentCanvas.markForRedraw();
                    }
                }
            }
        }
    }

    public static boolean isDebugMode() {
        return !GWT.isScript();
    }
}
