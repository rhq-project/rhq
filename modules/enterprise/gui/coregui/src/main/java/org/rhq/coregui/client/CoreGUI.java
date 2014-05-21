/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client;

import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.core.KeyIdentifier;
import com.smartgwt.client.i18n.SmartGwtMessages;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.KeyCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.coregui.client.admin.AdministrationView;
import org.rhq.coregui.client.alert.AlertHistoryView;
import org.rhq.coregui.client.bundle.BundleTopView;
import org.rhq.coregui.client.dashboard.DashboardsView;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.help.HelpView;
import org.rhq.coregui.client.help.RhAccessView;
import org.rhq.coregui.client.inventory.InventoryView;
import org.rhq.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.coregui.client.inventory.groups.detail.ResourceGroupTopView;
import org.rhq.coregui.client.inventory.resource.detail.ResourceTopView;
import org.rhq.coregui.client.menu.MenuBarView;
import org.rhq.coregui.client.report.ReportTopView;
import org.rhq.coregui.client.report.tag.TaggedView;
import org.rhq.coregui.client.test.TestDataSourceResponseStatisticsView;
import org.rhq.coregui.client.test.TestRemoteServiceStatisticsView;
import org.rhq.coregui.client.test.TestTopView;
import org.rhq.coregui.client.util.ErrorHandler;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.MessageCenter;

