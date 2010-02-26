package org.rhq.enterprise.gui.coregui.client;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourcesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceView;
import org.rhq.enterprise.gui.coregui.client.menu.MenuBarView;
import org.rhq.enterprise.gui.coregui.client.places.Place;
import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;

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


/**
 * @author Greg Hinkle
 */
public class CoreGUI implements EntryPoint {

    public static final String CONTENT_CANVAS_ID = "BaseContent";

    private static Subject sessionSubject;
    private static Subject fullSubject;

    private static ErrorHandler errorHandler = new ErrorHandler();

    private static BreadCrumb breadCrumb;

    private static Canvas content;

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

        VLayout layout = new VLayout();
        layout.setWidth100();//(1200);
        layout.setHeight100(); //(900);

//        HTMLPane menuPane = new HTMLPane();
//        menuPane.setWidth100();
//        menuPane.setHeight(26);
//        menuPane.setContentsType(ContentsType.PAGE);
//        menuPane.setContentsURL("/rhq/common/menu/menu.xhtml");
//        menuPane.setZIndex(400000);
//
//        layout.addMember(menuPane);


        MenuBarView menuBarView = new MenuBarView();
        menuBarView.setWidth("100%");
//        WidgetCanvas menuCanvas = new WidgetCanvas(menuBarView);
//        menuCanvas.setTop(0);
//        menuCanvas.setWidth100();
//        menuCanvas.draw();

        layout.addMember(menuBarView);


        breadCrumb = new BreadCrumb();

        layout.addMember(breadCrumb);


        DOM.setInnerHTML(RootPanel.get("Loading-Panel").getElement(), "");


        Canvas canvas = new Canvas(CONTENT_CANVAS_ID);
        canvas.setWidth100();
        canvas.setHeight100();

        layout.addMember(canvas);


//        canvas.addChild(new AdministrationView()) ; //DemoCanvas());


        layout.draw();


        breadCrumb.initialize(History.getToken());


        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> historyChangeEvent) {
                System.out.println("History request: " + historyChangeEvent.getValue());


                breadCrumb.verify(historyChangeEvent.getValue());

                ArrayList<Place> trail = breadCrumb.getTrail();

                Place base = trail.get(0);


                if (content != null && content instanceof Presenter) {
                    Presenter p = ((Presenter) content);
                    if (!p.fireDisplay(base, trail.subList(1, trail.size()))) {
                        Canvas c = displayContent(base.getId());
                        setContent(c);

                        if (trail.size() >= 2 && c instanceof Presenter) {
                            ((Presenter) c).fireDisplay(trail.get(0), trail.subList(1, trail.size()));
                        }
                    }
                } else {
                    Canvas c = displayContent(base.getId());
                    setContent(c);
                    if (trail.size() >= 2 && c instanceof Presenter) {
                        ((Presenter) c).fireDisplay(trail.get(0), trail.subList(1, trail.size()));
                    }
                }
            }
        });

        History.fireCurrentHistoryState();
    }

    public Canvas displayContent(String key) {
        Canvas c = null;
        if (key.equals("Administration")) {
            c = new AdministrationView();
        } else if (key.equals("Demo")) {
            c = new DemoCanvas();
        } else if (key.equals("Resources")) {
            c = new ResourcesView();
        } else if (key.equals("Resource")) {
            c = new ResourceView();
        } else if (key.equals("Dashboard")) {
            c = new DashboardView();
        }
        return c;
    }


    // -------------------- Static application utilities ----------------------

    public static ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public static Subject getSessionSubject() {
        return sessionSubject;
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

    public static void addBreadCrumb(Place place) {
        breadCrumb.addPlace(place);

    }

    public static void setBreadCrumb(Place place) {
        breadCrumb.setPlace(place);

    }

}


