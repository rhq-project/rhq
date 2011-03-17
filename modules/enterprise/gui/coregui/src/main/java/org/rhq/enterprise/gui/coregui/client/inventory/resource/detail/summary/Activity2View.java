package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardCategory;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.InitializableView;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardContainer;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MessagePortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.DashboardGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The content pane for the resource Summary>Dashboard subtab.
 *
 * @author Jay Shaughnessy
 */

public class Activity2View extends LocatableVLayout implements DashboardContainer, InitializableView {

    private static final String DASHBOARD_NAME_PREFIX = "ResourceDashboard_";

    private ResourceComposite resourceComposite;

    private DashboardGWTServiceAsync dashboardService = GWTServiceLookup.getDashboardService();

    private DashboardView dashboardView;

    private LocatableToolStrip footer;
    private IButton editButton;
    private IButton resetButton;

    // Capture the user's global permissions for use by any dashboard or portlet that may need it for rendering.
    private Set<Permission> globalPermissions;

    private boolean editMode = false;

    private boolean isInitialized = false;

    public Activity2View(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected void onInit() {
        if (!isInitialized()) {
            super.onInit();

            // first async call to get global permissions
            new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {

                public void onPermissionsLoaded(Set<Permission> permissions) {
                    globalPermissions = permissions;

                    // now make async call to look for customized dash for this user and entity
                    DashboardCriteria criteria = new DashboardCriteria();
                    criteria.addFilterCategory(DashboardCategory.RESOURCE);
                    criteria.addFilterResourceId(resourceComposite.getResource().getId());
                    dashboardService.findDashboardsByCriteria(criteria, new AsyncCallback<PageList<Dashboard>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error1(), caught);
                        }

                        public void onSuccess(final PageList<Dashboard> result) {
                            Dashboard dashboard = result.isEmpty() ? getDefaultDashboard() : result.get(0);
                            setDashboard(dashboard);

                            isInitialized = true;

                            // draw() may be done since onInit finishes asynchronously, if so redraw 
                            if (isDrawn()) {
                                markForRedraw();
                            }
                        }
                    });
                }
            });
        }
    }

    private void setDashboard(Dashboard dashboard) {
        Canvas[] members = getMembers();
        removeMembers(members);

        //pass in the resource information
        dashboardView = new DashboardView(extendLocatorId(dashboard.getName()), this, dashboard, null,
            this.resourceComposite.getResource());
        addMember(dashboardView);

        footer = new LocatableToolStrip(extendLocatorId("Footer"));
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        editButton = new LocatableIButton(footer.extendLocatorId("Mode"), editMode ? MSG.common_title_view_mode() : MSG
            .common_title_edit_mode());
        editButton.setAutoFit(true);
        editButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                editMode = !editMode;
                editButton.setTitle(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
                dashboardView.setEditMode(editMode);
            }
        });

        resetButton = new LocatableIButton(footer.extendLocatorId("Reset"), MSG.common_button_reset());
        resetButton.setAutoFit(true);
        resetButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                String message = MSG.view_summaryDashboard_resetConfirm();

                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            dashboardView.delete();
                            setDashboard(getDefaultDashboard());
                            markForRedraw();
                        }
                    }
                });
            }
        });

        footer.addMember(editButton);
        footer.addMember(resetButton);

        addMember(footer);
    }

    protected Dashboard getDefaultDashboard() {
        Subject sessionSubject = UserSessionManager.getSessionSubject();
        Resource resource = resourceComposite.getResource();

        Dashboard dashboard = new Dashboard();

        dashboard.setName(DASHBOARD_NAME_PREFIX + sessionSubject.getId() + "_" + resource.getId());
        dashboard.setCategory(DashboardCategory.RESOURCE);
        dashboard.setResource(resource);
        dashboard.setColumns(2);

        // TODO, add real portlets
        // set leftmost column and let the rest be equally divided
        dashboard.setColumnWidths("40%");
        dashboard.getConfiguration().put(new PropertySimple(Dashboard.CFG_BACKGROUND, "#F1F2F3"));

        // Left Column
        DashboardPortlet dummyLeft = new DashboardPortlet(MessagePortlet.NAME, MessagePortlet.KEY, 220);
        dummyLeft.getConfiguration().put(new PropertySimple("message", "<br/>Coming Soon... :-)"));
        dashboard.addPortlet(dummyLeft, 0, 0);

        // right Column
        DashboardPortlet dummyRight = new DashboardPortlet(MessagePortlet.NAME, MessagePortlet.KEY, 220);
        dummyRight.getConfiguration().put(new PropertySimple("message", "<br/>Coming Soon... :-)"));
        dashboard.addPortlet(dummyRight, 1, 0);

        return dashboard;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    public Set<Permission> getGlobalPermissions() {
        return globalPermissions;
    }

    /**
     * name update not supported because the name is derived from the entity id.
     * @return
     */
    public boolean supportsDashboardNameEdit() {
        return false;
    }

    public void updateDashboardNames() {
        return;
    }

}
