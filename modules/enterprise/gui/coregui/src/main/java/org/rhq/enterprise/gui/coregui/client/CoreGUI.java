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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.smartgwt.client.core.KeyIdentifier;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.KeyCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.alert.AlertsView;
import org.rhq.enterprise.gui.coregui.client.bundle.BundleTopView;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardsView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupTopView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTopView;
import org.rhq.enterprise.gui.coregui.client.menu.MenuBarView;
import org.rhq.enterprise.gui.coregui.client.report.ReportTopView;
import org.rhq.enterprise.gui.coregui.client.report.tag.TaggedView;
import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;
import org.rhq.enterprise.gui.coregui.client.util.WidgetUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.preferences.UserPreferences;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class CoreGUI implements EntryPoint, ValueChangeHandler<String> {

    public static final String CONTENT_CANVAS_ID = "BaseContent";

    private static Subject sessionSubject;

    private static Timer sessionTimer = new Timer() {
        @Override
        public void run() {
            System.out.println("Session Timer Expired");
            new LoginView(true).showLoginDialog(); // log user out, show login dialog
        }
    };

    private static UserPreferences userPreferences;

    private static ErrorHandler errorHandler = new ErrorHandler();

    private static BreadcrumbTrailPane breadCrumbTrailPane;

    private static MessageCenter messageCenter;

    private static String currentPath;

    @SuppressWarnings("unused")
    private static Canvas content;

    private RootCanvas rootCanvas;

    private static ViewPath currentViewPath;

    private static CoreGUI coreGUI;

    private static Messages messages;

    private static ProductInfo productInfo;

    public void onModuleLoad() {
        String hostPageBaseURL = GWT.getHostPageBaseURL();
        if (hostPageBaseURL.indexOf("/coregui/") == -1) {
            System.out.println("Suppressing load of CoreGUI module");
            return; // suppress loading this module if not using the new GWT app
        }

        String enableLocators = Location.getParameter("enableLocators");
        if ((null != enableLocators) && Boolean.parseBoolean(enableLocators)) {
            SeleniumUtility.setUseDefaultIds(false);
        }

        coreGUI = this;

        if (!GWT.isScript()) {
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
                getErrorHandler().handleError("Globally uncaught exception", e);
            }
        });

        messageCenter = new MessageCenter();

        messages = GWT.create(Messages.class);

        checkLoginStatus();
    }

    public static void checkLoginStatus() {
        //        String sessionIdString = com.google.gwt.user.client.Cookies.getCookie("RHQ_Sesssion");
        //        if (sessionIdString == null) {

        if (detectIe6()) {
            forceIe6Hacks();
        }

        RequestBuilder b = new RequestBuilder(RequestBuilder.GET, "/sessionAccess");
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {
                    String sessionIdString = response.getText();
                    if (sessionIdString != null && sessionIdString.length() > 0) {

                        int subjectId = Integer.parseInt(sessionIdString.split(":")[0]);
                        final int sessionId = Integer.parseInt(sessionIdString.split(":")[1]);

                        Subject subject = new Subject();
                        subject.setId(subjectId);
                        subject.setSessionId(sessionId);

                        GWTServiceLookup.registerSession(String.valueOf(subject.getSessionId()));

                        // look up real user prefs

                        SubjectCriteria criteria = new SubjectCriteria();
                        criteria.fetchConfiguration(true);
                        criteria.addFilterId(subjectId);
                        //criteria.fetchRoles(true);

                        GWTServiceLookup.getSubjectService().findSubjectsByCriteria(criteria,
                            new AsyncCallback<PageList<Subject>>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError("Failed to load user's subject", caught);
                                    new LoginView().showLoginDialog();
                                }

                                public void onSuccess(PageList<Subject> result) {

                                    Subject subject = result.get(0);
                                    subject.setSessionId(sessionId);
                                    setSessionSubject(subject);
                                    //System.out.println("Portal-War logged in");
                                }
                            });
                    } else {
                        new LoginView().showLoginDialog();
                    }
                }

                public void onError(Request request, Throwable exception) {
                    SC.say("Unable to determine login status, check server status");
                }
            });
            b.send();
        } catch (RequestException e) {
            SC.say("Unable to determine login status, check server status");
            e.printStackTrace();
        } finally {
            if (detectIe6()) {
                unforceIe6Hacks();
            }
        }
    }

    public static void refreshSessionTimer() {
        System.out.println("Refreshing Session Timer");
        sessionTimer.schedule(29 * 60 * 1000); // 29 minutes from now, timeout before the http session timeout
    }

    public static void destroySessionTimer() {
        System.out.println("Destroying Session Timer");
        sessionTimer.cancel();
    }

    private void buildCoreUI() {
        // If the core gui is already built (eg. from previous login, just refire event)
        if (this.rootCanvas == null) {
            this.rootCanvas = new RootCanvas();
            rootCanvas.setOverflow(Overflow.HIDDEN);

            //        HTMLPane menuPane = new HTMLPane();
            //        menuPane.setWidth100();
            //        menuPane.setHeight(26);
            //        menuPane.setContentsType(ContentsType.PAGE);
            //        menuPane.setContentsURL("/rhq/common/menu/menu.xhtml");
            //        menuPane.setZIndex(400000);
            //        layout.addMember(menuPane);

            MenuBarView menuBarView = new MenuBarView("TopMenu");
            menuBarView.setWidth("100%");
            //        WidgetCanvas menuCanvas = new WidgetCanvas(menuBarView);
            //        menuCanvas.setTop(0);
            //        menuCanvas.setWidth100();
            //        menuCanvas.draw();
            rootCanvas.addMember(menuBarView);

            breadCrumbTrailPane = new BreadcrumbTrailPane();
            rootCanvas.addMember(breadCrumbTrailPane);

            DOM.setInnerHTML(RootPanel.get("Loading-Panel").getElement(), "");

            Canvas canvas = new Canvas(CONTENT_CANVAS_ID);
            canvas.setWidth100();
            canvas.setHeight100();
            rootCanvas.addMember(canvas);

            rootCanvas.addMember(new Footer("CoreFooter"));

            rootCanvas.draw();

            History.addValueChangeHandler(this);
        }

        History.fireCurrentHistoryState();
    }

    public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
        String event = URL.decodeComponent(stringValueChangeEvent.getValue());
        //System.out.println("Handling history event: " + event);
        currentPath = event;

        currentViewPath = new ViewPath(event);

        rootCanvas.renderView(currentViewPath);
    }

    public static void refresh() {
        currentViewPath = new ViewPath(currentPath);

        currentViewPath.setRefresh(true);
        coreGUI.rootCanvas.renderView(currentViewPath);

    }

    public Canvas createContent(String breadcrumbName) {
        Canvas canvas;

        if (breadcrumbName.equals("Administration")) {
            canvas = new AdministrationView("Admin");
        } else if (breadcrumbName.equals("Demo")) {
            canvas = new DemoCanvas();
        } else if (breadcrumbName.equals("Inventory")) {
            canvas = new InventoryView("Inventory");
        } else if (breadcrumbName.equals("Resource")) {
            canvas = new ResourceTopView("Resource");
        } else if (breadcrumbName.equals("ResourceGroup")) {
            canvas = new ResourceGroupTopView("Group");
        } else if (breadcrumbName.equals("Dashboard")) {
            canvas = new DashboardsView("Dashboard");
        } else if (breadcrumbName.equals("Bundles")) {
            canvas = new BundleTopView("Bundle");
        } else if (breadcrumbName.equals("LogOut")) {
            canvas = new LoginView(true);
        } else if (breadcrumbName.equals("Tag")) {
            canvas = new TaggedView("Tag");
        } else if (breadcrumbName.equals("Subsystems")) {
            canvas = new AlertsView("Alert");
        } else if (breadcrumbName.equals("Reports")) {
            canvas = new ReportTopView("Report");
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

    public static Subject getSessionSubject() {
        return sessionSubject;
    }

    public static UserPreferences getUserPreferences() {
        return userPreferences;
    }

    public static void printWidgetTree() {
        WidgetUtility.printWidgetTree(coreGUI.rootCanvas);
    }

    public static void setSessionSubject(Subject subject) {
        // TODO this breaks because of reattach rules, bizarrely even in queries. gonna switch out to non-subject include apis
        // Create a minimized session object for validation on requests
        //        Subject s = new Subject(subject.getName(),subject.getFactive(), subject.getFsystem());
        //        s.setSessionId(subject.getSessionId());
        CoreGUI.sessionSubject = subject;
        CoreGUI.userPreferences = new UserPreferences(subject);
        loadProductInfo();
        // After a user initiated logout start back at the default view        
        if ("LogOut".equals(CoreGUI.currentPath)) {
            History.newItem(getDefaultView());
        }
    }

    private static String getDefaultView() {
        // TODO: should this be Dashboard or a User Preference?
        return "";
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
        String currentViewPath = History.getToken();
        if (currentViewPath.equals(viewPath)) {
            // We're already there - just refresh the view.
            refresh();
        } else {
            if (viewPath.matches("(Resource|ResourceGroup)/[^/]*")) {
                // e.g. "Resource/10001"
                if (!currentViewPath.startsWith(viewPath)) {
                    // The Resource that was selected is not the same Resource that was previously selected -
                    // grab the end portion of the previous history URL and append it to the new history URL,
                    // so the same tab is selected for the new Resource.
                    String suffix = currentViewPath.replaceFirst("^[^/]*/[^/]*", "");
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

    public static ProductInfo getProductInfo() {
        return productInfo;
    }

    private static void loadProductInfo() {
        GWTServiceLookup.getSystemService().getProductInfo(new AsyncCallback<ProductInfo>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load product information.", caught);
            }

            public void onSuccess(ProductInfo result) {
                productInfo = result;
                coreGUI.buildCoreUI();
            }
        });
    }

    public static void goToResourceOrGroupView(String newToken) {

    }

    private class RootCanvas extends VLayout implements BookmarkableView {

        ViewId currentViewId;
        Canvas currentCanvas;

        private RootCanvas() {
            setWidth100();
            setHeight100();
        }

        public void renderView(ViewPath viewPath) {
            if (viewPath.isEnd()) {
                // default view
                History.newItem("Dashboard");
            } else {
                if (!viewPath.getCurrent().equals(currentViewId)) {
                    currentViewId = viewPath.getCurrent();

                    currentCanvas = createContent(viewPath.getCurrent().getPath());
                    setContent(currentCanvas);
                }
                if (currentCanvas instanceof BookmarkableView) {
                    ((BookmarkableView) currentCanvas).renderView(viewPath.next()); // e.g.
                }

                refreshBreadCrumbTrail();
            }
        }
    }

    /**
     * Detects IE6.
     * <p/>
     * This is a nasty hack; but it's extremely reliable when running with other
     * js libraries on the same page at the same time as gwt.
     */
    public static native boolean detectIe6()
    /*-{
        if (typeof $doc.body.style.maxHeight != "undefined")
            return(false);
        else
            return(true);
    }-*/;

    public static native void forceIe6Hacks()
    /*-{
        $wnd.XMLHttpRequestBackup = $wnd.XMLHttpRequest;
        $wnd.XMLHttpRequest = null;
    }-*/;

    public static native void unforceIe6Hacks()
    /*-{
        $wnd.XMLHttpRequest = $wnd.XMLHttpRequestBackup;
        $wnd.XMLHttpRequestBackup = null;
    }-*/;

}
