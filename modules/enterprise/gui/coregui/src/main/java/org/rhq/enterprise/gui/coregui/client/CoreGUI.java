package org.rhq.enterprise.gui.coregui.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.smartgwt.client.core.KeyIdentifier;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.KeyCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.bundle.BundleTopView;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardsView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceView;
import org.rhq.enterprise.gui.coregui.client.menu.MenuBarView;
import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.preferences.UserPreferences;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class CoreGUI implements EntryPoint, ValueChangeHandler<String> {

    public static final String CONTENT_CANVAS_ID = "BaseContent";

    private static Subject sessionSubject;

    private static UserPreferences userPreferences;

    private static ErrorHandler errorHandler = new ErrorHandler();

    private static BreadcrumbTrailPane breadCrumbTrailPane;

    private static MessageCenter messageCenter;

    private static Canvas content;

    private RootCanvas rootCanvas;

    private static ViewPath currentViewPath;

    public void onModuleLoad() {

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

        RequestBuilder b = new RequestBuilder(RequestBuilder.GET,
                "/j_security_check.do?j_username=rhqadmin&j_password=rhqadmin");
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(Request request, Response response) {
                    System.out.println("Portal-War logged in");
                }

                public void onError(Request request, Throwable exception) {
                    System.out.println("Portal-War login failed");
                }
            });
            b.send();
        } catch (RequestException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }

        messageCenter = new MessageCenter();

        SubjectGWTServiceAsync subjectService = SubjectGWTServiceAsync.Util.getInstance();

        subjectService.login("rhqadmin", "rhqadmin", new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                System.out.println("Failed to login - cause: " + caught);
                Label loginFailed = new Label("Failed to login - cause: " + caught);
                loginFailed.draw();
            }

            public void onSuccess(Subject result) {
                System.out.println("Logged in: " + result.getSessionId());
                setSessionSubject(result);
                userPreferences = new UserPreferences(result);

                buildCoreUI();

                /* We can cache all metadata right here
                ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                        (Integer[]) null, EnumSet.allOf(ResourceTypeRepository.MetadataType.class), new ResourceTypeRepository.TypesLoadedCallback() {
                    public void onTypesLoaded(HashMap<Integer, ResourceType> types) {
                        System.out.println("Preloaded [" + types.size() + "] resource types");
                        buildCoreUI();
                    }
                });
                */
            }
        });
    }

    private void buildCoreUI() {
        this.rootCanvas = new RootCanvas();
        rootCanvas.setOverflow(Overflow.HIDDEN);

        //        HTMLPane menuPane = new HTMLPane();
        //        menuPane.setWidth100();
        //        menuPane.setHeight(26);
        //        menuPane.setContentsType(ContentsType.PAGE);
        //        menuPane.setContentsURL("/rhq/common/menu/menu.xhtml");
        //        menuPane.setZIndex(400000);
        //        layout.addMember(menuPane);

        MenuBarView menuBarView = new MenuBarView();
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

        rootCanvas.addMember(new Footer());

        rootCanvas.draw();

        History.addValueChangeHandler(this);

        History.fireCurrentHistoryState();
    }


    public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
        System.out.println("Handling history event: " + stringValueChangeEvent.getValue());
        currentViewPath = new ViewPath(stringValueChangeEvent.getValue());

        rootCanvas.renderView(currentViewPath);

    }


    public Canvas createContent(String breadcrumbName) {
        Canvas canvas;

        if (breadcrumbName.equals("Administration")) {
            canvas = new AdministrationView();
        } else if (breadcrumbName.equals("Demo")) {
            canvas = new DemoCanvas();
        } else if (breadcrumbName.equals("Inventory")) {
            canvas = new InventoryView();
        } else if (breadcrumbName.equals("Resource")) {
            canvas = new ResourceView();
        } else if (breadcrumbName.equals("Dashboard")) {
            canvas = new DashboardsView();
        } else if (breadcrumbName.equals("Bundles")) {
            canvas = new BundleTopView();
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


    public static void setSessionSubject(Subject subject) {
        GWTServiceLookup.registerSession(String.valueOf(subject.getSessionId()));

        // TODO this breaks because of reattach rules, bizarely even in queries. gonna switch out to non-subject include apis
        // Create a minimized session object for validation on requests
        //        Subject s = new Subject(subject.getName(),subject.getFactive(), subject.getFsystem());
        //        s.setSessionId(subject.getSessionId());
        CoreGUI.sessionSubject = subject;
    }

    public static void setContent(Canvas newContent) {
        Canvas contentCanvas = Canvas.getById(CONTENT_CANVAS_ID);
        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();

        if (newContent != null) {
            content = newContent;
            contentCanvas.addChild(newContent);
        }
        contentCanvas.markForRedraw();
    }

    public static void goTo(String path) {
        History.newItem(path);
    }

    public static void refreshBreadCrumbTrail() {
        breadCrumbTrailPane.refresh(currentViewPath);
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
}