/**
 * The GWT {@link EntryPoint entry point} to the RHQ GUI.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class CoreGUI implements EntryPoint, ValueChangeHandler<String>, Event.NativePreviewHandler {

    public static final String CONTENT_CANVAS_ID = "BaseContent";

    // This must come first to ensure proper I18N class loading for dev mode
    private static final Messages MSG = GWT.create(Messages.class);
    private static final MessageConstants MSGCONST = GWT.create(MessageConstants.class);
    private static final SmartGwtMessages SMART_GWT_MSG = GWT.create(SmartGwtMessages.class);

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

    //spinder [BZ 731864] defining variable that can be set at build time to enable/disable TAG ui components.
    // This will be set to 'false' on the release branch.
    private static boolean enableTagsForUI = Boolean.valueOf(MSG.enable_tags());

    private static boolean rhq = true;

    public static boolean isRHQ() {
        return rhq;
    }

    public static boolean isTagsEnabledForUI() {
        return enableTagsForUI;
    }

    private RootCanvas rootCanvas;
    private MenuBarView menuBarView;
    private int rpcTimeout;
    private ProductInfo productInfo;

    @Override
    public void onModuleLoad() {
        String hostPageBaseURL = GWT.getHostPageBaseURL();
        if (!hostPageBaseURL.contains("/coregui/")) {
            Log.info("Suppressing load of CoreGUI module");
            return; // suppress loading this module if not using the new GWT app
        }

        rpcTimeout = -1;
        String rpcTimeoutParam = Location.getParameter("rpcTimeout");
        if (rpcTimeoutParam != null) {
            try {
                rpcTimeout = Integer.parseInt(rpcTimeoutParam) * 1000;
            } catch (NumberFormatException ignored) {
                // nada
            }
        }

        coreGUI = this;

        Event.addNativePreviewHandler(this);

        registerPageKeys();

        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable e) {
                if (e instanceof ViewChangedException) {
                    ViewChangedException viewChangedException = (ViewChangedException) e;
                    String obsoleteView = viewChangedException.getObsoleteView();
                    Log.debug("User navigated to view [" + currentView + "] before view [" + obsoleteView
                        + "] was done rendering - rendering of view [" + obsoleteView + "] has been aborted.");
                } else {
                    getErrorHandler().handleError(MSG.view_core_uncaught(), e);
                }
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

//        UserSessionManager.login();
//        UserSessionManager.login("rhqadmin", "rhqadmin");
        new LoginView().login("rhqadmin", "rhqadmin");
    }

    public int getRpcTimeout() {
        return rpcTimeout;
    }

    @Override
    public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
        if (SC.isIE() && event.getTypeInt() == Event.ONCLICK) {
            NativeEvent nativeEvent = event.getNativeEvent();
            EventTarget target = nativeEvent.getEventTarget();
            if (Element.is(target)) {
                Element element = Element.as(target);
                if ("a".equalsIgnoreCase(element.getTagName())) {
                    // make sure it's not a hyperlink that GWT already handles
                    if (element.getPropertyString("__listener") == null) {
                        String url = element.getAttribute("href");
                        String viewPath = getViewPath(url);
                        if (viewPath != null) {
                            GWT.log("Forcing History.newItem(\"" + viewPath + "\")...");
                            History.newItem(viewPath);
                            nativeEvent.preventDefault();
                        }
                    }
                }
            }
        }
    }

    private static String getViewPath(String url) {
        String token;
        if (url.startsWith("#")) {
            token = url.substring(1);
        } else if (url.startsWith("/#")) {
            token = url.substring(2);
        } else if (url.contains(Location.getHost()) && url.indexOf('#') > 0) {
            token = url.substring(url.indexOf('#') + 1);
        } else {
            token = null;
        }
        return token;
    }

    private void registerPageKeys() {
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

        // Control-Shift-S for aggregate rpc service stats
        KeyIdentifier statisticsWindowKey = new KeyIdentifier();
        statisticsWindowKey.setCtrlKey(true);
        statisticsWindowKey.setShiftKey(true);
        statisticsWindowKey.setAltKey(false);
        statisticsWindowKey.setKeyName("S");
        Page.registerKey(statisticsWindowKey, new KeyCallback() {
            public void execute(String keyName) {
                TestRemoteServiceStatisticsView.showInWindow();
            }
        });

        // Control-Alt-S for response stats
        KeyIdentifier responseStatsWindowKey = new KeyIdentifier();
        responseStatsWindowKey.setCtrlKey(true);
        statisticsWindowKey.setShiftKey(false);
        responseStatsWindowKey.setAltKey(true);
        responseStatsWindowKey.setKeyName("s");
        Page.registerKey(responseStatsWindowKey, new KeyCallback() {
            public void execute(String keyName) {
                TestDataSourceResponseStatisticsView.showInWindow();
            }
        });

        KeyIdentifier messageCenterWindowKey = new KeyIdentifier();
        messageCenterWindowKey.setCtrlKey(true);
        messageCenterWindowKey.setKeyName("M");
        Page.registerKey(messageCenterWindowKey, new KeyCallback() {
            public void execute(String keyName) {
                menuBarView.getMessageCenter().showMessageCenterWindow();
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
    }

    public static CoreGUI get() {
        return coreGUI;
    }

    public void init() {
        if (productInfo == null) {
            GWTServiceLookup.getSystemService().getProductInfo(new AsyncCallback<ProductInfo>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_aboutBox_failedToLoad(), caught);
                    buildCoreUI();
                }

                @Override
                public void onSuccess(ProductInfo result) {
                    productInfo = result;
                    rhq = (productInfo != null) && "RHQ".equals(productInfo.getShortName());
                    Window.setTitle(productInfo.getName());
                    buildCoreUI();
                }
            });
        } else {
            buildCoreUI();
        }
    }

    public ProductInfo getProductInfo() {
        return productInfo;
    }

    public void buildCoreUI() {
        // If the core gui is already built (eg. from previous login) skip the build and just refire event
        if (rootCanvas == null) {
            menuBarView = new MenuBarView();
            menuBarView.setWidth("100%");
            menuBarView.setExtraSpace(0);

            Canvas canvas = new Canvas(CONTENT_CANVAS_ID);
            canvas.setWidth100();
            canvas.setHeight100();
            canvas.setZIndex(0);

            rootCanvas = new RootCanvas();
            rootCanvas.setOverflow(Overflow.HIDDEN);

            rootCanvas.addMember(menuBarView);
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

            // otherwise just fire an event for the bookmarked URL they are returning to and
            // ensure it is refreshed if it is a RefreshableView
            pendingRefresh = true;
            History.fireCurrentHistoryState();
        }

        // The root canvas is hidden by user log out, show again if necessary
        if (!rootCanvas.isVisible()) {
            rootCanvas.show();
        }
    }

    @Override
    public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
        currentView = URL.decodeQueryString(stringValueChangeEvent.getValue());
        Log.debug("Handling history event for view: " + currentView);

        currentViewPath = new ViewPath(currentView);
        coreGUI.rootCanvas.renderView(currentViewPath);
    }

    public static native void showBusy(boolean on) /*-{
      if ($wnd.loadQ == undefined) {
        $wnd.loadQ = [];
      }
      if (on) {
        $wnd.loadQ.push(on);
        $wnd.Pace.restart();
      } else {
        $wnd.loadQ.shift();
        if ($wnd.loadQ.length < 1) {
          $wnd.Pace.stop();
        }
      }
    }-*/;


    public static void refresh() {
        showBusy(true);
        currentViewPath = new ViewPath(currentView, true);
        coreGUI.rootCanvas.renderView(currentViewPath);
        showBusy(false);
    }

    public Canvas createContent(String viewName) {
        Canvas canvas;

//        if (viewName.equals(LOGOUT_VIEW) || LoginView.isLoginView()) {
//            UserSessionManager.logout();
//            rootCanvas.hide();
//            LoginView logoutView = new LoginView();
//            canvas = logoutView;
//            logoutView.showLoginDialog();
        if (viewName.equals(DashboardsView.VIEW_ID.getName())) {
            canvas = new DashboardsView();
        } else if (viewName.equals(InventoryView.VIEW_ID.getName())) {
            canvas = new InventoryView();
        } else if (viewName.equals(ResourceTopView.VIEW_ID.getName())) {
            canvas = new ResourceTopView();
        } else if (viewName.equals(ResourceGroupTopView.VIEW_ID.getName())) {
            canvas = new ResourceGroupTopView();
        } else if (viewName.equals(ReportTopView.VIEW_ID.getName())) {
            canvas = new ReportTopView();
        } else if (viewName.equals(BundleTopView.VIEW_ID.getName())) {
            canvas = new BundleTopView();
        } else if (viewName.equals(AdministrationView.VIEW_ID.getName())) {
            canvas = new AdministrationView();
        } else if (viewName.equals(HelpView.VIEW_ID.getName())) {
            canvas = new HelpView();
        } else if (viewName.equals(TaggedView.VIEW_ID.getName())) {
            canvas = new TaggedView();
        } else if (viewName.equals("Subsystems")) {
            canvas = new AlertHistoryView();
        } else if (viewName.equals(TestTopView.VIEW_ID.getName())) {
            canvas = new TestTopView();
        } else if (!isRHQ() && viewName.equals(RhAccessView.VIEW_ID.getName())) {
            canvas = new RhAccessView();
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
                // TODO: need to support string IDs "Drift/History/0id_abcdefghijk"
                // TODO: see StringIDTableSection.ID_PREFIX
                // TODO: remember \D is a non-digit, and \d is a digit
                // TODO: String suffix = currentViewPath.replaceFirst("\\D*[^/]*", ""); // this might be OK if 0id_ starts with a digit
                // TODO: suffix = suffix.replaceFirst("((\\d.*)|(0id_[^/]*))", "");
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

    public static SmartGwtMessages getSmartGwtMessages() {
        return SMART_GWT_MSG;
    }

    public void reset() {
        messageCenter.reset();
    }

    private class RootCanvas extends VLayout implements BookmarkableView {
        private ViewId currentViewId;
        private Canvas currentCanvas;

        private RootCanvas() {
            setWidth100();
            setHeight100();
        }

        // TODO (ips, 09/06/11): i18n the title.
        private String getViewPathTitle(ViewPath viewPath) {
            // Set the default title to the view path minus any IDs.
            StringBuilder viewPathTitle = new StringBuilder();
            String productName = (productInfo != null) ? productInfo.getName() : "RHQ";
            List<ViewId> viewIds = viewPath.getViewPath();
            if (!viewIds.isEmpty()) {
                viewPathTitle.append(productName).append(": ");
                viewPathTitle.append(viewIds.get(0));
                for (int i = 1, viewPathSize = viewIds.size(); i < viewPathSize; i++) {
                    ViewId viewId = viewIds.get(i);
                    // None of our path elements start with a digit that is NOT an ID, so if we see an ID, skip it.
                    if (!Character.isDigit(viewId.getPath().charAt(0))) {
                        viewPathTitle.append(" | ");
                        viewPathTitle.append(viewId.getPath());
                    }
                }
            }
            return viewPathTitle.toString();
        }

        public void initCanvas() {
            // request a redraw of the MenuBarItem to ensure the correct session info is displayed
            getMember(0).markForRedraw();

            // remove any current viewId so the next requested view generates new content
            this.currentViewId = null;
        }

        @Override
        public void renderView(final ViewPath viewPath) {
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
                coreGUI.menuBarView.getMessageBar().clearMessage(true);
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
                    final Canvas contentCanvas = Canvas.getById(CONTENT_CANVAS_ID);
                    Canvas[] children;
                    while ((children = contentCanvas.getChildren()).length > 0) {
                        children[0].destroy();
                    }

                    // Set the new content and redraw
                    this.currentViewId = topLevelViewId;

                    // Using GWT Code Splitting feature to decrease the size of generated JS code using lazy
                    // fetching. Each view built in createContent method has its Java Script code in a separate
                    // file.
                    final long startTime = System.currentTimeMillis();
                    GWT.runAsync(new RunAsyncCallback() {
                        public void onFailure(Throwable caught) {
                          Window.alert("Code download failed");
                          Log.error("Code download failed");
                        }

                        public void onSuccess() {
                            RootCanvas.this.currentCanvas = createContent(RootCanvas.this.currentViewId.getPath());
                            if (null != RootCanvas.this.currentCanvas) {
                                contentCanvas.addChild(RootCanvas.this.currentCanvas);
                            }
                            contentCanvas.markForRedraw();
                            render(viewPath);
                            Log.info("Time to Load first codesplit fragment: "+(System.currentTimeMillis() - startTime) + " ms.");
                        }
                      });
                } else {
                    render(viewPath);
                }
            }
        }

        private void render(ViewPath viewPath) {
            if (this.currentCanvas instanceof InitializableView) {
                final InitializableView initializableView = (InitializableView) this.currentCanvas;
                final ViewPath initializableViewPath = viewPath;
                new Timer() {
                    final long startTime = System.currentTimeMillis();

                    @Override
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
                if (this.currentCanvas != null) {
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
