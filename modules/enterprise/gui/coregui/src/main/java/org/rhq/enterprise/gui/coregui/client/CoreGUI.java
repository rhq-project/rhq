package org.rhq.enterprise.gui.coregui.client;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.bundle.BundleTopView;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourcesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceView;
import org.rhq.enterprise.gui.coregui.client.menu.MenuBarView;
import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;
import org.rhq.enterprise.gui.coregui.client.util.preferences.UserPreferences;

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
import com.smartgwt.client.util.KeyCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class CoreGUI implements EntryPoint {

    public static final String CONTENT_CANVAS_ID = "BaseContent";

    private static Subject sessionSubject;
    //private static Subject fullSubject;

    private static UserPreferences userPreferences;

    private static ErrorHandler errorHandler = new ErrorHandler();

    private static BreadcrumbTrailPane breadCrumbTrailPane;

    private static Canvas content;

    private View rootView;

    public View currentView;

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
                SC.say("Globally uncaught exception... " + e.getMessage());
                e.printStackTrace();
            }
        });

        RequestBuilder b = new RequestBuilder(RequestBuilder.GET, "/j_security_check.do?j_username=rhqadmin&j_password=rhqadmin");
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

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
        RootCanvas rootCanvas = new RootCanvas();
        this.currentView = this.rootView = new View(new ViewId("", null), rootCanvas);

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

        rootCanvas.draw();

        History.addValueChangeHandler(new ValueChangeHandler<String>() {

            public void onValueChange(ValueChangeEvent<String> historyChangeEvent) {
                String path = historyChangeEvent.getValue();
                System.out.println("History request: " + path);

                List<String> viewIdNames = path.equals("") ? Collections.<String>emptyList() : Arrays.asList(path.split("\\/"));
                String currentPath = CoreGUI.this.currentView.getId().getPath();
                List<String> currentViewIdNames = currentPath.equals("") ? Collections.<String>emptyList() : Arrays.asList(currentPath.split("\\/"));

                int commonBasePathSize = 0;
                for (int i = 0; i < viewIdNames.size() && i < currentViewIdNames.size(); i++) {
                    String name = viewIdNames.get(i);
                    String currentName = currentViewIdNames.get(i);
                    if (name.equals(currentName)) {
                        commonBasePathSize++;
                    } else {
                        break;
                    }
                }
                int startIndex;
                View parentView;
                if (commonBasePathSize > 0) {
                    // The requested path shares a common base path with the current view, so skip rendering of
                    // views corresponding to this common base path. For example, if the current view is
                    // Resource/10001/Summary/Overview and Resource/10001/Monitor/Graphs is requested, call renderView()
                    // only on for the Monitor and Graphs components of the path.
                    startIndex = commonBasePathSize;
                    int subViewsToRenderPathSize = viewIdNames.size() - commonBasePathSize;
                    parentView = CoreGUI.this.currentView;
                    for (int i = 0; i < subViewsToRenderPathSize; i++) {
                        parentView = parentView.getParent();
                    }
                } else {
                    // Otherwise, start at the root view (i.e. call renderView() for every component in the path).
                    startIndex = 0;
                    parentView = CoreGUI.this.rootView;
                }
                System.out.println("Starting parent view: " + parentView);

                ViewRenderer viewRenderer = parentView.getDescendantViewRenderer();
                try {
                    for (int i = startIndex, viewIdNamesSize = viewIdNames.size(); i < viewIdNamesSize; i++) {
                        String viewIdName = viewIdNames.get(i);
                        // See if the parent view provided a view renderer to use for its descendants. If not,
                        // continue using the view renderer that renderer the parent view.
                        ViewRenderer descendantViewRenderer = parentView.getDescendantViewRenderer();
                        if (descendantViewRenderer != null) {
                            viewRenderer = descendantViewRenderer;
                        }
                        ViewId viewId = new ViewId(viewIdName, parentView.getId());
                        boolean lastNode = (i == (viewIdNamesSize - 1));
                        View view = viewRenderer.renderView(viewId, lastNode);
                        view.setParent(parentView);

                        parentView = view;
                    }
                } catch (UnknownViewException e) {
                    // Abort the for-loop, since once we hit an unknown name, we don't care about any remaining names
                    // in the list. The breadcrumbs list will contain breadcrumbs for only the names that were
                    // recognized.
                    System.err.println(e.getMessage());
                    // TODO: Should we add a new token to the History to point to the valid location
                    //       we ended up at?
                }
                CoreGUI.this.currentView = parentView;

                // Update the breadcrumb trail.
                List<Breadcrumb> breadcrumbs = new LinkedList<Breadcrumb>();
                while (parentView.getParent() != null) {
                    breadcrumbs.add(0, parentView.getBreadcrumb());
                    parentView = parentView.getParent();
                }
                System.out.println("Breadcrumbs: " + breadcrumbs);
                breadCrumbTrailPane.setBreadcrumbs(breadcrumbs);
                breadCrumbTrailPane.refresh();
            }
        });

        History.fireCurrentHistoryState();
    }

    public Canvas createContent(String breadcrumbName) {
        Canvas canvas;
        if (breadcrumbName.equals("Administration")) {
            canvas = new AdministrationView();
        } else if (breadcrumbName.equals("Demo")) {
            canvas = new DemoCanvas();
        } else if (breadcrumbName.equals("Resources")) {
            canvas = new ResourcesView();
        } else if (breadcrumbName.equals("Resource")) {
            canvas = new ResourceView();
        } else if (breadcrumbName.equals("Dashboard")) {
            canvas = new DashboardView();
        } else if (breadcrumbName.equals("Bundles")) {
            canvas = new BundleTopView();
        } else {
            canvas = null;
        }
        return canvas;
    }


    // -------------------- Static application utilities ----------------------

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
        contentCanvas.draw();
    }

    public static void goTo(String path) {
        History.newItem(path);
    }

    public static void refreshBreadCrumbTrail() {
        breadCrumbTrailPane.refresh();
    }

    private class RootCanvas extends VLayout implements ViewRenderer {

        ViewId currentViewId;
        Canvas currentCanvas;

        private RootCanvas() {
            setWidth100(); // (1200);
            setHeight100(); // (900);
        }

        public View renderView(ViewId viewId, boolean lastNode) throws UnknownViewException {
            if (!viewId.equals(currentViewId)) {
                currentViewId = viewId;
                String path = viewId.getPath();
                Canvas canvas = createContent(path);
                if (canvas == null) {
                    throw new UnknownViewException();
                }
                currentCanvas = canvas;
                setContent(canvas);
            }
            ViewRenderer descendantViewRender = (currentCanvas instanceof ViewRenderer) ?
                    (ViewRenderer) currentCanvas : null;

            return new View(viewId, descendantViewRender);
        }
    }
}


